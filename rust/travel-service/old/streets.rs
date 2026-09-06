use geo::{Bearing, Distance, Haversine, Point};
use osm4routing::{BikeAccessibility, CarAccessibility, FootAccessibility};
use petgraph::graph::{Graph, NodeIndex};
use rstar::primitives::GeomWithData;
use rstar::RTree;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

pub const WALK_SPEED_MPS: f64 = 1.0;
pub const BIKE_SPEED_MPS: f64 = 12.0 / 3.6;
pub const SNAP_RADIUS_METERS: f64 = 300.0;
pub const MAX_TRIP_SECONDS: u32 = 90 * 60;
pub const UNREACHABLE: u32 = u32::MAX;

const EARTH_RADIUS_M: f64 = 6_371_000.0;

#[derive(Clone, Copy, PartialEq)]
pub enum Mode {
    Walk,
    Bike,
    Car,
}

const FLAG_WALK: u8 = 1;
const FLAG_BIKE: u8 = 2;
const FLAG_CAR: u8 = 4;

#[derive(Clone, Copy)]
pub struct SnapPoint {
    a: u32,
    b: u32,
    to_a_mm: u32,
    to_b_mm: u32,
    car_mmps: u32,
    flags: u8,
}

impl SnapPoint {
    pub fn endpoints(&self) -> [u32; 2] {
        [self.a, self.b]
    }
}

#[derive(Serialize, Deserialize)]
struct SnapVertex {
    a: u32,
    b: u32,
    to_a_mm: u32,
    to_b_mm: u32,
    car_mmps: u32,
    flags: u8,
}

#[derive(Serialize, Deserialize)]
struct CarEdge {
    to: u32,
    secs: u32,
    start_deg: u16,
    end_deg: u16,
}

#[derive(Serialize, Deserialize)]
pub struct StreetGraph {
    walk: Graph<(), u32>,
    bike: Graph<(), u32>,
    car_edges: Vec<CarEdge>,
    car_out: Vec<Vec<u32>>,
    verts: Vec<SnapVertex>,
    tree: RTree<GeomWithData<[f64; 2], u32>>,
    node_lat: Vec<f64>,
    node_lon: Vec<f64>,
    lat_scale: f64,
    lon_scale: f64,
}

fn car_allowed(class: CarAccessibility) -> bool {
    !matches!(class, CarAccessibility::Unknown | CarAccessibility::Forbidden)
}

fn osm_default_kph(highway: &str) -> f64 {
    match highway {
        "motorway" | "motorway_link" => 110.0,
        "trunk" | "trunk_link" => 80.0,
        "primary" | "primary_link" => 60.0,
        "secondary" | "secondary_link" => 50.0,
        "tertiary" | "tertiary_link" => 40.0,
        "living_street" | "service" => 15.0,
        _ => 30.0,
    }
}

fn parse_maxspeed(value: &str) -> Option<f64> {
    let v = value.trim();
    if let Some(mph) = v.strip_suffix("mph") {
        return mph.trim().parse::<f64>().ok().map(|m| m * 1.60934);
    }
    v.parse::<f64>().ok()
}

fn bike_allowed(access: BikeAccessibility) -> bool {
    !matches!(
        access,
        BikeAccessibility::Unknown | BikeAccessibility::Forbidden
    )
}

pub fn build_street_graph(osm_path: &str) -> Result<StreetGraph, String> {
    let cache_path = format!("{osm_path}.streets.v3.bin");
    let osm_modified = std::fs::metadata(osm_path)
        .and_then(|m| m.modified())
        .map_err(|e| e.to_string())?;
    if let Ok(cache_meta) = std::fs::metadata(&cache_path) {
        if cache_meta.modified().map(|m| m > osm_modified).unwrap_or(false) {
            if let Ok(bytes) = std::fs::read(&cache_path) {
                if let Ok(graph) = bincode::deserialize::<StreetGraph>(&bytes) {
                    println!("street graph loaded from {cache_path}");
                    return Ok(graph);
                }
            }
        }
    }
    let graph = parse_street_graph(osm_path)?;
    match bincode::serialize(&graph) {
        Ok(bytes) => {
            if let Err(e) = std::fs::write(&cache_path, bytes) {
                println!("street cache not written: {e}");
            } else {
                println!("street graph cached to {cache_path}");
            }
        }
        Err(e) => println!("street cache not serialized: {e}"),
    }
    Ok(graph)
}

fn parse_street_graph(osm_path: &str) -> Result<StreetGraph, String> {
    let (nodes, edges) = osm4routing::Reader::new()
        .read_tag("maxspeed")
        .read_tag("highway")
        .read_tag("route")
        .read(osm_path)
        .map_err(|e| e.to_string())?;

    let mut index: HashMap<i64, u32> = HashMap::with_capacity(nodes.len());
    let mut node_lat: Vec<f64> = Vec::with_capacity(nodes.len());
    let mut node_lon: Vec<f64> = Vec::with_capacity(nodes.len());
    let mut mean_lat = 0.0;
    for (n, node) in nodes.iter().enumerate() {
        index.insert(node.id.0, n as u32);
        node_lat.push(node.coord.y);
        node_lon.push(node.coord.x);
        mean_lat += node.coord.y;
    }
    mean_lat /= nodes.len().max(1) as f64;
    let lat_scale = EARTH_RADIUS_M * std::f64::consts::PI / 180.0;
    let lon_scale = mean_lat.to_radians().cos() * lat_scale;

    let mut walk: Graph<(), u32> = Graph::with_capacity(nodes.len(), edges.len() * 2);
    let mut bike: Graph<(), u32> = Graph::with_capacity(nodes.len(), edges.len() * 2);
    for _ in &nodes {
        walk.add_node(());
        bike.add_node(());
    }
    let mut car_edges: Vec<CarEdge> = Vec::new();
    let mut car_out: Vec<Vec<u32>> = vec![Vec::new(); nodes.len()];

    let mut verts: Vec<SnapVertex> = Vec::new();
    let mut points: Vec<GeomWithData<[f64; 2], u32>> = Vec::new();

    for edge in &edges {
        let (Some(&a), Some(&b)) = (index.get(&edge.source.0), index.get(&edge.target.0)) else {
            continue;
        };
        if a == b {
            continue;
        }
        if edge.tags.get("route").map(|r| r == "ferry").unwrap_or(false) {
            continue;
        }
        let length = edge.length();
        if length <= 0.0 {
            continue;
        }
        let len_mm = (length * 1000.0) as u32;
        let properties = edge.properties;

        let walk_ok = properties.foot == FootAccessibility::Allowed;
        let highway = edge.tags.get("highway").map(|h| h.as_str()).unwrap_or("");
        let high_stress = matches!(
            highway,
            "primary" | "primary_link" | "trunk" | "trunk_link" | "motorway" | "motorway_link"
        );
        let stress_ok = |access: BikeAccessibility| -> bool {
            !high_stress
                || matches!(
                    access,
                    BikeAccessibility::Lane | BikeAccessibility::Busway | BikeAccessibility::Track
                )
        };
        let bike_fwd = bike_allowed(properties.bike_forward) && stress_ok(properties.bike_forward);
        let bike_back = bike_allowed(properties.bike_backward) && stress_ok(properties.bike_backward);
        let class_kph = osm_default_kph(highway);
        let car_fwd = car_allowed(properties.car_forward);
        let car_back = car_allowed(properties.car_backward);
        let maxspeed = edge.tags.get("maxspeed").and_then(|v| parse_maxspeed(v));
        let car_kph = maxspeed.unwrap_or(class_kph);
        let car_mmps = ((car_kph / 3.6) * 1000.0).max(1.0) as u32;
        let car_secs = len_mm.div_ceil(car_mmps).max(1);

        let na = NodeIndex::new(a as usize);
        let nb = NodeIndex::new(b as usize);
        let g = &edge.geometry;
        let bearing = |i: usize, j: usize| -> u16 {
            let p = Point::new(g[i].x, g[i].y);
            let q = Point::new(g[j].x, g[j].y);
            let deg = Haversine.bearing(p, q);
            (((deg % 360.0) + 360.0) % 360.0) as u16
        };
        let n = g.len();
        let (fwd_start, fwd_end, back_start, back_end) = if n >= 2 {
            (bearing(0, 1), bearing(n - 2, n - 1), bearing(n - 1, n - 2), bearing(1, 0))
        } else {
            (0, 0, 0, 0)
        };

        if walk_ok {
            let w = ((len_mm as f64 / (WALK_SPEED_MPS * 1000.0)) as u32).max(1);
            walk.add_edge(na, nb, w);
            walk.add_edge(nb, na, w);
        }
        let bike_w = ((len_mm as f64 / (BIKE_SPEED_MPS * 1000.0)) as u32).max(1);
        if bike_fwd {
            bike.add_edge(na, nb, bike_w);
        }
        if bike_back {
            bike.add_edge(nb, na, bike_w);
        }
        if car_fwd {
            car_out[a as usize].push(car_edges.len() as u32);
            car_edges.push(CarEdge {
                to: b,
                secs: car_secs,
                start_deg: fwd_start,
                end_deg: fwd_end,
            });
        }
        if car_back {
            car_out[b as usize].push(car_edges.len() as u32);
            car_edges.push(CarEdge {
                to: a,
                secs: car_secs,
                start_deg: back_start,
                end_deg: back_end,
            });
        }

        let mut flags = 0u8;
        if walk_ok {
            flags |= FLAG_WALK;
        }
        if bike_fwd || bike_back {
            flags |= FLAG_BIKE;
        }
        if car_fwd || car_back {
            flags |= FLAG_CAR;
        }
        if flags == 0 {
            continue;
        }

        let mut cum_mm = 0u32;
        for (i, coord) in edge.geometry.iter().enumerate() {
            if i > 0 {
                let previous = edge.geometry[i - 1];
                let p = Point::new(previous.x, previous.y);
                let q = Point::new(coord.x, coord.y);
                cum_mm += (Haversine.distance(p, q) * 1000.0) as u32;
            }
            let vert = verts.len() as u32;
            verts.push(SnapVertex {
                a,
                b,
                to_a_mm: cum_mm,
                to_b_mm: len_mm.saturating_sub(cum_mm),
                car_mmps,
                flags,
            });
            points.push(GeomWithData::new(
                [coord.x * lon_scale / lat_scale, coord.y],
                vert,
            ));
        }
    }

    let tree = RTree::bulk_load(points);

    Ok(StreetGraph {
        walk,
        bike,
        car_edges,
        car_out,
        verts,
        tree,
        node_lat,
        node_lon,
        lat_scale,
        lon_scale,
    })
}

impl StreetGraph {
    pub fn node_count(&self) -> usize {
        self.walk.node_count()
    }

    fn mode_flag(mode: Mode) -> u8 {
        match mode {
            Mode::Walk => FLAG_WALK,
            Mode::Bike => FLAG_BIKE,
            Mode::Car => FLAG_CAR,
        }
    }

    pub fn snap(&self, lat: f64, lon: f64, mode: Mode) -> Option<SnapPoint> {
        let flag = Self::mode_flag(mode);
        let query = [lon * self.lon_scale / self.lat_scale, lat];
        let radius_deg = SNAP_RADIUS_METERS / self.lat_scale;
        let target = Point::new(lon, lat);
        let mut best: Option<(u32, f64)> = None;
        for candidate in self
            .tree
            .locate_within_distance(query, radius_deg * radius_deg)
        {
            let vert = &self.verts[candidate.data as usize];
            if vert.flags & flag == 0 {
                continue;
            }
            let lon_deg = candidate.geom()[0] * self.lat_scale / self.lon_scale;
            let point = Point::new(lon_deg, candidate.geom()[1]);
            let d = Haversine.distance(target, point);
            if d <= SNAP_RADIUS_METERS && best.map(|(_, bd)| d < bd).unwrap_or(true) {
                best = Some((candidate.data, d));
            }
        }
        best.map(|(v, _)| {
            let vert = &self.verts[v as usize];
            SnapPoint {
                a: vert.a,
                b: vert.b,
                to_a_mm: vert.to_a_mm,
                to_b_mm: vert.to_b_mm,
                car_mmps: vert.car_mmps,
                flags: vert.flags,
            }
        })
    }

    fn mode_mmps(point: &SnapPoint, mode: Mode) -> u32 {
        match mode {
            Mode::Walk => (WALK_SPEED_MPS * 1000.0) as u32,
            Mode::Bike => (BIKE_SPEED_MPS * 1000.0) as u32,
            Mode::Car => point.car_mmps.max(1),
        }
    }

    pub fn walk_reach(&self, point: &SnapPoint, cap_seconds: u32) -> Vec<(u32, u32)> {
        if point.flags & FLAG_WALK == 0 {
            return Vec::new();
        }
        use petgraph::visit::EdgeRef;
        let mmps = (WALK_SPEED_MPS * 1000.0) as u32;
        let start = u32::MAX;
        pathfinding::directed::dijkstra::dijkstra_reach(&start, |&n| -> Vec<(u32, u32)> {
            if n == start {
                vec![
                    (point.a, point.to_a_mm / mmps),
                    (point.b, point.to_b_mm / mmps),
                ]
            } else {
                self.walk
                    .edges(NodeIndex::new(n as usize))
                    .map(|e| (e.target().index() as u32, *e.weight()))
                    .collect()
            }
        })
        .take_while(|item| item.total_cost <= cap_seconds)
        .filter(|item| item.node != start)
        .map(|item| (item.node, item.total_cost))
        .collect()
    }

    fn car_dists(&self, point: &SnapPoint) -> HashMap<u32, u32> {
        let mmps = point.car_mmps.max(1);
        let virt = u32::MAX;
        let mut best: HashMap<u32, u32> = HashMap::new();
        for (endpoint, offset_mm) in [(point.a, point.to_a_mm), (point.b, point.to_b_mm)] {
            let offset = offset_mm / mmps;
            best
                .entry(endpoint)
                .and_modify(|v| *v = (*v).min(offset))
                .or_insert(offset);
        }
        for item in pathfinding::directed::dijkstra::dijkstra_reach(&virt, |&e| -> Vec<(u32, u32)> {
            if e == virt {
                let mut out = Vec::new();
                for (endpoint, offset_mm) in [(point.a, point.to_a_mm), (point.b, point.to_b_mm)] {
                    let offset = offset_mm / mmps;
                    for &ei in &self.car_out[endpoint as usize] {
                        out.push((ei, offset + self.car_edges[ei as usize].secs));
                    }
                }
                out
            } else {
                let current = &self.car_edges[e as usize];
                self.car_out[current.to as usize]
                    .iter()
                    .map(|&ei| (ei, self.car_edges[ei as usize].secs))
                    .collect()
            }
        })
        .take_while(|item| item.total_cost <= MAX_TRIP_SECONDS + 600)
        {
            if item.node == virt {
                continue;
            }
            let node = self.car_edges[item.node as usize].to;
            best
                .entry(node)
                .and_modify(|v| *v = (*v).min(item.total_cost))
                .or_insert(item.total_cost);
        }
        best
    }

    pub fn dists(&self, point: &SnapPoint, mode: Mode) -> Option<HashMap<u32, u32>> {
        if point.flags & Self::mode_flag(mode) == 0 {
            return None;
        }
        if mode == Mode::Car {
            return Some(self.car_dists(point));
        }
        let graph = match mode {
            Mode::Walk => &self.walk,
            Mode::Bike => &self.bike,
            Mode::Car => unreachable!(),
        };
        let mmps = Self::mode_mmps(point, mode);
        let mut merged: HashMap<u32, u32> = HashMap::new();
        for (endpoint, offset_mm) in [(point.a, point.to_a_mm), (point.b, point.to_b_mm)] {
            let offset = offset_mm / mmps;
            let scores = petgraph::algo::dijkstra(
                graph,
                NodeIndex::new(endpoint as usize),
                None,
                |e| *e.weight(),
            );
            for (node, d) in scores {
                let total = d.saturating_add(offset);
                merged
                    .entry(node.index() as u32)
                    .and_modify(|v| *v = (*v).min(total))
                    .or_insert(total);
            }
        }
        Some(merged)
    }

    pub fn route_nodes(&self, from: &SnapPoint, to: &SnapPoint, mode: Mode) -> Option<Vec<(f64, f64)>> {
        use petgraph::visit::EdgeRef;
        let graph = match mode {
            Mode::Walk => &self.walk,
            Mode::Bike => &self.bike,
            Mode::Car => return None,
        };
        let mmps = Self::mode_mmps(from, mode);
        let virt = u32::MAX;
        let targets = [to.a, to.b];
        let result = pathfinding::directed::dijkstra::dijkstra(
            &virt,
            |&n| -> Vec<(u32, u32)> {
                if n == virt {
                    vec![
                        (from.a, from.to_a_mm / mmps),
                        (from.b, from.to_b_mm / mmps),
                    ]
                } else {
                    graph
                        .edges(NodeIndex::new(n as usize))
                        .map(|e| (e.target().index() as u32, *e.weight()))
                        .collect()
                }
            },
            |&n| n != virt && targets.contains(&n),
        );
        result.map(|(path, _)| {
            path.into_iter()
                .filter(|&n| n != virt)
                .map(|n| (self.node_lat[n as usize], self.node_lon[n as usize]))
                .collect()
        })
    }

    pub fn arrival(&self, dists: &HashMap<u32, u32>, point: &SnapPoint, mode: Mode) -> u32 {
        if point.flags & Self::mode_flag(mode) == 0 {
            return UNREACHABLE;
        }
        let mmps = Self::mode_mmps(point, mode);
        let via = |endpoint: u32, offset_mm: u32| -> u32 {
            dists
                .get(&endpoint)
                .map(|d| d.saturating_add(offset_mm / mmps))
                .unwrap_or(UNREACHABLE)
        };
        via(point.a, point.to_a_mm).min(via(point.b, point.to_b_mm))
    }
}

package market.femi

import com.conveyal.gtfs.GTFSFeed
import com.typesafe.config.ConfigFactory
import com.conveyal.osmlib.OSM
import com.conveyal.r5.analyst.FreeFormPointSet
import com.conveyal.r5.analyst.TravelTimeComputer
import com.conveyal.r5.analyst.cluster.RegionalTask
import com.conveyal.r5.analyst.cluster.TransportNetworkConfig
import com.conveyal.r5.analyst.scenario.Scenario
import com.conveyal.r5.api.util.LegMode
import com.conveyal.r5.api.util.TransitModes
import com.conveyal.r5.profile.StreetMode
import com.conveyal.r5.streets.StreetLayer
import com.conveyal.r5.transit.GtfsTransferLoader
import com.conveyal.r5.transit.TransferFinder
import com.conveyal.r5.transit.TransitLayer
import com.conveyal.r5.transit.TransportNetwork
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime
import java.util.EnumSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import market.femi.tables.Clients
import market.femi.tables.Postcodes
import market.femi.tables.Staffs
import market.femi.tables.TravelTimes
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val UNREACHABLE = Int.MAX_VALUE
const val MAX_TRIP_MINUTES = 90
const val DEPARTURE_WINDOW_SECONDS = 600
val FIRST_DEPARTURE: LocalTime = LocalTime.of(5, 0)
val LAST_DEPARTURE: LocalTime = LocalTime.of(23, 0)
const val STEP_MINUTES = 15L

data class Place(val id: String, val lat: Double, val lon: Double)

data class TravelRow(
    val from: String,
    val to: String,
    val modeId: String,
    val mins: Int,
    val departure: String?,
)

private val networks = mutableMapOf<String, TransportNetwork>()
private val syncLock = Mutex()

suspend fun syncTravelTimes(range: String): String = withContext(Dispatchers.IO) {
    syncLock.withLock {
        val bounds = range.trim().split(",")
        val fromDate = LocalDate.parse(bounds[0].trim())
        val toDate = LocalDate.parse(bounds[1].trim())
        val config = ConfigFactory.load()
        val osmPath = config.getString("osm")
        val gtfsPaths = config.getStringList("gtfs")

        val network = networks.getOrPut(osmPath) { buildNetwork(osmPath, gtfsPaths) }
        val rawHomes = transaction {
            Staffs.join(Postcodes, JoinType.INNER, Staffs.postcodeId, Postcodes.id)
                .selectAll()
                .map { Place(it[Postcodes.id], it[Postcodes.latitude], it[Postcodes.longitude]) }
                .distinct()
        }
        val rawSites = transaction {
            Clients.join(Postcodes, JoinType.INNER, Clients.postcodeId, Postcodes.id)
                .selectAll()
                .map { Place(it[Postcodes.id], it[Postcodes.latitude], it[Postcodes.longitude]) }
                .distinct()
        }
        val homes = snap(network, rawHomes)
        val sites = snap(network, rawSites)
        val siteIds = sites.map { it.id }.toSet()
        val homesAwayFromSites = homes.filter { it.id !in siteIds }
        val origins = homesAwayFromSites + sites
        println("Rota span: $fromDate -> $toDate")
        println("${homes.size} staff homes, ${sites.size} client sites -> ${origins.size} origins x ${sites.size} destinations")

        val destinationBytes = ByteArrayOutputStream()
        val destinationData = DataOutputStream(destinationBytes)
        destinationData.writeInt(sites.size)
        for (place in sites) destinationData.writeUTF(place.id)
        for (place in sites) destinationData.writeDouble(place.lat)
        for (place in sites) destinationData.writeDouble(place.lon)
        for (place in sites) destinationData.writeDouble(0.0)
        val destinationStream = ByteArrayInputStream(destinationBytes.toByteArray())
        val destinations = FreeFormPointSet(destinationStream)

        val walk = EnumSet.of(LegMode.WALK)
        val noTransit = EnumSet.noneOf(TransitModes::class.java)
        val noEgress = EnumSet.noneOf(LegMode::class.java)
        val eightAm = LocalTime.of(8, 0)

        suspend fun computeAndInsert(
            day: LocalDate,
            time: LocalTime,
            direct: EnumSet<LegMode>,
            access: EnumSet<LegMode>,
            egress: EnumSet<LegMode>,
            transit: EnumSet<TransitModes>,
            modeId: String,
            departure: String?,
        ): Int {
            val template = RegionalTask()
            val scenario = Scenario()
            scenario.id = "id"
            template.scenario = scenario
            template.destinationPointSets = arrayOf(destinations)
            template.directModes = direct
            template.accessModes = access
            template.egressModes = egress
            template.transitModes = transit
            template.date = day
            template.fromTime = time.toSecondOfDay()
            template.toTime = template.fromTime + DEPARTURE_WINDOW_SECONDS
            template.percentiles = intArrayOf(50)
            template.streetTime = MAX_TRIP_MINUTES
            template.maxTripDurationMinutes = MAX_TRIP_MINUTES
            template.maxBikeTime = MAX_TRIP_MINUTES
            template.maxCarTime = MAX_TRIP_MINUTES
            template.maxWalkTime = MAX_TRIP_MINUTES
            template.walkSpeed = 1.0f
            template.bikeSpeed = 12.0f / 3.6f
            template.maxRides = 8
            template.bikeTrafficStress = 3
            template.recordTimes = true
            template.makeTauiSite = false
            template.oneToOne = false
            template.monteCarloDraws = 60
            template.recordAccessibility = false

            val rows = coroutineScope {
                origins.map { origin ->
                    async(Dispatchers.Default) {
                        val request = template.clone()
                        request.fromLat = origin.lat
                        request.fromLon = origin.lon
                        val minutes =
                            TravelTimeComputer(request, network).computeTravelTimes().travelTimes.values[0]
                        sites.withIndex()
                            .filter { (_, site) -> origin.id != site.id }
                            .filter { (i, _) -> minutes[i] != UNREACHABLE }
                            .map { (i, site) -> TravelRow(origin.id, site.id, modeId, minutes[i], departure) }
                    }
                }.awaitAll().flatten()
            }

            transaction {
                TravelTimes.batchInsert(rows) { row ->
                    this[TravelTimes.fromPostcodeId] = row.from
                    this[TravelTimes.toPostcodeId] = row.to
                    this[TravelTimes.transportModeId] = row.modeId
                    this[TravelTimes.travelMins] = row.mins
                    this[TravelTimes.departureTime] = row.departure
                }
            }
            return rows.size
        }

        for ((modeId, streetMode) in listOf(
            "car" to LegMode.CAR,
            "bicycle" to LegMode.BICYCLE,
            "walk" to LegMode.WALK,
        )) {
            val have = transaction {
                TravelTimes.selectAll()
                    .where { TravelTimes.transportModeId eq modeId }.count()
            }
            if (have > 0) {
                println("$modeId already present ($have rows), skipping")
                continue
            }
            println("Computing $modeId (time-independent)...")
            val direct = EnumSet.of(streetMode, LegMode.WALK)
            val count = computeAndInsert(
                fromDate,
                eightAm,
                direct,
                direct,
                noEgress,
                noTransit,
                modeId,
                null
            )
            println("   $count rows")
        }

        val allTransit = EnumSet.allOf(TransitModes::class.java)
        val totalSlots = 73
        var day = fromDate
        while (day <= toDate) {
            val present = transaction {
                TravelTimes.selectAll()
                    .where { (TravelTimes.transportModeId eq "transit") and
                        (TravelTimes.departureTime like "$day%") }
                    .mapNotNull { it[TravelTimes.departureTime] }
                    .toSet()
            }
            if (present.size.toLong() == totalSlots.toLong()) {
                println("$day transit complete ($totalSlots departures), skipping")
                day = day.plusDays(1)
                continue
            }
            var slot = FIRST_DEPARTURE
            var slotNumber = 0
            while (slot <= LAST_DEPARTURE) {
                slotNumber += 1
                val departure = "${day}T$slot:00+00:00"
                if (departure in present) {
                    slot = slot.plusMinutes(STEP_MINUTES)
                    continue
                }
                val count = computeAndInsert(
                    day,
                    slot,
                    walk,
                    walk,
                    walk,
                    allTransit,
                    "transit",
                    departure
                )
                if (slotNumber % 10 == 0 || slot == LAST_DEPARTURE) {
                    println("   $day: $slotNumber/$totalSlots transit slots done ($count rows this slot)")
                }
                slot = slot.plusMinutes(STEP_MINUTES)
            }
            day = day.plusDays(1)
        }

        val total = transaction { TravelTimes.selectAll().count() }
        "travel times synced for $fromDate -> $toDate; table holds $total rows"
    }
}

fun snap(network: TransportNetwork, places: List<Place>): List<Place> {
    val snapped = mutableListOf<Place>()
    for (place in places) {
        val split = network.streetLayer.findSplit(
            place.lat,
            place.lon,
            StreetLayer.LINK_RADIUS_METERS,
            StreetMode.WALK,
        )
        if (split == null) {
            error("${place.id} could not be snapped to the street network")
        }
        snapped.add(Place(place.id, split.fixedLat / 1e7, split.fixedLon / 1e7))
    }
    return snapped
}

fun buildNetwork(osmPath: String, gtfsPaths: List<String>): TransportNetwork {
    println("Loading map: $osmPath")
    val network = TransportNetwork()
    network.scenarioId = "femi-matrix"

    val mapdb = Files.createTempFile("femi-matrix-osm", ".mapdb")
    val osm = OSM(mapdb.toString())
    osm.intersectionDetection = true
    osm.readFromFile(osmPath)

    network.streetLayer = StreetLayer()
    network.streetLayer.parentNetwork = network
    network.streetLayer.loadFromOsm(osm)
    network.streetLayer.indexStreets()

    network.transitLayer = TransitLayer()
    network.transitLayer.saveShapes = true
    network.transitLayer.parentNetwork = network
    val transferLoader =
        GtfsTransferLoader(network.transitLayer, TransportNetworkConfig.TransferConfig.OSM_ONLY)
    for (gtfsPath in gtfsPaths) {
        println("Loading GTFS feed: $gtfsPath")
        val feed = GTFSFeed.writableTempFileFromGtfs(gtfsPath)
        if (feed.errors.size > 0) {
            System.err.println("GTFS issues in $gtfsPath: ${feed.errors.size} (continuing)")
        }
        network.transitLayer.loadFromGtfs(feed, transferLoader)
        feed.close()
    }

    network.streetLayer.associateStops(network.transitLayer)
    network.streetLayer.buildEdgeLists()
    network.transitLayer.rebuildTransientIndexes()
    val transferFinder = TransferFinder(network, transferLoader)
    transferFinder.findTransfers()
    transferFinder.findParkRideTransfer()
    network.transitLayer.buildDistanceTables(null)
    println("--> Transport network built")
    return network
}

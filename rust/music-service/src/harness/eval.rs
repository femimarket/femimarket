//! The eval seam — "the harness is the measured artifact."
//!
//! Record model decisions into a cassette; replay them with NO network for
//! deterministic tests that still exercise the real loop + tool code. Plus a
//! metrics fold (with loop detection). The test module below is the green gate.

use crate::harness::agent::*;
use crate::harness::spine::*;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicUsize, Ordering};

// Turn needs to round-trip for cassette persistence.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct RecordedTurn {
    pub blocks: Vec<ContentBlock>,
    pub provider_raw: Option<String>,
}
impl From<Turn> for RecordedTurn {
    fn from(t: Turn) -> Self {
        RecordedTurn { blocks: t.blocks, provider_raw: t.provider_raw }
    }
}
impl From<RecordedTurn> for Turn {
    fn from(t: RecordedTurn) -> Self {
        Turn { blocks: t.blocks, provider_raw: t.provider_raw }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct CassetteEntry {
    pub request_fingerprint: String,
    pub response: RecordedTurn,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Cassette {
    pub entries: Vec<CassetteEntry>,
}

/// Stable cross-process fingerprint of a request (FNV-1a; NOT a seeded hasher).
/// Tool ids are omitted (they are nondeterministic in real runs).
pub fn fingerprint(messages: &[Msg]) -> String {
    let mut s = String::new();
    for m in messages {
        s.push_str(&m.role);
        s.push('|');
        for b in &m.blocks {
            match b {
                ContentBlock::Text { text } => {
                    s.push_str("t:");
                    s.push_str(text);
                }
                ContentBlock::ToolUse { name, args, .. } => {
                    s.push_str("u:");
                    s.push_str(name);
                    s.push_str(args);
                }
                ContentBlock::ToolResult { summary, is_error, .. } => {
                    s.push_str("r:");
                    s.push_str(summary);
                    s.push(if *is_error { '1' } else { '0' });
                }
                ContentBlock::ItemRef { kind, mime, .. } => {
                    s.push_str("i:");
                    s.push_str(kind);
                    s.push_str(mime);
                }
            }
            s.push(';');
        }
        s.push('\n');
    }
    let mut h: u64 = 0xcbf29ce484222325;
    for byte in s.as_bytes() {
        h ^= *byte as u64;
        h = h.wrapping_mul(0x100000001b3);
    }
    format!("{h:x}")
}

/// A brain that replays scripted turns deterministically (no network).
pub struct ReplayBrain {
    turns: Vec<Turn>,
    idx: AtomicUsize,
}
impl ReplayBrain {
    pub fn new(turns: Vec<Turn>) -> Self {
        ReplayBrain { turns, idx: AtomicUsize::new(0) }
    }
    pub fn from_cassette(c: &Cassette) -> Self {
        ReplayBrain::new(c.entries.iter().map(|e| e.response.clone().into()).collect())
    }
}
impl Brain for ReplayBrain {
    fn turn(
        &self,
        _messages: &[Msg],
    ) -> impl std::future::Future<Output = Result<Turn, HarnessError>> + Send {
        let i = self.idx.fetch_add(1, Ordering::SeqCst);
        let out = self
            .turns
            .get(i)
            .cloned()
            .ok_or_else(|| HarnessError::new(FaultClass::ModelProtocol, "cassette exhausted"));
        async move { out }
    }
}

/// Telemetry: a pure fold over the log, incl. a loop detector.
#[derive(Clone, Debug, PartialEq)]
pub struct RunMetrics {
    pub model_calls: u32,
    pub tool_calls: u32,
    pub items: u32,
    pub turns: u32,
    pub terminal: String, // completed | failed | budget | cancelled | running
    pub stuck: bool,
}

pub fn metrics(log: &EventLog, loop_threshold: u32) -> RunMetrics {
    let mut model_calls = 0;
    let mut tool_calls = 0;
    let mut items = 0;
    let mut turns = 0;
    let mut terminal = "running".to_string();
    let mut tool_keys: Vec<String> = Vec::new();

    for e in log.events() {
        match &e.kind {
            EventKind::TurnStarted { .. } => turns += 1,
            EventKind::ModelRequest { .. } | EventKind::ModelRetry { .. } => model_calls += 1,
            EventKind::AssistantMessage { blocks, .. } => {
                for b in blocks {
                    if let ContentBlock::ToolUse { name, args, .. } = b {
                        tool_calls += 1;
                        tool_keys.push(format!("{name}|{args}"));
                    }
                }
            }
            EventKind::ItemProduced { .. } => items += 1,
            EventKind::RunCompleted { .. } => terminal = "completed".into(),
            EventKind::RunFailed { .. } => terminal = "failed".into(),
            EventKind::BudgetExhausted { .. } => terminal = "budget".into(),
            EventKind::RunCancelled => terminal = "cancelled".into(),
            _ => {}
        }
    }

    let mut max_run = 0;
    let mut cur = 0;
    let mut last: Option<&str> = None;
    for k in &tool_keys {
        if last == Some(k.as_str()) {
            cur += 1;
        } else {
            cur = 1;
            last = Some(k.as_str());
        }
        max_run = max_run.max(cur);
    }

    RunMetrics {
        model_calls,
        tool_calls,
        items,
        turns,
        terminal,
        stuck: max_run >= loop_threshold,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // --- fakes ---------------------------------------------------------------

    fn tool_turn(id: &str, name: &str, args: &str) -> Turn {
        Turn { blocks: vec![ContentBlock::ToolUse { id: id.into(), name: name.into(), args: args.into() }], provider_raw: None }
    }
    fn text_turn(t: &str) -> Turn {
        Turn { blocks: vec![ContentBlock::Text { text: t.into() }], provider_raw: None }
    }

    struct FakeTools;
    impl Tools for FakeTools {
        fn run(
            &self,
            _id: &str,
            name: &str,
            _args: &str,
        ) -> impl std::future::Future<Output = Result<ToolOutcome, HarnessError>> + Send {
            let name = name.to_string();
            async move {
                Ok(ToolOutcome {
                    summary: format!("fake {name} ok — img_fake (image/png)"),
                    is_error: false,
                    item: Some(Item { item_id: "img_fake".into(), kind: "image".into(), mime: "image/png".into() }),
                })
            }
        }
    }

    // A brain that fails `fail_times` with `fault`, then replays `then`.
    struct FlakyBrain {
        fault: FaultClass,
        fail_times: usize,
        then: Vec<Turn>,
        n: AtomicUsize,
    }
    impl Brain for FlakyBrain {
        fn turn(
            &self,
            _messages: &[Msg],
        ) -> impl std::future::Future<Output = Result<Turn, HarnessError>> + Send {
            let i = self.n.fetch_add(1, Ordering::SeqCst);
            let out = if i < self.fail_times {
                Err(HarnessError::new(self.fault, "boom"))
            } else {
                self.then
                    .get(i - self.fail_times)
                    .cloned()
                    .ok_or_else(|| HarnessError::new(FaultClass::ModelProtocol, "exhausted"))
            };
            async move { out }
        }
    }

    fn simple_cfg(run_id: &str) -> RunConfig<'static, LedgerSummarizer> {
        RunConfig::simple(run_id.to_string())
    }

    // --- tests ---------------------------------------------------------------

    #[tokio::test]
    async fn loop_generates_then_completes() {
        let brain = ReplayBrain::new(vec![
            tool_turn("c1", "generate_image", r#"{"prompt":"a red apple"}"#),
            text_turn("Here's your red apple."),
        ]);
        let log = run(&brain, &FakeTools, "sys", "make a red apple", Budget::turns(8), simple_cfg("t1")).await;

        let m = metrics(&log, 3);
        assert_eq!(m.model_calls, 2);
        assert_eq!(m.tool_calls, 1);
        assert_eq!(m.items, 1);
        assert_eq!(m.terminal, "completed");
        assert!(!m.stuck);
        // reference-not-bytes: no ContentBlock in the log is ever raw bytes.
        assert!(matches!(log.events().last().unwrap().kind, EventKind::RunCompleted { .. }));
    }

    #[tokio::test]
    async fn budget_cap_stops_the_loop() {
        // Always asks for a tool (distinct ids per turn) → must hit the turn cap
        // and finalize gracefully.
        let brain = ReplayBrain::new((0..20).map(|i| tool_turn(&format!("c{i}"), "generate_image", "{}")).collect());
        let log = run(&brain, &FakeTools, "sys", "go", Budget::turns(3), simple_cfg("t2")).await;
        let m = metrics(&log, 3);
        assert_eq!(m.terminal, "budget");
        // transcript stays valid: the cut turn's tool_use was answered as skipped.
        assert!(validate_transcript(&fold_transcript(&log)).is_ok());
    }

    #[test]
    fn invariant_catches_orphan_and_unanswered() {
        let orphan = vec![Msg::new("tool", vec![ContentBlock::ToolResult { id: "x".into(), summary: "".into(), is_error: false }])];
        assert_eq!(validate_transcript(&orphan), Err(TranscriptError::OrphanToolResult("x".into())));

        let unanswered = vec![Msg::new("assistant", vec![ContentBlock::ToolUse { id: "y".into(), name: "t".into(), args: "{}".into() }])];
        assert_eq!(validate_transcript(&unanswered), Err(TranscriptError::UnansweredToolUse("y".into())));
    }

    #[tokio::test]
    async fn retryable_fault_retries_then_succeeds() {
        let brain = FlakyBrain {
            fault: FaultClass::ModelUnavailable,
            fail_times: 2,
            then: vec![text_turn("ok")],
            n: AtomicUsize::new(0),
        };
        let mut cfg = simple_cfg("t3");
        cfg.retry = RetryPolicy::no_delay(3);
        let log = run(&brain, &FakeTools, "sys", "hi", Budget::turns(8), cfg).await;
        let m = metrics(&log, 3);
        assert_eq!(m.terminal, "completed");
        // two retries were recorded loudly in the log.
        let retries = log.events().iter().filter(|e| matches!(e.kind, EventKind::ModelRetry { .. })).count();
        assert_eq!(retries, 2);
    }

    #[tokio::test]
    async fn terminal_fault_finalizes_immediately() {
        let brain = FlakyBrain { fault: FaultClass::ModelProtocol, fail_times: 1, then: vec![], n: AtomicUsize::new(0) };
        let mut cfg = simple_cfg("t4");
        cfg.retry = RetryPolicy::no_delay(3);
        let log = run(&brain, &FakeTools, "sys", "hi", Budget::turns(8), cfg).await;
        let m = metrics(&log, 3);
        assert_eq!(m.terminal, "failed");
        assert_eq!(log.events().iter().filter(|e| matches!(e.kind, EventKind::ModelRetry { .. })).count(), 0);
    }

    #[tokio::test]
    async fn retryable_exhaustion_finalizes_after_cap() {
        let brain = FlakyBrain { fault: FaultClass::ModelUnavailable, fail_times: 99, then: vec![], n: AtomicUsize::new(0) };
        let mut cfg = simple_cfg("t5");
        cfg.retry = RetryPolicy::no_delay(3);
        let log = run(&brain, &FakeTools, "sys", "hi", Budget::turns(8), cfg).await;
        match &log.events().last().unwrap().kind {
            EventKind::RunFailed { fault, .. } => assert_eq!(*fault, FaultClass::ModelUnavailable),
            other => panic!("expected RunFailed, got {other:?}"),
        }
        assert_eq!(log.events().iter().filter(|e| matches!(e.kind, EventKind::ModelRetry { .. })).count(), 3);
    }

    #[tokio::test]
    async fn cancellation_finalizes_as_cancelled() {
        let brain = ReplayBrain::new(vec![tool_turn("c1", "generate_image", "{}"); 20]);
        let flag = std::sync::atomic::AtomicBool::new(true);
        let cancelled = move || flag.load(Ordering::SeqCst);
        let ledger = LedgerSummarizer;
        let cfg = RunConfig {
            run_id: "t6".into(),
            retry: RetryPolicy::no_delay(3),
            compaction: None,
            summarizer: &ledger,
            cancelled: &cancelled as &(dyn Fn() -> bool + Sync),
        };
        let log = run(&brain, &FakeTools, "sys", "go", Budget::turns(8), cfg).await;
        assert!(matches!(log.events().last().unwrap().kind, EventKind::RunCancelled));
        assert_eq!(metrics(&log, 3).terminal, "cancelled");
    }

    #[tokio::test]
    async fn long_chat_compacts_but_log_is_whole() {
        // 8 tool turns then done, under a tiny token budget → compaction must fire.
        let mut turns: Vec<Turn> = (0..8).map(|i| tool_turn(&format!("c{i}"), "generate_image", &format!("{{\"prompt\":\"item {i}\"}}"))).collect();
        turns.push(text_turn("All done."));
        let brain = ReplayBrain::new(turns);
        let ledger = LedgerSummarizer;
        let never = || false;
        let cfg = RunConfig {
            run_id: "t7".into(),
            retry: RetryPolicy::no_delay(0),
            compaction: Some(CompactionPolicy { max_transcript_tokens: 40, keep_turns: 2 }),
            summarizer: &ledger,
            cancelled: &never as &(dyn Fn() -> bool + Sync),
        };
        let log = run(&brain, &FakeTools, "SYSTEM-KEEP-ME", "make a series", Budget::turns(20), cfg).await;

        let compactions = log.events().iter().filter(|e| matches!(e.kind, EventKind::Compaction { .. })).count();
        assert!(compactions >= 1, "compaction should fire");
        let msgs = fold_transcript(&log);
        assert!(log.events().len() > msgs.len(), "log kept everything; folded view shrank");
        assert_eq!(msgs[0].role, "system");
        assert!(validate_transcript(&msgs).is_ok());
        assert!(matches!(log.events().last().unwrap().kind, EventKind::RunCompleted { .. }));
    }

    #[test]
    fn cassette_round_trips_through_json() {
        let c = Cassette {
            entries: vec![CassetteEntry {
                request_fingerprint: fingerprint(&[Msg::text("user", "hi")]),
                response: RecordedTurn { blocks: vec![ContentBlock::Text { text: "yo".into() }], provider_raw: Some("{\"x\":1}".into()) },
            }],
        };
        let json = serde_json::to_string(&c).unwrap();
        let back: Cassette = serde_json::from_str(&json).unwrap();
        assert_eq!(c, back);
    }

    // Live: exercises the REAL LmStudioBrain (tools advertised, tool_calls parsed,
    // provider_raw echoed across turns) driving the loop with fake tools. Needs LM
    // Studio serving qwen/qwen3.6-35b-a3b. Run: `cargo test --lib real_lmstudio -- --ignored`.
    #[tokio::test]
    #[ignore = "needs LM Studio at localhost:1234"]
    async fn real_lmstudio_brain_drives_the_loop() {
        use crate::harness::lmstudio::LmStudioBrain;
        use crate::harness::tools::femi_tool_schema;
        let brain = LmStudioBrain::new(femi_tool_schema());
        let log = run(
            &brain,
            &FakeTools,
            "You control an image studio. Call generate_image when the user asks for a picture.",
            "make a red apple",
            Budget::turns(6),
            simple_cfg("live"),
        )
        .await;
        let m = metrics(&log, 3);
        eprintln!("LIVE terminal={} model_calls={} tool_calls={} items={}", m.terminal, m.model_calls, m.tool_calls, m.items);
        assert_eq!(m.terminal, "completed");
        assert!(m.tool_calls >= 1, "model should have called generate_image");
    }

    // The decisive looping proof: the REAL brain, a task that REQUIRES sequential
    // tool rounds. Prints the actual round-by-round trace.
    #[tokio::test]
    #[ignore = "needs LM Studio at localhost:1234"]
    async fn real_lmstudio_multistep_loops() {
        use crate::harness::lmstudio::LmStudioBrain;
        use crate::harness::tools::femi_tool_schema;
        let brain = LmStudioBrain::new(femi_tool_schema());
        let log = run(
            &brain,
            &FakeTools,
            "You are a generation agent. Use your tools.",
            "Generate three separate images, strictly one at a time: first a red apple, \
             then a green banana, then a blue cherry. Call generate_image once per image \
             and wait for each result before starting the next. After all three, say done.",
            Budget::turns(8),
            simple_cfg("live-multi"),
        )
        .await;

        // Print the legible trace: every model round and tool call in order.
        for e in log.events() {
            match &e.kind {
                EventKind::ModelRequest { turn, msgs } => eprintln!("round {turn}: model called ({msgs} msgs in context)"),
                EventKind::AssistantMessage { blocks, .. } => {
                    for b in blocks {
                        match b {
                            ContentBlock::ToolUse { name, args, .. } => eprintln!("   -> tool: {name} {args}"),
                            ContentBlock::Text { text } => eprintln!("   -> text: {}", text.trim()),
                            _ => {}
                        }
                    }
                }
                EventKind::RunCompleted { turns } => eprintln!("completed after {} tool rounds", turns),
                other if other.is_terminal() => eprintln!("terminal: {other:?}"),
                _ => {}
            }
        }
        let m = metrics(&log, 10);
        eprintln!("SUMMARY: model_calls={} tool_calls={} terminal={}", m.model_calls, m.tool_calls, m.terminal);
        assert_eq!(m.terminal, "completed");
        assert!(m.tool_calls >= 3, "expected >=3 sequential tool calls, got {}", m.tool_calls);
        assert!(m.model_calls >= 2, "a real loop must call the model again after tool results");
    }

    #[tokio::test]
    async fn record_replay_is_deterministic() {
        let cassette = Cassette {
            entries: vec![
                CassetteEntry { request_fingerprint: "a".into(), response: RecordedTurn { blocks: vec![ContentBlock::ToolUse { id: "c1".into(), name: "generate_image".into(), args: "{}".into() }], provider_raw: None } },
                CassetteEntry { request_fingerprint: "b".into(), response: RecordedTurn { blocks: vec![ContentBlock::Text { text: "done".into() }], provider_raw: None } },
            ],
        };
        let a = run(&ReplayBrain::from_cassette(&cassette), &FakeTools, "sys", "go", Budget::turns(8), simple_cfg("A")).await;
        let b = run(&ReplayBrain::from_cassette(&cassette), &FakeTools, "sys", "go", Budget::turns(8), simple_cfg("B")).await;
        let kinds = |l: &EventLog| l.events().iter().map(|e| std::mem::discriminant(&e.kind)).collect::<Vec<_>>();
        assert_eq!(kinds(&a), kinds(&b));
    }
}

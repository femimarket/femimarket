//! The agent: ports (Brain, Tools, MediaStore), budgets, bounded retry, and the loop.
//!
//! The loop is the dumb, single-threaded core: fold the log into a transcript,
//! validate the invariant, call the model, execute any tool calls, append results,
//! repeat until the model stops calling tools. Everything robust (budgets, fault
//! taxonomy, retry, cancellation, compaction) wraps that core.

use crate::harness::spine::*;
use std::time::Instant;

/// A classified error surfaced by a port. The `fault` drives retry vs terminate.
#[derive(Clone, Debug, PartialEq)]
pub struct HarnessError {
    pub fault: FaultClass,
    pub message: String,
}
impl HarnessError {
    pub fn new(fault: FaultClass, message: impl Into<String>) -> Self {
        HarnessError { fault, message: message.into() }
    }
}

/// The model's response for one turn: text and/or tool_use blocks + opaque echo.
#[derive(Clone, Debug, PartialEq)]
pub struct Turn {
    pub blocks: Vec<ContentBlock>,
    pub provider_raw: Option<String>,
}

/// A produced pool item — a reference, never bytes.
#[derive(Clone, Debug, PartialEq)]
pub struct Item {
    pub item_id: String,
    pub kind: String,
    pub mime: String,
}

/// The outcome of running one tool: a short text summary (no bytes) + optional item.
#[derive(Clone, Debug, PartialEq)]
pub struct ToolOutcome {
    pub summary: String,
    pub is_error: bool,
    pub item: Option<Item>,
}

/// The brain port (swappable model). Generic dispatch keeps the loop dyn-free.
pub trait Brain {
    fn turn(
        &self,
        messages: &[Msg],
    ) -> impl std::future::Future<Output = Result<Turn, HarnessError>> + Send;
}

/// The tools port. A tool run persists any bytes and returns only a reference.
pub trait Tools {
    fn run(
        &self,
        id: &str,
        name: &str,
        args: &str,
    ) -> impl std::future::Future<Output = Result<ToolOutcome, HarnessError>> + Send;
}

/// Loud + graceful budgets.
#[derive(Clone, Copy, Debug)]
pub struct Budget {
    pub max_turns: u32,
    pub wall_clock_ms: Option<u128>,
}
impl Budget {
    pub fn turns(max_turns: u32) -> Self {
        Budget { max_turns, wall_clock_ms: None }
    }
}

/// Bounded retry with an injectable (deterministic-in-test) backoff.
#[derive(Clone)]
pub struct RetryPolicy {
    pub max_retries: u32,
    /// Sleep before retry `attempt` (0-based). Production sleeps; tests pass a no-op.
    pub backoff_ms: fn(u32) -> u64,
}
impl RetryPolicy {
    pub fn production() -> Self {
        RetryPolicy { max_retries: 3, backoff_ms: |a| (250u64 << a).min(8_000) }
    }
    pub fn none() -> Self {
        RetryPolicy { max_retries: 0, backoff_ms: |_| 0 }
    }
    pub fn no_delay(max_retries: u32) -> Self {
        RetryPolicy { max_retries, backoff_ms: |_| 0 }
    }
}

/// Deterministic-first compaction: roll old turns into a summary once the folded
/// transcript exceeds a token budget. The log stays whole; only the view shrinks.
#[derive(Clone, Copy, Debug)]
pub struct CompactionPolicy {
    pub max_transcript_tokens: usize,
    pub keep_turns: u32,
}

/// A summarizer turns a range of old events into replacement text. Default is
/// deterministic (no model call) so eval-replay stays deterministic.
pub trait Summarizer {
    fn summarize(&self, events: &[Event]) -> String;
}

pub struct LedgerSummarizer;
impl Summarizer for LedgerSummarizer {
    fn summarize(&self, events: &[Event]) -> String {
        ledger(events)
    }
}

/// Deterministic structured ledger: what was asked/said, tools run, items produced.
pub fn ledger(events: &[Event]) -> String {
    use std::collections::BTreeMap;
    let mut user_texts = Vec::new();
    let mut assistant_texts = Vec::new();
    let mut tool_counts: BTreeMap<String, u32> = BTreeMap::new();
    let mut items = Vec::new();
    for e in events {
        match &e.kind {
            EventKind::RunStarted { user, .. } => user_texts.push(user.clone()),
            EventKind::AssistantMessage { blocks, .. } => {
                for b in blocks {
                    match b {
                        ContentBlock::Text { text } if !text.is_empty() => {
                            assistant_texts.push(text.clone())
                        }
                        ContentBlock::ToolUse { name, .. } => {
                            *tool_counts.entry(name.clone()).or_insert(0) += 1
                        }
                        _ => {}
                    }
                }
            }
            EventKind::ItemProduced { item_id, kind, .. } => {
                items.push(format!("{kind}:{item_id}"))
            }
            _ => {}
        }
    }
    let mut lines = Vec::new();
    if !user_texts.is_empty() {
        lines.push(format!("User: {}", user_texts.join(" | ")));
    }
    if !assistant_texts.is_empty() {
        lines.push(format!("Assistant: {}", assistant_texts.join(" | ")));
    }
    if !tool_counts.is_empty() {
        let parts: Vec<String> = tool_counts.iter().map(|(k, v)| format!("{k}×{v}")).collect();
        lines.push(format!("Tools used: {}", parts.join(", ")));
    }
    if !items.is_empty() {
        lines.push(format!("Items produced ({}): {}", items.len(), items.join(", ")));
    }
    lines.join("\n")
}

/// Coarse token estimate of a folded transcript (~4 chars/token). No tokenizer dep.
pub fn estimate_tokens(msgs: &[Msg]) -> usize {
    let mut chars = 0usize;
    for m in msgs {
        chars += m.role.len();
        for b in &m.blocks {
            match b {
                ContentBlock::Text { text } => chars += text.len(),
                ContentBlock::ToolUse { name, args, .. } => chars += name.len() + args.len(),
                ContentBlock::ToolResult { summary, .. } => chars += summary.len(),
                ContentBlock::ItemRef { item_id, kind, mime } => {
                    chars += item_id.len() + kind.len() + mime.len()
                }
            }
        }
        chars += m.provider_raw.as_ref().map_or(0, |s| s.len());
    }
    chars / 4
}

/// The clean cut: seq of the first event of the first turn we KEEP. Everything
/// before it is whole completed turns (tool pairs never split). None if nothing old.
fn compaction_boundary_seq(log: &EventLog, keep_turns: u32) -> Option<usize> {
    let max_turn = log.events().iter().filter_map(|e| e.turn).max()?;
    if max_turn + 1 <= keep_turns {
        return None;
    }
    let first_kept = max_turn - keep_turns + 1;
    log.events()
        .iter()
        .find(|e| e.turn.map_or(false, |t| t >= first_kept))
        .map(|e| e.seq)
}

fn maybe_compact<S: Summarizer>(log: &mut EventLog, policy: CompactionPolicy, summ: &S, turn: u32) {
    if estimate_tokens(&fold_transcript(log)) <= policy.max_transcript_tokens {
        return;
    }
    let Some(boundary) = compaction_boundary_seq(log, policy.keep_turns) else {
        return;
    };
    let mut prior = -1i64;
    for e in log.events() {
        if let EventKind::Compaction { .. } = e.kind {
            prior = e.seq as i64;
        }
    }
    let range: Vec<Event> = log
        .events()
        .iter()
        .filter(|e| (e.seq as i64) > prior && e.seq < boundary)
        .cloned()
        .collect();
    if range.is_empty() {
        return;
    }
    let summary = summ.summarize(&range);
    log.append(EventKind::Compaction { replaces_up_to_seq: boundary, summary }, Some(turn));
}

/// Call the brain, retrying ONLY retryable faults up to the cap. Each retry is
/// recorded to the log (loud), so the true model-call count is recoverable.
/// Honors cooperative cancellation before spending a call.
async fn call_brain_with_retry<B: Brain>(
    brain: &B,
    msgs: &[Msg],
    policy: &RetryPolicy,
    log: &mut EventLog,
    turn: u32,
    cancelled: &(dyn Fn() -> bool + Sync),
) -> Result<Turn, HarnessError> {
    let mut attempt = 0u32;
    loop {
        if cancelled() {
            return Err(HarnessError::new(FaultClass::Cancelled, "cancelled"));
        }
        match brain.turn(msgs).await {
            Ok(t) => return Ok(t),
            Err(e) => {
                if !e.fault.is_retryable() || attempt >= policy.max_retries {
                    return Err(e);
                }
                log.append(
                    EventKind::ModelRetry { turn, fault: e.fault, attempt },
                    Some(turn),
                );
                let ms = (policy.backoff_ms)(attempt);
                if ms > 0 {
                    tokio::time::sleep(std::time::Duration::from_millis(ms)).await;
                }
                attempt += 1;
            }
        }
    }
}

/// Optional knobs for a run. Defaults reproduce the plain loop.
pub struct RunConfig<'a, S: Summarizer> {
    pub run_id: String,
    pub retry: RetryPolicy,
    pub compaction: Option<CompactionPolicy>,
    pub summarizer: &'a S,
    /// Cooperative cancellation probe, checked at each loop boundary.
    pub cancelled: &'a (dyn Fn() -> bool + Sync),
}

fn never_cancel() -> bool {
    false
}
static NEVER: fn() -> bool = never_cancel;
static LEDGER: LedgerSummarizer = LedgerSummarizer;

impl<'a> RunConfig<'a, LedgerSummarizer> {
    pub fn simple(run_id: impl Into<String>) -> RunConfig<'a, LedgerSummarizer> {
        RunConfig {
            run_id: run_id.into(),
            retry: RetryPolicy::production(),
            compaction: None,
            summarizer: &LEDGER,
            cancelled: &NEVER,
        }
    }
}

/// The loop. Returns the authoritative log; the transcript is folded each turn.
pub async fn run<B, T, S>(
    brain: &B,
    tools: &T,
    system: &str,
    user: &str,
    budget: Budget,
    cfg: RunConfig<'_, S>,
) -> EventLog
where
    B: Brain,
    T: Tools,
    S: Summarizer,
{
    run_into(EventLog::new(cfg.run_id.clone()), brain, tools, system, user, budget, cfg).await
}

/// Like `run`, but into a caller-provided log (which may carry an `on_append` sink).
pub async fn run_into<B, T, S>(
    mut log: EventLog,
    brain: &B,
    tools: &T,
    system: &str,
    user: &str,
    budget: Budget,
    cfg: RunConfig<'_, S>,
) -> EventLog
where
    B: Brain,
    T: Tools,
    S: Summarizer,
{
    log.append(EventKind::RunStarted { system: system.to_string(), user: user.to_string() }, None);
    let start = Instant::now();
    let mut turn = 0u32;

    loop {
        if (cfg.cancelled)() {
            log.append(EventKind::RunCancelled, Some(turn));
            return log;
        }
        log.append(EventKind::TurnStarted { turn }, Some(turn));

        if let Some(policy) = cfg.compaction {
            maybe_compact(&mut log, policy, cfg.summarizer, turn);
        }

        let msgs = fold_transcript(&log);
        if let Err(e) = validate_transcript(&msgs) {
            log.append(
                EventKind::RunFailed {
                    fault: FaultClass::TranscriptInvalid,
                    reason: format!("invalid transcript: {e:?}"),
                },
                Some(turn),
            );
            return log;
        }

        log.append(EventKind::ModelRequest { turn, msgs: msgs.len() }, Some(turn));

        let outcome =
            match call_brain_with_retry(brain, &msgs, &cfg.retry, &mut log, turn, cfg.cancelled)
                .await
            {
                Ok(t) => t,
                Err(e) if e.fault == FaultClass::Cancelled => {
                    log.append(EventKind::RunCancelled, Some(turn));
                    return log;
                }
                Err(e) => {
                    log.append(
                        EventKind::RunFailed { fault: e.fault, reason: e.message },
                        Some(turn),
                    );
                    return log;
                }
            };

        log.append(
            EventKind::AssistantMessage {
                turn,
                blocks: outcome.blocks.clone(),
                provider_raw: outcome.provider_raw.clone(),
            },
            Some(turn),
        );

        let tool_uses: Vec<(String, String, String)> = outcome
            .blocks
            .iter()
            .filter_map(|b| match b {
                ContentBlock::ToolUse { id, name, args } => {
                    Some((id.clone(), name.clone(), args.clone()))
                }
                _ => None,
            })
            .collect();

        if tool_uses.is_empty() {
            log.append(EventKind::RunCompleted { turns: turn }, Some(turn)); // natural stop
            return log;
        }

        // About to spend another tool round — enforce budgets loudly + gracefully.
        let over_wall = budget
            .wall_clock_ms
            .map_or(false, |cap| start.elapsed().as_millis() >= cap);
        if turn >= budget.max_turns || over_wall {
            let axis = if turn >= budget.max_turns { "tool_turns" } else { "wall_clock" };
            for (id, _, _) in &tool_uses {
                log.append(
                    EventKind::ToolResult {
                        turn,
                        id: id.clone(),
                        summary: format!("skipped: budget exhausted ({axis})"),
                        is_error: true,
                    },
                    Some(turn),
                );
            }
            log.append(EventKind::BudgetExhausted { axis: axis.to_string() }, Some(turn));
            return log;
        }

        for (id, name, args) in &tool_uses {
            match tools.run(id, name, args).await {
                Ok(result) => {
                    if let Some(item) = &result.item {
                        log.append(
                            EventKind::ItemProduced {
                                item_id: item.item_id.clone(),
                                kind: item.kind.clone(),
                                mime: item.mime.clone(),
                            },
                            Some(turn),
                        );
                    }
                    log.append(
                        EventKind::ToolResult {
                            turn,
                            id: id.clone(),
                            summary: result.summary,
                            is_error: result.is_error,
                        },
                        Some(turn),
                    );
                }
                Err(e) => {
                    log.append(
                        EventKind::ToolResult {
                            turn,
                            id: id.clone(),
                            summary: format!("tool error: {}", e.message),
                            is_error: true,
                        },
                        Some(turn),
                    );
                }
            }
        }
        turn += 1;
    }
}

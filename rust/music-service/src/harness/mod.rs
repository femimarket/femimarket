// //! The agent harness, ported from the proven Swift reference into femi.
// //!
// //! Layers, bottom-up:
// //!   * `spine`  — the append-only event log (source of truth), typed content blocks
// //!     (reference-not-bytes), fold-to-transcript, and the transcript-validity invariant.
// //!   * `agent`  — the ports (Brain / Tools), budgets, fault taxonomy + bounded retry,
// //!     compaction, and the loop.
// //!   * `eval`   — record/replay cassette + metrics + the deterministic test suite.
// //!   * `lmstudio` / `tools` — the real adapters: LM Studio brain and femi's own
// //!     `handle_*` generation fns as in-process tools (reference-not-bytes persistence).
//
// pub mod spine;
// pub mod agent;
// pub mod eval;
// pub mod lmstudio;
// pub mod tools;

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package market.femi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import market.femi.models.AudioQA
import market.femi.models.allAnswered
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

fun Boolean.assertTrue(message: String? = null) {
    assertTrue(this, message)
}

/** Asserts that this [Boolean] value is false. */
fun Boolean.assertFalse(message: String? = null) {
    assertFalse(this, message)
}

/**
 * A real user journey. The only thing the test "sets up" is the external world — a precondition
 * that exists before the app runs. Everything else is the app running through WorkspaceViewModel.
 * No browser, no wasm, no IndexedDB. Runs on Node.
 */

abstract class SharedCommonTest(private val state: State) {

    private val dispatcher = UnconfinedTestDispatcher()

    fun integrationTest(body: suspend CoroutineScope.() -> Unit): TestResult =
        runTest(timeout = 10.minutes) {          // real wall-clock cap, not the 60s default
            Dispatchers.setMain(Dispatchers.Default)   // real main, NOT UnconfinedTestDispatcher
            try {
                withContext(Dispatchers.Default) { body() }   // ← outside the test dispatcher = real time, real threads
            } finally {
                Dispatchers.resetMain()
            }
        }


    // The VMs launch on viewModelScope (Dispatchers.Main) with an init collector, so Main must point
    // at a test dispatcher. UnconfinedTestDispatcher runs fire-and-forget intents eagerly, so
    // advanceUntilIdle() reliably drains them before we assert.
    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
    }
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun care() = integrationTest {
        assertEmptyStorage()
        assertNav()

        state.footer.clickJobs()
    }

    @Test
    fun songToMusicVideo() = integrationTest {
        state.db.clearAll()

        assertEmptyStorage()
        assertMusicNav()

//        setup()

        importDirectory()

        searchAndSelectAudio()
//
//        editLyricsAndAssertCommit()
//
//        editAboutFaqsAndAssert(true)
//
//        editSocialMediaBlueprintAndAssert(true)
////
//        // 4. Align assert word alignments
//        alignLyricsAndAssertWordAlignments {
//            advanceUntilIdle()
//        }
//
//        // 5. Assert edited lyrics again
//        assertEditedLyrics {
//            advanceUntilIdle()
//        }
//
//        // 6. Edit a word's alignment (drag): draft holds the new range, commit persists, reload proves it.
//        editWordAlignmentAndAssertPersist {
//            advanceUntilIdle()
//        }
//
//        // 7. Play that word's region, then pause: the engine plays the correct range.
//        playWordAndAssertRange {
//            advanceUntilIdle()
//        }
//
//        // 8. Pick + commit a protagonist image; reload proves the id3 picture write survives — same bar
//        //    as editedLyrics/wordAlignments (a real importDirectory() re-read, not a peek).
//        pickAndCommitProtagonistAndAssertPersist {
//            advanceUntilIdle()
//        }
//
//        // 9. Author the storyboard theme on line 0. IN-MEMORY only (mirrors updateItem) — proven on the
//        //    audioLines handle, not a reload (there is no durable write to round-trip).
//        authorStoryboardThemeAndAssert()
//
//        // 10. Generate a storyboard image for line 0: Jobs runs the i2i gateway, Workspace collects + persists
//        //     it to the repo; a real selectAudio re-read surfaces it on the UI-bound projectImages.
//        generateImageForLineAndAssertProjectImage { advanceUntilIdle() }
//
//        // 11. Queue that generated frame (a real user tap on the line-0 gallery), generate a Veo clip: Jobs
//        //     runs it, Workspace persists the clip to the repo; a re-read surfaces it on projectVideos.
//        generateVeoClipAndAssertProjectVideo { advanceUntilIdle() }
//
//        // 12. Save Sequence: mux the project's clip(s) into ONE final cut; capture it for export.
//        val finalCut = muxFinalCutAndReturnFinalCut { advanceUntilIdle() }
//
//        // 13. Export: press Download on the final cut; the host saves the finished music video via the DOM
//        //     anchor-download mechanism the app already uses for exportDatabase (FileOps.kt).
//        exportMusicVideoAndAssertDownload(finalCut) { advanceUntilIdle() }
    }

    fun clickMenu(){
        state.showMenu.click()
    }

    fun assertEmptyStorage(){
        assertEquals(state.data.audios.map.values.size, 0)
    }

    fun assertNav(){
        assertEquals(SplashRoute, state.nav.backStack.last())
        state.nav.openInfo()
        assertEquals(LegalInfoRoute, state.nav.backStack.last())

        state.nav.goBack()
        assertEquals(SplashRoute, state.nav.backStack.last())
    }

    fun assertCareNav(){
        assertNav()
        state.nav.openCare()
    }

    fun assertMusicNav(){
        assertNav()


        state.nav.openMusicApp()
        if (state.nav.backStack.last() == MusicAboutRoute) {
            state.nav.openInfo()
            assertEquals(LegalInfoRoute, state.nav.backStack.last())

            state.nav.goBack()
            state.music.about.openMusicApp()
        }
        assertEquals(MusicAppRoute, state.nav.backStack.last())
    }


    fun setup(){
        clickMenu()
        state.setupDialog.click()
        state.kv.fsUrl = TEST_FS_URL
        state.kv.dbUrl = TEST_DB_URL
        state.kv.codecUrl = TEST_CODEC_URL
        state.setup()
    }

    suspend fun importDirectory(){
        clickMenu()
        state.importDialog.click()
        state.importDialog.importDirectory().join()
        state.showMenu.value.assertFalse()
        with(state.data.audios.map.values.first()){
            assertNotNull(genre,"genre")
            assertNotNull(lyrics,"lyrics")
            assertNotNull(name,"name")
            assertNotNull(uid,"uid")
            assertNotNull(project,"project")
        }
    }

    suspend fun searchAndSelectAudio(){
        clickMenu()
        state.audioDialog.click()
        require(state.data.audios.list.isNotEmpty())
        state.audioDialog.searchLyrics("abc123").join()

        require(state.data.audios.list.isEmpty())
        state.audioDialog.searchLyrics("relative","abc123").join()

        require(state.data.audios.list.isEmpty())
        state.audioDialog.searchLyrics("relative").join()

        state.audioDialog.playAudio(state.data.audios.map.values.first())
        state.audioDialog.confirm().join()

        require(!state.showMenu.value)
        state.audio.pause()
    }

//    suspend fun importDirectoryAndAssertNonEmptyStorage(advanceUntilIdle:()-> Unit){
//        state.importDirectory()
//        advanceUntilIdle()
//    }
//
//    fun selectAudioAndAssertActive(advanceUntilIdle:()-> Unit){
//        val audio = state.activeData.audios.values.first()
//        state.selectAudio(audio)
//        advanceUntilIdle()
//        assertEquals(audio, state.activeData.activeAudio)
//    }
//
//
//    fun assertEditedLyrics(advanceUntilIdle:()-> Unit){
//        state.importDirectory()
//        advanceUntilIdle()
//        // edit still in the file
//        assertEquals(EDITED_LYRICS, state.activeData.activeAudio!!.editedLyrics)
//    }
//
//    fun alignLyricsAndAssertWordAlignments(advanceUntilIdle:()-> Unit){
//        state.alignLyrics(state.activeData.activeAudio!!.id)
//        assertTrue(state.activeData.action.isAligning)
//        advanceUntilIdle()
//        assertFalse(state.activeData.action.isAligning)
//        state.importDirectory()
//        assertTrue(state.activeData.activeAudio!!.wordAlignments.isNotEmpty())
//        // assert on the VM's real derivation (audioLines), not a raw model call: 3 lines == the
//        // 3-line EDITED text (via the wordAlignments branch — this audio has no authored lines yet).
//        assertEquals(EDITED_LYRICS.lines().size, state.activeData.audioLines.size)
//    }

    suspend fun editAboutFaqsAndAssert(skip: Boolean=false){
        clickMenu()
        state.faqsDialog.click()
        if (!skip){
            state.faqsDialog.generateFaqs().join()
            val q = "q1"
            state.faqsDialog.editQuestion(q,0)
            require(state.faqsDialog.faqs.first().question == q)
            repeat(state.faqsDialog.faqs.size) {
                state.faqsDialog.editAnswer("answer ${it + 1}", it)
            }
        }
        state.faqsDialog.faqs = faqs
        state.faqsDialog.confirm().join()
        require(state.data.audio!!.current.faqs.allAnswered)
    }

    suspend fun editSocialMediaBlueprintAndAssert(skip: Boolean=false){
        if (!skip) {
            clickMenu()
            state.socialMediaBlueprintDialog.click()
            state.socialMediaBlueprintDialog.generateBlueprint().join()
            println(state.socialMediaBlueprintDialog.blueprint)
            state.socialMediaBlueprintDialog.confirm().join()
        }
        require(!state.data.audio!!.current.socialMediaBlueprint.isNullOrBlank())
    }


    suspend fun editLyricsAndAssertCommit(){
        clickMenu()
        state.lyricsDialog.click()
        val expected = state.data.audio!!.current.lyrics().orEmpty().let {
            if (it.endsWith(" ")) {
                it.dropLast(1)
            } else {
                "$it "
            }
        }
        state.lyricsDialog.lyrics.type(expected)
        state.lyricsDialog.confirmLyrics().join()

        assertEquals(state.data.audio!!.current.editedLyrics, expected)
    }
//
//    fun editWordAlignmentAndAssertPersist(advanceUntilIdle:()-> Unit){
//        val audioId = state.activeAudio!!.id
//        val word = state.activeAudio!!.wordAlignments.first()
//        state.beginWordAlignmentEdit(0)
//        // begin seeds the draft from the committed word (captured audio id + index + start/end):
//        assertEquals(WordAlignmentDraft(audioId, 0, word.start, word.end), state.wordAlignmentDraft)
//        // single slider (shift): both ends move by the delta, duration preserved — transient:
//        state.shiftWordAlignment(0.1)
//        assertEquals(WordAlignmentDraft(audioId, 0, word.start + 0.1, word.end + 0.1), state.wordAlignmentDraft)
//        // range slider: in/out move independently — overwrites the draft; still nothing committed:
//        state.setWordAlignmentRange(word.start + 0.25, word.end + 0.5)
//        assertEquals(WordAlignmentDraft(audioId, 0, word.start + 0.25, word.end + 0.5), state.wordAlignmentDraft)
//        // committed data still untouched
//        assertEquals(word, state.activeAudio!!.wordAlignments.first())
//
//        state.commitWordAlignment()
//        advanceUntilIdle()
//        // draft cleared on commit
//        assertNull(state.wordAlignmentDraft)
//        // reload reads the file back
//        state.importDirectory()
//        advanceUntilIdle()
//        val reloaded = state.activeAudio!!.wordAlignments.first()
//        // new range was written to the file
//        assertEquals(word.start + 0.25, reloaded.start)
//        assertEquals(word.end + 0.5, reloaded.end)
//    }
//
//    fun playWordAndAssertRange(advanceUntilIdle:()-> Unit){
//        assertFalse(state.playback.isPlaying.value)
//        val word = state.activeAudio!!.wordAlignments.first()
//        state.playWord(0)
//        advanceUntilIdle()
//        // engine is playing
//        assertTrue(state.playback.isPlaying.value)
//        // loopRegion is a real exposed handle (the UI highlights it). Assert the OBSERVABLE BEHAVIOR —
//        // the loop SURROUNDS the word so it's heard in context — NOT the internal padding formula (rule
//        // 14: don't encode -5/+5; the test has no business knowing the padding). Survives a padding change.
//        val loop = state.playback.loopRegion.value!!
//        val inSec = loop.inSec!!
//        val outSec = loop.outSec!!
//        // clamped, never negative
//        assertTrue(inSec >= 0.0)
//        // lead-in: the loop starts at or before the word
//        assertTrue(inSec <= word.start)
//        // lead-out: the loop ends at or after the word
//        assertTrue(outSec >= word.end)
//
//        state.pausePlayback()
//        // pause stops it
//        assertFalse(state.playback.isPlaying.value)
//    }
//
//    suspend fun pickAndCommitProtagonistAndAssertPersist(advanceUntilIdle: () -> Unit) {
//        val audioId = state.activeAudio!!.id
//        // the user presses "pick image" → the OS image picker (files.pickImages) returns hero.png; pickProtagonist
//        // holds it as a draft ONLY — no reach-in, no fake entity, and (transience) no store/repo write yet.
//        state.pickProtagonist()
//        advanceUntilIdle()
//        // draft holds the picked bytes; NOTHING durable yet — no file, no library row, no committed protagonist.
//        // (proves #7/#9: a cancel or re-pick here would touch neither FileStore nor the repo — nothing to orphan)
//        val draft = state.protagonistDraft
//        check(draft is ProtagonistDraft.Uploaded) { "expected an Uploaded draft from the OS picker" }
//        assertEquals(PROTAGONIST_IMAGE_FILE, draft.picked.name)
//        assertFalse(state.fs.exists(PROTAGONIST_IMAGE_FILE))
//        assertNull(state.images[PROTAGONIST_IMAGE_FILE])
//        assertNull(state.activeAudio!!.protagonist)
//
//        state.commitProtagonist(audioId)
//        advanceUntilIdle()
//        // derived name set at commit; draft cleared; the protagonist image file materialized
//        assertEquals(AUDIO_PROTAGONIST_FILE, state.activeAudio!!.protagonist)
//        assertNull(state.protagonistDraft)
//        assertTrue(state.fs.exists(AUDIO_PROTAGONIST_FILE))
//        // the deferred source write + library upsert happened NOW, at commit — not back at pick time:
//        assertTrue(state.fs.exists(PROTAGONIST_IMAGE_FILE))
//        assertEquals(PROTAGONIST_IMAGE_FILE, state.images[PROTAGONIST_IMAGE_FILE]?.name)
//
//        // reload re-reads the id3 picture — same proof bar as the cover / editedLyrics on import
//        state.importDirectory()
//        advanceUntilIdle()
//        val reloaded = state.activeAudio!!
//        // SAME literal — commit and reload agree
//        assertEquals(AUDIO_PROTAGONIST_FILE, reloaded.protagonist)
//        assertTrue(state.fs.exists(reloaded.protagonist!!))
//        // load-bearing: proves the reload took the mp3 SUCCESS path (rebuilt from a fresh id3 read),
//        // not the MissingCoverArt early-return — which would abort this file and leave audios["song.mp3"]
//        // on its stale pre-reload cache, making the two assertions above pass even if writeProtagonist
//        // had clobbered the cover picture. MissingCoverArt is a real semantic VM effect (§14).
//        assertFalse(state.effects.contains(UiEffect.MissingCoverArt(AUDIO_FILE)))
//
//        // #3: RE-PICK a different-type image (repick.jpg, JPEG magic → commit derives "song-protagonist.jpg").
//        // commit must clean up the OLD derived file it made ("song-protagonist.png") — an artifact it owns —
//        // but NEVER the picked source.
//        state.pickProtagonist()
//        advanceUntilIdle()
//        state.commitProtagonist(audioId)
//        advanceUntilIdle()
//        assertEquals(AUDIO_PROTAGONIST_FILE_JPG, state.activeAudio!!.protagonist)
//        assertTrue(state.fs.exists(AUDIO_PROTAGONIST_FILE_JPG))
//        // the old derived file is gone (owned cleanup, #3); the picked source is untouched
//        assertFalse(state.fs.exists(AUDIO_PROTAGONIST_FILE))
//        assertTrue(state.fs.exists(REPICK_IMAGE_FILE))
//    }
//
//    // SYNCHRONOUS — no advanceUntilIdle lambda: the four theme intents are plain (non-launch), so the
//    // assertions run right after the fold, in-thread. Storyboard theme authoring is IN-MEMORY only (it
//    // mirrors AudioLineCard's "Save" → updateItem, no id3/repo write), so the proof is the audioLines
//    // handle, not a reload.
//    fun authorStoryboardThemeAndAssert() {
//        // precondition: line 0 has no AUTHORED themes yet — the 3 lines shown are DERIVED from the
//        // wordAlignments branch (activeAudio.audioLines is still empty), so this is genuinely authoring a
//        // theme onto a bare line, not editing an already-authored one.
//        assertTrue(state.activeAudio!!.audioLines.isEmpty())
//        // open line 0's theme editor: seeds the draft from that line's (empty) themes
//        state.beginStoryboardThemeEdit(0)
//        // "Add Theme": appends a blank AudioTheme the user then types into
//        state.addStoryboardTheme()
//        // type the theme text into the freshly-added blank theme
//        state.editStoryboardTheme(0, STORYBOARD_THEME)
//        // "Save": folds the draft into the line list (in-memory, mirrors updateItem)
//        state.commitStoryboardThemeEdit()
//        // assert on the UI-bound audioLines handle: still 3 lines, but now via the AUTHORED branch (commit
//        // materialized audio.audioLines), and line 0 carries the theme the user typed.
//        assertEquals(3, state.audioLines.size)
//        assertEquals(STORYBOARD_THEME, state.audioLines[0].themes.first().theme)
//    }
//
//    // Phase 6 image-gen: dispatch a storyboard image job for line 0. The Jobs satellite runs the i2i
//    // gateway; Workspace collects + persists the produced frame in its init results-collector and emits
//    // ImagesAdded; a real selectAudio re-read then surfaces the frame on the UI-bound projectImages. The
//    // durability bar here is repo-persist + surfacing on the project list (C8) — NOT an id3 round-trip:
//    // images have no id3 path, so an importDirectory re-read is impossible for them.
//    fun generateImageForLineAndAssertProjectImage(advanceUntilIdle: () -> Unit) {
//        val activeAudio = state.activeAudio!!
//        // the user submits the prompt for line 0's frame — the committed protagonist rides in as the i2i ref
//        state.generateImageForLine(activeAudio.id, 0, IMAGE_PROMPT)
//        advanceUntilIdle()
//        // the collector fired the semantic effect after persisting the one produced frame
//        assertTrue(state.effects.contains(UiEffect.ImagesAdded(1)))
//        // re-read the project gallery the real UI shows (selectAudio reloads projectImages from the repo)
//        state.selectAudio(activeAudio)
//        advanceUntilIdle()
//        // ONE surfaced frame must carry all three provenance facts together — proving it durably persisted to
//        // the repo AND flows onto the UI-bound project list, with the "imagegen follows protagonist/theme"
//        // ordering made falsifiable:
//        //  • name == "image-from-<protagonist>" is FakeGenerationGateway's "image-from-<refImage>" filename
//        //    encoding (Fakes.kt), so this only holds if imagegen ran FROM the committed protagonist i2i
//        //    reference (song-protagonist.jpg, committed in step 8) — it fails if refImage were blank/wrong,
//        //    closing the step-8 → step-10 link.
//        //  • theme == STORYBOARD_THEME proves step 9's authored theme threaded onto the frame — closing the
//        //    step-9 → step-10 link (fails if theme threading broke).
//        //  • audioLineText == LINE0_TEXT keeps line 0's derived text as the frame's storyboard provenance.
//        assertTrue(
//            state.projectImages.any {
//                it.name == "image-from-$AUDIO_PROTAGONIST_FILE_JPG" &&
//                        it.theme == STORYBOARD_THEME &&
//                        it.audioLineText == LINE0_TEXT
//            }
//        )
//    }
//
//    // Phase 7 veo: queue the generated storyboard frame (a real user tap on line 0's gallery), then generate
//    // a Veo clip FROM it. The Jobs satellite runs the veo gateway; Workspace collects + persists the produced
//    // clip in its init results-collector and emits ClipsAdded; a real selectAudio re-read then surfaces the
//    // clip on the UI-bound projectVideos. The durability bar here is repo-persist + surfacing on the project
//    // list (C8) — NOT an id3 round-trip: videos have no id3 path, so an importDirectory re-read is impossible
//    // for them. Provenance is DOUBLY asserted (R2/R5): the clip carries line 0's text AND its identity embeds
//    // the GENERATED frame's filename (not the protagonist SOURCE image's), a strong discriminator that
//    // survives the cumulative ClipsAdded — so a bare contains(ClipsAdded(1)) is deliberately NOT relied on.
//    fun generateVeoClipAndAssertProjectVideo(advanceUntilIdle: () -> Unit) {
//        val activeAudio = state.activeAudio!!
//        // the user taps a generated frame shown under line 0 in the storyboard gallery — projectImages is the
//        // durable, UI-bound project list (same read-idiom as the green journey's audios.values.first()),
//        // filtered to the frame produced under line 0 (audioLineText == LINE0_TEXT). This is app-PRODUCED
//        // output — NOT the raw workspace.images map and NOT a fabricated precondition — so it is not the F7
//        // pickImages case; it is also DISTINCT from the protagonist SOURCE image (whose audioLineText is null).
//        val frame = state.projectImages.first { it.audioLineText == LINE0_TEXT }
//        // real intent — mirrors onAddToVideoQueue(selectedImage) (Studio.kt:2535): the tapped frame joins the
//        // keyframe queue that feeds the next clip.
//        state.queueKeyframe(frame)
//        // the user presses Generate: Jobs runs the veo gateway over the queued keyframe; the produced clip
//        // returns via the init collector (persist + ClipsAdded).
//        state.generateVideoClips()
//        advanceUntilIdle()
//        // re-read the project gallery the real UI shows (selectAudio reloads projectVideos from the repo)
//        state.selectAudio(activeAudio)
//        advanceUntilIdle()
//        // the produced clip now surfaces on projectVideos carrying line 0's text as its provenance — proving it
//        // durably persisted to the repo AND flows onto the UI-bound project list.
//        val producedClip = state.projectVideos.first { it.audioLineText == LINE0_TEXT }
//        // load-bearing provenance: the clip's identity embeds the QUEUED generated frame's filename (submitVeo
//        // names the clip after its refImage), proving it was generated FROM the generated frame — NOT the
//        // protagonist source image, whose clip would embed "song-protagonist.jpg" and never the frame's
//        // "image-from-…" name. This is the strong discriminator that survives the cumulative ClipsAdded (R5).
//        assertTrue(producedClip.name.contains(frame.name))
//    }
//
//    // Phase 9 mux: press "Save Sequence" to stitch the project's persisted clip(s) into ONE final cut. The
//    // Jobs satellite runs the codec's muxSequence; Workspace collects + persists the produced final-cut Video
//    // in its init results-collector; a real selectAudio re-read then surfaces it on the UI-bound projectVideos.
//    // The durability bar is repo-persist + surfacing on the project list (C8) — NOT an id3 round-trip: videos
//    // have no id3 path, so an importDirectory re-read is impossible for them. The final cut is identified on the
//    // DURABLE, project-scoped re-read (projectVideos.single { it.id !in inputClipIds }) — the one video that was
//    // NOT an input clip — NOT the raw workspace.videos map (R4 / C7). RETURNS that Video so step 13 (export) can
//    // consume it (R4 — mux produces the final cut, export CONSUMES it). A bare contains(ClipsAdded(1)) is a weak
//    // discriminator by step 12 (cumulative ImagesAdded + two ClipsAdded), so it is deliberately NOT relied on (R5).
//    fun muxFinalCutAndReturnFinalCut(advanceUntilIdle: () -> Unit): Video {
//        val activeAudio = state.activeAudio!!
//        val audioId = activeAudio.id
//        // precondition: exactly one clip is present, so unfiltered projectVideos == the intended stitch input
//        // (the source's export-flag filtering is inert with a single clip — R3 / PLAN #4 / C13).
//        assertEquals(1, state.projectVideos.size)
//        // capture the input clip ids BEFORE the mux so the final cut is identifiable as the one NEW video the
//        // mux adds — the durable discriminator, independent of the cumulative ClipsAdded effect (R4 / C7).
//        val inputClipIds = state.projectVideos.map { it.id }.toSet()
//        // the user presses "Save Sequence": Jobs muxes the project's clip(s) into ONE final cut.
//        state.requestFinalCut(audioId)
//        // during-window (C5): the mux job is in flight — muxSequence's delay(1) suspends it, so jobs.isRunning
//        // is observably true BEFORE we drain the scheduler (mirrors align's assertTrue(isAligning) at step 4).
//        assertTrue(state.jobs.isRunning)
//        advanceUntilIdle()
//        // re-read the project gallery the real UI shows (selectAudio reloads projectVideos from the repo)
//        state.selectAudio(activeAudio)
//        advanceUntilIdle()
//        // the final cut is the one video on the durable project-scoped re-read that was NOT an input clip —
//        // proving the mux durably persisted a NEW clip to the repo AND it surfaces on the UI-bound project list.
//        return state.projectVideos.single { it.id !in inputClipIds }
//    }
//
//    // Phase 9 export: press "Download" on the final cut the mux step produced (R4 — mux PRODUCES it, export
//    // CONSUMES it; threaded in as `finalCut`, identified by the id the user clicked). exportMusicVideo emits the
//    // semantic UiEffect.Download naming the muxed mp4; the per-platform UI does the DOM anchor dance
//    // (FileOps.kt:54-68), deliberately NOT a port. No reload here — videos do not round-trip through id3 (C8/C12),
//    // so durability was already proven in step 12; export only names that same persisted file for download.
//    suspend fun exportMusicVideoAndAssertDownload(finalCut: Video, advanceUntilIdle: () -> Unit) {
//        // the muxed mp4 really landed in the store during the mux step (R1) — the download names a real file
//        assertTrue(state.fs.exists(finalCut.name))
//        state.exportMusicVideo(finalCut.id)
//        advanceUntilIdle()
//        // Download is a semantic effect (no English); the UI does the DOM anchor dance (FileOps.kt:54-68).
//        // This exact-name assertion alone establishes the success path ran: exportMusicVideo is called once
//        // and its two outcomes are mutually-exclusive early-returns (fail-and-return on a missing file, OR
//        // the Download below), so a surfaced Download(finalCut.name) is itself proof the file-missing branch
//        // did not fire.
//        assertTrue(state.effects.contains(UiEffect.Download(finalCut.name)))
//    }
}

/**
 * The one sanctioned pure, non-journey test (PLAN.md §3 rule 1: "Pure non-journey logic may get a tiny
 * ModelLogicTest. That's the only exception."), giving planRenderSegments its own load-bearing coverage.
 *
 * The journey itself CANNOT exercise the planner: FakeMediaCodec.muxSequence keys its muxed output off the
 * out name and IGNORES the RenderSegment list it is handed, so a broken (or empty) planner would never
 * surface in step 12 — and asserting the planner's private RenderSegment geometry inside a journey step is
 * itself disallowed (it is not a UI-bound handle). So the planner's overlap-resolution + adjacent-merge
 * geometry is proven directly here, on hand-constructed clips, exactly as ModelLogicTest proves the pure
 * getAlignedLines / buildLanes logic.
 */
//class RenderSegmentPlannerTest {
//
//    // A lone clip fills its whole [startMs, endMs) window with exactly one render segment — the baseline
//    // the muxer stitches when nothing overlaps.
//    @Test
//    fun singleClip_yieldsOneSegmentSpanningItsWholeWindow() {
//        val loneClip = Video(id = "clip-lone", startMs = 0.0, endMs = 4000.0)
//
//        val segments = planRenderSegments(listOf(loneClip))
//
//        assertEquals(1, segments.size)
//        assertEquals(loneClip, segments.single().video)
//        assertEquals(0.0, segments.single().visibleStartMs)
//        assertEquals(4000.0, segments.single().visibleEndMs)
//    }
//
//    // Two clips overlapping in time: the planner must (a) resolve the contested window so the topmost clip
//    // wins it, and (b) merge that winner's now-adjacent windows into a single segment. Input-list order IS
//    // the precedence order — the planner samples each window's midpoint and the FIRST clip covering it is
//    // "on top" — so the top clip is listed first.
//    @Test
//    fun overlappingClips_topClipWinsTheOverlapAndItsAdjacentWindowsMerge() {
//        // starts later, sits on top; covers [1000, 3000)
//        val topClip = Video(id = "clip-top", startMs = 1000.0, endMs = 3000.0)
//        // starts first, underneath; covers [0, 2000) — the [1000, 2000) slice is contested by topClip
//        val bottomClip = Video(id = "clip-bottom", startMs = 0.0, endMs = 2000.0)
//
//        // topClip first == topmost at any contested instant
//        val segments = planRenderSegments(listOf(topClip, bottomClip))
//
//        // Two segments survive: bottomClip is visible only until topClip takes over at 1000, and topClip's
//        // two consecutive windows (the [1000, 2000) it won from bottomClip, then its own [2000, 3000))
//        // coalesce into a single [1000, 3000) segment.
//        assertEquals(2, segments.size)
//        // bottomClip is cut off at the overlap boundary (1000), NOT at its own end (2000) — overlap resolved
//        assertEquals(bottomClip, segments[0].video)
//        assertEquals(0.0, segments[0].visibleStartMs)
//        assertEquals(1000.0, segments[0].visibleEndMs)
//        // topClip spans the whole contested-plus-solo stretch as ONE merged segment
//        assertEquals(topClip, segments[1].video)
//        assertEquals(1000.0, segments[1].visibleStartMs)
//        assertEquals(3000.0, segments[1].visibleEndMs)
//    }
//}

val faqs =listOf(
    AudioQA(
        question = "What does the \"candy shop\" metaphor represent?",
        answer = "It's like a kid that loves candy but doesn't really respect that the sugar is bad for you. Kids will eat candy till cows come home. In this song the line represents consciously dying in the candy of life",
    ),
    AudioQA(
        question = "The \"wealth can't buy mental health\" line?",
        answer = "This represents that you can have all the success and gold which are all material. They're very binary mathematical forms of success. But mental health, just waking up and feeling sad. Money doesn't fix that illogicality. Maths can't deduce it.",
    ),
    AudioQA(
        question = "What does \"Tek the EL... boy\" mean?",
        answer = "It means take the loss. It's ok to lose.",
    ),
    AudioQA(
        question = "What does the \"demon in your ear\" represent?",
        answer = "It's not literally tied to a demon but could be as it represents the voice you listen to good or bad. In this case the voice was bad voice being a demon.",
    ),
    AudioQA(
        question = "What genre and audience did you envision when writing these lyrics and ad-libs?",
        answer = "Wasn't really envisioning a audience nor can imagine. I wrote the song feeling the extreme accuracy of each word. That was all. I think it's rare to hear very true accurate words as a song.",
    ),
    AudioQA(
        question = "What sparked the idea for the opening relative morality line?",
        answer = "Because personally I remember in vivid conscious experience that I frankly used to believe that right or wrong were all subjective concepts until I realised otherwise.",
    ),
    AudioQA(
        question = "What is your 2nd favourite verse or line and why?",
        answer = "Hard to say. I would definitely say it's \"Oh boy I really need help\" Because I show my vulnerability without shame which is almost empowering. I'd never do that before.",
    ),
    AudioQA(
        question = "What's with verbal gestures interjections here and there like woof woof?",
        answer = "Great question! It's part of the song. There is a concept I learnt called call and response. That's not what I'm doing but similar concept. The reality of the song demanded those interjections as they are lyrics in their own right with meaning.",
    ),
    AudioQA(
        question = "What happens to the artist in the end?",
        answer = "The journey continues to present day",
    ),
    AudioQA(
        question = "What feeling should listeners experience during the final chorus?",
        answer = "This is one of those questions I don't even think I can answer. I don't know how you should feel when u hear a accurate account of a reality. I think my job was just to put it out there.",
    ),
//        AudioQA(
//            question = "How did you decide on the repetitive \"throw it down\" structure?",
//            answer = "LOL wasn't me. Was AI entirely. I worked my part and AI collaborated added its own contribution independently.",
//        ),
//        AudioQA(
//            question = "Sorry last question, what was the trap and big lie you refer to?",
//            answer = "Basically imagine having money feeling on top of the world accomplished with many goals. That's the trap.",
//        ),
)
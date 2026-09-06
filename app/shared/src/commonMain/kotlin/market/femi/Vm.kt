package market.femi

import com.russhwolf.settings.Settings

//class Vm(
//    val repo: FakeMediaRepository = FakeMediaRepository(),
//    val files: FakeFileStore = FakeFileStore(),
//    val meta: FakeMetadataService = FakeMetadataService(),
//    val gen: FakeGenerationGateway = FakeGenerationGateway(),
//    val codec: FakeMediaCodec = FakeMediaCodec(files),
//    val settings: FakeSettingsStore = FakeSettingsStore(),
//    // Parity Ledger ROW 6 — the secret store fake the VM's keys/config setters route through (KEY-04).
//    val secrets: FakeSecretStore = FakeSecretStore(),
//    val ids: SeqIdGenerator = SeqIdGenerator(),
//    val audioEngine: FakeAudioEngine = FakeAudioEngine(),
//    val frameClock: FakeFrameClock = FakeFrameClock(),
//)
//
//interface StudioHarness {
//    // 1. Your interface dependencies
//    val repo: MediaRepository
//    val files: FileStore
//    val meta: MetadataService
//    val gen: GenerationGateway
//    val codec: MediaCodec
//    val settings: Settings
////    val secrets: FakeSecretStore
////    val ids: SeqIdGenerator
//    val audioEngine: AudioEngine
////    val frameClock: FakeFrameClock
//
//    // 2. The ViewModels, wired up automatically using the properties above
////    val jobs: JobsViewModel
////        get() = JobsViewModel(gen, codec, ids)
//
//    val playback: PlaybackViewModel
//        get() = PlaybackViewModel(audioEngine, frameClock)
//
//    val workspace: WorkspaceViewModel
//        get() = WorkspaceViewModel(repo, files, gen, meta, settings, secrets, ids, jobs, playback)
//
//    // 3. Effects observing
//    val effects: MutableList<UiEffect>
//
//    fun observeEffects(scope: CoroutineScope) {
//        scope.launch { workspace.effects.collect { effects += it } }
//    }
//}
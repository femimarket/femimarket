package market.femi

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import market.femi.services.FakeAudioService
import market.femi.services.createRealDbService
import market.femi.services.createRealFsService
import market.femi.services.createRealKvService
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicDesktopTest private constructor(
    state: State
): SharedCommonTest(state) {
    constructor() : this(
        run {
            State()
        }
    )

    @Test
    fun dd() = runTest {

    }

//    @Test
//    fun example() = runTest {
//
//        val file = FileKit.openFilePicker()
//
//        advanceUntilIdle()
//        runBlocking {
//            adva
//
//
//        }
//    }

}
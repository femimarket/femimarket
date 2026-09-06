package market.femi.state

import market.femi.State

class MusicAboutState(private val state: State){
    fun openMusicApp() {
        state.kv.musicInfo = true
        state.nav.openMusicApp()
    }
}
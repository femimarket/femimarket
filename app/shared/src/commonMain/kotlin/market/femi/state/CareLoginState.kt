package market.femi.state

import market.femi.State

class CareLoginState(private val state: State){
    fun openLogin() {
        state.kv.careLogin = true
        state.nav.openLogin()
    }
}

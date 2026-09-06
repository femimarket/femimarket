package market.femi.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import market.femi.API_URL_KEY
import market.femi.CANDIDATE_ID_KEY
import market.femi.MATCH_SESSION_ID_KEY
import market.femi.TEST_CANDIDATE_ID
import market.femi.TEST_MATCH_SESSION_ID
import market.femi.CODEC_URL_KEY
import market.femi.DB_PASS_KEY
import market.femi.DB_URL_KEY
import market.femi.DB_USER_KEY
import market.femi.FS_URL_KEY
import market.femi.LLM_URL_KEY
import market.femi.MATRIX_ACCESS_TOKEN_KEY
import market.femi.MATRIX_CLIENT_ID_KEY
import market.femi.MATRIX_REFRESH_TOKEN_KEY
import market.femi.META_URL_KEY
import market.femi.CARE_LOGIN_KEY
import market.femi.MUSIC_INFO_KEY
import market.femi.TEST_API_URL
import market.femi.TEST_CODEC_URL
import market.femi.TEST_DB_PASS
import market.femi.TEST_DB_URL
import market.femi.TEST_DB_USER
import market.femi.TEST_FS_URL
import market.femi.TEST_LLM_URL
import market.femi.TEST_MATRIX_ACCESS_TOKEN
import market.femi.TEST_MATRIX_CLIENT_ID
import market.femi.TEST_MATRIX_REFRESH_TOKEN
import market.femi.MATRIX_URL_KEY
import market.femi.TEST_MATRIX_URL
import market.femi.TEST_META_URL

interface KvService: Settings {                                         // a named byte store; the web adapter backs it with the picked FS directory
    var fsUrl: String
    var dbUrl: String
    var codecUrl: String
    var metaUrl: String
    var llmUrl: String
    var apiUrl: String
    var dbUsername: String
    var dbPass: String
    var musicInfo: Boolean
    var careLogin: Boolean
    var matrixAccessToken: String
    var matrixRefreshToken: String
    var matrixClientId: String
    var matrixUrl: String
    var candidateId: String
    var matchSessionId: String

    fun withFsUrl(s: String) = "$fsUrl/$s"
}

class RealKvService(
    private val delegate: Settings = Settings() // Uses the multiplatform default instance
) : KvService, Settings by delegate {           // 'by delegate' forwards all Settings methods automatically

    private val fsUrlState = mutableStateOf(getString(FS_URL_KEY,TEST_FS_URL))
    override var fsUrl: String
        get() = fsUrlState.value
        set(value) {
            fsUrlState.value = value
            putString(FS_URL_KEY, value)
        }

    private val dbUrlState = mutableStateOf(getString(DB_URL_KEY,TEST_DB_URL))
    override var dbUrl: String
        get() = dbUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            dbUrlState.value = formatted
            putString(DB_URL_KEY, formatted)
        }
    private val codecUrlState = mutableStateOf(getString(CODEC_URL_KEY, TEST_CODEC_URL))
    override var codecUrl: String
        get() = codecUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            codecUrlState.value = formatted
            putString(CODEC_URL_KEY, formatted)
        }
    private val metaUrlState = mutableStateOf(getString(META_URL_KEY, TEST_META_URL))
    override var metaUrl: String
        get() = metaUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            metaUrlState.value = formatted
            putString(META_URL_KEY, formatted)
        }

    private val llmUrlState = mutableStateOf(getString(LLM_URL_KEY, TEST_LLM_URL))
    override var llmUrl: String
        get() = llmUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            llmUrlState.value = formatted
            putString(LLM_URL_KEY, formatted)
        }

    private val candidateIdState = mutableStateOf(getString(CANDIDATE_ID_KEY, TEST_CANDIDATE_ID))
    override var candidateId: String
        get() = candidateIdState.value
        set(value) {
            candidateIdState.value = value
            putString(CANDIDATE_ID_KEY, value)
        }

    private val matchSessionIdState = mutableStateOf(getString(MATCH_SESSION_ID_KEY, TEST_MATCH_SESSION_ID))
    override var matchSessionId: String
        get() = matchSessionIdState.value
        set(value) {
            matchSessionIdState.value = value
            putString(MATCH_SESSION_ID_KEY, value)
        }

    private val apiUrlState = mutableStateOf(getString(API_URL_KEY, TEST_API_URL))
    override var apiUrl: String
        get() = apiUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            apiUrlState.value = formatted
            putString(API_URL_KEY, formatted)
        }

    private val dbUsernameState = mutableStateOf(getString(DB_USER_KEY, TEST_DB_USER))
    override var dbUsername: String
        get() = dbUsernameState.value
        set(value) {
            val formatted = value.trimEnd('/')
            dbUsernameState.value = formatted
            putString(DB_USER_KEY, formatted)
        }

    private val dbPassState = mutableStateOf(getString(DB_PASS_KEY, TEST_DB_PASS))
    override var dbPass: String
        get() = dbPassState.value
        set(value) {
            val formatted = value.trimEnd('/')
            dbPassState.value = formatted
            putString(DB_PASS_KEY, formatted)
        }

    private val musicInfoState = mutableStateOf(getBoolean(MUSIC_INFO_KEY, false))
    override var musicInfo: Boolean
        get() = musicInfoState.value
        set(value) {
            musicInfoState.value = value
            putBoolean(MUSIC_INFO_KEY, value)
        }

    private val careLoginState = mutableStateOf(getBoolean(CARE_LOGIN_KEY, false))
    override var careLogin: Boolean
        get() = careLoginState.value
        set(value) {
            careLoginState.value = value
            putBoolean(CARE_LOGIN_KEY, value)
        }


    private val matrixUrlState = mutableStateOf(getString(MATRIX_URL_KEY, TEST_MATRIX_URL))
    override var matrixUrl: String
        get() = matrixUrlState.value
        set(value) {
            matrixUrlState.value = value
            putString(MATRIX_URL_KEY, value)
        }

    private val matrixAccessTokenState = mutableStateOf(getString(MATRIX_ACCESS_TOKEN_KEY, TEST_MATRIX_ACCESS_TOKEN))
    override var matrixAccessToken: String
        get() = matrixAccessTokenState.value
        set(value) {
            matrixAccessTokenState.value = value
            putString(MATRIX_ACCESS_TOKEN_KEY, value)
        }

    private val matrixRefreshTokenState = mutableStateOf(getString(MATRIX_REFRESH_TOKEN_KEY, TEST_MATRIX_REFRESH_TOKEN))
    override var matrixRefreshToken: String
        get() = matrixRefreshTokenState.value
        set(value) {
            matrixRefreshTokenState.value = value
            putString(MATRIX_REFRESH_TOKEN_KEY, value)
        }

    private val matrixClientIdState = mutableStateOf(getString(MATRIX_CLIENT_ID_KEY, TEST_MATRIX_CLIENT_ID))
    override var matrixClientId: String
        get() = matrixClientIdState.value
        set(value) {
            matrixClientIdState.value = value
            putString(MATRIX_CLIENT_ID_KEY, value)
        }

}


fun createRealKvService(): KvService = RealKvService()


class FakeKvService : KvService {

    override var fsUrl: String
        get() = getString(FS_URL_KEY, TEST_FS_URL)
        set(value) = putString(FS_URL_KEY,value.orEmpty())
    override var dbUrl: String
        get() = getString(DB_URL_KEY, TEST_DB_URL)
        set(value) = putString(DB_URL_KEY,value.orEmpty())
    override var codecUrl: String
        get() = getString(CODEC_URL_KEY, TEST_CODEC_URL)
        set(value) = putString(CODEC_URL_KEY, value.trimEnd('/'))
    override var metaUrl: String
        get() = getString(META_URL_KEY, TEST_META_URL)
        set(value) = putString(META_URL_KEY, value.trimEnd('/'))
    override var llmUrl: String
        get() = getString(LLM_URL_KEY, TEST_LLM_URL)
        set(value) = putString(LLM_URL_KEY, value.trimEnd('/'))
    override var apiUrl: String
        get() = getString(API_URL_KEY, TEST_API_URL)
        set(value) = putString(API_URL_KEY, value.trimEnd('/'))
    override var musicInfo: Boolean
        get() = getBoolean(MUSIC_INFO_KEY, false)
        set(value) = putBoolean(MUSIC_INFO_KEY, value)
    override var careLogin: Boolean
        get() = getBoolean(CARE_LOGIN_KEY, false)
        set(value) = putBoolean(CARE_LOGIN_KEY, value)
    override var matrixAccessToken: String
        get() = getString(MATRIX_ACCESS_TOKEN_KEY, TEST_MATRIX_ACCESS_TOKEN)
        set(value) = putString(MATRIX_ACCESS_TOKEN_KEY, value)
    override var matrixUrl: String
        get() = getString(MATRIX_URL_KEY, TEST_MATRIX_URL)
        set(value) = putString(MATRIX_URL_KEY, value.trimEnd('/'))
    override var matrixRefreshToken: String
        get() = getString(MATRIX_REFRESH_TOKEN_KEY, TEST_MATRIX_REFRESH_TOKEN)
        set(value) = putString(MATRIX_REFRESH_TOKEN_KEY, value)
    override var matrixClientId: String
        get() = getString(MATRIX_CLIENT_ID_KEY, TEST_MATRIX_CLIENT_ID)
        set(value) = putString(MATRIX_CLIENT_ID_KEY, value)
    override var candidateId: String
        get() = getString(CANDIDATE_ID_KEY, TEST_CANDIDATE_ID)
        set(value) = putString(CANDIDATE_ID_KEY, value)
    override var matchSessionId: String
        get() = getString(MATCH_SESSION_ID_KEY, TEST_MATCH_SESSION_ID)
        set(value) = putString(MATCH_SESSION_ID_KEY, value)
    override var dbUsername by mutableStateOf(getString(DB_USER_KEY, ""))
    override var dbPass by mutableStateOf(getString(DB_PASS_KEY, ""))


    private val s = mutableMapOf<String, Any?>()
    override fun getStringOrNull(key: String): String? = s[key] as String?
    override fun putString(key: String, value: String) {
        s[key] = value
    }
    override fun getLongOrNull(key: String): Long? = s[key] as Long?
    override fun putLong(key: String, value: Long) {
        s[key] = value
    }

    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun remove(key: String) {
        TODO("Not yet implemented")
    }

    override fun hasKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun putInt(key: String, value: Int) {
        TODO("Not yet implemented")
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        TODO("Not yet implemented")
    }

    override fun getIntOrNull(key: String): Int? {
        TODO("Not yet implemented")
    }



    override fun getLong(key: String, defaultValue: Long): Long {
        TODO("Not yet implemented")
    }





    override fun getString(key: String, defaultValue: String): String {
        TODO("Not yet implemented")
    }


    override fun putFloat(key: String, value: Float) {
        TODO("Not yet implemented")
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        TODO("Not yet implemented")
    }

    override fun getFloatOrNull(key: String): Float? {
        TODO("Not yet implemented")
    }

    override fun putDouble(key: String, value: Double) {
        TODO("Not yet implemented")
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        TODO("Not yet implemented")
    }

    override fun getDoubleOrNull(key: String): Double? {
        TODO("Not yet implemented")
    }

    override fun putBoolean(key: String, value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBooleanOrNull(key: String): Boolean? {
        TODO("Not yet implemented")
    }
}
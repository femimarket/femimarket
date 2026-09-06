package market.femi.services

import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import market.femi.AppJson
import market.femi.money.Bank
import market.femi.money.Currency
import market.femi.money.FixedSide
import market.femi.money.FundingAccount
import market.femi.money.FxRate
import market.femi.money.KycPolicy
import market.femi.money.KycTier
import market.femi.money.Money
import market.femi.money.RATE_SCALE
import market.femi.money.Rail
import market.femi.money.Recipient
import market.femi.money.ResolveState
import market.femi.money.SEED_RATES
import market.femi.money.Transaction
import market.femi.money.User
import market.femi.money.seedMidE6
import market.femi.money.toWireString
import kotlin.time.Clock
import kotlin.uuid.Uuid

interface MoneyService {
    var dbUrl: String

    var ccBaseUrl: String

    var ccLoginId: String

    var ccApiKey: String

    val isDemo: Boolean get() = ccLoginId.isBlank() || ccApiKey.isBlank()

    suspend fun saveUser(user: User)
    suspend fun user(): User?
    suspend fun saveRecipient(recipient: Recipient)
    suspend fun recipients(): List<Recipient>
    suspend fun deleteRecipient(id: String)
    suspend fun saveTransaction(transaction: Transaction)
    suspend fun transactions(): List<Transaction>
    suspend fun clearAll()

    val isSeeded: Boolean

    suspend fun refresh()

    fun rate(sell: Currency, buy: Currency, markupBps: Int): FxRate?

    fun tierRequiredFor(amount: Money, lifetimeSentMinor: Long): KycTier

    fun blockingTier(user: User?, amount: Money, lifetimeSentMinor: Long): KycTier?

    fun reasonFor(tier: KycTier): String

    suspend fun startVerification(user: User): String?

    fun supports(bank: Bank): Boolean

    suspend fun resolve(bank: Bank, accountNumber: String): ResolveState

    val isSandbox: Boolean

    suspend fun fundingAccount(currency: Currency, reference: String): FundingAccount?

    suspend fun createBeneficiary(recipient: Recipient): String

    suspend fun createConversion(
        sell: Money,
        buy: Money,
        fixedSide: FixedSide,
        uniqueRequestId: String,
    ): String

    suspend fun createPayment(
        beneficiaryId: String,
        conversionId: String,
        amount: Money,
        reason: String,
        reference: String,
        uniqueRequestId: String,
    ): PaymentResult

    suspend fun paymentStatus(paymentId: String): String

    suspend fun simulatePayin(receiverAccountNumber: String, amount: Money, reference: String)
}

data class PaymentResult(
    val paymentId: String,
    val status: String,
    val shortReference: String = "",
)

val TERMINAL_SUCCESS = setOf("completed")

val TERMINAL_FAILURE = setOf("failed", "deleted")

const val MONEY_DB_URL_KEY = "rmt.db.url"
const val MONEY_CC_BASE_URL_KEY = "rmt.cc.baseUrl"
const val MONEY_CC_LOGIN_ID_KEY = "rmt.cc.loginId"
const val MONEY_CC_API_KEY_KEY = "rmt.cc.apiKey"

const val MONEY_DEFAULT_DB_URL = "https://surreal.femi.market"

const val CURRENCYCLOUD_SANDBOX_URL = "https://devapi.currencycloud.com"

const val TABLE_MONEY_USER = "rmt_users"
const val TABLE_MONEY_RECIPIENT = "rmt_recipients"
const val TABLE_MONEY_TRANSACTION = "rmt_transactions"

fun createRealMoneyService(kv: Settings): MoneyService = RealMoneyService(kv)

@Serializable
private data class ErApiResponse(
    val result: String = "",
    @SerialName("base_code") val baseCode: String = "",
    val rates: Map<String, Double> = emptyMap(),
)

class RealMoneyService(
    private val kv: Settings,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(AppJson) }
    },
    private val log: LogService = createRealLogService("RealMoneyService"),
    private val namespace: String = "main",
    private val database: String = "main",
    private val ratesEndpoint: String = "https://open.er-api.com/v6/latest",
    private val latencyMs: Long = 250,
) : MoneyService {

    private val dbUrlState = mutableStateOf(kv.getString(MONEY_DB_URL_KEY, MONEY_DEFAULT_DB_URL))
    override var dbUrl: String
        get() = dbUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            dbUrlState.value = formatted
            kv.putString(MONEY_DB_URL_KEY, formatted)
        }

    private val ccBaseUrlState = mutableStateOf(kv.getString(MONEY_CC_BASE_URL_KEY, CURRENCYCLOUD_SANDBOX_URL))
    override var ccBaseUrl: String
        get() = ccBaseUrlState.value
        set(value) {
            val formatted = value.trimEnd('/')
            ccBaseUrlState.value = formatted
            kv.putString(MONEY_CC_BASE_URL_KEY, formatted)
        }

    private val ccLoginIdState = mutableStateOf(kv.getString(MONEY_CC_LOGIN_ID_KEY, ""))
    override var ccLoginId: String
        get() = ccLoginIdState.value
        set(value) {
            ccLoginIdState.value = value
            kv.putString(MONEY_CC_LOGIN_ID_KEY, value)
        }

    private val ccApiKeyState = mutableStateOf(kv.getString(MONEY_CC_API_KEY_KEY, ""))
    override var ccApiKey: String
        get() = ccApiKeyState.value
        set(value) {
            ccApiKeyState.value = value
            kv.putString(MONEY_CC_API_KEY_KEY, value)
        }

    override suspend fun saveUser(user: User) = upsert(TABLE_MONEY_USER, user.id, user)

    override suspend fun user(): User? = selectAll<User>(TABLE_MONEY_USER).firstOrNull()

    override suspend fun saveRecipient(recipient: Recipient) =
        upsert(TABLE_MONEY_RECIPIENT, recipient.id, recipient)

    override suspend fun recipients(): List<Recipient> =
        selectAll<Recipient>(TABLE_MONEY_RECIPIENT).sortedByDescending { it.createdAt }

    override suspend fun deleteRecipient(id: String) {
        sql("DELETE $TABLE_MONEY_RECIPIENT:`${id.substringAfter(':')}`;")
    }

    override suspend fun saveTransaction(transaction: Transaction) =
        upsert(TABLE_MONEY_TRANSACTION, transaction.id, transaction)

    override suspend fun transactions(): List<Transaction> =
        selectAll<Transaction>(TABLE_MONEY_TRANSACTION).sortedByDescending { it.createdAt }

    override suspend fun clearAll() {
        sql("DELETE $TABLE_MONEY_USER; DELETE $TABLE_MONEY_RECIPIENT; DELETE $TABLE_MONEY_TRANSACTION;")
    }

    private suspend inline fun <reified T> upsert(table: String, id: String, item: T) {
        val content = AppJson.encodeToJsonElement(item).toString()
        sql("UPSERT $table:`${id.substringAfter(':')}` CONTENT $content;")
    }

    private suspend inline fun <reified T> selectAll(table: String): List<T> {
        val response = sql("SELECT *, record::id(id) AS id FROM $table;") ?: return emptyList()
        return try {
            AppJson.decodeFromString<List<SurrealResponse<T>>>(response).firstOrNull()?.result.orEmpty()
        } catch (e: Exception) {
            log.e(e) { "could not parse $table: ${response.take(300)}" }
            emptyList()
        }
    }

    private suspend fun sql(query: String): String? = try {
        val response = client.post("$dbUrl/sql") {
            header("Surreal-NS", namespace)
            header("Surreal-DB", database)
            header("Accept", "application/json")
            contentType(ContentType.Text.Plain)
            setBody(query)
        }
        val body = response.bodyAsText()
        if (response.status.isSuccess()) {
            body
        } else {
            log.e { "HTTP ${response.status} for `$query`: ${body.take(300)}" }
            null
        }
    } catch (e: Exception) {
        log.e(e) { "request failed for `$query`" }
        null
    }

    private val mids = mutableMapOf<Currency, MutableMap<Currency, Long>>()

    override var isSeeded: Boolean = true
        private set

    init {
        SEED_RATES.forEach { seed ->
            mids.getOrPut(seed.base) { mutableMapOf() }[seed.quote] = seed.midE6
        }
    }

    override suspend fun refresh() {
        val bases = listOf(Currency.GBP, Currency.EUR, Currency.USD)
        var anyLive = false
        bases.forEach { base ->
            val fetched = fetchBase(base)
            if (fetched != null) {
                mids.getOrPut(base) { mutableMapOf() }.putAll(fetched)
                anyLive = true
            }
        }
        isSeeded = !anyLive
        if (isSeeded) log.w { "live feed unavailable — using the frozen seed snapshot" }
    }

    private suspend fun fetchBase(base: Currency): Map<Currency, Long>? = try {
        val response = client.get("$ratesEndpoint/${base.code}")
        if (!response.status.isSuccess()) {
            log.w { "HTTP ${response.status} fetching ${base.code}" }
            null
        } else {
            val body = response.body<ErApiResponse>()
            if (body.result != "success") {
                null
            } else {
                Currency.entries
                    .mapNotNull { quote ->
                        if (quote == base) return@mapNotNull null
                        val mid = body.rates[quote.code] ?: return@mapNotNull null
                        val e6 = (mid * RATE_SCALE).toLong()
                        if (e6 <= 0) null else quote to e6
                    }
                    .toMap()
            }
        }
    } catch (e: Exception) {
        log.w(e) { "fetch failed for ${base.code}" }
        null
    }

    override fun rate(sell: Currency, buy: Currency, markupBps: Int): FxRate? {
        if (sell == buy) return null
        val midE6 = mids[sell]?.get(buy) ?: seedMidE6(sell, buy) ?: return null
        return FxRate(
            sell = sell,
            buy = buy,
            coreRateE6 = midE6,
            markupBps = markupBps,
            asOf = Clock.System.now(),
        )
    }

    override fun tierRequiredFor(amount: Money, lifetimeSentMinor: Long): KycTier {
        require(amount.currency.exponent == Currency.GBP.exponent) {
            "KYC thresholds assume a 2dp send currency, got ${amount.currency.code}"
        }
        val single = amount.minor
        val cumulative = lifetimeSentMinor + single
        return when {
            single >= KycPolicy.docThresholdMinor -> KycTier.ID_VERIFIED
            cumulative >= KycPolicy.lifetimeThresholdMinor -> KycTier.ID_VERIFIED
            else -> KycTier.NAME_DOB
        }
    }

    override fun blockingTier(user: User?, amount: Money, lifetimeSentMinor: Long): KycTier? {
        val required = tierRequiredFor(amount, lifetimeSentMinor)
        val current = user?.kycTier ?: KycTier.ANONYMOUS
        return if (current.atLeast(required)) null else required
    }

    override fun reasonFor(tier: KycTier): String = when (tier) {
        KycTier.ANONYMOUS -> ""
        KycTier.PHONE_VERIFIED -> "We'll text you a code so we can save this recipient and keep your transfers together."
        KycTier.NAME_DOB -> "We need your legal name and date of birth before we can move money. Takes about 15 seconds."
        KycTier.ID_VERIFIED -> "Transfers this size need a quick photo ID check. You only do this once."
    }

    override suspend fun startVerification(user: User): String? = null

    override fun supports(bank: Bank): Boolean =
        !(bank.country == "KE" && bank.rail == Rail.MOBILE_MONEY)

    override suspend fun resolve(bank: Bank, accountNumber: String): ResolveState {
        delay(latencyMs)
        val digits = accountNumber.filter { it.isDigit() }
        if (digits.length < bank.accountLen) {
            return ResolveState.NotFound("Enter all ${bank.accountLen} digits")
        }
        if (digits.all { it == '0' }) {
            return ResolveState.NotFound("We couldn't find that account at ${bank.shortName}")
        }
        return ResolveState.Matched(nameFor(digits))
    }

    private fun nameFor(digits: String): String {
        val first = FIRST_NAMES[digits.take(3).toIntOrNull()?.mod(FIRST_NAMES.size) ?: 0]
        val middle = MIDDLE_NAMES[digits.drop(3).take(3).toIntOrNull()?.mod(MIDDLE_NAMES.size) ?: 0]
        val last = LAST_NAMES[digits.takeLast(4).toIntOrNull()?.mod(LAST_NAMES.size) ?: 0]
        return "$first $middle $last"
    }

    override val isSandbox: Boolean get() = isDemo || ccBaseUrl.contains("devapi")

    private val statusWalk = mutableMapOf<String, Int>()
    private val lifecycle = listOf("ready_to_send", "released", "submitted", "completed")

    private var authToken: String? = null

    @Serializable
    private data class AuthResponse(@SerialName("auth_token") val authToken: String = "")

    @Serializable
    private data class BeneficiaryResponse(val id: String = "")

    @Serializable
    private data class ConversionResponse(val id: String = "")

    @Serializable
    private data class PaymentResponse(
        val id: String = "",
        val status: String = "",
        @SerialName("short_reference") val shortReference: String = "",
    )

    @Serializable
    private data class FundingAccountsResponse(
        @SerialName("funding_accounts") val fundingAccounts: List<CcFundingAccount> = emptyList(),
    )

    @Serializable
    private data class CcFundingAccount(
        val id: String = "",
        @SerialName("account_number") val accountNumber: String = "",
        @SerialName("account_number_type") val accountNumberType: String = "",
        @SerialName("account_holder_name") val accountHolderName: String = "",
        @SerialName("routing_code") val routingCode: String = "",
        @SerialName("routing_code_type") val routingCodeType: String = "",
        @SerialName("bank_name") val bankName: String = "",
        @SerialName("bank_address") val bankAddress: String = "",
        val currency: String = "",
    )

    private suspend fun token(): String {
        authToken?.let { return it }
        require(ccLoginId.isNotBlank() && ccApiKey.isNotBlank()) {
            "Currencycloud credentials are not configured"
        }
        val response = client.submitForm(
            url = "$ccBaseUrl/v2/authenticate/api",
            formParameters = parameters {
                append("login_id", ccLoginId)
                append("api_key", ccApiKey)
            },
        )
        check(response.status.isSuccess()) { "authenticate failed: ${response.status} ${response.bodyAsText()}" }
        return response.body<AuthResponse>().authToken.also { authToken = it }
    }

    private suspend fun form(path: String, build: io.ktor.http.ParametersBuilder.() -> Unit): String {
        suspend fun attempt(): io.ktor.client.statement.HttpResponse {
            val authToken = token()
            return client.submitForm(
                url = "$ccBaseUrl$path",
                formParameters = parameters(build),
            ) { header("X-Auth-Token", authToken) }
        }

        var response = attempt()
        if (response.status == HttpStatusCode.Unauthorized) {
            authToken = null
            response = attempt()
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "$path failed: ${response.status} $body" }
        return body
    }

    private suspend fun getJson(path: String): String {
        suspend fun attempt(): io.ktor.client.statement.HttpResponse {
            val authToken = token()
            return client.get("$ccBaseUrl$path") { header("X-Auth-Token", authToken) }
        }
        var response = attempt()
        if (response.status == HttpStatusCode.Unauthorized) {
            authToken = null
            response = attempt()
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "$path failed: ${response.status} $body" }
        return body
    }

    override suspend fun fundingAccount(currency: Currency, reference: String): FundingAccount? {
        if (isDemo) {
            delay(latencyMs)
            return if (currency == Currency.EUR) {
                FundingAccount(
                    id = Uuid.random().toString(),
                    accountHolderName = "Femi Remittance Ltd",
                    accountNumber = "GB01TCCL06642902435207",
                    accountNumberType = "iban",
                    routingCode = "TCCLGB31",
                    routingCodeType = "bic_swift",
                    bankName = "The Currency Cloud Limited",
                    bankAddress = "12 Steward Street, London, E1 6FQ, GB",
                    currencyCode = currency.code,
                    reference = reference,
                )
            } else {
                FundingAccount(
                    id = Uuid.random().toString(),
                    accountHolderName = "Femi Remittance Ltd",
                    accountNumber = "42902435",
                    accountNumberType = "account_number",
                    routingCode = "12-34-56",
                    routingCodeType = "sort_code",
                    bankName = "The Currency Cloud Limited",
                    bankAddress = "12 Steward Street, London, E1 6FQ, GB",
                    currencyCode = currency.code,
                    reference = reference,
                )
            }
        }
        val body = getJson("/v2/funding_accounts/find?currency=${currency.code}&payment_type=regular")
        val account = AppJson.decodeFromString<FundingAccountsResponse>(body).fundingAccounts.firstOrNull()
            ?: run {
                log.w { "no funding account for ${currency.code} — is collections enabled?" }
                return null
            }
        return FundingAccount(
            id = account.id,
            accountHolderName = account.accountHolderName,
            accountNumber = account.accountNumber,
            accountNumberType = account.accountNumberType,
            routingCode = account.routingCode,
            routingCodeType = account.routingCodeType,
            bankName = account.bankName,
            bankAddress = account.bankAddress,
            currencyCode = account.currency,
            reference = reference,
        )
    }

    override suspend fun createBeneficiary(recipient: Recipient): String {
        if (isDemo) {
            delay(latencyMs)
            log.d { "beneficiaries/create ${recipient.displayName} @ ${recipient.bankName}" }
            return Uuid.random().toString()
        }
        val body = form("/v2/beneficiaries/create") {
            append("name", recipient.displayName)
            append("bank_account_holder_name", recipient.displayName)
            append("bank_country", recipient.country)
            append("currency", recipient.currencyCode)
            append("account_number", recipient.accountNumber)
            append("routing_code_type_1", "bank_code")
            append("routing_code_value_1", recipient.bankCode)
            append("beneficiary_entity_type", "individual")
            append("beneficiary_country", recipient.country)
            append("payment_types[]", if (recipient.rail == Rail.MOBILE_MONEY) "regular" else "regular")
        }
        return AppJson.decodeFromString<BeneficiaryResponse>(body).id
    }

    override suspend fun createConversion(
        sell: Money,
        buy: Money,
        fixedSide: FixedSide,
        uniqueRequestId: String,
    ): String {
        if (isDemo) {
            delay(latencyMs)
            log.d {
                "conversions/create sell=${sell.toWireString()}${sell.currency.code} " +
                    "buy=${buy.toWireString()}${buy.currency.code} fixed=${fixedSide.wire}"
            }
            return Uuid.random().toString()
        }
        val body = form("/v2/conversions/create") {
            append("buy_currency", buy.currency.code)
            append("sell_currency", sell.currency.code)
            append("fixed_side", fixedSide.wire)
            append("amount", if (fixedSide == FixedSide.SELL) sell.toWireString() else buy.toWireString())
            append("term_agreement", "true")
            append("unique_request_id", uniqueRequestId)
        }
        return AppJson.decodeFromString<ConversionResponse>(body).id
    }

    override suspend fun createPayment(
        beneficiaryId: String,
        conversionId: String,
        amount: Money,
        reason: String,
        reference: String,
        uniqueRequestId: String,
    ): PaymentResult {
        if (isDemo) {
            delay(latencyMs)
            val id = Uuid.random().toString()
            statusWalk[id] = 0
            log.d { "payments/create $reference ${amount.toWireString()} ${amount.currency.code}" }
            return PaymentResult(paymentId = id, status = lifecycle.first(), shortReference = reference)
        }
        val body = form("/v2/payments/create") {
            append("currency", amount.currency.code)
            append("beneficiary_id", beneficiaryId)
            append("conversion_id", conversionId)
            append("amount", amount.toWireString())
            append("reason", reason)
            append("reference", reference)
            append("payment_type", "regular")
            append("unique_request_id", uniqueRequestId)
        }
        val payment = AppJson.decodeFromString<PaymentResponse>(body)
        return PaymentResult(payment.id, payment.status, payment.shortReference)
    }

    override suspend fun paymentStatus(paymentId: String): String {
        if (isDemo) {
            delay(latencyMs)
            val step = (statusWalk[paymentId] ?: 0) + 1
            statusWalk[paymentId] = step
            return lifecycle[step.coerceAtMost(lifecycle.lastIndex)]
        }
        return AppJson.decodeFromString<PaymentResponse>(getJson("/v2/payments/$paymentId")).status
    }

    override suspend fun simulatePayin(receiverAccountNumber: String, amount: Money, reference: String) {
        if (isDemo) {
            delay(latencyMs)
            log.d { "demo/funding/create $reference ${amount.toWireString()}" }
            return
        }
        check(isSandbox) { "demo funding is not implemented in production" }
        form("/v2/demo/funding/create") {
            append("id", Uuid.random().toString())
            append("receiver_account_number", receiverAccountNumber)
            append("amount", amount.toWireString())
            append("currency", amount.currency.code)
            append("sender_reference", reference)
        }
    }

    private companion object {
        val FIRST_NAMES = listOf(
            "ADEBAYO", "CHIAMAKA", "OLUWASEUN", "NGOZI", "EMEKA", "FOLASADE",
            "IBRAHIM", "AMINA", "TUNDE", "BLESSING", "KWAME", "WANJIKU",
        )
        val MIDDLE_NAMES = listOf(
            "CHINEDU", "ADAEZE", "OLUMIDE", "KEHINDE", "NNAMDI", "TEMITOPE",
            "MUSA", "ZAINAB", "AKINYELE", "CHIOMA", "AKOSUA", "NJERI",
        )
        val LAST_NAMES = listOf(
            "OKAFOR", "ADEYEMI", "BALOGUN", "ENEBELI", "OGUNDIMU", "NWACHUKWU",
            "ABUBAKAR", "ELUEMUNOR", "OYELARAN", "UZOMA", "MENSAH", "KAMAU",
        )
    }
}

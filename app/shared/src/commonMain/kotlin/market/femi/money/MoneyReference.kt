package market.femi.money

enum class Rail(val label: String) {
    BANK("Bank account"),
    MOBILE_MONEY("Mobile money"),
}

data class Bank(
    val code: String,
    val name: String,
    val shortName: String,
    val country: String,
    val rail: Rail = Rail.BANK,
    val accountLen: Int = 10,
    val popular: Boolean = false,
)

val NG_BANKS: List<Bank> = listOf(
    Bank("044", "Access Bank", "Access", "NG", popular = true),
    Bank("058", "Guaranty Trust Bank", "GTBank", "NG", popular = true),
    Bank("057", "Zenith Bank", "Zenith", "NG", popular = true),
    Bank("011", "First Bank of Nigeria", "First Bank", "NG", popular = true),
    Bank("033", "United Bank for Africa", "UBA", "NG", popular = true),
    Bank("50211", "Kuda Bank", "Kuda", "NG", popular = true),
    Bank("999992", "OPay Digital Services", "OPay", "NG", popular = true),
    Bank("999991", "PalmPay", "PalmPay", "NG", popular = true),
    Bank("50515", "Moniepoint MFB", "Moniepoint", "NG", popular = true),
    Bank("070", "Fidelity Bank", "Fidelity", "NG"),
    Bank("032", "Union Bank of Nigeria", "Union Bank", "NG"),
    Bank("232", "Sterling Bank", "Sterling", "NG"),
    Bank("221", "Stanbic IBTC Bank", "Stanbic IBTC", "NG"),
    Bank("035", "Wema Bank", "Wema", "NG"),
    Bank("076", "Polaris Bank", "Polaris", "NG"),
    Bank("050", "Ecobank Nigeria", "Ecobank", "NG"),
    Bank("214", "First City Monument Bank", "FCMB", "NG"),
    Bank("082", "Keystone Bank", "Keystone", "NG"),
    Bank("215", "Unity Bank", "Unity", "NG"),
    Bank("101", "Providus Bank", "Providus", "NG"),
    Bank("301", "Jaiz Bank", "Jaiz", "NG"),
    Bank("51318", "FairMoney Microfinance Bank", "FairMoney", "NG"),
    Bank("100002", "Paga", "Paga", "NG"),
    Bank("120003", "MTN MoMo PSB", "MoMo PSB", "NG"),
    Bank("120004", "Airtel Smartcash PSB", "Smartcash", "NG"),
)

val KE_BANKS: List<Bank> = listOf(
    Bank("68", "Equity Bank Kenya", "Equity", "KE", accountLen = 12, popular = true),
    Bank("01", "Kenya Commercial Bank", "KCB", "KE", accountLen = 13, popular = true),
    Bank("11", "Co-operative Bank of Kenya", "Co-op Bank", "KE", accountLen = 12, popular = true),
    Bank("07", "NCBA Bank Kenya", "NCBA", "KE", accountLen = 12, popular = true),
    Bank("03", "Absa Bank Kenya", "Absa", "KE", accountLen = 10),
    Bank("31", "Stanbic Bank Kenya", "Stanbic", "KE", accountLen = 13),
    Bank("63", "Diamond Trust Bank", "DTB", "KE", accountLen = 13),
    Bank("70", "Family Bank", "Family Bank", "KE", accountLen = 12),
    Bank("57", "I&M Bank", "I&M", "KE", accountLen = 13),
    Bank("02", "Standard Chartered Bank Kenya", "StanChart", "KE", accountLen = 10),
    Bank("12", "National Bank of Kenya", "NBK", "KE", accountLen = 12),
    Bank("43", "Ecobank Kenya", "Ecobank", "KE", accountLen = 13),
)

val KE_WALLETS: List<Bank> = listOf(
    Bank("MPESA", "M-Pesa (Safaricom)", "M-Pesa", "KE", Rail.MOBILE_MONEY, accountLen = 12, popular = true),
    Bank("AIRTEL", "Airtel Money", "Airtel Money", "KE", Rail.MOBILE_MONEY, accountLen = 12),
)

val GH_WALLETS: List<Bank> = listOf(
    Bank("MTN", "MTN Mobile Money", "MTN MoMo", "GH", Rail.MOBILE_MONEY, accountLen = 10, popular = true),
    Bank("VOD", "Telecel Cash", "Telecel Cash", "GH", Rail.MOBILE_MONEY, accountLen = 10, popular = true),
    Bank("ATL", "AT Money", "AT Money", "GH", Rail.MOBILE_MONEY, accountLen = 10),
)

val GH_BANKS: List<Bank> = listOf(
    Bank("040100", "GCB Bank Limited", "GCB", "GH", accountLen = 13, popular = true),
    Bank("030100", "Absa Bank Ghana", "Absa", "GH", accountLen = 13, popular = true),
    Bank("130100", "Ecobank Ghana", "Ecobank", "GH", accountLen = 13, popular = true),
    Bank("240100", "Fidelity Bank Ghana", "Fidelity", "GH", accountLen = 13, popular = true),
    Bank("190100", "Stanbic Bank Ghana", "Stanbic", "GH", accountLen = 13),
    Bank("140100", "CalBank Limited", "CalBank", "GH", accountLen = 13),
    Bank("230100", "Guaranty Trust Bank Ghana", "GTBank GH", "GH", accountLen = 13),
    Bank("120100", "Zenith Bank Ghana", "Zenith GH", "GH", accountLen = 13),
    Bank("280100", "Access Bank Ghana", "Access GH", "GH", accountLen = 13),
)

val ALL_BANKS: List<Bank> = NG_BANKS + KE_BANKS + KE_WALLETS + GH_BANKS + GH_WALLETS

fun banksFor(country: String, rail: Rail): List<Bank> =
    ALL_BANKS.filter { it.country == country && it.rail == rail }

fun bankByCode(code: String): Bank? = ALL_BANKS.firstOrNull { it.code == code }

data class Country(
    val iso: String,
    val name: String,
    val flag: String,
    val currency: Currency,
    val dialCode: String,
    val rails: List<Rail>,
)

val SEND_COUNTRIES: List<Country> = listOf(
    Country("GB", "United Kingdom", "🇬🇧", Currency.GBP, "+44", listOf(Rail.BANK)),
    Country("IE", "Ireland", "🇮🇪", Currency.EUR, "+353", listOf(Rail.BANK)),
)

val RECEIVE_COUNTRIES: List<Country> = listOf(
    Country("NG", "Nigeria", "🇳🇬", Currency.NGN, "+234", listOf(Rail.BANK)),
    Country("KE", "Kenya", "🇰🇪", Currency.KES, "+254", listOf(Rail.MOBILE_MONEY, Rail.BANK)),
    Country("GH", "Ghana", "🇬🇭", Currency.GHS, "+233", listOf(Rail.MOBILE_MONEY, Rail.BANK)),
)

fun countryOf(iso: String): Country? =
    (SEND_COUNTRIES + RECEIVE_COUNTRIES).firstOrNull { it.iso == iso }

data class Corridor(
    val from: Currency,
    val to: Currency,
    val destination: String,
    val rail: Rail,
    val deliveryLabel: String,
    val minSendMinor: Long,
    val maxSendMinor: Long,
    val flatFeeMinor: Long,
    val markupBps: Int,
)

val CORRIDORS: List<Corridor> = listOf(
    Corridor(Currency.GBP, Currency.NGN, "NG", Rail.BANK, "Arrives in seconds", 100, 1_000_000, 0, 120),
    Corridor(Currency.EUR, Currency.NGN, "NG", Rail.BANK, "Arrives in seconds", 100, 1_000_000, 0, 130),
    Corridor(Currency.GBP, Currency.KES, "KE", Rail.MOBILE_MONEY, "Arrives in seconds", 100, 1_000_000, 99, 150),
    Corridor(Currency.GBP, Currency.KES, "KE", Rail.BANK, "Usually within the hour", 100, 1_000_000, 199, 150),
    Corridor(Currency.EUR, Currency.KES, "KE", Rail.MOBILE_MONEY, "Arrives in seconds", 100, 1_000_000, 99, 160),
    Corridor(Currency.GBP, Currency.GHS, "GH", Rail.MOBILE_MONEY, "Arrives in seconds", 100, 1_000_000, 99, 170),
    Corridor(Currency.GBP, Currency.GHS, "GH", Rail.BANK, "Same business day", 100, 1_000_000, 199, 170),
    Corridor(Currency.EUR, Currency.GHS, "GH", Rail.MOBILE_MONEY, "Arrives in seconds", 100, 1_000_000, 99, 180),
)

fun corridorFor(from: Currency, to: Currency, rail: Rail): Corridor? =
    CORRIDORS.firstOrNull { it.from == from && it.to == to && it.rail == rail }

fun defaultCorridor(from: Currency, to: Currency): Corridor? =
    CORRIDORS.firstOrNull { it.from == from && it.to == to }

data class PaymentPurpose(val id: String, val label: String)

val PAYMENT_PURPOSES: List<PaymentPurpose> = listOf(
    PaymentPurpose("family_support", "Family support"),
    PaymentPurpose("personal_transfer", "Personal transfer"),
    PaymentPurpose("education", "Education / school fees"),
    PaymentPurpose("medical", "Medical expenses"),
    PaymentPurpose("rent", "Rent or utilities"),
    PaymentPurpose("gift", "Gift"),
    PaymentPurpose("business", "Business payment"),
)

const val DEFAULT_PURPOSE_ID: String = "family_support"

fun purposeOf(id: String): PaymentPurpose =
    PAYMENT_PURPOSES.firstOrNull { it.id == id } ?: PAYMENT_PURPOSES.first()

fun normalisePhone(input: String, dialCode: String): String {
    val cc = dialCode.removePrefix("+")
    val digits = input.filter { it.isDigit() }
    return when {
        digits.isEmpty() -> ""
        digits.startsWith(cc) -> digits
        digits.startsWith("0") -> cc + digits.trimStart('0')
        else -> cc + digits
    }
}

fun phoneLooksPlausible(e164: String): Boolean = e164.length in 10..15

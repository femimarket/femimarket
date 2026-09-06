package market.femi.money

data class SeedRate(
    val base: Currency,
    val quote: Currency,
    val midE6: Long,
)

const val SEED_RATE_ASOF: String = "2026-07-25T00:02:32Z"
const val SEED_RATE_SOURCE: String = "open.er-api.com (mid-market snapshot)"
const val SEED_RATE_DISCLAIMER: String = "Indicative demo data — not a live rate and not a quote."

val SEED_RATES: List<SeedRate> = listOf(
    SeedRate(Currency.GBP, Currency.NGN, 1_807_720_000),
    SeedRate(Currency.GBP, Currency.KES, 172_850_000),
    SeedRate(Currency.GBP, Currency.GHS, 15_559_000),
    SeedRate(Currency.GBP, Currency.USD, 1_332_400),
    SeedRate(Currency.GBP, Currency.EUR, 1_171_000),
    SeedRate(Currency.EUR, Currency.NGN, 1_543_760_000),
    SeedRate(Currency.EUR, Currency.KES, 147_610_000),
    SeedRate(Currency.EUR, Currency.GHS, 13_287_000),
    SeedRate(Currency.USD, Currency.NGN, 1_356_780_000),
    SeedRate(Currency.USD, Currency.KES, 129_730_000),
    SeedRate(Currency.USD, Currency.GHS, 11_678_000),
)

fun seedMidE6(base: Currency, quote: Currency): Long? =
    SEED_RATES.firstOrNull { it.base == base && it.quote == quote }?.midE6

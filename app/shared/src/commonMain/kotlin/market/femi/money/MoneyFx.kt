package market.femi.money

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

const val RATE_SCALE: Long = 1_000_000L

const val INVERSE_RATE_SCALE: Long = 1_000_000_000L

const val BPS_SCALE: Long = 10_000L

enum class Rounding { HALF_UP, FLOOR, CEILING }

internal fun mulDiv(a: Long, b: Long, d: Long, rounding: Rounding): Long {
    require(d > 0) { "divisor must be positive" }
    require(d <= Long.MAX_VALUE / 2) { "divisor too large for tie detection" }
    val negative = (a < 0) != (b < 0)
    val ua = if (a < 0) -a else a
    val ub = if (b < 0) -b else b
    require(ua >= 0 && ub >= 0) { "Long.MIN_VALUE operand" }

    val q = ua / d
    val r = ua % d
    require(ub == 0L || q <= Long.MAX_VALUE / ub) { "overflow in mulDiv($a, $b, $d)" }
    val high = q * ub
    require(ub == 0L || r <= Long.MAX_VALUE / ub) { "overflow in mulDiv($a, $b, $d)" }
    val lowNumerator = r * ub
    val low = lowNumerator / d
    val remainder = lowNumerator % d

    var magnitude = high + low
    if (remainder != 0L) {
        when (rounding) {
            Rounding.HALF_UP -> if (remainder * 2 >= d) magnitude += 1
            Rounding.FLOOR -> Unit
            Rounding.CEILING -> magnitude += 1
        }
    }
    return if (negative) -magnitude else magnitude
}

data class FxRate(
    val sell: Currency,
    val buy: Currency,
    val coreRateE6: Long,
    val markupBps: Int,
    val asOf: Instant,
) {
    init {
        require(coreRateE6 > 0) { "core rate must be positive" }
        require(markupBps in 0..5_000) { "markup out of range" }
        require(sell != buy) { "same-currency rate" }
    }

    val clientRateE6: Long =
        mulDiv(coreRateE6, BPS_SCALE - markupBps, BPS_SCALE, Rounding.FLOOR)

    val inverseClientRateE9: Long =
        mulDiv(RATE_SCALE, INVERSE_RATE_SCALE, clientRateE6, Rounding.HALF_UP)
}

fun FxRate.convert(principal: Money, rounding: Rounding = Rounding.HALF_UP): Money {
    require(principal.currency == sell) {
        "cannot convert ${principal.currency.code} with a ${sell.code}->${buy.code} rate"
    }
    val expDelta = buy.exponent - sell.exponent
    var numerator = clientRateE6
    var denominator = RATE_SCALE
    if (expDelta >= 0) numerator *= POW10[expDelta] else denominator *= POW10[-expDelta]
    return Money.ofMinor(mulDiv(principal.minor, numerator, denominator, rounding), buy)
}

fun FxRate.principalFor(target: Money, rounding: Rounding = Rounding.CEILING): Money {
    require(target.currency == buy) { "target must be ${buy.code}" }
    val expDelta = buy.exponent - sell.exponent
    var numerator = clientRateE6
    var denominator = RATE_SCALE
    if (expDelta >= 0) numerator *= POW10[expDelta] else denominator *= POW10[-expDelta]
    return Money.ofMinor(mulDiv(target.minor, denominator, numerator, rounding), sell)
}

data class FeeSchedule(val flat: Money, val bps: Int) {
    init { require(bps in 0 until 10_000) { "fee bps out of range" } }

    fun onPrincipal(principal: Money): Money {
        require(principal.currency == flat.currency) { "fee currency mismatch" }
        if (bps == 0) return flat
        val base = principal.minor + flat.minor
        val gross = mulDiv(base, BPS_SCALE, BPS_SCALE - bps, Rounding.CEILING)
        return Money.ofMinor(gross - principal.minor, flat.currency)
    }

    fun onGross(gross: Money): Money {
        require(gross.currency == flat.currency) { "fee currency mismatch" }
        val variable = mulDiv(gross.minor, bps.toLong(), BPS_SCALE, Rounding.HALF_UP)
        return Money.ofMinor(flat.minor + variable, flat.currency)
    }
}

enum class FixedSide(val wire: String) {
    SELL("sell"),

    BUY("buy"),
}

data class Quote(
    val id: Uuid,
    val rate: FxRate,
    val fixedSide: FixedSide,
    val sendGross: Money,
    val fee: Money,
    val principal: Money,
    val receive: Money,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    val clientRateE6: Long get() = rate.clientRateE6
    val inverseClientRateE9: Long get() = rate.inverseClientRateE9
    val markupBps: Int get() = rate.markupBps

    init {
        require(sendGross == principal + fee) { "quote does not balance" }
    }

    fun isExpired(now: Instant = Clock.System.now()): Boolean = now >= expiresAt

    fun timeLeft(now: Instant = Clock.System.now()): Duration =
        (expiresAt - now).coerceAtLeast(Duration.ZERO)
}

object QuoteEngine {

    var ttl: Duration = 60.seconds

    fun quoteBySend(
        gross: Money,
        rate: FxRate,
        fees: FeeSchedule,
        now: Instant = Clock.System.now(),
    ): Quote {
        require(gross.currency == rate.sell)
        val fee = fees.onGross(gross)
        require(fee < gross) { "fee exceeds the amount sent" }
        val principal = gross - fee
        val receive = rate.convert(principal, Rounding.HALF_UP)
        return Quote(Uuid.random(), rate, FixedSide.SELL, gross, fee, principal, receive, now, now + ttl)
    }

    fun quoteByReceive(
        target: Money,
        rate: FxRate,
        fees: FeeSchedule,
        now: Instant = Clock.System.now(),
    ): Quote {
        require(target.currency == rate.buy)
        val principal = rate.principalFor(target, Rounding.CEILING)
        val fee = fees.onPrincipal(principal)
        val gross = principal + fee
        return Quote(Uuid.random(), rate, FixedSide.BUY, gross, fee, principal, target, now, now + ttl)
    }
}

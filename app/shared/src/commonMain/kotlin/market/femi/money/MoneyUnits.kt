package market.femi.money

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

enum class Currency(
    val code: String,
    val exponent: Int,
    val symbol: String,
    val symbolSpacer: String = "",
) {
    GBP("GBP", 2, "£"),
    EUR("EUR", 2, "€"),
    USD("USD", 2, "$"),
    NGN("NGN", 2, "₦"),
    KES("KES", 2, "KSh", " "),
    GHS("GHS", 2, "GH₵"),

    XOF("XOF", 0, "CFA", " "),
    XAF("XAF", 0, "FCFA", " "),
    UGX("UGX", 0, "USh", " "),
    RWF("RWF", 0, "FRw", " "),
    JPY("JPY", 0, "¥");

    val minorPerMajor: Long
        get() = POW10[exponent]

    companion object {
        fun ofCode(code: String): Currency? = entries.firstOrNull { it.code == code }
    }
}

internal val POW10 = longArrayOf(
    1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
    10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L,
    100_000_000_000L, 1_000_000_000_000L,
)

@Serializable
@JvmInline
value class MinorUnits(val raw: Long) {
    operator fun plus(other: MinorUnits) = MinorUnits(raw + other.raw)
    operator fun minus(other: MinorUnits) = MinorUnits(raw - other.raw)
    operator fun unaryMinus() = MinorUnits(-raw)
}

@Serializable
data class Money(val units: MinorUnits, val currency: Currency) : Comparable<Money> {

    val minor: Long get() = units.raw

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(units + other.units, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(units - other.units, currency)
    }

    operator fun unaryMinus(): Money = Money(-units, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minor.compareTo(other.minor)
    }

    val isZero: Boolean get() = minor == 0L
    val isPositive: Boolean get() = minor > 0L
    val isNegative: Boolean get() = minor < 0L

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "currency mismatch: ${currency.code} vs ${other.currency.code}"
        }
    }

    override fun toString(): String = "${format()} (${currency.code})"

    companion object {
        fun zero(currency: Currency) = Money(MinorUnits(0), currency)

        fun ofMinor(minor: Long, currency: Currency) = Money(MinorUnits(minor), currency)

        fun ofMajor(major: Long, fraction: Long = 0, currency: Currency): Money {
            require(fraction >= 0 && fraction < currency.minorPerMajor) {
                "fraction $fraction out of range for ${currency.code}"
            }
            val sign = if (major < 0) -1L else 1L
            return Money(MinorUnits(major * currency.minorPerMajor + sign * fraction), currency)
        }

        fun parse(text: String, currency: Currency): Money {
            val s = text.trim()
            require(s.isNotEmpty()) { "empty amount" }
            val negative = s.startsWith('-')
            val body = s.removePrefix("-").removePrefix("+")
            val dot = body.indexOf('.')
            val intPart = if (dot < 0) body else body.substring(0, dot)
            val fracPart = if (dot < 0) "" else body.substring(dot + 1)
            require(intPart.isNotEmpty() && intPart.all { it in '0'..'9' }) { "bad amount: $text" }
            require(fracPart.all { it in '0'..'9' }) { "bad amount: $text" }
            require(fracPart.length <= currency.exponent) {
                "$text has more precision than ${currency.code} (exponent ${currency.exponent})"
            }
            val padded = fracPart.padEnd(currency.exponent, '0')
            val digits = intPart + padded
            val magnitude = digits.toLong()
            return Money(MinorUnits(if (negative) -magnitude else magnitude), currency)
        }
    }
}

fun Money.toWireString(): String = renderDigits(groupThousands = false)

fun Money.format(
    withSymbol: Boolean = true,
    grouping: Boolean = true,
    withCode: Boolean = false,
): String {
    val digits = renderDigits(groupThousands = grouping)
    val negative = minor < 0
    val body = if (negative) digits.substring(1) else digits
    val sb = StringBuilder()
    if (negative) sb.append('-')
    if (withSymbol) sb.append(currency.symbol).append(currency.symbolSpacer)
    sb.append(body)
    if (withCode) sb.append(' ').append(currency.code)
    return sb.toString()
}

private fun Money.renderDigits(groupThousands: Boolean): String {
    val raw = minor.toString()
    val negative = raw.startsWith('-')
    val magnitude = if (negative) raw.substring(1) else raw
    val exp = currency.exponent
    val padded = if (magnitude.length <= exp) magnitude.padStart(exp + 1, '0') else magnitude
    val intPart = padded.substring(0, padded.length - exp)
    val fracPart = if (exp == 0) "" else padded.substring(padded.length - exp)
    val shownInt = if (groupThousands) group3(intPart) else intPart
    val sb = StringBuilder()
    if (negative) sb.append('-')
    sb.append(shownInt)
    if (exp > 0) sb.append('.').append(fracPart)
    return sb.toString()
}

private fun group3(s: String): String {
    if (s.length <= 3) return s
    val sb = StringBuilder(s.length + s.length / 3)
    val lead = s.length % 3
    if (lead != 0) sb.append(s.substring(0, lead))
    var i = lead
    while (i < s.length) {
        if (sb.isNotEmpty()) sb.append(',')
        sb.append(s.substring(i, i + 3))
        i += 3
    }
    return sb.toString()
}

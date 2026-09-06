package market.femi.money

import kotlinx.serialization.Serializable

enum class KycTier {
    ANONYMOUS,

    PHONE_VERIFIED,

    NAME_DOB,

    ID_VERIFIED,
    ;

    fun atLeast(other: KycTier): Boolean = ordinal >= other.ordinal
}

object KycPolicy {
    const val docThresholdMinor: Long = 80_000L

    const val lifetimeThresholdMinor: Long = 200_000L
}

@Serializable
data class User(
    val id: String,
    val phoneE164: String,
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "",
    val kycTier: KycTier = KycTier.ANONYMOUS,
    val lifetimeSentMinor: Long = 0,
    val createdAt: String = "",
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { "?" }
}

@Serializable
data class Recipient(
    val id: String,
    val displayName: String,
    val country: String,
    val rail: Rail,
    val bankCode: String,
    val bankName: String,
    val accountNumber: String,
    val currencyCode: String,
    val createdAt: String = "",
) {
    val initials: String get() = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

    val maskedAccount: String get() = if (accountNumber.length <= 4) accountNumber else "••••${accountNumber.takeLast(4)}"
}

enum class TxStatus(val label: String) {
    PROCESSING("Processing"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
}

@Serializable
data class TxEvent(
    val label: String,
    val at: String = "",
    val done: Boolean = false,
)

@Serializable
data class Transaction(
    val id: String,
    val uniqueRequestId: String,
    val recipientId: String,
    val recipientName: String,
    val bankName: String,
    val maskedAccount: String,
    val sendGross: Money,
    val fee: Money,
    val receive: Money,
    val rateE6: Long,
    val purposeId: String,
    val reference: String,
    val status: TxStatus = TxStatus.PROCESSING,
    val timeline: List<TxEvent> = emptyList(),
    val createdAt: String = "",
    val providerPaymentId: String = "",
    val failureReason: String = "",
)

@Serializable
data class FundingAccount(
    val id: String,
    val accountHolderName: String,
    val accountNumber: String,
    val accountNumberType: String,
    val routingCode: String,
    val routingCodeType: String,
    val bankName: String,
    val bankAddress: String = "",
    val currencyCode: String,
    val reference: String = "",
) {
    val accountNumberLabel: String get() = if (accountNumberType == "iban") "IBAN" else "Account number"
    val routingCodeLabel: String
        get() = when (routingCodeType) {
            "sort_code" -> "Sort code"
            "bic_swift" -> "BIC / SWIFT"
            "ach_routing_number" -> "ACH routing number"
            "wire_routing_number" -> "Wire routing number"
            else -> "Routing code"
        }
}

enum class PayMethod(val label: String, val blurb: String) {
    OPEN_BANKING("Pay by bank", "Approve in your banking app — no card details"),
    BANK_TRANSFER("Bank transfer", "Push to your account number and reference"),
    CARD("Debit card", "Instant, small card fee"),
}

sealed interface ResolveState {
    data object Idle : ResolveState
    data object Resolving : ResolveState
    data class Matched(val name: String) : ResolveState
    data class NotFound(val message: String) : ResolveState
}

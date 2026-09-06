package market.femi.money

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class Account {
    CUSTOMER_WALLET,
    PAYIN_CLEARING,
    FX_POSITION,
    PAYOUT_PAYABLE,
    FEE_REVENUE,
    SPREAD_REVENUE,
}

data class LedgerEntry(
    val id: Uuid,
    val transferId: Uuid,
    val at: Instant,
    val account: Account,
    val amount: Money,
    val memo: String,
)

enum class TransferState {
    QUOTED,
    AWAITING_PAYIN,
    FUNDED,
    CONVERTED,
    PAID_OUT,
    FAILED,
    EXPIRED,
    ;

    fun canMoveTo(next: TransferState): Boolean = next in when (this) {
        QUOTED -> setOf(AWAITING_PAYIN, EXPIRED, FAILED)
        AWAITING_PAYIN -> setOf(FUNDED, EXPIRED, FAILED)
        FUNDED -> setOf(CONVERTED, FAILED)
        CONVERTED -> setOf(PAID_OUT, FAILED)
        PAID_OUT, FAILED, EXPIRED -> emptySet()
    }
}

data class Transfer(
    val id: Uuid,
    val quote: Quote,
    val state: TransferState,
    val beneficiaryName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PayinCredited(
    val reference: String,
    val amount: Money,
    val at: Instant,
)

class Ledger(private val clock: Clock = Clock.System) {

    private val _entries = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val entries: StateFlow<List<LedgerEntry>> = _entries.asStateFlow()

    private val _payins = MutableSharedFlow<PayinCredited>(replay = 8)
    val payins: SharedFlow<PayinCredited> = _payins.asSharedFlow()

    fun balance(account: Account, currency: Currency): Money =
        _entries.value
            .filter { it.account == account && it.amount.currency == currency }
            .fold(Money.zero(currency)) { acc, e -> acc + e.amount }

    fun post(transferId: Uuid, memo: String, legs: List<Pair<Account, Money>>) {
        val byCurrency = legs.groupBy { it.second.currency }
        byCurrency.forEach { (currency, group) ->
            val sum = group.fold(Money.zero(currency)) { acc, l -> acc + l.second }
            require(sum.isZero) { "unbalanced posting in ${currency.code}: ${sum.format()}" }
        }
        val now = clock.now()
        _entries.value = _entries.value + legs.map { (account, amount) ->
            LedgerEntry(Uuid.random(), transferId, now, account, amount, memo)
        }
    }

    suspend fun creditPayin(transferId: Uuid, amount: Money, reference: String) {
        post(
            transferId, "payin $reference",
            listOf(
                Account.CUSTOMER_WALLET to amount,
                Account.PAYIN_CLEARING to -amount,
            ),
        )
        _payins.emit(PayinCredited(reference, amount, clock.now()))
    }

    fun bookSend(transfer: Transfer) {
        val q = transfer.quote
        val coreValue = Money.ofMinor(
            mulDiv(q.principal.minor, q.rate.coreRateE6, RATE_SCALE, Rounding.FLOOR),
            q.rate.buy,
        )
        val spread = coreValue - q.receive
        post(
            transfer.id, "send ${q.id}",
            listOf(
                Account.CUSTOMER_WALLET to -q.sendGross,
                Account.FEE_REVENUE to q.fee,
                Account.FX_POSITION to q.principal,
                Account.FX_POSITION to -coreValue,
                Account.PAYOUT_PAYABLE to q.receive,
                Account.SPREAD_REVENUE to spread,
            ),
        )
    }

    fun isBalanced(): Boolean = _entries.value
        .groupBy { it.amount.currency }
        .all { (currency, group) ->
            group.fold(Money.zero(currency)) { acc, e -> acc + e.amount }.isZero
        }
}

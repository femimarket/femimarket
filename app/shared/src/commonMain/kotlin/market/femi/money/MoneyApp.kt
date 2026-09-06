package market.femi.money

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import market.femi.State
import market.femi.services.TERMINAL_FAILURE
import market.femi.services.TERMINAL_SUCCESS
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class MoneyApp(private val state: State) {

    val ledger = Ledger()

    var user by mutableStateOf<User?>(null)
        private set
    val recipients = mutableStateListOf<Recipient>()
    val transactions = mutableStateListOf<Transaction>()

    val send = ActiveSendState()
    val recipientForm = ActiveRecipientState()
    val identity = ActiveIdentityState()

    var ratesLoaded by mutableStateOf(false)
        private set
    var tickerLine by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var toast by mutableStateOf<String?>(null)

    val isDemoData: Boolean get() = state.money.isSandbox || state.money.isSeeded || state.money.isDemo

    val sendCountries: List<Country> = SEND_COUNTRIES
    val receiveCountries: List<Country> = RECEIVE_COUNTRIES

    fun start() = state.scope.launch {
        busy = true
        try {
            state.money.refresh()
            ratesLoaded = true
            refreshTicker()
            user = state.money.user()
            recipients.clear()
            recipients.addAll(state.money.recipients())
            transactions.clear()
            transactions.addAll(state.money.transactions())
        } catch (e: Exception) {
            state.log.e(e) { "[start]" }
        } finally {
            busy = false
        }
    }

    private fun refreshTicker() {
        val corridor = send.corridor
        val rate = rateFor(corridor) ?: return
        val oneUnit = Money.ofMajor(1, currency = corridor.from)
        val converted = Money.ofMinor(
            rate.clientRateE6 * oneUnit.minor / RATE_SCALE * powTen(corridor.to.exponent - corridor.from.exponent),
            corridor.to,
        )
        tickerLine = "1 ${corridor.from.code} = ${converted.format()}"
    }

    private fun powTen(exp: Int): Long {
        var result = 1L
        repeat(if (exp < 0) 0 else exp) { result *= 10 }
        return result
    }

    fun rateFor(corridor: Corridor): FxRate? = state.money.rate(corridor.from, corridor.to, corridor.markupBps)

    fun dismissToast() { toast = null }

    fun startSend(corridor: Corridor = send.corridor) {
        send.reset(corridor)
        recipientForm.reset(corridor)
        state.nav.openMoneyAmount()
    }

    fun setCorridor(from: Currency, to: Currency, rail: Rail) {
        val corridor = corridorFor(from, to, rail) ?: return
        send.corridor = corridor
        recipientForm.reset(corridor)
        refreshTicker()
        if (send.fixedSide == FixedSide.BUY) setReceiveAmountText(send.receiveText)
        else setSendAmountText(send.sendText)
    }

    fun setSendAmountText(text: String) {
        send.sendText = sanitiseAmount(text, send.corridor.from)
        send.fixedSide = FixedSide.SELL
        val rate = rateFor(send.corridor)
        val amount = parseOrNull(send.sendText, send.corridor.from)
        if (rate == null || amount == null || !amount.isPositive) {
            send.clearQuote()
            send.receiveText = ""
            return
        }
        applyQuote(runCatching { QuoteEngine.quoteBySend(amount, rate, feesFor(send.corridor)) }.getOrNull())
    }

    fun setReceiveAmountText(text: String) {
        send.receiveText = sanitiseAmount(text, send.corridor.to)
        send.fixedSide = FixedSide.BUY
        val rate = rateFor(send.corridor)
        val target = parseOrNull(send.receiveText, send.corridor.to)
        if (rate == null || target == null || !target.isPositive) {
            send.clearQuote()
            send.sendText = ""
            return
        }
        applyQuote(runCatching { QuoteEngine.quoteByReceive(target, rate, feesFor(send.corridor)) }.getOrNull())
    }

    private fun applyQuote(quote: Quote?) {
        if (quote == null) {
            send.quoteError = "We can't price that amount right now"
            return
        }
        send.rate = quote.rate
        send.sendGross = quote.sendGross
        send.fee = quote.fee
        send.principal = quote.principal
        send.receive = quote.receive
        send.etaLabel = send.corridor.deliveryLabel
        if (send.fixedSide == FixedSide.SELL) {
            send.receiveText = quote.receive.format(withSymbol = false, grouping = false)
        } else {
            send.sendText = quote.sendGross.format(withSymbol = false, grouping = false)
        }
        send.quoteError = validateLimits(quote.sendGross)
    }

    private fun validateLimits(gross: Money): String? {
        val corridor = send.corridor
        return when {
            gross.minor < corridor.minSendMinor ->
                "Minimum is ${Money.ofMinor(corridor.minSendMinor, corridor.from).format()}"
            gross.minor > corridor.maxSendMinor ->
                "Maximum is ${Money.ofMinor(corridor.maxSendMinor, corridor.from).format()}"
            else -> null
        }
    }

    private fun feesFor(corridor: Corridor) =
        FeeSchedule(Money.ofMinor(corridor.flatFeeMinor, corridor.from), bps = 0)

    private fun sanitiseAmount(text: String, currency: Currency): String {
        val filtered = buildString {
            var seenDot = false
            text.forEach { ch ->
                when {
                    ch.isDigit() -> append(ch)
                    (ch == '.' || ch == ',') && !seenDot && currency.exponent > 0 -> {
                        seenDot = true
                        append('.')
                    }
                }
            }
        }
        val dot = filtered.indexOf('.')
        if (dot < 0) return filtered
        return filtered.substring(0, minOf(filtered.length, dot + 1 + currency.exponent))
    }

    private fun parseOrNull(text: String, currency: Currency): Money? =
        if (text.isBlank() || text == ".") null else runCatching { Money.parse(text, currency) }.getOrNull()

    fun continueFromAmount() {
        if (send.quoteError != null || !send.sendGross.isPositive) return
        state.nav.openMoneyRecipient()
    }

    val canContinueFromAmount: Boolean
        get() = send.quoteError == null && send.sendGross.isPositive && send.receive.isPositive

    fun setRecipientRail(rail: Rail) {
        val corridor = corridorFor(send.corridor.from, send.corridor.to, rail) ?: return
        send.corridor = corridor
        recipientForm.reset(corridor)
        if (send.fixedSide == FixedSide.SELL) setSendAmountText(send.sendText)
        else setReceiveAmountText(send.receiveText)
    }

    fun banksForForm(): List<Bank> = banksFor(send.corridor.destination, send.corridor.rail)

    fun selectBank(code: String) {
        recipientForm.bank = banksForForm().firstOrNull { it.code == code } ?: return
        recipientForm.bankQuery = ""
        recipientForm.resolveState = ResolveState.Idle
        maybeResolve()
    }

    fun setAccountNumber(text: String) {
        val bank = recipientForm.bank
        val digits = text.filter { it.isDigit() }
        recipientForm.accountNumber = if (bank != null) digits.take(bank.accountLen) else digits
        recipientForm.resolveState = ResolveState.Idle
        maybeResolve()
    }

    private fun maybeResolve() {
        val bank = recipientForm.bank ?: return
        if (recipientForm.accountNumber.length < bank.accountLen) return
        if (!state.money.supports(bank)) return
        resolveName()
    }

    fun resolveName() = state.scope.launch {
        val bank = recipientForm.bank ?: return@launch
        val number = recipientForm.accountNumber
        recipientForm.resolveState = ResolveState.Resolving
        recipientForm.resolveState = try {
            state.money.resolve(bank, number)
        } catch (e: Exception) {
            state.log.e(e) { "[resolveName]" }
            ResolveState.NotFound("We couldn't check that name right now")
        }
    }

    val canConfirmRecipient: Boolean
        get() {
            val bank = recipientForm.bank ?: return false
            if (recipientForm.accountNumber.length < bank.accountLen) return false
            if (recipientForm.resolveState is ResolveState.Resolving) return false
            return resolvedRecipientName().isNotBlank()
        }

    private fun resolvedRecipientName(): String =
        (recipientForm.resolveState as? ResolveState.Matched)?.name?.takeIf { it.isNotBlank() }
            ?: recipientForm.typedName.trim()

    fun confirmRecipient() = state.scope.launch {
        val bank = recipientForm.bank ?: return@launch
        val name = resolvedRecipientName()
        if (name.isBlank()) return@launch
        busy = true
        try {
            val recipient = Recipient(
                id = Uuid.random().toString(),
                displayName = name,
                country = send.corridor.destination,
                rail = send.corridor.rail,
                bankCode = bank.code,
                bankName = bank.name,
                accountNumber = recipientForm.accountNumber,
                currencyCode = send.corridor.to.code,
                createdAt = Clock.System.now().toString(),
            )
            state.money.saveRecipient(recipient)
            recipients.removeAll { it.id == recipient.id }
            recipients.add(0, recipient)
            send.recipient = recipient
            if (user == null) state.nav.openMoneyPhone() else state.nav.openMoneyPay()
        } catch (e: Exception) {
            state.log.e(e) { "[confirmRecipient]" }
            toast = "We couldn't save that recipient"
        } finally {
            busy = false
        }
    }

    fun useRecipient(id: String) {
        val recipient = recipients.firstOrNull { it.id == id } ?: return
        send.recipient = recipient
        if (user == null) state.nav.openMoneyPhone() else state.nav.openMoneyPay()
    }

    fun setPhone(raw: String) {
        identity.phoneRaw = raw
        identity.error = null
    }

    val phoneE164: String get() = normalisePhone(identity.phoneRaw, identity.phoneCountry.dialCode)
    val phoneLooksValid: Boolean get() = phoneLooksPlausible(phoneE164)
    val otpSent: Boolean get() = identity.otpSent

    fun requestOtp() = state.scope.launch {
        if (!phoneLooksValid) {
            identity.error = "That number doesn't look right"
            return@launch
        }
        identity.otpSent = true
        identity.error = null
        debugOtp = (100000..999999).random().toString()
        toast = "Demo code: $debugOtp"
    }

    var debugOtp by mutableStateOf("")
        private set

    fun setOtp(text: String) {
        identity.otp = text.filter { it.isDigit() }.take(6)
        identity.error = null
    }

    fun verifyOtp() = state.scope.launch {
        if (identity.otp != debugOtp) {
            identity.error = "That code doesn't match"
            return@launch
        }
        busy = true
        try {
            val existing = user
            val updated = existing?.copy(phoneE164 = phoneE164) ?: User(
                id = Uuid.random().toString(),
                phoneE164 = phoneE164,
                kycTier = KycTier.PHONE_VERIFIED,
                createdAt = Clock.System.now().toString(),
            )
            persistUser(updated)
            if (updated.kycTier.atLeast(KycTier.NAME_DOB)) state.nav.openMoneyPay() else state.nav.openMoneyIdentity()
        } catch (e: Exception) {
            state.log.e(e) { "[verifyOtp]" }
            identity.error = "Something went wrong. Try again."
        } finally {
            busy = false
        }
    }

    fun setFirstName(text: String) { identity.firstName = text }
    fun setLastName(text: String) { identity.lastName = text }
    fun setDob(text: String) { identity.dob = text }

    val canSubmitIdentity: Boolean
        get() = identity.firstName.isNotBlank() && identity.lastName.isNotBlank() && identity.dob.length == 10

    fun submitIdentity() = state.scope.launch {
        val current = user ?: return@launch
        if (!canSubmitIdentity) {
            identity.error = "We need your name and date of birth"
            return@launch
        }
        busy = true
        try {
            persistUser(
                current.copy(
                    firstName = identity.firstName.trim(),
                    lastName = identity.lastName.trim(),
                    dob = identity.dob,
                    kycTier = maxOf(current.kycTier, KycTier.NAME_DOB),
                ),
            )
            state.nav.openMoneyPay()
        } catch (e: Exception) {
            state.log.e(e) { "[submitIdentity]" }
            identity.error = "Something went wrong. Try again."
        } finally {
            busy = false
        }
    }

    fun startIdVerification() = state.scope.launch {
        val current = user ?: return@launch
        busy = true
        try {
            val url = state.money.startVerification(current)
            if (url != null) {
                kycVerificationUrl = url
                return@launch
            }
            persistUser(current.copy(kycTier = KycTier.ID_VERIFIED))
            toast = "ID verified"
            state.nav.openMoneyPay()
        } catch (e: Exception) {
            state.log.e(e) { "[startIdVerification]" }
            toast = "We couldn't start that check"
        } finally {
            busy = false
        }
    }

    var kycVerificationUrl by mutableStateOf<String?>(null)

    private suspend fun persistUser(updated: User) {
        state.money.saveUser(updated)
        user = updated
    }

    fun blockingKycTier(): KycTier? =
        state.money.blockingTier(user, send.sendGross, user?.lifetimeSentMinor ?: 0)

    fun selectPayMethod(method: PayMethod) { send.payMethod = method }

    fun continueFromPay() = state.scope.launch {
        val rate = send.rate ?: return@launch
        send.lockedRate = rate
        send.lockExpiresAt = Clock.System.now() + LOCK_DURATION
        if (send.payMethod == PayMethod.BANK_TRANSFER) {
            send.fundingAccount = runCatching {
                state.money.fundingAccount(send.corridor.from, fundingReference())
            }.getOrNull()
        }
        state.nav.openMoneyReview()
    }

    private fun fundingReference(): String = "FEMI-${Uuid.random().toString().take(4).uppercase()}"

    fun refreshLock() = state.scope.launch {
        state.money.refresh()
        refreshTicker()
        if (send.fixedSide == FixedSide.SELL) setSendAmountText(send.sendText)
        else setReceiveAmountText(send.receiveText)
        send.lockedRate = send.rate
        send.lockExpiresAt = Clock.System.now() + LOCK_DURATION
    }

    val lockSecondsLeft: Int
        get() = send.lockExpiresAt
            ?.let { (it - Clock.System.now()).inWholeSeconds.coerceAtLeast(0).toInt() }
            ?: 0

    val lockExpired: Boolean get() = send.lockExpiresAt?.let { Clock.System.now() >= it } ?: false

    fun confirmSend() = state.scope.launch { confirmSendNow() }

    suspend fun confirmSendNow() {
        val recipient = send.recipient ?: return
        if (blockingKycTier() != null) {
            if (user == null) state.nav.openMoneyPhone() else state.nav.openMoneyIdentity()
            return
        }
        if (send.sending) return
        send.sending = true
        val uniqueRequestId = Uuid.random().toString()
        val reference = fundingReference()
        try {
            val beneficiaryId = state.money.createBeneficiary(recipient)
            val conversionId = state.money.createConversion(
                sell = send.principal,
                buy = send.receive,
                fixedSide = send.fixedSide,
                uniqueRequestId = uniqueRequestId,
            )
            val purpose = purposeOf(send.purposeId)
            val result = state.money.createPayment(
                beneficiaryId = beneficiaryId,
                conversionId = conversionId,
                amount = send.receive,
                reason = purpose.label,
                reference = reference,
                uniqueRequestId = uniqueRequestId,
            )

            val transferId = Uuid.random()
            ledger.creditPayin(transferId, send.sendGross, reference)
            val quote = QuoteEngine.quoteBySend(send.sendGross, send.lockedRate ?: send.rate!!, feesFor(send.corridor))
            ledger.bookSend(
                Transfer(
                    id = transferId,
                    quote = quote.copy(receive = send.receive, principal = send.principal, fee = send.fee),
                    state = TransferState.CONVERTED,
                    beneficiaryName = recipient.displayName,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
            )

            val now = Clock.System.now().toString()
            val transaction = Transaction(
                id = Uuid.random().toString(),
                uniqueRequestId = uniqueRequestId,
                recipientId = recipient.id,
                recipientName = recipient.displayName,
                bankName = recipient.bankName,
                maskedAccount = recipient.maskedAccount,
                sendGross = send.sendGross,
                fee = send.fee,
                receive = send.receive,
                rateE6 = (send.lockedRate ?: send.rate)?.clientRateE6 ?: 0,
                purposeId = send.purposeId,
                reference = reference,
                status = TxStatus.PROCESSING,
                timeline = listOf(TxEvent("Payment received", now, done = true)),
                createdAt = now,
                providerPaymentId = result.paymentId,
            )
            state.money.saveTransaction(transaction)
            transactions.add(0, transaction)
            user?.let {
                persistUser(it.copy(lifetimeSentMinor = it.lifetimeSentMinor + send.sendGross.minor))
            }
            state.nav.openMoneyReceipt()
        } catch (e: Exception) {
            state.log.e(e) { "[confirmSend]" }
            toast = "We couldn't complete that transfer"
        } finally {
            send.sending = false
        }
    }

    suspend fun pollUntilSettled(maxPolls: Int = 12) {
        val index = transactions.indexOfFirst { it.status == TxStatus.PROCESSING }
        if (index < 0) return
        var transaction = transactions[index]
        repeat(maxPolls) {
            val providerStatus = runCatching { state.money.paymentStatus(transaction.providerPaymentId) }
                .getOrElse { return }
            transaction = transaction.copy(timeline = timelineFor(providerStatus, transaction))
            when (providerStatus) {
                in TERMINAL_SUCCESS -> {
                    transaction = transaction.copy(status = TxStatus.DELIVERED)
                    transactions[index] = transaction
                    state.money.saveTransaction(transaction)
                    return
                }
                in TERMINAL_FAILURE -> {
                    transaction = transaction.copy(status = TxStatus.FAILED, failureReason = providerStatus)
                    transactions[index] = transaction
                    state.money.saveTransaction(transaction)
                    return
                }
                else -> transactions[index] = transaction
            }
        }
        state.money.saveTransaction(transaction)
    }

    private fun timelineFor(providerStatus: String, transaction: Transaction): List<TxEvent> {
        val reached = when (providerStatus) {
            "ready_to_send" -> 1
            "released" -> 2
            "submitted" -> 3
            "completed" -> 4
            else -> 1
        }
        val now = Clock.System.now().toString()
        val labels = listOf(
            "Payment received",
            "Converting to ${transaction.receive.currency.code}",
            "Sent to ${transaction.bankName}",
            "Delivered",
        )
        return labels.mapIndexed { i, label ->
            val done = i < reached
            val existing = transaction.timeline.getOrNull(i)
            TxEvent(label, existing?.at?.takeIf { it.isNotBlank() && existing.done } ?: if (done) now else "", done)
        }
    }

    fun startRepeatSend(recipientId: String) = state.scope.launch {
        val recipient = recipients.firstOrNull { it.id == recipientId } ?: return@launch
        val last = transactions.firstOrNull { it.recipientId == recipientId }
        val corridor = CORRIDORS.firstOrNull {
            it.destination == recipient.country && it.rail == recipient.rail && it.from == send.corridor.from
        } ?: send.corridor
        send.reset(corridor)
        send.recipient = recipient
        setSendAmountText(
            last?.sendGross?.format(withSymbol = false, grouping = false)
                ?: Money.ofMajor(100, currency = corridor.from).format(withSymbol = false, grouping = false),
        )
        send.lockedRate = send.rate
        send.lockExpiresAt = Clock.System.now() + LOCK_DURATION
        state.nav.openMoneyReview()
    }

    fun setPurpose(id: String) { send.purposeId = id }

    companion object {
        val LOCK_DURATION = 600.seconds
    }
}

class ActiveSendState {
    var corridor by mutableStateOf(CORRIDORS.first())
    var fixedSide by mutableStateOf(FixedSide.SELL)
    var sendText by mutableStateOf("")
    var receiveText by mutableStateOf("")
    var sendGross by mutableStateOf(Money.zero(Currency.GBP))
    var fee by mutableStateOf(Money.zero(Currency.GBP))
    var principal by mutableStateOf(Money.zero(Currency.GBP))
    var receive by mutableStateOf(Money.zero(Currency.NGN))
    var rate by mutableStateOf<FxRate?>(null)
    var lockedRate by mutableStateOf<FxRate?>(null)
    var lockExpiresAt by mutableStateOf<kotlin.time.Instant?>(null)
    var quoteError by mutableStateOf<String?>(null)
    var recipient by mutableStateOf<Recipient?>(null)
    var purposeId by mutableStateOf(DEFAULT_PURPOSE_ID)
    var payMethod by mutableStateOf(PayMethod.OPEN_BANKING)
    var fundingAccount by mutableStateOf<FundingAccount?>(null)
    var etaLabel by mutableStateOf("")
    var sending by mutableStateOf(false)

    fun reset(next: Corridor) {
        corridor = next
        fixedSide = FixedSide.SELL
        sendText = ""
        receiveText = ""
        sendGross = Money.zero(next.from)
        fee = Money.zero(next.from)
        principal = Money.zero(next.from)
        receive = Money.zero(next.to)
        rate = null
        lockedRate = null
        lockExpiresAt = null
        quoteError = null
        recipient = null
        purposeId = DEFAULT_PURPOSE_ID
        fundingAccount = null
        etaLabel = next.deliveryLabel
        sending = false
    }

    fun clearQuote() {
        sendGross = Money.zero(corridor.from)
        fee = Money.zero(corridor.from)
        principal = Money.zero(corridor.from)
        receive = Money.zero(corridor.to)
        quoteError = null
    }
}

class ActiveRecipientState {
    var country by mutableStateOf<Country?>(countryOf("NG"))
    var bank by mutableStateOf<Bank?>(null)
    var bankQuery by mutableStateOf("")
    var accountNumber by mutableStateOf("")
    var typedName by mutableStateOf("")
    var resolveState by mutableStateOf<ResolveState>(ResolveState.Idle)

    fun reset(corridor: Corridor) {
        country = countryOf(corridor.destination)
        bank = null
        bankQuery = ""
        accountNumber = ""
        typedName = ""
        resolveState = ResolveState.Idle
    }
}

class ActiveIdentityState {
    var phoneCountry by mutableStateOf(SEND_COUNTRIES.first())
    var phoneRaw by mutableStateOf("")
    var otp by mutableStateOf("")
    var otpSent by mutableStateOf(false)
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var dob by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
}

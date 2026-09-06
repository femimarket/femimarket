package market.femi

fun Number.toHoursMinuteSeconds(): String {
    // Whole + fractional hours, then peel minutes and seconds off the fraction (float math mirrors
    // the original exactly, including its truncation-at-each-step behavior).
    val hours = this.toFloat() / 3600f
    val fullHours = hours.toInt()
    val minutes = (hours - fullHours) * 60f
    val fullMinutes = minutes.toInt()
    val seconds = (minutes - fullMinutes) * 60f
    val fullSeconds = seconds.toInt()
    return "${fullHours.toString().padStart(2, '0')}:${fullMinutes.toString().padStart(2, '0')}:${fullSeconds.toString().padStart(2, '0')}"
}

fun Number.toFemi(): String {
    val formattedNumber = this.toDouble().toString().let {
        if (it.contains(".")) {
            it.padEnd(it.indexOf('.') + 3, '0').substring(0, it.indexOf('.') + 3)
        } else {
            "$it.00"
        }
    }
    return "$formattedNumber $TOKEN_SYMBOL"
}
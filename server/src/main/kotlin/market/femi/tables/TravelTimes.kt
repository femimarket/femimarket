package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object TravelTimes : Table("travel_times") {
    val fromPostcodeId = text("from_postcode_id")
    val toPostcodeId = text("to_postcode_id")
    val transportModeId = text("transport_mode_id")
    val travelMins = integer("travel_mins")
    val departureTime = text("departure_time").nullable()
}

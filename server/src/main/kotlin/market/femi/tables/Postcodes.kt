package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object Postcodes : Table("postcodes") {
    val id = text("id")
    val latitude = double("latitude")
    val longitude = double("longitude")
}

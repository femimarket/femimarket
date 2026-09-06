package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object Clients : Table("clients") {
    val postcodeId = text("postcode_id")
}

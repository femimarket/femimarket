package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object Staffs : Table("staffs") {
    val postcodeId = text("postcode_id")
}

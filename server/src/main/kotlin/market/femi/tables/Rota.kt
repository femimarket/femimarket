package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object Rota : Table("rota") {
    val id = integer("id")
    val fromDate = text("from_date")
    val toDate = text("to_date")
}

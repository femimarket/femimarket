package market.femi.tables

import org.jetbrains.exposed.v1.core.Table

object Assignments : Table("assignments") {
    val rotaId = integer("rota_id")
}

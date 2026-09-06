package market.femi

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.Database

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = ConfigFactory.load()
    val database = config.getString("database")
    Database.connect("jdbc:sqlite:$database")
    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        post("/travel-times/sync") {
            call.respondText(syncTravelTimes(call.receiveText()))
        }
    }
}

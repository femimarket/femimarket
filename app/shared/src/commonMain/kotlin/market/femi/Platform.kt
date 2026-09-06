package market.femi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
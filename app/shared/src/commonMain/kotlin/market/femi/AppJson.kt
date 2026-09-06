package market.femi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}
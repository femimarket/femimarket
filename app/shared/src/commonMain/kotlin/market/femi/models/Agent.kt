@file:OptIn(ExperimentalUuidApi::class)

package market.femi.models

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class Agent(
    val id: Int? = null,
    val name: String = "",
    val goal: String = "",
    val backstory: String = "",
) {
    val reqId: String = Uuid.random().toString()

    fun systemPrompt(): String = """
        Role: $name
        Goal: $goal
        Backstory: $backstory
    """.trimIndent()
}

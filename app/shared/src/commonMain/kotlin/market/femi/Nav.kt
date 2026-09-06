package market.femi

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

// Navigation 3 destination keys (kotlinlang.org/docs/multiplatform/compose-navigation-3.html).
// NavKey is the polymorphic base the saved-state serializer restores the back stack through, so
// every Route subclass is registered against NavKey — the individual-registration pattern from
// the docs, plain kotlinx.serialization, commonMain only.


val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SplashRoute::class, SplashRoute.serializer())
            subclass(SetupRoute::class, SetupRoute.serializer())
            subclass(CareRulesRoute::class, CareRulesRoute.serializer())
            subclass(EditorRoute::class, EditorRoute.serializer())
            subclass(LineRoute::class, LineRoute.serializer())
        }
    }
}

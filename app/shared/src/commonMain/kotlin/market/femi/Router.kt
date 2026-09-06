package market.femi

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import femi.app.shared.generated.resources.Res
import femi.app.shared.generated.resources.full_company_name
import femi.app.shared.generated.resources.music_videos
import femi.app.shared.generated.resources.splash_care
import femi.app.shared.generated.resources.splash_match
import femi.app.shared.generated.resources.splash_tech
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import market.femi.services.LogService
import market.femi.services.createRealLogService
import market.femi.carer.CareJobs
import market.femi.carer.CareLogin
import market.femi.carer.CarerJobsExperience
import market.femi.carer.CarerJobsHours
import market.femi.carer.CarerJobsSkills
import market.femi.carer.CarerJobsShifts
import market.femi.carer.CarerRota
import market.femi.carer.CarerRules
import market.femi.carer.stats.CarerUncovered
import market.femi.carer.register.CareNames
import market.femi.carer.register.CareContact
import market.femi.carer.register.CareRightToWork
import market.femi.carer.register.CareSponsorship
import market.femi.carer.register.CareChecklist
import market.femi.carer.CarerAvailability
import market.femi.carer.CarerAvailabilityNew
import market.femi.carer.CarerTasks
import market.femi.carer.CareAccount
import market.femi.carer.CareCandidateCheck
import market.femi.carer.CareServers
import market.femi.carer.CareServiceUser.CareServiceUserCreateUserName
import market.femi.carer.CareServiceUser.CareServiceUserId
import market.femi.carer.CareServiceUser.CareServiceUserList
import market.femi.match.MatchQuestionnaire
import market.femi.match.MatchCheck
import market.femi.match.MatchQuestionnaireList
import market.femi.tech.TechComposable
import market.femi.tech.TechJobs
import market.femi.carer.register.CareNoSlots
import market.femi.carer.register.CareShareCode
import market.femi.carer.register.CareOverseas
import market.femi.carer.register.CareDbs
import market.femi.carer.register.CareTrading
import market.femi.carer.register.CareBank
import market.femi.carer.register.CareDocuments
import market.femi.carer.register.CareHistory
import market.femi.carer.register.CareReferences
import market.femi.carer.register.CareHealth
import market.femi.carer.register.CareEquality
import market.femi.carer.register.CareVideo
import market.femi.carer.register.CareReview
import market.femi.carer.register.CarePassword
import market.femi.carer.register.CareAssessment
import market.femi.carer.register.CareDone
import market.femi.models.AudioLine
import market.femi.money.MoneyHome
import market.femi.money.MoneyAmount
import market.femi.money.MoneyRecipient
import market.femi.money.MoneyPhone
import market.femi.money.MoneyIdentity
import market.femi.money.MoneyPay
import market.femi.money.MoneyReview
import market.femi.money.MoneyReceipt
import market.femi.money.MoneyTransactions
import market.femi.money.MoneySettings
import market.femi.music.MusicSelect
import market.femi.music.MusicServers
import market.femi.music.MusicAbout
import market.femi.music.MusicStudio
import market.femi.music.studio.MusicLines
import market.femi.music.studio.MusicBlueprint
import market.femi.music.studio.MusicFaqs
import market.femi.music.studio.MusicScenes

class RouterState(private val state: State): ActiveShowState() {
    private val log: LogService = createRealLogService("RouterState")
    val backStack = mutableStateListOf<NavKey>(SplashRoute)
    val route by derivedStateOf {
        backStack.last()
    }
    private fun navigateTo(route: Route) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }
    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val title by derivedStateOf {
        if (isMusicRoute) {
            Res.string.music_videos
        } else if (isCareRoute) {
            Res.string.splash_care
        } else if (isMatchRoute) {
            Res.string.splash_match
        } else if (isTechRoute) {
            Res.string.splash_tech
        } else {
            Res.string.full_company_name
        }
    }
    val subtitle by derivedStateOf {
        if (route is SplashRoute) {
            null
        } else {
            Res.string.full_company_name
        }

    }

    val isSuperApp by derivedStateOf {
        when (backStack.last()) {
            SplashRoute, LegalInfoRoute -> true
            else -> false
        }
    }
    val isAbout by derivedStateOf {
        when (backStack.last()) {
            MusicAboutRoute -> true
            else -> false
        }
    }
    val isCareRegisterRoute by derivedStateOf {
        when (route) {
            CareNamesRoute, CareContactRoute, CareRightToWorkRoute, CareSponsorshipRoute, CareShareCodeRoute, CareOverseasRoute, CareDbsRoute, CareTradingRoute, CareBankRoute, CareDocumentsRoute, CareHistoryRoute, CareReferencesRoute, CareHealthRoute, CareEqualityRoute, CareVideoRoute, CareReviewRoute, CarePasswordRoute, CareAssessmentRoute, CareChecklistRoute, CareNoSlotsRoute, CareDoneRoute -> true
            else -> false
        }
    }
    val isCareRoute by derivedStateOf {
        route is CareRoute
    }
    val isMusicRoute by derivedStateOf {
        route is MusicRoute
    }
    val isMatchRoute by derivedStateOf {
        route is MatchRoute
    }
    val isTechRoute by derivedStateOf {
        route is TechRoute
    }
    val isMoneyRoute by derivedStateOf {
        route is MoneyRoute
    }

    fun openMusicApp() {
        navigateTo(MusicAboutRoute)
    }
    fun openLogin() {
        navigateTo(LoginRoute)
    }
    fun openCare() {
        navigateTo(CareLoginRoute)
    }
    fun openMatchApp() {
        navigateTo(MatchCheckRoute)
        state.matchApp.check.check()
    }
    fun openTechApp() {
        navigateTo(TechJobsRoute)
    }
    fun openMoneyHome() {
        navigateTo(MoneyHomeRoute)
    }
    fun openMoneyAmount() {
        navigateTo(MoneyAmountRoute)
    }
    fun openMoneyRecipient() {
        navigateTo(MoneyRecipientRoute)
    }
    fun openMoneyPhone() {
        navigateTo(MoneyPhoneRoute)
    }
    fun openMoneyIdentity() {
        navigateTo(MoneyIdentityRoute)
    }
    fun openMoneyPay() {
        navigateTo(MoneyPayRoute)
    }
    fun openMoneyReview() {
        navigateTo(MoneyReviewRoute)
    }
    fun openMoneyReceipt() {
        navigateTo(MoneyReceiptRoute)
    }
    fun openMoneyTransactions() {
        navigateTo(MoneyTransactionsRoute)
    }
    fun openMoneySettings() {
        navigateTo(MoneySettingsRoute)
    }
    fun openSettings() {
        navigateTo(SettingsRoute)
    }
    fun openTechComposable(composableId: Int) {
        navigateTo(TechComposableRoute(composableId))
    }
    fun openMatchQuestionnaireList() {
        navigateTo(MatchQuestionnaireListRoute)
    }
    fun openMatchQuestionnaire(questionnaire: JsonElement) {
        navigateTo(MatchQuestionnaireRoute(questionnaire))
    }
    fun openCareAccount() {
        navigateTo(CareAccountRoute)
    }
    fun openCareRota() {
        navigateTo(CareRotaRoute)
    }

    fun openCareJobsExperience() {
        navigateTo(CareJobsExperienceRoute)
    }
    fun openCareJobsSkills() {
        navigateTo(CareJobsSkillsRoute)
    }
    fun openCareJobsHours() {
        navigateTo(CareJobsHoursRoute)
    }
    fun openCareNames() {
        navigateTo(CareNamesRoute)
    }
    fun openCareJobsShifts() {
        navigateTo(CareJobsShiftsRoute)
    }
    fun openCareUncovered() {
        navigateTo(CareUncoveredRoute)
    }
    fun openCareAvailability() {
        navigateTo(CareAvailabilityRoute)
    }
    fun openCareAvailabilityNew() {
        navigateTo(CareAvailabilityNewRoute)
    }
    fun openCareServers() {
        navigateTo(CareServersRoute)
    }
    fun openCareServiceUserList() {
        navigateTo(CareServiceUserListRoute)
    }
    fun openCareServiceUserCreateUserName() {
        navigateTo(CareServiceUserCreateUserNameRoute)
    }
    fun openCareServiceUserId(id: String) {
        backStack[backStack.lastIndex] = CareServiceUserIdRoute(id)
    }
    fun openCareTasks() {
        navigateTo(CareTasksRoute)
    }
    fun openCareResume() {
        navigateTo(CareCandidateCheckRoute)
    }
    fun openCareContact() {
        navigateTo(CareContactRoute)
    }
    fun openCareRightToWork() {
        navigateTo(CareRightToWorkRoute)
    }
    fun openCareSponsorship() {
        navigateTo(CareSponsorshipRoute)
    }
    fun openCareShareCode() {
        navigateTo(CareShareCodeRoute)
    }
    fun openCareOverseas() {
        navigateTo(CareOverseasRoute)
    }
    fun openCareDbs() {
        navigateTo(CareDbsRoute)
    }
    fun openCareTrading() {
        navigateTo(CareTradingRoute)
    }
    fun openCareBank() {
        navigateTo(CareBankRoute)
    }
    fun openCareDocuments() {
        navigateTo(CareDocumentsRoute)
    }
    fun openCareHistory() {
        navigateTo(CareHistoryRoute)
    }
    fun openCareReferences() {
        navigateTo(CareReferencesRoute)
    }
    fun openCareHealth() {
        navigateTo(CareHealthRoute)
    }
    fun openCareEquality() {
        navigateTo(CareEqualityRoute)
    }
    fun openCareVideo() {
        navigateTo(CareVideoRoute)
    }
    fun openCareReview() {
        navigateTo(CareReviewRoute)
    }
    fun openCarePassword() {
        navigateTo(CarePasswordRoute)
    }
    fun openCareAssessment() {
        navigateTo(CareAssessmentRoute)
    }
    fun openCareChecklist() {
        navigateTo(CareChecklistRoute)
    }
    fun openCareNoSlots() {
        navigateTo(CareNoSlotsRoute)
    }
    fun openCareDone() {
        navigateTo(CareDoneRoute)
    }
    fun openMusicLyrics() {
        navigateTo(MusicLyricsRoute)
    }
    fun openMusicLine(id: Int) {
        navigateTo(LineRoute(id))
    }
    fun openMusicStudio() {
        navigateTo(MusicStudioRoute)
    }
    fun openMusicServer() {
        navigateTo(MusicServersRoute)
    }
    fun openMusicSongs() {
        navigateTo(MusicSongsRoute)
    }
    fun openMusicThemes(line: AudioLine) {
        state.scenes.pickLine(line)
        navigateTo(MusicScenesRoute)
    }
    fun openLegal() {
        navigateTo(LegalInfoRoute)
    }

    fun openInfo(){
        if (isCareRoute) {
            navigateTo(CareInfoRoute)
        } else if (isMusicRoute) {
            when (backStack.last()) {
                MusicAboutRoute -> openLegal()
                else -> openMusicStudio()
            }
        } else {
            openSettings()
        }
    }

    val lineRouteAudioLine by derivedStateOf {
        val route = backStack.lastOrNull() as? LineRoute ?: return@derivedStateOf null
        state.data.audio?.lines?.getOrNull(route.id)
    }
}

val routes: Map<Route, @Composable (State) -> Unit> = mapOf(
    SplashRoute to { state -> Splash(state) },
    MusicAppRoute to { state -> MusicLines(state) },
    MusicAboutRoute to { state -> MusicAbout(state) },
    MusicStudioRoute to { state -> MusicStudio(state) },
    MusicSongsRoute to { state -> MusicSelect(state) },
    MusicServersRoute to { state -> MusicServers(state) },
    MusicLyricsRoute to { state -> MusicLines(state) },
    MusicFaqsRoute to { state -> MusicFaqs(state) },
    MusicBlueprintRoute to { state -> MusicBlueprint(state) },
    MusicScenesRoute to { state -> MusicScenes(state) },
    LegalInfoRoute to { state -> Legal(state) },
    LoginRoute to { state -> Login(state) },
    MusicLoginRoute to { state -> MusicLogin(state) },
    CareLoginRoute to { state -> CareLogin(state) },
    CareRulesRoute to { state -> CarerRules(state) },
    MatchCheckRoute to { state -> MatchCheck(state) },
    MatchQuestionnaireListRoute to { state -> MatchQuestionnaireList(state) },
    TechJobsRoute to { state -> TechJobs(state) },
    SettingsRoute to { state -> Settings(state) },
    CareRotaRoute to { state -> CarerRota(state) },
    CareJobsRoute to { state -> CareJobs(state) },
    CareJobsExperienceRoute to { state -> CarerJobsExperience(state) },
    CareJobsSkillsRoute to { state -> CarerJobsSkills(state) },
    CareJobsHoursRoute to { state -> CarerJobsHours(state) },
    CareJobsShiftsRoute to { state -> CarerJobsShifts(state) },
    CareUncoveredRoute to { state -> CarerUncovered(state) },
    CareAvailabilityRoute to { state -> CarerAvailability(state) },
    CareAvailabilityNewRoute to { state -> CarerAvailabilityNew(state) },
    CareServersRoute to { state -> CareServers(state) },
    CareServiceUserListRoute to { state -> CareServiceUserList(state) },
    CareServiceUserCreateUserNameRoute to { state -> CareServiceUserCreateUserName(state) },
    CareTasksRoute to { state -> CarerTasks(state) },
    CareAccountRoute to { state -> CareAccount(state) },
    CareCandidateCheckRoute to { state -> CareCandidateCheck(state) },
    CareNamesRoute to { state -> CareNames(state) },
    CareContactRoute to { state -> CareContact(state) },
    CareRightToWorkRoute to { state -> CareRightToWork(state) },
    CareSponsorshipRoute to { state -> CareSponsorship(state) },
    CareShareCodeRoute to { state -> CareShareCode(state) },
    CareOverseasRoute to { state -> CareOverseas(state) },
    CareDbsRoute to { state -> CareDbs(state) },
    CareTradingRoute to { state -> CareTrading(state) },
    CareBankRoute to { state -> CareBank(state) },
    CareDocumentsRoute to { state -> CareDocuments(state) },
    CareHistoryRoute to { state -> CareHistory(state) },
    CareReferencesRoute to { state -> CareReferences(state) },
    CareHealthRoute to { state -> CareHealth(state) },
    CareEqualityRoute to { state -> CareEquality(state) },
    CareVideoRoute to { state -> CareVideo(state) },
    CareReviewRoute to { state -> CareReview(state) },
    CarePasswordRoute to { state -> CarePassword(state) },
    CareAssessmentRoute to { state -> CareAssessment(state) },
    CareChecklistRoute to { state -> CareChecklist(state) },
    CareNoSlotsRoute to { state -> CareNoSlots(state) },
    CareDoneRoute to { state -> CareDone(state) },
    CarerJobsRoute to { state -> CareJobs(state) },
    MoneyHomeRoute to { state -> MoneyHome(state) },
    MoneyAmountRoute to { state -> MoneyAmount(state) },
    MoneyRecipientRoute to { state -> MoneyRecipient(state) },
    MoneyPhoneRoute to { state -> MoneyPhone(state) },
    MoneyIdentityRoute to { state -> MoneyIdentity(state) },
    MoneyPayRoute to { state -> MoneyPay(state) },
    MoneyReviewRoute to { state -> MoneyReview(state) },
    MoneyReceiptRoute to { state -> MoneyReceipt(state) },
    MoneyTransactionsRoute to { state -> MoneyTransactions(state) },
    MoneySettingsRoute to { state -> MoneySettings(state) },
)

@Composable
fun Router(state: State){
    NavDisplay(
        transitionSpec = {
            (slideInHorizontally(tween(600)) { it } + fadeIn(tween(600))) togetherWith fadeOut(tween(600))
        },
        popTransitionSpec = {       // back: the push in reverse, same clock
            fadeIn(tween(600)) togetherWith (slideOutHorizontally(tween(600)) { it } + fadeOut(tween(600)))
        },
        predictivePopTransitionSpec = {  // gesture-back, same as pop
            fadeIn(tween(600)) togetherWith (slideOutHorizontally(tween(600)) { it } + fadeOut(tween(600)))
        },
        backStack = state.nav.backStack,
        onBack = { state.nav.goBack() },
        entryProvider = entryProvider {
            routes.forEach { (route, page) ->
                entry(route) { page(state) }
            }
            entry<MatchQuestionnaireRoute> { route -> MatchQuestionnaire(state, route.questionnaire) }
            entry<TechComposableRoute> { route -> TechComposable(state, route.composableId) }
            entry<CareServiceUserIdRoute> { route -> CareServiceUserId(state, route.id) }
        },
    )
}


sealed interface Route : NavKey

sealed interface MusicRoute : Route

sealed interface CareRoute : Route

sealed interface TechRoute : Route

sealed interface MatchRoute : Route

sealed interface MoneyRoute : Route

@Serializable
data object SplashRoute : Route
data object MusicAppRoute : MusicRoute
data object MusicServersRoute : MusicRoute
data object MusicLyricsRoute : MusicRoute
data object LegalInfoRoute : Route
data object MusicStudioRoute : MusicRoute
data object MusicFaqsRoute : MusicRoute
data object MusicBlueprintRoute : MusicRoute
data object MusicScenesRoute : MusicRoute
data object CareInfoRoute : CareRoute
data object CarerJobsRoute : CareRoute
data object LoginRoute : Route
data object MusicLoginRoute : Route
data class MatchQuestionnaireRoute(val questionnaire: JsonElement) : MatchRoute
data object MatchCheckRoute : MatchRoute
data object MatchQuestionnaireListRoute : MatchRoute
data object SettingsRoute : Route
data object TechJobsRoute : TechRoute
data class TechComposableRoute(val composableId: Int) : TechRoute

data object MusicAboutRoute : MusicRoute
data object MusicSongsRoute : MusicRoute

@Serializable
data object SetupRoute : Route

data object CareLoginRoute : CareRoute
@Serializable
data object CareRulesRoute : CareRoute
data object CareRotaRoute : CareRoute
data object CareJobsRoute : CareRoute
data object CareJobsExperienceRoute : CareRoute
data object CareJobsSkillsRoute : CareRoute
data object CareJobsHoursRoute : CareRoute
data object CareJobsShiftsRoute : CareRoute
data object CareUncoveredRoute : CareRoute
data object CareAvailabilityRoute : CareRoute
data object CareAvailabilityNewRoute : CareRoute
data object CareServersRoute : CareRoute
data object CareServiceUserListRoute : CareRoute
data object CareServiceUserCreateUserNameRoute : CareRoute
data class CareServiceUserIdRoute(val id: String) : CareRoute
data object CareTasksRoute : CareRoute
data object CareAccountRoute : CareRoute
data object CareCandidateCheckRoute : CareRoute
data object CareNamesRoute : CareRoute
data object CareContactRoute : CareRoute
data object CareRightToWorkRoute : CareRoute
data object CareSponsorshipRoute : CareRoute
data object CareShareCodeRoute : CareRoute
data object CareOverseasRoute : CareRoute
data object CareDbsRoute : CareRoute
data object CareTradingRoute : CareRoute
data object CareBankRoute : CareRoute
data object CareDocumentsRoute : CareRoute
data object CareHistoryRoute : CareRoute
data object CareReferencesRoute : CareRoute
data object CareHealthRoute : CareRoute
data object CareEqualityRoute : CareRoute
data object CareVideoRoute : CareRoute
data object CareReviewRoute : CareRoute
data object CarePasswordRoute : CareRoute
data object CareAssessmentRoute : CareRoute
data object CareChecklistRoute : CareRoute
data object CareNoSlotsRoute : CareRoute
data object CareDoneRoute : CareRoute

data object MoneyHomeRoute : MoneyRoute
data object MoneyAmountRoute : MoneyRoute
data object MoneyRecipientRoute : MoneyRoute
data object MoneyPhoneRoute : MoneyRoute
data object MoneyIdentityRoute : MoneyRoute
data object MoneyPayRoute : MoneyRoute
data object MoneyReviewRoute : MoneyRoute
data object MoneyReceiptRoute : MoneyRoute
data object MoneyTransactionsRoute : MoneyRoute
data object MoneySettingsRoute : MoneyRoute

@Serializable
data object EditorRoute : Route

@Serializable
data class LineRoute(val id: Int) : Route

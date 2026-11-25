package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.firebase.configureAppCheck
import com.example.core.di.AppContainer
import com.example.data.ai.ChefAiRepository
import com.example.data.curriculum.CourseContent
import com.example.data.remote.CommunityRepository
import com.example.ui.components.AtelierFullLogoBanner
import com.example.ui.components.AudioPlayerUI
import com.example.ui.components.SwissCrossBadge
import com.example.ui.components.TagPill
import com.example.ui.components.TimerAlertsPermissionBanner
import com.example.ui.screens.AssessmentCalculatorsScreen
import com.example.ui.screens.CommunityChefAiScreen
import com.example.ui.screens.Dashboard24SScreen
import com.example.ui.screens.RecipesKitchenMepScreen
import com.example.ui.screens.ShoppingEquipmentScreen
import com.example.ui.screens.TheoryAtlasSm2Screen
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.AtelierGastronomiaTheme
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PureSurface
import com.example.ui.theme.Slate200
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AppShellViewModel
import com.example.ui.viewmodel.AssessmentViewModel
import com.example.ui.viewmodel.CourseViewModel
import com.example.ui.viewmodel.KitchenViewModel
import com.example.ui.viewmodel.PantryViewModel
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.StreamUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandatory App Check initialization BEFORE setContent or any Firebase.ai / Firestore call
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AtelierGastronomiaTheme {
                CourseContentGate {
                    AtelierShell()
                }
            }
        }
    }
}

/**
 * Carrega e parseia o conteúdo do curso (JSONs em assets) em Dispatchers.IO antes de montar a UI,
 * evitando trabalho pesado na main thread no cold start.
 */
@Composable
private fun CourseContentGate(content: @Composable () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    var isReady by remember { mutableStateOf(CourseContent.isLoaded) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    if (!isReady) {
        LaunchedEffect(attempt) {
            loadError = null
            runCatching { CourseContent.preload(appContext) }
                .onSuccess { isReady = true }
                .onFailure { loadError = it.localizedMessage ?: it.javaClass.simpleName }
        }
    }

    if (isReady) {
        content()
        return
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AtelierFullLogoBanner(maxHeight = 96.dp)
            val error = loadError
            if (error == null) {
                CircularProgressIndicator(color = SwissRed, modifier = Modifier.testTag("content_loading"))
                Text(
                    text = stringResource(R.string.content_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.content_load_error, error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { attempt++ },
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier.testTag("content_retry")
                ) { Text(stringResource(R.string.content_retry)) }
            }
        }
    }
}

/** Índice da sub-aba "Autoavaliação & Cópia" em [AssessmentCalculatorsScreen]. */
private const val ASSESSMENT_SELF_EVAL_SUBTAB = 1

private data class NavDestination(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
)

@Composable
private fun AtelierShell() {
    val appContext = LocalContext.current.applicationContext
    val uid = ""
    val isSignedIn = false
    val defaultName = "Chef Teste (Local)"

    val container = remember { AppContainer.from(appContext) }
    val shellVm: AppShellViewModel = viewModel(factory = container.viewModelFactory)
    val courseVm: CourseViewModel = viewModel(factory = container.viewModelFactory)
    val kitchenVm: KitchenViewModel = viewModel(factory = container.viewModelFactory)
    val pantryVm: PantryViewModel = viewModel(factory = container.viewModelFactory)
    val assessmentVm: AssessmentViewModel = viewModel(factory = container.viewModelFactory)

    val communityVm: CommunityAiViewModel = viewModel(
        key = "community_${uid.ifBlank { "guest_test" }}",
        factory = viewModelFactory {
            initializer {
                CommunityAiViewModel(
                    communityRepo = CommunityRepository(appContext),
                    chefAiRepo = ChefAiRepository(),
                    currentUserId = uid,
                    initialDisplayName = defaultName
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var assessmentSubTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val audioPlayerVm: AudioPlayerViewModel = viewModel(
        key = "audio_player_viewmodel",
        factory = viewModelFactory {
            initializer { AudioPlayerViewModel(appContext) }
        }
    )
    val audioNarrator = audioPlayerVm.audioNarrator
    val podcastPlayer = audioPlayerVm.podcastPlayer

    val cloudFeedback by communityVm.feedbackMessage.collectAsStateWithLifecycle()

    LaunchedEffect(shellVm) {
        shellVm.userMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    val myMepLinksState by communityVm.myMepLinksState.collectAsStateWithLifecycle()

    LaunchedEffect(isSignedIn, myMepLinksState) {
        if (isSignedIn) {
            val state = myMepLinksState
            if (state is StreamUiState.Success) {
                kitchenVm.syncCloudMepLinksIntoLocal(state.data)
            }
        }
    }

    LaunchedEffect(cloudFeedback) {
        cloudFeedback?.let {
            snackbarHostState.showSnackbar(it)
            communityVm.clearFeedbackMessage()
        }
    }

    if (currentTab != 0) {
        BackHandler { currentTab = 0 }
    }

    val destinations = remember {
        listOf(
            NavDestination(0, R.string.nav_course, Icons.Default.CalendarMonth, "nav_tab_course"),
            NavDestination(1, R.string.nav_atlas, Icons.AutoMirrored.Filled.MenuBook, "nav_tab_atlas"),
            NavDestination(2, R.string.nav_recipes, Icons.Default.RestaurantMenu, "nav_tab_recipes"),
            NavDestination(3, R.string.nav_shopping, Icons.Default.ShoppingCart, "nav_tab_shopping"),
            NavDestination(4, R.string.nav_gestion, Icons.Default.Calculate, "nav_tab_gestion"),
            NavDestination(5, R.string.nav_community, Icons.Default.Groups, "nav_tab_community")
        )
    }

    val isKitchenMode by kitchenVm.isKitchenModeActive.collectAsStateWithLifecycle()
    val anyTimerRunning by shellVm.anyTimerRunning.collectAsStateWithLifecycle()
    val selectedWeek by courseVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val currentCycleNumber = ((selectedWeek - 1) / 6 + 1).coerceIn(1, 4)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!(currentTab == 2 && isKitchenMode)) {
                    Surface(color = AtelierCharcoal) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SwissCrossBadge(
                                        sizeDp = 36,
                                        modifier = Modifier.testTag("topbar_brand_icon")
                                    )
                                    Text(
                                        text = "ATELIER GASTRONOMIA",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PureSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TagPill(
                                        text = "24S",
                                        containerColor = SwissRed,
                                        contentColor = PureSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!isSignedIn) {
                                        TagPill(
                                            text = stringResource(R.string.test_mode_badge),
                                            containerColor = PccAmberBg,
                                            contentColor = PccAmber
                                        )
                                    }
                                    TagPill(
                                        text = "SEM ${"%02d".format(selectedWeek)}",
                                        containerColor = SwissRedContainer,
                                        contentColor = OnSwissRedContainer
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 2.dp, color = SwissRed)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideTablet) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AudioPlayerUI(
                            viewModel = audioPlayerVm,
                            currentCycleNumber = currentCycleNumber
                        )
                        NavigationBar(containerColor = PureSurface) {
                            destinations.forEach { dest ->
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = currentTab == dest.index,
                                    onClick = {
                                        assessmentSubTab = 0
                                        currentTab = dest.index
                                    },
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SwissRed,
                                        selectedTextColor = AtelierCharcoal,
                                        indicatorColor = SwissRedContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideTablet) {
                    NavigationRail(
                        containerColor = PureSurface,
                        header = {
                            Spacer(Modifier.height(8.dp))
                            SwissCrossBadge(sizeDp = 42)
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Slate200.copy(alpha = 0.35f))
                    ) {
                        destinations.forEach { dest ->
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = currentTab == dest.index,
                                onClick = {
                                    assessmentSubTab = 0
                                    currentTab = dest.index
                                },
                                icon = { Icon(dest.icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SwissRed,
                                    selectedTextColor = AtelierCharcoal,
                                    indicatorColor = SwissRedContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TimerAlertsPermissionBanner(
                        anyTimerRunning = anyTimerRunning,
                        canScheduleExactAlarms = shellVm::canScheduleExactTimerAlarms,
                        onPermissionsChanged = shellVm::refreshTimers
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (currentTab) {
                            0 -> Dashboard24SScreen(
                                courseVm = courseVm,
                                kitchenVm = kitchenVm,
                                communityVm = communityVm,
                                audioNarrator = audioNarrator,
                                podcastPlayer = podcastPlayer,
                                onOpenTheoryTab = { currentTab = 1 },
                                onOpenRecipeKitchen = { recipeId ->
                                    kitchenVm.selectRecipe(recipeId)
                                    currentTab = 2
                                },
                                onOpenCommunityTab = { currentTab = 5 },
                                onOpenSelfEvaluation = {
                                    assessmentSubTab = ASSESSMENT_SELF_EVAL_SUBTAB
                                    currentTab = 4
                                }
                            )
                            1 -> TheoryAtlasSm2Screen(
                                courseVm = courseVm,
                                audioNarrator = audioNarrator,
                                podcastPlayer = podcastPlayer,
                                onJumpToRecipe = { recipeId ->
                                    kitchenVm.selectRecipe(recipeId)
                                    currentTab = 2
                                }
                            )
                            2 -> RecipesKitchenMepScreen(
                                kitchenVm = kitchenVm,
                                communityVm = communityVm,
                                onNavigateToPhotoRegister = { currentTab = 5 },
                                onNavigateToChefSos = {
                                    kitchenVm.exitKitchenMode()
                                    currentTab = 5
                                }
                            )
                            3 -> ShoppingEquipmentScreen(pantryVm = pantryVm)
                            4 -> AssessmentCalculatorsScreen(
                                assessmentVm = assessmentVm,
                                kitchenVm = kitchenVm,
                                initialSubTab = assessmentSubTab,
                                onOpenMepSheet = { currentTab = 2 },
                                onCreateNewMepSheet = { currentTab = 2 }
                            )
                            5 -> CommunityChefAiScreen(
                                kitchenVm = kitchenVm,
                                communityVm = communityVm,
                                defaultAuthorName = defaultName
                            )
                        }
                    }

                    if (isWideTablet) {
                        AudioPlayerUI(
                            viewModel = audioPlayerVm,
                            currentCycleNumber = currentCycleNumber
                        )
                    }
                }
            }
        }
    }
}

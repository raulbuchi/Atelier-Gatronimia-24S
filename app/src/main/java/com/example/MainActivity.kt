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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.firebase.authStateFlow
import com.example.core.firebase.configureAppCheck
import com.example.data.ai.ChefAiRepository
import com.example.data.local.AtelierDatabase
import com.example.data.local.LocalKitchenRepository
import com.example.data.remote.CommunityRepository
import com.example.ui.components.SwissCrossBadge
import com.example.ui.components.TagPill
import com.example.ui.screens.AssessmentCalculatorsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityChefAiScreen
import com.example.ui.screens.Dashboard24SScreen
import com.example.ui.screens.RecipesKitchenMepScreen
import com.example.ui.screens.ShoppingEquipmentScreen
import com.example.ui.screens.TheoryAtlasSm2Screen
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.AtelierGastronomiaTheme
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PureSurface
import com.example.ui.theme.Slate200
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.StreamUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

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
                AtelierRootGate()
            }
        }
    }
}

private data class NavDestination(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
)

@Composable
private fun AtelierRootGate() {
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow updates reactively */ })
    } else {
        key(user.uid) {
            AuthenticatedAtelierShell(user = user)
        }
    }
}

@Composable
private fun AuthenticatedAtelierShell(user: FirebaseUser) {
    val appContext = LocalContext.current.applicationContext
    val defaultName = user.displayName?.takeIf { it.isNotBlank() } ?: "Brigada Atelier"

    val atelierVm: AtelierViewModel = viewModel(
        key = "atelier_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val dao = AtelierDatabase.getInstance(appContext).atelierDao()
                AtelierViewModel(LocalKitchenRepository(dao))
            }
        }
    )

    val communityVm: CommunityAiViewModel = viewModel(
        key = "community_${user.uid}",
        factory = viewModelFactory {
            initializer {
                CommunityAiViewModel(
                    communityRepo = CommunityRepository(appContext),
                    chefAiRepo = ChefAiRepository(),
                    currentUserId = user.uid,
                    initialDisplayName = defaultName
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val localBanner by atelierVm.statusBanner.collectAsStateWithLifecycle()
    val cloudFeedback by communityVm.feedbackMessage.collectAsStateWithLifecycle()

    LaunchedEffect(localBanner) {
        localBanner?.let {
            snackbarHostState.showSnackbar(it)
            atelierVm.clearBanner()
        }
    }

    val myMepLinksState by communityVm.myMepLinksState.collectAsStateWithLifecycle()

    LaunchedEffect(myMepLinksState) {
        val state = myMepLinksState
        if (state is StreamUiState.Success) {
            atelierVm.syncCloudMepLinksIntoLocal(state.data)
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

    val isKitchenMode by atelierVm.isKitchenModeActive.collectAsStateWithLifecycle()
    val selectedWeek by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()

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
                                TagPill(
                                    text = "SEM ${"%02d".format(selectedWeek)}",
                                    containerColor = SwissRedContainer,
                                    contentColor = OnSwissRedContainer
                                )
                            }
                            HorizontalDivider(thickness = 2.dp, color = SwissRed)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideTablet) {
                    NavigationBar(containerColor = PureSurface) {
                        destinations.forEach { dest ->
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = currentTab == dest.index,
                                onClick = { currentTab = dest.index },
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
                                onClick = { currentTab = dest.index },
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            0 -> Dashboard24SScreen(
                                atelierVm = atelierVm,
                                communityVm = communityVm,
                                onOpenTheoryTab = { currentTab = 1 },
                                onOpenRecipeKitchen = { recipeId ->
                                    atelierVm.selectRecipe(recipeId)
                                    currentTab = 2
                                },
                                onOpenCommunityTab = { currentTab = 5 }
                            )
                            1 -> TheoryAtlasSm2Screen(
                                atelierVm = atelierVm,
                                onJumpToRecipe = { recipeId ->
                                    atelierVm.selectRecipe(recipeId)
                                    currentTab = 2
                                }
                            )
                            2 -> RecipesKitchenMepScreen(
                                atelierVm = atelierVm,
                                communityVm = communityVm,
                                onNavigateToPhotoRegister = { currentTab = 5 },
                                onNavigateToChefSos = {
                                    atelierVm.exitKitchenMode()
                                    currentTab = 5
                                }
                            )
                            3 -> ShoppingEquipmentScreen(atelierVm = atelierVm)
                            4 -> AssessmentCalculatorsScreen(
                                atelierVm = atelierVm,
                                onOpenMepSheet = { currentTab = 2 },
                                onCreateNewMepSheet = { currentTab = 2 }
                            )
                            5 -> CommunityChefAiScreen(
                                atelierVm = atelierVm,
                                communityVm = communityVm,
                                defaultAuthorName = defaultName
                            )
                        }
                    }
                }
            }
        }
    }
}

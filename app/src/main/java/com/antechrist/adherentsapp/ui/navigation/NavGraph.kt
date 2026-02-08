package com.antechrist.adherentsapp.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antechrist.adherentsapp.core.debug.attachNavLogger
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.domain.model.isFinanceManager
import com.antechrist.adherentsapp.domain.model.isStaff
import com.antechrist.adherentsapp.domain.utils.SeasonUtils
import com.antechrist.adherentsapp.ui.common.LogoutViewModel
import com.antechrist.adherentsapp.ui.components.AccessDeniedScreen
import com.antechrist.adherentsapp.ui.role.RoleViewModel
import com.antechrist.adherentsapp.ui.screens.access.AccessDeniedScreen
import com.antechrist.adherentsapp.ui.screens.whatsapp.WhatsAppParentsScreen
import com.antechrist.adherentsapp.ui.screens.config.ClubConfigScreen
import com.antechrist.adherentsapp.ui.screens.bureau.BureauHomeScreen
import com.antechrist.adherentsapp.ui.screens.bureau.SeasonAdminScreen
import com.antechrist.adherentsapp.ui.screens.cotisations.CotisationHouseholdScreen
import com.antechrist.adherentsapp.ui.screens.cotisations.CotisationsListScreen
import com.antechrist.adherentsapp.ui.screens.detail.AdherentDetailScreen
import com.antechrist.adherentsapp.ui.screens.finance.FinanceSeasonScreen
import com.antechrist.adherentsapp.ui.screens.form.AdherentFormScreen
import com.antechrist.adherentsapp.ui.screens.kihon.KihonBoardScreen
import com.antechrist.adherentsapp.ui.screens.kihon.KihonCatalogScreen
import com.antechrist.adherentsapp.ui.screens.kihon.KihonGradeListScreen
import com.antechrist.adherentsapp.ui.screens.kihon.KihonSequenceDetailScreen
import com.antechrist.adherentsapp.ui.screens.kihon.KihonSequenceEditorScreen
import com.antechrist.adherentsapp.ui.screens.list.AdherentsListScreen
import com.antechrist.adherentsapp.ui.screens.login.LoginScreen
import com.antechrist.adherentsapp.ui.screens.presence.PresenceReportsScreen
import com.antechrist.adherentsapp.ui.screens.presence.PresenceTakeScreen
import com.antechrist.adherentsapp.ui.screens.splash.SplashScreen
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.rememberSaveable
import com.antechrist.adherentsapp.domain.model.isSuperAdmin
import com.antechrist.adherentsapp.ui.screens.cotisations.CotisationsReportScreen
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianEditScreen
import com.antechrist.adherentsapp.ui.screens.finance.LicencesReportScreen
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianListScreen
import com.antechrist.adherentsapp.ui.screens.info.InfoMessagesScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.ArrowBack
import com.antechrist.adherentsapp.ui.screens.parent.ParentHouseholdHomeScreen


/* ---------------- Bottom bar config ---------------- */

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun canSeeBottom(role: Role?): Boolean =
    role?.isStaff() == true ||
            role == Role.TRESORIER ||
            role == Role.SECRETAIRE

private object BottomRoots {
    const val PRESENCES = Destinations.PresenceTake
    const val ADHERENTS = Destinations.List
    const val COTISATIONS = Destinations.Cotisations
    const val BUREAU = Destinations.BureauHome
}

@Composable
private fun bottomItemsForRole(role: Role?): List<BottomItem> {
    if (!canSeeBottom(role)) return emptyList()
    return listOf(
        BottomItem(BottomRoots.ADHERENTS,  "Adhérents",   Icons.Filled.Groups),
        BottomItem(BottomRoots.PRESENCES,   "Présence",    Icons.Filled.EventAvailable),
        BottomItem(BottomRoots.COTISATIONS,"Cotisations", Icons.AutoMirrored.Filled.ReceiptLong),
        BottomItem(BottomRoots.BUREAU,     "Bureau",      Icons.Filled.AdminPanelSettings),
    )
}

@Composable
private fun showBottomBar(currentRoute: String?, role: Role?): Boolean {
    if (!canSeeBottom(role)) return false
    return when (currentRoute) {
        BottomRoots.PRESENCES,
        BottomRoots.ADHERENTS,
        BottomRoots.COTISATIONS,
        BottomRoots.BUREAU -> true
        else -> false
    }
}

/* ---------------- Couleurs & styles BottomBar ---------------- */

private val NavRed   = Color(0xFF4D1919)
private val IconBlue = Color(0xFF3D8FEA)
private val IconNotSelected = Color(0xFF5B5B5B)
private val IconDore = Color(0xFFD5A03B)

/* ---------------- Entrée principale ---------------- */

@Composable
fun AppNavHost(
    startDeepLinkDestination: String? = null
) {
    val nav = rememberNavController()
    LaunchedEffect(nav) { attachNavLogger(nav) }

    val roleVm: RoleViewModel = hiltViewModel()
    LaunchedEffect(Unit) { roleVm.refresh() }
    val role by roleVm.role.collectAsState()

    val items = bottomItemsForRole(role)
    val backstack by nav.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route

    var deepLinkConsumed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(startDeepLinkDestination, deepLinkConsumed) {
        if (!deepLinkConsumed && startDeepLinkDestination != null) {
            when (startDeepLinkDestination) {
                "info_messages" -> {
                    nav.navigate(Destinations.InfoMessages)
                }
            }
            deepLinkConsumed = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar(currentRoute, role) && items.isNotEmpty()) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(48.dp),
                    containerColor = NavRed
                ) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        val iconSize by animateDpAsState(
                            targetValue = if (selected) 50.dp else 30.dp,
                            animationSpec = tween(340),
                            label = "navIconSize"
                        )
                        val iconBoxHeight by animateDpAsState(
                            targetValue = if (selected) 50.dp else 30.dp,
                            animationSpec = tween(340),
                            label = "navIconBox"
                        )
                        val labelBoxHeight by animateDpAsState(
                            targetValue = if (selected) 0.dp else 12.dp,
                            animationSpec = tween(340),
                            label = "navLabelBox"
                        )

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(item.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Box(
                                    Modifier
                                        .height(iconBoxHeight)
                                        .padding(top = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(iconSize)
                                    )
                                }
                            },
                            label = {
                                Box(
                                    Modifier
                                        .height(labelBoxHeight)
                                        .offset(y = (-8).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!selected) Text(item.label, fontSize = 12.sp)
                                }
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = IconBlue,
                                unselectedIconColor = IconDore,
                                selectedTextColor = Color.Black,
                                unselectedTextColor = IconDore
                            )
                        )
                    }
                }
            }
        }
    ) { paddings ->
        AppGraph(navPadding = paddings, nav = nav)
    }
}

/* --------------- Graphe de navigation --------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppGraph(navPadding: PaddingValues, nav: NavHostController) {
    val allGradeKeys = remember {
        listOf("BLANCHE","JAUNE","ORANGE","VERTE","BLEUE","MARRON","NOIRE")
    }

    NavHost(navController = nav, startDestination = Destinations.Splash, modifier = Modifier.padding(navPadding)) {

        composable(Destinations.Splash) {
            SplashScreen(
                toLogin = { nav.navigate(Destinations.Login) { popUpTo(0) } },
                toList = { nav.navigate(Destinations.List) { popUpTo(0) } },
                toDenied = { nav.navigate(Destinations.AccessDenied) { popUpTo(0) } },
                toParentHome = { nav.navigate(Destinations.ParentHouseholdHome) { popUpTo(0) } }  // ← AJOUT
            )
        }

        composable(Destinations.Login) {
            LoginScreen(
                onAdmin = {
                    nav.navigate(Destinations.Splash) {
                        popUpTo(Destinations.Login) { inclusive = true }
                    }
                },
                onNonAdmin = {
                    nav.navigate(Destinations.Splash) {
                        popUpTo(Destinations.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.BureauHome) {
            BureauHomeScreen(
                onOpenClubConfig = { nav.navigate(Destinations.ClubConfig) },
                onOpenAgCr = { nav.navigate(Destinations.bureauAgCr(SeasonUtils.currentSeasonKey())) },
                onOpenStats = { nav.navigate(Destinations.BureauStats) },
            )
        }


        composable(Destinations.ClubConfig) {
            ClubConfigScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Destinations.BureauAgCr,
            arguments = listOf(navArgument("seasonKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonKey = backStackEntry.arguments?.getString("seasonKey")
                ?: SeasonUtils.currentSeasonKey()

            SeasonAdminScreen(
                seasonKey = seasonKey,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Destinations.BureauStats) {
            BureauPlaceholderScreen(
                title = "Stats",
                subtitle = "Module à venir"
            )
        }

        composable(Destinations.AccessDenied) {
            AccessDeniedScreen(onLogout = {
                nav.navigate(Destinations.Login) { popUpTo(0) }
            })
        }

        // Liste adhérents
        composable(Destinations.List) { backStackEntry ->
            val logoutVm: LogoutViewModel = hiltViewModel(backStackEntry)

            val roleVm: RoleViewModel = hiltViewModel(backStackEntry)
            LaunchedEffect(Unit) { roleVm.refresh() }
            val role by roleVm.role.collectAsState()
            val canFinance = role?.isFinanceManager() == true

            AdherentsListScreen(
                onLogout = {
                    logoutVm.logout {
                        nav.navigate(Destinations.Splash) { popUpTo(0) }
                    }
                },
                onAdd = { nav.navigate(Destinations.Form) },
                onOpenDetail = { adherentId, orderedIds ->
                    // On convertit la liste d'IDs en une seule chaîne séparée par des virgules.
                    val idsString = orderedIds.joinToString(",")
                    // On navigue vers la nouvelle route en passant l'ID et la liste encodée.
                    // Il est important d'encoder l'argument pour éviter les problèmes avec les caractères spéciaux.
                    nav.navigate("adherent_detail/$adherentId?ids=$idsString")
                },
                onTakePresence = { nav.navigate(Destinations.PresenceTake) },
                onViewReports  = { nav.navigate(Destinations.PresenceReports) },
                isAdmin = role?.isStaff() == true,
                onOpenKihon = { nav.navigate(KihonDestinations.BOARD) },
                onOpenCotisations = if (canFinance) {
                    {
                        Log.d("NavLog", "navigate -> ${Destinations.Cotisations} (role=${role?.name})")
                        nav.navigate(Destinations.Cotisations)
                    }
                } else null,
                onOpenClubConfig = { nav.navigate(Destinations.ClubConfig) },
                onOpenInfoMessages = { nav.navigate(Destinations.InfoMessages) },
                onCreateWhatsAppList = { nav.navigate( Destinations.WhatsAppParents)}
            )
        }

        // Form adhérent (param optionnel)
        composable(
            route = "${Destinations.Form}?${Destinations.ArgId}={${Destinations.ArgId}}",
            arguments = listOf(
                navArgument(Destinations.ArgId) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AdherentFormScreen(
                onCancel = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }

        // Détail adhérent
        // MODIFIÉ : La route de l'écran de détail est mise à jour pour accepter la liste d'IDs.
        composable(
            route = "adherent_detail/{id}?ids={ids}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("ids") { type = NavType.StringType; nullable = true }
            )
        ) { backStack ->
            val id = backStack.arguments?.getString("id")!!
            // On récupère la chaîne d'IDs et on la re-transforme en List<String>.
            val idsString = backStack.arguments?.getString("ids") ?: ""
            val orderedIds = idsString.split(',').filter { it.isNotBlank() }

            // MODIFIÉ : On passe les nouvelles informations à AdherentDetailScreen.
            // Ce fichier aura des erreurs maintenant, c'est notre prochaine étape.
            AdherentDetailScreen(
                initialAdherentId = id,
                adherentIds = orderedIds,
                onBack = { nav.popBackStack() },
                onEdit = { currentId -> nav.navigate("${Destinations.Form}?${Destinations.ArgId}=$currentId") },
                onDeleted = { nav.popBackStack() }
                // onNavigateToAdherent sera ajouté plus tard.
            )
        }

        // WhatsApp Parents
        composable(Destinations.WhatsAppParents) {
            WhatsAppParentsScreen(
                onBack = { nav.popBackStack() }
            )
        }

        // Présences
        composable(Destinations.PresenceTake) {
            PresenceTakeScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }
        composable(Destinations.PresenceReports) {
            PresenceReportsScreen(
                onBack = { nav.popBackStack() },
                onOpenTake = { dateKey, groupDisplay ->
                    nav.navigate("${Destinations.PresenceTake}?date=$dateKey&group=$groupDisplay")
                }
            )
        }

        /* ---------------- Kihon ---------------- */

        composable(KihonDestinations.BOARD) {
            KihonBoardScreen(
                allGradeKeys = allGradeKeys,
                onNavigate = { route -> nav.navigate(route) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(KihonDestinations.CATALOG) {
            KihonCatalogScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = KihonDestinations.GRADE_LIST,
            arguments = listOf(
                navArgument(KihonDestinations.ARG_GRADE_KEY) { type = NavType.StringType }
            )
        ) { backStack ->
            val gradeKey = backStack.arguments!!.getString(KihonDestinations.ARG_GRADE_KEY)!!
            KihonGradeListScreen(
                gradeKey = gradeKey,
                onOpenDetail = { gk, seqId ->
                    nav.navigate(KihonDestinations.sequenceDetail(gk, seqId))
                },
                onOpenEditor = { gk, seqId ->
                    nav.navigate(KihonDestinations.sequenceEditor(gk, seqId))
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            route = KihonDestinations.SEQUENCE_DETAIL,
            arguments = listOf(
                navArgument(KihonDestinations.ARG_GRADE_KEY) { type = NavType.StringType },
                navArgument(KihonDestinations.ARG_SEQUENCE_ID) { type = NavType.StringType }
            )
        ) { backStack ->
            val gradeKey = backStack.arguments!!.getString(KihonDestinations.ARG_GRADE_KEY)!!
            val seqId = backStack.arguments!!.getString(KihonDestinations.ARG_SEQUENCE_ID)!!
            KihonSequenceDetailScreen(
                gradeKey = gradeKey,
                sequenceId = seqId,
                onBack = { nav.popBackStack() },
                onEdit = { gk, id ->
                    nav.navigate(KihonDestinations.sequenceEditor(gk, id))
                }
            )
        }
        composable(
            route = KihonDestinations.SEQUENCE_EDITOR,
            arguments = listOf(
                navArgument(KihonDestinations.ARG_GRADE_KEY) { type = NavType.StringType },
                navArgument(KihonDestinations.ARG_SEQUENCE_ID) { type = NavType.StringType }
            )
        ) { backStack ->
            val gradeKey = backStack.arguments!!.getString(KihonDestinations.ARG_GRADE_KEY)!!
            val seqId = backStack.arguments!!.getString(KihonDestinations.ARG_SEQUENCE_ID)!!
            KihonSequenceEditorScreen(
                gradeKey = gradeKey,
                sequenceId = seqId,
                onBack = { nav.popBackStack() },
                onOpenDetail = { gk, id ->
                    nav.navigate(KihonDestinations.sequenceDetail(gk, id)) {
                        popUpTo(KihonDestinations.sequenceEditor(gk, id)) { inclusive = true }
                    }
                }
            )
        }

        /* ---------------- Cotisations ---------------- */

        // Liste (onglet racine)
        composable(Destinations.Cotisations) {
            val roleVm: RoleViewModel = hiltViewModel()
            val role = roleVm.role.collectAsState().value

            when {
                role == null -> {
                    Log.d("NavLog", "Cotisations: role=null (loading)")
                    CircularProgressIndicator()
                }
                role.isFinanceManager() -> {
                    Log.d("NavLog", "Cotisations: allowed as ${role.name}")
                    CotisationsListScreen(
                        onBack = { nav.popBackStack() },
                        onOpenHousehold = { guardianId ->
                            nav.navigate(Destinations.cotisationHousehold(guardianId))
                        },
                        onOpenFinanceSeason = { seasonKey ->
                            nav.navigate("financeSeason/$seasonKey")
                        },
                        onOpenLicencesReport = { seasonKey ->
                            nav.navigate(Destinations.licencesReport(seasonKey))
                        },
                        onOpenCotisationsReport = { seasonKey ->
                            nav.navigate(Destinations.cotisationsReport(seasonKey))
                        },
                        onOpenSeasonClosure = { seasonKey ->
                            nav.navigate(Destinations.seasonClosure(seasonKey))
                        },
                        onOpenGuardians = {
                            nav.navigate(Destinations.GuardianList)
                        },
                        onEditGuardian = { guardianId ->
                            nav.navigate(Destinations.guardianEdit(guardianId))
                        }
                    )
                }
                else -> {
                    Log.d("NavLog", "Cotisations: ACCESS DENIED as ${role.name}")
                    AccessDeniedScreen(onBack = { nav.popBackStack() })
                }
            }
        }

        // Détail foyer
        composable(
            route = Destinations.CotisationHousehold,
            arguments = listOf(navArgument(Destinations.ArgGuardianId) { type = NavType.StringType })
        ) { backStack ->
            val gid = backStack.arguments?.getString(Destinations.ArgGuardianId)

            val roleVm: RoleViewModel = hiltViewModel()
            val role = roleVm.role.collectAsState().value

            when {
                gid == null -> {
                    Log.w("NavLog", "CotisationHousehold: guardianId MISSING")
                    AccessDeniedScreen(onBack = { nav.popBackStack() })
                }
                role == null -> {
                    Log.d("NavLog", "CotisationHousehold: role=null (loading)")
                    CircularProgressIndicator()
                }
                role.isFinanceManager() -> {
                    Log.d("NavLog", "CotisationHousehold: allowed as ${role.name}, gid=$gid")
                    CotisationHouseholdScreen(
                        guardianId = gid,
                        seasonKey = SeasonUtils.currentSeasonKey(),
                        onBack = { nav.popBackStack() },
                        onEditGuardian = { id -> nav.navigate(Destinations.guardianEdit(id)) }
                    )
                }
                else -> {
                    Log.d("NavLog", "CotisationHousehold: ACCESS DENIED as ${role.name}, gid=$gid")
                    AccessDeniedScreen(onBack = { nav.popBackStack() })
                }
            }
        }

        // ✅ Rapport cotisations
        composable(
            route = Destinations.CotisationsReport,
            arguments = listOf(navArgument(Destinations.ArgSeasonKey) { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonKey = backStackEntry.arguments?.getString(Destinations.ArgSeasonKey)
                ?: SeasonUtils.currentSeasonKey()

            val roleVm: RoleViewModel = hiltViewModel()
            val role = roleVm.role.collectAsState().value

            when {
                role == null -> CircularProgressIndicator()
                role.isFinanceManager() -> {
                    CotisationsReportScreen(
                        seasonKey = seasonKey,
                        onBack = { nav.popBackStack() }
                    )
                }
                else -> AccessDeniedScreen(onBack = { nav.popBackStack() })
            }
        }

        // Rapport licences FFK
        composable(
            route = Destinations.LicencesReport,
            arguments = listOf(navArgument(Destinations.ArgSeasonKey) { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonKey = backStackEntry.arguments?.getString(Destinations.ArgSeasonKey)
                ?: SeasonUtils.currentSeasonKey()

            val roleVm: RoleViewModel = hiltViewModel()
            val role = roleVm.role.collectAsState().value

            when {
                role == null -> {
                    CircularProgressIndicator()
                }
                role.isFinanceManager() -> {
                    LicencesReportScreen(
                        seasonKey = seasonKey,
                        onBack = { nav.popBackStack() }
                    )
                }
                else -> {
                    AccessDeniedScreen(onBack = { nav.popBackStack() })
                }
            }
        }

        // Config saison
        composable(
            route = "financeSeason/{seasonKey}",
            arguments = listOf(navArgument("seasonKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonKey = backStackEntry.arguments?.getString("seasonKey")
                ?: SeasonUtils.currentSeasonKey()
            FinanceSeasonScreen(
                seasonKey = seasonKey,
                onBack = { nav.popBackStack() },
                onOpenAgCr = { nav.navigate(Destinations.bureauAgCr(seasonKey)) }
            )
        }

        composable(
            route = Destinations.SeasonClosure,
            arguments = listOf(navArgument(Destinations.ArgSeasonKey) { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonKey = backStackEntry.arguments?.getString(Destinations.ArgSeasonKey)
                ?: com.antechrist.adherentsapp.core.utils.SeasonUtils.currentSeasonKey()

            val roleVm: RoleViewModel = hiltViewModel()
            val role = roleVm.role.collectAsState().value

            when {
                role == null -> CircularProgressIndicator()
                role.isSuperAdmin() -> {
                    com.antechrist.adherentsapp.ui.screens.finance.SeasonClosureScreen(
                        seasonKey = seasonKey,
                        onBack = { nav.popBackStack() }
                    )
                }
                else -> AccessDeniedScreen(onBack = { nav.popBackStack() })
            }
        }

        composable(Destinations.GuardianList) {
            GuardianListScreen(
                onBack = { nav.popBackStack() },
                onEditGuardian = { gid ->
                    nav.navigate(Destinations.guardianEdit(gid))
                }
            )
        }

        // 🔹 Écran d’édition du responsable
        composable(
            route = Destinations.GuardianEdit,
            arguments = listOf(navArgument(Destinations.ArgGuardianId) { type = NavType.StringType })
        ) { backStack ->
            val gid = backStack.arguments?.getString(Destinations.ArgGuardianId)!!
            GuardianEditScreen(
                guardianId = gid,
                onClose = { nav.popBackStack() }
            )
        }

        // Notification et Messagerie
        composable(Destinations.InfoMessages) { backStackEntry ->

            val roleVm: RoleViewModel = hiltViewModel(backStackEntry)
            LaunchedEffect(Unit) { roleVm.refresh() }
            val role by roleVm.role.collectAsState()

            // UID Firebase courant
            val currentUid = remember {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
            }

            // Audience utilisateur (placeholder pour le moment)
            val isAdultPractitioner = true
            val isGuardian = false

            // Peut publier ? (doit matcher tes règles)
            val canPublish = remember(role) {
                when {
                    role == null -> false
                    role!!.isStaff() -> true
                    role == Role.SUPER_ADMIN -> true
                    role == Role.ADMIN -> true
                    else -> false
                }
            }

            // LOG NAVGRAPH
            LaunchedEffect(role, currentUid, canPublish, isAdultPractitioner, isGuardian) {
                Log.d(
                    "InfoNav",
                    "Nav -> InfoMessagesScreen { uid=$currentUid, role=${role?.name}, canPublish=$canPublish, isAdultPractitioner=$isAdultPractitioner, isGuardian=$isGuardian }"
                )
            }

            InfoMessagesScreen(
                onBack = { nav.popBackStack() },
                onNavigateToGroups = { nav.navigate(Destinations.NotificationGroups) },
                currentUserIsAdultPraticiant = isAdultPractitioner,
                currentUserIsGuardian = isGuardian,
                currentUserCanPublish = canPublish,
                currentUserUid = currentUid
            )
        }
        // 🆕 Route vers les groupes de notifications
        composable(Destinations.NotificationGroups) {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

            com.antechrist.adherentsapp.ui.screens.notifications.NotificationGroupsScreen(
                onBack = { nav.popBackStack() },
                currentUserUid = currentUid
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // ESPACE PARENT
        // ═══════════════════════════════════════════════════════════════

        // Écran principal : sélection des enfants du foyer
        composable(Destinations.ParentHouseholdHome) {
            ParentHouseholdHomeScreen(
                onSelectAdherent = { adherentId ->
                    nav.navigate(Destinations.parentAdherentProfile(adherentId))
                },
                onOpenPayment = {
                    nav.navigate(Destinations.ParentPayment)
                },
                onOpenMessages = {
                    nav.navigate(Destinations.ParentMessages)
                },
                onOpenProfile = {
                    nav.navigate(Destinations.ParentProfile)
                },
                onLogout = {
                    nav.navigate(Destinations.Login) { popUpTo(0) }
                }
            )
        }

        // Profil d'un adhérent (enfant) - PLACEHOLDER
        composable(
            route = Destinations.ParentAdherentProfile,
            arguments = listOf(navArgument(Destinations.ArgAdherentId) { type = NavType.StringType })
        ) { backStackEntry ->
            val adherentId = backStackEntry.arguments?.getString(Destinations.ArgAdherentId) ?: ""

            // TODO: Remplacer par ParentAdherentProfileScreen
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Profil adhérent") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Retour")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Profil adhérent")
                        Text("ID: $adherentId", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Text("Écran à créer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Cotisations du foyer - PLACEHOLDER
        composable(Destinations.ParentPayment) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Cotisations") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Retour")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cotisations du foyer")
                        Spacer(Modifier.height(16.dp))
                        Text("Écran à créer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Messages du club - PLACEHOLDER
        composable(Destinations.ParentMessages) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Messages") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Retour")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Messages du club")
                        Spacer(Modifier.height(16.dp))
                        Text("Écran à créer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Profil parent - PLACEHOLDER
        composable(Destinations.ParentProfile) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mon profil") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Retour")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mon profil parent")
                        Spacer(Modifier.height(16.dp))
                        Text("Écran à créer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BureauPlaceholderScreen(
    title: String,
    subtitle: String
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp)
        ) {
            Text(text = subtitle)
        }
    }
}
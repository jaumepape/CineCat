package cat.cinecat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cat.cinecat.data.AppContainer
import cat.cinecat.ui.catalog.CatalogScreen
import cat.cinecat.ui.catalog.CatalogViewModel
import cat.cinecat.ui.components.Icons
import cat.cinecat.ui.movie.MovieScreen
import cat.cinecat.ui.movie.MovieViewModel
import cat.cinecat.ui.profile.ProfileScreen
import cat.cinecat.ui.profile.ProfileViewModel
import cat.cinecat.ui.theme.CineCatTheme
import cat.cinecat.ui.theme.CineColors
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.serialization.Serializable

// Rutes de navegació amb tipus: els arguments (l'id de la fitxa) viatgen
// com a camps d'una classe serialitzable, no com a text en una URL.
@Serializable data class CatalogRoute(val focusSearch: Boolean = false)
@Serializable data class MovieRoute(val id: String)
@Serializable data object ProfileRoute

/**
 * Arrel de l'app, COMPARTIDA entre Android i iOS (Compose Multiplatform).
 * Cada plataforma només crida App() amb el seu AppContainer (que porta el
 * TokenStorage natiu).
 */
@Composable
fun App(container: AppContainer) {
    // Coil (imatges) baixa els pòsters amb Ktor, el mateix client HTTP
    // multiplataforma que fem servir per a l'API.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    CineCatTheme {
        val nav = rememberNavController()
        val session by container.session.session.collectAsStateWithLifecycle()
        val backStack by nav.currentBackStackEntryAsState()
        val destination = backStack?.destination
        val onCatalog = destination?.hasRoute(CatalogRoute::class) == true
        val searching = onCatalog && backStack?.toRoute<CatalogRoute>()?.focusSearch == true

        // El ViewModel del catàleg viu a nivell d'app: en tornar d'una fitxa,
        // el catàleg ja és a punt (i es refresca si s'ha valorat).
        val catalogViewModel = viewModel { CatalogViewModel(container.api) }

        fun goTab(route: Any) = nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }

        Scaffold(
            containerColor = CineColors.Bg,
            // Cap marge automàtic de les barres del sistema: cada pantalla
            // decideix. Així el pòster de la fitxa arriba fins a dalt, per
            // sota de la barra d'estat (edge-to-edge), com al disseny.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // La fitxa ocupa tota la pantalla (té el seu CTA a baix).
                if (destination?.hasRoute(MovieRoute::class) != true) {
                    NavigationBar(containerColor = CineColors.Surface, tonalElevation = 0.dp) {
                        TabItem("Catàleg", Icons.Home, onCatalog && !searching) { goTab(CatalogRoute()) }
                        TabItem("Cerca", Icons.Search, searching) { goTab(CatalogRoute(focusSearch = true)) }
                        TabItem("Perfil", Icons.Person, destination?.hasRoute(ProfileRoute::class) == true) { goTab(ProfileRoute) }
                    }
                }
            },
        ) { padding ->
            NavHost(nav, startDestination = CatalogRoute(), modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
                composable<CatalogRoute> { entry ->
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        CatalogScreen(
                            viewModel = catalogViewModel,
                            api = container.api,
                            focusSearch = entry.toRoute<CatalogRoute>().focusSearch,
                            onMovieClick = { nav.navigate(MovieRoute(it)) },
                        )
                    }
                }
                composable<MovieRoute> { entry ->
                    val id = entry.toRoute<MovieRoute>().id
                    val vm = viewModel(key = id) { MovieViewModel(container.api, id) }
                    MovieScreen(
                        viewModel = vm,
                        api = container.api,
                        session = session,
                        onBack = { nav.popBackStack() },
                        onLogin = { nav.navigate(ProfileRoute) },
                        onRated = { catalogViewModel.refresh() },
                    )
                }
                composable<ProfileRoute> {
                    val vm = viewModel { ProfileViewModel(container.api, container.session) }
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        ProfileScreen(
                            viewModel = vm,
                            session = session,
                            // Si venia d'una fitxa (per valorar amb l'àlies), hi torna.
                            onLoggedIn = { if (nav.previousBackStackEntry?.destination?.hasRoute(MovieRoute::class) == true) nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CineColors.Accent,
            selectedTextColor = CineColors.Accent,
            indicatorColor = CineColors.Accent15,
            unselectedIconColor = CineColors.TextDim,
            unselectedTextColor = CineColors.TextDim,
        ),
    )
}

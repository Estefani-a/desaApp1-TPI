package com.example.uniride.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.feed.FeedRoute
import com.example.uniride.presentation.home.HomeRoute
import com.example.uniride.presentation.profile.ProfileRoute
import com.example.uniride.presentation.publish.PublishTripRoute
import kotlin.reflect.KClass

/** Tabs of the bottom bar. Each mode shows its own tabs. */
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val modes: Set<AppMode>,
) {
    HOME(HomeRoute, R.drawable.ic_nav_home, R.string.nav_home, AppMode.entries.toSet()),
    FEED(FeedRoute, R.drawable.ic_nav_search, R.string.nav_feed, setOf(AppMode.PASAJERO)),
    PUBLISH(PublishTripRoute, R.drawable.ic_nav_add, R.string.nav_publish, setOf(AppMode.CONDUCTOR)),
    PROFILE(ProfileRoute, R.drawable.ic_nav_person, R.string.nav_profile, AppMode.entries.toSet()),
    ;

    /** Used to know whether a destination of the back stack is this tab. */
    val routeClass: KClass<*> get() = route::class

    companion object {
        fun forMode(mode: AppMode): List<TopLevelDestination> = entries.filter { mode in it.modes }
    }
}

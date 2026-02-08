package com.antechrist.adherentsapp.core.debug

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController

private const val TAG = "NavLog"

private fun Bundle?.toLog(): String {
    if (this == null || isEmpty) return "{}"
    return keySet().joinToString(prefix = "{", postfix = "}") { k ->
        val v = get(k)
        "$k=${v?.toString() ?: "null"}"
    }
}

/** Use only public info to label a destination. */
private fun NavDestination.routeOrId(): String =
    this.route ?: "id:${this.id}"

/**
 * Best-effort stack dump:
 * 1) Try reflection on controller.backQueue (not public API on some versions)
 * 2) Fallback to prev/current using only public APIs
 */
private fun NavController.backStackRoutesSafe(): String {
    try {
        val field = NavController::class.java.getDeclaredField("backQueue")
        field.isAccessible = true
        val deque = field.get(this) as? Iterable<*>
        val items = deque?.mapNotNull { it as? NavBackStackEntry } ?: emptyList()
        return items.joinToString(prefix = "[", postfix = "]") { e ->
            e.destination.routeOrId()
        }
    } catch (_: Throwable) {
        val cur = currentDestination?.routeOrId() ?: "?"
        val prev = previousBackStackEntry?.destination?.routeOrId() ?: "none"
        return "[prev=$prev, current=$cur]"
    }
}

fun attachNavLogger(nav: NavHostController) {
    nav.addOnDestinationChangedListener { controller: NavController, dest: NavDestination, args: Bundle? ->
        Log.d(TAG, "➡️ onDestinationChanged: route=${dest.routeOrId()} args=${args.toLog()} stack=${controller.backStackRoutesSafe()}")
    }
}

fun logNavigateAttempt(route: String, reason: String? = null) {
    Log.d(TAG, "🧭 navigate(route=$route) ${reason?.let { "— $it" } ?: ""}")
}

fun logGuard(route: String, allowed: Boolean, role: String?) {
    Log.d(TAG, "🛡️ guard(route=$route) allowed=$allowed role=$role")
}

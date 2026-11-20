package com.kmail.thekami.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val IN = 300
private const val OUT = 200
private const val FADE_OUT = 90
private const val FADE_IN = 210

private enum class Kind { PAGE, DETAIL, COMPOSE }

private fun NavBackStackEntry.kind(): Kind = when (destination.route) {
    Routes.READER -> Kind.DETAIL
    Routes.COMPOSE -> Kind.COMPOSE
    else -> Kind.PAGE
}

/**
 * Pages fade through each other with a short slide, a message slides in from the side, and the
 * composer rises from the bottom. Sideways motion follows the reading direction, so it is mirrored
 * for right-to-left languages. When the system animation scale is off, every screen change is instant.
 */
object KmailTransitions {
    fun enter(reduce: Boolean): Scope.() -> EnterTransition = {
        if (reduce) {
            EnterTransition.None
        } else {
            when (targetState.kind()) {
                Kind.COMPOSE -> slideInVertically(tween(IN, easing = Decelerate)) { it / 6 } + fadeIn(tween(IN))
                Kind.DETAIL -> slideIntoContainer(SlideDirection.Start, tween(IN, easing = Decelerate)) { it / 5 } +
                    fadeIn(tween(IN))
                Kind.PAGE -> slideIntoContainer(SlideDirection.Start, tween(IN, easing = Decelerate)) { it / 14 } +
                    fadeIn(tween(FADE_IN, delayMillis = FADE_OUT))
            }
        }
    }

    fun exit(reduce: Boolean): Scope.() -> ExitTransition = {
        if (reduce) {
            ExitTransition.None
        } else {
            when (targetState.kind()) {
                Kind.COMPOSE -> fadeOut(tween(OUT))
                Kind.DETAIL -> slideOutOfContainer(SlideDirection.Start, tween(OUT, easing = Accelerate)) { it / 8 } +
                    fadeOut(tween(OUT))
                Kind.PAGE -> slideOutOfContainer(SlideDirection.Start, tween(OUT, easing = Accelerate)) { it / 14 } +
                    fadeOut(tween(FADE_OUT))
            }
        }
    }

    fun popEnter(reduce: Boolean): Scope.() -> EnterTransition = {
        if (reduce) {
            EnterTransition.None
        } else {
            when (initialState.kind()) {
                Kind.COMPOSE -> fadeIn(tween(IN))
                Kind.DETAIL -> slideIntoContainer(SlideDirection.End, tween(IN, easing = Decelerate)) { it / 8 } +
                    fadeIn(tween(IN))
                Kind.PAGE -> slideIntoContainer(SlideDirection.End, tween(IN, easing = Decelerate)) { it / 14 } +
                    fadeIn(tween(FADE_IN, delayMillis = FADE_OUT))
            }
        }
    }

    fun popExit(reduce: Boolean): Scope.() -> ExitTransition = {
        if (reduce) {
            ExitTransition.None
        } else {
            when (initialState.kind()) {
                Kind.COMPOSE -> slideOutVertically(tween(OUT, easing = Accelerate)) { it / 6 } + fadeOut(tween(OUT))
                Kind.DETAIL -> slideOutOfContainer(SlideDirection.End, tween(OUT, easing = Accelerate)) { it / 5 } +
                    fadeOut(tween(OUT))
                Kind.PAGE -> slideOutOfContainer(SlideDirection.End, tween(OUT, easing = Accelerate)) { it / 14 } +
                    fadeOut(tween(FADE_OUT))
            }
        }
    }
}

package com.kmail.thekami.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val IN = 300
private const val OUT = 200

private enum class Kind { PAGE, DETAIL, COMPOSE }

private fun NavBackStackEntry.kind(): Kind = when (destination.route) {
    Routes.READER -> Kind.DETAIL
    Routes.COMPOSE -> Kind.COMPOSE
    else -> Kind.PAGE
}

/**
 * Detail screens (a message) slide sideways, the composer rises from the bottom, and everything
 * else cross-fades with a small scale. Going back plays the same motion in reverse.
 */
object KmailTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (targetState.kind()) {
            Kind.COMPOSE -> slideInVertically(tween(IN, easing = Decelerate)) { it / 6 } + fadeIn(tween(IN))
            Kind.DETAIL -> slideInHorizontally(tween(IN, easing = Decelerate)) { it / 5 } + fadeIn(tween(IN))
            Kind.PAGE -> fadeIn(tween(IN, delayMillis = 60)) +
                scaleIn(tween(IN, delayMillis = 60, easing = Decelerate), initialScale = 0.96f)
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (targetState.kind()) {
            Kind.COMPOSE -> fadeOut(tween(OUT))
            Kind.DETAIL -> slideOutHorizontally(tween(OUT, easing = Accelerate)) { -it / 8 } + fadeOut(tween(OUT))
            Kind.PAGE -> fadeOut(tween(120))
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (initialState.kind()) {
            Kind.COMPOSE -> fadeIn(tween(IN))
            Kind.DETAIL -> slideInHorizontally(tween(IN, easing = Decelerate)) { -it / 8 } + fadeIn(tween(IN))
            Kind.PAGE -> fadeIn(tween(IN, delayMillis = 60)) +
                scaleIn(tween(IN, delayMillis = 60, easing = Decelerate), initialScale = 1.03f)
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (initialState.kind()) {
            Kind.COMPOSE -> slideOutVertically(tween(OUT, easing = Accelerate)) { it / 6 } + fadeOut(tween(OUT))
            Kind.DETAIL -> slideOutHorizontally(tween(OUT, easing = Accelerate)) { it / 5 } + fadeOut(tween(OUT))
            Kind.PAGE -> fadeOut(tween(120)) + scaleOut(tween(OUT), targetScale = 0.96f)
        }
    }
}

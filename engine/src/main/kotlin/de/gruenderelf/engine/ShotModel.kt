package de.gruenderelf.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Geometric chance model. Coordinates use the same normalized pitch as LiveMatch:
 * x = touchline-to-touchline, y = home goal (0) to away goal (1).
 */
data class ShotContext(
    val x: Float,
    val y: Float,
    val attackingHome: Boolean,
    val type: ShotType = ShotType.BOX_SHOT,
    val pressure: Double = .45,
    val defendersNearby: Int = 1,
    val passQuality: Double = .55,
    val clearChance: Boolean = false,
    val counter: Boolean = false,
    val strongFoot: Boolean = true,
)

data class ShotGeometry(val distanceMeters: Double, val angleRadians: Double, val lateralMeters: Double)

object ShotModel {
    private const val PITCH_LENGTH = 105.0
    private const val PITCH_WIDTH = 68.0
    private const val GOAL_WIDTH = 7.32

    fun locationLabel(context: ShotContext): String {
        val g = geometry(context)
        val attackingY = if (context.attackingHome) context.y else 1f - context.y
        val longitudinalMeters = attackingY.coerceIn(0f, 1f) * PITCH_LENGTH
        val lateralMeters = kotlin.math.abs((context.x.coerceIn(0f, 1f) - .5f) * PITCH_WIDTH)
        val insideBox = longitudinalMeters <= 16.5 && lateralMeters <= 20.16
        val meters = g.distanceMeters.toInt().coerceAtLeast(1)
        return when {
            context.type == ShotType.PENALTY -> "Elfmeter · 11 m"
            insideBox && g.distanceMeters <= 8.5 -> "Kurze Distanz · ${meters} m"
            insideBox -> "Im Strafraum · ${meters} m"
            g.distanceMeters <= 23.5 -> "Vor dem Strafraum · ${meters} m"
            else -> "Distanz · ${meters} m"
        }
    }

    fun geometry(context: ShotContext): ShotGeometry {
        val lateral = (context.x.coerceIn(0f, 1f) - .5f) * PITCH_WIDTH
        val longitudinal = ((if (context.attackingHome) context.y else 1f - context.y).coerceIn(.004f, .996f) * PITCH_LENGTH)
        val distance = sqrt(lateral * lateral + longitudinal * longitudinal)
        val halfGoal = GOAL_WIDTH / 2.0
        val left = atan2(-halfGoal - lateral, longitudinal)
        val right = atan2(halfGoal - lateral, longitudinal)
        var angle = abs(right - left)
        if (angle > PI) angle = 2.0 * PI - angle
        return ShotGeometry(distance, angle.coerceIn(.01, PI), lateral)
    }

    /** Chance quality before shooter/keeper execution quality. Never capped at team level. */
    fun xg(context: ShotContext): Double {
        if (context.type == ShotType.PENALTY) return .76
        val g = geometry(context)
        // Location is deliberately the dominant term. This roughly produces:
        // 30m central ~.02-.03, box edge ~.12-.18, 11m central ~.30-.40, 6m ~.60+.
        val z = -.10 - .13 * g.distanceMeters + 1.50 * g.angleRadians
        var value = 1.0 / (1.0 + exp(-z))
        value *= when (context.type) {
            ShotType.LONG_RANGE -> .90
            ShotType.BOX_SHOT -> 1.00
            ShotType.CLOSE_RANGE -> 1.08
            ShotType.ONE_ON_ONE -> 1.20
            ShotType.HEADER -> .74
            ShotType.VOLLEY -> .80
            ShotType.CUTBACK -> 1.16
            ShotType.REBOUND -> 1.12
            ShotType.FREE_KICK -> .68
            ShotType.PENALTY -> 1.0
        }
        // The open-goal angle already penalizes wide positions; this additional lateral term
        // represents the harder body angle/near-post window at the same raw distance.
        value *= (1.0 - (kotlin.math.abs(g.lateralMeters) / (PITCH_WIDTH / 2.0)).coerceIn(0.0, 1.0) * .34).coerceIn(.66, 1.0)
        value *= (1.0 - context.pressure.coerceIn(0.0, 1.0) * .27)
        value *= Math.pow(.94, context.defendersNearby.coerceIn(0, 5).toDouble())
        value *= .88 + context.passQuality.coerceIn(0.0, 1.0) * .20
        if (context.clearChance) value *= 1.12
        if (context.counter) value *= 1.05
        return value.coerceIn(.004, .90)
    }

    /** Actual scoring probability after chance quality, shooter execution and goalkeeper quality. */
    fun goalProbability(xg: Double, shooter: Player, keeper: Player?, context: ShotContext): Double {
        val prime = shooter.messiMentored
        val elite = shooter.attributes.finishing > REGULAR_DEVELOPMENT_CAP || shooter.attributes.technique > REGULAR_DEVELOPMENT_CAP
        val finishing = (.96 + (shooter.attributes.finishing - 50) * .0033).coerceIn(.78, if (prime) 1.42 else 1.32)
        val technique = (.985 + (shooter.attributes.technique - 50) * .0013).coerceIn(.93, if (prime) 1.22 else 1.15)
        val consistency = (.99 + (shooter.hidden.consistency - 50) * .0010).coerceIn(.94, 1.06)
        val bigMoment = (.995 + (shooter.hidden.pressure - 50) * .0007).coerceIn(.96, 1.03)
        val form = (1.0 + (shooter.form - 6.5) * .022).coerceIn(.92, 1.08)
        val fitness = (.94 + shooter.fitness.coerceIn(20.0, 100.0) * .0006).coerceIn(.952, 1.0)
        val foot = if (context.type == ShotType.HEADER || context.strongFoot || shooter.foot == Foot.BOTH) 1.0 else .86
        val executionUnderPressure = (.96 + (1.0 - context.pressure.coerceIn(0.0, 1.0)) * .05).coerceIn(.96, 1.01)
        val keeperQuality = keeper?.let {
            it.attributes.keeping * .68 + it.hidden.consistency * .10 + it.hidden.pressure * .06 + it.sharpness * .08 + it.fitness.coerceIn(0.0, 100.0) * .05 + (it.form * 10.0).coerceIn(30.0, 90.0) * .03
        } ?: 20.0
        // Keeper quality must materially change finishing outcomes. Close-range chances stay dangerous,
        // while strong keepers now save a clearly larger share of otherwise identical shots.
        val keeperImpact = when (context.type) {
            ShotType.PENALTY -> .0033
            ShotType.CLOSE_RANGE, ShotType.CUTBACK, ShotType.REBOUND -> .0043
            ShotType.ONE_ON_ONE -> .0050
            else -> .0048
        }
        val normalKeeperQuality = keeperQuality.coerceAtMost(100.0)
        val eliteKeeperQuality = (keeperQuality - 100.0).coerceAtLeast(0.0)
        val anticipationQuality = keeper?.let { it.attributes.vision * .55 + it.attributes.pace * .25 + it.attributes.technique * .20 } ?: 50.0
        val sweeperBonus = when(context.type) {
            ShotType.ONE_ON_ONE -> (anticipationQuality - 55.0).coerceAtLeast(0.0) * .0011
            ShotType.CUTBACK, ShotType.REBOUND -> (anticipationQuality - 60.0).coerceAtLeast(0.0) * .0004
            else -> 0.0
        }
        val keeperModifier = (1.0 - (normalKeeperQuality - 50.0) * keeperImpact - eliteKeeperQuality * keeperImpact * .35 - sweeperBonus).coerceIn(.60, 1.24)
        val primeExecution = if (prime) 1.10 else 1.0
        val raw = xg * finishing * technique * consistency * bigMoment * form * fitness * foot * executionUnderPressure * keeperModifier * primeExecution
        val ceiling = when {
            prime -> minOf(.97, xg * 1.70 + .020)
            elite -> minOf(.95, xg * 1.52 + .012)
            else -> minOf(.92, xg * 1.34 + .006)
        }
        return raw.coerceIn(.002, ceiling)
    }

    /** Probability that a shot is blocked before reaching the goalkeeper. */
    fun blockProbability(context: ShotContext): Double {
        if (context.type == ShotType.PENALTY || context.type == ShotType.ONE_ON_ONE) return if (context.type == ShotType.PENALTY) 0.0 else .025
        var p = .055 + context.pressure.coerceIn(0.0, 1.0) * .13 + context.defendersNearby.coerceIn(0, 5) * .032
        p += when (context.type) {
            ShotType.FREE_KICK -> .10
            ShotType.LONG_RANGE -> .025
            ShotType.HEADER -> -.025
            ShotType.CLOSE_RANGE, ShotType.CUTBACK, ShotType.REBOUND -> -.02
            else -> 0.0
        }
        return p.coerceIn(.025, .38)
    }

    /** Total on-target probability, including goals, after accounting for blocks. */
    fun onTargetProbability(goalProbability: Double, shooter: Player, context: ShotContext): Double {
        val g = geometry(context)
        var raw = .35 + shooter.attributes.finishing * .0021 + shooter.attributes.technique * .0012
        raw += (g.angleRadians / 1.2).coerceIn(0.0, 1.0) * .09
        raw -= ((g.distanceMeters - 12.0).coerceAtLeast(0.0) * .0060).coerceAtMost(.17)
        raw -= context.pressure.coerceIn(0.0, 1.0) * .10
        if (!context.strongFoot && shooter.foot != Foot.BOTH && context.type != ShotType.HEADER) raw -= .055
        raw += when (context.type) {
            ShotType.CLOSE_RANGE -> .10
            ShotType.ONE_ON_ONE -> .09
            ShotType.PENALTY -> .22
            ShotType.CUTBACK, ShotType.REBOUND -> .06
            ShotType.HEADER -> -.035
            ShotType.VOLLEY -> -.055
            ShotType.FREE_KICK -> .015
            ShotType.LONG_RANGE -> -.035
            else -> 0.0
        }
        val block = blockProbability(context)
        if (shooter.messiMentored) raw += .075
        val available = 1.0 - block
        val elite = shooter.attributes.finishing > REGULAR_DEVELOPMENT_CAP || shooter.attributes.technique > REGULAR_DEVELOPMENT_CAP
        val targetCeiling=when{shooter.messiMentored->.96;elite->.93;else->.84}
        return maxOf(goalProbability + .025, raw.coerceIn(.22,targetCeiling) * available).coerceAtMost(available)
    }

    fun shotSpeed(shooter: Player, context: ShotContext): Double {
        var speed = .42 + shooter.attributes.finishing * .0024 + shooter.attributes.technique * .0012
        speed += when (context.type) { ShotType.VOLLEY, ShotType.LONG_RANGE, ShotType.FREE_KICK -> .09; ShotType.HEADER -> -.08; else -> 0.0 }
        if (shooter.messiMentored) speed += .065
        return speed.coerceIn(.35, if (shooter.messiMentored) .92 else .82)
    }
}

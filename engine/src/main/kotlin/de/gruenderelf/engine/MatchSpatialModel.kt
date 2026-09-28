package de.gruenderelf.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Pure spatial presentation model for the tactical top-down live view.
 *
 * Coordinates are the canonical LiveMatch pitch coordinates:
 * x = left touchline (0) -> right touchline (1)
 * y = left goal (0) -> right goal (1)
 *
 * MatchEngine currently uses HOME attacking toward y=0 and AWAY toward y=1.
 * This object is deliberately the single conversion point so the UI cannot
 * accidentally mirror one team or make both teams attack the same goal.
 */
data class MatchSpatialPlayer(
    val id: Int,
    val isHome: Boolean,
    val slotIndex: Int,
    val position: Position,
    val lateral: Float,
    val longitudinal: Float,
    val active: Boolean,
)

data class MatchSpatialFrame(
    val players: List<MatchSpatialPlayer>,
    val homeAttacksToward: Int = -1,
    val awayAttacksToward: Int = 1,
)

object MatchSpatialModel {
    fun progressFromOwnGoal(isHome: Boolean, absoluteY: Float): Float =
        (if (isHome) 1f - absoluteY else absoluteY).coerceIn(0f, 1f)

    fun absoluteY(isHome: Boolean, progressFromOwnGoal: Float): Float =
        (if (isHome) 1f - progressFromOwnGoal else progressFromOwnGoal).coerceIn(0f, 1f)

    /** Presentation-only side switch. Engine coordinates never change. */
    fun sidesSwitched(period: Int): Boolean = period == 2 || period == 4

    fun displayLongitudinal(period: Int, absoluteY: Float): Float =
        (if (sidesSwitched(period)) 1f - absoluteY else absoluteY).coerceIn(0f, 1f)

    /**
     * Direction on the rendered horizontal pitch: -1 = left, +1 = right.
     * HOME attacks engine y=0; AWAY attacks y=1. After the interval the
     * presentation rotates the ends so the teams visibly change sides.
     */
    fun screenAttackDirection(isHome: Boolean, period: Int): Int {
        val canonical = if (isHome) -1 else 1
        return if (sidesSwitched(period)) -canonical else canonical
    }

    private fun ownerClubId(m: LiveMatch): Int =
        m.liveClubId.takeIf { it == m.homeId || it == m.awayId }
            ?: m.chainOwnerClubId.takeIf { it == m.homeId || it == m.awayId }
            ?: if (m.homeInPossession) m.homeId else m.awayId

    private fun club(w: World, m: LiveMatch, isHome: Boolean) =
        w.clubs.getValue(if (isHome) m.homeId else m.awayId)

    private fun xi(m: LiveMatch, isHome: Boolean) = if (isHome) m.homeXi else m.awayXi
    private fun formation(m: LiveMatch, isHome: Boolean) = if (isHome) m.homeFormation else m.awayFormation
    private fun coords(m: LiveMatch, isHome: Boolean) = Formations.coordinates(formation(m, isHome))
    private fun slots(m: LiveMatch, isHome: Boolean) = Formations.positions(formation(m, isHome))
    private fun hasBall(m: LiveMatch, isHome: Boolean) = ownerClubId(m) == if (isHome) m.homeId else m.awayId

    private fun effectiveLine(w: World, m: LiveMatch, isHome: Boolean): Int {
        val c = club(w, m, isHome)
        return when {
            (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) -> 1
            (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) -> 5
            (if (isHome) m.homeControlGame else m.awayControlGame) -> 2
            else -> c.tactics.line
        }.coerceIn(1, 5)
    }

    private fun effectivePressing(w: World, m: LiveMatch, isHome: Boolean): Int {
        val c = club(w, m, isHome)
        return when {
            (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) -> 1
            (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) -> 5
            (if (isHome) m.homeControlGame else m.awayControlGame) -> 2
            else -> c.tactics.pressing
        }.coerceIn(1, 5)
    }

    private fun effectiveWidth(w: World, m: LiveMatch, isHome: Boolean): Int {
        val c = club(w, m, isHome)
        val base = c.tactics.width.coerceIn(1, 5)
        return when {
            (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) -> base.coerceAtLeast(4)
            (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) -> base.coerceAtMost(3)
            else -> base
        }
    }

    private fun effectiveMentality(m: LiveMatch, isHome: Boolean): Int =
        (if (isHome) m.homeMentality else m.awayMentality).coerceIn(1, 5)

    private fun slotPosition(w: World, m: LiveMatch, isHome: Boolean, index: Int, id: Int): Position =
        slots(m, isHome).getOrElse(index) { w.players[id]?.position ?: Position.ZM }

    private fun role(w: World, m: LiveMatch, isHome: Boolean, id: Int): PlayerRole =
        club(w, m, isHome).tactics.roles[id] ?: w.players[id]?.role ?: PlayerRole.AUTO

    private fun instructions(w: World, m: LiveMatch, isHome: Boolean, id: Int): List<PlayerInstruction> =
        club(w, m, isHome).tactics.instructions[id]?.toList() ?: emptyList()

    /** Local formation axis: 0 = own goal, 1 = opponent goal. */
    private fun formationBase(m: LiveMatch, isHome: Boolean, index: Int): Pair<Float, Float> {
        val base = coords(m, isHome).getOrNull(index) ?: (.5f to .5f)
        return base.first to (1f - base.second)
    }

    private fun depthNorm(m: LiveMatch, isHome: Boolean, index: Int): Float {
        val depth = formationBase(m, isHome, index).second
        return ((depth - .29f) / .59f).coerceIn(0f, 1f)
    }

    private fun phaseTeamLength(phase: LivePhase): Float = when (phase) {
        LivePhase.DANGEROUS_ATTACK -> .29f
        LivePhase.ATTACK -> .34f
        LivePhase.COUNTER -> .52f
        LivePhase.CORNER -> .24f
        LivePhase.DANGEROUS_FREE_KICK -> .29f
        LivePhase.FREE_KICK -> .37f
        else -> .44f
    }

    private fun attackingBackLine(w: World, m: LiveMatch, isHome: Boolean, ballProgress: Float): Float {
        val line = effectiveLine(w, m, isHome)
        val mentality = effectiveMentality(m, isHome)
        val lineBias = (line - 3) * .018f
        val mentalityBias = (mentality - 3) * .012f

        // The back line follows the attack as a unit. Counters are intentionally
        // stretched; settled attacks and dangerous attacks push the rest-defence
        // much higher. This is the key rule that prevents the defence from
        // staying beside its own box while the attack is at the other end.
        var back = when (m.livePhase) {
            LivePhase.COUNTER -> .16f + ballProgress * .24f
            LivePhase.ATTACK -> .20f + ballProgress * .38f
            LivePhase.DANGEROUS_ATTACK -> .31f + ballProgress * .31f
            LivePhase.CORNER -> .52f
            LivePhase.DANGEROUS_FREE_KICK -> .47f
            LivePhase.FREE_KICK -> .26f + ballProgress * .30f
            else -> .15f + ballProgress * .32f
        } + lineBias + mentalityBias

        when (m.livePhase) {
            LivePhase.DANGEROUS_ATTACK -> {
                if (ballProgress >= .62f) back = max(back, .44f + (ballProgress - .62f) * .36f)
                if (ballProgress >= .78f) back = max(back, .52f)
            }
            LivePhase.ATTACK -> if (ballProgress >= .58f) {
                back = max(back, .34f + (ballProgress - .58f) * .33f)
            }
            LivePhase.CORNER -> back = max(back, .52f)
            LivePhase.DANGEROUS_FREE_KICK -> back = max(back, .47f)
            else -> Unit
        }

        if (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) back += .050f
        if (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) back -= .050f
        return back.coerceIn(.13f, .66f)
    }

    private fun defendingBackLine(w: World, m: LiveMatch, isHome: Boolean, ballProgress: Float): Float {
        val line = effectiveLine(w, m, isHome)
        val pressing = effectivePressing(w, m, isHome)
        val lineBias = (line - 3) * .018f

        // ballProgress is measured from THIS team's own goal. Small values are
        // therefore dangerous for the defending side.
        var back = when {
            ballProgress < .18f -> .115f + ballProgress * .14f
            ballProgress < .34f -> .14f + (ballProgress - .18f) * .55f
            ballProgress < .55f -> .23f + (ballProgress - .34f) * .72f
            else -> .38f + (ballProgress - .55f) * .42f
        } + lineBias

        // High pressing only moves the line up when the ball is safely away from
        // goal. Near the own box, defenders preserve the line instead of all
        // charging the ball carrier.
        if (ballProgress > .55f) back += (pressing - 3).coerceAtLeast(0) * .012f
        if (m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress <= .25f) {
            back = min(back, .155f + ballProgress * .10f)
        }
        if (m.livePhase == LivePhase.COUNTER && ballProgress <= .38f) back -= .018f

        return back.coerceIn(.085f, .60f)
    }

    private fun backLine(w: World, m: LiveMatch, isHome: Boolean, ballProgress: Float): Float =
        if (hasBall(m, isHome)) attackingBackLine(w, m, isHome, ballProgress)
        else defendingBackLine(w, m, isHome, ballProgress)

    private fun frontLine(
        w: World,
        m: LiveMatch,
        isHome: Boolean,
        ballProgress: Float,
        back: Float,
    ): Float {
        return if (hasBall(m, isHome)) {
            val ahead = when (m.livePhase) {
                LivePhase.COUNTER -> .12f
                LivePhase.DANGEROUS_ATTACK -> .045f
                LivePhase.ATTACK -> .065f
                LivePhase.CORNER -> .020f
                LivePhase.DANGEROUS_FREE_KICK -> .035f
                else -> .075f
            }
            val minimumLength = when (m.livePhase) {
                LivePhase.COUNTER -> .40f
                LivePhase.DANGEROUS_ATTACK -> .30f
                LivePhase.ATTACK -> .34f
                LivePhase.CORNER -> .34f
                else -> .36f
            }
            max(back + minimumLength, ballProgress + ahead).coerceIn(.43f, .975f)
        } else {
            val length = when {
                m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress < .30f -> .30f
                m.livePhase == LivePhase.COUNTER -> .38f
                m.livePhase == LivePhase.ATTACK -> .34f
                else -> .36f
            }
            (back + length).coerceAtMost(.82f)
        }
    }

    private fun skeleton(
        w: World,
        m: LiveMatch,
        isHome: Boolean,
        index: Int,
        id: Int,
        ballX: Float,
        ballY: Float,
    ): Pair<Float, Float> {
        val owns = hasBall(m, isHome)
        val ballProgress = progressFromOwnGoal(isHome, ballY)
        val (baseLat, _) = formationBase(m, isHome, index)
        val pos = slotPosition(w, m, isHome, index, id)
        val width = effectiveWidth(w, m, isHome)
        val back = backLine(w, m, isHome, ballProgress)
        val front = frontLine(w, m, isHome, ballProgress, back)
        val keeper = pos == Position.TW || index == 0
        val r = role(w, m, isHome, id)
        val ins = instructions(w, m, isHome, id)

        if (keeper) {
            var progress = .045f + back * .36f
            if (r == PlayerRole.BUILD_UP_KEEPER) progress += .030f
            progress = min(progress, back - .115f).coerceIn(.04f, .29f)
            val lat = (.5f + (ballX - .5f) * .22f).coerceIn(.35f, .65f)
            return lat to progress
        }

        val norm = depthNorm(m, isHome, index)
        var progress = back + (front - back) * norm

        // In possession keep more width; without the ball compress around the
        // centre and shift as a block to the ball side.
        val widthScale = if (owns) .80f + width * .065f else .58f + width * .050f
        val shiftToBall = if (owns) .12f else .25f
        val center = .5f + (ballX - .5f) * shiftToBall
        var lateral = center + (baseLat - .5f) * widthScale

        val leftSide = baseLat < .5f
        val ballLeft = ballX < .5f
        val sameSide = leftSide == ballLeft

        if (owns) {
            // Rest defence and line spacing in possession.
            when (pos) {
                Position.IV -> progress = back
                Position.DM -> progress = max(progress, back + (front - back) * .29f)
                Position.ZM -> progress = max(progress, back + (front - back) * .47f)
                Position.OM -> progress = max(progress, back + (front - back) * .68f)
                Position.ST -> progress = max(progress, back + (front - back) * .88f)
                Position.LA, Position.RA -> progress = max(progress, back + (front - back) * .73f)
                Position.LV, Position.RV -> {
                    val overlap = r == PlayerRole.OVERLAPPING_FULLBACK || PlayerInstruction.OVERLAP in ins
                    if (sameSide && overlap) {
                        progress = max(progress, back + (front - back) * .62f)
                        lateral += if (leftSide) -.035f else .035f
                    } else {
                        // Far-side/fullback rest-defence: narrow and only a little
                        // ahead of the centre-backs instead of sitting on the own box.
                        progress = max(back + .045f, min(progress, back + (front - back) * .34f))
                        if (!sameSide) lateral += (.5f - lateral) * .22f
                    }
                }
                Position.TW -> Unit
            }

            if (m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress >= .68f) {
                // Hard minimums for an established final-third attack.
                when (pos) {
                    Position.IV -> progress = max(progress, .46f)
                    Position.LV, Position.RV -> progress = max(progress, .49f)
                    Position.DM -> progress = max(progress, .55f)
                    Position.ZM -> progress = max(progress, .61f)
                    Position.OM, Position.LA, Position.RA -> progress = max(progress, .70f)
                    Position.ST -> progress = max(progress, .76f)
                    Position.TW -> Unit
                }
            }
        } else {
            // Defensive block: preserve rows instead of pulling the whole line to
            // the ball. The far side narrows more strongly.
            when (pos) {
                Position.IV -> progress = back
                Position.LV, Position.RV -> progress = back + .012f
                Position.DM -> progress = back + (front - back) * .28f
                Position.ZM -> progress = back + (front - back) * .48f
                Position.OM, Position.LA, Position.RA -> progress = back + (front - back) * .68f
                Position.ST -> progress = back + (front - back) * .91f
                Position.TW -> Unit
            }
            if (!sameSide) lateral += (.5f - lateral) * .24f
        }

        return lateral.coerceIn(.045f, .955f) to progress.coerceIn(.065f, .975f)
    }

    private fun supportIndexes(w: World, m: LiveMatch, isHome: Boolean, ballX: Float, ballY: Float): Set<Int> {
        if (!hasBall(m, isHome)) return emptySet()
        val list = xi(m, isHome)
        val ballProgress = progressFromOwnGoal(isHome, ballY)
        return list.indices.filter { idx ->
            val id = list[idx]
            if (id == 0 || id == m.livePlayerId || id in m.sentOff || id in m.injured) false
            else {
                val pos = slotPosition(w, m, isHome, idx, id)
                val r = role(w, m, isHome, id)
                val ins = instructions(w, m, isHome, id)
                val allowedByRole = when (pos) {
                    Position.TW -> false
                    Position.IV -> m.livePhase == LivePhase.POSSESSION && r == PlayerRole.BALL_PLAYING_CB
                    Position.LV, Position.RV -> r in setOf(PlayerRole.OVERLAPPING_FULLBACK, PlayerRole.INVERTED_FULLBACK) ||
                        PlayerInstruction.OVERLAP in ins
                    else -> true
                }
                allowedByRole && PlayerInstruction.HOLD_POSITION !in ins
            }
        }.sortedBy { idx ->
            val id = list[idx]
            val p = skeleton(w, m, isHome, idx, id, ballX, ballY)
            val dx = p.first - ballX
            val dy = p.second - ballProgress
            val pos = slotPosition(w, m, isHome, idx, id)
            dx * dx + dy * dy + if (pos in setOf(Position.DM, Position.ZM, Position.OM, Position.LA, Position.RA)) -.035f else 0f
        }.take(if (m.livePhase == LivePhase.DANGEROUS_ATTACK) 3 else 2).toSet()
    }

    private fun runnerIndexes(w: World, m: LiveMatch, isHome: Boolean): Set<Int> {
        if (!hasBall(m, isHome)) return emptySet()
        val list = xi(m, isHome)
        val count = when (m.livePhase) {
            LivePhase.DANGEROUS_ATTACK, LivePhase.COUNTER -> 3
            LivePhase.ATTACK -> 2
            else -> 1
        }
        return list.indices.filter { idx ->
            val id = list[idx]
            if (id == 0 || id == m.livePlayerId || id in m.sentOff || id in m.injured) false
            else {
                val pos = slotPosition(w, m, isHome, idx, id)
                val r = role(w, m, isHome, id)
                val ins = instructions(w, m, isHome, id)
                val forwardRunner = pos in setOf(Position.ST, Position.LA, Position.RA, Position.OM)
                val fullbackRunner = pos in setOf(Position.LV, Position.RV) &&
                    (r == PlayerRole.OVERLAPPING_FULLBACK || PlayerInstruction.OVERLAP in ins)
                (forwardRunner || fullbackRunner) && PlayerInstruction.HOLD_POSITION !in ins
            }
        }.sortedByDescending { idx ->
            val id = list[idx]
            val p = w.players[id]
            val r = role(w, m, isHome, id)
            val ins = instructions(w, m, isHome, id)
            depthNorm(m, isHome, idx) * 100f +
                (p?.attributes?.pace ?: 50) * .12f +
                (if (PlayerInstruction.RUN_IN_BEHIND in ins) 18f else 0f) +
                (if (r in setOf(PlayerRole.OVERLAPPING_FULLBACK, PlayerRole.WINGER, PlayerRole.INSIDE_FORWARD, PlayerRole.POACHER)) 7f else 0f) +
                (if (slotPosition(w, m, isHome, idx, id) == Position.ST) 6f else 0f)
        }.take(count).toSet()
    }

    private fun pressers(w: World, m: LiveMatch, isHome: Boolean, ballX: Float, ballY: Float): Set<Int> {
        if (hasBall(m, isHome)) return emptySet()
        val list = xi(m, isHome)
        val ballProgress = progressFromOwnGoal(isHome, ballY)
        var count = when (effectivePressing(w, m, isHome)) {
            1 -> 1
            2, 3 -> 2
            else -> 3
        }
        if (ballProgress < .20f) count = 1
        else if (ballProgress < .30f) count = min(count, 2)

        return list.indices.filter { idx ->
            val id = list[idx]
            id != 0 && id !in m.sentOff && id !in m.injured &&
                slotPosition(w, m, isHome, idx, id) != Position.TW
        }.sortedBy { idx ->
            val id = list[idx]
            val p = skeleton(w, m, isHome, idx, id, ballX, ballY)
            val dx = p.first - ballX
            val dy = p.second - ballProgress
            val pos = slotPosition(w, m, isHome, idx, id)
            val r = role(w, m, isHome, id)
            val ins = instructions(w, m, isHome, id)
            var score = dx * dx + dy * dy

            // Midfield/forwards should normally initiate pressure. Centre-backs
            // only step out when the ball is genuinely in their zone.
            score += when (pos) {
                Position.IV -> when {
                    r == PlayerRole.STOPPER && ballProgress < .42f -> .035f
                    ballProgress < .16f -> .055f
                    else -> .190f
                }
                Position.LV, Position.RV -> .065f
                Position.DM -> -.055f
                Position.ZM -> -.035f
                Position.OM, Position.LA, Position.RA -> -.020f
                Position.ST -> -.010f
                Position.TW -> .30f
            }
            if (r == PlayerRole.PRESSING_FORWARD) score -= .055f
            if (PlayerInstruction.PRESS_MORE in ins) score -= .060f
            if (PlayerInstruction.PRESS_LESS in ins) score += .140f
            score
        }.take(count).toSet()
    }

    fun frame(w: World, m: LiveMatch, ballX: Float = m.ballX, ballY: Float = m.ballY): MatchSpatialFrame {
        val bx = ballX.coerceIn(.025f, .975f)
        val by = ballY.coerceIn(.025f, .975f)
        val homeSupport = supportIndexes(w, m, true, bx, by)
        val awaySupport = supportIndexes(w, m, false, bx, by)
        val homeRunners = runnerIndexes(w, m, true)
        val awayRunners = runnerIndexes(w, m, false)
        val homePress = pressers(w, m, true, bx, by)
        val awayPress = pressers(w, m, false, bx, by)
        val out = mutableListOf<MatchSpatialPlayer>()

        for (isHome in listOf(true, false)) {
            val list = xi(m, isHome)
            val owns = hasBall(m, isHome)
            val ballProgress = progressFromOwnGoal(isHome, by)
            val support = if (isHome) homeSupport else awaySupport
            val runners = if (isHome) homeRunners else awayRunners
            val pressing = if (isHome) homePress else awayPress

            list.forEachIndexed { index, id ->
                if (id == 0 || id in m.sentOff || id in m.injured) return@forEachIndexed
                val pos = slotPosition(w, m, isHome, index, id)
                val keeper = pos == Position.TW || index == 0
                val r = role(w, m, isHome, id)
                val ins = instructions(w, m, isHome, id)
                val base = skeleton(w, m, isHome, index, id, bx, by)
                var lat = base.first
                var progress = base.second

                if (id == m.livePlayerId && owns) {
                    lat = bx
                    progress = ballProgress
                } else if (!keeper) {
                    if (owns) {
                        val side = if (formationBase(m, isHome, index).first < .5f) -1f else 1f
                        if (index in support && PlayerInstruction.HOLD_POSITION !in ins) {
                            lat += (bx - lat) * .34f + side * .018f
                            progress += (ballProgress - progress) * .30f
                        }
                        if (index in runners && PlayerInstruction.HOLD_POSITION !in ins) {
                            progress += when (m.livePhase) {
                                LivePhase.COUNTER -> .105f
                                LivePhase.DANGEROUS_ATTACK -> .080f
                                LivePhase.ATTACK -> .055f
                                else -> .035f
                            }
                        }

                        when (r) {
                            PlayerRole.OVERLAPPING_FULLBACK -> { progress += .060f; lat += side * .045f }
                            PlayerRole.INVERTED_FULLBACK -> { progress += .025f; lat += (.5f - lat) * .38f }
                            PlayerRole.ANCHOR -> progress -= .055f
                            PlayerRole.DEEP_PLAYMAKER -> progress -= .025f
                            PlayerRole.BOX_TO_BOX -> progress += .030f
                            PlayerRole.PLAYMAKER -> { lat += (bx - lat) * .12f; progress += (ballProgress - progress) * .10f }
                            PlayerRole.INSIDE_FORWARD -> { lat += (.5f - lat) * .33f; progress += .035f }
                            PlayerRole.WINGER -> lat += side * .035f
                            PlayerRole.POACHER -> progress += .050f
                            PlayerRole.TARGET_FORWARD -> progress -= .012f
                            else -> Unit
                        }
                        if (PlayerInstruction.OVERLAP in ins) { progress += .050f; lat += side * .040f }
                        if (PlayerInstruction.CUT_INSIDE in ins) lat += (.5f - lat) * .30f
                        if (PlayerInstruction.RUN_IN_BEHIND in ins) progress += .055f

                        if (m.livePhase != LivePhase.CORNER) {
                            val oppBack = defendingBackLine(w, m, !isHome, progressFromOwnGoal(!isHome, by))
                            val opponentLineFromOurView = 1f - oppBack
                            val ceiling = max(ballProgress + .018f, opponentLineFromOurView + .018f).coerceAtMost(.965f)
                            progress = min(progress, ceiling)
                        }
                    } else {
                        if (index in pressing) {
                            val pressure = (.38f + effectivePressing(w, m, isHome) * .065f).coerceIn(.44f, .70f)
                            lat += (bx - lat) * pressure
                            progress += (ballProgress - progress) * pressure
                        } else if (PlayerInstruction.TIGHT_MARKING in ins) {
                            lat += (bx - lat) * .08f
                            progress += (ballProgress - progress) * .07f
                        }
                        if (r == PlayerRole.ANCHOR) progress -= .025f
                        if (r == PlayerRole.PRESSING_FORWARD && effectivePressing(w, m, isHome) >= 3) progress += .025f
                    }

                    if (PlayerInstruction.HOLD_POSITION in ins) {
                        lat = base.first + (lat - base.first) * .18f
                        progress = base.second + (progress - base.second) * .18f
                    }
                }

                progress = if (keeper) progress.coerceIn(.04f, .30f) else progress.coerceIn(.07f, .975f)
                out += MatchSpatialPlayer(
                    id = id,
                    isHome = isHome,
                    slotIndex = index,
                    position = pos,
                    lateral = lat.coerceIn(.035f, .965f),
                    longitudinal = absoluteY(isHome, progress),
                    active = id == m.livePlayerId && owns,
                )
            }
        }
        return MatchSpatialFrame(out)
    }
}

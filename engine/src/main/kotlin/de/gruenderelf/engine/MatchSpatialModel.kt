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
        var back = ballProgress - phaseTeamLength(m.livePhase) +
            (line - 3) * .022f + (mentality - 3) * .014f

        // Established attacks: the rest defence must actually cross toward halfway.
        when (m.livePhase) {
            LivePhase.DANGEROUS_ATTACK -> if (ballProgress >= .68f) {
                back = max(back, .48f + (ballProgress - .68f) * .40f)
            }
            LivePhase.ATTACK -> if (ballProgress >= .56f) {
                back = max(back, .36f + (ballProgress - .56f) * .34f)
            }
            LivePhase.CORNER -> back = max(back, .48f)
            LivePhase.DANGEROUS_FREE_KICK -> back = max(back, .44f)
            else -> Unit
        }
        if (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) back += .045f
        if (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) back -= .045f
        return back.coerceIn(.14f, .67f)
    }

    private fun defendingBackLine(w: World, m: LiveMatch, isHome: Boolean, ballProgress: Float): Float {
        val line = effectiveLine(w, m, isHome)
        val mentality = effectiveMentality(m, isHome)
        var back = .085f + ballProgress * .53f + (line - 3) * .022f + (mentality - 3) * .005f

        // Opponent is at/around our box: keep a believable penalty-area line.
        if (ballProgress <= .22f && m.livePhase in setOf(LivePhase.DANGEROUS_ATTACK, LivePhase.COUNTER)) {
            back = min(back, .145f + ballProgress * .12f)
        }
        return back.coerceIn(.075f, .60f)
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
                LivePhase.COUNTER -> .11f
                LivePhase.DANGEROUS_ATTACK -> .050f
                LivePhase.ATTACK -> .060f
                LivePhase.CORNER -> .025f
                else -> .075f
            }
            max(back + .24f, ballProgress + ahead).coerceIn(.40f, .975f)
        } else {
            val length = when (m.livePhase) {
                LivePhase.DANGEROUS_ATTACK -> .275f
                LivePhase.ATTACK -> .325f
                LivePhase.COUNTER -> .385f
                else -> .355f
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

        if (keeper) {
            var progress = .045f + back * .36f
            if (role(w, m, isHome, id) == PlayerRole.BUILD_UP_KEEPER) progress += .028f
            progress = min(progress, back - .115f).coerceIn(.04f, .28f)
            val lat = (.5f + (ballX - .5f) * .23f).coerceIn(.35f, .65f)
            return lat to progress
        }

        val norm = depthNorm(m, isHome, index)
        var progress = back + (front - back) * norm

        val widthScale = if (owns) .76f + width * .075f else .59f + width * .057f
        val center = .5f + (ballX - .5f) * if (owns) .16f else .30f
        var lateral = center + (baseLat - .5f) * widthScale

        when (pos) {
            Position.IV -> if (owns) progress -= .006f
            Position.LV, Position.RV -> if (owns) progress += .030f
            Position.DM -> progress -= if (owns) .025f else .012f
            Position.OM -> if (owns) progress += .020f
            Position.LA, Position.RA -> if (owns) progress += .028f
            Position.ST -> if (owns) progress += .020f
            else -> Unit
        }

        // Far side compresses when defending.
        if (!owns) {
            val sameSide = (baseLat < .5f) == (ballX < .5f)
            if (!sameSide) lateral += (.5f - lateral) * .20f
        }

        // Explicit rest-defence spacing in the final third.
        if (owns && m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress > .70f) {
            when (pos) {
                Position.IV -> progress = max(progress, back)
                Position.LV, Position.RV -> progress = max(progress, back + .060f)
                Position.DM -> progress = max(progress, back + .115f)
                Position.ZM -> progress = max(progress, back + .17f)
                else -> Unit
            }
        }

        return lateral.coerceIn(.045f, .955f) to progress.coerceIn(.065f, .975f)
    }

    private fun supportIndexes(w: World, m: LiveMatch, isHome: Boolean, ballX: Float, ballY: Float): Set<Int> {
        if (!hasBall(m, isHome)) return emptySet()
        val list = xi(m, isHome)
        val ballProgress = progressFromOwnGoal(isHome, ballY)
        return list.indices.filter { idx ->
            val id = list[idx]
            id != 0 && id != m.livePlayerId && id !in m.sentOff && id !in m.injured &&
                slotPosition(w, m, isHome, idx, id) != Position.TW &&
                PlayerInstruction.HOLD_POSITION !in instructions(w, m, isHome, id)
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
            id != 0 && id != m.livePlayerId && id !in m.sentOff && id !in m.injured &&
                slotPosition(w, m, isHome, idx, id) in setOf(Position.ST, Position.LA, Position.RA, Position.OM, Position.LV, Position.RV) &&
                PlayerInstruction.HOLD_POSITION !in instructions(w, m, isHome, id)
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
        if (ballProgress < .23f) count = min(count, 2)

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
            if (pos == Position.IV) score += if (ballProgress < .24f) .015f else .075f
            if (pos == Position.DM) score -= .025f
            if (r == PlayerRole.PRESSING_FORWARD) score -= .045f
            if (PlayerInstruction.PRESS_MORE in ins) score -= .055f
            if (PlayerInstruction.PRESS_LESS in ins) score += .12f
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

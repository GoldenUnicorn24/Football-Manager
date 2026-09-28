package de.gruenderelf.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchSpatialModelTest {
    private fun running(): Pair<World, LiveMatch> {
        val w = WorldFactory.createWorld(912345)
        val f = w.nextFixture() ?: error("fixture missing")
        val m = MatchEngine.start(w, f)
        return w to m
    }

    private fun setOwner(m: LiveMatch, home: Boolean, phase: LivePhase, ballX: Float, ballY: Float) {
        val clubId = if (home) m.homeId else m.awayId
        val lineup = if (home) m.homeXi else m.awayXi
        val formation = if (home) m.homeFormation else m.awayFormation
        val slots = Formations.positions(formation)
        m.homeInPossession = home
        m.chainOwnerClubId = clubId
        m.liveClubId = clubId
        m.livePhase = phase
        m.ballX = ballX
        m.ballY = ballY
        val attackingSlot = lineup.indices.lastOrNull { idx ->
            lineup[idx] != 0 && slots.getOrNull(idx) in setOf(Position.ST, Position.LA, Position.RA, Position.OM)
        }
        m.livePlayerId = attackingSlot?.let { lineup[it] } ?: lineup.last { it != 0 }
    }

    @Test
    fun homeReallyAttacksLeftAndWholeBlockPushesUp() {
        val (w, m) = running()
        setOwner(m, home = true, phase = LivePhase.DANGEROUS_ATTACK, ballX = .52f, ballY = .12f)

        val frame = MatchSpatialModel.frame(w, m)
        val homeDefenders = frame.players.filter {
            it.isHome && it.position in setOf(Position.IV, Position.LV, Position.RV)
        }
        val awayDefenders = frame.players.filter {
            !it.isHome && it.position in setOf(Position.IV, Position.LV, Position.RV)
        }
        val homeKeeper = frame.players.first { it.isHome && it.position == Position.TW }
        val active = frame.players.first { it.active }

        assertTrue(homeDefenders.isNotEmpty())
        assertTrue(awayDefenders.isNotEmpty())

        // HOME owns the right-hand goal and attacks left (toward y=0).
        // In a dangerous attack its back line must be around midfield rather than
        // stuck beside its own box at y~0.85-0.95.
        assertTrue(homeDefenders.map { it.longitudinal }.average() < .62)
        assertTrue(homeDefenders.map { it.longitudinal }.average() > .28)

        // AWAY is defending its left-hand goal and therefore drops toward y=0.
        assertTrue(awayDefenders.map { it.longitudinal }.average() < .32)

        // Keeper remains behind HOME's advancing defensive line.
        assertTrue(homeKeeper.longitudinal > homeDefenders.map { it.longitudinal }.average())

        assertEquals(m.ballX.toDouble(), active.lateral.toDouble(), .0001)
        assertEquals(m.ballY.toDouble(), active.longitudinal.toDouble(), .0001)
    }

    @Test
    fun awayAttackMirrorsHomeInsteadOfRunningTheWrongWay() {
        val (w, m) = running()
        setOwner(m, home = false, phase = LivePhase.DANGEROUS_ATTACK, ballX = .44f, ballY = .88f)

        val frame = MatchSpatialModel.frame(w, m)
        val awayDefenders = frame.players.filter {
            !it.isHome && it.position in setOf(Position.IV, Position.LV, Position.RV)
        }
        val homeDefenders = frame.players.filter {
            it.isHome && it.position in setOf(Position.IV, Position.LV, Position.RV)
        }
        val awayKeeper = frame.players.first { !it.isHome && it.position == Position.TW }
        val active = frame.players.first { it.active }

        // AWAY owns the left-hand goal and attacks right (toward y=1).
        assertTrue(awayDefenders.map { it.longitudinal }.average() > .38)
        assertTrue(awayDefenders.map { it.longitudinal }.average() < .72)

        // HOME defending its right-hand goal drops toward y=1.
        assertTrue(homeDefenders.map { it.longitudinal }.average() > .68)

        // Keeper remains behind AWAY's advancing defensive line.
        assertTrue(awayKeeper.longitudinal < awayDefenders.map { it.longitudinal }.average())

        assertEquals(m.ballX.toDouble(), active.lateral.toDouble(), .0001)
        assertEquals(m.ballY.toDouble(), active.longitudinal.toDouble(), .0001)
    }

    @Test
    fun formationsRemainLayeredWhileFollowingBallSide() {
        val (w, m) = running()
        setOwner(m, home = true, phase = LivePhase.ATTACK, ballX = .18f, ballY = .30f)

        val frame = MatchSpatialModel.frame(w, m)
        val home = frame.players.filter { it.isHome }
        val defenders = home.filter { it.position in setOf(Position.IV, Position.LV, Position.RV) }
        val midfield = home.filter { it.position in setOf(Position.DM, Position.ZM, Position.OM) }
        val forwards = home.filter { it.position in setOf(Position.ST, Position.LA, Position.RA) }

        fun progress(p: MatchSpatialPlayer) = MatchSpatialModel.progressFromOwnGoal(true, p.longitudinal)

        assertTrue(defenders.isNotEmpty() && midfield.isNotEmpty() && forwards.isNotEmpty())
        assertTrue(defenders.map(::progress).average() < midfield.map(::progress).average())
        assertTrue(midfield.map(::progress).average() < forwards.map(::progress).average())

        // Ball on the upper/left touchline side shifts the block there without
        // collapsing every player onto exactly the same lateral coordinate.
        assertTrue(home.map { it.lateral }.average() < .50)
        assertTrue(home.maxOf { it.lateral } - home.minOf { it.lateral } > .30)
    }
    @Test
    fun dangerousAttackKeepsRestDefenceHighForBothTeams() {
        val (w, m) = running()

        setOwner(m, home = true, phase = LivePhase.DANGEROUS_ATTACK, ballX = .54f, ballY = .10f)
        val homeFrame = MatchSpatialModel.frame(w, m)
        val homeCbs = homeFrame.players.filter { it.isHome && it.position == Position.IV }
        val homeCbProgress = homeCbs.map { MatchSpatialModel.progressFromOwnGoal(true, it.longitudinal) }
        assertTrue("HOME centre-backs must leave their own box in a settled attack", homeCbProgress.average() >= .46)
        assertTrue("HOME back line should remain a line, not scatter", homeCbProgress.maxOrNull()!! - homeCbProgress.minOrNull()!! < .09)

        setOwner(m, home = false, phase = LivePhase.DANGEROUS_ATTACK, ballX = .46f, ballY = .90f)
        val awayFrame = MatchSpatialModel.frame(w, m)
        val awayCbs = awayFrame.players.filter { !it.isHome && it.position == Position.IV }
        val awayCbProgress = awayCbs.map { MatchSpatialModel.progressFromOwnGoal(false, it.longitudinal) }
        assertTrue("AWAY centre-backs must leave their own box in a settled attack", awayCbProgress.average() >= .46)
        assertTrue("AWAY back line should remain a line, not scatter", awayCbProgress.maxOrNull()!! - awayCbProgress.minOrNull()!! < .09)
    }

    @Test
    fun deepDefenceKeepsCentreBacksTogetherWhileMidfieldPresses() {
        val (w, m) = running()
        setOwner(m, home = false, phase = LivePhase.DANGEROUS_ATTACK, ballX = .25f, ballY = .88f)

        val frame = MatchSpatialModel.frame(w, m)
        val homeDefenders = frame.players.filter { it.isHome && it.position in setOf(Position.IV, Position.LV, Position.RV) }
        val homeCbs = homeDefenders.filter { it.position == Position.IV }
        val cbProgress = homeCbs.map { MatchSpatialModel.progressFromOwnGoal(true, it.longitudinal) }

        assertTrue("Defending centre-backs should stay close to the own box", cbProgress.average() < .24)
        assertTrue("Centre-backs should keep a compact horizontal line", cbProgress.maxOrNull()!! - cbProgress.minOrNull()!! < .07)
        assertTrue("Defensive line should keep useful lateral width", homeDefenders.maxOf { it.lateral } - homeDefenders.minOf { it.lateral } > .24)
    }

    @Test
    fun progressConversionMirrorsHomeAndAwayExactly() {
        assertEquals(.9, MatchSpatialModel.progressFromOwnGoal(true, .1f).toDouble(), .0001)
        assertEquals(.1, MatchSpatialModel.progressFromOwnGoal(true, .9f).toDouble(), .0001)
        assertEquals(.1, MatchSpatialModel.progressFromOwnGoal(false, .1f).toDouble(), .0001)
        assertEquals(.9, MatchSpatialModel.progressFromOwnGoal(false, .9f).toDouble(), .0001)
        assertEquals(.1, MatchSpatialModel.absoluteY(true, .9f).toDouble(), .0001)
        assertEquals(.9, MatchSpatialModel.absoluteY(false, .9f).toDouble(), .0001)
    }

    @Test
    fun screenDirectionsUseHomeRightInFirstHalfAndSwitchAfterBreak() {
        assertEquals(1, MatchSpatialModel.screenAttackDirection(true, 1))
        assertEquals(-1, MatchSpatialModel.screenAttackDirection(false, 1))
        assertEquals(-1, MatchSpatialModel.screenAttackDirection(true, 2))
        assertEquals(1, MatchSpatialModel.screenAttackDirection(false, 2))

        assertEquals(.88, MatchSpatialModel.displayLongitudinal(1, .12f).toDouble(), .0001)
        assertEquals(.12, MatchSpatialModel.displayLongitudinal(2, .12f).toDouble(), .0001)
        assertEquals(.88, MatchSpatialModel.displayLongitudinal(4, .88f).toDouble(), .0001)
    }

    @Test
    fun playersReceiveIndividualMovementTasksInsteadOfOneSynchronizedBlock() {
        val (w, m) = running()
        setOwner(m, home = true, phase = LivePhase.DANGEROUS_ATTACK, ballX = .30f, ballY = .13f)

        val frame = MatchSpatialModel.frame(w, m)
        val home = frame.players.filter { it.isHome }
        val away = frame.players.filter { !it.isHome }

        assertTrue(home.any { it.motion == MatchSpatialMotion.BALL })
        assertTrue(home.any { it.motion == MatchSpatialMotion.RUN })
        assertTrue(home.any { it.motion == MatchSpatialMotion.SUPPORT })
        assertTrue(away.any { it.motion == MatchSpatialMotion.PRESS })
        assertTrue(away.any { it.motion == MatchSpatialMotion.MARK })

        // The pitch must contain several independent target positions, not a
        // single translated formation line.
        assertTrue(home.map { it.longitudinal }.distinct().size >= 4)
        assertTrue(away.map { it.longitudinal }.distinct().size >= 4)
    }

}

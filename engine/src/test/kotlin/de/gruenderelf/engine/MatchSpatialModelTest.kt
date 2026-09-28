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
        m.homeInPossession = home
        m.chainOwnerClubId = clubId
        m.liveClubId = clubId
        m.livePhase = phase
        m.ballX = ballX
        m.ballY = ballY
        m.livePlayerId = lineup.firstOrNull { it != 0 && it != lineup.firstOrNull() } ?: lineup.first { it != 0 }
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
}

package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class MatchTerritoryPhaseTest {

    @Test
    fun phaseThresholdsMatchRealPitchTerritoryForBothTeams() {
        // HOME attacks toward engine y=0.
        assertEquals(LivePhase.POSSESSION, MatchEngine.territorialPhase(true, .80f, LivePhase.ATTACK))
        assertEquals(LivePhase.POSSESSION, MatchEngine.territorialPhase(true, .50f, LivePhase.ATTACK))
        assertEquals(LivePhase.ATTACK, MatchEngine.territorialPhase(true, .42f, LivePhase.ATTACK))
        assertEquals(LivePhase.ATTACK, MatchEngine.territorialPhase(true, .30f, LivePhase.DANGEROUS_ATTACK))
        assertEquals(LivePhase.DANGEROUS_ATTACK, MatchEngine.territorialPhase(true, .27f, LivePhase.DANGEROUS_ATTACK))

        // AWAY attacks toward engine y=1. Same thresholds must mirror exactly.
        assertEquals(LivePhase.POSSESSION, MatchEngine.territorialPhase(false, .20f, LivePhase.ATTACK))
        assertEquals(LivePhase.POSSESSION, MatchEngine.territorialPhase(false, .50f, LivePhase.ATTACK))
        assertEquals(LivePhase.ATTACK, MatchEngine.territorialPhase(false, .58f, LivePhase.ATTACK))
        assertEquals(LivePhase.ATTACK, MatchEngine.territorialPhase(false, .70f, LivePhase.DANGEROUS_ATTACK))
        assertEquals(LivePhase.DANGEROUS_ATTACK, MatchEngine.territorialPhase(false, .73f, LivePhase.DANGEROUS_ATTACK))
    }

    @Test
    fun activeCounterMayStartDeepButNormalAttackCannot() {
        assertEquals(
            LivePhase.COUNTER,
            MatchEngine.territorialPhase(true, .83f, LivePhase.COUNTER, counterActive = true),
        )
        assertEquals(
            LivePhase.POSSESSION,
            MatchEngine.territorialPhase(true, .83f, LivePhase.COUNTER, counterActive = false),
        )
    }

    @Test
    fun longLiveSimulationNeverShowsAttackInOwnHalfOrDangerAtMidfield() {
        val w = WorldFactory.createWorld(160016L)
        val m = MatchEngine.start(w, w.nextFixture()!!)
        var guard = 0

        while (!m.finished && guard++ < 260) {
            when {
                m.pendingDecision -> MatchEngine.decide(w, m, Decision.PASS)
                m.incidentPause -> MatchEngine.resumeIncident(w, m)
                m.assistantSubPending -> MatchEngine.rejectAssistantSubstitution(w, m)
                m.halfTime -> MatchEngine.secondHalf(m)
                else -> MatchEngine.step(w, m)
            }

            val ownerHome = m.chainOwnerClubId == m.homeId
            val progress = (if (ownerHome) 1f - m.ballY else m.ballY).coerceIn(0f, 1f)
            when (m.livePhase) {
                LivePhase.ATTACK -> {
                    assertTrue(
                        progress >= .54f,
                        "ATTACK shown before clear entry into opponent half: progress=$progress, ballY=${m.ballY}, ownerHome=$ownerHome, detail=${m.liveDetail}",
                    )
                    assertTrue(
                        progress < .72f,
                        "ATTACK should become DANGEROUS_ATTACK in final territory: progress=$progress, detail=${m.liveDetail}",
                    )
                }
                LivePhase.DANGEROUS_ATTACK -> assertTrue(
                    progress >= .72f,
                    "DANGEROUS_ATTACK shown outside final territory: progress=$progress, ballY=${m.ballY}, ownerHome=$ownerHome, detail=${m.liveDetail}",
                )
                else -> Unit
            }
        }

        assertTrue(guard > 30)
    }
}

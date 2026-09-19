package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchAiTest {
    private fun makeEliteUser(w: World, value: Int = 92) {
        w.squad(w.user.clubId).filter { !it.youth }.forEach { p ->
            p.attributes = Attributes(value,value,value,value,value,value,value,value,value,if (p.position==Position.TW) value else 18,value)
            p.fitness = 100.0; p.morale = 90; p.sharpness = 90; p.form = 8.0
        }
        WorldFactory.autoLineup(w,w.user.clubId)
    }

    private fun makeWeakClub(w: World, clubId: Int, value: Int = 32) {
        w.squad(clubId).filter { !it.youth }.forEach { p ->
            p.attributes = Attributes(value,value,value,value,value,value,value,value,value,if (p.position==Position.TW) value else 10,value)
            p.fitness = 100.0; p.morale = 60; p.sharpness = 55; p.form = 6.2
        }
        WorldFactory.autoLineup(w,clubId)
    }

    private fun runToEnd(w: World, m: LiveMatch, observe: ((LiveMatch)->Unit)? = null) {
        var guard=0
        while(!m.finished && guard++<900) {
            when {
                m.incidentPause -> {
                    if(m.incidentReason==MatchPauseReason.INJURY && m.incidentClubId==w.user.clubId) {
                        MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)}
                    }
                    MatchEngine.resumeIncident(w,m)
                }
                m.pendingDecision -> MatchEngine.decide(w,m,Decision.PASS)
                m.halfTime -> MatchEngine.secondHalf(m)
                else -> MatchEngine.step(w,m)
            }
            observe?.invoke(m)
        }
        assertTrue(m.finished,"Match did not finish")
    }

    @Test fun eliteLowerLeagueTeamActuallyControlsPossessionAndChances() {
        var possession=0.0; var xgFor=0.0; var xgAgainst=0.0
        repeat(24){i->
            val w=WorldFactory.createWorld(12000L+i)
            val f=w.nextFixture()!!; val opponent=if(f.homeId==w.user.clubId)f.awayId else f.homeId
            makeEliteUser(w); makeWeakClub(w,opponent)
            w.club().tactics.apply{buildUp=BuildUp.TIKI_TAKA;pressing=4;line=4;tempo=3;width=4;mentality=4}
            val m=MatchEngine.start(w,f)
            runToEnd(w,m)
            val own=if(m.homeId==w.user.clubId)m.home else m.away
            val other=if(m.homeId==w.user.clubId)m.away else m.home
            val total=(own.possessionTicks+other.possessionTicks).coerceAtLeast(1)
            possession+=own.possessionTicks.toDouble()/total;xgFor+=own.xg;xgAgainst+=other.xg
        }
        val avg=possession/24.0
        println("ELITE_AI: possession=$avg xgFor=${xgFor/24} xgAgainst=${xgAgainst/24}")
        assertTrue(avg>0.58,"elite team possession=$avg")
        assertTrue(xgFor>xgAgainst*1.8,"xgFor=$xgFor xgAgainst=$xgAgainst")
    }

    @Test fun liveBallUsesWholeWidthLongPassesAndKeeperRecycling() {
        var minX=1f;var maxX=0f;var longPass=false;var recycle=false
        repeat(4){i->
            val w=WorldFactory.createWorld(13000L+i);val f=w.nextFixture()!!;val opponent=if(f.homeId==w.user.clubId)f.awayId else f.homeId
            makeEliteUser(w,86);makeWeakClub(w,opponent,38)
            w.club().tactics.apply{buildUp=if(i%2==0)BuildUp.TIKI_TAKA else BuildUp.WIDE;width=5;pressing=4;line=4;tempo=3;mentality=3}
            val m=MatchEngine.start(w,f);w.clubs.getValue(opponent).tactics.pressing=5
            runToEnd(w,m){live->
                minX=minOf(minX,live.ballX);maxX=maxOf(maxX,live.ballX)
                val d=live.liveDetail
                if(d.contains("Langer")||d.contains("Seitenwechsel")||d.contains("Steilpass"))longPass=true
                if(d.contains("Torwart")||d.contains("Neuaufbau über hinten"))recycle=true
            }
        }
        println("FIELD_AI: minX=$minX maxX=$maxX long=$longPass recycle=$recycle")
        assertTrue(minX<.16f&&maxX>.84f,"field width only $minX..$maxX")
        assertTrue(longPass,"no long pass or side switch seen")
        assertTrue(recycle,"no keeper/back-line recycle seen")
    }

    @Test fun tikiTakaProducesClearlyMorePassingThanDirectPlay() {
        fun passes(build: BuildUp): Int {
            val w=WorldFactory.createWorld(14001);makeEliteUser(w,78);w.club().tactics.apply{buildUp=build;width=4;tempo=3;pressing=3;line=3;mentality=3}
            val m=MatchEngine.start(w,w.nextFixture()!!);runToEnd(w,m)
            val ids=if(m.homeId==w.user.clubId)m.homeXi else m.awayXi
            return ids.sumOf{m.playerPerformance[it]?.passesAttempted?:0}
        }
        val tiki=passes(BuildUp.TIKI_TAKA);val direct=passes(BuildUp.DIRECT)
        println("STYLE_AI: tiki=$tiki direct=$direct")
        assertTrue(tiki>direct*1.25,"tiki=$tiki direct=$direct")
    }
    @Test fun foulsCreateLocationAwareSetPiecesAndCardsForBothTeams() {
        var free=0;var dangerous=0;var penalties=0;var homeCards=0;var awayCards=0
        repeat(30){i->
            val w=WorldFactory.createWorld(15000L+i);val m=MatchEngine.start(w,w.nextFixture()!!)
            w.clubs.getValue(m.homeId).tactics.pressing=5;w.clubs.getValue(m.awayId).tactics.pressing=5
            runToEnd(w,m){live->
                when(live.livePhase){LivePhase.FREE_KICK->free++;LivePhase.DANGEROUS_FREE_KICK->dangerous++;LivePhase.PENALTY->penalties++;else->{}}
            }
            homeCards+=m.yellows.filter{w.players[it.key]?.clubId==m.homeId}.values.sum()+m.sentOff.count{w.players[it]?.clubId==m.homeId}
            awayCards+=m.yellows.filter{w.players[it.key]?.clubId==m.awayId}.values.sum()+m.sentOff.count{w.players[it]?.clubId==m.awayId}
        }
        println("FOUL_AI: free=$free dangerous=$dangerous penalties=$penalties homeCards=$homeCards awayCards=$awayCards")
        assertTrue(free>15,"too few normal free kicks: $free")
        assertTrue(dangerous>2,"no dangerous free-kick variety: $dangerous")
        assertTrue(penalties>0,"no penalties generated from box fouls")
        assertTrue(homeCards>0&&awayCards>0,"cards not visible for both teams: $homeCards/$awayCards")
    }

    @Test fun targetPlayerIsActuallySoughtMoreOften() {
        fun shots(targeted: Boolean): Int {
            var total=0
            repeat(18){i->
                val w=WorldFactory.createWorld(16000L+i)
                w.squad(w.user.clubId).filter{!it.youth}.forEach{p->
                    p.attributes=Attributes(70,70,70,70,60,65,75,70,70,if(p.position==Position.TW)70 else 12,70);p.fitness=100.0;p.form=7.0;p.morale=75
                }
                WorldFactory.autoLineup(w,w.user.clubId)
                val c=w.club();val candidate=c.tactics.xi.firstOrNull{id->id!=0&&id!=w.user.playerId&&w.players.getValue(id).position!=Position.TW}?:return@repeat
                c.tactics.targetPlayerId=if(targeted)candidate else 0
                val m=MatchEngine.start(w,w.nextFixture()!!);runToEnd(w,m)
                total+=m.playerPerformance[candidate]?.shots?:0
            }
            return total
        }
        val normal=shots(false);val target=shots(true)
        println("TARGET_ROLE: normal=$normal target=$target")
        assertTrue(target>normal*1.25,"target role did not materially increase shot involvement: $normal -> $target")
    }

    @Test fun assignedSetPieceTakerIsUsedWhenOnPitch() {
        var seen=0;var wrong=0
        repeat(14){i->
            val w=WorldFactory.createWorld(17000L+i);WorldFactory.autoLineup(w,w.user.clubId)
            val assigned=w.club().tactics.xi.first{id->id!=0&&w.players.getValue(id).position!=Position.TW}
            w.club().tactics.apply{penaltyTakerId=assigned;freeKickTakerId=assigned;cornerLeftTakerId=assigned;cornerRightTakerId=assigned}
            val m=MatchEngine.start(w,w.nextFixture()!!);w.clubs.getValue(m.homeId).tactics.pressing=5;w.clubs.getValue(m.awayId).tactics.pressing=5
            runToEnd(w,m){live->
                val ownXi=if(live.homeId==w.user.clubId)live.homeXi else live.awayXi
                if(assigned in ownXi&&assigned !in live.injured&&live.liveClubId==w.user.clubId&&live.livePhase in setOf(LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY,LivePhase.CORNER)){seen++;if(live.livePlayerId!=assigned)wrong++}
            }
        }
        println("SET_PIECE_ROLE: seen=$seen wrong=$wrong")
        assertTrue(seen>3,"assigned taker was not exercised")
        assertTrue(wrong==0,"assigned set-piece taker was ignored $wrong times")
    }

    @Test fun modernMatchFlowProducesOffsidesThrowInsVarWoodworkAndAssists() {
        var offsides=0;var throwIns=0;var varChecks=0;var assists=0;var woodwork=0
        repeat(24){i->
            val w=WorldFactory.createWorld(19000L+i);val m=MatchEngine.start(w,w.nextFixture()!!)
            runToEnd(w,m)
            offsides+=m.home.offsides+m.away.offsides
            throwIns+=m.home.throwIns+m.away.throwIns
            varChecks+=m.home.varChecks+m.away.varChecks
            assists+=m.goals.count{it.assistId!=0}
            woodwork+=m.shotEvents.count{it.outcome==ShotOutcome.WOODWORK}
        }
        println("MODERN_MATCH_AI: offsides=$offsides throwIns=$throwIns var=$varChecks assists=$assists woodwork=$woodwork")
        assertTrue(offsides>4,"too few offsides: $offsides")
        assertTrue(throwIns>20,"too few throw-ins: $throwIns")
        assertTrue(varChecks>0,"no VAR checks")
        assertTrue(assists>5,"AI goals lack assists: $assists")
        assertTrue(woodwork>0,"no woodwork outcomes")
    }

    @Test fun coTrainerReturnsFiveContextAwareSuggestionsAndPrioritizesEmergency() {
        val w=WorldFactory.createWorld(19111L);val m=MatchEngine.start(w,w.nextFixture()!!)
        val home=w.user.clubId==m.homeId;val lineup=if(home)m.homeXi else m.awayXi
        val emergency=lineup.first{id->id!=0&&w.players.getValue(id).position!=Position.TW}
        w.players.getValue(emergency).fitness=41.0;m.yellows[emergency]=1;m.minutesPlayed[emergency]=76
        m.playerPerformance[emergency]=PlayerMatchPerformance(minutes=76,fitnessStart=98.0,rating=5.4,turnovers=7)
        val suggestions=MatchEngine.substitutionSuggestions(w,m,w.user.clubId)
        assertEquals(5,suggestions.size,"Co-Trainer soll fünf Vorschläge liefern")
        assertEquals(emergency,suggestions.first().outId,"Notfall aus Fitness/Karte muss priorisiert werden")
        assertTrue(suggestions.first().reason.contains("Fitness")&&suggestions.first().reason.contains("Gelb"),suggestions.first().reason)
    }

    @Test fun bothHalvesHaveStoppageTimeAndSecondHalfRestartsAtCenter() {
        val w=WorldFactory.createWorld(19200L);val m=MatchEngine.start(w,w.nextFixture()!!);val firstKickoffHome=m.kickoffHomeFirst
        var guard=0
        while(!m.halfTime&&guard++<500){when{m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.pendingDecision->MatchEngine.decide(w,m,Decision.PASS);else->MatchEngine.step(w,m)}}
        assertTrue(m.halfTime&&m.breakType==MatchBreakType.HALF_TIME,"first half did not reach a real interval")
        assertTrue(m.firstHalfAdded>=1,"first-half stoppage missing")
        assertTrue(m.ticker.any{it.clockLabel.startsWith("45+")},"45+ stoppage was never shown")
        MatchEngine.secondHalf(m)
        assertEquals(2,m.period);assertEquals(45,m.minute);assertEquals(0,m.stoppageMinute)
        assertEquals(.5f,m.ballX);assertEquals(.5f,m.ballY);assertEquals(!firstKickoffHome,m.homeInPossession)
        assertEquals("Anstoß zur 2. Halbzeit",m.liveDetail)
        runToEnd(w,m)
        assertTrue(m.secondHalfAdded>=2,"second-half stoppage missing")
        assertTrue(m.ticker.any{it.clockLabel.startsWith("90+")},"90+ stoppage was never shown")
    }

    @Test fun knockoutDrawRunsThroughExtraTimeAndRealPenaltyShootout() {
        val w=WorldFactory.createWorld(19210L);val f=w.fixtures.first{it.competition==CompetitionType.NATIONAL_CUP};val m=MatchEngine.start(w,f)
        m.home.goals=1;m.away.goals=1;m.period=2;m.secondHalf=true;m.minute=90;m.secondHalfAdded=2;m.stoppageAnnounced=true;m.stoppageMinute=2
        MatchEngine.step(w,m)
        assertTrue(m.halfTime&&m.breakType==MatchBreakType.EXTRA_TIME_START,"draw after 90 must enter extra time")
        MatchEngine.secondHalf(m);assertEquals(3,m.period);assertTrue(m.extraTimePlayed);assertEquals(90,m.minute);assertEquals(.5f,m.ballX);assertEquals(.5f,m.ballY)
        m.minute=105;m.extraFirstAdded=1;m.stoppageAnnounced=true;m.stoppageMinute=1;MatchEngine.step(w,m)
        assertTrue(m.halfTime&&m.breakType==MatchBreakType.EXTRA_TIME_HALF)
        MatchEngine.secondHalf(m);assertEquals(4,m.period);assertEquals(105,m.minute);assertEquals(.5f,m.ballX);assertEquals(.5f,m.ballY)
        m.minute=120;m.extraSecondAdded=1;m.stoppageAnnounced=true;m.stoppageMinute=1;MatchEngine.step(w,m)
        assertTrue(m.halfTime&&m.breakType==MatchBreakType.SHOOTOUT_START,"draw after 120 must enter shootout")
        MatchEngine.secondHalf(m);assertTrue(m.shootoutActive)
        var guard=0;while(!m.finished&&guard++<30)MatchEngine.step(w,m)
        assertTrue(m.finished,"shootout did not finish");assertTrue(m.homePens!=m.awayPens,"shootout must produce a winner")
        assertTrue(m.penaltyShootout.size>=6,"too few individual shootout kicks: ${m.penaltyShootout.size}")
        assertTrue(m.penaltyShootout.all{it.targetLabel.isNotBlank()})
    }

    @Test fun everyShotStoresExactGoalTargetForGoalsSavesAndMisses() {
        val events=mutableListOf<ShotEvent>()
        repeat(8){i->val w=WorldFactory.createWorld(19300L+i);val m=MatchEngine.start(w,w.nextFixture()!!);runToEnd(w,m);events+=m.shotEvents}
        assertTrue(events.size>20,"not enough shots to validate target model")
        assertTrue(events.all{it.targetLabel.isNotBlank()},"shot without target description")
        assertTrue(events.filter{it.outcome==ShotOutcome.GOAL}.all{it.targetX in 0f..1f&&it.targetY in 0f..1f},"goal target outside frame")
        assertTrue(events.filter{it.outcome in setOf(ShotOutcome.SAVED,ShotOutcome.CORNER,ShotOutcome.DEFLECTED,ShotOutcome.REBOUND)}.all{it.targetX in 0f..1f&&it.targetY in 0f..1f},"saved shot target outside frame")
        assertTrue(events.filter{it.outcome==ShotOutcome.OFF_TARGET}.all{it.targetX !in 0f..1f||it.targetY>1f},"miss should visibly miss goal")
    }

    @Test fun stoppageTimeGrowsFromRealInterruptionsAndShowsPreviewBeforeBreak() {
        val w=WorldFactory.createWorld(19350L);val m=MatchEngine.start(w,w.nextFixture()!!)
        m.minute=43
        assertEquals("Nachspielzeit +1",MatchEngine.stoppageTimeOverlay(m))
        m.minute=45;m.stoppageAnnounced=true;m.firstHalfAdded=1;m.stoppageLossSeconds=38
        val before=m.stoppageLossSeconds
        m.pendingDecision=true
        MatchEngine.decide(w,m,Decision.FOUL)
        assertTrue(m.stoppageLossSeconds>before,"foul must add interruption time")
        assertTrue(m.firstHalfAdded>=2,"interruption during announced stoppage must extend added time")
        assertEquals("Nachspielzeit +${m.firstHalfAdded}",MatchEngine.stoppageTimeOverlay(m))
    }

    @Test fun varReviewShowsReasonThenDecisionWithoutAdvancingMatchClock() {
        val w=WorldFactory.createWorld(19360L);val m=MatchEngine.start(w,w.nextFixture()!!)
        val shooter=m.homeXi.first{id->id!=0&&w.players.getValue(id).position!=Position.TW}
        val keeper=m.awayXi.firstOrNull{id->id!=0&&w.players.getValue(id).position==Position.TW}?:m.awayXi.first{it!=0}
        m.minute=44;m.lastShotTargetX=.84f;m.lastShotTargetY=.82f;m.lastShotTargetLabel="oben rechts";m.lastShotOutcome=ShotOutcome.GOAL
        m.shotEvents.add(ShotEvent(minute=44,clubId=m.homeId,playerId=shooter,type=ShotType.BOX_SHOT,xg=.24,goalProbability=.18,outcome=ShotOutcome.GOAL,targetX=.84f,targetY=.82f,targetLabel="oben rechts",clockLabel="44"))
        m.home.shots=1;m.home.xg=.24;m.pendingVarShotIndex=0;m.pendingVarKeeperId=keeper;m.varReviewStage=0
        m.livePhase=LivePhase.GOAL;m.liveClubId=m.homeId;m.livePlayerId=shooter;m.liveDetail="TOR · oben rechts · Strafraumabschluss"
        val frozenMinute=m.minute
        MatchEngine.step(w,m)
        assertEquals(frozenMinute,m.minute,"clock must freeze while VAR reason is shown")
        assertEquals(LivePhase.VAR,m.livePhase);assertTrue(m.varReviewReason.isNotBlank());assertTrue(m.varReviewResult.isBlank())
        assertTrue(m.stoppageLossSeconds>=55,"VAR interruption must feed stoppage time")
        val reason=m.varReviewReason
        MatchEngine.step(w,m)
        assertEquals(frozenMinute,m.minute,"clock must remain frozen until VAR decision is shown")
        assertEquals(LivePhase.VAR,m.livePhase);assertEquals(reason,m.varReviewReason);assertTrue(m.varReviewResult.isNotBlank());assertEquals(-1,m.pendingVarShotIndex)
        if(m.varReviewResult.startsWith("Kein Tor")){assertTrue(m.shotEvents.isEmpty(),"disallowed VAR goal must not remain an official shot");assertEquals(ShotOutcome.DISALLOWED,m.lastShotOutcome)}
        else assertEquals(1,m.home.goals)
        assertEquals("oben rechts",m.lastShotTargetLabel,"reviewed shot must keep its exact visual target")
    }

}

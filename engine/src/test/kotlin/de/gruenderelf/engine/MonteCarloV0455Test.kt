package de.gruenderelf.engine

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class MonteCarloV0455Test {
    @Test fun realisticRangesAcross5000Matches() {
        val matches=5000
        val w=WorldFactory.createWorld(99001L)
        val f=w.fixtures.first{it.tier==10&&it.homeId!=w.user.clubId&&it.awayId!=w.user.clubId}
        val clubs=listOf(f.homeId,f.awayId)
        clubs.forEach{cid->
            w.clubs.getValue(cid).stadium.pitchQuality=55
            w.squad(cid).filter{!it.youth}.forEach{p->
                p.attributes=Attributes(45,45,45,45,45,45,45,45,45,if(p.position==Position.TW)45 else 12,45)
                p.hidden=Hidden();p.form=6.5;p.morale=65;p.sharpness=65
            }
        }
        var goals=0L;var xg=0.0;var shots=0L;var sot=0L;var zeroZero=0;var fourPlus=0;var extreme=0;var lowXgFourGoals=0
        repeat(matches){i->
            clubs.forEach{cid->
                w.clubs.getValue(cid).tactics=Tactics()
                w.squad(cid).filter{!it.youth}.forEach{p->p.fitness=100.0;p.injuryWeeks=0;p.injury="";p.unavailableWeeks=0;p.unavailableReason=null}
                WorldFactory.autoLineup(w,cid)
            }
            val m=MatchEngine.simulateFullMatch(w,f.copy(id=600000+i));val sides=listOf(m.home,m.away)
            goals+=sides.sumOf{it.goals}.toLong();xg+=sides.sumOf{it.xg};shots+=sides.sumOf{it.shots}.toLong();sot+=sides.sumOf{it.shotsOnTarget}.toLong()
            if(m.home.goals==0&&m.away.goals==0)zeroZero++
            if(m.home.goals+m.away.goals>=4)fourPlus++
            if(maxOf(m.home.goals,m.away.goals)>=5||abs(m.home.goals-m.away.goals)>=4)extreme++
            if((m.home.goals>=4&&m.home.xg<=1.0)||(m.away.goals>=4&&m.away.xg<=1.0))lowXgFourGoals++
        }
        val teamGames=matches*2.0
        val goalsPerTeam=goals/teamGames;val xgPerTeam=xg/teamGames;val shotsPerTeam=shots/teamGames;val sotPerTeam=sot/teamGames
        val conversion=goals.toDouble()/shots;val zeroShare=zeroZero.toDouble()/matches;val fourShare=fourPlus.toDouble()/matches;val extremeShare=extreme.toDouble()/matches;val mismatchShare=lowXgFourGoals.toDouble()/matches
        println("MONTE_CARLO_AFTER matches=$matches goalsPerTeam=$goalsPerTeam xgPerTeam=$xgPerTeam shotsPerTeam=$shotsPerTeam sotPerTeam=$sotPerTeam conversionPerShot=$conversion conversionPerSot=${goals.toDouble()/sot} zeroZeroShare=$zeroShare fourPlusShare=$fourShare extremeShare=$extremeShare lowXgFourGoalsShare=$mismatchShare")
        assertTrue(goalsPerTeam in 1.00..1.70,"goals/team=$goalsPerTeam")
        assertTrue(xgPerTeam in 1.20..1.70,"xG/team=$xgPerTeam")
        assertTrue(shotsPerTeam in 9.5..16.5,"shots/team=$shotsPerTeam")
        assertTrue(sotPerTeam in 3.2..6.0,"SOT/team=$sotPerTeam")
        assertTrue(conversion in .08.. .15,"conversion=$conversion")
        assertTrue(zeroShare in .03.. .18,"0:0 share=$zeroShare")
        assertTrue(fourShare in .08.. .30,"4+ share=$fourShare")
        assertTrue(extremeShare<.04,"extreme share=$extremeShare")
        assertTrue(mismatchShare<.006,"4 goals from <=1 xG too frequent: $mismatchShare")
    }
}

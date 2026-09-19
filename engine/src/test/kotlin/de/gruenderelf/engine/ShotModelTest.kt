package de.gruenderelf.engine

import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShotModelTest {
    private fun contextAt(distance: Double, x: Float=.5f, type: ShotType=ShotType.BOX_SHOT, pressure: Double=.35, defenders: Int=1, pass: Double=.65, clear: Boolean=false) =
        ShotContext(x,(distance/105.0).toFloat(),true,type,pressure,defenders,pass,clear,false,true)

    @Test fun closeRangeHasMoreXgThanLongRange() {
        val close=ShotModel.xg(contextAt(7.0,type=ShotType.CLOSE_RANGE,pressure=.30,defenders=1))
        val long=ShotModel.xg(contextAt(30.0,type=ShotType.LONG_RANGE,pressure=.30,defenders=1))
        println("XG_LOCATION close=$close long=$long")
        assertTrue(close>long*8.0)
        assertTrue(long in .005.. .05)
        assertTrue(close in .35.. .90)
    }

    @Test fun centralShotHasMoreXgThanAcuteAngleAtSameDistance() {
        val distance=20.0
        val lateral=14.0
        val y=sqrt(distance*distance-lateral*lateral)/105.0
        val central=ShotModel.xg(contextAt(distance,x=.5f,pressure=.30,defenders=1))
        val acute=ShotModel.xg(ShotContext((.5+lateral/68.0).toFloat(),y.toFloat(),true,ShotType.BOX_SHOT,.30,1,.65,false,false,true))
        println("XG_ANGLE central=$central acute=$acute")
        assertTrue(central>acute*1.25,"central=$central acute=$acute")
    }

    @Test fun shotLocationLabelMatchesActualPitchGeometry() {
        assertTrue(ShotModel.locationLabel(contextAt(13.0)).startsWith("Im Strafraum"))
        assertTrue(ShotModel.locationLabel(contextAt(20.0)).startsWith("Vor dem Strafraum"))
        assertTrue(ShotModel.locationLabel(contextAt(29.0,type=ShotType.LONG_RANGE)).startsWith("Distanz"))
    }

    @Test fun penaltyStaysNearExpectedXg() {
        val xg=ShotModel.xg(contextAt(11.0,type=ShotType.PENALTY,pressure=.05,defenders=0,clear=true))
        assertTrue(xg in .74.. .78,"penalty xG=$xg")
    }

    @Test fun teamXgHasNoTwoGoalCapAndThirtyGoodShotsCanExceedFour() {
        val each=ShotModel.xg(contextAt(13.0,type=ShotType.BOX_SHOT,pressure=.25,defenders=1,pass=.72))
        val total=(1..30).sumOf{each}
        println("XG_NO_CAP each=$each total30=$total")
        assertTrue(total>4.0,"30 chances only produced $total xG")
        assertTrue(total>2.0)
    }

    @Test fun sameChanceBetterFinisherScoresSomewhatMoreOftenAndKeeperMatters() {
        val ctx=contextAt(12.0,type=ShotType.BOX_SHOT,pressure=.35,defenders=1,pass=.65)
        val xg=ShotModel.xg(ctx)
        val weak=Player(1,attributes=Attributes(finishing=35,technique=40),hidden=Hidden(consistency=50,pressure=50),form=6.5,fitness=95.0)
        val elite=Player(2,attributes=Attributes(finishing=92,technique=88),hidden=Hidden(consistency=82,pressure=78),form=7.5,fitness=95.0)
        val averageKeeper=Player(3,position=Position.TW,attributes=Attributes(keeping=50),hidden=Hidden(consistency=60,pressure=55),form=6.5,fitness=95.0,sharpness=65)
        val eliteKeeper=Player(4,position=Position.TW,attributes=Attributes(keeping=92),hidden=Hidden(consistency=84,pressure=78),form=7.4,fitness=98.0,sharpness=90)
        val pWeak=ShotModel.goalProbability(xg,weak,averageKeeper,ctx)
        val pElite=ShotModel.goalProbability(xg,elite,averageKeeper,ctx)
        val pVsEliteKeeper=ShotModel.goalProbability(xg,elite,eliteKeeper,ctx)
        println("EXECUTION weak=$pWeak elite=$pElite eliteVsEliteKeeper=$pVsEliteKeeper xg=$xg")
        assertTrue(pElite>pWeak*1.15)
        assertTrue(pVsEliteKeeper<pElite*.93)
        assertTrue(pElite<xg*1.35+.01)
    }

    @Test fun matchXgIsExactSumOfGeneratedShotsAndGoalsComeOnlyFromShots() {
        repeat(20){seed->
            val w=WorldFactory.createWorld(21000L+seed);val m=MatchEngine.simulateFullMatch(w,w.nextFixture()!!)
            val homeEvents=m.shotEvents.filter{it.clubId==m.homeId};val awayEvents=m.shotEvents.filter{it.clubId==m.awayId}
            assertTrue(abs(m.home.xg-homeEvents.sumOf{it.xg})<1e-9)
            assertTrue(abs(m.away.xg-awayEvents.sumOf{it.xg})<1e-9)
            assertEquals(m.home.shots,homeEvents.size);assertEquals(m.away.shots,awayEvents.size)
            assertEquals(m.home.goals,homeEvents.count{it.outcome==ShotOutcome.GOAL});assertEquals(m.away.goals,awayEvents.count{it.outcome==ShotOutcome.GOAL})
            assertEquals(m.home.goals+m.away.goals,m.goals.size)
            for(stats in listOf(m.home,m.away))assertEquals(stats.shots,stats.shotsOnTarget+stats.shotsOffTarget+stats.blockedShots)
        }
    }

    @Test fun randomnessStillProducesDifferentResults() {
        val scores=mutableSetOf<Pair<Int,Int>>()
        repeat(40){i->val w=WorldFactory.createWorld(22000L+i);val m=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);scores+=m.home.goals to m.away.goals}
        assertTrue(scores.size>=8,"too little variance: $scores")
    }

    @Test fun messiMasterclassChangesRealShotExecutionNotOnlyDisplayedStats() {
        val ctx=contextAt(14.0,type=ShotType.BOX_SHOT,pressure=.45,defenders=2,pass=.60);val xg=ShotModel.xg(ctx)
        val normal=Player(901,attributes=Attributes(finishing=95,technique=95),hidden=Hidden(consistency=85,pressure=85),form=8.0,fitness=99.0)
        val prime=normal.copy(id=902,messiMentored=true)
        val keeper=Player(903,position=Position.TW,attributes=Attributes(keeping=88),hidden=Hidden(consistency=82,pressure=80),form=7.5,fitness=98.0,sharpness=90)
        val normalGoal=ShotModel.goalProbability(xg,normal,keeper,ctx);val primeGoal=ShotModel.goalProbability(xg,prime,keeper,ctx)
        val normalTarget=ShotModel.onTargetProbability(normalGoal,normal,ctx);val primeTarget=ShotModel.onTargetProbability(primeGoal,prime,ctx)
        assertTrue(primeGoal>normalGoal,"prime=$primeGoal normal=$normalGoal");assertTrue(primeTarget>normalTarget);assertTrue(ShotModel.shotSpeed(prime,ctx)>ShotModel.shotSpeed(normal,ctx))
    }

}

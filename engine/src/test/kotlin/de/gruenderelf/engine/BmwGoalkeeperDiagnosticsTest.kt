package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BmwGoalkeeperDiagnosticsTest {
 @Test fun neuerIsActuallyUsedAndOutperformsAnAverageKeeper(){
  var eliteGoals=0;var weakGoals=0;var eliteXg=0.0;var weakXg=0.0
  var eliteSot=0;var weakSot=0;var eliteClean=0;var weakClean=0
  repeat(50){i->
   fun run(weaken:Boolean):Triple<LiveMatch,MatchStats,Player>{
    val w=BmwDeveloperWorldFactory.create(310000L+i)
    val neuer=w.squad().first{it.name=="Manuel Neuer"}
    if(weaken){
     neuer.attributes.keeping=55
     neuer.hidden.consistency=55
     neuer.hidden.pressure=55
     neuer.sharpness=70
     neuer.form=6.5
    }
    w.squad().forEach{it.fitness=100.0;it.injuryWeeks=0;it.unavailableWeeks=0}
    WorldFactory.autoLineup(w,w.user.clubId)
    val f=w.nextFixture()!!
    val m=MatchEngine.simulateFullMatch(w,f)
    val bmwHome=m.homeId==w.user.clubId
    val opp=if(bmwHome)m.away else m.home
    val gkId=(if(bmwHome)m.homeXi else m.awayXi)[Formations.positions(if(bmwHome)m.homeFormation else m.awayFormation).indexOf(Position.TW)]
    assertEquals(neuer.id,gkId,"Manuel Neuer is not in the actual goalkeeper slot")
    return Triple(m,opp,neuer)
   }
   val (em,es,en)=run(false)
   val (wm,ws,wn)=run(true)
   if(i==0)println("NEUER_PROFILE ca=${en.ca} keeping=${en.attributes.keeping} consistency=${en.hidden.consistency} pressure=${en.hidden.pressure} sharpness=${en.sharpness} form=${en.form}")
   eliteGoals+=es.goals;weakGoals+=ws.goals;eliteXg+=es.xg;weakXg+=ws.xg;eliteSot+=es.shotsOnTarget;weakSot+=ws.shotsOnTarget
   if(es.goals==0)eliteClean++;if(ws.goals==0)weakClean++
  }
  val eliteSave=if(eliteSot==0)0.0 else 1.0-eliteGoals.toDouble()/eliteSot
  val weakSave=if(weakSot==0)0.0 else 1.0-weakGoals.toDouble()/weakSot
  println("NEUER_DIAG elite goals=$eliteGoals xg=$eliteXg sot=$eliteSot savePct=$eliteSave clean=$eliteClean")
  println("NEUER_DIAG weak  goals=$weakGoals xg=$weakXg sot=$weakSot savePct=$weakSave clean=$weakClean")
  assertTrue(eliteGoals<weakGoals,"elite Neuer should concede fewer goals: elite=$eliteGoals weak=$weakGoals")
  assertTrue(eliteSave>weakSave,"elite Neuer should save a larger share: elite=$eliteSave weak=$weakSave")
 }

 @Test fun neuerReducesIdenticalShotProbabilityMaterially(){
  val w=BmwDeveloperWorldFactory.create(319999L)
  val neuer=w.squad().first{it.name=="Manuel Neuer"}
  val shooter=Player(999991,position=Position.ST,attributes=Attributes(finishing=90,technique=90),hidden=Hidden(consistency=82,pressure=80),form=7.4,fitness=98.0,sharpness=90)
  val average=Player(999992,position=Position.TW,attributes=Attributes(keeping=65),hidden=Hidden(consistency=65,pressure=65),form=6.5,fitness=95.0,sharpness=70)
  val contexts=listOf(
   ShotContext(.5f,(12.0/105.0).toFloat(),true,ShotType.BOX_SHOT,.35,1,.65),
   ShotContext(.5f,(8.0/105.0).toFloat(),true,ShotType.CLOSE_RANGE,.25,1,.72),
   ShotContext(.5f,(14.0/105.0).toFloat(),true,ShotType.ONE_ON_ONE,.12,0,.82,true)
  )
  for(ctx in contexts){
   val xg=ShotModel.xg(ctx)
   val avg=ShotModel.goalProbability(xg,shooter,average,ctx)
   val neu=ShotModel.goalProbability(xg,shooter,neuer,ctx)
   println("NEUER_SHOT type=${ctx.type} xg=$xg avgKeeper=$avg neuer=$neu reduction=${1-neu/avg}")
   assertTrue(neu<avg*.90,"Neuer effect too small for ${ctx.type}: avg=$avg neuer=$neu")
  }
 }

 @Test fun saveRestartActuallyReturnsBallToTheGoalkeeper(){
  var verified=false
  for(seed in 320000L..320120L){
   val w=BmwDeveloperWorldFactory.create(seed)
   val m=MatchEngine.start(w,w.nextFixture()!!)
   var guard=0
   while(!m.finished&&guard++<800){
    if(m.pendingPossessionReason==PossessionChangeReason.SAVE&&m.pendingPossessionClubId!=0){
     val home=m.pendingPossessionClubId==m.homeId
     val lineup=if(home)m.homeXi else m.awayXi
     val formation=if(home)m.homeFormation else m.awayFormation
     val slot=Formations.positions(formation).indexOf(Position.TW)
     val expected=lineup[slot]
     MatchEngine.step(w,m)
     assertEquals(expected,m.livePlayerId,"save restart did not assign the ball to the goalkeeper")
     assertTrue(w.players[m.livePlayerId]?.position==Position.TW)
     assertTrue(m.liveDetail.contains("Torwart",true))
     verified=true
     break
    }
    when{
     m.pendingDecision->MatchEngine.decide(w,m,Decision.SHOOT)
     m.halfTime->MatchEngine.secondHalf(m)
     m.incidentPause->MatchEngine.resumeIncident(w,m)
     m.assistantSubPending->MatchEngine.acceptAssistantSubstitution(w,m)
     else->MatchEngine.step(w,m)
    }
   }
   if(verified)break
  }
  assertTrue(verified,"no held save found to validate goalkeeper restart")
 }

 @Test fun bmwGoalkeeperTechnologyHasARealShotStoppingEffect(){
  val w=BmwDeveloperWorldFactory.create(321000L)
  val bmw=BmwDeveloperSystems.goalkeeperShotMultiplier(w,w.user.clubId)
  assertTrue(bmw<.98,"BMW goalkeeper technology is not affecting shot stopping: $bmw")
  val normal=WorldFactory.createWorld(321001L)
  assertEquals(1.0,BmwDeveloperSystems.goalkeeperShotMultiplier(normal,normal.user.clubId))
 }
}

package de.gruenderelf.engine

import org.junit.Assert.*
import org.junit.Test

class UnifiedLiveRuntimeTest {
 @Test fun livePresentationUsesCanonicalEngineFrames(){
  val w=WorldFactory.createWorld(99051)
  val m=MatchEngine.start(w)
  w.live=m
  assertTrue(m.ballTrace.isNotEmpty())
  val before=m.ballTrace.size
  repeat(12){
   when{
    m.finished -> Unit
    m.incidentPause -> {
     if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId){
      MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)}
     }
     if(m.incidentPause)MatchEngine.resumeIncident(w,m)
    }
    m.pendingDecision -> MatchEngine.decide(w,m,Decision.PASS)
    m.halfTime -> MatchEngine.secondHalf(m)
    else -> MatchEngine.step(w,m)
   }
  }
  assertTrue(m.ballTrace.size>=before)
  assertTrue(m.ballTrace.all{it.x in .04f.. .96f&&it.y in .03f.. .97f})
  assertEquals(m.ballX,m.ballTrace.last().x)
  assertEquals(m.ballY,m.ballTrace.last().y)
  assertEquals(m.liveEventSerial,m.ballTrace.last().serial)
 }

 @Test fun livePacingHasSingleDeterministicSource(){
  for(phase in LivePhase.entries){
   val slow=MatchEngine.liveFrameDurationMs(phase,MatchSpeed.SLOW)
   val normal=MatchEngine.liveFrameDurationMs(phase,MatchSpeed.NORMAL)
   val fast=MatchEngine.liveFrameDurationMs(phase,MatchSpeed.FAST)
   assertTrue("$phase slow < normal",slow>=normal)
   assertTrue("$phase normal < fast",normal>=fast)
   assertTrue("$phase too fast",fast>=600L)
   assertEquals(normal,MatchEngine.liveFrameDurationMs(phase,MatchSpeed.NORMAL))
  }
 }

 @Test fun ballTraceIsDeduplicatedAndBounded(){
  val w=WorldFactory.createWorld(99052)
  val m=MatchEngine.start(w)
  val initial=m.ballTrace.size
  MatchEngine.captureBallFrame(m)
  assertEquals(initial,m.ballTrace.size)
  repeat(90){i->
   m.liveEventSerial+=1
   m.minute=i.coerceAtMost(90)
   m.ballX=(.04f+((i%80)/100f)).coerceIn(.04f,.96f)
   m.ballY=(.03f+((i%70)/100f)).coerceIn(.03f,.97f)
   MatchEngine.captureBallFrame(m)
  }
  assertEquals(64,m.ballTrace.size)
  assertEquals(m.liveEventSerial,m.ballTrace.last().serial)
  assertEquals(m.ballX,m.ballTrace.last().x)
  assertEquals(m.ballY,m.ballTrace.last().y)
 }

 @Test fun ballTraceSurvivesSaveRoundtrip(){
  val w=WorldFactory.createWorld(99053)
  val m=MatchEngine.start(w)
  w.live=m
  repeat(8){i->
   m.liveEventSerial+=1
   m.minute=i+1
   m.ballX=(.15f+i*.07f).coerceIn(.04f,.96f)
   m.ballY=(.20f+i*.05f).coerceIn(.03f,.97f)
   MatchEngine.captureBallFrame(m)
  }
  val copy=SaveCodec.copy(w)
  val cm=copy.live!!
  assertEquals(m.ballTrace,cm.ballTrace)
  assertEquals(m.ballX,cm.ballTrace.last().x)
  assertEquals(m.ballY,cm.ballTrace.last().y)
 }
 @Test fun completedMatchHistoryDropsHeavyLiveTelemetry(){
  val w=WorldFactory.createWorld(99054)
  val f=w.nextFixture()!!;val m=MatchEngine.start(w,f);MatchEngine.simulateRemaining(w,m)
  assertTrue(m.passEvents.isNotEmpty()||m.shotEvents.isNotEmpty())
  MatchEngine.record(w,m)
  val stored=w.matches.getValue(f.id)
  assertTrue(stored.passEvents.isEmpty());assertTrue(stored.shotEvents.isEmpty());assertTrue(stored.tacticChanges.isEmpty())
  assertEquals(m.home.goals,stored.home.goals);assertEquals(m.away.goals,stored.away.goals);assertEquals(m.goals,stored.goals)
 }

 @Test fun oldHeavyMatchHistoryIsCompactedWhenLoaded(){
  val w=WorldFactory.createWorld(99055)
  val f=w.nextFixture()!!;val m=MatchEngine.start(w,f);MatchEngine.simulateRemaining(w,m);f.played=true
  w.matches[f.id]=MatchRecord(f.id,m.homeId,m.awayId,m.home.copy(),m.away.copy(),m.minute,m.goals.toList(),m.attendance,m.shotEvents.toList(),m.passEvents.toList(),m.tacticChanges.toList(),m.homePens,m.awayPens,m.extraTimePlayed)
  val encoded=SaveCodec.encode(w);val decoded=SaveCodec.decode(encoded);val stored=decoded.matches.getValue(f.id)
  assertTrue(stored.passEvents.isEmpty());assertTrue(stored.shotEvents.isEmpty());assertTrue(stored.tacticChanges.isEmpty())
  assertEquals(m.home.goals,stored.home.goals);assertEquals(m.away.goals,stored.away.goals)
 }

}

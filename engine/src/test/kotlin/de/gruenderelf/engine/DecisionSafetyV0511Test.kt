package de.gruenderelf.engine

import org.junit.Assert.*
import org.junit.Test

class DecisionSafetyV0511Test {
 @Test fun decisionContextShowsDistanceAndOpponentPressure(){
  val w=WorldFactory.createWorld(51101L);val m=MatchEngine.start(w,w.nextFixture()!!);w.live=m
  val self=w.self();val home=self.clubId==m.homeId
  m.ballX=.5f;m.ballY=if(home).17f else .83f;m.pendingDecision=true
  val c=MatchEngine.playerDecisionContext(w,m)
  assertTrue(c.distanceMeters in 12..28)
  assertTrue(c.nearbyOpponents in 1..6)
  assertTrue(c.pressureLabel in setOf("sehr wenige","wenige","mehrere","viele"))
 }

 @Test fun playerDecisionMenuActionsHaveRequestedLabelsAndRemainRuntimeSafe(){
  assertEquals("Schuss",Decision.SHOOT.label)
  assertEquals("Dribbling",Decision.DRIBBLE.label)
  assertEquals("Flanke",Decision.CROSS.label)
  assertEquals("Steilpass",Decision.THROUGH_PASS.label)
  assertEquals("Sicherungspass",Decision.PASS.label)
  for((index,decision) in listOf(Decision.SHOOT,Decision.DRIBBLE,Decision.CROSS,Decision.THROUGH_PASS,Decision.PASS).withIndex()){
   val w=WorldFactory.createWorld(51110L+index);val m=MatchEngine.start(w,w.nextFixture()!!);w.live=m
   val self=w.self();val home=self.clubId==m.homeId
   m.pendingDecision=true;m.ballX=.5f;m.ballY=if(home).16f else .84f;m.decisionShotType=ShotType.BOX_SHOT;m.decisionAssistId=0
   MatchEngine.decide(w,m,decision)
   assertFalse("$decision left the choice open",m.pendingDecision)
   SaveCodec.requireRuntimeIntegrity(w)
  }
 }

 @Test fun runtimeCopyRejectsMissingLiveReferencesBeforeUiCanSeeThem(){
  val w=WorldFactory.createWorld(51120L);val m=MatchEngine.start(w,w.nextFixture()!!);w.live=m
  m.homeXi[0]=Int.MAX_VALUE
  try{SaveCodec.copy(w);fail("corrupt live player reference must be rejected")}catch(_:IllegalArgumentException){}
 }
}

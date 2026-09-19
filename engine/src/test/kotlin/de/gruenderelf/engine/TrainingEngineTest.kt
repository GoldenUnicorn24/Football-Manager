package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class TrainingEngineTest {
 @Test fun messiMasterclassCostsTenMillionAndBoostsBeyondNinetyNine(){
  val w=WorldFactory.createWorld(77777L);w.club().budget=20_000_000L
  val p=w.self();p.attributes=Attributes(99,99,99,99,99,99,99,99,99,99,99)
  val before=w.club().budget
  TrainingEngine.bookMessiMasterclass(w,p.id)
  assertEquals(before-TrainingEngine.MESSI_MASTERCLASS_COST,w.club().budget)
  assertTrue(p.messiMentored);assertEquals("Prime-Messi-Masterclass",p.archetype)
  assertTrue(p.attributes.values().values.all{it>=119})
  assertEquals(127,p.attributes.technique);assertEquals(127,p.attributes.vision)
  assertEquals(125,p.attributes.finishing);assertEquals(124,p.attributes.passing)
  assertEquals(123,p.attributes.pace);assertEquals(125,p.attributes.setPieces)
  assertEquals(100,p.morale);assertEquals(100,p.sharpness);assertTrue(p.hidden.pressure>=75)
  assertFailsWith<IllegalArgumentException>{TrainingEngine.bookMessiMasterclass(w,p.id)}
 }

 @Test fun messiMasterclassPersistsThroughSaveRoundtrip(){
  val w=WorldFactory.createWorld(77778L);w.club().budget=20_000_000L
  val id=w.user.playerId
  w.players.getValue(id).attributes=Attributes(99,99,99,99,99,99,99,99,99,99,99)
  TrainingEngine.bookMessiMasterclass(w,id)
  val copy=SaveCodec.decode(SaveCodec.encode(w));val p=copy.players.getValue(id)
  assertTrue(p.messiMentored);assertEquals("Prime-Messi-Masterclass",p.archetype)
  assertTrue(p.attributes.technique>99);assertEquals(w.club().budget,copy.club().budget)
  assertEquals("Dieser Spieler hat die Messi-Masterclass bereits absolviert.",TrainingEngine.messiMasterclassReason(copy,id))
 }

 @Test fun messiMasterclassRejectsMissingBudgetAndRunningMatch(){
  val w=WorldFactory.createWorld(77779L);val id=w.user.playerId
  w.club().budget=TrainingEngine.MESSI_MASTERCLASS_COST-1
  assertTrue(TrainingEngine.messiMasterclassReason(w,id)?.contains("fehlen")==true)
  w.club().budget=TrainingEngine.MESSI_MASTERCLASS_COST
  w.live=MatchEngine.start(w)
  assertEquals("Die Masterclass kann nicht während eines laufenden Spiels gebucht werden.",TrainingEngine.messiMasterclassReason(w,id))
 }
}

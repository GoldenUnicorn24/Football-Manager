package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class BmwDeveloperModeTest {
 @Test fun developerWorldLoadsCompleteBmwIdentityAndRoster(){
  val w=BmwDeveloperWorldFactory.create(260925L)
  assertTrue(w.developer.enabled)
  assertEquals("BMW FC",w.club().name)
  assertEquals("BMW Performance Arena",w.club().stadium.name)
  assertEquals(92_500,w.club().stadium.capacity)
  assertEquals(24,w.squad().count{!it.youth})
  val leon=w.squad().first{it.name=="Leon Stark"}
  assertEquals(10,leon.number)
  assertEquals(99,leon.ca)
  assertEquals(125,w.developer.playerMeta.getValue(leon.id).potential)
  assertEquals(listOf(Position.OM,Position.ZM,Position.RA,Position.LA),leon.secondary)
 }

 @Test fun bmwStartsTechnologyLeaderAndCompetitionCanDevelop(){
  val w=BmwDeveloperWorldFactory.create(260926L)
  val before=BmwDeveloperSystems.technologyRanking(w)
  assertEquals(w.user.clubId,before.first().clubId)
  val rival=before.first{it.clubId!=w.user.clubId}
  val old=rival.overall
  repeat(20){w.calendar.absoluteWeek++;BmwDeveloperSystems.weekly(w)}
  assertTrue(BmwDeveloperSystems.profile(w,rival.clubId)!!.overall>=old)
  assertEquals(w.user.clubId,BmwDeveloperSystems.technologyRanking(w).first().clubId)
 }

 @Test fun investmentsAndLongevityPersistThroughSave(){
  val w=BmwDeveloperWorldFactory.create(260927L)
  val before=w.club().budget
  BmwDeveloperSystems.invest(w,TechDomain.AI,1_000_000_000L)
  assertEquals(before-1_000_000_000L,w.club().budget)
  val neuer=w.squad().first{it.lastName=="Neuer"}
  repeat(52){BmwDeveloperSystems.weekly(w)}
  val age=BmwDeveloperSystems.effectiveAge(w,neuer)
  assertTrue(age<(w.calendar.season-neuer.birthYear))
  assertTrue((w.developer.longevity[neuer.id]?.biologicalYearsReduced?:0.0)<=4.0)
  val copy=SaveCodec.decode(SaveCodec.encode(w))
  assertTrue(copy.developer.enabled)
  assertTrue(copy.developer.technology.isNotEmpty())
  assertEquals(125,copy.developer.playerMeta.values.first{copy.players[it.playerId]?.name=="Leon Stark"}.potential)
 }

 @Test fun bmwTacticalShapesAreSupported(){
  assertTrue("4-4-1-1" in Formations.all)
  assertTrue("4-5-1" in Formations.all)
  assertEquals(11,Formations.positions("4-4-1-1").size)
 }
}

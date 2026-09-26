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

 @Test fun bmwEliteChanceConversionIsCompetitive(){
  val w=BmwDeveloperWorldFactory.create(261001L)
  val clubId=w.user.clubId
  val base=w.fixtures.first{it.homeId==clubId||it.awayId==clubId}
  var wins=0;var draws=0;var losses=0;var xg=0.0;var goals=0;var against=0
  repeat(24){i->
   w.squad(base.homeId).forEach{it.fitness=100.0;it.injuryWeeks=0;it.unavailableWeeks=0}
   w.squad(base.awayId).forEach{it.fitness=100.0;it.injuryWeeks=0;it.unavailableWeeks=0}
   val m=MatchEngine.simulateFullMatch(w,base.copy(id=900000+i,played=false))
   val own=if(m.homeId==clubId)m.home else m.away
   val opp=if(m.homeId==clubId)m.away else m.home
   xg+=own.xg;goals+=own.goals;against+=opp.goals
   when{own.goals>opp.goals->wins++;own.goals==opp.goals->draws++;else->losses++}
  }
  val ratio=goals/xg.coerceAtLeast(.01)
  println("BMW_BALANCE wins=$wins draws=$draws losses=$losses goals=$goals against=$against xg=$xg conversionRatio=$ratio")
  assertTrue(xg>30.0,"BMW did not create enough chances: xG=$xg")
  assertTrue(ratio>.62,"BMW chance conversion is too weak relative to xG: $ratio")
  assertTrue(wins>=12,"BMW elite squad underperforms too often: $wins wins / 24")
 }

 @Test fun leonStartsWithMessiMasterclassOpenAndCanExceedOneHundred(){
  val w=BmwDeveloperWorldFactory.create(261002L)
  val leon=w.squad().first{it.name=="Leon Stark"}
  assertFalse(leon.messiMentored)
  assertEquals("BMW Free 10",leon.archetype)
  assertNull(TrainingEngine.messiMasterclassReason(w,leon.id))
  assertTrue(leon.attributes.values().values.all{it<=100})
  TrainingEngine.bookMessiMasterclass(w,leon.id)
  assertTrue(leon.messiMentored)
  assertEquals("Prime-Messi-Masterclass",leon.archetype)
  assertTrue(leon.attributes.values().values.any{it>100})
  assertTrue(leon.attributes.technique>100)
  assertTrue(leon.attributes.vision>100)
  assertTrue(leon.attributes.finishing>100)
 }

 @Test fun legacyBmwSaveWithFalseCompletionFlagIsRepairedButRealMasterclassIsKept(){
  val w=BmwDeveloperWorldFactory.create(261003L)
  val leon=w.squad().first{it.name=="Leon Stark"}
  leon.messiMentored=true
  leon.archetype="Prime-Messi-Masterclass"
  assertTrue(BmwDeveloperWorldFactory.repairDeveloperSave(w))
  assertFalse(leon.messiMentored)
  assertEquals("BMW Free 10",leon.archetype)

  TrainingEngine.bookMessiMasterclass(w,leon.id)
  val technique=leon.attributes.technique
  assertFalse(BmwDeveloperWorldFactory.repairDeveloperSave(w))
  assertTrue(leon.messiMentored)
  assertEquals(technique,leon.attributes.technique)
 }

 @Test fun globalRatingsCanReach150ButEliteProgressionIsLockedBehindFacilities(){
  val all=Attributes(150,150,150,150,150,150,150,150,150,150,150)
  val elite=Player(99001,position=Position.ST,attributes=all,hidden=Hidden(potential=150))
  assertEquals(150,elite.ca)
  assertEquals(150,elite.ratingAt(Position.ST))
  assertFalse(elite.attributes.improve(Focus.FINISHING,200))

  val w=WorldFactory.createWorld(261010L);val p=w.self()
  p.position=Position.ST;p.attributes=Attributes(99,99,99,99,70,99,99,99,99,10,99)
  p.hidden.potential=99;p.hidden.professionalism=90;p.hidden.development=90
  w.club().budget=2_000_000_000L
  w.club().stadium.training=100;w.club().stadium.gym=100;w.club().stadium.medicine=100;w.club().dynamics.staffQuality=100
  assertTrue(IntensiveTrainingSystem.potentialReason(w,p.id)?.contains("105")==true)
  w.club().stadium.training=105;w.club().stadium.gym=105;w.club().stadium.medicine=105
  assertNull(IntensiveTrainingSystem.potentialReason(w,p.id))
  IntensiveTrainingSystem.startPotential(w,p.id,Focus.FINISHING)
  assertEquals(12,w.intensiveTraining.first{it.playerId==p.id}.totalWeeks)
 }

 @Test fun bmwTechnologyRaceStartsOnlyWithBarcaThenBayern(){
  val w=BmwDeveloperWorldFactory.create(261011L)
  val tech0=BmwDeveloperSystems.technologyRanking(w)
  assertEquals(2,tech0.size)
  assertTrue(tech0.any{it.clubId==w.user.clubId})
  val barca=w.clubs.values.first{it.name.contains("Barcelona",true)||it.shortName=="BAR"}
  val bayern=w.clubs.values.first{it.name.contains("Bayern",true)||it.shortName=="FCB"}
  assertNotNull(BmwDeveloperSystems.profile(w,barca.id))
  assertNull(w.developer.technology[bayern.id])
  bayern.budget=500_000_000L
  repeat(26){BmwDeveloperSystems.weekly(w)}
  assertNotNull(w.developer.technology[bayern.id])
  assertTrue(w.developer.technology.getValue(bayern.id).overall<w.developer.technology.getValue(barca.id).overall)
  assertTrue(w.developer.technology.values.all{it.clubId==w.user.clubId||it.clubId==barca.id||it.clubId==bayern.id})
 }

 @Test fun otherClubsMustPayAndWaitBeforeStartingAi(){
  val w=BmwDeveloperWorldFactory.create(261012L)
  val bayern=w.clubs.values.first{it.name.contains("Bayern",true)||it.shortName=="FCB"};bayern.budget=500_000_000L
  w.clubs.values.filter{it.id!=w.user.clubId&&!it.name.contains("Barcelona",true)&&!it.name.contains("Bayern",true)}.forEach{it.reputation=80;it.budget=100_000_000L}
  val candidate=w.clubs.values.first{it.id!=w.user.clubId&&!it.name.contains("Barcelona",true)&&!it.name.contains("Bayern",true)&&it.tier>0}
  candidate.reputation=99;candidate.budget=900_000_000L
  candidate.stadium.training=100;candidate.stadium.medicine=100;candidate.stadium.youth=100;candidate.stadium.gym=100;candidate.stadium.pitchQuality=100
  repeat(52){BmwDeveloperSystems.weekly(w)}
  assertNull(w.developer.technology[candidate.id])
  while(w.developer.weeksActive<78)BmwDeveloperSystems.weekly(w)
  assertNotNull(w.developer.technology[candidate.id])
  assertTrue(candidate.budget<=400_000_000L)
 }

 @Test fun bmwFacilitiesUseElite150Scale(){
  val w=BmwDeveloperWorldFactory.create(261013L)
  assertTrue(w.club().stadium.training>100)
  assertTrue(w.club().stadium.medicine>100)
  val before=w.club().stadium.training
  val price=ConstructionEngine.price(w,Facility.TRAINING)
  assertTrue(price>=75_000_000L)
  ConstructionEngine.start(w,Facility.TRAINING)
  val project=w.construction.first{it.facility==Facility.TRAINING}
  assertTrue(project.totalWeeks>=12)
  repeat(project.totalWeeks){ConstructionEngine.advance(w)}
  assertTrue(w.club().stadium.training>before)
  assertTrue(w.club().stadium.training<=FACILITY_LEVEL_MAX)
 }

 @Test fun singleNamePlayersHaveVisibleLineupName(){
  val p=Player(99100,firstName="Gavi",lastName="")
  assertEquals("Gavi",p.shortName)
 }
}

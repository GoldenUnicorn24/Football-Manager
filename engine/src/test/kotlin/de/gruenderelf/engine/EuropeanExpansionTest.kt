package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class EuropeanExpansionTest {
 @Test fun expandedLeagueDatabaseHasUniquePlayableStructures(){
  val tiers=RealModeDatabase.leagues.map{it.tier}
  assertEquals(tiers.size,tiers.distinct().size,"Jede Liga braucht eine eindeutige interne Tier-ID")
  val keys=RealModeDatabase.options.map{it.key}
  assertEquals(keys.size,keys.distinct().size,"Vereins-Keys muessen eindeutig bleiben")
  RealModeDatabase.leagues.forEach{league->
   assertTrue(league.clubs.size>=2,"${league.name} hat zu wenige Vereine")
   assertEquals(0,league.clubs.size%2,"${league.name} muss fuer den Rundenspielplan eine gerade Vereinszahl haben")
  }
  assertEquals(10,RealModeDatabase.leaguesForCountry("Kroatien").first{it.level==1}.clubs.size)
  assertEquals(16,RealModeDatabase.leaguesForCountry("Russland").first{it.level==1}.clubs.size)
  assertEquals(18,RealModeDatabase.leaguesForCountry("Niederlande").first{it.level==1}.clubs.size)
  assertEquals(18,RealModeDatabase.leaguesForCountry("Belgien").first{it.level==1}.clubs.size)
  assertEquals(12,RealModeDatabase.leaguesForCountry("Österreich").first{it.level==1}.clubs.size)
  assertEquals(18,RealModeDatabase.leaguesForCountry("Polen").first{it.level==1}.clubs.size)
  assertEquals(16,RealModeDatabase.leaguesForCountry("Schweden").first{it.level==1}.clubs.size)
 }

 @Test fun domesticCupIdsAreUniqueAndCoverExpandedCountries(){
  val cups=EuropeanLeagueData.domesticCups
  assertEquals(cups.size,cups.map{it.id}.distinct().size)
  val countries=cups.map{it.country}.toSet()
  listOf("Deutschland","England","Spanien","Italien","Frankreich","Kroatien","Russland","Niederlande","Belgien","Österreich","Polen","Schweden").forEach{
   assertTrue(it in countries,"Nationaler Pokal fehlt fuer $it")
  }
  assertEquals("Copa del Rey",EuropeanLeagueData.cupById("COPA_DEL_REY")?.name)
  assertEquals("Hrvatski nogometni kup",EuropeanLeagueData.cupById("HRVATSKI_KUP")?.name)
  assertEquals("Russian Cup",EuropeanLeagueData.cupById("RUSSIAN_CUP")?.name)
 }

 @Test fun realWorldSchedulesParallelCupsAndKeepsRussiaOutOfUefa(){
  val croatianLeague=RealModeDatabase.leaguesForCountry("Kroatien").first{it.level==1}
  val person=PlayerDraft(firstName="Test",lastName="Manager",birthYear=2000,nationality="Deutschland",position=Position.ZM,number=10)
  val w=WorldFactory.createRealModeWorld(260923L,croatianLeague.clubs.first().key,person)

  val cupIds=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP}.map{it.group}.filter{it.isNotBlank()}.toSet()
  assertTrue("DFB_POKAL" in cupIds)
  assertTrue("COPA_DEL_REY" in cupIds)
  assertTrue("HRVATSKI_KUP" in cupIds)
  assertTrue("RUSSIAN_CUP" in cupIds)

  val russianIds=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)=="Russland"}.flatMap{it.clubIds}.toSet()
  val uefaIds=w.fixtures.filter{it.competition==CompetitionType.CHAMPIONS_LEAGUE||it.competition==CompetitionType.EUROPA_LEAGUE}
   .flatMap{listOf(it.homeId,it.awayId)}.toSet()
  assertTrue(russianIds.intersect(uefaIds).isEmpty())

  val ownCup=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP&&it.group=="HRVATSKI_KUP"}
  assertTrue(ownCup.isNotEmpty())
  assertTrue(ownCup.all{w.clubs[it.homeId]?.tier==15&&w.clubs[it.awayId]?.tier==15})
 }
}

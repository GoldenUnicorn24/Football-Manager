package de.gruenderelf.engine

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import kotlin.test.*

class V0516NotificationTest {
 @Test fun notificationBadgesOnlyCountNewItemsAndAcknowledgePrecisely(){
  val w=WorldFactory.createWorld(51601L)
  val seniors=w.squad().filter{!it.youth&&it.id!=w.user.playerId}
  NotificationSystem.acknowledgeSquad(w);NotificationSystem.acknowledgeScoutReports(w)
  val p=seniors.first{it.contractYears>1};p.fitness=50.0;p.contractYears=1
  val external=w.players.values.first{it.clubId!=0&&it.clubId!=w.user.clubId&&!it.retired}
  w.scoutReports[external.id]=ScoutReport(playerId=external.id,progress=100,note="Scouting abgeschlossen")

  assertEquals(2,NotificationSystem.squadBadge(w))
  assertEquals(1,NotificationSystem.moreBadge(w))
  NotificationSystem.acknowledgeSquad(w);NotificationSystem.acknowledgeScoutReports(w)
  assertEquals(0,NotificationSystem.squadBadge(w));assertEquals(0,NotificationSystem.moreBadge(w))

  val newlyTired=seniors[1];newlyTired.fitness=49.0
  assertEquals(1,NotificationSystem.tiredCount(w),"Ein neu ermüdeter Spieler in derselben Woche muss wieder gemeldet werden.")
  val secondExternal=w.players.values.first{it.clubId!=0&&it.clubId!=w.user.clubId&&!it.retired&&it.id!=external.id}
  w.scoutReports[secondExternal.id]=ScoutReport(playerId=secondExternal.id,progress=100,note="Scouting abgeschlossen")
  assertEquals(1,NotificationSystem.moreBadge(w),"Nur der neu fertiggestellte Scoutbericht darf gezählt werden.")

  NotificationSystem.acknowledgeSquad(w);w.calendar.season++
  val expiringNow=w.squad().count{!it.youth&&it.id!=w.user.playerId&&it.contractYears<=1}
  assertTrue(expiringNow>0)
  assertEquals(expiringNow,NotificationSystem.contractCount(w),"Alle aktuell auslaufenden Verträge dürfen in einer neuen Saison erneut erscheinen.")
 }

 @Test fun v6MigrationClearsTheOldPersistentNineStyleBadges(){
  val w=WorldFactory.createWorld(51602L)
  val allSeniors=w.squad().filter{!it.youth&&it.id!=w.user.playerId}
  allSeniors.forEach{it.contractYears=3}
  val seniors=allSeniors.take(9);assertEquals(9,seniors.size)
  seniors.forEach{it.contractYears=1}
  val external=w.players.values.filter{it.clubId!=0&&it.clubId!=w.user.clubId&&!it.retired}.take(9)
  external.forEach{w.scoutReports[it.id]=ScoutReport(playerId=it.id,progress=100,note="Scouting abgeschlossen")}
  assertEquals(9,NotificationSystem.contractCount(w));assertEquals(9,NotificationSystem.moreBadge(w))

  val root=SaveCodec.json.parseToJsonElement(SaveCodec.encode(w)).jsonObject.toMutableMap()
  root.remove("notifications");root["saveVersion"]=JsonPrimitive(6)
  val migrated=SaveCodec.decode(JsonObject(root).toString())
  assertEquals(SAVE_VERSION,migrated.saveVersion)
  assertEquals(0,NotificationSystem.squadBadge(migrated),"Bestehende v0.5.15-Kaderhinweise werden bei der Migration einmalig quittiert.")
  assertEquals(0,NotificationSystem.moreBadge(migrated),"Bestehende fertige Scoutberichte werden bei der Migration einmalig quittiert.")
 }
}

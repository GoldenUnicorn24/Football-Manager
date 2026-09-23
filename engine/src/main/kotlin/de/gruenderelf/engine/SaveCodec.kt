package de.gruenderelf.engine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
object SaveCodec {
 val json=Json{encodeDefaults=false;ignoreUnknownKeys=true;coerceInputValues=true}
 private val versionRegex=Regex("\\\"saveVersion\\\"\\s*:\\s*(\\d+)")
 fun encode(w: World): String {
  // Direkt in einen String serialisieren: ein zusätzlicher JsonElement-Baum kann bei großen
  // Real-Mode-Welten hunderte MB Peak-RAM kosten. saveVersion wird kompakt vorne ergänzt,
  // falls encodeDefaults=false den Defaultwert ausgelassen hat.
  val encoded=json.encodeToString(w)
  if(versionRegex.containsMatchIn(encoded))return encoded
  require(encoded.startsWith("{")&&encoded.endsWith("}")){"Spielstand konnte nicht serialisiert werden."}
  return if(encoded.length==2) "{\"saveVersion\":${w.saveVersion}}" else "{\"saveVersion\":${w.saveVersion},"+encoded.substring(1)
 }
 fun copy(w: World)=decode(encode(w))
 fun requireRuntimeIntegrity(w:World){
  require(w.user.clubId in w.clubs&&w.user.playerId in w.players){"Verein oder Spielertrainer fehlt."}
  val live=w.live?:return
  require(live.homeId in w.clubs&&live.awayId in w.clubs&&live.homeId!=live.awayId){"Laufende Partie hat ungültige Vereine."}
  require(live.homeXi.size==11&&live.awayXi.size==11&&live.period in 1..5&&live.minute in 0..120&&live.stoppageMinute in 0..10){"Laufende Partie beschädigt."}
  require(live.ballX in .0f..1f&&live.ballY in .0f..1f){"Ballposition der laufenden Partie ist ungültig."}
  fun validTeam(ids:List<Int>,clubId:Int)=ids.all{id->id==0||w.players[id]?.clubId==clubId}
  require(validTeam(live.homeXi,live.homeId)&&validTeam(live.awayXi,live.awayId)&&validTeam(live.homeBench,live.homeId)&&validTeam(live.awayBench,live.awayId)){"Spieler einer laufenden Partie fehlen oder gehören zum falschen Verein."}
  require(live.homeXi.filter{it!=0}.distinct().size==live.homeXi.count{it!=0}&&live.awayXi.filter{it!=0}.distinct().size==live.awayXi.count{it!=0}){"Spieler ist in einer laufenden Aufstellung doppelt vorhanden."}
  fun validPlayer(id:Int)=id==0||id in w.players
  val referenced=buildList{
   add(live.livePlayerId);add(live.incidentPlayerId);add(live.pendingCornerPlayerId);add(live.pendingSetPiecePlayerId);add(live.decisionAssistId);add(live.pendingVarKeeperId);add(live.assistantSubOutId);add(live.assistantSubInId);add(live.assistantSubRejectedOutId);add(live.assistantSubRejectedInId)
   addAll(live.injured);addAll(live.sentOff);addAll(live.participation);addAll(live.yellows.keys);addAll(live.minutesPlayed.keys);addAll(live.playerPerformance.keys);addAll(live.assistantSubOutIds);addAll(live.assistantSubInIds);addAll(live.assistantSubRejectedPlayers)
   live.goals.forEach{g->add(g.playerId);add(g.assistId)}
   live.penaltyShootout.forEach{k->add(k.playerId);add(k.keeperId)}
  }
  require(referenced.all(::validPlayer)){"Laufende Partie verweist auf einen fehlenden Spieler."}
  fun validClub(id:Int)=id==0||id==live.homeId||id==live.awayId
  require(listOf(live.liveClubId,live.incidentClubId,live.pendingCornerClubId,live.pendingSetPieceClubId,live.pendingPossessionClubId,live.chainOwnerClubId).all(::validClub)){"Laufende Partie verweist auf einen ungültigen Verein."}
  if(live.pendingDecision){val self=w.players[w.user.playerId];require(self!=null&&(self.clubId==live.homeId||self.clubId==live.awayId)&&w.user.playerId in (live.homeXi+live.awayXi)){"Spielerentscheidung ohne aktiven Spieler."}}
 }
 private fun compactFinishedMatchHistory(w:World){
  for((id,m) in w.matches.toMap())if(m.shotEvents.isNotEmpty()||m.passEvents.isNotEmpty()||m.tacticChanges.isNotEmpty())
   w.matches[id]=m.copy(shotEvents=emptyList(),passEvents=emptyList(),tacticChanges=emptyList())
 }
 fun versionOf(text:String):Int=versionRegex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?:1
 fun decode(text: String): World {
  require(text.length<=128*1024*1024){"Spielstand ist zu groß (maximal 128 MB)."}
  val version=versionOf(text);val legacyVersion=version<=3
  require(version in 1..SAVE_VERSION){"Dieser Spielstand benötigt eine neuere Gründerelf-Version."}
  // Aktuelle Saves direkt in World dekodieren. Der frühere Umweg über einen vollständigen
  // JsonElement-Baum verdoppelte bei großen Real-Mode-Welten kurzzeitig den RAM-Verbrauch.
  val w=if(version==SAVE_VERSION) json.decodeFromString<World>(text) else {
   val root=json.parseToJsonElement(text).jsonObject.toMutableMap()
   if(version==1){root.putIfAbsent("live",JsonNull);root.putIfAbsent("relationships",JsonObject(emptyMap()));root["saveVersion"]=JsonPrimitive(2)}
   if(version<=2)root["saveVersion"]=JsonPrimitive(3)
   if(version<=3)root["saveVersion"]=JsonPrimitive(4)
   json.decodeFromJsonElement<World>(JsonObject(root))
  }
  compactFinishedMatchHistory(w)
  // IDs zuerst stabilisieren, damit alte Spielstände gefahrlos um neue Gastvereine/Wettbewerbe ergänzt werden können.
  w.nextIds.player=maxOf(w.nextIds.player,(w.players.keys.maxOrNull()?:0)+1);w.nextIds.fixture=maxOf(w.nextIds.fixture,(w.fixtures.maxOfOrNull{it.id}?:0)+1)
  val realMode=w.privateTopClubMode&&w.leagues.size>=5
  require((realMode&&w.clubs.size>=w.leagues.sumOf{it.clubIds.size})||((!realMode)&&w.leagues.size==10&&w.clubs.size>=120)){"Keine vollständige Welt im Spielstand."}
  require(w.user.clubId in w.clubs&&w.user.playerId in w.players&&w.self().clubId==w.user.clubId){"Verein oder Spielertrainer fehlt."}
  val userLeague=w.leagues.firstOrNull{w.user.clubId in it.clubIds}?:error("Nutzerliga fehlt.")
  val userRounds=(userLeague.clubIds.size-1)*2
  require(w.calendar.matchday in 1..maxOf(userRounds,w.leagues.maxOf{(it.clubIds.size-1)*2},w.fixtures.maxOfOrNull{it.matchday}?:0)){"Ungültiger Spieltag."}
  val leagueClubIds=w.leagues.flatMap{it.clubIds}.toSet()
  if(realMode){
   require(w.leagues.map{it.tier}.toSet().size==w.leagues.size&&w.leagues.all{it.clubIds.size in 6..24&&it.clubIds.size%2==0}&&leagueClubIds.size==w.leagues.sumOf{it.clubIds.size}&&leagueClubIds.all{it in w.clubs}){"Real-Modus-Ligastruktur beschädigt."}
  }else require(w.leagues.map{it.tier}.toSet()==(1..10).toSet()&&w.leagues.all{it.clubIds.size==12}&&leagueClubIds.size==120&&leagueClubIds.all{it in w.clubs}){"Ligastruktur beschädigt."}
  WorldFactory.migrateGeneratedIdentity(w)
  if(version<=8&&w.privateTopClubMode)WorldFactory.migrateExpandedRealModeLeagues(w)
  w.players.values.filter{it.firstName=="Spieler"&&it.lastName.isNotBlank()}.forEach{it.firstName=it.lastName;it.lastName=""}
  for(h in w.history.filter{it.clubId==w.user.clubId&&it.awards.any{award->award.startsWith("Meister")}}){
   val title="Meister: ${h.league}"
   if(w.trophies.none{it.season==h.season&&it.competition==title})w.trophies.add(Trophy(h.season,title,h.clubId,0))
  }
  CompetitionEngine.ensureForLoadedWorld(w)
  require(w.players.all{(id,p)->id==p.id&&(p.clubId==0||p.clubId in w.clubs)}){"Spielerliste beschädigt."}
  val leagueFixtures=w.fixtures.filter{it.competition==CompetitionType.LEAGUE}
  val expectedLeagueFixtures=w.leagues.sumOf{it.clubIds.size*(it.clubIds.size-1)}
  val maxLeagueRound=w.leagues.maxOf{(it.clubIds.size-1)*2}
  val maxFixtureDay=maxOf(maxLeagueRound,w.fixtures.maxOfOrNull{it.matchday}?:maxLeagueRound)
  require(leagueFixtures.size==expectedLeagueFixtures&&w.fixtures.map{it.id}.toSet().size==w.fixtures.size&&w.fixtures.all{it.homeId in w.clubs&&it.awayId in w.clubs&&it.homeId!=it.awayId&&it.matchday in 1..maxFixtureDay&&(!it.played||it.id in w.matches)}){"Spielplan oder Ergebnisse unvollständig."}
  for(c in w.clubs.values){require(c.tier>=0&&c.stadium.capacity>0){"Vereinsdaten beschädigt."};if(c.tactics.formation !in Formations.all)c.tactics.formation="4-4-2";if(c.tactics.xi.size!=11||c.tactics.xi.any{it!=0&&w.players[it]?.clubId!=c.id})WorldFactory.autoLineup(w,c.id)}
  if(w.training.days.size!=7)w.training.days=TrainingPlan().days
  w.training.extra=w.training.extra.filter{w.players[it.playerId]?.clubId==w.user.clubId}.distinctBy{it.playerId}.take(if(w.assistantCoach.autoSeniorTraining||w.assistantCoach.autoYouthTraining)6 else if(w.privateTopClubMode)4 else if(w.club().tier>=7)2 else 4).toMutableList()
  w.live?.let{require(w.nextFixture()?.id==it.fixtureId&&it.homeXi.size==11&&it.awayXi.size==11&&it.minute in 0..110){"Laufende Partie beschädigt."}}
  requireRuntimeIntegrity(w)
  w.nextIds.player=maxOf(w.nextIds.player,(w.players.keys.maxOrNull()?:0)+1);w.nextIds.fixture=maxOf(w.nextIds.fixture,(w.fixtures.maxOfOrNull{it.id}?:0)+1);w.nextIds.news=maxOf(w.nextIds.news,(w.news.maxOfOrNull{it.id}?:0)+1);w.nextIds.construction=maxOf(w.nextIds.construction,(w.construction.maxOfOrNull{it.id}?:0)+1);w.nextIds.negotiation=maxOf(w.nextIds.negotiation,(w.negotiations.keys.maxOrNull()?:0)+1);w.nextIds.sponsor=maxOf(w.nextIds.sponsor,(w.clubs.values.flatMap{it.sponsorDeals+it.sponsorOffers}.maxOfOrNull{it.id}?:0)+1);w.clubs.values.forEach{if(legacyVersion)YouthEngine.configureClub(it);ClubSystems.clamp(it)};w.intensiveTraining.removeAll{it.playerId !in w.players||w.players[it.playerId]?.clubId!=w.user.clubId};EconomySystem.initialize(w);w.saveVersion=SAVE_VERSION
  return w
 }
}

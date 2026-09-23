package de.gruenderelf.engine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
object SaveCodec {
 val json=Json{encodeDefaults=true;ignoreUnknownKeys=true;coerceInputValues=true}
 fun encode(w: World)=json.encodeToString(w)
 fun copy(w: World)=decode(encode(w))
 fun decode(text: String): World {
  require(text.length<=32*1024*1024){"Spielstand ist zu groß (maximal 32 MB)."}
  val root=json.parseToJsonElement(text).jsonObject.toMutableMap();val version=root["saveVersion"]?.jsonPrimitive?.intOrNull?:1;val legacyVersion=version<=3
  require(version in 1..SAVE_VERSION){"Dieser Spielstand benötigt eine neuere Gründerelf-Version."}
  if(version==1){root.putIfAbsent("live",JsonNull);root.putIfAbsent("relationships",JsonObject(emptyMap()));root["saveVersion"]=JsonPrimitive(2)}
  if(version<=2)root["saveVersion"]=JsonPrimitive(3)
  if(version<=3)root["saveVersion"]=JsonPrimitive(4)
  val w=json.decodeFromJsonElement<World>(JsonObject(root))
  // IDs zuerst stabilisieren, damit alte Spielstände gefahrlos um neue Gastvereine/Wettbewerbe ergänzt werden können.
  w.nextIds.player=maxOf(w.nextIds.player,(w.players.keys.maxOrNull()?:0)+1);w.nextIds.fixture=maxOf(w.nextIds.fixture,(w.fixtures.maxOfOrNull{it.id}?:0)+1)
  val realMode=w.privateTopClubMode&&w.leagues.size in 5..9
  val legacyTopclub=w.privateTopClubMode&&w.leagues.size==10
  require((realMode&&w.clubs.size>=w.leagues.sumOf{it.clubIds.size})||((!realMode)&&w.leagues.size==10&&w.clubs.size>=120)){"Keine vollständige Welt im Spielstand."}
  require(w.user.clubId in w.clubs&&w.user.playerId in w.players&&w.self().clubId==w.user.clubId){"Verein oder Spielertrainer fehlt."}
  val userLeague=w.leagues.firstOrNull{w.user.clubId in it.clubIds}?:error("Nutzerliga fehlt.")
  val userRounds=(userLeague.clubIds.size-1)*2
  require(w.calendar.matchday in 1..userRounds){"Ungültiger Spieltag."}
  val leagueClubIds=w.leagues.flatMap{it.clubIds}.toSet()
  if(realMode){
   require(w.leagues.map{it.tier}.toSet()==(1..w.leagues.size).toSet()&&w.leagues.all{it.clubIds.size>=10&&it.clubIds.size%2==0}&&leagueClubIds.size==w.leagues.sumOf{it.clubIds.size}&&leagueClubIds.all{it in w.clubs}){"Real-Modus-Ligastruktur beschädigt."}
  }else require(w.leagues.map{it.tier}.toSet()==(1..10).toSet()&&w.leagues.all{it.clubIds.size==12}&&leagueClubIds.size==120&&leagueClubIds.all{it in w.clubs}){"Ligastruktur beschädigt."}
  WorldFactory.migrateGeneratedIdentity(w)
  CompetitionEngine.ensureForLoadedWorld(w)
  require(w.players.all{(id,p)->id==p.id&&(p.clubId==0||p.clubId in w.clubs)}){"Spielerliste beschädigt."}
  val leagueFixtures=w.fixtures.filter{it.competition==CompetitionType.LEAGUE}
  val expectedLeagueFixtures=w.leagues.sumOf{it.clubIds.size*(it.clubIds.size-1)}
  val maxLeagueRound=w.leagues.maxOf{(it.clubIds.size-1)*2}
  val maxFixtureDay=maxOf(maxLeagueRound,w.fixtures.maxOfOrNull{it.matchday}?:maxLeagueRound)
  require(leagueFixtures.size==expectedLeagueFixtures&&w.fixtures.map{it.id}.toSet().size==w.fixtures.size&&w.fixtures.all{it.homeId in w.clubs&&it.awayId in w.clubs&&it.homeId!=it.awayId&&it.matchday in 1..maxFixtureDay&&(!it.played||it.id in w.matches)}){"Spielplan oder Ergebnisse unvollständig."}
  for(c in w.clubs.values){require((c.tier==0||if(realMode)c.tier in 1..w.leagues.size else c.tier in 1..10)&&c.stadium.capacity>0){"Vereinsdaten beschädigt."};if(c.tactics.formation !in Formations.all)c.tactics.formation="4-4-2";if(c.tactics.xi.size!=11||c.tactics.xi.any{it!=0&&w.players[it]?.clubId!=c.id})WorldFactory.autoLineup(w,c.id)}
  if(w.training.days.size!=7)w.training.days=TrainingPlan().days
  w.training.extra=w.training.extra.filter{w.players[it.playerId]?.clubId==w.user.clubId}.distinctBy{it.playerId}.take(if(w.assistantCoach.autoSeniorTraining||w.assistantCoach.autoYouthTraining)6 else if(w.privateTopClubMode)4 else if(w.club().tier>=7)2 else 4).toMutableList()
  w.live?.let{require(w.nextFixture()?.id==it.fixtureId&&it.homeXi.size==11&&it.awayXi.size==11&&it.minute in 0..110){"Laufende Partie beschädigt."}}
  w.nextIds.player=maxOf(w.nextIds.player,(w.players.keys.maxOrNull()?:0)+1);w.nextIds.fixture=maxOf(w.nextIds.fixture,(w.fixtures.maxOfOrNull{it.id}?:0)+1);w.nextIds.news=maxOf(w.nextIds.news,(w.news.maxOfOrNull{it.id}?:0)+1);w.nextIds.construction=maxOf(w.nextIds.construction,(w.construction.maxOfOrNull{it.id}?:0)+1);w.nextIds.negotiation=maxOf(w.nextIds.negotiation,(w.negotiations.keys.maxOrNull()?:0)+1);w.nextIds.sponsor=maxOf(w.nextIds.sponsor,(w.clubs.values.flatMap{it.sponsorDeals+it.sponsorOffers}.maxOfOrNull{it.id}?:0)+1);w.clubs.values.forEach{if(legacyVersion)YouthEngine.configureClub(it);ClubSystems.clamp(it)};w.intensiveTraining.removeAll{it.playerId !in w.players||w.players[it.playerId]?.clubId!=w.user.clubId};EconomySystem.initialize(w);w.saveVersion=SAVE_VERSION
  return w
 }
}

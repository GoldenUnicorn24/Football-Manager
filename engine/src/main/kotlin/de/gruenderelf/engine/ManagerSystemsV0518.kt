package de.gruenderelf.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Recovered v0.5.18 systems reconstructed from the shipped APK. */
object CompetitionPrizeSystem {
 fun winnerId(w: World, competition: CompetitionType, group:String=""): Int = w.fixtures
  .filter { it.season == w.calendar.season && it.competition == competition && (group.isBlank()||CompetitionEngine.cupGroup(w,it)==group) && it.played && it.winnerId != 0 }
  .maxWithOrNull(compareBy<Fixture> { it.round }.thenBy { it.matchday }.thenBy { it.id })?.winnerId ?: 0

 fun award(w: World, key: String, clubId: Int, amount: Long): Boolean {
  val club = w.clubs[clubId] ?: return false
  if (clubId == 0 || amount <= 0L || key in w.paidSeasonPrizes) return false
  club.budget += amount
  w.paidSeasonPrizes += key
  if (clubId == w.user.clubId) w.news("Preisgeld", "${amount} € Preisgeld wurden gutgeschrieben.", "good")
  return true
 }

 fun leagueChampionPrize(w: World, tier: Int): Long {
  if (!w.privateTopClubMode) return when (tier) {
   1 -> 10_000_000L; 2 -> 4_000_000L; 3 -> 1_600_000L; 4 -> 650_000L; 5 -> 280_000L
   6 -> 120_000L; 7 -> 50_000L; 8 -> 20_000L; 9 -> 8_000L; else -> 3_000L
  }
  val league = w.leagues.firstOrNull { it.tier == tier }?.name.orEmpty()
  return when (league) {
   "Bundesliga", "Premier League", "Serie A", "La Liga", "Ligue 1" -> 20_000_000L
   "2. Bundesliga" -> 5_000_000L
   "3. Liga" -> 1_500_000L
   "Regionalliga Nord" -> 350_000L
   "Oberliga Hamburg" -> 150_000L
   else -> when(RealModeDatabase.levelForTier(tier)){1->8_000_000L;2->1_500_000L;else->350_000L}
  }
 }

 fun settleSeason(w:World,tables:Map<Int,List<TableRow>>):List<String>{
  val earned=mutableListOf<String>()
  fun trophy(title:String,clubId:Int,amount:Long,key:String){
   if(clubId==0||w.trophies.any{it.season==w.calendar.season&&it.competition==title})return
   award(w,"${w.calendar.season}:$key",clubId,amount)
   w.trophies.add(Trophy(w.calendar.season,title,clubId,amount))
   if(clubId==w.user.clubId)earned.add(title)
  }
  for((tier,rows) in tables){val league=w.leagues.first{it.tier==tier};rows.firstOrNull()?.let{trophy("Meister: ${league.name}",it.clubId,leagueChampionPrize(w,tier),"league:$tier")}}
  for(group in w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP}.map{CompetitionEngine.cupGroup(w,it)}.distinct()){
   val final=w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&CompetitionEngine.cupGroup(w,it)==group}.maxByOrNull{it.round}
   if(final?.played==true&&final.winnerId!=0)trophy(CompetitionEngine.cupName(w,group),final.winnerId,DomesticCompetitionData.byId(group)?.winnerPrize?:if(group=="Deutschland")6_000_000L else 2_000_000L,"cup:$group")
  }
  for((type,prize) in listOf(CompetitionType.CHAMPIONS_LEAGUE to 25_000_000L,CompetitionType.EUROPA_LEAGUE to 12_000_000L,CompetitionType.CLUB_WORLD_CUP to 40_000_000L,CompetitionType.ETERNAL_CROWN to 50_000_000L,CompetitionType.EURO_ELITE to 12_000_000L)){
   val final=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.stage=="Finale"}.firstOrNull()
   if(final?.played==true&&final.winnerId!=0)trophy(type.label,final.winnerId,prize,"title:${type.name}")
  }
  return earned
 }

 fun awardSeasonPrizes(w: World, leagueWinnerId: Int, tier: Int) {
  award(w, "${w.calendar.season}:league:$tier", leagueWinnerId, leagueChampionPrize(w, tier))
  val cup = winnerId(w, CompetitionType.NATIONAL_CUP)
  if (cup != 0) award(w, "${w.calendar.season}:cup", cup, if (w.privateTopClubMode) 6_000_000L else 500_000L)
  val cl = winnerId(w, CompetitionType.CHAMPIONS_LEAGUE)
  if (cl != 0) award(w, "${w.calendar.season}:cl", cl, 25_000_000L)
  val el = winnerId(w, CompetitionType.EUROPA_LEAGUE)
  if (el != 0) award(w, "${w.calendar.season}:el", el, 12_000_000L)
 }
}

object CompetitionRulesEngine {
 fun forFixture(w:World,fixture:Fixture?):CompetitionRuleSet {
  if(fixture==null)return CompetitionRuleSet()
  val league=w.leagues.firstOrNull{fixture.homeId in it.clubIds || fixture.awayId in it.clubIds}
  val level=league?.let{RealModeDatabase.levelForTier(it.tier)}?:fixture.tier.coerceAtLeast(1)
  return when(fixture.competition){
   CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE,CompetitionType.EURO_ELITE,
   CompetitionType.NATIONAL_CUP,CompetitionType.CLUB_WORLD_CUP,CompetitionType.ETERNAL_CROWN->
    CompetitionRuleSet(varEnabled=true,extraTimeAdditionalSubstitution=true)
   CompetitionType.LEAGUE->CompetitionRuleSet(varEnabled=!w.privateTopClubMode||level<=2,extraTimeAdditionalSubstitution=false)
  }
 }
 fun forMatch(w:World,m:LiveMatch)=forFixture(w,w.fixtures.getOrNull(m.fixtureId-1)?.takeIf{it.id==m.fixtureId}?:w.fixtures.firstOrNull{it.id==m.fixtureId})
 fun rules(w:World,m:LiveMatch)=forMatch(w,m)

 fun maxSubs(w:World,m:LiveMatch):Int{
  val r=forMatch(w,m)
  return r.maxSubstitutions+if(m.period>=3&&m.knockout&&r.extraTimeAdditionalSubstitution)1 else 0
 }
 fun maxSubstitutions(w:World,m:LiveMatch)=maxSubs(w,m)
 fun maxWindows(w:World,m:LiveMatch):Int{
  val r=forMatch(w,m)
  return r.substitutionWindows+if(m.period>=3&&m.knockout&&r.extraTimeAdditionalSubstitution)1 else 0
 }
 fun substitutionIssue(w:World,m:LiveMatch,home:Boolean):String?{
  val r=forMatch(w,m)
  val subs=if(home)m.homeSubs else m.awaySubs
  if(subs>=maxSubs(w,m))return "Das Auswechselkontingent ist ausgeschöpft."
  if(m.halfTime&&r.halfTimeIsFreeWindow)return null
  val windows=if(home)m.homeSubWindows else m.awaySubWindows
  val lastMinute=if(home)m.lastHomeSubMinute else m.lastAwaySubMinute
  if(lastMinute==m.minute)return null
  val limit=maxWindows(w,m)
  if(windows>=limit)return "Die ${if(limit==3)"drei" else limit.toString()} Wechselgelegenheiten sind bereits verbraucht."
  return null
 }
 fun substitutionBlockReason(w:World,m:LiveMatch,home:Boolean)=substitutionIssue(w,m,home)

 fun registerSubstitution(m:LiveMatch,home:Boolean,rules:CompetitionRuleSet=CompetitionRuleSet()){
  if(m.halfTime&&rules.halfTimeIsFreeWindow)return
  if(home){
   if(m.lastHomeSubMinute!=m.minute){m.homeSubWindows++;m.lastHomeSubMinute=m.minute}
  }else if(m.lastAwaySubMinute!=m.minute){m.awaySubWindows++;m.lastAwaySubMinute=m.minute}
 }
 fun registerSubstitution(w:World,m:LiveMatch,home:Boolean)=registerSubstitution(m,home,forMatch(w,m))
 fun goalkeeperViolation(seconds:Int,rules:CompetitionRuleSet=CompetitionRuleSet())=seconds>rules.goalkeeperControlSeconds
}
object TacticalInstructionSystem {
 fun forPlayer(club: Club, playerId: Int): List<PlayerInstruction> = club.tactics.instructions[playerId]?.toList() ?: emptyList()
 fun has(club: Club, playerId: Int, instruction: PlayerInstruction) = instruction in forPlayer(club, playerId)
 fun toggle(club: Club, playerId: Int, instruction: PlayerInstruction) {
  val list = club.tactics.instructions.getOrPut(playerId) { mutableListOf() }
  if (!list.remove(instruction)) list += instruction
  if (list.isEmpty()) club.tactics.instructions.remove(playerId)
 }
}

object MatchAnalysisSystem {
 fun recordTacticChange(w: World, m: LiveMatch, clubId: Int, label: String) {
  val stats = if (clubId == m.homeId) m.home else m.away
  val last = m.tacticChanges.lastOrNull()
  if (last?.clubId == clubId && last.minute == m.minute) return
  m.tacticChanges += TacticChangeEvent(m.minute, clubId, label, stats.xg, stats.shots, stats.possessionTicks)
 }

 fun xgTimeline(m: LiveMatch, clubId: Int): List<Pair<Int, Double>> =
  xgTimeline(m.minute, m.shotEvents, clubId)

 fun xgTimeline(minute:Int, shots:List<ShotEvent>, clubId:Int): List<Pair<Int,Double>> {
  val maxMinute = max(minute, max(shots.maxOfOrNull { it.minute } ?: 0, 1))
  val checkpoints = (15..maxMinute step 15).toMutableList().also { if (it.lastOrNull() != maxMinute) it += maxMinute }
  return checkpoints.distinct().map { checkpoint ->
   checkpoint to shots.filter { it.clubId == clubId && it.minute <= checkpoint }.sumOf { it.xg }
  }
 }

 private fun completedPasses(events:List<PassEvent>,clubId:Int)=events.filter {
  it.clubId==clubId&&it.completed&&it.fromId!=0&&it.toId!=0
 }

 /** Original v0.5.18 pass-network edge aggregation recovered from the APK. */
 fun passNetworkEdges(events:List<PassEvent>,clubId:Int):List<PassNetworkEdge> =
  completedPasses(events,clubId)
   .groupBy { it.fromId to it.toId }
   .map { (ids,list) ->
    PassNetworkEdge(
     ids.first,ids.second,list.size,
     list.map{it.startX}.average().toFloat(),list.map{it.startY}.average().toFloat(),
     list.map{it.endX}.average().toFloat(),list.map{it.endY}.average().toFloat()
    )
   }
   .sortedByDescending { it.count }
   .take(22)

 /** Original v0.5.18 node positions: average all real pass starts/receipts per player. */
 fun passNetworkNodes(events:List<PassEvent>,clubId:Int):List<PassNetworkNode> {
  val passes=completedPasses(events,clubId)
  val points=linkedMapOf<Int,MutableList<Pair<Float,Float>>>()
  passes.forEach { e ->
   points.getOrPut(e.fromId){mutableListOf()} += e.startX to e.startY
   points.getOrPut(e.toId){mutableListOf()} += e.endX to e.endY
  }
  return points.map { (id,pts) ->
   PassNetworkNode(id,pts.map{it.first}.average().toFloat(),pts.map{it.second}.average().toFloat(),pts.size)
  }.sortedByDescending { it.touches }.take(16)
 }

 fun passNetworkEdges(m:LiveMatch,clubId:Int)=passNetworkEdges(m.passEvents,clubId)
 fun passNetworkNodes(m:LiveMatch,clubId:Int)=passNetworkNodes(m.passEvents,clubId)

 /** Compatibility helper retained for recovery tests and callers. */
 fun passNetwork(m: LiveMatch, clubId: Int): Pair<List<PassNetworkNode>, List<PassNetworkEdge>> =
  passNetworkNodes(m,clubId) to passNetworkEdges(m,clubId)
}
object NotificationSystem {
 fun tiredPlayers(w: World): Set<Int> {
  val n = w.notifications
  if (n.tiredSeenWeek != w.calendar.absoluteWeek) { n.tiredSeenWeek = w.calendar.absoluteWeek; n.seenTiredPlayerIds.clear() }
  return w.squad().filter { !it.retired && !it.youth && (it.fitness < 72.0 || w.club().dynamics.fatigueLoad >= 75) }.map { it.id }.toSet()
 }
 fun expiringContracts(w: World): Set<Int> {
  val n = w.notifications
  if (n.contractSeenSeason != w.calendar.season) { n.contractSeenSeason = w.calendar.season; n.seenExpiringPlayerIds.clear() }
  return w.squad().filter { !it.retired && it.contractYears <= 1 }.map { it.id }.toSet()
 }
 fun completedScoutReports(w: World): Set<Int> = w.scoutReports.values.filter { it.progress >= 100 }.map { it.playerId }.toSet()
 fun unreadTired(w: World) = tiredPlayers(w) - w.notifications.seenTiredPlayerIds
 fun unreadExpiring(w: World) = expiringContracts(w) - w.notifications.seenExpiringPlayerIds
 fun unreadScoutReports(w: World) = completedScoutReports(w) - w.notifications.seenCompletedScoutReportIds
 fun totalUnread(w: World) = unreadTired(w).size + unreadExpiring(w).size + unreadScoutReports(w).size
 fun markTiredSeen(w: World) { w.notifications.seenTiredPlayerIds += tiredPlayers(w) }
 fun markContractsSeen(w: World) { w.notifications.seenExpiringPlayerIds += expiringContracts(w) }
 fun markScoutReportsSeen(w: World) { w.notifications.seenCompletedScoutReportIds += completedScoutReports(w) }
}

object ScoutingTransferSystem {
 private fun regionMultiplier(region: ScoutRegion) = when (region) { ScoutRegion.DOMESTIC -> 1.0; ScoutRegion.DACH -> 1.15; ScoutRegion.EUROPE -> 1.35; ScoutRegion.SOUTH_AMERICA -> 1.55; ScoutRegion.WORLD -> 1.8 }
 fun windowOpen(w: World) = w.calendar.matchday <= 7 || w.calendar.matchday in 15..17
 fun cost(w: World, player: Player, region: ScoutRegion): Long {
  val tier=if(w.privateTopClubMode)RealModeDatabase.levelForTier(w.club().tier) else w.club().tier.coerceAtMost(10)
  val base = player.ca * 22L + 900L + tier * 70L
  return (base * regionMultiplier(region)).roundToInt().toLong().coerceAtLeast(250L)
 }
 fun report(w: World, playerId: Int): ScoutReport? {
  val p = w.players[playerId] ?: return null
  if (p.clubId == w.user.clubId) return ScoutReport(p.id,100,p.ca,p.ca,p.hidden.potential,p.hidden.potential,true,true,w.calendar.absoluteWeek,"Eigener Spieler – vollständige Daten")
  return w.scoutReports[playerId]
 }
 fun start(w: World, playerId: Int, region: ScoutRegion, weeks: Int = 3) {
  require(w.live == null) { "Scouting-Aufträge außerhalb eines laufenden Spiels starten." }
  val p = w.players[playerId] ?: error("Spieler nicht gefunden.")
  require(p.clubId != w.user.clubId && !p.retired) { "Nur externe aktive Spieler können gescoutet werden." }
  val price = cost(w,p,region); require(w.club().budget >= price) { "Vereinskasse reicht für den Scouting-Auftrag nicht aus." }
  w.club().budget -= price
  w.scoutAssignments[playerId] = ScoutAssignment(playerId,region,weeks.coerceIn(1,6),w.calendar.absoluteWeek,price)
  w.scoutReports.putIfAbsent(playerId, ScoutReport(playerId,0,1,99,1,99,false,false,w.calendar.absoluteWeek,"Scouting gestartet"))
  if (playerId !in w.watchlist) w.watchlist += playerId
 }
 fun weekly(w: World, rng: SeededRandom) {
  val quality = w.club().dynamics.staffQuality/8 + w.club().academy.scouting/4 + 26
  for ((playerId,a) in w.scoutAssignments.toMap()) {
   val p=w.players[playerId]
   if (p == null) { w.scoutAssignments.remove(playerId); continue }
   val r=w.scoutReports.getOrPut(playerId){ScoutReport(playerId)}
   val gain=(quality+rng.int(-4,4)).coerceIn(28,55);r.progress=(r.progress+gain).coerceAtMost(100);r.lastUpdatedWeek=w.calendar.absoluteWeek
   val error=((100-r.progress)/9+1).coerceAtLeast(1)
   r.caMin=(p.ca-error).coerceAtLeast(1);r.caMax=(p.ca+error).coerceAtMost(99)
   val potError=((100-r.progress)/7+2).coerceAtLeast(1);r.potentialMin=(p.hidden.potential-potError).coerceAtLeast(p.ca);r.potentialMax=(p.hidden.potential+potError).coerceAtMost(99)
   r.personalityKnown=r.progress>=70;r.medicalKnown=r.progress>=88
   r.note=when { r.progress>=100->"Scouting abgeschlossen";r.progress>=70->"Persönlichkeit und Rolle weitgehend bekannt";r.progress>=40->"Leistungsbild wird belastbar";else->"Erste Beobachtungen" }
   a.weeksRemaining--
   if(a.weeksRemaining<=0||r.progress>=100)w.scoutAssignments.remove(playerId)
  }
  w.competingBids.removeAll{it.expiresWeek<w.calendar.absoluteWeek}
  if(w.calendar.absoluteWeek%2==0){
   for(id in w.watchlist.distinct().take(20)){
    val p=w.players[id]?:continue;if(p.retired||p.clubId==w.user.clubId||w.competingBids.any{it.playerId==id&&it.expiresWeek>=w.calendar.absoluteWeek})continue
    if(!rng.chance(.18))continue
    val clubs=w.clubs.values.filter{it.id!=w.user.clubId&&it.id!=p.clubId&&it.budget>TransferEngine.marketValue(w,p)/2}.sortedByDescending{it.reputation}.take(12)
    if(clubs.isNotEmpty()){val c=rng.pick(clubs);w.competingBids+=CompetingBid(id,c.id,(TransferEngine.marketValue(w,p)*(.75+rng.nextDouble()*.35)).toLong(),w.calendar.absoluteWeek+2)}
   }
  }
 }
 fun activeCompetingBids(w:World,playerId:Int)=w.competingBids.filter{it.playerId==playerId&&it.expiresWeek>=w.calendar.absoluteWeek}
}

object YouthCompetitionSystem {
 fun assign(w:World,playerId:Int,squad:YouthSquad){
  require(w.live==null){"Kaderzuordnung erst außerhalb eines laufenden Spiels ändern."};val p=w.players.getValue(playerId);val age=w.calendar.season-p.birthYear
  require(!p.retired&&p.clubId==w.user.clubId&&p.loanParentClubId==0&&p.id!=w.user.playerId&&age<=22&&(squad==YouthSquad.U23||age<=19)){"Dieser Spieler ist für ${squad.label} nicht einsatzberechtigt."}
  p.youth=true;p.youthSquad=squad;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false;p.youthProfile.confidence=(p.youthProfile.confidence+2).coerceAtMost(100);w.training.extra.removeAll{it.playerId==p.id};WorldFactory.autoLineup(w,w.user.clubId);w.news("${p.name} in ${squad.label}","Der Spieler gehört jetzt fest zum ${squad.label}-Kader und erhält dort Nachwuchsspielpraxis und den Academy-Entwicklungsweg.")
 }
 fun move(w:World,playerId:Int,squad:YouthSquad){require(w.players.getValue(playerId).youth){"Nur Jugendspieler können zwischen U19 und U23 verschoben werden."};assign(w,playerId,squad)}
 fun callUp(w:World,playerId:Int){
  require(w.live==null){"Notfall-Nominierung erst außerhalb eines laufenden Spiels."};val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.youth&&!p.retired&&p.loanParentClubId==0){"Nur eigene Jugendspieler können vorübergehend hochgezogen werden."}
  p.temporarySeniorCallUp=true;p.temporaryReturnSquad=p.youthSquad;p.youth=false;p.youthProfile.seniorTraining=true;p.morale=(p.morale+2).coerceAtMost(100);WorldFactory.rebuildBench(w,w.club());val target=p.temporaryReturnSquad?.label?:p.youthSquad.label;w.news("Notfall-Nominierung","${p.name} steht für das nächste Profispiel zur Verfügung und kehrt danach automatisch in $target zurück.")
 }
 fun returnToYouth(w:World,playerId:Int){
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.temporarySeniorCallUp){"Keine vorübergehende Jugend-Nominierung aktiv."};val target=p.temporaryReturnSquad?:if(w.calendar.season-p.birthYear<=19)YouthSquad.U19 else YouthSquad.U23
  p.youth=true;p.youthSquad=target;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false;WorldFactory.autoLineup(w,w.user.clubId);w.news("Zurück im ${target.label}","${p.name} kehrt nach der Profikader-Nominierung in den Nachwuchs zurück.")
 }
 private fun goals(rng:SeededRandom,edge:Double):Int{val p=(.18+edge*.012).coerceIn(.06,.48);var goals=0;repeat(5){i->if(rng.chance(p*(1-i*.08)))goals++};return goals}
 private fun simulate(w:World,c:Club,squad:YouthSquad,rng:SeededRandom){
  val players=w.squad(c.id).filter{it.youth&&it.youthSquad==squad&&!it.retired}.sortedByDescending{it.ca}.take(11);if(players.isEmpty())return
  val quality=if(squad==YouthSquad.U19)c.academy.u19Quality else c.academy.u23Quality;val own=(players.map{it.ca}.average().takeUnless{it.isNaN()}?:30.0)+quality*.12+c.dynamics.staffQuality*.04;val opponent=own+rng.int(-8,8);val gf=goals(rng,own-opponent);val ga=goals(rng,opponent-own)
  val season=if(squad==YouthSquad.U19)c.academy.u19Season else c.academy.u23Season;season.played++;season.goalsFor+=gf;season.goalsAgainst+=ga;season.lastResult="$gf:$ga";when{gf>ga->{season.wins++;season.points+=3};gf==ga->{season.draws++;season.points++};else->season.losses++}
  players.forEach{p->val s=p.youthTeamStats;s.appearances++;val rating=(6.15+(p.ca-own)*.012+(gf-ga)*.16+rng.int(-5,5)*.04).coerceIn(4.8,9.4);s.averageRating=(s.averageRating*(s.appearances-1)+rating)/s.appearances;p.trainingProgress+=.018*(.7+p.youthProfile.learning/140.0)*(if(squad==YouthSquad.U23)1.12 else 1.0)}
  repeat(gf){val scorer=players.maxByOrNull{it.attributes.finishing+rng.int(0,35)}?:return@repeat;scorer.youthTeamStats.goals++;players.filter{it.id!=scorer.id}.maxByOrNull{it.attributes.passing+it.attributes.vision+rng.int(0,35)}?.youthTeamStats?.let{it.assists++}}
 }
 fun weekly(w:World,rng:SeededRandom){
  for(p in w.players.values){if(!p.youth)continue;val age=w.calendar.season-p.birthYear;if(age>=23)p.youth=false else if(age>=19&&p.youthSquad==YouthSquad.U19)p.youthSquad=YouthSquad.U23}
  if(w.calendar.absoluteWeek%2==0)for(c in w.clubs.values){simulate(w,c,YouthSquad.U19,rng);simulate(w,c,YouthSquad.U23,rng)}
 }
}

object OutboundTransferSystem {
 fun isOutbound(w:World,o:TransferOffer)=o.sellerClubId==w.user.clubId&&o.buyerClubId!=w.user.clubId
 fun activeOffers(w:World)=w.negotiations.values
  .filter{isOutbound(w,it)&&it.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN,NegotiationStatus.COMPLETED)}
  .sortedWith(compareBy<TransferOffer>{it.playerId}.thenByDescending{it.fee}.thenBy{it.id})

 fun maxBuyOffer(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionNeed(w,buyer,p);val age=w.calendar.season-p.birthYear
  val upside=(p.hidden.potential-p.ca).coerceAtLeast(0);val repGap=(buyer.reputation-w.club().reputation).coerceIn(-20,25)
  val factor=(.78+need*.075+(if(age<=23)upside.coerceAtMost(20)*.008 else 0.0)+repGap*.003).coerceIn(.72,1.30)
  return minOf((buyer.budget*.78).toLong().coerceAtLeast(0L),(value*factor).toLong()).coerceAtLeast(1_000L)
 }
 private fun positionNeed(w:World,buyer:Club,p:Player):Int{
  val same=w.squad(buyer.id).filter{!it.youth&&!it.retired&&it.position==p.position};val strongest=same.maxOfOrNull{it.ca}?:0
  return when{same.isEmpty()||strongest+5<=p.ca->4;strongest<p.ca->3;same.size<=2->2;else->1}
 }
 fun loanFeeLimit(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionNeed(w,buyer,p)
  return minOf((buyer.budget*.18).toLong().coerceAtLeast(0L),(value*(.045+need*.018)).toLong()).coerceAtLeast(500L)
 }
 fun buyOptionLimit(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionNeed(w,buyer,p)
  return minOf((buyer.budget*.80).toLong().coerceAtLeast(0L),(value*(.88+need*.055)).toLong()).coerceAtLeast(1_000L)
 }

 /** Exact user-triggered multi-club offer round from shipped v0.5.18. */
 fun requestOffers(w:World,playerId:Int,type:DealType):List<TransferOffer>{
  require(w.live==null){"Spieler erst außerhalb eines laufenden Spiels anbieten."}
  require(type in listOf(DealType.BUY,DealType.LOAN,DealType.LOAN_OPTION)){"Nur Verkauf oder Leihe können angeboten werden."}
  require(ScoutingTransferSystem.windowOpen(w)){"Das Transferfenster ist geschlossen."}
  val p=w.players.getValue(playerId)
  require(!p.retired&&p.clubId==w.user.clubId&&p.loanParentClubId==0&&p.id!=w.user.playerId){"Dieser Spieler kann aktuell nicht angeboten werden."}
  activeOffers(w).filter{it.playerId==p.id}.forEach{it.status=NegotiationStatus.REJECTED;it.message="Durch eine neue Angebotsrunde ersetzt."}
  val value=TransferEngine.marketValue(w,p)
  val buyers=w.clubs.values.asSequence()
   .filter{it.id!=w.user.clubId&&it.id!=p.clubId}
   .map{it to TransferInterestSystem.score(w,p,it.id)}
   .filter{(club,score)->score>=35&&club.budget>value/3}
   .sortedWith(compareByDescending<Pair<Club,Int>>{it.second}.thenByDescending{it.first.reputation}.thenBy{it.first.id})
   .take(4).toList()
  require(buyers.size>=2){"Aktuell gibt es nicht genügend ernsthafte Interessenten."}
  val created=buyers.mapIndexed{index,(buyer,interest)->
   val senior=w.squad(buyer.id).filter{!it.youth&&!it.retired}
   val average=senior.map{it.ca}.average().takeUnless{it.isNaN()}?:p.ca.toDouble()
   val role=when{p.ca>=average+8->SquadRole.STAR;p.ca>=average+3->SquadRole.STARTER;p.ca>=average-4->SquadRole.ROTATION;else->SquadRole.PROSPECT}
   val cap=if(type==DealType.BUY)maxBuyOffer(w,buyer,p) else loanFeeLimit(w,buyer,p)
   val variance=.78+kotlin.math.abs((index*7+w.calendar.absoluteWeek*13+buyer.id*17+p.id*31)%15)/100.0
   val fee=(cap*variance).toLong().coerceAtLeast(500L)
   val wage=maxOf(p.wage,maxOf((value/50_000L).toInt(),5))
   val option=if(type==DealType.LOAN_OPTION)(buyOptionLimit(w,buyer,p)*(.82+index*.035)).toLong() else 0L
   val promise=when(role){SquadRole.STAR->95;SquadRole.STARTER->82;SquadRole.ROTATION->65;SquadRole.PROSPECT->58;SquadRole.BACKUP->42}
   TransferOffer(
    id=w.nextIds.negotiation++,buyerClubId=buyer.id,sellerClubId=w.user.clubId,playerId=p.id,type=type,role=role,fee=fee,wage=wage,
    signingBonus=wage.toLong()*4,sellOnPercent=if(type==DealType.BUY&&index%2==0)5 else 0,buyOption=option,playingTimePromise=promise,
    status=NegotiationStatus.COUNTER,stage=TransferStage.CLUB,loanWeeksRequested=if(type==DealType.BUY)24 else listOf(24,40,52)[index%3],
    recallAllowed=type!=DealType.BUY&&index%2==0,
    message="${buyer.name} bietet ${if(type==DealType.BUY)"einen Kauf" else type.label.lowercase()} an · Interesse $interest/100. Du kannst annehmen, ablehnen oder nachverhandeln."
   ).also{w.negotiations[it.id]=it}
  }
  w.news("Angebote für ${p.name}","${created.size} Vereine haben auf die ${if(type==DealType.BUY)"Verkaufsliste" else "Leihanfrage"} reagiert.","normal")
  return created
 }
 fun solicitOffers(w:World,playerId:Int,type:DealType)=requestOffers(w,playerId,type)

 fun reject(w:World,offerId:Int){
  val o=w.negotiations.getValue(offerId);require(isOutbound(w,o)&&o.status!=NegotiationStatus.COMPLETED)
  o.status=NegotiationStatus.REJECTED;o.message="Angebot von dir abgelehnt."
 }
 fun accept(w:World,offerId:Int){
  require(w.live==null){"Transfer erst außerhalb eines laufenden Spiels bestätigen."}
  val o=w.negotiations.getValue(offerId);require(isOutbound(w,o)&&o.status in listOf(NegotiationStatus.COUNTER,NegotiationStatus.AGREED)){"Dieses Angebot kann nicht angenommen werden."}
  val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId)
  if(TransferInterestSystem.score(w,p,buyer.id)<28&&!p.wantsMove){o.status=NegotiationStatus.REJECTED;o.message="${p.name} lehnt den Wechsel zu ${buyer.name} ab.";return}
  if(buyer.budget<o.fee+o.signingBonus){o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} kann das Angebot finanziell nicht mehr hinterlegen.";return}
  o.status=NegotiationStatus.AGREED;o.stage=TransferStage.REGISTRATION;o.medicalPassed=true;o.medicalNote="Medizincheck vom aufnehmenden Verein bestanden";o.registrationReady=true
  val type=o.type;val fee=o.fee;val option=o.buyOption;val playerName=p.name;val buyerName=buyer.name
  TransferEngine.complete(w,o.id)
  activeOffers(w).filter{it.id!=o.id&&it.playerId==p.id}.forEach{it.status=NegotiationStatus.REJECTED;it.message="Spieler hat sich für ein anderes Angebot entschieden."}
  w.news(if(type==DealType.BUY)"Spieler verkauft" else "Leihe vereinbart","$playerName → $buyerName · ${if(fee>0)fee.toString()+" €" else "ohne Gebühr"}${if(type==DealType.LOAN_OPTION&&option>0)" · Kaufoption $option €" else ""}","good")
 }
 fun negotiate(w:World,offerId:Int,kind:String){
  val o=w.negotiations.getValue(offerId)
  require(isOutbound(w,o)&&o.status==NegotiationStatus.COUNTER){"Dieses Angebot kann nicht nachverhandelt werden."}
  val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId);val value=TransferEngine.marketValue(w,p)
  when(kind){
   "fee"->o.fee=(o.fee*1.08+maxOf(500L,value/80L)).toLong()
   "sellon"->o.sellOnPercent=(o.sellOnPercent+5).coerceAtMost(25)
   "option"->{require(o.type==DealType.LOAN_OPTION){"Nur bei einer Leihe mit Option kann die Kaufoption verändert werden."};o.buyOption=(o.buyOption*1.08+maxOf(1_000L,value/60L)).toLong()}
   else->error("Unbekannter Verhandlungspunkt.")
  }
  o.round++
  val cap=if(o.type==DealType.BUY)maxBuyOffer(w,buyer,p) else loanFeeLimit(w,buyer,p)
  val optionCap=if(o.type==DealType.LOAN_OPTION)buyOptionLimit(w,buyer,p) else Long.MAX_VALUE
  val effective=if(o.type==DealType.BUY)o.fee+value*o.sellOnPercent/100/4 else o.fee
  val maxPackage=if(o.type==DealType.BUY)cap+value/16 else cap
  val optionFits=o.type!=DealType.LOAN_OPTION||o.buyOption<=optionCap
  val optionSoft=o.type!=DealType.LOAN_OPTION||o.buyOption<=(optionCap*1.08).toLong()
  when{
   buyer.budget<o.fee+o.signingBonus->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} zieht das Angebot zurück: Das Paket passt nicht mehr ins Budget."}
   o.round>=4&&(effective>maxPackage||!optionFits)->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} bricht die Verhandlung nach mehreren Runden ab."}
   effective<=maxPackage&&optionFits->{o.status=NegotiationStatus.AGREED;o.message="${buyer.name} akzeptiert dein Gegenangebot. Du kannst den Deal jetzt bestätigen."}
   effective<=(maxPackage*1.08).toLong()&&optionSoft->{
    o.status=NegotiationStatus.COUNTER;o.fee=minOf(o.fee,cap);if(o.type==DealType.LOAN_OPTION)o.buyOption=minOf(o.buyOption,optionCap)
    o.message="${buyer.name} bleibt am Tisch und legt ein letztes Gegenangebot vor."
   }
   else->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} lehnt deine Forderung ab und steigt aus."}
  }
 }
}
object TransferV0518System {
 fun reservedBudget(w:World,buyerClubId:Int,exceptOfferId:Int=0):Long = w.negotiations.values.filter{it.id!=exceptOfferId&&it.buyerClubId==buyerClubId&&it.registrationReady&&it.status==NegotiationStatus.AGREED}.sumOf{it.fee+it.signingBonus}
 fun availableBudget(w:World,buyerClubId:Int,exceptOfferId:Int=0):Long=(w.clubs[buyerClubId]?.budget?:0L)-reservedBudget(w,buyerClubId,exceptOfferId)
 private fun youthSlotValid(w:World,o:TransferOffer,p:Player):Boolean{if(o.buyerClubId!=w.user.clubId||o.targetYouthSquad==null)return false;val age=w.calendar.season-p.birthYear;return age<=22&&(o.targetYouthSquad==YouthSquad.U23||age<=19)}
 fun medicalAndRegistration(w:World,o:TransferOffer):Boolean{
  val p=w.players.getValue(o.playerId)
  if(o.stage==TransferStage.MEDICAL&&!o.medicalPassed){
   o.medicalRisk=(p.hidden.injuryProneness*.55+p.injuryWeeks*8+(w.calendar.season-p.birthYear-30).coerceAtLeast(0)*2.2).roundToInt().coerceIn(0,100)
   if(o.medicalRisk>=78){o.medicalPassed=false;o.status=NegotiationStatus.REJECTED;o.medicalNote="Medizincheck nicht bestanden: Risiko ${o.medicalRisk}/100.";o.message=o.medicalNote;return false}
   o.medicalPassed=true;o.medicalNote="Medizincheck bestanden: Risiko ${o.medicalRisk}/100.";o.stage=TransferStage.REGISTRATION
  }
  if(o.stage!=TransferStage.REGISTRATION)return false
  val rosterOk=youthSlotValid(w,o,p)||w.squad(o.buyerClubId).count{!it.youth&&!it.retired}<32
  val budgetOk=availableBudget(w,o.buyerClubId,o.id)>=o.fee+o.signingBonus
  o.registrationReady=rosterOk&&budgetOk
  o.status=if(o.registrationReady)NegotiationStatus.AGREED else NegotiationStatus.COUNTER
  o.message=when{!budgetOk->"Registrierung blockiert: reserviertes Budget reicht nicht aus.";!rosterOk->"Registrierung blockiert: Profikader ist voll.";else->"Medizincheck bestanden. Registrierung ist vorbereitet."}
  return o.registrationReady
 }
 fun withdraw(w:World,offerId:Int){val o=w.negotiations.getValue(offerId);require(o.buyerClubId==w.user.clubId){"Nur eigene Verhandlungen können zurückgezogen werden."};require(o.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.WITHDRAWN)){"Abgeschlossene oder bereits zurückgezogene Transfers können nicht zurückgezogen werden."};o.registrationReady=false;o.status=NegotiationStatus.WITHDRAWN;o.message="Verhandlung zurückgezogen; reserviertes Budget ist wieder frei."}
}

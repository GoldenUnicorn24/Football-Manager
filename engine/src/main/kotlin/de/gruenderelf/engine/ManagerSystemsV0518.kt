package de.gruenderelf.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Recovered v0.5.18 systems reconstructed from the shipped APK. */
object CompetitionPrizeSystem {
 fun winnerId(w: World, competition: CompetitionType): Int = w.fixtures
  .filter { it.season == w.calendar.season && it.competition == competition && it.played && it.winnerId != 0 }
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
   else -> 1_000_000L
  }
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
 fun rules(w: World, m: LiveMatch): CompetitionRuleSet {
  val fixture = w.fixtures.firstOrNull { it.id == m.fixtureId } ?: return CompetitionRuleSet()
  val league = w.leagues.firstOrNull { fixture.homeId in it.clubIds || fixture.awayId in it.clubIds }
  val level = league?.tier ?: fixture.tier.coerceAtLeast(1)
  return when (fixture.competition) {
   CompetitionType.LEAGUE -> CompetitionRuleSet(extraTimeAdditionalSubstitution = false, varEnabled = !(w.privateTopClubMode && level > 2))
   else -> CompetitionRuleSet(extraTimeAdditionalSubstitution = true, varEnabled = true)
  }
 }
 fun maxSubstitutions(w: World, m: LiveMatch) = 5 + if (m.period >= 3 && m.extraTimePlayed && rules(w, m).extraTimeAdditionalSubstitution) 1 else 0
 fun maxWindows(w: World, m: LiveMatch) = 3 + if (m.period >= 3 && m.extraTimePlayed && rules(w, m).extraTimeAdditionalSubstitution) 1 else 0
 fun substitutionBlockReason(w: World, m: LiveMatch, home: Boolean): String? {
  val used = if (home) m.homeSubs else m.awaySubs
  if (used >= maxSubstitutions(w, m)) return "Das Auswechselkontingent ist ausgeschöpft."
  if (m.halfTime) return null
  val windows = if (home) m.homeSubWindows else m.awaySubWindows
  val lastMinute = if (home) m.lastHomeSubMinute else m.lastAwaySubMinute
  val maxWindows = maxWindows(w, m)
  if (lastMinute != m.minute && windows >= maxWindows) return "Die ${if (maxWindows == 3) "drei" else maxWindows} Wechselgelegenheiten sind bereits verbraucht."
  return null
 }
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
 fun xgTimeline(m: LiveMatch, clubId: Int): List<Pair<Int, Double>> {
  val maxMinute = max(m.minute, max(m.shotEvents.maxOfOrNull { it.minute } ?: 0, 1))
  val checkpoints = (15..maxMinute step 15).toMutableList().also { if (it.lastOrNull() != maxMinute) it += maxMinute }
  return checkpoints.distinct().map { minute -> minute to m.shotEvents.filter { it.clubId == clubId && it.minute <= minute }.sumOf { it.xg } }
 }
 fun passNetwork(m: LiveMatch, clubId: Int): Pair<List<PassNetworkNode>, List<PassNetworkEdge>> {
  val events = m.passEvents.filter { it.clubId == clubId && it.completed && it.fromId != 0 && it.toId != 0 }
  val touches = linkedMapOf<Int, MutableList<Pair<Float, Float>>>()
  events.forEach { e ->
   touches.getOrPut(e.fromId) { mutableListOf() } += e.startX to e.startY
   touches.getOrPut(e.toId) { mutableListOf() } += e.endX to e.endY
  }
  val nodes = touches.map { (id, pts) -> PassNetworkNode(id, pts.map { it.first }.average().toFloat(), pts.map { it.second }.average().toFloat(), pts.size) }
  val edges = events.groupBy { it.fromId to it.toId }.map { (ids, list) ->
   PassNetworkEdge(ids.first, ids.second, list.size, list.map { it.startX }.average().toFloat(), list.map { it.startY }.average().toFloat(), list.map { it.endX }.average().toFloat(), list.map { it.endY }.average().toFloat())
  }.sortedByDescending { it.count }
  return nodes to edges
 }
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
  val base = player.ca * 22L + 900L + w.club().tier.coerceAtMost(10) * 70L
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
  p.youth=true;p.youthSquad=squad;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false;WorldFactory.autoLineup(w,w.user.clubId);w.news("${p.name} in ${squad.label}","Der Spieler gehört jetzt fest zum ${squad.label}-Kader und erhält dort Nachwuchsspielpraxis.")
 }
 fun move(w:World,playerId:Int,squad:YouthSquad){require(w.players.getValue(playerId).youth){"Nur Jugendspieler können zwischen U19 und U23 verschoben werden."};assign(w,playerId,squad)}
 fun callUp(w:World,playerId:Int){
  require(w.live==null){"Nominierung erst außerhalb eines laufenden Spiels."};val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.youth&&!p.retired){"Nur eigene Jugendspieler können vorübergehend hochgezogen werden."}
  p.temporarySeniorCallUp=true;p.temporaryReturnSquad=p.youthSquad;p.youth=false;p.youthProfile.seniorTraining=true;p.morale=(p.morale+2).coerceAtMost(100);WorldFactory.rebuildBench(w,w.club());w.news("${p.name} bei den Profis","Vorübergehende Nominierung aus ${p.temporaryReturnSquad?.label ?: "der Jugend"}.","good")
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
 fun weekly(w:World,rng:SeededRandom){
  if(w.calendar.absoluteWeek%2!=0)return
  val existing=w.negotiations.values.filter{isOutbound(w,it)&&it.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.COMPLETED)}.map{it.playerId}.toSet()
  val candidates=w.squad().filter{it.id!=w.user.playerId&&!it.retired&&!it.youth&&it.id !in existing}.sortedByDescending{(if(it.wantsMove)20 else 0)+(100-it.morale)+it.ca/3}.take(12)
  if(candidates.isEmpty()||!rng.chance(.65))return
  val p=rng.pick(candidates);val buyers=w.clubs.values.filter{it.id!=w.user.clubId&&it.id!=p.clubId&&it.budget>TransferEngine.marketValue(w,p)/2}.filter{TransferInterestSystem.score(w,p,it.id)>=35}.sortedByDescending{it.reputation}.take(15);if(buyers.isEmpty())return
  val buyer=rng.pick(buyers);val type=if(w.calendar.season-p.birthYear<=24&&rng.chance(.28))DealType.LOAN_OPTION else DealType.BUY;val value=TransferEngine.marketValue(w,p);val fee=if(type==DealType.BUY)(value*(.76+rng.nextDouble()*.30)).toLong() else (value*(.04+rng.nextDouble()*.05)).toLong();val role=if(p.ca>=70)SquadRole.STARTER else SquadRole.ROTATION
  val o=TransferOffer(id=w.nextIds.negotiation++,buyerClubId=buyer.id,sellerClubId=w.user.clubId,playerId=p.id,type=type,role=role,fee=fee,wage=maxOf(p.wage,(p.wage*1.08).roundToInt()),buyOption=if(type==DealType.LOAN_OPTION)(value*1.05).toLong() else 0L,status=NegotiationStatus.COUNTER,stage=TransferStage.CLUB,message="${buyer.name} legt ein Angebot für ${p.name} vor.")
  w.negotiations[o.id]=o;w.news("Transferangebot für ${p.name}","${buyer.name}: ${type.label} · ${fee} €${if(o.buyOption>0)" · Option ${o.buyOption} €" else ""}.","normal")
 }
 fun isOutbound(w:World,o:TransferOffer)=o.sellerClubId==w.user.clubId&&o.buyerClubId!=w.user.clubId
 fun maxBuyOffer(w:World,buyer:Club,p:Player):Long{val need=positionNeed(w,buyer,p);val age=w.calendar.season-p.birthYear;val upside=(p.hidden.potential-p.ca).coerceAtLeast(0);val repGap=(buyer.reputation-(w.clubs[p.clubId]?.reputation?:buyer.reputation)).coerceIn(-30,30);return (TransferEngine.marketValue(w,p)*(0.72+need*.09+upside*.008+repGap*.006+(if(age<=23).08 else 0.0))).toLong().coerceAtMost((buyer.budget*.8).toLong()).coerceAtLeast(0)}
 private fun positionNeed(w:World,buyer:Club,p:Player):Int{val count=w.squad(buyer.id).count{!it.youth&&!it.retired&&it.position==p.position};return when(count){0->4;1->3;2->2;else->1}}
 fun reject(w:World,offerId:Int){val o=w.negotiations.getValue(offerId);require(isOutbound(w,o));o.status=NegotiationStatus.REJECTED;o.message="Angebot abgelehnt."}
 fun accept(w:World,offerId:Int){
  val o=w.negotiations.getValue(offerId);require(isOutbound(w,o));val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId);require(buyer.budget>=o.fee){"Käufer kann den Transfer nicht finanzieren."};require(TransferInterestSystem.score(w,p,buyer.id)>=35){"Der Spieler lehnt den Wechsel ab."}
  o.status=NegotiationStatus.AGREED;o.stage=TransferStage.REGISTRATION;o.registrationReady=true;TransferEngine.complete(w,o.id);w.negotiations.values.filter{it.id!=o.id&&it.playerId==p.id&&isOutbound(w,it)&&it.status!=NegotiationStatus.COMPLETED}.forEach{it.status=NegotiationStatus.REJECTED}
 }
 fun negotiate(w:World,offerId:Int,kind:String){
  val o=w.negotiations.getValue(offerId);require(isOutbound(w,o)&&o.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.COMPLETED));val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId);o.round++
  when(kind){"fee"->o.fee=(o.fee*1.08).toLong();"sellon"->o.sellOnPercent=(o.sellOnPercent+5).coerceAtMost(25);"option"->o.buyOption=(o.buyOption*1.08).toLong()}
  val cap=maxBuyOffer(w,buyer,p);if(o.round>=4&&o.fee>cap*1.08){o.status=NegotiationStatus.REJECTED;o.message="Der Käufer steigt aus."}else if(o.fee<=cap){o.status=NegotiationStatus.AGREED;o.message="Der Käufer akzeptiert die Konditionen."}else{o.status=NegotiationStatus.COUNTER;o.fee=((o.fee+cap)/2);o.message="Der Käufer legt ein Gegenangebot vor."}
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
 fun withdraw(w:World,offerId:Int){val o=w.negotiations.getValue(offerId);require(o.buyerClubId==w.user.clubId){"Nur eigene Verhandlungen können zurückgezogen werden."};require(o.status!=NegotiationStatus.COMPLETED){"Abgeschlossene Transfers können nicht zurückgezogen werden."};o.registrationReady=false;o.status=NegotiationStatus.REJECTED;o.message="Verhandlung zurückgezogen; reserviertes Budget ist wieder frei."}
}

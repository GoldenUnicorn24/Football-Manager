package de.gruenderelf.engine

import kotlin.math.roundToInt

/** v0.5.2: Nachwuchsspielbetrieb, Scouting/Verträge und individuelle Matchanweisungen. */
object YouthCompetitionSystem {
 fun eligible(w:World,p:Player,squad:YouthSquad):Boolean{
  val age=w.calendar.season-p.birthYear
  return !p.retired&&p.clubId==w.user.clubId&&p.loanParentClubId==0&&p.id!=w.user.playerId&&when(squad){YouthSquad.U19->age<=19;YouthSquad.U23->age<=22}
 }
 fun normalizeSquad(w:World,p:Player){
  if(!p.youth)return
  val age=w.calendar.season-p.birthYear
  p.youthSquad=if(age<=18)YouthSquad.U19 else YouthSquad.U23
 }
 fun assignToYouth(w:World,playerId:Int,squad:YouthSquad){
  require(w.live==null){"Kaderzuordnung erst außerhalb eines laufenden Spiels ändern."}
  val p=w.players.getValue(playerId);require(eligible(w,p,squad)){"Dieser Spieler ist für ${squad.label} nicht einsatzberechtigt."}
  p.youth=true;p.youthSquad=squad;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null
  p.youthProfile.seniorTraining=false;p.youthProfile.confidence=(p.youthProfile.confidence+2).coerceAtMost(100)
  w.training.extra.removeAll{it.playerId==p.id}
  WorldFactory.autoLineup(w,w.user.clubId)
  w.news("${p.name} in ${squad.label}","Der Spieler gehört jetzt fest zum ${squad.label}-Kader und erhält dort Nachwuchsspielpraxis und den Academy-Entwicklungsweg.","normal")
 }
 fun move(w:World,playerId:Int,squad:YouthSquad){
  val p=w.players.getValue(playerId);require(p.youth){"Nur Jugendspieler können zwischen U19 und U23 verschoben werden."}
  assignToYouth(w,playerId,squad)
 }
 fun temporaryCallUp(w:World,playerId:Int){
  require(w.live==null){"Notfall-Nominierung erst außerhalb eines laufenden Spiels."}
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.youth&&!p.retired&&p.loanParentClubId==0){"Nur eigene Jugendspieler können vorübergehend hochgezogen werden."}
  p.temporarySeniorCallUp=true;p.temporaryReturnSquad=p.youthSquad;p.youth=false
  p.youthProfile.seniorTraining=true;p.morale=(p.morale+2).coerceAtMost(100)
  WorldFactory.rebuildBench(w,w.club())
  w.news("Notfall-Nominierung","${p.name} steht für das nächste Profispiel zur Verfügung und kehrt danach automatisch in ${p.temporaryReturnSquad?.label?:p.youthSquad.label} zurück.","normal")
 }
 fun returnTemporary(w:World,playerId:Int,announce:Boolean=true){
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.temporarySeniorCallUp){"Keine vorübergehende Jugend-Nominierung aktiv."}
  val target=p.temporaryReturnSquad?:if(w.calendar.season-p.birthYear<=19)YouthSquad.U19 else YouthSquad.U23
  p.youth=true;p.youthSquad=target;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false
  if(announce)w.news("Zurück im ${target.label}","${p.name} kehrt nach der Profikader-Nominierung in den Nachwuchs zurück.","normal")
  WorldFactory.autoLineup(w,w.user.clubId)
 }
 fun makeTemporaryPermanent(w:World,playerId:Int){
  require(w.live==null){"Kaderstatus erst außerhalb eines laufenden Spiels ändern."}
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.temporarySeniorCallUp){"Keine vorübergehende Jugend-Nominierung aktiv."}
  p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youth=false;p.youthProfile.seniorTraining=false
  w.news("Dauerhaft im Profikader","${p.name} bleibt nach seiner Notfall-Nominierung dauerhaft bei den Profis.","good")
 }
 fun returnTemporaryAfterMatch(w:World,m:LiveMatch){
  if(w.user.clubId !in listOf(m.homeId,m.awayId))return
  w.squad(w.user.clubId).filter{it.temporarySeniorCallUp}.map{it.id}.toList().forEach{if(w.players[it]?.temporarySeniorCallUp==true)returnTemporary(w,it,true)}
 }
 private fun goalsFor(rng:SeededRandom,advantage:Double):Int{
  var goals=0;val base=(.18+advantage*.012).coerceIn(.06,.48)
  repeat(5){if(rng.chance(base*(1.0-it*.08)))goals++}
  return goals
 }
 private fun simulate(w:World,c:Club,squad:YouthSquad,rng:SeededRandom){
  val roster=w.squad(c.id).filter{it.youth&&it.youthSquad==squad&&!it.retired}
  if(roster.isEmpty())return
  val quality=if(squad==YouthSquad.U19)c.academy.u19Quality else c.academy.u23Quality
  val team=roster.sortedByDescending{it.ca}.take(11)
  val strength=(team.map{it.ca}.average().takeIf{!it.isNaN()}?:30.0)+quality*.12+c.dynamics.staffQuality*.04
  val opponent=strength+rng.int(-8,8)
  val gf=goalsFor(rng,strength-opponent);val ga=goalsFor(rng,opponent-strength)
  val season=if(squad==YouthSquad.U19)c.academy.u19Season else c.academy.u23Season
  season.played++;season.goalsFor+=gf;season.goalsAgainst+=ga;season.lastResult="$gf:$ga"
  when{gf>ga->{season.wins++;season.points+=3};gf==ga->{season.draws++;season.points++};else->season.losses++}
  team.forEach{p->
   val y=p.youthTeamStats;y.appearances++
   val rating=(6.15+(gf-ga)*.16+(p.ca-strength)*.012+rng.int(-5,5)*.04).coerceIn(4.8,9.4)
   y.averageRating=((y.averageRating*(y.appearances-1)+rating)/y.appearances)
   p.trainingProgress+=.018*(if(squad==YouthSquad.U23)1.12 else 1.0)*(0.7+p.youthProfile.learning/140.0)
  }
  repeat(gf){
   val scorer=team.maxByOrNull{it.attributes.finishing+rng.int(0,35)}?:return@repeat;scorer.youthTeamStats.goals++
   val assister=team.filter{it.id!=scorer.id}.maxByOrNull{it.attributes.passing+it.attributes.vision+rng.int(0,35)}
   if(assister!=null&&rng.chance(.72))assister.youthTeamStats.assists++
  }
 }
 fun weekly(w:World,rng:SeededRandom){
  for(c in w.clubs.values){
   w.squad(c.id).filter{it.youth}.forEach{p->
    val age=w.calendar.season-p.birthYear
    if(age>=23){p.youth=false;p.youthProfile.seniorTraining=false}
    else if(age>=19&&p.youthSquad==YouthSquad.U19)p.youthSquad=YouthSquad.U23
   }
   if(w.calendar.absoluteWeek%2==0){simulate(w,c,YouthSquad.U19,rng);simulate(w,c,YouthSquad.U23,rng)}
  }
 }
 fun newSeason(w:World){
  for(c in w.clubs.values){c.academy.u19Season=AcademyTeamSeason();c.academy.u23Season=AcademyTeamSeason()}
  w.players.values.filter{it.youth&&!it.retired}.forEach{normalizeSquad(w,it);it.youthTeamStats=YouthTeamStats()}
 }
}

object ScoutingTransferSystem {
 fun transferWindowOpen(w:World):Boolean = w.calendar.matchday<=7 || w.calendar.matchday in 15..17
 fun transferWindowLabel(w:World):String = when{w.calendar.matchday<=7->"Sommerfenster offen · Deadline nach Spieltag 7";w.calendar.matchday in 15..17->"Winterfenster offen · Deadline nach Spieltag 17";w.calendar.matchday<15->"Transferfenster geschlossen · Winterfenster ab Spieltag 15";else->"Transferfenster geschlossen · neues Sommerfenster zum Saisonstart"}
 fun toggleWatchlist(w:World,playerId:Int){
  if(playerId in w.watchlist)w.watchlist.remove(playerId) else {require(playerId in w.players&&w.players[playerId]?.clubId!=w.user.clubId);w.watchlist.add(playerId)}
 }
 fun scoutCost(w:World,p:Player,region:ScoutRegion):Long{
  val regionFactor=when(region){ScoutRegion.DOMESTIC->1.0;ScoutRegion.DACH->1.15;ScoutRegion.EUROPE->1.35;ScoutRegion.SOUTH_AMERICA->1.55;ScoutRegion.WORLD->1.8}
  return ((900L+p.ca*22L+w.club().tier.coerceAtMost(10)*70L)*regionFactor).toLong()
 }
 fun startScouting(w:World,playerId:Int,region:ScoutRegion=ScoutRegion.DOMESTIC){
  val p=w.players.getValue(playerId);require(p.clubId!=w.user.clubId&&!p.retired){"Eigene Spieler müssen nicht extern gescoutet werden."};require(playerId !in w.scoutAssignments){"Dieser Spieler wird bereits beobachtet."}
  val cost=scoutCost(w,p,region);require(w.club().budget>=cost){"Budget für den Scoutingauftrag reicht nicht."};w.club().budget-=cost
  val weeks=when(region){ScoutRegion.DOMESTIC->2;ScoutRegion.DACH->2;ScoutRegion.EUROPE->3;ScoutRegion.SOUTH_AMERICA,ScoutRegion.WORLD->4}
  w.scoutAssignments[playerId]=ScoutAssignment(playerId,region,weeks,w.calendar.absoluteWeek,cost)
  w.scoutReports.putIfAbsent(playerId,ScoutReport(playerId=playerId,progress=5,caMin=(p.ca-16).coerceAtLeast(1),caMax=(p.ca+16).coerceAtMost(99),potentialMin=(p.ca-4).coerceAtLeast(1),potentialMax=99,note="Scout beobachtet den Spieler."))
  if(playerId !in w.watchlist)w.watchlist.add(playerId)
 }
 fun report(w:World,p:Player):ScoutReport?=if(p.clubId==w.user.clubId)ScoutReport(p.id,100,p.ca,p.ca,p.hidden.potential,p.hidden.potential,true,true,w.calendar.absoluteWeek,"Vollständige interne Daten") else w.scoutReports[p.id]
 fun strengthLabel(w:World,p:Player):String{val r=report(w,p);return if(r==null)ClubActions.scouting(w,p) else if(r.progress>=95)"${p.ca}" else "${r.caMin}–${r.caMax}"}
 fun potentialLabel(w:World,p:Player):String{val r=report(w,p)?:return "noch offen";return if(r.progress>=95)"${p.hidden.potential}" else "${r.potentialMin}–${r.potentialMax}"}
 fun competition(w:World,playerId:Int)=w.competingBids.filter{it.playerId==playerId&&it.expiresWeek>=w.calendar.absoluteWeek}
 fun bosmanEligible(w:World,p:Player):Boolean{
  val age=w.calendar.season-p.birthYear
  return p.clubId!=0&&p.clubId!=w.user.clubId&&!p.retired&&p.loanParentClubId==0&&age>=18&&p.contractYears<=1&&w.calendar.matchday>=18&&p.precontractClubId==0
 }
 fun precontractDemand(w:World,p:Player):Pair<Int,Long>{
  val interest=TransferInterestSystem.score(w,p,w.user.clubId);val relation=w.agentRelations[p.agentId]?:50
  val wage=maxOf(p.wage+1,(p.wage*(1.12+(65-interest).coerceAtLeast(0)*.004)).roundToInt(),(TransferEngine.marketValue(w,p)/43000L).toInt())
  val signing=(wage*(10+(60-relation).coerceAtLeast(0)/8)).toLong().coerceAtLeast(1000L)
  return wage to signing
 }
 fun signPrecontract(w:World,playerId:Int,years:Int=3){
  val p=w.players.getValue(playerId);require(bosmanEligible(w,p)){"Ein Bosman-Vorvertrag ist aktuell nicht möglich."}
  val interest=TransferInterestSystem.score(w,p,w.user.clubId);require(interest>=56){"Der Spieler möchte aktuell keinen Vorvertrag unterschreiben."}
  val (wage,bonus)=precontractDemand(w,p);require(w.club().budget>=bonus){"Budget für das Handgeld reicht nicht."}
  w.club().budget-=bonus;p.precontractClubId=w.user.clubId;p.precontractSeason=w.calendar.season+1;p.precontractWage=wage;p.precontractYears=years
  w.news("Vorvertrag unterschrieben","${p.name} kommt zur Saison ${p.precontractSeason} ablösefrei. Gehalt $wage €/Woche · Handgeld $bonus €.","good")
 }
 fun extendContract(w:World,playerId:Int,years:Int){
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&!p.retired&&playerId!=w.user.playerId){"Verlängerung nicht möglich."};require(years in 2..5)
  val newWage=maxOf(p.wage+1,(p.wage*1.08).roundToInt());val bonus=(newWage*6L).coerceAtLeast(400L);require(w.club().budget>=bonus){"Budget für Handgeld reicht nicht."}
  w.club().budget-=bonus;p.contractYears=years;p.wage=newWage;p.precontractClubId=0;p.precontractSeason=0;p.precontractWage=0;p.precontractYears=0
  w.news("Vertrag verlängert","${p.name} unterschreibt für $years Jahre · $newWage €/Woche.","good")
 }
 fun recallLoan(w:World,playerId:Int){
  val p=w.players.getValue(playerId);require(p.loanParentClubId==w.user.clubId&&p.clubId!=w.user.clubId&&p.loanRecallAllowed){"Diese Leihe kann aktuell nicht zurückgerufen werden."}
  val loanClub=p.clubId;p.clubId=w.user.clubId;p.youth=p.loanReturnYouth;if(p.youth)p.youthSquad=p.loanReturnYouthSquad;p.loanParentClubId=0;p.loanBuyerClubId=0;p.loanWeeks=0;p.loanOptionFee=0;p.loanRecallAllowed=true;p.loanReturnYouth=false;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null
  WorldFactory.autoLineup(w,w.user.clubId);w.clubs[loanClub]?.let{WorldFactory.autoLineup(w,it.id)};w.news("Leihe beendet","${p.name} kehrt vorzeitig zurück${if(p.youth)" in ${p.youthSquad.label}" else ""}.","normal")
 }
 private fun updateReport(w:World,a:ScoutAssignment){
  val p=w.players[a.playerId]?:return;val r=w.scoutReports.getOrPut(p.id){ScoutReport(playerId=p.id)}
  val gain=(26+w.club().academy.scouting/4+w.club().dynamics.staffQuality/8).coerceIn(28,55);r.progress=(r.progress+gain).coerceAtMost(100);r.lastUpdatedWeek=w.calendar.absoluteWeek
  val spread=((100-r.progress)/6+1).coerceIn(1,17);r.caMin=(p.ca-spread).coerceAtLeast(1);r.caMax=(p.ca+spread).coerceAtMost(99)
  val pSpread=((100-r.progress)/4+2).coerceIn(2,24);r.potentialMin=(p.hidden.potential-pSpread).coerceAtLeast(p.ca);r.potentialMax=(p.hidden.potential+pSpread).coerceAtMost(99)
  r.personalityKnown=r.progress>=70;r.medicalKnown=r.progress>=88;r.note=when{r.progress>=100->"Scouting abgeschlossen";r.progress>=70->"Charakter und Rollenprofil sind belastbar";else->"Beobachtung läuft"}
 }
 private fun refreshCompetingBids(w:World,rng:SeededRandom){
  w.competingBids.removeAll{it.expiresWeek<w.calendar.absoluteWeek||w.players[it.playerId]?.retired!=false}
  for(pid in w.watchlist.toList().take(30)){
   val p=w.players[pid]?:continue;if(p.clubId==0||p.clubId==w.user.clubId||w.competingBids.any{it.playerId==pid})continue
   if(rng.chance(.10+(p.ca-60).coerceAtLeast(0)*.003)){
    val candidates=w.clubs.values.filter{it.id!=w.user.clubId&&it.id!=p.clubId&&it.reputation>=((w.clubs[p.clubId]?.reputation?:30)-12)}
    if(candidates.isNotEmpty()){val club=rng.pick(candidates);val fee=(TransferEngine.marketValue(w,p)*(90+rng.int(0,28))/100).coerceAtLeast(500);w.competingBids.add(CompetingBid(pid,club.id,fee,w.calendar.absoluteWeek+4))}
   }
  }
 }
 fun weekly(w:World,rng:SeededRandom){
  for(a in w.scoutAssignments.values.toList()){updateReport(w,a);a.weeksRemaining--;if(a.weeksRemaining<=0||w.scoutReports[a.playerId]?.progress==100)w.scoutAssignments.remove(a.playerId)}
  if(w.calendar.absoluteWeek%2==0)refreshCompetingBids(w,rng)
 }
 fun newSeason(w:World){
  val newSeason=w.calendar.season
  for(p in w.players.values.toList()){
   if(p.retired||p.youth||p.id==w.user.playerId)continue
   if(p.precontractClubId!=0&&p.precontractSeason<=newSeason){
    val from=p.clubId;val to=p.precontractClubId;p.clubId=to;p.wage=p.precontractWage;p.contractYears=maxOf(2,p.precontractYears);p.precontractClubId=0;p.precontractSeason=0;p.precontractWage=0;p.precontractYears=0;p.wantsMove=false
    w.transferHistory.add(0,TransferHistoryEntry(newSeason,w.calendar.absoluteWeek,p.id,from,to,DealType.BUY,0,"Bosman / Vorvertrag"));WorldFactory.autoLineup(w,to);if(from in w.clubs)WorldFactory.autoLineup(w,from)
    if(to==w.user.clubId)w.news("Bosman-Transfer vollzogen","${p.name} ist ablösefrei zum Verein gestoßen.","good")
    continue
   }
   if(p.clubId!=0){p.contractYears=(p.contractYears-1).coerceAtLeast(0);if(p.contractYears==0){val from=p.clubId;p.clubId=0;p.wage=0;p.wantsMove=true;w.transferHistory.add(0,TransferHistoryEntry(newSeason,w.calendar.absoluteWeek,p.id,from,0,DealType.BUY,0,"Vertragsende"));if(from==w.user.clubId)w.news("Vertrag ausgelaufen","${p.name} verlässt den Verein ablösefrei.","bad")}}
  }
  w.competingBids.clear();w.scoutAssignments.clear()
 }
}

object TacticalInstructionSystem {
 fun instructions(c:Club,playerId:Int):List<PlayerInstruction> = c.tactics.instructions[playerId]?:emptyList()
 fun has(c:Club,playerId:Int,i:PlayerInstruction)=i in instructions(c,playerId)
 fun set(w:World,playerId:Int,i:PlayerInstruction,enabled:Boolean){
  val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId){"Nur eigene Spieler erhalten Anweisungen."};require(w.live==null){"Individuelle Anweisungen vor dem Spiel festlegen."}
  val list=w.club().tactics.instructions.getOrPut(playerId){mutableListOf()}
  if(enabled){
   if(i==PlayerInstruction.PRESS_MORE)list.remove(PlayerInstruction.PRESS_LESS);if(i==PlayerInstruction.PRESS_LESS)list.remove(PlayerInstruction.PRESS_MORE)
   require(i in list||list.size<3){"Maximal drei individuelle Anweisungen pro Spieler."};if(i !in list)list.add(i)
  }else list.remove(i)
  if(list.isEmpty())w.club().tactics.instructions.remove(playerId)
 }
 fun teamFactor(c:Club,ids:List<Int>,attack:Boolean):Double{
  var bonus=0.0
  ids.forEach{id->instructions(c,id).forEach{i->bonus+=if(attack)when(i){PlayerInstruction.RUN_IN_BEHIND->.008;PlayerInstruction.OVERLAP->.006;PlayerInstruction.CUT_INSIDE->.006;PlayerInstruction.RISKY_PASSES->.004;PlayerInstruction.SHOOT_MORE->.005;PlayerInstruction.HOLD_POSITION->-.004;else->0.0}else when(i){PlayerInstruction.HOLD_POSITION->.007;PlayerInstruction.TIGHT_MARKING->.008;PlayerInstruction.PRESS_MORE->.006;PlayerInstruction.PRESS_LESS->-.004;PlayerInstruction.OVERLAP->-.003;else->0.0}}
  }
  return (1.0+bonus).coerceIn(.92,1.12)
 }
 fun fitnessFactor(c:Club,id:Int):Double{var v=1.0;for(i in instructions(c,id))v*=when(i){PlayerInstruction.PRESS_MORE->1.12;PlayerInstruction.OVERLAP->1.08;PlayerInstruction.RUN_IN_BEHIND->1.06;PlayerInstruction.PRESS_LESS->.92;PlayerInstruction.HOLD_POSITION->.95;else->1.0};return v.coerceIn(.82,1.28)}
 fun shooterWeight(c:Club,p:Player):Int{val x=instructions(c,p.id);return (if(PlayerInstruction.SHOOT_MORE in x)5 else 0)+(if(PlayerInstruction.RUN_IN_BEHIND in x)3 else 0)+(if(PlayerInstruction.CUT_INSIDE in x&&p.position in listOf(Position.LA,Position.RA))3 else 0)}
 fun creatorWeight(c:Club,p:Player):Int{val x=instructions(c,p.id);return (if(PlayerInstruction.RISKY_PASSES in x)5 else 0)+(if(PlayerInstruction.OVERLAP in x)2 else 0)}
 fun offsideExtra(c:Club,id:Int)=if(has(c,id,PlayerInstruction.RUN_IN_BEHIND)).012 else 0.0
 fun riskyPass(c:Club,id:Int)=has(c,id,PlayerInstruction.RISKY_PASSES)
}

data class PassNetworkEdge(val fromId:Int,val toId:Int,val count:Int,val startX:Float,val startY:Float,val endX:Float,val endY:Float)
data class PassNetworkNode(val playerId:Int,val x:Float,val y:Float,val touches:Int)

object MatchAnalysisSystem {
 fun recordTacticChange(w:World,m:LiveMatch,clubId:Int,label:String){
  val stats=if(clubId==m.homeId)m.home else m.away
  val last=m.tacticChanges.lastOrNull();if(last!=null&&last.clubId==clubId&&last.minute==m.minute)return
  m.tacticChanges.add(TacticChangeEvent(m.minute,clubId,label,stats.xg,stats.shots,stats.possessionTicks))
 }

 fun xgByIntervals(m:LiveMatch,clubId:Int):List<Pair<Int,Double>>{
  val endMinute=maxOf(m.minute,m.shotEvents.maxOfOrNull{it.minute}?:0,1)
  val marks=mutableListOf<Int>();var mark=15
  while(mark<=endMinute){marks.add(mark);mark+=15}
  if(marks.isEmpty()||marks.last()!=endMinute)marks.add(endMinute)
  return marks.distinct().map{end->end to m.shotEvents.asSequence().filter{it.clubId==clubId&&it.minute<=end}.sumOf{it.xg}}
 }

 fun passEdges(m:LiveMatch,clubId:Int):List<Triple<Int,Int,Int>> = m.passEvents.asSequence().filter{it.clubId==clubId&&it.completed&&it.fromId!=0&&it.toId!=0}.groupingBy{if(it.fromId<it.toId)it.fromId to it.toId else it.toId to it.fromId}.eachCount().entries.sortedByDescending{it.value}.map{Triple(it.key.first,it.key.second,it.value)}

 fun passNetworkEdges(m:LiveMatch,clubId:Int):List<PassNetworkEdge>{
  val events=m.passEvents.asSequence().filter{it.clubId==clubId&&it.completed&&it.fromId!=0&&it.toId!=0}.toList()
  return events.groupBy{it.fromId to it.toId}.map{(key,group)->
   PassNetworkEdge(key.first,key.second,group.size,group.map{it.startX}.average().toFloat(),group.map{it.startY}.average().toFloat(),group.map{it.endX}.average().toFloat(),group.map{it.endY}.average().toFloat())
  }.sortedByDescending{it.count}
 }

 fun passNetworkNodes(m:LiveMatch,clubId:Int):List<PassNetworkNode>{
  val events=m.passEvents.asSequence().filter{it.clubId==clubId&&it.completed&&it.fromId!=0&&it.toId!=0}.toList();if(events.isEmpty())return emptyList()
  val ids=events.asSequence().flatMap{sequenceOf(it.fromId,it.toId)}.distinct().toList()
  return ids.map{id->
   var sx=0.0;var sy=0.0;var n=0
   for(e in events){if(e.fromId==id){sx+=e.startX;sy+=e.startY;n++};if(e.toId==id){sx+=e.endX;sy+=e.endY;n++}}
   PassNetworkNode(id,(sx/n.coerceAtLeast(1)).toFloat(),(sy/n.coerceAtLeast(1)).toFloat(),n)
  }.sortedByDescending{it.touches}
 }

 fun completedPasses(m:LiveMatch,clubId:Int)=m.passEvents.count{it.clubId==clubId&&it.completed&&it.fromId!=0&&it.toId!=0}
 fun attemptedPasses(m:LiveMatch,clubId:Int)=m.passEvents.count{it.clubId==clubId&&it.fromId!=0&&it.toId!=0}

 fun sinceLastChange(m:LiveMatch,clubId:Int):String{
  val change=m.tacticChanges.lastOrNull{it.clubId==clubId}?:return "Noch keine Live-Taktikänderung erfasst."
  val st=if(clubId==m.homeId)m.home else m.away;val dxg=st.xg-change.xg;val shots=st.shots-change.shots;val poss=(st.possessionTicks-change.possessionTicks).coerceAtLeast(0)
  return "Seit ${change.minute}. Min (${change.label}): ${"%.2f".format(dxg)} xG · $shots Schüsse · $poss Ballbesitz-Ticks"
 }
 fun tacticImpactRows(m:LiveMatch,clubId:Int):List<String>{
  val changes=m.tacticChanges.filter{it.clubId==clubId};if(changes.isEmpty())return emptyList();val finalStats=if(clubId==m.homeId)m.home else m.away
  return changes.mapIndexed{i,c->val next=changes.getOrNull(i+1);val endXg=next?.xg?:finalStats.xg;val endShots=next?.shots?:finalStats.shots;val endMinute=next?.minute?:m.minute;"${c.minute}–$endMinute' · ${c.label}: +${"%.2f".format((endXg-c.xg).coerceAtLeast(0.0))} xG · +${(endShots-c.shots).coerceAtLeast(0)} Schüsse"}
 }
}

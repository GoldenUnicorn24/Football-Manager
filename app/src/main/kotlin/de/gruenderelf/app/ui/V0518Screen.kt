package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*

@Composable fun V0518Screen(w:World,vm:GameViewModel,onProfile:(Int)->Unit){
 var scoutId by rememberSaveable{mutableIntStateOf(w.watchlist.firstOrNull()?:w.players.values.firstOrNull{!it.retired&&it.clubId!=w.user.clubId}?.id?:0)}
 var scoutRegion by rememberSaveable{mutableStateOf(ScoutRegion.DOMESTIC)}
 var tacticName by rememberSaveable{mutableStateOf("Mein Matchplan")}
 var outboundPlayerId by rememberSaveable{mutableIntStateOf(w.squad().firstOrNull{it.id!=w.user.playerId&&!it.retired&&it.loanParentClubId==0}?.id?:0)}
 var outboundType by rememberSaveable{mutableStateOf(DealType.BUY)}
 Page("Manager-Zentrale","GRÜNDERELF · v0.5.22"){
  Section("Changelog & erneuertes Tutorial"){
   Text("v0.5.22 schließt die noch fehlenden v0.5.18-Recovery-Funktionen und behält den großen Ligen-Ausbau bei.",color=Grass)
   Text("Wieder vollständig: mehrere Verkaufs-/Leihangebote aktiv einholen, Bosman-Vorverträge, Vertragsverlängerungen, Zielkader U19/U23 bei Transfers sowie xG-Verlauf und echtes Passnetz. Die erweiterten europäischen Ligen, Qualifikationen und Wettbewerbe bleiben erhalten.",color=Muted)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Tutorial aktiv");Switch(w.user.tutorialEnabled,{v->vm.action{it.user.tutorialEnabled=v;if(v)it.user.tutorialCompleted=false}})}
   Pick("Karrierefokus",w.user.playerCareerFocus,PlayerCareerFocus.entries.toList(),{it.label}){v->vm.action{it.user.playerCareerFocus=v}}
   Text(w.user.playerCareerFocus.description,color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Benachrichtigungen · ${NotificationSystem.totalUnread(w)} neu"){
   val tired=NotificationSystem.unreadTired(w);val contracts=NotificationSystem.unreadExpiring(w);val reports=NotificationSystem.unreadScoutReports(w)
   Metric("Müde Spieler",tired.size.toString());Metric("Auslaufende Verträge",contracts.size.toString());Metric("Fertige Scoutberichte",reports.size.toString())
   if(tired.isNotEmpty())Text(tired.mapNotNull{w.players[it]?.lastName}.take(5).joinToString(", "),color=Gold)
   Action("Benachrichtigungen als gelesen markieren",secondary=true){vm.action{NotificationSystem.markTiredSeen(it);NotificationSystem.markContractsSeen(it);NotificationSystem.markScoutReportsSeen(it)}}
  }
  Section("U19 & U23 Spielbetrieb"){
   val a=w.club().academy
   Metric("U19","${a.u19Season.points} P · ${a.u19Season.goalsFor}:${a.u19Season.goalsAgainst} · ${a.u19Season.lastResult.ifBlank{"–"}}")
   Metric("U23","${a.u23Season.points} P · ${a.u23Season.goalsFor}:${a.u23Season.goalsAgainst} · ${a.u23Season.lastResult.ifBlank{"–"}}")
   val youth=w.squad().filter{it.youth||it.temporarySeniorCallUp}.sortedWith(compareBy<Player>{it.youthSquad}.thenByDescending{it.ca})
   if(youth.isEmpty())Text("Aktuell keine Nachwuchsspieler im U19/U23-Kader.",color=Muted)
   youth.take(20).forEach{p->
    Text("${p.name} · ${if(p.temporarySeniorCallUp)"Profis (temporär)" else p.youthSquad.label} · Stärke ${p.ca}",style=MaterialTheme.typography.titleMedium)
    Text("Jugend: ${p.youthTeamStats.appearances} Sp. · ${p.youthTeamStats.goals} T · ${p.youthTeamStats.assists} A · Ø ${"%.2f".format(p.youthTeamStats.averageRating)}",color=Muted)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){if(!p.temporarySeniorCallUp){TextButton({vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U19)}}){Text("U19")};TextButton({vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U23)}}){Text("U23")};TextButton({vm.action{YouthCompetitionSystem.callUp(it,p.id)}}){Text("Zu Profis")}}else TextButton({vm.action{YouthCompetitionSystem.returnToYouth(it,p.id)}}){Text("Zurück")};TextButton({onProfile(p.id)}){Text("Profil")}}
    HorizontalDivider()
   }
  }
  Section("Scouting-Zentrale"){
   val external=w.players.values.filter{!it.retired&&it.clubId!=w.user.clubId}.sortedByDescending{TransferInterestSystem.score(w,it,w.user.clubId)}.take(100)
   if(external.isNotEmpty()){
    if(external.none{it.id==scoutId})scoutId=external.first().id
    Pick("Spieler",scoutId,external.map{it.id},{id->w.players[id]?.name?:"Spieler"}){scoutId=it};Pick("Region",scoutRegion,ScoutRegion.entries.toList(),{it.label}){scoutRegion=it}
    val p=w.players.getValue(scoutId);val report=ScoutingTransferSystem.report(w,p.id)
    report?.let{Text("Bericht ${it.progress}% · Stärke ${it.caMin}–${it.caMax} · Potenzial ${it.potentialMin}–${it.potentialMax}",color=Grass);Text(it.note,color=Muted)}
    Action(if(p.id in w.scoutAssignments)"Scouting läuft" else "Scout beauftragen · ${euros(ScoutingTransferSystem.cost(w,p,scoutRegion))}",p.id !in w.scoutAssignments&&w.live==null){vm.action{ScoutingTransferSystem.start(it,p.id,scoutRegion)}}
    Action(if(p.id in w.watchlist)"Von Watchlist entfernen" else "Zur Watchlist",secondary=true){vm.action{world->if(!world.watchlist.remove(p.id))world.watchlist.add(p.id)}}
    val bids=ScoutingTransferSystem.activeCompetingBids(w,p.id);if(bids.isNotEmpty())Text("Konkurrenz: "+bids.joinToString{b->"${w.clubs[b.clubId]?.shortName?:"Club"} ${euros(b.fee)}"},color=Gold)
    Text("Vertrag: ${p.contractYears} Saison(en)",color=Muted)
    when{p.precontractClubId==w.user.clubId->Text("Bosman-Vorvertrag: Wechsel ab Saison ${p.precontractSeason}",color=Grass);TransferV0518System.canSignPrecontract(w,p)->Action("Bosman / Vorvertrag · ca. ${euros(TransferV0518System.estimatedPrecontractWage(w,p).toLong())}/Woche",secondary=true){vm.action{TransferV0518System.signPrecontract(it,p.id)}}}
   }
  }
  Section("Eigene Spieler verkaufen & verleihen"){
   val candidates=w.squad().filter{it.id!=w.user.playerId&&!it.retired&&it.loanParentClubId==0}.sortedByDescending{it.ca}
   if(candidates.isEmpty())Text("Aktuell kann kein eigener Spieler angeboten werden.",color=Muted) else {
    if(candidates.none{it.id==outboundPlayerId})outboundPlayerId=candidates.first().id
    Pick("Spieler",outboundPlayerId,candidates.map{it.id},{id->w.players[id]?.name?:"Spieler"}){outboundPlayerId=it}
    Pick("Angebotsart",outboundType,listOf(DealType.BUY,DealType.LOAN),{it.label}){outboundType=it}
    Text("Der Spieler wird mehreren passenden Vereinen angeboten. Jedes Angebot bleibt separat verhandelbar.",color=Muted)
    Action("Mehrere Angebote einholen",w.live==null&&ScoutingTransferSystem.windowOpen(w)){vm.action{OutboundTransferSystem.requestOffers(it,outboundPlayerId,outboundType)}}
    if(!ScoutingTransferSystem.windowOpen(w))Text("Das Transferfenster ist derzeit geschlossen.",color=Gold)
   }
  }
  val outbound=w.negotiations.values.filter{OutboundTransferSystem.isOutbound(w,it)&&it.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.COMPLETED)}.sortedByDescending{it.id}
  Section("Angebote für unsere Spieler"){
   if(outbound.isEmpty())Text("Derzeit liegt kein aktives Angebot vor. Du kannst oben selbst eine Angebotsrunde starten; KI-Clubs melden sich weiterhin auch eigenständig.",color=Muted)
   outbound.forEach{o->val p=w.players[o.playerId]?:return@forEach;Text("${p.name} · ${w.clubs[o.buyerClubId]?.name?:"Verein"}",style=MaterialTheme.typography.titleMedium);Text("${o.type.label} · ${euros(o.fee)}${if(o.buyOption>0)" · Kaufoption ${euros(o.buyOption)}" else ""}",color=Muted);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton({vm.action{OutboundTransferSystem.negotiate(it,o.id,"fee")}}){Text("Nachverhandeln")};TextButton({vm.action{OutboundTransferSystem.accept(it,o.id)}}){Text("Annehmen")};TextButton({vm.action{OutboundTransferSystem.reject(it,o.id)}}){Text("Ablehnen")}};HorizontalDivider()}
  }
  Section("Medizincheck & Registrierung"){
   val incoming=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.status==NegotiationStatus.AGREED&&it.stage in listOf(TransferStage.MEDICAL,TransferStage.REGISTRATION)}
   if(incoming.isEmpty())Text("Keine Grundsatzeinigung wartet auf Medizincheck oder Registrierung.",color=Muted)
   incoming.forEach{o->
    val p=w.players[o.playerId]?:return@forEach;val age=w.calendar.season-p.birthYear
    Text("${p.name} · ${o.stage.label}",style=MaterialTheme.typography.titleMedium);Text(o.medicalNote.ifBlank{o.message},color=Muted)
    if(age<=22){Text("Zielkader: ${o.targetYouthSquad?.label?:"Profis"}",color=Muted);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton({vm.action{TransferV0518System.setTargetYouthSquad(it,o.id,null)}}){Text("Profis")};if(age<=19)TextButton({vm.action{TransferV0518System.setTargetYouthSquad(it,o.id,YouthSquad.U19)}}){Text("U19")};TextButton({vm.action{TransferV0518System.setTargetYouthSquad(it,o.id,YouthSquad.U23)}}){Text("U23")}}}
    Action(if(o.registrationReady)"Transfer registrieren" else "Medizincheck & Registrierung durchführen"){vm.action{world->if(!o.registrationReady)TransferV0518System.medicalAndRegistration(world,o);if(o.registrationReady)TransferEngine.complete(world,o.id)}}
    Action("Verhandlung zurückziehen",secondary=true){vm.action{TransferV0518System.withdraw(it,o.id)}};HorizontalDivider()
   }
  }
  Section("Transferhistorie"){
   if(w.transferHistory.isEmpty())Text("Noch keine abgeschlossenen Transfers in der neuen Historie.",color=Muted)
   w.transferHistory.take(20).forEach{h->Text("${w.players[h.playerId]?.name?:"Spieler"} · ${w.clubs[h.fromClubId]?.shortName?:"frei"} → ${w.clubs[h.toClubId]?.shortName?:"frei"}",style=MaterialTheme.typography.titleMedium);Text("${h.type.label} · ${euros(h.fee)} · Woche ${h.week+1}",color=Muted)}
  }
  Section("Vertrag & Versprechen"){
   val expiring=w.squad().filter{!it.retired&&it.loanParentClubId==0&&it.contractYears<=1}.sortedByDescending{it.ca}
   if(expiring.isEmpty())Text("Kein eigener Vertrag läuft zum Saisonende aus.",color=Muted)
   expiring.take(12).forEach{p->Text("${p.name} · noch ${p.contractYears} Saison(en) · ${euros(p.wage.toLong())}/Woche",style=MaterialTheme.typography.titleMedium);Action("Vertrag verlängern",secondary=true){vm.action{TransferV0518System.extendContract(it,p.id,3)}};HorizontalDivider()}
  }
  Section("Co-Trainer-Profil"){
   Pick("Profil",w.assistantCoach.profile,AssistantCoachProfile.entries.toList(),{it.label}){v->vm.action{it.assistantCoach.profile=v}}
   Text(w.assistantCoach.profile.description,color=Muted)
  }
  Section("Taktik-Zentrale"){
   OutlinedTextField(tacticName,{tacticName=it.take(30)},label={Text("Name des Matchplans")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({vm.action{it.savedTactics[tacticName.ifBlank{"Matchplan"}]=SavedTactic(it.club().tactics.formation,it.club().tactics.mentality,it.club().tactics.pressing,it.club().tactics.line,it.club().tactics.tempo,it.club().tactics.width,it.club().tactics.buildUp)}}){Text("Speichern")};w.savedTactics.keys.firstOrNull()?.let{name->TextButton({vm.action{world->world.savedTactics[name]?.let{t->with(world.club().tactics){formation=t.formation;mentality=t.mentality;pressing=t.pressing;line=t.line;tempo=t.tempo;width=t.width;buildUp=t.buildUp}}}}){Text("$name laden")}}}
   Text("Spieleranweisungen für ${w.self().lastName}",style=MaterialTheme.typography.titleMedium);PlayerInstruction.entries.forEach{i->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(i.label);Checkbox(TacticalInstructionSystem.has(w.club(),w.user.playerId,i),{vm.action{world->TacticalInstructionSystem.toggle(world.club(),world.user.playerId,i)}})}}
  }
  Section("Analyse & Taktik-Impact"){
   val last=w.matches.values.filter{it.homeId==w.user.clubId||it.awayId==w.user.clubId}.maxByOrNull{it.fixtureId}
   if(last==null)Text("Nach dem nächsten Spiel stehen Passnetz und Taktik-Impact bereit.",color=Muted) else {
    val clubId=w.user.clubId;val timeline=MatchAnalysisSystem.xgTimeline(last,clubId);val network=MatchAnalysisSystem.passNetwork(last,clubId);val completed=last.passEvents.count{it.clubId==clubId&&it.completed};val total=last.passEvents.count{it.clubId==clubId};val xg=timeline.lastOrNull()?.second?:0.0
    Metric("xG",String.format("%.2f",xg));Metric("Erfasste Pässe","${completed} / ${total}");Metric("Passnetz","${network.first.size} Spieler · ${network.second.size} Verbindungen")
    if(timeline.isNotEmpty())Text("xG-Verlauf: "+timeline.takeLast(6).joinToString(" · "){"${it.first}' ${String.format("%.2f",it.second)}"},color=Muted)
    network.second.take(5).forEach{e->Text("${w.players[e.fromId]?.lastName?:"?"} → ${w.players[e.toId]?.lastName?:"?"}: ${e.count} Pässe",color=Muted)}
    Text("Taktikänderungen: ${last.tacticChanges.count{it.clubId==clubId}}",color=Muted)
   }
  }
 }
}

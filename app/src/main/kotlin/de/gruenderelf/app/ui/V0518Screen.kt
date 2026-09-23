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
 var tacticName by rememberSaveable{mutableStateOf("Mein Matchplan")}\n var outboundPlayerId by rememberSaveable{mutableIntStateOf(w.squad().firstOrNull{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}?.id?:0)}\n var outboundType by rememberSaveable{mutableStateOf(DealType.BUY)}
 Page("Manager-Zentrale","GRÜNDERELF · v0.5.22"){
  Section("Changelog & erneuertes Tutorial"){
   Text("v0.5.18 stellt die in der aktuellen APK enthaltenen Manager-Systeme wieder her.",color=Grass)
   Text("Neu bzw. wiederhergestellt: U19/U23-Spielbetrieb, Potenzialtraining, Scouting-Zentrale und Watchlist, Verkauf/Verleih eigener Spieler, Medizincheck & Registrierung, Transferhistorie, Konkurrenzangebote, Co-Trainer-Profile, Benachrichtigungen, Taktik-Zentrale sowie Matchanalyse mit Passdaten.",color=Muted)
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
   }
  }
  Section("Eigene Spieler verkaufen & verleihen"){
   val offerable=w.squad().filter{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}.sortedByDescending{TransferEngine.marketValue(w,it)}
   if(offerable.isEmpty())Text("Aktuell ist kein eigener Spieler für Verkauf oder Leihe verfügbar.",color=Muted) else {
    if(offerable.none{it.id==outboundPlayerId})outboundPlayerId=offerable.first().id
    Pick("Eigener Spieler",outboundPlayerId,offerable.map{it.id},{id->w.players[id]?.name?:"Spieler"}){outboundPlayerId=it}
    Pick("Angebotsart",outboundType,listOf(DealType.BUY,DealType.LOAN,DealType.LOAN_OPTION),{it.label}){outboundType=it}
    val open=ScoutingTransferSystem.windowOpen(w)
    Action("Mehrere Angebote einholen",w.live==null&&open){vm.action{OutboundTransferSystem.requestOffers(it,outboundPlayerId,outboundType)}}
    Text(if(open)"Der Spieler wird aktiv mehreren passenden Vereinen angeboten. Angebote bleiben getrennt und können einzeln verhandelt werden." else "Das Transferfenster ist geschlossen.",color=Muted)
   }
  }
  val outbound=OutboundTransferSystem.activeOffers(w)
  Section("Angebote für unsere Spieler"){
   if(outbound.isEmpty())Text("Derzeit liegt kein aktives Angebot für einen eigenen Spieler vor.",color=Muted)
   outbound.forEach{o->
    val p=w.players[o.playerId]?:return@forEach
    Text("${p.name} · ${w.clubs[o.buyerClubId]?.name?:"Verein"}",style=MaterialTheme.typography.titleMedium)
    Text("${o.type.label} · ${euros(o.fee)}${if(o.buyOption>0)" · Kaufoption ${euros(o.buyOption)}" else ""} · ${o.message}",color=Muted)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
     TextButton({vm.action{OutboundTransferSystem.negotiate(it,o.id,"fee")}},enabled=o.status==NegotiationStatus.COUNTER){Text(if(o.type==DealType.BUY)"Ablöse +" else "Leihgebühr +")}
     if(o.type==DealType.LOAN_OPTION)TextButton({vm.action{OutboundTransferSystem.negotiate(it,o.id,"option")}},enabled=o.status==NegotiationStatus.COUNTER){Text("Kaufoption +")}
     TextButton({vm.action{OutboundTransferSystem.negotiate(it,o.id,"sellon")}},enabled=o.status==NegotiationStatus.COUNTER){Text("Weiterverkauf +")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton({vm.action{OutboundTransferSystem.accept(it,o.id)}}){Text("Annehmen")};TextButton({vm.action{OutboundTransferSystem.reject(it,o.id)}}){Text("Ablehnen")};TextButton({onProfile(p.id)}){Text("Profil")}}
    HorizontalDivider()
   }
  }
  Section("Medizincheck & Registrierung"){
   val incoming=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.status==NegotiationStatus.AGREED&&it.stage in listOf(TransferStage.MEDICAL,TransferStage.REGISTRATION)}
   if(incoming.isEmpty())Text("Keine Grundsatzeinigung wartet auf Medizincheck oder Registrierung.",color=Muted)
   incoming.forEach{o->val p=w.players[o.playerId]?:return@forEach;Text("${p.name} · ${o.stage.label}",style=MaterialTheme.typography.titleMedium);Text(o.medicalNote.ifBlank{o.message},color=Muted);Action(if(o.registrationReady)"Transfer registrieren" else "Medizincheck & Registrierung durchführen"){vm.action{world->if(!o.registrationReady)TransferV0518System.medicalAndRegistration(world,o);if(o.registrationReady)TransferEngine.complete(world,o.id)}};Action("Verhandlung zurückziehen",secondary=true){vm.action{TransferV0518System.withdraw(it,o.id)}}}
  }
  Section("Transferhistorie"){
   if(w.transferHistory.isEmpty())Text("Noch keine abgeschlossenen Transfers in der neuen Historie.",color=Muted)
   w.transferHistory.take(20).forEach{h->Text("${w.players[h.playerId]?.name?:"Spieler"} · ${w.clubs[h.fromClubId]?.shortName?:"frei"} → ${w.clubs[h.toClubId]?.shortName?:"frei"}",style=MaterialTheme.typography.titleMedium);Text("${h.type.label} · ${euros(h.fee)} · Woche ${h.week+1}",color=Muted)}
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
   if(last==null)Text("Nach dem nächsten Spiel stehen Passnetz und Taktik-Impact bereit.",color=Muted) else {val clubId=w.user.clubId;val timeline=last.shotEvents.filter{it.clubId==clubId}.sumOf{it.xg};val completed=last.passEvents.count{it.clubId==clubId&&it.completed};val total=last.passEvents.count{it.clubId==clubId};Metric("xG",String.format("%.2f",timeline));Metric("Erfasste Pässe","$completed / $total");Text("Taktikänderungen: ${last.tacticChanges.count{it.clubId==clubId}}",color=Muted)}
  }
 }
}

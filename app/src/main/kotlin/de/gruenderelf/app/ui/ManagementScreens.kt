package de.gruenderelf.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*

@Composable fun MoreScreen(onNavigate: (String)->Unit,onMenu: ()->Unit){Page("Hinter der Bande","VEREIN & VERANTWORTUNG"){listOf("verein" to "Verein, Finanzen & Partner","training" to "Training & Co-Trainer","transfers" to "Transfers & Jugend","trophaeen" to "Trophäenschrank","karriere" to "Deine Karriere","hilfe" to "Hilfe & Tutorial","editor" to "Editor","einstellungen" to "Einstellungen","speichern" to "Speicherstände").forEach{(route,label)->Action(label,secondary=true){onNavigate(route)}};Section("Gründerelf · 0.5.22"){Text("Offline. Ohne Werbung. Ohne Käufe.",color=Grass);Text("Das vollständige v0.5.18-Spielsystem bleibt erhalten. Ergänzt wurden weitere europäische Ligen und Pokale, moderne UEFA-Qualifikation, Club World Cup, Krone der Kontinente sowie kompaktere und schnellere Spielstände.",color=Muted)};Action("Speichern & zum Startbildschirm",secondary=true,onClick=onMenu)}}

@Composable fun TrainingScreen(w: World,vm: GameViewModel){
 val effective=TrainingEngine.effectiveDays(w);val amateur=!w.privateTopClubMode&&w.club().tier>=7;val assistant=w.assistantCoach
 var playerId by remember{mutableIntStateOf(w.user.playerId)};var focus by remember{mutableStateOf(Focus.FINISHING)}
 var intensivePlayerId by rememberSaveable{mutableIntStateOf(w.user.playerId)};var intensiveFocus by rememberSaveable{mutableStateOf(Focus.TECHNIQUE)};var intensiveConfirm by remember{mutableStateOf<Int?>(null)}
 var masterclassPlayerId by rememberSaveable{mutableIntStateOf(w.user.playerId)};var masterclassConfirm by remember{mutableStateOf<Int?>(null)}
 Page("Training & Co-Trainer","PLANUNG · ENTWICKLUNG · DELEGATION"){
  Section("Co-Trainer-Automatik"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Profikader automatisch trainieren");Text("Der Co-Trainer plant Woche, Intensität, Gegnerfokus und individuelle Schwerpunkte.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoSeniorTraining,{v->vm.action{it.assistantCoach.autoSeniorTraining=v}})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Jugend automatisch entwickeln");Text("Seniortraining, Mentoren und Extra-Foki werden nach Reife und Risiko dosiert.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoYouthTraining,{v->vm.action{it.assistantCoach.autoYouthTraining=v}})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Wechsel im Match übernehmen");Text("Fitness, Karten, Rating, Spielstand und Bankqualität bestimmen die Wechsel.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoSubstitutions,{v->vm.action{it.assistantCoach.autoSubstitutions=v}})}
   Pick("Trainingsphilosophie",assistant.trainingStyle,AssistantTrainingStyle.entries.toList(),{it.label}){v->vm.action{it.assistantCoach.trainingStyle=v}};Pick("Co-Trainer-Profil",assistant.profile,AssistantCoachProfile.entries.toList(),{it.label}){v->vm.action{it.assistantCoach.profile=v}};Text(assistant.profile.description,color=Muted,style=MaterialTheme.typography.bodySmall)
   StepSlider("Wechsel-Aggressivität",assistant.substitutionAggression,onChange={v->vm.action{it.assistantCoach.substitutionAggression=v}})
   StepSlider("Jugend-Risikobereitschaft",assistant.youthAggression,onChange={v->vm.action{it.assistantCoach.youthAggression=v}})
   if(assistant.autoSeniorTraining){val preview=AssistantCoachSystem.recommendedPlan(w);Text("Nächster Co-Trainer-Plan",style=MaterialTheme.typography.titleMedium);Text(preview.days.joinToString(" · "){it.label},color=Grass);Text(assistant.lastPlanReason.ifBlank{"Der Plan wird vor der nächsten Trainingswoche anhand von Belastung und Gegner finalisiert."},color=Muted)}
   if(assistant.lastYouthReason.isNotBlank())Text(assistant.lastYouthReason,color=Muted)
   if(assistant.lastSubReason.isNotBlank())Text("Letzter automatischer Wechsel: ${assistant.lastSubReason}",color=Muted)
  }
  Section("Dein Wochenplan"){
   if(assistant.autoSeniorTraining)Text("Automatik aktiv: manuelle Änderungen bleiben sichtbar, werden vor der nächsten Trainingsauswertung aber vom Co-Trainer sinnvoll neu geplant.",color=Gold)
   Text(if(amateur)"Nur die ersten drei echten Einheiten wirken voll. Regeneration und freie Tage helfen zusätzlich bei der Erholung." else "Im Profibereich wirken alle sieben Tage. Achte auf Belastung und Erholung.",color=Muted)
   Pick("Vorlage laden","Individuell",listOf("Individuell","Matchwoche","Aufbau","Schonung","Pressing","Mental")){if(it!="Individuell")vm.action{w2->TrainingEngine.preset(w2,it)}}
   listOf("Montag","Dienstag","Mittwoch","Donnerstag","Freitag","Samstag","Sonntag").forEachIndexed{i,day->val unit=w.training.days[i];Pick(day,unit,UnitType.entries.toList(),{it.label}){u->vm.action{it.training.days[i]=u}};Text(when{unit==UnitType.OFF->"Erholung · frei";unit==UnitType.RECOVERY->"Erholung · aktive Regeneration";i in effective->"Wirksame Einheit ${effective.indexOf(i)+1} · Belastung ${unit.load}";else->"Zählt nicht voll: Arbeit und Familie gehen vor."},color=if(i in effective)Grass else Muted,style=MaterialTheme.typography.bodySmall)}
  }
  Section("Belastung & Matchvorbereitung"){
   Pick("Intensität",w.training.intensity,(1..5).toList(),{when(it){1->"Sehr leicht";2->"Leicht";3->"Normal";4->"Hart";else->"Sehr hart"}}){v->vm.action{it.training.intensity=v}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Gegnerspezifische Vorbereitung");Text("Video/Taktik baut konkrete Gegnerkenntnis auf.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(w.training.opponentPrep,{v->vm.action{it.training.opponentPrep=v}})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Geheimtraining");Text("Vor Derby/Pokal: mehr Fokus, aber zusätzliche Belastung.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(w.training.secretSession,{v->vm.action{it.training.secretSession=v;it.club().dynamics.opponentPrep=(it.club().dynamics.opponentPrep+if(v)4 else -2).coerceIn(0,100);it.club().dynamics.fatigueLoad=(it.club().dynamics.fatigueLoad+if(v)3 else 0).coerceIn(0,100)}})}
  }
  Section("Intensivtraining"){
   Text("Vier Wochen gezielte Förderung. Sie beschleunigt einen Schwerpunkt spürbar, kostet Geld und erhöht Belastungs- sowie Verletzungsrisiko. Kein Sofort-Rating-Sprung.",color=Muted)
   val candidates=w.squad().filter{!it.retired}.sortedBy{it.name};if(candidates.isNotEmpty()){val selected=intensivePlayerId.takeIf{id->candidates.any{it.id==id}}?:candidates.first().id;val p=w.players.getValue(selected);Pick("Spieler",selected,candidates.map{it.id},{w.players.getValue(it).name}){intensivePlayerId=it};Pick("Schwerpunkt",intensiveFocus,Focus.entries.toList(),{it.label}){intensiveFocus=it};val reason=IntensiveTrainingSystem.reason(w,selected);Action("Intensivprogramm starten · ${euros(IntensiveTrainingSystem.cost(w,p))}",reason==null){intensiveConfirm=selected};val potentialReason=IntensiveTrainingSystem.potentialReason(w,selected);Action("Potenzialtraining starten · ${euros(IntensiveTrainingSystem.potentialCost(w,p))}",potentialReason==null,secondary=true){vm.action("Potenzialtraining gestartet."){IntensiveTrainingSystem.startPotential(it,selected,intensiveFocus)}};if(reason!=null)Text(reason,color=Muted,style=MaterialTheme.typography.bodySmall);if(potentialReason!=null)Text("Potenzialtraining: $potentialReason",color=Muted,style=MaterialTheme.typography.bodySmall)}
   if(w.intensiveTraining.isNotEmpty()){HorizontalDivider();w.intensiveTraining.forEach{pr->val p=w.players[pr.playerId];Text("${p?.name?:"Spieler"} · ${pr.focus.label}",style=MaterialTheme.typography.titleMedium);Text("Noch ${pr.weeksLeft} Wochen · Fortschritt ${(pr.progress*100).toInt()} % bis zum nächsten gezielten Attributpunkt",color=Grass)}}
  }
  Section("Was wirklich hängen bleibt"){
   val d=w.club().dynamics;Metric("Teamchemie","${d.chemistry} / 100");Metric("Taktisches Verständnis","${d.tacticalUnderstanding} / 100");Metric("Pressing-Abstimmung","${d.pressingCoordination} / 100");Metric("Mentale Härte","${d.mentalHardness} / 100");Metric("Führung / Hierarchie","${d.leadership} / ${d.hierarchyStability}");Metric("Aktuelle Belastung","${d.fatigueLoad} / 100");Metric("Gegnervorbereitung","${d.opponentPrep} / 100")
   Text("Automatismen",style=MaterialTheme.typography.titleMedium);d.patterns.entries.sortedBy{it.key}.forEach{(k,v)->Meter(k.replace("FLUEGEL","Flügel").replace("HALBRAUM","Halbraum").replace("DIAGONALE","Lange Diagonale").replace("AUFBAU","Aufbau").replace("PRESSINGFALLE","Pressingfalle").replace("RESTVERTEIDIGUNG","Restverteidigung").replace("STANDARDS","Standards"),v)}
  }
  Section("Extra-Foki · ${w.training.extra.size} / ${if(assistant.autoSeniorTraining||assistant.autoYouthTraining)6 else if(amateur)2 else 4}"){
   w.training.extra.forEach{extra->Text("${w.players[extra.playerId]?.name} · ${extra.focus.label}");Action("Fokus entfernen",secondary=true){vm.action{it.training.extra.remove(extra)}}}
   val players=w.squad().filter{!it.youth||w.club().stadium.youth>=28};if(players.isNotEmpty()){val selected=playerId.takeIf{id->players.any{it.id==id}}?:players.first().id;Pick("Spieler",selected,players.map{it.id},{w.players.getValue(it).name}){playerId=it};Pick("Extra-Fokus",focus,Focus.entries.toList(),{it.label}){focus=it};val cap=if(assistant.autoSeniorTraining||assistant.autoYouthTraining)6 else if(amateur)2 else 4;Action("Extra-Fokus hinzufügen",w.training.extra.size<cap){val chosen=IndividualFocus(selected,focus);vm.action{require(it.training.extra.size<cap){"Alle Extra-Foki sind belegt."};require(chosen !in it.training.extra){"Dieser Fokus ist schon eingetragen."};it.training.extra.add(chosen)}}}
  }
  Section("Lionel-Messi-Masterclass"){
   val candidates=w.squad().filter{!it.retired}.sortedBy{it.name};if(candidates.isEmpty())Text("Kein Spieler verfügbar.",color=Muted) else {val selected=masterclassPlayerId.takeIf{id->candidates.any{it.id==id}}?:candidates.first().id;val p=w.players.getValue(selected);Pick("Spieler",selected,candidates.map{it.id},{w.players.getValue(it).name}){masterclassPlayerId=it};Text("Exklusive Privatsession · ${euros(TrainingEngine.MESSI_MASTERCLASS_COST)}",color=Gold,style=MaterialTheme.typography.titleMedium);if(p.messiMentored)Text("${p.name}: Masterclass abgeschlossen · ${p.archetype}",color=Grass);val reason=TrainingEngine.messiMasterclassReason(w,selected);Action("Masterclass buchen · ${euros(TrainingEngine.MESSI_MASTERCLASS_COST)}",reason==null){masterclassConfirm=selected};if(reason!=null&&!p.messiMentored)Text(reason,color=Muted,style=MaterialTheme.typography.bodySmall)}
  }
  val r=w.training.lastReport;Section("Letzter Trainingsbericht"){if(w.calendar.absoluteWeek==0)Text("Der erste Bericht kommt nach deinem ersten Spieltag.",color=Muted)else{Metric("Fitness nach dem Training","${r.averageFitness} / 100");r.effects.forEach{Text(it,color=Gold)};if(r.gains.isEmpty())Text("Fortschritte gesammelt; noch kein voller Attributpunkt.")else r.gains.forEach{Text(it,color=Grass)};if(r.injuries.isEmpty())Text("Keine Trainingsverletzung.",color=Grass)else r.injuries.forEach{Text(it,color=Clay)}}}
 }
 intensiveConfirm?.let{id->val p=w.players[id];Confirm("Intensivtraining für ${p?.name?:"Spieler"}?","Vier Wochen ${intensiveFocus.label}. ${p?.let{euros(IntensiveTrainingSystem.cost(w,it))}?:""} werden sofort bezahlt. Belastung und Verletzungsrisiko steigen.",{intensiveConfirm=null}){intensiveConfirm=null;vm.action("Intensivtraining gestartet."){IntensiveTrainingSystem.start(it,id,intensiveFocus)}}}
 masterclassConfirm?.let{id->val p=w.players[id];Confirm("Messi-Masterclass für ${p?.name?:"Spieler"}?","${euros(TrainingEngine.MESSI_MASTERCLASS_COST)} werden sofort aus der Vereinskasse bezahlt.",{masterclassConfirm=null}){masterclassConfirm=null;vm.action("Messi-Masterclass abgeschlossen."){TrainingEngine.bookMessiMasterclass(it,id)}}}
}

fun facilityEffect(f: Facility)=when(f){Facility.FLOODLIGHTS->"18 % mehr Zuschauernachfrage. Besuch bleibt durch Kapazität und Wetter begrenzt.";Facility.ARTIFICIAL->"Ersetzt Hartplatz durch Kunstrasen. Platzqualität steigt auf 85.";Facility.PITCH->"Platzqualität +15. Bessere Kurzpässe und weniger Verletzungsrisiko.";Facility.TRAINING->"Trainingsqualität +15. Attributentwicklung wird schneller.";Facility.GYM->"Kraftraum +15. Erhöht die Trainingswirkung.";Facility.MEDICINE->"Medizin +15. Weniger Verletzungen, bessere Erholung; ab 60 schnellere Heilung.";Facility.CABIN->"Kabine +15. Mehr Fitnessregeneration nach jedem Spieltag.";Facility.STAND->"Tribüne +15 und bis zu 100 zusätzliche Sitzplätze. Mehr Zuschauernachfrage.";Facility.CAPACITY->"Mehr Plätze für zahlende Zuschauer. Erweiterung skaliert mit der Liga.";Facility.CLUBHOUSE->"Vereinsheim +15, zusätzlich 15 Mitglieder und +5 Kader-Moral.";Facility.YOUTH->"Jugendzentrum +15. Stärkere Nachwuchsspieler; ab 28 Training mit den Herren."}
@Composable fun ClubScreen(w: World,vm: GameViewModel,onSaves: ()->Unit){val c=w.club();val s=c.stadium;var confirm by remember{mutableStateOf<Facility?>(null)}
 Page(c.name,"VEREIN · WIRTSCHAFT · PARTNER"){
  Section{Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){Crest(c.logo,c.primary,c.secondary,Modifier.size(80.dp));Column{Text(if(w.privateTopClubMode&&w.leagues.size==5)"${c.city} · ${WorldFactory.leagueName(w,c.tier)}" else "${c.city} · gegründet ${c.founded}",color=Muted);Text("${c.members} Mitglieder",style=MaterialTheme.typography.titleLarge);Text(c.philosophy,color=Grass)}};Text("${c.playPhilosophy} · ${c.youthPhilosophy}",color=Muted);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Shirt(c.kits.home,Modifier.size(76.dp));Shirt(c.kits.away,Modifier.size(76.dp));Shirt(c.kits.keeper,Modifier.size(76.dp))};Metric("Vereinskasse",euros(c.budget));Text("Letzte Woche: ${euros(c.lastIncome.toLong())} Zuflüsse · ${euros(c.lastCosts.toLong())} Kosten",color=Muted);Text("Gehaltssumme: ${euros(w.squad().sumOf{it.wage}.toLong())} / Woche.",color=Muted);Action("Speichern",secondary=true){vm.saveAs(vm.state.value.slot)};Action("Speichern unter / Export",secondary=true,onClick=onSaves)}
  Section("Wirtschaft & Partner"){
   Metric("Kommerzielle Reputation","${c.commercialReputation} / 100");Metric("Finanzvertrauen","${c.financialTrust} / 100")
   val active=c.sponsorDeals.filter{it.active};Text("Aktive Partner · ${active.size} / 4",style=MaterialTheme.typography.titleMedium);active.forEach{d->Text("${d.category.label}: ${d.name}",style=MaterialTheme.typography.titleMedium);Text("${euros(d.weekly.toLong())}/Woche · Siegbonus ${euros(d.performanceBonus.toLong())} · noch ${d.weeksLeft} Wochen",color=Muted);if(d.youthBoost>0)Text("Jugendnetzwerk +${d.youthBoost} beim Abschluss",color=Grass);if(d.membersBoost>0)Text("Regionalwirkung: +${d.membersBoost} Mitglieder beim Abschluss",color=Grass);HorizontalDivider()}
   if(c.sponsorOffers.isEmpty())Text("Aktuell liegen keine zusätzlichen Partnerangebote vor.",color=Muted) else {Text("Offene Angebote",style=MaterialTheme.typography.titleMedium);c.sponsorOffers.forEach{d->Text("${d.category.label}: ${d.name}",style=MaterialTheme.typography.titleMedium);Text("${euros(d.weekly.toLong())}/Woche · Handgeld ${euros(d.signingBonus)} · ${d.weeksLeft} Wochen · Mindest-Reputation ${d.minReputation}",color=Muted);Action("Partnerschaft annehmen",c.reputation>=d.minReputation&&active.size<4&&active.none{it.category==d.category}){vm.action("Partnerschaft abgeschlossen."){EconomySystem.accept(it,it.user.clubId,d.id)}};HorizontalDivider()}}
   val canRefresh=w.calendar.absoluteWeek-c.lastCommercialRefreshWeek>=8;Action(if(canRefresh)"Neue Sponsorangebote prüfen" else "Neue Angebote in ${(8-(w.calendar.absoluteWeek-c.lastCommercialRefreshWeek)).coerceAtLeast(0)} Wochen",enabled=canRefresh,secondary=true){vm.action{EconomySystem.refreshOffers(it,it.club())}}
   Text("Einnahmen bestehen nicht mehr nur aus dem Hauptsponsor: Mitglieder, Merchandising, Medienwert, Siegboni und mehrere Partner fließen ein. Trikotpartner stärken Merchandising, Hauptpartner den Medienwert; Finanzvertrauen beeinflusst neue Angebote. Gleichzeitig kosten Stadion, Staff und Academy laufend Geld.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section(s.name){Text("${s.surface.label} · ${s.capacity} Plätze · ${s.seats} Sitzplätze");Text(if(s.floodlights)"Flutlicht vorhanden" else "Noch ohne Flutlicht",color=if(s.floodlights)Grass else Muted);Meter("Platzqualität",s.pitchQuality)}
  if(w.construction.isNotEmpty())Section("Laufende Arbeiten"){w.construction.forEach{p->Text(p.facility.label,style=MaterialTheme.typography.titleMedium);LinearProgressIndicator(progress={1f-p.weeksLeft.toFloat()/p.totalWeeks},modifier=Modifier.fillMaxWidth());Text("Noch ${p.weeksLeft} von ${p.totalWeeks} Wochen",color=Muted)}}
  Text("${if(c.tier>=7)1 else 2} gleichzeitige Baustelle(n)",color=Muted)
  Facility.entries.forEach{f->Section(f.label){if(f !in listOf(Facility.FLOODLIGHTS,Facility.ARTIFICIAL,Facility.CAPACITY))Meter("Ausbaustand",ConstructionEngine.level(s,f));Text(facilityEffect(f));Text("${euros(ConstructionEngine.price(w,f))} · ${ConstructionEngine.weeks(w,f)} Wochen",color=Grass);val reason=ConstructionEngine.reason(w,f);Action("Ausbau beauftragen",reason==null&&w.live==null){confirm=f};if(reason!=null)Text(reason,color=Muted,style=MaterialTheme.typography.bodySmall)}}
 }
 confirm?.let{f->Confirm("${f.label} bauen?","${euros(ConstructionEngine.price(w,f))} werden sofort bezahlt. Bauzeit: ${ConstructionEngine.weeks(w,f)} Wochen.",{confirm=null}){confirm=null;vm.action{ConstructionEngine.start(it,f)}}}
}

@Composable fun TransfersScreen(w: World,vm: GameViewModel,onProfile: (Int)->Unit){
 var area by rememberSaveable{mutableStateOf("Jugend")}
 var filter by rememberSaveable{mutableStateOf("Alle")}
 var dealType by rememberSaveable{mutableStateOf(DealType.BUY)}
 var role by rememberSaveable{mutableStateOf(SquadRole.ROTATION)}
 var marketView by rememberSaveable{mutableStateOf("Spielersuche")}
 var nameQuery by rememberSaveable{mutableStateOf("")}
 var clubFilter by rememberSaveable{mutableIntStateOf(0)}
 var outboundPlayerId by rememberSaveable{mutableIntStateOf(w.squad().firstOrNull{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}?.id?:0)}
 var outboundType by rememberSaveable{mutableStateOf(DealType.BUY)}
 var youthFirst by rememberSaveable{mutableStateOf("")}
 var youthLast by rememberSaveable{mutableStateOf("")}
 var youthNation by rememberSaveable{mutableStateOf("Deutschland")}
 var youthAge by rememberSaveable{mutableIntStateOf(16)}
 var youthPos by rememberSaveable{mutableStateOf(Position.ZM)}
 var youthFoot by rememberSaveable{mutableStateOf(Foot.RIGHT)}
 var youthBlueprint by rememberSaveable{mutableStateOf(YouthBlueprint.BALANCED)}
 var youthRole by rememberSaveable{mutableStateOf(PlayerRole.BOX_TO_BOX)}

 val c=w.club()
 val academy=c.academy
 fun seasonLine(label:String,s:AcademyTeamSeason):String{
  val last=s.lastResult.takeIf{it.isNotBlank()}?.let{" · zuletzt $it"}?:""
  return "$label · ${s.played} Sp · ${s.wins}S ${s.draws}U ${s.losses}N · ${s.points} P · ${s.goalsFor}:${s.goalsAgainst}$last"
 }

 Page("Kaderplanung & Academy","SUCHE · SCOUTING · U19/U23 · VERTRÄGE"){
  Section("Bereich"){
   Pick("Ansicht",area,listOf("Transfers","Jugend"),{it}){area=it}
   Text(if(area=="Jugend")"U19 und U23 sind eigenständige Nachwuchsmannschaften mit getrennten Kadern, Spielzeiten, Statistiken und Entwicklungswegen." else "Merkliste, Scoutberichte, Konkurrenzangebote, Vorverträge, Leihen und Historie an einem Ort.",color=Muted)
  }

  if(area=="Jugend"){
   Section("Academy"){
    Pick("Academy-Identität",academy.identity,AcademyIdentity.entries.toList(),{it.label}){v->vm.action{it.club().academy.identity=v}}
    Metric("Scouting","${academy.scouting} / 100")
    Metric("Internat","${academy.boarding} / 100")
    Metric("Partnervereine","${academy.partnerNetwork} / 100")
    Metric("U19 / U23","${academy.u19Quality} / ${academy.u23Quality}")
    Text(if(academy.goldenGeneration)"Goldene Generation: außergewöhnliche Tiefe im Jahrgang." else "Jahrgangszyklus ${academy.generationCycle}/100 · Dürrephase ${academy.droughtYears} Saison(en).",color=if(academy.goldenGeneration)Gold else Muted)
   }

   Section("U19 & U23 Spielbetrieb"){
    Text(seasonLine("U19",academy.u19Season),color=Grass)
    Text(seasonLine("U23",academy.u23Season),color=Gold)
    Text("Beide Mannschaften haben einen eigenen Kader und eine eigene Saison. Alle zwei Vereinswochen wird je ein Nachwuchsspiel simuliert; Einsätze, Tore, Vorlagen und Bewertungen wirken auf die Entwicklung.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Text("Jugendspieler können dauerhaft hochgezogen oder nur für das nächste Profispiel nominiert werden. Eine Notfall-Nominierung kehrt nach diesem Spiel automatisch zurück.",color=Grass,style=MaterialTheme.typography.bodySmall)
   }

   Section("Junge Profis in den Nachwuchs schicken"){
    val eligible=w.squad().filter{p->
     val age=w.calendar.season-p.birthYear
     !p.retired&&!p.youth&&!p.temporarySeniorCallUp&&p.id!=w.user.playerId&&p.loanParentClubId==0&&age<=22
    }.sortedByDescending{it.ca}
    if(eligible.isEmpty())Text("Aktuell gibt es keinen U23-berechtigten Profispieler.",color=Muted)
    eligible.forEach{p->
     PlayerRow(p,w,onProfile)
     Text("Als Nachwuchsspieler erhält er wieder U19/U23-Spielpraxis und den zusätzlichen Academy-Entwicklungsweg.",color=Muted,style=MaterialTheme.typography.bodySmall)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      if(w.calendar.season-p.birthYear<=19)TextButton(onClick={vm.action{YouthCompetitionSystem.assign(it,p.id,YouthSquad.U19)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("In U19")}
      TextButton(onClick={vm.action{YouthCompetitionSystem.assign(it,p.id,YouthSquad.U23)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("In U23")}
     }
     HorizontalDivider()
    }
   }

   Section("Gezielte Talentsuche"){
    Text("Name, Nationalität, Alter, Position und Entwicklungsrichtung dürfen vorgegeben werden. Stärke und Potenzial bleiben academyabhängig und unsicher.",color=Muted)
    Text("Verfügbare Suchen: ${CustomYouthSystem.remainingSlots(w)} / 2",color=Grass)
    Field("Vorname",youthFirst){youthFirst=it.take(30)}
    Field("Nachname",youthLast){youthLast=it.take(30)}
    Field("Nationalität",youthNation){youthNation=it.take(30)}
    Pick("Alter",youthAge,listOf(15,16,17),{"$it Jahre"}){youthAge=it}
    Pick("Position",youthPos,Position.entries.toList(),{it.label}){youthPos=it}
    Pick("Fuß",youthFoot,Foot.entries.toList(),{it.label}){youthFoot=it}
    Pick("Grundprofil",youthBlueprint,YouthBlueprint.entries.toList(),{it.label}){youthBlueprint=it}
    Pick("Zielrolle / Kompatibilität",youthRole,PlayerRole.entries.filter{it!=PlayerRole.AUTO},{it.label}){youthRole=it}
    Metric("Voraussichtliche System-Kompatibilität","${CustomYouthSystem.compatibility(w,youthRole)} / 100")
    Action("Jugendspieler aufnehmen",CustomYouthSystem.remainingSlots(w)>0&&youthFirst.trim().length>=2&&youthLast.trim().length>=2&&w.live==null){
     vm.action("Jugendspieler wurde in die Academy aufgenommen."){CustomYouthSystem.create(it,youthFirst,youthLast,youthNation,youthAge,youthPos,youthFoot,youthBlueprint,youthRole)}
    }
    Action("Jugend-Transfermarkt öffnen",secondary=true){area="Transfers";marketView="Jugend-Transfermarkt"}
    Text("Dort kannst du reale U19/U23-Talente anderer Vereine kaufen oder leihen und vor Abschluss direkt U19, U23 oder Profikader als Ziel festlegen.",color=Muted,style=MaterialTheme.typography.bodySmall)
   }

   YouthSquad.entries.forEach{squad->
    val season=if(squad==YouthSquad.U19)academy.u19Season else academy.u23Season
    Section("${squad.label}-Kader · ${season.points} Punkte"){
     val players=w.squad().filter{it.youth&&it.youthSquad==squad&&!it.retired}.sortedByDescending{it.ca}
     if(players.isEmpty())Text("Aktuell kein Spieler in diesem Nachwuchsteam.",color=Muted)
     players.forEach{p->
      val y=p.youthProfile
      PlayerRow(p,w,onProfile)
      val ys=p.youthTeamStats
      Text("${ys.appearances} Sp · ${ys.goals} Tore · ${ys.assists} Vorlagen · Ø ${String.format(java.util.Locale.GERMANY,"%.1f",ys.averageRating)}",color=Gold)
      Text("${y.path.label} · Lernen ${y.learning} · Reife ${y.maturity} · Bereitschaft ${YouthEngine.readiness(w,p)}/100",color=Muted)
      Text("Entwicklungsfortschritt ${(p.trainingProgress*100).toInt().coerceIn(0,99)}% · Co-Trainer ${if(w.assistantCoach.autoYouthTraining)if(y.seniorTraining)"Proftraining + Academy" else "Academy-Plan" else "Automatik aus"}",color=if(w.assistantCoach.autoYouthTraining)Grass else Muted,style=MaterialTheme.typography.bodySmall)
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
       if(squad==YouthSquad.U19)TextButton(onClick={vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U23)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("Zur U23")}
       else if(w.calendar.season-p.birthYear<=19)TextButton(onClick={vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U19)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("Zur U19")}
       TextButton(onClick={vm.action{YouthCompetitionSystem.callUp(it,p.id)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("1 Spiel")}
       TextButton(onClick={vm.action{ClubActions.promote(it,p.id)}},enabled=w.live==null,modifier=Modifier.weight(1f)){Text("Dauerhaft hoch")}
      }
      HorizontalDivider()
     }
    }
   }
  } else {
   Section("Scouting-Zentrale"){
    Pick("Ansicht",marketView,listOf("Spielersuche","Jugend-Transfermarkt","Interesse an unserem Verein"),{it}){marketView=it}
    if(marketView!="Interesse an unserem Verein"){
     Field("Name suchen",nameQuery){nameQuery=it.take(40)}
     val clubIds=listOf(0)+w.clubs.values.sortedBy{it.name}.map{it.id}
     Pick("Verein",clubFilter,clubIds,{id->if(id==0)"Alle Vereine" else w.clubs[id]?.name?:"Verein"}){clubFilter=it}
     Pick("Positionsfilter",filter,listOf("Alle")+Position.entries.map{it.name},{it}){filter=it}
    }
    Pick("Deal",dealType,DealType.entries.toList(),{it.label}){dealType=it}
    Pick("Versprochene Rolle",role,SquadRole.entries.toList(),{it.label}){role=it}
   }

   Section("Eigene Spieler anbieten"){
    val own=w.squad().filter{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}.sortedByDescending{it.ca}
    if(own.isEmpty())Text("Aktuell ist kein eigener Spieler für Verkauf oder Leihe verfügbar.",color=Muted)
    else{
     val safeId=outboundPlayerId.takeIf{id->own.any{it.id==id}}?:own.first().id
     if(safeId!=outboundPlayerId)outboundPlayerId=safeId
     Pick("Spieler",safeId,own.map{it.id},{id->w.players[id]?.let{"${it.name} · ${it.position.label} · Stärke ${it.ca}"}?:"Spieler"}){outboundPlayerId=it}
     Pick("Angebotsart",outboundType,listOf(DealType.BUY,DealType.LOAN,DealType.LOAN_OPTION),{it.label}){outboundType=it}
     Text("Der Spieler wird aktiv mehreren passenden Vereinen angeboten. Angebote bleiben getrennt, damit du vergleichen und mit jedem Verein einzeln verhandeln kannst.",color=Muted)
     Action("Mehrere Angebote einholen",w.live==null&&ScoutingTransferSystem.windowOpen(w),secondary=true){vm.action{OutboundTransferSystem.requestOffers(it,safeId,outboundType)}}
     if(!ScoutingTransferSystem.windowOpen(w))Text("Das Transferfenster ist aktuell geschlossen.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
   }

   val outbound=OutboundTransferSystem.activeOffers(w)
   if(outbound.isNotEmpty())Section("Angebote für eigene Spieler"){
    outbound.forEach{o->
     val p=w.players[o.playerId]?:return@forEach
     val buyer=w.clubs[o.buyerClubId]
     Text("${p.name} · ${o.type.label} · ${o.status.label}",style=MaterialTheme.typography.titleMedium)
     Text("${buyer?.name?:"Verein"} · ${euros(o.fee)}${if(o.buyOption>0)" · Kaufoption ${euros(o.buyOption)}" else ""}",color=Muted)
     Text(o.message,color=if(o.status==NegotiationStatus.AGREED)Grass else Gold)
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      if(o.status==NegotiationStatus.COUNTER)TextButton({vm.action{OutboundTransferSystem.negotiate(it,o.id,"fee")}}){Text("Nachverhandeln")}
      TextButton({vm.action{OutboundTransferSystem.accept(it,o.id)}},enabled=o.status in listOf(NegotiationStatus.COUNTER,NegotiationStatus.AGREED)){Text("Annehmen")}
      TextButton({vm.action{OutboundTransferSystem.reject(it,o.id)}}){Text("Ablehnen")}
     }
     HorizontalDivider()
    }
   }

   val candidates=when(marketView){
    "Interesse an unserem Verein"->TransferInterestSystem.candidatesForClub(w,w.user.clubId,58).take(30)
    else->{
     val youthOnly=marketView=="Jugend-Transfermarkt"
     w.players.values.asSequence().filter{p->
      !p.retired&&p.clubId!=w.user.clubId&&(if(youthOnly)p.youth||w.calendar.season-p.birthYear<=20 else !p.youth)&&
      (filter=="Alle"||p.position.name==filter)&&(clubFilter==0||p.clubId==clubFilter)&&
      (nameQuery.isBlank()||p.name.contains(nameQuery.trim(),ignoreCase=true))
     }.sortedWith(compareByDescending<Player>{TransferInterestSystem.score(w,it,w.user.clubId)}.thenByDescending{it.ca}).take(30).map{it to TransferInterestSystem.snapshot(w,it,w.user.clubId)}.toList()
    }
   }
   if(candidates.isEmpty())Section{Text("Keine Spieler passen zu den aktuellen Filtern.",color=Muted)}
   candidates.forEach{(p,interest)->
    Section(p.name){
     val seller=w.clubs[p.clubId]
     Text("${p.position.label} · ${w.calendar.season-p.birthYear} Jahre · ${seller?.name?:"vereinslos"}",color=Muted)
     Metric("Scouting: Stärke",ClubActions.scouting(w,p))
     Metric("Interesse an ${c.shortName}","${interest.score} / 100 · ${interest.level.label}")
     if(interest.reasons.isNotEmpty())Text(interest.reasons.joinToString(" · "),color=if(interest.score>=74)Grass else Muted)
     val report=ScoutingTransferSystem.report(w,p.id)
     report?.let{Text("Scoutbericht ${it.progress}% · Stärke ${it.caMin}–${it.caMax} · Potenzial ${it.potentialMin}–${it.potentialMax}",color=Grass,style=MaterialTheme.typography.bodySmall)}
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      TextButton(onClick={vm.action{world->if(!world.watchlist.remove(p.id))world.watchlist.add(p.id)}}){Text(if(p.id in w.watchlist)"Merkliste −" else "Merkliste +")}
      if(p.id !in w.scoutAssignments)TextButton(onClick={vm.action{ScoutingTransferSystem.start(it,p.id,ScoutRegion.DOMESTIC)}},enabled=w.live==null){Text("Scouten")}
     }
     val existing=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.playerId==p.id&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.maxByOrNull{it.id}
     Action(if(existing==null)"Verhandlung starten: ${dealType.label}" else "Verhandlung läuft",existing==null&&w.live==null,secondary=true){
      vm.action{TransferEngine.createOffer(it,it.user.clubId,p.id,dealType,role)}
     }
    }
   }

   val incoming=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.sortedByDescending{it.id}
   if(incoming.isNotEmpty())Section("Aktive Verhandlungen"){
    incoming.forEach{o->
     val p=w.players[o.playerId]?:return@forEach
     Text("${p.name} · ${o.type.label} · Runde ${o.round}",style=MaterialTheme.typography.titleMedium)
     Text(o.message,color=if(o.status==NegotiationStatus.AGREED)Grass else Gold)
     Text("Verein ${o.sellerScore}/100 · Spieler ${o.playerScore}/100 · Berater ${o.agentScore}/100",color=Muted)
     val age=w.calendar.season-p.birthYear
     if(age<=22){
      val targetOptions=buildList{add("Profikader");if(age<=19)add("U19");add("U23")}
      val currentTarget=o.targetYouthSquad?.label?:"Profikader"
      Pick("Zielkader nach Abschluss",currentTarget,targetOptions,{it}){target->
       vm.action{world->world.negotiations[o.id]?.targetYouthSquad=when(target){"U19"->YouthSquad.U19;"U23"->YouthSquad.U23;else->null}}
      }
     }
     if(o.type in listOf(DealType.LOAN,DealType.LOAN_OPTION)){
      Pick("Leihdauer",o.loanWeeksRequested,listOf(12,24,40),{"$it Wochen"}){weeks->vm.action{world->world.negotiations[o.id]?.loanWeeksRequested=weeks}}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
       Text("Rückrufklausel")
       Switch(checked=o.recallAllowed,onCheckedChange={allowed->vm.action{world->world.negotiations[o.id]?.recallAllowed=allowed}})
      }
      if(o.type==DealType.LOAN_OPTION)Text("Kaufoption ${euros(o.buyOption)} · Rückruf ${if(o.recallAllowed)"möglich" else "ausgeschlossen"}",color=Muted)
     }
     if(o.status==NegotiationStatus.COUNTER)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      TextButton({vm.action{TransferEngine.improve(it,o.id,"fee")}}){Text("Ablöse +")}
      TextButton({vm.action{TransferEngine.improve(it,o.id,"wage")}}){Text("Gehalt +")}
      TextButton({vm.action{TransferEngine.improve(it,o.id,"role")}}){Text("Rolle +")}
     }
     if(o.status==NegotiationStatus.AGREED)Action(if(o.registrationReady)"Transfer registrieren" else "Medizincheck & Registrierung"){vm.action{world->if(!o.registrationReady)TransferV0518System.medicalAndRegistration(world,o);if(o.registrationReady)TransferEngine.complete(world,o.id)}}
     Action("Verhandlung zurückziehen",secondary=true){vm.action{TransferV0518System.withdraw(it,o.id)}}
     HorizontalDivider()
    }
   }
  }
 }
}

@Composable fun CareerScreen(w: World){val p=w.self();Page(p.name,"DEINE SPUREN IM VEREIN"){
 Section("Saison ${w.calendar.season}"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Einsätze","${p.stats.appearances}");Metric("Tore","${p.stats.goals}");Metric("Vorlagen","${p.stats.assists}")};Text("${p.stats.minutes} Minuten · ${p.stats.yellow} gelbe Karten · ${p.stats.red} Platzverweise",color=Muted);Text("${w.calendar.season-p.birthYear} Jahre · Stärke ${p.ca} · ${p.foot.label}",color=Muted);if(p.injuryWeeks>0)Text("${p.injury}: ${p.injuryWeeks} Wochen",color=Clay)}
 Section("Karriere gesamt"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Spiele","${p.stats.appearances+p.career.sumOf{it.stats.appearances}}");Metric("Tore","${p.stats.goals+p.career.sumOf{it.stats.goals}}");Metric("Vorlagen","${p.stats.assists+p.career.sumOf{it.stats.assists}}")}}
 Section("Vereinsgeschichte"){if(w.history.isEmpty())Text("Euer erstes Kapitel läuft. Nach Saisonende stehen hier Platzierung, Aufstieg und Auszeichnungen.",color=Muted);w.history.asReversed().forEach{h->Text("${h.season} · ${h.outcome}",style=MaterialTheme.typography.titleLarge,color=if(h.outcome=="Aufstieg")Grass else Chalk);Text("${h.league}: Platz ${h.rank}, ${h.points} Punkte, ${h.goals} Tore");h.awards.forEach{Text(it,color=Grass)};HorizontalDivider()}}
 if(p.career.isNotEmpty())Section("Deine abgeschlossenen Spielzeiten"){p.career.asReversed().forEach{Text("${it.season} · ${it.clubName}");Text("${it.stats.appearances} Einsätze · ${it.stats.goals} Tore · ${it.stats.assists} Vorlagen",color=Muted)}}
}}


@Composable fun SettingsScreen(vm: GameViewModel){
 val soundsEnabled by vm.soundsEnabled.collectAsState()
 Page("Einstellungen","AUDIO & SPIELERLEBNIS"){
  Section("Match-Sounds"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text("Stadion- und Spielsounds",style=MaterialTheme.typography.titleMedium);Text(if(soundsEnabled)"Alle Match-Sounds sind aktiv." else "Alle Match-Sounds sind stummgeschaltet.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    Switch(checked=soundsEnabled,onCheckedChange={vm.setSoundsEnabled(it)})
   }
   Text("Ein einziger Schalter steuert sämtliche Pfiffe, Ballkontakte, Schüsse, Pfosten, Torjubel, Crowd-Reaktionen, Karten, Standards, Wechsel, Nachspielzeit, Elfmeter und VAR-Sounds. Es gibt bewusst keine einzelnen Sound-Schalter.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Hinweis"){Text("Die Einstellung gilt appweit und bleibt auch nach einem Neustart erhalten. Beim Ausschalten oder sobald die App in den Hintergrund wechselt, werden laufende Match-Sounds sofort beendet.",color=Muted)}
  Section("Audio-Quellen"){Text("v0.4.69 verwendet weiterhin echte Aufnahmen: Stadion/Crowd aus ‘WWS FootballAustriavs.Sweden’ (CC BY 4.0), echter Applaus aus ‘Applause.ogg’ (CC BY-SA) und echter Pfiff aus ‘Whistle.ogg’ (CC BY-SA 3.0). Ballkontakt, Keeper- und Metall-Samples bleiben reale Aufnahmen. FFmpeg wird nur für Schnitt, Pegel, Filter und Mischung verwendet – keine synthetischen Crowd-/Pfiffgeneratoren mehr.",color=Muted,style=MaterialTheme.typography.bodySmall)}
 }
}

@Composable fun EditorScreen(w: World,vm: GameViewModel){
 var tier by rememberSaveable{mutableIntStateOf(w.club().tier)}
 val league=w.leagues.first{it.tier==tier}
 var clubId by rememberSaveable{mutableIntStateOf(league.clubIds.first())}
 val safeClubId=clubId.takeIf{it in league.clubIds}?:league.clubIds.first()
 val club=w.clubs.getValue(safeClubId)
 val players=w.squad(safeClubId).filter{!it.retired}.sortedBy{it.name}
 var playerId by rememberSaveable{mutableIntStateOf(players.firstOrNull()?.id?:w.user.playerId)}
 val safePlayerId=playerId.takeIf{id->players.any{it.id==id}}?:players.firstOrNull()?.id
 var leagueText by remember(tier,league.name){mutableStateOf(league.name)}
 var clubText by remember(safeClubId,club.name){mutableStateOf(club.name)}
 var shortText by remember(safeClubId,club.shortName){mutableStateOf(club.shortName)}
 val currentPlayer=safePlayerId?.let{w.players[it]}
 var firstText by remember(safePlayerId,currentPlayer?.firstName){mutableStateOf(currentPlayer?.firstName?:"")}
 var lastText by remember(safePlayerId,currentPlayer?.lastName){mutableStateOf(currentPlayer?.lastName?:"")}
 Page("Editor","DEINE WELT · DEINE NAMEN"){
  Section("Liga umbenennen"){
   Pick("Liga",tier,(1..10).toList(),{WorldFactory.leagueName(w,it)}){tier=it;clubId=w.leagues.first{l->l.tier==it}.clubIds.first()}
   Field("Ligabezeichnung",leagueText){leagueText=it.take(40)}
   Action("Liganamen speichern",leagueText.trim().length>=3,true){vm.action("Liganame wurde geändert."){world->world.leagues.first{it.tier==tier}.name=leagueText.trim()}}
  }
  Section("Verein umbenennen"){
   Pick("Verein",safeClubId,league.clubIds,{w.clubs.getValue(it).name}){clubId=it}
   Field("Vereinsname",clubText){clubText=it.take(40)}
   Field("Kürzel · 2–4 Zeichen",shortText){shortText=it.uppercase().take(4)}
   Action("Vereinsnamen speichern",clubText.trim().length>=3&&shortText.trim().length in 2..4,true){vm.action("Vereinsname wurde geändert."){world->val c=world.clubs.getValue(safeClubId);c.name=clubText.trim();c.shortName=shortText.trim().uppercase();c.logo.letters=c.shortName.take(4)}}
  }
  Section("Spieler umbenennen"){
   if(players.isEmpty())Text("Dieser Verein hat keine aktiven Spieler.",color=Muted) else {
    val pid=safePlayerId?:players.first().id
    Pick("Spieler",pid,players.map{it.id},{w.players.getValue(it).name}){playerId=it}
    Field("Vorname",firstText){firstText=it.take(30)}
    Field("Nachname",lastText){lastText=it.take(30)}
    Action("Spielername speichern",firstText.isNotBlank()&&lastText.isNotBlank(),true){vm.action("Spielername wurde geändert."){world->val p=world.players.getValue(pid);p.firstName=firstText.trim();p.lastName=lastText.trim()}}
   }
  }
  Section("Hinweis"){
   Text("Änderungen gelten nur für diesen Speicherstand und werden automatisch gespeichert. Spielplan, Ergebnisse und Spieler-IDs bleiben unverändert.",color=Muted)
   Text(if(w.user.difficulty==Difficulty.SANDBOX)"Sandbox ist aktiv: Deine frei gesetzten Startattribute bleiben erhalten und entwickeln sich danach normal weiter." else "Schwierigkeit: ${w.user.difficulty.label}",color=if(w.user.difficulty==Difficulty.SANDBOX)Gold else Grass)
  }
 }
}


@Composable fun HelpScreen(w:World,vm:GameViewModel){Page("Hilfe & Tutorial","MANAGER-HANDBUCH"){
 Section("Tutorial"){Text(if(w.user.tutorialEnabled&&!w.user.tutorialCompleted)"Das geführte Tutorial ist in dieser Karriere aktiv." else "Das Tutorial ist aktuell beendet oder deaktiviert.",color=Muted);Action("Tutorial von vorn starten",secondary=true){vm.action("Tutorial neu gestartet."){it.user.tutorialEnabled=true;it.user.tutorialCompleted=false;it.user.tutorialStep=0}};if(w.user.tutorialEnabled&&!w.user.tutorialCompleted)Action("Tutorial beenden",secondary=true){vm.action{it.user.tutorialEnabled=false;it.user.tutorialCompleted=true}}}
 Section("Erste Schritte"){Text("Start: Aufgaben und nächstes Spiel. Kader: Formation, Rollen und Verträge. Training: Belastung und Entwicklung. Transfers: Scouting, Jugend und Verhandlungen.",color=Muted)}
 Section("Live-Spiel"){Text("Live zeigt Ball und Ticker. Taktik bündelt Formation, Regler, Sofortanweisungen und Wechsel. Statistik zeigt Matchwerte; Analyse xG, Schusskarte, Passnetz und Wirkung deiner Taktikänderungen.",color=Muted)}
 Section("xG & Analyse"){Text("xG beschreibt die Qualität von Torchancen, nicht die Zahl der Tore, die zwingend fallen müsste. Die Taktik-Wirkungsanalyse vergleicht echte Matchdaten vor und nach Änderungen.",color=Muted)}
 Section("Spielerkarriere"){Text("Dein Karrierefokus gibt deinem eigenen Spieler einen kleinen Entwicklungsimpuls. Saisonziele, Vermächtnis, Meilensteine und Vereinsrekorde machen lange Karrieren nachvollziehbarer.",color=Muted)}
 Section("v0.5.22 Update"){Text("Neue Ligen und nationale Pokale erweitern die alte Karriere. UEFA-Wettbewerbe qualifizieren sich aus den nationalen Wettbewerben; Club World Cup und die optionale Krone der Kontinente ergänzen den Spielkalender.",color=Grass)}
}}

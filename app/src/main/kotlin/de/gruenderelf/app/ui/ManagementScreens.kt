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

@Composable fun MoreScreen(newScoutReports:Int,onNavigate: (String)->Unit,onMenu: ()->Unit){Page("Hinter der Bande","VEREIN & VERANTWORTUNG"){
 val entries=listOf("verein" to "Verein, Finanzen & Partner","training" to "Training & Co-Trainer","transfers" to if(newScoutReports>0)"Transfers & Jugend · $newScoutReports neu" else "Transfers & Jugend","karriere" to "Deine Karriere","whatsnew" to "What's New","hilfe" to "Hilfe & Tutorial","editor" to "Editor","einstellungen" to "Einstellungen","speichern" to "Speicherstände")
 entries.forEach{(route,label)->Action(label,secondary=true){onNavigate(route)}}
 Section("Gründerelf · 0.5.22"){Text("Offline. Ohne Werbung. Ohne Käufe.",color=Grass);Text("What's New erscheint bei einer neuen Version einmal automatisch und kann hier jederzeit erneut geöffnet werden.",color=Muted);Text("Das Tutorial und Manager-Handbuch erklären jetzt Matchday-Bank, Live-Aufstellung, Drag & Drop nach Rot, Co-Trainer-Wechsel, Krone der Kontinente, Club World Cup, Jugend, Transfers und die aktuellen Speicher-/Stabilitätsänderungen.",color=Muted,style=MaterialTheme.typography.bodySmall)}
 Action("Speichern & zum Startbildschirm",secondary=true,onClick=onMenu)
}}

@Composable fun TrainingScreen(w: World,vm: GameViewModel){
 val effective=TrainingEngine.effectiveDays(w);val amateur=w.club().tier>=7;val assistant=w.assistantCoach
 var playerId by remember{mutableIntStateOf(w.user.playerId)};var focus by remember{mutableStateOf(Focus.FINISHING)}
 var intensivePlayerId by rememberSaveable{mutableIntStateOf(w.user.playerId)};var intensiveFocus by rememberSaveable{mutableStateOf(Focus.TECHNIQUE)};var intensiveConfirm by remember{mutableStateOf<Int?>(null)}
 var masterclassPlayerId by rememberSaveable{mutableIntStateOf(w.user.playerId)};var masterclassConfirm by remember{mutableStateOf<Int?>(null)}
 Page("Training & Co-Trainer","PLANUNG · ENTWICKLUNG · DELEGATION"){
  Section("Co-Trainer-Automatik"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Profikader automatisch trainieren");Text("Der Co-Trainer plant Woche, Intensität, Gegnerfokus und individuelle Schwerpunkte.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoSeniorTraining,{v->vm.action{it.assistantCoach.autoSeniorTraining=v}})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Jugend automatisch entwickeln");Text("Seniortraining, Mentoren und Extra-Foki werden nach Reife und Risiko dosiert.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoYouthTraining,{v->vm.action{it.assistantCoach.autoYouthTraining=v}})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Wechsel im Match übernehmen");Text("Fitness, Karten, Rating, Spielstand und Bankqualität bestimmen die Wechsel.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(assistant.autoSubstitutions,{v->vm.action{it.assistantCoach.autoSubstitutions=v}})}
   Pick("Co-Trainer-Profil",assistant.profile,AssistantCoachProfile.entries.toList(),{it.label}){v->vm.action{it.assistantCoach.profile=v}}
   Text("${assistant.profile.description} · ${AssistantCoachSystem.profileSummary(assistant)}",color=Muted,style=MaterialTheme.typography.bodySmall)
   Pick("Trainingsphilosophie",assistant.trainingStyle,AssistantTrainingStyle.entries.toList(),{it.label}){v->vm.action{it.assistantCoach.trainingStyle=v}}
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
  Section("Intensivtraining & Eliteentwicklung"){
   Text("Normales Training endet praktisch bei Rating 99. Werte bis 150 sind für jeden Spieler möglich, aber nur über lange, teure Intensiv- und Potenzialblöcke mit Elite-Einrichtungen.",color=Muted)
   val candidates=w.squad().filter{!it.retired}.sortedBy{it.name};if(candidates.isNotEmpty()){val selected=intensivePlayerId.takeIf{id->candidates.any{it.id==id}}?:candidates.first().id;val p=w.players.getValue(selected);Pick("Spieler",selected,candidates.map{it.id},{w.players.getValue(it).name}){intensivePlayerId=it};Pick("Schwerpunkt",intensiveFocus,Focus.entries.toList(),{it.label}){intensiveFocus=it};Metric("Gesamtstärke","${p.ca} / $PLAYER_RATING_MAX");Metric("Entwicklungsgrenze","${p.hidden.potential} / $PLAYER_RATING_MAX");val reason=IntensiveTrainingSystem.reason(w,selected);Action("Intensivprogramm · ${IntensiveTrainingSystem.duration(w,p)} Wochen · ${euros(IntensiveTrainingSystem.cost(w,p))}",reason==null){intensiveConfirm=selected};if(reason!=null)Text(reason,color=Muted,style=MaterialTheme.typography.bodySmall);val potentialReason=IntensiveTrainingSystem.potentialReason(w,selected);Action("Potenzialprogramm · ${IntensiveTrainingSystem.potentialDuration(w,p)} Wochen · ${euros(IntensiveTrainingSystem.potentialCost(w,p))}",potentialReason==null,true){vm.action("Potenzialtraining gestartet."){IntensiveTrainingSystem.startPotential(it,selected,intensiveFocus)}};Text(if(p.hidden.potential>=99)"Elitebereich 100–150: ein kompletter Spezialblock hebt die Entwicklungsgrenze höchstens um 1. Je höher das Niveau, desto länger und teurer wird jeder weitere Schritt." else "Bis 99 bleibt die Entwicklung schneller. Danach gelten Elite-Anforderungen an Trainingszentrum, Kraftraum, Medizin, Staff und Professionalität.",color=if(p.hidden.potential>=99)Gold else Muted,style=MaterialTheme.typography.bodySmall);if(potentialReason!=null)Text(potentialReason,color=Muted,style=MaterialTheme.typography.bodySmall)}
   if(w.intensiveTraining.isNotEmpty()){HorizontalDivider();w.intensiveTraining.forEach{pr->val p=w.players[pr.playerId];Text("${p?.name?:"Spieler"} · ${pr.focus.label}",style=MaterialTheme.typography.titleMedium);Text("Noch ${pr.weeksLeft} von ${pr.totalWeeks} Wochen · ${if(pr.raisesPotential)"Potenzialförderung" else "Attributförderung"} · Fortschritt ${(pr.progress*100).toInt()} %",color=Grass)}}
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
 intensiveConfirm?.let{id->val p=w.players[id];Confirm("Intensivtraining für ${p?.name?:"Spieler"}?","${p?.let{IntensiveTrainingSystem.duration(w,it)}?:4} Wochen ${intensiveFocus.label}. ${p?.let{euros(IntensiveTrainingSystem.cost(w,it))}?:""} werden sofort bezahlt. Im Elitebereich steigt Belastung und Verletzungsrisiko deutlich.",{intensiveConfirm=null}){intensiveConfirm=null;vm.action("Intensivtraining gestartet."){IntensiveTrainingSystem.start(it,id,intensiveFocus)}}}
 masterclassConfirm?.let{id->val p=w.players[id];Confirm("Messi-Masterclass für ${p?.name?:"Spieler"}?","${euros(TrainingEngine.MESSI_MASTERCLASS_COST)} werden sofort aus der Vereinskasse bezahlt.",{masterclassConfirm=null}){masterclassConfirm=null;vm.action("Messi-Masterclass abgeschlossen."){TrainingEngine.bookMessiMasterclass(it,id)}}}
}

fun facilityEffect(f: Facility)=when(f){Facility.FLOODLIGHTS->"18 % mehr Zuschauernachfrage. Besuch bleibt durch Kapazität und Wetter begrenzt.";Facility.ARTIFICIAL->"Ersetzt Hartplatz durch Kunstrasen. Platzqualität steigt mindestens auf 85.";Facility.PITCH->"Bessere Ballzirkulation und weniger Belastung. Ab Stufe 100 wird jeder Ausbau kleiner und deutlich teurer.";Facility.TRAINING->"Höhere Trainingswirkung. Stufe 105 ist Voraussetzung für Eliteentwicklung über Rating 99.";Facility.GYM->"Steigert physische Trainingswirkung. Stufe 105 ist Voraussetzung für Eliteentwicklung über Rating 99.";Facility.MEDICINE->"Weniger Verletzungen und bessere Regeneration. Stufe 105 ist Voraussetzung für Eliteentwicklung über Rating 99.";Facility.CABIN->"Mehr Fitnessregeneration und professionellere Spieltagsabläufe.";Facility.STAND->"Bessere Stadioninfrastruktur und zusätzliche Sitzplatzqualität.";Facility.CAPACITY->"Mehr Plätze für zahlende Zuschauer. Erweiterung skaliert mit der Liga.";Facility.CLUBHOUSE->"Stärkt Moral, Mitgliederbindung und professionelle Vereinsstrukturen.";Facility.YOUTH->"Verbessert Nachwuchsentwicklung. Eliteausbau über 100 wird zunehmend teuer und langsam."}
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
  Section(s.name){Text("${s.surface.label} · ${s.capacity} Plätze · ${s.seats} Sitzplätze");Text(if(s.floodlights)"Flutlicht vorhanden" else "Noch ohne Flutlicht",color=if(s.floodlights)Grass else Muted);Meter("Platzqualität",s.pitchQuality,FACILITY_LEVEL_MAX)}
  if(w.construction.isNotEmpty())Section("Laufende Arbeiten"){w.construction.forEach{p->Text(p.facility.label,style=MaterialTheme.typography.titleMedium);LinearProgressIndicator(progress={1f-p.weeksLeft.toFloat()/p.totalWeeks},modifier=Modifier.fillMaxWidth());Text("Noch ${p.weeksLeft} von ${p.totalWeeks} Wochen",color=Muted)}}
  Text("${if(!w.privateTopClubMode&&c.tier>=7)1 else if(w.developer.enabled)3 else 2} gleichzeitige Baustelle(n)",color=Muted)
  Facility.entries.forEach{f->Section(f.label){if(f !in listOf(Facility.FLOODLIGHTS,Facility.ARTIFICIAL,Facility.CAPACITY))Meter("Ausbaustand",ConstructionEngine.level(s,f),FACILITY_LEVEL_MAX);Text(facilityEffect(f));Text("${euros(ConstructionEngine.price(w,f))} · ${ConstructionEngine.weeks(w,f)} Wochen",color=Grass);val reason=ConstructionEngine.reason(w,f);Action("Ausbau beauftragen",reason==null&&w.live==null){confirm=f};if(reason!=null)Text(reason,color=Muted,style=MaterialTheme.typography.bodySmall)}}
 }
 confirm?.let{f->Confirm("${f.label} bauen?","${euros(ConstructionEngine.price(w,f))} werden sofort bezahlt. Bauzeit: ${ConstructionEngine.weeks(w,f)} Wochen.",{confirm=null}){confirm=null;vm.action{ConstructionEngine.start(it,f)}}}
}

@Composable fun TransfersScreen(w: World,vm: GameViewModel,onProfile: (Int)->Unit){
 var filter by rememberSaveable{mutableStateOf("Alle")};var dealType by rememberSaveable{mutableStateOf(DealType.BUY)};var role by rememberSaveable{mutableStateOf(SquadRole.ROTATION)}
 var negotiationPlayerId by rememberSaveable{mutableStateOf<Int?>(null)};var marketView by rememberSaveable{mutableStateOf("Spielersuche")};var planningView by rememberSaveable{mutableStateOf("Transfers")};var nameQuery by rememberSaveable{mutableStateOf("")};var clubFilter by rememberSaveable{mutableIntStateOf(0)};var scoutRegion by rememberSaveable{mutableStateOf(ScoutRegion.DOMESTIC)}
 var outboundPlayerId by rememberSaveable{mutableIntStateOf(w.squad().firstOrNull{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}?.id?:w.user.playerId)};var outboundDeal by rememberSaveable{mutableStateOf(DealType.BUY)}
 var youthFirst by rememberSaveable{mutableStateOf("")};var youthLast by rememberSaveable{mutableStateOf("")};var youthNation by rememberSaveable{mutableStateOf("Deutschland")};var youthAge by rememberSaveable{mutableIntStateOf(16)};var youthPos by rememberSaveable{mutableStateOf(Position.ZM)};var youthFoot by rememberSaveable{mutableStateOf(Foot.RIGHT)};var youthBlueprint by rememberSaveable{mutableStateOf(YouthBlueprint.BALANCED)};var youthRole by rememberSaveable{mutableStateOf(PlayerRole.BOX_TO_BOX)}
 Page("Kaderplanung & Academy","SUCHE · SCOUTING · U19/U23 · VERTRÄGE"){
  val c=w.club();val academy=c.academy
  Section("Bereich"){Pick("Ansicht",planningView,listOf("Transfers","Jugend")){planningView=it};Text(if(planningView=="Jugend")"U19 und U23 sind eigenständige Nachwuchsmannschaften mit getrennten Kadern, Spielzeiten, Statistiken und Entwicklungswegen." else "Merkliste, Scoutberichte, Konkurrenzangebote, Vorverträge, Leihen und Historie an einem Ort.",color=Muted,style=MaterialTheme.typography.bodySmall)}
  if(planningView=="Jugend"){
   Section("Academy"){
    Pick("Academy-Identität",academy.identity,AcademyIdentity.entries.toList(),{it.label}){v->vm.action{it.club().academy.identity=v}}
    Metric("Scouting","${academy.scouting} / 100");Metric("Internat","${academy.boarding} / 100");Metric("Partnervereine","${academy.partnerNetwork} / 100");Metric("U19 / U23","${academy.u19Quality} / ${academy.u23Quality}")
    Text(if(academy.goldenGeneration)"Goldene Generation: außergewöhnliche Tiefe im Jahrgang." else "Jahrgangszyklus ${academy.generationCycle}/100 · Dürrephase ${academy.droughtYears} Saison(en).",color=if(academy.goldenGeneration)Gold else Muted)
   }
   Section("U19 & U23 Spielbetrieb"){
    fun summary(label:String,x:AcademyTeamSeason)="$label · ${x.played} Sp · ${x.wins}S ${x.draws}U ${x.losses}N · ${x.points} P · ${x.goalsFor}:${x.goalsAgainst}${x.lastResult.takeIf{it.isNotBlank()}?.let{" · zuletzt $it"}?:""}"
    Text(summary("U19",academy.u19Season),color=Grass);Text(summary("U23",academy.u23Season),color=Gold)
    Text("Beide Mannschaften haben einen eigenen Kader und eine eigene Saison. Alle zwei Vereinswochen wird je ein Nachwuchsspiel simuliert; Einsätze, Tore, Vorlagen und Bewertungen wirken auf die Entwicklung.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Text("Jugendspieler können dauerhaft hochgezogen oder nur für das nächste Profispiel nominiert werden. Eine Notfall-Nominierung kehrt nach diesem Spiel automatisch zurück.",color=Grass,style=MaterialTheme.typography.bodySmall)
   }
   Section("Junge Profis in den Nachwuchs schicken"){
    val prospects=w.squad().filter{!it.youth&&!it.retired&&!it.temporarySeniorCallUp&&it.id!=w.user.playerId&&it.loanParentClubId==0&&w.calendar.season-it.birthYear<=22}.sortedBy{w.calendar.season-it.birthYear}
    if(prospects.isEmpty())Text("Aktuell gibt es keinen U23-berechtigten Profispieler.",color=Muted)
    prospects.forEach{p->
     PlayerRow(p,w,onProfile);Text("Als Nachwuchsspieler erhält er wieder U19/U23-Spielpraxis und den zusätzlichen Academy-Entwicklungsweg.",color=Muted,style=MaterialTheme.typography.bodySmall)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      if(w.calendar.season-p.birthYear<=19)TextButton({vm.action{YouthCompetitionSystem.assignToYouth(it,p.id,YouthSquad.U19)}},Modifier.weight(1f),enabled=w.live==null){Text("In U19")}
      TextButton({vm.action{YouthCompetitionSystem.assignToYouth(it,p.id,YouthSquad.U23)}},Modifier.weight(1f),enabled=w.live==null){Text("In U23")}
     }
     HorizontalDivider()
    }
   }
   Section("Gezielte Talentsuche"){
    Text("Name, Nationalität, Alter, Position und Entwicklungsrichtung dürfen vorgegeben werden. Stärke und Potenzial bleiben academyabhängig und unsicher.",color=Muted)
    Text("Verfügbare Suchen: ${CustomYouthSystem.remainingSlots(w)} / 2",color=Grass)
    Field("Vorname",youthFirst){youthFirst=it.take(30)};Field("Nachname",youthLast){youthLast=it.take(30)};Field("Nationalität",youthNation){youthNation=it.take(30)}
    Pick("Alter",youthAge,listOf(15,16,17),{"$it Jahre"}){youthAge=it};Pick("Position",youthPos,Position.entries.toList(),{it.label}){youthPos=it};Pick("Fuß",youthFoot,Foot.entries.toList(),{it.label}){youthFoot=it};Pick("Grundprofil",youthBlueprint,YouthBlueprint.entries.toList(),{it.label}){youthBlueprint=it};Pick("Zielrolle",youthRole,PlayerRole.entries.filter{it!=PlayerRole.AUTO},{it.label}){youthRole=it}
    val cost=CustomYouthSystem.searchCost(w,youthAge,youthPos,youthFoot,youthBlueprint,youthRole);Metric("Scouting-Auftrag",euros(cost));Action("Talentsuche beauftragen · ${euros(cost)}",CustomYouthSystem.remainingSlots(w)>0&&c.budget>=cost&&youthFirst.trim().length>=2&&youthLast.trim().length>=2&&w.live==null){vm.action("Gezielte Talentsuche abgeschlossen."){CustomYouthSystem.create(it,youthFirst,youthLast,youthNation,youthAge,youthPos,youthFoot,youthBlueprint,youthRole)}}
    Action("Jugend-Transfermarkt öffnen",secondary=true){planningView="Transfers";marketView="Jugendmarkt"}
    Text("Dort kannst du reale U19/U23-Talente anderer Vereine kaufen oder leihen und vor Abschluss direkt U19, U23 oder Profikader als Ziel festlegen.",color=Muted,style=MaterialTheme.typography.bodySmall)
   }
   YouthSquad.entries.forEach{squad->
    val season=if(squad==YouthSquad.U19)academy.u19Season else academy.u23Season
    Section("${squad.label}-Kader · ${season.points} Punkte"){
     val youth=w.squad().filter{it.youth&&it.youthSquad==squad}.sortedByDescending{it.ca}
     if(youth.isEmpty())Text("Aktuell kein Spieler in diesem Nachwuchsteam.",color=Muted)
     youth.forEach{p->val y=p.youthProfile;PlayerRow(p,w,onProfile);Text("${p.youthTeamStats.appearances} Sp · ${p.youthTeamStats.goals} Tore · ${p.youthTeamStats.assists} Vorlagen · Ø ${dec(p.youthTeamStats.averageRating)}",color=Gold,style=MaterialTheme.typography.bodySmall);Text("${y.path.label} · Lernen ${y.learning} · Reife ${y.maturity} · Bereitschaft ${YouthEngine.readiness(w,p)}/100",color=Muted);Text("Entwicklungsfortschritt ${(p.trainingProgress*100).toInt().coerceIn(0,99)}% · Co-Trainer ${if(w.assistantCoach.autoYouthTraining)if(y.seniorTraining)"Proftraining + Academy" else "Academy-Plan" else "Automatik aus"}",color=if(w.assistantCoach.autoYouthTraining)Grass else Muted,style=MaterialTheme.typography.bodySmall)
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(squad==YouthSquad.U19)TextButton({vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U23)}},Modifier.weight(1f),enabled=w.live==null){Text("Zur U23")} else if(w.calendar.season-p.birthYear<=19)TextButton({vm.action{YouthCompetitionSystem.move(it,p.id,YouthSquad.U19)}},Modifier.weight(1f),enabled=w.live==null){Text("Zur U19")};TextButton({vm.action{YouthCompetitionSystem.temporaryCallUp(it,p.id)}},Modifier.weight(1f),enabled=w.live==null){Text("1 Spiel")};TextButton({vm.action{ClubActions.promote(it,p.id)}},Modifier.weight(1f),enabled=w.live==null){Text("Dauerhaft hoch")}}
      HorizontalDivider()
     }
    }
   }
  } else {
   Section("Scouting-Zentrale"){
    Pick("Scoutregion",scoutRegion,ScoutRegion.entries.toList(),{it.label}){scoutRegion=it};Metric("Merkliste","${w.watchlist.size} Spieler");Metric("Laufende Aufträge","${w.scoutAssignments.size}")
    w.scoutAssignments.values.sortedBy{it.weeksRemaining}.take(8).forEach{a->val p=w.players[a.playerId]?:return@forEach;val r=w.scoutReports[p.id];Text("${p.name} · ${a.region.label} · ${a.weeksRemaining} Woche(n)",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Meter("Berichtsstand",r?.progress?:0)}
   }
   Section("Eigene Spieler anbieten"){
    val offerable=w.squad().filter{!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0}.sortedByDescending{it.ca}
    if(offerable.isEmpty())Text("Aktuell ist kein eigener Spieler für Verkauf oder Leihe verfügbar.",color=Muted) else {
     val selected=outboundPlayerId.takeIf{id->offerable.any{it.id==id}}?:offerable.first().id
     Pick("Spieler",selected,offerable.map{it.id},{id->w.players[id]?.let{"${it.name} · ${if(it.youth)"${it.youthSquad.label} · " else ""}${it.position.label} · Stärke ${it.ca}"}?:"Spieler"}){outboundPlayerId=it}
     Pick("Angebotsart",outboundDeal,listOf(DealType.BUY,DealType.LOAN,DealType.LOAN_OPTION),{it.label}){outboundDeal=it}
     Text("Der Spieler wird aktiv mehreren passenden Vereinen angeboten. Angebote bleiben getrennt, damit du vergleichen und mit jedem Verein einzeln verhandeln kannst.",color=Muted,style=MaterialTheme.typography.bodySmall)
     Action("Mehrere Angebote einholen",w.live==null&&ScoutingTransferSystem.transferWindowOpen(w),true){vm.action{OutboundTransferSystem.requestOffers(it,selected,outboundDeal)}}
    }
   }
   val outboundOffers=OutboundTransferSystem.activeOffers(w)
   if(outboundOffers.isNotEmpty())Section("Angebote für unsere Spieler"){
    outboundOffers.forEach{o->val p=w.players[o.playerId]?:return@forEach;val buyer=w.clubs[o.buyerClubId]?:return@forEach
     Text("${p.name} · ${buyer.name}",style=MaterialTheme.typography.titleMedium)
     Text("${o.type.label} · ${o.status.label} · Runde ${o.round}",color=when(o.status){NegotiationStatus.AGREED->Grass;NegotiationStatus.REJECTED->Clay;else->Gold})
     Text("Angebot ${euros(o.fee)}${if(o.type==DealType.BUY)" · Weiterverkauf ${o.sellOnPercent}%" else " · ${o.loanWeeksRequested} Wochen"}",color=Muted)
     if(o.type==DealType.LOAN_OPTION)Text("Kaufoption ${euros(o.buyOption)} · Rückruf ${if(o.recallAllowed)"möglich" else "ausgeschlossen"}",color=Muted)
     Text(o.message,color=Muted,style=MaterialTheme.typography.bodySmall)
     if(o.status==NegotiationStatus.COUNTER){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton({vm.action{OutboundTransferSystem.accept(it,o.id)}},Modifier.weight(1f)){Text("Annehmen")};TextButton({vm.action{OutboundTransferSystem.counter(it,o.id,"fee")}},Modifier.weight(1f)){Text(if(o.type==DealType.BUY)"Ablöse +" else "Leihgebühr +")};TextButton({vm.action{OutboundTransferSystem.reject(it,o.id)}},Modifier.weight(1f)){Text("Ablehnen")}}
      if(o.type==DealType.BUY)TextButton({vm.action{OutboundTransferSystem.counter(it,o.id,"sellon")}}){Text("Weiterverkaufsanteil +")}
      if(o.type==DealType.LOAN_OPTION)TextButton({vm.action{OutboundTransferSystem.counter(it,o.id,"option")}}){Text("Kaufoption +")}
     } else if(o.status==NegotiationStatus.AGREED){
      Action("Einigung bestätigen",w.live==null,true){vm.action{OutboundTransferSystem.accept(it,o.id)}};TextButton({vm.action{OutboundTransferSystem.reject(it,o.id)}}){Text("Doch ablehnen")}
     }
     HorizontalDivider()
    }
   }
   Section("Transfermarkt"){
    Pick("Ansicht",marketView,listOf("Spielersuche","Jugendmarkt","Merkliste","Interesse an unserem Verein")){marketView=it}
    if(marketView=="Spielersuche"||marketView=="Jugendmarkt"){Field("Name suchen",nameQuery){nameQuery=it.take(40)};val clubIds=listOf(0)+w.clubs.values.sortedBy{it.name}.map{it.id};Pick("Verein",clubFilter,clubIds,{id->if(id==0)"Alle Vereine" else w.clubs[id]?.name?:"Verein"}){clubFilter=it};Pick("Position",filter,listOf("Alle")+Position.entries.map{it.name}){filter=it}}
    Pick("Deal",dealType,DealType.entries.toList(),{it.label}){dealType=it};Pick("Versprochene Rolle",role,SquadRole.entries.toList(),{it.label}){role=it};Text(ScoutingTransferSystem.transferWindowLabel(w),color=if(ScoutingTransferSystem.transferWindowOpen(w))Grass else Clay,style=MaterialTheme.typography.bodySmall)
   }
   val candidates=remember(w.calendar.absoluteWeek,w.players.size,w.watchlist.hashCode(),marketView,filter,clubFilter,nameQuery){
    val ctx=TransferInterestSystem.context(w,w.user.clubId)
    when(marketView){
     "Merkliste"->w.watchlist.asSequence().mapNotNull{w.players[it]}.filter{!it.retired&&it.clubId!=w.user.clubId}.map{it to TransferInterestSystem.snapshot(w,it,ctx)}.sortedByDescending{it.second.score}.take(24).toList()
     "Interesse an unserem Verein"->TransferInterestSystem.candidatesForClub(w,w.user.clubId,58).take(24)
     "Jugendmarkt"->{
      val base=w.players.values.asSequence().filter{p->!p.retired&&p.clubId!=w.user.clubId&&w.calendar.season-p.birthYear<=22&&(filter=="Alle"||p.position.name==filter)&&(clubFilter==0||p.clubId==clubFilter)&&(nameQuery.isBlank()||p.name.contains(nameQuery.trim(),true))}
      base.map{it to TransferInterestSystem.snapshot(w,it,ctx)}.sortedWith(compareBy<Pair<Player,TransferInterestSystem.Snapshot>>{w.calendar.season-it.first.birthYear}.thenByDescending{it.first.ca}).take(40).toList()
     }
     else->{
      val base=w.players.values.asSequence().filter{p->!p.retired&&!p.youth&&p.clubId!=w.user.clubId&&(filter=="Alle"||p.position.name==filter)&&(clubFilter==0||p.clubId==clubFilter)&&(nameQuery.isBlank()||p.name.contains(nameQuery.trim(),true))}
      // Interesse ist relativ teuer. Erst eine breite sportlich/vertraglich relevante Shortlist bilden,
      // dann nur für diese Kandidaten den vollständigen Interessen-Snapshot rechnen.
      val all=base.toList();val shortlist=(all.sortedByDescending{it.ca}.take(180)+all.filter{it.wantsMove||it.clubId==0||it.contractYears<=1}.sortedByDescending{it.ca}.take(120)).distinctBy{it.id}
      shortlist.asSequence().map{it to TransferInterestSystem.snapshot(w,it,ctx)}.sortedWith(compareByDescending<Pair<Player,TransferInterestSystem.Snapshot>>{it.second.score}.thenByDescending{it.first.ca}).take(24).toList()
     }
    }
   }
   if(candidates.isEmpty())Section{Text("Keine Spieler passen zu dieser Ansicht.",color=Muted)}
   candidates.forEach{(p,interest)->Section(p.name){
    val seller=w.clubs[p.clubId];val report=ScoutingTransferSystem.report(w,p);Text("${p.position.label} · ${w.calendar.season-p.birthYear} Jahre · ${if(p.youth)"${p.youthSquad.label} · " else ""}${seller?.name?:"vereinslos"} · Vertrag ${p.contractYears} J.",color=Muted)
    Metric("Stärke",ScoutingTransferSystem.strengthLabel(w,p));Metric("Potenzial",ScoutingTransferSystem.potentialLabel(w,p));if(report!=null)Meter("Scoutbericht",report.progress)
    Metric("Interesse an ${c.shortName}","${interest.score} / 100 · ${interest.level.label}");if(interest.reasons.isNotEmpty())Text(interest.reasons.joinToString(" · "),color=if(interest.score>=74)Grass else Muted)
    val bids=ScoutingTransferSystem.competition(w,p.id);if(bids.isNotEmpty()){Text("Transferkonkurrenz",style=MaterialTheme.typography.titleMedium);bids.forEach{b->Text("${w.clubs[b.clubId]?.name?:"Konkurrent"}: ca. ${euros(b.fee)} · noch ${b.expiresWeek-w.calendar.absoluteWeek} Wochen",color=Clay,style=MaterialTheme.typography.bodySmall)}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton({vm.action{ScoutingTransferSystem.toggleWatchlist(it,p.id)}},Modifier.weight(1f)){Text(if(p.id in w.watchlist)"Von Merkliste" else "Merken")};TextButton({vm.action{ScoutingTransferSystem.startScouting(it,p.id,scoutRegion)}},Modifier.weight(1f),enabled=p.id !in w.scoutAssignments&&c.budget>=ScoutingTransferSystem.scoutCost(w,p,scoutRegion)){Text(if(p.id in w.scoutAssignments)"Scout läuft" else "Scouten")}}
    val existing=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.playerId==p.id&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.maxByOrNull{it.id};Action(if(existing==null)"Verhandlung starten: ${dealType.label}" else "Verhandlung öffnen",w.live==null&&(existing!=null||p.clubId==0||ScoutingTransferSystem.transferWindowOpen(w)),true){negotiationPlayerId=p.id;if(existing==null)vm.action{TransferEngine.createOffer(it,it.user.clubId,p.id,dealType,role)}}
    if(ScoutingTransferSystem.bosmanEligible(w,p)){val d=ScoutingTransferSystem.precontractDemand(w,p);Action("Bosman-Vorvertrag · ${euros(d.second)} Handgeld",w.live==null&&interest.score>=56&&c.budget>=d.second,true){vm.action{ScoutingTransferSystem.signPrecontract(it,p.id)}};Text("Vorvertrag: etwa ${euros(d.first.toLong())}/Woche · Wechsel zum Saisonstart ablösefrei.",color=Grass,style=MaterialTheme.typography.bodySmall)}
   }}
   val incoming=w.squad().filter{it.loanParentClubId!=0};val outgoing=w.players.values.filter{it.loanParentClubId==w.user.clubId&&it.clubId!=w.user.clubId&&!it.retired}
   if(incoming.isNotEmpty()||outgoing.isNotEmpty())Section("Leihen & Rückruf"){
    incoming.forEach{p->Text("Bei uns: ${p.name} · ${p.loanWeeks} Wochen",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("Kaufoption ${if(p.loanOptionFee>0)euros(p.loanOptionFee) else "keine"}",color=Muted);if(p.loanOptionFee>0)Action("Kaufoption ziehen",w.live==null&&c.budget>=p.loanOptionFee,true){vm.action{TransferEngine.exerciseOption(it,p.id)}}}
    outgoing.forEach{p->Text("Verliehen: ${p.name} → ${w.clubs[p.clubId]?.name?:"Verein"} · ${p.loanWeeks} Wochen",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("Rückruf ${if(p.loanRecallAllowed)"erlaubt" else "vertraglich ausgeschlossen"}",color=Muted);if(p.loanRecallAllowed)Action("Leihe vorzeitig zurückrufen",w.live==null,true){vm.action{ScoutingTransferSystem.recallLoan(it,p.id)}}}
   }
   val talks=w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.sortedByDescending{it.id};if(talks.isNotEmpty())Section("Aktive Verhandlungen"){
    talks.forEach{o->val p=w.players[o.playerId]?:return@forEach;Text("${p.name} · ${o.type.label} · ${o.stage.label} · Runde ${o.round}",style=MaterialTheme.typography.titleMedium);Text(o.message,color=when(o.status){NegotiationStatus.AGREED->Grass;NegotiationStatus.REJECTED->Clay;else->Gold});Text("Ablöse ${euros(o.fee)} · Gehalt ${euros(o.wage.toLong())}/W · Handgeld ${euros(o.signingBonus)}",color=Muted)
     val targetAge=w.calendar.season-p.birthYear
     if(targetAge<=22){
      val destinations=buildList{add("Profikader");if(targetAge<=19)add("U19");add("U23")};val current=o.targetYouthSquad?.label?:"Profikader"
      Pick("Zielkader nach Abschluss",current,destinations){dest->vm.action{world->world.negotiations.getValue(o.id).targetYouthSquad=when(dest){"U19"->YouthSquad.U19;"U23"->YouthSquad.U23;else->null}}}
     }
     if(o.type==DealType.LOAN||o.type==DealType.LOAN_OPTION){Pick("Leihdauer",o.loanWeeksRequested,listOf(12,24,40),{"$it Wochen"}){v->vm.action{it.negotiations.getValue(o.id).loanWeeksRequested=v}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Rückrufklausel");Switch(o.recallAllowed,{v->vm.action{it.negotiations.getValue(o.id).recallAllowed=v}})}}
     if(o.status==NegotiationStatus.COUNTER){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton({vm.action{TransferEngine.improve(it,o.id,"fee")}}){Text("Ablöse +")};TextButton({vm.action{TransferEngine.improve(it,o.id,"wage")}}){Text("Gehalt +")};TextButton({vm.action{TransferEngine.improve(it,o.id,"role")}}){Text("Rolle +")}}}
     if(o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.MEDICAL)Action("Medizincheck & Registrierung",w.live==null,true){vm.action{TransferEngine.advanceProcess(it,o.id)}};if(o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.REGISTRATION){if(o.registrationReady)Action("Deal abschließen",w.live==null,true){vm.action{TransferEngine.complete(it,o.id)}} else Action("Registrierung erneut prüfen",w.live==null,true){vm.action{TransferEngine.advanceProcess(it,o.id)}}};TextButton({vm.action("Verhandlung zurückgezogen."){TransferEngine.withdraw(it,o.id)}}){Text("Verhandlung zurückziehen")};HorizontalDivider()
    }
   }
   if(w.transferHistory.isNotEmpty())Section("Transferhistorie"){
    w.transferHistory.take(20).forEach{h->val p=w.players[h.playerId];Text("S${h.season} W${h.week+1} · ${p?.name?:"Spieler"}",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("${w.clubs[h.fromClubId]?.shortName?:"frei"} → ${w.clubs[h.toClubId]?.shortName?:"frei"} · ${if(h.fee>0)euros(h.fee) else "ablösefrei"}${h.note.takeIf{it.isNotBlank()}?.let{" · $it"}?:""}",color=Muted,style=MaterialTheme.typography.bodySmall)}
   }
  }
 }
 val selectedTalk=negotiationPlayerId?.let{id->w.negotiations.values.filter{it.buyerClubId==w.user.clubId&&it.playerId==id&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.maxByOrNull{it.id}}
 selectedTalk?.let{o->val p=w.players[o.playerId];AlertDialog(onDismissRequest={negotiationPlayerId=null},title={Text(if(p!=null)"Verhandlung · ${p.name}" else "Verhandlung")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(o.message,color=when(o.status){NegotiationStatus.AGREED->Grass;NegotiationStatus.REJECTED->Clay;else->Gold});Text("Verein ${o.sellerScore}/100 · Spieler ${o.playerScore}/100 · Berater ${o.agentScore}/100",color=Muted);p?.let{val interest=TransferInterestSystem.snapshot(w,it,w.user.clubId);Text("Wechselinteresse: ${interest.score}/100 · ${interest.level.label}",color=if(interest.score>=74)Grass else Muted)};Text("Ablöse ${euros(o.fee)} · Gehalt ${euros(o.wage.toLong())}/W",color=Muted);Text("Handgeld ${euros(o.signingBonus)} · Weiterverkauf ${o.sellOnPercent}% · Rolle ${o.role.label}",color=Muted);if(o.status==NegotiationStatus.COUNTER){Text("Gegenangebot verbessern",style=MaterialTheme.typography.titleMedium)}}},confirmButton={when{o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.MEDICAL->TextButton({vm.action{TransferEngine.advanceProcess(it,o.id)}}){Text("Medizincheck")};o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.REGISTRATION&&o.registrationReady->TextButton({vm.action{TransferEngine.complete(it,o.id)}}){Text("Deal abschließen")};o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.REGISTRATION->TextButton({vm.action{TransferEngine.advanceProcess(it,o.id)}}){Text("Registrierung prüfen")};else->TextButton({negotiationPlayerId=null}){Text("Schließen")}}},dismissButton={TextButton({negotiationPlayerId=null;vm.action("Verhandlung zurückgezogen."){TransferEngine.withdraw(it,o.id)}}){Text("Zurückziehen")}})}
}

private fun careerTotals(p:Player)=Stats(
 appearances=p.stats.appearances+p.career.sumOf{it.stats.appearances},
 goals=p.stats.goals+p.career.sumOf{it.stats.goals},
 assists=p.stats.assists+p.career.sumOf{it.stats.assists},
 minutes=p.stats.minutes+p.career.sumOf{it.stats.minutes},
 yellow=p.stats.yellow+p.career.sumOf{it.stats.yellow},
 red=p.stats.red+p.career.sumOf{it.stats.red}
)
private fun nextMilestone(value:Int,steps:List<Int>)=steps.firstOrNull{it>value}?:((value/100)+1)*100

@Composable fun CareerScreen(w: World,vm:GameViewModel){val p=w.self();val totals=careerTotals(p);val age=w.calendar.season-p.birthYear
 val contributionTarget=when(p.position){Position.ST,Position.LA,Position.RA->18;Position.OM,Position.ZM->14;Position.DM->9;Position.LV,Position.RV,Position.IV->6;Position.TW->0}
 val contribution=p.stats.goals+p.stats.assists;val seasons=p.career+PlayerSeason(w.calendar.season,w.club().name,p.stats);val best=seasons.maxByOrNull{it.stats.goals+it.stats.assists}
 val legacy=(totals.appearances+totals.goals*3+totals.assists*2+w.history.sumOf{it.awards.size*8}+w.history.count{it.outcome=="Aufstieg"}*15).coerceAtLeast(0)
 val legacyLabel=when{legacy>=500->"Vereinslegende";legacy>=260->"Vereinsikone";legacy>=120->"Identifikationsfigur";legacy>=45->"Stammkraft";else->"Gründer"}
 Page(p.name,"SPIELERKARRIERE & VEREINSGESCHICHTE"){
  Section("Saison ${w.calendar.season}"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Einsätze","${p.stats.appearances}");Metric("Tore","${p.stats.goals}");Metric("Vorlagen","${p.stats.assists}")};Text("${p.stats.minutes} Minuten · ${p.stats.yellow} gelbe Karten · ${p.stats.red} Platzverweise",color=Muted);Text("$age Jahre · Stärke ${p.ca} · ${p.foot.label}",color=Muted);if(p.injuryWeeks>0)Text("${p.injury}: ${p.injuryWeeks} Wochen",color=Clay)}
  Section("Dein Karriereweg"){
   Pick("Karrierefokus",w.user.playerCareerFocus,PlayerCareerFocus.entries.toList(),{it.label}){focus->vm.action("Karrierefokus geändert."){it.user.playerCareerFocus=focus}}
   Text(w.user.playerCareerFocus.description,color=Muted,style=MaterialTheme.typography.bodySmall)
   if(w.user.playerCareerFocus!=PlayerCareerFocus.BALANCED)Text("Der Fokus hat einen kleinen echten Einfluss auf deine wöchentliche Entwicklung; Teamtraining und Spielpraxis bleiben wichtiger.",color=Grass,style=MaterialTheme.typography.bodySmall)
  }
  Section("Persönliche Saisonziele"){
   Text("Einsätze · ${p.stats.appearances}/24",color=if(p.stats.appearances>=24)Grass else Chalk);LinearProgressIndicator(progress={(p.stats.appearances/24f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
   if(contributionTarget>0){Text("Torbeteiligungen · $contribution/$contributionTarget",color=if(contribution>=contributionTarget)Grass else Chalk);LinearProgressIndicator(progress={(contribution/contributionTarget.toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())}
   Text("Disziplin · ${p.stats.yellow} Gelbe / ${p.stats.red} Rot",color=if(p.stats.red==0&&p.stats.yellow<=6)Grass else Gold);Text("Ziel: höchstens 6 Gelbe und kein Platzverweis.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Karriere gesamt"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Spiele","${totals.appearances}");Metric("Tore","${totals.goals}");Metric("Vorlagen","${totals.assists}")};Metric("Vermächtnis","$legacy · $legacyLabel");best?.let{Text("Beste Saison: ${it.season} · ${it.stats.goals} Tore + ${it.stats.assists} Vorlagen",color=Gold)};Text("Nächste Marken: ${nextMilestone(totals.appearances,listOf(25,50,100,150,200,300,500))} Spiele · ${nextMilestone(totals.goals,listOf(10,25,50,100,150,250))} Tore · ${nextMilestone(totals.assists,listOf(10,25,50,100,150,250))} Vorlagen",color=Muted,style=MaterialTheme.typography.bodySmall)}
  Section("Vereinsziele"){
   val table=WorldFactory.table(w,w.club().tier);val rank=table.indexOfFirst{it.clubId==w.user.clubId}.let{if(it<0)0 else it+1};val topHalf=(table.size+1)/2;val youthMinutes=w.squad().filter{w.calendar.season-it.birthYear<=21}.sumOf{it.stats.minutes}
   Text("Sportlich · obere Tabellenhälfte",color=if(rank in 1..topHalf)Grass else Chalk);Text("Aktuell Platz ${if(rank==0)"–" else rank.toString()} von ${table.size}",color=Muted)
   Text("Entwicklung · 900 U21-Minuten",color=if(youthMinutes>=900)Grass else Chalk);Text("$youthMinutes / 900 Minuten",color=Muted)
   Text("Wirtschaft · positive Vereinskasse",color=if(w.club().budget>=0)Grass else Clay);Text(euros(w.club().budget),color=Muted)
  }
  val all=w.players.values.filter{!it.retired||it.career.isNotEmpty()};val scorer=all.maxByOrNull{careerTotals(it).goals};val apps=all.maxByOrNull{careerTotals(it).appearances};val assists=all.maxByOrNull{careerTotals(it).assists}
  Section("Vereinsrekorde & Hall of Fame"){scorer?.let{Text("Rekordtorschütze · ${it.name} · ${careerTotals(it).goals}",color=Gold)};apps?.let{Text("Meiste Einsätze · ${it.name} · ${careerTotals(it).appearances}")};assists?.let{Text("Meiste Vorlagen · ${it.name} · ${careerTotals(it).assists}")};Text("Rekorde berücksichtigen laufende und abgeschlossene Spielzeiten der im Spiel bekannten Karrieren.",color=Muted,style=MaterialTheme.typography.bodySmall)}
  Section("Vereinsgeschichte"){if(w.history.isEmpty())Text("Euer erstes Kapitel läuft. Nach Saisonende stehen hier Platzierung, Aufstieg und Auszeichnungen.",color=Muted);w.history.asReversed().forEach{h->Text("${h.season} · ${h.outcome}",style=MaterialTheme.typography.titleLarge,color=if(h.outcome=="Aufstieg")Grass else Chalk);Text("${h.league}: Platz ${h.rank}, ${h.points} Punkte, ${h.goals} Tore");h.awards.forEach{Text(it,color=Grass)};HorizontalDivider()}}
  if(p.career.isNotEmpty())Section("Deine abgeschlossenen Spielzeiten"){p.career.asReversed().forEach{Text("${it.season} · ${it.clubName}");Text("${it.stats.appearances} Einsätze · ${it.stats.goals} Tore · ${it.stats.assists} Vorlagen",color=Muted)}}
 }
}

@Composable fun HelpScreen(w:World,vm:GameViewModel){Page("Hilfe & Tutorial","MANAGER-HANDBUCH · v0.5.22"){
 Section("Geführtes Tutorial"){Text(if(w.user.tutorialEnabled&&!w.user.tutorialCompleted)"Das geführte Tutorial ist in dieser Karriere aktiv." else "Das Tutorial ist aktuell beendet oder deaktiviert.",color=Muted);Text("Es führt durch Startseite, Matchday-Kader, Training, Transfers/Jugend, Wettbewerbe und das Live-Spiel. Der Ablauf entspricht den aktuellen Systemen von v0.5.22.",color=Muted,style=MaterialTheme.typography.bodySmall);Action("Tutorial von vorn starten",secondary=true){vm.action("Tutorial neu gestartet."){it.user.tutorialEnabled=true;it.user.tutorialCompleted=false;it.user.tutorialStep=0}};if(w.user.tutorialEnabled&&!w.user.tutorialCompleted)Action("Tutorial beenden",secondary=true){vm.action{it.user.tutorialEnabled=false;it.user.tutorialCompleted=true}}}
 Section("Startelf & Ersatzbank"){Text("Die Startelf bleibt deine Auswahl, solange die Spieler verfügbar sind. Vor dem Spiel bestimmst du die sieben Ersatzspieler selbst. Das Spiel ergänzt nur fehlende oder ungültige Plätze, statt deine komplette Bank oder Elf neu zu sortieren.",color=Muted)}
 Section("Live-Aufstellung"){Text("Im Taktik-/Wechselbereich siehst du die komplette Formation. Spieler antippen wählt ihn für einen Wechsel aus; danach führt ein Tipp auf einen Ersatzspieler den Wechsel aus. Per Halten und Ziehen kannst du Positionsplätze tauschen. Nach einem Platzverweis bleibt eine sichtbare Lücke, die du taktisch verschieben kannst.",color=Muted)}
 Section("Co-Trainer-Wechsel"){Text("Wechselvorschläge berücksichtigen Live-Rating, Form, Fitness, Kartenrisiko, Spielstand, Minute, Positionspassung und Bankqualität. Derselbe Spieler wird innerhalb eines Pakets nicht mehrfach vorgeschlagen. Mehrfachvorschläge können einzeln angenommen oder abgelehnt werden.",color=Muted)}
 Section("U19 & U23"){Text("U19 und U23 sind eigenständige Mannschaften. Jugendspieler zählen nicht zur Profikadergrenze und müssen vor einem Einsatz bei den Profis hochgezogen oder notfallnominiert werden. Talente können gezielt entwickelt, gekauft, verkauft und verliehen werden.",color=Muted)}
 Section("Training & Potenzial"){Text("Wochenplan, Intensität, Gegnerfokus und Extra-Foki beeinflussen Entwicklung und Belastung. Potenzialtraining kann Potenzial und Attribute erhöhen; unter 20-Jährige profitieren besonders stark und können zusätzlich echte Gesamtstärke gewinnen. Ältere Profis entwickeln sich moderater.",color=Muted)}
 Section("Transfers & Verhandlungen"){Text("Scouting verbessert Berichte mit der Zeit. Eigene Spieler können mehrere Kauf- oder Leihangebote erhalten. Aktive Verhandlungen reservieren Budget; beim Rücktritt wird es wieder freigegeben. Medizincheck und Registrierung bleiben Teil desselben Deals. Transfers und Leih-Rückkehr setzen eine gültige manuelle Elf nicht mehr unnötig zurück.",color=Muted)}
 Section("Wettbewerbe & Preisgeld"){Text("Liga, nationale Pokale, Champions League, Europa League, Club World Cup und die optionale Krone der Kontinente werden getrennt geführt. Die Krone nimmt die besten sechs Vereine jeder spielbaren Liga auf und läuft nach einer möglichen Vorrunde direkt im K.-o.-System. Rundenprämien steigen je Runde; der Sieger erhält zusätzlich 50 Mio. €. Internationale Titelprämien werden nur einmal ausgezahlt.",color=Muted)}
 Section("Live-Spiel & Simulation"){Text("Live, Taktik, Statistik und Analyse greifen auf denselben Match-Zustand zu. Torhüter wirken deutlich auf Abschlusswahrscheinlichkeiten; internationale Pokale besitzen eine eigene Tor-Kalibrierung. Schnellsimulation und Live-Runner nutzen dieselbe Matchlogik.",color=Muted)}
 Section("Statistik & Analyse"){Text("Statistik zeigt Matchwerte. Analyse nutzt echte Ereignisse für xG, Schusskarte, Passnetz und Taktikänderungen. xG beschreibt Chancenqualität und ist keine Garantie für Tore.",color=Muted)}
 Section("Speichern & Stabilität"){Text("Spielstände werden komprimiert mit Sicherheitskopie und speicherschonenden Live-Checkpoints geschrieben. UI-Aktionen veröffentlichen sofort einen neuen Zustand, damit Schalter, Formation und Kaderänderungen ohne erneutes Öffnen sichtbar werden. Ältere Saves werden migriert und auf bekannte Altzustände geprüft.",color=Muted)}
 Section("What's New"){Text("Die wichtigsten Änderungen jeder neuen Version erscheinen beim ersten Start einmal automatisch. Unter Mehr → What's New kannst du die aktuelle Übersicht jederzeit erneut öffnen.",color=Muted)}
 Section("Spielerkarriere"){Text("Karrierefokus, Saisonziele, Vermächtnis, Meilensteine, Vereinsrekorde und abgeschlossene Spielzeiten machen lange Karrieren nachvollziehbar.",color=Muted)}
 }}

@Composable fun SettingsScreen(vm: GameViewModel){
 val soundsEnabled by vm.soundsEnabled.collectAsState()
 val tacticalMatchView by vm.tacticalMatchView.collectAsState()
 Page("Einstellungen","AUDIO & SPIELERLEBNIS"){
  Section("Match-Ansicht"){
   Text("Wähle die Darstellung für Live-Spiele.",color=Muted,style=MaterialTheme.typography.bodySmall)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text("Taktische Draufsicht 3.0",style=MaterialTheme.typography.titleMedium);Text(if(tacticalMatchView)"Alle 22 Spieler · flüssige Blockverschiebung · korrekte Angriffsrichtung · Seitenwechsel zur Halbzeit." else "Klassische Gründerelf-Matchansicht ist aktiv.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    Switch(checked=tacticalMatchView,onCheckedChange={vm.setTacticalMatchView(it)})
   }
   Text("AUS = bisherige Matchansicht unverändert. AN = flüssige taktische Draufsicht mit realistischen Abständen, Restverteidigung, Pressing, Laufwegen und sichtbarer Angriffsrichtung. Zur Halbzeit wechseln die Teams die Seiten; bei Schüssen springt die bekannte Toransicht automatisch hinein.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Match-Sounds"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text("Stadion- und Spielsounds",style=MaterialTheme.typography.titleMedium);Text(if(soundsEnabled)"Alle Match-Sounds sind aktiv." else "Alle Match-Sounds sind stummgeschaltet.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    Switch(checked=soundsEnabled,onCheckedChange={vm.setSoundsEnabled(it)})
   }
   Text("Ein einziger Schalter steuert sämtliche Pfiffe, Ballkontakte, Schüsse, Pfosten, Torjubel, Crowd-Reaktionen, Karten, Standards, Wechsel, Nachspielzeit, Elfmeter und VAR-Sounds. Es gibt bewusst keine einzelnen Sound-Schalter.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Hinweis"){Text("Die Einstellung gilt appweit und bleibt auch nach einem Neustart erhalten. Beim Ausschalten oder sobald die App in den Hintergrund wechselt, werden laufende Match-Sounds sofort beendet.",color=Muted)}
  Section("Audio-Quellen"){Text("v0.5.2 verwendet weiterhin echte Aufnahmen: Stadion/Crowd aus ‘WWS FootballAustriavs.Sweden’ (CC BY 4.0), echter Applaus aus ‘Applause.ogg’ (CC BY-SA) und echter Pfiff aus ‘Whistle.ogg’ (CC BY-SA 3.0). Ballkontakt, Keeper- und Metall-Samples bleiben reale Aufnahmen. FFmpeg wird nur für Schnitt, Pegel, Filter und Mischung verwendet – keine synthetischen Crowd-/Pfiffgeneratoren mehr.",color=Muted,style=MaterialTheme.typography.bodySmall)}
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

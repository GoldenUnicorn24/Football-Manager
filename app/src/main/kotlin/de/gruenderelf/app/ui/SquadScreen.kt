package de.gruenderelf.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*
import kotlin.math.roundToInt

private fun suitabilityColor(p: Player?,target: Position)=when{p==null->Muted;p.position==target->Grass;p.fit(target)>=.92->Blue;p.fit(target)>=.84->Gold;else->Clay}

@Composable fun Pitch(w: World,onSlot: (Int)->Unit){val c=w.club();val positions=Formations.positions(c.tactics.formation)
 BoxWithConstraints(Modifier.fillMaxWidth().height(470.dp).background(Color(0xFF123B27),shape=androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).padding(6.dp)){
  Canvas(Modifier.fillMaxSize().semantics{contentDescription="Spielfeld, Formation ${c.tactics.formation}"}){val line=Chalk.copy(alpha=.4f);repeat(8){if(it%2==0)drawRect(Color.White.copy(alpha=.018f),Offset(0f,size.height*it/8),Size(size.width,size.height/8))};drawRect(line,Offset(8.dp.toPx(),8.dp.toPx()),Size(size.width-16.dp.toPx(),size.height-16.dp.toPx()),style=Stroke(1.dp.toPx()));drawLine(line,Offset(8.dp.toPx(),size.height*.5f),Offset(size.width-8.dp.toPx(),size.height*.5f),1.dp.toPx());drawCircle(line,size.width*.16f,center,style=Stroke(1.dp.toPx()));drawRect(line,Offset(size.width*.22f,8.dp.toPx()),Size(size.width*.56f,size.height*.13f),style=Stroke(1.dp.toPx()));drawRect(line,Offset(size.width*.22f,size.height*.87f-8.dp.toPx()),Size(size.width*.56f,size.height*.13f),style=Stroke(1.dp.toPx()))}
  Formations.coordinates(c.tactics.formation).forEachIndexed{i,(x,y)->val target=positions[i];val p=c.tactics.xi.getOrNull(i)?.let{w.players[it]};val color=suitabilityColor(p,target);Column(Modifier.offset((maxWidth-64.dp)*x,(maxHeight-72.dp)*y).width(64.dp).heightIn(min=62.dp).clickable{onSlot(i)}.semantics{contentDescription="${target.label}: ${p?.name?:"Unbesetzt"}. Antippen zum Aufstellen."},horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(36.dp).background(if(p?.id==w.user.playerId)Grass else color.copy(alpha=.9f),CircleShape).border(2.dp,Chalk.copy(alpha=.28f),CircleShape),contentAlignment=Alignment.Center){Text(p?.number?.toString()?:"–",color=Ink,fontWeight=FontWeight.Bold)};Text("${target.name} ${p?.ratingAt(target)?:"–"}",style=MaterialTheme.typography.labelSmall,color=color);Text(p?.lastName?.take(10)?:"Offen",fontSize=10.sp,maxLines=1)}
  }
 }
}

@Composable fun PlayerRow(p: Player,w: World,onProfile: (Int)->Unit,target: Position?=null){Column(Modifier.fillMaxWidth().clickable{onProfile(p.id)}.padding(vertical=7.dp).heightIn(min=52.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Pill(p.position.name,if(target==null)Grass else suitabilityColor(p,target));Column(Modifier.weight(1f)){Text(p.name+(if(p.id==w.user.playerId)" · Du" else ""));Text(if(p.injuryWeeks>0)"${p.injury} · ${p.injuryWeeks} Wo." else if(p.unavailableWeeks>0)"${p.unavailableReason?.label} · ${p.unavailableWeeks} Spieltag(e)" else "${p.position.label}${p.secondaryOptions().takeIf{it.isNotEmpty()}?.let{" · + ${it.joinToString{pos->pos.name}}"}?:""}",color=if(!p.available)Clay else Muted,style=MaterialTheme.typography.bodySmall)};Column(horizontalAlignment=Alignment.End){Text((target?.let{p.ratingAt(it)}?:p.ca).toString(),style=MaterialTheme.typography.titleLarge);if(target!=null)Text(p.roleAt(target),color=suitabilityColor(p,target),style=MaterialTheme.typography.labelSmall)}};LinearProgressIndicator(progress={p.fitness.toFloat()/100},modifier=Modifier.fillMaxWidth().height(4.dp),color=if(p.fitness<60)Clay else Grass,trackColor=Color(0xFF2D3F34))}}

@Composable fun SquadScreen(w: World,vm: GameViewModel,onProfile: (Int)->Unit){val c=w.club();var slot by remember{mutableStateOf<Int?>(null)};var tacticPreset by remember{mutableStateOf("Individuell")};val slots=Formations.positions(c.tactics.formation)
 Page("Die Mannschaft","${c.shortName} · KADER & TAKTIK"){
  Section("Aufstellung"){
   Pick("Formation",c.tactics.formation,Formations.all.keys.toList()){v->vm.action{require(it.live==null){"Formation nach dem Spiel ändern."};it.club().tactics.formation=v;WorldFactory.autoLineup(it)}}
   Pitch(w){slot=it}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Pill("Grün = Hauptposition",Grass);Pill("Blau = Nebenposition",Blue)}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Pill("Gold = spielbar",Gold);Pill("Rot = unpassend",Clay)}
   Action("Beste Elf nach Position & Fitness",w.live==null){vm.action{WorldFactory.autoLineup(it)}}
   Text("Jeder Platz zeigt jetzt die Stärke des Spielers genau auf dieser Position. Du musst die Positionen des Kaders nicht mehr auswendig kennen.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }

  val current=slots.mapIndexedNotNull{i,pos->c.tactics.xi.getOrNull(i)?.let{w.players[it]}?.let{Triple(it,pos,it.ratingAt(pos))}}
  val weakest=current.sortedBy{it.third}.take(3);val tired=current.filter{it.first.fitness<68}.sortedBy{it.first.fitness}.take(3)
  Section("Kadercheck"){
   if(weakest.isNotEmpty()){Text("Positionscheck",style=MaterialTheme.typography.titleMedium);weakest.forEach{(p,pos,r)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${pos.name} · ${p.lastName}");Text("$r / 99",color=suitabilityColor(p,pos))}}}
   if(tired.isNotEmpty()){Text("Belastung",style=MaterialTheme.typography.titleMedium);tired.forEach{(p,_,_)->Text("${p.name}: ${p.fitness.roundToInt()} % Fitness",color=if(p.fitness<55)Clay else Gold)}}else Text("Die Startelf ist körperlich bereit.",color=Grass)
  }

  val rolePlayers=w.squad().filter{!it.youth&&!it.retired}.sortedWith(compareBy<Player>{it.position.ordinal}.thenByDescending{it.ca})
  val roleIds=listOf(0)+rolePlayers.map{it.id}
  val targetIds=listOf(0)+rolePlayers.filter{it.position!=Position.TW}.map{it.id}
  fun roleName(id: Int, automatic: String="Automatisch"): String {
   if(id==0)return automatic
   val p=w.players[id]?:return "Nicht verfügbar"
   return "${p.name} · ${p.position.name} · ${p.ca}"
  }
  Section("Rollen & Standards"){
   Text("Lege fest, wer Verantwortung übernimmt und wen die Mannschaft im Angriff gezielt sucht.",color=Muted,style=MaterialTheme.typography.bodySmall)
   Pick("Kapitän",c.tactics.captainId,roleIds,{roleName(it)}){v->vm.action{require(it.live==null){"Rollen erst nach dem Spiel ändern."};it.club().tactics.captainId=v}}
   Pick("Zielspieler",c.tactics.targetPlayerId,targetIds,{roleName(it,"Kein fester Zielspieler")}){v->vm.action{require(it.live==null){"Rollen erst nach dem Spiel ändern."};it.club().tactics.targetPlayerId=v}}
   Text("Der Zielspieler wird im letzten Drittel häufiger gesucht – bei Steilpässen, Flanken, zweiten Bällen und Abschlüssen. Das erhöht seine Abschlussbeteiligung, garantiert aber keine Tore.",color=Muted,style=MaterialTheme.typography.bodySmall)
   HorizontalDivider(color=Color(0xFF29463A))
   Pick("Elfmeterschütze",c.tactics.penaltyTakerId,roleIds,{roleName(it)}){v->vm.action{require(it.live==null){"Standards erst nach dem Spiel ändern."};it.club().tactics.penaltyTakerId=v}}
   Pick("Freistöße",c.tactics.freeKickTakerId,roleIds,{roleName(it)}){v->vm.action{require(it.live==null){"Standards erst nach dem Spiel ändern."};it.club().tactics.freeKickTakerId=v}}
   Pick("Ecken links",c.tactics.cornerLeftTakerId,roleIds,{roleName(it)}){v->vm.action{require(it.live==null){"Standards erst nach dem Spiel ändern."};it.club().tactics.cornerLeftTakerId=v}}
   Pick("Ecken rechts",c.tactics.cornerRightTakerId,roleIds,{roleName(it)}){v->vm.action{require(it.live==null){"Standards erst nach dem Spiel ändern."};it.club().tactics.cornerRightTakerId=v}}
   Text("Ist ein eingeteilter Spieler nicht auf dem Platz, übernimmt automatisch der passendste Mitspieler.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }

  Section("Ersatzbank auswählen · ${c.tactics.bench.size} / 7"){
   Text("Du bestimmst die sieben Ersatzspieler selbst. Entferne bei voller Bank zuerst einen Spieler und wähle danach den Ersatz.",color=Muted,style=MaterialTheme.typography.bodySmall)
   val benchCandidates=w.squad().filter{!it.youth&&!it.retired&&it.id !in c.tactics.xi}.sortedWith(compareBy<Player>{it.position.ordinal}.thenByDescending{it.ca})
   benchCandidates.forEach{p->
    val selected=p.id in c.tactics.bench
    Row(Modifier.fillMaxWidth().heightIn(min=58.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Checkbox(selected,{vm.action{WorldFactory.toggleBench(it,p.id)}},enabled=w.live==null&&p.available)
     Column(Modifier.weight(1f)){Text(p.name,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal);Text("${p.position.label} · Stärke ${p.ca} · Fitness ${p.fitness.roundToInt()} %${if(!p.available)" · nicht verfügbar" else ""}",color=if(p.available)Muted else Clay,style=MaterialTheme.typography.bodySmall)}
     TextButton({onProfile(p.id)}){Text("Profil")}
    }
   }
   if(c.tactics.bench.size<7)Text("Noch ${7-c.tactics.bench.size} Bankplatz/-plätze frei. Beim Spielstart werden nur fehlende Plätze automatisch aufgefüllt.",color=Gold,style=MaterialTheme.typography.bodySmall)
  }
  Section("Spielidee"){
   Pick("Taktik-Vorlage",tacticPreset,tacticPresets,{it}){v->tacticPreset=v;if(v!="Individuell")vm.action{require(it.live==null){"Taktik nach dem Spiel ändern."};applyTacticPreset(it.club().tactics,v)}}
   StepSlider("Mentalität",c.tactics.mentality,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.mentality=v}};StepSlider("Pressing",c.tactics.pressing,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.pressing=v}};StepSlider("Defensive Linie",c.tactics.line,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.line=v}};StepSlider("Tempo",c.tactics.tempo,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.tempo=v}};StepSlider("Breite",c.tactics.width,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.width=v}};Pick("Spielaufbau",c.tactics.buildUp,BuildUp.entries.toList(),{it.label}){v->tacticPreset="Individuell";vm.action{require(it.live==null){"Spielaufbau nach dem Spiel ändern."};it.club().tactics.buildUp=v}}
   Text(tacticSummary(c.tactics),color=Muted,style=MaterialTheme.typography.bodySmall)
   HorizontalDivider(color=Color(0xFF29463A))
   Text("Eigene Taktikpläne",style=MaterialTheme.typography.titleMedium)
   listOf("Plan A","Plan B","Plan C").forEach{plan->
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
     Text(plan,Modifier.weight(1f),color=if(plan in w.savedTactics)Chalk else Muted)
     TextButton(onClick={vm.action("$plan gespeichert."){world->require(world.live==null){"Taktik erst nach dem Spiel speichern."};world.savedTactics[plan]=tacticSnapshot(world.club().tactics)}}){Text("Speichern")}
     TextButton(onClick={vm.action("$plan geladen."){world->require(world.live==null){"Taktik erst nach dem Spiel laden."};val saved=world.savedTactics[plan]?:return@action;val changed=world.club().tactics.formation!=saved.formation;applySavedTactic(world.club().tactics,saved);if(changed)WorldFactory.autoLineup(world)}},enabled=plan in w.savedTactics){Text("Laden")}
    }
   }
   Text("Gespeichert werden Formation und Spielidee. Bei einer anderen Formation stellt der Co-Trainer die Elf positionsgerecht neu auf.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Alle Spieler"){w.squad().filter{!it.youth}.sortedWith(compareBy<Player>{it.position.ordinal}.thenByDescending{it.ca}).forEach{PlayerRow(it,w,onProfile)}}
 }

 slot?.let{index->val target=slots[index];val players=w.squad().filter{it.available&&!it.youth}.sortedWith(compareByDescending<Player>{it.ratingAt(target)}.thenByDescending{it.fitness});AlertDialog(onDismissRequest={slot=null},title={Text("${target.label} besetzen")},text={LazyColumn(Modifier.heightIn(max=480.dp)){items(players.size){i->val p=players[i];TextButton({slot=null;vm.action{WorldFactory.assignSlot(it,index,p.id)}},Modifier.fillMaxWidth().heightIn(min=64.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.weight(1f)){Text(p.name+(if(p.id==w.user.playerId)" · Du" else ""));Text("${p.position.name}${p.secondaryOptions().takeIf{it.isNotEmpty()}?.let{" · ${it.joinToString{pos->pos.name}}"}?:""} · Fitness ${p.fitness.roundToInt()} %",color=Muted,style=MaterialTheme.typography.bodySmall)};SuitabilityBadge(p,target)}}}}},confirmButton={TextButton({slot=null}){Text("Schließen")}})}
}

@Composable fun ProfileScreen(w: World,id: Int,vm: GameViewModel,onBack: ()->Unit){val p=w.players[id]?:return;var release by remember{mutableStateOf(false)}
 Page(p.name,if(id==w.user.playerId)"DU · SPIELERTRAINER" else "SPIELERPROFIL"){
  Section{Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){PlayerPortrait(p.appearance,w.club().kits.home);Column{Text("${p.position.label} · Nr. ${p.number}");Text("${w.calendar.season-p.birthYear} Jahre · ${p.nationality}",color=Muted);Text("${p.height} cm · ${p.weight} kg · ${p.foot.label}",color=Muted)}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Stärke","${p.ca}");Metric("Tore","${p.stats.goals}");Metric("Vorlagen","${p.stats.assists}")};Meter("Fitness",p.fitness.roundToInt());Meter("Moral",p.morale);Meter("Schärfe",p.sharpness);if(p.injuryWeeks>0)Text("${p.injury} · ${p.injuryWeeks} Wochen",color=Clay);if(p.unavailableWeeks>0)Text("${p.unavailableReason?.label} · ${p.unavailableWeeks} Spieltag(e)",color=Clay)}
  Section("Positionsprofil"){Position.entries.sortedByDescending{p.ratingAt(it)}.take(6).forEach{pos->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("${pos.label} · ${p.roleAt(pos)}",color=if(pos==p.position)Chalk else Muted);SuitabilityBadge(p,pos)}}}
  if(p.clubId==w.user.clubId)Section("Matchrolle"){Pick("Rolle",p.role,PlayerRole.entries.toList(),{it.label}){v->vm.action{require(it.live==null){"Rolle erst nach dem Spiel ändern."};it.players.getValue(id).role=v}};Text("Die Rolle beeinflusst Entscheidungswege der Match-KI und nicht nur die angezeigte Position.",color=Muted,style=MaterialTheme.typography.bodySmall)}
  if(p.clubId==w.user.clubId&&!p.youth)Section("Individuelle Anweisungen"){
   val active=w.club().tactics.instructions[p.id]?:emptyList();Text("${active.size} / 3 aktiv · wirken direkt auf Laufwege, Pressing, Passrisiko, Abschlusswahl und Fitness.",color=Muted,style=MaterialTheme.typography.bodySmall)
   PlayerInstruction.entries.forEach{ins->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(ins.label);Text(when(ins){PlayerInstruction.OVERLAP->"Mehr Vorstöße über außen, höhere Belastung.";PlayerInstruction.CUT_INSIDE->"Flügel zieht in Abschlussräume.";PlayerInstruction.RUN_IN_BEHIND->"Mehr Tiefenläufe, aber erhöhtes Abseitsrisiko.";PlayerInstruction.HOLD_POSITION->"Mehr Restverteidigung, weniger Vorwärtsdrang.";PlayerInstruction.PRESS_MORE->"Aggressiver gegen den Ball, kostet Fitness.";PlayerInstruction.PRESS_LESS->"Kräfte sparen und Raum halten.";PlayerInstruction.TIGHT_MARKING->"Engere Zuordnung und mehr Defensivdruck.";PlayerInstruction.RISKY_PASSES->"Mehr kreative Vertikalpässe, aber zusätzliche Fehlpässe.";PlayerInstruction.SHOOT_MORE->"Wird häufiger als Schütze ausgewählt."},color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(ins in active,{enabled->vm.action{TacticalInstructionSystem.set(it,p.id,ins,enabled)}},enabled=w.live==null&&(ins in active||active.size<3))}}
  }
  if(p.clubId==w.user.clubId&&!p.youth&&id!=w.user.playerId)Section("Vertrag & Versprechen"){
   Text("Noch ${p.contractYears} Jahr(e) · ${euros(p.wage.toLong())}/Woche",color=Muted);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(2,3,4).forEach{years->TextButton({vm.action{ScoutingTransferSystem.extendContract(it,p.id,years)}},Modifier.weight(1f),enabled=w.live==null){Text("$years J.")}}}
   val seniorMatches=w.fixtures.count{it.season==w.calendar.season&&it.played&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)}.coerceAtLeast(1)
   val share=p.stats.appearances.toDouble()/seniorMatches
   val target=when(p.promisedRole){SquadRole.STAR->.80;SquadRole.STARTER->.65;SquadRole.ROTATION->.35;SquadRole.PROSPECT->.20;SquadRole.BACKUP->.10}
   val promiseText=when{share+0.05>=target->"Versprechen aktuell erfüllt";share+0.20>=target->"Spielzeit knapp unter Erwartung";else->"Spielzeit deutlich unter Versprechen"}
   Pick("Versprochene Kaderrolle",p.promisedRole,SquadRole.entries.toList(),{it.label}){newRole->vm.action("Kaderrolle mit ${p.lastName} besprochen."){world->require(world.live==null){"Rollenversprechen erst nach dem Spiel ändern."};val q=world.players.getValue(id);val old=q.promisedRole;if(newRole!=old){q.promisedRole=newRole;q.morale=(q.morale+when{newRole.ordinal<old.ordinal->2;newRole.ordinal>old.ordinal->-4;else->0}).coerceIn(5,100);world.club().dynamics.hierarchyStability=(world.club().dynamics.hierarchyStability+when{newRole.ordinal<old.ordinal->1;newRole.ordinal>old.ordinal->-2;else->0}).coerceIn(0,100)}}}
   Text("$promiseText · ${p.stats.appearances} Einsätze in $seniorMatches Pflichtspielen",color=if(share+0.05>=target)Grass else if(share+0.20>=target)Gold else Clay,style=MaterialTheme.typography.bodySmall)
   Text("Nicht eingehaltene Spielzeitversprechen drücken Moral, Beziehung und Hierarchie und können Wechselwünsche auslösen.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Sichtbare Attribute"){val attributeScale=if(w.developer.enabled)130 else 99;p.attributes.values().forEach{(label,value)->Meter(label,value,attributeScale)};if(w.developer.enabled&&p.attributes.values().values.any{it>99})Text("BMW Elite-Skala · Werte über 100 sind durch Spezialprogramme möglich.",color=Color(0xFF9BCBFF),style=MaterialTheme.typography.bodySmall)}
  Section("Einschätzung"){Text(if(w.user.difficulty==Difficulty.HARDCORE)"Potenzial wird auf dieser Schwierigkeit nicht angezeigt." else if(w.user.difficulty==Difficulty.SANDBOX)"Sandbox-Spielerwerte sind frei gesetzt. Geschätztes Potenzial: ${p.hidden.potential}" else "Geschätztes Potenzial: ${if(w.user.difficulty==Difficulty.CASUAL)p.hidden.potential.toString() else "${(p.hidden.potential-5).coerceAtLeast(p.ca)}–${(p.hidden.potential+5).coerceAtMost(99)}"}");Text("Form ${dec(p.form)} · ${p.stats.appearances} Einsätze · ${p.stats.minutes} Minuten",color=Muted);Text("Spielertyp: ${p.personalityType(w.calendar.season).label} · Matchrolle: ${p.effectiveRole().label}",color=Gold);if(p.youth){Text("${p.youthSquad.label} · Entwicklung: ${p.youthProfile.path.label} · Lernfähigkeit ${p.youthProfile.learning} · Profibereitschaft ${YouthEngine.readiness(w,p)}/100",color=Muted);Text("Nachwuchs: ${p.youthTeamStats.appearances} Sp · ${p.youthTeamStats.goals} Tore · ${p.youthTeamStats.assists} Vorlagen · Ø ${dec(p.youthTeamStats.averageRating)}",color=Gold);Text(YouthEngine.riskLabel(w,p),color=if(YouthEngine.readiness(w,p)>=65)Grass else Clay)}}
  if(id!=w.user.playerId&&p.clubId==w.user.clubId)Section("Unter vier Augen"){val talk=p.lastTalkWeek!=w.calendar.absoluteWeek;Action("Mehr zeigen",talk,true){vm.action{ClubActions.talk(it,id,0)}};Action("Chance geben",talk,true){vm.action{ClubActions.talk(it,id,1)}};Action("Ehrlich sein",talk,true){vm.action{ClubActions.talk(it,id,2)}};if(!talk)Text("Ihr habt diese Woche bereits gesprochen.",color=Muted);Action("Gehen lassen",w.live==null,true){release=true}}
  if(p.youth)Section("Nachwuchsteam"){
   Text("${p.youthSquad.label} ist sein fester Kader. Du kannst ihn dauerhaft hochziehen oder nur für das nächste Profispiel nominieren.",color=Muted,style=MaterialTheme.typography.bodySmall)
   if(p.youthSquad==YouthSquad.U19)Action("In die U23 verschieben",w.live==null,true){vm.action{YouthCompetitionSystem.move(it,id,YouthSquad.U23)}} else if(w.calendar.season-p.birthYear<=19)Action("Zur U19 verschieben",w.live==null,true){vm.action{YouthCompetitionSystem.move(it,id,YouthSquad.U19)}}
   Action("Nur für das nächste Profispiel",w.live==null,true){vm.action{YouthCompetitionSystem.temporaryCallUp(it,id)}}
   Action("Dauerhaft in den Profikader hochziehen",w.live==null){vm.action{ClubActions.promote(it,id)}}
  } else if(p.clubId==w.user.clubId&&id!=w.user.playerId&&w.calendar.season-p.birthYear<=22)Section(if(p.temporarySeniorCallUp)"Notfall-Nominierung" else "Nachwuchsoptionen"){
   if(p.temporarySeniorCallUp){
    Text("Der Spieler kehrt nach dem nächsten Profispiel automatisch in ${p.temporaryReturnSquad?.label?:p.youthSquad.label} zurück.",color=Gold)
    Action("Sofort in den Nachwuchs zurück",w.live==null,true){vm.action{YouthCompetitionSystem.returnTemporary(it,id,true)}}
    Action("Dauerhaft im Profikader behalten",w.live==null){vm.action{YouthCompetitionSystem.makeTemporaryPermanent(it,id)}}
   }else{
    Text("Junge Profis können gezielt in U19/U23 wechseln. Dort erhalten sie Nachwuchsspielpraxis und zusätzlichen Academy-Entwicklungsfortschritt.",color=Muted,style=MaterialTheme.typography.bodySmall)
    if(w.calendar.season-p.birthYear<=19)Action("In die U19 schicken",w.live==null,true){vm.action{YouthCompetitionSystem.assignToYouth(it,id,YouthSquad.U19)}}
    Action("In die U23 schicken",w.live==null,true){vm.action{YouthCompetitionSystem.assignToYouth(it,id,YouthSquad.U23)}}
   }
  };Action("Zurück",secondary=true,onClick=onBack)
 }
 if(release)Confirm("${p.lastName} freistellen?","Er verlässt deinen Verein ablösefrei.",{release=false}){release=false;vm.action{ClubActions.release(it,id)}}
}

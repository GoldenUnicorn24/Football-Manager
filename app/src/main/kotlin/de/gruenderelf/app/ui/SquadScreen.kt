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

  Section("Die Bank · ${c.tactics.bench.size} / 7"){c.tactics.bench.forEach{id->w.players[id]?.let{PlayerRow(it,w,onProfile)}};if(c.tactics.bench.isEmpty())Text("Keine verfügbaren Ersatzspieler.")}
  Section("Spielidee"){
   Pick("Taktik-Vorlage",tacticPreset,tacticPresets,{it}){v->tacticPreset=v;if(v!="Individuell")vm.action{require(it.live==null){"Taktik nach dem Spiel ändern."};applyTacticPreset(it.club().tactics,v)}}
   StepSlider("Mentalität",c.tactics.mentality,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.mentality=v}};StepSlider("Pressing",c.tactics.pressing,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.pressing=v}};StepSlider("Defensive Linie",c.tactics.line,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.line=v}};StepSlider("Tempo",c.tactics.tempo,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.tempo=v}};StepSlider("Breite",c.tactics.width,w.live==null){v->tacticPreset="Individuell";vm.action{it.club().tactics.width=v}};Pick("Spielaufbau",c.tactics.buildUp,BuildUp.entries.toList(),{it.label}){v->tacticPreset="Individuell";vm.action{require(it.live==null){"Spielaufbau nach dem Spiel ändern."};it.club().tactics.buildUp=v}}
   Text(tacticSummary(c.tactics),color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  Section("Alle Spieler"){w.squad().filter{!it.youth}.sortedWith(compareBy<Player>{it.position.ordinal}.thenByDescending{it.ca}).forEach{PlayerRow(it,w,onProfile)}}
 }

 slot?.let{index->val target=slots[index];val players=w.squad().filter{it.available}.sortedWith(compareByDescending<Player>{it.ratingAt(target)}.thenByDescending{it.fitness});AlertDialog(onDismissRequest={slot=null},title={Text("${target.label} besetzen")},text={LazyColumn(Modifier.heightIn(max=480.dp)){items(players.size){i->val p=players[i];TextButton({slot=null;vm.action{WorldFactory.assignSlot(it,index,p.id)}},Modifier.fillMaxWidth().heightIn(min=64.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.weight(1f)){Text(p.name+(if(p.id==w.user.playerId)" · Du" else ""));Text("${p.position.name}${p.secondaryOptions().takeIf{it.isNotEmpty()}?.let{" · ${it.joinToString{pos->pos.name}}"}?:""} · Fitness ${p.fitness.roundToInt()} %",color=Muted,style=MaterialTheme.typography.bodySmall)};SuitabilityBadge(p,target)}}}}},confirmButton={TextButton({slot=null}){Text("Schließen")}})}
}

@Composable fun ProfileScreen(w: World,id: Int,vm: GameViewModel,onBack: ()->Unit){val p=w.players[id]?:return;var release by remember{mutableStateOf(false)}
 Page(p.name,if(id==w.user.playerId)"DU · SPIELERTRAINER" else "SPIELERPROFIL"){
  Section{Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){PlayerPortrait(p.appearance,w.club().kits.home);Column{Text("${p.position.label} · Nr. ${p.number}");Text("${w.calendar.season-p.birthYear} Jahre · ${p.nationality}",color=Muted);Text("${p.height} cm · ${p.weight} kg · ${p.foot.label}",color=Muted)}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Stärke","${p.ca}");Metric("Tore","${p.stats.goals}");Metric("Vorlagen","${p.stats.assists}")};Meter("Fitness",p.fitness.roundToInt());Meter("Moral",p.morale);Meter("Schärfe",p.sharpness);if(p.injuryWeeks>0)Text("${p.injury} · ${p.injuryWeeks} Wochen",color=Clay);if(p.unavailableWeeks>0)Text("${p.unavailableReason?.label} · ${p.unavailableWeeks} Spieltag(e)",color=Clay)}
  Section("Positionsprofil"){Position.entries.sortedByDescending{p.ratingAt(it)}.take(6).forEach{pos->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("${pos.label} · ${p.roleAt(pos)}",color=if(pos==p.position)Chalk else Muted);SuitabilityBadge(p,pos)}}}
  if(p.clubId==w.user.clubId)Section("Matchrolle"){Pick("Rolle",p.role,PlayerRole.entries.toList(),{it.label}){v->vm.action{require(it.live==null){"Rolle erst nach dem Spiel ändern."};it.players.getValue(id).role=v}};Text("Die Rolle beeinflusst Entscheidungswege der Match-KI und nicht nur die angezeigte Position.",color=Muted,style=MaterialTheme.typography.bodySmall)}
  Section("Sichtbare Attribute"){p.attributes.values().forEach{(label,value)->Meter(label,value,99)}}
  Section("Einschätzung"){Text(if(w.user.difficulty==Difficulty.HARDCORE)"Potenzial wird auf dieser Schwierigkeit nicht angezeigt." else if(w.user.difficulty==Difficulty.SANDBOX)"Sandbox-Spielerwerte sind frei gesetzt. Geschätztes Potenzial: ${p.hidden.potential}" else "Geschätztes Potenzial: ${if(w.user.difficulty==Difficulty.CASUAL)p.hidden.potential.toString() else "${(p.hidden.potential-5).coerceAtLeast(p.ca)}–${(p.hidden.potential+5).coerceAtMost(99)}"}");Text("Form ${dec(p.form)} · ${p.stats.appearances} Einsätze · ${p.stats.minutes} Minuten",color=Muted);Text("Spielertyp: ${p.personalityType(w.calendar.season).label} · Matchrolle: ${p.effectiveRole().label}",color=Gold);if(p.youth){Text("Entwicklung: ${p.youthProfile.path.label} · Lernfähigkeit ${p.youthProfile.learning} · Profibereitschaft ${YouthEngine.readiness(w,p)}/100",color=Muted);Text(YouthEngine.riskLabel(w,p),color=if(YouthEngine.readiness(w,p)>=65)Grass else Clay)}}
  if(id!=w.user.playerId&&p.clubId==w.user.clubId)Section("Unter vier Augen"){val talk=p.lastTalkWeek!=w.calendar.absoluteWeek;Action("Mehr zeigen",talk,true){vm.action{ClubActions.talk(it,id,0)}};Action("Chance geben",talk,true){vm.action{ClubActions.talk(it,id,1)}};Action("Ehrlich sein",talk,true){vm.action{ClubActions.talk(it,id,2)}};if(!talk)Text("Ihr habt diese Woche bereits gesprochen.",color=Muted);Action("Gehen lassen",w.live==null,true){release=true}}
  if(p.youth)Action("In den Herrenkader hochziehen",w.live==null){vm.action{ClubActions.promote(it,id)}};Action("Zurück",secondary=true,onClick=onBack)
 }
 if(release)Confirm("${p.lastName} freistellen?","Er verlässt deinen Verein ablösefrei.",{release=false}){release=false;vm.action{ClubActions.release(it,id)}}
}

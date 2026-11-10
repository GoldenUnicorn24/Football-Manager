package de.gruenderelf.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*

/* ───────────────────────── Spieltag ───────────────────────── */

/** Gegnervorschau: Tabellenplatz, Form, Stärkevergleich, voraussichtliche Elf. */
@Composable fun OpponentPreview(w: World,f: Fixture){
 val home=f.homeId==w.user.clubId;val opp=w.clubs.getValue(if(home)f.awayId else f.homeId);val own=w.club()
 val week=w.calendar.absoluteWeek
 val ownStrength=remember(week,own.id){BoardSystem.strength(w,own.id)};val oppStrength=remember(week,opp.id){BoardSystem.strength(w,opp.id)}
 val rank=remember(week,opp.id){WorldFactory.table(w,opp.tier).indexOfFirst{it.clubId==opp.id}+1}
 val expected=remember(week,opp.id){
  val pool=w.squad(opp.id).filter{it.available}.toMutableList()
  Formations.positions(opp.tactics.formation).mapNotNull{pos->val best=pool.maxByOrNull{it.ratingAt(pos)*(.6+it.fitness*.004)};if(best!=null)pool.remove(best);best?.let{pos to it}}
 }
 Section("Gegner"){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Crest(opp.logo,opp.primary,opp.secondary,Modifier.size(44.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
    Text(opp.name,style=MaterialTheme.typography.titleMedium,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis)
    Text("${if(rank>0)"Platz $rank · " else ""}${WorldFactory.leagueName(w,opp.tier)} · ${opp.tactics.formation}",color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
   }
   if(opp.form.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){opp.form.takeLast(5).forEach{FormDot(it)}}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   StatTile("Stärke ${own.shortName}",dec(ownStrength),Modifier.weight(1f),accent=if(ownStrength>=oppStrength)Grass else Chalk)
   StatTile("Stärke ${opp.shortName}",dec(oppStrength),Modifier.weight(1f),accent=if(oppStrength>ownStrength)Clay else Chalk)
  }
  if(expected.isNotEmpty()){
   Text("VORAUSSICHTLICHE ELF",color=Muted,style=MaterialTheme.typography.labelSmall)
   Text(expected.joinToString(" · "){(pos,p)->"${pos.name} ${p.lastName}"},color=Chalk.copy(alpha=.88f),style=MaterialTheme.typography.bodySmall)
   val stars=expected.map{it.second}.sortedByDescending{it.ca}.take(3)
   stars.forEach{p->
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Pill(p.position.name,Gold)
     Text(p.name,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis)
     Text("${p.ca}",style=MaterialTheme.typography.labelLarge,color=Chalk,fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}

private fun pressHint(s: PressStance)=when(s){
 PressStance.CONFIDENT->"Moral +3. Ein Sieg stärkt dich beim Vorstand, eine Niederlage schadet."
 PressStance.HUMBLE->"Kein Risiko. Ein Sieg wirkt leicht positiv."
 PressStance.PROTECT->"Moral +2. Der Vorstand sieht es kritisch."
 else->""
}

/** Eine Entscheidung vor jedem Ligaspiel. */
@Composable fun PressConference(w: World,f: Fixture,vm: GameViewModel){
 if(f.competition!=CompetitionType.LEAGUE)return
 val held=w.life.pressFixtureId==f.id
 if(held&&w.life.pressStance==PressStance.NONE)return
 Section("Pressekonferenz"){
  if(held)ListRow(w.life.pressStance.label,w.life.pressStance.line,Icons.Filled.Check,Grass)
  else listOf(PressStance.CONFIDENT,PressStance.HUMBLE,PressStance.PROTECT).forEach{s->
   ListRow(s.label,pressHint(s)){vm.action{MatchdaySystem.holdPress(it,s)}}
  }
 }
}

/** In der Halbzeit: einmalige Ansprache, wirkt über die Moral der Elf auf dem Platz. */
@Composable fun TeamTalkSection(w: World,vm: GameViewModel){
 if(!MatchdaySystem.canTalk(w))return
 Section("Kabinenansprache"){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TeamTalk.entries.forEach{t->Box(Modifier.weight(1f)){Action(t.label,secondary=true){vm.teamTalk(t)}}}
  }
 }
}

/* ───────────────────────── Vorstand & Saison ───────────────────────── */

/** Nach der Entlassung: Der nächste Verein muss gewählt werden. */
@Composable fun DismissalDialog(w: World,busy: Boolean,onAccept: (Int)->Unit){
 AlertDialog(onDismissRequest={},
  title={Text("Entlassen",style=MaterialTheme.typography.headlineSmall)},
  text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("${w.club().name} trennt sich von dir. Wähle deinen nächsten Verein.",color=Muted)
   w.life.board.jobOffers.mapNotNull{w.clubs[it]}.forEach{c->
    Surface(onClick={onAccept(c.id)},enabled=!busy,shape=RoundedCornerShape(14.dp),color=Inset,border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){
     Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
      Crest(c.logo,c.primary,c.secondary,Modifier.size(40.dp))
      Column(Modifier.weight(1f)){
       Text(c.name,style=MaterialTheme.typography.titleMedium,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis)
       Text(WorldFactory.leagueName(w,c.tier),color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
      }
      Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,contentDescription=null,tint=Faint)
     }
    }
   }
  }},
  confirmButton={},shape=RoundedCornerShape(26.dp),containerColor=PanelHigh,
  properties=DialogProperties(dismissOnBackPress=false,dismissOnClickOutside=false))
}

@Composable fun SeasonReviewDialog(r: SeasonReview,onClose: ()->Unit){
 val color=when(r.outcome){"Aufstieg"->Grass;"Abstieg"->Clay;else->Chalk}
 AlertDialog(onDismissRequest=onClose,
  title={Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
   Eyebrow("SAISON ${r.season}/${(r.season+1)%100} · ${r.league.uppercase()}")
   Text("Platz ${r.rank}",style=MaterialTheme.typography.displaySmall,color=color)
  }},
  text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Pill(r.outcome,if(r.outcome=="Verbleib")Muted else color)
    if(r.expectedRank>0)Text("Erwartet war Platz ${r.expectedRank}",color=Muted,style=MaterialTheme.typography.bodySmall)
   }
   Row(Modifier.fillMaxWidth()){
    Metric("Punkte","${r.points}",Modifier.weight(1f))
    Metric("Tore","${r.goalsFor}:${r.goalsAgainst}",Modifier.weight(1f))
   }
   Row(Modifier.fillMaxWidth()){
    Metric("S · U · N","${r.won} · ${r.drawn} · ${r.lost}",Modifier.weight(1f))
    Metric("Kasse",euros(r.budget),Modifier.weight(1f))
   }
   if(r.topScorer.isNotBlank()||r.topAssist.isNotBlank()){
    HairlineDivider()
    if(r.topScorer.isNotBlank())Text(r.topScorer,color=Chalk,style=MaterialTheme.typography.bodyMedium)
    if(r.topAssist.isNotBlank())Text(r.topAssist,color=Chalk,style=MaterialTheme.typography.bodyMedium)
   }
   if(r.awards.isNotEmpty()){
    HairlineDivider()
    r.awards.forEach{a->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Icon(Icons.Filled.Star,contentDescription=null,tint=Gold,modifier=Modifier.size(18.dp))
     Text(a,color=Gold,style=MaterialTheme.typography.bodyMedium)
    }}
   }
  }},
  confirmButton={Button(onClose,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Grass,contentColor=OnAccent)){Text("Neue Saison",fontWeight=FontWeight.Bold)}},
  shape=RoundedCornerShape(26.dp),containerColor=PanelHigh)
}

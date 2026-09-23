package de.gruenderelf.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*

/** Drawn vector artwork stays available offline, including on old saves. */
@Composable fun TrophyIllustration(modifier:Modifier=Modifier.size(88.dp),major:Boolean=false){
 val gold=if(major)Color(0xFFFFD475) else Color(0xFFD7B36C)
 Canvas(modifier){val w=size.width;val h=size.height
  drawCircle(gold.copy(alpha=.12f),w*.46f,Offset(w*.5f,h*.5f))
  val cup=Path().apply{moveTo(w*.25f,h*.14f);lineTo(w*.75f,h*.14f);lineTo(w*.69f,h*.53f);quadraticBezierTo(w*.62f,h*.66f,w*.5f,h*.66f);quadraticBezierTo(w*.38f,h*.66f,w*.31f,h*.53f);close()}
  drawPath(cup,gold)
  drawPath(cup,Color.White.copy(alpha=.45f),style=Stroke(w*.018f))
  drawArc(gold,80f,190f,false,Offset(w*.09f,h*.19f),Size(w*.32f,h*.33f),style=Stroke(w*.06f))
  drawArc(gold,-90f,190f,false,Offset(w*.59f,h*.19f),Size(w*.32f,h*.33f),style=Stroke(w*.06f))
  drawRect(gold,Offset(w*.46f,h*.66f),Size(w*.08f,h*.15f))
  drawRoundRect(gold,Offset(w*.31f,h*.81f),Size(w*.38f,h*.08f),androidx.compose.ui.geometry.CornerRadius(w*.02f))
  drawCircle(Color.White.copy(alpha=.55f),w*.025f,Offset(w*.42f,h*.28f))
 }
}

@Composable fun TrophyScreen(w:World,vm:GameViewModel){
 val owned=w.trophies.filter{it.clubId==w.user.clubId}.sortedWith(compareByDescending<Trophy>{it.season}.thenBy{it.competition})
 Page("Trophäenschrank","DEINE TITEL & POKALE"){
  Section("Vereinschronik"){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
    TrophyIllustration(Modifier.size(96.dp),true)
    Column{Text("${owned.size} Titel",style=MaterialTheme.typography.headlineMedium,color=Gold);Text(w.club().name,color=Muted);Text("Seit Karrierebeginn · offline gespeichert",color=Muted,style=MaterialTheme.typography.bodySmall)}
   }
  }
  if(owned.isEmpty())Section("Der erste Pokal wartet"){Text("Nach einem gewonnenen Finale oder einer Meisterschaft erscheinen hier Saison, Wettbewerb und Preisgeld.",color=Muted)}
  owned.groupBy{it.season}.forEach{(year,trophies)->
   Section("Saison $year/${(year+1)%100}"){
    trophies.forEach{t->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
     TrophyIllustration(Modifier.size(68.dp),t.competition.contains("Champions")||t.competition.contains("Welt")||t.competition.contains("Krone"))
     Column(Modifier.weight(1f)){Text(t.competition,style=MaterialTheme.typography.titleMedium,color=Chalk);Text(if(t.prize>0)"Preisgeld ${euros(t.prize)}" else "Älterer Titel · Preisgeld nicht überliefert",color=Grass)}
    };HorizontalDivider(color=Muted.copy(alpha=.2f))}
   }
  }
  Section("Krone der Kontinente"){
   Text("Platz 1–6 aus jeder enthaltenen Liga treten gegeneinander an. Der Sieger erhält 50 Mio. €.",color=Muted)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Text(if(w.fantasyCupEnabled)"Nächste Saison: aktiviert" else "Nächste Saison: deaktiviert",Modifier.weight(1f))
    Switch(w.fantasyCupEnabled,{enabled->vm.action("Turniereinstellung gespeichert."){it.fantasyCupEnabled=enabled}})
   }
   Text("Die laufende Saison und ihre bereits angesetzten Partien bleiben bestehen.",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
 }
}

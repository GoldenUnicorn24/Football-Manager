package de.gruenderelf.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruenderelf.engine.*
import kotlin.math.max
import kotlin.math.roundToInt

private data class AnalysisSnapshot(
 val homeId:Int,
 val awayId:Int,
 val minute:Int,
 val home:MatchStats,
 val away:MatchStats,
 val shots:List<ShotEvent>,
 val passes:List<PassEvent>,
 val changes:List<TacticChangeEvent>
)

@Composable
fun MatchAnalysisPanel(w:World,m:LiveMatch,title:String="Analyse"){
 MatchAnalysisContent(w,AnalysisSnapshot(m.homeId,m.awayId,m.minute,m.home,m.away,m.shotEvents,m.passEvents,m.tacticChanges),title)
}

@Composable
fun MatchAnalysisPanel(w:World,m:MatchRecord,title:String="Analyse & Taktik-Impact"){
 MatchAnalysisContent(w,AnalysisSnapshot(m.homeId,m.awayId,m.minute,m.home,m.away,m.shotEvents,m.passEvents,m.tacticChanges),title)
}

@Composable
private fun MatchAnalysisContent(w:World,s:AnalysisSnapshot,title:String){
 val home=w.clubs.getValue(s.homeId)
 val away=w.clubs.getValue(s.awayId)
 val ownId=w.user.clubId.takeIf{it==s.homeId||it==s.awayId}?:s.homeId
 val own=w.clubs.getValue(ownId)
 val ownStats=if(ownId==s.homeId)s.home else s.away
 val oppStats=if(ownId==s.homeId)s.away else s.home

 Section(title){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Metric("xG ${own.shortName}",String.format(java.util.Locale.GERMANY,"%.2f",ownStats.xg))
   Metric("Gegner-xG",String.format(java.util.Locale.GERMANY,"%.2f",oppStats.xg))
  }
  Text("Statistik zeigt Matchwerte. Analyse nutzt echte Ereignisse für xG, Schusskarte, Passnetz und die Wirkung deiner Taktikänderungen. xG beschreibt Chancenqualität und ist keine Garantie für Tore.",color=Muted,style=MaterialTheme.typography.bodySmall)
 }

 XgTimelineCard(s,home,away)
 ShotMapCard(w,s,home,away)
 PassNetworkCard(w,s,ownId)
 TacticImpactCard(s,ownId)
}

@Composable
private fun XgTimelineCard(s:AnalysisSnapshot,home:Club,away:Club){
 val homeLine=MatchAnalysisSystem.xgTimeline(s.minute,s.shots,s.homeId)
 val awayLine=MatchAnalysisSystem.xgTimeline(s.minute,s.shots,s.awayId)
 Section("xG-Verlauf"){
  if(homeLine.isEmpty()&&awayLine.isEmpty())Text("Noch keine xG-Daten.",color=Muted)
  else{
   val count=maxOf(homeLine.size,awayLine.size)
   repeat(count){i->
    val homePoint=homeLine.getOrNull(i)?:homeLine.lastOrNull()?:return@repeat
    val awayPoint=awayLine.getOrNull(i)?:awayLine.lastOrNull()
    val minute=homePoint.first
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     Text("$minute'",color=Muted,modifier=Modifier.width(44.dp))
     Text("${home.shortName} ${String.format(java.util.Locale.GERMANY,"%.2f",homePoint.second)}",color=Color(home.primary),fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
     Text("${away.shortName} ${String.format(java.util.Locale.GERMANY,"%.2f",awayPoint?.second?:0.0)}",color=Color(away.primary),fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}

private fun shotResultText(o:ShotOutcome)=when(o){
 ShotOutcome.GOAL->"Tor"
 ShotOutcome.SAVED,ShotOutcome.DEFLECTED,ShotOutcome.CORNER,ShotOutcome.REBOUND->"aufs Tor"
 ShotOutcome.WOODWORK->"Aluminium"
 else->"daneben/kein SOT"
}

private fun isShotOnTarget(o:ShotOutcome)=o in listOf(ShotOutcome.GOAL,ShotOutcome.SAVED,ShotOutcome.DEFLECTED,ShotOutcome.CORNER,ShotOutcome.REBOUND)

private fun shotColor(o:ShotOutcome)=when(o){
 ShotOutcome.GOAL->Grass
 ShotOutcome.SAVED,ShotOutcome.DEFLECTED,ShotOutcome.CORNER,ShotOutcome.REBOUND->Gold
 else->Clay
}

@Composable
private fun ShotMapCard(w:World,s:AnalysisSnapshot,home:Club,away:Club){
 Section("Schusskarte · beide Teams"){
  Text("Links greift ${home.shortName} an · rechts greift ${away.shortName} an. Jede Linie zeigt die tatsächliche Schussrichtung bis zum Ziel am Tor.",color=Muted,style=MaterialTheme.typography.bodySmall)
  if(s.shots.isEmpty()) Text("Noch kein Abschluss im Spiel.",color=Muted)
  else{
   Canvas(Modifier.fillMaxWidth().aspectRatio(1.72f)){
    val pitch=Color(0xFF064A31)
    val line=Color.White.copy(alpha=.43f)
    drawRoundRect(pitch,cornerRadius=androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(),18.dp.toPx()))
    val inset=14.dp.toPx()
    val l=inset;val t=inset;val r=size.width-inset;val btm=size.height-inset
    drawRect(line,Offset(l,t),androidx.compose.ui.geometry.Size(r-l,btm-t),style=Stroke(1.25.dp.toPx()))
    drawLine(line,Offset(size.width/2,t),Offset(size.width/2,btm),1.25.dp.toPx())
    drawCircle(line,(btm-t)*.15f,Offset(size.width/2,(t+btm)/2),style=Stroke(1.25.dp.toPx()))
    drawCircle(line,2.4.dp.toPx(),Offset(size.width/2,(t+btm)/2))
    val boxW=(r-l)*.16f;val boxH=(btm-t)*.55f
    val sixW=(r-l)*.055f;val sixH=(btm-t)*.27f
    drawRect(line,Offset(l,(size.height-boxH)/2),androidx.compose.ui.geometry.Size(boxW,boxH),style=Stroke(1.25.dp.toPx()))
    drawRect(line,Offset(r-boxW,(size.height-boxH)/2),androidx.compose.ui.geometry.Size(boxW,boxH),style=Stroke(1.25.dp.toPx()))
    drawRect(line,Offset(l,(size.height-sixH)/2),androidx.compose.ui.geometry.Size(sixW,sixH),style=Stroke(1.25.dp.toPx()))
    drawRect(line,Offset(r-sixW,(size.height-sixH)/2),androidx.compose.ui.geometry.Size(sixW,sixH),style=Stroke(1.25.dp.toPx()))
    fun pos(x:Float,y:Float)=Offset(l+y.coerceIn(0f,1f)*(r-l),t+x.coerceIn(0f,1f)*(btm-t))
    val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.White.toArgb();textSize=10.dp.toPx();isFakeBoldText=true}
    s.shots.forEach{shot->
     val start=pos(shot.x,shot.y)
     val team=if(shot.clubId==s.homeId)home else away
     val teamColor=Color(team.primary)
     val goalX=if(shot.clubId==s.homeId)l else r
     val goalY=(size.height*.5f)+(shot.targetX.coerceIn(0f,1f)-.5f)*size.height*.22f
     val end=Offset(goalX,goalY)
     drawLine(teamColor.copy(alpha=.58f),start,end,(1.0f+(shot.xg*2.8).toFloat()).dp.toPx())
     drawCircle(teamColor,2.3.dp.toPx(),end)
     val radius=(8f+(shot.xg*4.5).toFloat()).dp.toPx()
     drawCircle(Color(0xFFF7F7F2),radius,start)
     drawCircle(teamColor,radius,start,style=Stroke(2.6.dp.toPx()))
     when{
      shot.outcome==ShotOutcome.GOAL->{
       drawCircle(Color.Black,radius*.43f,start,style=Stroke(1.4.dp.toPx()))
       drawCircle(Color.Black,radius*.12f,start)
      }
      isShotOnTarget(shot.outcome)->drawCircle(Color.Black,radius*.15f,start)
     }
     val player=w.players[shot.playerId]
     val label="#${player?.number?:"?"} ${team.shortName}"
     val labelX=(start.x+radius+4.dp.toPx()).coerceAtMost(size.width-textPaint.measureText(label)-2.dp.toPx())
     val labelY=(start.y-textPaint.textSize*.55f).coerceAtLeast(textPaint.textSize)
     drawContext.canvas.nativeCanvas.drawText(label,labelX,labelY,textPaint)
    }
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
    Text("◉ Tor",color=Grass)
    Text("⊙ aufs Tor",color=Gold)
    Text("○ daneben/kein SOT",color=Muted)
   }
   s.shots.takeLast(8).asReversed().forEach{shot->
    val club=w.clubs[shot.clubId]
    val player=w.players[shot.playerId]
    val context=ShotContext(shot.x,shot.y,shot.clubId==s.homeId,shot.type,shot.pressure,shot.defendersNearby,shot.passQuality)
    val clock=shot.clockLabel.ifBlank{"${shot.minute}'"}
    val who=player?.let{"#${it.number} ${it.lastName}"}?:"Spieler"
    Text("$clock · ${club?.shortName?:"-"} $who · ${shotResultText(shot.outcome)} · ${ShotModel.locationLabel(context)} · ${shot.targetLabel.ifBlank{shot.outcome.label}} · xG ${String.format(java.util.Locale.GERMANY,"%.1f",shot.xg)}",color=Muted,style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}

@Composable
private fun PassNetworkCard(w:World,s:AnalysisSnapshot,clubId:Int){
 val club=w.clubs.getValue(clubId)
 val relevant=s.passes.filter{it.clubId==clubId}
 val completed=relevant.count{it.completed}
 val pct=if(relevant.isEmpty())0 else (100.0*completed/relevant.size).roundToInt()
 val nodes=MatchAnalysisSystem.passNetworkNodes(s.passes,clubId)
 val edges=MatchAnalysisSystem.passNetworkEdges(s.passes,clubId)
 Section("Passnetz · ${club.shortName}"){
  Text("$completed/${relevant.size} angekommene Analyse-Pässe · $pct % · Positionen werden aus den tatsächlich protokollierten Pässen gemittelt.",color=Muted,style=MaterialTheme.typography.bodySmall)
  if(nodes.isEmpty()) Text("Noch nicht genug angekommene Pässe für ein Passnetz.",color=Muted)
  else{
   Canvas(Modifier.fillMaxWidth().aspectRatio(1.72f)){
    val line=Color.White.copy(alpha=.32f)
    drawRect(Color(0xFF123C2B))
    drawRect(line,style=Stroke(1.dp.toPx()))
    drawLine(line,Offset(size.width/2,0f),Offset(size.width/2,size.height),1.dp.toPx())
    drawCircle(line,size.height*.15f,Offset(size.width/2,size.height/2),style=Stroke(1.dp.toPx()))
    fun p(x:Float,y:Float)=Offset(y.coerceIn(0f,1f)*size.width,x.coerceIn(0f,1f)*size.height)
    edges.forEach{e->
     val a=p(e.startX,e.startY);val b=p(e.endX,e.endY)
     val strength=(e.count.coerceAtMost(8)/8f)
     drawLine(Color(club.primary).copy(alpha=.22f+.55f*strength),a,b,(1f+3f*strength).dp.toPx())
    }
    val maxTouches=(nodes.maxOfOrNull{it.touches}?:1).coerceAtLeast(1)
    val paint=Paint().apply{color=Color.White.toArgb();textAlign=Paint.Align.CENTER;textSize=11.dp.toPx();isFakeBoldText=true}
    nodes.forEach{n->
     val c=p(n.x,n.y);val r=(7f+7f*n.touches/maxTouches.toFloat()).dp.toPx()
     drawCircle(Color(club.primary),r,c);drawCircle(Color.White.copy(alpha=.7f),r,c,style=Stroke(1.dp.toPx()))
     val label=w.players[n.playerId]?.number?.toString()?:"?"
     drawContext.canvas.nativeCanvas.drawText(label,c.x,c.y+paint.textSize*.34f,paint)
    }
   }
   val leaders=nodes.take(6).mapNotNull{n->w.players[n.playerId]?.let{"#${it.number} ${it.lastName} · ${n.touches} Kontakte"}}
   if(leaders.isNotEmpty())Text(leaders.joinToString("  ·  "),color=Muted,style=MaterialTheme.typography.bodySmall)
  }
 }
}

@Composable
private fun TacticImpactCard(s:AnalysisSnapshot,clubId:Int){
 val stats=if(clubId==s.homeId)s.home else s.away
 val changes=s.changes.filter{it.clubId==clubId}.sortedBy{it.minute}
 Section("Taktik-Impact"){
  if(changes.isEmpty())Text("Noch keine Live-Taktikänderung erfasst.",color=Muted)
  else{
   val last=changes.last()
   Text("Seit ${last.minute}. Min (${last.label}): ${signed(stats.xg-last.xg)} xG · ${signed(stats.shots-last.shots)} Schüsse · ${signed(stats.possessionTicks-last.possessionTicks)} Ballbesitz-Ticks",fontWeight=FontWeight.Bold)
   changes.forEachIndexed{i,ch->
    val next=changes.getOrNull(i+1)
    val end=next?.minute?:s.minute
    val endXg=next?.xg?:stats.xg
    val endShots=next?.shots?:stats.shots
    Text("${ch.minute}–$end' · ${ch.label}: ${signed(endXg-ch.xg)} xG · ${signed(endShots-ch.shots)} Schüsse",color=Muted,style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}

private fun signed(v:Double):String {
 val rounded=String.format(java.util.Locale.GERMANY,"%.2f",kotlin.math.abs(v))
 return (if(v>=0) "+" else "−")+rounded
}
private fun signed(v:Int):String=(if(v>=0) "+" else "−")+kotlin.math.abs(v)

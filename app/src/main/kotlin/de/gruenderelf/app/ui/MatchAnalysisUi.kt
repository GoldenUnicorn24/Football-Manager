package de.gruenderelf.app.ui

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruenderelf.engine.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ShotMapKind { GOAL, ON_TARGET, OFF_TARGET }

private fun shotMapKind(outcome:ShotOutcome)=when(outcome){
 ShotOutcome.GOAL->ShotMapKind.GOAL
 ShotOutcome.SAVED,ShotOutcome.CORNER,ShotOutcome.DEFLECTED,ShotOutcome.REBOUND->ShotMapKind.ON_TARGET
 else->ShotMapKind.OFF_TARGET
}

/** Engine: x = Querachse, y = Längsachse. Analyse-Pitch: Tore links/rechts. */
private fun DrawScope.pitchPoint(longitudinal:Float,lateral:Float):Offset{
 val pad=8.dp.toPx();val w=size.width-pad*2;val h=size.height-pad*2
 return Offset(pad+longitudinal.coerceIn(-.035f,1.035f)*w,pad+lateral.coerceIn(0f,1f)*h)
}

private fun DrawScope.drawPitch(){
 val line=Chalk.copy(alpha=.42f);val pad=8.dp.toPx();val fieldW=size.width-pad*2;val fieldH=size.height-pad*2
 drawRect(line,Offset(pad,pad),Size(fieldW,fieldH),style=Stroke(1.6.dp.toPx()))
 drawLine(line,pitchPoint(.5f,0f),pitchPoint(.5f,1f),1.4.dp.toPx())
 drawCircle(line,fieldH*(9.15f/68f),pitchPoint(.5f,.5f),style=Stroke(1.4.dp.toPx()))
 drawCircle(line,2.2.dp.toPx(),pitchPoint(.5f,.5f))
 val boxDepth=16.5f/105f;val boxTop=(1f-40.32f/68f)/2f;val boxHeight=40.32f/68f
 drawRect(line,pitchPoint(0f,boxTop),Size(fieldW*boxDepth,fieldH*boxHeight),style=Stroke(1.4.dp.toPx()))
 drawRect(line,pitchPoint(1f-boxDepth,boxTop),Size(fieldW*boxDepth,fieldH*boxHeight),style=Stroke(1.4.dp.toPx()))
 val sixDepth=5.5f/105f;val sixTop=(1f-18.32f/68f)/2f;val sixHeight=18.32f/68f
 drawRect(line,pitchPoint(0f,sixTop),Size(fieldW*sixDepth,fieldH*sixHeight),style=Stroke(1.2.dp.toPx()))
 drawRect(line,pitchPoint(1f-sixDepth,sixTop),Size(fieldW*sixDepth,fieldH*sixHeight),style=Stroke(1.2.dp.toPx()))
 val goalTop=(1f-7.32f/68f)/2f;val goalHeight=7.32f/68f
 drawRect(line,pitchPoint(-.018f,goalTop),Size(fieldW*.018f,fieldH*goalHeight),style=Stroke(1.5.dp.toPx()))
 drawRect(line,pitchPoint(1f,goalTop),Size(fieldW*.018f,fieldH*goalHeight),style=Stroke(1.5.dp.toPx()))
}

private fun shotTargetPoint(s:ShotEvent,homeShot:Boolean):Pair<Float,Float>{
 val goalWidth=7.32f/68f
 var lateral=.5f+(s.targetX-.5f)*goalWidth
 if(!homeShot)lateral=1f-lateral
 val over=s.targetY>1f
 val longitudinal=if(homeShot){if(over)-.025f else 0f}else{if(over)1.025f else 1f}
 return longitudinal to lateral
}

private fun DrawScope.drawFootballMarker(center:Offset,r:Float,kind:ShotMapKind,teamColor:Color){
 drawCircle(Color(0xFFF7F8F6),r,center)
 drawCircle(teamColor,r,center,style=Stroke(2.1.dp.toPx()))
 when(kind){
  ShotMapKind.GOAL->{
   val inner=r*.34f;val hex=Path()
   for(i in 0 until 6){val angle=-PI/2+i*PI/3;val pt=Offset(center.x+(cos(angle)*inner).toFloat(),center.y+(sin(angle)*inner).toFloat());if(i==0)hex.moveTo(pt.x,pt.y)else hex.lineTo(pt.x,pt.y)}
   hex.close();drawPath(hex,Color(0xFF16201B))
   for(i in 0 until 5){val a=-PI/2+i*2*PI/5;val p=Offset(center.x+(cos(a)*r*.67).toFloat(),center.y+(sin(a)*r*.67).toFloat());drawCircle(Color(0xFF16201B),r*.12f,p);drawLine(Color(0xFF16201B).copy(alpha=.75f),center,p,.75.dp.toPx())}
  }
  ShotMapKind.ON_TARGET->drawCircle(Color(0xFF16201B),r*.22f,center)
  ShotMapKind.OFF_TARGET->Unit
 }
}

@Composable private fun AnalysisPitch(content:DrawScope.()->Unit){
 Canvas(Modifier.fillMaxWidth().aspectRatio(105f/68f).background(Color(0xFF123B27),RoundedCornerShape(18.dp)).padding(6.dp)){
  drawPitch();content()
 }
}

@Composable fun MatchAnalysisView(w:World,m:LiveMatch,title:String="Matchanalyse"){
 val home=w.clubs.getValue(m.homeId);val away=w.clubs.getValue(m.awayId);val userId=w.user.clubId
 val homeColor=Color(home.primary);val awayColor=Color(away.primary)
 val shotKey=listOf(m.shotEvents.size,m.pendingVarShotIndex,m.home.goals,m.away.goals)
 val shots=remember(shotKey){m.shotEvents.toList()}
 val hxg=remember(m.shotEvents.size,m.minute/5){MatchAnalysisSystem.xgByIntervals(m,m.homeId)}
 val axg=remember(m.shotEvents.size,m.minute/5){MatchAnalysisSystem.xgByIntervals(m,m.awayId)}
 val passEdges=remember(m.passEvents.size,userId){MatchAnalysisSystem.passNetworkEdges(m,userId).take(22)}
 val passNodes=remember(m.passEvents.size,userId){MatchAnalysisSystem.passNetworkNodes(m,userId).take(16)}
 val completedPasses=remember(m.passEvents.size,userId){MatchAnalysisSystem.completedPasses(m,userId)}
 val attemptedPasses=remember(m.passEvents.size,userId){MatchAnalysisSystem.attemptedPasses(m,userId)}
 Section(title){
  Text("xG-Verlauf",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  hxg.indices.forEach{i->val minute=hxg[i].first;val awayAt=axg.getOrNull(i)?.second?:axg.lastOrNull()?.second?:0.0;Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("$minute'",color=Muted);Text("${home.shortName} ${dec(hxg[i].second)}",color=homeColor);Text("${away.shortName} ${dec(awayAt)}",color=awayColor)}}
  HorizontalDivider()

  Text("Schusskarte · beide Teams",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  Text("Links greift ${home.shortName} an · rechts greift ${away.shortName} an. Jede Linie zeigt die tatsächliche Schussrichtung bis zum Ziel am Tor.",color=Muted,style=MaterialTheme.typography.bodySmall)
  AnalysisPitch{
   val labelPaint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=8.5.dp.toPx();typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)}
   val shadowPaint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.argb(190,0,0,0);textSize=8.5.dp.toPx();typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)}
   shots.takeLast(90).forEach{s->
    val homeShot=s.clubId==m.homeId;val teamColor=if(homeShot)homeColor else awayColor;val club=if(homeShot)home else away;val player=w.players[s.playerId]
    val origin=pitchPoint(s.y,s.x);val targetRaw=shotTargetPoint(s,homeShot);val target=pitchPoint(targetRaw.first,targetRaw.second)
    drawLine(teamColor.copy(alpha=.56f),origin,target,1.35.dp.toPx())
    drawCircle(teamColor.copy(alpha=.92f),2.25.dp.toPx(),target)
    val kind=shotMapKind(s.outcome);val radius=7.2.dp.toPx();drawFootballMarker(origin,radius,kind,teamColor)
    val label="#${player?.number?:"?"} ${club.shortName}";val textWidth=labelPaint.measureText(label);val lx=if(homeShot)origin.x+radius+3.dp.toPx() else origin.x-radius-3.dp.toPx()-textWidth;val ly=(origin.y-radius*.75f).coerceIn(labelPaint.textSize,size.height-3f)
    drawContext.canvas.nativeCanvas.drawText(label,lx+1.2f,ly+1.2f,shadowPaint);drawContext.canvas.nativeCanvas.drawText(label,lx,ly,labelPaint)
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Canvas(Modifier.size(18.dp)){drawFootballMarker(center,7.dp.toPx(),ShotMapKind.GOAL,Grass)};Text("Tor",style=MaterialTheme.typography.bodySmall)}
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Canvas(Modifier.size(18.dp)){drawFootballMarker(center,7.dp.toPx(),ShotMapKind.ON_TARGET,Gold)};Text("aufs Tor",style=MaterialTheme.typography.bodySmall)}
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Canvas(Modifier.size(18.dp)){drawFootballMarker(center,7.dp.toPx(),ShotMapKind.OFF_TARGET,Muted)};Text("daneben/kein SOT",style=MaterialTheme.typography.bodySmall)}
  }
  if(shots.isEmpty())Text("Noch kein Abschluss im Spiel.",color=Muted) else shots.takeLast(12).asReversed().forEach{s->
   val club=w.clubs[s.clubId];val p=w.players[s.playerId];val where=ShotModel.locationLabel(ShotContext(s.x,s.y,s.clubId==m.homeId,s.type));val status=when(shotMapKind(s.outcome)){ShotMapKind.GOAL->"TOR";ShotMapKind.ON_TARGET->"aufs Tor";ShotMapKind.OFF_TARGET->s.outcome.label}
   Text("${s.clockLabel.ifBlank{s.minute.toString()+"'"}} · ${club?.shortName?:"?"} #${p?.number?:"?"} ${p?.lastName?:"?"} · $status · $where · ${s.targetLabel} · xG ${dec(s.xg)}",color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider()

  val userClub=w.clubs[userId];val userColor=userClub?.let{Color(it.primary)}?:Grass
  Text("Passnetz · ${userClub?.shortName?:"Dein Team"}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  val accuracy=if(attemptedPasses>0)completedPasses*100/attemptedPasses else 0
  Text("$completedPasses/$attemptedPasses angekommene Analyse-Pässe · $accuracy % · Positionen werden aus den tatsächlich protokollierten Pässen gemittelt.",color=Muted,style=MaterialTheme.typography.bodySmall)
  AnalysisPitch{
   passEdges.forEach{e->
    val a=pitchPoint(e.startY,e.startX);val b=pitchPoint(e.endY,e.endX);val alpha=(.12f+e.count*.025f).coerceAtMost(.70f);val width=(.8f+e.count*.18f).coerceAtMost(5.5f).dp.toPx();drawLine(userColor.copy(alpha=alpha),a,b,width)
   }
   val numberPaint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(20,28,23);textAlign=AndroidPaint.Align.CENTER;textSize=8.dp.toPx();typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)}
   passNodes.forEach{n->val pt=pitchPoint(n.y,n.x);drawCircle(Color(0xFFF4F6F2),8.dp.toPx(),pt);drawCircle(userColor,8.dp.toPx(),pt,style=Stroke(2.dp.toPx()));val nr=w.players[n.playerId]?.number?.toString()?:"?";drawContext.canvas.nativeCanvas.drawText(nr,pt.x,pt.y+numberPaint.textSize*.34f,numberPaint)}
  }
  if(passEdges.isEmpty())Text("Noch zu wenige abgeschlossene Pässe für ein belastbares Netz.",color=Muted) else passEdges.take(7).forEach{e->Text("${w.players[e.fromId]?.lastName?:"?"} → ${w.players[e.toId]?.lastName?:"?"}: ${e.count}",color=Muted,style=MaterialTheme.typography.bodySmall)}
  HorizontalDivider()

  Text("Taktik-Impact",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  Text(MatchAnalysisSystem.sinceLastChange(m,userId),color=if(m.tacticChanges.any{it.clubId==userId})Grass else Muted)
  MatchAnalysisSystem.tacticImpactRows(m,userId).takeLast(8).asReversed().forEach{row->Text(row,color=Muted,style=MaterialTheme.typography.bodySmall)}
  val userShots=shots.count{it.clubId==userId};val userXg=shots.filter{it.clubId==userId}.sumOf{it.xg};val oppId=if(userId==m.homeId)m.awayId else m.homeId;val oppXg=shots.filter{it.clubId==oppId}.sumOf{it.xg}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Metric("Eigene Schüsse","$userShots");Metric("xG",dec(userXg));Metric("Gegner-xG",dec(oppXg))}
 }
}

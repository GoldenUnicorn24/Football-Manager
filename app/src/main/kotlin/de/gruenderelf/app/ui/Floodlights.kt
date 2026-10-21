package de.gruenderelf.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.sin

/* ───────────────────────── Flutlicht ─────────────────────────
 * Gemeinsame Lichtsprache für Startbildschirm und Vereins-Header:
 * LED-Scheinwerferköpfe mit mehrstufigem Bloom, volumetrische Lichtkegel
 * mit weichen Kanten, schwebender Dunst und beleuchtete Lichtinseln auf dem Rasen.
 */

/** Lichtfarbe: neutral-warm bei Gründerelf, kühl-blau im BMW-FC-Modus. */
val FloodlightColor get()=if(UiBrand.bmwMode)Color(0xFFDDEBFF) else Color(0xFFFFF7E6)

/** Ein Scheinwerferkopf mit LED-Raster, mehrstufigem Bloom und Linsenreflex. */
fun DrawScope.floodlightHead(center: Offset,width: Float,intensity: Float=1f,light: Color=FloodlightColor){
 val h=width*.42f
 // Bloom: drei Lagen von weit und weich bis klein und heiß.
 for((radius,alpha) in listOf(width*3.4f to .14f,width*1.6f to .30f,width*.75f to .70f)){
  val r=radius.coerceAtLeast(1f)
  drawCircle(Brush.radialGradient(listOf(light.copy(alpha=alpha*intensity),Color.Transparent),center=center,radius=r),r,center)
 }
 // Linsenreflex: dünner horizontaler Streifen.
 val flare=width*1.9f
 drawOval(Brush.horizontalGradient(listOf(Color.Transparent,light.copy(alpha=.22f*intensity),Color.Transparent),startX=center.x-flare,endX=center.x+flare),Offset(center.x-flare,center.y-width*.05f),Size(flare*2,width*.10f),blendMode=BlendMode.Screen)
 // Gehäuse und LED-Raster (4 × 2).
 val tl=Offset(center.x-width/2,center.y-h/2)
 drawRoundRect(Color(0xFF0E1412),tl,Size(width,h),CornerRadius(h*.25f))
 val cols=4;val rows=2;val gap=width*.06f
 val cw=(width-gap*(cols+1))/cols;val ch=(h-gap*(rows+1))/rows
 for(r in 0 until rows)for(c in 0 until cols){
  val o=Offset(tl.x+gap+c*(cw+gap),tl.y+gap+r*(ch+gap))
  drawRoundRect(Color.White.copy(alpha=(.88f+.12f*intensity).coerceAtMost(1f)),o,Size(cw,ch),CornerRadius(ch*.3f))
 }
 // heißer Kern über dem Raster
 val core=(width*.55f).coerceAtLeast(1f)
 drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=.55f*intensity),Color.Transparent),center=center,radius=core),core,center,blendMode=BlendMode.Screen)
}

/** Mast mit Fachwerk-Andeutung, vom Scheinwerfer bis zum Boden. */
fun DrawScope.floodlightMast(top: Offset,bottomY: Float,width: Float){
 val mast=Color(0xFF26332D)
 drawLine(mast,Offset(top.x,top.y),Offset(top.x,bottomY),width)
 val step=(width*5f).coerceAtLeast(6f)
 var y=top.y+step
 while(y<bottomY){drawLine(mast.copy(alpha=.7f),Offset(top.x-width*1.6f,y),Offset(top.x+width*1.6f,y),width*.45f);y+=step}
}

/**
 * Volumetrischer Lichtkegel vom Scheinwerfer [from] auf die Bodenlinie zwischen [toLeft] und [toRight].
 * Mehrere überlagerte, nach außen breiter und schwächer werdende Lagen ergeben weiche Kanten.
 */
fun DrawScope.floodlightBeam(from: Offset,toLeft: Offset,toRight: Offset,strength: Float=1f,light: Color=FloodlightColor){
 val mid=Offset((toLeft.x+toRight.x)/2,(toLeft.y+toRight.y)/2)
 val layers=6
 for(i in layers downTo 0){
  val spread=1f+i*.16f
  val l=Offset(mid.x+(toLeft.x-mid.x)*spread,mid.y+(toLeft.y-mid.y)*spread)
  val r=Offset(mid.x+(toRight.x-mid.x)*spread,mid.y+(toRight.y-mid.y)*spread)
  val path=Path().apply{moveTo(from.x-2f,from.y);lineTo(from.x+2f,from.y);lineTo(r.x,r.y);lineTo(l.x,l.y);close()}
  val a=(if(i==0).13f else .05f)*strength
  drawPath(path,Brush.linearGradient(0f to light.copy(alpha=a*1.8f),.45f to light.copy(alpha=a*.7f),1f to light.copy(alpha=a*.12f),start=from,end=mid),blendMode=BlendMode.Screen)
 }
}

/** Beleuchtete Lichtinsel auf dem Rasen (elliptisch, im Screen-Modus). */
fun DrawScope.lightPool(center: Offset,radiusX: Float,radiusY: Float,strength: Float=1f,light: Color=FloodlightColor){
 val rx=radiusX.coerceAtLeast(1f);val ry=radiusY.coerceAtLeast(1f)
 scale(1f,ry/rx,center){
  drawCircle(Brush.radialGradient(listOf(light.copy(alpha=.16f*strength),Color.Transparent),center=center,radius=rx),rx,center,blendMode=BlendMode.Screen)
 }
}

/** Schwebender Dunst in den Lichtkegeln; [phase] 0..1 bewegt die Partikel langsam. */
fun DrawScope.floodlightHaze(region: List<Pair<Offset,Pair<Offset,Offset>>>,phase: Float,count: Int=26,light: Color=FloodlightColor){
 region.forEachIndexed{b,(from,ground)->
  for(i in 0 until count){
   // deterministische Pseudozufallszahlen pro Partikel
   val s1=frac(sin((i*12.9898f+b*78.233f).toDouble()).toFloat()*43758.547f)
   val s2=frac(sin((i*39.3468f+b*11.135f).toDouble()).toFloat()*24634.634f)
   val s3=frac(sin((i*73.156f+b*52.235f).toDouble()).toFloat()*12345.678f)
   val t=frac(s1+phase*(.25f+.5f*s3))
   val g=Offset(ground.first.x+(ground.second.x-ground.first.x)*s2,ground.first.y+(ground.second.y-ground.first.y)*s2)
   val p=Offset(from.x+(g.x-from.x)*t,from.y+(g.y-from.y)*t)
   val twinkle=.5f+.5f*sin((phase*2*PI+i).toFloat())
   drawCircle(light.copy(alpha=(.06f+.12f*s3)*twinkle*(1f-t*.6f)),(1.2f+1.8f*s3)*density,p)
  }
 }
}

private fun frac(x: Float)=x-kotlin.math.floor(x)

/** Langsam laufende Phase 0..1 für Dunst und dezentes Flackern. */
@Composable fun rememberFloodlightPhase(periodMs: Int=9000): Float{
 val t=rememberInfiniteTransition(label="flutlicht")
 val phase by t.animateFloat(0f,1f,infiniteRepeatable(tween(periodMs,easing=LinearEasing),RepeatMode.Restart),label="phase")
 return phase
}

/* ───────────────────────── Startbildschirm-Stadion ───────────────────────── */

/**
 * Nächtliches Stadion für den Startbildschirm: Sternenhimmel, Lichtverschmutzung,
 * geschwungene Tribüne mit Publikum, vier Flutlichtmasten, Rasen in Perspektive.
 * Der Übergang zum Inhalt ist eine eigene Abdunkelung (siehe [StadiumScrim]),
 * damit Text nie über Linien oder Kegeln liegt.
 */
@Composable fun NightStadium(modifier: Modifier){
 val phase=rememberFloodlightPhase()
 val light=FloodlightColor
 val turfTop=PitchGreenLight;val turfBottom=PitchGreen
 Canvas(modifier.semantics{contentDescription="Nächtliches Stadion unter Flutlicht"}){
  val w=size.width;val h=size.height
  // Himmel + Lichtglocke über dem Stadion
  drawRect(Brush.verticalGradient(listOf(Color(0xFF05080A),Color(0xFF0A1513),Color(0xFF0D1B17))))
  drawCircle(Brush.radialGradient(listOf(light.copy(alpha=.10f),Color.Transparent),center=Offset(w*.5f,h*.46f),radius=w*.9f),w*.9f,Offset(w*.5f,h*.46f))
  for(i in 0 until 38){
   val x=frac(sin(i*91.7f)*1000f)*w;val y=frac(sin(i*47.3f)*1000f)*h*.32f
   val tw=.35f+.65f*frac(sin(i*13.1f+phase*6.283f)*.5f+.5f)
   drawCircle(Color.White.copy(alpha=.10f+.25f*tw*frac(sin(i*7.7f)*99f)),(.6f+frac(sin(i*3.3f)*77f))*density,Offset(x,y))
  }
  // Tribüne: zwei geschwungene Ränge mit Publikum
  val standTop=h*.40f;val standBottom=h*.60f
  val stand=Path().apply{moveTo(-w*.05f,standBottom);quadraticTo(w*.5f,standTop-h*.10f,w*1.05f,standBottom);lineTo(w*1.05f,standBottom+h*.02f);lineTo(-w*.05f,standBottom+h*.02f);close()}
  drawPath(stand,Brush.verticalGradient(listOf(Color(0xFF15221D),Color(0xFF0C1512)),startY=standTop-h*.1f,endY=standBottom))
  drawPath(Path().apply{moveTo(-w*.05f,standBottom);quadraticTo(w*.5f,standTop-h*.10f,w*1.05f,standBottom)},light.copy(alpha=.22f),style=Stroke(1.2f*density))
  val crowd=listOf(Grass,Chalk,Gold,Blue,Clay)
  for(row in 0 until 4){
   val n=46
   for(i in 0..n){
    val tx=i/n.toFloat()
    val yy=standBottom-(standBottom-(standTop-h*.10f))*(1-(2*tx-1)*(2*tx-1))*(.55f+row*.11f)*.9f+row*2.2f*density
    val flick=.5f+.5f*sin((phase*6.283f*2+i*1.7f+row).toDouble()).toFloat()
    drawCircle(crowd[(i+row*3)%crowd.size].copy(alpha=.10f+.10f*flick),1.3f*density,Offset(-w*.05f+tx*w*1.1f,yy))
   }
  }
  // Rasen in Perspektive mit Mähstreifen
  val gTop=h*.60f;val gL=w*.04f;val gR=w*.96f
  val turf=Path().apply{moveTo(gL,gTop);lineTo(gR,gTop);lineTo(w*1.25f,h);lineTo(-w*.25f,h);close()}
  drawPath(turf,Brush.verticalGradient(listOf(turfTop,turfBottom),startY=gTop,endY=h))
  clipPath(turf){
   for(i in 0 until 12)if(i%2==0){val a=i/12f;val b=(i+1)/12f
    drawPath(Path().apply{moveTo(gL+(gR-gL)*a,gTop);lineTo(gL+(gR-gL)*b,gTop);lineTo(-w*.25f+w*1.5f*b,h);lineTo(-w*.25f+w*1.5f*a,h);close()},Color.White.copy(alpha=.035f))}
   lightPool(Offset(w*.30f,h*.78f),w*.42f,h*.16f)
   lightPool(Offset(w*.70f,h*.78f),w*.42f,h*.16f)
   lightPool(Offset(w*.5f,h*.68f),w*.36f,h*.08f,.8f)
  }
  val chalk=Chalk.copy(alpha=.42f);val sw=1.4f*density
  drawPath(turf,chalk,style=Stroke(sw))
  val halfY=h*.70f;val halfL=gL-(gL+w*.25f)*((halfY-gTop)/(h-gTop));val halfR=gR+(w*1.25f-gR)*((halfY-gTop)/(h-gTop))
  drawLine(chalk,Offset(halfL,halfY),Offset(halfR,halfY),sw)
  drawOval(chalk,Offset(w*.39f,halfY-h*.025f),Size(w*.22f,h*.05f),style=Stroke(sw))
  drawCircle(chalk,2f*density,Offset(w*.5f,halfY))
  // Strafraum hinten
  drawPath(Path().apply{moveTo(w*.33f,gTop);lineTo(w*.31f,gTop+h*.035f);lineTo(w*.69f,gTop+h*.035f);lineTo(w*.67f,gTop)},chalk,style=Stroke(sw))
  // Flutlichtmasten: zwei vorne außen, zwei hinten innen
  data class Mast(val x: Float,val top: Float,val head: Float,val ground: Pair<Offset,Offset>,val strength: Float)
  val masts=listOf(
   Mast(w*.06f,h*.08f,w*.17f,Offset(w*.20f,h*.98f) to Offset(w*.66f,h*.86f),1f),
   Mast(w*.94f,h*.08f,w*.17f,Offset(w*.34f,h*.86f) to Offset(w*.80f,h*.98f),1f),
   Mast(w*.26f,h*.22f,w*.10f,Offset(w*.30f,h*.70f) to Offset(w*.62f,h*.64f),.7f),
   Mast(w*.74f,h*.22f,w*.10f,Offset(w*.38f,h*.64f) to Offset(w*.70f,h*.70f),.7f)
  )
  for(m in masts.sortedBy{it.strength})floodlightMast(Offset(m.x,m.top),if(m.strength<1f)standTop+h*.03f else h*.66f,(if(m.strength<1f)1.6f else 2.6f)*density)
  for(m in masts)floodlightBeam(Offset(m.x,m.top),m.ground.first,m.ground.second,m.strength)
  floodlightHaze(masts.map{Offset(it.x,it.top) to it.ground},phase,22,light)
  val flicker=.94f+.06f*sin((phase*6.283f*3).toDouble()).toFloat()
  for(m in masts)floodlightHead(Offset(m.x,m.top),m.head*.55f,flicker*m.strength,light)
  // Vignette
  drawRect(Brush.radialGradient(listOf(Color.Transparent,Ink.copy(alpha=.55f)),center=Offset(w*.5f,h*.45f),radius=w*.95f))
 }
}

/** Abdunkelung zwischen Stadion und Inhalt: Text liegt immer auf ruhigem Grund. */
@Composable fun StadiumScrim(modifier: Modifier){
 Canvas(modifier){
  drawRect(Brush.verticalGradient(0f to Color.Transparent,.62f to Ink.copy(alpha=.30f),.86f to Ink.copy(alpha=.92f),1f to Ink))
 }
}

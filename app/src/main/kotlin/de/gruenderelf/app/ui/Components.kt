package de.gruenderelf.app.ui
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruenderelf.engine.*
import java.text.NumberFormat
import java.util.Locale

/* ───────────────────────── Design-Tokens ─────────────────────────
 * Zwei Looks aus einem System:
 *  • Gründerelf „Flutlicht“: tiefes Nacht-Grün, leuchtendes Rasengrün als Akzent.
 *  • BMW FC (Entwicklermodus): Graphit/Schwarz, M-Blau als Akzent, M-Rot für Warnungen.
 * Gold/Blau/Rot werden ausschließlich für Bedeutung verwendet.
 */
object UiBrand{var bmwMode:Boolean=false}
private fun brand(normal: Long,bmw: Long)=Color(if(UiBrand.bmwMode)bmw else normal)
val Ink get()=brand(0xFF070B0A,0xFF050608)          // App-Hintergrund
val Inset get()=brand(0xFF0B1110,0xFF0A0D12)        // eingelassene Felder (Auswahl, Eingabe)
val Panel get()=brand(0xFF101715,0xFF10141A)        // Karten
val PanelHigh get()=brand(0xFF162019,0xFF171D25)    // erhöhte Flächen, Kacheln
val PanelHigher get()=brand(0xFF1D2A24,0xFF1F2731)  // Selektion, Dialoge
val Line get()=brand(0xFF213029,0xFF252C35)         // Haarlinien
val LineStrong get()=brand(0xFF2E4238,0xFF343C47)   // betonte Kanten
val Track get()=brand(0xFF1F2D27,0xFF29313B)        // Fortschrittsbalken-Hintergrund
val Grass get()=brand(0xFF4FE0A0,0xFF1C69D4)        // Akzent
val GrassBright get()=brand(0xFF8BF5C4,0xFF55B8FF)  // Akzent-Verlauf
val GrassDeep get()=brand(0xFF178A5E,0xFF0B2D52)
val OnAccent get()=brand(0xFF070B0A,0xFFFFFFFF)     // Text auf Akzentflächen
val Chalk get()=brand(0xFFF1F5F2,0xFFF8FAFC)        // Text primär
val Muted get()=brand(0xFF8C9E97,0xFFC7CED7)        // Text sekundär
val Faint get()=brand(0xFF586A63,0xFF6E7A88)        // Text tertiär / deaktiviert
val Clay get()=brand(0xFFFF7864,0xFFE7222E)         // negativ / Warnung
val Blue get()=brand(0xFF6DB3FF,0xFF0066B1)         // Info / Nebenposition
val Gold get()=brand(0xFFF4C766,0xFF6DCFF6)         // Hinweis / Auszeichnung
val PitchGreen=Color(0xFF0E3A27)
val PitchGreenLight=Color(0xFF13482F)
/** Eckenradien: Gründerelf weich, BMW FC kantig-technisch. */
val CardRadius get()=if(UiBrand.bmwMode)12.dp else 20.dp
val ControlRadius get()=if(UiBrand.bmwMode)8.dp else 14.dp
val BmwMBlue=Color(0xFF2A7DE1)
val BmwMDarkBlue=Color(0xFF004B87)
val BmwMRed=Color(0xFFE7222E)
val BmwGraphite=Color(0xFF171C23)
val BmwSurface=Color(0xFF10141A)
val BmwSurfaceRaised=Color(0xFF171D25)
val BmwLine=Color(0xFF343C47)
val Palette=listOf(0xFF287254,0xFFE7EEE5,0xFF17251E,0xFF365F89,0xFFA13E38,0xFF818C84,0xFFCC643E,0xFF2F8992)

private val DisplayCondensed=FontFamily(Typeface.create("sans-serif-condensed",Typeface.BOLD))
private val DisplayBmw=FontFamily(Typeface.create("sans-serif",Typeface.BOLD))
private val Medium=FontFamily(Typeface.create("sans-serif-medium",Typeface.NORMAL))
private const val Tabular="tnum"

@Composable fun GruenderelfTheme(content: @Composable ()->Unit){
 val scheme=darkColorScheme(
  primary=Grass,onPrimary=OnAccent,primaryContainer=GrassDeep,onPrimaryContainer=Chalk,
  secondary=if(UiBrand.bmwMode)Color(0xFF9FCBFF) else Color(0xFFB5CBC0),onSecondary=Ink,secondaryContainer=Track,onSecondaryContainer=Chalk,
  tertiary=Gold,onTertiary=Ink,
  background=Ink,onBackground=Chalk,surface=Panel,onSurface=Chalk,
  surfaceVariant=PanelHigh,onSurfaceVariant=Muted,surfaceTint=Color.Transparent,
  outline=Color(0xFF4A5D55),outlineVariant=Line,
  error=Clay,onError=OnAccent,
  surfaceContainerLowest=Ink,surfaceContainerLow=Panel,surfaceContainer=PanelHigh,
  surfaceContainerHigh=PanelHigh,surfaceContainerHighest=PanelHigher,surfaceBright=PanelHigher,surfaceDim=Ink
 )
 val Display=if(UiBrand.bmwMode)DisplayBmw else DisplayCondensed
 val typography=Typography(
  displaySmall=TextStyle(fontFamily=Display,fontSize=44.sp,lineHeight=46.sp,letterSpacing=(-0.5).sp,fontFeatureSettings=Tabular),
  headlineLarge=TextStyle(fontFamily=Display,fontSize=34.sp,lineHeight=37.sp,letterSpacing=(-0.3).sp),
  headlineMedium=TextStyle(fontFamily=Display,fontSize=28.sp,lineHeight=31.sp,letterSpacing=(-0.2).sp),
  headlineSmall=TextStyle(fontFamily=Display,fontSize=24.sp,lineHeight=28.sp),
  titleLarge=TextStyle(fontFamily=Display,fontSize=21.sp,lineHeight=25.sp),
  titleMedium=TextStyle(fontFamily=Medium,fontSize=15.5.sp,lineHeight=21.sp),
  titleSmall=TextStyle(fontFamily=Medium,fontSize=14.sp,lineHeight=19.sp),
  bodyLarge=TextStyle(fontSize=16.sp,lineHeight=23.sp),
  bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
  bodySmall=TextStyle(fontSize=12.5.sp,lineHeight=18.sp),
  labelLarge=TextStyle(fontFamily=Medium,fontSize=14.sp,lineHeight=18.sp,letterSpacing=0.2.sp),
  labelMedium=TextStyle(fontFamily=Medium,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.3.sp),
  labelSmall=TextStyle(fontWeight=FontWeight.Bold,fontSize=10.5.sp,lineHeight=14.sp,letterSpacing=1.4.sp)
 )
 val shapes=Shapes(
  extraSmall=RoundedCornerShape(10.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),
  large=RoundedCornerShape(22.dp),extraLarge=RoundedCornerShape(28.dp)
 )
 MaterialTheme(colorScheme=scheme,typography=typography,shapes=shapes,content=content)
}

fun euros(value: Long)=NumberFormat.getCurrencyInstance(Locale.GERMANY).apply{maximumFractionDigits=0}.format(value)
fun dec(value: Double)=String.format(Locale.GERMANY,"%.1f",value)
/** Ampel für Werte in Prozent eines Maximums: rot · gold · grün. */
fun ratingColor(fraction: Float)=when{fraction<.4f->Clay;fraction<.65f->Gold;else->Grass}

/* ───────────────────────── Seitengerüst ───────────────────────── */

/** Hintergrund mit Flutlicht-Schein und zentrierter, scrollbarer Spalte. */
@Composable fun PageFrame(transparent: Boolean=false,content: @Composable ColumnScope.()->Unit){
 Box(Modifier.fillMaxSize().background(if(transparent)Color.Transparent else Ink).drawBehind{
  val r=(size.width*1.1f).coerceAtLeast(1f)
  drawRect(Brush.radialGradient(listOf(Grass.copy(alpha=.11f),Color.Transparent),center=Offset(size.width*.92f,-size.width*.08f),radius=r))
  drawRect(Brush.radialGradient(listOf(Color(0xFF1B4A6B).copy(alpha=.10f),Color.Transparent),center=Offset(0f,size.height*.35f),radius=r*.8f))
 },contentAlignment=Alignment.TopCenter){
  Column(Modifier.widthIn(max=640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp).padding(top=18.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
 }
}

@Composable fun Eyebrow(text: String,color: Color=Grass){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Box(Modifier.width(14.dp).height(3.dp).background(color,CircleShape))
  Text(text,color=color,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
 }
}

@Composable fun Page(title: String,kicker: String="DEIN VEREIN. DEIN WEG.",content: @Composable ColumnScope.()->Unit){
 PageFrame{
  Column(Modifier.padding(top=6.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Eyebrow(kicker,if(UiBrand.bmwMode)Color(0xFF9BCBFF) else Grass)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Text(title,Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge,color=Chalk)
    if(UiBrand.bmwMode)MStripes()
   }
  }
  content()
 }
}

/** Die drei M-Streifen als Markenzeichen im BMW-FC-Modus. */
@Composable fun MStripes(height: Dp=30.dp){
 Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){
  Box(Modifier.width(5.dp).height(height).background(Color(0xFF55B8FF)))
  Box(Modifier.width(5.dp).height(height).background(Color(0xFF1261A0)))
  Box(Modifier.width(5.dp).height(height).background(BmwMRed))
 }
}

@Composable fun SectionTitle(title: String,trailing: (@Composable ()->Unit)?=null){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Box(Modifier.width(if(UiBrand.bmwMode)3.dp else 4.dp).height(18.dp).background(if(UiBrand.bmwMode)SolidColor(BmwMBlue) else Brush.verticalGradient(listOf(GrassBright,Grass)),if(UiBrand.bmwMode)RectangleShape else CircleShape))
  Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,color=Chalk)
  trailing?.invoke()
 }
}

@Composable fun Section(title: String?=null,content: @Composable ColumnScope.()->Unit){
 Surface(color=Panel,shape=RoundedCornerShape(CardRadius),border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.background(Brush.verticalGradient(listOf(PanelHigh.copy(alpha=.6f),Panel.copy(alpha=0f)),endY=420f)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   if(title!=null)SectionTitle(title)
   content()
  }
 }
}

/** Kleine erhöhte Kachel für Kennzahlen. */
@Composable fun StatTile(label: String,value: String,modifier: Modifier=Modifier,caption: String?=null,accent: Color=Chalk){
 Surface(color=PanelHigh,shape=RoundedCornerShape(if(UiBrand.bmwMode)10.dp else 16.dp),border=BorderStroke(1.dp,Line),modifier=modifier){
  Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   Text(label.uppercase(Locale.GERMANY),color=Muted,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text(value,color=accent,style=MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings=Tabular),maxLines=1,overflow=TextOverflow.Ellipsis)
   if(caption!=null)Text(caption,color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
  }
 }
}

/** Menü- bzw. Aufgabenzeile mit Icon-Kachel, Text und Pfeil. */
@Composable fun ListRow(title: String,subtitle: String?=null,icon: ImageVector?=null,tint: Color=Grass,badge: Int=0,onClick: (()->Unit)?=null){
 val base=Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
 Row((if(onClick!=null)base.clickable(onClick=onClick) else base).padding(horizontal=4.dp,vertical=8.dp).heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
  if(icon!=null)Box(Modifier.size(40.dp).background(tint.copy(alpha=.13f),RoundedCornerShape(12.dp)).border(1.dp,tint.copy(alpha=.22f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(icon,contentDescription=null,tint=tint,modifier=Modifier.size(20.dp))}
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
   Text(title,style=MaterialTheme.typography.titleMedium,color=Chalk)
   if(subtitle!=null)Text(subtitle,color=Muted,style=MaterialTheme.typography.bodySmall)
  }
  if(badge>0)CountBadge(badge)
  if(onClick!=null)Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,contentDescription=null,tint=Faint)
 }
}

@Composable fun CountBadge(count: Int,color: Color=Clay){
 Box(Modifier.heightIn(min=20.dp).widthIn(min=20.dp).background(color,CircleShape).padding(horizontal=6.dp),contentAlignment=Alignment.Center){
  Text(if(count>9)"9+" else count.toString(),color=OnAccent,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold))
 }
}

@Composable fun HairlineDivider(){HorizontalDivider(color=Line,thickness=1.dp)}

/** Segmentierte Umschaltung – ersetzt klassische Tab-Leisten. */
@Composable fun SegmentedTabs(options: List<String>,selected: Int,onSelect: (Int)->Unit){
 Surface(color=Inset,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){
  Row(Modifier.padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
   options.forEachIndexed{i,label->
    val active=i==selected
    Box(Modifier.weight(1f).heightIn(min=40.dp).clip(RoundedCornerShape(12.dp)).background(if(active)PanelHigher else Color.Transparent).border(1.dp,if(active)LineStrong else Color.Transparent,RoundedCornerShape(12.dp)).clickable{onSelect(i)}.padding(horizontal=6.dp,vertical=9.dp),contentAlignment=Alignment.Center){
     Text(label,color=if(active)Grass else Muted,style=MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
    }
   }
  }
 }
}

/** Fortschritt in mehrstufigen Abläufen (z. B. Karriere-Erstellung). */
@Composable fun StepProgress(current: Int,total: Int){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
  repeat(total){i->Box(Modifier.weight(1f).height(4.dp).background(if(i<=current)Grass else Track,CircleShape))}
 }
}

/** Pulsierender Punkt für „live“. */
@Composable fun LiveDot(active: Boolean=true,color: Color=Clay){
 val pulse=if(active)rememberInfiniteTransition(label="live").animateFloat(.35f,1f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="pulse").value else 1f
 Box(Modifier.size(8.dp).background(color.copy(alpha=if(active)pulse else .5f),CircleShape))
}

/* ───────────────────────── Bausteine ───────────────────────── */

@Composable fun Pill(text: String,color: Color=Grass){
 Surface(color=color.copy(alpha=.13f),contentColor=color,shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,color.copy(alpha=.30f))){
  Text(text,Modifier.padding(horizontal=9.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold),maxLines=1)
 }
}

/** Quadratische Wertungs-Plakette wie auf Spielerkarten. */
@Composable fun RatingBadge(value: String,color: Color=Grass,size: Dp=44.dp){
 Box(Modifier.size(size).background(Brush.verticalGradient(listOf(color.copy(alpha=.26f),color.copy(alpha=.10f))),RoundedCornerShape(12.dp)).border(1.dp,color.copy(alpha=.45f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
  Text(value,color=color,style=MaterialTheme.typography.titleLarge.copy(fontFeatureSettings=Tabular),maxLines=1)
 }
}

@Composable fun SuitabilityBadge(player: Player,target: Position){val rating=player.ratingAt(target);val fit=player.fit(target);val color=when{player.position==target->Grass;fit>=.92->Blue;fit>=.84->Gold;else->Clay};Pill("${target.name} · $rating",color)}

@Composable fun ClubHero(w: World){
 val c=w.club();val p=w.self();val primary=Color(c.primary)
 val realMode=w.privateTopClubMode&&w.leagues.size==5;val sandbox=w.user.difficulty==Difficulty.SANDBOX
 Surface(shape=RoundedCornerShape(24.dp),color=Panel,border=BorderStroke(1.dp,LineStrong),modifier=Modifier.fillMaxWidth()){
  Box(Modifier.fillMaxWidth().heightIn(min=188.dp)){
   Canvas(Modifier.matchParentSize()){
    drawRect(Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(primary,Ink,.62f),Panel,Ink),start=Offset.Zero,end=Offset(size.width,size.height)))
    // Flutlichtkegel von oben rechts
    for(i in 0..2){
     val beam=Path().apply{moveTo(size.width*(.86f+i*.05f),-10f);lineTo(size.width*(.18f+i*.16f),size.height);lineTo(size.width*(.30f+i*.16f),size.height);close()}
     drawPath(beam,Brush.verticalGradient(listOf(Chalk.copy(alpha=.07f),Color.Transparent)))
    }
    // Rasen mit Mähstreifen
    val turfTop=size.height*.80f
    drawRect(Brush.verticalGradient(listOf(PitchGreen.copy(alpha=0f),PitchGreen.copy(alpha=.85f)),startY=turfTop-30f,endY=size.height),Offset(0f,turfTop-30f),Size(size.width,size.height-turfTop+30f))
    for(i in 0..7)if(i%2==0)drawRect(Color.White.copy(alpha=.025f),Offset(size.width*i/8f,turfTop),Size(size.width/8f,size.height-turfTop))
    // Glanz hinter dem Wappen
    val glow=Offset(size.width*.80f,size.height*.44f);val gr=(size.height*.8f).coerceAtLeast(1f)
    drawCircle(Brush.radialGradient(listOf(primary.copy(alpha=.38f),Color.Transparent),center=glow,radius=gr),radius=gr,center=glow)
   }
   Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Pill(if(realMode)"REAL-MODUS" else if(sandbox)"SANDBOX" else WorldFactory.leagueName(w,c.tier),if(sandbox&&!realMode)Gold else Grass)
     Text(c.name,style=MaterialTheme.typography.headlineMedium,color=Chalk,maxLines=2,overflow=TextOverflow.Ellipsis)
     Text("${p.name} · ${p.position.label} · Stärke ${p.ca}",color=Chalk.copy(alpha=.84f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
     Text("${c.city} · ${c.stadium.name}",color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
    Crest(c.logo,c.primary,c.secondary,Modifier.size(92.dp))
   }
  }
 }
}

@Composable fun Action(text: String,enabled: Boolean=true,secondary: Boolean=false,onClick: ()->Unit){
 val shape=RoundedCornerShape(ControlRadius)
 if(secondary){
  Surface(onClick=onClick,enabled=enabled,shape=shape,color=PanelHigh,contentColor=if(enabled)Chalk else Faint,border=BorderStroke(1.dp,if(enabled)LineStrong else Line),modifier=Modifier.fillMaxWidth()){
   Box(Modifier.fillMaxWidth().heightIn(min=50.dp).padding(horizontal=16.dp,vertical=12.dp),contentAlignment=Alignment.Center){
    Text(text,style=MaterialTheme.typography.labelLarge,textAlign=TextAlign.Center)
   }
  }
 }else{
  Surface(onClick=onClick,enabled=enabled,shape=shape,color=Color.Transparent,contentColor=if(enabled)OnAccent else Faint,modifier=Modifier.fillMaxWidth()){
   Box(Modifier.fillMaxWidth().heightIn(min=52.dp).background(if(enabled)Brush.horizontalGradient(listOf(Grass,GrassBright)) else SolidColor(PanelHigh)).padding(horizontal=16.dp,vertical=13.dp),contentAlignment=Alignment.Center){
    Text(text,style=MaterialTheme.typography.labelLarge.copy(fontWeight=FontWeight.Bold),textAlign=TextAlign.Center)
   }
  }
 }
}

/** Kompakter Knopf für Zeilen (nicht volle Breite). */
@Composable fun CompactAction(text: String,enabled: Boolean=true,secondary: Boolean=false,modifier: Modifier=Modifier,onClick: ()->Unit){
 val shape=RoundedCornerShape(if(UiBrand.bmwMode)8.dp else 12.dp)
 Surface(onClick=onClick,enabled=enabled,shape=shape,color=if(secondary)PanelHigh else if(enabled)Grass else PanelHigh,contentColor=if(!enabled)Faint else if(secondary)Chalk else OnAccent,border=if(secondary)BorderStroke(1.dp,LineStrong) else null,modifier=modifier){
  Box(Modifier.heightIn(min=40.dp).padding(horizontal=14.dp,vertical=9.dp),contentAlignment=Alignment.Center){
   Text(text,style=MaterialTheme.typography.labelLarge.copy(fontWeight=if(secondary)FontWeight.Normal else FontWeight.Bold),maxLines=1)
  }
 }
}

@Composable fun <T> Pick(label: String,value: T,options: List<T>,name: (T)->String={it.toString()},onChange: (T)->Unit){
 var open by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text(label,color=Muted,style=MaterialTheme.typography.labelMedium)
  Box{
   Surface(onClick={open=true},shape=RoundedCornerShape(if(UiBrand.bmwMode)8.dp else 12.dp),color=Inset,contentColor=Chalk,border=BorderStroke(1.dp,if(open)Grass.copy(alpha=.6f) else Line),modifier=Modifier.fillMaxWidth()){
    Row(Modifier.fillMaxWidth().heightIn(min=50.dp).padding(start=14.dp,end=10.dp,top=10.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically){
     Text(name(value),Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
     Icon(Icons.Filled.KeyboardArrowDown,contentDescription=null,tint=if(open)Grass else Muted)
    }
   }
   DropdownMenu(open,{open=false},modifier=Modifier.heightIn(max=380.dp)){
    options.forEach{option->
     val selected=option==value
     DropdownMenuItem(
      text={Text(name(option),color=if(selected)Grass else Chalk,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)},
      onClick={open=false;onChange(option)},
      trailingIcon=if(selected){{Icon(Icons.Filled.Check,contentDescription=null,tint=Grass)}} else null
     )
    }
   }
  }
 }
}

@Composable fun Field(label: String,value: String,onChange: (String)->Unit){
 OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),
  colors=OutlinedTextFieldDefaults.colors(
   focusedBorderColor=Grass,unfocusedBorderColor=Line,focusedContainerColor=Inset,unfocusedContainerColor=Inset,
   focusedLabelColor=Grass,unfocusedLabelColor=Muted,cursorColor=Grass,focusedTextColor=Chalk,unfocusedTextColor=Chalk
  ))
}

/** Gerundeter Balken mit Verlauf. */
@Composable fun Bar(fraction: Float,color: Color,modifier: Modifier=Modifier,height: Dp=6.dp){
 Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Track)){
  Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(color.copy(alpha=.55f),color))))
 }
}

@Composable fun Meter(label: String,value: Int,maximum: Int=100){
 val fraction=(value.toFloat()/maximum).coerceIn(0f,1f)
 Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text(label,Modifier.weight(1f),color=Muted,style=MaterialTheme.typography.bodySmall)
   Text("$value",color=Chalk,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings=Tabular))
  }
  Bar(fraction,ratingColor(fraction))
 }
}

@Composable fun Metric(label: String,value: String,modifier: Modifier=Modifier){
 Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
  Text(label.uppercase(Locale.GERMANY),color=Muted,style=MaterialTheme.typography.labelSmall)
  Text(value,color=Chalk,style=MaterialTheme.typography.titleLarge.copy(fontFeatureSettings=Tabular))
 }
}

@Composable fun StepSlider(label: String,value: Int,enabled: Boolean=true,onChange: (Int)->Unit){
 var local by remember(value){mutableFloatStateOf(value.toFloat())}
 Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=if(enabled)Chalk else Faint)
   Box(Modifier.background(Grass.copy(alpha=.14f),RoundedCornerShape(8.dp)).padding(horizontal=9.dp,vertical=3.dp)){
    Text("${local.toInt()} / 5",color=if(enabled)Grass else Faint,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold,fontFeatureSettings=Tabular))
   }
  }
  Slider(local,{local=it},onValueChangeFinished={onChange(local.toInt())},valueRange=1f..5f,steps=3,enabled=enabled,
   colors=SliderDefaults.colors(thumbColor=Chalk,activeTrackColor=Grass,inactiveTrackColor=Track,activeTickColor=OnAccent.copy(alpha=.45f),inactiveTickColor=Muted.copy(alpha=.5f)))
 }
}

@Composable fun Confirm(title: String,text: String,onDismiss: ()->Unit,onConfirm: ()->Unit){
 AlertDialog(onDismissRequest=onDismiss,
  title={Text(title,style=MaterialTheme.typography.headlineSmall)},
  text={Text(text,color=Muted)},
  confirmButton={Button(onConfirm,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Grass,contentColor=OnAccent)){Text("Bestätigen",fontWeight=FontWeight.Bold)}},
  dismissButton={TextButton(onDismiss){Text("Abbrechen",color=Muted)}},
  shape=RoundedCornerShape(26.dp),containerColor=PanelHigh)
}

/* ───────────────────────── Grafiken ───────────────────────── */

@Composable fun Crest(logo: Logo,primary: Long,secondary: Long,modifier: Modifier=Modifier.size(64.dp)){
 val custom=remember(logo.customImage){decodeBrandImage(logo.customImage)?.asImageBitmap()}
 Canvas(modifier.semantics{contentDescription=if(custom!=null)"Eigenes Vereinswappen" else "Vereinswappen ${logo.letters}, Vorlage ${logo.template+1}"}){
  val w=size.width;val h=size.height
  if(custom!=null){
   val scale=minOf(w/custom.width.toFloat(),h/custom.height.toFloat())
   val dw=(custom.width*scale).toInt().coerceAtLeast(1);val dh=(custom.height*scale).toInt().coerceAtLeast(1)
   drawImage(custom,dstOffset=IntOffset(((w-dw)/2f).toInt(),((h-dh)/2f).toInt()),dstSize=IntSize(dw,dh))
  }else{
   val path=Path().apply{when(logo.template%8){
    0->{moveTo(w*.13f,h*.1f);lineTo(w*.87f,h*.1f);lineTo(w*.85f,h*.62f);quadraticTo(w*.8f,h*.84f,w*.5f,h*.95f);quadraticTo(w*.2f,h*.84f,w*.15f,h*.62f);close()}
    1->addOval(androidx.compose.ui.geometry.Rect(w*.07f,h*.07f,w*.93f,h*.93f))
    2->{moveTo(w*.5f,h*.03f);lineTo(w*.94f,h*.5f);lineTo(w*.5f,h*.97f);lineTo(w*.06f,h*.5f);close()}
    3->{moveTo(w*.5f,h*.03f);lineTo(w*.91f,h*.26f);lineTo(w*.91f,h*.74f);lineTo(w*.5f,h*.97f);lineTo(w*.09f,h*.74f);lineTo(w*.09f,h*.26f);close()}
    4->{moveTo(w*.1f,h*.13f);lineTo(w*.9f,h*.13f);lineTo(w*.9f,h*.78f);lineTo(w*.5f,h*.95f);lineTo(w*.1f,h*.78f);close()}
    5->addRoundRect(androidx.compose.ui.geometry.RoundRect(w*.1f,h*.1f,w*.9f,h*.9f,w*.2f,h*.2f))
    6->{moveTo(w*.08f,h*.13f);quadraticTo(w*.5f,-h*.02f,w*.92f,h*.13f);lineTo(w*.77f,h*.79f);lineTo(w*.5f,h*.97f);lineTo(w*.23f,h*.79f);close()}
    else->{moveTo(w*.5f,h*.04f);lineTo(w*.93f,h*.22f);lineTo(w*.83f,h*.7f);lineTo(w*.5f,h*.96f);lineTo(w*.17f,h*.7f);lineTo(w*.07f,h*.22f);close()}
   }}
   // weicher Schatten, Grundfläche, Diagonale und Glanzlicht
   translate(0f,h*.025f){drawPath(path,Color.Black.copy(alpha=.35f))}
   drawPath(path,Color(primary))
   clipPath(path){
    drawLine(Color(secondary).copy(alpha=.25f),Offset(0f,h*.9f),Offset(w,h*.05f),w*.25f)
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha=.20f),Color.Transparent),startY=0f,endY=h*.55f),size=size)
   }
   drawPath(path,Color(secondary),style=Stroke(w*.035f))
   drawContext.canvas.nativeCanvas.drawText(logo.letters.take(4),w*.5f,h*.58f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color(secondary).toArgb();textAlign=Paint.Align.CENTER;textSize=w*(if(logo.letters.length>3).25f else .32f);typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)})
  }
 }
}

@Composable fun Shirt(kit: Kit,modifier: Modifier=Modifier.size(100.dp),number: String="10"){
 val custom=remember(kit.customImage){decodeBrandImage(kit.customImage)?.asImageBitmap()}
 Canvas(modifier.semantics{contentDescription=if(custom!=null)"Eigenes Trikotbild" else "Trikot, Muster ${kit.pattern+1}, Nummer $number"}){
  val w=size.width;val h=size.height
  val p=Path().apply{moveTo(w*.3f,h*.08f);lineTo(w*.15f,h*.15f);lineTo(w*.01f,h*.4f);lineTo(w*.2f,h*.5f);lineTo(w*.25f,h*.4f);lineTo(w*.25f,h*.94f);lineTo(w*.75f,h*.94f);lineTo(w*.75f,h*.4f);lineTo(w*.8f,h*.5f);lineTo(w*.99f,h*.4f);lineTo(w*.85f,h*.15f);lineTo(w*.7f,h*.08f);quadraticTo(w*.5f,h*.32f,w*.3f,h*.08f);close()}
  if(custom!=null){
   drawPath(p,Color(kit.primary))
   clipPath(p){drawImage(custom,dstSize=IntSize(w.toInt().coerceAtLeast(1),h.toInt().coerceAtLeast(1)))}
   drawPath(p,Chalk.copy(alpha=.55f),style=Stroke(1.5.dp.toPx()))
  }else{
   drawPath(p,Color(kit.primary))
   clipPath(p){
    when(kit.pattern%10){
     1->for(i in 0..4)drawRect(Color(kit.secondary),Offset(w*(.13f+i*.2f),0f),Size(w*.07f,h))
     2->drawLine(Color(kit.secondary),Offset(0f,0f),Offset(w,h),w*.2f)
     3->drawRect(Color(kit.secondary),Offset(0f,h*.38f),Size(w,h*.18f))
     4->drawRect(Color(kit.secondary),Offset(w*.5f,0f),Size(w*.5f,h))
     5->drawRect(Color(kit.secondary),Offset(w*.39f,0f),Size(w*.22f,h))
     6->for(i in 0..5)drawRect(Color(kit.secondary),Offset(0f,h*(.12f+i*.14f)),Size(w,h*.055f))
     7->{drawRect(Color(kit.secondary),Offset(0f,0f),Size(w*.27f,h*.55f));drawRect(Color(kit.secondary),Offset(w*.73f,0f),Size(w*.27f,h*.55f))}
     8->{drawLine(Color(kit.secondary),Offset(w*.16f,h*.28f),Offset(w*.5f,h*.5f),w*.09f);drawLine(Color(kit.secondary),Offset(w*.84f,h*.28f),Offset(w*.5f,h*.5f),w*.09f)}
     9->for(i in 1..8)drawRect(Color(kit.secondary),Offset(w*(i/9f),0f),Size(w*.016f,h))
     else->drawLine(Color(kit.secondary),Offset(w*.31f,h*.135f),Offset(w*.69f,h*.135f),w*.025f)
    }
    // Stofffalten: dunklere Flanken für Tiefe
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha=.18f),Color.Transparent,Color.Black.copy(alpha=.18f))),size=size)
   }
   drawPath(p,Chalk.copy(alpha=.4f),style=Stroke(1.5.dp.toPx()))
   drawContext.canvas.nativeCanvas.drawText(number,w*.5f,h*.73f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color(kit.secondary).toArgb();textAlign=Paint.Align.CENTER;textSize=w*.22f;typeface=Typeface.DEFAULT_BOLD;setShadowLayer(2f,1f,1f,android.graphics.Color.BLACK)})
  }
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ColorPicker(label: String,value: Long,onChange: (Long)->Unit){
 Text(label,color=Muted,style=MaterialTheme.typography.labelMedium)
 FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Palette.forEachIndexed{i,color->
   val selected=value==color
   Box(Modifier.size(46.dp).border(if(selected)2.dp else 1.dp,if(selected)Grass else Line,RoundedCornerShape(14.dp)).padding(if(selected)4.dp else 0.dp).background(Color(color),RoundedCornerShape(if(selected)10.dp else 14.dp)).clip(RoundedCornerShape(14.dp)).clickable{onChange(color)}.semantics{contentDescription="$label: Farbe ${i+1}${if(selected)", gewählt" else ""}"},contentAlignment=Alignment.Center){
    if(selected)Icon(Icons.Filled.Check,contentDescription=null,tint=if(Color(color).luminance()>.5f)Color(0xFF070B0A) else Color.White,modifier=Modifier.size(18.dp))
   }
  }
 }
}

@Composable fun PlayerPortrait(a: Appearance,kit: Kit,modifier: Modifier=Modifier.size(76.dp)){
 Canvas(modifier.semantics{contentDescription="Spielerporträt mit gewähltem Hautton, Haaren, Bart und Statur"}){
  val w=size.width;val h=size.height;val skin=Color(listOf(0xFFF3CEB5,0xFFDAB091,0xFFB7825C,0xFF895B40,0xFF5B3829)[a.skin.coerceIn(0,4)]);val hair=Color(listOf(0xFFCDB884,0xFF513C2D,0xFF20201D,0xFF9B522D,0xFFC4A088)[a.hair.coerceIn(0,4)])
  drawCircle(Brush.verticalGradient(listOf(Color(0xFF2B4A3B),Color(0xFF15261F))),w*.48f,Offset(w*.5f,h*.5f));drawCircle(Grass.copy(alpha=.35f),w*.48f,Offset(w*.5f,h*.5f),style=Stroke(1.5.dp.toPx()))
  clipPath(Path().apply{addOval(androidx.compose.ui.geometry.Rect(w*.02f,h*.02f,w*.98f,h*.98f))}){
   val shoulder=.3f+a.build*.045f;drawOval(Color(kit.primary),Offset(w*(.5f-shoulder),h*.6f),Size(w*shoulder*2,h*.6f));drawRect(skin,Offset(w*.44f,h*.48f),Size(w*.12f,h*.18f));drawOval(skin,Offset(w*.29f,h*.13f),Size(w*.42f,h*.44f))
   if(a.hair!=4)drawArc(hair,180f,180f,true,Offset(w*.28f,h*.1f),Size(w*.44f,h*.32f));for(x in listOf(.42f,.58f))drawCircle(Color(0xFF272620),w*.018f,Offset(w*x,h*.35f));drawLine(Color(0xFF845B4D),Offset(w*.45f,h*.47f),Offset(w*.55f,h*.47f),w*.018f)
   if(a.beard>0)drawArc(hair.copy(alpha=if(a.beard==1).55f else 1f),0f,180f,false,Offset(w*.35f,h*.37f),Size(w*.3f,h*.17f),style=Stroke(w*(if(a.beard==1).025f else .065f)));drawLine(Color(kit.secondary),Offset(w*.2f,h*.82f),Offset(w*.8f,h*.82f),w*.035f)
  }
 }
}

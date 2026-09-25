package de.gruenderelf.app.ui
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruenderelf.engine.*
import java.text.NumberFormat
import java.util.Locale

object UiBrand{var bmwMode:Boolean=false}
val Ink get()=if(UiBrand.bmwMode)Color(0xFF05070A) else Color(0xFF07110D)
val Panel get()=if(UiBrand.bmwMode)Color(0xFF111820) else Color(0xFF12231B)
val Grass get()=if(UiBrand.bmwMode)Color(0xFF00ADEF) else Color(0xFF7BDBA8)
val Chalk get()=if(UiBrand.bmwMode)Color(0xFFF4F7FA) else Color(0xFFF0F6F2)
val Muted get()=if(UiBrand.bmwMode)Color(0xFFA7B1BA) else Color(0xFF9CB1A4)
val Clay get()=if(UiBrand.bmwMode)Color(0xFFE4002B) else Color(0xFFE48C73)
val Blue get()=if(UiBrand.bmwMode)Color(0xFF0066B1) else Color(0xFF79AEEA)
val Gold get()=if(UiBrand.bmwMode)Color(0xFF6DCFF6) else Color(0xFFE0BE73)
val Palette=listOf(0xFF287254,0xFFE7EEE5,0xFF17251E,0xFF365F89,0xFFA13E38,0xFF818C84,0xFFCC643E,0xFF2F8992)
private val Display=FontFamily(Typeface.create("sans-serif-condensed",Typeface.BOLD))
@Composable fun GruenderelfTheme(content: @Composable ()->Unit){MaterialTheme(colorScheme=darkColorScheme(primary=Grass,onPrimary=Ink,secondary=Color(0xFFB4C9BE),background=Ink,onBackground=Chalk,surface=Panel,onSurface=Chalk,surfaceVariant=Color(0xFF243A2E),onSurfaceVariant=Muted,error=Clay),typography=Typography(displaySmall=androidx.compose.ui.text.TextStyle(fontFamily=Display,fontSize=36.sp,lineHeight=38.sp),headlineLarge=androidx.compose.ui.text.TextStyle(fontFamily=Display,fontSize=32.sp,lineHeight=34.sp),headlineMedium=androidx.compose.ui.text.TextStyle(fontFamily=Display,fontSize=27.sp,lineHeight=30.sp),titleLarge=androidx.compose.ui.text.TextStyle(fontFamily=Display,fontSize=23.sp,lineHeight=26.sp),bodyLarge=androidx.compose.ui.text.TextStyle(fontSize=16.sp,lineHeight=23.sp),bodyMedium=androidx.compose.ui.text.TextStyle(fontSize=14.sp,lineHeight=20.sp)),content=content)}
fun euros(value: Long)=NumberFormat.getCurrencyInstance(Locale.GERMANY).apply{maximumFractionDigits=0}.format(value)
fun dec(value: Double)=String.format(Locale.GERMANY,"%.1f",value)
@Composable fun Page(title: String,kicker: String="DEIN VEREIN. DEIN WEG.",content: @Composable ColumnScope.()->Unit){Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF172D21),Ink,Ink))),contentAlignment=Alignment.TopCenter){Column(Modifier.widthIn(max=640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text(kicker,color=Grass,style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp);Text(title,style=MaterialTheme.typography.headlineLarge);content();Spacer(Modifier.height(20.dp))}}}
@Composable fun Section(title: String?=null,content: @Composable ColumnScope.()->Unit){Surface(color=Panel,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,Color(0xFF29463A)),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){if(title!=null)Text(title,style=MaterialTheme.typography.titleLarge);content()}}}

@Composable fun Pill(text: String,color: Color=Grass){Surface(color=color.copy(alpha=.14f),contentColor=color,shape=CircleShape,border=BorderStroke(1.dp,color.copy(alpha=.35f))){Text(text,Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelMedium)}}
@Composable fun SuitabilityBadge(player: Player,target: Position){val rating=player.ratingAt(target);val fit=player.fit(target);val color=when{player.position==target->Grass;fit>=.92->Blue;fit>=.84->Gold;else->Clay};Pill("${target.name} · $rating",color)}
@Composable fun ClubHero(w: World){val c=w.club();val p=w.self();Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFF102C20),border=BorderStroke(1.dp,Grass.copy(alpha=.22f)),modifier=Modifier.fillMaxWidth()){Box(Modifier.fillMaxWidth().height(190.dp)){Canvas(Modifier.fillMaxSize()){drawRect(Brush.verticalGradient(listOf(Color(0xFF244D39),Color(0xFF10261C))),size=size);val line=Chalk.copy(alpha=.16f);for(i in 0..6)drawLine(line,Offset(0f,size.height*(.55f+i*.06f)),Offset(size.width,size.height*(.48f+i*.07f)),1.dp.toPx());drawOval(Color(0xFF0B5A31),Offset(-size.width*.12f,size.height*.58f),Size(size.width*1.25f,size.height*.62f));drawArc(Chalk.copy(alpha=.35f),195f,150f,false,Offset(size.width*.2f,size.height*.62f),Size(size.width*.6f,size.height*.28f),style=Stroke(2.dp.toPx()));drawCircle(Chalk.copy(alpha=.5f),size.width*.045f,Offset(size.width*.78f,size.height*.72f))};Row(Modifier.fillMaxSize().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){Pill(if(w.privateTopClubMode&&w.leagues.size==5)"REAL-MODUS" else if(w.user.difficulty==Difficulty.SANDBOX)"SANDBOX" else WorldFactory.leagueName(w,c.tier),if(w.privateTopClubMode&&w.leagues.size==5)Grass else if(w.user.difficulty==Difficulty.SANDBOX)Gold else Grass);Text(c.name,style=MaterialTheme.typography.headlineLarge);Text("${p.name} · ${p.position.label} · ${p.ca}",color=Chalk.copy(alpha=.82f));Text("${c.city} · ${c.stadium.name}",color=Muted,style=MaterialTheme.typography.bodySmall)};if(w.developer.enabled&&c.id==w.developer.bmwClubId)BmwFcMark(90.dp) else Crest(c.logo,c.primary,c.secondary,Modifier.size(90.dp))}}}}
@Composable fun Action(text: String,enabled: Boolean=true,secondary: Boolean=false,onClick: ()->Unit){if(secondary)OutlinedButton(onClick,Modifier.fillMaxWidth().heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(14.dp)){Text(text)}else Button(onClick,Modifier.fillMaxWidth().heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(14.dp)){Text(text,fontWeight=FontWeight.Bold)}}
@Composable fun <T> Pick(label: String,value: T,options: List<T>,name: (T)->String={it.toString()},onChange: (T)->Unit){var open by remember{mutableStateOf(false)};Column(verticalArrangement=Arrangement.spacedBy(3.dp)){Text(label,color=Muted,style=MaterialTheme.typography.labelMedium);Box{OutlinedButton({open=true},Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(8.dp)){Text(name(value),Modifier.weight(1f));Text("▾",Modifier.padding(start=8.dp))};DropdownMenu(open,{open=false},modifier=Modifier.heightIn(max=360.dp)){options.forEach{option->DropdownMenuItem(text={Text(name(option))},onClick={open=false;onChange(option)})}}}}}
@Composable fun Field(label: String,value: String,onChange: (String)->Unit){OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(8.dp))}
@Composable fun Meter(label: String,value: Int,maximum: Int=100){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Muted,style=MaterialTheme.typography.bodySmall);Text("$value",style=MaterialTheme.typography.labelMedium)};LinearProgressIndicator(progress={(value.toFloat()/maximum).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(5.dp),color=if(value<40)Clay else Grass,trackColor=Color(0xFF2D3F34))}}
@Composable fun Metric(label: String,value: String,modifier: Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){Text(label,color=Muted,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.titleLarge)}}
@Composable fun StepSlider(label: String,value: Int,enabled: Boolean=true,onChange: (Int)->Unit){var local by remember(value){mutableFloatStateOf(value.toFloat())};Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Text("${local.toInt()}",color=Grass)};Slider(local,{local=it},onValueChangeFinished={onChange(local.toInt())},valueRange=1f..5f,steps=3,enabled=enabled)}}
@Composable fun Confirm(title: String,text: String,onDismiss: ()->Unit,onConfirm: ()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(text)},confirmButton={TextButton(onConfirm){Text("Bestätigen")}},dismissButton={TextButton(onDismiss){Text("Abbrechen")}})}
@Composable fun Crest(logo: Logo,primary: Long,secondary: Long,modifier: Modifier=Modifier.size(64.dp)){
 Canvas(modifier.semantics{contentDescription="Vereinswappen ${logo.letters}, Vorlage ${logo.template+1}"}){
  val w=size.width;val h=size.height
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
  drawPath(path,Color(primary));clipPath(path){drawLine(Color(secondary).copy(alpha=.25f),Offset(0f,h*.9f),Offset(w,h*.05f),w*.25f)};drawPath(path,Color(secondary),style=Stroke(w*.035f))
  drawContext.canvas.nativeCanvas.drawText(logo.letters.take(4),w*.5f,h*.58f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color(secondary).toArgb();textAlign=Paint.Align.CENTER;textSize=w*(if(logo.letters.length>3).25f else .32f);typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)})
 }
}
@Composable fun Shirt(kit: Kit,modifier: Modifier=Modifier.size(100.dp),number: String="10"){
 Canvas(modifier.semantics{contentDescription="Trikot, Muster ${kit.pattern+1}, Nummer $number"}){
  val w=size.width;val h=size.height;val p=Path().apply{moveTo(w*.3f,h*.08f);lineTo(w*.15f,h*.15f);lineTo(w*.01f,h*.4f);lineTo(w*.2f,h*.5f);lineTo(w*.25f,h*.4f);lineTo(w*.25f,h*.94f);lineTo(w*.75f,h*.94f);lineTo(w*.75f,h*.4f);lineTo(w*.8f,h*.5f);lineTo(w*.99f,h*.4f);lineTo(w*.85f,h*.15f);lineTo(w*.7f,h*.08f);quadraticTo(w*.5f,h*.32f,w*.3f,h*.08f);close()}
  drawPath(p,Color(kit.primary));clipPath(p){when(kit.pattern%4){1->for(i in 0..4)drawRect(Color(kit.secondary),Offset(w*(.13f+i*.2f),0f),Size(w*.07f,h));2->drawLine(Color(kit.secondary),Offset(0f,0f),Offset(w,h),w*.2f);3->drawRect(Color(kit.secondary),Offset(0f,h*.38f),Size(w,h*.18f));else->drawLine(Color(kit.secondary),Offset(w*.25f,h*.13f),Offset(w*.75f,h*.13f),w*.025f)}};drawPath(p,Chalk.copy(alpha=.4f),style=Stroke(1.5.dp.toPx()))
  drawContext.canvas.nativeCanvas.drawText(number,w*.5f,h*.73f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color(kit.secondary).toArgb();textAlign=Paint.Align.CENTER;textSize=w*.22f;typeface=Typeface.DEFAULT_BOLD;setShadowLayer(2f,1f,1f,android.graphics.Color.BLACK)})
 }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ColorPicker(label: String,value: Long,onChange: (Long)->Unit){Text(label,color=Muted);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Palette.forEachIndexed{i,color->Box(Modifier.size(48.dp).border(if(value==color)3.dp else 1.dp,if(value==color)Chalk else Muted,RoundedCornerShape(10.dp)).background(Color(color),RoundedCornerShape(10.dp)).clickable{onChange(color)}.semantics{contentDescription="$label: Farbe ${i+1}${if(value==color)", gewählt" else ""}"})}}}
@Composable fun PlayerPortrait(a: Appearance,kit: Kit,modifier: Modifier=Modifier.size(76.dp)){
 Canvas(modifier.semantics{contentDescription="Spielerporträt mit gewähltem Hautton, Haaren, Bart und Statur"}){
  val w=size.width;val h=size.height;val skin=Color(listOf(0xFFF3CEB5,0xFFDAB091,0xFFB7825C,0xFF895B40,0xFF5B3829)[a.skin.coerceIn(0,4)]);val hair=Color(listOf(0xFFCDB884,0xFF513C2D,0xFF20201D,0xFF9B522D,0xFFC4A088)[a.hair.coerceIn(0,4)])
  drawCircle(Color(0xFF304C3A),w*.48f,Offset(w*.5f,h*.5f));val shoulder=.3f+a.build*.045f;drawOval(Color(kit.primary),Offset(w*(.5f-shoulder),h*.6f),Size(w*shoulder*2,h*.6f));drawRect(skin,Offset(w*.44f,h*.48f),Size(w*.12f,h*.18f));drawOval(skin,Offset(w*.29f,h*.13f),Size(w*.42f,h*.44f))
  if(a.hair!=4)drawArc(hair,180f,180f,true,Offset(w*.28f,h*.1f),Size(w*.44f,h*.32f));for(x in listOf(.42f,.58f))drawCircle(Color(0xFF272620),w*.018f,Offset(w*x,h*.35f));drawLine(Color(0xFF845B4D),Offset(w*.45f,h*.47f),Offset(w*.55f,h*.47f),w*.018f)
  if(a.beard>0)drawArc(hair.copy(alpha=if(a.beard==1).55f else 1f),0f,180f,false,Offset(w*.35f,h*.37f),Size(w*.3f,h*.17f),style=Stroke(w*(if(a.beard==1).025f else .065f)));drawLine(Color(kit.secondary),Offset(w*.2f,h*.82f),Offset(w*.8f,h*.82f),w*.035f)
 }
}

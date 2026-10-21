package de.gruenderelf.app.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.*
import de.gruenderelf.app.data.SaveSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun SaveInfo(s: SaveSummary){
 Text(s.clubName,style=MaterialTheme.typography.titleLarge,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis)
 Text("${s.leagueName} · ${s.season}/${(s.season+1)%100}",color=Grass,style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
 Text("Spieltag ${s.matchday} · ${s.difficulty} · ${SimpleDateFormat("dd.MM.yyyy · HH:mm",Locale.GERMANY).format(Date(s.updatedAt))}",color=Muted,style=MaterialTheme.typography.bodySmall)
}

/** Nächtliches Stadion: Flutlichtmasten, Lichtkegel und Rasen in Perspektive. */
@Composable private fun StadiumIllustration(modifier: Modifier){
 Canvas(modifier){
  val w=size.width;val h=size.height
  drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1A16),Color(0xFF0A1311),Ink)))
  // Lichtkegel
  for((x,dir) in listOf(.12f to 1f,.88f to -1f)){
   val beam=Path().apply{moveTo(w*x,h*.16f);lineTo(w*(.5f-dir*.05f),h*.95f);lineTo(w*(.5f+dir*.42f),h*.95f);close()}
   drawPath(beam,Brush.verticalGradient(listOf(Chalk.copy(alpha=.13f),Color.Transparent),startY=h*.16f,endY=h*.95f))
   // Mast und Scheinwerferkopf
   drawLine(Color(0xFF2A3A34),Offset(w*x,h*.16f),Offset(w*x,h*.62f),3.dp.toPx())
   drawRoundRect(Chalk.copy(alpha=.9f),Offset(w*x-12.dp.toPx(),h*.13f),Size(24.dp.toPx(),8.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
   drawCircle(Brush.radialGradient(listOf(Chalk.copy(alpha=.45f),Color.Transparent),center=Offset(w*x,h*.15f),radius=46.dp.toPx()),46.dp.toPx(),Offset(w*x,h*.15f))
  }
  // Tribüne
  drawRect(Brush.verticalGradient(listOf(Color(0xFF13201C),Color(0xFF0E1714))),Offset(0f,h*.48f),Size(w,h*.12f))
  for(i in 0..40){val x=w*i/40f;drawCircle(listOf(Grass,Chalk,Gold,Blue)[i%4].copy(alpha=.18f),1.4.dp.toPx(),Offset(x,h*(.52f+(i%3)*.025f)))}
  // Rasen in Perspektive
  val top=h*.58f
  val turf=Path().apply{moveTo(w*.06f,top);lineTo(w*.94f,top);lineTo(w*1.2f,h);lineTo(-w*.2f,h);close()}
  drawPath(turf,Brush.verticalGradient(listOf(PitchGreenLight,PitchGreen),startY=top,endY=h))
  for(i in 0..9)if(i%2==0){val a=i/10f;val b=(i+1)/10f
   val stripe=Path().apply{moveTo(w*(.06f+.88f*a),top);lineTo(w*(.06f+.88f*b),top);lineTo(w*(-.2f+1.4f*b),h);lineTo(w*(-.2f+1.4f*a),h);close()}
   drawPath(stripe,Color.White.copy(alpha=.03f))}
  val chalk=Chalk.copy(alpha=.4f);val stroke=Stroke(1.5.dp.toPx())
  drawPath(turf,chalk,style=stroke)
  drawLine(chalk,Offset(w*-.07f,h*.66f),Offset(w*1.07f,h*.66f),1.5.dp.toPx())
  drawOval(chalk,Offset(w*.38f,h*.625f),Size(w*.24f,h*.07f),style=stroke)
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,Ink),startY=h*.6f,endY=h*.84f))
 }
}

@Composable private fun SlotCard(slot: Int,s: SaveSummary?,busy: Boolean,highlighted: Boolean,onLoad: ()->Unit,onDelete: ()->Unit){
 Surface(color=Panel,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,if(highlighted)Grass.copy(alpha=.45f) else Line),modifier=Modifier.fillMaxWidth()){
  Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
   Box(Modifier.size(40.dp).background(if(s==null)Inset else Grass.copy(alpha=.14f),CircleShape).border(1.dp,if(s==null)Line else Grass.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center){
    Text("$slot",color=if(s==null)Faint else Grass,style=MaterialTheme.typography.titleLarge)
   }
   if(s==null){
    Column(Modifier.weight(1f)){Text("Freier Speicherplatz",color=Muted,style=MaterialTheme.typography.titleMedium);Text("Bereit für eine neue Vereinsgeschichte",color=Faint,style=MaterialTheme.typography.bodySmall)}
   }else{
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)){SaveInfo(s)}
    Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
     CompactAction("Laden",!busy,onClick=onLoad)
     IconButton(onClick=onDelete,enabled=!busy,modifier=Modifier.size(40.dp)){Icon(Icons.Filled.Delete,contentDescription="Slot $slot löschen",tint=if(busy)Faint else Clay.copy(alpha=.85f))}
    }
   }
  }
 }
}

@Composable fun StartScreen(vm: GameViewModel,slots: List<SaveSummary>,lastSlot: Int,busy: Boolean,onNew: ()->Unit,onImport: ()->Unit){
 var delete by remember{mutableStateOf<Int?>(null)}
 var developerTaps by rememberSaveable{mutableIntStateOf(0)}
 var developerGate by rememberSaveable{mutableStateOf(false)}
 val developerConfigured by vm.developerPasswordConfigured.collectAsState()
 val latest=slots.firstOrNull{it.slot==lastSlot}?:slots.maxByOrNull{it.updatedAt}
 Box(Modifier.fillMaxSize().background(Ink)){
  StadiumIllustration(Modifier.fillMaxWidth().height(320.dp))
  PageFrame(transparent=true){
   Spacer(Modifier.height(214.dp))
   Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
    Eyebrow("VOM HARTPLATZ NACH GANZ OBEN")
    Text("GRÜNDERELF",style=MaterialTheme.typography.displaySmall,color=Chalk)
    Text("Gründe deinen Club. Stell dich selbst auf. Mach aus einer Mannschaft einen Verein, an den man sich erinnert.",color=Muted,style=MaterialTheme.typography.bodyLarge)
   }
   if(latest!=null){
    Surface(color=Panel,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,Grass.copy(alpha=.35f)),modifier=Modifier.fillMaxWidth()){
     Column(Modifier.background(Brush.verticalGradient(listOf(Grass.copy(alpha=.10f),Color.Transparent))).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      Eyebrow("ZULETZT GESPIELT · SLOT ${latest.slot}")
      Column(verticalArrangement=Arrangement.spacedBy(1.dp)){SaveInfo(latest)}
      Action("Weiterspielen",!busy){vm.load(latest.slot)}
     }
    }
   }
   Action("Neue Karriere",!busy,secondary=latest!=null,onClick=onNew)
   Action("Spielstand importieren",!busy,true,onImport)
   SectionTitle("Deine Vereinsgeschichten",trailing={Text("${slots.size} / 5",color=Muted,style=MaterialTheme.typography.labelLarge)})
   for(slot in 1..5){val s=slots.firstOrNull{it.slot==slot};SlotCard(slot,s,busy,s!=null&&s.slot==latest?.slot,{vm.load(slot)},{delete=slot})}
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Icon(Icons.Filled.Lock,contentDescription=null,tint=Faint,modifier=Modifier.size(16.dp))
    Text("Offline · ohne Konto · ohne Werbung. Für einen Gerätewechsel kannst du Spielstände als Datei exportieren.",color=Faint,style=MaterialTheme.typography.bodySmall)
   }
   // Versionszeile: 7× antippen öffnet den Entwicklerzugang (BMW FC).
   Text("GRÜNDERELF · 0.5.23",color=Faint,style=MaterialTheme.typography.labelSmall,modifier=Modifier.fillMaxWidth().clickable{
    developerTaps++
    if(developerTaps>=7){developerTaps=0;developerGate=true}
   }.padding(vertical=8.dp))
  }
 }
 if(developerGate)DeveloperAccessDialog(vm,developerConfigured){developerGate=false}
 delete?.let{slot->Confirm("Slot $slot löschen?","${slots.firstOrNull{it.slot==slot}?.clubName} wird dauerhaft aus diesem Speicherplatz entfernt.",{delete=null}){delete=null;vm.delete(slot)}}
}

@Composable fun SavesScreen(vm: GameViewModel,slots: List<SaveSummary>,state: GameState,onBack: ()->Unit){var slot by rememberSaveable{mutableIntStateOf(state.slot)};var confirm by remember{mutableStateOf<String?>(null)}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){it?.let{uri->vm.exportTo(uri)}}
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let{uri->vm.importFrom(uri,slot)}}
 fun openImport(){importer.launch(arrayOf("application/octet-stream","application/json","*/*"))}
 Page("Spielstände","FÜNF PLÄTZE FÜR DEINE GESCHICHTEN"){
  state.world?.let{w->Section("Aktive Karriere"){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
    Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(52.dp))
    Column(Modifier.weight(1f)){Text(w.club().name,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Aktuell in Slot ${state.slot}",color=Muted,style=MaterialTheme.typography.bodySmall)}
   }
   Action("Jetzt speichern",!state.busy){vm.saveAs(state.slot)}
  }}
  Section("Ziel auswählen"){Pick("Speicherplatz",slot,(1..5).toList(),{i->"Slot $i · ${slots.firstOrNull{it.slot==i}?.clubName?:"Leer"}"}){slot=it};if(state.world!=null)Action("Speichern unter Slot $slot",!state.busy){if(slots.any{it.slot==slot}&&slot!=state.slot)confirm="save" else vm.saveAs(slot)};Action("Datei in Slot $slot importieren",!state.busy,true){if(slots.any{it.slot==slot})confirm="import" else openImport()};state.world?.let{w->Action("Als .gruenderelf exportieren",!state.busy,true){export.launch("Gruenderelf-${w.calendar.season}-Spieltag-${w.calendar.matchday}.gruenderelf")}}}
  Section("Speicherverhalten"){Text("Nach einem bestätigten Spieltag wird die gesamte Welt automatisch gespeichert. Aufstellung, Training und Ausbauten werden bei einer Änderung gesichert.",style=MaterialTheme.typography.bodyMedium);Text("Laufende Spiele werden etwa alle 15 Spielminuten und zusätzlich bei Entscheidungen, Unterbrechungen, Halbzeit und Abpfiff gesichert. Exportdateien vor dem Deinstallieren aufbewahren.",color=Muted,style=MaterialTheme.typography.bodySmall)};Action("Zurück",secondary=true,onClick=onBack)
 }
 confirm?.let{action->Confirm("Slot $slot überschreiben?","Die bisherige Karriere in diesem Slot wird ersetzt. Beim Dateiimport nur, wenn die Datei gültig ist.",{confirm=null}){confirm=null;if(action=="save")vm.saveAs(slot)else openImport()}}
}

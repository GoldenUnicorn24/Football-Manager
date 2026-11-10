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
 val others=slots.filter{it.slot!=latest?.slot}.sortedBy{it.slot}
 Box(Modifier.fillMaxSize().background(Ink)){
  NightStadium(Modifier.fillMaxWidth().height(380.dp))
  StadiumScrim(Modifier.fillMaxWidth().height(380.dp))
  PageFrame(transparent=true){
   Spacer(Modifier.height(262.dp))
   Text("GRÜNDERELF",style=MaterialTheme.typography.displaySmall,color=Chalk)
   if(latest!=null){
    Surface(color=Panel,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,Grass.copy(alpha=.35f)),modifier=Modifier.fillMaxWidth()){
     Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)){SaveInfo(latest)}
       IconButton(onClick={delete=latest.slot},enabled=!busy,modifier=Modifier.size(40.dp)){Icon(Icons.Filled.Delete,contentDescription="Slot ${latest.slot} löschen",tint=if(busy)Faint else Muted)}
      }
      Action("Weiterspielen",!busy){vm.load(latest.slot)}
     }
    }
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Box(Modifier.weight(1f)){Action("Neue Karriere",!busy,secondary=latest!=null,onClick=onNew)}
    Box(Modifier.weight(1f)){Action("Importieren",!busy,true,onImport)}
   }
   if(others.isNotEmpty()){
    SectionTitle("Weitere Spielstände")
    others.forEach{s->SlotCard(s.slot,s,busy,false,{vm.load(s.slot)},{delete=s.slot})}
   }
   // Versionszeile: 7× antippen öffnet den Entwicklerzugang (BMW FC).
   Text("GRÜNDERELF · ${de.gruenderelf.app.BuildConfig.VERSION_NAME}",color=Faint,style=MaterialTheme.typography.labelSmall,modifier=Modifier.fillMaxWidth().clickable{
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
 CompositionLocalProvider(LocalPageBack provides onBack){Page("Spielstände"){
  state.world?.let{w->Section{
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
    Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(52.dp))
    Column(Modifier.weight(1f)){Text(w.club().name,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Slot ${state.slot}",color=Muted,style=MaterialTheme.typography.bodySmall)}
   }
   Action("Speichern",!state.busy){vm.saveAs(state.slot)}
  }}
  Section(if(state.world!=null)"Anderer Slot & Datei" else "Datei importieren"){
   Pick("Speicherplatz",slot,(1..5).toList(),{i->"Slot $i · ${slots.firstOrNull{it.slot==i}?.clubName?:"Leer"}"}){slot=it}
   if(state.world!=null&&slot!=state.slot)Action("In Slot $slot speichern",!state.busy,true){if(slots.any{it.slot==slot})confirm="save" else vm.saveAs(slot)}
   Action("Datei in Slot $slot importieren",!state.busy,true){if(slots.any{it.slot==slot})confirm="import" else openImport()}
   state.world?.let{w->Action("Als Datei exportieren",!state.busy,true){export.launch("Gruenderelf-${w.calendar.season}-Spieltag-${w.calendar.matchday}.gruenderelf")}}
  }
 }}
 confirm?.let{action->Confirm("Slot $slot überschreiben?","Die bisherige Karriere in diesem Slot wird ersetzt. Beim Dateiimport nur, wenn die Datei gültig ist.",{confirm=null}){confirm=null;if(action=="save")vm.saveAs(slot)else openImport()}}
}

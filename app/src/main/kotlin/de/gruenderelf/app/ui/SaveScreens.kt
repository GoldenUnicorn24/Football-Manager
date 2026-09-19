package de.gruenderelf.app.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import de.gruenderelf.app.*
import de.gruenderelf.app.data.SaveSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun SaveInfo(s: SaveSummary){Text(s.clubName,style=MaterialTheme.typography.titleLarge);Text("${s.leagueName} · ${s.season}/${(s.season+1)%100}",color=Grass);Text("Spieltag ${s.matchday} · ${s.difficulty}",color=Muted);Text(SimpleDateFormat("dd.MM.yyyy · HH:mm",Locale.GERMANY).format(Date(s.updatedAt)),color=Muted,style=MaterialTheme.typography.labelSmall)}
@Composable fun StartScreen(vm: GameViewModel,slots: List<SaveSummary>,lastSlot: Int,busy: Boolean,onNew: ()->Unit,onImport: ()->Unit){var delete by remember{mutableStateOf<Int?>(null)}
 Page("GRÜNDERELF","VOM HARTPLATZ NACH GANZ OBEN"){
  Section{Text("Ein kleiner Verein. Eine große Geschichte.",style=MaterialTheme.typography.headlineMedium);Text("Gründe deinen Club. Stell dich selbst auf. Mach aus einer Mannschaft einen Verein, an den man sich erinnert.",color=Muted);val latest=slots.firstOrNull{it.slot==lastSlot}?:slots.maxByOrNull{it.updatedAt};if(latest!=null)Action("Weiter · ${latest.clubName}",!busy){vm.load(latest.slot)};Action("Neue Karriere",!busy,onClick=onNew);Action("Spielstand importieren",!busy,true,onImport)}
  Text("Deine fünf Vereinsgeschichten",style=MaterialTheme.typography.titleLarge)
  for(slot in 1..5){val s=slots.firstOrNull{it.slot==slot};Section("Slot $slot"){if(s==null)Text("Leer",color=Muted)else{SaveInfo(s);Action("Stand laden",!busy){vm.load(slot)};Action("Stand löschen",!busy,true){delete=slot}}}}
  Text("Kein Konto erforderlich. Für einen Gerätewechsel kannst du deine Spielstände als Datei exportieren.",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
 delete?.let{slot->Confirm("Slot $slot löschen?","${slots.firstOrNull{it.slot==slot}?.clubName} wird dauerhaft aus diesem Speicherplatz entfernt.",{delete=null}){delete=null;vm.delete(slot)}}
}
@Composable fun SavesScreen(vm: GameViewModel,slots: List<SaveSummary>,state: GameState,onBack: ()->Unit){var slot by rememberSaveable{mutableIntStateOf(state.slot)};var confirm by remember{mutableStateOf<String?>(null)}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){it?.let{uri->vm.exportTo(uri)}}
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let{uri->vm.importFrom(uri,slot)}}
 fun openImport(){importer.launch(arrayOf("application/octet-stream","application/json","*/*"))}
 Page("Spielstände","FÜNF PLÄTZE FÜR DEINE GESCHICHTEN"){
  state.world?.let{w->Section("Aktive Karriere"){Text(w.club().name,style=MaterialTheme.typography.titleLarge);Text("Aktuell in Slot ${state.slot}",color=Muted);Action("Jetzt speichern",!state.busy){vm.saveAs(state.slot)}}}
  Section("Ziel auswählen"){Pick("Speicherplatz",slot,(1..5).toList(),{i->"Slot $i · ${slots.firstOrNull{it.slot==i}?.clubName?:"Leer"}"}){slot=it};if(state.world!=null)Action("Speichern unter Slot $slot",!state.busy){if(slots.any{it.slot==slot}&&slot!=state.slot)confirm="save" else vm.saveAs(slot)};Action("Datei in Slot $slot importieren",!state.busy,true){if(slots.any{it.slot==slot})confirm="import" else openImport()};state.world?.let{w->Action("Als .gruenderelf exportieren",!state.busy,true){export.launch("Gruenderelf-${w.calendar.season}-Spieltag-${w.calendar.matchday}.gruenderelf")}}}
  Section("Speicherverhalten"){Text("Nach einem bestätigten Spieltag wird die gesamte Welt automatisch gespeichert. Aufstellung, Training und Ausbauten werden bei einer Änderung gesichert.");Text("Laufende Spiele werden etwa alle fünf Spielminuten und bei Entscheidungen, Halbzeit und Abpfiff gesichert. Exportdateien vor dem Deinstallieren aufbewahren.",color=Muted)};Action("Zurück",secondary=true,onClick=onBack)
 }
 confirm?.let{action->Confirm("Slot $slot überschreiben?","Die bisherige Karriere in diesem Slot wird ersetzt. Beim Dateiimport nur, wenn die Datei gültig ist.",{confirm=null}){confirm=null;if(action=="save")vm.saveAs(slot)else openImport()}}
}

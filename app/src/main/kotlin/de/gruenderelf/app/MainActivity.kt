package de.gruenderelf.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import de.gruenderelf.app.ui.*
import de.gruenderelf.engine.NotificationSystem

class MainActivity: ComponentActivity(){override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));setContent{GruenderelfTheme{GruenderelfApp()}}}}
@Composable fun GruenderelfApp(vm: GameViewModel=viewModel()){
 val state by vm.state.collectAsStateWithLifecycle();val slots by vm.slots.collectAsStateWithLifecycle();val last by vm.lastSlot.collectAsStateWithLifecycle();val matchSpeed by vm.matchSpeed.collectAsStateWithLifecycle()
 var startPage by rememberSaveable{mutableStateOf("start")};var exitConfirm by remember{mutableStateOf(false)};val snack=remember{SnackbarHostState()}
 LaunchedEffect(state.world?.user?.clubId){if(state.world!=null)startPage="start"}
 LaunchedEffect(state.message){state.message?.let{snack.showSnackbar(it);vm.clearMessage()}}
 Surface(Modifier.fillMaxSize(),color=Ink){
  if(state.world==null)Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)){
   when(startPage){"create"->CreationScreen(vm,slots,state.busy){startPage="start"};"import"->SavesScreen(vm,slots,state){startPage="start"};else->StartScreen(vm,slots,last,state.busy,{startPage="create"},{startPage="import"})}
   BackHandler(startPage!="start"){startPage="start"};if(state.busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().align(Alignment.TopCenter))
  }else{
   val w=state.world!!;val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route?:"home"
   val tabs=listOf(Triple("home","Start",Icons.Default.Home),Triple("kader","Kader",Icons.Default.Person),Triple("spiel","Spiel",Icons.Default.PlayArrow),Triple("liga","Liga",Icons.AutoMirrored.Filled.List),Triple("mehr","Mehr",Icons.Default.MoreVert))
   val squadUnread=NotificationSystem.unreadTired(w).size+NotificationSystem.unreadExpiring(w).size;val moreUnread=NotificationSystem.unreadScoutReports(w).size
   Scaffold(containerColor=Ink,snackbarHost={SnackbarHost(snack)},bottomBar={NavigationBar(containerColor=Ink,tonalElevation=0.dp){tabs.forEach{(dest,label,icon)->val badgeCount=when(dest){"kader"->squadUnread;"mehr"->moreUnread;else->0};NavigationBarItem(selected=route==dest||(dest=="mehr"&&route !in tabs.map{it.first}),onClick={if(dest=="kader")vm.action{NotificationSystem.markTiredSeen(it);NotificationSystem.markContractsSeen(it)};if(dest=="mehr")vm.action{NotificationSystem.markScoutReportsSeen(it)};if(route!=dest)nav.navigate(dest){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={BadgedBox(badge={if(badgeCount>0)Badge{Text(badgeCount.coerceAtMost(99).toString())}}){Icon(icon,contentDescription=label)}},label={Text(label)})}}}){padding->Box(Modifier.fillMaxSize().padding(padding)){
    NavHost(nav,"home"){
     composable("home"){HomeScreen(w,{nav.navigate("spiel")},{nav.navigate("spieler/${w.user.playerId}")})}
     composable("kader"){SquadScreen(w,vm){nav.navigate("spieler/$it")}}
     composable("spiel"){MatchScreen(state,vm,matchSpeed)}
     composable("liga"){LeagueScreen(w)}
     composable("mehr"){MoreScreen({nav.navigate(it)},{exitConfirm=true})}
     composable("verein"){ClubScreen(w,vm){nav.navigate("speichern")}}
     composable("training"){TrainingScreen(w,vm)}
     composable("transfers"){TransfersScreen(w,vm){nav.navigate("spieler/$it")}}
     composable("karriere"){CareerScreen(w)}
     composable("trophaeen"){TrophyScreen(w,vm)}
     composable("editor"){EditorScreen(w,vm)}
     composable("einstellungen"){SettingsScreen(vm)}
     composable("v0518"){V0518Screen(w,vm){nav.navigate("spieler/$it")}}
     composable("speichern"){SavesScreen(vm,slots,state){nav.popBackStack()}}
     composable("spieler/{id}"){back->ProfileScreen(w,back.arguments?.getString("id")?.toIntOrNull()?:w.user.playerId,vm){nav.popBackStack()}}
    }
    if(state.busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().align(Alignment.TopCenter))
   }}
   if(w.user.lastSeenChangelogVersion<522){
    AlertDialog(
     onDismissRequest={vm.action{it.user.lastSeenChangelogVersion=522}},
     title={Text("Neu in Gründerelf v0.5.22")},
     text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
      Text("Recovery der ursprünglichen v0.5.18-Systeme abgeschlossen.",color=Grass)
      Text("• eigene Spieler aktiv mehreren Vereinen anbieten und Angebote getrennt verhandeln")
      Text("• Bosman-Vorverträge, Vertragsenden und Vertragsverlängerungen")
      Text("• Jugend-Transfermarkt sowie Profis/U19/U23 als Zielkader")
      Text("• Medizincheck/Registrierung bleibt bei Blockaden erhalten und kann erneut geprüft werden")
      Text("• xG-Verlauf, Passnetz und korrigierte Kader-/Mehr-Badges")
      Text("• zusätzlich bleiben der große Ligen-Ausbau, internationale Qualifikation, Club World Cup und die optionale Krone der Kontinente erhalten",color=Muted)
     }},
     confirmButton={Button({vm.action{it.user.lastSeenChangelogVersion=522}}){Text("Verstanden")}}
    )
   }else if(w.user.tutorialEnabled&&!w.user.tutorialCompleted){
    val tutorialStep=w.user.tutorialStep.coerceIn(0,4)
    val tutorialTitles=listOf("1/5 · Startseite","2/5 · Profikader & Jugend","3/5 · Training & Entwicklung","4/5 · Transfers & Jugendmarkt","5/5 · Live-Spiel")
    val tutorialTexts=listOf(
     "Die Startseite ist deine Manager-Zentrale. Prüfe nächstes Spiel, Aufgaben, Finanzen und Fitness. Badges zeigen nur neue Hinweise.",
     "Im Kader verwaltest du Profis, Rollen und Verträge. U19 und U23 sind eigene Nachwuchsmannschaften; temporär nominierte Jugendspieler kehren nach dem Spiel zurück.",
     "Training, Co-Trainer-Automatik und gezielte Potenzialförderung beeinflussen aktuelle Stärke und Entwicklung. U20-Spieler profitieren besonders stark.",
     "Scouting, Merkliste, Jugendmarkt, mehrere Angebote für eigene Spieler, Bosman-Vorverträge sowie Medizincheck und Registrierung greifen hier zusammen.",
     "Live- und Schnellsimulation nutzen dieselbe Match-KI. Du kannst Taktik, Sofort-Anweisungen und Wechsel steuern; danach stehen xG-Verlauf und Passnetz bereit."
    )
    AlertDialog(
     onDismissRequest={},
     title={Text(tutorialTitles[tutorialStep])},
     text={Text(tutorialTexts[tutorialStep])},
     confirmButton={Button({
      val nextRoute=when(tutorialStep){0->"kader";1->"training";2->"transfers";3->"spiel";else->null}
      vm.action{world->if(tutorialStep>=4){world.user.tutorialCompleted=true;world.user.tutorialEnabled=false}else world.user.tutorialStep=tutorialStep+1}
      nextRoute?.let{nav.navigate(it){launchSingleTop=true}}
     }){Text(if(tutorialStep>=4)"Tutorial abschließen" else "Weiter")}},
     dismissButton={TextButton({vm.action{it.user.tutorialCompleted=true;it.user.tutorialEnabled=false}}){Text("Überspringen")}}
    )
   }
   BackHandler(route=="home"){exitConfirm=true}
  }
  state.error?.let{AlertDialog(onDismissRequest={vm.clearError()},title={Text("Das hat nicht geklappt")},text={Text(it)},confirmButton={TextButton({vm.clearError()}){Text("Verstanden")}})}
  if(exitConfirm)Confirm("Zurück zum Start?","Deine Karriere wird vorher gespeichert.",{exitConfirm=false}){exitConfirm=false;vm.backToMenu()}
 }
}

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
 val state by vm.state.collectAsStateWithLifecycle();val slots by vm.slots.collectAsStateWithLifecycle();val last by vm.lastSlot.collectAsStateWithLifecycle();val matchSpeed by vm.matchSpeed.collectAsStateWithLifecycle();val changelogSeenVersion by vm.changelogSeenVersion.collectAsStateWithLifecycle()
 var startPage by rememberSaveable{mutableStateOf("start")};var exitConfirm by remember{mutableStateOf(false)};var changelogDismissedThisSession by rememberSaveable{mutableStateOf(false)};val snack=remember{SnackbarHostState()}
 LaunchedEffect(state.world?.user?.clubId){if(state.world!=null)startPage="start"}
 LaunchedEffect(state.message){state.message?.let{snack.showSnackbar(it);vm.clearMessage()}}
 Surface(Modifier.fillMaxSize(),color=Ink){
  if(state.world==null)Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)){
   when(startPage){"create"->CreationScreen(vm,slots,state.busy){startPage="start"};"import"->SavesScreen(vm,slots,state){startPage="start"};else->StartScreen(vm,slots,last,state.busy,{startPage="create"},{startPage="import"})}
   BackHandler(startPage!="start"){startPage="start"};if(state.busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().align(Alignment.TopCenter))
  }else{
   val w=state.world!!;val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route?:"home"
   val tabs=listOf(Triple("home","Start",Icons.Default.Home),Triple("kader","Kader",Icons.Default.Person),Triple("spiel","Spiel",Icons.Default.PlayArrow),Triple("liga","Liga",Icons.AutoMirrored.Filled.List),Triple("mehr","Mehr",Icons.Default.MoreVert))
   val squadBadge=NotificationSystem.squadBadge(w)
   val reportCount=NotificationSystem.moreBadge(w)
   LaunchedEffect(route,squadBadge,reportCount,w.calendar.absoluteWeek,w.calendar.season){
    if(route=="kader"&&squadBadge>0)vm.action{NotificationSystem.acknowledgeSquad(it)}
    if(route=="transfers"&&reportCount>0)vm.action{NotificationSystem.acknowledgeScoutReports(it)}
   }
   Scaffold(containerColor=Ink,snackbarHost={SnackbarHost(snack)},bottomBar={NavigationBar(containerColor=Ink,tonalElevation=0.dp){tabs.forEach{(dest,label,icon)->
    val badge=when(dest){"kader"->squadBadge;"mehr"->reportCount;else->0}
    NavigationBarItem(selected=route==dest||(dest=="mehr"&&route !in tabs.map{it.first}),onClick={if(route!=dest)nav.navigate(dest){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={BadgedBox(badge={if(badge>0)Badge{Text(if(badge>9)"9+" else badge.toString())}}){Icon(icon,contentDescription=label)}},label={Text(label)})
   }}}){padding->Box(Modifier.fillMaxSize().padding(padding)){
    NavHost(nav,"home"){
     composable("home"){HomeScreen(w,{nav.navigate("spiel")},{nav.navigate("spieler/${w.user.playerId}")}){nav.navigate(it)}}
     composable("kader"){SquadScreen(w,vm){nav.navigate("spieler/$it")}}
     composable("spiel"){MatchScreen(state,vm,matchSpeed)}
     composable("liga"){LeagueScreen(w)}
     composable("mehr"){MoreScreen(reportCount,{nav.navigate(it)},{exitConfirm=true})}
     composable("verein"){ClubScreen(w,vm){nav.navigate("speichern")}}
     composable("training"){TrainingScreen(w,vm)}
     composable("transfers"){TransfersScreen(w,vm){nav.navigate("spieler/$it")}}
     composable("karriere"){CareerScreen(w,vm)}
     composable("hilfe"){HelpScreen(w,vm)}
     composable("editor"){EditorScreen(w,vm)}
     composable("einstellungen"){SettingsScreen(vm)}
     composable("speichern"){SavesScreen(vm,slots,state){nav.popBackStack()}}
     composable("spieler/{id}"){back->ProfileScreen(w,back.arguments?.getString("id")?.toIntOrNull()?:w.user.playerId,vm){nav.popBackStack()}}
    }
    TutorialCoach(w,route,vm){dest->if(route!=dest)nav.navigate(dest){launchSingleTop=true}}
    if(state.busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().align(Alignment.TopCenter))
   }}
   BackHandler(route=="home"){exitConfirm=true}
  }
  state.error?.let{AlertDialog(onDismissRequest={vm.clearError()},title={Text("Das hat nicht geklappt")},text={Text(it)},confirmButton={TextButton({vm.clearError()}){Text("Verstanden")}})}
  if(!changelogDismissedThisSession&&changelogSeenVersion!=CHANGELOG_LOADING&&changelogSeenVersion!=CHANGELOG_VERSION)ChangelogDialog{changelogDismissedThisSession=true;vm.markChangelogSeen()}
  if(exitConfirm)Confirm("Zurück zum Start?","Deine Karriere wird vorher gespeichert.",{exitConfirm=false}){exitConfirm=false;vm.backToMenu()}
 }
}

package de.gruenderelf.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import de.gruenderelf.app.GameState
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.app.data.SaveSummary
import de.gruenderelf.engine.*
import kotlin.math.roundToInt

@Composable
fun DeveloperAccessDialog(vm:GameViewModel,configured:Boolean,onDismiss:()->Unit){
 var code by rememberSaveable{mutableStateOf("")}
 var confirm by rememberSaveable{mutableStateOf("")}
 val valid=code.length>=6&&(!configured&&code==confirm||configured)
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text(if(configured)"Developer Mode" else "Developer-Code festlegen")},
  text={
   Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text(if(configured)"Developer-Code eingeben. Der BMW-FC-Modus liegt in einem unsichtbaren separaten Speicherstand." else "Beim ersten Öffnen legst du den lokalen Developer-Code selbst fest. Er wird nur als Hash gespeichert.",color=Muted)
    OutlinedTextField(code,{code=it},singleLine=true,label={Text("Developer-Code")},visualTransformation=PasswordVisualTransformation())
    if(!configured)OutlinedTextField(confirm,{confirm=it},singleLine=true,label={Text("Code wiederholen")},visualTransformation=PasswordVisualTransformation())
   }
  },
  dismissButton={TextButton(onClick=onDismiss){Text("Abbrechen")}},
  confirmButton={Button(onClick={vm.openDeveloperMode(code,!configured);onDismiss()},enabled=valid){Text(if(configured)"BMW FC laden" else "Code setzen & laden")}}
 )
}

@Composable
fun BmwFcMark(size:Dp=92.dp){
 Box(Modifier.size(size),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize()){
   val r=this.size.minDimension/2f
   val c=Offset(this.size.width/2f,this.size.height/2f)
   drawCircle(Color(0xFF05070A),r,c)
   drawCircle(Color(0xFFF4F7FA),r*.70f,c)
   val ir=r*.56f
   val tl=Offset(c.x-ir,c.y-ir)
   val sz=Size(ir*2,ir*2)
   drawArc(Color(0xFF0066B1),0f,90f,true,tl,sz)
   drawArc(Color(0xFFF4F7FA),90f,90f,true,tl,sz)
   drawArc(Color(0xFF0066B1),180f,90f,true,tl,sz)
   drawArc(Color(0xFFF4F7FA),270f,90f,true,tl,sz)
   drawCircle(Color(0xFF05070A),r*.72f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(r*.10f))
   drawCircle(Color(0xFF0066B1),r*.96f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(r*.045f))
  }
  Text("BMW",modifier=Modifier.align(Alignment.TopCenter).padding(top=size*.08f),color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
  Text("FC",modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=size*.07f),color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
 }
}

@Composable
fun MPerformanceMark(){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
  Box(Modifier.width(5.dp).height(18.dp).background(Color(0xFF00ADEF)))
  Box(Modifier.width(5.dp).height(18.dp).background(Color(0xFF0066B1)))
  Box(Modifier.width(5.dp).height(18.dp).background(Color(0xFFE4002B)))
  Text("M PERFORMANCE",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelLarge)
 }
}

@Composable
fun BmwDeveloperShell(state:GameState,vm:GameViewModel,slots:List<SaveSummary>,matchSpeed:MatchSpeed){
 val w=state.world?:return
 val nav=rememberNavController()
 val entry by nav.currentBackStackEntryAsState()
 val route=entry?.destination?.route?:"bmw_home"
 val snack=remember{SnackbarHostState()}
 var exit by remember{mutableStateOf(false)}
 LaunchedEffect(state.message){state.message?.let{snack.showSnackbar(it);vm.clearMessage()}}
 val tabs=listOf(
  Triple("bmw_home","Command",Icons.Default.Home),
  Triple("kader","Kader",Icons.Default.Person),
  Triple("spiel","Spiel",Icons.Default.PlayArrow),
  Triple("bmw_tech","NEXUS",Icons.Default.Build),
  Triple("bmw_more","Mehr",Icons.Default.MoreVert)
 )
 Scaffold(containerColor=Ink,snackbarHost={SnackbarHost(snack)},bottomBar={
  NavigationBar(containerColor=Ink,tonalElevation=0.dp){
   tabs.forEach{(dest,label,icon)->
    NavigationBarItem(selected=route==dest,onClick={if(route!=dest)nav.navigate(dest){launchSingleTop=true}},icon={Icon(icon,label)},label={Text(label)})
   }
  }
 }){padding->
  Box(Modifier.fillMaxSize().padding(padding)){
   NavHost(nav,"bmw_home"){
    composable("bmw_home"){BmwCommandCenterScreen(w){nav.navigate(it)}}
    composable("kader"){SquadScreen(w,vm){nav.navigate("spieler/$it")}}
    composable("spiel"){MatchScreen(state,vm,matchSpeed)}
    composable("bmw_tech"){BmwTechnologyScreen(w,vm){nav.navigate(it)}}
    composable("bmw_more"){BmwMoreScreen(w,vm,{nav.navigate(it)}){exit=true}}
    composable("bmw_rankings"){BmwWorldRankingScreen(w)}
    composable("bmw_infrastructure"){BmwInfrastructureScreen(w)}
    composable("bmw_medical"){BmwMedicalScreen(w,vm)}
    composable("bmw_roster"){BmwRosterDataScreen(w)}
    composable("liga"){LeagueScreen(w)}
    composable("verein"){ClubScreen(w,vm){vm.saveAs(BMW_DEVELOPER_SAVE_SLOT)}}
    composable("training"){TrainingScreen(w,vm)}
    composable("transfers"){TransfersScreen(w,vm){nav.navigate("spieler/$it")}}
    composable("karriere"){CareerScreen(w,vm)}
    composable("einstellungen"){SettingsScreen(vm)}
    composable("spieler/{id}"){back->ProfileScreen(w,back.arguments?.getString("id")?.toIntOrNull()?:w.user.playerId,vm){nav.popBackStack()}}
   }
   if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
  }
 }
 BackHandler(route=="bmw_home"){exit=true}
 if(exit)Confirm("BMW FC Experience verlassen?","Der versteckte Developer-Spielstand wird vorher gespeichert.",{exit=false}){exit=false;vm.backToMenu()}
}

@Composable
private fun BmwCommandCenterScreen(w:World,onNavigate:(String)->Unit){
 val c=w.club()
 val tech=BmwDeveloperSystems.profile(w,c.id)
 val power=BmwDeveloperSystems.clubPowerRanking(w)
 val rank=power.indexOfFirst{it.clubId==c.id}+1
 val next=w.nextFixture()
 Page("BMW FC","BMW MOTORSPORT DNA · FOOTBALL PERFORMANCE"){
  Section{
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){
    BmwFcMark(88.dp)
    Column(Modifier.weight(1f)){MPerformanceMark();Spacer(Modifier.height(8.dp));Text("BMW FC EXPERIENCE",style=MaterialTheme.typography.headlineMedium);Text("Developer Mode · München",color=Muted)}
   }
  }
  Section("Command Center"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Metric("Weltrang","#"+rank)
    Metric("NEXUS",tech?.overall?.let{"%.1f".format(it)}?:"—")
    Metric("Budget",euros(c.budget))
   }
   Text("Stadion: "+c.stadium.name+" · "+c.stadium.capacity+" Plätze",color=Muted)
   next?.let{Text("Nächstes Spiel: "+w.clubs.getValue(if(it.homeId==c.id)it.awayId else it.homeId).name+" · "+it.competition.label,color=Grass)}
  }
  Section("Aktive Projekte"){
   tech?.projects?.sortedByDescending{it.level}?.forEach{p->
    Text(p.name,style=MaterialTheme.typography.titleMedium)
    Text(p.domain.label+" · Level "+"%.1f".format(p.level),color=Grass)
    LinearProgressIndicator(progress={p.progress.toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
   }
  }
  Section("BMW Group Technology"){
   Metric("Automotive Tech","%.1f".format(w.developer.automotiveTechnology))
   Metric("Produktions-KI","%.1f".format(w.developer.productionTechnology))
   Metric("Medical Research","%.1f".format(w.developer.medicalResearch))
   Metric("Brand Power","%.1f".format(w.developer.brandPower))
  }
  Action("Globale Rankings",secondary=true){onNavigate("bmw_rankings")}
  Action("Performance Campus",secondary=true){onNavigate("bmw_infrastructure")}
  Action("Human Performance / Longevity",secondary=true){onNavigate("bmw_medical")}
  Action("Kaderdaten · POT · Marktwerte · Gehälter",secondary=true){onNavigate("bmw_roster")}
 }
}

@Composable
private fun BmwTechnologyScreen(w:World,vm:GameViewModel,onNavigate:(String)->Unit){
 val p=BmwDeveloperSystems.profile(w,w.user.clubId)?:return
 Page("NEXUS PRIME","BMW FOOTBALL INTELLIGENCE"){
  Section("Technologiestand"){
   TechDomain.entries.forEach{d->Meter(d.label,p.value(d).roundToInt(),130)}
   Text("Gesamtindex: "+"%.1f".format(p.overall)+" / 130",color=Grass,style=MaterialTheme.typography.titleLarge)
   Text("Konkurrenz entwickelt eigene Systeme jede Spielwoche. Finanzkraft, Reputation und Infrastruktur bestimmen, wie schnell sie aufholt.",color=Muted)
  }
  TechDomain.entries.forEach{domain->
   Section(domain.label){
    Text("Aktuell: "+"%.1f".format(p.value(domain)),style=MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button({vm.investTechnology(domain,250_000_000L)},modifier=Modifier.weight(1f)){Text("+ 250 Mio.")}
     OutlinedButton({vm.investTechnology(domain,1_000_000_000L)},modifier=Modifier.weight(1f)){Text("+ 1 Mrd.")}
    }
   }
  }
  Action("Technologie-Weltrangliste",secondary=true){onNavigate("bmw_rankings")}
 }
}

@Composable
private fun BmwWorldRankingScreen(w:World){
 val power=BmwDeveloperSystems.clubPowerRanking(w)
 val tech=BmwDeveloperSystems.technologyRanking(w)
 val infra=BmwDeveloperSystems.infrastructureRanking(w)
 Page("Global Intelligence","VEREINSSTÄRKE · TECHNOLOGIE · INFRASTRUKTUR"){
  Section("Club Power Ranking"){
   power.take(20).forEachIndexed{i,r->
    val c=w.clubs.getValue(r.clubId)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text((i+1).toString()+". "+c.name,fontWeight=if(c.id==w.user.clubId)FontWeight.Black else FontWeight.Normal);Text("%.1f".format(r.score),color=if(c.id==w.user.clubId)Grass else Chalk)}
    Text("Kader %.1f · Form %.1f · Tech %.1f · Campus %.1f".format(r.squad,r.form,r.technology,r.infrastructure),color=Muted,style=MaterialTheme.typography.bodySmall)
   }
  }
  Section("Technology Ranking"){
   tech.take(20).forEachIndexed{i,p->
    val c=w.clubs[p.clubId]?:return@forEachIndexed
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text((i+1).toString()+". "+c.name);Text("%.1f".format(p.overall),color=if(c.id==w.user.clubId)Grass else Chalk)}
    Text(p.systemName,color=Muted,style=MaterialTheme.typography.bodySmall)
   }
  }
  Section("Infrastructure Ranking"){
   infra.take(20).forEachIndexed{i,r->
    val c=w.clubs.getValue(r.clubId)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text((i+1).toString()+". "+c.name);Text("%.1f".format(r.score),color=if(c.id==w.user.clubId)Grass else Chalk)}
   }
  }
 }
}

@Composable
private fun BmwInfrastructureScreen(w:World){
 val s=w.club().stadium
 val rank=BmwDeveloperSystems.infrastructureRanking(w).indexOfFirst{it.clubId==w.user.clubId}+1
 Page("Performance Campus","BMW HUMAN PERFORMANCE CENTER"){
  Section("BMW Performance Arena"){BmwFcMark(76.dp);Text(s.name,style=MaterialTheme.typography.headlineMedium);Text(s.capacity.toString()+" Plätze · Hybridrasen · vollständige Performance-Infrastruktur",color=Muted)}
  Section("Einrichtungen"){
   Meter("Training",s.training);Meter("Medizin",s.medicine);Meter("Kraftraum",s.gym);Meter("Jugend",s.youth);Meter("Platzqualität",s.pitchQuality);Meter("Clubhouse",s.clubhouse)
   Text("Infrastructure Ranking: #"+rank,color=Grass)
  }
  Section("Spezialanlagen"){
   Text("56 spezialisierte Trainingsflächen")
   Text("BLACK PITCH II · 4 ORIGIN-Felder · 3 PROJECT-ZERO-Felder",color=Muted)
   Text("2 GK-NEXUS-Hallen · Neuro-Vision-Arenen · Biomechanics Research Wing",color=Muted)
  }
 }
}

@Composable
private fun BmwMedicalScreen(w:World,vm:GameViewModel){
 Page("Human Performance","MEDIZIN · REGENERATION · LONGEVITY"){
  Section("Grundsatz"){Text("Das Programm simuliert legale Sportmedizin, Prävention und Regeneration. Es verlangsamt altersbedingten Leistungsabfall, kann Alterung aber nicht unbegrenzt rückgängig machen.",color=Muted)}
  w.squad().filter{!it.youth}.sortedByDescending{w.calendar.season-it.birthYear}.forEach{p->
   val chronological=w.calendar.season-p.birthYear
   if(chronological>=28||p.id in w.developer.longevity){
    val lp=w.developer.longevity[p.id]
    Section(p.name){
     Text("Alter $chronological · biologisches Leistungsalter "+"%.1f".format(BmwDeveloperSystems.effectiveAge(w,p)),style=MaterialTheme.typography.titleMedium)
     if(lp?.active==true){
      Text("Programm aktiv · Reduktion "+"%.2f".format(lp.biologicalYearsReduced)+" Jahre",color=Grass)
      Text("Prime-Erhalt "+(lp.primeRetention*100).roundToInt()+" % · Recovery "+(lp.recoveryBoost*100).roundToInt()+" %",color=Muted)
     }else Action("Longevity-Programm starten · 250 Mio. €",secondary=true){vm.enrollLongevity(p.id)}
    }
   }
  }
 }
}

@Composable
private fun BmwRosterDataScreen(w:World){
 Page("BMW FC Kaderdaten","RATING · POTENZIAL · MARKTWERT · GEHALT"){
  w.squad().filter{!it.youth}.sortedWith(compareBy<Player>{it.position.ordinal}.thenBy{it.number}).forEach{p->
   val meta=w.developer.playerMeta[p.id]
   Section("#"+p.number+" · "+p.name){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("GES",p.ca.toString());Metric("POT",(meta?.potential?:p.hidden.potential).toString());Metric("Alter",(w.calendar.season-p.birthYear).toString())}
    Text(p.position.name+(if(p.secondary.isNotEmpty())" / "+p.secondary.joinToString(" / "){it.name} else ""),color=Muted)
    Text("Marktwert "+euros(meta?.marketValue?:0L)+" · Gehalt/Jahr "+euros(meta?.annualSalary?:p.wage.toLong()*52),color=Grass)
   }
  }
 }
}

@Composable
private fun BmwMoreScreen(w:World,vm:GameViewModel,onNavigate:(String)->Unit,onExit:()->Unit){
 Page("BMW FC Operations","DEVELOPER CONTROL CENTER"){
  Section{BmwFcMark(74.dp);Spacer(Modifier.height(6.dp));MPerformanceMark();Text("Versteckter Developer-Spielstand",color=Muted)}
  Action("Global Rankings",secondary=true){onNavigate("bmw_rankings")}
  Action("Performance Campus",secondary=true){onNavigate("bmw_infrastructure")}
  Action("Human Performance / Longevity",secondary=true){onNavigate("bmw_medical")}
  Action("Kaderdaten & Finanzen",secondary=true){onNavigate("bmw_roster")}
  Action("Training & Co-Trainer",secondary=true){onNavigate("training")}
  Action("Transfers & Jugend",secondary=true){onNavigate("transfers")}
  Action("Liga & Wettbewerbe",secondary=true){onNavigate("liga")}
  Action("Verein & Finanzen",secondary=true){onNavigate("verein")}
  Action("Karriere",secondary=true){onNavigate("karriere")}
  Action("BMW Developer-Spielstand sichern",secondary=true){vm.saveAs(BMW_DEVELOPER_SAVE_SLOT)}
  Action("Einstellungen",secondary=true){onNavigate("einstellungen")}
  Action("BMW FC Experience verlassen",secondary=true,onClick=onExit)
 }
}

package de.gruenderelf.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun MPerformanceMark(compact:Boolean=false){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(if(compact)3.dp else 4.dp)){
  Box(Modifier.width(if(compact)4.dp else 5.dp).height(if(compact)15.dp else 19.dp).background(Color(0xFF55B8FF),RoundedCornerShape(1.dp)))
  Box(Modifier.width(if(compact)4.dp else 5.dp).height(if(compact)15.dp else 19.dp).background(Color(0xFF1261A0),RoundedCornerShape(1.dp)))
  Box(Modifier.width(if(compact)4.dp else 5.dp).height(if(compact)15.dp else 19.dp).background(BmwMRed,RoundedCornerShape(1.dp)))
  Text("M PERFORMANCE",color=Chalk,fontWeight=FontWeight.Black,style=if(compact)MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,letterSpacing=.7.dp.value.sp)
 }
}

@Composable
private fun BmwStatusPill(text:String,active:Boolean=true){
 val color=if(active)Color(0xFF70D79B) else Color(0xFFFFC36B)
 Surface(color=color.copy(alpha=.10f),contentColor=color,shape=RoundedCornerShape(5.dp),border=androidx.compose.foundation.BorderStroke(1.dp,color.copy(alpha=.42f))){
  Text(text.uppercase(),Modifier.padding(horizontal=9.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
 }
}

@Composable
private fun BmwMetricTile(label:String,value:String,modifier:Modifier=Modifier,accent:Color=BmwMBlue){
 Surface(modifier=modifier,shape=RoundedCornerShape(8.dp),color=Color(0xFF0D1117),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine)){
  Column(Modifier.padding(horizontal=12.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Box(Modifier.width(24.dp).height(2.dp).background(accent))
   Text(label.uppercase(),color=Color(0xFF9DA9B6),style=MaterialTheme.typography.labelSmall,letterSpacing=.6.sp)
   Text(value,color=Chalk,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  }
 }
}

@Composable
private fun BmwProjectCard(project:TechProject){
 Surface(shape=RoundedCornerShape(9.dp),color=Color(0xFF0C1015),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF2A323D)),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text(project.name,color=Chalk,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(project.domain.label,color=Muted,style=MaterialTheme.typography.bodySmall)}
    BmwStatusPill(if(project.active)"ONLINE" else "PAUSIERT",project.active)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Text("LEVEL",color=Muted,style=MaterialTheme.typography.labelSmall)
    Text("%.1f".format(project.level),color=Color(0xFF9BCBFF),fontWeight=FontWeight.Bold)
   }
   LinearProgressIndicator(progress={project.progress.toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),color=BmwMBlue,trackColor=Color(0xFF252D37))
  }
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
  Surface(color=Color(0xFF090C10),shadowElevation=12.dp,tonalElevation=0.dp){
   NavigationBar(containerColor=Color.Transparent,tonalElevation=0.dp){
    tabs.forEach{(dest,label,icon)->
     val selected=route==dest||(dest=="bmw_more"&&route !in tabs.map{it.first})
     NavigationBarItem(
      selected=selected,
      onClick={if(route!=dest)nav.navigate(dest){launchSingleTop=true}},
      icon={Icon(icon,label)},
      label={Text(label,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)},
      colors=NavigationBarItemDefaults.colors(
       selectedIconColor=Color.White,selectedTextColor=Color.White,
       indicatorColor=Color(0xFF14365D),
       unselectedIconColor=Color(0xFF8995A3),unselectedTextColor=Color(0xFFA8B1BC)
      )
     )
    }
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
 val power=remember(w.calendar.absoluteWeek,w.developer.weeksActive,w.players.size){BmwDeveloperSystems.clubPowerRanking(w)}
 val rank=power.indexOfFirst{it.clubId==c.id}+1
 val next=w.nextFixture()
 Page("BMW FC","BMW PERFORMANCE OS · M DIVISION"){
  Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0B1016),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine),modifier=Modifier.fillMaxWidth()){
   Box(Modifier.fillMaxWidth()){
    Canvas(Modifier.matchParentSize()){
     drawRect(Brush.horizontalGradient(listOf(Color(0xFF0A315B).copy(alpha=.70f),Color.Transparent)),Offset.Zero,Size(size.width*.72f,size.height))
     drawLine(Color.White.copy(alpha=.08f),Offset(size.width*.62f,0f),Offset(size.width*.38f,size.height),1.dp.toPx())
     drawLine(Color.White.copy(alpha=.05f),Offset(size.width*.78f,0f),Offset(size.width*.54f,size.height),1.dp.toPx())
    }
    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
      Crest(c.logo,c.primary,c.secondary,Modifier.size(90.dp))
      Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
       MPerformanceMark()
       Text("BMW FC EXPERIENCE",color=Chalk,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
       Text("München · BMW Performance Arena",color=Color(0xFFD8DEE6),style=MaterialTheme.typography.bodyMedium)
      }
     }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      BmwStatusPill("SYSTEM READY")
      Text("DEVELOPER MODE",color=Color(0xFF9DA9B6),style=MaterialTheme.typography.labelSmall,letterSpacing=1.2.sp)
     }
    }
   }
  }

  Section("Command Center"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    BmwMetricTile("World Rank","#"+rank,Modifier.weight(1f),Color(0xFF55B8FF))
    BmwMetricTile("NEXUS",tech?.overall?.let{"%.1f".format(it)}?:"—",Modifier.weight(1f),Color(0xFF1261A0))
    BmwMetricTile("Budget",euros(c.budget),Modifier.weight(1f),BmwMRed)
   }
   Text("BMW Performance Arena · "+c.stadium.capacity+" Plätze",color=Muted,style=MaterialTheme.typography.bodyMedium)
   next?.let{
    Surface(shape=RoundedCornerShape(8.dp),color=Color(0xFF0B1826),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF1E4D7B)),modifier=Modifier.fillMaxWidth()){
     Row(Modifier.padding(12.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Column(Modifier.weight(1f)){Text("NEXT MATCH",color=Color(0xFF9BCBFF),style=MaterialTheme.typography.labelSmall,letterSpacing=.8.sp);Text(w.clubs.getValue(if(it.homeId==c.id)it.awayId else it.homeId).name,color=Chalk,fontWeight=FontWeight.Bold)}
      Text(it.competition.label,color=Muted,style=MaterialTheme.typography.bodySmall)
     }
    }
   }
  }

  Section("BMW Intelligence Projects"){
   tech?.projects?.sortedByDescending{it.level}?.forEach{BmwProjectCard(it)}
  }

  Section("BMW Group Technology"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    BmwMetricTile("Automotive","%.1f".format(w.developer.automotiveTechnology),Modifier.weight(1f))
    BmwMetricTile("Production AI","%.1f".format(w.developer.productionTechnology),Modifier.weight(1f),Color(0xFF55B8FF))
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    BmwMetricTile("Medical","%.1f".format(w.developer.medicalResearch),Modifier.weight(1f),Color(0xFF70D79B))
    BmwMetricTile("Brand","%.1f".format(w.developer.brandPower),Modifier.weight(1f),BmwMRed)
   }
  }

  Section("Performance Services"){
   Action("Global Intelligence Rankings",secondary=true){onNavigate("bmw_rankings")}
   Action("BMW Performance Campus",secondary=true){onNavigate("bmw_infrastructure")}
   Action("Human Performance & Longevity",secondary=true){onNavigate("bmw_medical")}
   Action("BMW FC Kaderdaten",secondary=true){onNavigate("bmw_roster")}
  }
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
  Section("BMW Performance Arena"){Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(76.dp));Text(s.name,style=MaterialTheme.typography.headlineMedium);Text(s.capacity.toString()+" Plätze · Hybridrasen · vollständige Performance-Infrastruktur",color=Muted)}
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
  Section{Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(74.dp));Spacer(Modifier.height(6.dp));MPerformanceMark();Text("Versteckter Developer-Spielstand",color=Muted)}
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

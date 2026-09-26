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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun BmwRoundelMark(size:Dp=52.dp){
 Box(Modifier.size(size),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize()){
   val r=this.size.minDimension/2f
   val center=Offset(this.size.width/2f,this.size.height/2f)
   drawCircle(Color(0xFF111111),r,center)
   drawCircle(Color(0xFFF4F4F4),r*.70f,center)
   val inner=r*.57f
   val tl=Offset(center.x-inner,center.y-inner)
   val sz=Size(inner*2,inner*2)
   drawArc(Color(0xFF0066B1),0f,90f,true,tl,sz)
   drawArc(Color.White,90f,90f,true,tl,sz)
   drawArc(Color(0xFF0066B1),180f,90f,true,tl,sz)
   drawArc(Color.White,270f,90f,true,tl,sz)
   drawCircle(Color(0xFF111111),r*.72f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(r*.105f))
   drawCircle(Color(0xFFF5F5F5),r*.96f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(r*.035f))
  }
  Text("BMW",modifier=Modifier.align(Alignment.TopCenter).padding(top=size*.055f),color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
 }
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
  Text("M PERFORMANCE",color=Chalk,fontWeight=FontWeight.Black,style=if(compact)MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,letterSpacing=.7.sp)
 }
}

@Composable
private fun BmwStatusPill(text:String,active:Boolean=true){
 val color=if(active)Color(0xFF70D79B) else Color(0xFFFFC36B)
 Surface(color=color.copy(alpha=.10f),contentColor=color,shape=RoundedCornerShape(5.dp),border=androidx.compose.foundation.BorderStroke(1.dp,color.copy(alpha=.42f))){
  Text(text.uppercase(),Modifier.padding(horizontal=9.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
 }
}

private fun bmwCompactMoney(value:Long):String=when{
 value>=1_000_000_000L->"%.1f Mrd. €".format(value/1_000_000_000.0)
 value>=1_000_000L->"%.0f Mio. €".format(value/1_000_000.0)
 value>=1_000L->"%.0f Tsd. €".format(value/1_000.0)
 else->euros(value)
}

@Composable
private fun BmwMetricTile(label:String,value:String,modifier:Modifier=Modifier,accent:Color=BmwMBlue){
 Surface(modifier=modifier,shape=RoundedCornerShape(8.dp),color=Color(0xFF0D1117),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine)){
  Column(Modifier.padding(horizontal=12.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Box(Modifier.width(24.dp).height(2.dp).background(accent))
   Text(label.uppercase(),color=Color(0xFF9DA9B6),style=MaterialTheme.typography.labelSmall,letterSpacing=.6.sp)
   Text(value,color=Chalk,style=if(value.length>10)MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
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
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){BmwRoundelMark(42.dp);MPerformanceMark()}
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
    BmwMetricTile("Budget",bmwCompactMoney(c.budget),Modifier.weight(1f),BmwMRed)
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
 Page("NEXUS PRIME","BMW INTELLIGENCE · PERFORMANCE COMPUTE"){
  Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0A111A),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF234B72)),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Column{Text("BMW NEXUS PRIME",color=Chalk,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Unified Football Intelligence",color=Color(0xFF9BCBFF))}
     BmwStatusPill("ONLINE")
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     BmwMetricTile("System Index","%.1f".format(p.overall),Modifier.weight(1f))
     BmwMetricTile("Research",bmwCompactMoney(p.researchBudget),Modifier.weight(1f),Color(0xFF55B8FF))
    }
    Text("Technologie ist kein kosmetischer Wert: Analyse, Training und NEXUS wirken auf Entwicklung, Entscheidungsqualität und Match-Ausführung.",color=Muted,style=MaterialTheme.typography.bodyMedium)
   }
  }

  Section("Technology Stack"){
   TechDomain.entries.forEach{d->Meter(d.label,p.value(d).roundToInt(),130)}
  }

  Section("Research Control"){
   TechDomain.entries.forEach{domain->
    Surface(shape=RoundedCornerShape(8.dp),color=Color(0xFF0C1015),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF29323C)),modifier=Modifier.fillMaxWidth()){
     Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
       Column{Text(domain.label,color=Chalk,fontWeight=FontWeight.Bold);Text("Aktueller Index",color=Muted,style=MaterialTheme.typography.bodySmall)}
       Text("%.1f".format(p.value(domain)),color=Color(0xFF9BCBFF),fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       Button({vm.investTechnology(domain,250_000_000L)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(7.dp)){Text("250 Mio. €",color=Color.White,fontWeight=FontWeight.Bold)}
       OutlinedButton({vm.investTechnology(domain,1_000_000_000L)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(7.dp),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine)){Text("1 Mrd. €",color=Chalk,fontWeight=FontWeight.Bold)}
      }
     }
    }
   }
  }

  Section("Competitive Intelligence"){
   Text("Andere Vereine bauen eigene Systeme auf und entwickeln sie über die Saison weiter. BMW startet technologisch vorne, bleibt aber nicht automatisch uneinholbar.",color=Muted)
   Action("Technology World Ranking",secondary=true){onNavigate("bmw_rankings")}
  }
 }
}

@Composable
private fun BmwWorldRankingScreen(w:World){
 val power=remember(w.calendar.absoluteWeek,w.developer.weeksActive){BmwDeveloperSystems.clubPowerRanking(w)}
 val tech=remember(w.calendar.absoluteWeek,w.developer.weeksActive){BmwDeveloperSystems.technologyRanking(w)}
 val infra=remember(w.calendar.absoluteWeek){BmwDeveloperSystems.infrastructureRanking(w)}
 Page("Global Intelligence","BMW CLUB INTELLIGENCE · LIVE WORLD INDEX"){
  Section("Club Power Ranking"){
   Text("Bewertung aus Kader, Form, Technologie, Infrastruktur und Finanzkraft.",color=Muted,style=MaterialTheme.typography.bodySmall)
   power.take(20).forEachIndexed{i,r->
    val club=w.clubs.getValue(r.clubId);val own=club.id==w.user.clubId
    Surface(shape=RoundedCornerShape(7.dp),color=if(own)Color(0xFF0C2137) else Color(0xFF0C1015),border=androidx.compose.foundation.BorderStroke(1.dp,if(own)Color(0xFF2B6FAE) else Color(0xFF262D36)),modifier=Modifier.fillMaxWidth()){
     Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
       Text((i+1).toString().padStart(2,'0')+"  "+club.name,color=Chalk,fontWeight=if(own)FontWeight.Black else FontWeight.SemiBold)
       Text("%.1f".format(r.score),color=if(own)Color(0xFF9BCBFF) else Chalk,fontWeight=FontWeight.Black)
      }
      Text("Kader %.1f   Form %.1f   Tech %.1f   Campus %.1f".format(r.squad,r.form,r.technology,r.infrastructure),color=Color(0xFFB4BEC9),style=MaterialTheme.typography.bodySmall)
     }
    }
   }
  }

  Section("Technology Ranking"){
   tech.take(20).forEachIndexed{i,p->
    val club=w.clubs[p.clubId]?:return@forEachIndexed;val own=club.id==w.user.clubId
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){Text((i+1).toString().padStart(2,'0')+"  "+club.name,color=Chalk,fontWeight=if(own)FontWeight.Black else FontWeight.Medium);Text(p.systemName,color=if(own)Color(0xFF9BCBFF) else Muted,style=MaterialTheme.typography.bodySmall)}
     Text("%.1f".format(p.overall),color=if(own)Color(0xFF9BCBFF) else Chalk,fontWeight=FontWeight.Bold)
    }
    if(i<19)HorizontalDivider(color=Color.White.copy(alpha=.06f))
   }
  }

  Section("Infrastructure Ranking"){
   infra.take(20).forEachIndexed{i,r->
    val club=w.clubs.getValue(r.clubId);val own=club.id==w.user.clubId
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){
     Text((i+1).toString().padStart(2,'0')+"  "+club.name,color=Chalk,fontWeight=if(own)FontWeight.Black else FontWeight.Medium)
     Text("%.1f".format(r.score),color=if(own)Color(0xFF9BCBFF) else Chalk,fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}

@Composable
private fun BmwInfrastructureScreen(w:World){
 val s=w.club().stadium
 val rank=BmwDeveloperSystems.infrastructureRanking(w).indexOfFirst{it.clubId==w.user.clubId}+1
 Page("Performance Campus","BMW M · HUMAN PERFORMANCE CENTER"){
  Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0A1118),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine),modifier=Modifier.fillMaxWidth()){
   Row(Modifier.padding(16.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
    Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(82.dp))
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){BmwRoundelMark(34.dp);MPerformanceMark(compact=true)};Text(s.name,color=Chalk,style=MaterialTheme.typography.headlineMedium);Text(s.capacity.toString()+" Plätze · München",color=Muted)}
   }
  }
  Section("Campus Status"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    BmwMetricTile("World Rank","#"+rank,Modifier.weight(1f))
    BmwMetricTile("Surfaces","56",Modifier.weight(1f),Color(0xFF55B8FF))
    BmwMetricTile("Status","100%",Modifier.weight(1f),Color(0xFF70D79B))
   }
  }
  Section("Core Facilities"){
   Meter("Training",s.training);Meter("Medizin",s.medicine);Meter("Kraftraum",s.gym);Meter("Jugend",s.youth);Meter("Platzqualität",s.pitchQuality);Meter("Clubhouse",s.clubhouse)
  }
  Section("M Performance Facilities"){
   listOf(
    "BLACK PITCH II" to "Adaptive Spielsituationen und Entscheidungsdruck",
    "ORIGIN Fields" to "Vier datenadaptive Trainingsfelder",
    "PROJECT ZERO Fields" to "Drei spezialisierte Defensiv- und Restverteidigungsfelder",
    "GK NEXUS" to "Zwei Torwart-Hallen mit Szenario-Simulation",
    "Neuro-Vision Arena" to "Scanning, Wahrnehmung und Vororientierung",
    "Biomechanics Research Wing" to "Bewegungsanalyse, Prävention und Performance"
   ).forEach{(name,detail)->
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){Text(name,color=Chalk,fontWeight=FontWeight.Bold);Text(detail,color=Muted,style=MaterialTheme.typography.bodySmall)}
     BmwStatusPill("ACTIVE")
    }
   }
  }
 }
}

@Composable
private fun BmwMedicalScreen(w:World,vm:GameViewModel){
 Page("Human Performance","BMW MEDICAL · RECOVERY · LONGEVITY"){
  Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0A1118),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF285144)),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Column{Text("BMW HUMAN PERFORMANCE",color=Chalk,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Medical Research Wing",color=Color(0xFF9ED9B7))}
     BmwStatusPill("MEDICAL ONLINE")
    }
    Text("Regeneration, Prävention und Longevity werden als Sportmedizin-System simuliert. Das biologische Leistungsalter kann begrenzt verbessert werden; Alterung wird nie vollständig ausgeschaltet.",color=Muted)
   }
  }

  w.squad().filter{!it.youth}.sortedByDescending{w.calendar.season-it.birthYear}.forEach{p->
   val chronological=w.calendar.season-p.birthYear
   if(chronological>=28||p.id in w.developer.longevity){
    val lp=w.developer.longevity[p.id]
    Surface(shape=RoundedCornerShape(10.dp),color=Color(0xFF0D1218),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine),modifier=Modifier.fillMaxWidth()){
     Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
       Column{Text(p.name,color=Chalk,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("#"+p.number+" · "+p.position.label,color=Muted)}
       if(lp?.active==true)BmwStatusPill("PROGRAM ACTIVE") else BmwStatusPill("AVAILABLE",false)
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
       BmwMetricTile("Age",chronological.toString(),Modifier.weight(1f))
       BmwMetricTile("Bio Age","%.1f".format(BmwDeveloperSystems.effectiveAge(w,p)),Modifier.weight(1f),Color(0xFF70D79B))
       BmwMetricTile("Fitness","%.0f%%".format(p.fitness),Modifier.weight(1f),Color(0xFF55B8FF))
      }
      if(lp?.active==true){
       Meter("Prime Retention",(lp.primeRetention*100).roundToInt())
       Meter("Recovery",(lp.recoveryBoost*100).roundToInt())
       Text("Biologische Reduktion: "+"%.2f".format(lp.biologicalYearsReduced)+" Jahre",color=Color(0xFF9ED9B7),fontWeight=FontWeight.Bold)
      }else Action("Longevity Program · 250 Mio. €",secondary=true){vm.enrollLongevity(p.id)}
     }
    }
   }
  }
 }
}

@Composable
private fun BmwRosterDataScreen(w:World){
 Page("BMW FC Kaderdaten","BMW SQUAD INTELLIGENCE · PERFORMANCE DATA"){
  val squad=w.squad().filter{!it.youth}.sortedWith(compareBy<Player>{it.position.ordinal}.thenBy{it.number})
  Section("Squad Overview"){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    BmwMetricTile("Players",squad.size.toString(),Modifier.weight(1f))
    BmwMetricTile("Ø GES","%.1f".format(squad.map{it.ca}.average()),Modifier.weight(1f),Color(0xFF55B8FF))
    BmwMetricTile("Market",bmwCompactMoney(squad.sumOf{w.developer.playerMeta[it.id]?.marketValue?:0L}),Modifier.weight(1f),BmwMRed)
   }
  }
  squad.forEach{p->
   val meta=w.developer.playerMeta[p.id]
   val isLeon=p.firstName=="Leon"&&p.lastName=="Stark"
   Surface(shape=RoundedCornerShape(10.dp),color=if(isLeon)Color(0xFF0B2035) else Color(0xFF0D1218),border=androidx.compose.foundation.BorderStroke(1.dp,if(isLeon)Color(0xFF2A70B3) else BmwLine),modifier=Modifier.fillMaxWidth()){
    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Column(Modifier.weight(1f)){Text("#"+p.number+"  "+p.name,color=Chalk,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(p.position.label+(if(p.secondary.isNotEmpty())" · "+p.secondary.joinToString(" / "){it.label} else ""),color=Muted,style=MaterialTheme.typography.bodySmall)}
      if(isLeon)BmwStatusPill("CAPTAIN")
     }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      BmwMetricTile("GES",p.ca.toString(),Modifier.weight(1f))
      BmwMetricTile("POT",(meta?.potential?:p.hidden.potential).toString(),Modifier.weight(1f),Color(0xFF55B8FF))
      BmwMetricTile("Age",(w.calendar.season-p.birthYear).toString(),Modifier.weight(1f),Color(0xFF70D79B))
     }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
      Column{Text("MARKTWERT",color=Color(0xFF9EABB9),style=MaterialTheme.typography.labelSmall);Text(euros(meta?.marketValue?:0L),color=Chalk,fontWeight=FontWeight.Bold)}
      Column(horizontalAlignment=Alignment.End){Text("GEHALT / JAHR",color=Color(0xFF9EABB9),style=MaterialTheme.typography.labelSmall);Text(euros(meta?.annualSalary?:p.wage.toLong()*52),color=Chalk,fontWeight=FontWeight.Bold)}
     }
    }
   }
  }
 }
}

@Composable
private fun BmwMoreScreen(w:World,vm:GameViewModel,onNavigate:(String)->Unit,onExit:()->Unit){
 Page("BMW FC Operations","BMW PERFORMANCE OS · CONTROL CENTER"){
  Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0A1118),border=androidx.compose.foundation.BorderStroke(1.dp,BmwLine),modifier=Modifier.fillMaxWidth()){
   Row(Modifier.padding(16.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)){
    Crest(w.club().logo,w.club().primary,w.club().secondary,Modifier.size(78.dp))
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){BmwRoundelMark(38.dp);MPerformanceMark()};Text("BMW FC OPERATIONS",color=Chalk,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Private Developer Environment",color=Muted)}
   }
  }
  Section("Intelligence & Performance"){
   Action("Global Intelligence Rankings",secondary=true){onNavigate("bmw_rankings")}
   Action("NEXUS PRIME",secondary=true){onNavigate("bmw_tech")}
   Action("BMW Performance Campus",secondary=true){onNavigate("bmw_infrastructure")}
   Action("Human Performance & Longevity",secondary=true){onNavigate("bmw_medical")}
   Action("Kaderdaten & Finanzen",secondary=true){onNavigate("bmw_roster")}
  }
  Section("Football Operations"){
   Action("Training & Co-Trainer",secondary=true){onNavigate("training")}
   Action("Transfers & Jugend",secondary=true){onNavigate("transfers")}
   Action("Liga & Wettbewerbe",secondary=true){onNavigate("liga")}
   Action("Verein & Finanzen",secondary=true){onNavigate("verein")}
   Action("Karriere",secondary=true){onNavigate("karriere")}
  }
  Section("Developer"){
   Action("BMW Developer-Spielstand sichern",secondary=true){vm.saveAs(BMW_DEVELOPER_SAVE_SLOT)}
   Action("Einstellungen",secondary=true){onNavigate("einstellungen")}
   Action("BMW FC Experience verlassen",secondary=true,onClick=onExit)
  }
 }
}


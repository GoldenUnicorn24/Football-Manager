package de.gruenderelf.app.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.gruenderelf.app.*
import de.gruenderelf.engine.*
import kotlin.math.roundToInt

private fun suitabilityColorForMatch(p: Player,target: Position)=when{p.position==target->Grass;p.fit(target)>=.92->Blue;p.fit(target)>=.84->Gold;else->Clay}
@Composable fun FormDot(result: String){
 val color=when(result){"S"->Grass;"N"->Clay;else->Muted}
 Box(Modifier.size(26.dp).background(color.copy(alpha=.18f),RoundedCornerShape(8.dp)).border(1.dp,color.copy(alpha=.5f),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text(result,color=color,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold))}
}

@Composable private fun TeamColumn(club: Club,highlight: Boolean,modifier: Modifier){
 Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
  Crest(club.logo,club.primary,club.secondary,Modifier.size(64.dp))
  Text(club.shortName,style=MaterialTheme.typography.titleLarge,color=if(highlight)Grass else Chalk,maxLines=1)
  Text(club.name,style=MaterialTheme.typography.bodySmall,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
 }
}

@Composable fun NextGame(w: World,f: Fixture,label: String=if(w.live==null)"Zum Spiel" else "Partie fortsetzen",onGame: ()->Unit){val home=f.homeId==w.user.clubId;val opponent=w.clubs.getValue(if(home)f.awayId else f.homeId);val host=w.clubs.getValue(f.homeId);val homeClub=w.clubs.getValue(f.homeId);val awayClub=w.clubs.getValue(f.awayId)
 val eyebrow=(if(f.competition!=CompetitionType.LEAGUE)"${CompetitionEngine.displayName(w,f.competition).uppercase()} · ${f.stage.uppercase()} · " else "NÄCHSTES SPIEL · ")+(if(w.isDerby(f.homeId,f.awayId)&&f.competition==CompetitionType.LEAGUE)"DERBY · " else "")+(if(home)"ZU HAUSE" else "AUSWÄRTS")
 Surface(color=Panel,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,LineStrong),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.drawBehind{
   val r=(size.width*.6f).coerceAtLeast(1f)
   drawRect(Brush.radialGradient(listOf(Color(homeClub.primary).copy(alpha=.22f),Color.Transparent),center=Offset(0f,size.height*.35f),radius=r))
   drawRect(Brush.radialGradient(listOf(Color(awayClub.primary).copy(alpha=.22f),Color.Transparent),center=Offset(size.width,size.height*.35f),radius=r))
  }.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Eyebrow(eyebrow)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    TeamColumn(homeClub,homeClub.id==w.user.clubId,Modifier.weight(1f))
    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
     Text("VS",style=MaterialTheme.typography.headlineMedium,color=Faint)
     if(f.competition==CompetitionType.LEAGUE)Text("Spieltag ${f.matchday}",style=MaterialTheme.typography.labelMedium,color=Muted)
    }
    TeamColumn(awayClub,awayClub.id==w.user.clubId,Modifier.weight(1f))
   }
   HairlineDivider()
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text(host.stadium.name,style=MaterialTheme.typography.titleSmall,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${host.stadium.surface.label} · ${host.stadium.capacity} Plätze",color=Muted,style=MaterialTheme.typography.bodySmall)}
    if(opponent.form.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){opponent.form.takeLast(5).forEach{FormDot(it)}}
   }
   Action(label,onClick=onGame)
  }
 }
}

@Composable fun HomeScreen(w: World,onGame: ()->Unit,onProfile: ()->Unit,onNavigate:(String)->Unit){val c=w.club();val p=w.self()
 Page("Übersicht","SAISON ${w.calendar.season}/${(w.calendar.season+1)%100} · SPIELTAG ${w.calendar.matchday}",wide=true){
  ClubHero(w)
  val seniors=w.squad().filter{!it.youth}
  val tired=w.squad().filter{it.available&&!it.youth&&it.fitness<65}.sortedBy{it.fitness};val hot=seniors.maxByOrNull{it.form};val expiring=seniors.filter{it.contractYears<=1}.take(4);val finishedReports=w.scoutReports.values.filter{it.progress>=100}.take(4)
  val table=WorldFactory.table(w,c.tier);val rank=table.indexOfFirst{it.clubId==c.id}+1
  val morale=seniors.takeIf{it.isNotEmpty()}?.map{it.morale}?.average()?.roundToInt()?:0
  val fitness=seniors.takeIf{it.isNotEmpty()}?.map{it.fitness}?.average()?.roundToInt()?:0
  val history=remember(w.calendar.absoluteWeek,w.calendar.season){tableHistory(w)}
  AdaptiveColumns(left={
   NextStepCard(w,onGame,onNavigate)
   w.nextFixture()?.let{NextGame(w,it,onGame=onGame)}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    StatTile("Tabelle",if(rank>0)"$rank." else "–",Modifier.weight(1f),caption="von ${table.size}",accent=if(rank in 1..3)Grass else Chalk)
    StatTile("Kasse",euros(c.budget),Modifier.weight(1f),accent=if(c.budget<0)Clay else Chalk)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    StatTile("Ø Moral","$morale",Modifier.weight(1f),accent=ratingColor(morale/100f))
    StatTile("Ø Fitness","$fitness %",Modifier.weight(1f),accent=ratingColor(fitness/100f))
   }
   Section("Hinweise"){
    var count=0
    if(expiring.isNotEmpty()){count++;ListRow(if(expiring.size==1)"1 Vertrag läuft aus" else "${expiring.size} Verträge laufen aus",expiring.joinToString{it.lastName},Icons.Filled.DateRange,Gold){onNavigate("kader")}}
    if(finishedReports.isNotEmpty()){count++;ListRow(if(finishedReports.size==1)"1 Scoutbericht vollständig" else "${finishedReports.size} Scoutberichte vollständig","Im Scouting ansehen",Icons.Filled.Search,Blue){onNavigate("transfers")}}
    if(count==0)ListRow("Keine offenen Hinweise","Verträge und Scouting sind im grünen Bereich.",Icons.Filled.CheckCircle,Grass)
   }
   Section("Form & Kabine"){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Text("Letzte Spiele",color=Muted,style=MaterialTheme.typography.bodySmall)
     if(c.form.isEmpty())Text("Die Geschichte beginnt.",color=Muted,style=MaterialTheme.typography.bodySmall) else Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){c.form.takeLast(5).forEach{FormDot(it)}}
    }
    HairlineDivider()
    if(tired.isEmpty())Text("Der Kader wirkt frisch.",color=Grass,style=MaterialTheme.typography.bodyMedium)else Text("${tired.take(3).joinToString{it.lastName}} brauchen Aufmerksamkeit.",color=Gold,style=MaterialTheme.typography.bodyMedium)
    hot?.let{Text("Beste Form: ${it.name} · ${dec(it.form)}",color=Muted,style=MaterialTheme.typography.bodySmall)}
   }
  },right={
   BoardSection(w)
   Section("Saisonverlauf"){TableHistoryChart(history,table.size)}
   InboxPreview(w){onNavigate("posteingang")}
   Section("Gründer. Trainer. Nummer ${p.number}."){
    Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){
     PlayerPortrait(p.appearance,c.kits.home)
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){Text(p.name,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.position.label,color=Grass,style=MaterialTheme.typography.bodyMedium);Text("${p.stats.goals} Tore · ${p.stats.assists} Vorlagen",color=Muted,style=MaterialTheme.typography.bodySmall)}
     RatingBadge("${p.ca}",Grass,50.dp)
    }
    Meter("Deine Fitness",p.fitness.roundToInt())
    if(!p.available)Text(if(p.injuryWeeks>0)"${p.injury}: ${p.injuryWeeks} Wochen" else "${p.unavailableReason?.label}",color=Clay)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Box(Modifier.weight(1f)){Action("Profil",secondary=true,onClick=onProfile)}
     Box(Modifier.weight(1f)){Action("Karriere",secondary=true){onNavigate("karriere")}}
    }
   }
  })
 }
}

@Composable private fun CompetitionResultRow(w: World,f: Fixture){
 val h=w.clubs.getValue(f.homeId);val a=w.clubs.getValue(f.awayId);val own=f.homeId==w.user.clubId||f.awayId==w.user.clubId
 Row(Modifier.fillMaxWidth().background(if(own)Grass.copy(alpha=.08f) else Color.Transparent,RoundedCornerShape(10.dp)).padding(horizontal=6.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Text(h.name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.End,style=MaterialTheme.typography.bodySmall,color=if(h.id==w.user.clubId)Grass else Chalk,fontWeight=if(h.id==w.user.clubId)FontWeight.Bold else FontWeight.Normal)
  Crest(h.logo,h.primary,h.secondary,Modifier.size(20.dp))
  Box(Modifier.widthIn(min=54.dp).background(if(f.played)PanelHigher else Inset,RoundedCornerShape(8.dp)).padding(horizontal=8.dp,vertical=4.dp),contentAlignment=Alignment.Center){
   Text(CompetitionEngine.resultText(w,f),color=if(f.played)Chalk else Muted,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"),fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,maxLines=1)
  }
  Crest(a.logo,a.primary,a.secondary,Modifier.size(20.dp))
  Text(a.name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=if(a.id==w.user.clubId)Grass else Chalk,fontWeight=if(a.id==w.user.clubId)FontWeight.Bold else FontWeight.Normal)
 }
}

@Composable private fun RankChip(rank: Int,color: Color?,width: Dp=26.dp){
 Box(Modifier.width(width).height(24.dp).background(color?.copy(alpha=.16f)?:Color.Transparent,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center){
  Text("$rank",color=color?:Muted,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold,fontFeatureSettings="tnum"))
 }
}

@Composable private fun TableHeader(vararg columns: Pair<String,Dp>){
 Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
  Text("#",Modifier.width(26.dp),color=Faint,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)
  Text("VEREIN",Modifier.weight(1f),color=Faint,style=MaterialTheme.typography.labelSmall)
  columns.forEach{(label,width)->Text(label,Modifier.width(width),color=Faint,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.End)}
 }
}

private fun tableRowModifier(own: Boolean)=Modifier.fillMaxWidth().background(if(own)Grass.copy(alpha=.10f) else Color.Transparent,RoundedCornerShape(10.dp)).border(1.dp,if(own)Grass.copy(alpha=.32f) else Color.Transparent,RoundedCornerShape(10.dp))

@Composable private fun ZoneLegend(color: Color,text: String){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.size(10.dp).background(color,RoundedCornerShape(3.dp)));Text(text,color=Muted,style=MaterialTheme.typography.bodySmall)}
}

@Composable private fun LeagueTable(w: World,tier: Int,full: Boolean){
 val league=w.leagues.first{it.tier==tier};val country=WorldFactory.leagueCountry(w,tier);val germanPyramid=w.privateTopClubMode&&country=="Deutschland"&&tier in 1..5
 Section("$country · ${league.name}"){
  if(!full)TableHeader("SP" to 22.dp,"TORE" to 44.dp,"PKT" to 28.dp)
  val rows=WorldFactory.table(w,tier)
  Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
   rows.forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);val own=c.id==w.user.clubId
    val rankColor=when{germanPyramid&&tier>1&&i<2->Grass;germanPyramid&&tier<5&&i>=rows.size-2->Clay;else->null}
    if(full)Column(tableRowModifier(own).padding(horizontal=6.dp,vertical=9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){RankChip(i+1,rankColor);Crest(c.logo,c.primary,c.secondary,Modifier.size(22.dp));Text("${c.name} (${c.shortName})",Modifier.weight(1f),fontWeight=if(own)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${r.points} P",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"))}
     Text("Sp ${r.played} · S ${r.won} · U ${r.drawn} · N ${r.lost} · Tore ${r.goalsFor}:${r.goalsAgainst} · Diff ${if(r.difference>=0)"+" else ""}${r.difference}",Modifier.padding(start=34.dp),color=Muted,style=MaterialTheme.typography.bodySmall)
    }else Row(tableRowModifier(own).padding(horizontal=6.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
     RankChip(i+1,rankColor)
     Crest(c.logo,c.primary,c.secondary,Modifier.size(20.dp))
     Text(c.name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,fontWeight=if(own)FontWeight.Bold else FontWeight.Normal,color=if(own)Chalk else Chalk.copy(alpha=.9f))
     Text("${r.played}",Modifier.width(22.dp),style=MaterialTheme.typography.bodySmall.copy(fontFeatureSettings="tnum"),color=Muted,textAlign=TextAlign.End)
     Text("${r.goalsFor}:${r.goalsAgainst}",Modifier.width(44.dp),style=MaterialTheme.typography.bodySmall.copy(fontFeatureSettings="tnum"),color=Muted,textAlign=TextAlign.End)
     Text("${r.points}",Modifier.width(28.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"),textAlign=TextAlign.End)
    }
   }
  }
  if(germanPyramid){if(tier>1)ZoneLegend(Grass,"Platz 1–2 steigen auf");if(tier<5)ZoneLegend(Clay,"Die letzten zwei steigen ab")}
  else if(w.privateTopClubMode)Text("Für diese Topliga wird außerhalb des abgebildeten Ligabaums kein Auf- oder Abstieg simuliert.",color=Muted,style=MaterialTheme.typography.bodySmall)
  Text("Bei Punktgleichheit: Tordifferenz, erzielte Tore, feste Vereinsreihenfolge.",color=Faint,style=MaterialTheme.typography.bodySmall)
 }
}

@Composable private fun EuroLeaguePhaseTable(w: World,type: CompetitionType){
 Section("Ligaphasen-Tabelle"){
  TableHeader("SP" to 22.dp,"DIFF" to 36.dp,"PKT" to 28.dp)
  Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
   CompetitionEngine.euroLeagueTable(w,type).forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);val own=c.id==w.user.clubId;val rankColor=when{i<8->Grass;i<24->Gold;else->null}
    Row(tableRowModifier(own).padding(horizontal=6.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
     RankChip(i+1,rankColor)
     Crest(c.logo,c.primary,c.secondary,Modifier.size(20.dp))
     Text(c.name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(own)FontWeight.Bold else FontWeight.Normal,style=MaterialTheme.typography.bodySmall)
     Text("${r.played}",Modifier.width(22.dp),style=MaterialTheme.typography.bodySmall.copy(fontFeatureSettings="tnum"),color=Muted,textAlign=TextAlign.End)
     Text("${if(r.difference>=0)"+" else ""}${r.difference}",Modifier.width(36.dp),style=MaterialTheme.typography.bodySmall.copy(fontFeatureSettings="tnum"),color=Muted,textAlign=TextAlign.End)
     Text("${r.points}",Modifier.width(28.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"),textAlign=TextAlign.End)
    }
   }
  }
  ZoneLegend(Grass,"Platz 1–8 direkt ins Achtelfinale")
  ZoneLegend(Gold,"Platz 9–24 in die Play-offs")
  Text("Platz 25–36 scheidet aus.",color=Faint,style=MaterialTheme.typography.bodySmall)
 }
}

@Composable fun LeagueScreen(w: World){
 val real=w.privateTopClubMode;val cupName=CompetitionEngine.displayName(w,CompetitionType.NATIONAL_CUP)
 val ownLeague=w.leagues.first{w.user.clubId in it.clubIds};val ownCountry=WorldFactory.leagueCountry(w,ownLeague.tier)
 val competitionOptions=if(real)buildList{
  add("Liga");add(cupName);add(CompetitionType.CHAMPIONS_LEAGUE.label);add(CompetitionType.EUROPA_LEAGUE.label);add(CompetitionType.CLUB_WORLD_CUP.label)
  if(w.fantasyCupEnabled)add(CompetitionType.ETERNAL_CROWN.label)
 } else listOf("Liga",cupName,CompetitionType.EURO_ELITE.label)
 var competition by rememberSaveable{mutableStateOf("Liga")};var country by rememberSaveable(w.user.clubId,w.calendar.season){mutableStateOf(ownCountry)};var tier by rememberSaveable(w.user.clubId,w.calendar.season){mutableIntStateOf(ownLeague.tier)};var day by rememberSaveable(w.calendar.season){mutableIntStateOf(w.calendar.matchday)};var fullTable by rememberSaveable{mutableStateOf(false)};var group by rememberSaveable{mutableStateOf("A")};var euroRound by rememberSaveable(w.calendar.season){mutableIntStateOf(1)}
 if(competition !in competitionOptions)competition="Liga"
 val eyebrow=if(real)"LIGEN · POKALE · EUROPA · CLUB WORLD CUP${if(w.fantasyCupEnabled)" · KRONE" else ""}" else "LIGA · GRÜNDERPOKAL · EUROPA-ELITELIGA"
 Page("Wettbewerbe",eyebrow){
  Pick("Wettbewerb",competition,competitionOptions,{it}){competition=it}
  when(competition){
   "Liga"->{
    val countries=w.leagues.map{WorldFactory.leagueCountry(w,it.tier)}.distinct().sortedWith(compareBy<String>{if(it==ownCountry)0 else 1}.thenBy{it});if(country !in countries)country=ownCountry
    Pick("Land",country,countries,{it}){selected->country=selected;val options=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==selected}.sortedBy{it.tier};tier=if(selected==ownCountry)ownLeague.tier else options.first().tier;day=1}
    val countryLeagues=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==country}.sortedBy{it.tier};if(countryLeagues.none{it.tier==tier})tier=countryLeagues.first().tier
    Pick("Liga",tier,countryLeagues.map{it.tier},{WorldFactory.leagueName(w,it)}){tier=it;day=1}
    if(tier==ownLeague.tier)Pill("Deine aktuelle Liga",Grass)
    SegmentedTabs(listOf("Kompakt","Ausführlich"),if(fullTable)1 else 0){fullTable=it==1}
    LeagueTable(w,tier,fullTable)
    TopScorersSection(w,tier)
    val rounds=(w.leagues.first{it.tier==tier}.clubIds.size-1)*2;if(day !in 1..rounds)day=1
    Pick("Spieltag",day,(1..rounds).toList(),{"Spieltag $it"}){day=it}
    Section("Spieltag $day"){w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.tier==tier&&it.matchday==day}.forEach{f->CompetitionResultRow(w,f)}}
   }
   cupName->{
    val count=w.fixtures.count{it.competition==CompetitionType.NATIONAL_CUP&&it.round==1}
    Section(cupName){Text("${count*2} Vereine · sechs K.-o.-Runden · jede Runde ein Spiel. Bei Gleichstand folgen Verlängerung und Elfmeterschießen.",color=Muted);if(real)Text("Im DFB-Pokal starten 64 deutsche Vereine aus dem im Spiel enthaltenen Ligabaum.",color=Grass);val own=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)};if(own.isEmpty())Text("Dein Verein ist in dieser Saison nicht qualifiziert.",color=Muted)else Text("Dein Weg: ${own.count{it.played}} von ${own.size} bislang angesetzten Partien.",color=Grass)}
    val rounds=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP}.groupBy{it.round}.toSortedMap();rounds.forEach{(_,games)->Section(games.first().stage){games.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
   }
   CompetitionType.CLUB_WORLD_CUP.label->{
    val all=w.fixtures.filter{it.competition==CompetitionType.CLUB_WORLD_CUP}
    val groups=all.filter{it.stage=="Gruppenphase"}
    val ownIn=all.any{it.homeId==w.user.clubId||it.awayId==w.user.clubId}
    Section(CompetitionType.CLUB_WORLD_CUP.label){
     Text("32 Vereine · acht Vierergruppen. Die ersten zwei jeder Gruppe erreichen das Achtelfinale; danach geht es direkt im K.-o.-System bis zum Finale.",color=Muted)
     Text("Siegerprämie: ${euros(CompetitionPrizeSystem.clubWorldCupPrize(w))}.",color=Grass)
     if(!ownIn)Text("Dein Verein ist in dieser Saison nicht qualifiziert.",color=Muted)
    }
    if(groups.isNotEmpty()){
     if(group !in ('A'..'H').map{it.toString()})group="A"
     Pick("Gruppe",group,('A'..'H').map{it.toString()},{"Gruppe $it"}){group=it}
     Section("Gruppe $group"){CompetitionEngine.worldCupGroupTable(w,group).forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);Column(tableRowModifier(c.id==w.user.clubId).padding(horizontal=6.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){RankChip(i+1,if(i<2)Grass else null);Crest(c.logo,c.primary,c.secondary,Modifier.size(22.dp));Text("${c.name} (${c.shortName})",Modifier.weight(1f),fontWeight=if(i<2)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${r.points} P",fontWeight=FontWeight.Bold)};Text("Sp ${r.played} · S ${r.won} · U ${r.drawn} · N ${r.lost} · ${r.goalsFor}:${r.goalsAgainst}",Modifier.padding(start=34.dp),color=Muted,style=MaterialTheme.typography.bodySmall)}}}
     Section("Gruppenspiele"){groups.filter{it.group==group}.sortedWith(compareBy<Fixture>{it.round}.thenBy{it.id}).forEach{CompetitionResultRow(w,it)}}
    }
    all.filter{it.stage!="Gruppenphase"}.groupBy{it.round}.toSortedMap().forEach{(_,games)->Section(games.first().stage){games.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
   }
   CompetitionType.ETERNAL_CROWN.label->{
    val games=w.fixtures.filter{it.competition==CompetitionType.ETERNAL_CROWN}
    val ownIn=games.any{it.homeId==w.user.clubId||it.awayId==w.user.clubId}||w.user.clubId in w.fantasyCupByes
    Section(CompetitionType.ETERNAL_CROWN.label){
     Text("Die besten sechs Vereine jeder spielbaren Liga qualifizieren sich. Falls die Teilnehmerzahl keine Zweierpotenz ist, gibt es zuerst eine Vorrunde mit Freilosen; danach folgt ausschließlich K.-o. bis zum Finale.",color=Muted)
     Text("Siegprämien steigen je Runde um 200.000 € · Turniersieger zusätzlich ${euros(CompetitionPrizeSystem.eternalCrownPrize(w))}.",color=Grass)
     if(!ownIn)Text("Dein Verein ist in dieser Saison nicht qualifiziert.",color=Muted)
    }
    if(games.isEmpty())Section("Turnierstatus"){Text("Für diese Saison sind keine Partien angesetzt.",color=Muted)}
    else games.groupBy{it.round}.toSortedMap().forEach{(_,roundGames)->Section(roundGames.first().stage){roundGames.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
   }
   else->{
    val type=when(competition){CompetitionType.CHAMPIONS_LEAGUE.label->CompetitionType.CHAMPIONS_LEAGUE;CompetitionType.EUROPA_LEAGUE.label->CompetitionType.EUROPA_LEAGUE;else->CompetitionType.EURO_ELITE}
    val leaguePhase=w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"};val modern=real&&type in listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE)&&leaguePhase.isNotEmpty()&&leaguePhase.all{it.group.isBlank()}
    if(modern){
     val ownInCompetition=w.fixtures.any{it.competition==type&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)}
     Section(type.label){Text("36 Vereine · acht Ligaphasen-Spiele pro Club, davon vier zuhause und vier auswärts. Platz 1–8 erreicht direkt das Achtelfinale, Platz 9–24 spielt die Play-offs.",color=Muted);Text("Ab den Play-offs werden die Duelle bis einschließlich Halbfinale mit Hin- und Rückspiel ausgetragen; das Finale ist ein einzelnes Spiel.",color=Grass);if(!ownInCompetition)Text("Dein Verein ist in dieser Saison nicht für diesen Wettbewerb qualifiziert.",color=Muted)}
     EuroLeaguePhaseTable(w,type)
     if(euroRound !in 1..8)euroRound=1
     Pick("Ligaphasen-Spieltag",euroRound,(1..8).toList(),{"Runde $it"}){euroRound=it}
     Section("Ligaphase · Runde $euroRound"){leaguePhase.filter{it.round==euroRound}.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}
     val knockouts=w.fixtures.filter{it.competition==type&&it.stage!="Ligaphase"}.groupBy{it.round}.toSortedMap();knockouts.forEach{(_,games)->Section(games.first().stage){games.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
    }else{
     Section(type.label){Text("32 Vereine · acht Vierergruppen · Hin- und Rückspiele. Die ersten zwei jeder Gruppe erreichen das Achtelfinale.",color=Muted);Text("Internationale Fantasievereine treffen auf die stärksten Clubs der obersten nationalen Liga.",color=Grass)}
     Pick("Gruppe",group,('A'..'H').map{it.toString()},{"Gruppe $it"}){group=it}
     Section("Gruppe $group"){CompetitionEngine.groupTable(w,group,type).forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);Column(tableRowModifier(c.id==w.user.clubId).padding(horizontal=6.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){RankChip(i+1,if(i<2)Grass else null);Crest(c.logo,c.primary,c.secondary,Modifier.size(22.dp));Text("${c.name} (${c.shortName})",Modifier.weight(1f),fontWeight=if(i<2)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${r.points} P",fontWeight=FontWeight.Bold)};Text("Sp ${r.played} · S ${r.won} · U ${r.drawn} · N ${r.lost} · ${r.goalsFor}:${r.goalsAgainst}",Modifier.padding(start=34.dp),color=Muted,style=MaterialTheme.typography.bodySmall)}}}
     Section("Gruppenspiele"){w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"&&it.group==group}.sortedWith(compareBy<Fixture>{it.round}.thenBy{it.id}).forEach{f->Text("RUNDE ${f.round}",color=Faint,style=MaterialTheme.typography.labelSmall);CompetitionResultRow(w,f)}}
     val knockouts=w.fixtures.filter{it.competition==type&&it.stage!="Ligaphase"}.groupBy{it.round}.toSortedMap();knockouts.forEach{(_,games)->Section(games.first().stage){games.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
    }
   }
  }
 }
}

@Composable private fun LiveLineupEditor(w:World,m:LiveMatch,home:Boolean,selectedOut:Int,onSelect:(Int)->Unit,onMove:(Int,Int)->Unit){
 val formation=if(home)m.homeFormation else m.awayFormation;val lineup=if(home)m.homeXi else m.awayXi;val slots=Formations.positions(formation);val coords=Formations.coordinates(formation)
 var dragging by remember(m.fixtureId,lineup.hashCode(),formation){mutableIntStateOf(-1)};var dragOffset by remember{mutableStateOf(Offset.Zero)}
 val density=LocalDensity.current
 BoxWithConstraints(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(PitchGreenLight,PitchGreen))).border(1.dp,LineStrong,RoundedCornerShape(18.dp)).padding(6.dp)){
  val usableW=with(density){(maxWidth-68.dp).toPx()}.coerceAtLeast(1f);val usableH=with(density){(maxHeight-74.dp).toPx()}.coerceAtLeast(1f)
  coords.forEachIndexed{i,(x,y)->
   val id=lineup.getOrNull(i)?:0;val p=w.players[id];val target=slots.getOrElse(i){p?.position?:Position.ZM};val isDragging=dragging==i;val selected=id!=0&&id==selectedOut
   val dx=if(isDragging)dragOffset.x else 0f;val dy=if(isDragging)dragOffset.y else 0f
   val mod=Modifier.offset((maxWidth-68.dp)*x,(maxHeight-74.dp)*y).width(68.dp).heightIn(min=66.dp).graphicsLayer{translationX=dx;translationY=dy}.border(if(selected)3.dp else 1.dp,if(id==0)Clay else if(selected)Gold else Chalk.copy(alpha=.28f),RoundedCornerShape(12.dp)).background(if(id==0)Clay.copy(alpha=.15f) else if(selected)Gold.copy(alpha=.18f) else Ink.copy(alpha=.62f),RoundedCornerShape(12.dp)).clickable(enabled=id!=0){onSelect(id)}.pointerInput(i,id,lineup.hashCode(),formation){
    if(id!=0)detectDragGesturesAfterLongPress(onDragStart={dragging=i;dragOffset=Offset.Zero},onDragCancel={dragging=-1;dragOffset=Offset.Zero},onDragEnd={
     if(dragging==i){val nx=(x+dragOffset.x/usableW).coerceIn(0f,1f);val ny=(y+dragOffset.y/usableH).coerceIn(0f,1f);val drop=coords.indices.minByOrNull{j->val ax=coords[j].first-nx;val ay=coords[j].second-ny;ax*ax+ay*ay}?:i;if(drop!=i)onMove(i,drop)};dragging=-1;dragOffset=Offset.Zero
    },onDrag={change,amount->change.consume();dragOffset=dragOffset+amount})
   }
   Column(mod.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
    if(p==null){Text("LÜCKE",color=Clay,fontWeight=FontWeight.Bold,fontSize=10.sp);Text(target.name,color=Muted,fontSize=9.sp)}else{
     val rating=m.playerPerformance[id]?.rating?:6.5;Text("${p.number} · ${p.lastName.take(8)}",fontWeight=FontWeight.Bold,fontSize=10.sp,maxLines=1);Text(target.name,color=suitabilityColorForMatch(p,target),fontSize=9.sp);Text("${p.fitness.roundToInt()}% · ${String.format(java.util.Locale.GERMANY,"%.1f",rating)}${if((m.yellows[id]?:0)>0)" · 🟨" else ""}",color=if(p.fitness<60)Clay else Muted,fontSize=9.sp,maxLines=1)
    }
   }
  }
 }
 Text("Tippen = Spieler für Wechsel wählen · halten & ziehen = Position tauschen/Lücke nach Platzverweis verschieben.",color=Faint,style=MaterialTheme.typography.bodySmall)
}
private fun statNumber(text: String)=text.replace(',','.').filter{it.isDigit()||it=='.'}.toDoubleOrNull()

/** Vergleichszeile Heim/Gast mit zweiseitigem Balken. */
@Composable fun StatLine(label: String,left: String,right: String,leftColor: Color=Grass,rightColor: Color=Blue){
 val l=statNumber(left);val r=statNumber(right)
 Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Text(left,Modifier.weight(1f),style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"),fontWeight=if(l!=null&&r!=null&&l>r)FontWeight.Bold else FontWeight.Normal,color=if(l!=null&&r!=null&&l>r)Chalk else Muted)
   Text(label,color=Muted,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center)
   Text(right,Modifier.weight(1f),textAlign=TextAlign.End,style=MaterialTheme.typography.labelLarge.copy(fontFeatureSettings="tnum"),fontWeight=if(l!=null&&r!=null&&r>l)FontWeight.Bold else FontWeight.Normal,color=if(l!=null&&r!=null&&r>l)Chalk else Muted)
  }
  if(l!=null&&r!=null){
   val total=(l+r).takeIf{it>0}
   Row(Modifier.fillMaxWidth().height(5.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
    Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Track)){Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(total==null)0f else (l/total).toFloat()).fillMaxHeight().clip(CircleShape).background(leftColor))}
    Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Track)){Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(if(total==null)0f else (r/total).toFloat()).fillMaxHeight().clip(CircleShape).background(rightColor))}
   }
  }
 }
}

@Composable private fun TickerRow(clock: String,text: String,tone: String){
 val accent=when(tone){"goal","decision"->Grass;"bad"->Clay;else->null}
 Row(Modifier.fillMaxWidth().background(if(tone=="goal")Grass.copy(alpha=.09f) else Color.Transparent,RoundedCornerShape(10.dp)).padding(horizontal=6.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top){
  Box(Modifier.width(44.dp).background(accent?.copy(alpha=.15f)?:Inset,RoundedCornerShape(7.dp)).padding(vertical=3.dp),contentAlignment=Alignment.Center){Text("$clock′",color=accent?:Muted,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Bold,fontFeatureSettings="tnum"),maxLines=1)}
  Text(text,Modifier.weight(1f),color=if(accent==Clay)Clay else if(accent!=null)Chalk else Chalk.copy(alpha=.86f),fontWeight=if(tone=="goal")FontWeight.Bold else FontWeight.Normal,style=MaterialTheme.typography.bodyMedium)
 }
}

@Composable private fun Scoreboard(w: World,m: LiveMatch,h: Club,a: Club,running: Boolean){
 val live=!m.finished&&running&&!m.incidentPause&&!m.halfTime
 val (status,statusColor)=when{m.finished->"ABPFIFF" to Muted;m.halfTime->m.breakType.label.uppercase() to Gold;m.incidentPause->"AUTO-STOP" to Clay;!running->"PAUSE" to Muted;else->"LIVE" to Clay}
 Surface(color=Panel,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,LineStrong),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.drawBehind{
   val r=(size.width*.65f).coerceAtLeast(1f)
   drawRect(Brush.radialGradient(listOf(Color(h.primary).copy(alpha=.28f),Color.Transparent),center=Offset(0f,size.height*.55f),radius=r))
   drawRect(Brush.radialGradient(listOf(Color(a.primary).copy(alpha=.28f),Color.Transparent),center=Offset(size.width,size.height*.55f),radius=r))
  }.padding(horizontal=12.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.background(Inset,CircleShape).border(1.dp,Line,CircleShape).padding(horizontal=12.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    if(!m.finished)LiveDot(live,statusColor)
    Text(status,color=statusColor,style=MaterialTheme.typography.labelSmall)
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    TeamColumn(h,h.id==w.user.clubId,Modifier.weight(1f))
    Column(Modifier.weight(1.1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
     Text("${m.home.goals}:${m.away.goals}",style=MaterialTheme.typography.displaySmall,color=Chalk,maxLines=1)
     if(m.homePens>0||m.awayPens>0)Text("${m.homePens}:${m.awayPens} i. E.",color=Gold,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
     Text("${MatchEngine.clockLabel(m)}′",color=Grass,style=MaterialTheme.typography.titleMedium.copy(fontFeatureSettings="tnum"))
    }
    TeamColumn(a,a.id==w.user.clubId,Modifier.weight(1f))
   }
  }
 }
}
@Composable fun MatchScreen(state: GameState,vm: GameViewModel,speed: MatchSpeed){
 val w=state.world?:return;val m=w.live;var userPaused by rememberSaveable(m?.fixtureId){mutableStateOf(false)};var foreground by remember{mutableStateOf(true)};val running=foreground&&!userPaused;var liveTab by rememberSaveable(m?.fixtureId){mutableIntStateOf(0)};var showLiveGraphic by rememberSaveable(m?.fixtureId){mutableStateOf(true)};val lifecycle=LocalLifecycleOwner.current.lifecycle
 DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,e->when(e){Lifecycle.Event.ON_RESUME->foreground=true;Lifecycle.Event.ON_PAUSE,Lifecycle.Event.ON_STOP->foreground=false;else->{}}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
 LaunchedEffect(running,speed,m?.fixtureId){m?.let{vm.setLiveRunning(running,speed,it.fixtureId)}}
 DisposableEffect(m?.fixtureId){onDispose{m?.let{vm.setLiveRunning(false,speed,it.fixtureId)}}}
 if(m==null){
  Page("${w.nextFixture()?.let{CompetitionEngine.displayName(w,it.competition)}?:"Spieltag"} ${w.calendar.matchday}","KREIDE AN DEN SCHUHEN"){
   w.nextFixture()?.let{NextGame(w,it,"Anpfiff"){vm.startMatch();userPaused=false}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    StatTile("Formation",w.club().tactics.formation,Modifier.weight(1f))
    StatTile("Mentalität","${w.club().tactics.mentality} / 5",Modifier.weight(1f))
   }
   Section("Vor dem Anpfiff"){
    val missing=w.squad().filter{!it.available&&!it.youth}
    if(missing.isEmpty())ListRow("Alle Spieler sind verfügbar","Der komplette Profikader steht bereit.",Icons.Filled.CheckCircle,Grass)
    else missing.forEach{ListRow(it.name,if(it.injuryWeeks>0)it.injury else it.unavailableReason?.label,Icons.Filled.Warning,Clay)}
   }
  };return
 }
 val soundsEnabled by vm.soundsEnabled.collectAsState()
 val tacticalMatchView by vm.tacticalMatchView.collectAsState()
 MatchAudioEffects(w,m,soundsEnabled)
 val h=w.clubs.getValue(m.homeId);val a=w.clubs.getValue(m.awayId);val ownHome=m.homeId==w.user.clubId;val ownClub=w.club();var livePreset by rememberSaveable(m.fixtureId){mutableStateOf("Individuell")}
 val ownXiRaw=if(ownHome)m.homeXi else m.awayXi;val xi=ownXiRaw.filter{it!=0};val bench=if(ownHome)m.homeBench else m.awayBench;val ownSubs=if(ownHome)m.homeSubs else m.awaySubs;val ownFormation=if(ownHome)m.homeFormation else m.awayFormation;val conserve=if(ownHome)m.homeConserveEnergy else m.awayConserveEnergy;val allOut=if(ownHome)m.homeAllOutAttack else m.awayAllOutAttack;val controlGame=if(ownHome)m.homeControlGame else m.awayControlGame
 Page(if(m.finished)"Abpfiff" else if(m.halfTime)m.breakType.label else if(m.incidentPause)"Spiel unterbrochen" else "Am Seitenrand","${m.weather.uppercase()} · ${m.attendance} ZUSCHAUER"){
  Scoreboard(w,m,h,a,running)
  Section{
   if(!m.finished){
    if(m.halfTime)Action(MatchEngine.breakActionLabel(m),true){vm.secondHalf();userPaused=false}
    else if(!m.incidentPause)Action(if(userPaused)"Live fortsetzen" else "Pause",true){userPaused=!userPaused}
    if(!m.halfTime&&!m.incidentPause&&!m.pendingDecision&&!m.assistantSubPending){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      if(m.period==1)Box(Modifier.weight(1f)){Action("Bis Halbzeit",!state.busy,true){userPaused=true;vm.quickSimulate(true)}}
      Box(Modifier.weight(1f)){Action("Spiel simulieren",!state.busy,true){userPaused=true;vm.quickSimulate(false)}}
     }
     Text("Simulation nutzt dieselbe Match-KI wie das Live-Spiel.",color=Faint,style=MaterialTheme.typography.bodySmall)
    }
    Text("Spieltempo",color=Muted,style=MaterialTheme.typography.labelMedium)
    SegmentedTabs(MatchSpeed.entries.map{it.label},MatchSpeed.entries.indexOf(speed)){vm.setMatchSpeed(MatchSpeed.entries[it])}
   }else Action(if(w.fixtures.firstOrNull{it.id==m.fixtureId}?.competition==CompetitionType.LEAGUE)"Spieltag bestätigen & weiter" else "Pokalpartie bestätigen & weiter",!state.busy){userPaused=true;vm.finishWeek()}
  }

  if(!m.finished){
   SegmentedTabs(listOf("Live","Taktik","Statistik","Analyse"),liveTab){liveTab=it}
   if(liveTab==0)Section("Spielfeld"){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Live-Grafik",style=MaterialTheme.typography.bodyMedium,color=Muted);Switch(showLiveGraphic,{showLiveGraphic=it})}
    if(showLiveGraphic)LiveMatchAnimation(w,m,MatchEngine.liveFrameDurationMs(m.livePhase,speed),tacticalMatchView) else Text("Die Spielfeld-Grafik ist ausgeblendet. Das Match läuft normal weiter.",color=Muted)
    Text(if(tacticalMatchView)"Taktische Draufsicht aktiv: alle Spieler bewegen sich mit Ballposition, Ballbesitz, Formation und Pressing; der Ballbesitzer wird groß angezeigt. Bei Abschlüssen wechselt die Ansicht in die bekannte Torkamera." else "Klassische Matchansicht aktiv. Die taktische Draufsicht kannst du in den Einstellungen einschalten.",color=Faint,style=MaterialTheme.typography.bodySmall)
   }else if(liveTab==2) LiveStatistics(w,m,h,a) else if(liveTab==3) MatchAnalysisView(w,m)
  }else{
   LiveStatistics(w,m,h,a,"Endstand & Statistik")
   MatchAnalysisView(w,m,"Analyse & Taktik-Impact")
   Section("Spielerbewertungen"){
    for((club,lineup) in listOf(h to m.participation.filter{w.players[it]?.clubId==h.id},a to m.participation.filter{w.players[it]?.clubId==a.id})){
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Crest(club.logo,club.primary,club.secondary,Modifier.size(26.dp));Text(club.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
     lineup.distinct().sortedByDescending{m.playerPerformance[it]?.rating?:6.5}.forEach{id->val p=w.players[id]?:return@forEach;val perf=m.playerPerformance[id]?:PlayerMatchPerformance();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);val keeperStats=if(p.position==Position.TW)" · ${perf.saves} Paraden · ${perf.goalsConceded} GT" else "";Text("${perf.minutes} Min. · ${perf.goals} Tore · ${perf.assists} Assists$keeperStats · ${perf.summary}",color=Muted,style=MaterialTheme.typography.bodySmall)};RatingBadge(String.format(java.util.Locale.GERMANY,"%.1f",perf.rating),when{perf.rating>=8.0->Grass;perf.rating<6.0->Clay;else->Gold},42.dp)}}
     Spacer(Modifier.height(6.dp))
    }
   }
  }

  if(m.incidentPause){
   val player=w.players[m.incidentPlayerId];val ownIncident=m.incidentClubId==w.user.clubId;val injuryStillOn=ownIncident&&m.incidentReason==MatchPauseReason.INJURY&&m.incidentPlayerId in ownXiRaw;val canReplace=ownSubs<5&&bench.any{w.players[it]?.available==true}
   Section("Automatische Spielunterbrechung"){
    Pill(m.incidentReason.label,Clay)
    Text(when(m.incidentReason){MatchPauseReason.RED_CARD->if(ownIncident)"${player?.name?:"Ein Spieler"} ist vom Platz. Ordne Formation und Wechsel neu, bevor du fortsetzt." else "Der Gegner hat einen Platzverweis. Du kannst Formation und Mentalität sofort anpassen.";MatchPauseReason.INJURY->"${player?.name?:"Ein Spieler"} kann nicht weiterspielen. Die Uhr steht, bis die Situation geklärt ist.";else->"Das Spiel ist unterbrochen."})
    if(injuryStillOn&&canReplace)Text("Wechsle zuerst den verletzten Spieler.",color=Gold)
    Action("Spiel fortsetzen",!injuryStillOn||!canReplace){vm.resumeIncident();userPaused=false}
   }
  }

  if(!m.finished&&liveTab==1)Section("Live-Taktik"){
   Pick("Formation",ownFormation,Formations.all.keys.toList(),{it}){vm.changeFormation(it)}
   Pick("Taktik-Vorlage",livePreset,tacticPresets,{it}){v->livePreset=v;if(v!="Individuell")vm.updateLiveTactic{world->applyTacticPreset(world.club().tactics,v);world.live?.let{live->if(ownHome)live.homeMentality=world.club().tactics.mentality else live.awayMentality=world.club().tactics.mentality}}}
   StepSlider("Mentalität · defensiv bis offensiv",if(ownHome)m.homeMentality else m.awayMentality,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.mentality=v;it.live?.let{live->if(ownHome)live.homeMentality=v else live.awayMentality=v}}}
   StepSlider("Pressing",ownClub.tactics.pressing,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.pressing=v}}
   StepSlider("Tempo",ownClub.tactics.tempo,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.tempo=v}}
   StepSlider("Breite",ownClub.tactics.width,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.width=v}}
   StepSlider("Defensivlinie",ownClub.tactics.line,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.line=v}}
   Pick("Spielaufbau",ownClub.tactics.buildUp,BuildUp.entries.toList(),{it.label}){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.buildUp=v}}
   Text(tacticSummary(ownClub.tactics),color=Muted,style=MaterialTheme.typography.bodySmall)
   val liveImpact=when{allOut->"Aktiv: Angriff stark erhöht · Defensive/Konteranfälligkeit bleibt auf vollem Risiko · normaler Mehrverbrauch";conserve->"Aktiv: deutlich tieferer Block · stärkere Konter · rund 40 % weniger Grundverbrauch";controlGame->"Aktiv: deutlich mehr Ballsicherheit/Ballbesitz · merklich weniger Vorwärtsrisiko";else->"Regler wirken direkt auf Chancenaufbau, Pressing, Ballverluste, Abseits, Fouls und Fitness."}
   Text(liveImpact,color=if(allOut||conserve||controlGame)Gold else Muted,style=MaterialTheme.typography.bodySmall)
   Text("Sofort-Anweisungen",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Kräfte schonen",fontWeight=FontWeight.Bold);Text("Haramball-Modus: tiefer Block, fast alle hinter dem Ball und nach Ballgewinn schnell kontern. Spart Kraft und gibt Ballbesitz ab, ohne die Konter künstlich abzuwürgen.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(conserve,{vm.setConserveEnergy(it)})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Alles nach vorn",fontWeight=FontWeight.Bold);Text("Volles Risiko: hohe Linie, mehr Vorwärtsdrang und mehr Abschlüsse. Kann einen Rückstand drehen, kostet aber Kraft und öffnet große Räume für gegnerische Konter.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(allOut,{vm.setAllOutAttack(it)})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Spiel kontrollieren",fontWeight=FontWeight.Bold);Text("Ball halten, Tempo herausnehmen und Risiken reduzieren. Gut zum Verwalten einer Führung, dafür entstehen weniger direkte Abschlüsse.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(controlGame,{vm.setControlGame(it)})}
  }

  if(!m.finished&&liveTab==1)Section("Wechsel & Co-Trainer"){
   val incidentOut=if(m.incidentPause&&m.incidentReason==MatchPauseReason.INJURY&&m.incidentPlayerId in xi)m.incidentPlayerId else 0
   var out by remember(m.fixtureId,m.incidentPause,m.incidentPlayerId){mutableIntStateOf(incidentOut)};var incoming by remember(m.fixtureId){mutableIntStateOf(0)}
   val suggestions=remember(m.minute/5,xi.hashCode(),bench.hashCode(),ownFormation){MatchEngine.substitutionSuggestions(w,m)};val maxSubs=CompetitionRulesEngine.maxSubs(w,m);val subIssue=CompetitionRulesEngine.substitutionIssue(w,m,ownHome);val windows=if(ownHome)m.homeSubWindows else m.awaySubWindows
   Text("Wechsel: $ownSubs / $maxSubs · Wechselgelegenheiten $windows / ${CompetitionRulesEngine.maxWindows(w,m)}",color=Muted);if(subIssue!=null)Text(subIssue,color=Gold,style=MaterialTheme.typography.bodySmall)
   Text("Live-Aufstellung",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   LiveLineupEditor(w,m,ownHome,out,{out=it},{from,to->vm.moveLiveLineupSlot(from,to)})
   if(ownXiRaw.any{it==0})Text("Die rote LÜCKE zeigt den Platzverweis. Ziehe einen aktiven Spieler darauf, um die Unterzahl taktisch anders zu verteilen.",color=Gold,style=MaterialTheme.typography.bodySmall)
   if(suggestions.isNotEmpty()){
    Text("Co-Trainer-Empfehlungen",style=MaterialTheme.typography.titleMedium);Text("Priorisiert schwache Live-Leistung, Fitness, Form, Gelb-Risiko, Spielstand und Positionspassung. Jeder Raus- und Rein-Spieler erscheint nur einmal.",color=Muted,style=MaterialTheme.typography.bodySmall)
    suggestions.forEachIndexed{i,sug->val po=w.players.getValue(sug.outId);val pi=w.players.getValue(sug.inId);Surface(color=if(out==sug.outId&&incoming==sug.inId)Grass.copy(alpha=.10f) else PanelHigh,shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,if(out==sug.outId&&incoming==sug.inId)Grass.copy(alpha=.4f) else Line),modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{out=sug.outId;incoming=sug.inId}){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${i+1}. ${po.shortName} → ${pi.shortName}",fontWeight=FontWeight.Bold);SuitabilityBadge(pi,sug.target)};Text(sug.reason,color=Muted,style=MaterialTheme.typography.bodySmall)}}}
   }
   Text("Ersatzbank",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   val selectedOut=out.takeIf{it in xi};val targetIndex=selectedOut?.let{ownXiRaw.indexOf(it)}?:-1;val target=if(targetIndex>=0)Formations.positions(ownFormation).getOrNull(targetIndex) else null
   if(selectedOut==null)Text("Zuerst einen Spieler in der Live-Aufstellung antippen. Danach führt ein Tipp auf einen Ersatzspieler den Wechsel direkt aus.",color=Gold,style=MaterialTheme.typography.bodySmall)else Text("${w.players[selectedOut]?.name?:"Spieler"} ist ausgewählt · Ersatzspieler antippen = Wechsel ausführen.",color=Grass,style=MaterialTheme.typography.bodySmall)
   val orderedBench=if(target==null)bench.sortedByDescending{w.players[it]?.ca?:0}else bench.sortedByDescending{w.players[it]?.ratingAt(target)?:0}
   orderedBench.forEach{id->val p=w.players[id]?:return@forEach;val fit=target?.let{p.ratingAt(it)}?:p.ca;Surface(color=if(id==incoming)Grass.copy(alpha=.10f) else PanelHigh,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,if(id==incoming)Grass.copy(alpha=.4f) else Line),modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(enabled=p.available&&subIssue==null){incoming=id;if(out in xi)vm.substitute(out,id)}){Row(Modifier.fillMaxWidth().padding(11.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.position.label} · Fitness ${p.fitness.roundToInt()} % · Form ${(p.form*10).roundToInt()/10.0}",color=Muted,style=MaterialTheme.typography.bodySmall)};Pill("$fit",if(target==null)Grass else suitabilityColorForMatch(p,target))}}}
   if(bench.isEmpty())Text("Keine Ersatzspieler mehr verfügbar.",color=Muted)
  }

  if(liveTab==0){val tickerRows=remember(m.ticker.size){m.ticker.takeLast(30).asReversed()};Section("Live-Ticker"){Column(verticalArrangement=Arrangement.spacedBy(2.dp)){tickerRows.forEach{t->TickerRow(t.clockLabel.ifBlank{t.minute.toString()},t.text,t.tone)}}}}
 }
 if(m.assistantSubPending){
  val outs=(m.assistantSubOutIds.ifEmpty{mutableListOf(m.assistantSubOutId)});val ins=(m.assistantSubInIds.ifEmpty{mutableListOf(m.assistantSubInId)});val forced=outs.any{it in m.injured}
  var selectedAssistantOuts by remember(m.assistantSubSuggestedMinute,outs.hashCode(),ins.hashCode()){mutableStateOf(outs.toSet())}
  AlertDialog(onDismissRequest={},title={Text(if(forced)"Co-Trainer: Verletzungswechsel" else if(outs.size>1)"Co-Trainer: Wechsel auswählen" else "Co-Trainer: Wechselvorschlag")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("${MatchEngine.clockLabel(m)}. Minute · ${m.home.goals}:${m.away.goals}",color=Muted)
   if(outs.size>1)Text("Wähle per Klick genau die Wechsel aus, die du übernehmen willst. Nicht gewählte Vorschläge werden kurzfristig nicht erneut vorgeschlagen.",color=Gold,style=MaterialTheme.typography.bodySmall)
   outs.forEachIndexed{i,outId->val inId=ins.getOrNull(i)?:return@forEachIndexed;val outPlayer=w.players[outId]?:return@forEachIndexed;val inPlayer=w.players[inId]?:return@forEachIndexed;val ownHome=w.user.clubId==m.homeId;val lineup=if(ownHome)m.homeXi else m.awayXi;val slotIndex=lineup.indexOf(outId);val target=Formations.positions(if(ownHome)m.homeFormation else m.awayFormation).getOrElse(slotIndex.coerceAtLeast(0)){outPlayer.position};Row(Modifier.fillMaxWidth().clickable{selectedAssistantOuts=if(outId in selectedAssistantOuts)selectedAssistantOuts-outId else selectedAssistantOuts+outId},verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)){Checkbox(outId in selectedAssistantOuts,{checked->selectedAssistantOuts=if(checked)selectedAssistantOuts+outId else selectedAssistantOuts-outId});Column(Modifier.weight(1f)){Text("${outPlayer.name} → ${inPlayer.name}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("${target.label} · Eignung ${inPlayer.ratingAt(target)}/${PLAYER_RATING_MAX} · ${m.assistantSubReasons.getOrNull(i).orEmpty()}",color=Muted,style=MaterialTheme.typography.bodySmall)}}}
   if(forced)Text("Ein abgewählter verletzter Spieler führt anschließend zur manuellen Verletzungsunterbrechung.",color=Gold,style=MaterialTheme.typography.bodySmall)
  }},confirmButton={Button(onClick={vm.acceptAssistantSubstitution(selectedAssistantOuts);userPaused=false},enabled=selectedAssistantOuts.isNotEmpty()){Text(if(selectedAssistantOuts.size>1)"${selectedAssistantOuts.size} Wechsel annehmen" else "Auswahl annehmen")}},dismissButton={OutlinedButton(onClick={vm.rejectAssistantSubstitution();userPaused=false}){Text("Alle ablehnen")}})
 }
 if(m.pendingDecision){
  val context=remember(m.liveEventSerial,m.ballX,m.ballY,m.homeXi.hashCode(),m.awayXi.hashCode()){MatchEngine.playerDecisionContext(w,m)}
  val choices=listOf(Decision.SHOOT,Decision.DRIBBLE,Decision.CROSS,Decision.THROUGH_PASS,Decision.PASS)
  AlertDialog(onDismissRequest={},title={Text("Du am Ball")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("${MatchEngine.clockLabel(m)}. Minute · ${m.home.goals}:${m.away.goals}. Was machst du?")
   Surface(color=Inset,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){Text("Distanz zum Tor: ca. ${context.distanceMeters} m",fontWeight=FontWeight.Bold);Text("Gegnerdruck: ${context.pressureLabel} · ca. ${context.nearbyOpponents} Gegenspieler in der Nähe",color=Muted,style=MaterialTheme.typography.bodySmall)}}
   Text("Schuss = direkter Abschluss · Dribbling = Raumgewinn mit Ballverlustrisiko · Flanke/Steilpass = Chance kreieren · Sicherungspass = Ballbesitz priorisieren.",color=Muted,style=MaterialTheme.typography.bodySmall)
   choices.forEach{d->Action(d.label,true,d!=Decision.SHOOT){vm.decide(d)}}
  }},confirmButton={})
 }
}

@Composable private fun LiveStatistics(w: World,m: LiveMatch,h: Club,a: Club,title: String="Aktuelle Live-Statistiken"){
 val poss=if(m.home.possessionTicks+m.away.possessionTicks==0)50 else (100.0*m.home.possessionTicks/(m.home.possessionTicks+m.away.possessionTicks)).roundToInt()
 Section(title){
  val hc=Color(h.primary).let{if(it.luminance()<.08f)Grass else it};val ac=Color(a.primary).let{if(it.luminance()<.08f||(it.red-hc.red)*(it.red-hc.red)+(it.green-hc.green)*(it.green-hc.green)+(it.blue-hc.blue)*(it.blue-hc.blue)<.04f)Blue else it}
  StatLine("xG",dec(m.home.xg),dec(m.away.xg),hc,ac);StatLine("Schüsse gesamt","${m.home.shots}","${m.away.shots}",hc,ac);StatLine("Schüsse aufs Tor","${m.home.shotsOnTarget}","${m.away.shotsOnTarget}",hc,ac);StatLine("Schüsse neben das Tor","${m.home.shotsOffTarget}","${m.away.shotsOffTarget}",hc,ac);if(m.home.blockedShots+m.away.blockedShots>0)StatLine("Geblockt","${m.home.blockedShots}","${m.away.blockedShots}",hc,ac);StatLine("Ballbesitz","$poss %","${100-poss} %",hc,ac);StatLine("Ecken","${m.home.corners}","${m.away.corners}",hc,ac);StatLine("Einwürfe","${m.home.throwIns}","${m.away.throwIns}",hc,ac);StatLine("Abseits","${m.home.offsides}","${m.away.offsides}",hc,ac);if(m.home.varChecks+m.away.varChecks>0)StatLine("VAR-Prüfungen","${m.home.varChecks}","${m.away.varChecks}",hc,ac);StatLine("Fouls","${m.home.fouls}","${m.away.fouls}",hc,ac);val homeYellow=m.yellows.filter{w.players[it.key]?.clubId==m.homeId}.values.sum();val awayYellow=m.yellows.filter{w.players[it.key]?.clubId==m.awayId}.values.sum();val homeRed=m.sentOff.count{w.players[it]?.clubId==m.homeId};val awayRed=m.sentOff.count{w.players[it]?.clubId==m.awayId};StatLine("Gelbe Karten","$homeYellow","$awayYellow",hc,ac);if(homeRed+awayRed>0)StatLine("Platzverweise","$homeRed","$awayRed",hc,ac)
  val active=w.clubs[m.chainOwnerClubId.takeIf{it==m.homeId||it==m.awayId}?:if(m.homeInPossession)m.homeId else m.awayId];Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Aktuelle Phase",color=Muted);Pill("${m.livePhase.label} · ${active?.shortName?:"-"}",if(active?.id==m.homeId)Color(h.primary)else Color(a.primary))};if(m.chainPasses>0||m.chainNarrative.isNotBlank())Text("Angriffskette: ${m.chainPasses} Aktionen · Zone ${m.chainZone+1}/4 · ${m.chainNarrative}",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
}

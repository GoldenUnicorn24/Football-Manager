package de.gruenderelf.app.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun suitabilityColorForMatch(p: Player,target: Position)=when{p.position==target->Grass;p.fit(target)>=.92->Blue;p.fit(target)>=.84->Gold;else->Clay}
@Composable fun FormDot(result: String){Box(Modifier.size(28.dp).background(when(result){"S"->Grass;"N"->Clay;else->Muted},RoundedCornerShape(5.dp)),contentAlignment=Alignment.Center){Text(result,color=Ink,style=MaterialTheme.typography.labelMedium)}}
@Composable fun NextGame(w: World,f: Fixture,label: String=if(w.live==null)"Zum Spiel" else "Partie fortsetzen",onGame: ()->Unit){val home=f.homeId==w.user.clubId;val opponent=w.clubs.getValue(if(home)f.awayId else f.homeId);val host=w.clubs.getValue(f.homeId)
 Section{Text((if(f.competition!=CompetitionType.LEAGUE)"${CompetitionEngine.displayName(w,f.competition,CompetitionEngine.cupGroup(w,f)).uppercase()} · ${f.stage.uppercase()} · " else "")+(if(w.isDerby(f.homeId,f.awayId)&&f.competition==CompetitionType.LEAGUE)"DERBY · " else "")+(if(home)"ZU HAUSE" else "AUSWÄRTS"),color=Grass,style=MaterialTheme.typography.labelMedium,letterSpacing=1.sp);Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){Crest(opponent.logo,opponent.primary,opponent.secondary,Modifier.size(74.dp));Column(Modifier.weight(1f)){Text("${opponent.name} (${opponent.shortName})",style=MaterialTheme.typography.headlineMedium);Text(host.stadium.name,color=Muted);Text("${host.stadium.surface.label} · ${host.stadium.capacity} Plätze",color=Muted,style=MaterialTheme.typography.bodySmall)}};if(opponent.form.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("Gegnerform",color=Muted);opponent.form.forEach{FormDot(it)}};Action(label,onClick=onGame)}
}
@Composable fun HomeScreen(w: World,onGame: ()->Unit,onProfile: ()->Unit){val c=w.club();val p=w.self()
 Page(c.name,"SAISON ${w.calendar.season}/${(w.calendar.season+1)%100} · SPIELTAG ${w.calendar.matchday}"){
  ClubHero(w)
  w.nextFixture()?.let{NextGame(w,it,onGame=onGame)}
  Section{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Vereinskasse",euros(c.budget));Metric("Moral",w.squad().map{it.morale}.average().roundToInt().toString())};Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){Text("Form",color=Muted);if(c.form.isEmpty())Text("Die Geschichte beginnt.",color=Muted);c.form.forEach{FormDot(it)}}}
  val tired=w.squad().filter{it.available&&!it.youth&&it.fitness<65}.sortedBy{it.fitness};val hot=w.squad().filter{!it.youth}.maxByOrNull{it.form}
  Section("Trainer-Notizen"){if(tired.isEmpty())Text("Der Kader wirkt frisch.",color=Grass)else Text("${tired.size} Spieler unter 65 % Fitness. ${tired.take(3).joinToString{it.lastName}} brauchen Aufmerksamkeit.",color=Gold);hot?.let{Text("Beste Form: ${it.name} · ${dec(it.form)}",color=Muted)};Text("Die Kaderseite bewertet jeden Spieler jetzt positionsgenau und zeigt Schwachstellen direkt an.",color=Muted,style=MaterialTheme.typography.bodySmall)}
  Section("Gründer. Trainer. Nummer ${p.number}."){Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){PlayerPortrait(p.appearance,c.kits.home);Column{Text(p.name,style=MaterialTheme.typography.titleLarge);Text("${p.position.label} · Stärke ${p.ca}",color=Grass);Text("${p.stats.goals} Tore · ${p.stats.assists} Vorlagen",color=Muted)}};Meter("Deine Fitness",p.fitness.roundToInt());if(!p.available)Text(if(p.injuryWeeks>0)"${p.injury}: ${p.injuryWeeks} Wochen" else "${p.unavailableReason?.label}",color=Clay);Action("Dein Spielerprofil",secondary=true,onClick=onProfile)}
  Text("Am Spielfeldrand",style=MaterialTheme.typography.titleLarge)
  w.news.take(12).forEach{n->Section{Text(n.title,style=MaterialTheme.typography.titleMedium,color=when(n.tone){"good"->Grass;"bad"->Clay;else->Chalk});Text(n.text);Text("Vereinswoche ${n.week+1}",style=MaterialTheme.typography.labelSmall,color=Muted)}}
 }
}
@Composable private fun CompetitionResultRow(w: World,f: Fixture){
 val h=w.clubs.getValue(f.homeId);val a=w.clubs.getValue(f.awayId)
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Text("${h.shortName} · ${h.name}",Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
  Text(CompetitionEngine.resultText(w,f),Modifier.widthIn(min=54.dp),textAlign=TextAlign.Center,color=if(f.played)Grass else Muted,fontWeight=FontWeight.Bold)
  Text("${a.name} · ${a.shortName}",Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.End,style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun LeagueTable(w: World,tier: Int,full: Boolean){
 val league=w.leagues.first{it.tier==tier};val country=WorldFactory.leagueCountry(w,tier);val germanPyramid=w.privateTopClubMode&&country=="Deutschland"&&tier in 1..5
 Section("$country · ${league.name}"){
  if(!full)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("Pl.",Modifier.width(21.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Verein · Kürzel",Modifier.weight(1f),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Sp.",Modifier.width(20.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Tore",Modifier.width(43.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Pkt.",Modifier.width(24.dp),color=Muted,style=MaterialTheme.typography.labelSmall)}
  val rows=WorldFactory.table(w,tier)
  rows.forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);val bg=if(c.id==w.user.clubId)Color(0xFF284335)else Color.Transparent
   val rankColor=when{germanPyramid&&tier>1&&i<2->Grass;germanPyramid&&tier<5&&i>=rows.size-2->Clay;else->Muted}
   if(full)Column(Modifier.fillMaxWidth().background(bg,RoundedCornerShape(6.dp)).padding(horizontal=7.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${i+1}.",Modifier.width(28.dp),color=rankColor,fontWeight=FontWeight.Bold);Text("${c.name} (${c.shortName})",Modifier.weight(1f),fontWeight=if(c.id==w.user.clubId)FontWeight.Bold else FontWeight.Normal);Text("${r.points} P",fontWeight=FontWeight.Bold)}
    Text("Sp ${r.played} · S ${r.won} · U ${r.drawn} · N ${r.lost} · Tore ${r.goalsFor}:${r.goalsAgainst} · Diff ${if(r.difference>=0)"+" else ""}${r.difference}",color=Muted,style=MaterialTheme.typography.bodySmall)
   }else Row(Modifier.fillMaxWidth().background(bg,RoundedCornerShape(6.dp)).padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("${i+1}",Modifier.width(21.dp),color=rankColor,style=MaterialTheme.typography.labelMedium);Text("${c.name} (${c.shortName})",Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,fontWeight=if(c.id==w.user.clubId)FontWeight.Bold else FontWeight.Normal);Text("${r.played}",Modifier.width(20.dp),style=MaterialTheme.typography.bodySmall);Text("${r.goalsFor}:${r.goalsAgainst}",Modifier.width(43.dp),style=MaterialTheme.typography.bodySmall);Text("${r.points}",Modifier.width(24.dp),fontWeight=FontWeight.Bold)}
  }
  if(germanPyramid){if(tier>1)Text("Grün: Platz 1–2 steigen auf.",color=Grass,style=MaterialTheme.typography.bodySmall);if(tier<5)Text("Tonrot: Die letzten zwei steigen ab.",color=Clay,style=MaterialTheme.typography.bodySmall)}
  else if(w.privateTopClubMode)Text("Für diese Topliga wird außerhalb des abgebildeten Ligabaums kein Auf- oder Abstieg simuliert.",color=Muted,style=MaterialTheme.typography.bodySmall)
  Text("Bei Punktgleichheit: Tordifferenz, erzielte Tore, feste Vereinsreihenfolge.",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
}

@Composable private fun EuroLeaguePhaseTable(w: World,type: CompetitionType){
 Section("Ligaphasen-Tabelle"){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("Pl.",Modifier.width(23.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Verein",Modifier.weight(1f),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Sp.",Modifier.width(22.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Diff",Modifier.width(35.dp),color=Muted,style=MaterialTheme.typography.labelSmall);Text("Pkt.",Modifier.width(26.dp),color=Muted,style=MaterialTheme.typography.labelSmall)}
  CompetitionEngine.euroLeagueTable(w,type).forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);val bg=if(c.id==w.user.clubId)Color(0xFF284335)else Color.Transparent;val rankColor=when{i<8->Grass;i<24->Gold;else->Muted}
   Row(Modifier.fillMaxWidth().background(bg,RoundedCornerShape(6.dp)).padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("${i+1}",Modifier.width(23.dp),color=rankColor,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodySmall);Text("${c.name} (${c.shortName})",Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(c.id==w.user.clubId)FontWeight.Bold else FontWeight.Normal,style=MaterialTheme.typography.bodySmall);Text("${r.played}",Modifier.width(22.dp),style=MaterialTheme.typography.bodySmall);Text("${if(r.difference>=0)"+" else ""}${r.difference}",Modifier.width(35.dp),style=MaterialTheme.typography.bodySmall);Text("${r.points}",Modifier.width(26.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodySmall)}
  }
  Text("Grün: Platz 1–8 direkt ins Achtelfinale · Gold: Platz 9–24 in die Play-offs · Platz 25–36 scheidet aus.",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
}

@Composable fun LeagueScreen(w: World){
 val real=w.privateTopClubMode;val cupName=CompetitionEngine.displayName(w,CompetitionType.NATIONAL_CUP)
 val ownLeague=w.leagues.first{w.user.clubId in it.clubIds};val ownCountry=WorldFactory.leagueCountry(w,ownLeague.tier)
 val cups=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP}.map{CompetitionEngine.cupGroup(w,it)}.distinct().sortedWith(compareBy<String>{if(it==ownCountry)0 else 1}.thenBy{it})
 val cupOptions=cups.map{"Pokal · $it"}
 val competitionOptions=if(real)listOf("Liga")+cupOptions+listOf(CompetitionType.CHAMPIONS_LEAGUE.label,CompetitionType.EUROPA_LEAGUE.label,CompetitionType.CLUB_WORLD_CUP.label)+(if(w.fantasyCupEnabled)listOf(CompetitionType.ETERNAL_CROWN.label)else emptyList()) else listOf("Liga")+cupOptions+CompetitionType.EURO_ELITE.label
 var competition by rememberSaveable{mutableStateOf("Liga")};var country by rememberSaveable(w.user.clubId,w.calendar.season){mutableStateOf(ownCountry)};var tier by rememberSaveable(w.user.clubId,w.calendar.season){mutableIntStateOf(ownLeague.tier)};var day by rememberSaveable(w.calendar.season){mutableIntStateOf(w.calendar.matchday)};var fullTable by rememberSaveable{mutableStateOf(false)};var group by rememberSaveable{mutableStateOf("A")};var euroRound by rememberSaveable(w.calendar.season){mutableIntStateOf(1)}
 if(competition !in competitionOptions)competition="Liga"
 val eyebrow=if(real)"EUROPÄISCHE LIGEN · POKALE · WELTPOKAL" else "LIGA · GRÜNDERPOKAL · EUROPA-ELITELIGA"
 Page("Wettbewerbe",eyebrow){
  Pick("Wettbewerb",competition,competitionOptions,{if(it.startsWith("Pokal · "))CompetitionEngine.cupName(w,it.removePrefix("Pokal · ")) else it}){competition=it}
  when(competition){
   "Liga"->{
    val countries=w.leagues.map{WorldFactory.leagueCountry(w,it.tier)}.distinct().sortedWith(compareBy<String>{if(it==ownCountry)0 else 1}.thenBy{it});if(country !in countries)country=ownCountry
    Pick("Land",country,countries,{it}){selected->country=selected;val options=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==selected}.sortedBy{it.tier};tier=if(selected==ownCountry)ownLeague.tier else options.first().tier;day=1}
    val countryLeagues=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==country}.sortedBy{it.tier};if(countryLeagues.none{it.tier==tier})tier=countryLeagues.first().tier
    Pick("Liga",tier,countryLeagues.map{it.tier},{WorldFactory.leagueName(w,it)}){tier=it;day=1}
    if(tier==ownLeague.tier)Text("Deine aktuelle Liga",color=Grass,style=MaterialTheme.typography.labelMedium)
    Pick("Tabellenansicht",if(fullTable)"Komplett" else "Kurz",listOf("Kurz","Komplett"),{it}){fullTable=it=="Komplett"}
    LeagueTable(w,tier,fullTable)
    val rounds=(w.leagues.first{it.tier==tier}.clubIds.size-1)*2;if(day !in 1..rounds)day=1
    Pick("Spieltag",day,(1..rounds).toList(),{"Spieltag $it"}){day=it}
    w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.tier==tier&&it.matchday==day}.forEach{f->Section{CompetitionResultRow(w,f)}}
   }
   in cupOptions->{
    val cupCountry=competition.removePrefix("Pokal · ");val games=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP&&CompetitionEngine.cupGroup(w,it)==cupCountry}
    Section(CompetitionEngine.cupName(w,cupCountry)){TrophyIllustration(Modifier.size(70.dp));Text("${games.filter{it.round==games.minOfOrNull{g->g.round}}.size*2} Vereine · K.-o.-Runden mit Verlängerung und Elfmeterschießen.",color=Muted);val own=games.filter{it.homeId==w.user.clubId||it.awayId==w.user.clubId};if(own.isEmpty())Text("Dein Verein ist in dieser Saison nicht im Wettbewerb.",color=Muted)else Text("Dein Weg: ${own.count{it.played}} von ${own.size} angesetzten Partien.",color=Grass)}
    games.groupBy{it.round}.toSortedMap().forEach{(_,round)->Section(round.first().stage){round.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
   }
   else->{
    val type=when(competition){CompetitionType.CHAMPIONS_LEAGUE.label->CompetitionType.CHAMPIONS_LEAGUE;CompetitionType.EUROPA_LEAGUE.label->CompetitionType.EUROPA_LEAGUE;CompetitionType.CLUB_WORLD_CUP.label->CompetitionType.CLUB_WORLD_CUP;CompetitionType.ETERNAL_CROWN.label->CompetitionType.ETERNAL_CROWN;else->CompetitionType.EURO_ELITE}
    val leaguePhase=w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"};val modern=real&&type in listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE)&&leaguePhase.isNotEmpty()&&leaguePhase.all{it.group.isBlank()}
    if(type in listOf(CompetitionType.CLUB_WORLD_CUP,CompetitionType.ETERNAL_CROWN)){
     val matches=w.fixtures.filter{it.competition==type};Section(type.label){TrophyIllustration(Modifier.size(86.dp),true);Text(if(type==CompetitionType.ETERNAL_CROWN)"Aus jeder enthaltenen Liga qualifizieren sich die Plätze 1–6. Beim Karrierestart optional aktivierbar. Siegerprämie: 50 Mio. €." else "32 Clubs · acht Vierergruppen · die besten zwei erreichen das Achtelfinale. Siegerprämie: 40 Mio. €.",color=Muted);if(type==CompetitionType.ETERNAL_CROWN&&w.user.clubId in w.fantasyCupByes)Text("Dein Club hat ein Freilos für die erste Runde.",color=Grass)else if(matches.none{it.homeId==w.user.clubId||it.awayId==w.user.clubId})Text("Dein Verein nimmt derzeit nicht teil.",color=Muted)}
     if(type==CompetitionType.CLUB_WORLD_CUP){Pick("Gruppe",group,('A'..'H').map{it.toString()},{"Gruppe $it"}){group=it};Section("Gruppe $group"){CompetitionEngine.worldCupGroupTable(w,group).forEachIndexed{i,row->val c=w.clubs.getValue(row.clubId);Text("${i+1}. ${c.name} · ${row.points} P · ${row.goalsFor}:${row.goalsAgainst}",color=if(i<2)Grass else Muted)}}}
     matches.groupBy{it.round}.toSortedMap().forEach{(_,round)->Section(round.first().stage){round.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
    }else if(modern){
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
     Section("Gruppe $group"){CompetitionEngine.groupTable(w,group,type).forEachIndexed{i,r->val c=w.clubs.getValue(r.clubId);Column(Modifier.fillMaxWidth().background(if(c.id==w.user.clubId)Color(0xFF284335)else Color.Transparent,RoundedCornerShape(6.dp)).padding(7.dp)){Row(Modifier.fillMaxWidth()){Text("${i+1}. ${c.name} (${c.shortName})",Modifier.weight(1f),fontWeight=if(i<2)FontWeight.Bold else FontWeight.Normal,color=if(i<2)Grass else Chalk);Text("${r.points} P",fontWeight=FontWeight.Bold)};Text("Sp ${r.played} · S ${r.won} · U ${r.drawn} · N ${r.lost} · ${r.goalsFor}:${r.goalsAgainst}",color=Muted,style=MaterialTheme.typography.bodySmall)}}}
     Section("Gruppenspiele"){w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"&&it.group==group}.sortedWith(compareBy<Fixture>{it.round}.thenBy{it.id}).forEach{f->Text("Runde ${f.round}",color=Muted,style=MaterialTheme.typography.labelSmall);CompetitionResultRow(w,f)}}
     val knockouts=w.fixtures.filter{it.competition==type&&it.stage!="Ligaphase"}.groupBy{it.round}.toSortedMap();knockouts.forEach{(_,games)->Section(games.first().stage){games.sortedBy{it.id}.forEach{CompetitionResultRow(w,it)}}}
    }
   }
  }
 }
}

@Composable fun StatLine(label: String,left: String,right: String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(left,Modifier.weight(1f));Text(label,color=Muted);Text(right,Modifier.weight(1f),textAlign=TextAlign.End)}}
private fun liveDelayMs(phase: LivePhase,speed: MatchSpeed): Long {
 val normal=when(phase){
  LivePhase.GOAL->4200L
  LivePhase.SHOT_ON_TARGET,LivePhase.SHOT_OFF_TARGET,LivePhase.WOODWORK->2800L
  LivePhase.VAR->1600L
  LivePhase.OFFSIDE,LivePhase.THROW_IN->1800L
  LivePhase.CORNER->3000L
  LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY->2900L
  LivePhase.YELLOW_CARD,LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD,LivePhase.INJURY->2500L
  LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK->2000L
  LivePhase.COUNTER->1450L
  else->2250L
 }
 return when(speed){
  MatchSpeed.SLOW->if(phase==LivePhase.VAR)1900L else (normal*1.45).toLong()
  MatchSpeed.NORMAL->normal
  MatchSpeed.FAST->when(phase){LivePhase.GOAL->2300L;LivePhase.SHOT_ON_TARGET,LivePhase.SHOT_OFF_TARGET,LivePhase.WOODWORK->1550L;LivePhase.VAR->1200L;LivePhase.OFFSIDE,LivePhase.THROW_IN->950L;LivePhase.CORNER->1700L;LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY->1650L;LivePhase.YELLOW_CARD,LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD,LivePhase.INJURY->1450L;LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK->1100L;LivePhase.COUNTER->720L;else->850L}
 }
}

@Composable fun MatchScreen(state: GameState,vm: GameViewModel,speed: MatchSpeed){
 val w=state.world?:return;val m=w.live;var running by rememberSaveable{mutableStateOf(true)};var liveTab by rememberSaveable(m?.fixtureId){mutableIntStateOf(0)};var showLiveGraphic by rememberSaveable(m?.fixtureId){mutableStateOf(true)};val lifecycle=LocalLifecycleOwner.current.lifecycle
 DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_PAUSE||e==Lifecycle.Event.ON_STOP)running=false};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
 LaunchedEffect(m?.incidentPause,m?.assistantSubPending){if(m?.incidentPause==true||m?.assistantSubPending==true)running=false}
 // Der Match-Timer setzt absichtlich keinen globalen busy/loading-Zustand.
 LaunchedEffect(state.revision,running,speed,m?.fixtureId,m?.liveEventSerial){if(m!=null&&running&&!m.finished&&!m.halfTime&&!m.pendingDecision&&!m.incidentPause&&!m.assistantSubPending){delay(liveDelayMs(m.livePhase,speed));vm.liveStep()}}
 if(m==null){
  Page("${w.nextFixture()?.let{CompetitionEngine.displayName(w,it.competition)}?:"Spieltag"} ${w.calendar.matchday}","KREIDE AN DEN SCHUHEN"){
   w.nextFixture()?.let{NextGame(w,it,"Anpfiff"){vm.startMatch();running=true}}
   if(w.nextFixture()==null)Section("Spielfreie Woche"){Text("Dein Verein hat in dieser Woche kein Spiel. Die anderen Ligen und Pokale laufen weiter.",color=Muted);Action("Zum nächsten eigenen Spiel",!state.busy){vm.advanceUntilNextMatch()}}
   Section("Vor dem Anpfiff"){
    Text("Formation ${w.club().tactics.formation} · Mentalität ${w.club().tactics.mentality}")
    val missing=w.squad().filter{!it.available&&!it.youth};if(missing.isEmpty())Text("Alle Spieler sind verfügbar.",color=Grass)else missing.forEach{Text("${it.name}: ${if(it.injuryWeeks>0)it.injury else it.unavailableReason?.label}",color=Clay)}
    Text("Live-Match v0.4.67: realistischere Match-KI, nachvollziehbare Angriffsketten und kontextabhängige Stadionkulisse. Beim Wechsel in den Hintergrund pausieren Match und Audio sofort.",color=Muted)
   }
  };return
 }
 val soundsEnabled by vm.soundsEnabled.collectAsState()
 MatchAudioEffects(w,m,soundsEnabled)
 val h=w.clubs.getValue(m.homeId);val a=w.clubs.getValue(m.awayId);val ownHome=m.homeId==w.user.clubId;val ownClub=w.club();var livePreset by rememberSaveable(m.fixtureId){mutableStateOf("Individuell")}
 val ownXiRaw=if(ownHome)m.homeXi else m.awayXi;val xi=ownXiRaw.filter{it!=0};val bench=if(ownHome)m.homeBench else m.awayBench;val ownSubs=if(ownHome)m.homeSubs else m.awaySubs;val ownFormation=if(ownHome)m.homeFormation else m.awayFormation;val conserve=if(ownHome)m.homeConserveEnergy else m.awayConserveEnergy;val allOut=if(ownHome)m.homeAllOutAttack else m.awayAllOutAttack;val controlGame=if(ownHome)m.homeControlGame else m.awayControlGame
 Page(if(m.finished)"Abpfiff" else if(m.halfTime)m.breakType.label else if(m.incidentPause)"Spiel unterbrochen" else "Am Seitenrand","${m.weather.uppercase()} · ${m.attendance} ZUSCHAUER"){
  Section{
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Crest(h.logo,h.primary,h.secondary,Modifier.size(56.dp));Text(h.shortName)}
    Column(Modifier.weight(1.4f),horizontalAlignment=Alignment.CenterHorizontally){Text("${m.home.goals} : ${m.away.goals}",style=MaterialTheme.typography.displaySmall);if(m.homePens>0||m.awayPens>0)Text("${m.homePens}:${m.awayPens} i.E.",color=Gold,fontWeight=FontWeight.Bold);Text("${MatchEngine.clockLabel(m)}. Minute",color=Grass);if((!running||m.incidentPause)&&!m.finished)Text(if(m.incidentPause)"AUTO-STOP" else "PAUSE",style=MaterialTheme.typography.labelSmall,color=if(m.incidentPause)Clay else Muted)}
    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Crest(a.logo,a.primary,a.secondary,Modifier.size(56.dp));Text(a.shortName)}
   }
   if(!m.finished){
    if(m.halfTime)Action(MatchEngine.breakActionLabel(m),true){vm.secondHalf();running=true}
    else if(!m.incidentPause)Action(if(running)"Pause" else "Live fortsetzen",true){running=!running}
    Pick("Spieltempo",speed,MatchSpeed.entries.toList(),{it.label}){vm.setMatchSpeed(it)}
    Text(when(speed){MatchSpeed.SLOW->"Langsam: mehr Zeit für jede Ballbewegung und Szene.";MatchSpeed.NORMAL->"Normal: ungefähr 2–2,5 Sekunden pro gewöhnlicher Spielminute.";MatchSpeed.FAST->"Schnell: kürzere Abläufe, wichtige Ereignisse bleiben vollständig sichtbar."},color=Muted,style=MaterialTheme.typography.bodySmall)
   }else Action(if(w.fixtures.firstOrNull{it.id==m.fixtureId}?.competition==CompetitionType.LEAGUE)"Spieltag bestätigen & weiter" else "Pokalpartie bestätigen & weiter",!state.busy){running=false;vm.finishWeek()}
  }

  if(!m.finished){
   Section{PrimaryTabRow(selectedTabIndex=liveTab,containerColor=Color.Transparent,contentColor=Grass){Tab(selected=liveTab==0,onClick={liveTab=0},text={Text("Live")});Tab(selected=liveTab==1,onClick={liveTab=1},text={Text("Taktik")});Tab(selected=liveTab==2,onClick={liveTab=2},text={Text("Statistik")});Tab(selected=liveTab==3,onClick={liveTab=3},text={Text("Analyse")})}}
   when(liveTab){
    0->Section("Live-Animation"){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Spielfeld-Grafik anzeigen");Switch(showLiveGraphic,{showLiveGraphic=it})}
     if(showLiveGraphic)LiveMatchAnimation(w,m) else Text("Die Spielfeld-Grafik ist ausgeblendet. Das Match läuft normal weiter.",color=Muted)
     Text("Nur der Ball bewegt sich. Die letzten Bewegungen bleiben als verblassende Spur sichtbar; Konter, Schüsse und Ecken werden in zusammenhängenden Sequenzen animiert.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
    1->Section("Taktik-Zentrale"){
   Pick("Formation",ownFormation,Formations.all.keys.toList(),{it}){vm.changeFormation(it)}
   Pick("Taktik-Vorlage",livePreset,tacticPresets,{it}){v->livePreset=v;if(v!="Individuell")vm.updateLiveTactic{world->applyTacticPreset(world.club().tactics,v);world.live?.let{live->if(ownHome)live.homeMentality=world.club().tactics.mentality else live.awayMentality=world.club().tactics.mentality}}}
   StepSlider("Mentalität · defensiv bis offensiv",if(ownHome)m.homeMentality else m.awayMentality,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.mentality=v;it.live?.let{live->if(ownHome)live.homeMentality=v else live.awayMentality=v}}}
   StepSlider("Pressing",ownClub.tactics.pressing,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.pressing=v}}
   StepSlider("Tempo",ownClub.tactics.tempo,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.tempo=v}}
   StepSlider("Breite",ownClub.tactics.width,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.width=v}}
   StepSlider("Defensivlinie",ownClub.tactics.line,true){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.line=v}}
   Pick("Spielaufbau",ownClub.tactics.buildUp,BuildUp.entries.toList(),{it.label}){v->livePreset="Individuell";vm.updateLiveTactic{it.club().tactics.buildUp=v}}
   Text(tacticSummary(ownClub.tactics),color=Muted,style=MaterialTheme.typography.bodySmall)
   Text("Sofort-Anweisungen",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Kräfte schonen",fontWeight=FontWeight.Bold);Text("Haramball-Modus: tiefer Block, fast alle hinter dem Ball und nach Ballgewinn schnell kontern. Spart Kraft und gibt Ballbesitz ab, ohne die Konter künstlich abzuwürgen.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(conserve,{vm.setConserveEnergy(it)})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Alles nach vorn",fontWeight=FontWeight.Bold);Text("Volles Risiko: hohe Linie, mehr Vorwärtsdrang und mehr Abschlüsse. Kann einen Rückstand drehen, kostet aber Kraft und öffnet große Räume für gegnerische Konter.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(allOut,{vm.setAllOutAttack(it)})}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Spiel kontrollieren",fontWeight=FontWeight.Bold);Text("Ball halten, Tempo herausnehmen und Risiken reduzieren. Gut zum Verwalten einer Führung, dafür entstehen weniger direkte Abschlüsse.",color=Muted,style=MaterialTheme.typography.bodySmall)};Switch(controlGame,{vm.setControlGame(it)})}

    }
    2->LiveStatistics(w,m,h,a)
    else->MatchAnalysisPanel(w,m,"Analyse")
   }
  }else{
   LiveStatistics(w,m,h,a,"Endstand & Statistik")
   MatchAnalysisPanel(w,m,"Analyse & Taktik-Impact")
   Section("Spielerbewertungen"){
    for((club,lineup) in listOf(h to m.participation.filter{w.players[it]?.clubId==h.id},a to m.participation.filter{w.players[it]?.clubId==a.id})){
     Text(club.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
     lineup.distinct().sortedByDescending{m.playerPerformance[it]?.rating?:6.5}.forEach{id->val p=w.players[id]?:return@forEach;val perf=m.playerPerformance[id]?:PlayerMatchPerformance();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${perf.minutes} Min. · ${perf.goals} Tore · ${perf.assists} Assists · ${perf.summary}",color=Muted,style=MaterialTheme.typography.bodySmall)};Pill(String.format(java.util.Locale.GERMANY,"%.1f",perf.rating),when{perf.rating>=8.0->Grass;perf.rating<6.0->Clay;else->Gold})}}
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
    Action("Spiel fortsetzen",!injuryStillOn||!canReplace){vm.resumeIncident();running=true}
   }
  }


  if(!m.finished)Section("Wechsel & Co-Trainer"){
   val incidentOut=if(m.incidentPause&&m.incidentReason==MatchPauseReason.INJURY&&m.incidentPlayerId in xi)m.incidentPlayerId else 0;var out by remember(m.fixtureId,m.incidentPause,m.incidentPlayerId){mutableIntStateOf(incidentOut.takeIf{it!=0}?:xi.lastOrNull()?:0)};var incoming by remember(m.fixtureId,m.incidentPause,m.incidentPlayerId){mutableIntStateOf(bench.firstOrNull()?:0)}
   val suggestions=remember(state.revision,m.minute,xi,bench,ownFormation){MatchEngine.substitutionSuggestions(w,m)};Text("Wechsel: $ownSubs / 5",color=Muted)
   if(suggestions.isNotEmpty()){Text("Co-Trainer: 5 gewichtete Vorschläge",style=MaterialTheme.typography.titleMedium);Text("Berücksichtigt Fitness, Live-Rating, Karten, Spielstand, Minuten, Form, Moral und Positionspassung.",color=Muted,style=MaterialTheme.typography.bodySmall);suggestions.forEachIndexed{i,sug->val po=w.players.getValue(sug.outId);val pi=w.players.getValue(sug.inId);Surface(color=Color(0xFF183127),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{out=sug.outId;incoming=sug.inId}){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${i+1}. ${po.lastName} → ${pi.lastName}",fontWeight=FontWeight.Bold);SuitabilityBadge(pi,sug.target)};Text(sug.reason,color=Muted,style=MaterialTheme.typography.bodySmall)}}}}
   if(bench.isNotEmpty()&&xi.isNotEmpty()){val so=out.takeIf{it in xi}?:xi.first();val targetIndex=ownXiRaw.indexOf(so);val target=Formations.positions(ownFormation).getOrElse(targetIndex.coerceAtLeast(0)){w.players.getValue(so).position};val orderedBench=bench.sortedByDescending{w.players.getValue(it).ratingAt(target)};val si=incoming.takeIf{it in bench}?:orderedBench.first();Pick("Vom Platz",so,xi,{id->val pl=w.players.getValue(id);"${pl.name} · ${pl.fitness.roundToInt()} %${if(id in m.injured)" · verletzt" else ""}"}){out=it};Pick("Ins Spiel",si,orderedBench,{id->val pl=w.players.getValue(id);"${pl.name} · ${pl.position.name}/${pl.ratingAt(target)}"}){incoming=it};Text("Zielposition $target · ${w.players.getValue(si).roleAt(target)} · Eignung ${w.players.getValue(si).ratingAt(target)}/99",color=suitabilityColorForMatch(w.players.getValue(si),target),style=MaterialTheme.typography.bodySmall);Action("Wechsel durchführen",ownSubs<5,true){vm.substitute(so,si)}}else Text("Kein Wechsel verfügbar.",color=Muted)
  }

  Section("Live-Ticker"){m.ticker.asReversed().take(80).forEach{t->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Text("${t.clockLabel.ifBlank{t.minute.toString()}}′",Modifier.width(38.dp),color=Muted,style=MaterialTheme.typography.labelMedium);Text(t.text,Modifier.weight(1f),color=when(t.tone){"goal","decision"->Grass;"bad"->Clay;else->Chalk},style=MaterialTheme.typography.bodyMedium)}}}
 }
 if(m.assistantSubPending){
  val out=w.players[m.assistantSubOutId];val incoming=w.players[m.assistantSubInId];val ownHome=w.user.clubId==m.homeId;val lineup=if(ownHome)m.homeXi else m.awayXi;val slotIndex=lineup.indexOf(m.assistantSubOutId);val target=Formations.positions(if(ownHome)m.homeFormation else m.awayFormation).getOrElse(slotIndex.coerceAtLeast(0)){out?.position?:Position.ZM};val forced=m.assistantSubOutId in m.injured
  AlertDialog(onDismissRequest={},title={Text(if(forced)"Co-Trainer: Verletzungswechsel" else "Co-Trainer: Wechselvorschlag")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("${MatchEngine.clockLabel(m)}. Minute · ${m.home.goals}:${m.away.goals}",color=Muted);if(out!=null&&incoming!=null){Text("${out.name}  →  ${incoming.name}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Pill("RAUS · ${out.position.label}",Clay);Pill("REIN · ${incoming.position.label}",Grass)};Text("Zielposition: ${target.label} · Eignung ${incoming.ratingAt(target)}/99",color=suitabilityColorForMatch(incoming,target));Text("${out.lastName}: ${out.fitness.roundToInt()} % Fitness · Live-Rating ${String.format(java.util.Locale.GERMANY,"%.1f",m.playerPerformance[out.id]?.rating?:6.5)}",color=Muted,style=MaterialTheme.typography.bodySmall);Text("${incoming.lastName}: ${incoming.fitness.roundToInt()} % Fitness · Form ${String.format(java.util.Locale.GERMANY,"%.1f",incoming.form)}",color=Muted,style=MaterialTheme.typography.bodySmall)};Text(m.assistantSubReason.ifBlank{"Der Co-Trainer bewertet Fitness, Leistung, Karten, Spielstand und Positionspassung."});if(forced)Text("Bei Ablehnung öffnet sich die manuelle Verletzungs-Unterbrechung. Der verletzte Spieler wird nicht automatisch weiter eingesetzt.",color=Gold,style=MaterialTheme.typography.bodySmall)else Text("Bei Ablehnung wird genau dieser Vorschlag für einige Spielminuten gesperrt. Der Co-Trainer kann später eine neue Lösung vorschlagen.",color=Muted,style=MaterialTheme.typography.bodySmall)}},confirmButton={Button(onClick={vm.acceptAssistantSubstitution();running=true}){Text("Annehmen")}},dismissButton={OutlinedButton(onClick={vm.rejectAssistantSubstitution();running=!forced}){Text("Ablehnen")}})
 }
 if(m.pendingDecision)AlertDialog(onDismissRequest={},title={Text("Du am Ball")},text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("${MatchEngine.clockLabel(m)}. Minute · ${m.home.goals}:${m.away.goals}. Was machst du?");Text("Dribbling riskiert den Ball. Halten schont Kräfte. Ein Foul riskiert Gelb.",color=Muted,style=MaterialTheme.typography.bodySmall);Decision.entries.forEach{d->Action(d.label,true,d!=Decision.SHOOT){vm.decide(d)}}}},confirmButton={})
}

@Composable private fun LiveStatistics(w: World,m: LiveMatch,h: Club,a: Club,title: String="Aktuelle Live-Statistiken"){
 val poss=if(m.home.possessionTicks+m.away.possessionTicks==0)50 else (100.0*m.home.possessionTicks/(m.home.possessionTicks+m.away.possessionTicks)).roundToInt()
 Section(title){
  StatLine("xG",dec(m.home.xg),dec(m.away.xg));StatLine("Schüsse gesamt","${m.home.shots}","${m.away.shots}");StatLine("Schüsse aufs Tor","${m.home.shotsOnTarget}","${m.away.shotsOnTarget}");StatLine("Schüsse neben das Tor","${m.home.shotsOffTarget}","${m.away.shotsOffTarget}");if(m.home.blockedShots+m.away.blockedShots>0)StatLine("Geblockt","${m.home.blockedShots}","${m.away.blockedShots}");StatLine("Ballbesitz","$poss %","${100-poss} %");StatLine("Ecken","${m.home.corners}","${m.away.corners}");StatLine("Einwürfe","${m.home.throwIns}","${m.away.throwIns}");StatLine("Abseits","${m.home.offsides}","${m.away.offsides}");if(m.home.varChecks+m.away.varChecks>0)StatLine("VAR-Prüfungen","${m.home.varChecks}","${m.away.varChecks}");StatLine("Fouls","${m.home.fouls}","${m.away.fouls}");val homeYellow=m.yellows.filter{w.players[it.key]?.clubId==m.homeId}.values.sum();val awayYellow=m.yellows.filter{w.players[it.key]?.clubId==m.awayId}.values.sum();val homeRed=m.sentOff.count{w.players[it]?.clubId==m.homeId};val awayRed=m.sentOff.count{w.players[it]?.clubId==m.awayId};StatLine("Gelbe Karten","$homeYellow","$awayYellow");if(homeRed+awayRed>0)StatLine("Platzverweise","$homeRed","$awayRed")
  val active=w.clubs[m.chainOwnerClubId.takeIf{it==m.homeId||it==m.awayId}?:if(m.homeInPossession)m.homeId else m.awayId];Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Aktuelle Phase",color=Muted);Pill("${m.livePhase.label} · ${active?.shortName?:"-"}",if(active?.id==m.homeId)Color(h.primary)else Color(a.primary))}
 }
}

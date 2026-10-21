package de.gruenderelf.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.*
import kotlin.math.roundToInt

/* ───────────────────────── Auswertungen (rein abgeleitet, keine Spielstandänderung) ───────────────────────── */

/** Tabellenplatz und Punkte des eigenen Vereins nach jedem gespielten Ligaspieltag. */
data class TablePoint(val matchday: Int,val rank: Int,val points: Int)

fun tableHistory(w: World,clubId: Int=w.user.clubId): List<TablePoint>{
 val league=w.leagues.firstOrNull{clubId in it.clubIds}?:return emptyList()
 val rows=league.clubIds.associateWith{TableRow(it)}
 val played=w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.tier==league.tier&&it.played}.groupBy{it.matchday}.toSortedMap()
 val out=mutableListOf<TablePoint>()
 for((day,games) in played){
  for(f in games){
   val m=w.matches[f.id]?:continue;val h=rows[f.homeId]?:continue;val a=rows[f.awayId]?:continue
   h.played++;a.played++;h.goalsFor+=m.home.goals;h.goalsAgainst+=m.away.goals;a.goalsFor+=m.away.goals;a.goalsAgainst+=m.home.goals
   when{m.home.goals>m.away.goals->{h.won++;a.lost++};m.home.goals<m.away.goals->{a.won++;h.lost++};else->{h.drawn++;a.drawn++}}
  }
  val sorted=rows.values.sortedWith(compareByDescending<TableRow>{it.points}.thenByDescending{it.difference}.thenByDescending{it.goalsFor}.thenBy{it.clubId})
  val idx=sorted.indexOfFirst{it.clubId==clubId}
  if(idx>=0)out+=TablePoint(day,idx+1,sorted[idx].points)
 }
 return out
}

/** Einschätzung des Vorstands: Erwartung aus der Kaderstärke im Ligavergleich, gemessen an Tabelle, Form und Finanzen. */
data class BoardView(val confidence: Int,val expectedRank: Int,val actualRank: Int,val leagueSize: Int,val headline: String,val message: String)

fun boardView(w: World): BoardView{
 val c=w.club();val league=w.leagues.first{c.id in it.clubIds}
 fun strength(id: Int)=w.squad(id).filter{!it.youth}.map{it.ca}.sortedDescending().take(15).let{if(it.isEmpty())0.0 else it.average()}
 val byStrength=league.clubIds.sortedByDescending{strength(it)}
 val expected=byStrength.indexOf(c.id)+1
 val table=WorldFactory.table(w,league.tier);val actual=table.indexOfFirst{it.clubId==c.id}+1
 val played=table.firstOrNull{it.clubId==c.id}?.played?:0
 val size=league.clubIds.size
 // Je weiter die Saison, desto stärker zählt die Tabelle.
 val weight=(played/10.0).coerceAtMost(1.0)
 val tableScore=if(played==0)0.0 else (expected-actual).toDouble()/size*120*weight
 val formScore=c.form.takeLast(5).sumOf{when(it){"S"->3.0;"N"->-3.0;else->0.0}}
 val moneyScore=if(c.budget<0)-15.0 else 0.0
 val confidence=(62+tableScore+formScore+moneyScore).roundToInt().coerceIn(5,100)
 val (headline,message)=when{
  played==0->"Saisonziel" to "Der Vorstand erwartet ungefähr Platz $expected – passend zur Kaderstärke im Ligavergleich."
  confidence>=80->"Volles Vertrauen" to "Der Vorstand ist begeistert. Platz $actual liegt über der Erwartung (Platz $expected)."
  confidence>=60->"Zufrieden" to "Der Vorstand sieht den Verein auf Kurs. Erwartet wird etwa Platz $expected."
  confidence>=40->"Unruhe" to "Der Vorstand wird ungeduldig. Platz $actual liegt unter der Erwartung (Platz $expected)."
  confidence>=20->"Krisensitzung" to "Der Vorstand fordert eine schnelle Wende. Die nächsten Spiele sind entscheidend."
  else->"Ultimatum" to "Der Vorstand stellt dir ein Ultimatum: Ohne Punkte in den kommenden Wochen steht deine Zukunft infrage."
 }
 return BoardView(confidence,expected,actual,size,headline,message)
}

data class ScorerRow(val player: Player,val club: Club,val goals: Int,val assists: Int)

fun topScorers(w: World,tier: Int,byAssists: Boolean=false,limit: Int=10): List<ScorerRow>{
 val clubIds=w.leagues.firstOrNull{it.tier==tier}?.clubIds?.toSet()?:return emptyList()
 return w.players.values.asSequence().filter{!it.retired&&it.clubId in clubIds&&(it.stats.goals>0||it.stats.assists>0)}
  .sortedWith(if(byAssists)compareByDescending<Player>{it.stats.assists}.thenByDescending{it.stats.goals} else compareByDescending<Player>{it.stats.goals}.thenByDescending{it.stats.assists})
  .take(limit).mapNotNull{p->w.clubs[p.clubId]?.let{ScorerRow(p,it,p.stats.goals,p.stats.assists)}}.toList()
}

fun unreadNews(w: World)=w.news.count{it.id>w.user.newsReadUpTo}

/* ───────────────────────── Bausteine ───────────────────────── */

/** „Nächster Schritt“: führt durch die Woche wie ein Weiter-Knopf. */
@Composable fun NextStepCard(w: World,onGame: ()->Unit,onNavigate: (String)->Unit){
 val squad=w.squad().filter{!it.youth}
 val lineupHoles=w.club().tactics.xi.count{id->id==0||w.players[id]?.available!=true}
 val tired=squad.count{it.available&&it.fitness<65}
 val benchShort=(7-w.club().tactics.bench.size).coerceAtLeast(0)
 data class Check(val ok: Boolean,val title: String,val hint: String,val route: String)
 val checks=listOf(
  Check(lineupHoles==0,if(lineupHoles==0)"Startelf steht" else "$lineupHoles Lücke(n) in der Startelf","Aufstellung","kader"),
  Check(benchShort==0,if(benchShort==0)"Bank vollständig" else "Noch $benchShort Bankplatz/-plätze frei","Ersatzbank","kader"),
  Check(tired==0,if(tired==0)"Kader ist fit" else "$tired Spieler unter 65 % Fitness","Belastung","training")
 )
 val next=w.nextFixture()
 Surface(color=Panel,shape=RoundedCornerShape(CardRadius),border=BorderStroke(1.dp,Grass.copy(alpha=.35f)),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.background(Brush.verticalGradient(listOf(Grass.copy(alpha=.09f),Color.Transparent))).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Eyebrow("NÄCHSTER SCHRITT")
   if(w.live!=null){
    Text("Die Partie läuft noch.",style=MaterialTheme.typography.titleLarge,color=Chalk)
    Action("Zur Live-Partie",onClick=onGame)
    return@Column
   }
   checks.forEach{c->
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled=!c.ok){onNavigate(c.route)}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Icon(if(c.ok)Icons.Filled.CheckCircle else Icons.Filled.Warning,contentDescription=if(c.ok)"erledigt" else "offen",tint=if(c.ok)Grass else Gold,modifier=Modifier.size(20.dp))
     Text(c.title,Modifier.weight(1f),color=if(c.ok)Muted else Chalk,style=MaterialTheme.typography.bodyMedium)
     if(!c.ok)Text(c.hint+" ›",color=Gold,style=MaterialTheme.typography.labelMedium)
    }
   }
   if(next!=null){
    val opp=w.clubs.getValue(if(next.homeId==w.user.clubId)next.awayId else next.homeId)
    Action(if(checks.all{it.ok})"Weiter zum Anpfiff · ${opp.shortName}" else "Trotzdem zum Spiel · ${opp.shortName}",onClick=onGame)
   }else Text("Kein Spiel angesetzt.",color=Muted)
  }
 }
}

/** Vorstands-Kachel mit Vertrauensbalken. */
@Composable fun BoardSection(w: World){
 val b=remember(w.calendar.absoluteWeek,w.calendar.season,w.club().budget,w.club().form.size){boardView(w)}
 val color=when{b.confidence>=60->Grass;b.confidence>=40->Gold;else->Clay}
 Section("Vorstand"){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
   RatingBadge("${b.confidence}",color,52.dp)
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
    Text(b.headline,style=MaterialTheme.typography.titleLarge,color=color)
    Text("Vertrauen · Erwartung Platz ${b.expectedRank} von ${b.leagueSize}",color=Muted,style=MaterialTheme.typography.bodySmall)
   }
  }
  Bar(b.confidence/100f,color)
  Text(b.message,color=Chalk.copy(alpha=.86f),style=MaterialTheme.typography.bodyMedium)
 }
}

/** Liniendiagramm des Tabellenplatzes (Platz 1 oben). */
@Composable fun TableHistoryChart(points: List<TablePoint>,leagueSize: Int){
 if(points.size<2){Text("Der Verlauf erscheint nach dem zweiten Spieltag.",color=Muted,style=MaterialTheme.typography.bodySmall);return}
 val accent=Grass;val grid=Line;val faint=Faint
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
  Canvas(Modifier.fillMaxWidth().height(150.dp).semantics{contentDescription="Tabellenplatz-Verlauf: zuletzt Platz ${points.last().rank} von $leagueSize"}){
   val padL=6.dp.toPx();val padR=6.dp.toPx();val padT=8.dp.toPx();val padB=8.dp.toPx()
   val w=size.width-padL-padR;val h=size.height-padT-padB
   fun x(i: Int)=padL+w*i/(points.size-1).coerceAtLeast(1)
   fun y(rank: Int)=padT+h*(rank-1)/(leagueSize-1).coerceAtLeast(1)
   // Gitter: Platz 1, Mitte, letzter Platz
   for(r in listOf(1,(leagueSize+1)/2,leagueSize))drawLine(grid,Offset(padL,y(r)),Offset(padL+w,y(r)),1.dp.toPx())
   val line=Path();val area=Path()
   points.forEachIndexed{i,p->val px=x(i);val py=y(p.rank);if(i==0){line.moveTo(px,py);area.moveTo(px,padT+h);area.lineTo(px,py)}else{line.lineTo(px,py);area.lineTo(px,py)}}
   area.lineTo(x(points.lastIndex),padT+h);area.close()
   drawPath(area,Brush.verticalGradient(listOf(accent.copy(alpha=.25f),Color.Transparent),startY=padT,endY=padT+h))
   drawPath(line,accent,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
   val last=Offset(x(points.lastIndex),y(points.last().rank))
   drawCircle(accent,5.dp.toPx(),last);drawCircle(Panel,2.5.dp.toPx(),last)
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Text("Spieltag ${points.first().matchday}",color=faint,style=MaterialTheme.typography.labelMedium)
   Text("Platz ${points.last().rank} · ${points.last().points} Punkte",color=Chalk,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
   Text("Spieltag ${points.last().matchday}",color=faint,style=MaterialTheme.typography.labelMedium)
  }
  val best=points.minOf{it.rank};val worst=points.maxOf{it.rank}
  Text("Bester Platz $best · schlechtester Platz $worst",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
}

/** Torjäger- bzw. Vorlagenliste einer Liga. */
@Composable fun TopScorersSection(w: World,tier: Int){
 var assists by rememberSaveable{mutableStateOf(false)}
 val rows=remember(tier,assists,w.calendar.absoluteWeek){topScorers(w,tier,assists)}
 Section(if(assists)"Vorlagengeber" else "Torjäger"){
  SegmentedTabs(listOf("Tore","Vorlagen"),if(assists)1 else 0){assists=it==1}
  if(rows.isEmpty())Text("Noch keine Treffer in dieser Saison.",color=Muted)
  rows.forEachIndexed{i,r->
   val own=r.club.id==w.user.clubId
   Row(Modifier.fillMaxWidth().background(if(own)Grass.copy(alpha=.08f) else Color.Transparent,RoundedCornerShape(10.dp)).padding(horizontal=6.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Text("${i+1}",Modifier.width(22.dp),color=if(i<3)Gold else Muted,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
    Crest(r.club.logo,r.club.primary,r.club.secondary,Modifier.size(22.dp))
    Column(Modifier.weight(1f)){
     Text(r.player.name,style=MaterialTheme.typography.titleSmall,color=Chalk,maxLines=1,overflow=TextOverflow.Ellipsis)
     Text("${r.club.shortName} · ${r.player.position.name}",style=MaterialTheme.typography.bodySmall,color=Muted)
    }
    Text(if(assists)"${r.assists}" else "${r.goals}",style=MaterialTheme.typography.titleLarge,color=Chalk)
   }
  }
 }
}

/* ───────────────────────── Posteingang ───────────────────────── */

@Composable private fun InboxRow(n: NewsItem,unread: Boolean){
 val tone=when(n.tone){"good"->Grass;"bad"->Clay;else->Blue}
 Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.padding(top=6.dp).size(9.dp).background(if(unread)tone else Color.Transparent,CircleShape).border(1.5.dp,tone,CircleShape))
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    Text(n.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=Chalk,fontWeight=if(unread)FontWeight.Bold else FontWeight.Normal)
    if(unread)Text("NEU",color=tone,style=MaterialTheme.typography.labelSmall)
   }
   Text(n.text,style=MaterialTheme.typography.bodyMedium,color=Chalk.copy(alpha=if(unread).9f else .7f))
   Text("VEREINSWOCHE ${n.week+1}",style=MaterialTheme.typography.labelSmall,color=Faint)
  }
 }
}

/** Kurzansicht auf der Übersicht: die neuesten Meldungen und der Weg in den Posteingang. */
@Composable fun InboxPreview(w: World,onOpen: ()->Unit){
 if(w.news.isEmpty())return
 val unread=unreadNews(w)
 Section("Posteingang"){
  ListRow(if(unread>0)"$unread ungelesene Meldung${if(unread==1)"" else "en"}" else "Alles gelesen","Vereinsnachrichten, Verletzungen, Angebote",Icons.Filled.Email,if(unread>0)Gold else Muted,unread,onClick=onOpen)
  w.news.take(3).forEachIndexed{i,n->if(i>0)HairlineDivider();InboxRow(n,n.id>w.user.newsReadUpTo)}
 }
}

@Composable fun InboxScreen(w: World,vm: GameViewModel){
 var filter by rememberSaveable{mutableStateOf("Alle")}
 val unread=unreadNews(w)
 val items=w.news.filter{when(filter){"Gut"->it.tone=="good";"Kritisch"->it.tone=="bad";"Ungelesen"->it.id>w.user.newsReadUpTo;else->true}}
 Page("Posteingang",if(unread>0)"$unread UNGELESEN" else "ALLES GELESEN"){
  ChipRow(listOf("Alle","Ungelesen","Gut","Kritisch"),filter){filter=it}
  if(unread>0)Action("Alle als gelesen markieren",secondary=true){vm.action{world->world.user.newsReadUpTo=world.news.maxOfOrNull{it.id}?:world.user.newsReadUpTo}}
  Section{
   if(items.isEmpty())Text("Keine Meldungen in dieser Ansicht.",color=Muted)
   items.forEachIndexed{i,n->if(i>0)HairlineDivider();InboxRow(n,n.id>w.user.newsReadUpTo)}
  }
 }
}

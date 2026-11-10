package de.gruenderelf.engine

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/* ───────────────────────── Vereinsleben (0.5.27) ─────────────────────────
 * Vorstand mit Folgen, Saisonrückblick, Pressekonferenz, Kabinenansprache,
 * Ticketpreise und Länderspielpausen. Alle Zustände liegen in [ClubLifeState]
 * und haben Standardwerte – ältere Spielstände laden unverändert.
 */

@Serializable enum class PressStance(val label: String,val line: String){
 NONE("",""),
 CONFIDENT("Kampfansage","Wir holen die drei Punkte."),
 HUMBLE("Tiefstapeln","Der Gegner ist Favorit. Wir bleiben demütig."),
 PROTECT("Vor die Mannschaft stellen","Kritik geht an mich, nicht an die Spieler.")
}
@Serializable enum class TeamTalk(val label: String){PRAISE("Loben"),CALM("Ruhig bleiben"),DEMAND("Mehr fordern")}

@Serializable data class BoardState(var confidence: Int=62,var lastReviewWeek: Int=-1,var lowReviews: Int=0,var dismissed: Boolean=false,var jobOffers: MutableList<Int> = mutableListOf(),var pressBonus: Int=0,var lastVerdict: String="")
@Serializable data class SeasonReview(val season: Int,val clubName: String,val league: String,val rank: Int,val leagueSize: Int,val points: Int,val won: Int,val drawn: Int,val lost: Int,val goalsFor: Int,val goalsAgainst: Int,val outcome: String,val awards: List<String> = emptyList(),val topScorer: String="",val topAssist: String="",val expectedRank: Int=0,val budget: Long=0)
@Serializable data class ClubLifeState(var board: BoardState=BoardState(),var seasonReview: SeasonReview?=null,var pressFixtureId: Int=0,var pressStance: PressStance=PressStance.NONE,var teamTalkFixtureId: Int=0,var lastBreakKey: Int=-1)

object BoardSystem{
 /** Reviews zählen erst, wenn die Tabelle etwas aussagt. */
 const val MIN_MATCHES=6
 const val REVIEW_INTERVAL=4
 fun exempt(w: World)=w.user.difficulty==Difficulty.SANDBOX||w.developer.enabled
 fun strength(w: World,clubId: Int): Double=w.squad(clubId).filter{!it.youth}.map{it.ca}.sortedDescending().take(15).let{if(it.isEmpty())0.0 else it.average()}
 private fun ownLeague(w: World)=w.leagues.first{w.user.clubId in it.clubIds}
 fun expectedRank(w: World): Int=ownLeague(w).clubIds.sortedByDescending{strength(w,it)}.indexOf(w.user.clubId)+1
 fun confidence(w: World): Int{
  val c=w.club();val league=ownLeague(w);val expected=expectedRank(w)
  val table=WorldFactory.table(w,league.tier);val actual=table.indexOfFirst{it.clubId==c.id}+1;val played=table.firstOrNull{it.clubId==c.id}?.played?:0
  val weight=(played/10.0).coerceAtMost(1.0)
  val tableScore=if(played==0)0.0 else (expected-actual).toDouble()/league.clubIds.size*120*weight
  var formScore=0.0;for(r in c.form.takeLast(5))formScore+=when(r){"S"->3.0;"N"->-3.0;else->0.0}
  val moneyScore=if(c.budget<0)-15.0 else 0.0
  return (62+tableScore+formScore+moneyScore+w.life.board.pressBonus).roundToInt().coerceIn(5,100)
 }
 fun weeksToReview(w: World): Int=REVIEW_INTERVAL-(w.calendar.absoluteWeek%REVIEW_INTERVAL)
 fun weekly(w: World){
  val b=w.life.board
  if(b.pressBonus>0)b.pressBonus-- else if(b.pressBonus<0)b.pressBonus++
  b.confidence=confidence(w)
  if(exempt(w)||b.dismissed)return
  if(w.calendar.absoluteWeek%REVIEW_INTERVAL!=0||w.calendar.absoluteWeek==b.lastReviewWeek)return
  val league=ownLeague(w);val played=WorldFactory.table(w,league.tier).firstOrNull{it.clubId==w.user.clubId}?.played?:0
  if(played<MIN_MATCHES)return
  b.lastReviewWeek=w.calendar.absoluteWeek
  review(w,b.confidence)
 }
 /** Vorstandssitzung: Prämie, Mittelkürzung, Ultimatum oder Entlassung. */
 fun review(w: World,confidence: Int){
  val b=w.life.board;val c=w.club();val base=c.lastCosts.coerceAtLeast(200).toLong()
  when{
   confidence>=80->{
    val bonus=base*2;c.budget+=bonus;b.lowReviews=0;b.lastVerdict="Prämie: ${bonus} €"
    w.news("Vorstand zahlt Prämie","Der Vorstand ist begeistert und gibt ${bonus} € zusätzlich frei.","good")
   }
   confidence>=40->{b.lowReviews=0;b.lastVerdict="Keine Maßnahmen"}
   confidence>=20->{
    val cut=if(c.budget>0)minOf(base,c.budget/10) else 0L;c.budget-=cut;b.lastVerdict=if(cut>0)"Mittel gekürzt: ${cut} €" else "Verwarnung"
    w.news("Vorstand kürzt Mittel",if(cut>0)"Der Vorstand ist unzufrieden und hält ${cut} € zurück." else "Der Vorstand ist unzufrieden und erwartet eine schnelle Besserung.","bad")
   }
   else->{
    b.lowReviews++
    if(b.lowReviews>=2)dismiss(w) else{
     b.lastVerdict="Ultimatum"
     w.news("Ultimatum des Vorstands","Bleibt die Wende bis zur nächsten Sitzung in $REVIEW_INTERVAL Wochen aus, trennt sich der Verein von dir.","bad")
    }
   }
  }
 }
 fun dismiss(w: World){
  val b=w.life.board;b.jobOffers=offers(w).toMutableList()
  if(b.jobOffers.isEmpty()){b.lowReviews=0;return}
  b.dismissed=true;b.lastVerdict="Entlassung"
  w.news("Entlassung","${w.club().name} trennt sich von dir. ${b.jobOffers.size} Vereine bieten dir einen Neuanfang.","bad")
 }
 /** Bis zu drei Vereine aus gleich großen Ligen, die höchstens so stark sind wie der bisherige. */
 fun offers(w: World): List<Int>{
  val own=w.club();val league=ownLeague(w);val mine=strength(w,own.id)
  val rated=w.leagues.filter{it.clubIds.size==league.clubIds.size}.flatMap{it.clubIds}.filter{it!=own.id&&it in w.clubs}.distinct().map{it to strength(w,it)}.sortedByDescending{it.second}
  val weaker=rated.filter{it.second<=mine}
  val pick=if(weaker.size>=3)listOf(weaker[0],weaker[weaker.size/3],weaker[weaker.size*2/3]) else rated.takeLast(3)
  return pick.map{it.first}.distinct()
 }
 fun acceptOffer(w: World,clubId: Int){
  val b=w.life.board
  require(b.dismissed){"Es steht kein Vereinswechsel an."}
  require(clubId in b.jobOffers&&clubId in w.clubs){"Dieses Angebot gibt es nicht mehr."}
  require(w.live==null){"Erst das laufende Spiel beenden."}
  val old=w.club();val me=w.self();val target=w.clubs.getValue(clubId)
  val used=w.squad(clubId).map{it.number}.toSet()
  me.clubId=clubId;me.youth=false;me.wantsMove=false
  if(me.number in used)me.number=(1..99).firstOrNull{it !in used}?:me.number
  w.user.clubId=clubId
  // Vereinsgebundene Planung des alten Klubs verfällt.
  w.training=TrainingPlan();w.construction.clear();w.negotiations.clear();w.intensiveTraining.clear();w.scoutAssignments.clear();w.competingBids.clear()
  WorldFactory.autoLineup(w,old.id);WorldFactory.autoLineup(w,clubId)
  b.dismissed=false;b.jobOffers.clear();b.lowReviews=0;b.pressBonus=0;b.lastReviewWeek=w.calendar.absoluteWeek;b.lastVerdict="";b.confidence=confidence(w)
  w.life.pressFixtureId=0;w.life.pressStance=PressStance.NONE
  w.news("Neuer Verein","Du übernimmst ${target.name} als Spielertrainer.","good")
 }
}

object TicketSystem{
 private val crowd=listOf(1.18,1.08,1.0,.86,.70)
 private val price=listOf(.55,.8,1.0,1.3,1.65)
 val labels=listOf("Sehr günstig","Günstig","Normal","Gehoben","Teuer")
 fun level(c: Club)=c.ticketLevel.coerceIn(1,5)
 fun label(c: Club)=labels[level(c)-1]
 fun attendanceFactor(c: Club)=crowd[level(c)-1]
 fun priceFactor(c: Club)=price[level(c)-1]
 /** Günstige Karten gewinnen Mitglieder, teure kosten welche. */
 fun afterHomeMatch(c: Club){
  val drift=when(level(c)){1->0.004;2->0.002;4-> -0.002;5-> -0.005;else->0.0}
  if(drift>0)c.members+=maxOf(1,(c.members*drift).roundToInt())
  if(drift<0)c.members=(c.members-maxOf(1,(c.members*(-drift)).roundToInt())).coerceAtLeast(20)
 }
}

object MatchdaySystem{
 fun canHoldPress(w: World): Boolean{val f=w.nextFixture()?:return false;return w.live==null&&f.competition==CompetitionType.LEAGUE&&w.life.pressFixtureId!=f.id}
 fun holdPress(w: World,stance: PressStance){
  require(stance!=PressStance.NONE&&canHoldPress(w)){"Die Pressekonferenz ist schon vorbei."}
  val f=w.nextFixture()?:return;val s=w.life;s.pressFixtureId=f.id;s.pressStance=stance
  val seniors=w.squad().filter{!it.youth}
  when(stance){
   PressStance.CONFIDENT->{seniors.forEach{it.morale=(it.morale+3).coerceAtMost(100)}}
   PressStance.PROTECT->{seniors.forEach{it.morale=(it.morale+2).coerceAtMost(100)};s.board.pressBonus=(s.board.pressBonus-2).coerceAtLeast(-12)}
   else->{}
  }
 }
 fun resolvePress(w: World,live: LiveMatch){
  val s=w.life
  if(s.pressFixtureId!=live.fixtureId||s.pressStance==PressStance.NONE)return
  val home=live.homeId==w.user.clubId;val own=if(home)live.home.goals else live.away.goals;val other=if(home)live.away.goals else live.home.goals
  val b=s.board;val c=w.club()
  if(s.pressStance==PressStance.CONFIDENT){
   if(own>other){b.pressBonus=(b.pressBonus+4).coerceAtMost(12);c.members+=maxOf(1,c.members/200);w.news("Worte gehalten","Deine Kampfansage ging auf. Vorstand und Fans sind beeindruckt.","good")}
   else if(own<other){b.pressBonus=(b.pressBonus-5).coerceAtLeast(-12);w.squad().filter{!it.youth}.forEach{it.morale=(it.morale-2).coerceAtLeast(5)};w.news("Große Worte, keine Punkte","Nach deiner Kampfansage wird die Niederlage besonders kritisch gesehen.","bad")}
  }else if(s.pressStance==PressStance.HUMBLE&&own>other){b.pressBonus=(b.pressBonus+1).coerceAtMost(12)}
  s.pressStance=PressStance.NONE
 }
 fun canTalk(w: World): Boolean{val m=w.live?:return false;return m.halfTime&&m.breakType==MatchBreakType.HALF_TIME&&!m.finished&&w.life.teamTalkFixtureId!=m.fixtureId}
 /** Moraländerung der Elf auf dem Platz – abhängig davon, ob die Ansprache zum Spielstand passt. */
 fun talkDelta(w: World,talk: TeamTalk): Int{
  val m=w.live?:return 0;val home=m.homeId==w.user.clubId
  val diff=if(home)m.home.goals-m.away.goals else m.away.goals-m.home.goals
  return when(talk){
   TeamTalk.PRAISE->if(diff>0)6 else if(diff==0)2 else -4
   TeamTalk.CALM->2
   TeamTalk.DEMAND->if(diff<0)8 else if(diff==0)4 else -6
  }
 }
 fun talkReaction(delta: Int)=when{delta>=6->"Die Kabine brennt.";delta>0->"Die Mannschaft nickt.";else->"Die Ansprache kommt nicht gut an."}
 fun teamTalk(w: World,talk: TeamTalk){
  require(canTalk(w)){"Jetzt ist keine Ansprache möglich."}
  val m=w.live?:return;val d=talkDelta(w,talk);val ids=if(m.homeId==w.user.clubId)m.homeXi else m.awayXi
  for(id in ids){val p=w.players[id]?:continue;p.morale=(p.morale+d).coerceIn(5,100)}
  w.life.teamTalkFixtureId=m.fixtureId
 }
}

object InternationalBreakSystem{
 val matchdays=setOf(7,15,25)
 const val CALL_UP_RATING=80
 /** Nach bestimmten Spieltagen: Nationalspieler reisen ab, alle anderen erholen sich. */
 fun apply(w: World){
  val day=w.calendar.matchday;val key=w.calendar.season*100+day
  if(day !in matchdays||w.life.lastBreakKey==key)return
  w.life.lastBreakKey=key
  val rng=SeededRandom(w.seed xor (key.toLong()*7919L))
  val called=mutableListOf<Player>();val hurt=mutableListOf<Player>()
  for(p in w.players.values){
   if(p.retired||p.clubId==0)continue
   if(!p.youth&&p.injuryWeeks==0&&p.ca>=CALL_UP_RATING){
    p.fitness=(p.fitness-7).coerceAtLeast(40.0);p.morale=(p.morale+2).coerceAtMost(100)
    val injured=rng.chance(.03)
    if(injured){p.injuryWeeks=rng.int(1,2);p.injury="Blessur vom Länderspiel"}
    if(p.clubId==w.user.clubId){called+=p;if(injured)hurt+=p}
   }else p.fitness=(p.fitness+5).coerceAtMost(100.0)
  }
  if(called.isEmpty())w.news("Länderspielpause","Kein Spieler wurde abgestellt. Der Kader konnte durchatmen.","normal")
  else w.news("Länderspielpause","Abgestellt: ${called.take(6).joinToString{it.lastName}}${if(called.size>6)" und ${called.size-6} weitere" else ""}.${if(hurt.isNotEmpty())" Angeschlagen zurück: ${hurt.joinToString{it.lastName}}." else ""}",if(hurt.isNotEmpty())"bad" else "normal")
 }
}

object ClubLife{
 fun afterLeagueWeek(w: World,live: LiveMatch){
  MatchdaySystem.resolvePress(w,live)
  InternationalBreakSystem.apply(w)
  BoardSystem.weekly(w)
 }
 fun buildReview(w: World,rank: Int,size: Int,row: TableRow,outcome: String,awards: List<String>): SeasonReview{
  val c=w.club();val seniors=w.squad().filter{!it.youth}
  val scorer=seniors.maxByOrNull{it.stats.goals}?.takeIf{it.stats.goals>0}
  val maker=seniors.maxByOrNull{it.stats.assists}?.takeIf{it.stats.assists>0}
  w.life.board.lowReviews=0
  return SeasonReview(w.calendar.season,c.name,WorldFactory.leagueName(w,c.tier),rank,size,row.points,row.won,row.drawn,row.lost,row.goalsFor,row.goalsAgainst,outcome,awards.toList(),
   scorer?.let{"${it.name} · ${it.stats.goals} Tore"}?:"",maker?.let{"${it.name} · ${it.stats.assists} Vorlagen"}?:"",BoardSystem.expectedRank(w),c.budget)
 }
}

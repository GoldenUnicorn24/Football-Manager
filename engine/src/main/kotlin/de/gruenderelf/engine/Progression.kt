package de.gruenderelf.engine
import kotlin.math.roundToInt

object TrainingEngine {
 const val MESSI_MASTERCLASS_COST:Long=10_000_000L
 fun messiMasterclassReason(w:World,playerId:Int):String?{
  val p=w.players[playerId]?:return "Spieler nicht gefunden."
  return when{
   w.live!=null->"Die Masterclass kann nicht während eines laufenden Spiels gebucht werden."
   p.clubId!=w.user.clubId||p.retired->"Der Spieler gehört nicht zum aktiven Kader."
   p.messiMentored->"Dieser Spieler hat die Messi-Masterclass bereits absolviert."
   w.club().budget<MESSI_MASTERCLASS_COST->"Für die Masterclass fehlen ${MESSI_MASTERCLASS_COST-w.club().budget} €."
   else->null
  }
 }
 fun bookMessiMasterclass(w:World,playerId:Int){
  val reason=messiMasterclassReason(w,playerId);require(reason==null){reason?:"Masterclass nicht möglich."}
  val p=w.players.getValue(playerId);w.club().budget-=MESSI_MASTERCLASS_COST
  with(p.attributes){
   pace+=20;finishing+=20;passing+=20;technique+=20;tackling+=20;strength+=20;stamina+=20;vision+=20;heading+=20;keeping+=20;setPieces+=20
   // Signatur-Boni über das reine +20 hinaus: enge Ballführung, Spielwitz, Abschluss und Standards.
   technique+=8;vision+=8;finishing+=6;passing+=5;pace+=4;setPieces+=6
  }
  p.hidden.consistency=(p.hidden.consistency+22).coerceAtMost(120);p.hidden.pressure=(p.hidden.pressure+25).coerceAtMost(120)
  p.hidden.professionalism=(p.hidden.professionalism+12).coerceAtMost(120);p.hidden.development=(p.hidden.development+15).coerceAtMost(120);p.hidden.potential=maxOf(p.hidden.potential,120)
  p.fitness=maxOf(p.fitness,99.0);p.morale=100;p.form=maxOf(p.form,9.2);p.sharpness=100;p.archetype="Prime-Messi-Masterclass";p.messiMentored=true
  w.news("Lionel-Messi-Masterclass", "${p.name} hat die exklusive Masterclass abgeschlossen: alle Attribute +20, dazu Elite-Boni für Dribbling, Spielwitz, Abschluss, Pässe, Tempo und Standards.", "good")
 }
 fun effectiveDays(w: World)=TrainingSystems.effectiveDays(w,w.club(),w.training)
 fun preset(w: World,name: String){w.training.days=when(name){
  "Matchwoche"->mutableListOf(UnitType.RECOVERY,UnitType.VIDEO,UnitType.POSITIONAL,UnitType.TACTICS,UnitType.SET_PIECES,UnitType.RECOVERY,UnitType.OFF)
  "Aufbau"->mutableListOf(UnitType.RECOVERY,UnitType.FITNESS,UnitType.TECHNIQUE,UnitType.DUELS,UnitType.POSITIONAL,UnitType.OFF,UnitType.OFF)
  "Schonung"->mutableListOf(UnitType.RECOVERY,UnitType.OFF,UnitType.VIDEO,UnitType.TEAM_BONDING,UnitType.SET_PIECES,UnitType.OFF,UnitType.OFF)
  "Pressing"->mutableListOf(UnitType.RECOVERY,UnitType.TACTICS,UnitType.DUELS,UnitType.POSITIONAL,UnitType.GAME,UnitType.RECOVERY,UnitType.OFF)
  "Mental"->mutableListOf(UnitType.RECOVERY,UnitType.MENTAL,UnitType.TEAM_BONDING,UnitType.VIDEO,UnitType.POSITIONAL,UnitType.OFF,UnitType.OFF)
  else->TrainingPlan().days};w.training.intensity=when(name){"Schonung"->2;"Aufbau","Pressing"->4;else->3}}
 fun apply(w: World)=TrainingSystems.applyAll(w)
}
object ConstructionEngine {
 private fun level(w:World)=if(w.privateTopClubMode)RealModeDatabase.levelForTier(w.club().tier) else w.club().tier
 fun price(w: World,f: Facility)=(f.baseCost*(1+(10-level(w))*.45)).toLong().coerceAtLeast(f.baseCost.toLong())
 fun weeks(w: World,f: Facility)=f.weeks+(10-level(w))/3
 fun level(s: Stadium,f: Facility): Int=when(f){Facility.PITCH->s.pitchQuality;Facility.TRAINING->s.training;Facility.GYM->s.gym;Facility.MEDICINE->s.medicine;Facility.CABIN->s.cabin;Facility.STAND->s.stand;Facility.CLUBHOUSE->s.clubhouse;Facility.YOUTH->s.youth;Facility.CAPACITY->s.capacity;Facility.FLOODLIGHTS->if(s.floodlights)100 else 0;Facility.ARTIFICIAL->if(s.surface==Surface.ARTIFICIAL)100 else 0}
 fun reason(w: World,f: Facility): String? {val c=w.club();val s=c.stadium;return when{
  w.construction.count{it.clubId==c.id}>=(if(!w.privateTopClubMode&&c.tier>=7)1 else 2)->"Alle Baustellen sind belegt."
  w.construction.any{it.clubId==c.id&&it.facility==f}->"Dieser Ausbau läuft bereits."
  f==Facility.FLOODLIGHTS&&s.floodlights->"Flutlicht ist vorhanden."
  f==Facility.ARTIFICIAL&&s.surface!=Surface.HARD->"Kunstrasen ersetzt nur Hartplatz."
  f !in listOf(Facility.CAPACITY,Facility.ARTIFICIAL,Facility.FLOODLIGHTS)&&level(s,f)>=100->"Maximal ausgebaut."
  c.budget<price(w,f)->"Vereinskasse reicht nicht aus."
  else->null
 }}
 fun start(w: World,f: Facility){require(w.live==null){"Baustart nach dem Spiel."};require(reason(w,f)==null){reason(w,f)?:"Ausbau nicht möglich."};val cost=price(w,f);val weeks=weeks(w,f);w.club().budget-=cost;w.construction.add(ConstructionProject(w.nextIds.construction++,w.user.clubId,f,weeks,weeks,cost,if(f==Facility.CAPACITY)250*(11-level(w)) else 15));w.news("Baustart: ${f.label}","$cost € investiert. Bauzeit: $weeks Wochen.")}
 fun advance(w: World){for(p in w.construction.toList()){
  p.weeksLeft--;if(p.weeksLeft>0)continue;val c=w.clubs.getValue(p.clubId);val s=c.stadium;fun plus(v: Int)=(v+p.delta).coerceAtMost(100)
  when(p.facility){Facility.FLOODLIGHTS->s.floodlights=true;Facility.ARTIFICIAL->{s.surface=Surface.ARTIFICIAL;s.pitchQuality=85};Facility.PITCH->s.pitchQuality=plus(s.pitchQuality);Facility.TRAINING->s.training=plus(s.training);Facility.GYM->s.gym=plus(s.gym);Facility.MEDICINE->s.medicine=plus(s.medicine);Facility.CABIN->s.cabin=plus(s.cabin);Facility.STAND->{s.stand=plus(s.stand);s.seats=(s.seats+100).coerceAtMost(s.capacity)};Facility.CAPACITY->s.capacity+=p.delta;Facility.CLUBHOUSE->{s.clubhouse=plus(s.clubhouse);c.members+=15;w.squad(c.id).forEach{it.morale=(it.morale+5).coerceAtMost(100)}};Facility.YOUTH->s.youth=plus(s.youth)}
  w.construction.remove(p);if(p.clubId==w.user.clubId)w.news("${p.facility.label} fertig","Die Bauarbeiten sind abgeschlossen. Der Ausbau wirkt ab sofort.","good")
 }}
}
object EventsEngine {
 fun apply(w: World)=w.random{rng->
  val c=w.club();if(!rng.chance(if(!w.privateTopClubMode&&c.tier>=7).65 else .22))return@random
  val candidates=w.squad().filter{!it.youth&&it.id!=w.user.playerId};if(candidates.isEmpty())return@random
  val p=rng.pick(candidates);val kind=rng.int(0,9);var amount=0
  val text=when(kind){
   0->{p.fitness=(p.fitness-4).coerceAtLeast(5.0);amount=-4;"${p.name} kommt nach der Spätschicht zu spät. Fitness −4."}
   1->{p.unavailableReason=UnavailableReason.WORK;p.unavailableWeeks=1;amount=1;"${p.name} muss am Wochenende arbeiten und fehlt im nächsten Spiel."}
   2->{p.unavailableReason=UnavailableReason.HOLIDAY;p.unavailableWeeks=2;amount=2;"${p.name} ist zwei Spieltage im Urlaub."}
   3->{c.stadium.pitchQuality=(c.stadium.pitchQuality-8).coerceAtLeast(5);amount=-8;"Der Platz leidet unter schlechtem Wetter. Platzqualität −8."}
   4->{amount=rng.int(250,900);c.budget+=amount;"Der Dorf-Sponsor überweist zusätzlich $amount €."}
   5->{c.stadium.cabin=(c.stadium.cabin+5).coerceAtMost(100);amount=5;"Ehrenamtliche streichen die Kabine. Kabinenqualität +5."}
   6->{p.morale=(p.morale-8).coerceAtLeast(5);w.relationships[p.id]=(w.relationships[p.id]?:50)-5;amount=-8;"Die Kumpels halten ${p.lastName}, aber er möchte mehr Einsatzzeit. Moral −8."}
   7->{p.wantsMove=true;p.morale=(p.morale-6).coerceAtLeast(5);amount=-6;"${p.name} plant einen Umzug und ist wechselwillig. Moral −6."}
   8->{p.unavailableReason=UnavailableReason.RESERVE;p.unavailableWeeks=1;amount=1;"Die Reserve braucht ${p.name}. Er fehlt am nächsten Spieltag."}
   else->{w.squad().forEach{it.sharpness=(it.sharpness-2).coerceAtLeast(0)};amount=-2;if(!w.privateTopClubMode&&c.tier>=7)"Der Tennisverein blockt den Platz. Training fällt aus, Schärfe −2." else "Ein Medientermin stört die Vorbereitung. Schärfe −2."}
  }
  w.events.add(WorldEvent(w.calendar.absoluteWeek,kind.toString(),p.id,amount,text));while(w.events.size>100)w.events.removeAt(0);w.news("Rund um den Sportplatz",text,if(kind in 4..5)"good" else "bad")
 }
}
object SeasonEngine {
 private fun recordAndAdvanceCompetition(w: World,m: LiveMatch){val f=w.fixtures.first{it.id==m.fixtureId};MatchEngine.record(w,m);CompetitionEngine.afterRecorded(w,f)}
 private fun simulateScheduledCompetitionBackground(w: World,day: Int,competition: CompetitionType?=null){
  val matches=w.fixtures.filter{it.matchday==day&&!it.played&&it.competition!=CompetitionType.LEAGUE&&(competition==null||it.competition==competition)}.toList()
  for(f in matches){val m=MatchEngine.simulateFullMatch(w,f);recordAndAdvanceCompetition(w,m)}
 }
 /** The repository commits this entire transaction before confirming a completed week. */
 fun advanceWeek(w: World){
  val live=w.live?:error("Kein laufendes Spiel.");require(live.finished){"Zuerst das Spiel beenden."};require(w.nextFixture()?.id==live.fixtureId){"Spieltag und Partie passen nicht zusammen."}
  val current=w.fixtures.first{it.id==live.fixtureId}
  if(current.competition!=CompetitionType.LEAGUE){
   recordAndAdvanceCompetition(w,live);simulateScheduledCompetitionBackground(w,w.calendar.matchday,current.competition)
   val own=if(live.homeId==w.user.clubId)live.home.goals else live.away.goals;val other=if(live.homeId==w.user.clubId)live.away.goals else live.home.goals
   val detail=when{current.homePens>0||current.awayPens>0->" · Entscheidung im Elfmeterschießen";live.extraTimePlayed->" · nach Verlängerung";else->""}
   w.news(CompetitionEngine.displayName(w,current.competition,CompetitionEngine.cupGroup(w,current)),"${current.stage}: $own:$other$detail","normal");w.squad().filter{it.temporarySeniorCallUp}.map{it.id}.forEach{YouthCompetitionSystem.returnToYouth(w,it)};w.live=null;return
  }
  MatchEngine.record(w,live)
  for(f in w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.matchday==w.calendar.matchday&&!it.played})MatchEngine.record(w,MatchEngine.simulateFullMatch(w,f))
  // Ist der Nutzer in einem Zusatzwettbewerb nicht vertreten, werden diese Partien mit derselben MatchEngine im Hintergrund gespielt.
  simulateScheduledCompetitionBackground(w,w.calendar.matchday)
  TrainingEngine.apply(w);ConstructionEngine.advance(w)
  for(c in w.clubs.values){
   c.wageBill=w.squad(c.id).sumOf{it.wage};EconomySystem.weekly(w,c)
   for(p in w.squad(c.id)){
    if(p.injuryWeeks>0){p.injuryWeeks=(p.injuryWeeks-1-(if(c.stadium.medicine>=60&&w.calendar.absoluteWeek%2==0)1 else 0)).coerceAtLeast(0);if(p.injuryWeeks==0)p.injury=""}
    if(p.unavailableWeeks>0){p.unavailableWeeks--;if(p.unavailableWeeks==0)p.unavailableReason=null}
    p.fitness=(p.fitness+14+c.stadium.medicine*.03+c.stadium.cabin*.02).coerceAtMost(100.0)
    if(p.id !in live.participation&&p.clubId==w.user.clubId&&!p.youth){p.sharpness=(p.sharpness-2).coerceAtLeast(0);p.morale=(p.morale-1).coerceAtLeast(5)}
   }
   if(c.budget<0){w.squad(c.id).forEach{it.morale=(it.morale-3).coerceAtLeast(5)};if(c.id==w.user.clubId)w.news("Kasse im Minus","Neue Ausbauten sind gesperrt. Fehlende Mittel drücken die Moral.","bad")}
  }
  w.calendar.absoluteWeek++;EventsEngine.apply(w)
  val own=if(live.homeId==w.user.clubId)live.home.goals else live.away.goals;val other=if(live.homeId==w.user.clubId)live.away.goals else live.home.goals
  w.news("Spieltag ${w.calendar.matchday}: $own:$other","${live.attendance} Zuschauer. Wochenzuflüsse: ${w.club().lastIncome} €. Kosten: ${w.club().lastCosts} €.",if(own>other)"good" else if(own<other)"bad" else "normal")
  w.squad().filter{it.temporarySeniorCallUp}.map{it.id}.forEach{YouthCompetitionSystem.returnToYouth(w,it)}
  w.live=null;w.calendar.matchday++
  completeSeasonIfDue(w)
 }
 /** Advances weeks without a home-club fixture, common in shorter foreign divisions. */
 fun advanceIdleWeek(w:World){
  require(w.live==null&&w.nextFixture()==null){"Eine eigene Partie steht noch aus."}
  require(w.calendar.matchday<=maxOf(w.leagues.maxOf{(it.clubIds.size-1)*2},w.fixtures.maxOfOrNull{it.matchday}?:0)){"Die Saison ist bereits beendet."}
  for(f in w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.matchday==w.calendar.matchday&&!it.played})MatchEngine.record(w,MatchEngine.simulateFullMatch(w,f))
  simulateScheduledCompetitionBackground(w,w.calendar.matchday)
  TrainingEngine.apply(w);ConstructionEngine.advance(w)
  for(c in w.clubs.values){c.wageBill=w.squad(c.id).sumOf{it.wage};EconomySystem.weekly(w,c)
   for(p in w.squad(c.id)){if(p.injuryWeeks>0)p.injuryWeeks--;p.fitness=(p.fitness+14).coerceAtMost(100.0)}
  }
  w.calendar.absoluteWeek++;w.calendar.matchday++;completeSeasonIfDue(w)
 }
 fun advanceUntilNextMatch(w:World){
  var weeks=0
  while(w.live==null&&w.nextFixture()==null&&weeks++<50)advanceIdleWeek(w)
 }
 private fun completeSeasonIfDue(w:World){
  val seasonWeeks=maxOf(w.leagues.maxOf{(it.clubIds.size-1)*2},w.fixtures.maxOfOrNull{it.matchday}?:0)
  if(w.calendar.matchday>seasonWeeks){
   // 18er-Ligen enden vier Wochen vor 20er-Ligen. Die übrigen nationalen Partien sowie
   // eventuell durch Ergebnisse neu angesetzte K.-o.-Runden werden deterministisch beendet.
   var guard=0
   while(true){
    val f=w.fixtures.filter{it.season==w.calendar.season&&!it.played}.minWithOrNull(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.id})?:break
    check(guard++<10000){"Saisonabschluss konnte nicht vollständig simuliert werden."}
    if(f.competition==CompetitionType.LEAGUE)MatchEngine.record(w,MatchEngine.simulateFullMatch(w,f)) else recordAndAdvanceCompetition(w,MatchEngine.simulateFullMatch(w,f))
   }
   endSeason(w)
  }
 }
 fun endSeason(w: World){
  require(w.fixtures.filter{it.season==w.calendar.season}.all{it.played}){"Die Saison ist noch nicht vollständig gespielt."}
  val tables=w.leagues.associate{it.tier to WorldFactory.table(w,it.tier)};val tier=w.club().tier;val rows=tables.getValue(tier);val row=rows.first{it.clubId==w.user.clubId};val rank=rows.indexOf(row)+1
  val germanPyramid=w.privateTopClubMode&&w.leagues.any{it.name=="Oberliga Hamburg"}
  val outcome=when{
   germanPyramid&&tier in 1..5&&rank<=2&&tier>1->"Aufstieg"
   germanPyramid&&tier in 1..5&&rank>rows.size-2&&tier<5->"Abstieg"
   w.privateTopClubMode->{val country=WorldFactory.leagueCountry(w,tier);val divisions=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==country};val levels=divisions.map{RealModeDatabase.levelForTier(it.tier)}.distinct().sorted();val level=RealModeDatabase.levelForTier(tier);val promotionPlaces=if(divisions.count{RealModeDatabase.levelForTier(it.tier)==level}>1)1 else 2;val relegationPlaces=divisions.count{RealModeDatabase.levelForTier(it.tier)==level+1}.let{if(it>1)it else 2};when{level>levels.first()&&rank<=promotionPlaces->"Aufstieg";level<levels.last()&&rank>rows.size-relegationPlaces->"Abstieg";else->"Verbleib"}}
   rank<=2&&tier>1->"Aufstieg"
   rank>=11&&tier<10->"Abstieg"
   else->"Verbleib"
  };val awards=mutableListOf<String>()
  awards.addAll(CompetitionPrizeSystem.settleSeason(w,tables))
  val top=w.players.values.filter{it.clubId in w.leagues.first{l->l.tier==tier}.clubIds}.sortedWith(compareByDescending<Player>{it.stats.goals}.thenBy{it.id}).firstOrNull()
  if(top?.id==w.user.playerId&&w.self().stats.goals>0)awards.add("Torjäger: ${w.self().stats.goals} Tore")
  w.history.add(SeasonHistory(w.calendar.season,w.user.clubId,WorldFactory.leagueName(w,tier),rank,row.points,row.goalsFor,outcome,awards))
  w.lastLeagueRankings.clear();for((leagueTier,leagueRows) in tables)w.lastLeagueRankings[leagueTier]=leagueRows.map{it.clubId}.toMutableList()
  // Im privaten Topclub-Spielstand bleibt die 12er-Eliteliga bewusst stabil. Dadurch werden
  // Real-Clubs nicht nach einer Saison in die Fantasiepyramide verschoben.
  if(!w.privateTopClubMode){
   val targets=w.clubs.values.filter{it.tier in 1..10}.associate{it.id to it.tier}.toMutableMap()
   for(t in 1..9){tables.getValue(t).takeLast(2).forEach{targets[it.clubId]=t+1};tables.getValue(t+1).take(2).forEach{targets[it.clubId]=t}}
   for((id,t) in targets){val c=w.clubs.getValue(id);val promoted=t<c.tier;c.tier=t;c.reputation=(c.reputation+if(promoted)5 else -1).coerceIn(5,100);c.form.clear();if(promoted)c.budget+=if(t>=7)5000 else (11-t)*120000L;c.sponsor.weekly=if(t>=7)210+(10-t)*65 else (11-t)*5500}
   w.leagues.forEach{l->l.clubIds=w.clubs.values.filter{it.tier==l.tier}.map{it.id}.sorted().toMutableList()}
  }else if(germanPyramid){
   val targets=w.clubs.values.associate{it.id to it.tier}.toMutableMap()
   for(t in 1..4){
    tables.getValue(t).takeLast(2).forEach{targets[it.clubId]=t+1}
    tables.getValue(t+1).take(2).forEach{targets[it.clubId]=t}
   }
   for((id,newTier) in targets){
    val c=w.clubs.getValue(id);val old=c.tier;c.tier=newTier;c.form.clear()
    if(newTier<old){c.reputation=(c.reputation+4).coerceAtMost(100);c.budget+=when(newTier){1->2_500_000;2->900_000;3->300_000;4->100_000;else->40_000}}
    else if(newTier>old)c.reputation=(c.reputation-2).coerceAtLeast(25)
   }
   w.leagues.filter{it.tier in 1..5}.forEach{l->l.clubIds=w.clubs.values.filter{it.tier==l.tier}.map{it.id}.sorted().toMutableList()}
  }else w.clubs.values.filter{it.tier>=1}.forEach{it.form.clear()}
  if(w.privateTopClubMode){
   val countries=w.leagues.map{WorldFactory.leagueCountry(w,it.tier)}.distinct().filter{it!="Deutschland"}
   val moves=mutableMapOf<Int,Int>()
   for(country in countries){val byLevel=w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)==country}.groupBy{RealModeDatabase.levelForTier(it.tier)}.toSortedMap()
    val levels=byLevel.keys.toList()
    for(index in 0 until levels.lastIndex){val higher=byLevel.getValue(levels[index]);val lower=byLevel.getValue(levels[index+1])
     for(upper in higher){
      val lowerSlots=if(lower.size==1)listOf(lower.first(),lower.first()) else lower
      val promoted=lowerSlots.mapIndexed{i,l->tables.getValue(l.tier)[if(lower.size==1)i else 0].clubId to l.tier}
      val relegated=tables.getValue(upper.tier).takeLast(promoted.size)
      for(i in promoted.indices){moves[promoted[i].first]=upper.tier;moves[relegated[i].clubId]=promoted[i].second}
     }
    }
   }
   for((id,newTier) in moves){val c=w.clubs.getValue(id);c.tier=newTier;c.reputation=(c.reputation+if(newTier<tables.entries.first{it.value.any{r->r.clubId==id}}.key)3 else -2).coerceIn(25,100)}
   w.leagues.filter{WorldFactory.leagueCountry(w,it.tier)!="Deutschland"}.forEach{l->l.clubIds=w.clubs.values.filter{it.tier==l.tier}.map{it.id}.sorted().toMutableList()}
  }
  if(w.privateTopClubMode)check(w.leagues.all{it.clubIds.size in 6..24&&it.clubIds.size%2==0}) else check(w.leagues.all{it.clubIds.size==12})
  w.random{rng->
   for(p in w.players.values.toList()){
    if(p.retired)continue
    if(p.stats.appearances>0)p.career.add(PlayerSeason(w.calendar.season,w.clubs[p.clubId]?.name?:"Vereinslos",p.stats.copy()))
    p.stats=Stats();val age=w.calendar.season+1-p.birthYear
    if(age>=35){p.attributes.pace=(p.attributes.pace-1).coerceAtLeast(1);p.attributes.stamina=(p.attributes.stamina-1).coerceAtLeast(1)}
    if(p.id!=w.user.playerId&&age>=36&&rng.chance(.15+(age-36)*.12)){p.retired=true;p.clubId=0;continue}
    if(age<=23&&rng.chance(.7))p.attributes.improve(rng.pick(Focus.entries),p.hidden.potential)
    if(p.youth&&age>=19)p.youth=false
    p.fitness=95.0;p.sharpness=55;p.injuryWeeks=(p.injuryWeeks-4).coerceAtLeast(0);p.unavailableWeeks=0;p.unavailableReason=null;if(p.injuryWeeks==0)p.injury=""
    w.clubs[p.clubId]?.let{club->p.wage=if(club.tier==0)p.wage.coerceAtLeast(1500) else if(w.privateTopClubMode)p.wage.coerceAtLeast(120) else if(club.tier>=7)p.wage.coerceAtMost(15) else (11-club.tier)*rng.int(150,350)}
   }
   w.calendar.season++
   YouthEngine.newSeason(w,rng)
   for(c in w.clubs.values){
    if(c.tier>=1)repeat((if(c.stadium.youth>=28)4 else 2)+(if(c.youthPhilosophy=="Breitensport")1 else 0)){WorldFactory.spawnYouth(w,c,rng)}
    if(c.id!=w.user.clubId){w.squad(c.id).filter{it.youth&&w.calendar.season-it.birthYear>=17}.sortedByDescending{it.ca}.take(2).forEach{it.youth=false};val level=if(w.privateTopClubMode)RealModeDatabase.levelForTier(c.tier) else c.tier;while(w.squad(c.id).count{!it.youth}<20){val p=WorldFactory.generatePlayer(w.nextIds.player++,c.id,level,rng.pick(Position.entries),rng,w.calendar.season);w.players[p.id]=p}}
    WorldFactory.autoLineup(w,c.id)
   }
   repeat(12){val level=if(w.privateTopClubMode)RealModeDatabase.levelForTier(w.club().tier) else w.club().tier;val p=WorldFactory.generatePlayer(w.nextIds.player++,0,level,rng.pick(Position.entries),rng,w.calendar.season);w.players[p.id]=p}
  }
  w.training.extra.removeAll{w.players[it.playerId]?.let{p->p.clubId!=w.user.clubId||p.retired}!=false};w.calendar.matchday=1;WorldFactory.makeSchedule(w)
  w.news("$outcome: Saison ${w.calendar.season}","Platz $rank mit ${row.points} Punkten. Es geht weiter in der ${WorldFactory.leagueName(w,w.club().tier)}. Neue Jugendspieler stehen bereit.",if(outcome=="Aufstieg")"good" else if(outcome=="Abstieg")"bad" else "normal")
 }
}
object ClubActions {
 fun promote(w: World,id: Int){require(w.live==null){"Jugendspieler nach dem Spiel hochziehen."};val p=w.players.getValue(id);require(p.clubId==w.user.clubId&&p.youth);val ready=YouthEngine.readiness(w,p);p.youth=false;p.wage=maxOf(0,p.wage);p.morale=(p.morale+if(ready>=65)10 else if(ready>=45)4 else -6).coerceIn(5,100);if(ready<45){p.fitness=(p.fitness-8).coerceAtLeast(35.0);p.youthProfile.confidence=(p.youthProfile.confidence-12).coerceAtLeast(5);w.club().dynamics.chemistry=(w.club().dynamics.chemistry-2).coerceAtLeast(0)}else{p.youthProfile.confidence=(p.youthProfile.confidence+6).coerceAtMost(100)};WorldFactory.rebuildBench(w,w.club());w.news("Jugend rückt auf","${p.name} wird in den Profikader hochgezogen. Bereitschaft $ready/100 – ${YouthEngine.riskLabel(w,p)}.",if(ready>=60)"good" else "normal")}
 fun sign(w: World,id: Int){
  require(w.live==null){"Transfers nach dem Spiel abschließen."};val p=w.players.getValue(id);val o=TransferEngine.createOffer(w,w.user.clubId,id,DealType.BUY,if(p.ca>=w.squad().filter{!it.youth}.map{it.ca}.average())SquadRole.STARTER else SquadRole.ROTATION)
  repeat(5){if(o.status!=NegotiationStatus.AGREED&&o.status!=NegotiationStatus.REJECTED)TransferEngine.improve(w,o.id,when{ o.sellerScore<62->"fee";o.playerScore<62->"role";else->"wage"})}
  require(o.status==NegotiationStatus.AGREED){o.message};TransferEngine.complete(w,o.id)
 }
 fun release(w: World,id: Int){require(w.live==null){"Freistellung erst nach dem Spiel."};val p=w.players.getValue(id);require(id!=w.user.playerId&&p.clubId==w.user.clubId){"Der Spielertrainer bleibt im Verein."};require(p.youth||w.squad().count{!it.youth}>16){"Mindestens 16 Spieler im Kader behalten."};p.clubId=0;p.youth=false;p.wage=0;p.wantsMove=true;w.training.extra.removeAll{it.playerId==id};WorldFactory.autoLineup(w);w.news("${p.name} verabschiedet","Der Spieler ist freigestellt.")}
 fun talk(w: World,id: Int,kind: Int){val p=w.players.getValue(id);require(p.clubId==w.user.clubId);require(p.lastTalkWeek!=w.calendar.absoluteWeek){"Diese Woche habt ihr bereits gesprochen."};p.lastTalkWeek=w.calendar.absoluteWeek;val delta=when(kind){0->if(p.hidden.ambition>55)5 else -3;1->{p.sharpness=(p.sharpness+3).coerceAtMost(100);4};else->if(p.wantsMove)2 else 3};p.morale=(p.morale+delta).coerceIn(5,100);w.relationships[id]=((w.relationships[id]?:50)+delta).coerceIn(0,100);w.news("Gespräch mit ${p.lastName}","${if(kind==0)"Du forderst mehr Einsatz im Training." else if(kind==1)"Du ermutigst ihn, seine nächste Chance zu nutzen." else "Ihr sprecht offen über seine Rolle."} Moral ${if(delta>=0)"+" else ""}$delta.")}
 fun scouting(w: World,p: Player): String {if(p.clubId==w.user.clubId)return p.ca.toString();return when(w.user.difficulty){Difficulty.CASUAL->p.ca.toString();Difficulty.NORMAL->"${(p.ca-3).coerceAtLeast(1)}–${(p.ca+3).coerceAtMost(99)}";Difficulty.REALISTIC->"${(p.ca-7).coerceAtLeast(1)}–${(p.ca+7).coerceAtMost(99)}";Difficulty.HARDCORE->"${p.ca/15*15+1}–${((p.ca/15+1)*15).coerceAtMost(99)}";Difficulty.SANDBOX->p.ca.toString()}}
}

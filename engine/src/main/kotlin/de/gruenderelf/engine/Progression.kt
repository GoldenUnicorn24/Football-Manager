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
 private fun scalable(f:Facility)=f !in listOf(Facility.CAPACITY,Facility.ARTIFICIAL,Facility.FLOODLIGHTS)
 fun level(s: Stadium,f: Facility): Int=when(f){Facility.PITCH->s.pitchQuality;Facility.TRAINING->s.training;Facility.GYM->s.gym;Facility.MEDICINE->s.medicine;Facility.CABIN->s.cabin;Facility.STAND->s.stand;Facility.CLUBHOUSE->s.clubhouse;Facility.YOUTH->s.youth;Facility.CAPACITY->s.capacity;Facility.FLOODLIGHTS->if(s.floodlights)100 else 0;Facility.ARTIFICIAL->if(s.surface==Surface.ARTIFICIAL)100 else 0}
 private fun eliteMinimumCost(f:Facility,level:Int):Long{
  val base=when(f){Facility.TRAINING->75_000_000L;Facility.MEDICINE->90_000_000L;Facility.GYM->65_000_000L;Facility.YOUTH->80_000_000L;Facility.PITCH->45_000_000L;Facility.CLUBHOUSE->35_000_000L;Facility.CABIN->25_000_000L;Facility.STAND->55_000_000L;else->25_000_000L}
  val stage=when{level>=140->10.0;level>=130->6.0;level>=120->3.5;level>=110->2.0;else->1.0}
  return (base*stage).toLong()
 }
 fun price(w: World,f: Facility):Long{
  val normal=(f.baseCost*(1+(10-w.club().tier)*.45)).toLong()
  if(!scalable(f))return normal
  val current=level(w.club().stadium,f)
  return if(current<100)normal else maxOf(normal,eliteMinimumCost(f,current))
 }
 fun weeks(w: World,f: Facility):Int{
  val normal=f.weeks+(10-w.club().tier)/3
  if(!scalable(f))return normal
  val current=level(w.club().stadium,f)
  return when{current<100->normal;current<110->maxOf(normal,8);current<120->maxOf(normal,10);current<130->maxOf(normal,12);current<140->maxOf(normal,15);else->maxOf(normal,18)}
 }
 private fun delta(s:Stadium,f:Facility):Int{
  if(f==Facility.CAPACITY)return 250
  if(!scalable(f))return 0
  return when(level(s,f)){in Int.MIN_VALUE..99->15;in 100..119->5;in 120..134->3;in 135..144->2;else->1}
 }
 fun reason(w: World,f: Facility): String? {val c=w.club();val s=c.stadium;return when{
  w.construction.count{it.clubId==c.id}>=(if(!w.privateTopClubMode&&c.tier>=7)1 else if(w.developer.enabled)3 else 2)->"Alle Baustellen sind belegt."
  w.construction.any{it.clubId==c.id&&it.facility==f}->"Dieser Ausbau läuft bereits."
  f==Facility.FLOODLIGHTS&&s.floodlights->"Flutlicht ist vorhanden."
  f==Facility.ARTIFICIAL&&s.surface!=Surface.HARD->"Kunstrasen ersetzt nur Hartplatz."
  scalable(f)&&level(s,f)>=FACILITY_LEVEL_MAX->"Elite-Maximum 150 erreicht."
  c.budget<price(w,f)->"Vereinskasse reicht für ${price(w,f)} € nicht aus."
  else->null
 }}
 fun start(w: World,f: Facility){
  require(w.live==null){"Baustart nach dem Spiel."};require(reason(w,f)==null){reason(w,f)?:"Ausbau nicht möglich."}
  val cost=price(w,f);val duration=weeks(w,f);w.club().budget-=cost
  val d=if(f==Facility.CAPACITY)250*(11-w.club().tier) else delta(w.club().stadium,f)
  w.construction.add(ConstructionProject(w.nextIds.construction++,w.user.clubId,f,duration,duration,cost,d))
  w.news("Baustart: ${f.label}","$cost € investiert. Bauzeit: $duration Wochen. ${if(scalable(f)&&level(w.club().stadium,f)>=100)"Eliteausbau über Stufe 100: kleine Schritte, exponentiell höhere Kosten." else ""}")
 }
 fun advance(w: World){for(p in w.construction.toList()){
  p.weeksLeft--;if(p.weeksLeft>0)continue
  val c=w.clubs.getValue(p.clubId);val s=c.stadium
  fun plus(v: Int)=(v+p.delta).coerceAtMost(FACILITY_LEVEL_MAX)
  when(p.facility){
   Facility.FLOODLIGHTS->s.floodlights=true
   Facility.ARTIFICIAL->{s.surface=Surface.ARTIFICIAL;s.pitchQuality=maxOf(s.pitchQuality,85)}
   Facility.PITCH->s.pitchQuality=plus(s.pitchQuality)
   Facility.TRAINING->s.training=plus(s.training)
   Facility.GYM->s.gym=plus(s.gym)
   Facility.MEDICINE->s.medicine=plus(s.medicine)
   Facility.CABIN->s.cabin=plus(s.cabin)
   Facility.STAND->{s.stand=plus(s.stand);s.seats=(s.seats+maxOf(100,p.delta*100)).coerceAtMost(s.capacity)}
   Facility.CAPACITY->s.capacity+=p.delta
   Facility.CLUBHOUSE->{s.clubhouse=plus(s.clubhouse);c.members+=15;w.squad(c.id).forEach{it.morale=(it.morale+5).coerceAtMost(100)}}
   Facility.YOUTH->s.youth=plus(s.youth)
  }
  w.construction.remove(p)
  if(p.clubId==w.user.clubId)w.news("${p.facility.label} fertig","Die Bauarbeiten sind abgeschlossen. Ausbaustand: ${if(scalable(p.facility))level(s,p.facility) else "aktiv"}.","good")
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
   w.news(CompetitionEngine.displayName(w,current.competition),"${current.stage}: $own:$other$detail","normal");w.live=null;return
  }
  MatchEngine.record(w,live)
  for(f in w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.matchday==w.calendar.matchday&&!it.played})MatchEngine.record(w,MatchEngine.simulateFullMatch(w,f))
  // Ist der Nutzer in einem Zusatzwettbewerb nicht vertreten, werden diese Partien mit derselben MatchEngine im Hintergrund gespielt.
  simulateScheduledCompetitionBackground(w,w.calendar.matchday)
  TrainingEngine.apply(w);ConstructionEngine.advance(w);BmwDeveloperSystems.weekly(w)
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
  if(w.calendar.absoluteWeek%4==3){
   val played=w.fixtures.count{it.season==w.calendar.season&&it.played&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)}.coerceAtLeast(1)
   val club=w.club()
   for(p in w.squad().filter{!it.youth&&it.id!=w.user.playerId}){
    val target=when(p.promisedRole){SquadRole.STAR->.80;SquadRole.STARTER->.65;SquadRole.ROTATION->.35;SquadRole.PROSPECT->.20;SquadRole.BACKUP->.10}
    val share=p.stats.appearances.toDouble()/played
    when{
     share+0.20<target->{p.morale=(p.morale-4).coerceAtLeast(5);w.relationships[p.id]=((w.relationships[p.id]?:50)-3).coerceAtLeast(0);club.dynamics.hierarchyStability=(club.dynamics.hierarchyStability-1).coerceAtLeast(0);if(target>=.35&&share+0.35<target&&p.hidden.ambition>55)p.wantsMove=true}
     share+0.05<target->p.morale=(p.morale-1).coerceAtLeast(5)
     else->{p.morale=(p.morale+1).coerceAtMost(100);w.relationships[p.id]=((w.relationships[p.id]?:50)+1).coerceAtMost(100)}
    }
   }
  }
  w.calendar.absoluteWeek++;EventsEngine.apply(w)
  val own=if(live.homeId==w.user.clubId)live.home.goals else live.away.goals;val other=if(live.homeId==w.user.clubId)live.away.goals else live.home.goals
  w.news("Spieltag ${w.calendar.matchday}: $own:$other","${live.attendance} Zuschauer. Wochenzuflüsse: ${w.club().lastIncome} €. Kosten: ${w.club().lastCosts} €.",if(own>other)"good" else if(own<other)"bad" else "normal")
  w.live=null;w.calendar.matchday++
  val ownLeague=w.leagues.first{w.user.clubId in it.clubIds};val ownRounds=(ownLeague.clubIds.size-1)*2
  if(w.calendar.matchday>ownRounds){
   // 18er-Ligen enden vier Wochen vor 20er-Ligen. Die übrigen nationalen Partien sowie
   // eventuell durch Ergebnisse neu angesetzte K.-o.-Runden werden deterministisch beendet.
   var guard=0
   while(true){
    val f=w.fixtures.filter{it.season==w.calendar.season&&!it.played}.minWithOrNull(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.id})?:break
    check(guard++<5000){"Saisonabschluss konnte nicht vollständig simuliert werden."}
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
   w.privateTopClubMode->"Verbleib"
   rank<=2&&tier>1->"Aufstieg"
   rank>=11&&tier<10->"Abstieg"
   else->"Verbleib"
  };val awards=mutableListOf<String>()
  val titlePrizes=CompetitionPrizeSystem.awardSeasonTitles(w,tables)
  if(rank==1)awards.add("Meister der ${WorldFactory.leagueName(w,tier)}")
  titlePrizes.userAwards.filter{it !in awards}.forEach{awards.add(it)}
  val top=w.players.values.filter{it.clubId in w.leagues.first{l->l.tier==tier}.clubIds}.sortedWith(compareByDescending<Player>{it.stats.goals}.thenBy{it.id}).firstOrNull()
  if(top?.id==w.user.playerId&&w.self().stats.goals>0)awards.add("Torjäger: ${w.self().stats.goals} Tore")
  w.history.add(SeasonHistory(w.calendar.season,w.user.clubId,WorldFactory.leagueName(w,tier),rank,row.points,row.goalsFor,outcome,awards))
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
  }else w.clubs.values.filter{it.tier in 1..10}.forEach{it.form.clear()}
  if(w.privateTopClubMode)check(w.leagues.all{it.clubIds.size in 18..20&&it.clubIds.size%2==0}) else check(w.leagues.all{it.clubIds.size==12})
  w.random{rng->
   for(p in w.players.values.toList()){
    if(p.retired)continue
    if(p.stats.appearances>0)p.career.add(PlayerSeason(w.calendar.season,w.clubs[p.clubId]?.name?:"Vereinslos",p.stats.copy()))
    p.stats=Stats();val age=BmwDeveloperSystems.effectiveAge(w,p,w.calendar.season+1)
    if(age>=35){val keep=w.developer.longevity[p.id]?.primeRetention?:0.0;if(!w.developer.enabled||rng.chance((1.0-keep).coerceIn(.08,1.0))){p.attributes.pace=(p.attributes.pace-1).coerceAtLeast(1);p.attributes.stamina=(p.attributes.stamina-1).coerceAtLeast(1)}}
    if(p.id!=w.user.playerId&&age>=36&&rng.chance((.15+(age-36)*.12)*(1.0-(w.developer.longevity[p.id]?.primeRetention?:0.0)*.72))){p.retired=true;p.clubId=0;continue}
    if(age<=23&&rng.chance(.7))p.attributes.improve(rng.pick(Focus.entries),minOf(p.hidden.potential,REGULAR_DEVELOPMENT_CAP))
    if(p.youth&&age>=23)p.youth=false else if(p.youth)p.youthSquad=if(age<=18)YouthSquad.U19 else YouthSquad.U23
    p.fitness=95.0;p.sharpness=55;p.injuryWeeks=(p.injuryWeeks-4).coerceAtLeast(0);p.unavailableWeeks=0;p.unavailableReason=null;if(p.injuryWeeks==0)p.injury=""
    w.clubs[p.clubId]?.let{club->p.wage=if(club.tier==0)p.wage.coerceAtLeast(1500) else if(w.privateTopClubMode)p.wage.coerceAtLeast(120) else if(club.tier>=7)p.wage.coerceAtMost(15) else (11-club.tier)*rng.int(150,350)}
   }
   w.calendar.season++
   YouthEngine.newSeason(w,rng);YouthCompetitionSystem.newSeason(w);ScoutingTransferSystem.newSeason(w)
   for(c in w.clubs.values){
    if(c.tier in 1..10)repeat((if(c.stadium.youth>=28)4 else 2)+(if(c.youthPhilosophy=="Breitensport")1 else 0)){WorldFactory.spawnYouth(w,c,rng)}
    if(c.id!=w.user.clubId){w.squad(c.id).filter{it.youth&&w.calendar.season-it.birthYear>=19}.sortedByDescending{it.ca}.take(2).forEach{it.youth=false};val level=if(w.privateTopClubMode)RealModeDatabase.levelForTier(c.tier) else c.tier;while(w.squad(c.id).count{!it.youth}<20){val p=WorldFactory.generatePlayer(w.nextIds.player++,c.id,level,rng.pick(Position.entries),rng,w.calendar.season);w.players[p.id]=p}}
    WorldFactory.autoLineup(w,c.id)
   }
   repeat(12){val level=if(w.privateTopClubMode)RealModeDatabase.levelForTier(w.club().tier) else w.club().tier;val p=WorldFactory.generatePlayer(w.nextIds.player++,0,level,rng.pick(Position.entries),rng,w.calendar.season);w.players[p.id]=p}
  }
  w.training.extra.removeAll{w.players[it.playerId]?.let{p->p.clubId!=w.user.clubId||p.retired}!=false};w.calendar.matchday=1;WorldFactory.makeSchedule(w)
  w.news("$outcome: Saison ${w.calendar.season}","Platz $rank mit ${row.points} Punkten. Es geht weiter in der ${WorldFactory.leagueName(w,w.club().tier)}. Neue Jugendspieler stehen bereit.",if(outcome=="Aufstieg")"good" else if(outcome=="Abstieg")"bad" else "normal")
 }
}
object ClubActions {
 fun promote(w: World,id: Int){require(w.live==null){"Jugendspieler nach dem Spiel hochziehen."};val p=w.players.getValue(id);require(p.clubId==w.user.clubId&&p.youth);val ready=YouthEngine.readiness(w,p);p.youth=false;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false;p.wage=maxOf(0,p.wage);p.morale=(p.morale+if(ready>=65)10 else if(ready>=45)4 else -6).coerceIn(5,100);if(ready<45){p.fitness=(p.fitness-8).coerceAtLeast(35.0);p.youthProfile.confidence=(p.youthProfile.confidence-12).coerceAtLeast(5);w.club().dynamics.chemistry=(w.club().dynamics.chemistry-2).coerceAtLeast(0)}else{p.youthProfile.confidence=(p.youthProfile.confidence+6).coerceAtMost(100)};WorldFactory.normalizeBench(w,w.club(),true);w.news("Jugend rückt auf","${p.name} wird in den Profikader hochgezogen. Bereitschaft $ready/100 – ${YouthEngine.riskLabel(w,p)}.",if(ready>=60)"good" else "normal")}
 fun sign(w: World,id: Int){
  require(w.live==null){"Transfers nach dem Spiel abschließen."};val p=w.players.getValue(id);val o=TransferEngine.createOffer(w,w.user.clubId,id,DealType.BUY,if(p.ca>=w.squad().filter{!it.youth}.map{it.ca}.average())SquadRole.STARTER else SquadRole.ROTATION)
  repeat(5){if(o.status!=NegotiationStatus.AGREED&&o.status!=NegotiationStatus.REJECTED)TransferEngine.improve(w,o.id,when{ o.sellerScore<62->"fee";o.playerScore<62->"role";else->"wage"})}
  require(o.status==NegotiationStatus.AGREED){o.message};TransferEngine.complete(w,o.id)
 }
 fun release(w: World,id: Int){require(w.live==null){"Freistellung erst nach dem Spiel."};val p=w.players.getValue(id);require(id!=w.user.playerId&&p.clubId==w.user.clubId){"Der Spielertrainer bleibt im Verein."};require(p.youth||w.squad().count{!it.youth}>16){"Mindestens 16 Spieler im Kader behalten."};val c=w.club();val wasStarter=id in c.tactics.xi;p.clubId=0;p.youth=false;p.wage=0;p.wantsMove=true;w.training.extra.removeAll{it.playerId==id};c.tactics.bench.remove(id);WorldFactory.reconcileMatchdaySelection(w);w.news("${p.name} verabschiedet","Der Spieler ist freigestellt.")}
 fun talk(w: World,id: Int,kind: Int){val p=w.players.getValue(id);require(p.clubId==w.user.clubId);require(p.lastTalkWeek!=w.calendar.absoluteWeek){"Diese Woche habt ihr bereits gesprochen."};p.lastTalkWeek=w.calendar.absoluteWeek;val delta=when(kind){0->if(p.hidden.ambition>55)5 else -3;1->{p.sharpness=(p.sharpness+3).coerceAtMost(100);4};else->if(p.wantsMove)2 else 3};p.morale=(p.morale+delta).coerceIn(5,100);w.relationships[id]=((w.relationships[id]?:50)+delta).coerceIn(0,100);w.news("Gespräch mit ${p.lastName}","${if(kind==0)"Du forderst mehr Einsatz im Training." else if(kind==1)"Du ermutigst ihn, seine nächste Chance zu nutzen." else "Ihr sprecht offen über seine Rolle."} Moral ${if(delta>=0)"+" else ""}$delta.")}
 fun scouting(w: World,p: Player): String {if(p.clubId==w.user.clubId)return p.ca.toString();return when(w.user.difficulty){Difficulty.CASUAL->p.ca.toString();Difficulty.NORMAL->"${(p.ca-3).coerceAtLeast(1)}–${(p.ca+3).coerceAtMost(99)}";Difficulty.REALISTIC->"${(p.ca-7).coerceAtLeast(1)}–${(p.ca+7).coerceAtMost(99)}";Difficulty.HARDCORE->"${p.ca/15*15+1}–${((p.ca/15+1)*15).coerceAtMost(99)}";Difficulty.SANDBOX->p.ca.toString()}}
}

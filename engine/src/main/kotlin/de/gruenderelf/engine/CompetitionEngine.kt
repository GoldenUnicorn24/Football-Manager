package de.gruenderelf.engine

/** Zusätzliche Wettbewerbe laufen auf derselben MatchEngine wie die Liga. */
object CompetitionEngine {
 private data class GuestSpec(val id: Int,val name: String,val city: String,val country: String,val reputation: Int,val primary: Long)
 private data class EuroEdge(val round: Int,val a: Int,val b: Int)
 private val guests=listOf(
  GuestSpec(1001,"CF Valmora","Valmora","Spanien",94,0xFF7B2637), GuestSpec(1002,"Sporting Solverde","Solverde","Spanien",89,0xFF1E6B53),
  GuestSpec(1003,"AC Bellaforte","Bellaforte","Italien",93,0xFF202735), GuestSpec(1004,"US Portorosso","Portorosso","Italien",88,0xFF9B3333),
  GuestSpec(1005,"Olympique Montclair","Montclair","Frankreich",92,0xFF274F87), GuestSpec(1006,"AS Valmer","Valmer","Frankreich",87,0xFF65A0C7),
  GuestSpec(1007,"Northbridge Athletic","Northbridge","England",95,0xFF6C2438), GuestSpec(1008,"Kingsport United","Kingsport","England",91,0xFF283D72),
  GuestSpec(1009,"Rotterdam Union","Rijnstad","Niederlande",88,0xFFDE7E2A), GuestSpec(1010,"SC Waterdam","Waterdam","Niederlande",85,0xFF2C6F7C),
  GuestSpec(1011,"Estrela Lisboa Nova","Lisboa Nova","Portugal",90,0xFFB73745), GuestSpec(1012,"Atlântico Porto Azul","Porto Azul","Portugal",86,0xFF31598A),
  GuestSpec(1013,"FK Zlatograd","Zlatograd","Kroatien",84,0xFF325B8E), GuestSpec(1014,"SK Dunavica","Dunavica","Serbien",83,0xFF7E2E45),
  GuestSpec(1015,"Prag Aurora","Aurora","Tschechien",85,0xFF642E78), GuestSpec(1016,"Danubia Wien","Danubia","Österreich",87,0xFFB93737),
  GuestSpec(1017,"Helvetia Nord","Helvetia","Schweiz",84,0xFFB13B3B), GuestSpec(1018,"Nordhavn BK","Nordhavn","Dänemark",82,0xFF315C77),
  GuestSpec(1019,"Stockholm Krona","Kronvik","Schweden",83,0xFF2F5E87), GuestSpec(1020,"Athletikos Asteri","Asteri","Griechenland",86,0xFF234A77)
 )
 private val fantasyNationalDays=listOf(1,4,7,10,14,17)
 private val fantasyNationalNames=listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale")
 private val realNationalDays=listOf(1,4,8,12,18,25,33)
 private val realNationalNames=listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale")
 private val fantasyEuroGroupDays=listOf(2,5,8,11,15,18)
 private val fantasyEuroKoDays=listOf(19,20,21,22)
 private val legacyEuroKoNames=listOf("Achtelfinale","Viertelfinale","Halbfinale","Finale")
 private val modernEuroLeagueDays=listOf(2,5,8,11,14,17,20,23)
 private val modernEuroPlayoffDays=24 to 26
 private val modernEuroR16Days=27 to 28
 private val modernEuroQuarterDays=29 to 30
 private val modernEuroSemiDays=31 to 32
 private const val modernEuroFinalDay=34
 private fun isReal(w: World)=w.privateTopClubMode
 private fun isModernEuropean(type: CompetitionType)=type==CompetitionType.CHAMPIONS_LEAGUE||type==CompetitionType.EUROPA_LEAGUE

 fun displayName(w: World,type: CompetitionType)=when{
  type==CompetitionType.NATIONAL_CUP&&isReal(w)->EuropeanLeagueData.cupsForCountry(clubCountry(w,w.user.clubId)).firstOrNull()?.name?:"Nationaler Pokal"
  type==CompetitionType.NATIONAL_CUP->"Gründerpokal"
  else->type.label
 }

 fun displayName(w: World,f: Fixture): String {
  if(f.competition!=CompetitionType.NATIONAL_CUP)return f.competition.label
  if(!isReal(w))return "Gründerpokal"
  return cupForFixture(f)?.name?:"DFB-Pokal"
 }

 fun domesticCupNames(w: World): List<String> {
  if(!isReal(w))return listOf("Gründerpokal")
  return w.fixtures.asSequence().filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP}
   .map{displayName(w,it)}.distinct().sorted().toList()
 }

 private fun clubCountry(w: World,clubId: Int): String {
  val league=w.leagues.firstOrNull{clubId in it.clubIds}?:return ""
  return RealModeDatabase.countryForLeague(league.name)?:""
 }
 private fun cupForFixture(f: Fixture): DomesticCupSeed?=
  EuropeanLeagueData.cupById(f.group)?:if(f.group.isBlank())EuropeanLeagueData.cupById("DFB_POKAL") else null
 private fun isCupFixture(f: Fixture,cup: DomesticCupSeed)=
  f.competition==CompetitionType.NATIONAL_CUP&&(f.group==cup.id||(cup.id=="DFB_POKAL"&&f.group.isBlank()))

 fun ensureGuestClubs(w: World,rng: SeededRandom=SeededRandom(w.seed xor 0x4555524FL)){
  if(isReal(w))return
  val used=w.clubs.values.map{it.shortName}.toMutableSet()
  for((index,g) in guests.withIndex()){
   if(g.id in w.clubs)continue
   val short=WorldFactory.deriveShortName(g.name,used);used.add(short)
   val c=Club(g.id,g.name,short,0,city="${g.city}, ${g.country}",founded=1880+(index*7)%125,primary=g.primary,secondary=0xFFF2F2EE,
    budget=25_000_000L,stadium=Stadium(name="Arena ${g.city}",capacity=26000+index*850,pitchQuality=92,surface=Surface.GRASS,floodlights=true,training=82+index%12,youth=68+index%15,medicine=78+index%14),
    reputation=g.reputation,members=12000+index*700,sponsor=Sponsor("Continental Partner",28000))
   c.logo=Logo(index%8,short);c.kits.home.primary=c.primary;w.clubs[c.id]=c
   val positions=Formations.positions("4-3-3")+listOf(Position.TW,Position.IV,Position.IV,Position.LV,Position.RV,Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA,Position.ST)
   for(pos in positions){
    val p=WorldFactory.generatePlayer(w.nextIds.player++,c.id,1,pos,rng,w.calendar.season)
    p.nationality=g.country;p.number=w.squad(c.id).size+1
    val boost=(g.reputation-82)/4
    p.attributes=p.attributes.copy(
     pace=(p.attributes.pace+boost).coerceAtMost(94),finishing=(p.attributes.finishing+boost).coerceAtMost(94),passing=(p.attributes.passing+boost).coerceAtMost(94),
     technique=(p.attributes.technique+boost).coerceAtMost(94),tackling=(p.attributes.tackling+boost).coerceAtMost(94),strength=(p.attributes.strength+boost).coerceAtMost(94),
     stamina=(p.attributes.stamina+boost).coerceAtMost(94),vision=(p.attributes.vision+boost).coerceAtMost(94),heading=(p.attributes.heading+boost).coerceAtMost(94),
     keeping=(p.attributes.keeping+if(pos==Position.TW)boost else 0).coerceAtMost(94),setPieces=(p.attributes.setPieces+boost).coerceAtMost(94))
    w.players[p.id]=p
   }
  }
 }

 private fun shuffle(values: List<Int>,rng: SeededRandom): MutableList<Int>{val a=values.toMutableList();for(i in a.lastIndex downTo 1){val j=rng.int(0,i);val t=a[i];a[i]=a[j];a[j]=t};return a}
 private fun addFixture(w: World,competition: CompetitionType,round: Int,stage: String,day: Int,home: Int,away: Int,group: String=""){
  w.fixtures.add(Fixture(w.nextIds.fixture++,w.calendar.season,0,day,home,away,competition=competition,round=round,stage=stage,group=group))
 }

 fun scheduleSeason(w: World){
  if(isReal(w)){
   EuropeanLeagueData.domesticCups.forEach{scheduleRealDomesticCup(w,it)}
   val order=realEuropeanQualificationOrder(w)
   require(order.size>=72){"Für Champions League und Europa League werden mindestens 72 qualifizierbare Topliga-Vereine benötigt."}
   scheduleModernEurope(w,CompetitionType.CHAMPIONS_LEAGUE,order.take(36),0x43484CL)
   scheduleModernEurope(w,CompetitionType.EUROPA_LEAGUE,order.drop(36).take(36),0x45554CL)
  }else{
   ensureGuestClubs(w);scheduleFantasyNationalFirstRound(w)
   val domestic=w.leagues.first{it.tier==1}.clubIds.sortedByDescending{w.clubs.getValue(it).reputation}
   val participants=(domestic+guests.map{it.id}).distinct().take(32)
   scheduleLegacyEuroGroups(w,CompetitionType.EURO_ELITE,participants,0x4555524FL)
  }
 }

 private fun cupSalt(cup: DomesticCupSeed): Long=cup.id.fold(0x435550L){acc,ch->acc*31L+ch.code}
 private fun cupCandidates(w: World,cup: DomesticCupSeed): List<Int> =
  w.leagues.filter{RealModeDatabase.countryForLeague(it.name)==cup.country&&RealModeDatabase.levelForTier(it.tier)<=cup.maxLevel}
   .sortedWith(compareBy<League>{RealModeDatabase.levelForTier(it.tier)}.thenBy{it.tier})
   .flatMap{it.clubIds}.distinct()

 private fun selectedCupClubs(w: World,cup: DomesticCupSeed): List<Int>{
  val candidates=cupCandidates(w,cup)
  if(candidates.isEmpty())return emptyList()
  val ordered=if(cup.id=="DFB_POKAL"){
   val direct=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)=="Deutschland"&&RealModeDatabase.levelForTier(it.tier)<=3}.flatMap{it.clubIds}.distinct()
   val lower=candidates.filter{it !in direct}.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
   (direct+lower).distinct()
  }else candidates.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
  if(cup.maxParticipants<=0||ordered.size<=cup.maxParticipants)return ordered
  val selected=ordered.take(cup.maxParticipants).toMutableList()
  if(w.user.clubId in candidates&&w.user.clubId !in selected)selected[selected.lastIndex]=w.user.clubId
  return selected.distinct()
 }

 private fun highestPowerOfTwoAtMost(value:Int): Int{
  var p=1
  while(p*2<=value)p*=2
  return p
 }
 private fun cupFirstDraw(w: World,cup: DomesticCupSeed): MutableList<Int> =
  shuffle(selectedCupClubs(w,cup),SeededRandom(w.seed xor w.calendar.season.toLong() xor cupSalt(cup)))
 private fun cupStageName(originalSize:Int,teams:Int,preliminary:Boolean=false): String=when{
  preliminary->"Vorrunde"
  teams>=64->"1. Runde"
  teams==32&&originalSize>=64->"2. Runde"
  teams==32->"Sechzehntelfinale"
  teams==16->"Achtelfinale"
  teams==8->"Viertelfinale"
  teams==4->"Halbfinale"
  teams==2->"Finale"
  else->"K.-o.-Runde"
 }

 private fun scheduleRealDomesticCup(w: World,cup: DomesticCupSeed){
  if(w.fixtures.any{it.season==w.calendar.season&&isCupFixture(it,cup)})return
  val selected=selectedCupClubs(w,cup)
  if(selected.size<2)return
  val power=highestPowerOfTwoAtMost(selected.size)
  val preliminary=power!=selected.size
  val firstMatchCount=if(preliminary)selected.size-power else selected.size/2
  val draw=cupFirstDraw(w,cup)
  val playing=draw.take(firstMatchCount*2)
  val stage=cupStageName(selected.size,selected.size,preliminary)
  playing.chunked(2).forEach{pair->
   addFixture(w,CompetitionType.NATIONAL_CUP,1,stage,realNationalDays[0],pair[0],pair[1],cup.id)
  }
 }

 private fun scheduleFantasyNationalFirstRound(w: World){
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP})return
  val domestic=w.leagues.flatMap{it.clubIds}.distinct();val target=64
  val priority=domestic.filter{it!=w.user.clubId}.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
  val selected=(listOf(w.user.clubId)+priority.take(target-1)).distinct().take(target)
  require(selected.size==target){"Pokal benötigt $target Vereine, gefunden: ${selected.size}"}
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor 0x435550L);val draw=shuffle(selected,rng)
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,1,fantasyNationalNames[0],fantasyNationalDays[0],pair[0],pair[1])}
 }

 private fun realEuropeanQualificationOrder(w: World): List<Int>{
  // Russische Klubs bleiben 2026/27 fuer UEFA-Wettbewerbe suspendiert; die nationale Liga/Pokalwelt laeuft normal.\n  val top=w.leagues.filter{RealModeDatabase.levelForTier(it.tier)==1&&RealModeDatabase.countryForLeague(it.name)!="Russland"}.sortedBy{it.tier}
  require(top.size>=5){"Europapokal benötigt die fünf Topligen."}
  val rotation=(w.calendar.season-top.first().tier).mod(top.size)
  val orderedLeagues=(top.drop(rotation)+top.take(rotation)).map{league->league.clubIds.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})}
  val result=mutableListOf<Int>();val max=orderedLeagues.maxOf{it.size}
  for(rank in 0 until max)for(league in orderedLeagues)if(rank<league.size)result.add(league[rank])
  return result.distinct()
 }

 private fun scheduleModernEurope(w: World,type: CompetitionType,participants: List<Int>,salt: Long){
  require(type==CompetitionType.CHAMPIONS_LEAGUE||type==CompetitionType.EUROPA_LEAGUE)
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type})return
  require(participants.size==36){"${type.label} benötigt 36 Vereine."}
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor salt)
  val rotation=shuffle(participants,rng)
  val edges=mutableListOf<EuroEdge>()
  repeat(8){round->
   for(i in 0 until rotation.size/2)edges.add(EuroEdge(round,rotation[i],rotation[rotation.lastIndex-i]))
   val last=rotation.removeAt(rotation.lastIndex);rotation.add(1,last)
  }
  // Die ersten acht Runden bilden einen 8-regulären Graphen. Eine Euler-Orientierung garantiert exakt vier Heim- und vier Auswärtsspiele pro Verein.
  val adjacency=participants.associateWith{mutableListOf<Int>()}.toMutableMap()
  edges.forEachIndexed{index,e->adjacency.getValue(e.a).add(index);adjacency.getValue(e.b).add(index)}
  val used=BooleanArray(edges.size);val homes=IntArray(edges.size);val aways=IntArray(edges.size)
  for(start in participants){
   if(adjacency.getValue(start).none{!used[it]})continue
   val stack=mutableListOf(start)
   while(stack.isNotEmpty()){
    val v=stack.last();val edgeIndex=adjacency.getValue(v).firstOrNull{!used[it]}
    if(edgeIndex==null)stack.removeAt(stack.lastIndex) else{
     used[edgeIndex]=true;val e=edges[edgeIndex];val next=if(e.a==v)e.b else e.a;homes[edgeIndex]=v;aways[edgeIndex]=next;stack.add(next)
    }
   }
  }
  edges.forEachIndexed{index,e->addFixture(w,type,e.round+1,"Ligaphase",modernEuroLeagueDays[e.round],homes[index],aways[index])}
 }

 private fun scheduleLegacyEuroGroups(w: World,type: CompetitionType,participants: List<Int>,salt: Long){
  require(type==CompetitionType.EURO_ELITE)
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type})return
  require(participants.size==32){"${type.label} benötigt 32 Vereine."}
  val ordered=participants.sortedByDescending{w.clubs.getValue(it).reputation};val pots=ordered.chunked(8)
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor salt);val shuffledPots=pots.map{shuffle(it,rng)}
  val groups=(0 until 8).associateWith{g->(0 until 4).map{pot->shuffledPots[pot][g]}}
  val pairRounds=listOf(listOf(0 to 3,1 to 2),listOf(3 to 2,0 to 1),listOf(1 to 3,2 to 0),listOf(3 to 0,2 to 1),listOf(2 to 3,1 to 0),listOf(3 to 1,0 to 2))
  for((gIndex,teams) in groups){val group=('A'.code+gIndex).toChar().toString();for(r in 0..5)for((a,b) in pairRounds[r])addFixture(w,type,r+1,"Ligaphase",fantasyEuroGroupDays[r],teams[a],teams[b],group)}
 }

 fun groupTable(w: World,group: String,competition: CompetitionType=CompetitionType.EURO_ELITE): List<TableRow>{
  val fixtures=w.fixtures.filter{it.season==w.calendar.season&&it.competition==competition&&it.stage=="Ligaphase"&&it.group==group}
  return tableFromFixtures(w,fixtures)
 }
 fun euroLeagueTable(w: World,competition: CompetitionType): List<TableRow>{
  require(isModernEuropean(competition))
  val fixtures=w.fixtures.filter{it.season==w.calendar.season&&it.competition==competition&&it.stage=="Ligaphase"}
  return tableFromFixtures(w,fixtures)
 }
 private fun tableFromFixtures(w: World,fixtures: List<Fixture>): List<TableRow>{
  val ids=fixtures.flatMap{listOf(it.homeId,it.awayId)}.distinct();val rows=ids.associateWith{TableRow(it)}
  for(f in fixtures.filter{it.played}){val m=w.matches[f.id]?:continue;val h=rows.getValue(f.homeId);val a=rows.getValue(f.awayId);h.played++;a.played++;h.goalsFor+=m.home.goals;h.goalsAgainst+=m.away.goals;a.goalsFor+=m.away.goals;a.goalsAgainst+=m.home.goals;when{m.home.goals>m.away.goals->{h.won++;a.lost++};m.home.goals<m.away.goals->{a.won++;h.lost++};else->{h.drawn++;a.drawn++}}}
  return rows.values.sortedWith(compareByDescending<TableRow>{it.points}.thenByDescending{it.difference}.thenByDescending{it.goalsFor}.thenBy{it.clubId})
 }

 private fun isFirstLeg(f: Fixture)=f.stage.endsWith("· Hinspiel")
 private fun isSecondLeg(f: Fixture)=f.stage.endsWith("· Rückspiel")
 private fun stageBase(f: Fixture)=f.stage.substringBefore(" · ")
 private fun previousLeg(w: World,f: Fixture): Fixture?=if(!isSecondLeg(f))null else w.fixtures.firstOrNull{it.season==f.season&&it.competition==f.competition&&it.round==f.round-1&&isFirstLeg(it)&&stageBase(it)==stageBase(f)&&setOf(it.homeId,it.awayId)==setOf(f.homeId,f.awayId)}
 private fun goalsForClub(f: Fixture,m: MatchRecord,clubId: Int)=if(f.homeId==clubId)m.home.goals else m.away.goals
 private fun aggregateScore(w: World,f: Fixture,currentHomeGoals: Int,currentAwayGoals: Int): Pair<Int,Int>{
  val first=previousLeg(w,f)?:return currentHomeGoals to currentAwayGoals;val firstRecord=w.matches[first.id]?:return currentHomeGoals to currentAwayGoals
  return (currentHomeGoals+goalsForClub(first,firstRecord,f.homeId)) to (currentAwayGoals+goalsForClub(first,firstRecord,f.awayId))
 }

 fun isDecisiveKnockoutFixture(w: World,f: Fixture): Boolean=when{
  f.competition==CompetitionType.NATIONAL_CUP->true
  f.competition==CompetitionType.EURO_ELITE->f.stage!="Ligaphase"
  isModernEuropean(f.competition)&&f.stage!="Ligaphase"->if(isReal(w))!isFirstLeg(f) else true
  else->false
 }
 fun isTiedForAdvancement(w: World,m: LiveMatch): Boolean{
  val f=w.fixtures.firstOrNull{it.id==m.fixtureId}?:return m.home.goals==m.away.goals
  val aggregate=aggregateScore(w,f,m.home.goals,m.away.goals);return aggregate.first==aggregate.second
 }

 private fun teamPenaltyQuality(w: World,clubId: Int): Double{val c=w.clubs.getValue(clubId);val ids=c.tactics.xi.filter{it!=0};return ids.mapNotNull{w.players[it]}.map{it.attributes.finishing*.45+it.attributes.setPieces*.25+it.attributes.technique*.20+it.hidden.pressure*.10}.average().takeIf{!it.isNaN()}?:55.0}
 private fun resolveKnockout(w: World,f: Fixture){
  if(f.winnerId!=0||isFirstLeg(f))return;val m=w.matches[f.id]?:return
  val score=aggregateScore(w,f,m.home.goals,m.away.goals)
  if(score.first!=score.second){f.winnerId=if(score.first>score.second)f.homeId else f.awayId;return}
  if(m.homePens!=m.awayPens&&(m.homePens>0||m.awayPens>0)){f.homePens=m.homePens;f.awayPens=m.awayPens;f.winnerId=if(m.homePens>m.awayPens)f.homeId else f.awayId;return}
  // Rückwärtskompatibilität für ältere gespeicherte Partien, die noch kein Live-Elfmeterschießen hatten.
  val rng=SeededRandom(w.seed xor f.id.toLong()*7919L xor w.calendar.season.toLong())
  val hq=teamPenaltyQuality(w,f.homeId);val aq=teamPenaltyQuality(w,f.awayId)
  var hp=3+rng.int(0,2);var ap=3+rng.int(0,2);if(hp==ap){if(rng.chance((.5+(hq-aq)/180.0).coerceIn(.32,.68)))hp++ else ap++}
  f.homePens=hp;f.awayPens=ap;f.winnerId=if(hp>ap)f.homeId else f.awayId
 }

 fun resultText(w: World,f: Fixture): String{
  val m=w.matches[f.id]?:return "gegen";val score="${m.home.goals}:${m.away.goals}"
  if(isSecondLeg(f)){
   val aggregate=aggregateScore(w,f,m.home.goals,m.away.goals);val extra=when{f.homePens>0||f.awayPens>0->" · ${f.homePens}:${f.awayPens} i.E.";m.extraTimePlayed->" n.V.";else->""}
   return "$score$extra · gesamt ${aggregate.first}:${aggregate.second}"
  }
  return when{f.homePens>0||f.awayPens>0->"$score (${f.homePens}:${f.awayPens} i.E.)";m.extraTimePlayed->"$score n.V.";else->score}
 }

 fun afterRecorded(w: World,f: Fixture){
  if(f.competition==CompetitionType.LEAGUE)return
  if(isDecisiveKnockoutFixture(w,f))resolveKnockout(w,f)
  when(f.competition){
   CompetitionType.NATIONAL_CUP->advanceNationalIfReady(w,f)
   CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE->if(isReal(w))advanceModernEuroIfReady(w,f,f.competition) else advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.EURO_ELITE->advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.LEAGUE->Unit
  }
  w.fixtures.sortWith(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.id})
 }

 private fun advanceNationalIfReady(w: World,last: Fixture){
  if(!isReal(w)){advanceFantasyNationalIfReady(w,last.round);return}
  val cup=cupForFixture(last)?:return
  advanceRealNationalIfReady(w,cup,last.round)
 }

 private fun advanceFantasyNationalIfReady(w: World,round: Int){
  if(round>=fantasyNationalNames.size||w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.round==round+1})return
  val current=w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.round==round}
  if(current.isEmpty()||current.any{!it.played})return
  val winners=current.map{it.winnerId}.filter{it!=0};if(winners.size!=current.size)return
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor (round*991L));val draw=shuffle(winners,rng)
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,round+1,fantasyNationalNames[round],fantasyNationalDays[round],pair[0],pair[1])}
 }

 private fun advanceRealNationalIfReady(w: World,cup: DomesticCupSeed,round: Int){
  if(w.fixtures.any{it.season==w.calendar.season&&isCupFixture(it,cup)&&it.round==round+1})return
  val current=w.fixtures.filter{it.season==w.calendar.season&&isCupFixture(it,cup)&&it.round==round}
  if(current.isEmpty()||current.any{!it.played})return
  val winners=current.map{it.winnerId}.filter{it!=0}
  if(winners.size!=current.size)return
  val selected=selectedCupClubs(w,cup)
  val originalSize=selected.size
  val power=highestPowerOfTwoAtMost(originalSize)
  val preliminary=power!=originalSize
  val byes=if(round==1&&preliminary){
   val draw=cupFirstDraw(w,cup)
   draw.drop((originalSize-power)*2)
  }else emptyList()
  val teams=(winners+byes).distinct()
  if(teams.size<2)return
  require(teams.size%2==0){"${cup.name}: ungerade Zahl an Teams in Runde ${round+1}: ${teams.size}"}
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor cupSalt(cup) xor (round*991L))
  val draw=shuffle(teams,rng)
  val stage=cupStageName(originalSize,teams.size,false)
  val day=realNationalDays.getOrElse(round){realNationalDays.last()+round-realNationalDays.lastIndex}
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,round+1,stage,day,pair[0],pair[1],cup.id)}
 }

 private fun scheduleTwoLegged(w: World,type: CompetitionType,phase: String,firstRound: Int,days: Pair<Int,Int>,pairings: List<Pair<Int,Int>>){
  pairings.forEach{(seeded,other)->
   addFixture(w,type,firstRound,"$phase · Hinspiel",days.first,other,seeded)
   addFixture(w,type,firstRound+1,"$phase · Rückspiel",days.second,seeded,other)
  }
 }
 private fun advanceModernEuroIfReady(w: World,last: Fixture,type: CompetitionType){
  if(last.stage=="Ligaphase"){
   if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round>=9})return
   val leaguePhase=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.stage=="Ligaphase"};if(leaguePhase.size!=144||leaguePhase.any{!it.played})return
   val table=euroLeagueTable(w,type);if(table.size!=36)return
   val pairings=(0 until 8).map{i->table[8+i].clubId to table[23-i].clubId}
   scheduleTwoLegged(w,type,"Play-offs",9,modernEuroPlayoffDays,pairings);return
  }
  if(isFirstLeg(last))return
  if(isSecondLeg(last)){
   val round=last.round;val current=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.round==round};if(current.isEmpty()||current.any{!it.played}||current.any{it.winnerId==0})return
   val winners=current.map{it.winnerId}
   when(round){
    10->{
     if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round>=11})return
     val top8=euroLeagueTable(w,type).take(8).map{it.clubId};val shuffled=shuffle(winners,SeededRandom(w.seed xor w.calendar.season.toLong() xor type.ordinal.toLong()*1301L))
     scheduleTwoLegged(w,type,"Achtelfinale",11,modernEuroR16Days,top8.zip(shuffled))
    }
    12->{if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round>=13})return;scheduleTwoLegged(w,type,"Viertelfinale",13,modernEuroQuarterDays,winners.chunked(2).map{it[0] to it[1]})}
    14->{if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round>=15})return;scheduleTwoLegged(w,type,"Halbfinale",15,modernEuroSemiDays,winners.chunked(2).map{it[0] to it[1]})}
    16->{if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round==17})return;require(winners.size==2);addFixture(w,type,17,"Finale",modernEuroFinalDay,winners[0],winners[1])}
   }
  }
 }

 private fun advanceLegacyEuroIfReady(w: World,last: Fixture,type: CompetitionType){
  if(last.stage=="Ligaphase"){
   if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round==7})return
   val groups=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.stage=="Ligaphase"};if(groups.any{!it.played})return
   val tables=('A'..'H').associateWith{groupTable(w,it.toString(),type)};val pairings=listOf('A' to 'H','B' to 'G','C' to 'F','D' to 'E','E' to 'D','F' to 'C','G' to 'B','H' to 'A')
   pairings.forEach{(winnerGroup,runnerGroup)->val home=tables.getValue(winnerGroup)[0].clubId;val away=tables.getValue(runnerGroup)[1].clubId;addFixture(w,type,7,legacyEuroKoNames[0],fantasyEuroKoDays[0],home,away)};return
  }
  val round=last.round;if(round !in 7..9||w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round==round+1})return
  val current=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.round==round};if(current.isEmpty()||current.any{!it.played})return
  val winners=current.map{it.winnerId}.filter{it!=0};if(winners.size!=current.size)return
  winners.chunked(2).forEach{pair->addFixture(w,type,round+1,legacyEuroKoNames[round-6],fantasyEuroKoDays[round-6],pair[0],pair[1])}
 }

 private fun hasLegacyRealCompetitionShape(w: World): Boolean{
  if(!isReal(w))return false
  val season=w.calendar.season
  val cupFirst=w.fixtures.count{it.season==season&&it.competition==CompetitionType.NATIONAL_CUP&&it.round==1}
  val oldEuro=listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE).any{type->
   val phase=w.fixtures.filter{it.season==season&&it.competition==type&&it.stage=="Ligaphase"};phase.isNotEmpty()&&(phase.any{it.group.isNotBlank()}||phase.flatMap{listOf(it.homeId,it.awayId)}.distinct().size!=36)
  }
  return cupFirst in 1..31||oldEuro
 }

 /** Alte Saves bekommen die neuen Wettbewerbe; ungespielte v0.4.62-Spielpläne werden auf das neue Format migriert. */
 fun ensureForLoadedWorld(w: World){
  if(!isReal(w))ensureGuestClubs(w)
  val seasonExtras=w.fixtures.filter{it.season==w.calendar.season&&it.competition!=CompetitionType.LEAGUE}
  if(isReal(w)&&seasonExtras.isNotEmpty()&&seasonExtras.none{it.played}&&hasLegacyRealCompetitionShape(w)){
   val ids=seasonExtras.filter{it.competition!=CompetitionType.NATIONAL_CUP||it.group.isBlank()}.map{it.id}.toSet()
   w.fixtures.removeAll{it.id in ids};ids.forEach{w.matches.remove(it)}
  }
  // Die Scheduler sind idempotent: neue Laender/Pokale werden auch in bestehenden Saves nachgetragen.
  scheduleSeason(w)
  while(true){
   val past=w.fixtures.filter{it.season==w.calendar.season&&it.competition!=CompetitionType.LEAGUE&&!it.played&&it.matchday<w.calendar.matchday}.sortedWith(compareBy<Fixture>{it.matchday}.thenBy{it.id})
   if(past.isEmpty())break
   val day=past.first().matchday
   for(f in w.fixtures.filter{it.season==w.calendar.season&&it.competition!=CompetitionType.LEAGUE&&!it.played&&it.matchday==day}.toList()){
    val m=MatchEngine.simulateFullMatch(w,f);storeWithoutCareerSideEffects(w,f,m);afterRecorded(w,f)
   }
  }
 }

 private fun storeWithoutCareerSideEffects(w: World,f: Fixture,m: LiveMatch){
  f.played=true;w.matches[f.id]=MatchRecord(f.id,m.homeId,m.awayId,m.home.copy(),m.away.copy(),m.minute,m.goals.toList(),m.attendance,m.shotEvents.toList(),m.homePens,m.awayPens,m.extraTimePlayed)
 }
}

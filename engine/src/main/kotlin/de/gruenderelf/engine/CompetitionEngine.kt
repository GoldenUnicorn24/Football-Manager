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
 private val realNationalDays=listOf(1,4,10,16,25,33)
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

 fun displayName(w: World,type: CompetitionType,group:String="")=when{
  type==CompetitionType.NATIONAL_CUP&&isReal(w)->cupName(w,group.ifBlank{countryOf(w,w.user.clubId)})
  type==CompetitionType.NATIONAL_CUP->"Gründerpokal"
  else->type.label
 }
 private val namedCups=mapOf("Deutschland" to "DFB-Pokal","England" to "FA Cup","Spanien" to "Copa del Rey","Italien" to "Coppa Italia","Frankreich" to "Coupe de France")
 private fun countryOf(w:World,clubId:Int)=w.leagues.firstOrNull{clubId in it.clubIds}?.let{RealModeDatabase.countryForLeague(it.name)}?:w.clubs[clubId]?.city.orEmpty()
 fun cupName(w:World,countryOrId:String)=if(!isReal(w))"Gründerpokal" else DomesticCompetitionData.byId(countryOrId)?.name?:namedCups[countryOrId]?:EuropeanLeagueData.cupFor(countryOrId)?:"Nationalpokal $countryOrId"
 fun cupGroup(w:World,f:Fixture)=if(!isReal(w))"Gründerpokal" else if(f.group.isNotBlank())f.group else DomesticCompetitionData.primaryForCountry(countryOf(w,f.homeId))?.id?:countryOf(w,f.homeId)
 fun cupCountry(group:String)=DomesticCompetitionData.byId(group)?.country?:group

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
   scheduleRealNationalCups(w)
   val order=realEuropeanQualificationOrder(w)
   require(order.size>=72){"Für die europäischen Wettbewerbe werden mindestens 72 qualifizierte Vereine benötigt."}
   scheduleModernEurope(w,CompetitionType.CHAMPIONS_LEAGUE,order.take(36),0x43484CL)
   scheduleModernEurope(w,CompetitionType.EUROPA_LEAGUE,order.drop(36).take(36),0x45554CL)
   scheduleClubWorldCup(w)
   if(w.fantasyCupEnabled)scheduleFantasyCrown(w)
  }else{
   ensureGuestClubs(w);scheduleFantasyNationalFirstRound(w)
   val domestic=w.leagues.first{it.tier==1}.clubIds.sortedByDescending{w.clubs.getValue(it).reputation}
   val participants=(domestic+guests.map{it.id}).distinct().take(32)
   scheduleLegacyEuroGroups(w,CompetitionType.EURO_ELITE,participants,0x4555524FL)
  }
 }

 private fun germanLeagueClubIds(w: World)=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)=="Deutschland"}.flatMap{it.clubIds}.distinct()
 private fun scheduleRealDfbPokal(w: World){
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP})return
  val german=germanLeagueClubIds(w)
  val direct=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)=="Deutschland"&&it.tier<=3}.flatMap{it.clubIds}.distinct()
  val lower=german.filter{it !in direct}.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
  val userGerman=w.user.clubId in german
  val selected=(direct+lower).distinct().take(64).toMutableList()
  if(userGerman&&w.user.clubId !in selected){selected[selected.lastIndex]=w.user.clubId}
  require(selected.size==64){"DFB-Pokal benötigt 64 deutsche Vereine, gefunden: ${selected.size}"}
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor 0x444642L);val draw=shuffle(selected.distinct(),rng)
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,1,realNationalNames[0],realNationalDays[0],pair[0],pair[1],DomesticCompetitionData.DFB_ID)}
 }
 private fun scheduleRealNationalCups(w:World){
  scheduleRealDfbPokal(w)
  for(spec in DomesticCompetitionData.cups.filter{it.country!="Deutschland"}){
   if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.group==spec.id})continue
   val clubs=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)==spec.country}.flatMap{it.clubIds}
   if(clubs.size<8)continue
   val size=minOf(spec.targetSize,Integer.highestOneBit(clubs.size))
   val selected=clubs.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it}).take(size).toMutableList()
   if(w.user.clubId in clubs&&w.user.clubId !in selected)selected[selected.lastIndex]=w.user.clubId
   val rounds=Integer.numberOfTrailingZeros(size)
   val offset=spec.roundDays.size-rounds+1
   val draw=shuffle(selected,SeededRandom(w.seed xor w.calendar.season.toLong() xor spec.id.hashCode().toLong()))
   draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,offset,spec.roundNames[offset-1],spec.roundDays[offset-1],pair[0],pair[1],spec.id)}
  }
  val countries=w.leagues.mapNotNull{RealModeDatabase.countryForLeague(it.name)}.distinct().filter{DomesticCompetitionData.forCountry(it).isEmpty()}
  for(country in countries){
   if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.group==country})continue
   val clubs=w.leagues.filter{RealModeDatabase.countryForLeague(it.name)==country}.flatMap{it.clubIds}
   val size=if(clubs.size>=32)32 else 16
   val ranked=clubs.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
   val selected=ranked.take(size).toMutableList()
   if(w.user.clubId in clubs&&w.user.clubId !in selected)selected[selected.lastIndex]=w.user.clubId
   val draw=shuffle(selected,SeededRandom(w.seed xor w.calendar.season.toLong() xor country.hashCode().toLong()))
   val offset=if(size==32)2 else 3
   draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,offset,realNationalNames[offset-1],realNationalDays[offset-1],pair[0],pair[1],country)}
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

 internal fun realEuropeanQualificationOrder(w: World): List<Int>{
  // The preceding season's actual league standings replace reputation after year one.
  // Russia remains playable domestically, while its clubs are suspended from UEFA.
  val top=w.leagues.filter{RealModeDatabase.levelForTier(it.tier)==1&&RealModeDatabase.countryForLeague(it.name)!="Russland"}.sortedBy{it.tier}
  val ranked=top.associateWith{league->
   val previous=w.lastLeagueRankings[league.tier].orEmpty().filter{it in league.clubIds}
   (previous+league.clubIds.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})).distinct()
  }
  // Der Meister jedes vertretenen Landes kommt zuerst. Die übrigen Plätze gehen
  // zunächst an die besser gesetzten Verbände und danach an weitere Tabellenplätze.
  val seededCountries=listOf("Deutschland","England","Spanien","Italien","Frankreich","Niederlande","Portugal","Belgien","Österreich","Türkei")
  val seeded=top.sortedWith(compareBy<League>{seededCountries.indexOf(RealModeDatabase.countryForLeague(it.name)).let{index->if(index<0)Int.MAX_VALUE else index}}.thenBy{it.tier})
  val candidates=mutableListOf<Int>()
  top.forEach{ranked.getValue(it).firstOrNull()?.let(candidates::add)}
  seeded.take(10).forEach{ranked.getValue(it).getOrNull(1)?.let(candidates::add)}
  seeded.take(5).forEach{ranked.getValue(it).getOrNull(2)?.let(candidates::add)}
  seeded.take(3).forEach{ranked.getValue(it).getOrNull(3)?.let(candidates::add)}
  for(rank in 1..3)for(league in seeded)ranked.getValue(league).getOrNull(rank)?.let(candidates::add)
  val champions=candidates.distinct().take(36)
  // Pokalsieger starten in der Europa League, sofern sie nicht bereits in der CL sind.
  val cups=top.mapNotNull{league->
   val country=RealModeDatabase.countryForLeague(league.name).orEmpty()
   w.trophies.lastOrNull{it.season==w.calendar.season-1&&it.competition==cupName(w,country)}?.clubId?.takeIf{it in w.clubs&&it !in champions}
  }
  val otherChampions=seeded.mapNotNull{ranked.getValue(it).firstOrNull()}.filter{it !in champions}
  val europa=(otherChampions+cups+seeded.flatMap{ranked.getValue(it)}).distinct().filter{it !in champions}.take(36)
  return champions+europa+(seeded.flatMap{ranked.getValue(it)}.filter{it !in champions&&it !in europa})
 }

 private val globalNames=listOf(
  "Flamengo" to "Brasilien","Palmeiras" to "Brasilien","Fluminense" to "Brasilien","Botafogo" to "Brasilien",
  "River Plate" to "Argentinien","Boca Juniors" to "Argentinien","Inter Miami" to "USA","Seattle Sounders" to "USA",
  "CF Monterrey" to "Mexiko","CF Pachuca" to "Mexiko","Al Ahly" to "Ägypten","Wydad AC" to "Marokko",
  "Mamelodi Sundowns" to "Südafrika","Al Hilal" to "Saudi-Arabien","Urawa Red Diamonds" to "Japan","Auckland City" to "Neuseeland")
 private fun globalClubs(w:World):List<Int>{
  val ids=mutableListOf<Int>();val rng=SeededRandom(w.seed xor 0x574F524C44L)
  for((index,entry) in globalNames.withIndex()){
   val id=5000+index;ids.add(id);if(id in w.clubs)continue
   val (name,country)=entry;val short=WorldFactory.deriveShortName(name,w.clubs.values.map{it.shortName}.toSet())
   val c=Club(id,name,short,0,city=country,primary=listOf(0xFFB32025,0xFF254872,0xFF27674A)[index%3],reputation=80+index%12,
    budget=30_000_000,stadium=Stadium(name="Stadion $name",capacity=36000,training=82,youth=77,medicine=80))
   w.clubs[id]=c
   val positions=Formations.positions("4-2-3-1")+listOf(Position.TW,Position.IV,Position.IV,Position.LV,Position.RV,Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA,Position.ST)
   positions.forEachIndexed{n,pos->val p=WorldFactory.generatePlayer(w.nextIds.player++,id,1,pos,rng,w.calendar.season);p.nationality=country;p.number=n+1;w.players[p.id]=p}
   WorldFactory.autoLineup(w,id)
  }
  return ids
 }
 private val worldDays=listOf(27,29,31,33,35,37,39)
 private val crownDays=listOf(1,3,5,7,9,11,13,15,17,19)
 private fun scheduleClubWorldCup(w:World){
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.CLUB_WORLD_CUP})return
  val europe=realEuropeanQualificationOrder(w).take(16)
  val global=globalClubs(w)
  val draw=shuffle(europe+global,SeededRandom(w.seed xor w.calendar.season.toLong() xor 0x434C5542L))
  val pairings=listOf(listOf(0 to 3,1 to 2),listOf(0 to 2,3 to 1),listOf(0 to 1,2 to 3))
  draw.chunked(4).forEachIndexed{index,teams->
   for(round in 0..2)for((a,b) in pairings[round])addFixture(w,CompetitionType.CLUB_WORLD_CUP,round+1,"Gruppenphase",worldDays[round],teams[a],teams[b],('A'.code+index).toChar().toString())
  }
 }
 fun worldCupGroupTable(w:World,group:String):List<TableRow> = tableFromFixtures(w,w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.CLUB_WORLD_CUP&&it.stage=="Gruppenphase"&&it.group==group})
 private fun advanceWorldCup(w:World,last:Fixture){
  if(last.stage=="Gruppenphase"){
   val groups=w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.CLUB_WORLD_CUP&&it.stage=="Gruppenphase"}
   if(groups.size!=48||groups.any{!it.played}||w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.CLUB_WORLD_CUP&&it.round==4})return
   val tops=('A'..'H').associateWith{worldCupGroupTable(w,it.toString()).take(2)}
   for(i in 0..7){val a=('A'.code+i).toChar();val b=('A'.code+(i xor 1)).toChar()
    addFixture(w,CompetitionType.CLUB_WORLD_CUP,4,"Achtelfinale",worldDays[3],tops.getValue(a)[0].clubId,tops.getValue(b)[1].clubId)
   }
  }else advanceOpenCup(w,CompetitionType.CLUB_WORLD_CUP,last.round)
 }
 private fun scheduleFantasyCrown(w:World){
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.ETERNAL_CROWN})return
  val qualified=w.leagues.flatMap{league->
   val previous=w.lastLeagueRankings[league.tier].orEmpty().filter{it in league.clubIds}
   (previous+league.clubIds.sortedByDescending{w.clubs.getValue(it).reputation}).distinct().take(6)
  }.distinct()
  if(qualified.size<2)return
  val target=Integer.highestOneBit(qualified.size)
  val playIn=qualified.size-target
  val draw=shuffle(qualified,SeededRandom(w.seed xor w.calendar.season.toLong() xor 0x4B524F4EL))
  w.fantasyCupByes=draw.drop(playIn*2).toMutableList()
  draw.take(playIn*2).chunked(2).forEach{addFixture(w,CompetitionType.ETERNAL_CROWN,1,"Vorrunde",crownDays[0],it[0],it[1])}
  if(playIn==0)w.fantasyCupByes.clear()
  if(playIn==0)draw.chunked(2).forEach{addFixture(w,CompetitionType.ETERNAL_CROWN,1,"1. Runde",crownDays[0],it[0],it[1])}
 }
 private fun advanceOpenCup(w:World,type:CompetitionType,round:Int){
  val fixtures=w.fixtures.filter{it.season==w.calendar.season&&it.competition==type&&it.round==round}
  if(fixtures.isEmpty()||fixtures.any{!it.played||it.winnerId==0}||w.fixtures.any{it.season==w.calendar.season&&it.competition==type&&it.round==round+1})return
  val winners=fixtures.map{it.winnerId}.toMutableList()
  if(type==CompetitionType.ETERNAL_CROWN&&round==1&&w.fantasyCupByes.isNotEmpty()){winners.addAll(w.fantasyCupByes);w.fantasyCupByes.clear()}
  if(winners.size<=1)return
  val draw=shuffle(winners,SeededRandom(w.seed xor w.calendar.season.toLong() xor (round*4099L) xor type.ordinal.toLong()))
  val days=if(type==CompetitionType.CLUB_WORLD_CUP)worldDays else crownDays
  require(round<days.size){"Turnierkalender ist zu kurz."}
  val stage=when(draw.size){2->"Finale";4->"Halbfinale";8->"Viertelfinale";16->"Achtelfinale";else->"K.-o.-Runde"}
  draw.chunked(2).forEach{addFixture(w,type,round+1,stage,days[round],it[0],it[1])}
 }

 private data class ModernEuroMatch(val home:Int,val away:Int)

 private fun associationForClub(w:World,clubId:Int):String {
  val league=w.leagues.firstOrNull{clubId in it.clubIds}
  return league?.let{RealModeDatabase.countryForLeague(it.name)}
   ?:w.clubs[clubId]?.city?.substringAfterLast(", ","")?.takeIf{it.isNotBlank()}
   ?:"International"
 }

 private fun scheduleModernEurope(w: World,type: CompetitionType,participants: List<Int>,salt: Long){
  require(type==CompetitionType.CHAMPIONS_LEAGUE||type==CompetitionType.EUROPA_LEAGUE)
  if(w.fixtures.any{it.season==w.calendar.season&&it.competition==type})return
  require(participants.size==36){"${type.label} benötigt 36 Vereine."}
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor salt)
  val ordered=participants.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})
  val pots=ordered.chunked(9)
  require(pots.size==4&&pots.all{it.size==9}){"Die Ligaphase benötigt vier Töpfe mit je neun Vereinen."}
  val associations=participants.associateWith{associationForClub(w,it)}

  fun buildDraw():List<ModernEuroMatch>?{
   val potOf=pots.flatMapIndexed{index,pot->pot.map{it to index}}.toMap()
   val opponents=participants.associateWith{mutableSetOf<Int>()}
   val assocCounts=participants.associateWith{mutableMapOf<String,Int>()}
   val undirected=mutableListOf<Pair<Int,Int>>()

   fun canPair(a:Int,b:Int):Boolean{
    if(a==b||b in opponents.getValue(a))return false
    val aa=associations.getValue(a);val ab=associations.getValue(b)
    if(aa==ab)return false
    if((assocCounts.getValue(a)[ab]?:0)>=2)return false
    if((assocCounts.getValue(b)[aa]?:0)>=2)return false
    return true
   }
   fun add(a:Int,b:Int){
    opponents.getValue(a)+=b;opponents.getValue(b)+=a
    val aa=associations.getValue(a);val ab=associations.getValue(b)
    assocCounts.getValue(a)[ab]=(assocCounts.getValue(a)[ab]?:0)+1
    assocCounts.getValue(b)[aa]=(assocCounts.getValue(b)[aa]?:0)+1
    undirected+=minOf(a,b) to maxOf(a,b)
   }
   fun remove(a:Int,b:Int){
    val key=minOf(a,b) to maxOf(a,b);undirected.remove(key)
    opponents.getValue(a)-=b;opponents.getValue(b)-=a
    val aa=associations.getValue(a);val ab=associations.getValue(b)
    val ca=(assocCounts.getValue(a)[ab]?:1)-1;val cb=(assocCounts.getValue(b)[aa]?:1)-1
    if(ca<=0)assocCounts.getValue(a).remove(ab) else assocCounts.getValue(a)[ab]=ca
    if(cb<=0)assocCounts.getValue(b).remove(aa) else assocCounts.getValue(b)[aa]=cb
   }

   // Zwischen zwei Töpfen werden zwei disjunkte perfekte Matchings gebaut.
   // Damit erhält jeder Verein exakt zwei Gegner aus jedem fremden Topf, ohne
   // den früheren 144-Kanten-Global-Backtracker zu benötigen.
   fun addCrossMatching(left:List<Int>,right:List<Int>):Boolean{
    val unmatchedLeft=left.toMutableSet();val unmatchedRight=right.toMutableSet();val chosen=mutableListOf<Pair<Int,Int>>()
    fun rec():Boolean{
     if(unmatchedLeft.isEmpty())return true
     val a=unmatchedLeft.minByOrNull{x->unmatchedRight.count{y->canPair(x,y)}}?:return false
     val options=shuffle(unmatchedRight.filter{canPair(a,it)},rng)
      .sortedWith(compareBy<Int>{y->(assocCounts.getValue(a)[associations.getValue(y)]?:0)+(assocCounts.getValue(y)[associations.getValue(a)]?:0)}.thenBy{it})
     for(b in options){
      unmatchedLeft-=a;unmatchedRight-=b;add(a,b);chosen+=a to b
      if(rec())return true
      chosen.removeAt(chosen.lastIndex);remove(a,b);unmatchedLeft+=a;unmatchedRight+=b
     }
     return false
    }
    if(rec())return true
    // rec() rollt alle gewählten Kanten zurück; defensive Sicherung für künftige Änderungen.
    chosen.asReversed().forEach{(a,b)->if(b in opponents.getValue(a))remove(a,b)}
    return false
   }

   // Innerhalb eines Topfes bildet ein 9er-Zyklus den Grad 2. Ein Zyklus ist
   // gleichzeitig ideal für die spätere Heim/Auswärts-Orientierung (1/1 je Topf).
   fun addSamePotCycle(pot:List<Int>):Boolean{
    val start=pot.minByOrNull{club->pot.count{other->other!=club&&canPair(club,other)}}?:return false
    val path=mutableListOf(start);val unused=(pot-start).toMutableSet()
    fun rec(current:Int):Boolean{
     if(unused.isEmpty()){
      if(!canPair(current,start))return false
      add(current,start);return true
     }
     val options=shuffle(unused.filter{canPair(current,it)},rng)
      .sortedWith(compareBy<Int>{candidate->unused.count{other->other!=candidate&&canPair(candidate,other)}}.thenBy{it})
     for(next in options){
      unused-=next;path+=next;add(current,next)
      if(rec(next))return true
      remove(current,next);path.removeAt(path.lastIndex);unused+=next
     }
     return false
    }
    if(rec(start))return true
    // Alle Rekursionskanten sind zurückgerollt; nur eine evtl. Schlusskante kann nicht übrig bleiben.
    return false
   }

   // Restriktive Eigen-Topf-Zyklen zuerst, danach fremde Topfpaare. Die beiden
   // Matchings je Paar sind klein (9x9) und können lokal mit MRV gelöst werden.
   for(pot in pots)if(!addSamePotCycle(pot))return null
   val pairs=mutableListOf<Pair<Int,Int>>()
   for(i in 0..3)for(j in i+1..3)pairs+=i to j
   for((i,j) in pairs){
    if(!addCrossMatching(pots[i],pots[j]))return null
    if(!addCrossMatching(pots[i],pots[j]))return null
   }
   if(undirected.size!=144||participants.any{opponents.getValue(it).size!=8})return null

   // Jede Topf-Paarung ist ein 2-regulärer Graph. Zyklen werden gerichtet, sodass
   // jeder Klub gegen jeden Topf exakt ein Heim- und ein Auswärtsspiel erhält.
   val result=mutableListOf<ModernEuroMatch>()
   fun key(a:Int,b:Int)=minOf(a,b) to maxOf(a,b)
   for(i in 0..3)for(j in i..3){
    val subset=undirected.filter{(a,b)->val pa=potOf.getValue(a);val pb=potOf.getValue(b);minOf(pa,pb)==i&&maxOf(pa,pb)==j}
    val adj=mutableMapOf<Int,MutableList<Int>>()
    subset.forEach{(a,b)->adj.getOrPut(a){mutableListOf()}.add(b);adj.getOrPut(b){mutableListOf()}.add(a)}
    if(adj.values.any{it.size!=2})return null
    val unused=subset.map{key(it.first,it.second)}.toMutableSet()
    while(unused.isNotEmpty()){
     val first=unused.first();val start=first.first;var current=start;var previous=-1;var guard=0
     do{
      if(guard++>40)return null
      val next=adj.getValue(current).firstOrNull{n->key(current,n) in unused&&n!=previous}
       ?:adj.getValue(current).firstOrNull{n->key(current,n) in unused}?:return null
      unused.remove(key(current,next));result+=ModernEuroMatch(current,next)
      previous=current;current=next
     }while(current!=start)
    }
   }
   val homes=participants.associateWith{id->result.count{it.home==id}}
   val aways=participants.associateWith{id->result.count{it.away==id}}
   if(result.size!=144||participants.any{homes.getValue(it)!=4||aways.getValue(it)!=4})return null
   return result
  }

  fun factorize(all:List<ModernEuroMatch>):List<List<ModernEuroMatch>>?{
   var remaining=all.toMutableList();val rounds=mutableListOf<List<ModernEuroMatch>>()
   repeat(8){
    val selected=mutableListOf<ModernEuroMatch>();val used=mutableSetOf<Int>()
    fun rec():Boolean{
     if(used.size==participants.size)return true
     val free=participants.filter{it !in used}
     val v=free.minByOrNull{x->remaining.count{e->(e.home==x&&e.away !in used)||(e.away==x&&e.home !in used)}}?:return false
     val choices=shuffle(remaining.indices.filter{idx->val e=remaining[idx];(e.home==v&&e.away !in used)||(e.away==v&&e.home !in used)},rng)
     for(index in choices){
      val e=remaining[index];val other=if(e.home==v)e.away else e.home;if(other in used)continue
      used+=v;used+=other;selected+=e
      if(rec())return true
      selected.removeAt(selected.lastIndex);used-=v;used-=other
     }
     return false
    }
    if(!rec())return null
    val keys=selected.map{minOf(it.home,it.away) to maxOf(it.home,it.away)}.toSet()
    remaining=remaining.filterNot{(minOf(it.home,it.away) to maxOf(it.home,it.away)) in keys}.toMutableList();rounds+=selected.toList()
   }
   return if(remaining.isEmpty())rounds else null
  }

  var rounds:List<List<ModernEuroMatch>>?=null
  repeat(96){if(rounds==null){val draw=buildDraw();if(draw!=null)rounds=factorize(draw)}}
  val finalRounds=rounds?:error("${type.label}: keine gültige Ligaphasen-Auslosung unter Topf-/Verbandsregeln gefunden.")
  finalRounds.forEachIndexed{round,matches->matches.forEach{match->addFixture(w,type,round+1,"Ligaphase",modernEuroLeagueDays[round],match.home,match.away)}}
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
  f.competition==CompetitionType.CLUB_WORLD_CUP->f.stage!="Gruppenphase"
  f.competition in listOf(CompetitionType.NATIONAL_CUP,CompetitionType.ETERNAL_CROWN)->true
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
  // Game-balance round bonuses, paid once per fixture. Final champion prizes follow at season end.
  val record=w.matches[f.id]
  val winner=when{
   f.winnerId!=0->f.winnerId
   record==null||record.home.goals==record.away.goals->0
   record.home.goals>record.away.goals->f.homeId
   else->f.awayId
  }
  val bonus=when(f.competition){
   CompetitionType.NATIONAL_CUP->(DomesticCompetitionData.byId(cupGroup(w,f))?.winnerPrize?:2_000_000L)/10
   CompetitionType.CHAMPIONS_LEAGUE->if(f.stage=="Ligaphase")1_000_000L else 2_000_000L
   CompetitionType.EUROPA_LEAGUE,CompetitionType.EURO_ELITE->if(f.stage=="Ligaphase")400_000L else 900_000L
   CompetitionType.CLUB_WORLD_CUP->2_000_000L
   CompetitionType.ETERNAL_CROWN->300_000L+(f.round-1)*200_000L
   CompetitionType.LEAGUE->0L
  }
  if(winner!=0)CompetitionPrizeSystem.award(w,"${f.season}:match:${f.id}",winner,bonus)
  when(f.competition){
   CompetitionType.NATIONAL_CUP->advanceNationalIfReady(w,f.round,cupGroup(w,f))
   CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE->if(isReal(w))advanceModernEuroIfReady(w,f,f.competition) else advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.EURO_ELITE->advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.CLUB_WORLD_CUP->advanceWorldCup(w,f)
   CompetitionType.ETERNAL_CROWN->advanceOpenCup(w,f.competition,f.round)
   CompetitionType.LEAGUE->Unit
  }
  w.fixtures.sortWith(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.id})
 }

 private fun advanceNationalIfReady(w: World,round: Int,group:String){
  val names=if(isReal(w))realNationalNames else fantasyNationalNames;val days=if(isReal(w))realNationalDays else fantasyNationalDays
  if(round>=names.size||w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&cupGroup(w,it)==group&&it.round==round+1})return
  val current=w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&cupGroup(w,it)==group&&it.round==round}
  if(current.isEmpty()||current.any{!it.played})return
  val winners=current.map{it.winnerId}.filter{it!=0};if(winners.size!=current.size)return
  if(winners.size==1)return
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor (round*991L));val draw=shuffle(winners,rng)
  val spec=DomesticCompetitionData.byId(group)
  val nextName=spec?.roundNames?.getOrNull(round)?:names[round]
  val nextDay=spec?.roundDays?.getOrNull(round)?:days[round]
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,round+1,nextName,nextDay,pair[0],pair[1],if(isReal(w))group else "")}
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
   val ids=seasonExtras.map{it.id}.toSet();w.fixtures.removeAll{it.id in ids};ids.forEach{w.matches.remove(it)};scheduleSeason(w)
  }else if(seasonExtras.isEmpty())scheduleSeason(w)
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

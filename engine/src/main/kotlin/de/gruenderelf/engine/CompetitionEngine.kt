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
  GuestSpec(1019,"Stockholm Krona","Kronvik","Schweden",83,0xFF2F5E87), GuestSpec(1020,"Athletikos Asteri","Asteri","Griechenland",86,0xFF234A77),
  GuestSpec(1021,"Royal Brüssel","Brüssel","Belgien",90,0xFF2D4C8C), GuestSpec(1022,"Istanbul Bosporus","Istanbul","Türkei",89,0xFF7A2435),
  GuestSpec(1023,"Caledonia Glasgow","Glasgow","Schottland",88,0xFF1F4F79), GuestSpec(1024,"Oslo Fjord FK","Oslo","Norwegen",87,0xFF294F67),
  GuestSpec(1025,"Warszawa Orzel","Warszawa","Polen",86,0xFFB22D34), GuestSpec(1026,"Kyiv Dnipro","Kyiv","Ukraine",85,0xFF315E9B),
  GuestSpec(1027,"Bucuresti Steaua Noua","Bucuresti","Rumänien",84,0xFF8B2F44), GuestSpec(1028,"Budapest Danubius","Budapest","Ungarn",83,0xFF315C4E),
  GuestSpec(1029,"Ljubljana Zmaj","Ljubljana","Slowenien",82,0xFF2D6A48), GuestSpec(1030,"Bratislava Dunaj","Bratislava","Slowakei",81,0xFF355B87),
  GuestSpec(1031,"Sofia Vitosha","Sofia","Bulgarien",80,0xFF3972A6), GuestSpec(1032,"Helsinki Aurora","Helsinki","Finnland",79,0xFF315C77)
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

 fun displayName(w: World,type: CompetitionType)=when{
  type==CompetitionType.NATIONAL_CUP&&isReal(w)->"DFB-Pokal"
  type==CompetitionType.NATIONAL_CUP->"Gründerpokal"
  else->type.label
 }

 fun ensureGuestClubs(w: World,rng: SeededRandom=SeededRandom(w.seed xor 0x4555524FL)){
  val used=w.clubs.values.map{it.shortName}.toMutableSet()
  // Fantasy mode intentionally keeps the original 20 guest clubs so existing worlds,
  // cup sizes and legacy Europa Elite structures remain stable. Real mode needs the
  // extended 32-association pool for mathematically valid modern UEFA league phases.
  val activeGuests=if(isReal(w))guests else guests.take(20)
  for((index,g) in activeGuests.withIndex()){
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
   // Der Real-Modus enthält fünf vollständig gepackte Topligen. Für die moderne
   // UEFA-Ligaphase reichen fünf Verbände mathematisch nicht aus, sobald acht
   // Gegner und maximal zwei Gegner je fremdem Verband gleichzeitig gelten.
   // Qualifikanten aus weiteren europäischen Verbänden bilden deshalb denselben
   // realistischen Pfad ab, den die echten Wettbewerbe über Meister-/Ligawege haben.
   ensureGuestClubs(w)
   scheduleRealDfbPokal(w)
   val order=realEuropeanQualificationOrder(w)
   require(order.size>=72){"Für Champions League und Europa League werden mindestens 72 Topliga-Vereine benötigt."}
   scheduleModernEurope(w,CompetitionType.CHAMPIONS_LEAGUE,order.take(36),0x43484CL)
   scheduleModernEurope(w,CompetitionType.EUROPA_LEAGUE,order.drop(36).take(36),0x45554CL)
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
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,1,realNationalNames[0],realNationalDays[0],pair[0],pair[1])}
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
  val top=w.leagues.filter{RealModeDatabase.levelForTier(it.tier)==1}.sortedBy{it.tier}
  require(top.size>=5){"Europapokal benötigt die fünf Topligen."}
  val rotation=(w.calendar.season-top.first().tier).mod(top.size)
  val orderedLeagues=(top.drop(rotation)+top.take(rotation)).map{league->league.clubIds.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it})}
  val domestic=mutableListOf<Int>();val max=orderedLeagues.maxOf{it.size}
  for(rank in 0 until max)for(league in orderedLeagues)if(rank<league.size)domestic.add(league[rank])

  // 16 Qualifikanten pro Wettbewerb verbreitern das Feld auf deutlich mehr
  // Verbände. Zusammen mit 20 Vereinen aus den fünf gepackten Topligen bleiben
  // die UEFA-Regeln (2 Gegner je Topf, keine Landsduelle, max. 2 je Verband)
  // auch bei ungünstiger Topfverteilung mathematisch erfüllbar.
  val qualifierSpecs=guests.filter{it.id in w.clubs}.sortedWith(compareByDescending<GuestSpec>{it.reputation}.thenBy{it.id})
  require(qualifierSpecs.size>=32){"Europapokal benötigt 32 Qualifikanten aus zusätzlichen Verbänden."}
  val championsQualifiers=qualifierSpecs.filterIndexed{i,_->i%2==0}.take(16).map{it.id}
  val europaQualifiers=qualifierSpecs.filterIndexed{i,_->i%2==1}.take(16).map{it.id}
  fun field(domesticOffset:Int,extra:List<Int>):List<Int>{
   val local=domestic.drop(domesticOffset).take(20)
   require(local.size==20&&extra.size==16){"Europapokal-Feld konnte nicht vollständig besetzt werden."}
   return (local+extra).distinct()
  }
  val champions=field(0,championsQualifiers);val europa=field(20,europaQualifiers)
  require(champions.size==36&&europa.size==36&&champions.intersect(europa.toSet()).isEmpty()){ "Europapokal-Felder müssen 36 unterschiedliche Vereine enthalten." }
  return champions+europa+domestic.drop(40)
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
   CompetitionType.NATIONAL_CUP->advanceNationalIfReady(w,f.round)
   CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE->if(isReal(w))advanceModernEuroIfReady(w,f,f.competition) else advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.EURO_ELITE->advanceLegacyEuroIfReady(w,f,f.competition)
   CompetitionType.LEAGUE->Unit
  }
  w.fixtures.sortWith(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.id})
 }

 private fun advanceNationalIfReady(w: World,round: Int){
  val names=if(isReal(w))realNationalNames else fantasyNationalNames;val days=if(isReal(w))realNationalDays else fantasyNationalDays
  if(round>=names.size||w.fixtures.any{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.round==round+1})return
  val current=w.fixtures.filter{it.season==w.calendar.season&&it.competition==CompetitionType.NATIONAL_CUP&&it.round==round}
  if(current.isEmpty()||current.any{!it.played})return
  val winners=current.map{it.winnerId}.filter{it!=0};if(winners.size!=current.size)return
  val rng=SeededRandom(w.seed xor w.calendar.season.toLong() xor (round*991L));val draw=shuffle(winners,rng)
  draw.chunked(2).forEach{pair->addFixture(w,CompetitionType.NATIONAL_CUP,round+1,names[round],days[round],pair[0],pair[1])}
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
  ensureGuestClubs(w)
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
  f.played=true;w.matches[f.id]=MatchRecord(fixtureId=f.id,homeId=m.homeId,awayId=m.awayId,home=m.home.copy(),away=m.away.copy(),minute=m.minute,goals=m.goals.toList(),attendance=m.attendance,homePens=m.homePens,awayPens=m.awayPens,extraTimePlayed=m.extraTimePlayed)
 }
}

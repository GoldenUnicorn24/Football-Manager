package de.gruenderelf.engine

object Formations {
 val all=linkedMapOf(
  "4-4-2" to listOf(Position.TW,Position.LV,Position.IV,Position.IV,Position.RV,Position.LA,Position.ZM,Position.ZM,Position.RA,Position.ST,Position.ST),
  "4-3-3" to listOf(Position.TW,Position.LV,Position.IV,Position.IV,Position.RV,Position.DM,Position.ZM,Position.ZM,Position.LA,Position.ST,Position.RA),
  "4-2-3-1" to listOf(Position.TW,Position.LV,Position.IV,Position.IV,Position.RV,Position.DM,Position.DM,Position.LA,Position.OM,Position.RA,Position.ST),
  "5-3-2" to listOf(Position.TW,Position.LV,Position.IV,Position.IV,Position.IV,Position.RV,Position.ZM,Position.DM,Position.ZM,Position.ST,Position.ST),
  "3-5-2" to listOf(Position.TW,Position.IV,Position.IV,Position.IV,Position.LA,Position.ZM,Position.DM,Position.ZM,Position.RA,Position.ST,Position.ST))
 fun positions(formation: String)=all[formation]?:all.getValue("4-4-2")
 fun coordinates(formation: String): List<Pair<Float,Float>> {
  val rows=when(formation){"4-3-3"->listOf(1,4,3,3);"4-2-3-1"->listOf(1,4,2,3,1);"5-3-2"->listOf(1,5,3,2);"3-5-2"->listOf(1,3,5,2);else->listOf(1,4,4,2)}
  return rows.flatMapIndexed{r,n->(1..n).map{i->i.toFloat()/(n+1) to (.9f-r*.78f/(rows.size-1))}}
 }
}

object WorldFactory {
 val leagueNames=listOf("Bundesliga","2. Bundesliga","3. Liga","Regionalliga Nord","Oberliga Nord","Verbandsliga","Landesliga","Bezirksliga","Kreisliga","Kreisklasse")
 private val placeStems=listOf("Eichen","Falken","Sonnen","Wiesen","Birken","Rosen","Silber","Grün","Heide","Mühlen","Buchen","Linden","Kronen","Hohen","Auen","Wald","Stein","Berg","Flieder","Hafen")
 private val placeEnds=listOf("au","berg","feld","hagen","dorf","tal")
 private val firstNames=listOf(
  "Jan","Timo","Lukas","Nils","Mehmet","Paul","Jonas","Finn","Ben","Erik","Emre","Noah","Max","Mats","Sven","Luca","David","Felix","Ali","Tom","Ole","Malik",
  "Leon","Niklas","Moritz","Jannik","Tim","Dennis","Kevin","Daniel","Marvin","Julian","Fabian","Simon","Philipp","Sebastian","Robin","Pascal","Marco","Patrick","Florian","Tobias",
  "Yannick","Dominik","Mika","Lennard","Levin","Lasse","Marlon","Adrian","Anton","Samuel","Elias","Theo","Luis","Can","Kerem","Ozan","Deniz","Mert","Arda","Yusuf",
  "Leandro","Mateo","Rafael","Diego","Nico","Milan","Damian","Kilian","Joel","Jamie","Colin","Joris","Henrik","Hannes","Bennet","Tristan","Silas","Vincent","Aaron","Ruben"
 )
 private val lastNames=listOf(
  "Krüger","Neumann","Yilmaz","Schulz","Bauer","Richter","Wagner","Koch","Becker","Hansen","Fischer","Kaya","Wolf","Lorenz","Peters","Jansen","Schmidt","Hoffmann","Brandt","Seeger","Hinsch",
  "Meyer","Schneider","Weber","Schäfer","Klein","Zimmermann","Braun","Hartmann","Lange","Werner","Schmitz","Krause","Meier","Lehmann","Schmid","Schulze","Maier","Köhler","Herrmann","König",
  "Walter","Mayer","Huber","Kaiser","Fuchs","Pohl","Lang","Scholz","Möller","Keller","Vogel","Roth","Frank","Friedrich","Berger","Winkler","Albrecht","Graf","Haase","Simon",
  "Demir","Aydin","Öztürk","Celik","Arslan","Kilic","Dogan","Sahin","Acar","Kaplan","Aksoy","Erdem","Aslan","Güneş","Tekin","Korkmaz","Yildiz","Karaca","Bulut","Koc",
  "Nowak","Kowalski","Lewandowski","Zielinski","Mazur","Duda","Kovac","Horvat","Novak","Petrovic","Jovanovic","Markovic","Ilic","Popovic","Nikolic","Savic","Matic","Pavlovic","Vukovic","Stojanovic",
  "de Vries","van Dijk","Smit","Jansen","Bakker","Visser","Bos","Meijer","Vos","Mulder","Martens","Peeters","Jacobs","Willems","Vermeulen","Claes","Dubois","Lambert","Martin","Moreau"
 )
 private val legacyFirstNames=setOf("Jan","Timo","Lukas","Nils","Mehmet","Paul","Jonas","Finn","Ben","Erik","Emre","Noah","Max","Mats","Sven","Luca","David","Felix","Ali","Tom","Ole","Malik")
 private val legacyLastNames=setOf("Krüger","Neumann","Yilmaz","Schulz","Bauer","Richter","Wagner","Koch","Becker","Hansen","Fischer","Kaya","Wolf","Lorenz","Peters","Jansen","Schmidt","Hoffmann","Brandt","Seeger","Hinsch")
 private fun place(index: Int)=placeStems[(index/6)%placeStems.size]+placeEnds[index%6]
 fun leagueName(w: World,tier: Int)=w.leagues.firstOrNull{it.tier==tier}?.name?:leagueNames.getOrElse((tier-1).coerceAtLeast(0)){"Liga"}
 fun leagueLevel(w: World,tier: Int)=if(w.privateTopClubMode)RealModeDatabase.levelForTier(tier) else tier
 fun leagueCountry(w: World,tier: Int): String { val league=w.leagues.firstOrNull{it.tier==tier}?:return "Deutschland";return if(w.privateTopClubMode)RealModeDatabase.countryForLeague(league.name)?:"Deutschland" else "Deutschland" }
 private fun lettersOnly(text: String)=text.uppercase().filter{it.isLetter()}
 fun deriveShortName(name: String,used: Set<String> = emptySet()): String {
  val parts=name.trim().split(Regex("\\s+")).filter{it.isNotBlank()};if(parts.isEmpty())return "CLB"
  val prefix=parts.first();val locality=parts.drop(1).joinToString("").ifBlank{prefix}
  val prefixCode=when(prefix.uppercase()){ "FC"->"FC";"SV"->"SV";"TSV"->"TS";"SPVG"->"SP";"SC"->"SC";"VFL"->"VL";else->lettersOnly(prefix).take(2).padEnd(2,'X') }
  val city=lettersOnly(locality).ifBlank{"CLUB"}
  val pairs=listOf(
   "${city.first()}${city.last()}",
   "${city.first()}${city.getOrElse(1){city.last()}}",
   "${city.first()}${city.getOrElse(2){city.last()}}",
   "${city.getOrElse(1){city.first()}}${city.last()}",
   city.take(2).padEnd(2,'X')
  ).distinct()
  pairs.map{(prefixCode+it).take(4)}.firstOrNull{it !in used}?.let{return it}
  val fallback=(lettersOnly(parts.joinToString(""))).take(4).padEnd(3,'X')
  return if(fallback !in used)fallback else (prefixCode+city.takeLast(2)).take(4)
 }
 fun migrateGeneratedIdentity(w: World){
  val needsMigration=w.clubs.values.any{it.id!=w.user.clubId&&(it.shortName.matches(Regex("^[SFTV][0-9][A-L]$"))||it.shortName.any(Char::isDigit))}
  if(!needsMigration)return
  val used=mutableSetOf(w.club().shortName.uppercase())
  w.clubs.values.filter{it.id!=w.user.clubId}.sortedBy{it.id}.forEach{c->
   val legacy=c.shortName.matches(Regex("^[SFTV][0-9][A-L]$"))||c.shortName.any{it.isDigit()}
   if(legacy){c.shortName=deriveShortName(c.name,used);c.logo.letters=c.shortName.take(4)}
   used.add(c.shortName.uppercase())
  }
  // Alte generierte Gegner stammen aus einem sehr kleinen Namenspool. Diese Umverteilung
  // läuft zusammen mit der Kürzelmigration genau einmal und bleibt danach speicherstabil.
  val rng=SeededRandom(w.seed xor 0x4E414D4553L)
  w.players.values.filter{it.clubId!=0&&it.clubId!=w.user.clubId&&it.firstName in legacyFirstNames&&it.lastName in legacyLastNames}.sortedBy{it.id}.forEach{p->
   if(!rng.chance(.14))p.lastName=rng.pick(lastNames)
   if(!rng.chance(.28))p.firstName=rng.pick(firstNames)
  }
 }
 fun startingBudget(draft: ClubDraft)=((12500-(draft.capacity-250)*12)*draft.difficulty.money).toLong()
 private fun initializeManagerSystems(w:World){w.clubs.values.forEach{YouthEngine.configureClub(it);ClubSystems.clamp(it)};EconomySystem.initialize(w)}

 fun createWorld(seed: Long,draft: ClubDraft=ClubDraft(city="Musterstadt"),person: PlayerDraft=PlayerDraft(firstName="Alex",lastName="Muster"),sandboxPlayers: List<PlayerDraft> = emptyList()): World {
  require(draft.name.trim().length in 3..40){"Vereinsname: 3 bis 40 Zeichen."}
  require(draft.shortName.trim().length in 2..4){"Kürzel: 2 bis 4 Zeichen."}
  require(draft.capacity in 100..500){"Startkapazität: 100 bis 500."}
  require(draft.founded in 1850..2026&&draft.city.isNotBlank()){ "Ort oder Gründungsjahr ist ungültig." }
  validateDraft(person,"Dein Spieler")
  if(draft.difficulty==Difficulty.SANDBOX)require(sandboxPlayers.size<=19){"Im Sandbox-Modus sind höchstens 20 selbst erstellte Spieler inklusive dir möglich."}

  val w=World(seed=seed,user=User(120,2401,draft.difficulty));val rng=SeededRandom(seed)
  for(tier in 1..10){
   val league=League(tier,leagueNames[tier-1]);w.leagues.add(league)
   repeat(12){i->
    val id=(tier-1)*12+i+1;val pro=tier<=6;val town=place(id-1);val prefix=listOf("SV","TSV","FC","SpVg","SC","VfL")[id%6]
    val name="$prefix $town";val short=deriveShortName(name,w.clubs.values.map{it.shortName}.toSet())
    val c=Club(id,name,short,tier,city=town,founded=1890+tier*3+i,primary=listOf(0xFF305C49,0xFF365B7C,0xFF963F3C,0xFFBCBCB0,0xFF6E4A7D,0xFF8A6D32)[i%6],budget=if(pro)(11-tier)*850000L else 15000L,stadium=Stadium(name="Sportpark $town",capacity=if(pro)(11-tier)*3000 else 250,pitchQuality=if(pro)85 else rng.int(25,50),surface=if(pro)Surface.GRASS else Surface.HARD,floodlights=pro,training=(11-tier)*7,youth=(11-tier)*5,medicine=(11-tier)*4),reputation=(11-tier)*9,members=if(pro)(11-tier)*1000 else 60,sponsor=Sponsor(if(pro)"Nordwerke" else "Bäckerei $town",if(pro)(11-tier)*5500 else 210))
    c.logo=Logo(i%8,c.shortName);c.kits.home.primary=c.primary;w.clubs[id]=c;league.clubIds.add(id)
    val positions=Formations.positions("4-4-2")+listOf(Position.TW,Position.IV,Position.LV,Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA,Position.ST)
    for(pos in positions){val p=generatePlayer(w.nextIds.player++,id,tier,pos,rng,2026);p.number=w.squad(id).size+1;w.players[p.id]=p}
   }
  }

  val c=w.club();c.name=draft.name.trim();c.shortName=draft.shortName.trim().uppercase();c.founded=draft.founded;c.city=draft.city.trim();c.primary=draft.primary;c.secondary=draft.secondary
  c.logo=draft.logo.copy(letters=draft.logo.letters.take(4));c.kits=draft.kits;c.stadium=Stadium(name=draft.stadiumName.trim().ifEmpty{"Sportplatz am Waldrand"},capacity=draft.capacity)
  c.budget=startingBudget(draft);c.philosophy=draft.philosophy;c.playPhilosophy=draft.playPhilosophy;c.youthPhilosophy=draft.youthPhilosophy;c.reputation=10;c.members=55
  c.tactics.buildUp=if(draft.playPhilosophy=="Ballbesitz")BuildUp.SHORT else BuildUp.DIRECT
  if(draft.playPhilosophy=="Kampf und Ordnung"){c.tactics.pressing=4;c.tactics.mentality=2}
  if(draft.philosophy=="Offener Verein")c.members+=10

  if(draft.difficulty==Difficulty.SANDBOX){
   w.players.values.filter{it.clubId==c.id}.map{it.id}.forEach{w.players.remove(it)}
  }

  val p=generatePlayer(w.nextIds.player++,120,10,person.position,rng,2026);check(p.id==w.user.playerId)
  applyDraft(p,person,draft.difficulty==Difficulty.SANDBOX,0)
  p.hidden.potential=if(draft.difficulty==Difficulty.SANDBOX)(p.ca+12).coerceAtMost(99) else 88
  w.squad().filter{it.number==p.number}.forEach{it.number=22};w.players[p.id]=p

  if(draft.difficulty==Difficulty.SANDBOX){
   val used=mutableSetOf(p.number)
   sandboxPlayers.take(19).forEachIndexed{index,d->
    val extra=generatePlayer(w.nextIds.player++,120,10,d.position,rng,2026)
    applyDraft(extra,d,true,index+1)
    if(extra.number in used)extra.number=(1..99).firstOrNull{it !in used}?:extra.number
    used.add(extra.number);extra.hidden.potential=(extra.ca+12).coerceAtMost(99);w.players[extra.id]=extra
   }
   while(w.squad().size<20){val extra=generatePlayer(w.nextIds.player++,120,10,rng.pick(Position.entries),rng,2026);extra.number=(1..99).firstOrNull{n->w.squad().none{it.number==n}}?:99;w.players[extra.id]=extra}
  }else{
   when(person.archetype){"Schnell"->p.attributes.pace=57;"Technisch"->p.attributes.technique=57;"Physisch"->{p.attributes.strength=57;p.attributes.heading=48};"Abschluss"->p.attributes.finishing=57;else->{p.attributes.passing+=5;p.attributes.vision+=5;p.attributes.stamina+=5}}
  }

  w.squad().forEach{if(draft.philosophy=="Zusammenhalt")it.morale+=5;if(draft.philosophy=="Leistung")it.hidden.professionalism=(it.hidden.professionalism+5).coerceAtMost(99)}
  repeat(24){val free=generatePlayer(w.nextIds.player++,0,rng.int(8,10),rng.pick(Position.entries),rng,2026);w.players[free.id]=free}
  repeat(4){spawnYouth(w,c,rng)}
  CompetitionEngine.ensureGuestClubs(w,rng)
  w.clubs.values.forEach{autoLineup(w,it.id);it.wageBill=w.squad(it.id).sumOf{pl->pl.wage}}
  val slot=Formations.positions(c.tactics.formation).indexOf(person.position).takeIf{it>=0}?:9
  if(p.id !in c.tactics.xi){c.tactics.xi[slot]=p.id;rebuildBench(w,c)}
  w.rivalries.add(Rivalry(120,119,45));w.leagues.filter{it.tier!=10}.forEach{w.rivalries.add(Rivalry(it.clubIds[0],it.clubIds[1]))}
  w.rngState=rng.state;makeSchedule(w)
  w.news("Ein Verein entsteht","${c.name} startet in der ${leagueName(w,10)}. Dein erster Rivale: ${w.clubs.getValue(119).name}. Der Platzwart hat die Linien gezogen.","good")
  if(draft.difficulty==Difficulty.SANDBOX)w.news("Sandbox aktiv","Dein Kader wurde mit deinen individuellen Spielerwerten angelegt. Budget- und Attributgrenzen sind bewusst großzügig.","good")
  initializeManagerSystems(w)
  return w
 }


 private fun realSeedAttributes(r: Int,pos: Position): Attributes {
  fun v(delta:Int)=(r+delta).coerceIn(1,96)
  return when(pos){
   Position.TW->Attributes(v(-25),v(-38),v(-8),v(-10),v(-10),v(1),v(2),v(-4),v(-15),v(4),v(-10))
   Position.IV->Attributes(v(-5),v(-18),v(-5),v(-6),v(5),v(4),v(2),v(-3),v(4),12,v(-10))
   Position.LV,Position.RV->Attributes(v(3),v(-12),v(1),v(1),v(2),v(-1),v(4),v(-1),v(-4),11,v(-7))
   Position.DM->Attributes(v(-1),v(-10),v(4),v(1),v(3),v(2),v(3),v(3),v(-4),10,v(-4))
   Position.ZM->Attributes(v(0),v(-6),v(5),v(4),v(-2),v(-2),v(3),v(5),v(-5),10,v(0))
   Position.OM->Attributes(v(2),v(2),v(4),v(5),v(-11),v(-3),v(1),v(5),v(-4),9,v(3))
   Position.LA,Position.RA->Attributes(v(5),v(1),v(1),v(5),v(-14),v(-4),v(2),v(2),v(-6),8,v(1))
   Position.ST->Attributes(v(2),v(6),v(-4),v(2),v(-16),v(3),v(0),v(-2),v(3),8,v(0))
  }
 }

 private fun realBaseRating(level:Int)=when(level){1->74;2->68;3->62;4->55;else->49}

 fun createRealModeWorld(seed: Long,clubKey: String,person: PlayerDraft): World {
  validateDraft(person,"Dein Spieler")
  val selected=RealModeDatabase.requireClub(clubKey)
  val selectedLeague=RealModeDatabase.leagueForClub(clubKey)
  val rng=SeededRandom(seed)
  val w=World(seed=seed,user=User(0,0,Difficulty.SANDBOX),privateTopClubMode=true)
  var nextClubId=1
  for(leagueSeed in RealModeDatabase.leagues){
   val league=League(leagueSeed.tier,leagueSeed.name);w.leagues.add(league)
   for((clubIndex,clubSeed) in leagueSeed.clubs.withIndex()){
    val id=nextClubId++;league.clubIds.add(id)
    val short=clubSeed.shortName.uppercase().take(4).ifBlank{deriveShortName(clubSeed.name)}
    val fallbackRating=realBaseRating(leagueSeed.level)
    val avg=clubSeed.players.map{it.rating}.average().takeIf{!it.isNaN()}?:fallbackRating.toDouble()
    val reputation=(avg+8).toInt().coerceIn(42,96)
    val c=Club(id,clubSeed.name,short,leagueSeed.tier,city=clubSeed.country,founded=0,primary=clubSeed.primary,secondary=clubSeed.secondary,
     logo=Logo(clubIndex%8,short),stadium=Stadium(name="Heimstadion",capacity=(when(leagueSeed.level){1->28000;2->18000;3->10000;4->4500;else->2200})+(reputation-55).coerceAtLeast(0)*350,pitchQuality=(94-(leagueSeed.level-1)*5).coerceAtLeast(70),surface=Surface.GRASS,floodlights=leagueSeed.level<=4,training=(86-(leagueSeed.level-1)*9).coerceAtLeast(45),youth=(80-(leagueSeed.level-1)*8).coerceAtLeast(40),medicine=(84-(leagueSeed.level-1)*8).coerceAtLeast(40)),
     budget=(when(leagueSeed.level){1->20_000_000L;2->8_000_000L;3->2_500_000L;4->650_000L;else->220_000L})+(reputation-55).coerceAtLeast(0)*180_000L,reputation=reputation,members=(when(leagueSeed.level){1->18000;2->9000;3->4500;4->1800;else->700})+(reputation-55).coerceAtLeast(0)*250,sponsor=Sponsor("Hauptpartner",when(leagueSeed.level){1->42000;2->22000;3->9000;4->3200;else->1200}))
    c.kits.home.primary=clubSeed.primary;c.kits.home.secondary=clubSeed.secondary;c.kits.away.primary=clubSeed.secondary;c.kits.away.secondary=clubSeed.primary
    w.clubs[id]=c
    if(clubSeed.players.isNotEmpty()){
     clubSeed.players.forEach{ps->
      val parts=ps.name.trim().split(Regex("\\s+")).filter{it.isNotBlank()};val last=parts.lastOrNull()?:ps.name;val first=parts.dropLast(1).joinToString(" ").ifBlank{"Spieler"}
      val r=ps.rating.coerceIn(45,94)
      val p=Player(w.nextIds.player++,id,first,last,ps.born,nationality=ps.nationality,position=ps.position,number=ps.number.coerceIn(1,99),attributes=realSeedAttributes(r,ps.position),
       hidden=Hidden((r+7).coerceAtMost(96),25,72,75,65,75,76,70),fitness=94.0,morale=70,form=6.6,sharpness=74,wage=(300+(6-leagueSeed.level).coerceAtLeast(1)*900+(reputation-50)*55).coerceAtLeast(120))
      p.secondary=p.secondaryOptions().take(3).toMutableList();w.players[p.id]=p
     }
    }else{
     val positions=Formations.positions("4-2-3-1")+listOf(Position.TW,Position.IV,Position.LV,Position.RV,Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA,Position.ST,Position.ST)
     positions.forEach{pos->val p=generatePlayer(w.nextIds.player++,id,leagueSeed.level,pos,rng,2026);p.number=(1..99).first{n->w.squad(id).none{it.number==n}};p.nationality=clubSeed.country;w.players[p.id]=p}
    }
   }
  }
  val selectedId=w.clubs.values.firstOrNull{it.name==selected.name&&w.leagues.first{l->l.tier==it.tier}.name==selected.league}?.id?:error("Real-Modus-Verein konnte nicht zugeordnet werden.")
  val userClub=w.clubs.getValue(selectedId)
  val me=generatePlayer(w.nextIds.player++,selectedId,selectedLeague.level,person.position,rng,2026)
  applyDraft(me,person,true,0);me.clubId=selectedId;me.hidden.potential=(me.ca+10).coerceAtMost(99);me.morale=85;me.sharpness=85
  w.squad(selectedId).filter{it.number==me.number}.forEach{other->other.number=(1..99).firstOrNull{n->w.squad(selectedId).none{it.number==n}&&n!=me.number}?:other.number}
  w.players[me.id]=me
  repeat(28){val free=generatePlayer(w.nextIds.player++,0,rng.int(2,5),rng.pick(Position.entries),rng,2026);w.players[free.id]=free}
  repeat(4){spawnYouth(w,userClub,rng)}
  w.clubs.values.forEach{autoLineup(w,it.id);it.wageBill=w.squad(it.id).sumOf{pl->pl.wage}}
  val preferredSlot=Formations.positions(userClub.tactics.formation).indexOf(person.position).takeIf{it>=0}?:9
  if(me.id !in userClub.tactics.xi){userClub.tactics.xi[preferredSlot]=me.id;rebuildBench(w,userClub)}
  w.rngState=rng.state
  val result=w.copy(user=User(selectedId,me.id,Difficulty.SANDBOX),privateTopClubMode=true)
  makeSchedule(result)
  result.news("Verein übernommen","Du übernimmst ${userClub.name} in der ${leagueName(result,userClub.tier)}. Dein eigener Spieler wurde zusätzlich in den Kader aufgenommen.","good")
  result.news("Europäische Ligawelt","Der deutsche Ligabaum bleibt vollständig enthalten. Dazu kommen weitere reale Ligen aus England, Spanien, Italien, Frankreich, Kroatien, Russland, den Niederlanden, Belgien, Österreich, Polen, Schweden, Portugal, der Türkei, Schweiz, Schottland, Griechenland, Tschechien, Dänemark, Serbien, der Ukraine und Norwegen.","normal")
  result.news("Datenhinweis","Vorhandene Topligen nutzen den 2026/27-Kaderdatenstand. Neu ergänzte Ligen verwenden reale Vereine; wo noch kein vollständiger Kader hinterlegt ist, werden Spieler passend zu Land und Liganiveau erzeugt.","normal")
  initializeManagerSystems(result)
  return result
 }

 fun createCustomClubWorld(seed:Long,slotClubKey:String,draft:ClubDraft,person:PlayerDraft,players:List<PlayerDraft>):World{
  require(draft.name.trim().length in 3..40){"Vereinsname: 3 bis 40 Zeichen."};require(draft.shortName.trim().length in 2..4){"Kürzel: 2 bis 4 Zeichen."};require(draft.founded in 1850..2026&&draft.city.isNotBlank()){ "Ort oder Gründungsjahr ist ungültig." };require(draft.capacity in 100..100000){"Stadionkapazität ungültig."};validateDraft(person,"Dein Spieler");require(players.size<=19){"Maximal 19 zusätzliche selbst erstellte Spieler."}
  val league=RealModeDatabase.leagueForClub(slotClubKey);val w=createRealModeWorld(seed,slotClubKey,person);val c=w.club();val rng=SeededRandom(w.rngState);val selfId=w.user.playerId
  w.players.values.filter{it.clubId==c.id&&it.id!=selfId}.map{it.id}.forEach{w.players.remove(it)}
  c.name=draft.name.trim();c.shortName=draft.shortName.trim().uppercase();c.city=draft.city.trim();c.founded=draft.founded;c.primary=draft.primary;c.secondary=draft.secondary;c.logo=draft.logo.copy(letters=c.shortName);c.kits=draft.kits;c.stadium=Stadium(name=draft.stadiumName.trim().ifEmpty{"Stadion ${c.name}"},capacity=draft.capacity,pitchQuality=(92-(league.level-1)*5).coerceAtLeast(68),surface=Surface.GRASS,floodlights=league.level<=4,training=(84-(league.level-1)*8).coerceAtLeast(42),youth=(78-(league.level-1)*8).coerceAtLeast(38),medicine=(80-(league.level-1)*7).coerceAtLeast(40));c.philosophy=draft.philosophy;c.playPhilosophy=draft.playPhilosophy;c.youthPhilosophy=draft.youthPhilosophy;c.reputation=(88-(league.level-1)*10).coerceAtLeast(42);c.budget=((when(league.level){1->28_000_000L;2->10_000_000L;3->3_000_000L;4->800_000L;else->280_000L})*draft.difficulty.money).toLong();c.members=when(league.level){1->18000;2->9000;3->4200;4->1600;else->650};w.user.difficulty=draft.difficulty
  val me=w.self();applyDraft(me,person,true,0);me.clubId=c.id;me.hidden.potential=(me.ca+12).coerceAtMost(99)
  val used=mutableSetOf(me.number)
  players.take(19).forEachIndexed{index,d->val p=generatePlayer(w.nextIds.player++,c.id,league.level,d.position,rng,2026);applyDraft(p,d,true,index+1);if(p.number in used)p.number=(1..99).firstOrNull{it !in used}?:p.number;used.add(p.number);p.hidden.potential=(p.ca+12).coerceAtMost(99);w.players[p.id]=p}
  while(w.squad(c.id).count{!it.youth}<20){val p=generatePlayer(w.nextIds.player++,c.id,league.level,rng.pick(Position.entries),rng,2026);p.number=(1..99).firstOrNull{n->w.squad(c.id).none{it.number==n}}?:99;w.players[p.id]=p}
  repeat(4){spawnYouth(w,c,rng)};autoLineup(w,c.id);c.wageBill=w.squad(c.id).sumOf{it.wage};w.rngState=rng.state;w.news.clear();w.news("Individueller Verein","${c.name} startet in der ${league.name} (${league.country}). Der komplette Kader wurde für diesen Spielstand individuell aufgebaut.","good");w.news("Liga-Platz","Für eine gerade und spielbare Ligagröße übernimmt dein Verein den Startplatz des zuvor ausgewählten Vereins. Alle übrigen Vereine bleiben erhalten.","normal");initializeManagerSystems(w);return w
 }

 /** Kompatibilitätsalias für ältere UI-Aufrufe und v0.4.57-Tests. */
 fun createTopClubWorld(seed: Long,clubKey: String,person: PlayerDraft)=createRealModeWorld(seed,clubKey,person)

 private fun validateDraft(p: PlayerDraft,label: String){
  require(p.firstName.isNotBlank()&&p.lastName.isNotBlank()&&p.birthYear in 1976..2008){"$label: Namen ausfüllen; Geburtsjahr 1976 bis 2008."}
  require(p.height in 150..215&&p.weight in 45..140&&p.number in 1..99){"$label: Größe, Gewicht oder Rückennummer ungültig."}
 }
 private fun applyDraft(p: Player,d: PlayerDraft,sandbox: Boolean,index: Int){
  validateDraft(d,if(index==0)"Dein Spieler" else "Sandbox-Spieler ${index+1}")
  p.firstName=d.firstName.trim();p.lastName=d.lastName.trim();p.birthYear=d.birthYear;p.nationality=d.nationality.trim().ifEmpty{"Deutschland"};p.height=d.height;p.weight=d.weight;p.foot=d.foot;p.position=d.position;p.secondary=d.secondary.filter{it!=d.position}.distinct().toMutableList();p.number=d.number;p.appearance=d.appearance;p.archetype=d.archetype
  if(sandbox)p.attributes=d.attributes.copy(pace=d.attributes.pace.coerceIn(1,99),finishing=d.attributes.finishing.coerceIn(1,99),passing=d.attributes.passing.coerceIn(1,99),technique=d.attributes.technique.coerceIn(1,99),tackling=d.attributes.tackling.coerceIn(1,99),strength=d.attributes.strength.coerceIn(1,99),stamina=d.attributes.stamina.coerceIn(1,99),vision=d.attributes.vision.coerceIn(1,99),heading=d.attributes.heading.coerceIn(1,99),keeping=d.attributes.keeping.coerceIn(1,99),setPieces=d.attributes.setPieces.coerceIn(1,99))
 }

 internal fun generatePlayer(id: Int,clubId: Int,tier: Int,pos: Position,rng: SeededRandom,year: Int): Player {
  val base=32+(10-tier)*5;fun a()=rng.int(base-8,base+8).coerceIn(1,95)
  val p=Player(id,clubId,rng.pick(firstNames),rng.pick(lastNames),year-rng.int(18,35),position=pos,attributes=Attributes(a(),a(),a(),a(),a(),a(),a(),a(),a(),if(pos==Position.TW)a()+8 else rng.int(5,18),a()),hidden=Hidden((base+rng.int(10,30)).coerceAtMost(98),rng.int(10,65),rng.int(40,90),rng.int(35,90),rng.int(30,95),rng.int(30,90),rng.int(40,90),rng.int(30,85)),fitness=rng.int(86,100).toDouble(),wage=if(tier>=7)rng.int(0,9) else (11-tier)*rng.int(150,400))
  p.secondary=p.secondaryOptions().take(2).toMutableList();p.agentId=(id%97)+1;p.marketUncertainty=rng.int(8,35);p.contractYears=rng.int(1,4);p.promisedRole=if(p.ca>=72)SquadRole.STARTER else SquadRole.ROTATION;p.homegrownClubId=clubId
  return p
 }
 internal fun spawnYouth(w: World,c: Club,rng: SeededRandom){
  val leagueLevel=if(w.privateTopClubMode)RealModeDatabase.levelForTier(c.tier) else c.tier
  val p=generatePlayer(w.nextIds.player++,c.id,leagueLevel,rng.pick(Position.entries),rng,w.calendar.season)
  p.birthYear=w.calendar.season-rng.int(16,18);p.youth=true;p.wage=0
  p.hidden.potential=(38+c.stadium.youth/2+rng.int(0,30)+(if(c.youthPhilosophy=="Leistungsförderung")4 else 0)).coerceAtMost(99)
  val golden=if(c.academy.goldenGeneration)6 else 0;val level=(20+c.stadium.youth/3+rng.int(0,9)+golden).coerceAtMost(70);p.attributes=Attributes(level,level,level,level,level,level,level,level,level,if(p.position==Position.TW)level+6 else 10,level);p.secondary=p.secondaryOptions().take(2).toMutableList();YouthEngine.seedProfile(w,c,p,rng);p.role=p.youthProfile.roleSpark;p.homegrownClubId=c.id;w.players[p.id]=p
 }
 fun autoLineup(w: World,clubId: Int=w.user.clubId){
  val c=w.clubs.getValue(clubId);val available=w.squad(clubId).filter{it.available}.toMutableList()
  c.tactics.xi=Formations.positions(c.tactics.formation).map{pos->val best=available.maxByOrNull{it.ratingAt(pos)*(.6+it.fitness*.004)};available.remove(best);best?.id?:0}.toMutableList();rebuildBench(w,c)
 }
 fun rebuildBench(w: World,c: Club){
  val pool=w.squad(c.id).filter{it.available&&it.id !in c.tactics.xi}.toMutableList();val result=mutableListOf<Int>()
  fun take(count: Int,predicate: (Player)->Boolean){repeat(count){val p=pool.filter(predicate).maxByOrNull{it.ca*(.7+it.fitness*.003)}?:return@repeat;result.add(p.id);pool.remove(p)}}
  take(1){it.position==Position.TW};take(2){it.position in listOf(Position.IV,Position.LV,Position.RV,Position.DM)};take(2){it.position in listOf(Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA)};take(2){it.position in listOf(Position.ST,Position.LA,Position.RA,Position.OM)}
  while(result.size<7&&pool.isNotEmpty()){val p=pool.maxBy{it.ca*(.7+it.fitness*.003)};result.add(p.id);pool.remove(p)}
  c.tactics.bench=result.take(7).toMutableList()
 }
 fun assignSlot(w: World,index: Int,playerId: Int){require(w.live==null){"Aufstellung im Spiel über Wechsel ändern."};val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.available){"Spieler nicht verfügbar."};val xi=w.club().tactics.xi;require(index in 0..10);val old=xi.indexOf(playerId);if(old>=0)xi[old]=xi[index];xi[index]=playerId;rebuildBench(w,w.club())}
 fun makeSchedule(w: World){
  w.fixtures.clear();w.matches.clear()
  for(l in w.leagues){
   val ids=l.clubIds.toList();require(ids.size>=2&&ids.size%2==0){"Ligagröße muss gerade und mindestens 2 sein: ${l.name} (${ids.size})"}
   val order=ids.toMutableList();val rounds=ids.size-1;val half=ids.size/2
   for(r in 0 until rounds){
    for(i in 0 until half){
     val a=order[i];val b=order[order.lastIndex-i];val home=if((r+i)%2==0)a else b;val away=if(home==a)b else a
     w.fixtures.add(Fixture(w.nextIds.fixture++,w.calendar.season,l.tier,r+1,home,away,competition=CompetitionType.LEAGUE))
     w.fixtures.add(Fixture(w.nextIds.fixture++,w.calendar.season,l.tier,r+1+rounds,away,home,competition=CompetitionType.LEAGUE))
    }
    order.add(1,order.removeAt(order.lastIndex))
   }
  }
  CompetitionEngine.scheduleSeason(w)
  w.fixtures.sortWith(compareBy<Fixture>{it.matchday}.thenBy{it.competition.sortPriority()}.thenBy{it.tier}.thenBy{it.id})
 }

 fun table(w: World,tier: Int): List<TableRow>{
  val rows=w.leagues.first{it.tier==tier}.clubIds.associateWith{TableRow(it)}
  for(f in w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&it.tier==tier&&it.played}){val m=w.matches[f.id]?:continue;val h=rows.getValue(f.homeId);val a=rows.getValue(f.awayId);h.played++;a.played++;h.goalsFor+=m.home.goals;h.goalsAgainst+=m.away.goals;a.goalsFor+=m.away.goals;a.goalsAgainst+=m.home.goals;when{m.home.goals>m.away.goals->{h.won++;a.lost++};m.home.goals<m.away.goals->{a.won++;h.lost++};else->{h.drawn++;a.drawn++}}}
  return rows.values.sortedWith(compareByDescending<TableRow>{it.points}.thenByDescending{it.difference}.thenByDescending{it.goalsFor}.thenBy{it.clubId})
 }
}

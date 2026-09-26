package de.gruenderelf.engine

private data class BmwPlayerSeed(
 val first:String,val last:String,val born:Int,val nationality:String,val position:Position,val secondary:List<Position>,
 val number:Int,val rating:Int,val potential:Int,val marketValue:Long,val annualSalary:Long,val foot:Foot=Foot.RIGHT
)

object BmwDeveloperWorldFactory {
 private const val BMW_FC_LOGO_BASE64="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"
 private const val REPLACED_CLUB_KEY="bundesliga-598-1-fc-union-berlin"
 private fun million(v:Long)=v*1_000_000L

 private val roster=listOf(
  BmwPlayerSeed("Manuel","Neuer",1986,"Deutschland",Position.TW,emptyList(),1,91,91,million(8),million(15)),
  BmwPlayerSeed("Timo","Falk",2004,"Deutschland",Position.TW,emptyList(),12,82,94,million(35),million(3)),
  BmwPlayerSeed("Moritz","Thalhammer",2010,"Deutschland",Position.TW,emptyList(),31,76,108,million(45),million(1)),
  BmwPlayerSeed("Noah","Reiter",2005,"Deutschland",Position.RV,listOf(Position.IV),2,85,101,million(110),million(6)),
  BmwPlayerSeed("Finn","Lorenz",2003,"Deutschland",Position.LV,listOf(Position.LA),3,84,96,million(75),million(5)),
  BmwPlayerSeed("Lennart","Krüger",2004,"Deutschland",Position.IV,emptyList(),4,89,106,million(145),million(7)),
  BmwPlayerSeed("Mika","Schneider",2009,"Deutschland",Position.RV,listOf(Position.RA),5,77,107,million(55),million(2)),
  BmwPlayerSeed("Marvin","Kern",2007,"Deutschland",Position.IV,emptyList(),14,80,104,million(60),million(2)),
  BmwPlayerSeed("Tiago","Valente",2008,"Portugal",Position.IV,listOf(Position.DM),23,92,116,million(220),million(9)),
  BmwPlayerSeed("Leonard","Haas",2008,"Deutschland",Position.IV,listOf(Position.DM),25,79,105,million(65),million(2)),
  BmwPlayerSeed("Joshua","Kimmich",1995,"Deutschland",Position.DM,listOf(Position.ZM,Position.RV),6,94,96,million(85),million(22)),
  BmwPlayerSeed("Gavi","",2004,"Spanien",Position.ZM,listOf(Position.DM,Position.OM),8,94,109,million(160),million(19)),
  BmwPlayerSeed("Emil","Hartmann",2006,"Deutschland",Position.DM,listOf(Position.ZM),16,87,110,million(125),million(6)),
  BmwPlayerSeed("Milan","Seidel",2007,"Deutschland",Position.ZM,listOf(Position.OM),20,82,108,million(85),million(3)),
  BmwPlayerSeed("David","Yilmaz",2008,"Deutschland",Position.ZM,listOf(Position.DM),24,79,104,million(60),million(2)),
  BmwPlayerSeed("Elias","Brandt",2009,"Deutschland",Position.OM,listOf(Position.RA,Position.LA),27,80,111,million(100),million(2)),
  BmwPlayerSeed("Michael","Olise",2002,"Frankreich",Position.RA,listOf(Position.OM),11,93,105,million(135),million(18),Foot.LEFT),
  BmwPlayerSeed("Jannis","Engel",2006,"Deutschland",Position.RA,emptyList(),17,82,103,million(70),million(3)),
  BmwPlayerSeed("Nico","Bergmann",2007,"Deutschland",Position.LA,listOf(Position.ST,Position.RA),22,82,106,million(85),million(3)),
  BmwPlayerSeed("Jamal","Musiala",2003,"Deutschland",Position.OM,listOf(Position.LA,Position.ZM),42,96,114,million(220),million(25)),
  BmwPlayerSeed("Lukas","Aigner",2008,"Deutschland",Position.ST,emptyList(),7,80,108,million(65),million(2)),
  BmwPlayerSeed("Jonas","Winter",2010,"Deutschland",Position.ST,listOf(Position.LA),9,85,119,million(180),million(4)),
  BmwPlayerSeed("Leon","Stark",2003,"Deutschland",Position.ST,listOf(Position.OM,Position.ZM,Position.RA,Position.LA),10,99,125,million(450),million(32)),
  BmwPlayerSeed("Julián","Álvarez",2000,"Argentinien",Position.ST,listOf(Position.OM),19,94,104,million(140),million(20))
 )

 fun create(seed:Long=20260925L):World{
  val leonDraft=PlayerDraft(
   firstName="Leon",lastName="Stark",birthYear=2003,nationality="Deutschland",height=180,weight=78,foot=Foot.RIGHT,
   position=Position.ST,secondary=mutableListOf(Position.OM,Position.ZM,Position.RA,Position.LA),number=10,attributes=attributesFor(Position.ST,99)
  )
  val w=WorldFactory.createRealModeWorld(seed,REPLACED_CLUB_KEY,leonDraft)
  val club=w.club()
  val self=w.self()
  w.squad(club.id).filter{it.id!=self.id}.map{it.id}.forEach{w.players.remove(it)}

  club.name="BMW FC";club.shortName="BFC";club.city="München";club.founded=2026
  club.primary=0xFF0066B1;club.secondary=0xFF05070A;club.logo=Logo(7,"BFC",BMW_FC_LOGO_BASE64)
  club.kits=Kits(
   home=Kit(0xFF05070A,0xFF0066B1,3),
   away=Kit(0xFFF4F7FA,0xFF0066B1,1),
   third=Kit(0xFF003B70,0xFFE4002B,2),
   keeper=Kit(0xFF0B3D2E,0xFFF4F7FA,1),
   training=Kit(0xFF111820,0xFF00ADEF,3)
  )
  club.stadium=Stadium(
   name="BMW Performance Arena",capacity=92_500,seats=92_500,pitchQuality=100,surface=Surface.GRASS,floodlights=true,
   cabin=100,stand=100,training=100,clubhouse=100,youth=100,gym=100,medicine=100
  )
  club.budget=60_000_000_000L
  club.philosophy="Technologie. Leistung. Freiheit."
  club.playPhilosophy="Adaptive Dominanz"
  club.youthPhilosophy="Eliteentwicklung"
  club.reputation=100;club.members=185_000;club.sponsor=Sponsor("BMW Group",2_000_000)
  club.commercialReputation=100;club.financialTrust=100
  club.dynamics.chemistry=99;club.dynamics.tacticalUnderstanding=99;club.dynamics.pressingCoordination=99
  club.dynamics.mentalHardness=96;club.dynamics.staffQuality=100
  listOf("AUFBAU","PRESSINGFALLE","HALBRAUM","RESTVERTEIDIGUNG","DIAGONALE").forEach{club.dynamics.patterns[it]=99}
  club.dynamics.patterns["RESTVERTEIDIGUNG"]=100
  club.tactics.formation="4-4-1-1";club.tactics.mentality=4;club.tactics.pressing=4;club.tactics.line=4
  club.tactics.tempo=4;club.tactics.width=4;club.tactics.buildUp=BuildUp.MIXED

  val dev=BmwDeveloperState(enabled=true,bmwClubId=club.id)
  fun install(p:Player,s:BmwPlayerSeed){
   p.firstName=s.first;p.lastName=s.last;p.birthYear=s.born;p.nationality=s.nationality;p.position=s.position
   p.secondary=s.secondary.toMutableList();p.number=s.number;p.foot=s.foot;p.attributes=attributesFor(s.position,s.rating);tuneOverall(p,s.rating)
   p.hidden.potential=s.potential;p.hidden.professionalism=96;p.hidden.consistency=94;p.hidden.development=98;p.hidden.pressure=96;p.hidden.ambition=90
   p.fitness=99.0;p.morale=95;p.form=7.5;p.sharpness=96;p.wage=(s.annualSalary/52).toInt();p.contractYears=5
   p.promisedRole=if(s.rating>=92)SquadRole.STAR else if(s.rating>=86)SquadRole.STARTER else SquadRole.ROTATION
   dev.playerMeta[p.id]=DeveloperPlayerMeta(p.id,s.potential,s.marketValue,s.annualSalary)
  }

  roster.forEach{s->
   val p=if(s.first=="Leon"&&s.last=="Stark")self else Player(w.nextIds.player++,club.id)
   install(p,s);w.players[p.id]=p
  }
  val leon=w.squad(club.id).first{it.firstName=="Leon"&&it.lastName=="Stark"}
  // Leon starts as BMW's 99-rated franchise player, but the Messi-Masterclass is deliberately
  // still open. Booking it later is what pushes the individual attributes beyond 100.
  leon.messiMentored=false
  leon.archetype="BMW Free 10"
  club.tactics.captainId=leon.id;club.tactics.targetPlayerId=leon.id;club.tactics.penaltyTakerId=leon.id;club.tactics.freeKickTakerId=leon.id
  w.user.difficulty=Difficulty.SANDBOX;w.user.tutorialEnabled=false;w.user.tutorialCompleted=true
  w.developer=dev
  BmwDeveloperSystems.seedTechnology(w)
  w.squad(club.id).firstOrNull{it.lastName=="Neuer"}?.let{dev.longevity[it.id]=LongevityProfile(it.id,true,.8,.92,.88)}
  WorldFactory.autoLineup(w,club.id)
  w.news.clear()
  w.news("BMW FC Experience aktiviert","BMW Performance Arena, NEXUS, ORIGIN, PROJECT ZERO, Weltranglisten und Longevity-Forschung sind aktiv.","good")
  return w
 }

 fun repairDeveloperSave(w:World):Boolean{
  if(!w.developer.enabled)return false
  val leon=w.squad(w.developer.bmwClubId).firstOrNull{it.firstName=="Leon"&&it.lastName=="Stark"}?:return false
  // Older BMW builds incorrectly used messiMentored=true as a match-engine boost even though
  // the Masterclass attribute package had never been applied. Detect only that exact legacy
  // state: completed flag + no attribute above 100. A genuinely completed Masterclass remains untouched.
  if(leon.messiMentored&&((leon.attributes.values().values.maxOrNull()?:0)<=100)){
   leon.messiMentored=false
   leon.archetype="BMW Free 10"
   w.news("Messi-Masterclass freigeschaltet","Leon Stark hat die Masterclass noch nicht absolviert. Sie kann jetzt regulär gebucht werden und hebt seine individuellen Attribute über 100.","good")
   return true
  }
  return false
 }

 private fun tuneOverall(p:Player,target:Int){
  fun adjust(delta:Int){
   when(p.position){
    Position.TW->p.attributes.keeping=(p.attributes.keeping+delta).coerceIn(1,99)
    Position.IV,Position.LV,Position.RV,Position.DM->p.attributes.tackling=(p.attributes.tackling+delta).coerceIn(1,99)
    Position.ZM->p.attributes.passing=(p.attributes.passing+delta).coerceIn(1,99)
    Position.OM->p.attributes.vision=(p.attributes.vision+delta).coerceIn(1,99)
    Position.LA,Position.RA->p.attributes.technique=(p.attributes.technique+delta).coerceIn(1,99)
    Position.ST->p.attributes.finishing=(p.attributes.finishing+delta).coerceIn(1,99)
   }
  }
  var guard=0
  while(p.ca!=target&&guard++<30){
   val before=p.ca
   adjust(if(before<target)1 else -1)
   if(p.ca==before&&((before<target&&target>=99)||(before>target&&target<=1)))break
  }
 }

 private fun attributesFor(pos:Position,rating:Int):Attributes{
  fun v(d:Int=0)=(rating+d).coerceIn(1,99)
  return when(pos){
   Position.TW->Attributes(v(-25),v(-35),v(-8),v(-10),v(-20),v(-4),v(-3),v(-5),v(-8),v(2),v(-20))
   Position.IV->Attributes(v(-5),v(-22),v(-6),v(-8),v(3),v(2),v(-1),v(-5),v(2),v(-45),v(-18))
   Position.LV,Position.RV->Attributes(v(2),v(-12),v(-2),v(-1),v(1),v(-4),v(2),v(-2),v(-8),v(-45),v(-10))
   Position.DM->Attributes(v(-3),v(-12),v(2),v(),v(2),v(-1),v(2),v(2),v(-8),v(-45),v(-4))
   Position.ZM->Attributes(v(-1),v(-7),v(2),v(2),v(-3),v(-5),v(2),v(3),v(-10),v(-45),v())
   Position.OM->Attributes(v(1),v(1),v(2),v(3),v(-18),v(-9),v(),v(3),v(-10),v(-45),v(1))
   Position.LA,Position.RA->Attributes(v(3),v(1),v(),v(3),v(-20),v(-10),v(1),v(1),v(-9),v(-45),v())
   Position.ST->Attributes(v(2),v(3),v(-2),v(2),v(-25),v(),v(1),v(),v(1),v(-45),v(1))
  }
 }
}

package de.gruenderelf.engine

private data class BmwPlayerSeed(
 val first:String,val last:String,val born:Int,val nationality:String,val position:Position,val secondary:List<Position>,
 val number:Int,val rating:Int,val potential:Int,val marketValue:Long,val annualSalary:Long,val foot:Foot=Foot.RIGHT
)

object BmwDeveloperWorldFactory {
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
  club.primary=0xFF0066B1;club.secondary=0xFF05070A;club.logo=Logo(7,"BFC")
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
   p.secondary=s.secondary.toMutableList();p.number=s.number;p.foot=s.foot;p.attributes=attributesFor(s.position,s.rating)
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

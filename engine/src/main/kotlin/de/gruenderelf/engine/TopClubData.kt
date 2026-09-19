package de.gruenderelf.engine

import kotlin.math.max

/**
 * Private roster pack used only by the optional Topclub career mode.
 * It deliberately uses the existing generic crest renderer and ships no club logos, photos or kit artwork.
 * Player ratings are gameplay estimates; names, club membership and squad numbers are the roster-pack data.
 */
data class TopPlayerSeed(
 val firstName: String,
 val lastName: String,
 val position: Position,
 val number: Int,
 val rating: Int,
)

data class TopClubSeed(
 val key: String,
 val name: String,
 val shortName: String,
 val country: String,
 val city: String,
 val stadiumName: String,
 val founded: Int,
 val primary: Long,
 val secondary: Long,
 val reputation: Int,
 val budget: Long,
 val players: List<TopPlayerSeed>,
)

object TopClubDatabase {
 private fun p(first: String,last: String,pos: Position,no: Int,rating: Int)=TopPlayerSeed(first,last,pos,no,rating)

 val clubs: List<TopClubSeed> = listOf(
  TopClubSeed("bayern","FC Bayern München","FCB","Deutschland","München","Arena München",1900,0xFFDC052D,0xFFFFFFFF,96,180_000_000L,listOf(
   p("Manuel","Neuer",Position.TW,1,89),p("Sven","Ulreich",Position.TW,26,75),p("Jonas","Urbig",Position.TW,40,82),
   p("Dayot","Upamecano",Position.IV,2,86),p("Minjae","Kim",Position.IV,3,84),p("Jonathan","Tah",Position.IV,4,85),p("Nathaniel","Brown",Position.LV,11,79),p("Alphonso","Davies",Position.LV,19,88),p("Hiroki","Ito",Position.IV,21,82),p("Sacha","Boey",Position.RV,23,80),p("Josip","Stanišić",Position.RV,44,82),
   p("Joshua","Kimmich",Position.DM,6,91),p("Tom","Bischof",Position.ZM,8,80),p("Jamal","Musiala",Position.OM,10,93),p("Konrad","Laimer",Position.ZM,27,84),p("Bara Sapoko","Ndiaye",Position.ZM,39,72),p("Lennart","Karl",Position.OM,42,79),p("Aleksandar","Pavlović",Position.DM,45,84),p("David Santos","Daiber",Position.ZM,50,71),
   p("Serge","Gnabry",Position.RA,7,84),p("Harry","Kane",Position.ST,9,94),p("Luis","Díaz",Position.LA,14,88),p("Michael","Olise",Position.RA,17,90),p("Ismael","Saibari",Position.OM,34,84)
  )),
  TopClubSeed("dortmund","Borussia Dortmund","BVB","Deutschland","Dortmund","Westfalenstadion",1909,0xFFFDE100,0xFF000000,90,95_000_000L,listOf(
   p("Gregor","Kobel",Position.TW,1,88),p("Patrick","Drewes",Position.TW,30,76),p("Silas","Ostrzinski",Position.TW,31,71),p("Alexander","Meyer",Position.TW,33,75),
   p("Waldemar","Anton",Position.IV,3,83),p("Nico","Schlotterbeck",Position.IV,4,87),p("Ramy","Bensebaini",Position.LV,5,81),p("Joane","Gadou",Position.IV,22,76),p("Daniel","Svensson",Position.LV,24,80),p("Julian","Ryerson",Position.RV,26,82),p("Kaua","Prates",Position.IV,36,72),p("Filippo","Mane",Position.IV,39,73),p("Luca","Reggiani",Position.IV,49,70),
   p("Jobe","Bellingham",Position.ZM,7,84),p("Felix","Nmecha",Position.ZM,8,84),p("Carney","Chukwuemeka",Position.OM,17,82),p("Konstantinos","Karetsas",Position.OM,19,83),p("Marcel","Sabitzer",Position.ZM,20,83),p("Emre","Can",Position.DM,23,81),p("Joey","Veerman",Position.ZM,25,85),p("Justin","Lerma",Position.OM,28,76),p("Giannis","Konstantelias",Position.OM,45,82),
   p("Serhou","Guirassy",Position.ST,9,88),p("Maximilian","Beier",Position.ST,14,83),p("Fabio","Silva",Position.ST,21,82),p("Samuele","Inacio",Position.ST,40,74),p("Mathis","Albert",Position.LA,41,72),p("Enzo","dos Santos",Position.RA,44,71),p("Mussa","Kaba",Position.ST,48,70)
  )),
  TopClubSeed("real","Real Madrid","RMA","Spanien","Madrid","Santiago Bernabéu",1902,0xFFFFFFFF,0xFFEEE7D8,98,260_000_000L,listOf(
   p("Thibaut","Courtois",Position.TW,1,92),p("Andriy","Lunin",Position.TW,13,84),
   p("Raúl","Asencio",Position.IV,2,82),p("Éder","Militão",Position.IV,3,87),p("Dean","Huijsen",Position.IV,4,87),p("Trent","Alexander-Arnold",Position.RV,12,89),p("Ibrahima","Konaté",Position.IV,16,88),p("Marc","Cucurella",Position.LV,17,86),p("Álvaro","Carreras",Position.LV,18,84),p("Antonio","Rüdiger",Position.IV,22,86),p("Ferland","Mendy",Position.LV,23,82),p("Denzel","Dumfries",Position.RV,24,86),
   p("Jude","Bellingham",Position.OM,5,94),p("Eduardo","Camavinga",Position.ZM,6,88),p("Federico","Valverde",Position.ZM,8,92),p("Aurélien","Tchouaméni",Position.DM,14,89),p("Arda","Güler",Position.OM,15,88),p("Bernardo","Silva",Position.OM,20,90),p("Thiago","Pitarch",Position.ZM,27,76),
   p("Vinícius","Júnior",Position.LA,7,94),p("Endrick","Felipe",Position.ST,9,86),p("Kylian","Mbappé",Position.ST,10,96),p("Rodrygo","Goes",Position.RA,11,88),p("Carlos","Espí",Position.ST,19,78),p("Brahim","Díaz",Position.OM,21,85),p("Yan","Diomande",Position.RA,25,82)
  )),
  TopClubSeed("barca","FC Barcelona","BAR","Spanien","Barcelona","Camp Nou",1899,0xFF004D98,0xFFA50044,96,170_000_000L,listOf(
   p("Joan","García",Position.TW,1,85),p("Wojciech","Szczęsny",Position.TW,13,83),p("Dominik","Livaković",Position.TW,25,82),
   p("João","Cancelo",Position.RV,2,86),p("Alejandro","Balde",Position.LV,3,85),p("Brian","Fariñas",Position.IV,4,77),p("Pau","Cubarsí",Position.IV,5,89),p("Xavi","Espart",Position.RV,12,75),p("Andreas","Christensen",Position.IV,15,83),p("Gerard","Martín",Position.LV,18,78),p("Jules","Koundé",Position.RV,23,88),p("Eric","García",Position.IV,24,82),
   p("Gavi","Páez",Position.ZM,6,89),p("Fermín","López",Position.OM,7,86),p("Pedri","González",Position.ZM,8,94),p("Rodrigo","Hernández",Position.DM,16,92),p("Dani","Olmo",Position.OM,20,88),p("Frenkie","de Jong",Position.ZM,21,90),p("Marc","Bernal",Position.DM,22,82),
   p("Gabriel","Jesus",Position.ST,9,86),p("Lamine","Yamal",Position.RA,10,95),p("Raphinha","Dias",Position.LA,11,91),p("Karim","Adeyemi",Position.LA,14,86),p("Anthony","Gordon",Position.LA,17,87),p("Roony","Bardghji",Position.RA,19,82),p("Jesse","Bisiwu",Position.RA,27,73),p("Hamza","Abdelkarim",Position.ST,29,72)
  )),
  TopClubSeed("liverpool","Liverpool FC","LIV","England","Liverpool","Anfield",1892,0xFFC8102E,0xFFFFFFFF,95,190_000_000L,listOf(
   p("Alisson","Becker",Position.TW,1,90),p("Giorgi","Mamardashvili",Position.TW,25,86),p("Vítězslav","Jaroš",Position.TW,56,76),p("Freddie","Woodman",Position.TW,31,75),
   p("Ronald","Araújo",Position.IV,4,88),p("Conor","Bradley",Position.RV,84,82),p("Luke","Chambers",Position.LV,44,75),p("Jeremie","Frimpong",Position.RV,30,88),p("Joe","Gomez",Position.IV,2,82),p("Miloš","Kerkez",Position.LV,6,84),p("Isaac","Mabaya",Position.RV,52,72),p("Kostas","Tsimikas",Position.LV,21,80),p("Virgil","van Dijk",Position.IV,5,90),
   p("Wataru","Endo",Position.DM,3,80),p("Ryan","Gravenberch",Position.DM,38,88),p("Alexis","Mac Allister",Position.ZM,10,90),p("James","McConnell",Position.DM,53,75),p("Dominik","Szoboszlai",Position.OM,8,88),p("Florian","Wirtz",Position.OM,7,92),
   p("Bradley","Barcola",Position.LA,29,88),p("Federico","Chiesa",Position.RA,14,84),p("Hugo","Ekitiké",Position.ST,22,87),p("Cody","Gakpo",Position.LA,18,87),p("Alexander","Isak",Position.ST,9,91),p("Victor","Muñoz",Position.RA,47,76)
  )),
  TopClubSeed("city","Manchester City","MCI","England","Manchester","Etihad Stadium",1880,0xFF6CABDD,0xFFFFFFFF,96,230_000_000L,listOf(
   p("Gianluigi","Donnarumma",Position.TW,1,92),p("Gerónimo","Rulli",Position.TW,28,82),p("Marcus","Bettinelli",Position.TW,13,74),
   p("Rúben","Dias",Position.IV,3,89),p("Marc","Guéhi",Position.IV,6,88),p("Rayan","Aït-Nouri",Position.LV,21,86),p("Vitor","Reis",Position.IV,22,82),p("Joško","Gvardiol",Position.LV,24,89),p("Abdukodir","Khusanov",Position.IV,45,83),p("Rico","Lewis",Position.RV,82,83),
   p("Elliot","Anderson",Position.ZM,5,86),p("Mateo","Kovačić",Position.ZM,8,84),p("Rayan","Cherki",Position.OM,10,88),p("Nico","González",Position.DM,14,84),p("Enzo","Fernández",Position.ZM,17,89),p("Matheus","Nunes",Position.ZM,27,84),p("Ayyoub","Bouaddi",Position.DM,32,82),p("Phil","Foden",Position.OM,47,91),
   p("Omar","Marmoush",Position.ST,7,88),p("Erling","Haaland",Position.ST,9,95),p("Jérémy","Doku",Position.LA,11,87),p("Iliman","Ndiaye",Position.RA,19,84),p("Savinho","Moreira",Position.RA,26,86),p("Jack","Grealish",Position.LA,18,82),p("Antoine","Semenyo",Position.RA,42,87),p("Allan","Elias",Position.ST,39,78),p("Ryan","McAidoo",Position.RA,64,74)
  )),
  TopClubSeed("arsenal","Arsenal FC","ARS","England","London","Emirates Stadium",1886,0xFFEF0107,0xFFFFFFFF,94,180_000_000L,listOf(
   p("David","Raya",Position.TW,1,88),p("Kepa","Arrizabalaga",Position.TW,13,82),
   p("Ben","White",Position.RV,4,84),p("William","Saliba",Position.IV,2,91),p("Gabriel","Magalhães",Position.IV,6,90),p("Piero","Hincapié",Position.LV,5,86),p("Jurriën","Timber",Position.RV,12,88),p("Riccardo","Calafiori",Position.LV,33,85),p("Cristhian","Mosquera",Position.IV,15,82),p("Myles","Lewis-Skelly",Position.LV,49,84),
   p("Declan","Rice",Position.DM,41,91),p("Martin","Ødegaard",Position.OM,8,91),p("Martin","Zubimendi",Position.DM,36,88),p("Mikel","Merino",Position.ZM,23,85),p("Bruno","Guimarães",Position.ZM,39,90),p("Christian","Nørgaard",Position.DM,16,81),p("Eberechi","Eze",Position.OM,10,88),p("Ethan","Nwaneri",Position.OM,53,83),
   p("Bukayo","Saka",Position.RA,7,93),p("Gabriel","Martinelli",Position.LA,11,86),p("Noni","Madueke",Position.RA,20,85),p("Christos","Tzolis",Position.LA,17,85),p("Viktor","Gyökeres",Position.ST,14,89),p("Kai","Havertz",Position.ST,29,86),p("Gabriel","Jesus",Position.ST,9,83),p("Leandro","Trossard",Position.LA,19,84),p("Max","Dowman",Position.OM,56,79)
  )),
  TopClubSeed("chelsea","Chelsea FC","CHE","England","London","Stamford Bridge",1905,0xFF034694,0xFFFFFFFF,91,165_000_000L,listOf(
   p("Robert","Sánchez",Position.TW,1,82),p("Teddy","Sharman-Lowe",Position.TW,28,75),p("Mike","Penders",Position.TW,39,78),p("Gaga","Slonina",Position.TW,44,76),
   p("Marco","Palestra",Position.RV,2,81),p("Wesley","Fofana",Position.IV,3,84),p("Valentín","Barco",Position.LV,4,81),p("Maxence","Lacroix",Position.IV,5,84),p("Levi","Colwill",Position.IV,6,85),p("Mamadou","Sarr",Position.IV,19,81),p("Jorrel","Hato",Position.LV,21,84),p("Reece","James",Position.RV,24,86),p("Malo","Gusto",Position.RV,27,83),p("Pep","Chavarría",Position.LV,29,79),p("Aaron","Anselmino",Position.IV,30,79),p("Josh","Acheampong",Position.IV,34,78),p("Tosin","Adarabioyo",Position.IV,38,80),
   p("Enzo","Fernández",Position.ZM,8,88),p("Dário","Essugo",Position.DM,16,80),p("Morgan","Rogers",Position.OM,17,87),p("Jordan","Henderson",Position.ZM,14,79),p("Moisés","Caicedo",Position.DM,25,89),p("Romeo","Lavia",Position.DM,45,82),
   p("Pedro","Neto",Position.RA,7,85),p("João","Pedro",Position.ST,9,87),p("Cole","Palmer",Position.OM,10,91),p("Jamie","Gittens",Position.LA,11,84),p("Liam","Delap",Position.ST,12,82),p("Nicolas","Jackson",Position.ST,15,82),p("Danny","Welbeck",Position.ST,18,79),p("Emmanuel","Emegha",Position.ST,22,82),p("Geovany","Quenda",Position.RA,23,84),p("Estêvão","Willian",Position.RA,41,86)
  )),
  TopClubSeed("psg","Paris Saint-Germain","PSG","Frankreich","Paris","Parc des Princes",1970,0xFF004170,0xFFDA291C,95,210_000_000L,listOf(
   p("Lucas","Chevalier",Position.TW,30,87),p("Matvey","Safonov",Position.TW,39,83),p("Alessandro","Longoni",Position.TW,16,72),
   p("Achraf","Hakimi",Position.RV,2,90),p("Lucas","Beraldo",Position.IV,4,82),p("Marquinhos","Corrêa",Position.IV,5,86),p("Illia","Zabarnyi",Position.IV,6,86),p("Lucas","Digne",Position.LV,12,82),p("Lucas","Hernández",Position.IV,21,84),p("Nuno","Mendes",Position.LV,25,90),p("Willian","Pacho",Position.IV,51,86),
   p("Fabián","Ruiz",Position.ZM,8,87),p("Maghnes","Akliouche",Position.OM,11,87),p("Désiré","Doué",Position.OM,14,90),p("Vitinha","Ferreira",Position.ZM,17,92),p("Senny","Mayulu",Position.ZM,24,81),p("Dro","Fernández",Position.OM,27,78),p("Warren","Zaïre-Emery",Position.ZM,33,88),p("João","Neves",Position.DM,87,90),
   p("Khvicha","Kvaratskhelia",Position.LA,7,91),p("Ferran","Torres",Position.ST,9,86),p("Ousmane","Dembélé",Position.RA,10,92),p("Mika","Godts",Position.LA,22,82),p("Quentin","Ndjantou",Position.RA,47,75)
  )),
  TopClubSeed("inter","Inter Mailand","INT","Italien","Mailand","San Siro",1908,0xFF0068A8,0xFF000000,93,145_000_000L,listOf(
   p("Josep","Martínez",Position.TW,1,84),p("Raffaele","Di Gennaro",Position.TW,12,74),p("Ivan","Provedel",Position.TW,49,83),p("Mattia","Marello",Position.TW,54,69),
   p("John","Stones",Position.IV,6,85),p("Manuel","Akanji",Position.IV,25,86),p("Benjamin","Pavard",Position.IV,28,85),p("Carlos","Augusto",Position.LV,30,82),p("Yann-Aurel","Bisseck",Position.IV,31,83),p("Federico","Dimarco",Position.LV,32,88),p("Alessandro","Bastoni",Position.IV,95,90),p("Djed","Spence",Position.RV,99,82),
   p("Piotr","Zieliński",Position.ZM,7,84),p("Petar","Sučić",Position.ZM,8,83),p("Andy","Diouf",Position.ZM,17,82),p("Hakan","Çalhanoğlu",Position.DM,20,88),p("Curtis","Jones",Position.ZM,21,84),p("Henrikh","Mkhitaryan",Position.ZM,22,81),p("Nicolò","Barella",Position.ZM,23,90),p("Aleksandar","Stanković",Position.DM,5,78),
   p("Marcus","Thuram",Position.ST,9,87),p("Lautaro","Martínez",Position.ST,10,91),p("Luis","Henrique",Position.RA,11,82),p("Ange-Yoan","Bonny",Position.ST,14,82),p("Pio","Esposito",Position.ST,94,82)
  )),
  TopClubSeed("juventus","Juventus","JUV","Italien","Turin","Juventus Stadium",1897,0xFFFFFFFF,0xFF000000,90,120_000_000L,listOf(
   p("Kamil","Grabara",Position.TW,1,84),p("Carlo","Pinsoglio",Position.TW,23,73),p("Guglielmo","Vicario",Position.TW,25,86),
   p("Zeki","Çelik",Position.RV,2,81),p("Bremer","Silva",Position.IV,3,87),p("Federico","Gatti",Position.IV,4,83),p("Lloyd","Kelly",Position.IV,6,81),p("Pierre","Kalulu",Position.IV,15,84),p("Jhon","Lucumí",Position.IV,26,84),p("Daniele","Rugani",Position.IV,24,78),
   p("Manuel","Locatelli",Position.DM,5,85),p("Teun","Koopmeiners",Position.ZM,8,84),p("Douglas","Luiz",Position.ZM,12,85),p("Nicolás","González",Position.RA,31,83),
   p("Francisco","Conceição",Position.RA,7,86),p("Randal","Kolo Muani",Position.ST,9,86),p("Edon","Zhegrova",Position.RA,11,85),p("Jérémie","Boga",Position.LA,13,81),p("Kerim","Alajbegović",Position.LA,17,78),p("Nick","Woltemade",Position.ST,27,85),p("Joseph","Owusu",Position.ST,41,72),p("Thomas","Pagnucco",Position.RA,43,70)
  )),
  TopClubSeed("atletico","Atlético de Madrid","ATM","Spanien","Madrid","Metropolitano",1903,0xFFCB3524,0xFFFFFFFF,91,125_000_000L,listOf(
   p("Juan","Musso",Position.TW,1,83),p("Jan","Oblak",Position.TW,13,89),p("Salvi","Esquivel",Position.TW,25,72),
   p("Dávid","Hancko",Position.IV,17,85),p("Marc","Pubill",Position.RV,18,82),p("David","Møller Wolfe",Position.LV,21,81),p("Alejandro","Grimaldo",Position.LV,22,87),p("Robin","Le Normand",Position.IV,24,84),p("Dani","Martínez",Position.IV,30,74),
   p("Obed","Vargas",Position.DM,3,82),p("Johnny","Cardoso",Position.DM,5,84),p("Koke","Resurrección",Position.ZM,6,82),p("Kang-in","Lee",Position.OM,7,85),p("Pablo","Barrios",Position.ZM,8,85),p("Álex","Baena",Position.OM,10,88),p("Marcos","Llorente",Position.ZM,14,85),p("Arnau","Ortiz",Position.OM,16,78),p("Giuliano","Simeone",Position.RA,20,83),p("Morten","Hjulmand",Position.DM,23,86),
   p("Alexander","Sørloth",Position.ST,9,86),p("Ademola","Lookman",Position.LA,11,88),p("Jonathan","David",Position.ST,15,87),p("Julián","Álvarez",Position.ST,19,92)
  ))
 )

 val options: List<TopClubSeed> get()=clubs
 fun requireClub(key: String)=clubs.firstOrNull{it.key==key}?:error("Unbekannter Topclub.")

 private fun attributesFor(pos: Position,rating: Int): Attributes {
  val r=rating.coerceIn(55,96)
  fun v(delta: Int)= (r+delta).coerceIn(1,96)
  return when(pos){
   Position.TW->Attributes(v(-34),v(-55),v(-18),v(-22),v(-18),v(-6),v(-8),v(-10),v(-22),r,v(-22))
   Position.IV->Attributes(v(-12),v(-24),v(-9),v(-12),v(2),v(2),v(-3),v(-8),v(1),12,v(-16))
   Position.LV,Position.RV->Attributes(v(1),v(-14),v(-2),v(-1),v(-2),v(-8),v(1),v(-4),v(-12),10,v(-8))
   Position.DM->Attributes(v(-5),v(-13),v(1),v(-1),v(0),v(-4),v(0),v(1),v(-8),10,v(-4))
   Position.ZM->Attributes(v(-4),v(-7),v(2),v(1),v(-7),v(-7),v(0),v(2),v(-9),9,v(-1))
   Position.OM->Attributes(v(-1),v(0),v(1),v(3),v(-15),v(-10),v(-2),v(3),v(-10),8,v(0))
   Position.LA,Position.RA->Attributes(v(3),v(-1),v(-1),v(3),v(-18),v(-8),v(-2),v(1),v(-12),7,v(-2))
   Position.ST->Attributes(v(0),v(3),v(-8),v(0),v(-20),v(-2),v(-4),v(-3),v(0),7,v(-3))
  }
 }

 fun makeClub(id: Int,seed: TopClubSeed): Club = Club(
  id=id,name=seed.name,shortName=seed.shortName,tier=1,city="${seed.city}, ${seed.country}",founded=seed.founded,
  primary=seed.primary,secondary=seed.secondary,budget=seed.budget,
  stadium=Stadium(name=seed.stadiumName,capacity=55_000,seats=50_000,pitchQuality=96,surface=Surface.GRASS,floodlights=true,cabin=90,stand=95,training=94,clubhouse=90,youth=86,gym=92,medicine=94),
  reputation=seed.reputation,members=75_000,sponsor=Sponsor("Global Partner",120_000),
  tactics=Tactics(formation="4-3-3",mentality=4,pressing=4,line=4,tempo=4,width=4,buildUp=BuildUp.MIXED),
 ).also{it.logo=Logo(id%8,seed.shortName);it.kits.home=Kit(seed.primary,seed.secondary,id%4);it.kits.away=Kit(seed.secondary,seed.primary,(id+1)%4)}

 fun makePlayer(id: Int,clubId: Int,seed: TopPlayerSeed): Player {
  val full="${seed.firstName} ${seed.lastName}"
  val age=18+Math.floorMod(full.hashCode(),16)
  val r=seed.rating.coerceIn(55,96)
  return Player(
   id=id,clubId=clubId,firstName=seed.firstName,lastName=seed.lastName,birthYear=2026-age,nationality="International",
   position=seed.position,number=seed.number,attributes=attributesFor(seed.position,r),
   hidden=Hidden(potential=max(r,(r+Math.floorMod(full.hashCode(),5)).coerceAtMost(96)),injuryProneness=25+Math.floorMod(full.hashCode(),35),consistency=(r-10).coerceAtLeast(55),professionalism=75,loyalty=65,ambition=82,development=75,pressure=(r-8).coerceAtLeast(60)),
   fitness=96.0,morale=78,form=7.0,sharpness=82,wage=(r*r*4).coerceAtLeast(1800)
  )
 }
}

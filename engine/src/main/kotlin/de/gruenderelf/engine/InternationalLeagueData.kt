package de.gruenderelf.engine

/**
 * 2026/27 league expansion for Real Mode.
 *
 * Top-five first divisions keep the hand-curated squad data from [RealModeData].
 * The leagues below add the real 2026/27 club structures. Their squads intentionally
 * remain empty here so WorldFactory can populate playable generated squads until the
 * separate player-import pipeline provides verified player records.
 */
object InternationalLeagueData {
 private val colors=listOf(
  "#0B3D91" to "#FFFFFF","#A50044" to "#FFFFFF","#111111" to "#FFFFFF",
  "#0057B8" to "#FFD700","#B00020" to "#FFFFFF","#146B3A" to "#FFFFFF"
 )

 private fun league(name:String,country:String,tier:Int,level:Int,prefix:String,names:List<String>): RealModeLeagueSeed {
  val clubs=names.mapIndexed{i,n->
   val (primary,secondary)=colors[(tier+i)%colors.size]
   RealModeClubSeed(
    key="${prefix}_${i+1}",
    name=n,
    shortName=(prefix.take(2).uppercase()+String.format("%02d",i+1)).take(4),
    league=name,
    country=country,
    primary=primary,
    secondary=secondary,
    players=emptyList()
   )
  }
  require(clubs.size>=2&&clubs.size%2==0){"Ligagröße muss gerade sein: $name"}
  return RealModeLeagueSeed(name,country,tier,clubs,level)
 }

 val leagues: List<RealModeLeagueSeed> by lazy { listOf(
  league("Championship","England",10,2,"eng_ch",listOf(
   "Birmingham City","Blackburn Rovers","Bolton Wanderers","Bristol City","Burnley","Cardiff City",
   "Charlton Athletic","Derby County","Lincoln City","Middlesbrough","Millwall","Norwich City",
   "Portsmouth","Preston North End","Queens Park Rangers","Sheffield United","Southampton","Stoke City",
   "Swansea City","Watford","West Bromwich Albion","West Ham United","Wolverhampton Wanderers","Wrexham"
  )),
  league("League One","England",11,3,"eng_l1",listOf(
   "AFC Wimbledon","Barnsley","Blackpool","Bradford City","Bromley","Burton Albion","Cambridge United",
   "Doncaster Rovers","Huddersfield Town","Leicester City","Leyton Orient","Luton Town","Mansfield Town",
   "Milton Keynes Dons","Notts County","Oxford United","Peterborough United","Plymouth Argyle","Reading",
   "Sheffield Wednesday","Stevenage","Stockport County","Wigan Athletic","Wycombe Wanderers"
  )),
  league("Segunda División","Spanien",12,2,"esp_2",listOf(
   "AD Ceuta FC","Albacete BP","Burgos CF","Cádiz CF","CD Castellón","CD Eldense","CD Leganés",
   "CD Tenerife","CE Sabadell","Celta Fortuna","Córdoba CF","FC Andorra","Girona FC","Granada CF",
   "Real Sociedad B","RCD Mallorca","Real Oviedo","Real Sporting","Real Valladolid CF","SD Eibar",
   "UD Almería","UD Las Palmas"
  )),
  league("Primera Federación · Grupo 1","Spanien",13,3,"esp_p1",listOf(
   "AD Mérida","Arenas de Getxo","Barakaldo","Bilbao Athletic","CD Coria","CD Extremadura","CD Lugo",
   "CP Cacereño","Cultural Leonesa","Deportivo Fabril","Mirandés","Ponferradina","Pontevedra",
   "Racing Ferrol","Real Avilés Industrial","Real Unión Club","UD Logroñés","UD Ourense","Unionistas CF","Zamora CF"
  )),
  league("Primera Federación · Grupo 2","Spanien",14,3,"esp_p2",listOf(
   "Águilas FC","AD Alcorcón","Algeciras CF","Antequera CF","Atlético Madrileño","CD Teruel","CE Europa",
   "FC Cartagena","Gimnàstic Tarragona","Hércules CF","SD Huesca","Juventud Torremolinos","Rayo Majadahonda",
   "Real Jaén","Real Murcia","Real Zaragoza","Real Madrid Castilla","UD Ibiza","UE Sant Andreu","Villarreal B"
  )),
  league("Serie B","Italien",15,2,"ita_b",listOf(
   "Arezzo","Ascoli","Avellino","Benevento","Carrarese","Catanzaro","Cesena","Cremonese","Empoli",
   "Hellas Verona","Juve Stabia","L.R. Vicenza","Mantova","Modena","Padova","Palermo","Pisa","Sampdoria",
   "Südtirol","Virtus Entella"
  )),
  league("Serie C · Girone A","Italien",16,3,"ita_c1",listOf(
   "AlbinoLeffe","Alcione Milano","Arzignano Valchiampo","Carpi","Cittadella","Desenzano","Dolomiti Bellunesi",
   "Folgore Caratese","Giana Erminio","Juventus Next Gen","Lecco","Lumezzane","Novara","Ospitaletto Franciacorta",
   "Pergolettese","Pro Vercelli","Renate","Trento","Treviso","Union Brescia"
  )),
  league("Serie C · Girone B","Italien",17,3,"ita_c2",listOf(
   "Atalanta U23","Campobasso","Forlì","Grosseto","Gubbio","Guidonia Montecelio","Latina","Livorno","Ostiamare",
   "Pescara","Perugia","Pianese","Pineto","Ravenna","Reggiana","Sambenedettese","Spezia","Torres","Vado","Vis Pesaro"
  )),
  league("Serie C · Girone C","Italien",18,3,"ita_c3",listOf(
   "Team Altamura","Audace Cerignola","Bari","Barletta","Casarano","Casertana","Catania","Cavese","Cosenza",
   "Crotone","Foggia","Giugliano","Inter U23","Monopoli","Picerno","Potenza","Salernitana","Savoia","Scafatese","Sorrento"
  )),
  league("Ligue 2","Frankreich",19,2,"fra_l2",listOf(
   "FC Annecy","US Boulogne CO","Clermont Foot 63","Dijon FCO","USL Dunkerque","EA Guingamp","Grenoble Foot 38",
   "Stade Lavallois","FC Metz","Montpellier Hérault SC","AS Nancy Lorraine","FC Nantes","Pau FC","Red Star FC",
   "Stade de Reims","Rodez AF","AS Saint-Étienne","FC Sochaux-Montbéliard"
  )),
  league("Ligue 3","Frankreich",20,3,"fra_l3",listOf(
   "Amiens SC","SC Aubagne Air Bel","SC Bastia","Bourg-en-Bresse Péronnas","SM Caen","AS Cannes",
   "US Concarneau","FC Fleury 91","VFC La Roche-sur-Yon","Le Puy Foot 43","US Orléans","Paris 13 Atletico",
   "Quevilly-Rouen Métropole","FC Rouen 1899","US Thionville Lusitanos","Valenciennes FC","FC Versailles 78",
   "FC Villefranche Beaujolais"
  )),
  league("Eredivisie","Niederlande",21,1,"ned_1",listOf(
   "ADO Den Haag","Ajax","AZ","Excelsior Rotterdam","FC Groningen","FC Twente","FC Utrecht","Feyenoord",
   "Fortuna Sittard","Go Ahead Eagles","N.E.C. Nijmegen","PEC Zwolle","PSV","SC Cambuur","sc Heerenveen",
   "Sparta Rotterdam","Telstar","Willem II"
  )),
  league("Jupiler Pro League","Belgien",22,1,"bel_1",listOf(
   "Cercle Brugge","Club Brugge","KAA Gent","KRC Genk","KV Kortrijk","KV Mechelen","KVC Westerlo","Lommel SK",
   "OH Leuven","RAAL La Louvière","Royal Antwerp FC","Royale Union Saint-Gilloise","RSC Anderlecht","SK Beveren",
   "Sporting Charleroi","Standard de Liège","STVV","SV Zulte Waregem"
  )),
  league("ADMIRAL Bundesliga","Österreich",23,1,"aut_1",listOf(
   "SK Sturm Graz","FC Red Bull Salzburg","SK Rapid","Wolfsberger AC","TSV Hartberg","SCR Altach",
   "SV Ried","LASK","FK Austria Wien","Grazer AK 1902","WSG Tirol","SC Austria Lustenau"
  )),
  league("Brack Super League","Schweiz",24,1,"sui_1",listOf(
   "FC Basel 1893","Grasshopper Club Zürich","FC Lausanne-Sport","FC Lugano","FC Luzern","Servette FC",
   "FC St. Gallen 1879","FC Sion","FC Thun","FC Vaduz","BSC Young Boys","FC Zürich"
  )),
  league("Süper Lig","Türkei",25,1,"tur_1",listOf(
   "Amed SK","Çorum FK","Beşiktaş","Çaykur Rizespor","Alanyaspor","Erzurumspor FK","Eyüpspor","Fenerbahçe",
   "Galatasaray","Gaziantep FK","Gençlerbirliği","Göztepe","İstanbul Başakşehir","Kasımpaşa","Kocaelispor",
   "Samsunspor","Trabzonspor","Konyaspor"
  )),
  league("Liga Portugal Betclic","Portugal",26,1,"por_1",listOf(
   "SL Benfica","Sporting CP","FC Porto","SC Braga","FC Famalicão","Gil Vicente FC","Vitória SC","Moreirense FC",
   "FC Alverca","Estoril Praia","FC Arouca","Rio Ave FC","Santa Clara","CD Nacional","Estrela Amadora",
   "Casa Pia AC","Marítimo","Académico de Viseu"
  ))
 ) }
}

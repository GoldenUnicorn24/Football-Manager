package de.gruenderelf.engine

/**
 * Erweiterter europaeischer Datenbestand fuer den Real-Modus.
 *
 * Die Vereine sind real; fuer Ligen ohne vollstaendigen Kader-Snapshot erzeugt
 * WorldFactory ligaabhaengige Spieler. Tier ist eine globale interne Liga-ID,
 * level bildet die sportliche Ebene innerhalb des Landes ab.
 */
data class DomesticCupSeed(
    val id:String,
    val country:String,
    val name:String,
    val maxLevel:Int=99,
    val maxParticipants:Int=0
)

object EuropeanLeagueData {
    private val palette=listOf(
        0xFFB51F2E,0xFF173D6B,0xFF1E7048,0xFF202020,0xFF7B2E4B,0xFFD0A91E,
        0xFF2D628D,0xFF613A78,0xFFB45A26,0xFF176A72,0xFF6C772C,0xFF7C4B2A
    )

    private fun slug(value:String)=value.lowercase()
        .replace("ä","ae").replace("ö","oe").replace("ü","ue").replace("ß","ss")
        .replace("á","a").replace("à","a").replace("â","a").replace("ã","a")
        .replace("é","e").replace("è","e").replace("ê","e")
        .replace("í","i").replace("ı","i").replace("ó","o").replace("ô","o")
        .replace("ú","u").replace("ç","c").replace("ć","c").replace("č","c")
        .replace("š","s").replace("ž","z").replace("ł","l").replace("ń","n")
        .replace("ś","s").replace("ź","z").replace("ż","z")
        .replace(Regex("[^a-z0-9]+"),"-").trim('-')

    private fun club(country:String,league:String,name:String,short:String,index:Int)=RealModeClubSeed(
        key="eu-${slug(country)}-${slug(league)}-${slug(name)}",
        name=name,
        shortName=short,
        league=league,
        country=country,
        primary=palette[index%palette.size],
        secondary=0xFFF0F0EC,
        players=emptyList()
    )

    private fun league(
        name:String,country:String,tier:Int,level:Int,
        clubs:List<Pair<String,String>>
    )=RealModeLeagueSeed(
        name=name,country=country,tier=tier,level=level,
        clubs=clubs.mapIndexed{i,(clubName,short)->club(country,name,clubName,short,i)}
    )

    private val championship=listOf(
        "Blackburn Rovers" to "BBR","Birmingham City" to "BIR","Bristol City" to "BRC",
        "Charlton Athletic" to "CHA","Derby County" to "DER","Middlesbrough" to "MID",
        "Millwall" to "MIL","Norwich City" to "NOR","Portsmouth" to "POR",
        "Preston North End" to "PNE","Queens Park Rangers" to "QPR","Sheffield United" to "SHU",
        "Southampton" to "SOU","Stoke City" to "STK","Swansea City" to "SWA",
        "Watford" to "WAT","West Bromwich Albion" to "WBA","Wrexham" to "WRE",
        "West Ham United" to "WHU","Burnley" to "BUR","Wolverhampton Wanderers" to "WOL",
        "Lincoln City" to "LIN","Cardiff City" to "CAR","Bolton Wanderers" to "BOL"
    )

    private val leagueOne=listOf(
        "Barnsley" to "BAR","Blackpool" to "BLP","Bradford City" to "BRA",
        "Burton Albion" to "BURT","Doncaster Rovers" to "DON","Huddersfield Town" to "HUD",
        "Leyton Orient" to "LEY","Luton Town" to "LUT","Mansfield Town" to "MAN",
        "Peterborough United" to "PET","Plymouth Argyle" to "PLY","Reading" to "REA",
        "Stevenage" to "STE","Stockport County" to "STO","Wigan Athletic" to "WIG",
        "AFC Wimbledon" to "WIM","Wycombe Wanderers" to "WYC","Oxford United" to "OXF",
        "Leicester City" to "LEI","Sheffield Wednesday" to "SHW","Bromley" to "BRO",
        "Milton Keynes Dons" to "MKD","Cambridge United" to "CAM","Notts County" to "NOT"
    )

    private val segunda=listOf(
        "AD Ceuta" to "CEU","Albacete" to "ALB","Burgos CF" to "BUR","Cadiz CF" to "CAD",
        "CD Castellon" to "CAS","CD Eldense" to "ELD","CD Leganes" to "LEG","CD Tenerife" to "TEN",
        "CE Sabadell" to "SAB","Celta Fortuna" to "CELB","Cordoba CF" to "COR","FC Andorra" to "AND",
        "Girona FC" to "GIR","Granada CF" to "GRA","Real Sociedad B" to "RSB","RCD Mallorca" to "MLL",
        "Real Oviedo" to "OVI","Sporting Gijon" to "SPG","Real Valladolid" to "VLL",
        "SD Eibar" to "EIB","UD Almeria" to "ALM","UD Las Palmas" to "LPA"
    )

    private val serieB=listOf(
        "SS Arezzo" to "ARZ","Ascoli Calcio" to "ASC","US Avellino" to "AVE","Benevento Calcio" to "BEN",
        "Carrarese Calcio" to "CAR","US Catanzaro" to "CAT","AC Cesena" to "CES","US Cremonese" to "CRE",
        "Empoli FC" to "EMP","Hellas Verona" to "VER","Juve Stabia" to "JST","Mantova 1911" to "MAN",
        "Modena FC" to "MOD","Calcio Padova" to "PAD","Palermo FC" to "PAL","Pisa SC" to "PIS",
        "UC Sampdoria" to "SAM","FC Sudtirol" to "SUD","LR Vicenza" to "VIC","Virtus Entella" to "ENT"
    )

    private val ligue2=listOf(
        "FC Sochaux" to "SOC","AS Saint-Etienne" to "STE","AS Nancy" to "NAN","US Boulogne" to "USB",
        "Clermont Foot" to "CLE","Stade de Reims" to "REI","USL Dunkerque" to "DUN","Grenoble Foot 38" to "GRE",
        "FC Metz" to "MET","EA Guingamp" to "GUI","Montpellier HSC" to "MON","Dijon FCO" to "DIJ",
        "FC Nantes" to "NTE","Red Star FC" to "RED","Pau FC" to "PAU","FC Annecy" to "ANN",
        "Rodez AF" to "ROD","Stade Lavallois" to "LAV"
    )

    private val croatia=listOf(
        "GNK Dinamo Zagreb" to "DIN","HNK Gorica" to "GOR","HNK Hajduk Split" to "HAJ",
        "NK Istra 1961" to "IST","NK Lokomotiva Zagreb" to "LOK","NK Osijek" to "OSI",
        "HNK Rijeka" to "RIJ","NK Rudes" to "RUD","NK Slaven Belupo" to "SLA","NK Varazdin" to "VAR"
    )

    private val russia=listOf(
        "Akron Tolyatti" to "AKR","Akhmat Grozny" to "AKH","Baltika Kaliningrad" to "BAL",
        "Dynamo Makhachkala" to "DMK","Dynamo Moscow" to "DYN","Zenit St. Petersburg" to "ZEN",
        "FC Krasnodar" to "KRA","Krylia Sovetov Samara" to "KSS","Lokomotiv Moscow" to "LOK",
        "FC Orenburg" to "ORE","Rodina Moscow" to "ROD","FC Rostov" to "ROS",
        "Rubin Kazan" to "RUB","Spartak Moscow" to "SPA","Fakel Voronezh" to "FAK","CSKA Moscow" to "CSK"
    )

    private val netherlands=listOf(
        "ADO Den Haag" to "ADO","Ajax" to "AJA","AZ Alkmaar" to "AZ","Excelsior Rotterdam" to "EXC",
        "FC Groningen" to "GRO","FC Twente" to "TWE","FC Utrecht" to "UTR","Feyenoord" to "FEY",
        "Fortuna Sittard" to "FOR","Go Ahead Eagles" to "GAE","NEC Nijmegen" to "NEC",
        "PEC Zwolle" to "PEC","PSV Eindhoven" to "PSV","SC Cambuur" to "CAM",
        "SC Heerenveen" to "HEE","Sparta Rotterdam" to "SPR","Telstar" to "TEL","Willem II" to "WII"
    )

    private val belgium=listOf(
        "Cercle Brugge" to "CER","Club Brugge" to "CLU","KAA Gent" to "GNT","KRC Genk" to "GNK",
        "KV Kortrijk" to "KVK","KV Mechelen" to "KVM","KVC Westerlo" to "WES","Lommel SK" to "LOM",
        "OH Leuven" to "OHL","RAAL La Louviere" to "RAAL","Royal Antwerp FC" to "ANT",
        "Union Saint-Gilloise" to "USG","RSC Anderlecht" to "AND","SK Beveren" to "BEV",
        "Sporting Charleroi" to "CHA","Standard de Liege" to "STA","STVV" to "STV","SV Zulte Waregem" to "ZUL"
    )

    private val austria=listOf(
        "LASK" to "LASK","SK Sturm Graz" to "STU","FC Red Bull Salzburg" to "RBS","FK Austria Wien" to "FAK",
        "SK Rapid Wien" to "SCR","TSV Hartberg" to "HTB","SV Ried" to "SVR","Wolfsberger AC" to "WAC",
        "SCR Altach" to "ALT","Grazer AK" to "GAK","WSG Tirol" to "WSG","SC Austria Lustenau" to "ALU"
    )

    private val poland=listOf(
        "Gornik Zabrze" to "GOR","Wisla Krakow" to "WIS","Legia Warszawa" to "LEG","Lech Poznan" to "LPO",
        "Zaglebie Lubin" to "ZAG","Piast Gliwice" to "PIA","Pogon Szczecin" to "POG","Jagiellonia Bialystok" to "JAG",
        "Korona Kielce" to "KOR","GKS Katowice" to "GKS","Widzew Lodz" to "WID","Cracovia" to "CRA",
        "Wisla Plock" to "WPL","Radomiak Radom" to "RAD","Slask Wroclaw" to "SLA",
        "Wieczysta Krakow" to "WIE","Motor Lublin" to "MOT","Rakow Czestochowa" to "RAK"
    )

    private val sweden=listOf(
        "Mjallby AIF" to "MJA","BK Hacken" to "HAC","GAIS" to "GAI","IFK Goteborg" to "IFK",
        "Orgryte IS" to "ORG","Halmstads BK" to "HAL","Malmo FF" to "MFF","Kalmar FF" to "KAL",
        "AIK" to "AIK","Djurgardens IF" to "DIF","Hammarby IF" to "HAM","IF Brommapojkarna" to "BP",
        "IK Sirius" to "SIR","IF Elfsborg" to "ELF","Vasteras SK" to "VSK","Degerfors IF" to "DEG"
    )


    private val portugal=listOf(
        "FC Porto" to "FCP","SL Benfica" to "SLB","Sporting CP" to "SCP","Santa Clara" to "SAN",
        "FC Arouca" to "ARO","SC Braga" to "BRA","Academico de Viseu" to "AVI","Estrela da Amadora" to "EST",
        "Gil Vicente" to "GIL","FC Alverca" to "ALV","Maritimo" to "MAR","Moreirense" to "MOR",
        "FC Famalicao" to "FAM","Vitoria SC" to "VSC","Nacional" to "NAC","Rio Ave" to "RIO",
        "Casa Pia" to "CAS","Estoril Praia" to "ESTO"
    )

    private val turkey=listOf(
        "Amed SK" to "AMED","Galatasaray" to "GS","Besiktas" to "BJK","Kocaelispor" to "KOC",
        "Alanyaspor" to "ALA","Fenerbahce" to "FB","Trabzonspor" to "TS","Kasimpasa" to "KAS",
        "Caykur Rizespor" to "RIZ","Gaziantep FK" to "GFK","Corum FK" to "COR","Istanbul Basaksehir" to "IBFK",
        "Genclerbirligi" to "GEN","Erzurumspor FK" to "ERZ","Konyaspor" to "KON","Samsunspor" to "SAM",
        "Goztepe" to "GOZ","Eyupspor" to "EYU"
    )

    private val switzerland=listOf(
        "FC Lugano" to "LUG","FC Sion" to "SIO","BSC Young Boys" to "YB","FC Basel" to "BAS",
        "FC St. Gallen" to "STG","FC Luzern" to "LUZ","FC Zurich" to "FCZ","Servette FC" to "SER",
        "FC Thun" to "THU","Grasshopper Club Zurich" to "GCZ","FC Vaduz" to "VAD","Lausanne-Sport" to "LS"
    )

    private val scotland=listOf(
        "Celtic" to "CEL","Rangers" to "RAN","Heart of Midlothian" to "HEA","Dundee FC" to "DUN",
        "St. Mirren" to "STM","St. Johnstone" to "STJ","Aberdeen" to "ABE","Motherwell" to "MOT",
        "Dundee United" to "DUU","Hibernian" to "HIB","Falkirk" to "FAL","Kilmarnock" to "KIL"
    )

    private val greece=listOf(
        "Panathinaikos" to "PAO","PAOK" to "PAOK","AEK Athens" to "AEK","Olympiacos" to "OLY",
        "Atromitos" to "ATR","AE Kifisia" to "KIF","Aris Thessaloniki" to "ARI","Iraklis" to "IRA",
        "Levadiakos" to "LEV","Volos NPS" to "VOL","OFI Crete" to "OFI","Asteras Tripolis" to "AST",
        "Kalamata" to "KAL","Panetolikos" to "PAN"
    )

    private val czechia=listOf(
        "Slavia Praha" to "SLA","Viktoria Plzen" to "PLZ","Sparta Praha" to "SPA","Hradec Kralove" to "HKR",
        "Slovan Liberec" to "LIB","FK Jablonec" to "JAB","Banik Ostrava" to "BAN","Sigma Olomouc" to "SIG",
        "Zbrojovka Brno" to "ZBR","Bohemians 1905" to "BOH","SK Artis Brno" to "ART","Pardubice" to "PAR",
        "Slovacko" to "SLO","Teplice" to "TEP","Mlada Boleslav" to "MBL","FC Zlin" to "ZLI"
    )

    private val denmark=listOf(
        "FC Copenhagen" to "FCK","FC Midtjylland" to "FCM","Viborg FF" to "VFF","FC Nordsjaelland" to "FCN",
        "Brondby IF" to "BIF","AC Horsens" to "ACH","Silkeborg IF" to "SIF","Randers FC" to "RFC",
        "OB Odense" to "OB","Lyngby BK" to "LBK","AGF Aarhus" to "AGF","SonderjyskE" to "SJE"
    )

    private val serbia=listOf(
        "Crvena zvezda" to "CZV","Vojvodina" to "VOJ","Radnicki 1923" to "R23","Mladost Lucani" to "MLA",
        "IMT" to "IMT","Zeleznicar Pancevo" to "ZEL","Radnik Surdulica" to "RAD","Novi Pazar" to "NPA",
        "Cukaricki" to "CUK","OFK Beograd" to "OFK","Partizan" to "PAR","Radnicki Nis" to "RNI",
        "Macva Sabac" to "MAC","Zemun" to "ZEM"
    )

    private val ukraine=listOf(
        "Karpaty Lviv" to "KAR","Polissya Zhytomyr" to "POL","Shakhtar Donetsk" to "SHA","FC Kharkiv" to "KHA",
        "Epitsentr" to "EPI","LNZ Cherkasy" to "LNZ","Dynamo Kyiv" to "DKY","Zorya Luhansk" to "ZOR",
        "Bukovyna Chernivtsi" to "BUK","Veres Rivne" to "VER","Kryvbas Kryvyi Rih" to "KRY","Livyi Bereh Kyiv" to "LIV",
        "Chornomorets Odesa" to "CHO","Obolon Kyiv" to "OBO","Kolos Kovalivka" to "KOL","Kudrivka" to "KUD"
    )

    private val norway=listOf(
        "Bodo/Glimt" to "BOD","Viking FK" to "VIK","Molde FK" to "MOL","Tromso IL" to "TRO",
        "Lillestrom SK" to "LSK","Rosenborg BK" to "RBK","Fredrikstad FK" to "FFK","SK Brann" to "BRA",
        "Sarpsborg 08" to "SAR","HamKam" to "HAM","Valerenga" to "VIF","KFUM Oslo" to "KFUM",
        "Sandefjord" to "SAN","Kristiansund BK" to "KBK","Aalesund" to "AAL","IK Start" to "STA"
    )


    private val primeraFederacion1=listOf(
        "AD Merida" to "MER","Arenas Club" to "ARE","Athletic Club B" to "ATHB","Barakaldo CF" to "BAR",
        "CD Coria" to "COR","CD Extremadura" to "EXT","CD Lugo" to "LUG","CD Mirandes" to "MIR",
        "CP Cacereno" to "CAC","Cultural Leonesa" to "CUL","Pontevedra CF" to "PON","Racing Ferrol" to "FER",
        "RC Deportivo Fabril" to "FAB","Real Aviles Industrial" to "AVI","Real Union Club" to "RUN",
        "SD Ponferradina" to "PONF","UD Logrones" to "LOG","UD Ourense" to "OUR",
        "Unionistas de Salamanca" to "UNI","Zamora CF" to "ZAM"
    )

    private val primeraFederacion2=listOf(
        "AD Alcorcon" to "ALC","Aguilas FC" to "AGU","Algeciras CF" to "ALG","Antequera CF" to "ANT",
        "Atletico Madrileno" to "ATM2","CD Teruel" to "TER","CE Europa" to "EUR","CF Rayo Majadahonda" to "RMAJ",
        "FC Cartagena" to "CAR","Gimnastic de Tarragona" to "GIM","Hercules CF" to "HER",
        "Juventud Torremolinos" to "JUV","Real Jaen CF" to "RJA","Real Madrid Castilla" to "RMC",
        "Real Murcia CF" to "MUR","Real Zaragoza" to "ZAR","SD Huesca" to "HUE","UD Ibiza" to "IBI",
        "UE Sant Andreu" to "SAA","Villarreal CF B" to "VILB"
    )

    val leagues:List<RealModeLeagueSeed> by lazy(LazyThreadSafetyMode.PUBLICATION){
        listOf(
            league("Championship","England",10,2,championship),
            league("League One","England",11,3,leagueOne),
            league("LaLiga Hypermotion","Spanien",12,2,segunda),
            league("Serie B","Italien",13,2,serieB),
            league("Ligue 2","Frankreich",14,2,ligue2),
            league("SuperSport HNL","Kroatien",15,1,croatia),
            league("Russian Premier League","Russland",16,1,russia),
            league("Eredivisie","Niederlande",17,1,netherlands),
            league("Jupiler Pro League","Belgien",18,1,belgium),
            league("ADMIRAL Bundesliga","Österreich",19,1,austria),
            league("Ekstraklasa","Polen",20,1,poland),
            league("Allsvenskan","Schweden",21,1,sweden)
        )
    }

    val domesticCups:List<DomesticCupSeed> = listOf(
        DomesticCupSeed("DFB_POKAL","Deutschland","DFB-Pokal",maxLevel=5,maxParticipants=64),
        DomesticCupSeed("FA_CUP","England","FA Cup",maxLevel=3),
        DomesticCupSeed("EFL_CUP","England","EFL Cup",maxLevel=3),
        DomesticCupSeed("COPA_DEL_REY","Spanien","Copa del Rey",maxLevel=3),
        DomesticCupSeed("COPPA_ITALIA","Italien","Coppa Italia",maxLevel=2),
        DomesticCupSeed("COUPE_DE_FRANCE","Frankreich","Coupe de France",maxLevel=2),
        DomesticCupSeed("HRVATSKI_KUP","Kroatien","Hrvatski nogometni kup",maxLevel=1),
        DomesticCupSeed("RUSSIAN_CUP","Russland","Russian Cup",maxLevel=1),
        DomesticCupSeed("KNVB_BEKER","Niederlande","KNVB Beker",maxLevel=1),
        DomesticCupSeed("CROKY_CUP","Belgien","Croky Cup",maxLevel=1),
        DomesticCupSeed("OFB_CUP","Österreich","ÖFB-Cup",maxLevel=1),
        DomesticCupSeed("PUCHAR_POLSKI","Polen","Puchar Polski",maxLevel=1),
        DomesticCupSeed("SVENSKA_CUPEN","Schweden","Svenska Cupen",maxLevel=1)
    )

    fun cupById(id:String)=domesticCups.firstOrNull{it.id==id}
    fun cupsForCountry(country:String)=domesticCups.filter{it.country==country}
}

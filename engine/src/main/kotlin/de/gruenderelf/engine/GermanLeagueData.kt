package de.gruenderelf.engine

/**
 * Aktueller deutscher Herren-Ligabaum 2026/27 bis einschließlich Oberliga Hamburg.
 * Unterhalb der Bundesliga werden bewusst keine fremden Kaderdaten vorgetäuscht:
 * Die Vereine sind real, die Spieler werden beim Karrierestart ligaabhängig generiert.
 */
object GermanLeagueData {
    private fun club(key:String,name:String,short:String,league:String,index:Int)=RealModeClubSeed(
        key=key,
        name=name,
        shortName=short,
        league=league,
        country="Deutschland",
        primary=listOf(0xFFB32025,0xFF193B6A,0xFF1D6B45,0xFF202020,0xFF7A2D31,0xFFCFB53B,0xFF2E5A88,0xFF6A2B7B)[index%8],
        secondary=0xFFF0F0EC,
        players=emptyList(),
    )

    private val secondNames=listOf(
        Triple("2bl-hertha","Hertha BSC","BSC"),
        Triple("2bl-nuernberg","1. FC Nürnberg","FCN"),
        Triple("2bl-heidenheim","1. FC Heidenheim 1846","FCH"),
        Triple("2bl-magdeburg","1. FC Magdeburg","FCM"),
        Triple("2bl-wolfsburg","VfL Wolfsburg","WOB"),
        Triple("2bl-kaiserslautern","1. FC Kaiserslautern","FCK"),
        Triple("2bl-cottbus","FC Energie Cottbus","FCE"),
        Triple("2bl-bochum","VfL Bochum 1848","BOC"),
        Triple("2bl-st-pauli","FC St. Pauli","STP"),
        Triple("2bl-osnabrueck","VfL Osnabrück","OSN"),
        Triple("2bl-fuerth","SpVgg Greuther Fürth","SGF"),
        Triple("2bl-karlsruhe","Karlsruher SC","KSC"),
        Triple("2bl-braunschweig","Eintracht Braunschweig","EBS"),
        Triple("2bl-bielefeld","DSC Arminia Bielefeld","DSC"),
        Triple("2bl-hannover","Hannover 96","H96"),
        Triple("2bl-darmstadt","SV Darmstadt 98","SVD"),
        Triple("2bl-dresden","SG Dynamo Dresden","SGD"),
        Triple("2bl-kiel","Holstein Kiel","KSV"),
    )

    private val thirdNames=listOf(
        Triple("3liga-duisburg","MSV Duisburg","MSV"),
        Triple("3liga-rostock","FC Hansa Rostock","HRO"),
        Triple("3liga-hoffenheim2","TSG Hoffenheim II","TSG2"),
        Triple("3liga-viktoria-koeln","FC Viktoria Köln","VIK"),
        Triple("3liga-saarbruecken","1. FC Saarbrücken","FCS"),
        Triple("3liga-meppen","SV Meppen","SVM"),
        Triple("3liga-ingolstadt","FC Ingolstadt 04","FCI"),
        Triple("3liga-wuerzburg","FC Würzburger Kickers","FWK"),
        Triple("3liga-mannheim","SV Waldhof Mannheim","WMA"),
        Triple("3liga-aachen","Alemannia Aachen","AAC"),
        Triple("3liga-fortuna-koeln","SC Fortuna Köln","FKO"),
        Triple("3liga-muenster","SC Preußen Münster","PRE"),
        Triple("3liga-essen","Rot-Weiss Essen","RWE"),
        Triple("3liga-havelse","TSV Havelse","HAV"),
        Triple("3liga-regensburg","SSV Jahn Regensburg","REG"),
        Triple("3liga-stuttgart2","VfB Stuttgart II","VFB2"),
        Triple("3liga-duesseldorf","Fortuna Düsseldorf","F95"),
        Triple("3liga-grossaspach","SG Sonnenhof Großaspach","SGA"),
        Triple("3liga-wiesbaden","SV Wehen Wiesbaden","WEH"),
        Triple("3liga-verl","SC Verl","VER"),
    )

    private val regionalNames=listOf(
        Triple("rln-weiche","SC Weiche Flensburg 08","SCW"),
        Triple("rln-schoeningen","FSV Schöningen","FSVS"),
        Triple("rln-oldenburg","VfB Oldenburg","OLD"),
        Triple("rln-phoenix","1. FC Phönix Lübeck","PHL"),
        Triple("rln-drochtersen","SV Drochtersen/Assel","SVD"),
        Triple("rln-hannover2","Hannover 96 II","H962"),
        Triple("rln-emden","BSV Kickers Emden","BKE"),
        Triple("rln-atlas","SV Atlas Delmenhorst","SVA"),
        Triple("rln-norderstedt","FC Eintracht Norderstedt","ENO"),
        Triple("rln-hsc","HSC Hannover","HSC"),
        Triple("rln-eimsbuettel","Eimsbütteler TV","ETV"),
        Triple("rln-bremer","Bremer SV","BSV"),
        Triple("rln-todesfelde","SV Todesfelde","SVT"),
        Triple("rln-stpauli2","FC St. Pauli II","STP2"),
        Triple("rln-hsv2","Hamburger SV II","HSV2"),
        Triple("rln-werder2","SV Werder Bremen II","SVW2"),
        Triple("rln-luebeck","VfB Lübeck","VBL"),
        Triple("rln-jeddeloh","SSV Jeddeloh II","SSVJ"),
    )

    private val oberligaNames=listOf(
        Triple("olhh-dassendorf","TuS Dassendorf","TUS"),
        Triple("olhh-tbs","TBS Pinneberg","TBS"),
        Triple("olhh-sasel","TSV Sasel","SAS"),
        Triple("olhh-paloma","USC Paloma","USC"),
        Triple("olhh-victoria","SC Victoria Hamburg","SCV"),
        Triple("olhh-altona","Altona 93","AFC"),
        Triple("olhh-buchholz","TSV Buchholz 08","BU08"),
        Triple("olhh-suederelbe","FC Süderelbe","FCSU"),
        Triple("olhh-etsv","ETSV Hamburg","ETSV"),
        Triple("olhh-vorwaerts-wacker","SC Vorwärts-Wacker 04","VWA"),
        Triple("olhh-niendorf","Niendorfer TSV","NTSV"),
        Triple("olhh-ht16","HT 16 Hamburg","HT16"),
        Triple("olhh-hebc","HEBC Hamburg","HEBC"),
        Triple("olhh-nikola-tesla","SSG Nikola Tesla","SSG"),
        Triple("olhh-teutonia","FC Teutonia 05 Ottensen","T05"),
        Triple("olhh-harksheide","TuRa Harksheide","TURA"),
        Triple("olhh-concordia","SC Concordia Hamburg","SGC"),
        Triple("olhh-norderstedt2","FC Eintracht Norderstedt II","ENO2"),
    )

    private fun league(name:String,tier:Int,level:Int,seeds:List<Triple<String,String,String>>)=RealModeLeagueSeed(
        name=name,
        country="Deutschland",
        tier=tier,
        clubs=seeds.mapIndexed{i,(key,clubName,short)->club(key,clubName,short,name,i)},
        level=level,
    )

    val leagues:List<RealModeLeagueSeed> by lazy(LazyThreadSafetyMode.PUBLICATION){listOf(
        league("2. Bundesliga",2,2,secondNames),
        league("3. Liga",3,3,thirdNames),
        league("Regionalliga Nord",4,4,regionalNames),
        league("Oberliga Hamburg",5,5,oberligaNames),
    )}
}

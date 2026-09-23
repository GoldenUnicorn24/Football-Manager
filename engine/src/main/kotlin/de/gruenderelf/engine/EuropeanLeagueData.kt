package de.gruenderelf.engine

/**
 * Club selection for additional playable countries. First divisions in Croatia and Russia
 * follow the 2026/27 federation/league lists; other and lower divisions are representative
 * club selections. Generated players are explicitly distinguished from verified roster seeds.
 */
object EuropeanLeagueData {
 data class Country(val name:String,val cup:String,val divisions:List<Pair<String,List<String>>>)
 private fun names(text:String)=text.split("|").map(String::trim).filter(String::isNotEmpty)
 val countries=listOf(
  Country("Kroatien","Hrvatski kup",listOf(
   "SuperSport HNL" to names("Dinamo Zagreb|HNK Gorica|Hajduk Split|Istra 1961|Lokomotiva Zagreb|NK Osijek|HNK Rijeka|NK Rudeš|Slaven Belupo|NK Varaždin"),
   "Prva NL" to names("Cibalia Vinkovci|NK Dugopolje|NK Jarun|NK Sesvete|NK Dubrava|NK Opatija|HNK Orijent|NK BSK Bijelo Brdo"),
   "Druga NL" to names("NK Kustošija|NK Solin|NK Jadran Poreč|NK Mladost Ždralovi|NK Hrvatski Dragovoljac|NK Uljanik|NK Dugo Selo|NK Grobničan"))),
  Country("Russland","Kubok Rossii",listOf(
   "Premjer-Liga" to names("FK Krasnodar|Spartak Moskau|Zenit Sankt Petersburg|Dynamo Moskau|ZSKA Moskau|Rubin Kasan|Dynamo Machatschkala|Baltika Kaliningrad|Krylja Sowetow Samara|Achmat Grosny|FK Orenburg|FK Rostow|Lokomotive Moskau|Akron Toljatti|FK Sotschi|Pari Nischni Nowgorod"),
   "Perwaja Liga" to names("Rotor Wolgograd|Torpedo Moskau|Ural Jekaterinburg|Arsenal Tula|Schinnik Jaroslawl|SKA Chabarowsk|Jenissei Krasnojarsk|Rodina Moskau"),
   "Wtoraja Liga" to names("Tekstilschtschik Iwanowo|Dynamo Kirow|FK Tscheljabinsk|FK Murom|Dynamo Brjansk|FK Kaluga|Dynamo Stawropol|FK Rjasan"))),
  Country("Niederlande","KNVB Beker",listOf(
   "Eredivisie" to names("Ajax Amsterdam|PSV Eindhoven|Feyenoord Rotterdam|AZ Alkmaar|FC Utrecht|FC Twente|SC Heerenveen|FC Groningen|NEC Nijmegen|Sparta Rotterdam"),
   "Eerste Divisie" to names("Willem II|Roda JC|ADO Den Haag|De Graafschap|FC Dordrecht|FC Emmen|FC Eindhoven|MVV Maastricht"),
   "Tweede Divisie" to names("AFC Amsterdam|Katwijk|Rijnsburgse Boys|Koninklijke HFC|Spakenburg|HHC Hardenberg|Quick Boys|De Treffers"))),
  Country("Portugal","Taça de Portugal",listOf(
   "Primeira Liga" to names("Sporting CP|SL Benfica|FC Porto|SC Braga|Vitória SC|Famalicão|Gil Vicente|Rio Ave|Casa Pia|Estoril Praia"),
   "Liga Portugal 2" to names("Académico de Viseu|Leixões SC|CD Feirense|FC Penafiel|UD Oliveirense|CD Tondela|FC Vizela|SC Farense"),
   "Liga 3" to names("Académica Coimbra|União de Leiria|Varzim SC|SC Covilhã|Amora FC|Alverca|Canelas 2010|Lusitânia Lourosa"))),
  Country("Belgien","Croky Cup",listOf(
   "Pro League" to names("Club Brugge|Union Saint-Gilloise|RSC Anderlecht|KRC Genk|Royal Antwerp|KAA Gent|Standard Lüttich|KV Mechelen|Cercle Brugge|OH Leuven"),
   "Challenger Pro League" to names("SK Beveren|RWDM|Lierse SK|Patro Eisden|RFC Liège|KAS Eupen|Lommel SK|KMSK Deinze"),
   "Nationale 1" to names("Rupel Boom|Dessel Sport|KSK Heist|Knokke FC|Olympic Charleroi|UR Namur|Tienen|Hoogstraten VV"))),
  Country("Österreich","ÖFB-Cup",listOf(
   "Bundesliga Österreich" to names("RB Salzburg|Sturm Graz|Rapid Wien|Austria Wien|LASK|Wolfsberger AC|TSV Hartberg|Blau-Weiß Linz|SCR Altach|WSG Tirol"),
   "2. Liga Österreich" to names("Admira Wacker|SV Ried|First Vienna FC|SKN St. Pölten|Kapfenberger SV|Floridsdorfer AC|SV Lafnitz|FC Liefering"),
   "Regionalliga Ost" to names("Wiener Sport-Club|Kremser SC|FC Mauerwerk|SC Wiener Viktoria|ASV Siegendorf|TWL Elektra|FC Marchfeld|SV Leobendorf"))),
  Country("Schweiz","Schweizer Cup",listOf(
   "Super League Schweiz" to names("Young Boys Bern|FC Basel|FC Zürich|FC Lugano|Servette FC|FC Luzern|FC St. Gallen|FC Lausanne-Sport|FC Winterthur|Grasshopper Club Zürich"),
   "Challenge League" to names("FC Aarau|FC Thun|FC Vaduz|Neuchâtel Xamax|FC Wil|Stade Lausanne Ouchy|FC Schaffhausen|AC Bellinzona"),
   "Promotion League" to names("SC Brühl|FC Biel-Bienne|FC Breitenrain|SC Cham|FC Bavois|FC Baden|FC Rapperswil-Jona|FC Luzern II"))),
  Country("Türkei","Türkiye Kupası",listOf(
   "Süper Lig" to names("Galatasaray|Fenerbahçe|Beşiktaş|Trabzonspor|Başakşehir|Samsunspor|Göztepe|Kasımpaşa|Antalyaspor|Konyaspor"),
   "1. Lig Türkei" to names("Sakaryaspor|Erzurumspor|Boluspor|Bandırmaspor|Ümraniyespor|Manisa FK|Gençlerbirliği|Adanaspor"),
   "2. Lig Türkei" to names("Bursaspor|Ankaraspor|İnegölspor|Altınordu|Karacabey Belediye|İskenderunspor|Afyonspor|Somaspor"))),
  Country("Schottland","Scottish Cup",listOf(
   "Scottish Premiership" to names("Celtic|Rangers|Aberdeen|Heart of Midlothian|Hibernian|Dundee United|Motherwell|St Mirren|Kilmarnock|Dundee FC"),
   "Scottish Championship" to names("Partick Thistle|Ayr United|Greenock Morton|Queen's Park|Raith Rovers|Dunfermline Athletic|Airdrieonians|Falkirk"),
   "Scottish League One" to names("Alloa Athletic|Cove Rangers|Montrose|Kelty Hearts|Queen of the South|Stenhousemuir|Inverness CT|Arbroath"))),
  Country("Griechenland","Kypello Elladas",listOf(
   "Super League Griechenland" to names("Olympiakos|Panathinaikos|AEK Athen|PAOK|Aris Thessaloniki|OFI Kreta|Atromitos|Asteras Tripolis|Panetolikos|Volos NFC"),
   "Super League 2" to names("AE Larisa|Iraklis|Kalamata|Panionios|PAS Giannina|Niki Volos|Egaleo|Kissamikos"),
   "Gamma Ethniki" to names("Ethnikos Piräus|Panthrakikos|Aris Petroupolis|Thyella Rafinas|Fostiras|Apollon Paralimnio|AO Chania|Kavala"))),
  Country("Dänemark","DBU Pokalen",listOf(
   "Superliga Dänemark" to names("FC Kopenhagen|FC Midtjylland|Brøndby IF|AGF Aarhus|FC Nordsjælland|Silkeborg IF|Randers FC|Viborg FF|Aalborg BK|Sønderjyske"),
   "1. Division Dänemark" to names("Odense BK|AC Horsens|Kolding IF|Hillerød Fodbold|Hobro IK|Esbjerg fB|Fredericia|HB Køge"),
   "2. Division Dänemark" to names("AB Gladsaxe|Fremad Amager|Næstved BK|Aarhus Fremad|FC Helsingør|Skive IK|Brabrand IF|Ishøj IF"))),
  Country("Polen","Puchar Polski",listOf(
   "Ekstraklasa" to names("Legia Warschau|Lech Posen|Raków Częstochowa|Jagiellonia Białystok|Pogoń Szczecin|Śląsk Wrocław|Wisła Płock|Cracovia|Górnik Zabrze|Widzew Łódź"),
   "I liga Polen" to names("Wisła Krakau|Ruch Chorzów|ŁKS Łódź|Arka Gdynia|GKS Tychy|Stal Rzeszów|Miedź Legnica|Odra Opole"),
   "II liga Polen" to names("Zagłębie Sosnowiec|KKS Kalisz|Olimpia Grudziądz|Chojniczanka|Hutnik Kraków|Resovia|Polonia Bytom|Podbeskidzie"))),
  Country("Tschechien","MOL Cup",listOf(
   "Chance Liga" to names("Slavia Prag|Sparta Prag|Viktoria Pilsen|Baník Ostrava|Sigma Olomouc|Slovan Liberec|FK Jablonec|Mladá Boleslav|Bohemians Prag|FC Hradec Králové"),
   "Chance Národní Liga" to names("Zbrojovka Brno|Dukla Prag|Vyškov|Opava|Vlašim|Táborsko|Chrudim|Prostějov"),
   "ČFL" to names("Viktoria Žižkov|Motorlet Prag|Slovan Velvary|Admira Prag|Sokol Brozany|Jiskra Domažlice|Povltavská FA|Králův Dvůr"))),
  Country("Serbien","Kup Srbije",listOf(
   "SuperLiga Srbije" to names("Roter Stern Belgrad|Partizan Belgrad|Vojvodina Novi Sad|TSC Bačka Topola|Čukarički|Radnički Niš|Novi Pazar|Mladost Lučani|Spartak Subotica|OFK Beograd"),
   "Prva Liga Srbije" to names("Javor Ivanjica|Radnik Surdulica|Mačva Šabac|Borac Čačak|Grafičar|Dubocica|Smederevo|Inđija"),
   "Srpska Liga" to names("Zemun|Radnički Obrenovac|Teleoptik|Jedinstvo Surčin|BASK|Sinđelić Beograd|Budućnost Dobanovci|Prva Iskra"))),
  Country("Schweden","Svenska Cupen",listOf(
   "Allsvenskan" to names("Mjällby AIF|BK Häcken|GAIS|IFK Göteborg|Örgryte IS|Halmstads BK|Malmö FF|Kalmar FF|AIK|Djurgårdens IF|Hammarby IF|IF Brommapojkarna|IK Sirius|IF Elfsborg|Västerås SK|Degerfors IF"),
   "Superettan" to names("IK Oddevold|Ljungskile SK|IK Brage|Sandvikens IF|Falkenbergs FF|Varbergs BoIS|Östersunds FK|GIF Sundsvall|Helsingborgs IF|Landskrona BoIS|IFK Värnamo|Östers IF|Nordic United FC|Norrby IF|Örebro SK|IFK Norrköping"),
   "Ettan Norra" to names("AFC Eskilstuna|Assyriska FF|Enköpings SK|FBK Karlstad|FC Arlanda|FC Järfälla|FC Stockholm|Gefle IF|Hammarby TFF|IF Karlstad Fotboll|IFK Stocksund|Karlbergs BK|Piteå IF|Sollentuna FK|Umeå FC|Vasalund IF"))),
  Country("Norwegen","Norgesmesterskapet",listOf(
   "Eliteserien" to names("Viking FK|FK Bodø/Glimt|Tromsø IL|Molde FK|Rosenborg BK|Lillestrøm SK|SK Brann|Fredrikstad FK|Sarpsborg 08|KFUM Oslo|Sandefjord Fotball|Vålerenga|HamKam|Aalesunds FK|Kristiansund BK|IK Start"),
   "OBOS-ligaen" to names("FK Haugesund|Kongsvinger IL|Strømsgodset IF|Stabæk Fotball|Odds BK|Bryne FK|IL Hødd|Egersunds IK|Ranheim TF|Lyn 1896|Moss FK|Strømmen IF|Sandnes Ulf|Sogndal IL|Åsane Fotball|Raufoss IL"),
   "PostNord-ligaen avdeling 1" to names("Lysekloster IL|Sotra SK|IL Sandviken|Pors Fotball|Notodden FK|FK Jerv|Arendal Fotball|FK Eik Tønsberg|Mjøndalen IF|IL Bjarg|SK Vidar|Kvik Halden FK|Brattvåg IL|SK Træff"))),
  Country("Ukraine","Kubok Ukrainy",listOf(
   "Premjer-Liha" to names("FC Bukovyna Chernivtsi|NK Veres Rivne|Dynamo Kyiv|FC Epicentr|Zorya Luhansk|Karpaty Lviv|Kolos Kovalivka|Kryvbas Kryvyi Rih|Kudrivka|Livyi Bereh Kyiv|LNZ Cherkasy|Obolon Kyiv|Polissya Zhytomyr|Metalist 1925 Kharkiv|Chornomorets Odesa|Shakhtar Donetsk"),
   "Perscha Liha" to names("Inhulets Petrove|FC Probiy Horodenka|Prykarpattia Ivano-Frankivsk|YUKSA Tarasivka|Nyva Ternopil|FC Minaj|FC Ahrobiznes Volochysk|Viktoriya Sumy"),
   "Druha Liha" to names("FC Trostianets|FC Lokomotyv Kyiv|FC Oleksandriya-2|FC Chayka|FC Kulikiv|FC Skala 1911 Stryi|FC Vilkhivtsi|FC Uzhhorod"))),
  Country("Rumänien","Cupa României",listOf(
   "SuperLiga Rumänien" to names("FCSB|CFR Cluj|Universitatea Craiova|Farul Constanța|Sepsi OSK|Rapid București|UTA Arad|Oțelul Galați|Universitatea Cluj|Petrolul Ploiești|Corvinul Hunedoara|Csikszereda Miercurea Ciuc|Dinamo București|FC Argeș|FC Botoșani|FC Voluntari"),
   "Liga 2 Rumänien" to names("Steaua București|FC Bihor Oradea|Chindia Târgoviște|Concordia Chiajna|CSM Reșița|Gloria Bistrița|Politehnica Iași|ASA Târgu Mureș"),
   "Liga 3 Rumänien" to names("CS Tunari|CS Afumați|CSM Slatina|Unirea Dej|Metalul Buzău|CSM Focșani|Unirea Alba Iulia|CS Blejoi")))
 )
 private fun slug(name:String)=name.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')
 val leagues:List<RealModeLeagueSeed> by lazy {
  countries.flatMapIndexed { countryIndex,country ->
   country.divisions.mapIndexedNotNull { index,(leagueName,clubNames) ->
    val existing=InternationalLeagueData.leagues.filter{it.country==country.name}
    if(index==0&&existing.any{it.level==1})return@mapIndexedNotNull null
    val uniqueNames=clubNames.filter{name->existing.none{league->league.clubs.any{it.name==name}}}
    val playableNames=uniqueNames.take(uniqueNames.size-(uniqueNames.size%2))
    if(playableNames.size<6)return@mapIndexedNotNull null
    val tier=100+countryIndex*3+index
    RealModeLeagueSeed(leagueName,country.name,tier,playableNames.mapIndexed { clubIndex,name ->
     RealModeClubSeed("${slug(country.name)}-$index-$clubIndex",name,name.filter(Char::isLetterOrDigit).take(3).uppercase(),leagueName,country.name,
      listOf(0xFFB32025,0xFF253E71,0xFF327353,0xFF202020)[clubIndex%4],0xFFEDEDE7,
      if(country.name=="Ukraine"&&index==0)UkrainianSquadData.playersFor(name) else emptyList())
    },index+1)
   }
  }
 }
 fun cupFor(country:String)=countries.firstOrNull{it.name==country}?.cup
}

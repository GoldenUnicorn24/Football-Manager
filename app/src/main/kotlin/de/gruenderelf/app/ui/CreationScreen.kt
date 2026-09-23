package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.app.data.SaveSummary
import de.gruenderelf.engine.*
import kotlinx.serialization.builtins.ListSerializer

private fun sandboxTemplate(index: Int): PlayerDraft {
 val positions=listOf(Position.TW,Position.IV,Position.IV,Position.LV,Position.RV,Position.DM,Position.ZM,Position.OM,Position.LA,Position.RA,Position.ST,Position.ST,Position.IV,Position.ZM,Position.DM,Position.LA,Position.RA,Position.ST,Position.TW)
 val pos=positions[index.coerceIn(0,positions.lastIndex)]
 val a=if(pos==Position.TW)Attributes(45,25,52,48,42,54,58,50,35,68,42) else Attributes(55,52,54,54,52,53,58,53,50,12,48)
 return PlayerDraft(firstName="Spieler",lastName=(index+2).toString().padStart(2,'0'),birthYear=2000-index%10,position=pos,number=(index+2).coerceAtMost(99),attributes=a)
}

@Composable private fun AttributeSlider(label: String,value: Int,onChange: (Int)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Muted);Text("$value",color=Grass)}
  Slider(value.toFloat(),{onChange(it.toInt().coerceIn(1,99))},valueRange=1f..99f,steps=97)
 }
}

@Composable private fun AttributeEditor(p: PlayerDraft,onChange: (PlayerDraft)->Unit){
 val a=p.attributes
 Section("Individuelle Attribute"){
  Text("Sandbox: Jeder Wert kann frei von 1 bis 99 gesetzt werden.",color=Muted,style=MaterialTheme.typography.bodySmall)
  AttributeSlider("Tempo",a.pace){onChange(p.copy(attributes=a.copy(pace=it)))}
  AttributeSlider("Abschluss",a.finishing){onChange(p.copy(attributes=a.copy(finishing=it)))}
  AttributeSlider("Pässe",a.passing){onChange(p.copy(attributes=a.copy(passing=it)))}
  AttributeSlider("Technik",a.technique){onChange(p.copy(attributes=a.copy(technique=it)))}
  AttributeSlider("Zweikampf",a.tackling){onChange(p.copy(attributes=a.copy(tackling=it)))}
  AttributeSlider("Kraft",a.strength){onChange(p.copy(attributes=a.copy(strength=it)))}
  AttributeSlider("Ausdauer",a.stamina){onChange(p.copy(attributes=a.copy(stamina=it)))}
  AttributeSlider("Spielwitz",a.vision){onChange(p.copy(attributes=a.copy(vision=it)))}
  AttributeSlider("Kopfball",a.heading){onChange(p.copy(attributes=a.copy(heading=it)))}
  AttributeSlider("Torwart",a.keeping){onChange(p.copy(attributes=a.copy(keeping=it)))}
  AttributeSlider("Standards",a.setPieces){onChange(p.copy(attributes=a.copy(setPieces=it)))}
 }
}

@Composable private fun PlayerDraftEditor(p: PlayerDraft,onChange: (PlayerDraft)->Unit,showAppearance: Boolean=true){
 Field("Vorname",p.firstName){onChange(p.copy(firstName=it.take(30)))}
 Field("Nachname",p.lastName){onChange(p.copy(lastName=it.take(30)))}
 Field("Geburtsjahr · 1976–2008",p.birthYear.toString()){if(it.length<=4)onChange(p.copy(birthYear=it.toIntOrNull()?:0))}
 Field("Nationalität",p.nationality){onChange(p.copy(nationality=it.take(40)))}
 Field("Größe in cm",p.height.toString()){if(it.length<=3)onChange(p.copy(height=it.toIntOrNull()?:0))}
 Field("Gewicht in kg",p.weight.toString()){if(it.length<=3)onChange(p.copy(weight=it.toIntOrNull()?:0))}
 Pick("Starker Fuß",p.foot,Foot.entries.toList(),{it.label}){onChange(p.copy(foot=it))}
 Pick("Hauptposition",p.position,Position.entries.toList(),{it.label}){onChange(p.copy(position=it,secondary=p.secondary.filter{pos->pos!=it}.toMutableList()))}
 Text("Nebenpositionen",color=Muted)
 Position.entries.filter{it!=p.position}.forEach{pos->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pos in p.secondary,{checked->onChange(p.copy(secondary=p.secondary.toMutableList().apply{if(checked)add(pos)else remove(pos)}))});Text(pos.label)}}
 Field("Rückennummer · 1–99",p.number.toString()){if(it.length<=2)onChange(p.copy(number=it.toIntOrNull()?:0))}
 Pick("Spielertyp",p.archetype,listOf("Komplett","Schnell","Technisch","Physisch","Abschluss")){onChange(p.copy(archetype=it))}
 if(showAppearance)Section("Aussehen"){
  Pick("Hautton",p.appearance.skin,(0..4).toList(),{listOf("Sehr hell","Hell","Mittel","Dunkel","Sehr dunkel")[it]}){onChange(p.copy(appearance=p.appearance.copy(skin=it)))}
  Pick("Haare",p.appearance.hair,(0..4).toList(),{listOf("Blond","Braun","Schwarz","Rot","Rasiert")[it]}){onChange(p.copy(appearance=p.appearance.copy(hair=it)))}
  Pick("Bart",p.appearance.beard,(0..2).toList(),{listOf("Kein Bart","Kurzer Bart","Vollbart")[it]}){onChange(p.copy(appearance=p.appearance.copy(beard=it)))}
  Pick("Statur",p.appearance.build,(0..2).toList(),{listOf("Schlank","Normal","Kräftig")[it]}){onChange(p.copy(appearance=p.appearance.copy(build=it)))}
 }
}


@Composable private fun TakeoverCreationScreen(vm: GameViewModel,slots: List<SaveSummary>,busy: Boolean,onBack: ()->Unit){
 val countries=RealModeDatabase.countries
 var country by rememberSaveable{mutableStateOf(countries.first())}
 val leagues=RealModeDatabase.leaguesForCountry(country)
 var leagueName by rememberSaveable(country){mutableStateOf(leagues.first().name)}
 val league=leagues.firstOrNull{it.name==leagueName}?:leagues.first()
 var clubKey by rememberSaveable(league.name){mutableStateOf(league.clubs.first().key)}
 val options=league.clubs
 if(options.none{it.key==clubKey})clubKey=options.first().key
 var pj by rememberSaveable{mutableStateOf(SaveCodec.json.encodeToString(PlayerDraft.serializer(),PlayerDraft()))}
 val p=remember(pj){SaveCodec.json.decodeFromString(PlayerDraft.serializer(),pj)}
 fun person(v: PlayerDraft){pj=SaveCodec.json.encodeToString(PlayerDraft.serializer(),v)}
 var page by rememberSaveable{mutableIntStateOf(0)}
 var saveSlot by rememberSaveable{mutableIntStateOf((1..5).firstOrNull{i->slots.none{it.slot==i}}?:1)}
 var fantasyCupEnabled by rememberSaveable{mutableStateOf(true)}
 var confirm by remember{mutableStateOf(false)}
 val selected=options.first{it.key==clubKey}
 val leagueGames=(options.size-1)*2
 val titles=listOf("Verein auswählen","Dein Spieler","Karriere starten")
 Page(titles[page],"VEREIN ÜBERNEHMEN · ${page+1} / ${titles.size}"){
  LinearProgressIndicator(progress={(page+1)/titles.size.toFloat()},modifier=Modifier.fillMaxWidth())
  when(page){
   0->{
    Section("Land, Liga und Verein"){
     Text("Wähle zuerst das Land, danach die verfügbare Liga und schließlich deinen Verein. Deutschland reicht bis zur Oberliga Hamburg.",color=Muted)
     Pick("Land",country,countries,{it}){country=it}
     Pick("Liga",league.name,leagues.map{it.name},{it}){leagueName=it}
     Pick("Verein übernehmen",clubKey,options.map{it.key},{key->options.first{it.key==key}.let{"${it.shortName} · ${it.name}"}}){clubKey=it}
     Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){
      Crest(Logo(options.indexOf(selected)%8,selected.shortName),selected.primary,selected.secondary,Modifier.size(82.dp))
      Column{Text(selected.name,style=MaterialTheme.typography.titleLarge);Text("${selected.country} · ${selected.league}",color=Muted);Text("${selected.shortName} · $leagueGames Spieltage",color=Grass)}
     }
    }
    Section("Kader"){
     if(selected.players.isEmpty()){
      Text("Für diese Liga sind Verein und Wettbewerb real hinterlegt. Der Spielerkader wird beim Karrierestart passend zum Ligenniveau generiert.",color=Muted)
     }else selected.players.groupBy{it.position}.forEach{(pos,players)->
      Text(pos.label,color=Grass,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
      Text(players.joinToString(" · "){"${it.number} ${it.name}"},color=Muted,style=MaterialTheme.typography.bodySmall)
     }
     Text("Ratings sind interne Gründerelf-Schätzwerte und keine offiziellen Spieldaten.",color=Muted,style=MaterialTheme.typography.labelSmall)
    }
   }
   1->{
    Section("Dich zusätzlich in den Kader aufnehmen"){
     Text("Du wirst als zusätzlicher Spieler des ausgewählten Vereins eingefügt; kein vorhandener Spieler wird dafür entfernt.",color=Muted)
     PlayerDraftEditor(p,{person(it)},false)
    }
    AttributeEditor(p){person(it)}
   }
   else->{
    Section(selected.name){
     Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){Crest(Logo(options.indexOf(selected)%8,selected.shortName),selected.primary,selected.secondary,Modifier.size(72.dp));Column{Text("${selected.shortName} · ${selected.name}",style=MaterialTheme.typography.titleMedium);Text("${selected.league} · $leagueGames Spieltage",color=Grass)}}
     Text("Eigener Spieler: ${p.firstName.ifBlank{"Vorname fehlt"}} ${p.lastName} · ${p.position.label} · Nr. ${p.number}")
     Section("Zusatzwettbewerb"){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
       Column(Modifier.weight(1f)){Text("Krone der Kontinente");Text("Fantasy-Cup mit den Plätzen 1–6 aus jeder integrierten Liga. Kann nur für diese Karriere ein- oder ausgeschaltet werden.",color=Muted,style=MaterialTheme.typography.bodySmall)}
       Switch(fantasyCupEnabled,{fantasyCupEnabled=it})
      }
     }
     Pick("Speicherplatz",saveSlot,(1..5).toList(),{i->"Slot $i · ${slots.firstOrNull{it.slot==i}?.clubName?:"Leer"}"}){saveSlot=it}
     Text("Alle integrierten Ligen werden parallel simuliert. Deutsche Vereine können innerhalb der fünf deutschen Spielklassen auf- und absteigen.",color=Muted)
     Action("Karriere starten",!busy){if(slots.any{it.slot==saveSlot})confirm=true else vm.createRealMode(System.currentTimeMillis(),clubKey,p,saveSlot,fantasyCupEnabled)}
    }
   }
  }
  if(page<titles.lastIndex)Action("Weiter",!busy){page++}
  Action(if(page==0)"Zurück zur Moduswahl" else "Zurück",!busy,secondary=true){if(page==0)onBack()else page--}
 }
 if(confirm)Confirm("Slot $saveSlot überschreiben?","Der bisherige Spielstand in diesem Slot wird ersetzt.",{confirm=false}){confirm=false;vm.createRealMode(System.currentTimeMillis(),clubKey,p,saveSlot,fantasyCupEnabled)}
}

@Composable private fun CustomClubCreationScreen(vm: GameViewModel,slots: List<SaveSummary>,busy: Boolean,onBack: ()->Unit){
 val countries=RealModeDatabase.countries
 var country by rememberSaveable{mutableStateOf(countries.first())}
 val leagues=RealModeDatabase.leaguesForCountry(country)
 var leagueName by rememberSaveable(country){mutableStateOf(leagues.first().name)}
 val league=leagues.firstOrNull{it.name==leagueName}?:leagues.first()
 var leagueSlotClubKey by rememberSaveable(league.name){mutableStateOf(league.clubs.first().key)}
 if(league.clubs.none{it.key==leagueSlotClubKey})leagueSlotClubKey=league.clubs.first().key

 var cj by rememberSaveable{mutableStateOf(SaveCodec.json.encodeToString(ClubDraft.serializer(),ClubDraft()))}
 var pj by rememberSaveable{mutableStateOf(SaveCodec.json.encodeToString(PlayerDraft.serializer(),PlayerDraft()))}
 val listSer=remember{ListSerializer(PlayerDraft.serializer())}
 var sj by rememberSaveable{mutableStateOf(SaveCodec.json.encodeToString(listSer,List(19){sandboxTemplate(it)}))}
 val c=remember(cj){SaveCodec.json.decodeFromString(ClubDraft.serializer(),cj)}
 val p=remember(pj){SaveCodec.json.decodeFromString(PlayerDraft.serializer(),pj)}
 val rosterPlayers=remember(sj){SaveCodec.json.decodeFromString(listSer,sj)}
 fun club(v:ClubDraft){cj=SaveCodec.json.encodeToString(ClubDraft.serializer(),v)}
 fun person(v:PlayerDraft){pj=SaveCodec.json.encodeToString(PlayerDraft.serializer(),v)}
 fun roster(v:List<PlayerDraft>){sj=SaveCodec.json.encodeToString(listSer,v)}

 val titles=listOf("Land & Liga","Dein Verein","Wappen & Trikots","Dein Spieler","Kader erstellen","Karriere starten")
 var page by rememberSaveable{mutableIntStateOf(0)}
 var saveSlot by rememberSaveable{mutableIntStateOf((1..5).firstOrNull{i->slots.none{it.slot==i}}?:1)}
 var fantasyCupEnabled by rememberSaveable{mutableStateOf(true)}
 var confirm by remember{mutableStateOf(false)}
 var clothing by rememberSaveable{mutableStateOf("Heim")}
 var rosterIndex by rememberSaveable{mutableIntStateOf(0)}
 val replacedClub=league.clubs.first{it.key==leagueSlotClubKey}
 val capacityOptions=listOf(100,250,500,1000,2500,5000,10000,20000,35000,50000,75000,100000)

 Page(titles[page],"INDIVIDUELLER VEREIN · ${page+1} / ${titles.size}"){
  LinearProgressIndicator(progress={(page+1)/titles.size.toFloat()},modifier=Modifier.fillMaxWidth())
  when(page){
   0->Section("Wo soll dein Verein starten?"){
    Pick("Land",country,countries,{it}){country=it}
    Pick("Liga",league.name,leagues.map{it.name},{it}){leagueName=it}
    Pick("Liga-Platz",leagueSlotClubKey,league.clubs.map{it.key},{key->league.clubs.first{it.key==key}.let{"${it.shortName} · ${it.name}"}}){leagueSlotClubKey=it}
    Text("Dein neuer Verein übernimmt den Ligaplatz von ${replacedClub.name}. Dadurch bleiben Ligagröße, Spielplan, Pokale und Tabellen mathematisch korrekt; alle übrigen Vereine bleiben erhalten.",color=Muted)
   }
   1->{
    Section("Verein komplett anpassen"){
     Field("Vereinsname",c.name){club(c.copy(name=it.take(40)))}
     Field("Kürzel · 2–4 Zeichen",c.shortName){club(c.copy(shortName=it.uppercase().take(4),logo=c.logo.copy(letters=it.uppercase().take(4))))}
     Field("Stadt / Ort",c.city){club(c.copy(city=it.take(40)))}
     Field("Gründungsjahr",c.founded.toString()){if(it.length<=4)club(c.copy(founded=it.toIntOrNull()?:0))}
     Field("Stadionname",c.stadiumName){club(c.copy(stadiumName=it.take(50)))}
     Pick("Kapazität",c.capacity,capacityOptions,{"$it Zuschauer"}){club(c.copy(capacity=it))}
     Pick("Schwierigkeit",c.difficulty,Difficulty.entries.toList(),{it.label}){club(c.copy(difficulty=it))}
     Pick("Vereinsphilosophie",c.philosophy,listOf("Zusammenhalt","Leistung","Offener Verein")){club(c.copy(philosophy=it))}
     Pick("Spielphilosophie",c.playPhilosophy,listOf("Direkt nach vorn","Ballbesitz","Kampf und Ordnung")){club(c.copy(playPhilosophy=it))}
     Pick("Jugendphilosophie",c.youthPhilosophy,listOf("Talente aus dem Ort","Breitensport","Leistungsförderung")){club(c.copy(youthPhilosophy=it))}
    }
   }
   2->{
    Section("Wappen"){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Crest(c.logo,c.primary,c.secondary,Modifier.size(115.dp))}
     Pick("Wappenvorlage",c.logo.template,(0..7).toList(),{"Vorlage ${it+1}"}){club(c.copy(logo=c.logo.copy(template=it)))}
     Field("Buchstaben im Wappen",c.logo.letters){club(c.copy(logo=c.logo.copy(letters=it.uppercase().take(4))))}
     ColorPicker("Primärfarbe",c.primary){club(c.copy(primary=it))};ColorPicker("Sekundärfarbe",c.secondary){club(c.copy(secondary=it))}
    }
    Section("Trikots"){
     Pick("Kleidung",clothing,listOf("Heim","Auswärts","Drittes Trikot","Torwart","Training")){clothing=it}
     val kit=when(clothing){"Auswärts"->c.kits.away;"Drittes Trikot"->c.kits.third?:Kit();"Torwart"->c.kits.keeper;"Training"->c.kits.training;else->c.kits.home}
     fun change(k:Kit){club(c.copy(kits=when(clothing){"Auswärts"->c.kits.copy(away=k);"Drittes Trikot"->c.kits.copy(third=k);"Torwart"->c.kits.copy(keeper=k);"Training"->c.kits.copy(training=k);else->c.kits.copy(home=k)}))}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Shirt(kit,Modifier.size(130.dp))}
     Pick("Trikotmuster",kit.pattern,(0..3).toList(),{listOf("Einfarbig","Längsstreifen","Schärpe","Brustring")[it]}){change(kit.copy(pattern=it))}
     ColorPicker("Stofffarbe",kit.primary){change(kit.copy(primary=it))};ColorPicker("Kontrastfarbe",kit.secondary){change(kit.copy(secondary=it))}
    }
   }
   3->{Section("Spielertrainer"){PlayerDraftEditor(p,{person(it)},false);PlayerPortrait(p.appearance,c.kits.home,Modifier.size(120.dp))};AttributeEditor(p){person(it)}}
   4->{
    val safe=rosterIndex.coerceIn(0,rosterPlayers.lastIndex)
    val selected=rosterPlayers[safe]
    Section("20er-Kader · du + 19 Spieler"){
     Text("Jeder Mitspieler kann einzeln benannt, positioniert und mit eigenen Attributen ausgestattet werden.",color=Muted)
     Pick("Spieler bearbeiten",safe,rosterPlayers.indices.toList(),{"${it+2}. ${rosterPlayers[it].firstName} ${rosterPlayers[it].lastName} · ${rosterPlayers[it].position.label}"}){rosterIndex=it}
    }
    Section("Spieler ${safe+2}"){PlayerDraftEditor(selected,{changed->roster(rosterPlayers.toMutableList().apply{this[safe]=changed})},false)}
    AttributeEditor(selected){changed->roster(rosterPlayers.toMutableList().apply{this[safe]=changed})}
   }
   else->Section(c.name){
    Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){Crest(c.logo,c.primary,c.secondary);Column{Text("${c.city.ifBlank{"Ort noch offen"}} · seit ${c.founded}");Text("${league.country} · ${league.name}",color=Grass);Text("${c.capacity} Plätze",color=Muted)}}
    Text("Spielertrainer: ${p.firstName.ifBlank{"Vorname fehlt"}} ${p.lastName} · ${p.position.label} · Nr. ${p.number}")
    Text("19 individuell editierbare Mitspieler · ersetzt Ligaplatz ${replacedClub.shortName}",color=Muted)
    Section("Zusatzwettbewerb"){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Column(Modifier.weight(1f)){Text("Krone der Kontinente");Text("Fantasy-Cup mit den Plätzen 1–6 aus jeder integrierten Liga.",color=Muted,style=MaterialTheme.typography.bodySmall)}
      Switch(fantasyCupEnabled,{fantasyCupEnabled=it})
     }
    }
    Pick("Speicherplatz",saveSlot,(1..5).toList(),{i->"Slot $i · ${slots.firstOrNull{it.slot==i}?.clubName?:"Leer"}"}){saveSlot=it}
    Action("Individuellen Verein starten",!busy){if(slots.any{it.slot==saveSlot})confirm=true else vm.createCustomReal(System.currentTimeMillis(),leagueSlotClubKey,c,p,rosterPlayers,saveSlot,fantasyCupEnabled)}
   }
  }
  if(page<titles.lastIndex)Action("Weiter",!busy){page++}
  Action(if(page==0)"Zurück zur Moduswahl" else "Zurück",!busy,secondary=true){if(page==0)onBack() else page--}
 }
 if(confirm)Confirm("Slot $saveSlot überschreiben?","Der bisherige Spielstand in diesem Slot wird ersetzt.",{confirm=false}){confirm=false;vm.createCustomReal(System.currentTimeMillis(),leagueSlotClubKey,c,p,rosterPlayers,saveSlot,fantasyCupEnabled)}
}

@Composable fun CreationScreen(vm: GameViewModel,slots: List<SaveSummary>,busy: Boolean,onBack: ()->Unit){
 var mode by rememberSaveable{mutableStateOf("choose")}
 when(mode){
  "takeover"->{TakeoverCreationScreen(vm,slots,busy){mode="choose"};return}
  "custom"->{CustomClubCreationScreen(vm,slots,busy){mode="choose"};return}
 }
 Page("Neue Karriere","SPIELMODUS"){
  Section("Wie möchtest du starten?"){
   Text("Wähle zwischen einem bestehenden Verein oder einem vollständig eigenen Klub in einer frei gewählten integrierten Liga.",color=Muted)
   Action("Verein übernehmen",!busy){mode="takeover"}
   Action("Individuellen Verein erstellen",!busy,secondary=true){mode="custom"}
  }
  Section("Enthaltene Ligen"){
   val realRosterClubs=RealModeDatabase.options.count{it.players.isNotEmpty()};val simulatedRosterClubs=RealModeDatabase.options.size-realRosterClubs
   Text("${RealModeDatabase.countries.size} Länder mit ${RealModeDatabase.leagues.size} spielbaren Ligen.",color=Muted)
   Text("$realRosterClubs Vereine mit hinterlegtem Realkader · $simulatedRosterClubs Vereine mit simuliertem Kader.",color=if(simulatedRosterClubs==0)Grass else Muted)
  }
  Action("Zurück zum Start",!busy,secondary=true){onBack()}
 }
}

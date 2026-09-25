package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val WHATS_NEW_VERSION="0.5.23"
// Kompatibilitätsalias: bestehende Preferences/Tests behalten ihren bisherigen Schlüssel.
const val CHANGELOG_VERSION=WHATS_NEW_VERSION
const val CHANGELOG_LOADING="__loading__"

@Composable private fun WhatIsNewBlock(title:String,text:String){
 Text(title,style=MaterialTheme.typography.titleMedium,color=Grass)
 Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium)
}

@Composable private fun WhatsNewContent(){
 Text("Gründerelf 0.5.23 erweitert den individuellen Verein um echte Bild-Uploads und deutlich vielseitigere Trikotdesigns.",color=Chalk)\n WhatIsNewBlock("Eigene Vereinswappen hochladen","Beim individuellen Verein kannst du jetzt PNG- oder JPG-Wappen aus Dateien oder Galerie auswählen. Das Bild wird verkleinert, direkt im Spielstand gespeichert und bleibt auch beim Export der Karriere erhalten.")\n WhatIsNewBlock("Eigene Trikots hochladen","Heim-, Auswärts-, Dritt-, Torwart- und Trainingskleidung können jeweils ein eigenes Bild erhalten. Transparente PNGs werden über der Grundfarbe dargestellt; der Generator bleibt als Fallback erhalten.")\n WhatIsNewBlock("10 Trikotmuster","Der integrierte Generator wurde von vier auf zehn Designs erweitert: unter anderem Halbierung, Mittelstreifen, Querstreifen, Kontrastärmel, Chevron und Nadelstreifen.")
 WhatIsNewBlock("Sofort sichtbare Bedienung","Schalter, Aufstellung, Formation und Taktikänderungen werden nach einer Aktion sofort neu dargestellt. Änderungen sind nicht mehr erst nach erneutem Öffnen eines Screens sichtbar.")
 WhatIsNewBlock("Matchday-Bank selbst bestimmen","Vor dem Spiel wählst du deine sieben Ersatzspieler selbst. Die Auswahl bleibt erhalten und wird beim Anpfiff nicht mehr heimlich neu aufgebaut. Nur ungültige oder fehlende Plätze werden repariert.")
 WhatIsNewBlock("Neue Live-Aufstellung","Im Taktik-/Wechselbereich siehst du die komplette Formation. Spieler antippen wählt sie für einen Wechsel aus. Per Halten und Ziehen kannst du Positionen direkt tauschen. Nach einem Platzverweis bleibt die freie Position als sichtbare Lücke und kann taktisch verschoben werden.")
 WhatIsNewBlock("Realistischere Co-Trainer-Wechsel","Der Co-Trainer berücksichtigt Live-Rating, aktuelle Form, Fitness, Gelb-Risiko, Spielstand, Matchminute und Positionspassung stärker. Ein Spieler wird nicht mehrfach im selben Wechselpaket vorgeschlagen. Bei Mehrfachvorschlägen kannst du einzelne Wechsel annehmen und andere ablehnen.")
 WhatIsNewBlock("Krone der Kontinente","Der Fantasy-Wettbewerb ist jetzt im Wettbewerbsbereich sichtbar. Die besten sechs Vereine jeder spielbaren Liga qualifizieren sich; nach einer möglichen Vorrunde geht es direkt im K.-o.-System weiter. Siegprämien steigen rundenweise, der Turniersieger erhält zusätzlich 50 Mio. €.")
 WhatIsNewBlock("Club World Cup & internationale Prämien","Club World Cup und Krone werden im Wettbewerbsmenü dargestellt. Fehlende Titelprämien für Europa League, Club World Cup und Krone der Kontinente werden korrekt und nur einmal ausgezahlt.")
 WhatIsNewBlock("Torwart- & Torquoten-Fixes","Torhüter wirken deutlicher auf Abschlusswahrscheinlichkeiten. Internationale Pokale übernehmen nicht mehr versehentlich die Tor-Kalibrierung der heimischen Liga, und die Engine stellt sicher, dass ein verfügbarer Torwart tatsächlich im Tor eingesetzt wird.")
 WhatIsNewBlock("Transfers, Jugend & Kaderstabilität","Transfer-, Leih- und Jugendaktionen setzen eine gültige manuelle Startelf nicht mehr unnötig zurück. U19/U23-Spieler müssen vor einem Profieinsatz korrekt hochgezogen oder notfallnominiert werden.")
 WhatIsNewBlock("U19/U23 & Entwicklung","U19 und U23 sind eigenständige Mannschaften. Potenzialtraining kann Potenzial, Attribute und bei jungen Spielern auch die aktuelle Gesamtstärke erhöhen. Unter 20-Jährige profitieren besonders stark.")
 WhatIsNewBlock("Transferfluss & Budget","Verhandlungen können abgebrochen werden, reserviertes Budget wird freigegeben und akzeptierte Deals bleiben nach Medizincheck/Registrierung im korrekten Prozesszustand.")
 WhatIsNewBlock("Benachrichtigungen & Saves","Kader-/Mehr-Badges zählen nur echte neue Hinweise. Spielstände werden komprimiert gespeichert, Live-Checkpoints sind speicherschonender und ältere Saves werden weiterhin migriert.")
}

@Composable fun WhatsNewDialog(onDismiss:()->Unit){
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text("What's New · Gründerelf $WHATS_NEW_VERSION")},
  text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){WhatsNewContent()}},
  confirmButton={Button(onClick=onDismiss){Text("Los geht's")}}
 )
}

@Composable fun WhatsNewScreen(){
 Page("What's New","GRÜNDERELF · VERSION $WHATS_NEW_VERSION"){
  Section("Neu in dieser Version"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){WhatsNewContent()}}
 }
}

// Alte Aufrufer bleiben quellkompatibel.
@Composable fun ChangelogDialog(onDismiss:()->Unit)=WhatsNewDialog(onDismiss)

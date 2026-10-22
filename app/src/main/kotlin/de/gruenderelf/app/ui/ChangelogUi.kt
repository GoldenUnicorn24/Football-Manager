package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val WHATS_NEW_VERSION="0.5.25"
// Kompatibilitätsalias: bestehende Preferences/Tests behalten ihren bisherigen Schlüssel.
const val CHANGELOG_VERSION=WHATS_NEW_VERSION
const val CHANGELOG_LOADING="__loading__"

@Composable private fun WhatIsNewBlock(title:String,text:String){
 Text(title,style=MaterialTheme.typography.titleMedium,color=Grass)
 Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium)
}

@Composable private fun WhatsNewContent(){
 Text("Gründerelf 0.5.25 ist ein großes Design- und Komfort-Update mit korrigierter Torquote.",color=Chalk)
 WhatIsNewBlock("Neues Design „Flutlicht“","Komplett überarbeitete Oberfläche mit echten Flutlichtern, Anzeigetafel im Live-Spiel, Tabellen mit Wappen und einem neuen App-Icon. Der BMW-FC-Modus hat seinen eigenen Look in M-Blau.")
 WhatIsNewBlock("Nächster Schritt","Die Übersicht zeigt, was vor dem Anpfiff noch offen ist – Startelf, Bank, Fitness – und führt dich mit einem Knopf zum Spiel.")
 WhatIsNewBlock("Vorstand","Der Vorstand misst dich an der Kaderstärke im Ligavergleich. Tabelle, Form und Finanzen bestimmen sein Vertrauen.")
 WhatIsNewBlock("Saisonverlauf & Torjäger","Dein Tabellenplatz als Kurve über die Saison und unter Liga die Torjäger- und Vorlagenliste.")
 WhatIsNewBlock("Posteingang","Alle Meldungen mit Ungelesen-Markierung und Filtern.")
 WhatIsNewBlock("Schneller bedienen","Lange Listen wie Vereine und Spieler sind durchsuchbar, der Trainingsplan ist eine Wochenleiste und der Transfermarkt hat Filter-Chips.")
 WhatIsNewBlock("Barrierefreier","Positionseignung zusätzlich mit Symbolen: ✓ Haupt-, ≈ Neben-, ~ spielbare, ✕ unpassende Position.")
 WhatIsNewBlock("Realistischere Spiele","Angriffe brechen nicht mehr ständig ab: wieder rund 11–12 Schüsse und 1,1–1,2 Tore pro Team und Spiel.")
 WhatIsNewBlock("Sicherer","Spielstände sind in der Android-Datensicherung enthalten. Künftige Updates installieren sich ohne Deinstallation.")
}

@Composable fun WhatsNewDialog(onDismiss:()->Unit){
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text("Neu in Gründerelf $WHATS_NEW_VERSION",style=MaterialTheme.typography.headlineSmall)},
  text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){WhatsNewContent()}},
  confirmButton={Button(onClick=onDismiss,colors=ButtonDefaults.buttonColors(containerColor=Grass,contentColor=OnAccent)){Text("Los geht's")}},
  containerColor=PanelHigh
 )
}

@Composable fun WhatsNewScreen(){
 Page("What's New","GRÜNDERELF · VERSION $WHATS_NEW_VERSION"){
  Section("Neu in dieser Version"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){WhatsNewContent()}}
 }
}

// Alte Aufrufer bleiben quellkompatibel.
@Composable fun ChangelogDialog(onDismiss:()->Unit)=WhatsNewDialog(onDismiss)

package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val WHATS_NEW_VERSION="0.5.27"
// Kompatibilitätsalias: bestehende Preferences/Tests behalten ihren bisherigen Schlüssel.
const val CHANGELOG_VERSION=WHATS_NEW_VERSION
const val CHANGELOG_LOADING="__loading__"

@Composable private fun WhatIsNewBlock(title:String,text:String){
 Text(title,style=MaterialTheme.typography.titleMedium,color=Grass)
 Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium)
}

@Composable private fun WhatsNewContent(){
 WhatIsNewBlock("Vorstand mit Folgen","Alle vier Wochen tagt der Vorstand: Prämie, gekürzte Mittel, Ultimatum – und nach zwei Ultimaten die Entlassung mit Angeboten anderer Vereine.")
 WhatIsNewBlock("Saisonrückblick","Nach dem letzten Spieltag: Platz, Bilanz, beste Spieler und Titel auf einen Blick.")
 WhatIsNewBlock("Spieltag","Gegnervorschau mit voraussichtlicher Elf, Pressekonferenz vor Ligaspielen und Kabinenansprache in der Halbzeit.")
 WhatIsNewBlock("Ticketpreise","Im Vereinsbereich einstellbar: günstig füllt das Stadion und bringt Mitglieder, teuer bringt mehr pro Spiel.")
 WhatIsNewBlock("Länderspielpausen","Nach Spieltag 7, 15 und 25 reisen deine besten Spieler ab – der Rest erholt sich.")
 WhatIsNewBlock("Übersichtlicher","Kader, Spielerprofil und Transfers sind in Reiter gegliedert. Liga mit Wettbewerbs-Chips und Spieltag-Pfeilen.")
 WhatIsNewBlock("Besser lesbar","Größere Kleinschrift, mehr Kontrast, dezente Animationen und Vibration bei Toren.")
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
 Page("Neu in $WHATS_NEW_VERSION"){
  Section{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){WhatsNewContent()}}
 }
}

// Alte Aufrufer bleiben quellkompatibel.
@Composable fun ChangelogDialog(onDismiss:()->Unit)=WhatsNewDialog(onDismiss)

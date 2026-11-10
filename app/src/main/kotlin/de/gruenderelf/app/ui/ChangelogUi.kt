package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val WHATS_NEW_VERSION="0.5.26"
// Kompatibilitätsalias: bestehende Preferences/Tests behalten ihren bisherigen Schlüssel.
const val CHANGELOG_VERSION=WHATS_NEW_VERSION
const val CHANGELOG_LOADING="__loading__"

@Composable private fun WhatIsNewBlock(title:String,text:String){
 Text(title,style=MaterialTheme.typography.titleMedium,color=Grass)
 Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium)
}

@Composable private fun WhatsNewContent(){
 WhatIsNewBlock("Aufgeräumt","Weniger Erklärtexte, keine doppelten Knöpfe, ruhigere Karten. Die Übersicht zeigt Hinweise nur noch, wenn wirklich etwas offen ist.")
 WhatIsNewBlock("Neue Navigation","Schlankere Leiste unten und ein Zurück-Pfeil auf allen Unterseiten.")
 WhatIsNewBlock("Startbildschirm","Zeigt nur noch belegte Spielstände – Weiterspielen steht ganz oben.")
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

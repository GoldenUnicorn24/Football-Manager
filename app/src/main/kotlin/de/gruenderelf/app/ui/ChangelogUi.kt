package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val CHANGELOG_VERSION="0.5.18"
const val CHANGELOG_LOADING="__loading__"

@Composable private fun ChangeVersion(version:String,title:String,text:String){
 Text("$version · $title",style=MaterialTheme.typography.titleMedium,color=Grass)
 Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium)
}

@Composable fun ChangelogDialog(onDismiss:()->Unit){
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text("Neu in Gründerelf $CHANGELOG_VERSION")},
  text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("Seit der Einführung des geführten Tutorials in v0.5.4 hat sich Gründerelf deutlich erweitert. Hier stehen die spielrelevanten Änderungen seit diesem Stand.",color=Chalk)
   ChangeVersion("v0.5.4","Tutorial & Karriere-Grundlagen","Geführtes Tutorial eingeführt. Eigene Taktikpläne, schnellere sichere Checkpoints, Spielerkarriere, Vereinsgeschichte und weitere Langzeitinformationen wurden ergänzt.")
   ChangeVersion("v0.5.5","Live-Runtime","Die laufende Partie wurde technisch vom UI-Takt entkoppelt und die Live-Steuerung stabiler gemacht, damit Match, Taktik und Anzeige sauber zusammenspielen.")
   ChangeVersion("v0.5.6","Stabilität","Mehrere Laufzeit-, Match- und Speicherzustände wurden abgesichert, damit fehlerhafte Zwischenzustände nicht die Karriere beschädigen.")
   ChangeVersion("v0.5.7","Simulation","Live-Runner und Schnellsimulation wurden zuverlässiger. Halbzeit- und Restspiel-Simulation laufen kontrollierter durch echte Match-Zustände.")
   ChangeVersion("v0.5.8","Selbstheilender Live-Runner","Der automatische Live-Takt wurde in den ViewModel-Runner verlagert und überwacht sich selbst. Zusätzliche Kompatibilitäts- und Stressprüfungen schützen ältere Saves.")
   ChangeVersion("v0.5.9","Einheitliche Live-Engine & große Saves","Live-Spiel und Schnelllauf nutzen einen vereinheitlichten Runtime-Pfad. Die gültige Save-Grenze wurde konsistent auf 128 MB erhöht.")
   ChangeVersion("v0.5.10","Laden & Speicher","Das Laden großer oder älterer Spielstände wurde speicherschonender. Unnötige Vollkopien beim Laden wurden entfernt und Backup/Checkpoint-Verhalten weiter gehärtet.")
   ChangeVersion("v0.5.11","Entscheidungs-Sicherheit","Inkonsistente Ballbesitz- oder Entscheidungszustände werden repariert bzw. sicher abgefangen, statt einen Match-Absturz auszulösen.")
   ChangeVersion("v0.5.12","Eigene Spieler verkaufen & verleihen","Für eigene Spieler kommen mehrere externe Angebote, die angenommen bzw. verhandelt werden können. Jugend-Entwicklung wurde zusätzlich geprüft und erweitert.")
   ChangeVersion("v0.5.13","U19 & U23 als echte Mannschaften","U19 und U23 sind eigenständige Kader. Spieler können hochgezogen, im Notfall eingesetzt, gezielt in die Jugend verschoben, für die Jugend gekauft sowie verkauft oder verliehen werden. Der Co-Trainer entwickelt die Nachwuchsteams aktiv weiter.")
   ChangeVersion("v0.5.14","Verhandlungen, Titelprämien & Potenzialtraining","Verhandlungen können wieder verlassen werden und reserviertes Budget wird sofort freigegeben. Titel können zusätzliche einmalige Prämien zahlen. Gezieltes Potenzialtraining wurde für Jugend- und Profispieler ergänzt.")
   ChangeVersion("v0.5.15","Stärkeres U20-Training","Potenzialtraining für Spieler unter 20 wurde deutlich verstärkt: Es kann Potenzial, aktuelle Gesamtstärke und positionsbezogene Attribute gleichzeitig steigern. Profis profitieren moderater.")
   ChangeVersion("v0.5.16","Benachrichtigungen","Die Badges bei Kader und Mehr zählen nur noch wirklich neue Hinweise. Öffnen quittiert die passenden Warnungen bzw. Scoutberichte; alte dauerhaft stehende 9er-Badges werden bei der Migration bereinigt.")
   ChangeVersion("v0.5.17","Transferabschluss & Registrierung","Käufe und Leihen bleiben nach dem Medizincheck im korrekten Deal-Zustand. U19/U23 werden nicht mehr fälschlich auf die 32er-Profikadergrenze angerechnet; vorübergehend blockierte Registrierungen können erneut abgeschlossen werden.")
   ChangeVersion("v0.5.18","Changelog & erneuertes Tutorial","Dieses einmalige Versions-Changelog wurde eingebaut. Die Hilfe-/Tutorial-Seite und die geführten Hinweise erklären jetzt die aktuellen Kader-, Jugend-, Trainings-, Transfer-, Benachrichtigungs-, Wettbewerbs- und Live-Systeme.")
  }},
  confirmButton={Button(onClick=onDismiss){Text("Verstanden")}}
 )
}

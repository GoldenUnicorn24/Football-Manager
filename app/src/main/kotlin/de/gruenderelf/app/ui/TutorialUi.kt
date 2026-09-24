package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.World

private data class TutorialPage(
 val route:String,
 val title:String,
 val text:String,
 val nextRoute:String?,
 val nextStep:Int,
 val requiresLive:Boolean=false
)

private val tutorialPages=listOf(
 TutorialPage(
  "home","Willkommen bei Gründerelf",
  "Die Startseite ist deine Manager-Zentrale. Hier siehst du das nächste Spiel, Aufgaben, Finanzen, Fitness und wichtige Hinweise. Badges bei Kader oder Mehr stehen nur für neue Meldungen und werden quittiert, sobald du den passenden Bereich öffnest.",
  "kader",1
 ),
 TutorialPage(
  "kader","Startelf & Matchday-Bank",
  "Hier bestimmst du Formation und Startelf. Zusätzlich wählst du vor dem Spiel die sieben Ersatzspieler selbst. Deine Auswahl bleibt erhalten; das Spiel repariert nur ungültige oder fehlende Plätze. U19/U23-Spieler müssen zuerst hochgezogen oder für einen Notfalleinsatz nominiert werden.",
  "training",2
 ),
 TutorialPage(
  "training","Training, Entwicklung & Co-Trainer",
  "Plane Wochenbelastung, Gegnerfokus und individuelle Schwerpunkte selbst oder delegiere sie. Potenzialtraining kann Entwicklungsgrenze und Attribute erhöhen; Spieler unter 20 profitieren besonders stark. Der Co-Trainer kann Profis und Jugend automatisch entwickeln und besitzt ein eigenes Wechselprofil.",
  "transfers",3
 ),
 TutorialPage(
  "transfers","Transfers, Scouting & Jugend",
  "Scoutberichte werden mit der Zeit genauer. Eigene Spieler können mehrere Kauf- oder Leihangebote erhalten. Verhandlungen lassen sich abbrechen, wodurch reserviertes Budget wieder frei wird. Medizincheck und Registrierung gehören zum selben Deal und starten ihn nicht erneut. U19/U23 können separat verstärkt, verkauft oder verliehen werden.",
  "liga",4
 ),
 TutorialPage(
  "liga","Ligen & Wettbewerbe",
  "Hier findest du Tabellen und Pokale. Club World Cup besitzt Gruppen- und K.-o.-Phase. Die optionale Krone der Kontinente nimmt die besten sechs Vereine jeder spielbaren Liga auf und läuft nach einer möglichen Vorrunde direkt im K.-o.-System. Internationale Titel zahlen eigene Siegerprämien.",
  "spiel",5
 ),
 TutorialPage(
  "spiel","Vor dem Spiel",
  "Prüfe noch einmal Startelf, Ausfälle und deine bewusst gewählte Ersatzbank. Im Live-Spiel kannst du jederzeit Taktik, Sofortanweisungen und Wechsel steuern. Starte die Partie, wenn du bereit bist; der nächste Tutorial-Hinweis erscheint automatisch im laufenden Match.",
  null,6
 ),
 TutorialPage(
  "spiel","Live-Aufstellung & Wechsel",
  "Im Taktik-Tab siehst du die komplette Formation. Tippe einen Spieler an und danach einen Ersatzspieler, um direkt zu wechseln. Halte einen Spieler gedrückt und ziehe ihn auf eine andere Position, um Plätze zu tauschen. Nach Rot bleibt eine sichtbare Lücke, die du so dorthin verschieben kannst, wo die Unterzahl am wenigsten schmerzt. Co-Trainer-Pakete lassen sich einzeln annehmen oder ablehnen.",
  "mehr",7,true
 ),
 TutorialPage(
  "mehr","Mehr, Hilfe & What's New",
  "Unter Mehr findest du Verein, Training, Transfers, Karriere, Einstellungen, Speicherstände und das vollständige Manager-Handbuch. What's New kannst du dort jederzeit erneut öffnen, auch nachdem die einmalige Versionsanzeige beim ersten Start geschlossen wurde.",
  null,8
 )
)

@Composable fun TutorialCoach(w:World,route:String,vm:GameViewModel,onNavigate:(String)->Unit){
 val u=w.user
 if(!u.tutorialEnabled||u.tutorialCompleted)return
 val step=u.tutorialStep.coerceIn(0,tutorialPages.lastIndex)
 val page=tutorialPages[step]
 if(page.route!=route)return
 if(page.requiresLive&&w.live==null)return
 AlertDialog(
  onDismissRequest={},
  title={Text(page.title)},
  text={Column(Modifier.heightIn(max=440.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(page.text)}},
  confirmButton={TextButton(onClick={
   vm.action{world->
    if(page.nextStep>=tutorialPages.size){world.user.tutorialStep=tutorialPages.size;world.user.tutorialCompleted=true}
    else world.user.tutorialStep=page.nextStep
   }
   page.nextRoute?.let(onNavigate)
  }){Text(if(page.nextStep>=tutorialPages.size)"Tutorial abschließen" else if(page.requiresLive)"Weiter zu Mehr" else if(step==5)"Verstanden" else "Weiter")}},
  dismissButton={TextButton(onClick={vm.action{it.user.tutorialEnabled=false;it.user.tutorialCompleted=true}}){Text("Tutorial beenden")}}
 )
}

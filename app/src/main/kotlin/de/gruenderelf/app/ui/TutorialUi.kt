package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.GameViewModel
import de.gruenderelf.engine.World

private data class TutorialPage(val route:String,val title:String,val text:String,val nextRoute:String?,val nextStep:Int)
private val tutorialPages=listOf(
 TutorialPage("home","Willkommen bei Gründerelf","Die Startseite ist deine Manager-Zentrale. Prüfe Aufgaben, nächstes Spiel, Finanzen und Fitness. Rote Badges bei Kader oder Mehr stehen nur für neue Hinweise und verschwinden, sobald du den passenden Bereich geöffnet hast.","kader",1),
 TutorialPage("kader","Profikader, U19 & U23","Hier stellst du Formation, Startelf und Bank zusammen. U19 und U23 sind eigenständige Mannschaften: Talente können dort gezielt entwickelt, hochgezogen oder im Notfall eingesetzt werden. Jugendspieler zählen nicht zur 32er-Profikadergrenze.","training",2),
 TutorialPage("training","Training & Entwicklung","Plane selbst oder überlasse Profis und Jugend dem Co-Trainer. Intensivtraining fördert einen Schwerpunkt über vier Wochen. Potenzialtraining kann Potenzial und Attribute erhöhen; Spieler unter 20 profitieren besonders stark und können zusätzlich echte Gesamtstärke gewinnen.","transfers",3),
 TutorialPage("transfers","Transfers, Scouting & Verhandlungen","Scoutberichte werden genauer, eigene Spieler können mehrere Kauf- oder Leihangebote erhalten und Jugendspieler direkt für U19/U23 verpflichtet werden. In Verhandlungen bleibt Budget nur während eines aktiven Deals reserviert; beim Rücktritt wird es freigegeben. Nach dem Medizincheck folgt die Registrierung, ohne den Deal neu zu starten.","spiel",4),
 TutorialPage("spiel","Vor dem ersten Spiel","Prüfe Formation, Ausfälle und Belastung. Du kannst live spielen oder kontrolliert schnell simulieren. Im Live-Spiel lassen sich Pause, Taktik, Sofortanweisungen und Wechsel weiter direkt steuern.",null,5),
 TutorialPage("spiel","Live-Spiel, Statistik & Analyse","Live, Taktik, Statistik und Analyse greifen auf denselben Match-Zustand zu. xG, Schusskarte, Passnetz und Taktikänderungen beruhen auf echten Ereignissen der Partie. Der selbstheilende Live-Runner und sichere Checkpoints schützen laufende Spiele zusätzlich.",null,6)
)

@Composable fun TutorialCoach(w:World,route:String,vm:GameViewModel,onNavigate:(String)->Unit){
 val u=w.user
 if(!u.tutorialEnabled||u.tutorialCompleted)return
 val step=u.tutorialStep.coerceIn(0,tutorialPages.lastIndex)
 val page=tutorialPages[step]
 if(page.route!=route)return
 if(step==5&&w.live==null)return
 AlertDialog(
  onDismissRequest={},
  title={Text(page.title)},
  text={Column(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(page.text);if(step==4&&w.live==null)Text("Starte die Partie, sobald du bereit bist. Der nächste Hinweis erscheint automatisch im laufenden Match.",color=Muted)}},
  confirmButton={TextButton(onClick={
   vm.action{world->
    if(page.nextStep>=tutorialPages.size){world.user.tutorialStep=tutorialPages.size;world.user.tutorialCompleted=true}
    else world.user.tutorialStep=page.nextStep
   }
   page.nextRoute?.let(onNavigate)
  }){Text(if(page.nextStep>=tutorialPages.size)"Tutorial abschließen" else if(step==4)"Verstanden" else "Weiter")}},
  dismissButton={TextButton(onClick={vm.action{it.user.tutorialEnabled=false;it.user.tutorialCompleted=true}}){Text("Tutorial beenden")}}
 )
}

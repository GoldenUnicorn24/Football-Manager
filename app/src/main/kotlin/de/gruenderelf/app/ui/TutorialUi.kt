package de.gruenderelf.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
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
 TutorialPage("home","Willkommen bei Gründerelf","Die Startseite ist deine Manager-Zentrale. Hier siehst du das nächste Spiel, Finanzen, Fitnesswarnungen und die wichtigsten Aufgaben dieser Vereinswoche.","kader",1),
 TutorialPage("kader","Kader & Rollen","Hier stellst du Formation, Startelf und Bank zusammen. Positionspassung, Fitness, Moral und individuelle Rollen wirken tatsächlich auf die Match-Simulation.","training",2),
 TutorialPage("training","Training & Co-Trainer","Du kannst jede Trainingswoche selbst planen oder Teile an den Co-Trainer abgeben. Intensität verbessert Entwicklung, erhöht aber Belastung und Verletzungsrisiko.","transfers",3),
 TutorialPage("transfers","Transfers, Scouting & Jugend","Scoutberichte werden mit der Zeit genauer. Merkliste, Verhandlungen, U19/U23 und gezielte Talentsuche sind echte Systeme und keine reinen Menüanzeigen.","spiel",4),
 TutorialPage("spiel","Vor dem ersten Spiel","Prüfe Formation und Ausfälle. Im Live-Spiel kannst du jederzeit pausieren, Taktik verändern, Sofortanweisungen geben und Wechsel selbst bestätigen.",null,5),
 TutorialPage("spiel","Live-Spiel steuern","Live, Taktik, Statistik und Analyse sind getrennt, damit die Darstellung flüssig bleibt. Änderungen an Mentalität, Pressing oder Sofortanweisungen wirken direkt auf die MatchEngine.",null,6)
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

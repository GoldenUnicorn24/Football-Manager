package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class V0515YouthPotentialTest {
 @Test fun under20YouthPotentialTrainingStronglyRaisesPotentialAndCurrentRating(){
  val w=WorldFactory.createWorld(51501L);val c=w.club();c.budget=100_000_000L;c.stadium.training=100;c.stadium.gym=100;c.stadium.youth=100;c.stadium.medicine=100;c.dynamics.staffQuality=100
  val p=w.squad().first{it.youth};p.position=Position.ST;p.birthYear=w.calendar.season-17;p.attributes=Attributes(40,40,40,40,40,40,40,40,40,10,40);p.hidden.potential=40;p.hidden.development=90;p.hidden.professionalism=90;p.hidden.injuryProneness=0;p.injuryWeeks=0
  val beforePotential=p.hidden.potential;val beforeRating=p.ca;assertEquals(40,beforeRating);assertTrue(w.calendar.season-p.birthYear<20)
  IntensiveTrainingSystem.startPotential(w,p.id,Focus.FINISHING);val project=w.intensiveTraining.first{it.playerId==p.id};project.weeksLeft=1;project.progress=0.0
  IntensiveTrainingSystem.applyWeek(w,c,p,TrainingReport(),SeededRandom(51501L))
  assertEquals(beforePotential+5,p.hidden.potential,"U20-Jugend mit Top-Förderung soll +5 Potenzial erhalten.")
  assertTrue(p.ca>=beforeRating+4,"U20-Potenzialtraining muss neben Potenzial auch die aktuelle Gesamtstärke deutlich erhöhen.")
  assertTrue(p.ca<=p.hidden.potential);assertTrue(w.intensiveTraining.none{it.playerId==p.id})
 }

 @Test fun potentialTrainingAlsoRaisesCurrentRatingForSeniorPlayers(){
  val w=WorldFactory.createWorld(51502L);val c=w.club();c.budget=100_000_000L;c.stadium.training=100;c.stadium.gym=100;c.stadium.medicine=100;c.dynamics.staffQuality=100
  val p=w.squad().first{!it.youth&&!it.retired&&it.id!=w.user.playerId};p.position=Position.ZM;p.attributes=Attributes(60,60,60,60,60,60,60,60,60,10,60);p.hidden.potential=60;p.hidden.injuryProneness=0;p.injuryWeeks=0
  val beforePotential=p.hidden.potential;val beforeRating=p.ca;assertEquals(60,beforeRating)
  IntensiveTrainingSystem.startPotential(w,p.id,Focus.TECHNIQUE);val project=w.intensiveTraining.first{it.playerId==p.id};project.weeksLeft=1;project.progress=0.0
  IntensiveTrainingSystem.applyWeek(w,c,p,TrainingReport(),SeededRandom(51502L))
  assertTrue(p.hidden.potential>beforePotential);assertTrue(p.ca>=beforeRating+1,"Potenzialtraining soll auch bei Profis die aktuelle Gesamtstärke verbessern.")
 }
}

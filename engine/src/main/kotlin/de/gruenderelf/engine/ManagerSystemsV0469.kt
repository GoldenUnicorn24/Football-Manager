package de.gruenderelf.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/** Transferinteresse ist zielvereinsspezifisch und stabil aus der Welt ableitbar. */
object TransferInterestSystem {
 data class Snapshot(val score:Int,val level:TransferInterestLevel,val reasons:List<String>)
 private fun deterministicAffinity(w:World,p:Player,targetClubId:Int):Int {
  var x=(w.seed xor (p.id.toLong()*0x9E3779B1L) xor (targetClubId.toLong()*0x85EBCA77L))
  x=x xor (x ushr 17);x*=0xC2B2AE3DL;x=x xor (x ushr 13)
  return (((x and 0x7fffffffL)%31L).toInt()-10)
 }
 fun snapshot(w:World,p:Player,targetClubId:Int):Snapshot {
  val target=w.clubs[targetClubId]?:return Snapshot(0,TransferInterestLevel.LOW,listOf("Verein unbekannt"))
  if(p.retired||p.clubId==targetClubId)return Snapshot(0,TransferInterestLevel.LOW,listOf("kein externer Transfer"))
  val current=w.clubs[p.clubId]
  var score=38;val reasons=mutableListOf<String>()
  val affinity=deterministicAffinity(w,p,targetClubId);score+=affinity
  if(affinity>=10)reasons+="persönliche Sympathie" else if(affinity<=-6)reasons+="wenig persönliche Bindung"
  if(p.clubId==0){score+=22;reasons+="vereinslos"}
  if(p.wantsMove){score+=20;reasons+="aktuell wechselwillig"}
  if(p.homegrownClubId==targetClubId&&targetClubId!=0){score+=18;reasons+="kennt den Verein aus der Jugend"}
  val repGap=target.reputation-(current?.reputation?:target.reputation-5)
  score+=(repGap/2).coerceIn(-18,18)
  if(repGap>=12)reasons+="sportlicher Schritt nach oben" else if(repGap<=-14)reasons+="sportlicher Rückschritt"
  val samePos=w.squad(targetClubId).filter{!it.youth&&!it.retired&&it.position==p.position&&it.id!=p.id}
  val best=samePos.maxOfOrNull{it.ca}?:0
  if(best<=p.ca-4){score+=10;reasons+="sieht realistische Einsatzchancen"} else if(best>=p.ca+12){score-=8;reasons+="starke Konkurrenz auf seiner Position"}
  val age=w.calendar.season-p.birthYear
  if(age<=23&&target.academy.u23Quality>=60){score+=6;reasons+="gutes Entwicklungsumfeld"}
  val targetLevel=WorldFactory.leagueLevel(w,target.tier);val currentLevel=current?.let{WorldFactory.leagueLevel(w,it.tier)}?:targetLevel
  if(targetLevel<currentLevel){score+=5;reasons+="höhere Spielklasse"}
  if(targetLevel>currentLevel+2){score-=7;reasons+="deutlich niedrigere Spielklasse"}
  if(!p.wantsMove&&p.clubId!=0){val loyaltyPenalty=(p.hidden.loyalty-45).coerceAtLeast(0)/5;score-=loyaltyPenalty;if(loyaltyPenalty>=7)reasons+="starke Bindung an aktuellen Verein"}
  score+=(p.hidden.ambition-55)/7
  val final=score.coerceIn(5,100)
  return Snapshot(final,when{final>=85->TransferInterestLevel.DESPERATE;final>=74->TransferInterestLevel.VERY_INTERESTED;final>=62->TransferInterestLevel.INTERESTED;final>=48->TransferInterestLevel.OPEN;else->TransferInterestLevel.LOW},reasons.take(4))
 }
 fun score(w:World,p:Player,targetClubId:Int)=snapshot(w,p,targetClubId).score
 fun candidatesForClub(w:World,targetClubId:Int,minimum:Int=58):List<Pair<Player,Snapshot>> = w.players.values
  .asSequence().filter{!it.retired&&!it.youth&&it.clubId!=targetClubId}.map{it to snapshot(w,it,targetClubId)}
  .filter{it.second.score>=minimum}.sortedByDescending{it.second.score}.take(80).toList()
}

object AssistantCoachSystem {
 private fun weakestFocus(p:Player):Focus {
  val choices=when(p.position){
   Position.TW->listOf(Focus.KEEPING,Focus.VISION,Focus.TECHNIQUE)
   Position.IV,Position.DM->listOf(Focus.TACKLING,Focus.PACE,Focus.VISION)
   Position.LV,Position.RV->listOf(Focus.PACE,Focus.TACKLING,Focus.TECHNIQUE)
   Position.ZM,Position.OM->listOf(Focus.VISION,Focus.TECHNIQUE,Focus.PACE)
   Position.LA,Position.RA->listOf(Focus.PACE,Focus.TECHNIQUE,Focus.FINISHING)
   Position.ST->listOf(Focus.FINISHING,Focus.PACE,Focus.TECHNIQUE)
  }
  fun value(f:Focus)=when(f){Focus.FINISHING->p.attributes.finishing;Focus.PACE->p.attributes.pace;Focus.TECHNIQUE->p.attributes.technique;Focus.TACKLING->p.attributes.tackling;Focus.KEEPING->p.attributes.keeping;Focus.VISION->p.attributes.vision}
  return choices.minBy{value(it)}
 }
 private fun nextOpponent(w:World,c:Club):Club?{val f=w.fixtures.filter{!it.played&&(it.homeId==c.id||it.awayId==c.id)}.minByOrNull{it.matchday}?:return null;return w.clubs[if(f.homeId==c.id)f.awayId else f.homeId]}
 fun recommendedPlan(w:World,c:Club=w.club()):TrainingPlan {
  val a=w.assistantCoach;val fatigue=c.dynamics.fatigueLoad;val opponent=nextOpponent(w,c)
  val plan=when{
   a.profile==AssistantCoachProfile.MOTIVATOR&&c.dynamics.chemistry<58->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TEAM_BONDING,UnitType.MENTAL,UnitType.TACTICS,UnitType.RECOVERY,UnitType.TEAM_BONDING,UnitType.OFF),intensity=2)
   a.profile==AssistantCoachProfile.ANALYST&&opponent!=null->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.VIDEO,UnitType.VIDEO,UnitType.TACTICS,UnitType.POSITIONAL,UnitType.RECOVERY,UnitType.OFF),intensity=3,opponentPrep=true)
   a.profile==AssistantCoachProfile.TACTICIAN&&opponent!=null->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TACTICS,UnitType.POSITIONAL,UnitType.VIDEO,UnitType.SET_PIECES,UnitType.RECOVERY,UnitType.OFF),intensity=3,opponentPrep=true)
   a.profile==AssistantCoachProfile.DEVELOPER->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TECHNIQUE,UnitType.POSITIONAL,UnitType.MENTAL,UnitType.TACTICS,UnitType.RECOVERY,UnitType.OFF),intensity=3)
   fatigue>=72->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.VIDEO,UnitType.OFF,UnitType.TACTICS,UnitType.RECOVERY,UnitType.SET_PIECES,UnitType.OFF),intensity=2)
   a.trainingStyle==AssistantTrainingStyle.FITNESS->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.FITNESS,UnitType.DUELS,UnitType.TECHNIQUE,UnitType.FITNESS,UnitType.RECOVERY,UnitType.OFF),intensity=4)
   a.trainingStyle==AssistantTrainingStyle.DEVELOPMENT->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TECHNIQUE,UnitType.POSITIONAL,UnitType.FINISHING,UnitType.TACTICS,UnitType.RECOVERY,UnitType.OFF),intensity=3)
   a.trainingStyle==AssistantTrainingStyle.YOUTH->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TECHNIQUE,UnitType.POSITIONAL,UnitType.MENTAL,UnitType.TEAM_BONDING,UnitType.RECOVERY,UnitType.OFF),intensity=3)
   a.trainingStyle==AssistantTrainingStyle.MATCH_PREP->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.VIDEO,UnitType.TACTICS,UnitType.POSITIONAL,UnitType.SET_PIECES,UnitType.RECOVERY,UnitType.OFF),intensity=3,opponentPrep=true)
   opponent!=null&&opponent.tactics.pressing>=4->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.VIDEO,UnitType.POSITIONAL,UnitType.TECHNIQUE,UnitType.TACTICS,UnitType.RECOVERY,UnitType.OFF),intensity=3,opponentPrep=true)
   else->TrainingPlan(mutableListOf(UnitType.RECOVERY,UnitType.TACTICS,UnitType.TECHNIQUE,UnitType.POSITIONAL,UnitType.SET_PIECES,UnitType.RECOVERY,UnitType.OFF),intensity=3,opponentPrep=true)
  }
  val senior=w.squad(c.id).filter{!it.retired&&!it.youth&&it.injuryWeeks==0&&it.hidden.potential>it.ca}.sortedByDescending{it.hidden.potential-it.ca}.take(4)
  val youth=if(a.autoYouthTraining)w.squad(c.id).filter{it.youth&&it.injuryWeeks==0}.sortedByDescending{it.youthProfile.learning}.take(3) else emptyList()
  plan.extra=(senior+youth).distinctBy{it.id}.take(6).map{IndividualFocus(it.id,weakestFocus(it))}.toMutableList()
  return plan
 }
 fun prepareUserPlan(w:World):TrainingPlan {
  val c=w.club();val p=recommendedPlan(w,c);w.training.days=p.days.toMutableList();w.training.intensity=p.intensity;w.training.opponentPrep=p.opponentPrep;w.training.extra=p.extra.toMutableList()
  w.assistantCoach.lastPlanReason=when{c.dynamics.fatigueLoad>=72->"Belastung hoch: Regeneration und Video vor zusätzlicher Intensität.";w.assistantCoach.trainingStyle==AssistantTrainingStyle.MATCH_PREP->"Schwerpunkt auf Gegneranalyse, Taktik und Standards.";w.assistantCoach.trainingStyle==AssistantTrainingStyle.DEVELOPMENT->"Mehr Technik- und Positionsarbeit für langfristige Entwicklung.";w.assistantCoach.trainingStyle==AssistantTrainingStyle.YOUTH->"Technik, Mentalität und Positionsspiel mit Jugendfokus.";w.assistantCoach.trainingStyle==AssistantTrainingStyle.FITNESS->"Athletikblock mit zusätzlicher Regeneration.";else->"Ausgewogener Wochenplan anhand von Belastung und nächstem Gegner."}
  return w.training
 }
 fun prepareYouth(w:World,c:Club){if(c.id!=w.user.clubId||!w.assistantCoach.autoYouthTraining)return
  val youths=w.squad(c.id).filter{it.youth&&!it.retired};var withPros=0
  for(p in youths){val y=p.youthProfile;val readiness=YouthEngine.readiness(w,p);val aggressive=w.assistantCoach.youthAggression.coerceIn(1,5);val allow=readiness>=62-aggressive*3&&y.growthSpurtWeeks==0&&y.injuryGrowthRisk<68&&c.dynamics.fatigueLoad<78;y.seniorTraining=allow;if(allow)withPros++
   if(y.mentorId==0||w.players[y.mentorId]?.clubId!=c.id)y.mentorId=w.squad(c.id).filter{!it.youth&&w.calendar.season-it.birthYear>=25}.maxByOrNull{it.hidden.professionalism+it.hidden.consistency}?.id?:0
  }
  w.assistantCoach.lastYouthReason="$withPros von ${youths.size} Jugendspielern trainieren dosiert bei den Profis; Wachstum, Reife und Verletzungsrisiko wurden berücksichtigt."
 }
}

object IntensiveTrainingSystem {
 fun cost(w:World,p:Player):Long{val level=WorldFactory.leagueLevel(w,w.club().tier);val base=when{level>=8->1_500L;level>=6->6_000L;level>=4->25_000L;level>=2->80_000L;else->160_000L};return (base+TransferEngine.marketValue(w,p)/180L).coerceAtLeast(base)}
 fun potentialCost(w:World,p:Player):Long=maxOf(cost(w,p)*3/2,cost(w,p)+1_000L)
 private fun commonReason(w:World,playerId:Int,price:Long):String?{val p=w.players[playerId]?:return "Spieler nicht gefunden.";return when{w.live!=null->"Nicht während eines laufenden Spiels.";p.clubId!=w.user.clubId||p.retired->"Spieler gehört nicht zum Verein.";w.intensiveTraining.any{it.playerId==playerId}->"Für diesen Spieler läuft bereits Intensivtraining.";w.intensiveTraining.size>=3->"Maximal drei Intensivprogramme gleichzeitig.";w.club().budget<price->"Vereinskasse reicht für ${price} € nicht aus.";else->null}}
 fun reason(w:World,playerId:Int):String?{val p=w.players[playerId]?:return "Spieler nicht gefunden.";return commonReason(w,playerId,cost(w,p))?:if(p.hidden.potential<=p.ca)"Natürliches Potenzial erreicht. Nutze Potenzialtraining, um die Entwicklungsgrenze gezielt anzuheben." else null}
 fun potentialReason(w:World,playerId:Int):String?{val p=w.players[playerId]?:return "Spieler nicht gefunden.";return commonReason(w,playerId,potentialCost(w,p))?:if(p.hidden.potential>=99)"Das reguläre Maximalpotenzial 99 ist bereits erreicht." else null}
 fun start(w:World,playerId:Int,focus:Focus){val r=reason(w,playerId);require(r==null){r?:"Intensivtraining nicht möglich."};val p=w.players.getValue(playerId);val price=cost(w,p);w.club().budget-=price;w.intensiveTraining.add(IntensiveTrainingProject(playerId,focus,4,4,price,0.0,w.calendar.absoluteWeek,false));w.news("Intensivtraining gestartet","${p.name}: vier Wochen ${focus.label}. Kosten ${price} €. Mehr Entwicklung, aber höhere Belastung.","normal")}
 fun startPotential(w:World,playerId:Int,focus:Focus){val r=potentialReason(w,playerId);require(r==null){r?:"Potenzialtraining nicht möglich."};val p=w.players.getValue(playerId);val price=potentialCost(w,p);w.club().budget-=price;w.intensiveTraining.add(IntensiveTrainingProject(playerId,focus,4,4,price,0.0,w.calendar.absoluteWeek,true));w.news("Potenzialtraining gestartet","${p.name}: vier Wochen Potenzialförderung ${focus.label}. Kosten ${price} €. Entwicklungsgrenze und aktuelle Stärke werden gezielt angehoben.","normal")}
 fun applyWeek(w:World,c:Club,p:Player,report:TrainingReport,rng:SeededRandom){
  val project=w.intensiveTraining.firstOrNull{it.playerId==p.id}?:return;if(p.injuryWeeks>0)return
  val age=w.calendar.season-p.birthYear;val youth= p.youth && age<20;val youthFactor=when{age<=19->1.18;age<=23->1.08;age>=31->.72;else->1.0};val staff=.65+c.dynamics.staffQuality/180.0;val facility=1+c.stadium.training/220.0+c.stadium.gym/400.0
  project.progress+=.32*youthFactor*staff*facility*(if(project.raisesPotential&&youth)1.65 else 1.0)
  p.fitness=(p.fitness-3.0).coerceAtLeast(20.0);p.sharpness=(p.sharpness+2).coerceAtMost(100)
  while(project.progress>=1.0){project.progress-=1.0;if(p.attributes.improve(project.focus,p.hidden.potential)&&c.id==w.user.clubId)report.gains.add("${p.name}: ${if(project.raisesPotential)"Potenzialtraining" else "Intensivtraining"} ${project.focus.label} +1")}
  val risk=.012*(1+p.hidden.injuryProneness/80.0)*(1-c.stadium.medicine*.005)*(if(p.youthProfile.growthSpurtWeeks>0)1.8 else 1.0);if(rng.chance(risk)){p.injuryWeeks=maxOf(p.injuryWeeks,2);p.injury="Überlastung im Intensivtraining";if(c.id==w.user.clubId)report.injuries.add("${p.name}: Überlastung durch Intensivtraining")}
  project.weeksLeft--;if(project.weeksLeft>0)return
  w.intensiveTraining.remove(project);p.hidden.development=(p.hidden.development+1).coerceAtMost(100)
  if(project.raisesPotential){
   val beforePotential=p.hidden.potential;val beforeCa=p.ca
   val academyBonus=if(youth&&c.stadium.youth>=70)1 else 0;val professionalBonus=if(youth&&p.hidden.development>=80&&p.hidden.professionalism>=70)1 else 0
   val potentialGain=(if(youth)3 else 1)+academyBonus+professionalBonus;p.hidden.potential=(p.hidden.potential+potentialGain).coerceAtMost(99)
   val targetGain=if(youth)2+academyBonus+professionalBonus else 1
   repeat(targetGain){if(p.ca<p.hidden.potential){val candidates=Focus.entries.sortedBy{f->when(f){Focus.FINISHING->p.attributes.finishing;Focus.PACE->p.attributes.pace;Focus.TECHNIQUE->p.attributes.technique;Focus.TACKLING->p.attributes.tackling;Focus.KEEPING->p.attributes.keeping;Focus.VISION->p.attributes.vision}};p.attributes.improve(candidates.first(),p.hidden.potential)}}
   p.attributes.improve(project.focus,p.hidden.potential)
   if(c.id==w.user.clubId)w.news("Potenzialtraining abgeschlossen","${p.name} beendet die vierwöchige ${if(youth)"Jugend-" else ""}Potenzialförderung. Entwicklungsgrenze +${p.hidden.potential-beforePotential} auf ${p.hidden.potential}; aktuelle Gesamtstärke +${(p.ca-beforeCa).coerceAtLeast(0)} auf ${p.ca}.","good")
  }else if(c.id==w.user.clubId)w.news("Intensivtraining abgeschlossen","${p.name} beendet das vierwöchige Programm ${project.focus.label}. Entwicklung wurde gezielt beschleunigt.","good")
 }
}
object CustomYouthSystem {
 fun remainingSlots(w:World):Int{val a=w.club().academy;if(a.customIntakeSeason!=w.calendar.season)return 2;return (2-a.customIntakeUsed).coerceAtLeast(0)}
 private fun compatible(identity:AcademyIdentity,role:PlayerRole)=when(identity){AcademyIdentity.POSSESSION->role in listOf(PlayerRole.PLAYMAKER,PlayerRole.DEEP_PLAYMAKER,PlayerRole.BALL_PLAYING_CB,PlayerRole.INVERTED_FULLBACK);AcademyIdentity.ATHLETIC->role in listOf(PlayerRole.PRESSING_FORWARD,PlayerRole.OVERLAPPING_FULLBACK,PlayerRole.BOX_TO_BOX);AcademyIdentity.STREET->role in listOf(PlayerRole.INSIDE_FORWARD,PlayerRole.PLAYMAKER,PlayerRole.POACHER);AcademyIdentity.DEFENSIVE->role in listOf(PlayerRole.STOPPER,PlayerRole.ANCHOR,PlayerRole.INVERTED_FULLBACK);AcademyIdentity.PRESSING->role in listOf(PlayerRole.PRESSING_FORWARD,PlayerRole.BOX_TO_BOX,PlayerRole.OVERLAPPING_FULLBACK);AcademyIdentity.BALANCED->true}
 fun compatibility(w:World,role:PlayerRole):Int{val c=w.club();return (48+c.academy.u19Quality/4+c.dynamics.staffQuality/5+(if(compatible(c.academy.identity,role))15 else -5)).coerceIn(20,95)}
 private fun attrs(base:Int,pos:Position,blueprint:YouthBlueprint):Attributes{
  fun v(d:Int=0)=(base+d).coerceIn(10,58)
  val a=Attributes(v(),v(),v(),v(),v(),v(),v(),v(),v(),if(pos==Position.TW)v(7) else 10,v())
  when(blueprint){YouthBlueprint.TECHNICAL->{a.technique=v(7);a.passing=v(4)};YouthBlueprint.PHYSICAL->{a.pace=v(6);a.strength=v(6);a.stamina=v(5)};YouthBlueprint.CREATIVE->{a.vision=v(8);a.passing=v(7);a.technique=v(4)};YouthBlueprint.DEFENSIVE->{a.tackling=v(8);a.strength=v(5);a.heading=v(4)};YouthBlueprint.FINISHER->{a.finishing=v(9);a.pace=v(4);a.heading=v(3)};YouthBlueprint.BALANCED->Unit}
  if(pos==Position.TW){a.keeping=v(10);a.finishing=10;a.tackling=v(-4);a.heading=v(-4)}
  return a
 }
 fun create(w:World,first:String,last:String,nationality:String,age:Int,position:Position,foot:Foot,blueprint:YouthBlueprint,role:PlayerRole):Player{
  require(w.live==null){"Nicht während eines laufenden Spiels."};require(first.trim().length in 2..30&&last.trim().length in 2..30){"Vor- und Nachname müssen 2 bis 30 Zeichen haben."};require(age in 15..17){"Jugendspieler müssen 15 bis 17 Jahre alt sein."};require(remainingSlots(w)>0){"Die zwei individuellen Jugendplätze dieser Saison sind bereits belegt."}
  val c=w.club();YouthEngine.configureClub(c);var made:Player?=null
  w.random{rng->val compatibility=compatibility(w,role);val base=(24+c.academy.u19Quality/10+c.stadium.youth/12+rng.int(-2,3)).coerceIn(22,44);val hidden=Hidden(potential=55,injuryProneness=rng.int(12,48),consistency=rng.int(40,72),professionalism=rng.int(42,76),loyalty=rng.int(45,88),ambition=rng.int(42,82),development=rng.int(55,84),pressure=rng.int(35,72));val p=Player(w.nextIds.player++,c.id,first.trim(),last.trim(),w.calendar.season-age,nationality=nationality.trim().ifBlank{"Deutschland"},foot=foot,position=position,number=(1..99).firstOrNull{n->w.squad(c.id).none{it.number==n}}?:99,attributes=attrs(base,position,blueprint),hidden=hidden,fitness=92.0,morale=68,form=6.2,sharpness=45,youth=true,wage=0,homegrownClubId=c.id,role=PlayerRole.AUTO)
   val learn=(48+c.academy.scouting/5+compatibility/10+rng.int(0,14)).coerceAtMost(91);p.youthProfile=YouthProfile(learning=learn,maturity=rng.int(32,58),path=when(rng.int(0,99)){in 0..13->DevelopmentPath.EARLY;in 14..31->DevelopmentPath.LATE;in 32..37->DevelopmentPath.PLATEAU;else->DevelopmentPath.NORMAL},familySupport=rng.int(45,90),adviserPressure=rng.int(15,62),schoolStress=rng.int(15,55),injuryGrowthRisk=rng.int(12,42),seniorTraining=false,roleSpark=role,confidence=rng.int(48,68),homegrownYears=1)
   val potentialCap=(70+c.academy.scouting/5).coerceAtMost(90);p.hidden.potential=(p.ca+12+learn/12+rng.int(0,7)).coerceAtMost(potentialCap).coerceAtLeast(p.ca+8);w.players[p.id]=p;made=p}
  if(c.academy.customIntakeSeason!=w.calendar.season){c.academy.customIntakeSeason=w.calendar.season;c.academy.customIntakeUsed=0};c.academy.customIntakeUsed++;val p=made!!;w.news("Jugendspieler aufgenommen","${p.name}, ${age} Jahre, ${position.label}. Grundstärke ${p.ca}; Entwicklung hängt jetzt von Academy, Co-Trainer, Einsätzen und Training ab.","good");return p
 }
}

object EconomySystem {
 private val sponsorNames=mapOf(
  SponsorCategory.MAIN to listOf("Nordstern Gruppe","Hansewerk","Union Capital","Rheinland Holding"),
  SponsorCategory.KIT to listOf("Kern Sport","Aero Athletic","Novus Teamwear","Nordkit"),
  SponsorCategory.YOUTH to listOf("FutureLab","Campus Partner","Talentwerk","NextGen Bildung"),
  SponsorCategory.STADIUM to listOf("Bauwerk Partner","Arena Technik","Stadtbau","GreenPitch"),
  SponsorCategory.REGIONAL to listOf("Bäckerei am Markt","Autohaus Zentrum","Stadtwerke Partner","Regionalbank")
 )
 fun initialize(w:World){for(c in w.clubs.values){if(c.sponsorDeals.isEmpty()){c.sponsorDeals.add(SponsorDeal(w.nextIds.sponsor++,SponsorCategory.MAIN,c.sponsor.name,c.sponsor.weekly,(c.sponsor.weekly*6L),0,104,0,0,0,true,0));c.commercialReputation=maxOf(c.commercialReputation,(c.reputation*.75).roundToInt())};if(c.id==w.user.clubId&&c.sponsorOffers.isEmpty()&&c.lastCommercialRefreshWeek<=-99)refreshOffers(w,c,true)}}
 fun refreshOffers(w:World,c:Club,force:Boolean=false){if(!force&&c.sponsorOffers.isNotEmpty()&&w.calendar.absoluteWeek-c.lastCommercialRefreshWeek<8)return;c.sponsorOffers.clear();val rng=SeededRandom(w.seed xor (c.id.toLong()*9151L) xor (w.calendar.absoluteWeek.toLong()*104729L));val active=c.sponsorDeals.filter{it.active}.map{it.category}.toSet();val categories=SponsorCategory.entries.filter{it !in active}.toMutableList();while(c.sponsorOffers.size<3&&categories.isNotEmpty()){val cat=categories.removeAt(rng.int(0,categories.lastIndex));val base=maxOf(120,c.sponsor.weekly,c.members/3+c.reputation*18+c.commercialReputation*8);val mult=when(cat){SponsorCategory.MAIN->1.7;SponsorCategory.KIT->1.15;SponsorCategory.YOUTH->.70;SponsorCategory.STADIUM->.82;SponsorCategory.REGIONAL->.48};val trust=.82+c.financialTrust/260.0;val weekly=(base*mult*trust*(.88+rng.nextDouble()*.28)).roundToInt().coerceAtLeast(80);val minRep=(c.reputation+rng.int(-8,7)).coerceIn(0,100);val deal=SponsorDeal(w.nextIds.sponsor++,cat,rng.pick(sponsorNames.getValue(cat)),weekly,weekly.toLong()*rng.int(6,14),weekly*rng.int(1,3),rng.int(36,78),minRep,if(cat==SponsorCategory.YOUTH)rng.int(2,5) else 0,if(cat==SponsorCategory.REGIONAL)rng.int(8,35) else 0,false,-1);c.sponsorOffers.add(deal)};c.lastCommercialRefreshWeek=w.calendar.absoluteWeek}
 fun accept(w:World,clubId:Int,dealId:Int){val c=w.clubs.getValue(clubId);val d=c.sponsorOffers.firstOrNull{it.id==dealId}?:error("Sponsorangebot nicht gefunden.");require(c.reputation>=d.minReputation){"Vereinsreputation reicht für dieses Angebot noch nicht."};require(c.sponsorDeals.count{it.active}<4){"Maximal vier aktive Partner gleichzeitig."};require(c.sponsorDeals.none{it.active&&it.category==d.category}){"Diese Partnerkategorie ist bereits belegt."};d.active=true;d.acceptedWeek=w.calendar.absoluteWeek;c.sponsorOffers.remove(d);c.sponsorDeals.add(d);c.budget+=d.signingBonus;c.financialTrust=(c.financialTrust+2).coerceAtMost(100);if(d.youthBoost>0){c.academy.partnerNetwork=(c.academy.partnerNetwork+d.youthBoost).coerceAtMost(100);c.academy.scouting=(c.academy.scouting+maxOf(1,d.youthBoost/2)).coerceAtMost(100)};if(d.membersBoost>0)c.members+=d.membersBoost;if(c.id==w.user.clubId)w.news("Neuer ${d.category.label}","${d.name}: ${d.weekly} €/Woche, ${d.signingBonus} € Handgeld, Laufzeit ${d.weeksLeft} Wochen.","good")}
 fun weekly(w:World,c:Club){if(c.sponsorDeals.isEmpty())initialize(w);if(w.calendar.absoluteWeek-c.lastCommercialRefreshWeek>=8)refreshOffers(w,c)
  val wins=if(c.form.lastOrNull()=="S")1 else 0;var sponsorIncome=0
  for(d in c.sponsorDeals.filter{it.active}.toList()){sponsorIncome+=d.weekly+wins*d.performanceBonus;d.weeksLeft--;if(d.weeksLeft<=0){d.active=false;if(c.id==w.user.clubId)w.news("Partnerschaft ausgelaufen","${d.name} ist ausgelaufen. Neue Angebote erscheinen im Vereinsbereich.","normal")}}
  c.sponsorDeals.removeAll{!it.active&&it.weeksLeft<=0};val membership=c.members*2;val kitFactor=if(c.sponsorDeals.any{it.active&&it.category==SponsorCategory.KIT})1.12 else 1.0;val mainFactor=if(c.sponsorDeals.any{it.active&&it.category==SponsorCategory.MAIN})1.08 else 1.0;val merchandising=(c.members*(c.commercialReputation+20)/80.0*kitFactor).roundToInt();val level=WorldFactory.leagueLevel(w,c.tier);val media=(c.reputation*c.reputation*(11-level).coerceAtLeast(1)/55.0*mainFactor).roundToInt();val commercialIncome=sponsorIncome+membership+merchandising+media
  val stadiumUpkeep=c.stadium.capacity/18+(c.stadium.pitchQuality+c.stadium.training+c.stadium.medicine+c.stadium.youth+c.stadium.gym)/3;val staffCost=(c.dynamics.staffQuality*c.dynamics.staffQuality/16.0).roundToInt();val academyCost=(c.academy.scouting+c.academy.u19Quality+c.academy.u23Quality+c.academy.boarding)*4;val partnerDiscount=if(c.sponsorDeals.any{it.active&&it.category==SponsorCategory.STADIUM})(stadiumUpkeep*.10).roundToInt() else 0;val nonWage=(stadiumUpkeep+staffCost+academyCost-partnerDiscount).coerceAtLeast(50);c.lastCosts=c.wageBill+nonWage;c.lastIncome+=commercialIncome;c.budget+=commercialIncome-c.lastCosts
  c.financialTrust=(c.financialTrust+(if(c.budget>=0)1 else -4)).coerceIn(0,100);val targetCommercial=(c.reputation*.7+c.members.coerceAtMost(30000)/1000.0).roundToInt().coerceIn(10,100);c.commercialReputation=(c.commercialReputation+(targetCommercial-c.commercialReputation).coerceIn(-1,1)).coerceIn(0,100)
  if(w.calendar.absoluteWeek%8==0){c.sponsorDeals.filter{it.active&&it.category==SponsorCategory.YOUTH}.forEach{c.academy.u19Quality=(c.academy.u19Quality+1).coerceAtMost(100)};c.sponsorDeals.filter{it.active&&it.category==SponsorCategory.REGIONAL}.forEach{c.members+=maxOf(1,it.membersBoost/4)}}
  if(c.id!=w.user.clubId&&c.sponsorOffers.isNotEmpty()&&c.sponsorDeals.count{it.active}<3){val best=c.sponsorOffers.filter{c.reputation>=it.minReputation}.maxByOrNull{it.weekly+it.signingBonus/20};if(best!=null)accept(w,c.id,best.id)}
 }
}

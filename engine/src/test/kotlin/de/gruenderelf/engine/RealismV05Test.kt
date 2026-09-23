package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class RealismV05Test {
 @Test fun competitionRulesEnforceFiveSubsAndThreeWindows(){
  val w=WorldFactory.createWorld(50001L);val m=MatchEngine.start(w);val home=w.user.clubId==m.homeId
  assertEquals(5,CompetitionRulesEngine.maxSubs(w,m))
  for(minute in listOf(20,45,70)){m.minute=minute;assertNull(CompetitionRulesEngine.substitutionIssue(w,m,home));CompetitionRulesEngine.registerSubstitution(m,home)}
  m.minute=80
  assertEquals("Die drei Wechselgelegenheiten sind bereits verbraucht.",CompetitionRulesEngine.substitutionIssue(w,m,home))
  m.homeSubs=if(home)5 else m.homeSubs;m.awaySubs=if(!home)5 else m.awaySubs
  assertEquals("Das Auswechselkontingent ist ausgeschöpft.",CompetitionRulesEngine.substitutionIssue(w,m,home))
 }

 @Test fun goalkeeperEightSecondRuleIsExplicit(){
  val rules=CompetitionRuleSet(goalkeeperControlSeconds=8)
  assertFalse(CompetitionRulesEngine.goalkeeperViolation(8,rules))
  assertTrue(CompetitionRulesEngine.goalkeeperViolation(9,rules))
 }

 @Test fun leagueCalibrationDiffersByLevel(){
  val w=WorldFactory.createWorld(50002L)
  val top=w.fixtures.first{it.competition==CompetitionType.LEAGUE&&w.clubs.getValue(it.homeId).tier==1}
  val lower=w.fixtures.first{it.competition==CompetitionType.LEAGUE&&w.clubs.getValue(it.homeId).tier==10}
  val a=LeagueCalibration.forFixture(w,top);val b=LeagueCalibration.forFixture(w,lower)
  assertTrue(a.conversion>b.conversion);assertTrue(b.turnover>a.turnover)
 }

 @Test fun modernUefaDrawUsesPotsAndAssociationLimits(){
  val selected=RealModeDatabase.leagues.first().clubs.first()
  val w=WorldFactory.createRealModeWorld(50003L,selected.key,PlayerDraft(firstName="UEFA",lastName="Regel",number=73,position=Position.ZM))
  for(type in listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE)){
   val phase=w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"};assertEquals(144,phase.size)
   val ids=phase.flatMap{listOf(it.homeId,it.awayId)}.distinct();assertEquals(36,ids.size)
   fun association(id:Int)=w.leagues.firstOrNull{ id in it.clubIds }?.let{RealModeDatabase.countryForLeague(it.name)}
    ?:w.clubs.getValue(id).city.substringAfterLast(", ").ifBlank{"International"}
   assertTrue(ids.map(::association).distinct().size>=8,"$type braucht ein echtes Mehrverbandsfeld")
   assertTrue(ids.map(::association).any{it !in setOf("Deutschland","England","Spanien","Italien","Frankreich")},"$type braucht Qualifikanten aus den erweiterten europäischen Ligen")
   val pots=ids.sortedWith(compareByDescending<Int>{w.clubs.getValue(it).reputation}.thenBy{it}).chunked(9)
   val potOf=pots.flatMapIndexed{i,pot->pot.map{it to i}}.toMap()
   for(id in ids){
    val games=phase.filter{it.homeId==id||it.awayId==id};assertEquals(8,games.size);assertEquals(4,games.count{it.homeId==id});assertEquals(4,games.count{it.awayId==id})
    val opponents=games.map{if(it.homeId==id)it.awayId else it.homeId};assertEquals(8,opponents.toSet().size)
    val byPot=opponents.groupingBy{potOf.getValue(it)}.eachCount();for(pot in 0..3)assertEquals(2,byPot[pot]?:0,"$type club=$id pot=$pot")
    assertTrue(opponents.none{association(it)==association(id)},"Same-association pairing for $id")
    assertTrue(opponents.groupingBy{association(it)}.eachCount().values.all{it<=2})
   }
  }
 }

 @Test fun targetedYouthSearchCostsMoneyAndStillLimitsIntake(){
  val w=WorldFactory.createWorld(50004L);w.club().budget=2_000_000L
  val cost=CustomYouthSystem.searchCost(w,16,Position.ZM,Foot.BOTH,YouthBlueprint.CREATIVE,PlayerRole.PLAYMAKER);val before=w.club().budget
  val p=CustomYouthSystem.create(w,"Luca","Scoutfund","Deutschland",16,Position.ZM,Foot.BOTH,YouthBlueprint.CREATIVE,PlayerRole.PLAYMAKER)
  assertEquals(before-cost,w.club().budget);assertTrue(p.youth);assertTrue(p.hidden.potential>p.ca);assertEquals(1,CustomYouthSystem.remainingSlots(w))
 }

 @Test fun assistantProfilesChangePlanningAndPersist(){
  val w=WorldFactory.createWorld(50005L);w.assistantCoach.profile=AssistantCoachProfile.ANALYST;w.club().dynamics.fatigueLoad=20
  val analyst=AssistantCoachSystem.recommendedPlan(w);assertTrue(analyst.opponentPrep);assertTrue(UnitType.VIDEO in analyst.days)
  w.assistantCoach.profile=AssistantCoachProfile.DEVELOPER;val developer=AssistantCoachSystem.recommendedPlan(w);assertTrue(UnitType.TECHNIQUE in developer.days)
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertEquals(AssistantCoachProfile.DEVELOPER,copy.assistantCoach.profile)
 }

 @Test fun transferRunsThroughMedicalAndRegistration(){
  val w=WorldFactory.createWorld(50006L);val buyer=w.club();buyer.budget=900_000_000L
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id};p.hidden.injuryProneness=20;p.injuryWeeks=0
  val o=TransferEngine.createOffer(w,buyer.id,p.id,DealType.BUY,SquadRole.STARTER)
  o.fee=TransferEngine.askingPrice(w,w.clubs[p.clubId],p,DealType.BUY)*3;o.wage=maxOf(p.wage*10,5_000);o.signingBonus=2_000_000;o.playingTimePromise=100;o.sellOnPercent=20
  TransferEngine.evaluate(w,o);assertEquals(TransferStage.MEDICAL,o.stage);assertEquals(NegotiationStatus.AGREED,o.status)
  TransferEngine.advanceProcess(w,o.id);assertTrue(o.medicalPassed);assertEquals(TransferStage.REGISTRATION,o.stage);assertTrue(o.registrationReady)
  TransferEngine.complete(w,o.id);assertEquals(TransferStage.COMPLETED,o.stage);assertEquals(buyer.id,p.clubId)
 }
 @Test fun transferReservationsPreventDoubleSpending(){
  val w=WorldFactory.createWorld(50007L);val buyer=w.club();buyer.budget=900_000_000L
  val targets=w.players.values.filter{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id}.take(2);assertEquals(2,targets.size)
  fun ready(p:Player):TransferOffer{
   p.hidden.injuryProneness=10;p.injuryWeeks=0
   val o=TransferEngine.createOffer(w,buyer.id,p.id,DealType.BUY,SquadRole.STAR);o.fee=TransferEngine.askingPrice(w,w.clubs[p.clubId],p,DealType.BUY)*3;o.wage=maxOf(p.wage*12,8_000);o.signingBonus=1_000_000;o.playingTimePromise=100;o.sellOnPercent=25;TransferEngine.evaluate(w,o);assertEquals(TransferStage.MEDICAL,o.stage);TransferEngine.advanceProcess(w,o.id);assertTrue(o.registrationReady);return o
  }
  val first=ready(targets[0]);val firstCost=first.fee+first.signingBonus
  val second=TransferEngine.createOffer(w,buyer.id,targets[1].id,DealType.BUY,SquadRole.STAR);second.fee=TransferEngine.askingPrice(w,w.clubs[targets[1].clubId],targets[1],DealType.BUY)*3;second.wage=maxOf(targets[1].wage*12,8_000);second.signingBonus=1_000_000;second.playingTimePromise=100;second.sellOnPercent=25
  buyer.budget=firstCost+second.fee+second.signingBonus-1
  TransferEngine.evaluate(w,second)
  assertEquals(NegotiationStatus.REJECTED,second.status);assertTrue(second.message.contains("reservierten Transfers"))
 }

 @Test fun targetedYouthSearchPriceReflectsSpecificityAndNetwork(){
  val w=WorldFactory.createWorld(50008L);val c=w.club();c.academy.scouting=20;c.academy.partnerNetwork=10
  val broad=CustomYouthSystem.searchCost(w,16,Position.ZM,Foot.RIGHT,YouthBlueprint.BALANCED,PlayerRole.AUTO)
  val specific=CustomYouthSystem.searchCost(w,15,Position.TW,Foot.BOTH,YouthBlueprint.TECHNICAL,PlayerRole.BUILD_UP_KEEPER)
  assertTrue(specific>broad)
  c.academy.scouting=90;c.academy.partnerNetwork=70
  assertTrue(CustomYouthSystem.searchCost(w,15,Position.TW,Foot.BOTH,YouthBlueprint.TECHNICAL,PlayerRole.BUILD_UP_KEEPER)<specific)
 }

}

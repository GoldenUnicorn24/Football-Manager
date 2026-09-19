package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class AdvancedSystemsTest {
 @Test fun trainingAffectsEveryClubAndPersistsVisibleCauses(){
  val w=WorldFactory.createWorld(46801L);val before=w.clubs.mapValues{it.value.dynamics.tacticalUnderstanding}
  w.training.days=mutableListOf(UnitType.RECOVERY,UnitType.TACTICS,UnitType.POSITIONAL,UnitType.VIDEO,UnitType.TEAM_BONDING,UnitType.SET_PIECES,UnitType.OFF)
  w.training.intensity=4;TrainingEngine.apply(w)
  assertTrue(w.clubs.values.all{it.dynamics.tacticalUnderstanding>=before.getValue(it.id)})
  assertTrue(w.clubs.values.any{it.id!=w.user.clubId&&it.dynamics.tacticalUnderstanding>before.getValue(it.id)})
  assertTrue(w.training.lastReport.effects.any{it.contains("Chemie")})
  assertTrue(w.club().dynamics.patterns.getValue("STANDARDS")>30)
 }

 @Test fun academyReadinessUsesMoreThanPotentialAndOverpromotionHasConsequences(){
  val w=WorldFactory.createWorld(46802L);val p=w.squad().first{it.youth};p.hidden.potential=99;p.youthProfile.maturity=20;p.youthProfile.schoolStress=90;p.youthProfile.injuryGrowthRisk=80
  val readiness=YouthEngine.readiness(w,p);assertTrue(readiness<70)
  val morale=p.morale;ClubActions.promote(w,p.id);assertFalse(p.youth);assertTrue(p.morale<=morale+4)
 }

 @Test fun negotiatedTransferNeedsClubPlayerAndAgentAndStoresClauses(){
  val w=WorldFactory.createWorld(46803L);val buyer=w.club();buyer.budget=800_000_000L
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id};val sellerId=p.clubId
  val o=TransferEngine.createOffer(w,buyer.id,p.id,DealType.BUY,SquadRole.STARTER)
  assertNotEquals(NegotiationStatus.COMPLETED,o.status)
  o.fee=TransferEngine.askingPrice(w,w.clubs[sellerId],p,DealType.BUY)*3;o.wage=maxOf(p.wage*10,5_000);o.signingBonus=2_000_000;o.playingTimePromise=100;o.sellOnPercent=20;o.releaseClause=TransferEngine.marketValue(w,p)*3;o.buyBackClause=TransferEngine.marketValue(w,p)*2
  TransferEngine.evaluate(w,o);assertEquals(NegotiationStatus.AGREED,o.status);TransferEngine.complete(w,o.id)
  assertEquals(buyer.id,p.clubId);assertEquals(20,p.sellOnPercentToPrevious);assertEquals(sellerId,p.buyBackClubId);assertTrue(p.releaseClause>0)
 }

 @Test fun loanWithOptionReturnsOrCanBeBought(){
  val w=WorldFactory.createWorld(46804L);val buyer=w.club();buyer.budget=800_000_000L
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id};val parent=p.clubId
  val o=TransferEngine.createOffer(w,buyer.id,p.id,DealType.LOAN_OPTION,SquadRole.ROTATION);o.fee=TransferEngine.askingPrice(w,w.clubs[parent],p,DealType.LOAN_OPTION)*4;o.wage=maxOf(p.wage*10,5_000);o.signingBonus=1_000_000;o.playingTimePromise=100;TransferEngine.evaluate(w,o);assertEquals(NegotiationStatus.AGREED,o.status);TransferEngine.complete(w,o.id)
  assertEquals(parent,p.loanParentClubId);assertTrue(p.loanOptionFee>0);val option=p.loanOptionFee;buyer.budget=maxOf(buyer.budget,option+1);TransferEngine.exerciseOption(w,p.id);assertEquals(0,p.loanParentClubId);assertEquals(buyer.id,p.clubId)
 }

 @Test fun matchAiScoutsWeaknessAndUsesTrainingState(){
  val w=WorldFactory.createWorld(46805L);val f=w.nextFixture()!!;val oppId=if(f.homeId==w.user.clubId)f.awayId else f.homeId
  w.squad(oppId).filter{it.position==Position.IV}.forEach{it.attributes.pace=20}
  w.club().dynamics.opponentPrep=80;w.club().dynamics.patterns["AUFBAU"]=90
  val m=MatchEngine.start(w,f);val userProfile=if(w.user.clubId==m.homeId)m.homeAi else m.awayAi
  assertEquals("Langsame Innenverteidiger",userProfile.weakness);assertEquals(BuildUp.COUNTER,userProfile.approach)
  assertTrue(MatchIntelligence.teamFactor(w.club(),true)>.9)
 }

 @Test fun newSystemStateSurvivesSaveRoundtrip(){
  val w=WorldFactory.createWorld(46806L);w.club().dynamics.chemistry=88;w.club().academy.identity=AcademyIdentity.STREET;w.training.intensity=5
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertEquals(4,copy.saveVersion);assertEquals(88,copy.club().dynamics.chemistry);assertEquals(AcademyIdentity.STREET,copy.club().academy.identity);assertEquals(5,copy.training.intensity)
 }

 @Test fun aiClubExercisesSensibleLoanOptionBeforeExpiry(){
  val w=WorldFactory.createWorld(46807L);val buyer=w.clubs.values.first{it.id!=w.user.clubId&&it.tier<=6};buyer.budget=500_000_000L
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id};val parent=p.clubId
  p.clubId=buyer.id;p.loanParentClubId=parent;p.loanBuyerClubId=buyer.id;p.loanWeeks=1;p.loanOptionFee=maxOf(1L,TransferEngine.marketValue(w,p)/10);p.hidden.potential=maxOf(p.hidden.potential,p.ca+10)
  TransferEngine.processLoans(w)
  assertEquals(buyer.id,p.clubId);assertEquals(0,p.loanParentClubId);assertEquals(0,p.loanWeeks);assertEquals(0L,p.loanOptionFee)
 }
}

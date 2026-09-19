package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class ManagerSystemsV0469Test {
 @Test fun targetClubInterestIsStableAndMoveWishMatters(){
  val w=WorldFactory.createWorld(46901L);val target=w.club()
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=target.id}
  p.wantsMove=false;p.hidden.loyalty=55;p.hidden.ambition=55
  val a=TransferInterestSystem.snapshot(w,p,target.id);val b=TransferInterestSystem.snapshot(w,p,target.id)
  assertEquals(a,b)
  p.wantsMove=true
  val eager=TransferInterestSystem.snapshot(w,p,target.id)
  assertTrue(eager.score>a.score,"Ein expliziter Wechselwunsch muss die Bereitschaft erhöhen.")
  assertTrue(eager.reasons.any{it.contains("wechselwillig")})
 }

 @Test fun assistantCoachBuildsSeniorPlanAndManagesYouth(){
  val w=WorldFactory.createWorld(46902L);w.assistantCoach.autoSeniorTraining=true;w.assistantCoach.autoYouthTraining=true;w.assistantCoach.trainingStyle=AssistantTrainingStyle.DEVELOPMENT
  val youth=w.squad().first{it.youth};youth.youthProfile.maturity=90;youth.youthProfile.learning=90;youth.youthProfile.schoolStress=0;youth.youthProfile.injuryGrowthRisk=0;youth.youthProfile.growthSpurtWeeks=0;youth.hidden.potential=maxOf(youth.hidden.potential,youth.ca+20);w.club().dynamics.fatigueLoad=10
  val plan=AssistantCoachSystem.prepareUserPlan(w);AssistantCoachSystem.prepareYouth(w,w.club())
  assertEquals(7,plan.days.size);assertTrue(UnitType.TECHNIQUE in plan.days);assertTrue(plan.extra.isNotEmpty());assertTrue(w.assistantCoach.lastPlanReason.isNotBlank());assertTrue(w.assistantCoach.lastYouthReason.isNotBlank())
  assertTrue(youth.youthProfile.mentorId==0||w.players[youth.youthProfile.mentorId]?.clubId==w.user.clubId)
 }

 @Test fun customYouthHasConstrainedStartAndOnlyTwoSlotsPerSeason(){
  val w=WorldFactory.createWorld(46903L)
  val one=CustomYouthSystem.create(w,"Noah","Testtalent","Deutschland",16,Position.ZM,Foot.RIGHT,YouthBlueprint.CREATIVE,PlayerRole.PLAYMAKER)
  val two=CustomYouthSystem.create(w,"Mika","Zweittalent","Deutschland",15,Position.ST,Foot.LEFT,YouthBlueprint.FINISHER,PlayerRole.POACHER)
  assertTrue(one.youth&&two.youth);assertEquals("Noah Testtalent",one.name);assertTrue(one.attributes.values().values.maxOrNull()!!<=58);assertTrue(one.ca<60);assertTrue(one.hidden.potential>one.ca);assertTrue(one.hidden.potential<=90);assertEquals(0,CustomYouthSystem.remainingSlots(w))
  assertFailsWith<IllegalArgumentException>{CustomYouthSystem.create(w,"Drittes","Talent","Deutschland",16,Position.IV,Foot.RIGHT,YouthBlueprint.DEFENSIVE,PlayerRole.STOPPER)}
 }

 @Test fun intensiveTrainingCostsMoneyAndCreatesTargetedProgress(){
  val w=WorldFactory.createWorld(46904L);val p=w.squad().first{!it.youth&&!it.retired&&it.position!=Position.TW};w.club().budget=50_000_000L;p.hidden.potential=maxOf(p.hidden.potential,p.ca+25);p.hidden.injuryProneness=0;w.club().stadium.medicine=100;w.club().stadium.training=100;w.club().stadium.gym=100;w.club().dynamics.staffQuality=100
  val beforeBudget=w.club().budget;val before=p.attributes.technique;val price=IntensiveTrainingSystem.cost(w,p);IntensiveTrainingSystem.start(w,p.id,Focus.TECHNIQUE)
  assertEquals(beforeBudget-price,w.club().budget);assertTrue(w.intensiveTraining.any{it.playerId==p.id})
  // Make the test deterministic around the threshold while still exercising the real weekly path.
  w.intensiveTraining.first{it.playerId==p.id}.progress=.95
  IntensiveTrainingSystem.applyWeek(w,w.club(),p,TrainingReport(),SeededRandom(469040L))
  assertTrue(p.attributes.technique>before);assertTrue(p.fitness<95.0);assertTrue(w.intensiveTraining.first{it.playerId==p.id}.weeksLeft==3)
 }

 @Test fun sponsorOffersCanBeAcceptedAndEconomyUsesMultipleStreams(){
  val w=WorldFactory.createWorld(46905L);val c=w.club();c.reputation=100;c.budget=10_000_000L;EconomySystem.initialize(w);EconomySystem.refreshOffers(w,c,true)
  assertTrue(c.sponsorDeals.any{it.active});assertTrue(c.sponsorOffers.isNotEmpty())
  val deal=c.sponsorOffers.first();val beforeBudget=c.budget;EconomySystem.accept(w,c.id,deal.id)
  assertTrue(c.sponsorDeals.any{it.id==deal.id&&it.active});assertEquals(beforeBudget+deal.signingBonus,c.budget)
  val weeks=deal.weeksLeft;EconomySystem.weekly(w,c);assertEquals(weeks-1,deal.weeksLeft);assertTrue(c.lastIncome>0);assertTrue(c.lastCosts>0)
 }

 @Test fun assistantSubstitutionBecomesAcceptRejectProposal(){
  val w=WorldFactory.createWorld(46907L);w.assistantCoach.autoSubstitutions=true;w.assistantCoach.substitutionAggression=5
  val m=MatchEngine.start(w);val ownHome=w.user.clubId==m.homeId;val xi=if(ownHome)m.homeXi else m.awayXi;val out=xi.first{it!=0&&w.players.getValue(it).position!=Position.TW};w.players.getValue(out).fitness=38.0
  m.minute=59
  var guard=0;while(!m.assistantSubPending&&guard++<120&&!m.finished){when{m.pendingDecision->MatchEngine.decide(w,m,Decision.PASS);m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.halfTime->MatchEngine.secondHalf(m);else->MatchEngine.step(w,m)}}
  assertTrue(m.assistantSubPending,"Der Co-Trainer muss einen pausierenden Vorschlag erzeugen statt selbst zu wechseln.")
  val proposedOut=m.assistantSubOutId;val proposedIn=m.assistantSubInId;val subsBefore=if(ownHome)m.homeSubs else m.awaySubs
  MatchEngine.rejectAssistantSubstitution(w,m);assertFalse(m.assistantSubPending);assertEquals(subsBefore,if(ownHome)m.homeSubs else m.awaySubs);assertEquals(proposedOut,m.assistantSubRejectedOutId);assertEquals(proposedIn,m.assistantSubRejectedInId);assertTrue(m.assistantSubRejectedUntilMinute>m.minute)
  m.assistantSubRejectedUntilMinute=0;m.minute=maxOf(m.minute,71);w.players.getValue(proposedOut).fitness=35.0
  guard=0;while(!m.assistantSubPending&&guard++<120&&!m.finished){when{m.pendingDecision->MatchEngine.decide(w,m,Decision.PASS);m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.halfTime->MatchEngine.secondHalf(m);else->MatchEngine.step(w,m)}}
  assertTrue(m.assistantSubPending);val acceptedIn=m.assistantSubInId;MatchEngine.acceptAssistantSubstitution(w,m);assertFalse(m.assistantSubPending);assertEquals(subsBefore+1,if(ownHome)m.homeSubs else m.awaySubs);assertTrue(acceptedIn in (if(ownHome)m.homeXi else m.awayXi))
 }

 @Test fun newManagerSystemsSurviveSaveRoundtrip(){
  val w=WorldFactory.createWorld(46906L);w.assistantCoach.autoSeniorTraining=true;w.assistantCoach.autoYouthTraining=true;w.assistantCoach.autoSubstitutions=true;w.assistantCoach.trainingStyle=AssistantTrainingStyle.MATCH_PREP
  val p=w.squad().first{!it.youth};p.hidden.potential=maxOf(p.hidden.potential,p.ca+10);w.club().budget=50_000_000L;IntensiveTrainingSystem.start(w,p.id,Focus.VISION);EconomySystem.initialize(w)
  val copy=SaveCodec.decode(SaveCodec.encode(w))
  assertEquals(4,copy.saveVersion);assertTrue(copy.assistantCoach.autoSeniorTraining);assertTrue(copy.assistantCoach.autoYouthTraining);assertTrue(copy.assistantCoach.autoSubstitutions);assertEquals(AssistantTrainingStyle.MATCH_PREP,copy.assistantCoach.trainingStyle);assertTrue(copy.intensiveTraining.any{it.playerId==p.id});assertTrue(copy.club().sponsorDeals.isNotEmpty())
 }
}

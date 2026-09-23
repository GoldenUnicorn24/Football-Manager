package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class RecoveryV0518Test {
 @Test fun recoveredModelRoundtripKeepsV0518State(){
  val w=WorldFactory.createWorld(51801L);val p=w.squad().first{it.youth};p.youthSquad=YouthSquad.U23;p.youthTeamStats.goals=4
  w.watchlist+=p.id;w.scoutReports[p.id]=ScoutReport(p.id,82,p.ca-2,p.ca+2,p.hidden.potential-3,p.hidden.potential+3,true,false,w.calendar.absoluteWeek,"Test")
  w.savedTactics["Plan A"]=SavedTactic("4-2-3-1",4,5,4,5,4,BuildUp.COUNTER);w.notifications.seenTiredPlayerIds+=p.id;w.user.playerCareerFocus=PlayerCareerFocus.GOALGETTER
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertEquals(SAVE_VERSION,copy.saveVersion);assertEquals(YouthSquad.U23,copy.players.getValue(p.id).youthSquad);assertEquals(4,copy.players.getValue(p.id).youthTeamStats.goals);assertTrue(p.id in copy.watchlist);assertEquals(82,copy.scoutReports.getValue(p.id).progress);assertEquals("4-2-3-1",copy.savedTactics.getValue("Plan A").formation);assertEquals(PlayerCareerFocus.GOALGETTER,copy.user.playerCareerFocus)
 }

 @Test fun u19AndU23RunOwnCompetitionAndTemporaryCallUpReturns(){
  val w=WorldFactory.createWorld(51802L);val youth=w.squad().first{it.youth};youth.birthYear=w.calendar.season-18;youth.youthSquad=YouthSquad.U19
  val before=w.club().academy.u19Season.played;YouthCompetitionSystem.weekly(w,SeededRandom(518020L));assertTrue(w.club().academy.u19Season.played>before);assertTrue(youth.youthTeamStats.appearances>0)
  YouthCompetitionSystem.callUp(w,youth.id);assertFalse(youth.youth);assertTrue(youth.temporarySeniorCallUp);YouthCompetitionSystem.returnToYouth(w,youth.id);assertTrue(youth.youth);assertEquals(YouthSquad.U19,youth.youthSquad);assertFalse(youth.temporarySeniorCallUp)
 }

 @Test fun scoutingProgressesToUsefulReportAndTracksCompetition(){
  val w=WorldFactory.createWorld(51803L);w.club().budget=50_000_000L;val p=w.players.values.first{!it.retired&&it.clubId!=w.user.clubId};ScoutingTransferSystem.start(w,p.id,ScoutRegion.EUROPE,2);assertTrue(p.id in w.watchlist);assertTrue(p.id in w.scoutAssignments)
  repeat(3){ScoutingTransferSystem.weekly(w,SeededRandom(518030L+it))};val report=w.scoutReports.getValue(p.id);assertTrue(report.progress>0);assertTrue(report.caMin<=p.ca&&report.caMax>=p.ca);assertTrue(report.potentialMin<=p.hidden.potential&&report.potentialMax>=p.hidden.potential)
 }

 @Test fun potentialTrainingCanRaiseDevelopmentCeiling(){
  val w=WorldFactory.createWorld(51804L);w.club().budget=50_000_000L;val p=w.squad().first{!it.retired&&it.position!=Position.TW};p.birthYear=w.calendar.season-19;p.hidden.potential=p.ca.coerceAtMost(96);p.hidden.development=90;p.hidden.professionalism=90;w.club().stadium.youth=90;w.club().stadium.training=100;w.club().stadium.gym=100;w.club().stadium.medicine=100;w.club().dynamics.staffQuality=100
  val before=p.hidden.potential;IntensiveTrainingSystem.startPotential(w,p.id,Focus.TECHNIQUE);val project=w.intensiveTraining.single{it.playerId==p.id};project.weeksLeft=1;project.progress=.99;IntensiveTrainingSystem.applyWeek(w,w.club(),p,TrainingReport(),SeededRandom(518040L));assertTrue(p.hidden.potential>before);assertTrue(project !in w.intensiveTraining)
 }

 @Test fun medicalRegistrationCompletesTransferOnceWithoutRestartLoop(){
  val w=WorldFactory.createWorld(51805L);w.club().budget=50_000_000L;val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId};p.hidden.injuryProneness=5;p.injuryWeeks=0;val previous=p.clubId
  val o=TransferOffer(id=w.nextIds.negotiation++,buyerClubId=w.user.clubId,sellerClubId=previous,playerId=p.id,type=DealType.BUY,role=SquadRole.ROTATION,fee=1_000L,wage=maxOf(5,p.wage),signingBonus=100L,status=NegotiationStatus.AGREED,stage=TransferStage.MEDICAL);w.negotiations[o.id]=o
  assertTrue(TransferV0518System.medicalAndRegistration(w,o));assertEquals(TransferStage.REGISTRATION,o.stage);assertTrue(o.registrationReady);TransferEngine.complete(w,o.id);assertEquals(TransferStage.COMPLETED,o.stage);assertEquals(NegotiationStatus.COMPLETED,o.status);assertEquals(w.user.clubId,p.clubId);assertTrue(w.transferHistory.any{it.playerId==p.id&&it.fromClubId==previous&&it.toClubId==w.user.clubId})
 }

 @Test fun notificationsAndMatchAnalysisAreFunctional(){
  val w=WorldFactory.createWorld(51806L);val tired=w.squad().first{!it.youth};tired.fitness=50.0;tired.contractYears=1;assertTrue(tired.id in NotificationSystem.unreadTired(w));assertTrue(tired.id in NotificationSystem.unreadExpiring(w));NotificationSystem.markTiredSeen(w);NotificationSystem.markContractsSeen(w);assertFalse(tired.id in NotificationSystem.unreadTired(w));assertFalse(tired.id in NotificationSystem.unreadExpiring(w))
  val m=MatchEngine.start(w);m.minute=31;m.shotEvents+=ShotEvent(10,w.user.clubId,tired.id,xg=.25);m.shotEvents+=ShotEvent(30,w.user.clubId,tired.id,xg=.40);m.passEvents+=PassEvent(12,w.user.clubId,tired.id,w.user.playerId,true,.2f,.3f,.4f,.5f);MatchAnalysisSystem.recordTacticChange(w,m,w.user.clubId,"Pressing erhöht");val timeline=MatchAnalysisSystem.xgTimeline(m,w.user.clubId);assertTrue(timeline.last().second>=.65);val network=MatchAnalysisSystem.passNetwork(m,w.user.clubId);assertTrue(network.first.isNotEmpty());assertTrue(network.second.single().count==1);assertEquals(1,m.tacticChanges.size)
 }

 @Test fun manualOutboundRoundCreatesSeveralIndependentOffers(){
  val w=WorldFactory.createWorld(51807L);w.calendar.matchday=1
  val p=w.squad().first{it.id!=w.user.playerId&&!it.retired&&it.loanParentClubId==0};p.wantsMove=true;p.hidden.ambition=90;p.hidden.loyalty=20
  w.clubs.values.filter{it.id!=w.user.clubId}.forEach{it.budget=100_000_000L;it.reputation=90}
  val offers=OutboundTransferSystem.requestOffers(w,p.id,DealType.BUY)
  assertTrue(offers.size>=2);assertEquals(offers.size,offers.map{it.buyerClubId}.distinct().size);assertEquals(offers.map{it.id}.toSet(),OutboundTransferSystem.activeOffers(w,p.id).map{it.id}.toSet())
  val replaced=offers.map{it.id}.toSet();val next=OutboundTransferSystem.requestOffers(w,p.id,DealType.LOAN)
  assertTrue(next.isNotEmpty());assertTrue(w.negotiations.values.filter{it.id in replaced}.all{it.status==NegotiationStatus.REJECTED})
 }

 @Test fun bosmanPrecontractMovesPlayerAtSeasonBoundaryWithoutFee(){
  val w=WorldFactory.createWorld(51808L);w.calendar.matchday=15;w.club().budget=100_000_000L;w.club().reputation=100
  val p=w.players.values.filter{!it.retired&&it.clubId!=0&&it.clubId!=w.user.clubId&&it.loanParentClubId==0}.maxBy{TransferInterestSystem.score(w,it,w.user.clubId)}
  p.contractYears=1;p.wantsMove=true;p.hidden.ambition=90;p.hidden.loyalty=10
  assertTrue(TransferV0518System.canSignPrecontract(w,p));val previous=p.clubId
  TransferV0518System.signPrecontract(w,p.id,3);assertEquals(w.user.clubId,p.precontractClubId)
  TransferV0518System.processSeasonContracts(w,SeededRandom(518080L))
  assertEquals(w.user.clubId,p.clubId);assertEquals(3,p.contractYears);assertEquals(0,p.precontractClubId)
  assertTrue(w.transferHistory.any{it.playerId==p.id&&it.fromClubId==previous&&it.toClubId==w.user.clubId&&it.fee==0L&&it.note.contains("Bosman")})
 }

 @Test fun incomingYoungPlayerCanBeRegisteredDirectlyForU19(){
  val w=WorldFactory.createWorld(51809L);w.club().budget=100_000_000L
  val p=w.players.values.first{!it.retired&&it.clubId!=0&&it.clubId!=w.user.clubId};p.birthYear=w.calendar.season-18;p.hidden.injuryProneness=5;p.injuryWeeks=0
  val previous=p.clubId;val o=TransferOffer(id=w.nextIds.negotiation++,buyerClubId=w.user.clubId,sellerClubId=previous,playerId=p.id,type=DealType.BUY,role=SquadRole.PROSPECT,fee=1_000L,wage=maxOf(5,p.wage),status=NegotiationStatus.AGREED,stage=TransferStage.MEDICAL);w.negotiations[o.id]=o
  TransferV0518System.setTargetYouthSquad(w,o.id,YouthSquad.U19);assertEquals(YouthSquad.U19,o.targetYouthSquad)
  assertTrue(TransferV0518System.medicalAndRegistration(w,o));TransferEngine.complete(w,o.id)
  assertTrue(p.youth);assertEquals(YouthSquad.U19,p.youthSquad);assertEquals(w.user.clubId,p.clubId)
 }

 @Test fun savedMatchAnalysisExposesTimelineAndPassNetwork(){
  val w=WorldFactory.createWorld(51810L);val live=MatchEngine.start(w);val id=w.user.clubId;val p=w.squad().first{!it.youth}
  live.minute=90;live.shotEvents+=ShotEvent(12,id,p.id,xg=.20);live.shotEvents+=ShotEvent(67,id,p.id,xg=.55);live.passEvents+=PassEvent(20,id,p.id,w.user.playerId,true,.2f,.3f,.5f,.4f)
  MatchEngine.record(w,live);val rec=w.matches.values.maxBy{it.fixtureId};val timeline=MatchAnalysisSystem.xgTimeline(rec,id);val network=MatchAnalysisSystem.passNetwork(rec,id)
  assertTrue(timeline.last().second>=.75);assertTrue(network.first.isNotEmpty());assertEquals(1,network.second.single().count)
 }

}

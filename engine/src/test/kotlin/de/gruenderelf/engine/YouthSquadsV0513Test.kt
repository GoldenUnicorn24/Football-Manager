package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class YouthSquadsV0513Test {
 @Test fun youthPlayerCanBeSoldDirectlyWithSeveralOffers(){
  val w=WorldFactory.createWorld(51301L);val y=w.squad().first{it.youth};y.wantsMove=true;val originalSquad=y.youthSquad
  val offers=OutboundTransferSystem.requestOffers(w,y.id,DealType.BUY)
  assertTrue(offers.size>=2);assertTrue(offers.map{it.buyerClubId}.distinct().size>=2);assertTrue(y.youth)
  val chosen=offers.last();OutboundTransferSystem.accept(w,chosen.id)
  assertEquals(chosen.buyerClubId,y.clubId);assertTrue(y.youth,"Ein verkaufter Nachwuchsspieler soll beim neuen Verein Nachwuchsspieler bleiben.");assertEquals(originalSquad,y.youthSquad)
 }

 @Test fun youthLoanRestoresOriginalYouthTeamOnReturn(){
  val w=WorldFactory.createWorld(51302L);val y=w.squad().first{it.youth};y.wantsMove=true;val squad=y.youthSquad
  val chosen=OutboundTransferSystem.requestOffers(w,y.id,DealType.LOAN).last();OutboundTransferSystem.accept(w,chosen.id)
  assertEquals(w.user.clubId,y.loanParentClubId);assertFalse(y.youth,"Die Leihstation soll den Spieler im normalen Kader einsetzen können.");assertTrue(y.loanReturnYouth)
  y.loanWeeks=1;TransferEngine.processLoans(w)
  assertEquals(w.user.clubId,y.clubId);assertTrue(y.youth);assertEquals(squad,y.youthSquad);assertFalse(y.loanReturnYouth)
 }

 @Test fun youngSeniorCanBeAssignedBackAndDevelopsFasterInYouthSquad(){
  val base=WorldFactory.createWorld(51303L);val p=base.squad().first{!it.youth&&it.id!=base.user.playerId};p.birthYear=base.calendar.season-20;p.hidden.potential=maxOf(p.hidden.potential,p.ca+20);p.youthProfile.learning=90
  val senior=SaveCodec.copy(base);val youth=SaveCodec.copy(base)
  YouthCompetitionSystem.assignToYouth(youth,p.id,YouthSquad.U23)
  assertTrue(youth.players.getValue(p.id).youth);assertEquals(YouthSquad.U23,youth.players.getValue(p.id).youthSquad)
  YouthEngine.weekly(senior,SeededRandom(513030L));YouthEngine.weekly(youth,SeededRandom(513030L))
  assertTrue(youth.players.getValue(p.id).trainingProgress>senior.players.getValue(p.id).trainingProgress,"U19/U23-Zuordnung muss einen echten Academy-Entwicklungsbonus erzeugen.")
 }

 @Test fun emergencyCallUpReturnsAutomaticallyAfterNextSeniorMatch(){
  val w=WorldFactory.createWorld(51304L);val y=w.squad().first{it.youth};val original=y.youthSquad
  YouthCompetitionSystem.temporaryCallUp(w,y.id)
  assertFalse(y.youth);assertTrue(y.temporarySeniorCallUp);assertTrue(y.available)
  val m=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);MatchEngine.record(w,m)
  assertTrue(y.youth);assertEquals(original,y.youthSquad);assertFalse(y.temporarySeniorCallUp);assertNull(y.temporaryReturnSquad)
 }

 @Test fun incomingYoungPlayerCanBeBoughtDirectlyForU19OrU23(){
  val w=WorldFactory.createWorld(51305L);w.club().budget=10_000_000_000L
  val external=w.players.values.first{it.clubId!=0&&it.clubId!=w.user.clubId&&!it.retired&&it.id!=w.user.playerId&&it.loanParentClubId==0};external.birthYear=w.calendar.season-20;external.youth=false
  val o=TransferEngine.createOffer(w,w.user.clubId,external.id,DealType.BUY,SquadRole.PROSPECT)
  o.fee=0;o.signingBonus=0;o.status=NegotiationStatus.AGREED;o.stage=TransferStage.REGISTRATION;o.registrationReady=true;o.targetYouthSquad=YouthSquad.U23
  TransferEngine.complete(w,o.id)
  assertEquals(w.user.clubId,external.clubId);assertTrue(external.youth);assertEquals(YouthSquad.U23,external.youthSquad)
 }

 @Test fun externalYouthDefaultsToAYouthDestinationWhenNegotiationStarts(){
  val w=WorldFactory.createWorld(51306L);w.club().budget=10_000_000_000L
  val external=w.players.values.first{it.clubId!=0&&it.clubId!=w.user.clubId&&!it.retired&&w.calendar.season-it.birthYear<=22}
  external.youth=true;external.youthSquad=if(w.calendar.season-external.birthYear<=19)YouthSquad.U19 else YouthSquad.U23
  val o=TransferEngine.createOffer(w,w.user.clubId,external.id,DealType.BUY,SquadRole.PROSPECT)
  assertNotNull(o.targetYouthSquad)
  val age=w.calendar.season-external.birthYear
  if(age>19)assertEquals(YouthSquad.U23,o.targetYouthSquad)
 }
}

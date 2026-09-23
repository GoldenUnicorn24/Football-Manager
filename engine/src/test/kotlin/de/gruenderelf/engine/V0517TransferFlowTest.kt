package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class V0517TransferFlowTest {
 private fun agreedMedicalOffer(w:World,p:Player,type:DealType):TransferOffer{
  val buyer=w.club();buyer.budget=900_000_000L;p.hidden.injuryProneness=0;p.injuryWeeks=0
  val o=TransferEngine.createOffer(w,buyer.id,p.id,type,SquadRole.STARTER)
  o.fee=TransferEngine.askingPrice(w,w.clubs[if(p.loanParentClubId!=0)p.loanParentClubId else p.clubId],p,type)*4
  o.wage=maxOf(p.wage*12,8_000);o.signingBonus=1_000_000;o.playingTimePromise=100;o.sellOnPercent=25
  TransferEngine.evaluate(w,o)
  assertEquals(TransferStage.MEDICAL,o.stage);assertEquals(NegotiationStatus.AGREED,o.status)
  return o
 }
 private fun inflateWithYouth(w:World,excluded:Set<Int>){
  val user=w.user.clubId
  val fillers=w.players.values.filter{!it.retired&&it.id !in excluded&&it.clubId!=user}.iterator()
  while(w.squad(user).count{!it.retired}<=36&&fillers.hasNext()){
   val p=fillers.next();p.clubId=user;p.youth=true;p.youthSquad=if(w.calendar.season-p.birthYear<=19)YouthSquad.U19 else YouthSquad.U23
  }
  assertTrue(w.squad(user).count{!it.retired}>32)
  assertTrue(w.squad(user).count{!it.retired&&!it.youth}<32)
 }

 @Test fun purchaseCompletesAfterMedicalWithLargeSeparateYouthSquads(){
  val w=WorldFactory.createWorld(51701L);val target=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId};val seller=target.clubId
  inflateWithYouth(w,setOf(target.id,w.user.playerId))
  val o=agreedMedicalOffer(w,target,DealType.BUY)
  TransferEngine.advanceProcess(w,o.id)
  assertEquals(TransferStage.REGISTRATION,o.stage);assertEquals(NegotiationStatus.AGREED,o.status);assertTrue(o.registrationReady,o.message)
  val saved=SaveCodec.copy(w);val savedOffer=saved.negotiations.getValue(o.id);assertEquals(TransferStage.REGISTRATION,savedOffer.stage);assertTrue(savedOffer.registrationReady)
  TransferEngine.complete(saved,o.id)
  val bought=saved.players.getValue(target.id);assertEquals(saved.user.clubId,bought.clubId);assertEquals(0,bought.loanParentClubId);assertEquals(TransferStage.COMPLETED,saved.negotiations.getValue(o.id).stage)
  assertEquals(seller,saved.transferHistory.first{it.playerId==target.id}.fromClubId)
 }

 @Test fun loanCompletesAfterMedicalWithLargeSeparateYouthSquads(){
  val w=WorldFactory.createWorld(51702L);val target=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId};val parent=target.clubId
  inflateWithYouth(w,setOf(target.id,w.user.playerId))
  val o=agreedMedicalOffer(w,target,DealType.LOAN)
  TransferEngine.advanceProcess(w,o.id);assertEquals(NegotiationStatus.AGREED,o.status);assertTrue(o.registrationReady,o.message)
  val saved=SaveCodec.copy(w);TransferEngine.complete(saved,o.id)
  val loaned=saved.players.getValue(target.id);assertEquals(saved.user.clubId,loaned.clubId);assertEquals(parent,loaned.loanParentClubId);assertTrue(loaned.loanWeeks>0);assertEquals(TransferStage.COMPLETED,saved.negotiations.getValue(o.id).stage)
 }

 @Test fun temporarilyBlockedRegistrationStaysOpenAndCanBeRetried(){
  val w=WorldFactory.createWorld(51703L);val target=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId};val user=w.user.clubId
  val fillers=w.players.values.filter{!it.retired&&it.id!=target.id&&it.clubId!=user}.iterator()
  while(w.squad(user).count{!it.retired&&!it.youth}<32&&fillers.hasNext()){val p=fillers.next();p.clubId=user;p.youth=false}
  assertEquals(32,w.squad(user).count{!it.retired&&!it.youth})
  val o=agreedMedicalOffer(w,target,DealType.BUY);TransferEngine.advanceProcess(w,o.id)
  assertEquals(TransferStage.REGISTRATION,o.stage);assertEquals(NegotiationStatus.AGREED,o.status);assertFalse(o.registrationReady);assertTrue(o.message.contains("Deal bleibt bestehen"))
  val released=w.squad(user).first{!it.youth&&it.id!=w.user.playerId};released.youth=true;released.youthSquad=YouthSquad.U23
  TransferEngine.advanceProcess(w,o.id);assertTrue(o.registrationReady,o.message);TransferEngine.complete(w,o.id);assertEquals(user,target.clubId)
 }

 @Test fun v7BlockedMedicalDealIsReopenedOnLoad(){
  val w=WorldFactory.createWorld(51704L);val target=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId}
  val o=agreedMedicalOffer(w,target,DealType.BUY);o.stage=TransferStage.REGISTRATION;o.status=NegotiationStatus.REJECTED;o.medicalPassed=true;o.medicalNote="Medizincheck bestanden · Risiko 12/100";o.registrationReady=false;o.message="Registrierung blockiert: frei verfügbares Budget oder Kadergrenze nach bereits reservierten Transfers prüfen."
  w.saveVersion=7
  val loaded=SaveCodec.decode(SaveCodec.encode(w));val repaired=loaded.negotiations.getValue(o.id)
  assertEquals(SAVE_VERSION,loaded.saveVersion);assertEquals(TransferStage.REGISTRATION,repaired.stage);assertEquals(NegotiationStatus.AGREED,repaired.status);assertFalse(repaired.registrationReady);assertTrue(repaired.message.contains("erneut geöffnet"))
 }
}

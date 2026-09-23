package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class OutboundYouthV0512Test {
 @Test fun saleRequestCreatesSeveralIndependentOffersAndNeedsUserApproval(){
  val w=WorldFactory.createWorld(51201L);val p=w.squad().first{!it.youth&&!it.retired&&it.id!=w.user.playerId};p.wantsMove=true
  val offers=OutboundTransferSystem.requestOffers(w,p.id,DealType.BUY)
  assertTrue(offers.size>=2,"Ein angebotener Spieler soll mehrere Angebote erhalten.")
  assertEquals(offers.size,offers.map{it.buyerClubId}.toSet().size)
  assertTrue(offers.all{it.sellerClubId==w.user.clubId&&it.status==NegotiationStatus.COUNTER})
  assertEquals(w.user.clubId,p.clubId,"Angebote allein dürfen den Spieler nie automatisch verkaufen.")
  val first=offers.first();val before=first.fee;OutboundTransferSystem.counter(w,first.id,"fee")
  assertTrue(first.fee>before);assertEquals(w.user.clubId,p.clubId,"Auch Nachverhandeln darf noch keinen Transfer auslösen.")
  val accepted=offers.last();OutboundTransferSystem.accept(w,accepted.id)
  assertEquals(NegotiationStatus.COMPLETED,accepted.status)
  assertEquals(accepted.buyerClubId,p.clubId)
  assertTrue(w.transferHistory.any{it.playerId==p.id&&it.toClubId==accepted.buyerClubId})
 }

 @Test fun loanWithOptionCanBeComparedNegotiatedAndAccepted(){
  val w=WorldFactory.createWorld(51202L);val p=w.squad().first{!it.youth&&!it.retired&&it.id!=w.user.playerId};p.wantsMove=true
  val offers=OutboundTransferSystem.requestOffers(w,p.id,DealType.LOAN_OPTION)
  assertTrue(offers.size>=2);assertTrue(offers.all{it.buyOption>0&&it.loanWeeksRequested in 8..52})
  val negotiated=offers.first();val beforeOption=negotiated.buyOption;OutboundTransferSystem.counter(w,negotiated.id,"option");assertTrue(negotiated.buyOption>=beforeOption)
  val chosen=offers.last();OutboundTransferSystem.accept(w,chosen.id)
  assertEquals(w.user.clubId,p.loanParentClubId);assertEquals(chosen.buyerClubId,p.clubId);assertTrue(p.loanWeeks>0);assertTrue(p.loanOptionFee>0)
 }

 @Test fun genericAiMarketNeverSellsUserPlayersWithoutApproval(){
  val w=WorldFactory.createWorld(51203L);val p=w.squad().first{!it.youth&&!it.retired&&it.id!=w.user.playerId};p.wantsMove=true
  repeat(8){w.calendar.absoluteWeek=it*2;TransferEngine.aiMarket(w,SeededRandom(512030L+it))}
  assertEquals(w.user.clubId,p.clubId,"Die allgemeine KI darf Nutzer-Spieler nicht selbstständig verkaufen oder verleihen.")
 }

 @Test fun assistantYouthAutomationCreatesMeasurableDevelopmentAndCanBeDisabledCleanly(){
  val base=WorldFactory.createWorld(51204L);val y=base.squad().first{it.youth};y.hidden.potential=maxOf(y.hidden.potential,y.ca+25);y.hidden.injuryProneness=0;y.youthProfile.learning=96;y.youthProfile.maturity=92;y.youthProfile.schoolStress=0;y.youthProfile.injuryGrowthRisk=0;y.youthProfile.growthSpurtWeeks=0;base.club().dynamics.fatigueLoad=5;base.club().stadium.medicine=100;base.club().stadium.training=90;base.club().stadium.youth=90;base.club().dynamics.staffQuality=90
  val withCoach=SaveCodec.copy(base);val withoutCoach=SaveCodec.copy(base)
  withCoach.assistantCoach.autoYouthTraining=true;withCoach.assistantCoach.profile=AssistantCoachProfile.DEVELOPER;withCoach.assistantCoach.trainingStyle=AssistantTrainingStyle.YOUTH;withCoach.assistantCoach.youthAggression=4
  withoutCoach.assistantCoach.autoYouthTraining=false
  fun score(w:World,id:Int):Double{val p=w.players.getValue(id);return p.attributes.values().values.sum()+p.trainingProgress}
  repeat(16){TrainingSystems.applyAll(withCoach);TrainingSystems.applyAll(withoutCoach);withCoach.calendar.absoluteWeek++;withoutCoach.calendar.absoluteWeek++}
  val trained=withCoach.players.getValue(y.id);val untrained=withoutCoach.players.getValue(y.id)
  assertTrue(score(withCoach,y.id)>score(withoutCoach,y.id),"Aktive Jugend-Automatik muss messbar mehr Entwicklung erzeugen.")
  assertTrue(withCoach.assistantCoach.lastYouthReason.contains("Entwicklungsbonus"))
  assertTrue(trained.youthProfile.mentorId==0||withCoach.players[trained.youthProfile.mentorId]?.clubId==withCoach.user.clubId)
  withCoach.assistantCoach.autoYouthTraining=false;AssistantCoachSystem.prepareYouth(withCoach,withCoach.club());assertFalse(trained.youthProfile.seniorTraining,"Beim Abschalten darf kein altes Proftraining hängen bleiben.")
  assertTrue(withCoach.assistantCoach.lastYouthReason.contains("Automatik aus"))
  assertEquals(y.id,untrained.id)
 }
}

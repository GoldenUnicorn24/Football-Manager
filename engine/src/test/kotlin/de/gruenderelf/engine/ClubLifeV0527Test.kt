package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class ClubLifeV0527Test {
 private fun playWeek(w: World){w.live=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);SeasonEngine.advanceWeek(w)}

 @Test fun boardReviewsHaveConsequencesAndDismissAfterTwoUltimatums(){
  val w=WorldFactory.createWorld(52701L);val c=w.club();c.budget=100000;c.lastCosts=1000
  BoardSystem.review(w,90);assertEquals(102000L,c.budget,"Volles Vertrauen zahlt zwei Wochenkosten als Prämie.")
  BoardSystem.review(w,30);assertEquals(101000L,c.budget,"Unzufriedenheit hält Mittel zurück.")
  BoardSystem.review(w,10);assertEquals(1,w.life.board.lowReviews);assertFalse(w.life.board.dismissed)
  BoardSystem.review(w,50);assertEquals(0,w.life.board.lowReviews,"Eine ordentliche Sitzung hebt das Ultimatum auf.")
  BoardSystem.review(w,10);BoardSystem.review(w,10)
  assertTrue(w.life.board.dismissed);assertTrue(w.life.board.jobOffers.isNotEmpty());assertFalse(w.user.clubId in w.life.board.jobOffers)
 }

 @Test fun acceptingAJobOfferMovesTheCareerAndStaysPlayable(){
  val w=WorldFactory.createWorld(52702L);repeat(2){playWeek(w)}
  val old=w.user.clubId;BoardSystem.dismiss(w);assertTrue(w.life.board.dismissed)
  val target=w.life.board.jobOffers.first();BoardSystem.acceptOffer(w,target)
  assertEquals(target,w.user.clubId);assertEquals(target,w.self().clubId);assertNotEquals(old,w.user.clubId)
  assertFalse(w.life.board.dismissed);assertTrue(w.user.playerId !in w.clubs.getValue(old).tactics.xi)
  assertEquals(1,w.squad().count{it.number==w.self().number},"Die Rückennummer darf im neuen Verein nicht doppelt sein.")
  SaveCodec.requireRuntimeIntegrity(w)
  repeat(4){playWeek(w)}
  assertEquals(target,w.user.clubId)
 }

 @Test fun ticketPricesTradeCrowdAgainstRevenueAndStayNeutralByDefault(){
  val w=WorldFactory.createWorld(52703L);val c=w.club()
  assertEquals(1.0,TicketSystem.attendanceFactor(c));assertEquals(1.0,TicketSystem.priceFactor(c))
  val members=c.members;TicketSystem.afterHomeMatch(c);assertEquals(members,c.members)
  c.ticketLevel=5;assertTrue(TicketSystem.attendanceFactor(c)<1.0&&TicketSystem.priceFactor(c)>1.0);TicketSystem.afterHomeMatch(c);assertTrue(c.members<members)
  c.ticketLevel=1;val low=c.members;TicketSystem.afterHomeMatch(c);assertTrue(c.members>low)
 }

 @Test fun pressConferenceIsHeldOncePerLeagueMatchAndResolves(){
  val w=WorldFactory.createWorld(52704L);var guard=0
  while(!MatchdaySystem.canHoldPress(w)&&guard++<4)playWeek(w)
  assertTrue(MatchdaySystem.canHoldPress(w))
  val before=w.squad().filter{!it.youth}.sumOf{it.morale}
  MatchdaySystem.holdPress(w,PressStance.CONFIDENT)
  assertTrue(w.squad().filter{!it.youth}.sumOf{it.morale}>=before);assertFalse(MatchdaySystem.canHoldPress(w))
  playWeek(w)
  assertEquals(PressStance.NONE,w.life.pressStance)
 }

 @Test fun aFullSeasonRunsWithBoardBreaksAndWritesTheSeasonReview(){
  val w=WorldFactory.createWorld(52705L);var guard=0
  while(w.calendar.season==2026&&guard++<80)playWeek(w)
  assertEquals(2027,w.calendar.season)
  val r=assertNotNull(w.life.seasonReview);assertEquals(2026,r.season);assertTrue(r.rank in 1..r.leagueSize);assertEquals(r.won+r.drawn+r.lost,(r.leagueSize-1)*2)
  assertTrue(w.life.lastBreakKey>0,"Mindestens eine Länderspielpause muss stattgefunden haben.")
  val restored=SaveCodec.json.decodeFromString(World.serializer(),SaveCodec.json.encodeToString(World.serializer(),w))
  assertEquals(r,restored.life.seasonReview);assertEquals(w.club().ticketLevel,restored.club().ticketLevel)
 }
}

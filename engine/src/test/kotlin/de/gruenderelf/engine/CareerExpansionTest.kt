package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class CareerExpansionTest {
 @Test fun youthTeamsPlayAndTrackIndividualStats(){
  val w=WorldFactory.createWorld(52001L);val youth=w.squad().filter{it.youth};assertTrue(youth.isNotEmpty())
  youth.forEach{YouthCompetitionSystem.normalizeSquad(w,it)};w.calendar.absoluteWeek=2
  YouthCompetitionSystem.weekly(w,SeededRandom(520010L))
  val a=w.club().academy;assertTrue(a.u19Season.played+a.u23Season.played>=1)
  assertTrue(youth.any{it.youthTeamStats.appearances>0},"Mindestens ein Jugendspieler muss einen Nachwuchseinsatz erhalten.")
 }

 @Test fun scoutingWatchlistAndReportProgressAreRealSystems(){
  val w=WorldFactory.createWorld(52002L);w.club().budget=50_000_000L
  val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId}
  ScoutingTransferSystem.toggleWatchlist(w,p.id);assertTrue(p.id in w.watchlist)
  val before=w.club().budget;ScoutingTransferSystem.startScouting(w,p.id,ScoutRegion.EUROPE);assertTrue(w.club().budget<before);assertTrue(p.id in w.scoutAssignments)
  val start=w.scoutReports.getValue(p.id).progress;ScoutingTransferSystem.weekly(w,SeededRandom(520020L));assertTrue(w.scoutReports.getValue(p.id).progress>start)
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertTrue(p.id in copy.watchlist);assertNotNull(copy.scoutReports[p.id])
 }

 @Test fun bosmanPrecontractArrivesAtSeasonChange(){
  val w=WorldFactory.createWorld(52003L);val p=w.players.values.first{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=w.user.clubId};val oldClub=p.clubId
  p.precontractClubId=w.user.clubId;p.precontractSeason=w.calendar.season+1;p.precontractWage=1234;p.precontractYears=3
  w.calendar.season++
  ScoutingTransferSystem.newSeason(w)
  assertEquals(w.user.clubId,p.clubId);assertEquals(1234,p.wage);assertEquals(3,p.contractYears);assertTrue(w.transferHistory.any{it.playerId==p.id&&it.fromClubId==oldClub&&it.toClubId==w.user.clubId&&it.note.contains("Bosman")})
 }

 @Test fun individualInstructionsChangeMatchFactors(){
  val w=WorldFactory.createWorld(52004L);val p=w.squad().first{!it.youth&&it.id!=w.user.playerId&&it.position!=Position.TW};val c=w.club()
  val baseFitness=TacticalInstructionSystem.fitnessFactor(c,p.id);TacticalInstructionSystem.set(w,p.id,PlayerInstruction.PRESS_MORE,true);TacticalInstructionSystem.set(w,p.id,PlayerInstruction.RUN_IN_BEHIND,true)
  assertTrue(TacticalInstructionSystem.fitnessFactor(c,p.id)>baseFitness);assertTrue(TacticalInstructionSystem.offsideExtra(c,p.id)>0.0);assertTrue(TacticalInstructionSystem.teamFactor(c,listOf(p.id),true)>1.0)
  assertFailsWith<IllegalArgumentException>{TacticalInstructionSystem.set(w,p.id,PlayerInstruction.SHOOT_MORE,true);TacticalInstructionSystem.set(w,p.id,PlayerInstruction.RISKY_PASSES,true)}
 }

 @Test fun matchAnalysisAggregatesPassesXgAndTacticChanges(){
  val w=WorldFactory.createWorld(52005L);val m=MatchEngine.start(w);val club=w.user.clubId;val ids=if(m.homeId==club)m.homeXi.filter{it!=0}else m.awayXi.filter{it!=0};assertTrue(ids.size>=2)
  m.passEvents.add(PassEvent(5,club,ids[0],ids[1],true));m.passEvents.add(PassEvent(6,club,ids[1],ids[0],true));m.shotEvents.add(ShotEvent(12,club,ids[0],xg=.31))
  MatchAnalysisSystem.recordTacticChange(w,m,club,"Testwechsel")
  assertEquals(2,MatchAnalysisSystem.passEdges(m,club).first().third);assertTrue(MatchAnalysisSystem.xgByIntervals(m,club).first().second>=.31);assertTrue(MatchAnalysisSystem.sinceLastChange(m,club).contains("Testwechsel"))
 }
 @Test fun liveAnalysisUsesRealPassesAndConsistentShotGeometry(){
  val w=WorldFactory.createWorld(52006L);val m=MatchEngine.start(w);var guard=0
  while(m.minute<40&&!m.finished&&guard++<360){
   when{
    m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)}
    m.pendingDecision->MatchEngine.decide(w,m,Decision.PASS)
    m.halfTime->MatchEngine.secondHalf(m)
    else->MatchEngine.step(w,m)
   }
  }
  val completed=m.passEvents.filter{it.completed&&it.fromId!=0&&it.toId!=0};assertTrue(completed.size>20,"Die Analyse muss echte Passereignisse statt nur Phasenwechsel enthalten.")
  assertTrue(completed.any{kotlin.math.abs(it.startX-it.endX)>.02f||kotlin.math.abs(it.startY-it.endY)>.02f})
  val club=w.user.clubId;assertTrue(MatchAnalysisSystem.passNetworkNodes(m,club).size>=4)
  m.shotEvents.forEach{s->val home=s.clubId==m.homeId;val geometry=ShotModel.geometry(ShotContext(s.x,s.y,home,s.type));assertEquals(geometry.distanceMeters,s.distanceMeters,0.001);assertTrue(s.distanceMeters<48.0,"Abschluss darf nicht aus einer unplausiblen Kartenposition stammen: ${s.distanceMeters}")}
 }

}

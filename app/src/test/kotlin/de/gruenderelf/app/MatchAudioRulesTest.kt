package de.gruenderelf.app

import de.gruenderelf.app.ui.*
import de.gruenderelf.engine.*
import org.junit.Assert.*
import org.junit.Test

class MatchAudioRulesTest {
 private fun snap(
  serial:Int=1,phase:LivePhase=LivePhase.POSSESSION,detail:String="",varResult:String="",half:Boolean=false,
  period:Int=1,finished:Boolean=false,stoppage:Boolean=false,homeSubs:Int=0,awaySubs:Int=0,shootout:Int=0,
  shot:ShotType=ShotType.BOX_SHOT,outcome:ShotOutcome=ShotOutcome.OFF_TARGET,minute:Int=1,homeId:Int=1,awayId:Int=2,liveClubId:Int=0
 )=MatchAudioSnapshot(serial,phase,detail,varResult,half,MatchBreakType.NONE,period,finished,stoppage,homeSubs,awaySubs,List(11){it+1},List(11){it+20},shootout,shot,outcome,minute,homeId,awayId,liveClubId)

 @Test fun kickoffHalftimeAndFinalUseLongWhistle(){
  assertEquals(listOf(MatchSoundCue.LONG_WHISTLE),MatchAudioRules.cues(null,snap(minute=0)))
  assertTrue(MatchSoundCue.LONG_WHISTLE in MatchAudioRules.cues(snap(),snap(serial=1,half=true,minute=45)))
  assertTrue(MatchSoundCue.LONG_WHISTLE in MatchAudioRules.cues(snap(),snap(serial=1,finished=true,minute=90)))
 }

 @Test fun shotGoalWoodworkAndChanceHaveLayeredSounds(){
  val before=snap(serial=4)
  val goal=MatchAudioRules.cues(before,snap(serial=5,phase=LivePhase.GOAL,outcome=ShotOutcome.GOAL))
  assertEquals(listOf(MatchSoundCue.SHOT,MatchSoundCue.GOAL),goal)
  val wood=MatchAudioRules.cues(before,snap(serial=5,phase=LivePhase.WOODWORK,outcome=ShotOutcome.WOODWORK))
  assertEquals(listOf(MatchSoundCue.SHOT,MatchSoundCue.WOODWORK,MatchSoundCue.OOH),wood)
  val saved=MatchAudioRules.cues(before,snap(serial=5,phase=LivePhase.SHOT_ON_TARGET,outcome=ShotOutcome.SAVED))
  assertEquals(listOf(MatchSoundCue.SHOT,MatchSoundCue.SAVE,MatchSoundCue.OOH),saved)
 }

 @Test fun varCheckConfirmAndDisallowAreDifferent(){
  val before=snap(serial=9)
  assertEquals(listOf(MatchSoundCue.VAR_CHECK),MatchAudioRules.cues(before,snap(serial=10,phase=LivePhase.VAR)))
  assertEquals(listOf(MatchSoundCue.VAR_CONFIRM),MatchAudioRules.cues(before,snap(serial=10,phase=LivePhase.VAR,varResult="Tor zählt – kein Abseits")))
  assertEquals(listOf(MatchSoundCue.VAR_DISALLOWED),MatchAudioRules.cues(before,snap(serial=10,phase=LivePhase.VAR,varResult="Kein Tor – Abseits")))
 }

 @Test fun cardsStandardsPenaltiesAndSubstitutionsGetSpecificCues(){
  val before=snap(serial=20)
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE,MatchSoundCue.YELLOW),MatchAudioRules.cues(before,snap(serial=21,phase=LivePhase.YELLOW_CARD)))
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE,MatchSoundCue.RED),MatchAudioRules.cues(before,snap(serial=21,phase=LivePhase.RED_CARD)))
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE,MatchSoundCue.PENALTY_TENSION),MatchAudioRules.cues(before,snap(serial=21,phase=LivePhase.PENALTY)))
  assertEquals(listOf(MatchSoundCue.SUBSTITUTION),MatchAudioRules.cues(before,snap(serial=20,homeSubs=1)))
 }

 @Test fun ordinaryFreeKickGetsWhistleAndSetPieceTension(){
  val before=snap(serial=40)
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE,MatchSoundCue.SETPIECE_TENSION),MatchAudioRules.cues(before,snap(serial=41,phase=LivePhase.FREE_KICK)))
 }

 @Test fun throwInIsSilentWhileOffsideStillUsesWhistle(){
  val before=snap(serial=50)
  assertTrue(MatchAudioRules.cues(before,snap(serial=51,phase=LivePhase.THROW_IN)).isEmpty())
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE),MatchAudioRules.cues(before,snap(serial=51,phase=LivePhase.OFFSIDE)))
  assertEquals(listOf(MatchSoundCue.PASS),MatchAudioRules.cues(before,snap(serial=51,phase=LivePhase.POSSESSION)))
  assertTrue(MatchSoundCue.STOPPAGE in MatchAudioRules.cues(before,snap(serial=51,stoppage=true)))
 }

 @Test fun crowdPerspectiveTracksEventTeamAndSubstitutions(){
  val before=snap(serial=60,homeSubs=0,awaySubs=0)
  assertEquals(MatchCrowdSide.HOME,MatchAudioRules.crowdSide(before,snap(serial=61,phase=LivePhase.GOAL,liveClubId=1)))
  assertEquals(MatchCrowdSide.AWAY,MatchAudioRules.crowdSide(before,snap(serial=61,phase=LivePhase.GOAL,liveClubId=2)))
  assertEquals(MatchCrowdSide.HOME,MatchAudioRules.crowdSide(before,snap(serial=60,homeSubs=1,liveClubId=0)))
  assertEquals(MatchCrowdSide.AWAY,MatchAudioRules.crowdSide(before,snap(serial=60,awaySubs=1,liveClubId=0)))
  assertEquals(MatchCrowdSide.NEUTRAL,MatchAudioRules.crowdSide(before,snap(serial=61,liveClubId=0)))
 }

 @Test fun liveEventTeamWinsOverSimultaneousSubstitutionForCrowdPerspective(){
  val before=snap(serial=70,homeSubs=0,awaySubs=0)
  assertEquals(MatchCrowdSide.AWAY,MatchAudioRules.crowdSide(before,snap(serial=71,phase=LivePhase.GOAL,homeSubs=1,liveClubId=2,outcome=ShotOutcome.GOAL)))
  assertEquals(MatchCrowdSide.HOME,MatchAudioRules.crowdSide(before,snap(serial=71,phase=LivePhase.GOAL,awaySubs=1,liveClubId=1,outcome=ShotOutcome.GOAL)))
 }

 @Test fun longAndShortWhistleRemainContextuallySeparated(){
  assertEquals(listOf(MatchSoundCue.LONG_WHISTLE),MatchAudioRules.cues(null,snap(minute=0)))
  assertEquals(listOf(MatchSoundCue.SHORT_WHISTLE),MatchAudioRules.cues(snap(serial=80),snap(serial=81,phase=LivePhase.OFFSIDE)))
 }

 @Test fun shootoutResultUsesPenaltyCelebrationOrReaction(){
  val before=snap(serial=30,shootout=2,shot=ShotType.PENALTY)
  assertEquals(listOf(MatchSoundCue.PENALTY_GOAL),MatchAudioRules.cues(before,snap(serial=31,phase=LivePhase.SHOOTOUT,shootout=3,shot=ShotType.PENALTY,outcome=ShotOutcome.GOAL)))
  assertEquals(listOf(MatchSoundCue.PENALTY_SAVED),MatchAudioRules.cues(before,snap(serial=31,phase=LivePhase.SHOOTOUT,shootout=3,shot=ShotType.PENALTY,outcome=ShotOutcome.SAVED)))
 }
 @Test fun homeGoalNeverUsesAwayCrowdPerspective(){
  val before=snap(serial=90,homeSubs=0,awaySubs=0)
  for(homeSubs in 0..1)for(awaySubs in 0..1){
   val goal=snap(serial=91,phase=LivePhase.GOAL,homeSubs=homeSubs,awaySubs=awaySubs,homeId=10,awayId=20,liveClubId=10,outcome=ShotOutcome.GOAL)
   assertEquals(MatchCrowdSide.HOME,MatchAudioRules.crowdSide(before,goal))
   assertTrue(MatchSoundCue.GOAL in MatchAudioRules.cues(before,goal))
  }
 }

 @Test fun awayGoalNeverUsesHomeCrowdPerspective(){
  val before=snap(serial=100,homeSubs=0,awaySubs=0,homeId=10,awayId=20)
  val goal=snap(serial=101,phase=LivePhase.GOAL,homeId=10,awayId=20,liveClubId=20,outcome=ShotOutcome.GOAL)
  assertEquals(MatchCrowdSide.AWAY,MatchAudioRules.crowdSide(before,goal))
  assertTrue(MatchSoundCue.GOAL in MatchAudioRules.cues(before,goal))
 }

}

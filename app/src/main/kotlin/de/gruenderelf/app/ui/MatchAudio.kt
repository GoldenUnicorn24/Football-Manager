package de.gruenderelf.app.ui

import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import de.gruenderelf.engine.*
import java.util.concurrent.CopyOnWriteArrayList

enum class MatchSoundCue {
 LONG_WHISTLE, SHORT_WHISTLE, PASS, SHOT, SAVE, WOODWORK, GOAL, OOH,
 PENALTY_TENSION, PENALTY_GOAL, PENALTY_SAVED, YELLOW, RED,
 SETPIECE_TENSION, SUBSTITUTION, STOPPAGE, VAR_CHECK, VAR_CONFIRM, VAR_DISALLOWED
}

enum class MatchCrowdSide { HOME, AWAY, NEUTRAL }

data class MatchAudioSnapshot(
 val eventSerial:Int,
 val phase:LivePhase,
 val detail:String,
 val varResult:String,
 val halfTime:Boolean,
 val breakType:MatchBreakType,
 val period:Int,
 val finished:Boolean,
 val stoppageAnnounced:Boolean,
 val homeSubs:Int,
 val awaySubs:Int,
 val homeXi:List<Int>,
 val awayXi:List<Int>,
 val shootoutCount:Int,
 val lastShotType:ShotType,
 val lastShotOutcome:ShotOutcome,
 val minute:Int,
 val homeId:Int=1,
 val awayId:Int=2,
 val liveClubId:Int=0
)

private fun LiveMatch.audioSnapshot()=MatchAudioSnapshot(
 liveEventSerial,livePhase,liveDetail,varReviewResult,halfTime,breakType,period,finished,stoppageAnnounced,
 homeSubs,awaySubs,homeXi.toList(),awayXi.toList(),penaltyShootout.size,lastShotType,lastShotOutcome,minute,
 homeId,awayId,liveClubId
)

object MatchAudioRules {
 fun cues(previous: MatchAudioSnapshot?,current: MatchAudioSnapshot): List<MatchSoundCue> {
  val out=mutableListOf<MatchSoundCue>()
  if(previous==null){if(current.minute==0&&current.eventSerial<=1)out+=MatchSoundCue.LONG_WHISTLE;return out}

  if(!previous.stoppageAnnounced&&current.stoppageAnnounced)out+=MatchSoundCue.STOPPAGE
  if(!previous.halfTime&&current.halfTime)out+=MatchSoundCue.LONG_WHISTLE
  if(previous.halfTime&&!current.halfTime&&current.period!=previous.period)out+=MatchSoundCue.LONG_WHISTLE
  if(!previous.finished&&current.finished)out+=MatchSoundCue.LONG_WHISTLE
  if(current.homeSubs>previous.homeSubs||current.awaySubs>previous.awaySubs)out+=MatchSoundCue.SUBSTITUTION

  if(current.shootoutCount>previous.shootoutCount){
   out+=when(current.lastShotOutcome){ShotOutcome.GOAL->MatchSoundCue.PENALTY_GOAL;ShotOutcome.SAVED,ShotOutcome.CORNER,ShotOutcome.DEFLECTED,ShotOutcome.REBOUND->MatchSoundCue.PENALTY_SAVED;else->MatchSoundCue.OOH}
   return out.distinct()
  }

  if(current.eventSerial==previous.eventSerial)return out.distinct()
  when(current.phase){
   LivePhase.VAR->when{
    current.varResult.startsWith("Tor zählt",ignoreCase=true)||current.varResult.contains("Elfmeter gegeben",ignoreCase=true)->out+=MatchSoundCue.VAR_CONFIRM
    current.varResult.startsWith("Kein Tor",ignoreCase=true)||current.varResult.contains("aberkannt",ignoreCase=true)->out+=MatchSoundCue.VAR_DISALLOWED
    else->out+=MatchSoundCue.VAR_CHECK
   }
   LivePhase.YELLOW_CARD->{out+=MatchSoundCue.SHORT_WHISTLE;out+=MatchSoundCue.YELLOW}
   LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD->{out+=MatchSoundCue.SHORT_WHISTLE;out+=MatchSoundCue.RED}
   LivePhase.THROW_IN->{ }
   LivePhase.OFFSIDE->out+=MatchSoundCue.SHORT_WHISTLE
   LivePhase.CORNER->out+=MatchSoundCue.SETPIECE_TENSION
   LivePhase.FREE_KICK->{out+=MatchSoundCue.SHORT_WHISTLE;out+=MatchSoundCue.SETPIECE_TENSION}
   LivePhase.DANGEROUS_FREE_KICK->{out+=MatchSoundCue.SHORT_WHISTLE;out+=MatchSoundCue.SETPIECE_TENSION}
   LivePhase.PENALTY->{out+=MatchSoundCue.SHORT_WHISTLE;out+=MatchSoundCue.PENALTY_TENSION}
   LivePhase.GOAL->if(current.lastShotType==ShotType.PENALTY)out+=MatchSoundCue.PENALTY_GOAL else {out+=MatchSoundCue.SHOT;out+=MatchSoundCue.GOAL}
   LivePhase.WOODWORK->{out+=MatchSoundCue.SHOT;out+=MatchSoundCue.WOODWORK;out+=MatchSoundCue.OOH}
   LivePhase.SHOT_ON_TARGET->if(current.lastShotType==ShotType.PENALTY)out+=MatchSoundCue.PENALTY_SAVED else {out+=MatchSoundCue.SHOT;out+=MatchSoundCue.SAVE;out+=MatchSoundCue.OOH}
   LivePhase.SHOT_OFF_TARGET->{out+=MatchSoundCue.SHOT;out+=MatchSoundCue.OOH}
   LivePhase.POSSESSION,LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK,LivePhase.COUNTER->out+=MatchSoundCue.PASS
   else->{ }
  }
  return out.distinct()
 }

 fun crowdSide(previous:MatchAudioSnapshot?,current:MatchAudioSnapshot):MatchCrowdSide {
  if(previous==null || current.eventSerial!=previous.eventSerial){
   when(current.liveClubId){current.homeId->return MatchCrowdSide.HOME;current.awayId->return MatchCrowdSide.AWAY}
  }
  if(previous!=null){
   val homeChanged=current.homeSubs>previous.homeSubs
   val awayChanged=current.awaySubs>previous.awaySubs
   if(homeChanged&&!awayChanged)return MatchCrowdSide.HOME
   if(awayChanged&&!homeChanged)return MatchCrowdSide.AWAY
  }
  return when(current.liveClubId){
   current.homeId->MatchCrowdSide.HOME
   current.awayId->MatchCrowdSide.AWAY
   else->MatchCrowdSide.NEUTRAL
  }
 }
}

@Composable fun MatchAudioEffects(@Suppress("UNUSED_PARAMETER") w: World,m: LiveMatch,enabled: Boolean){
 val context=androidx.compose.ui.platform.LocalContext.current
 val lifecycleOwner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 val engine=remember(context){MatchAudioEngine(context.applicationContext)}
 var previous by remember(m.fixtureId){mutableStateOf<MatchAudioSnapshot?>(null)}
 DisposableEffect(engine,lifecycleOwner){
  val observer=androidx.lifecycle.LifecycleEventObserver{_,event->
   when(event){
    androidx.lifecycle.Lifecycle.Event.ON_RESUME->engine.setForeground(true)
    androidx.lifecycle.Lifecycle.Event.ON_PAUSE,androidx.lifecycle.Lifecycle.Event.ON_STOP->engine.setForeground(false)
    androidx.lifecycle.Lifecycle.Event.ON_DESTROY->engine.release()
    else->Unit
   }
  }
  lifecycleOwner.lifecycle.addObserver(observer)
  engine.setForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
  onDispose{lifecycleOwner.lifecycle.removeObserver(observer);engine.release()}
 }
 LaunchedEffect(enabled){engine.setEnabled(enabled)}
 LaunchedEffect(m.fixtureId,m.liveEventSerial,m.halfTime,m.period,m.finished,m.stoppageAnnounced,m.homeSubs,m.awaySubs,m.penaltyShootout.size,enabled){
  val current=m.audioSnapshot();val old=previous
  engine.play(MatchAudioRules.cues(old,current),MatchAudioRules.crowdSide(old,current))
  previous=current
 }
}

private class MatchAudioEngine(private val context:android.content.Context) {
 private val handler=Handler(Looper.getMainLooper())
 private val activeStreams=CopyOnWriteArrayList<Int>()
 private val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
 private val soundPool=android.media.SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build()
 private val resourceIds=mapOf(
  "kick" to de.gruenderelf.app.R.raw.match_kick,
  "save" to de.gruenderelf.app.R.raw.keeper_save,
  "clang" to de.gruenderelf.app.R.raw.woodwork_clang,
  "homeGoal" to de.gruenderelf.app.R.raw.crowd_home_goal,
  "awayGoal" to de.gruenderelf.app.R.raw.crowd_away_goal,
  "homeChance" to de.gruenderelf.app.R.raw.crowd_home_chance,
  "awayChance" to de.gruenderelf.app.R.raw.crowd_away_chance,
  "approval" to de.gruenderelf.app.R.raw.crowd_home_approval,
  "protest" to de.gruenderelf.app.R.raw.crowd_home_protest,
  "homeTension" to de.gruenderelf.app.R.raw.crowd_home_tension,
  "awayTension" to de.gruenderelf.app.R.raw.crowd_away_tension,
  "neutralTension" to de.gruenderelf.app.R.raw.crowd_neutral_tension,
  "homeApplause" to de.gruenderelf.app.R.raw.crowd_home_applause,
  "awayApplause" to de.gruenderelf.app.R.raw.crowd_away_applause,
  "whistleShort" to de.gruenderelf.app.R.raw.referee_whistle_short,
  "whistleLong" to de.gruenderelf.app.R.raw.referee_whistle_long
 )
 private val sampleIds=resourceIds.mapValues{(_,res)->soundPool.load(context,res,1)}
 @Volatile private var enabled=false
 @Volatile private var foreground=false
 @Volatile private var released=false
 private var ambiencePlayer:android.media.MediaPlayer?=null
 private val normalAmbience=.115f

 fun setEnabled(value:Boolean){
  if(released)return
  enabled=value
  if(value&&foreground)startAmbience() else if(!value)stopAll()
 }
 fun setForeground(value:Boolean){
  if(released)return
  foreground=value
  if(!value)stopAll() else if(enabled)startAmbience()
 }
 fun release(){
  if(released)return
  enabled=false;foreground=false;stopAll();released=true;runCatching{soundPool.release()}
 }
 private fun audible()=enabled&&foreground&&!released
 private fun restoreAmbience(){ambiencePlayer?.setVolume(normalAmbience,normalAmbience)}
 private fun stopTransient(){
  handler.removeCallbacksAndMessages(null)
  activeStreams.forEach{runCatching{soundPool.stop(it)}};activeStreams.clear()
  restoreAmbience()
 }
 private fun stopAll(){
  stopTransient()
  ambiencePlayer?.let{player->runCatching{player.stop()};runCatching{player.release()}}
  ambiencePlayer=null
 }
 private fun startAmbience(){
  if(!audible()||ambiencePlayer!=null)return
  val player=android.media.MediaPlayer.create(context,de.gruenderelf.app.R.raw.stadium_ambience)?:return
  player.isLooping=true;player.setVolume(normalAmbience,normalAmbience);ambiencePlayer=player;player.start()
 }
 private fun duckAmbience(level:Float,durationMs:Long){
  val player=ambiencePlayer?:return
  player.setVolume(level,level)
  handler.postDelayed({if(audible())restoreAmbience()},durationMs)
 }
 private fun playWhistle(cue:MatchSoundCue){
  if(!audible())return
  if(cue==MatchSoundCue.LONG_WHISTLE)playSample("whistleLong",.34f,.34f) else playSample("whistleShort",.31f,.31f)
 }
 private fun playSample(key:String,left:Float,right:Float=left,rate:Float=1f){
  if(!audible())return
  val id=sampleIds[key]?:return
  val stream=soundPool.play(id,left.coerceIn(0f,1f),right.coerceIn(0f,1f),1,0,rate.coerceIn(.5f,2f))
  if(stream!=0){activeStreams+=stream;handler.postDelayed({activeStreams.remove(stream)},6500L)}
 }
 private fun later(delay:Long,block:()->Unit){handler.postDelayed({if(audible())block()},delay)}
 private fun homeGoal(volume:Float=1f){playSample("homeGoal",volume,volume*.97f)}
 private fun awayGoal(volume:Float=.58f){playSample("awayGoal",volume*.72f,volume)}
 private fun chance(side:MatchCrowdSide,volume:Float=.65f){
  when(side){
   MatchCrowdSide.HOME->playSample("homeChance",volume,volume*.98f)
   MatchCrowdSide.AWAY->playSample("awayChance",volume*.92f,volume)
   MatchCrowdSide.NEUTRAL->playSample("homeChance",volume*.48f,volume*.48f)
  }
 }
 private fun tension(side:MatchCrowdSide,volume:Float=.38f){
  when(side){
   MatchCrowdSide.HOME->playSample("homeTension",volume,volume*.98f)
   MatchCrowdSide.AWAY->playSample("awayTension",volume*.90f,volume)
   MatchCrowdSide.NEUTRAL->playSample("neutralTension",volume*.70f,volume*.70f)
  }
 }
 private fun goalReaction(side:MatchCrowdSide,volume:Float=.90f){
  when(side){
   MatchCrowdSide.HOME->homeGoal(volume)
   MatchCrowdSide.AWAY->{awayGoal(volume*.62f);later(120){playSample("protest",.25f,.25f)}}
   MatchCrowdSide.NEUTRAL->homeGoal(volume*.55f)
  }
 }
 private fun confirmationReaction(side:MatchCrowdSide){
  when(side){
   MatchCrowdSide.HOME->{playSample("approval",.46f,.45f);later(100){playSample("homeApplause",.34f,.33f)}}
   MatchCrowdSide.AWAY->{playSample("awayApplause",.25f,.34f);later(120){playSample("protest",.18f,.18f)}}
   MatchCrowdSide.NEUTRAL->playSample("neutralTension",.20f,.20f)
  }
 }
 private fun cardReaction(side:MatchCrowdSide,severe:Boolean){
  when(side){
   MatchCrowdSide.HOME->playSample("protest",if(severe).66f else .40f,if(severe).64f else .39f)
   MatchCrowdSide.AWAY->playSample("approval",if(severe).62f else .38f,if(severe).60f else .37f)
   MatchCrowdSide.NEUTRAL->playSample("protest",if(severe).30f else .20f,if(severe).30f else .20f)
  }
 }

 fun play(cues:List<MatchSoundCue>,side:MatchCrowdSide){
  if(!audible()||cues.isEmpty())return
  if(cues.any{it==MatchSoundCue.VAR_CHECK||it==MatchSoundCue.VAR_DISALLOWED||it==MatchSoundCue.VAR_CONFIRM||it==MatchSoundCue.PENALTY_TENSION})stopTransient()
  var delay=0L
  cues.forEach{cue->later(delay){playCue(cue,side)};delay+=when(cue){MatchSoundCue.SHOT->115L;MatchSoundCue.SAVE->130L;MatchSoundCue.SHORT_WHISTLE->170L;MatchSoundCue.WOODWORK->135L;else->120L}}
 }
 private fun playCue(cue:MatchSoundCue,side:MatchCrowdSide){
  when(cue){
   MatchSoundCue.LONG_WHISTLE,MatchSoundCue.SHORT_WHISTLE->playWhistle(cue)
   MatchSoundCue.PASS->playSample("kick",.19f,.19f,.82f)
   MatchSoundCue.SHOT->playSample("kick",.59f,.59f,1.04f)
   MatchSoundCue.SAVE->{
    playSample("save",.56f,.56f,.94f)
    when(side){MatchCrowdSide.AWAY->later(85){playSample("homeApplause",.34f,.33f)};MatchCrowdSide.HOME->later(85){playSample("awayApplause",.12f,.18f)};MatchCrowdSide.NEUTRAL->Unit}
   }
   MatchSoundCue.WOODWORK->playSample("clang",.75f,.75f,1.03f)
   MatchSoundCue.GOAL->goalReaction(side,.96f)
   MatchSoundCue.OOH->chance(side,.68f)
   MatchSoundCue.PENALTY_TENSION->{duckAmbience(.035f,3200);tension(side,.44f)}
   MatchSoundCue.PENALTY_GOAL->{playSample("kick",.67f,.67f,1.02f);later(130){goalReaction(side,.98f)}}
   MatchSoundCue.PENALTY_SAVED->{playSample("kick",.65f,.65f,1.02f);later(95){playSample("save",.67f,.67f)};later(185){if(side==MatchCrowdSide.AWAY)playSample("approval",.69f,.67f) else chance(side,.76f)}}
   MatchSoundCue.YELLOW->cardReaction(side,false)
   MatchSoundCue.RED->cardReaction(side,true)
   MatchSoundCue.SETPIECE_TENSION->tension(side,.39f)
   MatchSoundCue.SUBSTITUTION->when(side){MatchCrowdSide.HOME->playSample("homeApplause",.48f,.47f);MatchCrowdSide.AWAY->playSample("awayApplause",.20f,.29f);MatchCrowdSide.NEUTRAL->playSample("homeApplause",.24f,.24f)}
   MatchSoundCue.STOPPAGE->tension(side,.22f)
   MatchSoundCue.VAR_CHECK->{duckAmbience(.060f,2300);tension(side,.24f)}
   MatchSoundCue.VAR_CONFIRM->confirmationReaction(side)
   MatchSoundCue.VAR_DISALLOWED->when(side){MatchCrowdSide.HOME->playSample("protest",.62f,.60f);MatchCrowdSide.AWAY->playSample("approval",.64f,.62f);MatchCrowdSide.NEUTRAL->chance(side,.50f)}
  }
 }
}

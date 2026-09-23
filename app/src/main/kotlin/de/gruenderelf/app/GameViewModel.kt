package de.gruenderelf.app
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.gruenderelf.app.data.*
import de.gruenderelf.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GameState(val world: World?=null,val slot: Int=1,val busy: Boolean=false,val error: String?=null,val message: String?=null,val revision: Long=0)
class GameViewModel(application: Application): AndroidViewModel(application){
 private val repo=GameRepository(application);private val lock=Mutex();private val mutable=MutableStateFlow(GameState())
 val state=mutable.asStateFlow()
 val slots=repo.saves.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val lastSlot=repo.lastSlot.stateIn(viewModelScope,SharingStarted.Eagerly,1)
 val matchSpeed=repo.matchSpeed.stateIn(viewModelScope,SharingStarted.Eagerly,MatchSpeed.NORMAL)
 val soundsEnabled=repo.soundsEnabled.stateIn(viewModelScope,SharingStarted.Eagerly,true)
 private var liveRunnerJob: Job?=null
 @Volatile private var liveRunnerEnabled=false
 private fun runnerDelayMs(phase:LivePhase,speed:MatchSpeed):Long {
  val normal=when(phase){
   LivePhase.GOAL->4200L
   LivePhase.SHOT_ON_TARGET,LivePhase.SHOT_OFF_TARGET,LivePhase.WOODWORK->2800L
   LivePhase.VAR->1600L
   LivePhase.OFFSIDE,LivePhase.THROW_IN->1800L
   LivePhase.CORNER->3000L
   LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY->2900L
   LivePhase.YELLOW_CARD,LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD,LivePhase.INJURY->2500L
   LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK->2000L
   LivePhase.COUNTER->1450L
   else->2250L
  }
  return when(speed){
   MatchSpeed.SLOW->if(phase==LivePhase.VAR)1900L else (normal*1.45).toLong()
   MatchSpeed.NORMAL->normal
   MatchSpeed.FAST->when(phase){LivePhase.GOAL->2300L;LivePhase.SHOT_ON_TARGET,LivePhase.SHOT_OFF_TARGET,LivePhase.WOODWORK->1550L;LivePhase.VAR->1200L;LivePhase.OFFSIDE,LivePhase.THROW_IN->950L;LivePhase.CORNER->1700L;LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY->1650L;LivePhase.YELLOW_CARD,LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD,LivePhase.INJURY->1450L;LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK->1100L;LivePhase.COUNTER->720L;else->850L}
  }
 }
 private fun liveBlocked(m:LiveMatch)=m.finished||m.halfTime||m.pendingDecision||m.incidentPause||m.assistantSubPending
 fun startLiveRunner(){
  liveRunnerEnabled=true
  if(liveRunnerJob?.isActive==true)return
  liveRunnerJob=viewModelScope.launch{
   try{
    while(isActive&&liveRunnerEnabled){
     val snapshot=mutable.value.world?.live?:break
     if(snapshot.finished)break
     if(liveBlocked(snapshot)){delay(120);continue}
     val fixtureId=snapshot.fixtureId;val serial=snapshot.liveEventSerial
     delay(runnerDelayMs(snapshot.livePhase,matchSpeed.value))
     if(!liveRunnerEnabled)break
     lock.withLock{
      val current=mutable.value.world?:return@withLock
      val live=current.live?:return@withLock
      if(!liveRunnerEnabled||live.fixtureId!=fixtureId||live.liveEventSerial!=serial||liveBlocked(live))return@withLock
      try{
       val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also{w->w.live?.let{MatchEngine.step(w,it)}}}
       val after=next.live
       val checkpoint=after!=null&&(after.minute/5!=(current.live?.minute?:0)/5||after.pendingDecision||after.halfTime||after.finished||after.incidentPause||after.assistantSubPending)
       mutable.update{it.copy(world=next,revision=it.revision+1,error=null)}
       if(checkpoint){
        try{repo.save(mutable.value.slot,next)}catch(e:CancellationException){throw e}catch(e:Exception){
         liveRunnerEnabled=false
         mutable.update{it.copy(error="Der Live-Checkpoint konnte nicht gespeichert werden.")}
        }
       }
      }catch(e:CancellationException){throw e}catch(e:Exception){
       liveRunnerEnabled=false
       mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Die Live-Simulation konnte nicht fortgesetzt werden.")}
      }
     }
    }
   }finally{liveRunnerJob=null}
  }
 }
 fun stopLiveRunner(){liveRunnerEnabled=false;liveRunnerJob?.cancel();liveRunnerJob=null}
 private fun work(task: suspend ()->Unit){viewModelScope.launch{lock.withLock{
  mutable.update{it.copy(busy=true,error=null,message=null)}
  try{task()}catch(e: CancellationException){throw e}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Aktion konnte nicht bestätigt werden. Bitte erneut versuchen oder den gespeicherten Stand laden.")}}
  finally{mutable.update{it.copy(busy=false)}}
 }}}
 fun create(seed: Long,c: ClubDraft,p: PlayerDraft,slot: Int,sandboxPlayers: List<PlayerDraft> = emptyList())=work{val w=withContext(Dispatchers.Default){WorldFactory.createWorld(seed,c,p,sandboxPlayers)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createRealMode(seed: Long,clubKey: String,p: PlayerDraft,slot: Int,fantasyCupEnabled:Boolean=true)=work{val w=withContext(Dispatchers.Default){WorldFactory.createRealModeWorld(seed,clubKey,p,fantasyCupEnabled)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createCustomReal(seed:Long,slotClubKey:String,c:ClubDraft,p:PlayerDraft,players:List<PlayerDraft>,slot:Int,fantasyCupEnabled:Boolean=true)=work{val w=withContext(Dispatchers.Default){WorldFactory.createCustomClubWorld(seed,slotClubKey,c,p,players,fantasyCupEnabled)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createTopClub(seed: Long,clubKey: String,p: PlayerDraft,slot: Int)=createRealMode(seed,clubKey,p,slot)
 fun load(slot: Int)=run{stopLiveRunner();work{val w=repo.load(slot);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}}
 fun delete(slot: Int)=work{repo.delete(slot)}
 fun saveAs(slot: Int)=work{val w=mutable.value.world?:return@work;repo.save(slot,w);mutable.update{it.copy(slot=slot,message="In Slot $slot gespeichert.")}}
 fun backToMenu()=run{stopLiveRunner();work{mutable.value.world?.let{repo.save(mutable.value.slot,it)};mutable.value=GameState()}}
 fun clearError(){mutable.update{it.copy(error=null)}}
 fun clearMessage(){mutable.update{it.copy(message=null)}}
 fun setMatchSpeed(v: MatchSpeed){viewModelScope.launch{repo.setMatchSpeed(v)}}
 fun setSoundsEnabled(enabled: Boolean){viewModelScope.launch{repo.setSoundsEnabled(enabled)}}
 fun action(message: String?=null,block: (World)->Unit)=work{
  val current=mutable.value.world?:return@work;val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also(block)}
  repo.save(mutable.value.slot,next);mutable.update{it.copy(world=next,revision=it.revision+1,message=message)}
 }
 fun startMatch()=action{require(it.live==null){"Das Spiel läuft bereits."};it.live=MatchEngine.start(it)}
 fun step(count: Int)=work{
  val current=mutable.value.world?:return@work
  val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also{w->val m=w.live?:return@also;repeat(count){MatchEngine.step(w,m)}}}
  val m=next.live;if(m!=null&&(m.minute/5!=(current.live?.minute?:0)/5||m.pendingDecision||m.halfTime||m.finished))repo.save(mutable.value.slot,next)
  mutable.update{it.copy(world=next,revision=it.revision+1)}
 }
 // Der automatische Livetakt nutzt absichtlich nicht den globalen busy-Status.
 // Dadurch bleiben Taktik-, Pause- und Wechselknöpfe zwischen den einzelnen Minuten bedienbar.
 fun liveStep(){viewModelScope.launch{lock.withLock{
  try{
   val current=mutable.value.world?:return@withLock
   val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also{w->w.live?.let{MatchEngine.step(w,it)}}}
   val m=next.live;if(m!=null&&(m.minute/5!=(current.live?.minute?:0)/5||m.pendingDecision||m.halfTime||m.finished||m.incidentPause||m.assistantSubPending))repo.save(mutable.value.slot,next)
   mutable.update{it.copy(world=next,revision=it.revision+1,error=null)}
  }catch(e: CancellationException){throw e}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Die Live-Simulation konnte nicht fortgesetzt werden.")}}
 }}}
 fun liveAction(block: (World)->Unit){viewModelScope.launch{lock.withLock{
  try{
   val current=mutable.value.world?:return@withLock
   val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also(block)}
   repo.save(mutable.value.slot,next);mutable.update{it.copy(world=next,revision=it.revision+1,error=null)}
  }catch(e: CancellationException){throw e}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Die Live-Aktion konnte nicht bestätigt werden.")}}
 }}}
 fun simulateToHalf()=work{
  val current=mutable.value.world?:return@work
  val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also{w->val m=w.live?:error("Keine laufende Partie.");MatchEngine.fastForward(w,m,true)}}
  repo.save(mutable.value.slot,next);mutable.update{it.copy(world=next,revision=it.revision+1,message="Bis zur Halbzeit simuliert.")}
 }
 fun simulateMatch()=work{
  val current=mutable.value.world?:return@work
  val next=withContext(Dispatchers.Default){SaveCodec.copy(current).also{w->val m=w.live?:error("Keine laufende Partie.");MatchEngine.fastForward(w,m,false)}}
  repo.save(mutable.value.slot,next);mutable.update{it.copy(world=next,revision=it.revision+1,message="Spiel vollständig simuliert.")}
 }
 fun decide(d: Decision)=liveAction{w->w.live?.let{MatchEngine.decide(w,it,d)}}
 fun changeFormation(formation: String)=liveAction{w->w.live?.let{MatchEngine.changeFormation(w,it,w.user.clubId,formation)}}
 fun setConserveEnergy(enabled: Boolean)=liveAction{w->w.live?.let{MatchEngine.setConserveEnergy(w,it,w.user.clubId,enabled)}}
 fun setAllOutAttack(enabled: Boolean)=liveAction{w->w.live?.let{MatchEngine.setAllOutAttack(w,it,w.user.clubId,enabled)}}
 fun setControlGame(enabled: Boolean)=liveAction{w->w.live?.let{MatchEngine.setControlGame(w,it,w.user.clubId,enabled)}}
 fun updateLiveTactic(block: (World)->Unit)=liveAction(block)
 fun substitute(out: Int,incoming: Int)=liveAction{w->w.live?.let{MatchEngine.substitute(w,it,out,incoming,w.user.clubId)}}
 fun acceptAssistantSubstitution()=liveAction{w->w.live?.let{MatchEngine.acceptAssistantSubstitution(w,it)}}
 fun rejectAssistantSubstitution()=liveAction{w->w.live?.let{MatchEngine.rejectAssistantSubstitution(w,it)}}
 fun resumeIncident()=liveAction{w->w.live?.let{MatchEngine.resumeIncident(w,it)}}
 fun secondHalf()=liveAction{it.live?.let{m->MatchEngine.secondHalf(m)}}
 fun finishWeek()=action("Partie abgeschlossen und gespeichert."){SeasonEngine.advanceWeek(it)}
 fun advanceIdleWeek()=action("Vereinswoche abgeschlossen und gespeichert."){SeasonEngine.advanceIdleWeek(it)}
 fun advanceUntilNextMatch()=action("Bis zur nächsten eigenen Partie vorgespult."){SeasonEngine.advanceUntilNextMatch(it)}
 fun exportTo(uri: Uri)=work{
  val w=mutable.value.world?:return@work
  withContext(Dispatchers.IO){val stream=getApplication<Application>().contentResolver.openOutputStream(uri)?:error("Datei nicht erreichbar.");stream.use{it.write(SaveCodec.encode(w).toByteArray(Charsets.UTF_8));it.flush()}}
  mutable.update{it.copy(message="Spielstand als Datei exportiert.")}
 }
 fun importFrom(uri: Uri,slot: Int)=work{
  val w=withContext(Dispatchers.IO){val stream=getApplication<Application>().contentResolver.openInputStream(uri)?:error("Datei nicht erreichbar.");val text=stream.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=128*1024*1024){"Die Datei ist zu groß."};out.write(buffer,0,n)};out.toString("UTF-8")};SaveCodec.decode(text)}
  repo.save(slot,w);mutable.value=GameState(w,slot,message="Spielstand importiert.",revision=mutable.value.revision+1)
 }
}

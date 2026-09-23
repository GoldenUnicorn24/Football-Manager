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
 private fun work(task: suspend ()->Unit){viewModelScope.launch{lock.withLock{
  mutable.update{it.copy(busy=true,error=null,message=null)}
  try{task()}catch(e: CancellationException){throw e}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Aktion konnte nicht bestätigt werden. Bitte erneut versuchen oder den gespeicherten Stand laden.")}}
  finally{mutable.update{it.copy(busy=false)}}
 }}}
 fun create(seed: Long,c: ClubDraft,p: PlayerDraft,slot: Int,sandboxPlayers: List<PlayerDraft> = emptyList())=work{val w=withContext(Dispatchers.Default){WorldFactory.createWorld(seed,c,p,sandboxPlayers)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createRealMode(seed: Long,clubKey: String,p: PlayerDraft,slot: Int)=work{val w=withContext(Dispatchers.Default){WorldFactory.createRealModeWorld(seed,clubKey,p)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createCustomReal(seed:Long,slotClubKey:String,c:ClubDraft,p:PlayerDraft,players:List<PlayerDraft>,slot:Int)=work{val w=withContext(Dispatchers.Default){WorldFactory.createCustomClubWorld(seed,slotClubKey,c,p,players)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createTopClub(seed: Long,clubKey: String,p: PlayerDraft,slot: Int)=createRealMode(seed,clubKey,p,slot)
 fun load(slot: Int)=work{val w=repo.load(slot);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun delete(slot: Int)=work{repo.delete(slot)}
 fun saveAs(slot: Int)=work{val w=mutable.value.world?:return@work;repo.save(slot,w);mutable.update{it.copy(slot=slot,message="In Slot $slot gespeichert.")}}
 fun backToMenu()=work{mutable.value.world?.let{repo.save(mutable.value.slot,it)};mutable.value=GameState()}
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

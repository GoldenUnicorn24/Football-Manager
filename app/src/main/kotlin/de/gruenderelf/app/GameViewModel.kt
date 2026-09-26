package de.gruenderelf.app
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.gruenderelf.app.data.*
import de.gruenderelf.app.ui.CHANGELOG_LOADING
import de.gruenderelf.app.ui.CHANGELOG_VERSION
import de.gruenderelf.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GameState(val world: World?=null,val slot: Int=1,val busy: Boolean=false,val error: String?=null,val message: String?=null,val revision: Long=0)
class GameViewModel(application: Application): AndroidViewModel(application){
 private val repo=GameRepository(application);private val lock=Mutex();private val mutable=MutableStateFlow(GameState())
 private var liveRunnerJob:Job?=null
 private var decisionJob:Job?=null
 private var deferredSaveJob:Job?=null
 @Volatile private var liveRunnerEnabled=false
 @Volatile private var liveRunnerFixtureId=0
 @Volatile private var liveRunnerSpeed=MatchSpeed.NORMAL
 val state=mutable.asStateFlow()
 val slots=repo.saves.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val lastSlot=repo.lastSlot.stateIn(viewModelScope,SharingStarted.Eagerly,1)
 val matchSpeed=repo.matchSpeed.stateIn(viewModelScope,SharingStarted.Eagerly,MatchSpeed.NORMAL)
 val soundsEnabled=repo.soundsEnabled.stateIn(viewModelScope,SharingStarted.Eagerly,true)
 val changelogSeenVersion=repo.changelogSeenVersion.stateIn(viewModelScope,SharingStarted.Eagerly,CHANGELOG_LOADING)
 val developerPasswordConfigured=repo.developerPasswordConfigured.stateIn(viewModelScope,SharingStarted.Eagerly,false)
 private fun memoryPressure(){
  liveRunnerEnabled=false;liveRunnerJob?.cancel();liveRunnerJob=null;decisionJob?.cancel();decisionJob=null
  mutable.update{it.copy(error="Zu wenig freier Arbeitsspeicher. Der Vorgang wurde gestoppt, statt die App zu beenden. Bitte erneut laden.")}
 }
 private fun scheduleSave(slot:Int){
  deferredSaveJob?.cancel()
  deferredSaveJob=viewModelScope.launch{
   delay(700)
   lock.withLock{
    val current=mutable.value
    val w=current.world
    if(w!=null&&current.slot==slot){
     try{repo.save(slot,w)}
     catch(e:CancellationException){throw e}
     catch(e:Exception){mutable.update{it.copy(error="Automatisches Speichern fehlgeschlagen.")}}
    }
   }
  }
 }
 private fun work(task: suspend ()->Unit){viewModelScope.launch{lock.withLock{
  mutable.update{it.copy(busy=true,error=null,message=null)}
  try{task()}catch(e: CancellationException){throw e}catch(_:OutOfMemoryError){memoryPressure()}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Aktion konnte nicht bestätigt werden. Bitte erneut versuchen oder den gespeicherten Stand laden.")}}
  finally{mutable.update{it.copy(busy=false)}}
 }}}
 private fun careerOptions(w:World,tutorial:Boolean):World{w.user.tutorialEnabled=tutorial;w.user.tutorialStep=0;w.user.tutorialCompleted=!tutorial;return w}
 fun create(seed: Long,c: ClubDraft,p: PlayerDraft,slot: Int,sandboxPlayers: List<PlayerDraft> = emptyList(),tutorial:Boolean=false)=work{val w=withContext(Dispatchers.Default){careerOptions(WorldFactory.createWorld(seed,c,p,sandboxPlayers),tutorial)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createRealMode(seed: Long,clubKey: String,p: PlayerDraft,slot: Int,tutorial:Boolean=false,fantasyCupEnabled:Boolean=true)=work{val w=withContext(Dispatchers.Default){careerOptions(WorldFactory.createRealModeWorld(seed,clubKey,p,fantasyCupEnabled),tutorial)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createCustomReal(seed:Long,slotClubKey:String,c:ClubDraft,p:PlayerDraft,players:List<PlayerDraft>,slot:Int,tutorial:Boolean=false,fantasyCupEnabled:Boolean=true)=work{val w=withContext(Dispatchers.Default){careerOptions(WorldFactory.createCustomClubWorld(seed,slotClubKey,c,p,players,fantasyCupEnabled),tutorial)};repo.save(slot,w);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun createTopClub(seed: Long,clubKey: String,p: PlayerDraft,slot: Int,tutorial:Boolean=false)=createRealMode(seed,clubKey,p,slot,tutorial)
 fun load(slot: Int)=work{val w=repo.load(slot);mutable.value=GameState(w,slot,revision=mutable.value.revision+1)}
 fun delete(slot: Int)=work{repo.delete(slot)}
 fun saveAs(slot: Int)=work{val w=mutable.value.world?:return@work;repo.save(slot,w);mutable.update{it.copy(slot=slot,message="In Slot $slot gespeichert.")}}
 fun backToMenu()=work{mutable.value.world?.let{repo.save(mutable.value.slot,it)};mutable.value=GameState()}
 fun clearError(){mutable.update{it.copy(error=null)}}
 fun clearMessage(){mutable.update{it.copy(message=null)}}
 private fun preferenceWrite(fallback:String,block:suspend ()->Unit){viewModelScope.launch{
  try{block()}catch(e:CancellationException){throw e}catch(e:Exception){mutable.update{it.copy(error=e.message?:fallback)}}
 }}
 fun setMatchSpeed(v: MatchSpeed)=preferenceWrite("Das Spieltempo konnte nicht gespeichert werden."){repo.setMatchSpeed(v)}
 fun setSoundsEnabled(enabled: Boolean)=preferenceWrite("Die Sound-Einstellung konnte nicht gespeichert werden."){repo.setSoundsEnabled(enabled)}
 fun markChangelogSeen(version:String=CHANGELOG_VERSION)=preferenceWrite("Der Changelog-Status konnte nicht gespeichert werden."){repo.markChangelogSeen(version)}
 fun openDeveloperMode(password:String,configure:Boolean=false)=work{
  require(repo.verifyDeveloperPassword(password)){"Developer-Code falsch."}
  val exists=repo.developerSaveExists()
  var world:World?=null
  if(exists){
   world=try{repo.load(BMW_DEVELOPER_SAVE_SLOT)}catch(_:Exception){null}
  }
  if(world?.developer?.enabled!=true){
   if(exists)try{repo.delete(BMW_DEVELOPER_SAVE_SLOT)}catch(_:Exception){}
   val fresh=withContext(Dispatchers.Default){BmwDeveloperWorldFactory.create()}
   repo.save(BMW_DEVELOPER_SAVE_SLOT,fresh)
   world=fresh
  }else if(BmwDeveloperWorldFactory.repairDeveloperSave(requireNotNull(world))){
   repo.save(BMW_DEVELOPER_SAVE_SLOT,world)
  }
  mutable.value=GameState(world,BMW_DEVELOPER_SAVE_SLOT,revision=mutable.value.revision+1,message="BMW FC Experience geladen.")
 }
 fun resetDeveloperWorld()=work{
  val world=BmwDeveloperWorldFactory.create(System.currentTimeMillis())
  repo.save(BMW_DEVELOPER_SAVE_SLOT,world)
  mutable.value=GameState(world,BMW_DEVELOPER_SAVE_SLOT,revision=mutable.value.revision+1,message="BMW FC Experience neu aufgebaut.")
 }
 fun investTechnology(domain:TechDomain,amount:Long)=action("Forschungsbudget wurde freigegeben.",persistNow=true){BmwDeveloperSystems.invest(it,domain,amount)}
 fun enrollLongevity(playerId:Int)=action("Longevity-Programm aktualisiert.",persistNow=true){BmwDeveloperSystems.enrollLongevity(it,playerId)}
 fun action(message: String?=null,persistNow:Boolean=false,block: (World)->Unit)=work{
  val current=mutable.value.world?:return@work
  withContext(Dispatchers.Default){block(current);SaveCodec.requireRuntimeIntegrity(current)}
  val slot=mutable.value.slot
  mutable.update{it.copy(world=current.copy(),revision=it.revision+1,message=message)}
  if(persistNow)repo.save(slot,current) else scheduleSave(slot)
 }
 fun startMatch()=action(persistNow=true){require(it.live==null){"Das Spiel läuft bereits."};it.live=MatchEngine.start(it)}
 fun step(count: Int)=work{
  val current=mutable.value.world?:return@work
  val beforeMinute=current.live?.minute?:0
  withContext(Dispatchers.Default){current.live?.let{m->repeat(count){if(!m.finished)MatchEngine.step(current,m)}};SaveCodec.requireRuntimeIntegrity(current)}
  val m=current.live;if(m!=null&&(m.minute/10!=beforeMinute/10||m.pendingDecision||m.halfTime||m.finished))repo.checkpoint(mutable.value.slot,current)
  mutable.update{it.copy(world=current.copy(),revision=it.revision+1)}
 }
 // Der automatische Livetakt nutzt absichtlich nicht den globalen busy-Status.
 // Dadurch bleiben Taktik-, Pause- und Wechselknöpfe zwischen den einzelnen Minuten bedienbar.
 private suspend fun liveStepAwait(){
  var checkpointWorld:World?=null;var checkpointSlot=0
  lock.withLock{
   try{
    val current=mutable.value.world?:return@withLock
    val beforeMinute=current.live?.minute?:0
    withContext(Dispatchers.Default){current.live?.let{m->MatchEngine.step(current,m)};SaveCodec.requireRuntimeIntegrity(current)}
    val m=current.live
    val checkpoint=m!=null&&(m.minute/15!=beforeMinute/15||m.pendingDecision||m.halfTime||m.finished||m.incidentPause||m.assistantSubPending)
    mutable.update{it.copy(world=current.copy(),revision=it.revision+1)}
    if(checkpoint){checkpointSlot=mutable.value.slot;checkpointWorld=current}
   }catch(e: CancellationException){throw e}catch(_:OutOfMemoryError){memoryPressure()}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Die Live-Simulation konnte nicht fortgesetzt werden.")}}
  }
  checkpointWorld?.let{snapshot->
   try{repo.checkpoint(checkpointSlot,snapshot)}catch(e:CancellationException){throw e}catch(_:OutOfMemoryError){memoryPressure()}catch(e:Exception){mutable.update{it.copy(error=e.message?:"Der Live-Checkpoint konnte nicht gespeichert werden.")}}
  }
 }
 fun liveStep():Job=viewModelScope.launch{liveStepAwait()}
 private fun ensureLiveRunner(){
  if(!liveRunnerEnabled||liveRunnerJob?.isActive==true)return
  val fixtureId=liveRunnerFixtureId
  liveRunnerJob=viewModelScope.launch{
   while(isActive&&liveRunnerEnabled&&liveRunnerFixtureId==fixtureId){
    val m=mutable.value.world?.live
    if(m==null||m.fixtureId!=fixtureId||m.finished){liveRunnerEnabled=false;break}
    if(m.halfTime||m.pendingDecision||m.incidentPause||m.assistantSubPending){delay(200);continue}
    delay(MatchEngine.liveFrameDurationMs(m.livePhase,liveRunnerSpeed))
    if(!liveRunnerEnabled||liveRunnerFixtureId!=fixtureId)break
    val current=mutable.value.world?.live
    if(current==null||current.fixtureId!=fixtureId||current.finished){liveRunnerEnabled=false;break}
    if(!current.halfTime&&!current.pendingDecision&&!current.incidentPause&&!current.assistantSubPending)liveStepAwait()
   }
  }.also{job->job.invokeOnCompletion{
   if(liveRunnerJob===job)liveRunnerJob=null
   if(liveRunnerEnabled)viewModelScope.launch{delay(250);ensureLiveRunner()}
  }}
 }
 fun setLiveRunning(enabled:Boolean,speed:MatchSpeed,fixtureId:Int){
  liveRunnerSpeed=speed;liveRunnerFixtureId=fixtureId;liveRunnerEnabled=enabled
  if(enabled)ensureLiveRunner()else{liveRunnerJob?.cancel();liveRunnerJob=null}
 }
 fun quickSimulate(toHalfTime:Boolean)=work{
  val current=mutable.value.world?:return@work
  withContext(Dispatchers.Default){
   val m=current.live?:error("Es läuft kein Spiel.")
   if(toHalfTime)MatchEngine.simulateToHalfTime(current,m) else MatchEngine.simulateRemaining(current,m)
   SaveCodec.requireRuntimeIntegrity(current)
  }
  repo.checkpoint(mutable.value.slot,current)
  mutable.update{it.copy(world=current.copy(),revision=it.revision+1,message=if(toHalfTime)"Bis zur Halbzeit simuliert." else "Spiel vollständig simuliert.")}
 }
 fun liveAction(block: (World)->Unit):Job=viewModelScope.launch{lock.withLock{
  try{
   val current=mutable.value.world?:return@withLock;val slot=mutable.value.slot
   withContext(Dispatchers.Default){block(current);current.live?.let{MatchEngine.captureBallFrame(it)};SaveCodec.requireRuntimeIntegrity(current)}
   mutable.update{it.copy(world=current.copy(),revision=it.revision+1,error=null)}
   scheduleSave(slot)
  }catch(e: CancellationException){throw e}catch(_:OutOfMemoryError){memoryPressure()}catch(e: Exception){mutable.update{it.copy(error=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Aktion nicht möglich." else "Die Live-Aktion konnte nicht bestätigt werden.")}}
 }}
 private fun liveTacticAction(block:(World)->Unit){viewModelScope.launch{lock.withLock{
  try{
   val current=mutable.value.world?:return@withLock
   withContext(Dispatchers.Default){
    val liveBefore=current.live;val t=current.club().tactics;val before=listOf<Any>(t.formation,t.mentality,t.pressing,t.line,t.tempo,t.width,t.buildUp,liveBefore?.homeMentality?:0,liveBefore?.awayMentality?:0)
    block(current)
    val t2=current.club().tactics;val liveAfter=current.live;val after=listOf<Any>(t2.formation,t2.mentality,t2.pressing,t2.line,t2.tempo,t2.width,t2.buildUp,liveAfter?.homeMentality?:0,liveAfter?.awayMentality?:0)
    if(before!=after&&liveAfter!=null)MatchAnalysisSystem.recordTacticChange(current,liveAfter,current.user.clubId,"Live-Taktik angepasst")
    liveAfter?.let{MatchEngine.captureBallFrame(it)};SaveCodec.requireRuntimeIntegrity(current)
   }
   mutable.update{it.copy(world=current.copy(),revision=it.revision+1,error=null)}
   scheduleSave(mutable.value.slot)
  }catch(e:CancellationException){throw e}catch(_:OutOfMemoryError){memoryPressure()}catch(e:Exception){mutable.update{it.copy(error=e.message?:"Die Live-Taktik konnte nicht geändert werden.")}}
 }}}
 fun decide(d: Decision){
  if(decisionJob?.isActive==true)return
  decisionJob=liveAction{w->val m=w.live?:error("Es läuft kein Spiel.");require(m.pendingDecision){"Gerade steht keine Entscheidung an."};MatchEngine.decide(w,m,d)}
 }
 fun changeFormation(formation: String)=liveTacticAction{w->w.live?.let{MatchEngine.changeFormation(w,it,w.user.clubId,formation)}}
 fun setConserveEnergy(enabled: Boolean)=liveTacticAction{w->w.live?.let{MatchEngine.setConserveEnergy(w,it,w.user.clubId,enabled)}}
 fun setAllOutAttack(enabled: Boolean)=liveTacticAction{w->w.live?.let{MatchEngine.setAllOutAttack(w,it,w.user.clubId,enabled)}}
 fun setControlGame(enabled: Boolean)=liveTacticAction{w->w.live?.let{MatchEngine.setControlGame(w,it,w.user.clubId,enabled)}}
 fun updateLiveTactic(block: (World)->Unit)=liveTacticAction(block)
 fun substitute(out: Int,incoming: Int)=liveAction{w->w.live?.let{MatchEngine.substitute(w,it,out,incoming,w.user.clubId)}}
 fun moveLiveLineupSlot(fromIndex:Int,toIndex:Int)=liveTacticAction{w->w.live?.let{MatchEngine.moveLiveLineupSlot(w,it,fromIndex,toIndex,w.user.clubId)}}
 fun acceptAssistantSubstitution(selectedOutIds:Set<Int>?=null)=liveAction{w->w.live?.let{MatchEngine.acceptAssistantSubstitution(w,it,selectedOutIds)}}
 fun rejectAssistantSubstitution()=liveAction{w->w.live?.let{MatchEngine.rejectAssistantSubstitution(w,it)}}
 fun resumeIncident()=liveAction{w->w.live?.let{MatchEngine.resumeIncident(w,it)}}
 fun secondHalf()=liveAction{it.live?.let{m->MatchEngine.secondHalf(m)}}
 fun finishWeek()=work{
  val current=mutable.value.world?:return@work
  withContext(Dispatchers.Default){SeasonEngine.advanceWeek(current);SaveCodec.requireRuntimeIntegrity(current)}
  repo.save(mutable.value.slot,current);mutable.update{it.copy(world=current.copy(),revision=it.revision+1,message="Partie abgeschlossen und gespeichert.")}
 }
 fun exportTo(uri: Uri)=work{
  val w=mutable.value.world?:return@work
  withContext(Dispatchers.IO){val stream=getApplication<Application>().contentResolver.openOutputStream(uri)?:error("Datei nicht erreichbar.");stream.use{it.write(SaveCodec.encode(w).toByteArray(Charsets.UTF_8));it.flush()}}
  mutable.update{it.copy(message="Spielstand als Datei exportiert.")}
 }
 fun importFrom(uri: Uri,slot: Int)=work{
  val w=withContext(Dispatchers.IO){val stream=getApplication<Application>().contentResolver.openInputStream(uri)?:error("Datei nicht erreichbar.");val text=stream.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=32*1024*1024){"Die Datei ist zu groß."};out.write(buffer,0,n)};out.toString("UTF-8")};SaveCodec.decode(text)}
  repo.save(slot,w);mutable.value=GameState(w,slot,message="Spielstand importiert.",revision=mutable.value.revision+1)
 }
}

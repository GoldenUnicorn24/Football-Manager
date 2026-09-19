package de.gruenderelf.engine

import kotlin.math.exp
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.pow


data class SubSuggestion(val outId: Int,val inId: Int,val target: Position,val score: Double,val reason: String)

object MatchEngine {
 private val ballPhases=setOf(LivePhase.POSSESSION,LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK,LivePhase.COUNTER,LivePhase.CORNER,LivePhase.FREE_KICK,LivePhase.DANGEROUS_FREE_KICK,LivePhase.PENALTY,LivePhase.SHOOTOUT,LivePhase.SHOT_OFF_TARGET,LivePhase.SHOT_ON_TARGET,LivePhase.WOODWORK,LivePhase.GOAL)

 fun start(w: World,f: Fixture=w.nextFixture()?:error("Kein Spiel vorhanden.")): LiveMatch {
  require(!f.played){"Dieses Spiel wurde schon gewertet."}
  for(id in listOf(f.homeId,f.awayId)){
   val c=w.clubs.getValue(id)
   if(id!=w.user.clubId||c.tactics.xi.size!=11)WorldFactory.autoLineup(w,id)
   else{
    val used=mutableSetOf<Int>();c.tactics.xi=c.tactics.xi.map{pid->if(w.players[pid]?.let{it.available&&it.clubId==id}==true&&used.add(pid))pid else 0}.toMutableList()
    val available=w.squad(id).filter{it.available&&it.id !in used}.toMutableList()
    c.tactics.xi.indices.filter{c.tactics.xi[it]==0}.forEach{i->val pos=Formations.positions(c.tactics.formation)[i];val p=available.maxByOrNull{it.ca*it.fit(pos)};if(p!=null){c.tactics.xi[i]=p.id;available.remove(p)}}
    WorldFactory.rebuildBench(w,c)
   }
  }
  val h=w.clubs.getValue(f.homeId);val a=w.clubs.getValue(f.awayId)
  autoTuneAiTactics(w,h,a);autoTuneAiTactics(w,a,h)
  val rng=SeededRandom(w.seed xor(f.id.toLong()*1000003) xor w.calendar.season.toLong())
  val weather=rng.pick(listOf("Trocken","Trocken","Leichter Regen","Wind","Dauerregen"));val derby=w.isDerby(h.id,a.id)
  val fans=((h.members*.7+h.reputation*3.4)*(if(derby)1.35 else 1.0)*(if(h.stadium.floodlights)1.18 else 1.0)*(1+h.stadium.stand*.003)*(if(weather=="Dauerregen").72 else 1.0)*rng.int(85,115)/100).roundToInt().coerceIn(0,h.stadium.capacity)
  val m=LiveMatch(
   f.id,h.id,a.id,rng.state,duration=90,
   homeXi=h.tactics.xi.toMutableList(),awayXi=a.tactics.xi.toMutableList(),
   homeBench=h.tactics.bench.toMutableList(),awayBench=a.tactics.bench.toMutableList(),
   homeMentality=h.tactics.mentality,awayMentality=a.tactics.mentality,
   homeFormation=h.tactics.formation,awayFormation=a.tactics.formation,
   weather=weather,attendance=fans,
   knockout=CompetitionEngine.isDecisiveKnockoutFixture(w,f)
  )
  MatchIntelligence.install(w,m)
  val kickoffHome=rng.chance(.5);m.kickoffHomeFirst=kickoffHome;m.extraKickoffHome=rng.chance(.5)
  m.homeInPossession=kickoffHome;m.chainOwnerClubId=if(kickoffHome)h.id else a.id;m.liveClubId=m.chainOwnerClubId;m.livePhase=LivePhase.POSSESSION;m.liveDetail="Anstoß";m.liveEventSerial=1;m.lastPossessionChangeEventSerial=1
  (m.homeXi+m.awayXi).filter{it!=0}.forEach{ensurePerformance(w,m,it)}
  m.rngState=rng.state
  log(m,"${h.stadium.name}: $fans Zuschauer. $weather. ${h.stadium.surface.label}.")
  log(m,if(derby)"Derby! Heute geht es auch um den Stolz im Ort." else "Der Schiedsrichter gibt den Anstoß frei.",if(derby)"derby" else "normal")
  val userHome=w.user.clubId==m.homeId;val captain=captainFor(w,m,userHome)
  if(captain!=0)log(m,"${w.players.getValue(captain).name} führt ${w.clubs.getValue(w.user.clubId).shortName} heute als Kapitän aufs Feld.")
  return m
 }

 private fun formation(m: LiveMatch,home: Boolean)=if(home)m.homeFormation else m.awayFormation
 private fun clubId(m: LiveMatch,home: Boolean)=if(home)m.homeId else m.awayId
 private fun isHome(m: LiveMatch,clubId: Int)=clubId==m.homeId
 private fun conserve(m: LiveMatch,home: Boolean)=if(home)m.homeConserveEnergy else m.awayConserveEnergy
 private fun allOut(m: LiveMatch,home: Boolean)=if(home)m.homeAllOutAttack else m.awayAllOutAttack
 private fun controlGame(m: LiveMatch,home: Boolean)=if(home)m.homeControlGame else m.awayControlGame
 private fun stats(m: LiveMatch,home: Boolean)=if(home)m.home else m.away
 private fun xi(m: LiveMatch,home: Boolean)=if(home)m.homeXi else m.awayXi

 fun clockLabel(m: LiveMatch): String {
  val base=when(m.period){1->45;2->90;3->105;4->120;else->m.minute}
  return if(m.stoppageMinute>0&&m.period in 1..4)"$base+${m.stoppageMinute}" else m.minute.toString()
 }
 fun breakActionLabel(m: LiveMatch)=m.breakType.action
 private fun periodBaseEnd(m: LiveMatch)=when(m.period){1->45;2->90;3->105;4->120;else->m.minute}
 private fun addedForPeriod(m: LiveMatch)=when(m.period){1->m.firstHalfAdded;2->m.secondHalfAdded;3->m.extraFirstAdded;4->m.extraSecondAdded;else->0}
 private fun setAddedForPeriod(m: LiveMatch,value: Int){when(m.period){1->m.firstHalfAdded=value;2->m.secondHalfAdded=value;3->m.extraFirstAdded=value;4->m.extraSecondAdded=value}}
 private fun addStoppageTime(m: LiveMatch,seconds: Int){
  if(m.period !in 1..4||seconds<=0)return
  m.stoppageLossSeconds=(m.stoppageLossSeconds+seconds).coerceAtMost(900)
  if(m.stoppageAnnounced){
   val recalculated=calculateStoppage(m)
   if(recalculated>addedForPeriod(m))setAddedForPeriod(m,recalculated)
  }
 }
 private fun calculateStoppage(m: LiveMatch): Int {
  val baseSeconds=when(m.period){1->50;2->80;3,4->35;else->0}
  val raw=((baseSeconds+m.stoppageLossSeconds+29)/60)
  return when(m.period){
   1->raw.coerceIn(1,7)
   2->raw.coerceIn(2,10)
   3,4->raw.coerceIn(1,5)
   else->0
  }
 }
 fun stoppageTimeOverlay(m: LiveMatch): String? {
  if(m.period !in 1..4||m.finished||m.halfTime)return null
  val base=periodBaseEnd(m)
  if(m.minute<base-2)return null
  val added=if(m.stoppageAnnounced)addedForPeriod(m) else calculateStoppage(m)
  return "Nachspielzeit +$added"
 }
 private fun announceStoppage(m: LiveMatch){
  if(m.period !in 1..4||m.stoppageAnnounced)return
  val base=periodBaseEnd(m);if(m.minute<base)return
  val added=calculateStoppage(m);setAddedForPeriod(m,added);m.stoppageAnnounced=true
  log(m,"Der vierte Offizielle zeigt +$added Minute${if(added==1)"" else "n"} Nachspielzeit an.")
 }
 private fun periodComplete(m: LiveMatch): Boolean {
  if(m.period !in 1..4)return false
  val base=periodBaseEnd(m);val added=addedForPeriod(m)
  return m.minute>=base&&m.stoppageAnnounced&&m.stoppageMinute>=added
 }
 private fun advanceClock(m: LiveMatch){val base=periodBaseEnd(m);if(m.minute<base)m.minute++ else{m.stoppageMinute++;if(m.stoppageMinute==1)log(m,"Die Nachspielzeit läuft.")}}
 private fun resetPeriodClock(m: LiveMatch,startMinute: Int){m.minute=startMinute;m.stoppageMinute=0;m.stoppageAnnounced=false;m.stoppageLossSeconds=0}
 private fun kickoff(m: LiveMatch,home: Boolean,detail: String){
  m.ballX=.5f;m.ballY=.5f;m.homeInPossession=home;m.chainOwnerClubId=clubId(m,home);m.liveClubId=m.chainOwnerClubId;m.livePlayerId=0
  m.livePhase=LivePhase.POSSESSION;m.liveDetail=detail;m.chainStep=0;m.chainTicks=0;m.counterTicksRemaining=0
  m.pendingCornerClubId=0;m.pendingSetPieceClubId=0;m.pendingPossessionClubId=0;m.possessionChangeReason=PossessionChangeReason.KICK_OFF;m.liveEventSerial++
 }

 private fun configuredOnPitch(w: World,m: LiveMatch,home: Boolean,id: Int,allowKeeper: Boolean=true): Int {
  if(id==0||id !in xi(m,home)||id in m.injured)return 0
  val p=w.players[id]?:return 0
  if(!allowKeeper&&p.position==Position.TW)return 0
  return id
 }

 private fun captainFor(w: World,m: LiveMatch,home: Boolean): Int {
  val c=w.clubs.getValue(clubId(m,home));val preferred=configuredOnPitch(w,m,home,c.tactics.captainId)
  if(preferred!=0)return preferred
  return xi(m,home).filter{it!=0&&it !in m.injured}.maxByOrNull{id->val p=w.players.getValue(id);p.hidden.pressure*.42+p.hidden.professionalism*.34+p.hidden.consistency*.16+(w.calendar.season-p.birthYear).coerceIn(18,38)*.08}?:0
 }

 private fun targetPlayerFor(w: World,m: LiveMatch,home: Boolean): Int {
  val c=w.clubs.getValue(clubId(m,home))
  return configuredOnPitch(w,m,home,c.tactics.targetPlayerId,false)
 }

 private fun lineupRating(w: World,c: Club): Double {
  val slots=Formations.positions(c.tactics.formation)
  val values=c.tactics.xi.mapIndexedNotNull{i,id->w.players[id]?.let{player->player.ratingAt(slots.getOrElse(i){player.position}).toDouble()}}
  return values.average().takeIf{!it.isNaN()}?:35.0
 }

 private fun autoTuneAiTactics(w: World,c: Club,opponent: Club){
  if(c.id==w.user.clubId)return
  val team=c.tactics.xi.mapNotNull{w.players[it]};if(team.isEmpty())return
  val technical=team.map{(it.attributes.passing*.38+it.attributes.vision*.32+it.attributes.technique*.30)}.average()
  val direct=team.map{(it.attributes.pace*.42+it.attributes.finishing*.35+it.attributes.strength*.23)}.average()
  val defensive=team.map{(it.attributes.tackling*.46+it.attributes.stamina*.31+it.attributes.strength*.23)}.average()
  val flank=team.filter{it.position in listOf(Position.LA,Position.RA,Position.LV,Position.RV)}.map{it.ratingAt(it.position)}.average().takeIf{!it.isNaN()}?:technical
  val central=team.filter{it.position in listOf(Position.DM,Position.ZM,Position.OM)}.map{it.ratingAt(it.position)}.average().takeIf{!it.isNaN()}?:technical
  val diff=lineupRating(w,c)-lineupRating(w,opponent)
  val scout=MatchIntelligence.preMatch(w,c,opponent)
  c.tactics.buildUp=when{
   scout.weakness!="Keine klare Schwäche"&&c.dynamics.opponentPrep>=35->scout.approach
   technical>=direct+8&&technical>=58->BuildUp.TIKI_TAKA
   technical>=direct+5->BuildUp.SHORT
   flank>=central+5->BuildUp.WIDE
   direct>=technical+8&&diff<3->BuildUp.COUNTER
   direct>=technical+5->BuildUp.DIRECT
   else->BuildUp.MIXED
  }
  c.tactics.mentality=when{diff>=14->4;diff<=-14->2;else->3}
  c.tactics.pressing=when{defensive>=62&&team.map{it.attributes.stamina}.average()>=60->4;diff<=-18->3;else->3}
  c.tactics.line=when{diff>=14->4;diff<=-12->2;else->3}
  c.tactics.tempo=when(c.tactics.buildUp){BuildUp.TIKI_TAKA->3;BuildUp.SHORT->2;BuildUp.DIRECT,BuildUp.COUNTER->4;BuildUp.WIDE->4;else->3}
  c.tactics.width=when(c.tactics.buildUp){BuildUp.WIDE->5;BuildUp.TIKI_TAKA,BuildUp.SHORT->4;BuildUp.COUNTER->4;else->3}
 }

 private fun controlStrength(w: World,m: LiveMatch,home: Boolean): Double {
  val ids=xi(m,home);val slots=Formations.positions(formation(m,home));var count=0
  val total=ids.mapIndexed{i,id->val p=w.players[id];if(p==null||id in m.injured)0.0 else{
   count++;val slot=slots.getOrElse(i){p.position}
   val role=when(slot){
    Position.TW->p.attributes.passing*.34+p.attributes.vision*.24+p.attributes.technique*.12+p.attributes.keeping*.30
    Position.IV,Position.LV,Position.RV->p.attributes.passing*.38+p.attributes.vision*.24+p.attributes.technique*.20+p.attributes.overall(slot)*.18
    Position.DM,Position.ZM,Position.OM->p.attributes.passing*.35+p.attributes.vision*.30+p.attributes.technique*.25+p.attributes.overall(slot)*.10
    else->p.attributes.passing*.30+p.attributes.vision*.22+p.attributes.technique*.30+p.attributes.overall(slot)*.18
   }
   val fatigue=(.72+p.fitness*.0028).coerceIn(.74,1.0)
   role*p.fit(slot)*fatigue*(.96+(p.form-5.0)*.012).coerceIn(.91,1.06)*(.95+p.morale*.0007)
  }}.sum()
  if(count==0)return 4.0
  val availability=(count/11.0).coerceIn(.55,1.0)
  val homeBoost=if(home)1.012 else 1.0
  val captainId=captainFor(w,m,home);val captain=w.players[captainId]
  val captainBoost=if(captain==null)1.0 else 1.0+(((captain.hidden.pressure+captain.hidden.professionalism)-90).coerceIn(0,100))*.00022
  return total/11.0*homeBoost*captainBoost/availability.pow(.15)
 }

 private fun pressureStrength(w: World,m: LiveMatch,home: Boolean): Double {
  val c=w.clubs.getValue(clubId(m,home));val ids=xi(m,home);val slots=Formations.positions(formation(m,home));var count=0
  val total=ids.mapIndexed{i,id->val p=w.players[id];if(p==null||id in m.injured)0.0 else{
   count++;val slot=slots.getOrElse(i){p.position}
   val role=if(slot==Position.TW)p.attributes.keeping*.55+p.attributes.passing*.20+p.attributes.vision*.25 else p.attributes.tackling*.42+p.attributes.stamina*.20+p.attributes.pace*.15+p.attributes.strength*.10+p.attributes.overall(slot)*.13
   role*p.fit(slot)*(.70+p.fitness*.003).coerceIn(.72,1.0)
  }}.sum()
  if(count==0)return 4.0
  val pressLevel=when{conserve(m,home)->1;allOut(m,home)->5;controlGame(m,home)->2;else->c.tactics.pressing}
  val lineLevel=when{conserve(m,home)->1;allOut(m,home)->5;controlGame(m,home)->2;else->c.tactics.line}
  val pressFactor=.88+pressLevel*.035
  val lineFactor=.97+(lineLevel-3)*.018
  return total/11.0*pressFactor*lineFactor
 }

 private fun buildRisk(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->-.020;BuildUp.SHORT->-.013;BuildUp.MIXED->0.0;BuildUp.WIDE->.005;BuildUp.DIRECT->.020;BuildUp.COUNTER->.028}
 private fun buildProgress(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->.82;BuildUp.SHORT->.90;BuildUp.MIXED->1.0;BuildUp.WIDE->1.02;BuildUp.DIRECT->1.15;BuildUp.COUNTER->1.10}
 private fun buildShotIntent(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->.90;BuildUp.SHORT->.94;BuildUp.MIXED->1.0;BuildUp.WIDE->.98;BuildUp.DIRECT->1.08;BuildUp.COUNTER->1.12}
 private fun recycleChance(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->.20;BuildUp.SHORT->.15;BuildUp.MIXED->.075;BuildUp.WIDE->.06;BuildUp.DIRECT->.025;BuildUp.COUNTER->.025}
 private fun longBallChance(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->.025;BuildUp.SHORT->.045;BuildUp.MIXED->.11;BuildUp.WIDE->.15;BuildUp.DIRECT->.32;BuildUp.COUNTER->.28}


 private fun teamChanceQuality(w: World,m: LiveMatch,home: Boolean): Double {
  val ids=xi(m,home).filter{it!=0&&it !in m.injured};if(ids.isEmpty())return .35
  val technical=ids.map{pId->val p=w.players.getValue(pId);p.attributes.passing*.36+p.attributes.vision*.34+p.attributes.technique*.30}.average()/100.0
  val c=w.clubs.getValue(clubId(m,home));val style=when(c.tactics.buildUp){BuildUp.TIKI_TAKA->.07;BuildUp.SHORT->.045;BuildUp.WIDE->.025;BuildUp.MIXED->0.0;BuildUp.DIRECT->-.02;BuildUp.COUNTER->-.01}
  val tempoPenalty=kotlin.math.abs(c.tactics.tempo-3)*.012
  return (technical+style-tempoPenalty).coerceIn(.18,.94)
 }

 private fun setDistanceFromGoal(m: LiveMatch,home: Boolean,distanceMeters: Double,x: Float){
  val y=(distanceMeters/105.0).toFloat().coerceIn(.035f,.965f)
  m.ballY=if(home)y else 1f-y;m.ballX=x.coerceIn(.035f,.965f)
 }

 /** Final pass/dribble creates a real pitch position before the shot is evaluated. */
 private fun prepareShotLocation(w: World,m: LiveMatch,home: Boolean,rng: SeededRandom): ShotType {
  val c=w.clubs.getValue(clubId(m,home));val opp=w.clubs.getValue(clubId(m,!home))
  val probe=ShotContext(m.ballX,m.ballY,home);val current=ShotModel.geometry(probe)
  val edge=((controlStrength(w,m,home)-pressureStrength(w,m,!home))/32.0).coerceIn(-1.5,1.5)
  val chanceQuality=teamChanceQuality(w,m,home)
  val wide=m.ballX !in .23f.. .77f
  val counter=m.liveDetail.contains("Konter",true)||m.liveDetail.contains("Gegenstoß",true)||m.counterTicksRemaining>0
  val create=(.34+edge*.13+(chanceQuality-.5)*.24+(c.tactics.mentality-3)*.025-(opp.tactics.pressing-3)*.018).coerceIn(.07,.70)

  // Flügelangriffe enden entweder mit echter Flanke oder Cutback in eine zentrale Zone.
  if(wide&&rng.chance((.28+(if(c.tactics.buildUp==BuildUp.WIDE).18 else 0.0)+(chanceQuality-.5)*.18).coerceIn(.16,.62))){
   val cutback=rng.chance((.36+(if(c.tactics.buildUp==BuildUp.TIKI_TAKA||c.tactics.buildUp==BuildUp.SHORT).20 else 0.0)+edge*.08).coerceIn(.18,.68))
   if(cutback){
    val d=(7.0+rng.nextDouble()*7.5).coerceIn(6.0,15.0);setDistanceFromGoal(m,home,d,(.38+rng.nextDouble()*.24).toFloat());m.liveDetail="Rückpass von der Grundlinie"
    return ShotType.CUTBACK
   }
   val d=6.5+rng.nextDouble()*8.0;setDistanceFromGoal(m,home,d,(.32+rng.nextDouble()*.36).toFloat());m.liveDetail="Flanke in den Strafraum"
   return if(rng.chance(.72))ShotType.HEADER else ShotType.VOLLEY
  }

  if(counter&&rng.chance((.34+edge*.13+chanceQuality*.10).coerceIn(.20,.72))){
   val d=8.0+rng.nextDouble()*9.0;setDistanceFromGoal(m,home,d,(.38+rng.nextDouble()*.24).toFloat());m.liveDetail="Frei vor dem Tor nach dem Konter"
   return ShotType.ONE_ON_ONE
  }

  // Technisch überlegene Mannschaften spielen häufiger bis in bessere Abschlussräume.
  if(rng.chance(create)){
   val veryGood=rng.chance((.22+edge*.14+(chanceQuality-.55)*.24).coerceIn(.08,.58))
   val d=if(veryGood)5.5+rng.nextDouble()*7.0 else 10.0+rng.nextDouble()*8.5
   val spread=if(veryGood).20 else .30
   setDistanceFromGoal(m,home,d,(.5-spread/2+rng.nextDouble()*spread).toFloat());m.liveDetail=if(veryGood)"Kombination bis in den Strafraum" else "Sauber freigespielt"
   return when{d<8.5->ShotType.CLOSE_RANGE;veryGood&&rng.chance(.35)->ShotType.ONE_ON_ONE;else->ShotType.BOX_SHOT}
  }

  // Unter starkem Druck wird ein Angriff gelegentlich zu einem frühen/ungünstigen Abschluss gezwungen.
  if(edge<-.35&&current.distanceMeters<19.0&&rng.chance((.18-edge*.14).coerceAtMost(.48))){
   val d=18.0+rng.nextDouble()*9.0;val side=if(rng.chance(.5))(.24+rng.nextDouble()*.20) else (.56+rng.nextDouble()*.20)
   setDistanceFromGoal(m,home,d,side.toFloat());m.liveDetail="Unter Druck zum Abschluss gezwungen"
  }
  val g=ShotModel.geometry(ShotContext(m.ballX,m.ballY,home))
  return when{g.distanceMeters>=23.5->ShotType.LONG_RANGE;g.distanceMeters<=8.5->ShotType.CLOSE_RANGE;else->ShotType.BOX_SHOT}
 }

 private fun buildShotContext(w: World,m: LiveMatch,home: Boolean,shooter: Player,type: ShotType,rng: SeededRandom,assistId: Int=0): ShotContext {
  val opp=w.clubs.getValue(clubId(m,!home));val edge=((controlStrength(w,m,home)-pressureStrength(w,m,!home))/34.0).coerceIn(-1.5,1.5)
  val teamPassQuality=(teamChanceQuality(w,m,home)-((opp.tactics.pressing-3)*.018)).coerceIn(.15,.96)
  val passer=w.players[assistId]
  val individualPassQuality=passer?.let{((it.attributes.passing*.46+it.attributes.vision*.34+it.attributes.technique*.20)/100.0).coerceIn(.15,.98)}
  val primePass=passer?.messiMentored==true
  val passQuality=((if(individualPassQuality!=null)teamPassQuality*.42+individualPassQuality*.58 else teamPassQuality)+(if(primePass).055 else 0.0)).coerceIn(.15,.995)
  var pressure=(.46-edge*.16+(opp.tactics.pressing-3)*.035+(rng.nextDouble()-.5)*.15-(if(shooter.messiMentored).11 else 0.0)).coerceIn(.03,.92)
  if(type==ShotType.ONE_ON_ONE)pressure=(pressure-.24).coerceAtLeast(.04)
  if(type==ShotType.CUTBACK||type==ShotType.REBOUND)pressure=(pressure-.10).coerceAtLeast(.04)
  if(type==ShotType.PENALTY)pressure=.05
  val defenders=when{
   type==ShotType.PENALTY||type==ShotType.ONE_ON_ONE->0
   pressure<.22->if(rng.chance(.72))0 else 1
   pressure<.45->rng.int(0,2)
   pressure<.68->rng.int(1,3)
   else->rng.int(2,4)
  }
  val clear=defenders==0&&pressure<.30||type==ShotType.ONE_ON_ONE
  val counter=m.liveDetail.contains("Konter",true)||m.liveDetail.contains("Gegenstoß",true)
  val strongFoot=type==ShotType.HEADER||shooter.foot==Foot.BOTH||rng.chance(((if(shooter.messiMentored).96 else .82)-pressure*(if(shooter.messiMentored).025 else .08)).coerceIn(.70,if(shooter.messiMentored).98 else .88))
  return ShotContext(m.ballX,m.ballY,home,type,pressure,defenders,passQuality,clear,counter,strongFoot)
 }

 private fun shotText(p: Player,type: ShotType,distance: Double)=when(type){
  ShotType.LONG_RANGE->"${p.lastName} zieht aus ${distance.roundToInt()} Metern ab."
  ShotType.BOX_SHOT->"${p.lastName} schließt aus ${distance.roundToInt()} Metern ab."
  ShotType.CLOSE_RANGE->"${p.lastName} kommt aus kurzer Distanz zum Abschluss."
  ShotType.ONE_ON_ONE->"${p.lastName} ist frei vor dem Tor."
  ShotType.HEADER->"${p.lastName} köpft aus ${distance.roundToInt()} Metern."
  ShotType.VOLLEY->"${p.lastName} nimmt den Ball volley."
  ShotType.CUTBACK->"Rückpass von der Grundlinie – ${p.lastName} mit der Direktabnahme."
  ShotType.REBOUND->"${p.lastName} kommt an den Abpraller."
  ShotType.FREE_KICK->"${p.lastName} zieht den Freistoß aus ${distance.roundToInt()} Metern direkt aufs Tor."
  ShotType.PENALTY->"${p.lastName} tritt zum Elfmeter an."
 }

 private fun shotTarget(outcome: ShotOutcome,rng: SeededRandom,penalty: Boolean=false): Triple<Float,Float,String> {
  if(outcome==ShotOutcome.OFF_TARGET){
   return when(rng.int(0,2)){0->Triple(-.12f,(.22+rng.nextDouble()*.52).toFloat(),"knapp links vorbei");1->Triple(1.12f,(.22+rng.nextDouble()*.52).toFloat(),"knapp rechts vorbei");else->Triple((.28+rng.nextDouble()*.44).toFloat(),1.16f,"über das Tor")}
  }
  if(outcome==ShotOutcome.WOODWORK){
   return when(rng.int(0,2)){0->Triple(0f,(.25+rng.nextDouble()*.55).toFloat(),"an den linken Pfosten");1->Triple(1f,(.25+rng.nextDouble()*.55).toFloat(),"an den rechten Pfosten");else->Triple((.22+rng.nextDouble()*.56).toFloat(),1f,"an die Latte")}
  }
  if(outcome==ShotOutcome.BLOCKED)return Triple(.5f,.42f,"zentral Richtung Tor")
  val lane=if(penalty&&rng.chance(.70))if(rng.chance(.5))0 else 2 else rng.int(0,2)
  val high=rng.chance(if(penalty).42 else .34)
  val x=when(lane){0->.16f;1->.50f;else->.84f};val y=if(high).82f else .24f
  val vertical=if(high)"oben" else "unten";val horizontal=when(lane){0->"links";1->"zentral";else->"rechts"}
  return Triple(x,y,"$vertical $horizontal")
 }

 private fun ensurePerformance(w: World,m: LiveMatch,id: Int): PlayerMatchPerformance {
  val p=w.players.getValue(id)
  return m.playerPerformance.getOrPut(id){PlayerMatchPerformance(fitnessStart=p.fitness)}
 }

 private fun setBallPhase(m: LiveMatch,phase: LivePhase,home: Boolean,playerId: Int=0,detail: String="",reason: PossessionChangeReason=PossessionChangeReason.NONE){
  require(phase in ballPhases){"${phase.name} ist keine Ballphase."}
  val id=clubId(m,home);val old=m.chainOwnerClubId
  if(old!=0&&old!=id){
   require(reason!=PossessionChangeReason.NONE){"Teamwechsel ohne nachvollziehbaren Ballbesitzwechsel: $old -> $id (${phase.label})"}
   m.possessionChangeReason=reason;m.lastPossessionChangeEventSerial=m.liveEventSerial+1
  }else if(reason!=PossessionChangeReason.NONE)m.possessionChangeReason=reason
  m.chainOwnerClubId=id;m.homeInPossession=home;m.livePhase=phase;m.liveClubId=id;m.livePlayerId=playerId;m.liveDetail=detail;m.liveEventSerial++
 }

 private fun setIncidentPhase(m: LiveMatch,phase: LivePhase,clubId: Int,playerId: Int,detail: String=""){
  require(phase !in ballPhases);m.livePhase=phase;m.liveClubId=clubId;m.livePlayerId=playerId;m.liveDetail=detail;m.liveEventSerial++
 }

 private fun strength(w: World,m: LiveMatch,home: Boolean,attack: Boolean): Double {
  val c=w.clubs.getValue(clubId(m,home));val ids=xi(m,home);val slots=Formations.positions(formation(m,home))
  val value=ids.mapIndexed{i,id->val p=w.players[id];if(p==null||id in m.injured)0.0 else{
   val slot=slots.getOrElse(i){p.position}
   val skill=if(attack)p.attributes.overall(slot)*.58+p.attributes.passing*.20+p.attributes.finishing*.22 else p.attributes.overall(slot)*.64+p.attributes.tackling*.25+p.attributes.vision*.11
   val fatigue=(.62+p.fitness*.0038).coerceIn(.65,1.0);val mental=MatchIntelligence.playerMentalFactor(w,m,p);val roleFit=when(p.effectiveRole()){PlayerRole.BALL_PLAYING_CB,PlayerRole.DEEP_PLAYMAKER,PlayerRole.PLAYMAKER->if(attack)1.025 else 1.0;PlayerRole.STOPPER,PlayerRole.ANCHOR,PlayerRole.INVERTED_FULLBACK->if(attack).99 else 1.03;PlayerRole.PRESSING_FORWARD->if(attack)1.02 else 1.015;PlayerRole.TARGET_FORWARD->if(attack&&c.tactics.buildUp in listOf(BuildUp.DIRECT,BuildUp.WIDE))1.035 else 1.0;else->1.0}
   skill*p.fit(slot)*(.86+p.form*.02)*fatigue*(.88+p.morale*.002)*(.9+p.sharpness*.001)*mental*roleFit
  }}.sum()/11
  val selectedMentality=if(home)m.homeMentality else m.awayMentality
  val mentality=when{conserve(m,home)->minOf(selectedMentality,1);allOut(m,home)->maxOf(selectedMentality,5);controlGame(m,home)->3;else->selectedMentality}
  val modeFactor=when{
   conserve(m,home)->if(attack).98 else 1.04
   allOut(m,home)->if(attack)1.08 else .92
   controlGame(m,home)->if(attack).96 else 1.03
   else->1.0
  }
  return (value*(if(attack)1+(mentality-3)*.055+(c.tactics.tempo-3)*.02 else 1-(mentality-3)*.04+(c.tactics.pressing-3)*.02)*modeFactor*MatchIntelligence.teamFactor(c,attack)).coerceAtLeast(4.0)
 }

 private fun averageFitness(w: World,m: LiveMatch,home: Boolean): Double = xi(m,home).filter{it!=0&&it !in m.injured}.map{w.players.getValue(it).fitness}.average().takeIf{!it.isNaN()}?:70.0

 private fun applyMinuteFitness(w: World,m: LiveMatch){
  val phaseOwner=m.chainOwnerClubId
  for(home in listOf(true,false)){
   val c=w.clubs.getValue(clubId(m,home));val under=(11-xi(m,home).count{it!=0}).coerceAtLeast(0)
   for(id in xi(m,home))if(id!=0&&id !in m.injured){
    val p=w.players.getValue(id);val perf=ensurePerformance(w,m,id)
    var cost=.022+c.tactics.pressing*.0045+c.tactics.tempo*.0035+under*.0035
    when{conserve(m,home)->cost*=.67;allOut(m,home)->cost*=1.28;controlGame(m,home)->cost*=.84}
    if(phaseOwner==c.id){cost+=when(m.livePhase){LivePhase.COUNTER->.045;LivePhase.DANGEROUS_ATTACK->.024;LivePhase.ATTACK->.014;else->0.0}}
    if(c.tactics.pressing>=4&&phaseOwner!=c.id)cost+=.008
    cost*=(1.20-p.attributes.stamina*.0048).coerceIn(.68,1.12)
    p.fitness=(p.fitness-cost).coerceAtLeast(5.0)
    m.minutesPlayed[id]=(m.minutesPlayed[id]?:0)+1;perf.minutes=m.minutesPlayed[id]?:perf.minutes
    if(id !in m.participation)m.participation.add(id)
   }
  }
 }

 private fun possessionStyleBonus(build: BuildUp)=when(build){BuildUp.TIKI_TAKA->.040;BuildUp.SHORT->.025;BuildUp.MIXED->0.0;BuildUp.WIDE->.005;BuildUp.DIRECT->-.020;BuildUp.COUNTER->-.025}

 private fun recordPossessionTime(w: World,m: LiveMatch,ownerHome: Boolean,rng: SeededRandom){
  // Eine Engine-Stufe repräsentiert ungefähr eine Spielminute. Der sichtbare Ballbesitz
  // wird deshalb als simulierte Sekunden innerhalb dieser Minute verteilt statt stumpf
  // nur dem aktuellen Kettenbesitzer einen Punkt zu geben. So zählt auch, wie gut ein
  // Team unter Druck den Ball hält bzw. wie schnell ein deutlich besseres Team ihn zurückerobert.
  val homeControl=controlStrength(w,m,true)
  val awayControl=controlStrength(w,m,false)
  val qualityShare=(1.0/(1.0+exp(-((homeControl-awayControl)/18.0).coerceIn(-3.2,3.2))))
  val ownerShare=if(ownerHome).72 else .28
  val homeStyle=possessionStyleBonus(w.clubs.getValue(m.homeId).tactics.buildUp)
  val awayStyle=possessionStyleBonus(w.clubs.getValue(m.awayId).tactics.buildUp)
  val styleDelta=(homeStyle-awayStyle)
  fun modePossession(home: Boolean)=when{conserve(m,home)->-.065;allOut(m,home)->.018;controlGame(m,home)->.055;else->0.0}
  val modeDelta=modePossession(true)-modePossession(false)
  val jitter=(rng.nextDouble()-.5)*.035
  val homeShare=(qualityShare*.55+ownerShare*.45+styleDelta+modeDelta+jitter).coerceIn(.12,.88)
  val homeSeconds=(homeShare*60.0).roundToInt().coerceIn(7,53)
  m.home.possessionTicks+=homeSeconds
  m.away.possessionTicks+=60-homeSeconds
 }

 private fun recordPossession(w: World,m: LiveMatch,home: Boolean,rng: SeededRandom){
  val team=xi(m,home).filter{it!=0&&it !in m.injured};if(team.isEmpty())return
  val c=w.clubs.getValue(clubId(m,home));val opponent=w.clubs.getValue(clubId(m,!home))
  val passes=(when(c.tactics.buildUp){
   BuildUp.TIKI_TAKA->rng.int(6,10);BuildUp.SHORT->rng.int(5,9);BuildUp.MIXED->rng.int(3,7);BuildUp.WIDE->rng.int(3,7);BuildUp.DIRECT->rng.int(2,5);BuildUp.COUNTER->rng.int(2,5)
  }+when{controlGame(m,home)->2;conserve(m,home)->-1;else->0}).coerceAtLeast(1)
  repeat(passes){
   val id=rng.pick(team);val p=w.players.getValue(id);val perf=ensurePerformance(w,m,id);perf.passesAttempted++
   val fatigue=((p.fitness-45)/100.0).coerceIn(-.12,.45)
   val style=when(c.tactics.buildUp){BuildUp.TIKI_TAKA->.055;BuildUp.SHORT->.035;BuildUp.MIXED->0.0;BuildUp.WIDE->-.005;BuildUp.DIRECT->-.035;BuildUp.COUNTER->-.045}
   val safe=when{controlGame(m,home)->.065;conserve(m,home)->-.012;allOut(m,home)->-.022;else->0.0}
   val automatism=(c.dynamics.patterns["AUFBAU"]?:30)*.00045+c.dynamics.tacticalUnderstanding*.00035+c.dynamics.chemistry*.00020-(opponent.dynamics.pressingCoordination-50)*.00025
   val success=(.515+p.attributes.passing*.0032+p.attributes.vision*.00155+p.attributes.technique*.00085+fatigue+style+safe+automatism-(opponent.tactics.pressing-3)*.021).coerceIn(.40,.982)
   val completed=rng.chance(success);val target=team.filter{it!=id}.let{if(it.isEmpty())0 else rng.pick(it)};val sx=m.ballX;val sy=m.ballY;val ex=(sx+(rng.nextDouble()-.5)*.18).toFloat().coerceIn(.04f,.96f);val ey=(sy+(if(home)-1 else 1)*(.02+rng.nextDouble()*.10)).toFloat().coerceIn(.03f,.97f);m.passEvents.add(PassEvent(m.minute,c.id,id,target,completed,sx,sy,ex,ey));if(m.passEvents.size>500)m.passEvents.removeAt(0);if(completed)perf.passesCompleted++ else perf.turnovers++
  }
 }

 private fun moverFor(w: World,m: LiveMatch,home: Boolean,rng: SeededRandom): Int {
  val team=xi(m,home).filter{it!=0&&it !in m.injured}
  if(team.isEmpty())return 0
  val progress=if(home)1f-m.ballY else m.ballY
  val target=targetPlayerFor(w,m,home)
  val weighted=team.flatMap{id->val p=w.players.getValue(id);val base=when{
   progress<.24f->when(p.position){Position.TW->4;Position.IV->6;Position.LV,Position.RV,Position.DM->5;Position.ZM->3;else->1}
   progress<.63f->when(p.position){Position.DM,Position.ZM->6;Position.LV,Position.RV,Position.LA,Position.RA->4;Position.OM->5;Position.IV->3;Position.ST->2;Position.TW->1}
   else->when(p.position){Position.ST->7;Position.LA,Position.RA,Position.OM->6;Position.ZM->4;Position.LV,Position.RV,Position.DM->2;Position.IV->1;Position.TW->0}
  };val targetBonus=if(id==target)when{progress>=.63f->9;progress>=.35f->4;else->1}else 0;val primeBonus=if(p.messiMentored)when{progress>=.63f->16;progress>=.35f->10;else->5}else 0;List((base+targetBonus+primeBonus).coerceAtLeast(0)){id}}
  return if(weighted.isNotEmpty())rng.pick(weighted) else team.first()
 }

 private fun moveBallToward(w: World,m: LiveMatch,home: Boolean,phase: LivePhase,rng: SeededRandom){
  val c=w.clubs.getValue(clubId(m,home));val opp=w.clubs.getValue(clubId(m,!home));val build=c.tactics.buildUp
  val dir=if(home)-1f else 1f
  val ownKeeperY=if(home).91f else .09f
  val widthLevel=(c.tactics.width-1)/4f
  val pressureDanger=(.055+opp.tactics.pressing*.028+(if(phase==LivePhase.ATTACK).025 else 0.0)).coerceIn(.08,.23)

  // Unter Druck darf eine Mannschaft wirklich abbrechen und bis zum eigenen Keeper zurückspielen.
  if(phase in setOf(LivePhase.POSSESSION,LivePhase.ATTACK)&&rng.chance(recycleChance(build)+(if(rng.chance(pressureDanger)).08 else 0.0))){
   val keeperBack=phase==LivePhase.POSSESSION&&rng.chance((.28+opp.tactics.pressing*.055+(if(build==BuildUp.TIKI_TAKA||build==BuildUp.SHORT).12 else 0.0)).coerceAtMost(.72))
   if(keeperBack){
    m.ballY=(ownKeeperY+(rng.nextDouble().toFloat()-.5f)*.035f).coerceIn(.04f,.96f);m.ballX=(.5f+(rng.nextDouble().toFloat()-.5f)*.10f).coerceIn(.05f,.95f)
    m.livePlayerId=xi(m,home).firstOrNull{it!=0&&w.players[it]?.position==Position.TW}?:m.livePlayerId;m.liveDetail="Rückpass zum Torwart"
   }else{
    val backward=.08f+rng.nextDouble().toFloat()*.12f;m.ballY=(m.ballY-dir*backward).coerceIn(.05f,.95f)
    val lane=listOf(.18f,.32f,.50f,.68f,.82f)[rng.int(0,4)];m.ballX=(m.ballX+(lane-m.ballX)*.55f).coerceIn(.04f,.96f);m.liveDetail=if(build==BuildUp.TIKI_TAKA)"Neuaufbau über hinten" else "Angriff abgebrochen – Neuaufbau"
   }
   return
  }

  // Seitenwechsel nutzen bewusst fast die komplette Platzbreite.
  val switchChance=(.075+widthLevel*.11+when(build){BuildUp.WIDE->.14;BuildUp.TIKI_TAKA->.075;BuildUp.SHORT->.045;else->0.0}).coerceIn(.06,.34)
  if(phase!=LivePhase.DANGEROUS_ATTACK&&rng.chance(switchChance)){
   val opposite=if(m.ballX<.5f)(.88f+rng.nextDouble().toFloat()*.08f) else (.04f+rng.nextDouble().toFloat()*.08f)
   val forward=when(phase){LivePhase.COUNTER->.13f;LivePhase.ATTACK->.085f;else->.035f}
   m.ballY=(m.ballY+dir*forward).coerceIn(.04f,.96f);m.ballX=opposite.coerceIn(.035f,.965f);m.liveDetail=if(build==BuildUp.WIDE)"Schneller Seitenwechsel auf den Flügel" else "Diagonaler Seitenwechsel"
   return
  }

  // Direkte Systeme und Konter schlagen sichtbar lange Bälle/Steilpässe.
  val modeLongBall=when{conserve(m,home)->.10;allOut(m,home)->.08;controlGame(m,home)->-.045;else->0.0}
  val longChance=(longBallChance(build)+(if(phase==LivePhase.COUNTER).18 else if(phase==LivePhase.ATTACK).04 else 0.0)+modeLongBall).coerceIn(.01,.64)
  if(phase!=LivePhase.DANGEROUS_ATTACK&&rng.chance(longChance)){
   val step=when(phase){LivePhase.COUNTER->.24f+rng.nextDouble().toFloat()*.16f;LivePhase.ATTACK->.19f+rng.nextDouble().toFloat()*.15f;else->.16f+rng.nextDouble().toFloat()*.16f}
   m.ballY=(m.ballY+dir*step).coerceIn(.035f,.965f)
   val lanes=if(c.tactics.width>=4)listOf(.06f,.16f,.84f,.94f)else listOf(.22f,.38f,.62f,.78f)
   m.ballX=(lanes[rng.int(0,lanes.lastIndex)]+(rng.nextDouble().toFloat()-.5f)*.035f).coerceIn(.035f,.965f)
   m.liveDetail=when{phase==LivePhase.COUNTER->"Steilpass in den freien Raum";build==BuildUp.DIRECT->"Langer Ball hinter die Kette";else->"Langer Diagonalball"}
   return
  }

  val target=when(phase){
   LivePhase.POSSESSION->if(home).58f else .42f
   LivePhase.ATTACK->if(home).34f else .66f
   LivePhase.DANGEROUS_ATTACK->if(home).13f else .87f
   LivePhase.COUNTER->if(home).24f else .76f
   else->m.ballY
  }
  val maxStep=when(phase){LivePhase.COUNTER->(.17f+c.tactics.tempo*.014f);LivePhase.DANGEROUS_ATTACK->.15f;LivePhase.ATTACK->.125f;else->.09f}
  val styleStep=when(build){BuildUp.TIKI_TAKA->.70f;BuildUp.SHORT->.80f;BuildUp.DIRECT->1.18f;BuildUp.COUNTER->1.15f;else->1f}
  val modeStep=when{conserve(m,home)&&phase!=LivePhase.COUNTER->.70f;allOut(m,home)->1.12f;controlGame(m,home)->.80f;else->1f}
  val delta=(target-m.ballY).coerceIn(-maxStep*modeStep*styleStep,maxStep*modeStep*styleStep)
  m.ballY=(m.ballY+delta+(rng.nextDouble().toFloat()-.5f)*.020f).coerceIn(.035f,.965f)

  val lanePool=when{
   build==BuildUp.WIDE||c.tactics.width==5->listOf(.045f,.10f,.20f,.80f,.90f,.955f)
   c.tactics.width>=4->listOf(.08f,.18f,.32f,.68f,.82f,.92f)
   c.tactics.width<=2->listOf(.30f,.40f,.50f,.60f,.70f)
   else->listOf(.16f,.29f,.42f,.58f,.71f,.84f)
  }
  val lane=lanePool[rng.int(0,lanePool.lastIndex)]
  val lateral=.18f+widthLevel*.30f+(if(build==BuildUp.TIKI_TAKA).08f else 0f)
  m.ballX=(m.ballX+(lane-m.ballX)*lateral+(rng.nextDouble().toFloat()-.5f)*.018f).coerceIn(.035f,.965f)
  m.liveDetail=when{
   build==BuildUp.TIKI_TAKA&&phase==LivePhase.POSSESSION->"Tiki-Taka – kurze Dreiecke"
   build==BuildUp.SHORT&&phase==LivePhase.POSSESSION->"Kurzpassspiel"
   build==BuildUp.WIDE&&m.ballX !in .22f.. .78f->"Über den Flügel"
   phase==LivePhase.COUNTER->"Schneller Gegenstoß"
   phase==LivePhase.DANGEROUS_ATTACK->"Kombination am Strafraum"
   phase==LivePhase.ATTACK->"Angriff wird ausgespielt"
   else->"Ball zirkuliert"
  }
 }

 private fun counterProbability(w: World,m: LiveMatch,newHome: Boolean): Double {
  val c=w.clubs.getValue(clubId(m,newHome));val opp=w.clubs.getValue(clubId(m,!newHome));val mentality=if(newHome)m.homeMentality else m.awayMentality
  val fitness=((averageFitness(w,m,newHome)-55)/100.0).coerceIn(0.0,.35)
  val zone=kotlin.math.abs(m.ballY-.5f).toDouble()*.16
  val lowBlockBoost=if(conserve(m,newHome)).18 else 0.0
  val pressingBoost=if(allOut(m,newHome)).055 else 0.0
  val exposedOpponent=if(allOut(m,!newHome)).17 else 0.0
  val controlPenalty=if(controlGame(m,newHome)).055 else 0.0
  val p=.10+(mentality-3)*.035+(c.tactics.tempo-3)*.028+(opp.tactics.line-3)*.035+fitness+zone+lowBlockBoost+pressingBoost+exposedOpponent-controlPenalty
  return p.coerceIn(.04,.78)
 }

 private fun transferPossession(w: World,m: LiveMatch,newHome: Boolean,reason: PossessionChangeReason,rng: SeededRandom,allowCounter: Boolean=true){
  require(reason!=PossessionChangeReason.NONE)
  val oldHome=m.chainOwnerClubId==m.homeId
  val oldPlayers=xi(m,oldHome).filter{it!=0&&it !in m.injured};if(oldPlayers.isNotEmpty())ensurePerformance(w,m,rng.pick(oldPlayers)).turnovers++
  val newPlayers=xi(m,newHome).filter{it!=0&&it !in m.injured};if(newPlayers.isNotEmpty())ensurePerformance(w,m,rng.pick(newPlayers)).defensiveActions++
  m.chainStep=0;m.chainTicks=0
  val counter=allowCounter&&rng.chance(counterProbability(w,m,newHome))
  m.counterTicksRemaining=if(counter)2 else 0
  val phase=if(counter)LivePhase.COUNTER else LivePhase.POSSESSION
  setBallPhase(m,phase,newHome,detail=if(counter)"Schneller Ballgewinn" else reason.label,reason=reason)
  moveBallToward(w,m,newHome,phase,rng)
  log(m,"${w.clubs.getValue(clubId(m,newHome)).shortName}: ${if(counter)"Ballgewinn – sofortiger Konter." else reason.label+" und neuer Ballbesitz."}")
 }

 private fun queueRestart(m: LiveMatch,clubId: Int,reason: PossessionChangeReason){m.pendingPossessionClubId=clubId;m.pendingPossessionReason=reason}

 private fun maybeThrowIn(w: World,m: LiveMatch,ownerHome: Boolean,rng: SeededRandom): Boolean {
  if(m.minute<=0||m.lastThrowInMinute==m.minute)return false
  if(m.livePhase !in setOf(LivePhase.POSSESSION,LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK,LivePhase.COUNTER))return false
  val wide=m.ballX !in .16f.. .84f
  val phaseFactor=when(m.livePhase){LivePhase.POSSESSION->.11;LivePhase.ATTACK->.15;LivePhase.DANGEROUS_ATTACK->.09;LivePhase.COUNTER->.12;else->0.0}
  val chance=(phaseFactor+(if(wide).09 else 0.0)).coerceIn(.08,.24)
  if(!rng.chance(chance))return false
  val receivingHome=if(rng.chance(if(wide).63 else .56))ownerHome else !ownerHome
  val id=clubId(m,receivingHome);stats(m,receivingHome).throwIns++;m.lastThrowInMinute=m.minute;addStoppageTime(m,3)
  m.ballX=if(m.ballX<.5f).035f else .965f
  queueRestart(m,id,PossessionChangeReason.THROW_IN)
  setIncidentPhase(m,LivePhase.THROW_IN,id,0,"Einwurf")
  log(m,"Einwurf für ${w.clubs.getValue(id).shortName}.")
  return true
 }

 private fun maybeOffside(w: World,m: LiveMatch,home: Boolean,runnerId: Int,type: ShotType,rng: SeededRandom): Boolean {
  if(type in setOf(ShotType.PENALTY,ShotType.FREE_KICK,ShotType.REBOUND)||runnerId==0)return false
  val attack=w.clubs.getValue(clubId(m,home));val defending=w.clubs.getValue(clubId(m,!home))
  val vertical=when(type){ShotType.ONE_ON_ONE->.034;ShotType.CUTBACK->.006;ShotType.HEADER,ShotType.VOLLEY->.010;ShotType.LONG_RANGE->-.010;else->.016}
  val direct=when(attack.tactics.buildUp){BuildUp.DIRECT->.018;BuildUp.COUNTER->.022;BuildUp.WIDE->.008;else->0.0}
  val line=(defending.tactics.line-3)*.010
  val tempo=(attack.tactics.tempo-3)*.004
  val chance=(.020+vertical+direct+line+tempo).coerceIn(.006,.105)
  if(!rng.chance(chance))return false
  stats(m,home).offsides++
  val p=w.players[runnerId]
  setIncidentPhase(m,LivePhase.OFFSIDE,clubId(m,home),runnerId,"Abseits")
  queueRestart(m,clubId(m,!home),PossessionChangeReason.OFFSIDE)
  log(m,"Abseits${p?.let{" – ${it.lastName} war einen Schritt zu früh gestartet."}?:"."}")
  return true
 }

 private fun applyRestart(w: World,m: LiveMatch,rng: SeededRandom): Boolean {
  val id=m.pendingPossessionClubId;if(id==0)return false
  val reason=m.pendingPossessionReason.takeIf{it!=PossessionChangeReason.NONE}?:PossessionChangeReason.INTERCEPTION
  m.pendingPossessionClubId=0;m.pendingPossessionReason=PossessionChangeReason.NONE
  val home=id==m.homeId
  when(reason){
   PossessionChangeReason.KICK_OFF->{m.ballY=.5f;m.ballX=.5f}
   PossessionChangeReason.GOAL_KICK,PossessionChangeReason.SAVE->{m.ballY=if(home).84f else .16f;m.ballX=.5f}
   PossessionChangeReason.THROW_IN->{m.ballX=if(m.ballX<.5f).045f else .955f;m.ballY=m.ballY.coerceIn(.10f,.90f)}
   PossessionChangeReason.OFFSIDE->{m.ballY=(m.ballY+(if(home).08f else -.08f)).coerceIn(.12f,.88f);m.ballX=m.ballX.coerceIn(.16f,.84f)}
   else->{m.ballY=(m.ballY*.72f+.5f*.28f).coerceIn(.09f,.91f)}
  }
  val preserveThrow=reason==PossessionChangeReason.THROW_IN&&id==m.chainOwnerClubId
  if(preserveThrow){
   val progress=if(home)1f-m.ballY else m.ballY
   val phase=when{progress>=.78f->LivePhase.DANGEROUS_ATTACK;progress>=.50f->LivePhase.ATTACK;else->LivePhase.POSSESSION}
   m.chainStep=m.chainStep.coerceAtLeast(if(phase==LivePhase.DANGEROUS_ATTACK)2 else 1);m.chainTicks=(m.chainTicks+1).coerceAtMost(5);m.counterTicksRemaining=0
   setBallPhase(m,phase,home,detail="Einwurf schnell ausgeführt",reason=reason)
  }else{
   m.chainStep=0;m.chainTicks=0;m.counterTicksRemaining=0
   setBallPhase(m,LivePhase.POSSESSION,home,detail=reason.label,reason=reason)
  }
  log(m,"${w.clubs.getValue(id).shortName} setzt mit ${reason.label.lowercase()} fort.")
  m.rngState=rng.state
  return true
 }

 private fun queueCorner(w: World,m: LiveMatch,home: Boolean,takerId: Int,rng: SeededRandom){
  val id=clubId(m,home)
  require(m.chainOwnerClubId==id){"Eine Ecke darf nur für das angreifende Team entstehen."}
  m.ballX=if(rng.chance(.5)).06f else .94f;m.ballY=if(home).07f else .93f
  val assigned=cornerTaker(w,m,home,m.ballX)
  stats(m,home).corners++;m.pendingCornerClubId=id;m.pendingCornerPlayerId=assigned.takeIf{it!=0}?:takerId;m.cornerAwaitingResolution=false
 }

 private fun setPiecePhase(type: SetPieceType)=when(type){
  SetPieceType.FREE_KICK->LivePhase.FREE_KICK
  SetPieceType.DANGEROUS_FREE_KICK->LivePhase.DANGEROUS_FREE_KICK
  SetPieceType.PENALTY->LivePhase.PENALTY
  else->LivePhase.POSSESSION
 }

 private fun setPieceTaker(w: World,m: LiveMatch,home: Boolean,type: SetPieceType): Int {
  val c=w.clubs.getValue(clubId(m,home))
  val configured=when(type){SetPieceType.PENALTY->c.tactics.penaltyTakerId;SetPieceType.FREE_KICK,SetPieceType.DANGEROUS_FREE_KICK->c.tactics.freeKickTakerId;else->0}
  val chosen=configuredOnPitch(w,m,home,configured,false);if(chosen!=0)return chosen
  val candidates=xi(m,home).filter{it!=0&&it !in m.injured&&w.players[it]?.position!=Position.TW}.ifEmpty{xi(m,home).filter{it!=0&&it !in m.injured}}
  return candidates.maxByOrNull{id->val p=w.players.getValue(id);p.attributes.setPieces*(if(type==SetPieceType.PENALTY).64 else .82)+p.attributes.finishing*(if(type==SetPieceType.PENALTY).36 else .18)}?:0
 }

 private fun cornerTaker(w: World,m: LiveMatch,home: Boolean,x: Float): Int {
  val c=w.clubs.getValue(clubId(m,home));val configured=if(x<.5f)c.tactics.cornerLeftTakerId else c.tactics.cornerRightTakerId
  val chosen=configuredOnPitch(w,m,home,configured,false);if(chosen!=0)return chosen
  val candidates=xi(m,home).filter{it!=0&&it !in m.injured&&w.players[it]?.position!=Position.TW}
  return candidates.maxByOrNull{w.players.getValue(it).attributes.setPieces}?:xi(m,home).firstOrNull{it!=0&&it !in m.injured}?:0
 }

 private fun crossTarget(w: World,m: LiveMatch,home: Boolean,taker: Int,rng: SeededRandom): Int {
  val target=targetPlayerFor(w,m,home)
  val candidates=xi(m,home).filter{it!=0&&it!=taker&&it !in m.injured&&w.players[it]?.position!=Position.TW}
  if(candidates.isEmpty())return 0
  val weighted=candidates.flatMap{id->val p=w.players.getValue(id);val role=when(p.position){Position.ST->5;Position.OM,Position.LA,Position.RA->3;Position.ZM->2;else->1};val aerial=(p.attributes.heading/18).coerceIn(0,5);val bonus=if(id==target)8 else 0;List((role+aerial+bonus).coerceAtLeast(1)){id}}
  return rng.pick(weighted)
 }

 private fun queueSetPiece(w: World,m: LiveMatch,attackingHome: Boolean,foulerId: Int,rng: SeededRandom): SetPieceType {
  val progress=if(attackingHome)1f-m.ballY else m.ballY
  val central=m.ballX in .225f.. .775f
  val type=when{
   progress>=.925f&&central->SetPieceType.PENALTY
   progress>=.72f&&(m.ballX in .12f.. .88f)->SetPieceType.DANGEROUS_FREE_KICK
   progress>=.80f->SetPieceType.DANGEROUS_FREE_KICK
   else->SetPieceType.FREE_KICK
  }
  m.pendingSetPieceClubId=clubId(m,attackingHome);m.pendingSetPieceType=type;m.pendingSetPiecePlayerId=setPieceTaker(w,m,attackingHome,type)
  m.pendingSetPieceX=m.ballX;m.pendingSetPieceY=m.ballY;m.setPieceAwaitingResolution=false
  val fouler=w.players[foulerId]
  log(m,"Foul${fouler?.let{" von ${it.name}"}?:""}. ${type.label} für ${w.clubs.getValue(clubId(m,attackingHome)).shortName}.",if(type==SetPieceType.PENALTY)"bad" else "normal")
  return type
 }

 private fun showQueuedSetPiece(w: World,m: LiveMatch,rng: SeededRandom): Boolean {
  val id=m.pendingSetPieceClubId;if(id==0||m.pendingSetPieceType==SetPieceType.NONE)return false
  val home=id==m.homeId;val taker=m.pendingSetPiecePlayerId.takeIf{it in xi(m,home)}?:setPieceTaker(w,m,home,m.pendingSetPieceType)
  m.pendingSetPiecePlayerId=taker;m.ballX=m.pendingSetPieceX;m.ballY=m.pendingSetPieceY
  val reason=if(m.chainOwnerClubId==id)PossessionChangeReason.NONE else PossessionChangeReason.FOUL
  setBallPhase(m,setPiecePhase(m.pendingSetPieceType),home,taker,m.pendingSetPieceType.label,reason)
  m.setPieceAwaitingResolution=true
  log(m,"${m.pendingSetPieceType.label}: ${w.players[taker]?.name?:w.clubs.getValue(id).shortName} legt sich den Ball zurecht.")
  m.rngState=rng.state;return true
 }

 private fun handleSetPiece(w: World,m: LiveMatch,rng: SeededRandom): Boolean {
  if(m.pendingSetPieceClubId==0||m.pendingSetPieceType==SetPieceType.NONE)return false
  if(!m.setPieceAwaitingResolution)return showQueuedSetPiece(w,m,rng)
  val id=m.pendingSetPieceClubId;val home=id==m.homeId;val type=m.pendingSetPieceType;val taker=m.pendingSetPiecePlayerId.takeIf{it in xi(m,home)}?:setPieceTaker(w,m,home,type)
  val foulX=m.pendingSetPieceX;val foulY=m.pendingSetPieceY
  m.pendingSetPieceClubId=0;m.pendingSetPieceType=SetPieceType.NONE;m.pendingSetPiecePlayerId=0;m.setPieceAwaitingResolution=false
  m.ballX=foulX;m.ballY=foulY
  val p=w.players[taker]
  when(type){
   SetPieceType.PENALTY->{
    setDistanceFromGoal(m,home,11.0,.5f)
    log(m,"Elfmeter für ${w.clubs.getValue(id).shortName}. ${p?.name?:"Der Schütze"} läuft an.","decision")
    resolveShot(w,m,home,taker,rng,typeHint=ShotType.PENALTY)
   }
   SetPieceType.DANGEROUS_FREE_KICK->{
    val directChance=(.44+(p?.attributes?.setPieces?:40)*.003).coerceIn(.50,.72)
    if(rng.chance(directChance)){
     log(m,"Gefährlicher Freistoß – ${p?.name?:"der Schütze"} versucht es direkt.")
     resolveShot(w,m,home,taker,rng,typeHint=ShotType.FREE_KICK)
    }else{
     m.chainStep=2;m.chainTicks=0;setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,taker,"Freistoßflanke in den Strafraum");moveBallToward(w,m,home,LivePhase.DANGEROUS_ATTACK,rng);ensurePerformance(w,m,taker).chancesCreated++
     log(m,"${p?.name?:"Der Schütze"} bringt den gefährlichen Freistoß in den Strafraum.")
    }
   }
   SetPieceType.FREE_KICK->{
    val progress=if(home)1f-foulY else foulY
    if(progress>=.60f&&rng.chance((.14+(p?.attributes?.setPieces?:40)*.0015).coerceAtMost(.30))){
     resolveShot(w,m,home,taker,rng,typeHint=ShotType.FREE_KICK)
    }else{
     val next=if(progress>=.55f)LivePhase.ATTACK else LivePhase.POSSESSION;m.chainStep=if(next==LivePhase.ATTACK)1 else 0;m.chainTicks=0
     setBallPhase(m,next,home,taker,if(next==LivePhase.ATTACK)"Freistoß schnell nach vorn" else "Freistoß kurz ausgeführt");moveBallToward(w,m,home,next,rng)
     log(m,"${w.clubs.getValue(id).shortName} führt den Freistoß ${if(next==LivePhase.ATTACK)"zügig nach vorn" else "kurz"} aus.")
    }
   }
   else->{}
  }
  m.rngState=rng.state;return true
 }

 private fun selectFouler(w: World,m: LiveMatch,defenderHome: Boolean,rng: SeededRandom): Int {
  val defenders=xi(m,defenderHome).filter{it!=0&&it !in m.injured};if(defenders.isEmpty())return 0
  val weighted=defenders.flatMap{id->val p=w.players.getValue(id);val posWeight=when(p.position){Position.IV,Position.LV,Position.RV,Position.DM->5;Position.ZM->4;Position.OM,Position.LA,Position.RA->3;Position.ST->2;Position.TW->1};val mistimed=((62-p.attributes.tackling)/12).coerceIn(-1,3);List((posWeight+mistimed).coerceAtLeast(1)){id}}
  return rng.pick(weighted)
 }

 private fun handleCorner(w: World,m: LiveMatch,rng: SeededRandom): Boolean {
  val id=m.pendingCornerClubId;if(id==0)return false
  val home=id==m.homeId;require(m.chainOwnerClubId==id){"Ecke ohne Ballbesitz des angreifenden Teams."}
  val team=xi(m,home).filter{it!=0&&it !in m.injured}
  val taker=m.pendingCornerPlayerId.takeIf{it in team}?:team.filter{w.players[it]?.position!=Position.TW}.maxByOrNull{w.players.getValue(it).attributes.setPieces}?:team.firstOrNull()?:0
  if(!m.cornerAwaitingResolution){
   m.cornerAwaitingResolution=true;setBallPhase(m,LivePhase.CORNER,home,taker,"Ecke");log(m,"Ecke für ${w.clubs.getValue(id).shortName}.");m.rngState=rng.state;return true
  }
  m.pendingCornerClubId=0;m.pendingCornerPlayerId=0;m.cornerAwaitingResolution=false
  val r=rng.nextDouble()
  when{
   r<.34->{
    val target=crossTarget(w,m,home,taker,rng)
    if(target!=0){
     val d=6.5+rng.nextDouble()*8.5;setDistanceFromGoal(m,home,d,(.32+rng.nextDouble()*.36).toFloat())
     resolveShot(w,m,home,target,rng,taker,if(rng.chance(.78))ShotType.HEADER else ShotType.VOLLEY)
    }else transferPossession(w,m,!home,PossessionChangeReason.CLEARANCE,rng)
   }
   r<.60->{setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,taker,"Zweiter Ball");m.chainStep=2;m.chainTicks=0;moveBallToward(w,m,home,LivePhase.DANGEROUS_ATTACK,rng);log(m,"${w.clubs.getValue(id).shortName} bleibt nach der Ecke am Ball.")}
   else->transferPossession(w,m,!home,PossessionChangeReason.CLEARANCE,rng,true)
  }
  m.rngState=rng.state;return true
 }

 private fun confirmGoal(w: World,m: LiveMatch,home: Boolean,id: Int,assist: Int,keeperId: Int,type: ShotType,showGoalPhase: Boolean=true){
  val s=stats(m,home);val p=w.players.getValue(id);s.goals++;s.shotsOnTarget++
  val perf=ensurePerformance(w,m,id);perf.goals++;perf.shotsOnTarget++
  val validAssist=assist.takeIf{it!=0&&it!=id&&w.players[it]?.clubId==clubId(m,home)}?:0
  if(validAssist!=0){val aPerf=ensurePerformance(w,m,validAssist);aPerf.assists++;aPerf.chancesCreated++}
  m.goals.add(Goal(id,validAssist,m.minute,clockLabel(m),m.lastShotTargetLabel));if(keeperId!=0)ensurePerformance(w,m,keeperId).goalsConceded++
  if(showGoalPhase)setBallPhase(m,LivePhase.GOAL,home,id,"TOR · ${m.lastShotTargetLabel} · ${type.label}")
  val assistText=if(validAssist!=0)" Vorlage: ${w.players.getValue(validAssist).name}." else ""
  log(m,"Tor! ${p.name} trifft ${m.lastShotTargetLabel}.$assistText ${m.home.goals}:${m.away.goals}.","goal")
  queueRestart(m,clubId(m,!home),PossessionChangeReason.KICK_OFF)
 }

 private fun handleVarReview(w: World,m: LiveMatch,rng: SeededRandom): Boolean {
  val index=m.pendingVarShotIndex;if(index !in m.shotEvents.indices)return false
  val shot=m.shotEvents[index];val home=shot.clubId==m.homeId;val defender=w.clubs.getValue(clubId(m,!home))
  if(m.varReviewStage==0){
   val offsideBias=when(shot.type){ShotType.ONE_ON_ONE->.07;ShotType.CUTBACK->.025;ShotType.HEADER,ShotType.VOLLEY->.03;else->.04}
   val overturnChance=(.095+offsideBias+(defender.tactics.line-3)*.012).coerceIn(.07,.24)
   val reasonRoll=rng.nextDouble()
   m.varReviewReason=when{reasonRoll<.68->"Mögliche Abseitsstellung in der Entstehung";reasonRoll<.86->"Mögliches Angreiferfoul vor dem Treffer";else->"Mögliches Handspiel in der Entstehung"}
   m.varWillOverturn=rng.chance(overturnChance);m.varReviewResult="";m.varReviewStage=1;addStoppageTime(m,55)
   setIncidentPhase(m,LivePhase.VAR,shot.clubId,shot.playerId,"VAR prüft: ${m.varReviewReason}")
   log(m,"VAR-Prüfung: ${m.varReviewReason}.","decision")
   m.rngState=rng.state;return true
  }

  val keeperId=m.pendingVarKeeperId;val reason=m.varReviewReason
  if(m.varWillOverturn){
   m.shotEvents.removeAt(index);m.lastShotOutcome=ShotOutcome.DISALLOWED
   val s=stats(m,home);s.shots=(s.shots-1).coerceAtLeast(0);s.xg=(s.xg-shot.xg).coerceAtLeast(0.0)
   val perf=ensurePerformance(w,m,shot.playerId);perf.shots=(perf.shots-1).coerceAtLeast(0);perf.xg=(perf.xg-shot.xg).coerceAtLeast(0.0)
   val offside=reason.contains("Abseits")
   if(offside){s.offsides++;queueRestart(m,clubId(m,!home),PossessionChangeReason.OFFSIDE)}else queueRestart(m,clubId(m,!home),PossessionChangeReason.FOUL)
   m.varReviewResult=when{offside->"Kein Tor – Abseits";reason.contains("Angreiferfoul")->"Kein Tor – Angreiferfoul";else->"Kein Tor – Handspiel"}
   setIncidentPhase(m,LivePhase.VAR,shot.clubId,shot.playerId,"VAR · ${m.varReviewResult}")
   log(m,"VAR: ${m.varReviewResult}.","bad")
  }else{
   confirmGoal(w,m,home,shot.playerId,shot.assistId,keeperId,shot.type,showGoalPhase=false)
   m.varReviewResult=when{reason.contains("Abseits")->"Tor zählt – kein Abseits";reason.contains("Angreiferfoul")->"Tor zählt – kein Angreiferfoul";else->"Tor zählt – kein strafbares Handspiel"}
   setIncidentPhase(m,LivePhase.VAR,shot.clubId,shot.playerId,"VAR · ${m.varReviewResult}")
   log(m,"VAR: ${m.varReviewResult}.","goal")
  }
  m.pendingVarShotIndex=-1;m.pendingVarKeeperId=0;m.varReviewStage=0
  m.rngState=rng.state;return true
 }

 private fun endCurrentPeriod(w: World,m: LiveMatch,rng: SeededRandom){
  when(m.period){
   1->{m.halfTime=true;m.breakType=MatchBreakType.HALF_TIME;log(m,"Halbzeit nach ${m.firstHalfAdded} Minute${if(m.firstHalfAdded==1)"" else "n"} Nachspielzeit.")}
   2->{
    if(m.knockout&&CompetitionEngine.isTiedForAdvancement(w,m)){m.halfTime=true;m.breakType=MatchBreakType.EXTRA_TIME_START;log(m,"90 Minuten plus Nachspielzeit sind vorbei. Es geht in die Verlängerung.","decision")}
    else finish(w,m)
   }
   3->{m.halfTime=true;m.breakType=MatchBreakType.EXTRA_TIME_HALF;log(m,"Halbzeit der Verlängerung.")}
   4->{
    if(m.knockout&&CompetitionEngine.isTiedForAdvancement(w,m)){m.halfTime=true;m.breakType=MatchBreakType.SHOOTOUT_START;log(m,"Auch nach 120 Minuten steht es unentschieden. Elfmeterschießen!","decision")}
    else finish(w,m)
   }
  }
  m.rngState=rng.state
 }

 fun secondHalf(m: LiveMatch){
  require(m.halfTime){"Es ist keine Spielpause aktiv."}
  val type=m.breakType;m.halfTime=false;m.breakType=MatchBreakType.NONE
  when(type){
   MatchBreakType.HALF_TIME->{m.period=2;m.secondHalf=true;resetPeriodClock(m,45);kickoff(m,!m.kickoffHomeFirst,"Anstoß zur 2. Halbzeit");log(m,"Die zweite Halbzeit läuft. Der Ball liegt wieder am Mittelpunkt.")}
   MatchBreakType.EXTRA_TIME_START->{m.period=3;m.extraTimePlayed=true;resetPeriodClock(m,90);kickoff(m,m.extraKickoffHome,"Anstoß zur Verlängerung");log(m,"Die Verlängerung läuft: erste 15 Minuten.")}
   MatchBreakType.EXTRA_TIME_HALF->{m.period=4;resetPeriodClock(m,105);kickoff(m,!m.extraKickoffHome,"Anstoß zur 2. Halbzeit der Verlängerung");log(m,"Die zweite Hälfte der Verlängerung läuft.")}
   MatchBreakType.SHOOTOUT_START->{m.period=5;m.shootoutActive=true;m.shootoutHomeTurn=true;m.livePhase=LivePhase.SHOOTOUT;m.livePlayerId=0;m.liveDetail="Elfmeterschießen";m.ballX=.5f;m.ballY=.88f;log(m,"Das Elfmeterschießen beginnt.","decision")}
   else->error("Unbekannte Spielpause.")
  }
 }

 private fun shootoutTaker(w: World,m: LiveMatch,home: Boolean,taken: Int): Int {
  val candidates=xi(m,home).filter{it!=0&&it !in m.injured}.sortedByDescending{id->val p=w.players.getValue(id);p.attributes.setPieces*.40+p.attributes.finishing*.32+p.hidden.pressure*.18+p.attributes.technique*.10}
  return candidates.getOrNull(taken % candidates.size.coerceAtLeast(1))?:0
 }
 private fun shootoutKeeper(w: World,m: LiveMatch,home: Boolean)=xi(m,home).firstOrNull{it!=0&&it !in m.injured&&w.players[it]?.position==Position.TW}?:xi(m,home).firstOrNull{it!=0}?:0
 private fun shootoutShouldEnd(m: LiveMatch): Boolean {
  val hr=(5-m.shootoutHomeTaken).coerceAtLeast(0);val ar=(5-m.shootoutAwayTaken).coerceAtLeast(0)
  if(m.homePens>m.awayPens+ar||m.awayPens>m.homePens+hr)return true
  return m.shootoutHomeTaken>=5&&m.shootoutAwayTaken>=5&&m.shootoutHomeTaken==m.shootoutAwayTaken&&m.homePens!=m.awayPens
 }
 private fun resolveShootoutKick(w: World,m: LiveMatch,rng: SeededRandom){
  val home=m.shootoutHomeTurn;val taken=if(home)m.shootoutHomeTaken else m.shootoutAwayTaken;val takerId=shootoutTaker(w,m,home,taken)
  if(takerId==0){finish(w,m);return}
  val keeperId=shootoutKeeper(w,m,!home);val taker=w.players.getValue(takerId);val keeper=w.players[keeperId]
  val quality=taker.attributes.finishing*.38+taker.attributes.setPieces*.32+taker.hidden.pressure*.18+taker.attributes.technique*.12
  val keeperQ=keeper?.let{it.attributes.keeping*.72+it.hidden.pressure*.12+it.hidden.consistency*.16}?:30.0
  val scoreChance=(.735+(quality-65)*.0032-(keeperQ-65)*.0022).coerceIn(.52,.88);val scored=rng.chance(scoreChance)
  val saved=!scored&&rng.chance((.58+(keeperQ-quality)*.004).coerceIn(.38,.76));val outcome=if(scored)ShotOutcome.GOAL else if(saved)ShotOutcome.SAVED else ShotOutcome.OFF_TARGET
  val target=shotTarget(outcome,rng,penalty=true);m.lastShotTargetX=target.first;m.lastShotTargetY=target.second;m.lastShotTargetLabel=target.third;m.lastShotOutcome=outcome
  m.lastShotType=ShotType.PENALTY;m.lastShotX=.5f;m.lastShotY=if(home).86f else .14f;m.livePhase=LivePhase.SHOOTOUT;m.liveClubId=clubId(m,home);m.livePlayerId=takerId;m.liveDetail="Elfmeterschießen · ${target.third} · ${if(scored)"TOR" else if(saved)"GEHALTEN" else "VERSCHOSSEN"}";m.liveEventSerial++
  if(home)m.shootoutHomeTaken++ else m.shootoutAwayTaken++;if(scored){if(home)m.homePens++ else m.awayPens++}
  m.penaltyShootout.add(PenaltyKick(clubId(m,home),takerId,keeperId,scored,outcome,target.first,target.second,target.third,"i.E."))
  val text=when{scored->"${taker.name} trifft im Elfmeterschießen ${target.third}.";saved->"${taker.name} schießt ${target.third} – ${keeper?.name?:"der Torwart"} hält!";else->"${taker.name} schießt ${target.third} und vergibt."}
  log(m,"$text Stand i.E.: ${m.homePens}:${m.awayPens}.",if(scored)"goal" else "bad")
  m.shootoutHomeTurn=!home
  if(shootoutShouldEnd(m)){m.shootoutActive=false;finish(w,m)}
 }

 fun step(w: World,m: LiveMatch){
  if(m.finished||m.pendingDecision||m.halfTime||m.incidentPause||m.assistantSubPending)return
  val rng=SeededRandom(m.rngState)
  if(m.shootoutActive){resolveShootoutKick(w,m,rng);m.rngState=rng.state;return}
  if(handleVarReview(w,m,rng))return
  if(applyRestart(w,m,rng))return
  if(handleSetPiece(w,m,rng))return
  if(handleCorner(w,m,rng))return
  if(periodComplete(m)){endCurrentPeriod(w,m,rng);return}

  val ownerHome=m.chainOwnerClubId==m.homeId
  // Einwürfe sind kurze Unterbrechungen innerhalb einer laufenden Spielminute. Sie werden
  // sichtbar simuliert, verbrauchen aber keine komplette Match-Minute und keine Fitnessminute.
  if(maybeThrowIn(w,m,ownerHome,rng)){m.rngState=rng.state;return}

  advanceClock(m);announceStoppage(m);applyMinuteFitness(w,m)
  val pitch=w.clubs.getValue(m.homeId).stadium
  recordPossessionTime(w,m,ownerHome,rng);recordPossession(w,m,ownerHome,rng);updateMomentum(w,m)

  // Fouls hängen von Pressing, Spielsituation, Zweikampfstärke und Derby-Härte ab. Der Ort des
  // Fouls entscheidet anschließend zwischen Freistoß, gefährlichem Freistoß und Elfmeter.
  val defenderHome=!ownerHome;val defendingClub=w.clubs.getValue(clubId(m,defenderHome))
  val phaseRisk=when(m.livePhase){LivePhase.COUNTER->.032;LivePhase.DANGEROUS_ATTACK->.042;LivePhase.ATTACK->.022;else->0.0}
  val defendingAi=if(defenderHome)m.homeAi else m.awayAi;val tacticalBias=if(m.livePhase in listOf(LivePhase.COUNTER,LivePhase.DANGEROUS_ATTACK))defendingAi.tacticalFoulBias*.0015 else 0.0
  val foulChance=(.082+(defendingClub.tactics.pressing-1)*.015+phaseRisk+tacticalBias+(100-defendingClub.dynamics.mentalHardness)*.00025+(if(w.isDerby(m.homeId,m.awayId)).024 else 0.0)).coerceIn(.065,.245)
  if(rng.chance(foulChance)){
   val id=selectFouler(w,m,defenderHome,rng)
   if(id!=0){
    val fouler=w.players.getValue(id);stats(m,defenderHome).fouls++;addStoppageTime(m,7)
    val setPiece=queueSetPiece(w,m,ownerHome,id,rng)
    val poorTackle=((58-fouler.attributes.tackling).coerceAtLeast(0))*.0022
    val tactical=if(m.livePhase==LivePhase.COUNTER).075 else if(m.livePhase==LivePhase.DANGEROUS_ATTACK).045 else 0.0
    val yellowChance=(.16+(defendingClub.tactics.pressing-3)*.035+poorTackle+tactical+(if(w.isDerby(m.homeId,m.awayId)).055 else 0.0)+(if(setPiece==SetPieceType.PENALTY).055 else 0.0)).coerceIn(.12,.55)
    val redChance=(.004+(if(m.livePhase==LivePhase.COUNTER).009 else 0.0)+(if(setPiece==SetPieceType.PENALTY).010 else 0.0)+(if(fouler.attributes.tackling<35).006 else 0.0)).coerceAtMost(.035)
    if(rng.chance(redChance))card(w,m,id,true)
    else if(rng.chance(yellowChance))card(w,m,id,false)
    else showQueuedSetPiece(w,m,rng)
    m.rngState=rng.state;return
   }
  }

  if(rng.chance(.00135*(1+(100-pitch.pitchQuality)*.006))){
   val candidates=(m.homeXi+m.awayXi).filter{it!=0&&it !in m.injured}
   if(candidates.isNotEmpty()){
    val id=rng.pick(candidates);val p=w.players.getValue(id);val medicine=w.clubs.getValue(p.clubId).stadium.medicine
    if(rng.chance((.38+p.hidden.injuryProneness*.009)*(1-medicine*.006))){
     p.injuryWeeks=(rng.int(1,4)*(1-medicine*.005)).roundToInt().coerceAtLeast(1)+1;p.injury="Zerrung im Spiel";m.injured.add(id);addStoppageTime(m,45)
     setIncidentPhase(m,LivePhase.INJURY,p.clubId,id,"Verletzung");log(m,"${p.name} bleibt liegen. Er kann nicht weiterspielen.","bad")
     if(p.clubId==w.user.clubId&&!w.assistantCoach.autoSubstitutions)pauseForIncident(m,MatchPauseReason.INJURY,id,p.clubId) else aiSub(w,m,p.clubId==m.homeId)
     m.rngState=rng.state;return
    }
   }
  }


  if(m.minute>=45&&m.minute%6==0)for(home in listOf(true,false)){
   val note=MatchIntelligence.adapt(w,m,home,rng);if(note!=null)log(m,note,"normal")
   if(clubId(m,home)!=w.user.clubId||w.assistantCoach.autoSubstitutions)aiSub(w,m,home)
   if(m.assistantSubPending){m.rngState=rng.state;return}
  }

  val home=ownerHome;val c=w.clubs.getValue(clubId(m,home));val opp=w.clubs.getValue(clubId(m,!home));val activeAi=if(home)m.homeAi else m.awayAi
  if(activeAi.timeWaste>0&&m.minute>=78&&rng.chance(activeAi.timeWaste*.012)){addStoppageTime(m,18);m.chainTicks=(m.chainTicks+1).coerceAtMost(4);if(rng.chance(.35)){log(m,"${c.shortName} nimmt bewusst Tempo aus der Partie.");m.rngState=rng.state;return}}
  val fatigue=(averageFitness(w,m,home)/100.0).coerceIn(.45,1.0)
  val attackRatio=exp((strength(w,m,home,true)-strength(w,m,!home,false))/40).coerceIn(.48,2.15)
  val controlEdge=((controlStrength(w,m,home)-pressureStrength(w,m,!home))/45.0).coerceIn(-1.45,1.45)
  val mentality=if(home)m.homeMentality else m.awayMentality
  val modeRisk=when{allOut(m,home)->.036;controlGame(m,home)->-.025;conserve(m,home)->.008;else->0.0}
  val primeCarrier=w.players[m.livePlayerId]?.messiMentored==true
  val patternKey=when(m.livePhase){LivePhase.POSSESSION->"AUFBAU";LivePhase.ATTACK,LivePhase.DANGEROUS_ATTACK->if(c.tactics.width>=4)"FLUEGEL" else "HALBRAUM";LivePhase.COUNTER->"DIAGONALE";else->"RESTVERTEIDIGUNG"};val automatismRisk=(1-MatchIntelligence.patternFactor(c,patternKey))*.055
  val turnoverChance=(when(m.livePhase){LivePhase.POSSESSION->.034;LivePhase.ATTACK->.048;LivePhase.DANGEROUS_ATTACK->.068;LivePhase.COUNTER->.064;else->.075}
   +buildRisk(c.tactics.buildUp)+(c.tactics.tempo-3)*.006+(mentality-3)*.004-controlEdge*.034+(1-fatigue)*.075+modeRisk+automatismRisk-(if(primeCarrier).040 else 0.0)).coerceIn(if(primeCarrier).006 else .016,.34)
  if(rng.chance(turnoverChance)){
   transferPossession(w,m,!home,if(rng.chance(.55))PossessionChangeReason.INTERCEPTION else PossessionChangeReason.TACKLE,rng,true);m.rngState=rng.state;return
  }

  when(m.livePhase){
   LivePhase.COUNTER->{
    m.counterTicksRemaining=(m.counterTicksRemaining-1).coerceAtLeast(0);m.chainTicks++
    val mover=moverFor(w,m,home,rng)
    if(m.counterTicksRemaining>0){setBallPhase(m,LivePhase.COUNTER,home,mover,"Schneller Gegenstoß");moveBallToward(w,m,home,LivePhase.COUNTER,rng)}
    else{
     val direct=(.46+(c.tactics.tempo-3)*.04+(if(mentality>=4).07 else 0.0)+(if(c.tactics.buildUp==BuildUp.COUNTER).10 else 0.0)+(if(conserve(m,home)).18 else 0.0)+(if(allOut(m,home)).12 else 0.0)-(if(controlGame(m,home)).08 else 0.0)).coerceIn(.28,.90)
     if(rng.chance(direct)){m.chainStep=2;setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,mover,"Konter in den Strafraum");moveBallToward(w,m,home,LivePhase.DANGEROUS_ATTACK,rng)}
     else{m.chainStep=1;setBallPhase(m,LivePhase.ATTACK,home,mover,"Konter läuft weiter");moveBallToward(w,m,home,LivePhase.ATTACK,rng)}
    }
   }
   LivePhase.POSSESSION->{
    m.chainTicks++
    val modeProgress=when{conserve(m,home)->.62;allOut(m,home)->1.22;controlGame(m,home)->.72;else->1.0}
    val progress=(.82*attackRatio.coerceIn(.72,1.55)*buildProgress(c.tactics.buildUp)*(.91+c.tactics.tempo*.03)*modeProgress*fatigue).coerceIn(.20,.97)
    if(rng.chance(progress)){
     val mover=moverFor(w,m,home,rng);m.chainStep=1;m.chainTicks=0;setBallPhase(m,LivePhase.ATTACK,home,mover,"Aufbau nach vorn");moveBallToward(w,m,home,LivePhase.ATTACK,rng)
    }else{setBallPhase(m,LivePhase.POSSESSION,home,detail=when{conserve(m,home)->"Tiefer Block – auf den Konter warten";controlGame(m,home)->"Ball sichern und Spiel beruhigen";allOut(m,home)->"Sofort wieder nach vorn";else->"Ball zirkuliert"});moveBallToward(w,m,home,LivePhase.POSSESSION,rng)}
   }
   LivePhase.ATTACK->{
    m.chainTicks++
    val flankBonus=if((c.tactics.buildUp==BuildUp.WIDE||c.tactics.width>=4)&&m.ballX !in .24f.. .76f).06 else 0.0
    val modeProgress=when{conserve(m,home)->.78;allOut(m,home)->1.16;controlGame(m,home)->.86;else->1.0}
    val primeProgress=if(w.players[m.livePlayerId]?.messiMentored==true).11 else 0.0
    val progress=(1.02*attackRatio.coerceIn(.72,1.48)*buildProgress(c.tactics.buildUp)*(.92+c.tactics.tempo*.024)*fatigue*modeProgress+flankBonus+primeProgress).coerceIn(.34,.995)
    if(rng.chance(progress)){
     val mover=moverFor(w,m,home,rng);ensurePerformance(w,m,mover).chancesCreated++;m.chainStep=2;m.chainTicks=0;setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,mover,if(m.ballX !in .24f.. .76f)"Über außen in Tornähe" else "In Tornähe");moveBallToward(w,m,home,LivePhase.DANGEROUS_ATTACK,rng)
    }else if(m.chainTicks>=3){m.chainStep=0;m.chainTicks=0;setBallPhase(m,LivePhase.POSSESSION,home,detail="Angriff neu aufgebaut");moveBallToward(w,m,home,LivePhase.POSSESSION,rng)}
    else{setBallPhase(m,LivePhase.ATTACK,home,moverFor(w,m,home,rng),"Angriff läuft");moveBallToward(w,m,home,LivePhase.ATTACK,rng)}
   }
   LivePhase.DANGEROUS_ATTACK->{
    m.chainTicks++;moveBallToward(w,m,home,LivePhase.DANGEROUS_ATTACK,rng)
    val modeShot=when{conserve(m,home)->.96;allOut(m,home)->1.16;controlGame(m,home)->.78;else->1.0}
    val shotChance=(1.04*attackRatio.coerceIn(.74,1.42)*buildShotIntent(c.tactics.buildUp)*fatigue*modeShot).coerceIn(.48,.995)
    if(rng.chance(shotChance)){
     val type=prepareShotLocation(w,m,home,rng);val shooter=selectShooter(w,m,home,rng)
     if(maybeOffside(w,m,home,shooter,type,rng)){m.rngState=rng.state;return}
     val assist=selectAssister(w,m,home,shooter,type,rng)
     if(shooter==w.user.playerId&&m.minute>=m.decisionCooldown){
      val player=w.players.getValue(shooter);val preview=buildShotContext(w,m,home,player,type,rng,assist)
      setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,shooter,"Du bist in Abschlussposition");m.pendingDecision=true;m.decisionShotType=type;m.decisionAssistId=assist;m.decisionXg=ShotModel.xg(preview);log(m,"${shotText(player,type,ShotModel.geometry(preview).distanceMeters)} Deine Entscheidung.","decision")
     }else resolveShot(w,m,home,shooter,rng,assist,type)
    }else if(rng.chance(.36)){
     val taker=moverFor(w,m,home,rng);queueCorner(w,m,home,taker,rng);log(m,"Schuss/Flanke wird geblockt – Ecke folgt.")
    }else if(m.chainTicks>=3){setBallPhase(m,LivePhase.ATTACK,home,moverFor(w,m,home,rng),"Zweiter Anlauf");m.chainStep=1;m.chainTicks=0;moveBallToward(w,m,home,LivePhase.ATTACK,rng)}
    else setBallPhase(m,LivePhase.DANGEROUS_ATTACK,home,moverFor(w,m,home,rng),"Druck am Strafraum")
   }
   LivePhase.YELLOW_CARD,LivePhase.YELLOW_RED_CARD,LivePhase.RED_CARD,LivePhase.INJURY->{
    setBallPhase(m,when(m.chainStep){0->LivePhase.POSSESSION;1->LivePhase.ATTACK;else->LivePhase.DANGEROUS_ATTACK},home,detail="Spiel fortgesetzt");moveBallToward(w,m,home,m.livePhase,rng)
   }
   else->{
    setBallPhase(m,LivePhase.POSSESSION,home,detail="Neuaufbau");moveBallToward(w,m,home,LivePhase.POSSESSION,rng)
   }
  }


  if(m.stoppageMinute==0&&m.minute%15==0&&!m.pendingDecision)log(m,if(pitch.surface==Surface.HARD&&pitch.pitchQuality<55)"Der Ball verspringt auf dem Hartplatz." else rng.pick(listOf("Der Trainer fordert Ruhe am Ball.","Die Mannschaft verschiebt kompakt.","Die Zuschauer treiben beide Teams an.")))
  m.rngState=rng.state
 }

 private fun selectShooter(w: World,m: LiveMatch,home: Boolean,rng: SeededRandom): Int {
  val team=xi(m,home).filter{it!=0&&it !in m.injured}
  val target=targetPlayerFor(w,m,home)
  val choices=team.flatMap{id->val player=w.players.getValue(id);val base=when(player.position){Position.ST->7;Position.LA,Position.RA,Position.OM->5;Position.ZM->3;Position.DM,Position.LV,Position.RV->2;Position.IV->1;Position.TW->0};val bonus=if(id==target)9 else 0;val primeBonus=if(player.messiMentored)14 else 0;List(base+bonus+primeBonus){id}}
  return if(choices.isNotEmpty())rng.pick(choices) else team.firstOrNull()?:0
 }

 private fun selectAssister(w: World,m: LiveMatch,home: Boolean,shooterId: Int,type: ShotType,rng: SeededRandom): Int {
  if(shooterId==0||type in setOf(ShotType.PENALTY,ShotType.FREE_KICK,ShotType.REBOUND))return 0
  val candidates=xi(m,home).filter{it!=0&&it!=shooterId&&it !in m.injured&&w.players[it]?.position!=Position.TW}
  if(candidates.isEmpty())return 0
  val assistChance=when(type){
   ShotType.ONE_ON_ONE->.82;ShotType.CUTBACK->.92;ShotType.HEADER,ShotType.VOLLEY->.90;ShotType.CLOSE_RANGE->.76;ShotType.BOX_SHOT->.70;ShotType.LONG_RANGE->.42;else->.50
  }
  if(!rng.chance(assistChance))return 0
  val last=m.livePlayerId.takeIf{it in candidates}
  val weighted=candidates.flatMap{id->
   val p=w.players.getValue(id);val creator=(p.attributes.passing*.45+p.attributes.vision*.37+p.attributes.technique*.18).roundToInt()
   val role=when(p.position){Position.OM->8;Position.LA,Position.RA->7;Position.ZM->6;Position.DM,Position.LV,Position.RV->4;Position.ST->3;Position.IV->2;Position.TW->0}
   val lastBonus=if(id==last)14 else 0;val primeBonus=if(p.messiMentored)12 else 0;List(((creator-45)/6+role+lastBonus+primeBonus).coerceAtLeast(1)){id}
  }
  return rng.pick(weighted)
 }

 private fun updateMomentum(w: World,m: LiveMatch){
  val territory=(.5f-m.ballY)*150f;val possession=if(m.chainOwnerClubId==m.homeId)18f else -18f;val quality=((strength(w,m,true,true)-strength(w,m,false,true))*1.15).toFloat()
  val raw=(territory+possession+quality).roundToInt().coerceIn(-100,100);m.attackMomentum=(m.attackMomentum*.58+raw*.42).roundToInt().coerceIn(-100,100);m.momentumHistory.add(m.attackMomentum);if(m.momentumHistory.size>100)m.momentumHistory.removeAt(0)
 }

 fun decide(w: World,m: LiveMatch,decision: Decision){
  require(m.pendingDecision){"Gerade steht keine Entscheidung an."};val rng=SeededRandom(m.rngState);val p=w.self();val home=p.clubId==m.homeId
  val mates=xi(m,home).filter{it!=0&&it!=p.id&&it !in m.injured&&w.players[it]?.position!=Position.TW}
  when(decision){
   Decision.SHOOT->resolveShot(w,m,home,p.id,rng,m.decisionAssistId,m.decisionShotType)
   Decision.DRIBBLE->{
    val perf=ensurePerformance(w,m,p.id);val prime=p.messiMentored
    val dribbleChance=(.31+p.attributes.technique*.005+p.fitness*.0015+(if(prime).16 else 0.0)).coerceAtMost(if(prime).965 else .82)
    if(rng.chance(dribbleChance)){
     val g=ShotModel.geometry(ShotContext(m.ballX,m.ballY,home));val gain=if(prime)7.0+rng.nextDouble()*7.5 else 3.5+rng.nextDouble()*4.0;setDistanceFromGoal(m,home,(g.distanceMeters-gain).coerceAtLeast(if(prime)3.8 else 4.5),(m.ballX+(0.5f-m.ballX)*(if(prime).62f else .35f)).coerceIn(.12f,.88f));log(m,if(prime)"Du ziehst im Messi-Stil zwischen den Gegenspielern durch." else "Du gehst am Gegenspieler vorbei.")
     val type=when{ShotModel.geometry(ShotContext(m.ballX,m.ballY,home)).distanceMeters<8.5->ShotType.CLOSE_RANGE;else->ShotType.BOX_SHOT};resolveShot(w,m,home,p.id,rng,m.decisionAssistId,type)
    }else{perf.turnovers++;log(m,"Beim Dribbling ist der Ball weg.");transferPossession(w,m,!home,PossessionChangeReason.TACKLE,rng,true)}
   }
   Decision.PASS,Decision.CROSS->{
    val perf=ensurePerformance(w,m,p.id);perf.passesAttempted++;val skill=if(decision==Decision.PASS)p.attributes.passing else (p.attributes.passing+p.attributes.technique)/2
    if(mates.isNotEmpty()&&rng.chance((.45+skill*.004+p.fitness*.001+(if(p.messiMentored).06 else 0.0)).coerceAtMost(if(p.messiMentored).97 else .9))){
     perf.passesCompleted++;perf.chancesCreated++;val target=targetPlayerFor(w,m,home);val id=if(target in mates&&rng.chance(if(decision==Decision.CROSS).76 else .62))target else mates.maxBy{w.players.getValue(it).attributes.finishing};m.passEvents.add(PassEvent(m.minute,p.clubId,p.id,id,true,m.ballX,m.ballY,(m.ballX+(rng.nextDouble()-.5)*.12).toFloat().coerceIn(.04f,.96f),(m.ballY+(if(home)-1 else 1)*.13).toFloat().coerceIn(.03f,.97f)))
     if(decision==Decision.CROSS){val d=6.5+rng.nextDouble()*8.0;val type=if(rng.chance(.72))ShotType.HEADER else ShotType.VOLLEY;setDistanceFromGoal(m,home,d,(.32+rng.nextDouble()*.36).toFloat());log(m,"Deine Flanke sucht ${w.players.getValue(id).lastName} im Strafraum.");if(!maybeOffside(w,m,home,id,type,rng))resolveShot(w,m,home,id,rng,p.id,type)}
     else{val d=6.0+rng.nextDouble()*10.0;val type=if(d<10.5)ShotType.CUTBACK else ShotType.BOX_SHOT;setDistanceFromGoal(m,home,d,(.37+rng.nextDouble()*.26).toFloat());log(m,"Du legst quer auf ${w.players.getValue(id).lastName}.");if(!maybeOffside(w,m,home,id,type,rng))resolveShot(w,m,home,id,rng,p.id,type)}
    }else{perf.turnovers++;m.passEvents.add(PassEvent(m.minute,p.clubId,p.id,0,false,m.ballX,m.ballY,m.ballX,m.ballY));log(m,"Die Hereingabe wird abgefangen.");transferPossession(w,m,!home,PossessionChangeReason.INTERCEPTION,rng,true)}
   }
   Decision.HOLD->{p.fitness=(p.fitness+.18).coerceAtMost(100.0);stats(m,home).possessionTicks+=2;m.chainStep=0;m.chainTicks=0;setBallPhase(m,LivePhase.POSSESSION,home,p.id,"Ball gesichert");moveBallToward(w,m,home,LivePhase.POSSESSION,rng);log(m,"Du sicherst den Ball. Dein Team kann nachrücken.")}
   Decision.FOUL->{
    stats(m,home).fouls++;addStoppageTime(m,7);val type=queueSetPiece(w,m,!home,p.id,rng);log(m,"Du stoppst den Gegenspieler. ${type.label} für den Gegner.")
    if(rng.chance(.65))card(w,m,p.id,false) else showQueuedSetPiece(w,m,rng)
   }
  }
  m.pendingDecision=false;m.decisionAssistId=0;m.decisionCooldown=m.minute+8;m.rngState=rng.state
  if(!m.incidentPause&&m.pendingSetPieceClubId==0&&m.pendingCornerClubId==0&&m.pendingPossessionClubId==0&&m.pendingVarShotIndex<0&&periodComplete(m))endCurrentPeriod(w,m,rng)
 }

 fun setConserveEnergy(w: World,m: LiveMatch,clubId: Int=w.user.clubId,enabled: Boolean){
  require(!m.finished){"Das Spiel ist beendet."};val home=clubId==m.homeId;require(home||clubId==m.awayId)
  if(home){m.homeConserveEnergy=enabled;if(enabled){m.homeAllOutAttack=false;m.homeControlGame=false}}else{m.awayConserveEnergy=enabled;if(enabled){m.awayAllOutAttack=false;m.awayControlGame=false}}
  MatchAnalysisSystem.recordTacticChange(w,m,clubId,if(enabled)"Kräfte schonen" else "Kräfte schonen beendet");log(m,"${w.clubs.getValue(clubId).shortName}: ${if(enabled)"Kräfte schonen – tiefer Block und Konter" else "Kräfte schonen beendet"}.")
 }

 fun setAllOutAttack(w: World,m: LiveMatch,clubId: Int=w.user.clubId,enabled: Boolean){
  require(!m.finished){"Das Spiel ist beendet."};val home=clubId==m.homeId;require(home||clubId==m.awayId)
  if(home){m.homeAllOutAttack=enabled;if(enabled){m.homeConserveEnergy=false;m.homeControlGame=false}}else{m.awayAllOutAttack=enabled;if(enabled){m.awayConserveEnergy=false;m.awayControlGame=false}}
  MatchAnalysisSystem.recordTacticChange(w,m,clubId,if(enabled)"Alles nach vorn" else "Alles nach vorn beendet");log(m,"${w.clubs.getValue(clubId).shortName}: ${if(enabled)"Alles nach vorn – volles Risiko" else "Alles nach vorn beendet"}.")
 }

 fun setControlGame(w: World,m: LiveMatch,clubId: Int=w.user.clubId,enabled: Boolean){
  require(!m.finished){"Das Spiel ist beendet."};val home=clubId==m.homeId;require(home||clubId==m.awayId)
  if(home){m.homeControlGame=enabled;if(enabled){m.homeConserveEnergy=false;m.homeAllOutAttack=false}}else{m.awayControlGame=enabled;if(enabled){m.awayConserveEnergy=false;m.awayAllOutAttack=false}}
  MatchAnalysisSystem.recordTacticChange(w,m,clubId,if(enabled)"Spiel kontrollieren" else "Spielkontrolle beendet");log(m,"${w.clubs.getValue(clubId).shortName}: ${if(enabled)"Spiel kontrollieren – Ball und Rhythmus sichern" else "Spielkontrolle beendet"}.")
 }

 fun changeFormation(w: World,m: LiveMatch,clubId: Int=w.user.clubId,newFormation: String){
  require(!m.finished){"Das Spiel ist beendet."};require(newFormation in Formations.all.keys){"Unbekannte Formation."}
  val home=clubId==m.homeId;require(home||clubId==m.awayId){"Verein spielt nicht in dieser Partie."}
  val current=xi(m,home);val reordered=reorderLineup(w,current,newFormation)
  if(home){m.homeXi=reordered;m.homeFormation=newFormation}else{m.awayXi=reordered;m.awayFormation=newFormation}
  w.clubs.getValue(clubId).tactics.formation=newFormation;MatchAnalysisSystem.recordTacticChange(w,m,clubId,"Formation $newFormation");log(m,"Taktik: ${w.clubs.getValue(clubId).shortName} stellt auf $newFormation um.")
 }

 private fun reorderLineup(w: World,current: List<Int>,newFormation: String): MutableList<Int>{
  val players=current.filter{it!=0}.distinct().toMutableList();val slots=Formations.positions(newFormation);val result=MutableList(slots.size){0};val open=slots.indices.toMutableList()
  while(players.isNotEmpty()&&open.isNotEmpty()){
   var bestPlayer=players.first();var bestSlot=open.first();var bestScore=Int.MIN_VALUE
   for(id in players){val p=w.players[id]?:continue;for(index in open){val target=slots[index];val keeperBias=when{target==Position.TW&&p.position==Position.TW->28;target==Position.TW->-45;p.position==Position.TW->-35;else->0};val score=p.ratingAt(target)+keeperBias;if(score>bestScore){bestScore=score;bestPlayer=id;bestSlot=index}}}
   result[bestSlot]=bestPlayer;players.remove(bestPlayer);open.remove(bestSlot)
  }
  return result
 }

 fun substitute(w: World,m: LiveMatch,out: Int,incoming: Int,clubId: Int=w.user.clubId){
  require(!m.finished&&!m.pendingDecision){"Wechsel gerade nicht möglich."};val home=clubId==m.homeId;require(home||clubId==m.awayId)
  val lineup=xi(m,home);val bench=if(home)m.homeBench else m.awayBench
  require((if(home)m.homeSubs else m.awaySubs)<5){"Fünf Wechsel sind bereits erfolgt."};require(out!=0&&out in lineup&&incoming in bench&&w.players.getValue(incoming).available){"Dieser Wechsel ist nicht möglich."}
  lineup[lineup.indexOf(out)]=incoming;bench.remove(incoming);if(home)m.homeSubs++ else m.awaySubs++;addStoppageTime(m,20)
  ensurePerformance(w,m,incoming);if(incoming !in m.participation)m.participation.add(incoming)
  log(m,"Wechsel: ${w.players.getValue(incoming).name} für ${w.players.getValue(out).name}.")
 }

 fun substitutionSuggestions(w: World,m: LiveMatch,clubId: Int=w.user.clubId): List<SubSuggestion>{
  val home=clubId==m.homeId;require(home||clubId==m.awayId)
  val lineup=xi(m,home);val bench=(if(home)m.homeBench else m.awayBench).filter{w.players[it]?.available==true};if(bench.isEmpty())return emptyList()
  val slots=Formations.positions(formation(m,home));val ownGoals=if(home)m.home.goals else m.away.goals;val oppGoals=if(home)m.away.goals else m.home.goals
  val behind=ownGoals<oppGoals;val ahead=ownGoals>oppGoals;val result=mutableListOf<SubSuggestion>()
  lineup.forEachIndexed{index,outId->if(outId!=0){
   val out=w.players[outId]?:return@forEachIndexed;val target=slots.getOrElse(index){out.position};val currentRating=calculatePlayerRating(w,m,outId)
   val yellow=m.yellows[outId]?:0;val minutes=m.minutesPlayed[outId]?:m.minute;val perf=m.playerPerformance[outId]
   bench.forEach{inId->
    val incoming=w.players.getValue(inId);val outRating=out.ratingAt(target);val inRating=incoming.ratingAt(target)
    val injury=if(outId in m.injured)220.0 else 0.0
    val fatigue=((78-out.fitness).coerceAtLeast(0.0)*1.25)+(if(out.fitness<62)16.0 else 0.0)
    val performance=((6.45-currentRating).coerceAtLeast(0.0)*15.0)
    val cardRisk=yellow*(if(target in listOf(Position.IV,Position.LV,Position.RV,Position.DM))14.0 else 9.0)
    val load=when{minutes>=82->8.0;minutes>=70->4.0;else->0.0}
    val hotProtection=(if(currentRating>=7.7)12.0 else 0.0)+(perf?.goals?:0)*9.0+(perf?.assists?:0)*5.5
    val ratingGain=(inRating-outRating)*1.65
    val readiness=(incoming.fitness-out.fitness)*.32+(incoming.form-out.form)*3.4+(incoming.sharpness-out.sharpness)*.055+(incoming.morale-out.morale)*.025
    val positional=(incoming.fit(target)-.80)*34.0
    val attackDelta=((incoming.attributes.finishing+incoming.attributes.passing+incoming.attributes.pace)-(out.attributes.finishing+out.attributes.passing+out.attributes.pace))/12.0
    val defendDelta=((incoming.attributes.tackling+incoming.attributes.stamina+incoming.attributes.strength)-(out.attributes.tackling+out.attributes.stamina+out.attributes.strength))/13.0
    val tactical=when{behind&&m.minute>=58->attackDelta;ahead&&m.minute>=68->defendDelta;else->(attackDelta+defendDelta)*.22}
    val score=injury+fatigue+performance+cardRisk+load-hotProtection+ratingGain+readiness+positional+tactical
    val factors=mutableListOf<String>()
    if(outId in m.injured)factors+="${out.lastName} verletzt"
    if(out.fitness<72)factors+="Fitness ${out.fitness.roundToInt()} %"
    if(currentRating<6.3)factors+="Rating ${(currentRating*10).roundToInt()/10.0}"
    if(yellow>0)factors+="${if(yellow>=2)"Platzverweis" else "Gelb-Risiko"}"
    if(behind&&m.minute>=58&&attackDelta>1.5)factors+="mehr Offensivwirkung"
    if(ahead&&m.minute>=68&&defendDelta>1.5)factors+="mehr Stabilität"
    if(factors.isEmpty())factors+="Belastung/Matchup"
    val reason=factors.take(3).joinToString(" · ")+" · ${incoming.lastName}: $inRating/99 ${target.name}, ${incoming.fitness.roundToInt()} % fit, Form ${(incoming.form*10).roundToInt()/10.0}"
    result.add(SubSuggestion(outId,inId,target,score,reason))
   }
  }}
  val sorted=result.sortedByDescending{it.score};val usedIn=mutableSetOf<Int>();val usedOut=mutableSetOf<Int>();val diverse=sorted.filter{usedIn.add(it.inId)&&usedOut.add(it.outId)}.take(5).toMutableList()
  if(diverse.size<5){val existing=diverse.map{it.outId to it.inId}.toMutableSet();for(sug in sorted){if(diverse.size>=5)break;if(existing.add(sug.outId to sug.inId))diverse.add(sug)}}
  return diverse.take(5)
 }

 fun resumeIncident(w: World,m: LiveMatch){
  require(m.incidentPause){"Das Spiel ist nicht unterbrochen."}
  if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId){
   val home=w.user.clubId==m.homeId;val lineup=xi(m,home);val bench=if(home)m.homeBench else m.awayBench;val subs=if(home)m.homeSubs else m.awaySubs;val canReplace=subs<5&&bench.any{w.players[it]?.available==true}
   require(!canReplace||m.incidentPlayerId !in lineup){"Verletzten Spieler zuerst wechseln."}
  }
  val reason=m.incidentReason;m.incidentPause=false;m.incidentReason=MatchPauseReason.NONE;m.incidentPlayerId=0;m.incidentClubId=0
  if(m.pendingSetPieceClubId==0&&periodComplete(m)){val rng=SeededRandom(m.rngState);endCurrentPeriod(w,m,rng)}
  else log(m,if(reason==MatchPauseReason.RED_CARD)"Nach der taktischen Neuordnung läuft das Spiel weiter." else "Der Schiedsrichter gibt das Spiel wieder frei.")
 }

 private fun pauseForIncident(m: LiveMatch,reason: MatchPauseReason,playerId: Int,clubId: Int){m.incidentPause=true;m.incidentReason=reason;m.incidentPlayerId=playerId;m.incidentClubId=clubId}

 private fun clearAssistantSubProposal(m:LiveMatch){m.assistantSubPending=false;m.assistantSubOutId=0;m.assistantSubInId=0;m.assistantSubReason="";m.assistantSubSuggestedMinute=-1}

 fun acceptAssistantSubstitution(w:World,m:LiveMatch){
  require(m.assistantSubPending){"Es liegt kein Co-Trainer-Wechselvorschlag vor."}
  val out=m.assistantSubOutId;val incoming=m.assistantSubInId;val reason=m.assistantSubReason
  clearAssistantSubProposal(m)
  substitute(w,m,out,incoming,w.user.clubId)
  w.assistantCoach.lastSubReason="${m.minute}. Minute: angenommen · $reason"
 }

 fun rejectAssistantSubstitution(w:World,m:LiveMatch){
  require(m.assistantSubPending){"Es liegt kein Co-Trainer-Wechselvorschlag vor."}
  val out=m.assistantSubOutId;val incoming=m.assistantSubInId;val reason=m.assistantSubReason;val forced=out in m.injured
  m.assistantSubRejectedOutId=out;m.assistantSubRejectedInId=incoming;m.assistantSubRejectedUntilMinute=m.minute+if(forced)1 else 8
  clearAssistantSubProposal(m)
  w.assistantCoach.lastSubReason="${m.minute}. Minute: abgelehnt · $reason"
  if(forced&&out in xi(m,w.user.clubId==m.homeId))pauseForIncident(m,MatchPauseReason.INJURY,out,w.user.clubId)
 }

 private fun aiSub(w: World,m: LiveMatch,home: Boolean){
  if((if(home)m.homeSubs else m.awaySubs)>=5||m.pendingDecision||m.assistantSubPending)return
  val club=clubId(m,home);val suggestions=substitutionSuggestions(w,m,club)
  val userAssistant=club==w.user.clubId&&w.assistantCoach.autoSubstitutions
  val suggestion=suggestions.firstOrNull{candidate->
   !userAssistant||m.minute>=m.assistantSubRejectedUntilMinute||candidate.outId!=m.assistantSubRejectedOutId||candidate.inId!=m.assistantSubRejectedInId
  }?:return
  val forced=suggestion.outId in m.injured
  val current=w.players[suggestion.outId]?:return
  val yellow=m.yellows[suggestion.outId]?:0
  val aggression=if(userAssistant)w.assistantCoach.substitutionAggression.coerceIn(1,5) else 3
  val urgent=forced||current.fitness<(64+(aggression-3)*2)||yellow>0||calculatePlayerRating(w,m,suggestion.outId)<(6.0+(aggression-3)*.08)
  val earliest=(62-(aggression-3)*4).coerceIn(52,70)
  val normalMinute=(74-(aggression-3)*3).coerceIn(64,80)
  val threshold=17.0-(aggression-3)*3.5
  if(!urgent&&m.minute<earliest)return
  if(!urgent&&m.minute<normalMinute&&suggestion.score<threshold)return
  if(userAssistant){
   m.assistantSubPending=true;m.assistantSubOutId=suggestion.outId;m.assistantSubInId=suggestion.inId;m.assistantSubReason=suggestion.reason;m.assistantSubSuggestedMinute=m.minute
   w.assistantCoach.lastSubReason="${m.minute}. Minute: Vorschlag · ${suggestion.reason}"
   log(m,"Co-Trainer empfiehlt: ${w.players.getValue(suggestion.outId).lastName} raus, ${w.players.getValue(suggestion.inId).lastName} rein.","decision")
   return
  }
  substitute(w,m,suggestion.outId,suggestion.inId,club)
 }

 private fun resolveShot(w: World,m: LiveMatch,home: Boolean,id: Int,rng: SeededRandom,assist: Int=0,typeHint: ShotType?=null,allowRebound: Boolean=true){
  if(id==0)return
  val s=stats(m,home);val p=w.players.getValue(id)
  val keeperId=xi(m,!home).firstOrNull{it!=0&&it !in m.injured&&w.players[it]?.position==Position.TW}?:xi(m,!home).firstOrNull{it!=0&&it !in m.injured}?:0
  val keeper=w.players[keeperId]
  val baseGeometry=ShotModel.geometry(ShotContext(m.ballX,m.ballY,home))
  val type=typeHint?:when{baseGeometry.distanceMeters>=23.5->ShotType.LONG_RANGE;baseGeometry.distanceMeters<=8.5->ShotType.CLOSE_RANGE;else->ShotType.BOX_SHOT}
  val context=buildShotContext(w,m,home,p,type,rng,assist);val geometry=ShotModel.geometry(context)
  val shotXg=ShotModel.xg(context);val goalProbability=ShotModel.goalProbability(shotXg,p,keeper,context)
  val blockProbability=ShotModel.blockProbability(context);val onTargetProbability=ShotModel.onTargetProbability(goalProbability,p,context)
  val savedOnTarget=(onTargetProbability-goalProbability).coerceAtLeast(0.0)
  val woodworkProbability=(.012+shotXg*.060+(if(type in setOf(ShotType.CLOSE_RANGE,ShotType.ONE_ON_ONE)).006 else 0.0)).coerceIn(.010,.052)
  val r=rng.nextDouble()
  var outcome=when{
   r<goalProbability->ShotOutcome.GOAL
   r<goalProbability+woodworkProbability->ShotOutcome.WOODWORK
   r<goalProbability+woodworkProbability+blockProbability->ShotOutcome.BLOCKED
   r<goalProbability+woodworkProbability+blockProbability+savedOnTarget->ShotOutcome.SAVED
   else->ShotOutcome.OFF_TARGET
  }

  if(outcome==ShotOutcome.SAVED){
   val speed=ShotModel.shotSpeed(p,context)
   val keeperQuality=keeper?.let{(it.attributes.keeping*.72+it.hidden.consistency*.10+it.sharpness*.08+it.fitness*.05+it.form*5.0*.05).coerceIn(10.0,99.0)}?:20.0
   val secure=(.30+(keeperQuality-45)*.0045+(geometry.distanceMeters-10).coerceIn(0.0,25.0)*.006-speed*.16).coerceIn(.22,.68)
   val rebound=((.18+speed*.16-(keeperQuality-45)*.0025)+(if(type==ShotType.CLOSE_RANGE||type==ShotType.REBOUND).06 else 0.0)).coerceIn(.08,.34)
   val corner=(.15+speed*.09+(if(kotlin.math.abs(geometry.lateralMeters)>10).04 else 0.0)).coerceIn(.12,.29)
   val kr=rng.nextDouble()
   outcome=when{kr<secure->ShotOutcome.SAVED;kr<secure+corner->ShotOutcome.CORNER;kr<secure+corner+rebound&&allowRebound->ShotOutcome.REBOUND;else->ShotOutcome.DEFLECTED}
  }

  val perf=ensurePerformance(w,m,id);perf.shots++;perf.xg+=shotXg;s.shots++;s.xg+=shotXg
  val target=shotTarget(outcome,rng,type==ShotType.PENALTY)
  m.lastShotType=type;m.lastShotX=context.x;m.lastShotY=context.y;m.lastShotXg=shotXg;m.lastShotGoalProbability=goalProbability;m.lastShotTargetX=target.first;m.lastShotTargetY=target.second;m.lastShotTargetLabel=target.third;m.lastShotOutcome=outcome
  val validAssist=assist.takeIf{it!=0&&it!=id&&w.players[it]?.clubId==clubId(m,home)}?:0
  val shotEvent=ShotEvent(m.minute,clubId(m,home),id,validAssist,type,context.x,context.y,geometry.distanceMeters,geometry.angleRadians,context.pressure,context.defendersNearby,context.passQuality,shotXg,goalProbability,outcome,target.first,target.second,target.third,clockLabel(m))
  m.shotEvents.add(shotEvent)
  log(m,shotText(p,type,geometry.distanceMeters))

  when(outcome){
   ShotOutcome.GOAL->{
    addStoppageTime(m,25)
    val reviewChance=when(type){ShotType.ONE_ON_ONE->.38;ShotType.CUTBACK->.32;ShotType.HEADER,ShotType.VOLLEY->.28;ShotType.PENALTY->.18;ShotType.FREE_KICK->.12;else->.24}
    if(rng.chance(reviewChance)){
     s.varChecks++;m.pendingVarShotIndex=m.shotEvents.lastIndex;m.pendingVarKeeperId=keeperId;m.varReviewStage=0;m.varReviewReason="";m.varReviewResult="";m.varWillOverturn=false
     setBallPhase(m,LivePhase.GOAL,home,id,"TOR · ${target.third} · ${type.label}")
     log(m,"Treffer von ${p.name}: ${target.third}. Der VAR kann die Szene noch prüfen.","goal")
    }else confirmGoal(w,m,home,id,validAssist,keeperId,type)
   }
   ShotOutcome.WOODWORK->{
    val frame=if(rng.chance(.5))"Pfosten" else "Latte"
    s.shotsOffTarget++;perf.shotsOffTarget++;setBallPhase(m,LivePhase.WOODWORK,home,id,"${target.third} · ${type.label}")
    log(m,"Aluminium! ${p.lastName} schießt ${target.third}.")
    if(rng.chance(.38)){m.chainStep=2;m.chainTicks=0;m.ballY=(if(home).08f else .92f);log(m,"Der Abpraller bleibt gefährlich.")}
    else queueRestart(m,clubId(m,!home),PossessionChangeReason.GOAL_KICK)
   }
   ShotOutcome.BLOCKED->{
    s.blockedShots++;val defenders=xi(m,!home).filter{it!=0&&it !in m.injured};if(defenders.isNotEmpty())ensurePerformance(w,m,rng.pick(defenders)).defensiveActions++
    if(rng.chance(.42)){queueCorner(w,m,home,id,rng);setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"Geblockt – Ecke");log(m,"Der Abschluss wird geblockt. Ecke.")}
    else{setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"Schuss geblockt");log(m,"Der Abschluss wird geblockt und geklärt.");queueRestart(m,clubId(m,!home),PossessionChangeReason.CLEARANCE)}
   }
   ShotOutcome.OFF_TARGET->{
    s.shotsOffTarget++;perf.shotsOffTarget++;setBallPhase(m,LivePhase.SHOT_OFF_TARGET,home,id,"${target.third} · ${type.label}");log(m,"${p.name} schießt ${target.third}.");queueRestart(m,clubId(m,!home),PossessionChangeReason.GOAL_KICK)
   }
   ShotOutcome.SAVED->{
    s.shotsOnTarget++;perf.shotsOnTarget++;if(keeperId!=0)ensurePerformance(w,m,keeperId).defensiveActions++
    setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"${target.third} · gehalten");log(m,"${p.lastName} schießt ${target.third} – der Torwart hält sicher.");queueRestart(m,clubId(m,!home),PossessionChangeReason.SAVE)
   }
   ShotOutcome.CORNER->{
    s.shotsOnTarget++;perf.shotsOnTarget++;if(keeperId!=0)ensurePerformance(w,m,keeperId).defensiveActions++
    setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"${target.third} · Parade zur Ecke");log(m,"${p.lastName} schießt ${target.third} – starke Parade zur Ecke.");queueCorner(w,m,home,id,rng)
   }
   ShotOutcome.DEFLECTED->{
    s.shotsOnTarget++;perf.shotsOnTarget++;if(keeperId!=0)ensurePerformance(w,m,keeperId).defensiveActions++
    setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"${target.third} · abgewehrt");log(m,"${p.lastName} schießt ${target.third} – der Torwart wehrt ab.");queueRestart(m,clubId(m,!home),PossessionChangeReason.CLEARANCE)
   }
   ShotOutcome.REBOUND->{
    s.shotsOnTarget++;perf.shotsOnTarget++;if(keeperId!=0)ensurePerformance(w,m,keeperId).defensiveActions++
    setBallPhase(m,LivePhase.SHOT_ON_TARGET,home,id,"${target.third} · Parade, Abpraller");log(m,"${p.lastName} schießt ${target.third} – der Torwart kann nur abwehren. Abpraller!")
    val rebounder=selectShooter(w,m,home,rng)
    if(rebounder!=0){val d=4.5+rng.nextDouble()*7.5;setDistanceFromGoal(m,home,d,(.36+rng.nextDouble()*.28).toFloat());resolveShot(w,m,home,rebounder,rng,typeHint=ShotType.REBOUND,allowRebound=false)}
    else queueRestart(m,clubId(m,!home),PossessionChangeReason.CLEARANCE)
   }
   ShotOutcome.DISALLOWED->{}
  }
 }

 private fun card(w: World,m: LiveMatch,id: Int,directRed: Boolean){
  val p=w.players.getValue(id);val previous=m.yellows[id]?:0;val yellow=if(directRed)previous else previous+1;if(!directRed)m.yellows[id]=yellow
  val perf=ensurePerformance(w,m,id)
  if(directRed||yellow>=2){
   if(!directRed)perf.yellows++ else perf.red++;if(!directRed)perf.red++
   if(id !in m.sentOff)m.sentOff.add(id);if(id in m.homeXi)m.homeXi[m.homeXi.indexOf(id)]=0 else if(id in m.awayXi)m.awayXi[m.awayXi.indexOf(id)]=0
   addStoppageTime(m,35);val phase=if(!directRed&&yellow>=2)LivePhase.YELLOW_RED_CARD else LivePhase.RED_CARD;setIncidentPhase(m,phase,p.clubId,id,phase.label);log(m,"${if(directRed)"Rot" else "Gelb-Rot"} für ${p.name}.","bad");pauseForIncident(m,MatchPauseReason.RED_CARD,id,p.clubId)
  }else{perf.yellows++;addStoppageTime(m,15);setIncidentPhase(m,LivePhase.YELLOW_CARD,p.clubId,id,"Gelbe Karte");log(m,"Gelb für ${p.name}.")}
 }

 fun calculatePlayerRating(w: World,m: LiveMatch,id: Int): Double {
  val p=w.players[id]?:return 6.5;val perf=m.playerPerformance[id]?:return 6.5;val minutes=perf.minutes.coerceAtLeast(m.minutesPlayed[id]?:0)
  val exposure=(minutes/45.0).coerceIn(.18,1.0);var r=6.5
  val passAccuracy=if(perf.passesAttempted>=4)perf.passesCompleted.toDouble()/perf.passesAttempted else .76
  r+=(passAccuracy-.76)*1.25*exposure;r-=perf.turnovers*.035*exposure;r+=perf.chancesCreated*.09*exposure
  val defensiveWeight=when(p.position){Position.TW->.13;Position.IV,Position.LV,Position.RV,Position.DM->.09;else->.045}
  r+=perf.defensiveActions*defensiveWeight*exposure
  val goalWeight=when(p.position){Position.TW->1.45;Position.IV,Position.LV,Position.RV->1.18;Position.DM,Position.ZM->1.02;Position.OM,Position.LA,Position.RA->.92;Position.ST->.82}
  r+=perf.goals*goalWeight+perf.assists*.52+perf.shotsOnTarget*.06-perf.shotsOffTarget*.035
  val concededWeight=when(p.position){Position.TW->.19;Position.IV,Position.LV,Position.RV,Position.DM->.10;else->.025}
  r-=perf.goalsConceded*concededWeight*exposure;r-=perf.yellows*.16;r-=perf.red*1.20
  val loss=(perf.fitnessStart-p.fitness).coerceAtLeast(0.0);if(minutes>=55&&loss>18)r-=(loss-18)*.018
  // Kurze Einsätze bleiben bewusst nahe an einer neutralen 6,5; klare Tore/Karten wirken dennoch direkt.
  if(minutes<15&&perf.goals==0&&perf.assists==0&&perf.red==0)r=6.5+(r-6.5)*.38
  return (round(r.coerceIn(1.0,10.0)*10)/10.0)
 }

 fun ratingSummary(w: World,m: LiveMatch,id: Int): String {
  val p=w.players[id]?:return "Ordentlicher, unauffälliger Auftritt.";val perf=m.playerPerformance[id]?:return "Ordentlicher, unauffälliger Auftritt.";val rating=perf.rating
  return when{
   perf.red>0->"Platzverweis belastet die Mannschaft deutlich."
   perf.goals>=2->"Überragend vor dem Tor und ständig gefährlich."
   perf.goals>0||perf.assists>0->"Entscheidend an den gefährlichen Aktionen beteiligt."
   perf.turnovers>=6&&perf.passesAttempted>0->"Viele Ballverluste und zu wenig Sicherheit im Spiel."
   (p.position==Position.TW||p.position in listOf(Position.IV,Position.LV,Position.RV,Position.DM))&&perf.defensiveActions>=4->"Defensiv aufmerksam mit mehreren wichtigen Aktionen."
   rating>=8.0->"Starker Auftritt, viele gefährliche Aktionen."
   rating>=7.0->"Gute Leistung mit vielen sauberen Aktionen."
   rating>=6.0->"Ordentlicher, weitgehend stabiler Auftritt."
   rating>=5.0->"Schwacher Auftritt mit zu wenig gelungenen Aktionen."
   else->"Sehr schwieriger Abend mit mehreren folgenschweren Fehlern."
  }
 }

 private fun finalizeRatings(w: World,m: LiveMatch){
  m.participation.distinct().forEach{id->val perf=ensurePerformance(w,m,id);perf.minutes=m.minutesPlayed[id]?:perf.minutes;perf.goals=m.goals.count{it.playerId==id};perf.assists=m.goals.count{it.assistId==id};perf.rating=calculatePlayerRating(w,m,id);perf.summary=ratingSummary(w,m,id)}
 }

 private fun log(m: LiveMatch,text: String,tone: String="normal"){m.ticker.add(Ticker(m.minute,text,tone,clockLabel(m)));if(m.ticker.size>260)m.ticker.removeAt(0)}
 private fun finish(w: World,m: LiveMatch){if(m.finished)return;m.finished=true;m.halfTime=false;m.shootoutActive=false;finalizeRatings(w,m);val extra=if(m.extraTimePlayed)" nach Verlängerung" else "";val pens=if(m.homePens>0||m.awayPens>0)" · ${m.homePens}:${m.awayPens} i.E." else "";log(m,"Abpfiff$extra. ${m.home.goals}:${m.away.goals}$pens. Die Mannschaft geht zu den Zuschauern.")}

 private fun autoResolveIncident(w: World,m: LiveMatch){
  if(!m.incidentPause)return
  if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId){val suggestion=substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId};if(suggestion!=null)substitute(w,m,suggestion.outId,suggestion.inId)}
  resumeIncident(w,m)
 }

 fun simulateFullMatch(w: World,f: Fixture): LiveMatch{val m=start(w,f);while(!m.finished){when{m.assistantSubPending->acceptAssistantSubstitution(w,m);m.incidentPause->autoResolveIncident(w,m);m.pendingDecision->decide(w,m,Decision.SHOOT);m.halfTime->secondHalf(m);else->step(w,m)}};return m}

 fun record(w: World,m: LiveMatch){
  require(m.finished){"Das Spiel läuft noch."};val f=w.fixtures.first{it.id==m.fixtureId};if(f.played)return;f.played=true
  w.matches[f.id]=MatchRecord(fixtureId=f.id,homeId=m.homeId,awayId=m.awayId,home=m.home.copy(),away=m.away.copy(),minute=m.minute,goals=m.goals.toList(),attendance=m.attendance,shotEvents=m.shotEvents.toList(),passEvents=m.passEvents.toList(),tacticChanges=m.tacticChanges.toList(),homePens=m.homePens,awayPens=m.awayPens,extraTimePlayed=m.extraTimePlayed)
  for(id in m.participation.distinct()){
   val p=w.players.getValue(id);val perf=m.playerPerformance[id]?:PlayerMatchPerformance(minutes=m.minutesPlayed[id]?:0,fitnessStart=p.fitness,rating=6.5)
   p.stats.appearances++;p.stats.minutes+=m.minutesPlayed[id]?:0;p.stats.goals+=m.goals.count{it.playerId==id};p.stats.assists+=m.goals.count{it.assistId==id};p.stats.yellow+=m.yellows[id]?:0
   if(id in m.sentOff){p.stats.red++;p.unavailableReason=UnavailableReason.SUSPENDED;p.unavailableWeeks=2};p.sharpness=(p.sharpness+5).coerceAtMost(100);p.form=perf.rating.coerceIn(1.0,10.0)
  }
  for((id,score,conceded) in listOf(Triple(m.homeId,m.home.goals,m.away.goals),Triple(m.awayId,m.away.goals,m.home.goals))){val c=w.clubs.getValue(id);val result=if(score>conceded)"S" else if(score==conceded)"U" else "N";c.form.add(result);if(c.form.size>5)c.form.removeAt(0);w.squad(id).forEach{it.morale=(it.morale+if(result=="S")4 else if(result=="N")-3 else 0).coerceIn(5,100)};if(id==m.homeId){c.lastIncome=m.attendance*(if(!w.privateTopClubMode&&c.tier>=7)4 else if(w.privateTopClubMode)18 else 22-c.tier*2);c.budget+=c.lastIncome}else c.lastIncome=0}
 }
}

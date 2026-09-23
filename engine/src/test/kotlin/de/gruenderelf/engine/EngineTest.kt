package de.gruenderelf.engine
import org.junit.Test
import kotlin.test.*
import java.nio.file.Files

class EngineTest {
 @Test fun createWorldHasTenLeagues120DomesticClubsAndNewCompetitions(){val w=WorldFactory.createWorld(42);assertEquals(10,w.leagues.size);assertEquals(120,w.leagues.flatMap{it.clubIds}.toSet().size);assertEquals(140,w.clubs.size);assertEquals(10,w.club().tier);assertTrue(w.leagues.all{it.clubIds.size==12});assertEquals(SaveCodec.encode(w),SaveCodec.encode(WorldFactory.createWorld(42)));assertEquals(1320,w.fixtures.count{it.competition==CompetitionType.LEAGUE});assertEquals(32,w.fixtures.count{it.competition==CompetitionType.NATIONAL_CUP&&it.round==1});assertEquals(96,w.fixtures.count{it.competition==CompetitionType.EURO_ELITE&&it.stage=="Ligaphase"});for(id in w.leagues.flatMap{it.clubIds}){val games=w.fixtures.filter{it.competition==CompetitionType.LEAGUE&&(it.homeId==id||it.awayId==id)};assertEquals(22,games.size);assertEquals(22,games.map{it.matchday}.toSet().size);assertEquals(11,games.count{it.homeId==id})}}

 @Test fun generatedClubsHaveUniqueNamesAndNeutralLeagueLabels(){val w=WorldFactory.createWorld(81);val domestic=w.leagues.flatMap{it.clubIds}.map{w.clubs.getValue(it)};assertEquals(120,domestic.map{it.name}.toSet().size);assertTrue(w.leagues.none{it.name.contains("Hannover",ignoreCase=true)})}
 @Test fun realModeContainsExpandedEuropeanPyramidOwnPlayerAndUefaCompetitions(){
  val selected=RealModeDatabase.leagues.first().clubs.first();val originalName=selected.players.first().name
  val self=PlayerDraft(firstName="Leon",lastName="Test",number=10,position=Position.ST,attributes=Attributes(91,94,88,93,55,82,90,90,84,9,91))
  val w=WorldFactory.createRealModeWorld(90458,selected.key,self)
  assertTrue(w.privateTopClubMode);assertEquals(33,w.leagues.size);val names=w.leagues.map{it.name}.toSet();assertTrue(setOf("Bundesliga","2. Bundesliga","3. Liga","Regionalliga Nord","Oberliga Hamburg","Premier League","Championship","League One","La Liga","LaLiga Hypermotion","Primera Federación Grupo 1","Primera Federación Grupo 2","Serie A","Serie B","Ligue 1","Ligue 2","SuperSport HNL","Russian Premier League","Eredivisie","Primeira Liga").all{it in names})
  assertTrue(w.leagues.all{it.clubIds.size in 10..24&&it.clubIds.size%2==0});assertEquals(selected.name,w.club().name);assertTrue(w.squad().any{it.name==originalName});assertEquals("Leon Test",w.self().name)
  val ownRounds=(w.leagues.first{w.user.clubId in it.clubIds}.clubIds.size-1)*2;assertEquals(ownRounds,w.fixtures.count{it.competition==CompetitionType.LEAGUE&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)})
  assertEquals(144,w.fixtures.count{it.competition==CompetitionType.CHAMPIONS_LEAGUE&&it.stage=="Ligaphase"});assertEquals(144,w.fixtures.count{it.competition==CompetitionType.EUROPA_LEAGUE&&it.stage=="Ligaphase"});assertEquals(32,w.fixtures.count{it.competition==CompetitionType.NATIONAL_CUP&&it.group=="DFB_POKAL"&&it.round==1});assertTrue(w.fixtures.any{it.competition==CompetitionType.NATIONAL_CUP&&it.group=="HRVATSKI_KUP"});assertTrue(w.fixtures.any{it.competition==CompetitionType.NATIONAL_CUP&&it.group=="RUSSIAN_CUP"})
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertTrue(copy.privateTopClubMode);assertEquals(33,copy.leagues.size);assertEquals("Leon Test",copy.self().name)
 }
 @Test fun realModeDatasetHasExpectedLeagueAndSquadShape(){
  assertEquals(listOf(18,18,20,18,18,20,20,20,18),RealModeDatabase.leagues.map{it.clubs.size})
  assertEquals(170,RealModeDatabase.options.size);assertEquals(170,RealModeDatabase.options.map{it.key}.toSet().size)
  assertTrue(RealModeDatabase.options.all{it.players.isEmpty()||it.players.size in 18..40})
  assertTrue(RealModeDatabase.options.all{club->club.players.map{it.name}.toSet().size==club.players.size})
  assertEquals(listOf("Bundesliga","2. Bundesliga","3. Liga","Regionalliga Nord","Oberliga Hamburg"),RealModeDatabase.leaguesForCountry("Deutschland").map{it.name})
 }
 @Test fun realModeUsesCurrentSquadsCanonicalCodesAndDetailedPositions(){
  val bayern=RealModeDatabase.options.first{it.name=="FC Bayern München"}
  assertEquals("FCB",bayern.shortName)
  assertEquals("B04",RealModeDatabase.options.first{it.name=="Bayer 04 Leverkusen"}.shortName)
  assertEquals("BSC",RealModeDatabase.options.first{it.name=="Hertha BSC"}.shortName)
  assertEquals("FCN",RealModeDatabase.options.first{it.name=="1. FC Nürnberg"}.shortName)
  assertEquals("HEBC",RealModeDatabase.options.first{it.name=="HEBC Hamburg"}.shortName)
  assertEquals("AFC",RealModeDatabase.options.first{it.name=="Altona 93"}.shortName)
  assertTrue("Nathaniel Brown" in bayern.players.map{it.name})
  assertTrue("Ismael Saibari" in bayern.players.map{it.name})
  assertTrue("Nicolas Jackson" !in bayern.players.map{it.name})
  assertEquals(Position.TW,bayern.players.first{it.name=="Manuel Neuer"}.position)
  assertEquals(Position.ST,bayern.players.first{it.name=="Harry Kane"}.position)
  assertEquals(Position.DM,bayern.players.first{it.name=="Joshua Kimmich"}.position)
  assertEquals(Position.LV,bayern.players.first{it.name=="Alphonso Davies"}.position)
  assertEquals(Position.OM,bayern.players.first{it.name=="Jamal Musiala"}.position)
  assertEquals(Position.RA,bayern.players.first{it.name=="Michael Olise"}.position)
 }
 @Test fun oberligaTakeoverAndCustomClubStartArePlayable(){
  val oberliga=RealModeDatabase.leagues.first{it.name=="Oberliga Hamburg"};assertEquals(18,oberliga.clubs.size)
  val slotClub=oberliga.clubs.first{it.name=="TuS Dassendorf"}
  val me=PlayerDraft(firstName="Leon",lastName="Stark",number=10,position=Position.ST,attributes=Attributes(72,76,68,73,45,66,74,70,70,10,68))
  val takeover=WorldFactory.createRealModeWorld(12401,slotClub.key,me)
  assertEquals("Oberliga Hamburg",WorldFactory.leagueName(takeover,takeover.club().tier));assertTrue(takeover.squad().count{!it.youth}>=20)
  val draft=ClubDraft(name="FC Test Hamburg",shortName="FCTH",city="Hamburg",founded=2026,capacity=2500,difficulty=Difficulty.SANDBOX)
  val roster=List(19){i->PlayerDraft(firstName="Spieler",lastName="${i+2}",number=i+2,position=if(i==0)Position.TW else Position.entries[(i%(Position.entries.size-1))+1],attributes=Attributes(55,55,55,55,55,55,55,55,55,if(i==0)62 else 10,55))}
  val custom=WorldFactory.createCustomClubWorld(12402,slotClub.key,draft,me,roster)
  assertEquals("FC Test Hamburg",custom.club().name);assertEquals("FCTH",custom.club().shortName);assertEquals("Oberliga Hamburg",WorldFactory.leagueName(custom,custom.club().tier));assertEquals(20,custom.squad().count{!it.youth})
 }

 @Test fun realModeUefaUsesModernLeaguePhaseAndDfbHas64GermanClubs(){
  val selected=RealModeDatabase.leagues.first().clubs.first()
  val w=WorldFactory.createRealModeWorld(9458L,selected.key,PlayerDraft(firstName="UEFA",lastName="Probe",number=71,position=Position.ZM))
  fun field(type: CompetitionType)=w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"}.flatMap{listOf(it.homeId,it.awayId)}.toSet()
  val champions=field(CompetitionType.CHAMPIONS_LEAGUE);val europa=field(CompetitionType.EUROPA_LEAGUE)
  assertEquals(36,champions.size);assertEquals(36,europa.size);assertTrue(champions.intersect(europa).isEmpty())
  for(type in listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE)){
   val phase=w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"};assertEquals(144,phase.size);assertTrue(phase.all{it.group.isBlank()})
   for(id in field(type)){val games=phase.filter{it.homeId==id||it.awayId==id};assertEquals(8,games.size);assertEquals(4,games.count{it.homeId==id});assertEquals(4,games.count{it.awayId==id});assertEquals(8,games.map{if(it.homeId==id)it.awayId else it.homeId}.toSet().size)}
  }
  val cup=w.fixtures.filter{it.competition==CompetitionType.NATIONAL_CUP&&it.group=="DFB_POKAL"&&it.round==1};assertEquals(32,cup.size);val cupTeams=cup.flatMap{listOf(it.homeId,it.awayId)}.toSet();assertEquals(64,cupTeams.size)
  assertTrue(cupTeams.all{id->val tier=w.clubs.getValue(id).tier;WorldFactory.leagueCountry(w,tier)=="Deutschland"})
  assertEquals("Deutschland",WorldFactory.leagueCountry(w,1));assertEquals("England",WorldFactory.leagueCountry(w,6));assertEquals("DFB-Pokal",CompetitionEngine.displayName(w,CompetitionType.NATIONAL_CUP))
 }
 @Test fun modernUefaAdvancesThroughPlayoffsTwoLeggedRoundsAndFinal(){
  val selected=RealModeDatabase.leagues.first().clubs.first();val w=WorldFactory.createRealModeWorld(9461L,selected.key,PlayerDraft(firstName="Euro",lastName="Test",number=72,position=Position.ZM))
  val type=CompetitionType.CHAMPIONS_LEAGUE
  fun play(fixtures: List<Fixture>){for(f in fixtures.filter{!it.played}){val m=MatchEngine.simulateFullMatch(w,f);MatchEngine.record(w,m);CompetitionEngine.afterRecorded(w,f)}}
  play(w.fixtures.filter{it.competition==type&&it.stage=="Ligaphase"}.toList())
  assertEquals(8,w.fixtures.count{it.competition==type&&it.round==9});assertEquals(8,w.fixtures.count{it.competition==type&&it.round==10})
  assertTrue(w.fixtures.filter{it.competition==type&&it.round==9}.all{it.stage=="Play-offs · Hinspiel"})
  play(w.fixtures.filter{it.competition==type&&it.round in 9..10}.toList());assertEquals(8,w.fixtures.count{it.competition==type&&it.round==11});assertEquals(8,w.fixtures.count{it.competition==type&&it.round==12})
  play(w.fixtures.filter{it.competition==type&&it.round in 11..12}.toList());assertEquals(4,w.fixtures.count{it.competition==type&&it.round==13});assertEquals(4,w.fixtures.count{it.competition==type&&it.round==14})
  play(w.fixtures.filter{it.competition==type&&it.round in 13..14}.toList());assertEquals(2,w.fixtures.count{it.competition==type&&it.round==15});assertEquals(2,w.fixtures.count{it.competition==type&&it.round==16})
  play(w.fixtures.filter{it.competition==type&&it.round in 15..16}.toList());val final=w.fixtures.single{it.competition==type&&it.round==17};assertEquals("Finale",final.stage);assertEquals(34,final.matchday)
  play(listOf(final));assertTrue(final.played);assertTrue(final.winnerId!=0)
 }
 @Test fun realModeCanStartAcrossRepresentativeExpandedLeaguesAndUsesDynamicLeagueLength(){
  val sampleTiers=listOf(1,5,10,15,16,22,23,31,32,33)
  for((index,tier) in sampleTiers.withIndex()){
   val league=RealModeDatabase.leagueForTier(tier);val selected=league.clubs.first();val w=WorldFactory.createRealModeWorld(92000L+index,selected.key,PlayerDraft(firstName="Alex",lastName="Manager",number=71,position=Position.ZM))
   assertEquals(selected.name,w.club().name);assertEquals(league.name,WorldFactory.leagueName(w,w.club().tier));assertEquals((league.clubs.size-1)*2,w.fixtures.count{it.competition==CompetitionType.LEAGUE&&(it.homeId==w.user.clubId||it.awayId==w.user.clubId)})
  }
 }
 @Test fun sandboxCreatesEditableTwentyPlayerSquad(){val club=ClubDraft(city="Teststadt",difficulty=Difficulty.SANDBOX);val self=PlayerDraft(firstName="Alex",lastName="Coach",position=Position.ST,attributes=Attributes(80,88,75,82,40,70,78,80,72,8,76));val extras=(0 until 19).map{i->PlayerDraft(firstName="Kader",lastName="${i+2}",number=i+2,position=Position.entries[i%Position.entries.size],attributes=Attributes(60+i%5,61,62,63,64,65,66,67,68,if(i%10==0)75 else 10,69))};val w=WorldFactory.createWorld(91,club,self,extras);assertEquals(Difficulty.SANDBOX,w.user.difficulty);assertEquals(24,w.squad().size);assertEquals(88,w.self().attributes.finishing);assertEquals(20,w.squad().count{!it.youth});assertEquals(20,w.squad().filter{!it.youth}.map{it.name}.toSet().size)}
 @Test fun generatedOpponentNamesAreBroadlyRandomInsteadOfMirroringOneSmallPool(){
  val w=WorldFactory.createWorld(5656,ClubDraft(city="Teststadt",difficulty=Difficulty.SANDBOX),PlayerDraft(firstName="Leon",lastName="Stark"))
  val opponents=w.players.values.filter{it.clubId in w.leagues.flatMap{l->l.clubIds}&&it.clubId!=w.user.clubId}
  val uniqueLast=opponents.map{it.lastName}.toSet().size;val mostCommon=opponents.groupingBy{it.lastName}.eachCount().maxOf{it.value}
  assertTrue(uniqueLast>=90,"unique surnames=$uniqueLast");assertTrue(mostCommon<55,"same surname occurs $mostCommon times")
 }
 @Test fun generatedClubAbbreviationsAreNameBasedAndNeverOldDigitCodes(){
  val w=WorldFactory.createWorld(5757);val clubs=w.leagues.flatMap{it.clubIds}.map{w.clubs.getValue(it)}
  assertTrue(clubs.all{it.shortName.length in 2..4&&!it.shortName.any(Char::isDigit)},clubs.joinToString{it.shortName})
  assertTrue(clubs.filter{it.id!=w.user.clubId}.all{it.shortName.take(2) in setOf("SV","TS","FC","SP","SC","VL")})
 }
 @Test fun fantasyCupAndEuropaEliteLeagueHaveRealStructuresAndSurviveSave(){
  val w=WorldFactory.createWorld(5858);assertEquals(20,w.clubs.values.count{it.tier==0});assertEquals(32,w.fixtures.count{it.competition==CompetitionType.NATIONAL_CUP&&it.round==1});assertEquals(8,w.fixtures.filter{it.competition==CompetitionType.EURO_ELITE&&it.stage=="Ligaphase"}.map{it.group}.toSet().size)
  val copy=SaveCodec.decode(SaveCodec.encode(w));assertEquals(w.fixtures.count{it.competition!=CompetitionType.LEAGUE},copy.fixtures.count{it.competition!=CompetitionType.LEAGUE});assertEquals(4,CompetitionEngine.groupTable(copy,"A").size)
 }

 @Test fun fullMatchEndsAfter90WithValidScore(){val w=WorldFactory.createWorld(6);val m=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);assertTrue(m.finished&&m.minute>=90);assertTrue(m.home.goals in 0..m.home.shots);assertTrue(m.away.goals in 0..m.away.shots);assertEquals(m.home.goals+m.away.goals,m.goals.size)}
 @Test fun completedUserMatchesAdvanceWeekAndAllLeagues(){val w=WorldFactory.createWorld(77);var guard=0;while(w.calendar.matchday==1&&guard++<4){w.live=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);SeasonEngine.advanceWeek(w)};assertEquals(2,w.calendar.matchday);assertEquals(60,w.fixtures.count{it.competition==CompetitionType.LEAGUE&&it.played});assertTrue(w.fixtures.count{it.competition==CompetitionType.NATIONAL_CUP&&it.played}>=32);assertNull(w.live);assertFailsWith<IllegalStateException>{SeasonEngine.advanceWeek(w)}}
 @Test fun saveKillLoadPreservesBudgetXiWeekAndConstruction(){
  val dir=Files.createTempDirectory("gruenderelf-process").toFile();val java=System.getProperty("java.home")+"/bin/java";val cp=System.getProperty("gruenderelf.test.classpath")
  fun process(mode: String): String{val p=ProcessBuilder(java,"-cp",cp,"de.gruenderelf.engine.SaveProcessProbe",mode,dir.absolutePath).redirectErrorStream(true).start();val text=p.inputStream.bufferedReader().readText();assertEquals(0,p.waitFor(),text);return text}
  process("write");assertContains(process("read"),"PROCESS_RELOAD_OK");dir.deleteRecursively()
 }
 @Test fun trainingChangesFitnessOrAttributes(){val w=WorldFactory.createWorld(100);TrainingEngine.preset(w,"Aufbau");val before=w.squad().associate{it.id to (it.fitness to it.attributes.copy())};TrainingEngine.apply(w);assertTrue(w.squad().any{before[it.id]!!.first!=it.fitness||before[it.id]!!.second!=it.attributes});assertEquals(3,TrainingEngine.effectiveDays(w).size)}
 @Test fun floodlightsBecomeFunctionalAfterConstructionDuration(){val w=WorldFactory.createWorld(30);w.club().budget=50000;ConstructionEngine.start(w,Facility.FLOODLIGHTS);repeat(w.construction.single().weeksLeft-1){ConstructionEngine.advance(w)};assertFalse(w.club().stadium.floodlights);ConstructionEngine.advance(w);assertTrue(w.club().stadium.floodlights&&w.construction.isEmpty());assertNotNull(ConstructionEngine.reason(w,Facility.FLOODLIGHTS))}
 @Test fun seasonTransitionsPreservePyramid(){val w=WorldFactory.createWorld(919);var guard=0;while(w.calendar.season==2026&&guard++<50){w.live=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);SeasonEngine.advanceWeek(w)};assertTrue(guard<50);assertEquals(2027,w.calendar.season);assertEquals(1,w.calendar.matchday);assertEquals(1,w.history.size);assertTrue(w.leagues.all{it.clubIds.size==12});assertTrue(w.fixtures.none{it.played});assertEquals(w.calendar,SaveCodec.decode(SaveCodec.encode(w)).calendar)}
 @Test fun midMatchRoundtripKeepsDeterministicFuture(){val a=WorldFactory.createWorld(2026);a.live=MatchEngine.start(a);repeat(20){val m=a.live!!;when{m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==a.user.clubId)MatchEngine.substitutionSuggestions(a,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(a,m,it.outId,it.inId)};MatchEngine.resumeIncident(a,m)};m.pendingDecision->MatchEngine.decide(a,m,Decision.PASS);else->MatchEngine.step(a,m)}};val b=SaveCodec.copy(a);fun finish(w: World){val m=w.live!!;while(!m.finished){when{m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.pendingDecision->MatchEngine.decide(w,m,Decision.SHOOT);m.halfTime->MatchEngine.secondHalf(m);else->MatchEngine.step(w,m)}}};finish(a);finish(b);assertEquals(SaveCodec.encode(a),SaveCodec.encode(b))}
 @Test fun versionOneMigratesAndFutureVersionIsRejected(){val text=SaveCodec.encode(WorldFactory.createWorld(5));assertEquals(SAVE_VERSION,SaveCodec.decode(text.replace("\"saveVersion\":$SAVE_VERSION","\"saveVersion\":1")).saveVersion);assertFailsWith<IllegalArgumentException>{SaveCodec.decode(text.replace("\"saveVersion\":$SAVE_VERSION","\"saveVersion\":999"))}}
 @Test fun liveFormationChangeKeepsPlayersAndUpdatesTactics(){val w=WorldFactory.createWorld(303);val m=MatchEngine.start(w);val home=w.user.clubId==m.homeId;val before=(if(home)m.homeXi else m.awayXi).count{it!=0};MatchEngine.changeFormation(w,m,w.user.clubId,"4-2-3-1");assertEquals("4-2-3-1",if(home)m.homeFormation else m.awayFormation);assertEquals("4-2-3-1",w.club().tactics.formation);assertEquals(before,(if(home)m.homeXi else m.awayXi).count{it!=0});assertEquals((if(home)m.homeXi else m.awayXi).filter{it!=0}.size,(if(home)m.homeXi else m.awayXi).filter{it!=0}.toSet().size)}
 @Test fun injuryPauseRequiresReplacementAndClockReallyStops(){val w=WorldFactory.createWorld(404);val m=MatchEngine.start(w);val home=w.user.clubId==m.homeId;val xi=if(home)m.homeXi else m.awayXi;val bench=if(home)m.homeBench else m.awayBench;val hurt=xi.first{it!=0&&w.players.getValue(it).position!=Position.TW};m.injured.add(hurt);m.incidentPause=true;m.incidentReason=MatchPauseReason.INJURY;m.incidentPlayerId=hurt;m.incidentClubId=w.user.clubId;val minute=m.minute;MatchEngine.step(w,m);assertEquals(minute,m.minute);assertFailsWith<IllegalArgumentException>{MatchEngine.resumeIncident(w,m)};val replacement=bench.first{w.players.getValue(it).available};MatchEngine.substitute(w,m,hurt,replacement);MatchEngine.resumeIncident(w,m);assertFalse(m.incidentPause);MatchEngine.step(w,m);assertEquals(minute+1,m.minute)}
 @Test fun liveFieldProducesBallPositionAndMomentum(){val w=WorldFactory.createWorld(505);val m=MatchEngine.start(w);repeat(12){if(m.incidentPause){if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)}else if(m.pendingDecision)MatchEngine.decide(w,m,Decision.PASS)else MatchEngine.step(w,m)};assertTrue(m.ballX in 0.04f..0.96f);assertTrue(m.ballY in 0.03f..0.97f);assertTrue(m.momentumHistory.isNotEmpty());assertTrue(m.momentumHistory.all{it in -100..100})}

 @Test fun shotStatisticsSeparateOnOffAndBlocked(){
  val w=WorldFactory.createWorld(606);val m=MatchEngine.simulateFullMatch(w,w.nextFixture()!!)
  for(stats in listOf(m.home,m.away)){
   assertEquals(stats.shots,stats.shotsOnTarget+stats.shotsOffTarget+stats.blockedShots)
   assertTrue(stats.goals<=stats.shotsOnTarget)
   assertTrue(stats.shotsOnTarget>=0&&stats.shotsOffTarget>=0&&stats.blockedShots>=0)
  }
 }
 @Test fun shortAppearanceRatingStaysNeutralUnlessSomethingImportantHappens(){
  val w=WorldFactory.createWorld(707);val m=MatchEngine.start(w);val id=(if(w.user.clubId==m.homeId)m.homeXi else m.awayXi).first{it!=0};val p=w.players.getValue(id)
  m.minutesPlayed[id]=8;m.playerPerformance[id]=PlayerMatchPerformance(minutes=8,fitnessStart=p.fitness)
  val neutral=MatchEngine.calculatePlayerRating(w,m,id);assertTrue(neutral in 6.3..6.7,"neutral=$neutral")
  m.playerPerformance.getValue(id).goals=1
  val decisive=MatchEngine.calculatePlayerRating(w,m,id);assertTrue(decisive>neutral+.4,"neutral=$neutral decisive=$decisive")
 }
 @Test fun conserveEnergyReducesFitnessCostAndActuallyChangesTactic(){
  fun run(seed: Long,conserve: Boolean): Pair<Double,LiveMatch>{
   val w=WorldFactory.createWorld(seed);val m=MatchEngine.start(w);val ownHome=w.user.clubId==m.homeId;val ids=(if(ownHome)m.homeXi else m.awayXi).filter{it!=0};val before=ids.associateWith{w.players.getValue(it).fitness}
   if(conserve)MatchEngine.setConserveEnergy(w,m,w.user.clubId,true)
   var guard=0;while(m.minute<35&&!m.finished&&guard++<300){when{m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.pendingDecision->MatchEngine.decide(w,m,Decision.HOLD);m.halfTime->MatchEngine.secondHalf(m);else->MatchEngine.step(w,m)}}
   val loss=ids.map{(before.getValue(it)-w.players.getValue(it).fitness).coerceAtLeast(0.0)}.average();return loss to m
  }
  val normal=run(808,false);val saved=run(808,true);assertTrue(saved.second.homeConserveEnergy||saved.second.awayConserveEnergy);assertTrue(saved.first<normal.first*.82,"normal=${normal.first} saved=${saved.first}")
 }

 @Test fun liveMatchPlansAreMutuallyExclusiveAndPersist(){
  val w=WorldFactory.createWorld(8181);val m=MatchEngine.start(w);w.live=m;val home=w.user.clubId==m.homeId
  MatchEngine.setConserveEnergy(w,m,w.user.clubId,true);assertTrue(if(home)m.homeConserveEnergy else m.awayConserveEnergy)
  MatchEngine.setAllOutAttack(w,m,w.user.clubId,true);assertTrue(if(home)m.homeAllOutAttack else m.awayAllOutAttack);assertFalse(if(home)m.homeConserveEnergy else m.awayConserveEnergy)
  MatchEngine.setControlGame(w,m,w.user.clubId,true);assertTrue(if(home)m.homeControlGame else m.awayControlGame);assertFalse(if(home)m.homeAllOutAttack else m.awayAllOutAttack)
  val copy=SaveCodec.decode(SaveCodec.encode(w));val cm=copy.live!!;assertTrue(if(home)cm.homeControlGame else cm.awayControlGame)
 }

 @Test fun cornerCannotJumpToOtherTeamWithoutPossessionChange(){
  val w=WorldFactory.createWorld(909);val m=MatchEngine.start(w);var guard=0;var cornersSeen=0
  while(!m.finished&&guard++<700){
   val previousOwner=m.chainOwnerClubId;val previousChange=m.lastPossessionChangeEventSerial
   when{m.incidentPause->{if(m.incidentReason==MatchPauseReason.INJURY&&m.incidentClubId==w.user.clubId)MatchEngine.substitutionSuggestions(w,m).firstOrNull{it.outId==m.incidentPlayerId}?.let{MatchEngine.substitute(w,m,it.outId,it.inId)};MatchEngine.resumeIncident(w,m)};m.pendingDecision->MatchEngine.decide(w,m,Decision.SHOOT);m.halfTime->MatchEngine.secondHalf(m);else->MatchEngine.step(w,m)}
   if(m.livePhase==LivePhase.CORNER){cornersSeen++;assertEquals(m.chainOwnerClubId,m.liveClubId);if(m.liveClubId!=previousOwner)assertTrue(m.lastPossessionChangeEventSerial>previousChange,"Ecke wechselte ohne dokumentierten Besitzwechsel")}
  }
  assertTrue(cornersSeen>0,"Testlauf erzeugte keine Ecke")
 }

 @Test fun squadRolesPersistThroughSaveRoundtrip(){
  val w=WorldFactory.createWorld(18181);val ids=w.club().tactics.xi.filter{it!=0};assertTrue(ids.size>=6)
  w.club().tactics.apply{captainId=ids[0];targetPlayerId=ids[1];penaltyTakerId=ids[2];freeKickTakerId=ids[3];cornerLeftTakerId=ids[4];cornerRightTakerId=ids[5]}
  val c=SaveCodec.decode(SaveCodec.encode(w)).club().tactics
  assertEquals(ids[0],c.captainId);assertEquals(ids[1],c.targetPlayerId);assertEquals(ids[2],c.penaltyTakerId);assertEquals(ids[3],c.freeKickTakerId);assertEquals(ids[4],c.cornerLeftTakerId);assertEquals(ids[5],c.cornerRightTakerId)
 }

 @Test fun amateurCalibrationAcross600Matches(){
  var xg=0.0;var goals=0;var blowouts=0;val w=WorldFactory.createWorld(123);val f=w.fixtures.first{it.tier==10&&it.homeId!=w.user.clubId&&it.awayId!=w.user.clubId}
  for(id in listOf(f.homeId,f.awayId)){w.clubs.getValue(id).tactics=Tactics();w.clubs.getValue(id).stadium.pitchQuality=40;for(p in w.squad(id)){p.attributes=Attributes(35,35,35,35,35,35,35,35,35,35,35);p.hidden=Hidden();p.form=6.5;p.morale=65;p.sharpness=60}}
  repeat(600){i->(w.squad(f.homeId)+w.squad(f.awayId)).forEach{it.fitness=100.0;it.injuryWeeks=0;it.unavailableWeeks=0};val m=MatchEngine.simulateFullMatch(w,f.copy(id=10000+i));xg+=m.home.xg+m.away.xg;goals+=m.home.goals+m.away.goals;if(m.home.goals>=7||m.away.goals>=7)blowouts++}
  val mean=xg/1200;println("CALIBRATION: xG/team=$mean goals/team=${goals/1200.0} seven-plus=$blowouts/600");assertTrue(mean in 1.10..1.55,"xG/team=$mean");assertTrue(blowouts<12)
 }
 @Test fun relatedPositionsUseSoftPenaltyAndExposeSecondaryOptions(){
  val p=Player(999,position=Position.DM,attributes=Attributes(82,74,88,84,87,83,88,87,75,9,80))
  val dm=p.ratingAt(Position.DM);val zm=p.ratingAt(Position.ZM)
  assertTrue(dm-zm<=8,"DM -> ZM darf nicht hart abstürzen: $dm -> $zm")
  assertTrue(Position.ZM in p.secondaryOptions(),"ZM sollte als natürliche Nebenposition eines DM angeboten werden")
  assertTrue(p.fit(Position.ZM)>=.95,"Positionsnähe DM/ZM zu niedrig: ${p.fit(Position.ZM)}")
 }
}
object SaveProcessProbe {
 @JvmStatic fun main(args: Array<String>){
  val file=java.io.File(args[1],"world.gruenderelf");val expected=java.io.File(args[1],"expected.txt")
  if(args[0]=="write"){val w=WorldFactory.createWorld(4545);w.club().budget=50000;ConstructionEngine.start(w,Facility.FLOODLIGHTS);w.live=MatchEngine.simulateFullMatch(w,w.nextFixture()!!);SeasonEngine.advanceWeek(w);java.io.FileOutputStream(file).use{it.write(SaveCodec.encode(w).toByteArray());it.fd.sync()};expected.writeText("${w.club().budget}|${w.club().tactics.xi}|${w.calendar.matchday}|${w.construction.single()}")}
  else{val w=SaveCodec.decode(file.readText());check(expected.readText()=="${w.club().budget}|${w.club().tactics.xi}|${w.calendar.matchday}|${w.construction.single()}");println("PROCESS_RELOAD_OK")}
 }
}

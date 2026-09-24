package de.gruenderelf.engine

import org.junit.Test
import kotlin.test.*

class V0514EconomyPotentialTest {
 @Test fun withdrawnNegotiationReleasesReservedBudgetImmediately(){
  val w=WorldFactory.createWorld(51401L);val buyer=w.club();buyer.budget=900_000_000L
  val targets=w.players.values.filter{!it.retired&&!it.youth&&it.clubId!=0&&it.clubId!=buyer.id}.take(2);assertEquals(2,targets.size)
  fun ready(p:Player):TransferOffer{p.hidden.injuryProneness=0;p.injuryWeeks=0;val o=TransferEngine.createOffer(w,buyer.id,p.id,DealType.BUY,SquadRole.STAR);o.fee=TransferEngine.askingPrice(w,w.clubs[p.clubId],p,DealType.BUY)*3;o.wage=maxOf(p.wage*12,8_000);o.signingBonus=1_000_000;o.playingTimePromise=100;o.sellOnPercent=25;TransferEngine.evaluate(w,o);assertEquals(TransferStage.MEDICAL,o.stage);TransferEngine.advanceProcess(w,o.id);assertTrue(o.registrationReady);return o}
  val first=ready(targets[0]);val firstCost=first.fee+first.signingBonus
  val secondProbe=TransferEngine.createOffer(w,buyer.id,targets[1].id,DealType.BUY,SquadRole.STAR);secondProbe.fee=TransferEngine.askingPrice(w,w.clubs[targets[1].clubId],targets[1],DealType.BUY)*3;secondProbe.wage=maxOf(targets[1].wage*12,8_000);secondProbe.signingBonus=1_000_000;secondProbe.playingTimePromise=100;secondProbe.sellOnPercent=25
  buyer.budget=firstCost+secondProbe.fee+secondProbe.signingBonus-1;secondProbe.stage=TransferStage.CLUB;secondProbe.status=NegotiationStatus.DRAFT;TransferEngine.evaluate(w,secondProbe);assertEquals(NegotiationStatus.REJECTED,secondProbe.status)
  TransferEngine.withdraw(w,first.id);assertEquals(NegotiationStatus.WITHDRAWN,first.status);assertFalse(first.registrationReady);assertTrue(first.message.contains("freigegeben"))
  val second=TransferEngine.createOffer(w,buyer.id,targets[1].id,DealType.BUY,SquadRole.STAR);second.fee=secondProbe.fee;second.wage=secondProbe.wage;second.signingBonus=secondProbe.signingBonus;second.playingTimePromise=100;second.sellOnPercent=25;second.stage=TransferStage.CLUB;second.status=NegotiationStatus.DRAFT;TransferEngine.evaluate(w,second);assertNotEquals(NegotiationStatus.REJECTED,second.status,"Nach Rücktritt muss das zuvor reservierte Geld sofort wieder für neue Verhandlungen verfügbar sein.")
 }

 @Test fun leagueCupAndChampionsLeagueTitlesPayExtraPrizeMoneyExactlyOnce(){
  val selected=RealModeDatabase.leagues.first().clubs.first();val w=WorldFactory.createRealModeWorld(51402L,selected.key,PlayerDraft(firstName="Preis",lastName="Test",number=71,position=Position.ZM));val user=w.user.clubId
  val tables=w.leagues.associate{league->league.tier to league.clubIds.map{TableRow(it)}.sortedByDescending{if(it.clubId==user)1 else 0}}
  val cup=Fixture(w.nextIds.fixture++,w.calendar.season,w.club().tier,40,user,w.leagues.first().clubIds.first{it!=user},true,CompetitionType.NATIONAL_CUP,6,"Finale",winnerId=user)
  val cl=Fixture(w.nextIds.fixture++,w.calendar.season,w.club().tier,41,user,w.clubs.keys.first{it!=user},true,CompetitionType.CHAMPIONS_LEAGUE,17,"Finale",winnerId=user)
  w.fixtures.add(cup);w.fixtures.add(cl);val before=w.club().budget
  val result=CompetitionPrizeSystem.awardSeasonTitles(w,tables);val expected=CompetitionPrizeSystem.leagueTitlePrize(w,w.club().tier)+CompetitionPrizeSystem.nationalCupPrize(w)+CompetitionPrizeSystem.championsLeaguePrize(w)
  assertEquals(expected,result.userTotal);assertEquals(before+expected,w.club().budget);assertTrue(result.userAwards.any{it.contains("Meister")});assertTrue("DFB-Pokalsieger" in result.userAwards);assertTrue("Champions-League-Sieger" in result.userAwards)
  val after=w.club().budget;val duplicate=CompetitionPrizeSystem.awardSeasonTitles(w,tables);assertEquals(0,duplicate.userTotal);assertEquals(after,w.club().budget,"Titelprämien dürfen nicht doppelt ausgezahlt werden.")
 }

 @Test fun internationalCupTitlesPayConfiguredFinalPrizesExactlyOnce(){
  val selected=RealModeDatabase.leagues.first().clubs.first();val w=WorldFactory.createRealModeWorld(51404L,selected.key,PlayerDraft(firstName="International",lastName="Preis",number=72,position=Position.ZM));val user=w.user.clubId
  val tables=w.leagues.associate{league->league.tier to league.clubIds.map{TableRow(it)}.sortedBy{if(it.clubId==user)1 else 0}}
  val opponent=w.clubs.keys.first{it!=user}
  fun addFinal(type:CompetitionType,round:Int,day:Int){w.fixtures.add(Fixture(id=w.nextIds.fixture++,season=w.calendar.season,tier=w.club().tier,matchday=day,homeId=user,awayId=opponent,played=true,competition=type,round=round,stage="Finale",winnerId=user))}
  addFinal(CompetitionType.EUROPA_LEAGUE,17,40);addFinal(CompetitionType.EURO_ELITE,10,41);addFinal(CompetitionType.CLUB_WORLD_CUP,7,42);addFinal(CompetitionType.ETERNAL_CROWN,9,43)
  val before=w.club().budget;val result=CompetitionPrizeSystem.awardSeasonTitles(w,tables)
  val expected=CompetitionPrizeSystem.europaLeaguePrize(w)*2+CompetitionPrizeSystem.clubWorldCupPrize(w)+CompetitionPrizeSystem.eternalCrownPrize(w)
  assertEquals(expected,result.userTotal);assertEquals(before+expected,w.club().budget)
  assertTrue("Europa-League-Sieger" in result.userAwards);assertTrue("Europa-Eliteliga-Sieger" in result.userAwards);assertTrue("Club-World-Cup-Sieger" in result.userAwards);assertTrue("Sieger der Krone der Kontinente" in result.userAwards)
  val after=w.club().budget;assertEquals(0,CompetitionPrizeSystem.awardSeasonTitles(w,tables).userTotal);assertEquals(after,w.club().budget)
 }

 @Test fun targetedPotentialTrainingRaisesCeilingForYouthAndSenior(){
  val w=WorldFactory.createWorld(51403L);w.club().budget=100_000_000L;w.club().stadium.training=100;w.club().stadium.gym=100;w.club().stadium.youth=100;w.club().stadium.medicine=100;w.club().dynamics.staffQuality=100
  val players=listOf(w.squad().first{it.youth},w.squad().first{!it.youth&&!it.retired&&it.id!=w.user.playerId})
  for((index,p) in players.withIndex()){
   p.hidden.potential=p.ca.coerceAtMost(98);p.hidden.development=90;p.hidden.injuryProneness=0;p.injuryWeeks=0
   assertNotNull(IntensiveTrainingSystem.reason(w,p.id),"Normales Attributtraining soll an der bisherigen Entwicklungsgrenze stoppen.")
   assertNull(IntensiveTrainingSystem.potentialReason(w,p.id));val before=p.hidden.potential;IntensiveTrainingSystem.startPotential(w,p.id,Focus.TECHNIQUE);val project=w.intensiveTraining.first{it.playerId==p.id};assertTrue(project.raisesPotential);project.weeksLeft=1;IntensiveTrainingSystem.applyWeek(w,w.club(),p,TrainingReport(),SeededRandom(514030L+index));assertTrue(p.hidden.potential>before,"Potenzialtraining muss die Grenze für Jugend- und Profispieler tatsächlich anheben.");assertTrue(p.hidden.potential<=99)
  }
 }
}

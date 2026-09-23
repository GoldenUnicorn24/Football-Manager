package de.gruenderelf.engine

/**
 * Competition / league-specific match calibration restored from the shipped v0.5.18 APK.
 * The original tempo value is retained for compatibility even though v0.5.18's MatchEngine
 * did not consume it directly.
 */
object LeagueCalibration {
 fun calibration(w:World,m:LiveMatch):MatchCalibration {
  val fixture=w.fixtures.getOrNull(m.fixtureId-1)?.takeIf{it.id==m.fixtureId}?:w.fixtures.firstOrNull{it.id==m.fixtureId}?:return MatchCalibration()
  if(fixture.competition in setOf(CompetitionType.EURO_ELITE,CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE)){
   return MatchCalibration(1.06,1.02,.97,1.04)
  }
  val homeTier=w.clubs[fixture.homeId]?.tier;val awayTier=w.clubs[fixture.awayId]?.tier
  val league=w.leagues.firstOrNull{it.tier==homeTier||it.tier==awayTier}?:w.leagues.firstOrNull{fixture.homeId in it.clubIds||fixture.awayId in it.clubIds}?:return MatchCalibration()
  val country=RealModeDatabase.countryForLeague(league.name)
  val level=RealModeDatabase.levelForTier(league.tier)
  if(w.privateTopClubMode){
   return when{
    country=="Deutschland"&&level==1->MatchCalibration(1.15,1.10,1.02,1.08)
    country=="England"&&level==1->MatchCalibration(1.10,1.06,.99,1.08)
    country in setOf("Spanien","Italien","Frankreich")&&level==1->MatchCalibration(1.06,1.04,.96,1.02)
    level==2->MatchCalibration(1.03,.98,1.05,1.03)
    level>=3->MatchCalibration(.98,.94,1.12,1.04)
    else->MatchCalibration()
   }
  }
  val index=(league.tier-1).coerceAtLeast(0).toDouble()
  return MatchCalibration(
   chanceCreation=(1.03-.006*index).coerceAtLeast(.94),
   conversion=(1.01-.009*index).coerceAtLeast(.90),
   turnover=(1.0+.018*index).coerceAtMost(1.16),
   tempo=(1.0+.006*index).coerceAtMost(1.06)
  )
 }
}

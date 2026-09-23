package de.gruenderelf.engine

/**
 * Regel- und Kalibrierungsschicht für v0.5. Sie hält die MatchEngine frei von
 * hart codierten Wettbewerbsannahmen und macht Unterschiede zwischen Liga,
 * Pokal und UEFA-Wettbewerben explizit testbar.
 */
data class CompetitionRuleSet(
 val maxSubstitutions:Int=5,
 val substitutionWindows:Int=3,
 val halfTimeIsFreeWindow:Boolean=true,
 val extraTimeAdditionalSubstitution:Boolean=true,
 val varEnabled:Boolean=true,
 val goalkeeperControlSeconds:Int=8,
 val substitutionExitSeconds:Int=10
)

object CompetitionRulesEngine {
 fun forFixture(w:World,fixture:Fixture?):CompetitionRuleSet {
  if(fixture==null)return CompetitionRuleSet()
  val league=w.leagues.firstOrNull{fixture.homeId in it.clubIds || fixture.awayId in it.clubIds}
  val level=league?.let{RealModeDatabase.levelForTier(it.tier)}?:fixture.tier.coerceAtLeast(1)
  return when(fixture.competition){
   CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE->CompetitionRuleSet(varEnabled=true,extraTimeAdditionalSubstitution=true)
   CompetitionType.NATIONAL_CUP->CompetitionRuleSet(varEnabled=true,extraTimeAdditionalSubstitution=true)
   CompetitionType.LEAGUE->CompetitionRuleSet(varEnabled=!w.privateTopClubMode||level<=2,extraTimeAdditionalSubstitution=false)
   CompetitionType.EURO_ELITE->CompetitionRuleSet(varEnabled=true,extraTimeAdditionalSubstitution=true)
  }
 }
 fun forMatch(w:World,m:LiveMatch)=forFixture(w,w.fixtures.firstOrNull{it.id==m.fixtureId})
 fun maxSubs(w:World,m:LiveMatch):Int {
  val r=forMatch(w,m)
  return r.maxSubstitutions + if(m.period>=3&&m.knockout&&r.extraTimeAdditionalSubstitution)1 else 0
 }
 fun maxWindows(w:World,m:LiveMatch):Int {val r=forMatch(w,m);return r.substitutionWindows+if(m.period>=3&&m.knockout&&r.extraTimeAdditionalSubstitution)1 else 0}
 fun substitutionIssue(w:World,m:LiveMatch,home:Boolean):String? {
  val r=forMatch(w,m)
  val subs=if(home)m.homeSubs else m.awaySubs
  if(subs>=maxSubs(w,m))return "Das Auswechselkontingent ist ausgeschöpft."
  if(m.halfTime)return null
  val windows=if(home)m.homeSubWindows else m.awaySubWindows
  val lastMinute=if(home)m.lastHomeSubMinute else m.lastAwaySubMinute
  if(lastMinute==m.minute)return null // mehrere Wechsel in derselben Unterbrechung = ein Fenster
  val maxWindows=maxWindows(w,m)
  if(windows>=maxWindows)return "Die ${if(maxWindows==3)"drei" else maxWindows.toString()} Wechselgelegenheiten sind bereits verbraucht."
  return null
 }
 fun registerSubstitution(m:LiveMatch,home:Boolean){
  if(m.halfTime)return
  if(home){if(m.lastHomeSubMinute!=m.minute){m.homeSubWindows++;m.lastHomeSubMinute=m.minute}}
  else if(m.lastAwaySubMinute!=m.minute){m.awaySubWindows++;m.lastAwaySubMinute=m.minute}
 }
 fun goalkeeperViolation(seconds:Int,rules:CompetitionRuleSet=CompetitionRuleSet())=seconds>rules.goalkeeperControlSeconds
}

data class MatchCalibration(
 val chanceCreation:Double=1.0,
 val conversion:Double=1.0,
 val turnover:Double=1.0,
 val tempo:Double=1.0
)

object LeagueCalibration {
 fun forFixture(w:World,fixture:Fixture?):MatchCalibration {
  if(fixture==null)return MatchCalibration()
  if(fixture.competition in listOf(CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE,CompetitionType.EURO_ELITE))
   return MatchCalibration(chanceCreation=1.06,conversion=1.02,turnover=.97,tempo=1.04)
  val league=w.leagues.firstOrNull{fixture.homeId in it.clubIds || fixture.awayId in it.clubIds}?:return MatchCalibration()
  val country=RealModeDatabase.countryForLeague(league.name)
  val level=RealModeDatabase.levelForTier(league.tier)
  if(!w.privateTopClubMode){
   val lower=(league.tier-1).coerceAtLeast(0)
   return MatchCalibration(chanceCreation=(1.03-lower*.006).coerceAtLeast(.94),conversion=(1.01-lower*.009).coerceAtLeast(.90),turnover=(1.0+lower*.018).coerceAtMost(1.16),tempo=(1.0+lower*.006).coerceAtMost(1.06))
  }
  return when{
   country=="Deutschland"&&level==1->MatchCalibration(1.15,1.10,1.02,1.08)
   country=="England"&&level==1->MatchCalibration(1.10,1.06,.99,1.08)
   country in setOf("Spanien","Italien","Frankreich")&&level==1->MatchCalibration(1.06,1.04,.96,1.02)
   level==2->MatchCalibration(1.03,.98,1.05,1.03)
   level>=3->MatchCalibration(.98,.94,1.12,1.04)
   else->MatchCalibration()
  }
 }
 fun forMatch(w:World,m:LiveMatch)=forFixture(w,w.fixtures.firstOrNull{it.id==m.fixtureId})
}

package de.gruenderelf.engine

/** Einmalige Titelprämien. Schlüssel in World verhindern Doppelzahlungen bei wiederholten Abschlussprüfungen. */
object CompetitionPrizeSystem {
 data class Result(val userTotal:Long=0,val userAwards:List<String> = emptyList())

 fun leagueTitlePrize(w:World,tier:Int):Long {
  if(w.privateTopClubMode){
   return when(WorldFactory.leagueName(w,tier)){
    "Bundesliga","Premier League","La Liga","Serie A","Ligue 1"->20_000_000L
    "2. Bundesliga"->5_000_000L
    "3. Liga"->1_500_000L
    "Regionalliga Nord"->350_000L
    "Oberliga Hamburg"->150_000L
    else->1_000_000L
   }
  }
  return when(tier){1->10_000_000L;2->4_000_000L;3->1_600_000L;4->650_000L;5->280_000L;6->120_000L;7->50_000L;8->20_000L;9->8_000L;else->3_000L}
 }
 fun nationalCupPrize(w:World):Long=if(w.privateTopClubMode)6_000_000L else 500_000L
 fun championsLeaguePrize(w:World):Long=25_000_000L

 fun award(w:World,key:String,clubId:Int,amount:Long):Boolean=pay(w,key,clubId,amount)
 fun europaLeaguePrize(w:World):Long=12_000_000L
 fun clubWorldCupPrize(w:World):Long=40_000_000L
 fun eternalCrownPrize(w:World):Long=50_000_000L

 private fun pay(w:World,key:String,clubId:Int,amount:Long):Boolean{
  if(clubId==0||amount<=0||key in w.paidSeasonPrizes)return false
  val c=w.clubs[clubId]?:return false;c.budget+=amount;w.paidSeasonPrizes.add(key);return true
 }
 private fun finalWinner(w:World,type:CompetitionType):Int=w.fixtures.asSequence().filter{it.season==w.calendar.season&&it.competition==type&&it.stage=="Finale"&&it.played&&it.winnerId!=0}.maxByOrNull{it.round}?.winnerId?:0

 fun awardSeasonTitles(w:World,tables:Map<Int,List<TableRow>>):Result{
  val season=w.calendar.season;var userTotal=0L;val userAwards=mutableListOf<String>()
  for(league in w.leagues){val winner=tables[league.tier]?.firstOrNull()?.clubId?:continue;val amount=leagueTitlePrize(w,league.tier);if(pay(w,"$season:LEAGUE:${league.tier}:$winner",winner,amount)&&winner==w.user.clubId){userTotal+=amount;userAwards+="Meister der ${WorldFactory.leagueName(w,league.tier)}"}}
  val cupWinner=finalWinner(w,CompetitionType.NATIONAL_CUP);val cupAmount=nationalCupPrize(w);if(pay(w,"$season:NATIONAL_CUP:$cupWinner",cupWinner,cupAmount)&&cupWinner==w.user.clubId){userTotal+=cupAmount;userAwards+=if(w.privateTopClubMode)"DFB-Pokalsieger" else "${CompetitionEngine.displayName(w,CompetitionType.NATIONAL_CUP)}-Sieger"}
  val clWinner=finalWinner(w,CompetitionType.CHAMPIONS_LEAGUE);val clAmount=championsLeaguePrize(w);if(pay(w,"$season:CHAMPIONS_LEAGUE:$clWinner",clWinner,clAmount)&&clWinner==w.user.clubId){userTotal+=clAmount;userAwards+="Champions-League-Sieger"}
  if(userTotal>0)w.news("Titelprämien ausgezahlt","Für ${userAwards.joinToString(", ")} erhält der Verein insgesamt ${userTotal} € zusätzliches Preisgeld.","good")
  return Result(userTotal,userAwards)
 }
}

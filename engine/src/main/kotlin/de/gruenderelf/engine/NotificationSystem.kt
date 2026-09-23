package de.gruenderelf.engine

object NotificationSystem {
 private fun tiredIds(w:World)=w.squad().asSequence().filter{!it.youth&&it.fitness<65}.map{it.id}.toSet()
 private fun expiringIds(w:World)=w.squad().asSequence().filter{!it.youth&&it.id!=w.user.playerId&&it.contractYears<=1}.map{it.id}.toSet()
 private fun completedReportIds(w:World)=w.scoutReports.asSequence().filter{it.value.progress>=100}.map{it.key}.toSet()

 fun tiredCount(w:World):Int {
  val current=tiredIds(w);val seen=if(w.notifications.tiredSeenWeek==w.calendar.absoluteWeek)w.notifications.seenTiredPlayerIds else emptySet()
  return current.count{it !in seen}
 }
 fun contractCount(w:World):Int {
  val current=expiringIds(w);val seen=if(w.notifications.contractSeenSeason==w.calendar.season)w.notifications.seenExpiringPlayerIds else emptySet()
  return current.count{it !in seen}
 }
 fun completedScoutCount(w:World):Int=completedReportIds(w).count{it !in w.notifications.seenCompletedScoutReportIds}
 fun squadBadge(w:World)=tiredCount(w)+contractCount(w)
 fun moreBadge(w:World)=completedScoutCount(w)

 fun acknowledgeSquad(w:World){
  if(w.notifications.tiredSeenWeek!=w.calendar.absoluteWeek){w.notifications.tiredSeenWeek=w.calendar.absoluteWeek;w.notifications.seenTiredPlayerIds.clear()}
  w.notifications.seenTiredPlayerIds.addAll(tiredIds(w))
  if(w.notifications.contractSeenSeason!=w.calendar.season){w.notifications.contractSeenSeason=w.calendar.season;w.notifications.seenExpiringPlayerIds.clear()}
  w.notifications.seenExpiringPlayerIds.addAll(expiringIds(w))
 }
 fun acknowledgeScoutReports(w:World){w.notifications.seenCompletedScoutReportIds.addAll(completedReportIds(w));trim(w)}
 fun acknowledgeCurrentForMigration(w:World){acknowledgeSquad(w);acknowledgeScoutReports(w)}
 private fun trim(w:World){
  if(w.notifications.seenCompletedScoutReportIds.size>500){val valid=w.scoutReports.keys;w.notifications.seenCompletedScoutReportIds.retainAll(valid)}
 }
}

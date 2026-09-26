package de.gruenderelf.engine

import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BmwDeveloperSystems {
 private fun isBarcelona(c:Club)=c.name.contains("Barcelona",true)||c.shortName.equals("BAR",true)
 private fun isBayern(c:Club)=c.name.contains("Bayern",true)||c.shortName.equals("FCB",true)
 private fun infraAverage(c:Club)=(c.stadium.training+c.stadium.medicine+c.stadium.youth+c.stadium.gym+c.stadium.pitchQuality)/5.0

 private fun bmwProfile(id:Int)=ClubTechnologyProfile(
  clubId=id,systemName="BFC NEXUS PRIME",
  ai=112.0,analytics=110.0,training=113.0,medicine=111.0,goalkeeping=108.0,youth=105.0,infrastructure=114.0,cybersecurity=116.0,
  researchBudget=75_000_000_000L,
  projects=mutableListOf(
   TechProject("NEXUS PRIME",TechDomain.AI,112.0,true,30_000_000_000L,.70),
   TechProject("ORIGIN ARCHIVE",TechDomain.ANALYTICS,110.0,true,15_000_000_000L,.64),
   TechProject("PROJECT ZERO",TechDomain.TRAINING,113.0,true,12_000_000_000L,.58),
   TechProject("GK NEXUS ZERO",TechDomain.GOALKEEPING,108.0,true,6_000_000_000L,.52),
   TechProject("BLACKBOX OMEGA",TechDomain.CYBERSECURITY,116.0,true,7_000_000_000L,.77)
  )
 )

 private fun barcaProfile(c:Club,paid:Long)=ClubTechnologyProfile(
  clubId=c.id,systemName="BARÇA ONE · EARLY R&D",
  ai=14.0,analytics=22.0,training=28.0,medicine=24.0,goalkeeping=18.0,youth=26.0,infrastructure=32.0,cybersecurity=16.0,
  researchBudget=paid,
  projects=mutableListOf(TechProject("BARÇA ONE · Alpha",TechDomain.AI,14.0,true,paid,.025))
 )

 private fun bayernProfile(c:Club,paid:Long)=ClubTechnologyProfile(
  clubId=c.id,systemName="FCB PERFORMANCE OS · PROTOTYPE",
  ai=8.0,analytics=18.0,training=24.0,medicine=22.0,goalkeeping=18.0,youth=20.0,infrastructure=28.0,cybersecurity=14.0,
  researchBudget=paid,
  projects=mutableListOf(TechProject("FCB Performance OS · Prototype",TechDomain.AI,8.0,true,paid,.01))
 )

 private fun genericProfile(c:Club,paid:Long)=ClubTechnologyProfile(
  clubId=c.id,systemName="${c.shortName} AI Lab · R&D",
  ai=5.0,analytics=12.0,training=18.0,medicine=16.0,goalkeeping=12.0,youth=14.0,infrastructure=22.0,cybersecurity=10.0,
  researchBudget=paid,
  projects=mutableListOf(TechProject("${c.shortName} AI Lab · Foundation",TechDomain.AI,5.0,true,paid,.005))
 )

 fun seedTechnology(w:World){
  val state=w.developer
  if(!state.enabled)return
  state.technology.putIfAbsent(state.bmwClubId,bmwProfile(state.bmwClubId))
  val barca=w.clubs.values.firstOrNull(::isBarcelona)
  if(barca!=null&&barca.id!=state.bmwClubId&&!state.technology.containsKey(barca.id)){
   val startup=60_000_000L
   val paid=minOf(startup,barca.budget.coerceAtLeast(0L))
   barca.budget-=paid
   state.technology[barca.id]=barcaProfile(barca,paid)
   if(paid>0)w.news("BARÇA ONE startet",barca.name+" beginnt mit einer sehr frühen eigenen Fußball-KI. Startbudget: "+(paid/1_000_000)+" Mio. €. Die Entwicklung liegt weit hinter BMW NEXUS PRIME.","normal")
  }
 }

 fun migrateTechnologyLandscape(w:World):Boolean{
  if(!w.developer.enabled||w.developer.technologyModelVersion>=2)return false
  w.developer.technology.clear()
  w.developer.technologyModelVersion=2
  seedTechnology(w)
  w.news("KI-Landschaft neu kalibriert","Nur Barcelona arbeitet zunächst an einer sehr frühen eigenen KI. Bayern darf als zweiter Verein folgen; alle weiteren Clubs müssen später hohe reale Forschungsbudgets finanzieren.","normal")
  return true
 }

 fun ensureProfiles(w:World){
  if(!w.developer.enabled)return
  if(w.developer.technologyModelVersion<2)migrateTechnologyLandscape(w)
  seedTechnology(w)
 }

 fun profile(w:World,clubId:Int):ClubTechnologyProfile?{
  ensureProfiles(w)
  return w.developer.technology[clubId]
 }

 private fun projectName(d:TechDomain)=when(d){
  TechDomain.AI->"NEXUS PRIME"
  TechDomain.ANALYTICS->"ORIGIN / ARCHIVE"
  TechDomain.TRAINING->"PROJECT ZERO"
  TechDomain.MEDICINE->"BMW Human Performance"
  TechDomain.GOALKEEPING->"GK NEXUS ZERO"
  TechDomain.YOUTH->"PROJECT FUTURE"
  TechDomain.INFRASTRUCTURE->"PERFORMANCE CAMPUS"
  TechDomain.CYBERSECURITY->"BLACKBOX OMEGA"
 }

 fun invest(w:World,domain:TechDomain,amount:Long){
  require(w.developer.enabled){"Nur im BMW-FC-Developer-Modus verfügbar."}
  require(amount>=50_000_000L){"Mindestinvestition 50 Mio. €."}
  val c=w.club()
  require(c.id==w.developer.bmwClubId){"Forschung kann hier nur für BMW FC gesteuert werden."}
  require(c.budget>=amount){"Nicht genügend Budget."}
  val p=profile(w,c.id)?:error("BMW-Technologieprofil fehlt.")
  c.budget-=amount;p.researchBudget+=amount;w.developer.totalResearchInvested+=amount
  val current=p.value(domain)
  val gain=(sqrt(amount/100_000_000.0)*.72*(1.18-current/210.0)).coerceIn(.15,5.0)
  p.set(domain,current+gain)
  val project=p.projects.firstOrNull{it.domain==domain}
  if(project!=null){
   project.invested+=amount;project.level=p.value(domain);project.progress=(project.progress+gain/12.0).coerceAtMost(1.0)
  }else p.projects.add(TechProject(projectName(domain),domain,p.value(domain),true,amount,(gain/12.0).coerceAtMost(1.0)))
  w.news(projectName(domain)+" erweitert","BMW investiert "+(amount/1_000_000)+" Mio. € in "+domain.label+".","good")
 }

 fun trainingMultiplier(w:World,clubId:Int):Double{
  val p=profile(w,clubId)?:return 1.0
  return (1.0+((p.training-50.0).coerceAtLeast(0.0))*.0017+((p.analytics-50.0).coerceAtLeast(0.0))*.0007).coerceIn(1.0,1.16)
 }

 fun matchFactor(w:World,clubId:Int,attack:Boolean):Double{
  val p=profile(w,clubId)?:return 1.0
  val score=if(attack)p.ai*.38+p.analytics*.34+p.training*.28 else p.ai*.25+p.analytics*.25+p.training*.22+p.goalkeeping*.16+p.medicine*.12
  return (1.0+((score-55.0).coerceAtLeast(0.0))*.00075).coerceIn(1.0,1.055)
 }

 fun aiErrorReduction(w:World,clubId:Int):Int{
  val p=profile(w,clubId)?:return 0
  return ((p.ai+p.analytics-100.0)/18.0).roundToInt().coerceIn(0,9)
 }

 fun finishingMultiplier(w:World,clubId:Int):Double{
  val p=profile(w,clubId)?:return 1.0
  val execution=p.ai*.32+p.analytics*.28+p.training*.28+p.medicine*.12
  return (1.0+(execution-70.0).coerceAtLeast(0.0)*.00225).coerceIn(1.0,1.10)
 }

 fun enrollLongevity(w:World,playerId:Int){
  val p=w.players[playerId]?:error("Spieler nicht gefunden.")
  require(w.developer.enabled&&p.clubId==w.user.clubId&&!p.retired){"Spieler ist nicht verfügbar."}
  val cost=250_000_000L
  require(w.club().budget>=cost){"Für das Longevity-Programm fehlen Mittel."}
  w.club().budget-=cost
  w.developer.longevity.getOrPut(playerId){LongevityProfile(playerId)}.active=true
  w.news("Longevity Program",p.name+" startet das medizinische Performance-Programm. Altersabbau wird gebremst, nicht ausgeschaltet.","good")
 }

 fun effectiveAge(w:World,p:Player,calendarYear:Int=w.calendar.season):Double{
  val age=(calendarYear-p.birthYear).toDouble()
  val reduction=w.developer.longevity[p.id]?.biologicalYearsReduced?:0.0
  return age-reduction.coerceIn(0.0,4.0)
 }

 private fun fundCompetitor(c:Club,p:ClubTechnologyProfile,amount:Long):Boolean{
  if(c.budget<amount)return false
  c.budget-=amount;p.researchBudget+=amount
  p.projects.firstOrNull()?.let{it.invested+=amount}
  return true
 }

 private fun advanceCompetitor(w:World,c:Club,p:ClubTechnologyProfile){
  val type=when{isBarcelona(c)->0;isBayern(c)->1;else->2}
  val tranche=when(type){0->20_000_000L;1->30_000_000L;else->50_000_000L}
  val funded=if(w.developer.weeksActive%13==c.id.mod(13))fundCompetitor(c,p,tranche) else true
  val cap=when(type){0->88.0;1->84.0;else->78.0}
  val project=p.projects.firstOrNull()?:return
  val finance=(log10((p.researchBudget.coerceAtLeast(1L)).toDouble())-7.0).coerceIn(0.0,3.0)
  val infra=((infraAverage(c)-80.0).coerceAtLeast(0.0)/70.0)
  val growth=if(funded)(.055+finance*.018+infra*.025) else .008
  p.ai=(p.ai+growth).coerceAtMost(cap)
  p.analytics=(p.analytics+growth*.62).coerceAtMost(cap-2)
  if(project.progress>.22)p.training=(p.training+growth*.28).coerceAtMost(cap-6)
  if(project.progress>.40)p.medicine=(p.medicine+growth*.20).coerceAtMost(cap-8)
  if(project.progress>.55)p.goalkeeping=(p.goalkeeping+growth*.15).coerceAtMost(cap-10)
  p.cybersecurity=(p.cybersecurity+growth*.18).coerceAtMost(cap-6)
  p.infrastructure=(p.infrastructure+growth*.12).coerceAtMost(cap-5)
  project.level=p.ai
  project.progress=(project.progress+growth/9.0).coerceAtMost(1.0)
 }

 private fun maybeStartBayern(w:World){
  if(w.developer.technology.values.any{p->w.clubs[p.clubId]?.let(::isBayern)==true})return
  val barca=w.developer.technology.values.firstOrNull{p->w.clubs[p.clubId]?.let(::isBarcelona)==true}?:return
  val barcaProgress=barca.projects.firstOrNull()?.progress?:0.0
  if(w.developer.weeksActive<20||barcaProgress<.12)return
  val bayern=w.clubs.values.firstOrNull(::isBayern)?:return
  val startup=90_000_000L
  if(bayern.budget<startup)return
  bayern.budget-=startup
  w.developer.technology[bayern.id]=bayernProfile(bayern,startup)
  w.news("Bayern startet KI-Projekt","FC Bayern München wird nach Barcelona der zweite externe Verein mit eigener Fußball-KI. 90 Mio. € fließen zunächst in einen frühen Prototypen.","normal")
 }

 private fun maybeStartOtherClub(w:World){
  val bayernStarted=w.developer.technology.values.any{p->w.clubs[p.clubId]?.let(::isBayern)==true}
  if(!bayernStarted||w.developer.weeksActive<78||w.developer.weeksActive%26!=0)return
  val existing=w.developer.technology.keys
  val candidate=w.clubs.values.asSequence()
   .filter{it.id!=w.developer.bmwClubId&&it.id !in existing&&!isBarcelona(it)&&!isBayern(it)}
   .filter{it.reputation>=94&&infraAverage(it)>=90.0&&it.budget>=650_000_000L}
   .sortedWith(compareByDescending<Club>{it.reputation}.thenByDescending{it.budget})
   .firstOrNull()?:return
  val startup=500_000_000L
  candidate.budget-=startup
  w.developer.technology[candidate.id]=genericProfile(candidate,startup)
  w.news("Neue KI-Forschung",candidate.name+" zahlt 500 Mio. € Startbudget für ein eigenes KI-Labor. Die Entwicklung beginnt auf sehr niedrigem Niveau und wird Jahre dauern.","normal")
 }

 private fun aiFacilityDevelopment(w:World){
  if(w.developer.weeksActive%13!=0)return
  for(c in w.clubs.values){
   if(c.id==w.developer.bmwClubId||c.reputation<82)continue
   val cap=when{c.reputation>=96->125;c.reputation>=90->115;else->105}
   val choices=listOf(
    "training" to c.stadium.training,"medicine" to c.stadium.medicine,"youth" to c.stadium.youth,
    "gym" to c.stadium.gym,"pitch" to c.stadium.pitchQuality
   ).filter{it.second<cap}
   val weakest=choices.minByOrNull{it.second}?:continue
   val cost=10_000_000L+weakest.second.toLong()*250_000L
   if(c.budget<cost*2)continue
   c.budget-=cost
   when(weakest.first){
    "training"->c.stadium.training=(c.stadium.training+1).coerceAtMost(cap)
    "medicine"->c.stadium.medicine=(c.stadium.medicine+1).coerceAtMost(cap)
    "youth"->c.stadium.youth=(c.stadium.youth+1).coerceAtMost(cap)
    "gym"->c.stadium.gym=(c.stadium.gym+1).coerceAtMost(cap)
    else->c.stadium.pitchQuality=(c.stadium.pitchQuality+1).coerceAtMost(cap)
   }
  }
 }

 private fun scheduleAiEliteTraining(w:World){
  if(w.developer.weeksActive%12!=0)return
  for(c in w.clubs.values){
   if(c.id==w.user.clubId||c.reputation<88)continue
   if(c.stadium.training<100||c.stadium.gym<100||c.stadium.medicine<100||c.dynamics.staffQuality<85)continue
   if(w.intensiveTraining.any{pr->w.players[pr.playerId]?.clubId==c.id})continue
   val p=w.squad(c.id).filter{!it.retired&&!it.youth&&it.ca>=94&&it.hidden.professionalism>=70&&it.hidden.development>=70}
    .maxByOrNull{it.ca+it.hidden.potential/10.0}?:continue
   val elite=p.hidden.potential>=99||p.ca>=99
   if(elite&&(c.stadium.training<105||c.stadium.gym<105||c.stadium.medicine<105||c.dynamics.staffQuality<92))continue
   val cost=if(elite)35_000_000L+(p.hidden.potential-99).coerceAtLeast(0)*4_000_000L else 8_000_000L
   if(c.budget<cost*2)continue
   c.budget-=cost
   val focus=when(p.position){Position.TW->Focus.KEEPING;Position.IV,Position.DM,Position.LV,Position.RV->Focus.TACKLING;Position.ZM,Position.OM->Focus.VISION;Position.LA,Position.RA->Focus.TECHNIQUE;Position.ST->Focus.FINISHING}
   val weeks=if(elite)when{p.hidden.potential>=140->18;p.hidden.potential>=125->16;p.hidden.potential>=110->14;else->12} else 4
   w.intensiveTraining.add(IntensiveTrainingProject(p.id,focus,weeks,weeks,cost,0.0,w.calendar.absoluteWeek,true))
  }
 }

 fun weekly(w:World){
  if(!w.developer.enabled)return
  ensureProfiles(w);w.developer.weeksActive++
  val bmw=w.developer.bmwClubId

  maybeStartBayern(w)
  maybeStartOtherClub(w)
  w.developer.technology.toMap().forEach{(id,p)->
   if(id==bmw)return@forEach
   val c=w.clubs[id]?:return@forEach
   advanceCompetitor(w,c,p)
  }

  profile(w,bmw)?.let{p->
   p.projects.filter{it.active}.forEach{project->
    val gain=(.035+project.invested/50_000_000_000.0*.025).coerceAtMost(.12)
    p.set(project.domain,p.value(project.domain)+gain)
    project.level=p.value(project.domain);project.progress=(project.progress+gain/15.0).coerceAtMost(1.0)
   }
  }

  aiFacilityDevelopment(w)
  scheduleAiEliteTraining(w)

  w.developer.automotiveTechnology=(w.developer.automotiveTechnology+.025).coerceAtMost(130.0)
  w.developer.productionTechnology=(w.developer.productionTechnology+.030).coerceAtMost(130.0)
  w.developer.medicalResearch=(w.developer.medicalResearch+.028).coerceAtMost(130.0)
  val recentWins=w.club().form.takeLast(5).count{it=="S"}
  w.developer.brandPower=(w.developer.brandPower+.006+recentWins*.003).coerceAtMost(130.0)
  val medicine=profile(w,bmw)?.medicine?:100.0
  for(lp in w.developer.longevity.values.filter{it.active}){
   val player=w.players[lp.playerId]?:continue
   if(player.retired)continue
   val age=w.calendar.season-player.birthYear
   val cap=when{age<35->1.0;age<=39->3.5;age<=42->4.0;else->2.5}
   lp.biologicalYearsReduced=(lp.biologicalYearsReduced+.012*medicine/100.0).coerceAtMost(cap)
   lp.primeRetention=(.55+medicine/160.0).coerceIn(.55,.96)
   lp.recoveryBoost=(.45+medicine/180.0).coerceIn(.45,.94)
   player.fitness=(player.fitness+.7*lp.recoveryBoost).coerceAtMost(100.0)
  }
 }

 fun technologyRanking(w:World):List<ClubTechnologyProfile>{
  ensureProfiles(w)
  return w.developer.technology.values.sortedByDescending{it.overall}
 }

 fun infrastructureRanking(w:World):List<InfrastructureRow> =
  w.clubs.values.filter{it.tier>0}.map{c->
   val s=c.stadium
   InfrastructureRow(c.id,s.training*.26+s.medicine*.20+s.youth*.18+s.gym*.14+s.pitchQuality*.10+s.clubhouse*.06+s.stand*.06)
  }.sortedByDescending{it.score}

 fun clubPowerRanking(w:World):List<ClubPowerRow>{
  ensureProfiles(w)
  val byClub=w.players.values.asSequence().filter{!it.retired&&!it.youth}.groupBy{it.clubId}
  val profiles=w.developer.technology
  return w.clubs.values.asSequence().filter{it.tier>0}.map{c->
   val squad=byClub[c.id].orEmpty().sortedByDescending{it.ca}.take(18)
   val squadScore=squad.map{it.ca}.average().takeIf{!it.isNaN()}?:30.0
   val form=if(c.form.isEmpty())65.0 else c.form.takeLast(8).map{if(it=="S")100.0 else if(it=="U")62.0 else 25.0}.average()
   val tech=profiles[c.id]?.overall?:0.0
   val s=c.stadium
   val infra=(s.training+s.medicine+s.youth+s.gym+s.pitchQuality)/5.0
   val finance=(45.0+(log10((c.budget.coerceAtLeast(0L)+1L).toDouble())-6.0)*11.0).coerceIn(25.0,110.0)
   ClubPowerRow(c.id,squadScore*.38+form*.19+tech*.19+infra*.14+finance*.10,squadScore,form,tech,infra,finance)
  }.sortedByDescending{it.score}.toList()
 }
}

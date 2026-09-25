package de.gruenderelf.engine

import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BmwDeveloperSystems {
 fun seedTechnology(w:World){
  val state=w.developer
  if(!state.enabled)return
  for(c in w.clubs.values){
   val bmw=c.id==state.bmwClubId
   val base=(28.0+c.reputation*.46+((c.stadium.training+c.stadium.medicine+c.stadium.youth)/300.0)*18.0).coerceIn(25.0,88.0)
   val system=when{
    bmw->"BFC NEXUS PRIME"
    c.name.contains("Bayern",true)->"AURORA FCB"
    c.name.contains("Barcelona",true)->"BARÇA ONE"
    c.name.contains("Real Madrid",true)->"REAL VISION"
    c.name.contains("Manchester City",true)->"CITYMIND"
    c.name.contains("Liverpool",true)->"LFC PULSE"
    c.name.contains("Paris",true)||c.shortName=="PSG"->"PARIS LAB"
    else->"${c.shortName} Intelligence"
   }
   val p=ClubTechnologyProfile(
    clubId=c.id,systemName=system,
    ai=if(bmw)112.0 else base,
    analytics=if(bmw)110.0 else (base+2).coerceAtMost(94.0),
    training=if(bmw)113.0 else (base-1).coerceAtLeast(20.0),
    medicine=if(bmw)111.0 else (base-2).coerceAtLeast(20.0),
    goalkeeping=if(bmw)108.0 else (base-5).coerceAtLeast(18.0),
    youth=if(bmw)105.0 else (base-4).coerceAtLeast(18.0),
    infrastructure=if(bmw)114.0 else base,
    cybersecurity=if(bmw)116.0 else (base-8).coerceAtLeast(15.0),
    researchBudget=if(bmw)30_000_000_000L else c.reputation.toLong()*12_000_000L
   )
   if(bmw)p.projects.addAll(listOf(
    TechProject("NEXUS PRIME",TechDomain.AI,112.0,true,30_000_000_000L,.70),
    TechProject("ORIGIN ARCHIVE",TechDomain.ANALYTICS,110.0,true,15_000_000_000L,.64),
    TechProject("PROJECT ZERO",TechDomain.TRAINING,113.0,true,12_000_000_000L,.58),
    TechProject("GK NEXUS ZERO",TechDomain.GOALKEEPING,108.0,true,6_000_000_000L,.52),
    TechProject("BLACKBOX OMEGA",TechDomain.CYBERSECURITY,116.0,true,7_000_000_000L,.77)
   )) else if(c.reputation>=86)p.projects.add(TechProject(system,TechDomain.AI,p.ai,true,p.researchBudget,.10))
   state.technology[c.id]=p
  }
 }

 fun ensureProfiles(w:World){
  if(w.developer.enabled&&w.developer.technology.size<w.clubs.size)seedTechnology(w)
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

 fun weekly(w:World){
  if(!w.developer.enabled)return
  ensureProfiles(w);w.developer.weeksActive++
  val bmw=w.developer.bmwClubId
  for((id,p) in w.developer.technology){
   if(id==bmw)continue
   val c=w.clubs[id]?:continue
   val ceiling=(72.0+c.reputation*.32).coerceIn(78.0,104.0)
   val finance=(log10((c.budget.coerceAtLeast(0L)+1L).toDouble())-6.0).coerceIn(0.0,4.0)
   val growth=.018+c.reputation*.00022+finance*.004
   val domains=listOf(TechDomain.AI,TechDomain.ANALYTICS,TechDomain.TRAINING,TechDomain.MEDICINE,TechDomain.INFRASTRUCTURE)
   val domain=domains[(w.calendar.absoluteWeek+id).mod(domains.size)]
   p.set(domain,(p.value(domain)+growth).coerceAtMost(ceiling))
   p.projects.firstOrNull()?.let{it.level=p.ai;it.progress=(it.progress+growth/20.0).coerceAtMost(1.0)}
  }
  profile(w,bmw)?.let{p->
   p.projects.filter{it.active}.forEach{project->
    val gain=(.035+project.invested/50_000_000_000.0*.025).coerceAtMost(.12)
    p.set(project.domain,p.value(project.domain)+gain)
    project.level=p.value(project.domain);project.progress=(project.progress+gain/15.0).coerceAtMost(1.0)
   }
  }
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
  return w.clubs.values.filter{it.tier>0}.map{c->
   val squad=w.squad(c.id).filter{!it.youth}.sortedByDescending{it.ca}.take(18)
   val squadScore=squad.map{it.ca}.average().takeIf{!it.isNaN()}?:30.0
   val form=if(c.form.isEmpty())65.0 else c.form.takeLast(8).map{if(it=="S")100.0 else if(it=="U")62.0 else 25.0}.average()
   val tech=(profile(w,c.id)?.overall?:35.0).coerceAtMost(115.0)
   val s=c.stadium
   val infra=(s.training+s.medicine+s.youth+s.gym+s.pitchQuality)/5.0
   val finance=(45.0+(log10((c.budget.coerceAtLeast(0L)+1L).toDouble())-6.0)*11.0).coerceIn(25.0,100.0)
   ClubPowerRow(c.id,squadScore*.38+form*.19+tech*.19+infra*.14+finance*.10,squadScore,form,tech,infra,finance)
  }.sortedByDescending{it.score}
 }
}

package de.gruenderelf.engine

import kotlinx.serialization.Serializable

const val BMW_DEVELOPER_SAVE_SLOT = 50

@Serializable
enum class TechDomain(val label:String){
 AI("KI / NEXUS"), ANALYTICS("Analyse"), TRAINING("Training"), MEDICINE("Medizin"),
 GOALKEEPING("Torwart"), YOUTH("Nachwuchs"), INFRASTRUCTURE("Infrastruktur"), CYBERSECURITY("Cybersecurity")
}

@Serializable
data class TechProject(
 var name:String="",
 var domain:TechDomain=TechDomain.AI,
 var level:Double=0.0,
 var active:Boolean=true,
 var invested:Long=0L,
 var progress:Double=0.0
)

@Serializable
data class ClubTechnologyProfile(
 var clubId:Int=0,
 var systemName:String="Club Intelligence",
 var ai:Double=25.0,
 var analytics:Double=25.0,
 var training:Double=25.0,
 var medicine:Double=25.0,
 var goalkeeping:Double=25.0,
 var youth:Double=25.0,
 var infrastructure:Double=25.0,
 var cybersecurity:Double=25.0,
 var researchBudget:Long=0L,
 var projects:MutableList<TechProject> = mutableListOf()
){
 fun value(d:TechDomain)=when(d){
  TechDomain.AI->ai; TechDomain.ANALYTICS->analytics; TechDomain.TRAINING->training; TechDomain.MEDICINE->medicine
  TechDomain.GOALKEEPING->goalkeeping; TechDomain.YOUTH->youth; TechDomain.INFRASTRUCTURE->infrastructure; TechDomain.CYBERSECURITY->cybersecurity
 }
 fun set(d:TechDomain,x:Double){
  val v=x.coerceIn(0.0,130.0)
  when(d){
   TechDomain.AI->ai=v; TechDomain.ANALYTICS->analytics=v; TechDomain.TRAINING->training=v; TechDomain.MEDICINE->medicine=v
   TechDomain.GOALKEEPING->goalkeeping=v; TechDomain.YOUTH->youth=v; TechDomain.INFRASTRUCTURE->infrastructure=v; TechDomain.CYBERSECURITY->cybersecurity=v
  }
 }
 val overall get()=ai*.22+analytics*.16+training*.16+medicine*.13+goalkeeping*.08+youth*.08+infrastructure*.10+cybersecurity*.07
}

@Serializable
data class LongevityProfile(
 var playerId:Int=0,
 var active:Boolean=false,
 var biologicalYearsReduced:Double=0.0,
 var primeRetention:Double=0.0,
 var recoveryBoost:Double=0.0
)

@Serializable
data class DeveloperPlayerMeta(
 var playerId:Int=0,
 var potential:Int=99,
 var marketValue:Long=0L,
 var annualSalary:Long=0L
)

@Serializable
data class BmwDeveloperState(
 var enabled:Boolean=false,
 var bmwClubId:Int=0,
 var technology:MutableMap<Int,ClubTechnologyProfile> = linkedMapOf(),
 var longevity:MutableMap<Int,LongevityProfile> = linkedMapOf(),
 var playerMeta:MutableMap<Int,DeveloperPlayerMeta> = linkedMapOf(),
 var automotiveTechnology:Double=101.0,
 var brandPower:Double=100.0,
 var productionTechnology:Double=98.0,
 var medicalResearch:Double=101.0,
 var totalResearchInvested:Long=75_000_000_000L,
 var weeksActive:Int=0
)

data class ClubPowerRow(
 val clubId:Int,val score:Double,val squad:Double,val form:Double,val technology:Double,val infrastructure:Double,val finance:Double
)
data class InfrastructureRow(val clubId:Int,val score:Double)

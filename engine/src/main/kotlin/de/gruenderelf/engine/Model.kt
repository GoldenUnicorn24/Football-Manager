package de.gruenderelf.engine

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

const val SAVE_VERSION = 9
@Serializable enum class Difficulty(val label: String,val money: Double) { CASUAL("Entspannt",1.25), NORMAL("Normal",1.0), REALISTIC("Realistisch",.85), HARDCORE("Hart",.7), SANDBOX("Sandbox",5.0) }
@Serializable enum class Position(val label: String) { TW("Torwart"),IV("Innenverteidiger"),LV("Linksverteidiger"),RV("Rechtsverteidiger"),DM("Defensives Mittelfeld"),ZM("Zentrales Mittelfeld"),OM("Offensives Mittelfeld"),LA("Linksaußen"),RA("Rechtsaußen"),ST("Stürmer") }

/**
 * Natürliche Positionsnähe. Sie verhindert harte Fantasie-Mali zwischen sehr ähnlichen Rollen
 * (z. B. DM/ZM), hält aber echte Spezialrollen wie Torwart weiterhin strikt getrennt.
 */
fun positionAffinity(from: Position,to: Position): Double {
 if(from==to)return 1.0
 if(from==Position.TW||to==Position.TW)return .25
 val pair=setOf(from,to)
 return when {
  pair==setOf(Position.DM,Position.ZM)->.97
  pair==setOf(Position.ZM,Position.OM)->.95
  pair==setOf(Position.DM,Position.IV)->.92
  pair==setOf(Position.LV,Position.LA)||pair==setOf(Position.RV,Position.RA)->.93
  pair==setOf(Position.LA,Position.RA)->.92
  pair==setOf(Position.OM,Position.LA)||pair==setOf(Position.OM,Position.RA)->.94
  pair==setOf(Position.OM,Position.ST)->.91
  pair==setOf(Position.ST,Position.LA)||pair==setOf(Position.ST,Position.RA)->.90
  pair==setOf(Position.IV,Position.LV)||pair==setOf(Position.IV,Position.RV)->.89
  pair==setOf(Position.LV,Position.RV)->.88
  pair==setOf(Position.DM,Position.LV)||pair==setOf(Position.DM,Position.RV)->.88
  pair==setOf(Position.ZM,Position.LA)||pair==setOf(Position.ZM,Position.RA)->.87
  pair==setOf(Position.DM,Position.OM)->.86
  pair==setOf(Position.ZM,Position.ST)->.84
  pair==setOf(Position.IV,Position.ZM)->.84
  else->.80
 }
}
@Serializable enum class Foot(val label: String) { RIGHT("Rechts"),LEFT("Links"),BOTH("Beide") }
@Serializable enum class Surface(val label: String) { HARD("Hartplatz"),GRASS("Rasen"),ARTIFICIAL("Kunstrasen") }
@Serializable enum class BuildUp(val label: String) { DIRECT("Direkt"),MIXED("Gemischt"),SHORT("Kurzpass"),TIKI_TAKA("Tiki-Taka"),WIDE("Über Außen"),COUNTER("Umschalten") }
@Serializable enum class UnitType(val label: String,val load: Int) { RECOVERY("Regeneration",-5),TECHNIQUE("Technik",3),TACTICS("Taktik",2),POSITIONAL("Positionsspiel",3),VIDEO("Videoanalyse",1),MENTAL("Mental & Führung",2),TEAM_BONDING("Teamchemie",1),FITNESS("Athletik",7),SET_PIECES("Standards",3),DUELS("Zweikampf",7),FINISHING("Abschluss",4),GOALKEEPING("Torwart",4),GAME("Internes Testspiel",8),OFF("Frei",-2) }
@Serializable enum class Focus(val label: String) { FINISHING("Abschluss"),PACE("Tempo"),TECHNIQUE("Technik"),TACKLING("Zweikampf"),KEEPING("Torwart"),VISION("Spielwitz") }
@Serializable enum class Facility(val label: String,val baseCost: Int,val weeks: Int) { FLOODLIGHTS("Flutlicht",18000,8),ARTIFICIAL("Kunstrasen",42000,10),PITCH("Platzpflege",1800,2),TRAINING("Trainingsplatz",4800,4),GYM("Kraftraum",6200,5),MEDICINE("Medizin",4200,4),CABIN("Kabine",2800,3),STAND("Tribüne",9500,6),CAPACITY("Erweiterung",7000,5),CLUBHOUSE("Vereinsheim",6800,5),YOUTH("Jugendzentrum",8500,6) }
@Serializable enum class Decision(val label: String) { PASS("Pass"),SHOOT("Schuss"),DRIBBLE("Dribbling"),CROSS("Flanke"),HOLD("Ball halten"),FOUL("Taktisches Foul") }
@Serializable enum class MatchPauseReason(val label: String) { NONE(""), RED_CARD("Platzverweis"), INJURY("Verletzung") }
@Serializable enum class MatchBreakType(val label: String,val action: String) { NONE("","Fortsetzen"), HALF_TIME("Halbzeit","Zweite Halbzeit"), EXTRA_TIME_START("Ende der regulären Spielzeit","Verlängerung starten"), EXTRA_TIME_HALF("Halbzeit der Verlängerung","2. Halbzeit Verlängerung"), SHOOTOUT_START("Ende der Verlängerung","Elfmeterschießen starten") }
@Serializable enum class MatchSpeed(val label: String) { SLOW("Langsam"), NORMAL("Normal"), FAST("Schnell") }
@Serializable enum class PossessionChangeReason(val label: String) { NONE(""), TACKLE("Ballgewinn"), INTERCEPTION("Abgefangen"), SAVE("Parade"), CLEARANCE("Klärung"), THROW_IN("Einwurf"), OFFSIDE("Abseits"), GOAL_KICK("Abstoß"), KICK_OFF("Anstoß"), FOUL("Foul") }
@Serializable enum class SetPieceType(val label: String) { NONE(""), FREE_KICK("Freistoß"), DANGEROUS_FREE_KICK("Gefährlicher Freistoß"), PENALTY("Elfmeter") }
@Serializable enum class CompetitionType(val label: String) { LEAGUE("Liga"), NATIONAL_CUP("Gründerpokal"), EURO_ELITE("Europa-Eliteliga"), CHAMPIONS_LEAGUE("UEFA Champions League"), EUROPA_LEAGUE("UEFA Europa League"), CLUB_WORLD_CUP("Club World Cup"), ETERNAL_CROWN("Krone der Kontinente") }
fun CompetitionType.sortPriority()=when(this){CompetitionType.NATIONAL_CUP,CompetitionType.ETERNAL_CROWN->0;CompetitionType.CHAMPIONS_LEAGUE,CompetitionType.EUROPA_LEAGUE,CompetitionType.EURO_ELITE,CompetitionType.CLUB_WORLD_CUP->1;CompetitionType.LEAGUE->2}
@Serializable enum class ShotType(val label: String) { LONG_RANGE("Distanzschuss"), BOX_SHOT("Abschluss"), CLOSE_RANGE("Nahdistanz"), ONE_ON_ONE("Eins-gegen-eins"), HEADER("Kopfball"), VOLLEY("Volley"), CUTBACK("Rückpass von der Grundlinie"), REBOUND("Abpraller"), FREE_KICK("Freistoß"), PENALTY("Elfmeter") }
@Serializable enum class ShotOutcome(val label: String) { GOAL("Tor"), DISALLOWED("Aberkannt"), WOODWORK("Aluminium"), SAVED("Gehalten"), DEFLECTED("Abgewehrt"), CORNER("Zur Ecke"), REBOUND("Abpraller"), OFF_TARGET("Daneben"), BLOCKED("Geblockt") }
@Serializable data class ShotEvent(
 val minute: Int=0,
 val clubId: Int=0,
 val playerId: Int=0,
 val assistId: Int=0,
 val type: ShotType=ShotType.BOX_SHOT,
 val x: Float=.5f,
 val y: Float=.5f,
 val distanceMeters: Double=0.0,
 val angleRadians: Double=0.0,
 val pressure: Double=0.5,
 val defendersNearby: Int=1,
 val passQuality: Double=0.5,
 val xg: Double=0.0,
 val goalProbability: Double=0.0,
 val outcome: ShotOutcome=ShotOutcome.OFF_TARGET,
 val targetX: Float=.5f,
 val targetY: Float=.5f,
 val targetLabel: String="",
 val clockLabel: String=""
)
@Serializable enum class LivePhase(val label: String) { POSSESSION("Ballbesitz"), ATTACK("Angriff"), DANGEROUS_ATTACK("Gefährlicher Angriff"), COUNTER("Konter"), CORNER("Ecke"), FREE_KICK("Freistoß"), DANGEROUS_FREE_KICK("Gefährlicher Freistoß"), PENALTY("Elfmeter"), SHOOTOUT("Elfmeterschießen"), SHOT_OFF_TARGET("Schuss neben das Tor"), SHOT_ON_TARGET("Schuss aufs Tor – gehalten/abgewehrt"), WOODWORK("Pfosten/Latte"), GOAL("TOR"), THROW_IN("Einwurf"), OFFSIDE("Abseits"), VAR("VAR-Prüfung"), YELLOW_CARD("Gelbe Karte"), YELLOW_RED_CARD("Gelb-Rote Karte"), RED_CARD("Rote Karte"), INJURY("Verletzung") }
@Serializable enum class UnavailableReason(val label: String) { WORK("Arbeit"),HOLIDAY("Urlaub"),RESERVE("Reserve"),SUSPENDED("Gesperrt") }
@Serializable enum class PlayerRole(val label:String){AUTO("Automatisch"),BUILD_UP_KEEPER("Mitspielender Torwart"),STOPPER("Stopper"),BALL_PLAYING_CB("Aufbau-IV"),INVERTED_FULLBACK("Einrückender AV"),OVERLAPPING_FULLBACK("Überlaufender AV"),ANCHOR("Anker"),DEEP_PLAYMAKER("Tiefer Spielmacher"),BOX_TO_BOX("Box-to-Box"),PLAYMAKER("Spielmacher"),INSIDE_FORWARD("Inverser Flügel"),WINGER("Flügelspieler"),PRESSING_FORWARD("Pressingstürmer"),TARGET_FORWARD("Zielspieler"),POACHER("Knipser")}
@Serializable enum class PersonalityType(val label:String){LEADER("Führungsspieler"),BIG_GAME("Matchwinner"),CHOKER("Choker"),LAZY_STAR("Fauler Star"),WILD_YOUNGSTER("Junger Wilder"),PROFESSIONAL("Profi"),BALANCED("Ausgeglichen")}
@Serializable enum class AcademyIdentity(val label:String){BALANCED("Ausgewogen"),POSSESSION("Ballbesitz-Schule"),ATHLETIC("Athletik-Schule"),STREET("Straßenfußball"),DEFENSIVE("Defensiv-Schule"),PRESSING("Pressing-Schule")}
@Serializable enum class DevelopmentPath(val label:String){EARLY("Frühreif"),NORMAL("Normal"),LATE("Spätentwickler"),PLATEAU("Plateau"),BREAKTHROUGH("Durchbruch"),CRASH("Absturz")}
@Serializable enum class DealType(val label:String){BUY("Kauf"),LOAN("Leihe"),LOAN_OPTION("Leihe + Kaufoption"),SWAP("Tausch")}
@Serializable enum class SquadRole(val label:String){STAR("Starspieler"),STARTER("Stammspieler"),ROTATION("Rotation"),PROSPECT("Perspektive"),BACKUP("Backup")}
@Serializable enum class NegotiationStatus(val label:String){DRAFT("Entwurf"),COUNTER("Gegenangebot"),AGREED("Einigung"),REJECTED("Abgelehnt"),COMPLETED("Abgeschlossen")}
@Serializable enum class TransferInterestLevel(val label:String){LOW("eher kein Interesse"),OPEN("offen für Gespräche"),INTERESTED("interessiert"),VERY_INTERESTED("sehr interessiert"),DESPERATE("will unbedingt zu diesem Verein")}
@Serializable enum class AssistantTrainingStyle(val label:String){BALANCED("Ausgewogen"),MATCH_PREP("Gegner & Matchplan"),DEVELOPMENT("Entwicklung"),FITNESS("Athletik"),YOUTH("Jugendförderung")}
@Serializable enum class YouthBlueprint(val label:String){BALANCED("Ausgewogen"),TECHNICAL("Techniker"),PHYSICAL("Athlet"),CREATIVE("Spielmacher"),DEFENSIVE("Defensiv"),FINISHER("Abschluss")}
@Serializable enum class YouthSquad(val label:String){U19("U19"),U23("U23")}
@Serializable enum class TransferStage(val label:String){SCOUTING("Scouting"),CLUB("Vereinsverhandlung"),PLAYER_AGENT("Spieler & Berater"),MEDICAL("Medizincheck"),REGISTRATION("Registrierung"),COMPLETED("Abgeschlossen")}
@Serializable enum class ScoutRegion(val label:String){DOMESTIC("Deutschland"),DACH("DACH"),EUROPE("Europa"),SOUTH_AMERICA("Südamerika"),WORLD("Weltweit")}
@Serializable enum class PlayerCareerFocus(val label:String,val description:String){BALANCED("Ausgewogen","Keine Spezialisierung; Entwicklung folgt Training und Spielrolle."),GOALGETTER("Torjäger","Zusätzlicher Abschlussfokus für deine Spielerkarriere."),PLAYMAKER("Spielmacher","Zusätzlicher Fokus auf Spielwitz und kreative Aktionen."),ATHLETE("Athlet","Zusätzlicher Tempofokus und körperliche Entwicklung."),CLUB_ICON("Vereinsikone","Bindung, Moral und langfristige Vereinsidentität stehen im Mittelpunkt.")}
@Serializable enum class PlayerInstruction(val label:String){OVERLAP("Hinterlaufen"),CUT_INSIDE("Nach innen ziehen"),RUN_IN_BEHIND("In die Tiefe"),HOLD_POSITION("Position halten"),PRESS_MORE("Mehr pressen"),PRESS_LESS("Weniger pressen"),TIGHT_MARKING("Enger decken"),RISKY_PASSES("Riskante Pässe"),SHOOT_MORE("Häufiger abschließen")}
@Serializable enum class AssistantCoachProfile(val label:String,val description:String){BALANCED("Allrounder","Ausgewogene Entscheidungen ohne extreme Schwerpunkte."),ANALYST("Analytiker","Stärker bei Gegneranalyse, Matchplan und datenbasierten Hinweisen."),TACTICIAN("Taktiker","Reagiert im Spiel früher auf taktische Probleme und Wechselbedarf."),DEVELOPER("Entwickler","Fördert Jugend, individuelle Rollen und langfristige Entwicklung."),MOTIVATOR("Motivator","Stabilisiert Moral, Belastung und Mannschaftsdynamik.")}
@Serializable enum class SponsorCategory(val label:String){MAIN("Hauptpartner"),KIT("Trikotpartner"),YOUTH("Jugendpartner"),STADIUM("Infrastrukturpartner"),REGIONAL("Regionalpartner")}
@Serializable data class AcademyTeamSeason(var played:Int=0,var wins:Int=0,var draws:Int=0,var losses:Int=0,var goalsFor:Int=0,var goalsAgainst:Int=0,var points:Int=0,var lastResult:String="")
@Serializable data class YouthTeamStats(var appearances:Int=0,var goals:Int=0,var assists:Int=0,var averageRating:Double=6.5)
@Serializable data class ScoutAssignment(var playerId:Int=0,var region:ScoutRegion=ScoutRegion.DOMESTIC,var weeksRemaining:Int=0,var startedWeek:Int=0,var cost:Long=0)
@Serializable data class ScoutReport(var playerId:Int=0,var progress:Int=0,var caMin:Int=1,var caMax:Int=99,var potentialMin:Int=1,var potentialMax:Int=99,var personalityKnown:Boolean=false,var medicalKnown:Boolean=false,var lastUpdatedWeek:Int=-1,var note:String="Noch nicht beobachtet")
@Serializable data class SavedTactic(var formation:String="4-4-2",var mentality:Int=3,var pressing:Int=3,var line:Int=3,var tempo:Int=3,var width:Int=3,var buildUp:BuildUp=BuildUp.MIXED)
@Serializable data class TransferHistoryEntry(var season:Int=0,var week:Int=0,var playerId:Int=0,var fromClubId:Int=0,var toClubId:Int=0,var type:DealType=DealType.BUY,var fee:Long=0,var note:String="")
@Serializable data class CompetingBid(var playerId:Int=0,var clubId:Int=0,var fee:Long=0,var expiresWeek:Int=0)
@Serializable data class NotificationState(var tiredSeenWeek:Int=-1,var seenTiredPlayerIds:MutableSet<Int> = linkedSetOf(),var contractSeenSeason:Int=-1,var seenExpiringPlayerIds:MutableSet<Int> = linkedSetOf(),var seenCompletedScoutReportIds:MutableSet<Int> = linkedSetOf())
@Serializable data class PassEvent(var minute:Int=0,var clubId:Int=0,var fromId:Int=0,var toId:Int=0,var completed:Boolean=true,var startX:Float=.5f,var startY:Float=.5f,var endX:Float=.5f,var endY:Float=.5f)
@Serializable data class TacticChangeEvent(var minute:Int=0,var clubId:Int=0,var label:String="",var xg:Double=0.0,var shots:Int=0,var possessionTicks:Int=0)
@Serializable data class BallFrame(var serial:Int=0,var minute:Int=0,var x:Float=.5f,var y:Float=.5f,var clubId:Int=0,var phase:LivePhase=LivePhase.POSSESSION,var detail:String="")
data class PassNetworkNode(val playerId:Int,val x:Float,val y:Float,val touches:Int)
data class PassNetworkEdge(val fromId:Int,val toId:Int,val count:Int,val startX:Float,val startY:Float,val endX:Float,val endY:Float)
data class MatchCalibration(val chanceCreation:Double=1.0,val conversion:Double=1.0,val turnover:Double=1.0,val tempo:Double=1.0)
data class PlayerDecisionContext(val distanceMeters:Double=0.0,val nearbyOpponents:Int=0,val pressureLabel:String="")
data class CompetitionRuleSet(val maxSubstitutions:Int=5,val substitutionWindows:Int=3,val halfTimeIsFreeWindow:Boolean=true,val extraTimeAdditionalSubstitution:Boolean=true,val varEnabled:Boolean=true,val goalkeeperControlSeconds:Int=8,val substitutionExitSeconds:Int=10)
@Serializable data class YouthProfile(var learning:Int=60,var maturity:Int=50,var path:DevelopmentPath=DevelopmentPath.NORMAL,var familySupport:Int=60,var adviserPressure:Int=35,var schoolStress:Int=25,var injuryGrowthRisk:Int=20,var seniorTraining:Boolean=false,var mentorId:Int=0,var roleSpark:PlayerRole=PlayerRole.AUTO,var lastTrackedMinutes:Int=0,var confidence:Int=55,var growthSpurtWeeks:Int=0,var homegrownYears:Int=0)
@Serializable data class AcademyState(var identity:AcademyIdentity=AcademyIdentity.BALANCED,var scouting:Int=25,var boarding:Int=10,var partnerNetwork:Int=10,var loanNetwork:Int=10,var u19Quality:Int=25,var u23Quality:Int=20,var generationCycle:Int=50,var goldenGeneration:Boolean=false,var droughtYears:Int=0,var customIntakeSeason:Int=-1,var customIntakeUsed:Int=0,var u19Season:AcademyTeamSeason=AcademyTeamSeason(),var u23Season:AcademyTeamSeason=AcademyTeamSeason())
@Serializable data class ClubDynamics(var chemistry:Int=55,var tacticalUnderstanding:Int=45,var pressingCoordination:Int=40,var mentalHardness:Int=50,var leadership:Int=45,var hierarchyStability:Int=55,var languageCohesion:Int=70,var fatigueLoad:Int=20,var opponentPrep:Int=20,var staffQuality:Int=45,var patterns:MutableMap<String,Int> = linkedMapOf("AUFBAU" to 35,"PRESSINGFALLE" to 25,"HALBRAUM" to 25,"FLUEGEL" to 30,"DIAGONALE" to 25,"RESTVERTEIDIGUNG" to 30,"STANDARDS" to 30))
@Serializable data class TransferOffer(var id:Int=0,var buyerClubId:Int=0,var sellerClubId:Int=0,var playerId:Int=0,var type:DealType=DealType.BUY,var role:SquadRole=SquadRole.ROTATION,var fee:Long=0,var wage:Int=0,var signingBonus:Long=0,var sellOnPercent:Int=0,var durationYears:Int=3,var releaseClause:Long=0,var buyOption:Long=0,var buyBackClause:Long=0,var swapPlayerId:Int=0,var playingTimePromise:Int=55,var sellerScore:Int=0,var playerScore:Int=0,var agentScore:Int=0,var status:NegotiationStatus=NegotiationStatus.DRAFT,var round:Int=0,var message:String="",var stage:TransferStage=TransferStage.SCOUTING,var medicalPassed:Boolean=false,var medicalRisk:Int=0,var medicalNote:String="",var registrationReady:Boolean=false,var loanWeeksRequested:Int=24,var recallAllowed:Boolean=true,var targetYouthSquad:YouthSquad?=null)
@Serializable data class MatchAiProfile(var weakness:String="Keine klare Schwäche",var approach:BuildUp=BuildUp.MIXED,var pressingTrap:String="Mittelfeld lenken",var pressureTargetId:Int=0,var adaptation:Int=0,var lastAdjustmentMinute:Int=0,var humanErrorRate:Int=10,var timeWaste:Int=0,var tacticalFoulBias:Int=0,var overloadSide:Int=0,var widthBias:Int=0)

@Serializable data class Attributes(var pace: Int=35,var finishing: Int=35,var passing: Int=35,var technique: Int=35,var tackling: Int=35,var strength: Int=35,var stamina: Int=35,var vision: Int=35,var heading: Int=35,var keeping: Int=15,var setPieces: Int=35) {
 fun values()=linkedMapOf("Tempo" to pace,"Abschluss" to finishing,"Pässe" to passing,"Technik" to technique,"Zweikampf" to tackling,"Kraft" to strength,"Ausdauer" to stamina,"Spielwitz" to vision,"Kopfball" to heading,"Torwart" to keeping,"Standards" to setPieces)
 fun improve(focus: Focus,cap: Int): Boolean {
  fun up(v: Int)=if(v>=cap.coerceIn(1,99))v else v+1
  val before=values()
  when(focus){Focus.FINISHING->finishing=up(finishing);Focus.PACE->pace=up(pace);Focus.TECHNIQUE->technique=up(technique);Focus.TACKLING->tackling=up(tackling);Focus.KEEPING->keeping=up(keeping);Focus.VISION->vision=up(vision)}
  return before!=values()
 }
 fun overall(pos: Position): Int=when(pos) {
  Position.TW->keeping*.65+vision*.1+passing*.1+strength*.1+pace*.05
  Position.IV->tackling*.35+heading*.2+strength*.2+vision*.15+pace*.1
  Position.LV,Position.RV->tackling*.25+pace*.25+stamina*.2+passing*.2+technique*.1
  Position.DM->tackling*.25+passing*.25+vision*.2+stamina*.2+strength*.1
  Position.ZM->passing*.3+vision*.25+technique*.2+stamina*.15+tackling*.1
  Position.OM->vision*.3+technique*.25+passing*.25+finishing*.2
  Position.LA,Position.RA->pace*.3+technique*.3+passing*.15+finishing*.2+stamina*.05
  Position.ST->finishing*.4+pace*.2+technique*.15+heading*.15+strength*.1
 }.roundToInt().coerceIn(1,99)
}
@Serializable data class Hidden(var potential: Int=65,var injuryProneness: Int=30,var consistency: Int=60,var professionalism: Int=60,var loyalty: Int=65,var ambition: Int=60,var development: Int=65,var pressure: Int=50)
@Serializable data class Stats(var appearances: Int=0,var goals: Int=0,var assists: Int=0,var minutes: Int=0,var yellow: Int=0,var red: Int=0)
@Serializable data class Appearance(var skin: Int=1,var hair: Int=1,var beard: Int=0,var build: Int=1)
@Serializable data class PlayerSeason(val season: Int,val clubName: String,val stats: Stats)
@Serializable data class Player(val id: Int,var clubId: Int=0,var firstName: String="",var lastName: String="",var birthYear: Int=2000,var nationality: String="Deutschland",var height: Int=180,var weight: Int=78,var foot: Foot=Foot.RIGHT,var position: Position=Position.ZM,var secondary: MutableList<Position> = mutableListOf(),var number: Int=10,var appearance: Appearance=Appearance(),var archetype: String="Komplett",var attributes: Attributes=Attributes(),var hidden: Hidden=Hidden(),var fitness: Double=95.0,var morale: Int=65,var form: Double=6.5,var sharpness: Int=60,var injuryWeeks: Int=0,var injury: String="",var unavailableReason: UnavailableReason?=null,var unavailableWeeks: Int=0,var stats: Stats=Stats(),var career: MutableList<PlayerSeason> = mutableListOf(),var youth: Boolean=false,var wantsMove: Boolean=false,var wage: Int=0,var retired: Boolean=false,var trainingProgress: Double=0.0,var lastTalkWeek: Int=-1,var messiMentored: Boolean=false,var role:PlayerRole=PlayerRole.AUTO,var youthProfile:YouthProfile=YouthProfile(),var contractYears:Int=2,var promisedRole:SquadRole=SquadRole.ROTATION,var agentId:Int=0,var marketUncertainty:Int=20,var loanParentClubId:Int=0,var loanBuyerClubId:Int=0,var loanOptionFee:Long=0,var loanWeeks:Int=0,var homegrownClubId:Int=0,var releaseClause:Long=0,var buyBackClubId:Int=0,var buyBackFee:Long=0,var sellOnPercentToPrevious:Int=0,var youthSquad:YouthSquad=YouthSquad.U19,var youthTeamStats:YouthTeamStats=YouthTeamStats(),var precontractClubId:Int=0,var precontractSeason:Int=0,var precontractWage:Int=0,var precontractYears:Int=0,var loanRecallAllowed:Boolean=true,var loanReturnYouth:Boolean=false,var loanReturnYouthSquad:YouthSquad=YouthSquad.U19,var temporarySeniorCallUp:Boolean=false,var temporaryReturnSquad:YouthSquad?=null,var generated:Boolean=false) {
 val name get()=listOf(firstName,lastName).filter{it.isNotBlank()}.joinToString(" ")
 val ca get()=attributes.overall(position)
 val available get()=!retired&&!youth&&injuryWeeks==0&&unavailableWeeks==0
 fun fit(pos: Position)=when{
  pos==position->1.0
  pos in secondary->.98
  else->positionAffinity(position,pos)
 }
 fun ratingAt(pos: Position)=(attributes.overall(pos)*fit(pos)).roundToInt().coerceIn(1,99)
 fun secondaryOptions(): List<Position> {
  val base=ca
  return Position.entries.filter{it!=position&&it!=Position.TW&&position!=Position.TW}
   .filter{it in secondary||(positionAffinity(position,it)>=.88&&ratingAt(it)>=base-10)}
   .sortedWith(compareByDescending<Position>{if(it in secondary)1 else 0}.thenByDescending{ratingAt(it)}).take(4)
 }
 fun roleAt(pos: Position)=when{
  pos==position->"Hauptposition"
  pos in secondary->"Nebenposition"
  position==Position.TW||pos==Position.TW->"ungeeignet"
  positionAffinity(position,pos)>=.92->"sehr vertraut"
  positionAffinity(position,pos)>=.88->"verwandte Position"
  positionAffinity(position,pos)>=.84->"spielbar"
  else->"positionsfremd"
 }
}
@Serializable data class Logo(var template: Int=0,var letters: String="GE")
@Serializable data class Kit(var primary: Long=0xFF287254,var secondary: Long=0xFFE7EEE5,var pattern: Int=0)
@Serializable data class Kits(var home: Kit=Kit(),var away: Kit=Kit(0xFFE7EEE5,0xFF287254,1),var third: Kit?=null,var keeper: Kit=Kit(0xFFDF663D,0xFF17251E),var training: Kit=Kit(0xFF1A2722,0xFFE7EEE5,2))
@Serializable data class Stadium(var name: String="Sportplatz am Waldrand",var capacity: Int=250,var seats: Int=0,var pitchQuality: Int=35,var surface: Surface=Surface.HARD,var floodlights: Boolean=false,var cabin: Int=15,var stand: Int=5,var training: Int=15,var clubhouse: Int=12,var youth: Int=10,var gym: Int=0,var medicine: Int=5)
@Serializable data class Tactics(
 var formation: String="4-4-2",
 var mentality: Int=3,
 var pressing: Int=3,
 var line: Int=3,
 var tempo: Int=3,
 var width: Int=3,
 var buildUp: BuildUp=BuildUp.MIXED,
 var xi: MutableList<Int> = mutableListOf(),
 var bench: MutableList<Int> = mutableListOf(),
 var captainId: Int=0,
 var targetPlayerId: Int=0,
 var penaltyTakerId: Int=0,
 var freeKickTakerId: Int=0,
 var cornerLeftTakerId: Int=0,
 var cornerRightTakerId: Int=0,
 var roles: MutableMap<Int,PlayerRole> = linkedMapOf(),
 var instructions: MutableMap<Int,MutableList<PlayerInstruction>> = linkedMapOf()
)
@Serializable data class Sponsor(var name: String="Bäckerei am Markt",var weekly: Int=210)
@Serializable data class SponsorDeal(var id:Int=0,var category:SponsorCategory=SponsorCategory.REGIONAL,var name:String="",var weekly:Int=0,var signingBonus:Long=0,var performanceBonus:Int=0,var weeksLeft:Int=40,var minReputation:Int=0,var youthBoost:Int=0,var membersBoost:Int=0,var active:Boolean=false,var acceptedWeek:Int=-1)
@Serializable data class AssistantCoachState(var autoSeniorTraining:Boolean=false,var autoYouthTraining:Boolean=false,var autoSubstitutions:Boolean=false,var trainingStyle:AssistantTrainingStyle=AssistantTrainingStyle.BALANCED,var profile:AssistantCoachProfile=AssistantCoachProfile.BALANCED,var substitutionAggression:Int=3,var youthAggression:Int=2,var lastPlanReason:String="",var lastYouthReason:String="",var lastSubReason:String="")
@Serializable data class IntensiveTrainingProject(var playerId:Int=0,var focus:Focus=Focus.TECHNIQUE,var weeksLeft:Int=4,var totalWeeks:Int=4,var cost:Long=0,var progress:Double=0.0,var startedWeek:Int=0,var raisesPotential:Boolean=false)
@Serializable data class Club(val id: Int,var name: String,var shortName: String,var tier: Int,var city: String="",var founded: Int=2026,var primary: Long=0xFF287254,var secondary: Long=0xFFE7EEE5,var logo: Logo=Logo(),var kits: Kits=Kits(),var stadium: Stadium=Stadium(),var budget: Long=12500,var wageBill: Int=0,var philosophy: String="Zusammenhalt",var playPhilosophy: String="Direkt nach vorn",var youthPhilosophy: String="Talente aus dem Ort",var tactics: Tactics=Tactics(),var sponsor: Sponsor=Sponsor(),var members: Int=55,var reputation: Int=10,var form: MutableList<String> = mutableListOf(),var lastIncome: Int=0,var lastCosts: Int=0,var dynamics:ClubDynamics=ClubDynamics(),var academy:AcademyState=AcademyState(),var sponsorDeals:MutableList<SponsorDeal> = mutableListOf(),var sponsorOffers:MutableList<SponsorDeal> = mutableListOf(),var commercialReputation:Int=30,var financialTrust:Int=60,var lastCommercialRefreshWeek:Int=-99)
@Serializable data class League(val tier: Int,var name: String,var clubIds: MutableList<Int> = mutableListOf())
@Serializable data class Calendar(var season: Int=2026,var matchday: Int=1,var absoluteWeek: Int=0)
@Serializable data class User(val clubId: Int,val playerId: Int,var difficulty: Difficulty=Difficulty.NORMAL,var tutorialEnabled:Boolean=false,var tutorialStep:Int=0,var tutorialCompleted:Boolean=false,var playerCareerFocus:PlayerCareerFocus=PlayerCareerFocus.BALANCED,var lastSeenChangelogVersion:Int=0)
@Serializable data class Fixture(val id: Int,val season: Int,val tier: Int,val matchday: Int,val homeId: Int,val awayId: Int,var played: Boolean=false,val competition: CompetitionType=CompetitionType.LEAGUE,val round: Int=0,val stage: String="",val group: String="",var winnerId: Int=0,var homePens: Int=0,var awayPens: Int=0)
@Serializable data class MatchStats(var goals: Int=0,var xg: Double=0.0,var shots: Int=0,var shotsOnTarget: Int=0,var shotsOffTarget: Int=0,var blockedShots: Int=0,var possessionTicks: Int=0,var corners: Int=0,var fouls: Int=0,var offsides: Int=0,var throwIns: Int=0,var varChecks: Int=0)
@Serializable data class PlayerMatchPerformance(
 var minutes: Int=0,
 var goals: Int=0,
 var assists: Int=0,
 var chancesCreated: Int=0,
 var shots: Int=0,
 var shotsOnTarget: Int=0,
 var shotsOffTarget: Int=0,
 var xg: Double=0.0,
 var passesAttempted: Int=0,
 var passesCompleted: Int=0,
 var turnovers: Int=0,
 var defensiveActions: Int=0,
 var yellows: Int=0,
 var red: Int=0,
 var goalsConceded: Int=0,
 var fitnessStart: Double=100.0,
 var rating: Double=6.5,
 var summary: String="Ordentlicher, unauffälliger Auftritt."
)
@Serializable data class Ticker(val minute: Int,val text: String,val tone: String="normal",val clockLabel: String="")
@Serializable data class Goal(val playerId: Int,val assistId: Int=0,val minute: Int,val clockLabel: String="",val targetLabel: String="")
@Serializable data class PenaltyKick(val clubId: Int,val playerId: Int,val keeperId: Int,val scored: Boolean,val outcome: ShotOutcome,val targetX: Float,val targetY: Float,val targetLabel: String,val clockLabel: String)
@Serializable data class MatchRecord(val fixtureId: Int,val homeId: Int,val awayId: Int,val home: MatchStats,val away: MatchStats,val minute: Int,val goals: List<Goal> = emptyList(),val attendance: Int=0,val shotEvents: List<ShotEvent> = emptyList(),val homePens: Int=0,val awayPens: Int=0,val extraTimePlayed: Boolean=false,val passEvents: List<PassEvent> = emptyList(),val tacticChanges: List<TacticChangeEvent> = emptyList())
@Serializable data class LiveMatch(
 val fixtureId: Int,
 val homeId: Int,
 val awayId: Int,
 var rngState: Long,
 var minute: Int=0,
 val duration: Int=93,
 var home: MatchStats=MatchStats(),
 var away: MatchStats=MatchStats(),
 var homeXi: MutableList<Int> = mutableListOf(),
 var awayXi: MutableList<Int> = mutableListOf(),
 var homeBench: MutableList<Int> = mutableListOf(),
 var awayBench: MutableList<Int> = mutableListOf(),
 var homeSubs: Int=0,
 var awaySubs: Int=0,
 var homeMentality: Int=3,
 var awayMentality: Int=3,
 var homeFormation: String="4-4-2",
 var awayFormation: String="4-4-2",
 var ticker: MutableList<Ticker> = mutableListOf(),
 var goals: MutableList<Goal> = mutableListOf(),
 var pendingDecision: Boolean=false,
 var decisionXg: Double=.12,
 var halfTime: Boolean=false,
 var secondHalf: Boolean=false,
 var period: Int=1,
 var breakType: MatchBreakType=MatchBreakType.NONE,
 var stoppageMinute: Int=0,
 var firstHalfAdded: Int=0,
 var secondHalfAdded: Int=0,
 var extraFirstAdded: Int=0,
 var extraSecondAdded: Int=0,
 var stoppageAnnounced: Boolean=false,
 var stoppageLossSeconds: Int=0,
 var knockout: Boolean=false,
 var extraTimePlayed: Boolean=false,
 var shootoutActive: Boolean=false,
 var homePens: Int=0,
 var awayPens: Int=0,
 var shootoutHomeTaken: Int=0,
 var shootoutAwayTaken: Int=0,
 var shootoutHomeTurn: Boolean=true,
 var penaltyShootout: MutableList<PenaltyKick> = mutableListOf(),
 var kickoffHomeFirst: Boolean=true,
 var extraKickoffHome: Boolean=true,
 var finished: Boolean=false,
 var weather: String="Trocken",
 var attendance: Int=80,
 var yellows: MutableMap<Int,Int> = mutableMapOf(),
 var sentOff: MutableList<Int> = mutableListOf(),
 var injured: MutableList<Int> = mutableListOf(),
 var minutesPlayed: MutableMap<Int,Int> = mutableMapOf(),
 var decisionCooldown: Int=0,
 var participation: MutableList<Int> = mutableListOf(),
 var ballX: Float=.5f,
 var ballY: Float=.5f,
 var homeInPossession: Boolean=true,
 var livePhase: LivePhase=LivePhase.POSSESSION,
 var liveClubId: Int=0,
 var livePlayerId: Int=0,
 var liveEventSerial: Int=0,
 var attackMomentum: Int=0,
 var momentumHistory: MutableList<Int> = mutableListOf(),
 var incidentPause: Boolean=false,
 var incidentReason: MatchPauseReason=MatchPauseReason.NONE,
 var incidentPlayerId: Int=0,
 var incidentClubId: Int=0,
 var pendingCornerClubId: Int=0,
 var pendingCornerPlayerId: Int=0,
 var cornerAwaitingResolution: Boolean=false,
 var pendingSetPieceClubId: Int=0,
 var pendingSetPieceType: SetPieceType=SetPieceType.NONE,
 var pendingSetPiecePlayerId: Int=0,
 var pendingSetPieceX: Float=.5f,
 var pendingSetPieceY: Float=.5f,
 var setPieceAwaitingResolution: Boolean=false,
 var pendingPossessionClubId: Int=0,
 var pendingPossessionReason: PossessionChangeReason=PossessionChangeReason.NONE,
 var possessionChangeReason: PossessionChangeReason=PossessionChangeReason.KICK_OFF,
 var lastPossessionChangeEventSerial: Int=1,
 var chainOwnerClubId: Int=0,
 var chainStep: Int=0,
 var chainTicks: Int=0,
 var counterTicksRemaining: Int=0,
 var liveDetail: String="",
 var homeConserveEnergy: Boolean=false,
 var awayConserveEnergy: Boolean=false,
 var homeAllOutAttack: Boolean=false,
 var awayAllOutAttack: Boolean=false,
 var homeControlGame: Boolean=false,
 var awayControlGame: Boolean=false,
 var decisionShotType: ShotType=ShotType.BOX_SHOT,
 var decisionAssistId: Int=0,
 var lastShotType: ShotType=ShotType.BOX_SHOT,
 var lastShotX: Float=.5f,
 var lastShotY: Float=.5f,
 var lastShotXg: Double=0.0,
 var lastShotGoalProbability: Double=0.0,
 var lastShotTargetX: Float=.5f,
 var lastShotTargetY: Float=.5f,
 var lastShotTargetLabel: String="",
 var lastShotOutcome: ShotOutcome=ShotOutcome.OFF_TARGET,
 var shotEvents: MutableList<ShotEvent> = mutableListOf(),
 var passEvents: MutableList<PassEvent> = mutableListOf(),
 var tacticChanges: MutableList<TacticChangeEvent> = mutableListOf(),
 var playerPerformance: MutableMap<Int,PlayerMatchPerformance> = mutableMapOf(),
 var pendingVarShotIndex: Int=-1,
 var pendingVarKeeperId: Int=0,
 var varReviewStage: Int=0,
 var varReviewReason: String="",
 var varReviewResult: String="",
 var varWillOverturn: Boolean=false,
 var lastThrowInMinute: Int=-1,
 var homeAi:MatchAiProfile=MatchAiProfile(),
 var awayAi:MatchAiProfile=MatchAiProfile(),
 var assistantSubPending:Boolean=false,
 var assistantSubOutId:Int=0,
 var assistantSubInId:Int=0,
 var assistantSubReason:String="",
 var assistantSubSuggestedMinute:Int=-1,
 var assistantSubRejectedOutId:Int=0,
 var assistantSubRejectedInId:Int=0,
 var assistantSubRejectedUntilMinute:Int=0,
 var assistantSubOutIds:MutableList<Int> = mutableListOf(),
 var assistantSubInIds:MutableList<Int> = mutableListOf(),
 var assistantSubReasons:MutableList<String> = mutableListOf(),
 var assistantSubRejectedPairs:MutableList<String> = mutableListOf(),
 var assistantSubRejectedPlayers:MutableList<Int> = mutableListOf(),
 var assistantNextSuggestionMinute:Int=0,
 var assistantLastBatchSize:Int=0,
 var homeSubWindows:Int=0,
 var awaySubWindows:Int=0,
 var lastHomeSubMinute:Int=-1,
 var lastAwaySubMinute:Int=-1,
 var keeperControlSeconds:Int=0,
 var chainPasses:Int=0,
 var chainZone:String="",
 var chainNarrative:String="",
 var ballTrace:MutableList<BallFrame> = mutableListOf()

)
@Serializable data class IndividualFocus(val playerId: Int,val focus: Focus)
@Serializable data class TrainingReport(var week: Int=0,var gains: MutableList<String> = mutableListOf(),var injuries: MutableList<String> = mutableListOf(),var averageFitness: Int=0,var effectiveDays: List<Int> = emptyList(),var effects:MutableList<String> = mutableListOf())
@Serializable data class TrainingPlan(var days: MutableList<UnitType> = mutableListOf(UnitType.RECOVERY,UnitType.TECHNIQUE,UnitType.OFF,UnitType.TACTICS,UnitType.FINISHING,UnitType.OFF,UnitType.OFF),var extra: MutableList<IndividualFocus> = mutableListOf(),var lastReport: TrainingReport=TrainingReport(),var intensity:Int=3,var opponentPrep:Boolean=true,var secretSession:Boolean=false)
@Serializable data class ConstructionProject(val id: Int,val clubId: Int,val facility: Facility,var weeksLeft: Int,val totalWeeks: Int,val cost: Long,val delta: Int=15)
@Serializable data class NewsItem(val id: Int,val week: Int,val title: String,val text: String,val tone: String="normal")
@Serializable data class WorldEvent(val week: Int,val type: String,val playerId: Int=0,val amount: Int=0,val description: String="")
@Serializable data class Rivalry(val a: Int,val b: Int,var intensity: Int=35)
@Serializable data class SeasonHistory(val season: Int,val clubId: Int,val league: String,val rank: Int,val points: Int,val goals: Int,val outcome: String,val awards: List<String> = emptyList())
@Serializable data class Trophy(val season:Int,val competition:String,val clubId:Int,val prize:Long)
@Serializable data class NextIds(var player: Int=1,var fixture: Int=1,var news: Int=1,var construction: Int=1,var negotiation:Int=1,var sponsor:Int=1)
@Serializable data class World(var saveVersion: Int=SAVE_VERSION,val seed: Long,var rngState: Long=seed,var calendar: Calendar=Calendar(),val user: User,var leagues: MutableList<League> = mutableListOf(),var clubs: MutableMap<Int,Club> = linkedMapOf(),var players: MutableMap<Int,Player> = linkedMapOf(),var fixtures: MutableList<Fixture> = mutableListOf(),var matches: MutableMap<Int,MatchRecord> = linkedMapOf(),var live: LiveMatch?=null,var news: MutableList<NewsItem> = mutableListOf(),var events: MutableList<WorldEvent> = mutableListOf(),var rivalries: MutableList<Rivalry> = mutableListOf(),var relationships: MutableMap<Int,Int> = mutableMapOf(),var training: TrainingPlan=TrainingPlan(),var construction: MutableList<ConstructionProject> = mutableListOf(),var history: MutableList<SeasonHistory> = mutableListOf(),var nextIds: NextIds=NextIds(),var privateTopClubMode: Boolean=false,var clubRelations:MutableMap<String,Int> = mutableMapOf(),var agentRelations:MutableMap<Int,Int> = mutableMapOf(),var negotiations:MutableMap<Int,TransferOffer> = linkedMapOf(),var assistantCoach:AssistantCoachState=AssistantCoachState(),var intensiveTraining:MutableList<IntensiveTrainingProject> = mutableListOf(),var watchlist:MutableList<Int> = mutableListOf(),var scoutReports:MutableMap<Int,ScoutReport> = linkedMapOf(),var scoutAssignments:MutableMap<Int,ScoutAssignment> = linkedMapOf(),var competingBids:MutableList<CompetingBid> = mutableListOf(),var transferHistory:MutableList<TransferHistoryEntry> = mutableListOf(),var savedTactics:MutableMap<String,SavedTactic> = linkedMapOf(),var paidSeasonPrizes:MutableSet<String> = linkedSetOf(),var notifications:NotificationState=NotificationState(),var trophies:MutableList<Trophy> = mutableListOf(),var lastLeagueRankings:MutableMap<Int,MutableList<Int>> = linkedMapOf(),var fantasyCupEnabled:Boolean=true,var fantasyCupByes:MutableList<Int> = mutableListOf()) {
 fun club()=clubs.getValue(user.clubId)
 fun self()=players.getValue(user.playerId)
 fun squad(clubId: Int=user.clubId)=players.values.filter{it.clubId==clubId&&!it.retired}
 fun nextFixture(): Fixture? {
  live?.let{active->fixtures.firstOrNull{it.id==active.fixtureId&&!it.played}?.let{return it}}
  return fixtures.filter{it.matchday==calendar.matchday&&!it.played&&(it.homeId==user.clubId||it.awayId==user.clubId)}.minWithOrNull(compareBy<Fixture>{it.competition.sortPriority()}.thenBy{it.id})
 }
 fun isDerby(a: Int,b: Int)=rivalries.any{(it.a==a&&it.b==b)||(it.a==b&&it.b==a)}
 fun news(title: String,text: String,tone: String="normal"){news.add(0,NewsItem(nextIds.news++,calendar.absoluteWeek,title,text,tone));while(news.size>100)news.removeAt(news.lastIndex)}
}
@Serializable data class ClubDraft(var name: String="FC Gründerelf",var shortName: String="FGE",var founded: Int=2026,var city: String="",var primary: Long=0xFF287254,var secondary: Long=0xFFE7EEE5,var logo: Logo=Logo(0,"FGE"),var kits: Kits=Kits(),var stadiumName: String="Sportplatz am Waldrand",var capacity: Int=250,var philosophy: String="Zusammenhalt",var playPhilosophy: String="Direkt nach vorn",var youthPhilosophy: String="Talente aus dem Ort",var difficulty: Difficulty=Difficulty.NORMAL)
@Serializable data class PlayerDraft(var firstName: String="",var lastName: String="",var birthYear: Int=2000,var nationality: String="Deutschland",var height: Int=180,var weight: Int=78,var foot: Foot=Foot.RIGHT,var position: Position=Position.ZM,var secondary: MutableList<Position> = mutableListOf(),var number: Int=10,var appearance: Appearance=Appearance(),var archetype: String="Komplett",var attributes: Attributes=Attributes(50,50,50,50,50,50,50,50,50,15,50))
data class TableRow(val clubId: Int,var played: Int=0,var won: Int=0,var drawn: Int=0,var lost: Int=0,var goalsFor: Int=0,var goalsAgainst: Int=0){val points get()=won*3+drawn;val difference get()=goalsFor-goalsAgainst}

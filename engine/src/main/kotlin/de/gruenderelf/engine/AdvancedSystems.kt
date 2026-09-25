package de.gruenderelf.engine

import kotlin.math.abs
import kotlin.math.roundToInt

fun Player.personalityType(season:Int):PersonalityType{
 val age=season-birthYear
 return when{
  hidden.pressure>=78&&hidden.consistency>=72->PersonalityType.BIG_GAME
  hidden.professionalism>=78&&hidden.loyalty>=65->PersonalityType.PROFESSIONAL
  hidden.pressure<=36&&hidden.ambition>=55->PersonalityType.CHOKER
  ca>=72&&hidden.professionalism<=38->PersonalityType.LAZY_STAR
  age<=21&&hidden.ambition>=72&&hidden.consistency<=58->PersonalityType.WILD_YOUNGSTER
  hidden.pressure>=68&&hidden.professionalism>=65&&age>=27->PersonalityType.LEADER
  else->PersonalityType.BALANCED
 }
}

fun Player.effectiveRole():PlayerRole = if(role!=PlayerRole.AUTO)role else when(position){
 Position.TW->PlayerRole.BUILD_UP_KEEPER
 Position.IV->if(attributes.passing+attributes.vision>=attributes.tackling+attributes.strength)PlayerRole.BALL_PLAYING_CB else PlayerRole.STOPPER
 Position.LV,Position.RV->if(attributes.pace+attributes.stamina>=attributes.tackling+attributes.strength)PlayerRole.OVERLAPPING_FULLBACK else PlayerRole.INVERTED_FULLBACK
 Position.DM->if(attributes.passing+attributes.vision>=attributes.tackling+attributes.strength)PlayerRole.DEEP_PLAYMAKER else PlayerRole.ANCHOR
 Position.ZM->PlayerRole.BOX_TO_BOX
 Position.OM->PlayerRole.PLAYMAKER
 Position.LA,Position.RA->if(attributes.finishing>=attributes.passing)PlayerRole.INSIDE_FORWARD else PlayerRole.WINGER
 Position.ST->when{attributes.strength+attributes.heading>attributes.pace+attributes.technique+8->PlayerRole.TARGET_FORWARD;attributes.stamina+hidden.professionalism>135->PlayerRole.PRESSING_FORWARD;else->PlayerRole.POACHER}
}

object ClubSystems {
 fun clamp(c:Club){with(c.dynamics){chemistry=chemistry.coerceIn(0,100);tacticalUnderstanding=tacticalUnderstanding.coerceIn(0,100);pressingCoordination=pressingCoordination.coerceIn(0,100);mentalHardness=mentalHardness.coerceIn(0,100);leadership=leadership.coerceIn(0,100);hierarchyStability=hierarchyStability.coerceIn(0,100);languageCohesion=languageCohesion.coerceIn(0,100);fatigueLoad=fatigueLoad.coerceIn(0,100);opponentPrep=opponentPrep.coerceIn(0,100);staffQuality=staffQuality.coerceIn(10,100);patterns.keys.toList().forEach{patterns[it]=(patterns[it]?:0).coerceIn(0,100)}}}
 fun relationKey(a:Int,b:Int)=if(a<b)"$a:$b" else "$b:$a"
 fun clubRelation(w:World,a:Int,b:Int)=w.clubRelations[relationKey(a,b)]?:50
 fun changeClubRelation(w:World,a:Int,b:Int,delta:Int){val k=relationKey(a,b);w.clubRelations[k]=((w.clubRelations[k]?:50)+delta).coerceIn(0,100)}
}

object YouthEngine {
 fun readiness(w:World,p:Player):Int{
  val c=w.clubs[p.clubId]?:return 0;val y=p.youthProfile;val age=w.calendar.season-p.birthYear
  val roleFit=if(y.roleSpark==PlayerRole.AUTO||y.roleSpark==p.effectiveRole())10 else 0
  val physical=((p.attributes.strength+p.attributes.stamina+p.attributes.pace)/3.0).roundToInt()
  val mental=(p.hidden.professionalism+p.hidden.pressure+y.maturity)/3
  val level=(p.ca*42+physical*18+mental*22+y.confidence*8+c.dynamics.tacticalUnderstanding*10)/100
  val ageGate=when{age<=15->-16;age==16->-8;age>=19->5;else->0}
  return (level+roleFit+ageGate-y.schoolStress/8-y.injuryGrowthRisk/10).coerceIn(0,100)
 }
 fun riskLabel(w:World,p:Player):String{val y=p.youthProfile;val ready=readiness(w,p);return when{y.growthSpurtWeeks>0->"Wachstumsschub: Belastung begrenzen";y.schoolStress>=70->"Schulstress hoch";y.adviserPressure>=75->"Berater drängt auf nächsten Schritt";ready<45->"Profifußball wäre aktuell Überforderung";ready<65->"Dosierte Profi-Minuten sinnvoll";else->"Bereit für echte Kaderchancen"}}
 fun configureClub(c:Club){
  if(c.academy.identity==AcademyIdentity.BALANCED)c.academy.identity=when(c.youthPhilosophy){"Leistungsförderung"->AcademyIdentity.POSSESSION;"Breitensport"->AcademyIdentity.ATHLETIC;else->when(c.playPhilosophy){"Direkt nach vorn"->AcademyIdentity.ATHLETIC;"Ballbesitz"->AcademyIdentity.POSSESSION;else->AcademyIdentity.STREET}}
  c.academy.scouting=maxOf(c.academy.scouting,(c.stadium.youth*.75).roundToInt());c.academy.u19Quality=maxOf(c.academy.u19Quality,(c.stadium.youth*.70).roundToInt());c.academy.u23Quality=maxOf(c.academy.u23Quality,(c.stadium.training*.55).roundToInt());c.dynamics.staffQuality=maxOf(c.dynamics.staffQuality,((c.stadium.training+c.stadium.youth+c.stadium.medicine)/3.0).roundToInt())
 }
 fun seedProfile(w:World,c:Club,p:Player,rng:SeededRandom){configureClub(c);val golden=c.academy.goldenGeneration;val identity=c.academy.identity
  p.youthProfile=YouthProfile(learning=(45+c.academy.scouting/4+rng.int(0,30)+(if(golden)6 else 0)).coerceAtMost(99),maturity=rng.int(35,72),path=when(rng.int(0,99)){in 0..16->DevelopmentPath.EARLY;in 17..32->DevelopmentPath.LATE;in 33..38->DevelopmentPath.PLATEAU;else->DevelopmentPath.NORMAL},familySupport=rng.int(35,90),adviserPressure=rng.int(15,72),schoolStress=rng.int(10,58),injuryGrowthRisk=rng.int(10,45),roleSpark=when(identity){AcademyIdentity.POSSESSION->rng.pick(listOf(PlayerRole.PLAYMAKER,PlayerRole.DEEP_PLAYMAKER,PlayerRole.BALL_PLAYING_CB));AcademyIdentity.ATHLETIC->rng.pick(listOf(PlayerRole.PRESSING_FORWARD,PlayerRole.OVERLAPPING_FULLBACK,PlayerRole.BOX_TO_BOX));AcademyIdentity.STREET->rng.pick(listOf(PlayerRole.INSIDE_FORWARD,PlayerRole.PLAYMAKER,PlayerRole.POACHER));AcademyIdentity.DEFENSIVE->rng.pick(listOf(PlayerRole.STOPPER,PlayerRole.ANCHOR,PlayerRole.INVERTED_FULLBACK));AcademyIdentity.PRESSING->rng.pick(listOf(PlayerRole.PRESSING_FORWARD,PlayerRole.BOX_TO_BOX,PlayerRole.OVERLAPPING_FULLBACK));else->PlayerRole.AUTO},homegrownYears=1)
 }
 fun weekly(w:World,rng:SeededRandom){
  for(c in w.clubs.values){configureClub(c);for(p in w.squad(c.id).filter{it.youth || w.calendar.season-it.birthYear<=21}){val y=p.youthProfile;val age=w.calendar.season-p.birthYear
    val minutesDelta=(p.stats.minutes-y.lastTrackedMinutes).coerceAtLeast(0);y.lastTrackedMinutes=p.stats.minutes
    if(y.growthSpurtWeeks>0)y.growthSpurtWeeks-- else if(age in 15..18&&rng.chance(.018)){y.growthSpurtWeeks=rng.int(2,5);y.injuryGrowthRisk=(y.injuryGrowthRisk+18).coerceAtMost(100)}
    val target=when{age<=16->0;age==17->25;age==18->45;else->65}
    val overload=(minutesDelta-target).coerceAtLeast(0);val under=(target/2-minutesDelta).coerceAtLeast(0)
    if(overload>0){y.confidence=(y.confidence+2).coerceAtMost(100);p.fitness=(p.fitness-overload*.05).coerceAtLeast(20.0);y.injuryGrowthRisk=(y.injuryGrowthRisk+overload/6).coerceAtMost(100)}
    if(under>0&&age>=18)y.confidence=(y.confidence-1).coerceAtLeast(10)
    if(y.mentorId==0)y.mentorId=w.squad(c.id).filter{!it.youth&&w.calendar.season-it.birthYear>=25}.maxByOrNull{it.hidden.professionalism+it.hidden.pressure+it.hidden.consistency}?.id?:0
    val mentor=w.players[y.mentorId];if(mentor?.clubId==c.id&&mentor.hidden.professionalism>=65){y.maturity=(y.maturity+1).coerceAtMost(100);p.hidden.professionalism=(p.hidden.professionalism+1).coerceAtMost(100)}
    if(y.seniorTraining){y.maturity=(y.maturity+1).coerceAtMost(100);p.sharpness=(p.sharpness+2).coerceAtMost(100);if(c.dynamics.fatigueLoad>70)p.fitness=(p.fitness-1.5).coerceAtLeast(20.0)}
    val spark=if(y.roleSpark==p.effectiveRole())1.28 else 1.0;val path=when(y.path){DevelopmentPath.EARLY->if(age<=19)1.25 else .75;DevelopmentPath.LATE->if(age>=19)1.30 else .72;DevelopmentPath.PLATEAU->.42;DevelopmentPath.BREAKTHROUGH->1.55;DevelopmentPath.CRASH->.25;else->1.0}
    val ecosystem=if(p.youth&&p.youthSquad==YouthSquad.U19)c.academy.u19Quality else c.academy.u23Quality
    val youthSquadDevelopment=if(p.youth)1.10 else 1.0
    val assistantDevelopment=if(c.id==w.user.clubId&&w.assistantCoach.autoYouthTraining){
     val profile=when(w.assistantCoach.profile){AssistantCoachProfile.DEVELOPER->.10;AssistantCoachProfile.ANALYST->.03;AssistantCoachProfile.MOTIVATOR->.04;else->.05}
     val style=when(w.assistantCoach.trainingStyle){AssistantTrainingStyle.YOUTH->.10;AssistantTrainingStyle.DEVELOPMENT->.08;else->.02}
     1.12+profile+style+w.assistantCoach.youthAggression.coerceIn(1,5)*.012
    }else 1.0
    p.trainingProgress+=.014*(.55+y.learning/100.0)*(1+c.stadium.youth/140.0+ecosystem/240.0)*spark*path*assistantDevelopment*youthSquadDevelopment
    if(p.trainingProgress>=1.0){p.trainingProgress-=1.0;p.attributes.improve(rng.pick(Focus.entries),p.hidden.potential)}
    if(y.adviserPressure>82&&readiness(w,p)>68&&rng.chance(.04)){p.wantsMove=true;p.morale=(p.morale-4).coerceAtLeast(5)}
    if(y.schoolStress>75)p.sharpness=(p.sharpness-2).coerceAtLeast(0);if(y.familySupport<35&&p.hidden.professionalism<45&&rng.chance(.025)){p.morale=(p.morale-5).coerceAtLeast(5);y.confidence=(y.confidence-4).coerceAtLeast(5);y.schoolStress=(y.schoolStress+4).coerceAtMost(100)}
    y.homegrownYears=(y.homegrownYears+if(w.calendar.absoluteWeek%40==0)1 else 0).coerceAtMost(20)
   }}
 }
 fun newSeason(w:World,rng:SeededRandom){for(c in w.clubs.values){configureClub(c);val cycle=(c.academy.generationCycle+rng.int(-18,18)+(c.academy.scouting-50)/6).coerceIn(0,100);c.academy.generationCycle=cycle;c.academy.goldenGeneration=cycle>=78;if(c.academy.goldenGeneration)c.academy.droughtYears=0 else c.academy.droughtYears++}}
}

object TrainingSystems {
 private fun aiPlan(c:Club):TrainingPlan{val tactical=if(c.tactics.pressing>=4)UnitType.TACTICS else UnitType.POSITIONAL;val technical=if(c.tactics.buildUp in listOf(BuildUp.SHORT,BuildUp.TIKI_TAKA))UnitType.TECHNIQUE else UnitType.DUELS;return TrainingPlan(mutableListOf(UnitType.RECOVERY,tactical,UnitType.VIDEO,technical,UnitType.FITNESS,UnitType.SET_PIECES,UnitType.OFF),intensity=if(c.dynamics.fatigueLoad>65)2 else 3)}
 fun planFor(w:World,c:Club)=if(c.id==w.user.clubId){if(w.assistantCoach.autoSeniorTraining)AssistantCoachSystem.prepareUserPlan(w) else w.training}else aiPlan(c)
 fun effectiveDays(w:World,c:Club,plan:TrainingPlan):List<Int>{val cap=if(!w.privateTopClubMode&&c.tier>=7)3 else 7;return plan.days.indices.filter{plan.days[it] !in listOf(UnitType.OFF,UnitType.RECOVERY)}.take(cap)}
 fun applyAll(w:World)=w.random{rng->
  for(c in w.clubs.values){AssistantCoachSystem.prepareYouth(w,c);val plan=planFor(w,c);val effective=effectiveDays(w,c,plan);val sessions=effective.map{plan.days[it]};val report=TrainingReport(w.calendar.absoluteWeek,effectiveDays=effective);val intensity=plan.intensity.coerceIn(1,5);val staff=(.55+c.dynamics.staffQuality/100.0*.45)*BmwDeveloperSystems.trainingMultiplier(w,c.id)
   val load=(sessions.sumOf{maxOf(0,it.load)}*intensity/3.0).roundToInt();c.dynamics.fatigueLoad=(c.dynamics.fatigueLoad+load-sessions.count{it==UnitType.RECOVERY}*5-plan.days.count{it==UnitType.OFF}*4).coerceIn(0,100)
   fun add(v:Int,delta:Double)=(v+delta*staff).roundToInt().coerceIn(0,100)
   // Amateur clubs still have a cap for physically demanding training days. Low-load classroom/team units
   // (video, bonding, mental work, set pieces) must nevertheless create the tactical effect the manager selected.
   val videoUnits=plan.days.count{it==UnitType.VIDEO};val bondingUnits=plan.days.count{it==UnitType.TEAM_BONDING};val mentalUnits=plan.days.count{it==UnitType.MENTAL};val setPieceUnits=plan.days.count{it==UnitType.SET_PIECES}
   val tactics=sessions.count{it in listOf(UnitType.TACTICS,UnitType.POSITIONAL,UnitType.GAME)}+videoUnits;val press=sessions.count{it in listOf(UnitType.TACTICS,UnitType.DUELS,UnitType.GAME)};val mental=sessions.count{it==UnitType.GAME}+mentalUnits+bondingUnits
   val next=w.fixtures.filter{!it.played&&(it.homeId==c.id||it.awayId==c.id)}.minByOrNull{it.matchday};val secretBoost=if(plan.secretSession&&next!=null&&(w.isDerby(next.homeId,next.awayId)||CompetitionEngine.isDecisiveKnockoutFixture(w,next)))4.0 else 0.0
   c.dynamics.tacticalUnderstanding=add(c.dynamics.tacticalUnderstanding,tactics*1.5+secretBoost*.35);c.dynamics.pressingCoordination=add(c.dynamics.pressingCoordination,press*1.25);c.dynamics.mentalHardness=add(c.dynamics.mentalHardness,mentalUnits*2.2+secretBoost*.25);c.dynamics.chemistry=add(c.dynamics.chemistry,mental*1.8-(if(intensity>=5)2.5 else 0.0));c.dynamics.leadership=add(c.dynamics.leadership,mentalUnits*1.2);c.dynamics.hierarchyStability=add(c.dynamics.hierarchyStability,bondingUnits*2.0-(if(intensity>=5)1.0 else 0.0));c.dynamics.opponentPrep=add(c.dynamics.opponentPrep,(if(plan.opponentPrep)(videoUnits+sessions.count{it==UnitType.TACTICS})*3.0 else -2.0)+secretBoost);if(secretBoost>0)c.dynamics.fatigueLoad=(c.dynamics.fatigueLoad+3).coerceAtMost(100)
   val patterns=c.dynamics.patterns;fun pat(k:String,d:Double){patterns[k]=add(patterns[k]?:25,d)};pat("AUFBAU",sessions.count{it==UnitType.POSITIONAL||it==UnitType.TECHNIQUE}*2.0);pat("PRESSINGFALLE",sessions.count{it==UnitType.TACTICS||it==UnitType.DUELS}*1.8);pat("HALBRAUM",sessions.count{it==UnitType.POSITIONAL}*2.2);pat("FLUEGEL",sessions.count{it==UnitType.TACTICS||it==UnitType.TECHNIQUE}*1.1);pat("DIAGONALE",sessions.count{it==UnitType.TECHNIQUE||it==UnitType.VIDEO}*1.3);pat("RESTVERTEIDIGUNG",sessions.count{it==UnitType.TACTICS||it==UnitType.POSITIONAL}*1.8);pat("STANDARDS",setPieceUnits*3.2)
   val majority=w.squad(c.id).groupingBy{it.nationality}.eachCount().maxByOrNull{it.value}?.key;val languageBonus=w.squad(c.id).count{it.nationality==majority};c.dynamics.languageCohesion=add(c.dynamics.languageCohesion,(languageBonus-w.squad(c.id).size/2)*.08+sessions.count{it==UnitType.TEAM_BONDING}*.8)
   for(p in w.squad(c.id).filter{!it.retired&&(!it.youth||it.youthProfile.seniorTraining||w.intensiveTraining.any{pr->pr.playerId==it.id})}){if(p.injuryWeeks>0)continue;val blocked=p.unavailableReason in listOf(UnavailableReason.WORK,UnavailableReason.HOLIDAY,UnavailableReason.RESERVE);val recovery=plan.days.count{it==UnitType.RECOVERY}*3.0+plan.days.count{it==UnitType.OFF}*1.3;p.fitness=(p.fitness-(if(blocked)0.0 else load*.36)+recovery).coerceIn(5.0,100.0);if(blocked)continue;p.sharpness=(p.sharpness+sessions.size*2-intensity/2).coerceIn(0,100)
    val careerFocus=if(p.id==w.user.playerId)when(w.user.playerCareerFocus){PlayerCareerFocus.GOALGETTER->listOf(Focus.FINISHING);PlayerCareerFocus.PLAYMAKER->listOf(Focus.VISION);PlayerCareerFocus.ATHLETE->listOf(Focus.PACE);else->emptyList()}else emptyList()
    val foci=sessions.mapNotNull{when(it){UnitType.TECHNIQUE,UnitType.POSITIONAL->Focus.TECHNIQUE;UnitType.TACTICS,UnitType.VIDEO,UnitType.GAME,UnitType.MENTAL,UnitType.TEAM_BONDING->Focus.VISION;UnitType.FITNESS->Focus.PACE;UnitType.DUELS->Focus.TACKLING;UnitType.FINISHING,UnitType.SET_PIECES->Focus.FINISHING;UnitType.GOALKEEPING->if(p.position==Position.TW)Focus.KEEPING else null;else->null}}+(if(c.id==w.user.clubId)plan.extra.filter{it.playerId==p.id}.map{it.focus}else emptyList())+careerFocus
    val age=w.calendar.season-p.birthYear;val ageEffect=when{age<23->1.2;age>33->.25;else->.7};val careerMultiplier=if(p.id==w.user.playerId&&w.user.playerCareerFocus!=PlayerCareerFocus.BALANCED)1.08 else 1.0;p.trainingProgress+=foci.size*.026*(1+c.stadium.training*.009+c.stadium.gym*.004)*(.5+p.hidden.professionalism*.01)*ageEffect*staff*careerMultiplier
    if(p.id==w.user.playerId&&w.user.playerCareerFocus==PlayerCareerFocus.CLUB_ICON&&w.calendar.absoluteWeek%4==0){p.morale=(p.morale+1).coerceAtMost(100);p.hidden.loyalty=(p.hidden.loyalty+1).coerceAtMost(100)}
    if(p.trainingProgress>=1&&foci.isNotEmpty()){p.trainingProgress-=1;val focus=rng.pick(foci);if(p.attributes.improve(focus,p.hidden.potential)&&c.id==w.user.clubId)report.gains.add("${p.name}: ${focus.label} +1")}
    IntensiveTrainingSystem.applyWeek(w,c,p,report,rng)
    val risk=sessions.count{it==UnitType.FITNESS||it==UnitType.DUELS||it==UnitType.GAME}*.0045*intensity/3.0*(1+p.hidden.injuryProneness*.01)*(1-c.stadium.medicine*.006)*(if(p.fitness<50)2.0 else 1.0)*(if(p.youthProfile.growthSpurtWeeks>0)1.6 else 1.0)
    if(rng.chance(risk)){p.injuryWeeks=rng.int(1,3)+1;p.injury="Trainingszerrung";if(c.id==w.user.clubId)report.injuries.add("${p.name}: ${p.injuryWeeks-1} Wochen")}
   }
   ClubSystems.clamp(c);if(c.id==w.user.clubId){if(setPieceUnits>0)report.effects.add("Standards-Automatisierung ${c.dynamics.patterns["STANDARDS"]?:30}/100");report.averageFitness=w.squad().map{it.fitness}.average().roundToInt();report.effects.add("Chemie ${c.dynamics.chemistry}/100 · Taktik ${c.dynamics.tacticalUnderstanding}/100 · Pressing ${c.dynamics.pressingCoordination}/100");report.effects.add("Belastung ${c.dynamics.fatigueLoad}/100 · Gegnervorbereitung ${c.dynamics.opponentPrep}/100");w.training.lastReport=report}
  };YouthEngine.weekly(w,rng);YouthCompetitionSystem.weekly(w,rng);ScoutingTransferSystem.weekly(w,rng);TransferEngine.aiMarket(w,rng)
 }
}

object TransferEngine {
 private fun reservedBudget(w:World,buyerClubId:Int,excludeOfferId:Int=0):Long = w.negotiations.values
  .filter{it.id!=excludeOfferId&&it.buyerClubId==buyerClubId&&it.stage==TransferStage.REGISTRATION&&it.status==NegotiationStatus.AGREED&&it.registrationReady}
  .sumOf{it.fee+it.signingBonus}
 private fun targetsYouthSquad(w:World,o:TransferOffer):Boolean{
  val p=w.players[o.playerId]?:return false;val age=w.calendar.season-p.birthYear;val target=o.targetYouthSquad?:return false
  return o.buyerClubId==w.user.clubId&&age<=22&&(target!=YouthSquad.U19||age<=19)
 }
 private fun reservedSquadSlots(w:World,buyerClubId:Int,excludeOfferId:Int=0):Int = w.negotiations.values.count{
  it.id!=excludeOfferId&&it.buyerClubId==buyerClubId&&it.stage==TransferStage.REGISTRATION&&it.status==NegotiationStatus.AGREED&&it.registrationReady&&!targetsYouthSquad(w,it)
 }
 private fun availableBudget(w:World,buyer:Club,excludeOfferId:Int=0)=buyer.budget-reservedBudget(w,buyer.id,excludeOfferId)
 fun marketValue(w:World,p:Player):Long{val age=w.calendar.season-p.birthYear;val ageFactor=when{age<=20->1.45;age<=24->1.25;age<=29->1.0;age<=32->.78;else->.45};val base=p.ca*p.ca*850L;val potential=1.0+(p.hidden.potential-p.ca).coerceAtLeast(0)/90.0;return (base*ageFactor*potential).toLong().coerceAtLeast(500L)}
 private fun roleScore(role:SquadRole)=when(role){SquadRole.STAR->95;SquadRole.STARTER->82;SquadRole.ROTATION->65;SquadRole.PROSPECT->58;SquadRole.BACKUP->42}
 fun askingPrice(w:World,seller:Club?,p:Player,type:DealType):Long{if(seller==null)return 0;val value=marketValue(w,p);val importance=if(p.id in seller.tactics.xi)1.35 else if(p.id in seller.tactics.bench)1.10 else .92;val financial=if(seller.budget<0).78 else if(seller.budget<value/3).92 else 1.05;val homegrown=if(p.homegrownClubId==seller.id)1.12 else 1.0;val willingness=if(p.wantsMove).78 else 1.0;val natural=when(type){DealType.LOAN,DealType.LOAN_OPTION->(value*.06*importance).toLong();else->(value*importance*financial*homegrown*willingness).toLong()};return if(type in listOf(DealType.BUY,DealType.SWAP)&&p.releaseClause>0)minOf(natural,p.releaseClause) else natural}
 fun createOffer(w:World,buyerClubId:Int,playerId:Int,type:DealType=DealType.BUY,role:SquadRole=SquadRole.ROTATION):TransferOffer{val p=w.players.getValue(playerId);require(!p.retired&&p.clubId!=buyerClubId);require(p.clubId==0||ScoutingTransferSystem.transferWindowOpen(w)){"Das Transferfenster ist geschlossen."};val buyer=w.clubs.getValue(buyerClubId);val ownerId=if(p.loanParentClubId!=0)p.loanParentClubId else p.clubId;val seller=w.clubs[ownerId];val ask=askingPrice(w,seller,p,type);val wage=maxOf(5,(p.wage*1.18).roundToInt(),(marketValue(w,p)/42000L).toInt());val swap=if(type==DealType.SWAP&&seller!=null)w.squad(buyer.id).filter{!it.youth&&it.id!=w.user.playerId&&it.ca in (p.ca-10)..(p.ca+8)}.minByOrNull{abs(it.ca-p.ca)}?.id?:0 else 0;val swapValue=w.players[swap]?.let{marketValue(w,it)}?:0;val offer=TransferOffer(id=w.nextIds.negotiation++,buyerClubId=buyerClubId,sellerClubId=seller?.id?:0,playerId=p.id,type=type,role=role,fee=(ask*.76-swapValue*.72).toLong().coerceAtLeast(0),wage=wage,signingBonus=(wage*8L),sellOnPercent=5,durationYears=3,releaseClause=(marketValue(w,p)*2.2).toLong(),buyOption=if(type==DealType.LOAN_OPTION)marketValue(w,p) else 0,buyBackClause=if(seller!=null&&w.calendar.season-p.birthYear<=23)(marketValue(w,p)*1.55).toLong() else 0,swapPlayerId=swap,playingTimePromise=roleScore(role),stage=if(seller==null)TransferStage.PLAYER_AGENT else TransferStage.CLUB);if(buyerClubId==w.user.clubId&&p.youth&&w.calendar.season-p.birthYear<=22)offer.targetYouthSquad=if(w.calendar.season-p.birthYear<=19)p.youthSquad else YouthSquad.U23;w.negotiations[offer.id]=offer;evaluate(w,offer);return offer}
 fun improve(w:World,offerId:Int,kind:String){val o=w.negotiations.getValue(offerId);require(o.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN));when(kind){"fee"->o.fee=(o.fee*1.12+500).toLong();"wage"->o.wage=(o.wage*1.10+1).roundToInt();"bonus"->o.signingBonus=(o.signingBonus*1.25+250).toLong();"role"->{o.playingTimePromise=(o.playingTimePromise+10).coerceAtMost(100);o.role=when{o.playingTimePromise>=90->SquadRole.STAR;o.playingTimePromise>=78->SquadRole.STARTER;o.playingTimePromise>=62->SquadRole.ROTATION;else->SquadRole.PROSPECT}};"sellon"->o.sellOnPercent=(o.sellOnPercent+5).coerceAtMost(30)};o.round++;evaluate(w,o)}
 fun evaluate(w:World,o:TransferOffer):TransferOffer{
  if(o.status==NegotiationStatus.WITHDRAWN||o.stage in listOf(TransferStage.MEDICAL,TransferStage.REGISTRATION,TransferStage.COMPLETED))return o
  val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId);val seller=w.clubs[o.sellerClubId]
  val baseAsk=askingPrice(w,seller,p,o.type);val rivals=w.negotiations.values.count{it.id!=o.id&&it.playerId==o.playerId&&it.buyerClubId!=o.buyerClubId&&it.status in listOf(NegotiationStatus.COUNTER,NegotiationStatus.AGREED)}+ScoutingTransferSystem.competition(w,p.id).count{it.clubId!=o.buyerClubId};val ask=(baseAsk*(1+rivals*.08)).toLong()
  val relation=if(seller!=null)ClubSystems.clubRelation(w,buyer.id,seller.id) else 60;val freeBudget=availableBudget(w,buyer,o.id);val budgetFit=if(freeBudget>=o.fee+o.signingBonus)12 else -35;val swapValue=w.players[o.swapPlayerId]?.let{marketValue(w,it)}?:0;val effectiveFee=o.fee+swapValue;val loanStationPenalty=if(p.loanParentClubId!=0&&p.clubId!=o.sellerClubId&&p.loanWeeks>8)6 else 0
  o.sellerScore=if(seller==null||p.releaseClause>0&&effectiveFee>=p.releaseClause)100 else (45+(effectiveFee-ask).toDouble()/maxOf(1L,ask)*55+relation/5+(if(p.wantsMove)10 else 0)+(if(o.sellOnPercent>=15)5 else 0)+(if(o.buyBackClause>0&&w.calendar.season-p.birthYear<=23)5 else 0)-loanStationPenalty).roundToInt().coerceIn(0,100)
  val repGap=buyer.reputation-(seller?.reputation?:buyer.reputation);val interest=TransferInterestSystem.score(w,p,buyer.id)
  o.playerScore=(24+o.playingTimePromise/3+(o.wage-p.wage).coerceAtLeast(0)/maxOf(1,p.wage+1)*12+repGap/2+p.hidden.ambition/10+interest/4+(if(o.type in listOf(DealType.LOAN,DealType.LOAN_OPTION)&&w.calendar.season-p.birthYear<=22)8 else 0)).coerceIn(0,100)
  val agentRel=w.agentRelations[p.agentId]?:50;o.agentScore=(35+o.wage/10+(o.signingBonus/maxOf(1L,o.wage*5L)).toInt()*5+agentRel/4+budgetFit).coerceIn(0,100)
  if(freeBudget<o.fee+o.signingBonus){o.status=NegotiationStatus.REJECTED;o.message="Finanzierung gescheitert: frei verfügbares Budget reicht nach bereits reservierten Transfers nicht für Ablöse und Handgeld.";return o}
  if(o.round>=5&&minOf(o.sellerScore,o.playerScore,o.agentScore)<45){o.status=NegotiationStatus.REJECTED;o.message="Die Gespräche sind aktuell festgefahren.";return o}
  val sellerReady=seller==null||o.sellerScore>=62
  val playerReady=o.playerScore>=62&&o.agentScore>=58
  when{
   !sellerReady->{o.stage=TransferStage.CLUB;o.status=NegotiationStatus.COUNTER;o.message="Vereinsverhandlung: ${o.sellerScore}/100 · Angebot oder Klauseln nachbessern."}
   !playerReady->{o.stage=TransferStage.PLAYER_AGENT;o.status=NegotiationStatus.COUNTER;o.message="Verein einig. Spieler ${o.playerScore}/100 · Berater ${o.agentScore}/100 verhandeln noch."}
   else->{o.stage=TransferStage.MEDICAL;o.status=NegotiationStatus.AGREED;o.message="Grundsatzeinigung erreicht. Vor der Registrierung folgt der Medizincheck."}
  }
  return o
 }

 fun advanceProcess(w:World,offerId:Int):TransferOffer{
  val o=w.negotiations.getValue(offerId);if(o.stage !in listOf(TransferStage.MEDICAL,TransferStage.REGISTRATION))return evaluate(w,o)
  val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId)
  if(o.stage==TransferStage.MEDICAL){
   val age=w.calendar.season-p.birthYear
   o.medicalRisk=(p.hidden.injuryProneness*.55+p.injuryWeeks*8+(age-30).coerceAtLeast(0)*2.2).roundToInt().coerceIn(0,100)
   val roll=kotlin.math.abs(((w.seed xor (o.id.toLong()*7919L))%100).toInt())
   val failed=o.medicalRisk>=78&&roll<(o.medicalRisk-67).coerceAtLeast(5)
   if(failed){o.medicalPassed=false;o.medicalNote="Medizincheck auffällig: Risiko ${o.medicalRisk}/100. Der Transfer wird nicht registriert.";o.status=NegotiationStatus.REJECTED;o.message=o.medicalNote;return o}
   o.medicalPassed=true;o.medicalNote="Medizincheck bestanden · Risiko ${o.medicalRisk}/100";o.stage=TransferStage.REGISTRATION
  }
  val seniorSquadSize=w.squad(buyer.id).count{!it.retired&&!it.youth};val reservedSlots=reservedSquadSlots(w,buyer.id,o.id);val freeBudget=availableBudget(w,buyer,o.id);val needsSeniorSlot=!targetsYouthSquad(w,o)
  val budgetOk=freeBudget>=o.fee+o.signingBonus;val squadOk=!needsSeniorSlot||seniorSquadSize+reservedSlots<32
  o.registrationReady=budgetOk&&squadOk;o.status=NegotiationStatus.AGREED
  o.message=when{
   o.registrationReady->"${o.medicalNote}. Registrierung freigegeben – Budget und Kaderplatz sind reserviert."
   !budgetOk->"${o.medicalNote}. Registrierung wartet: frei verfügbares Budget reicht aktuell nicht. Der ausgehandelte Deal bleibt bestehen und kann erneut geprüft werden."
   else->"${o.medicalNote}. Registrierung wartet: Der Profikader hat bereits 32 Spieler. U19 und U23 zählen nicht zu dieser Grenze. Der Deal bleibt bestehen und kann nach einem freien Profiplatz erneut geprüft werden."
  }
  return o
 }

 fun repairLegacyRegistrationStates(w:World){
  w.negotiations.values.filter{it.stage==TransferStage.REGISTRATION&&it.status==NegotiationStatus.REJECTED&&it.medicalPassed&&it.message.startsWith("Registrierung blockiert")}.forEach{
   it.status=NegotiationStatus.AGREED;it.registrationReady=false;it.message="${it.medicalNote}. Registrierung erneut geöffnet. Der ausgehandelte Deal bleibt bestehen und kann erneut geprüft werden."
  }
 }

 fun withdraw(w:World,offerId:Int){
  val o=w.negotiations.getValue(offerId);require(o.buyerClubId==w.user.clubId){"Nur eigene Kaufverhandlungen können hier zurückgezogen werden."};require(o.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.WITHDRAWN)){"Diese Verhandlung ist bereits beendet."}
  val p=w.players[o.playerId];val released=if(o.stage==TransferStage.REGISTRATION&&o.status==NegotiationStatus.AGREED&&o.registrationReady)o.fee+o.signingBonus else 0L
  o.registrationReady=false;o.status=NegotiationStatus.WITHDRAWN;o.message=if(released>0)"Verhandlung zurückgezogen. ${released} € reserviertes Budget und der Kaderplatz sind sofort wieder freigegeben." else "Verhandlung von dir zurückgezogen."
  if(p!=null)w.news("Verhandlung beendet: ${p.name}",o.message,"normal")
 }

 fun complete(w:World,offerId:Int){val o=w.negotiations.getValue(offerId);evaluate(w,o);if(o.stage==TransferStage.MEDICAL)advanceProcess(w,offerId);if(o.stage==TransferStage.REGISTRATION)advanceProcess(w,offerId);require(o.status==NegotiationStatus.AGREED&&o.stage==TransferStage.REGISTRATION&&o.registrationReady){o.message};val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId);val seller=w.clubs[o.sellerClubId];require(availableBudget(w,buyer,o.id)>=o.fee+o.signingBonus){"Reserviertes Transferbudget reicht nicht mehr aus."};buyer.budget-=o.fee+o.signingBonus;seller?.let{it.budget+=o.fee};val previous=p.clubId;val previousYouth=p.youth;val previousYouthSquad=p.youthSquad
  when(o.type){DealType.LOAN,DealType.LOAN_OPTION->{p.loanParentClubId=previous;p.loanBuyerClubId=buyer.id;p.loanWeeks=o.loanWeeksRequested.coerceIn(8,52);p.loanOptionFee=o.buyOption;p.loanRecallAllowed=o.recallAllowed;p.loanReturnYouth=previousYouth;p.loanReturnYouthSquad=previousYouthSquad;p.clubId=buyer.id};else->{p.clubId=buyer.id;p.loanParentClubId=0;p.loanBuyerClubId=0;p.loanWeeks=0;p.loanOptionFee=0;p.loanRecallAllowed=true;p.loanReturnYouth=false}}
  if(o.type==DealType.SWAP&&o.swapPlayerId!=0&&seller!=null){val outgoing=w.players.getValue(o.swapPlayerId);require(outgoing.clubId==buyer.id);outgoing.clubId=seller.id;outgoing.wantsMove=false;outgoing.morale=(outgoing.morale-3).coerceAtLeast(5);seller.dynamics.chemistry=(seller.dynamics.chemistry+1).coerceAtMost(100);buyer.dynamics.chemistry=(buyer.dynamics.chemistry-2).coerceAtLeast(0)}
  p.wage=o.wage;p.contractYears=o.durationYears;p.promisedRole=o.role;p.precontractClubId=0;p.precontractSeason=0;p.precontractWage=0;p.precontractYears=0;p.releaseClause=o.releaseClause;p.buyBackClubId=o.sellerClubId;p.buyBackFee=o.buyBackClause;p.sellOnPercentToPrevious=o.sellOnPercent;p.wantsMove=false
  val age=w.calendar.season-p.birthYear;val targetYouth=o.targetYouthSquad?.takeIf{buyer.id==w.user.clubId&&age<=22&&(it!=YouthSquad.U19||age<=19)}
  p.youth=when{o.type in listOf(DealType.LOAN,DealType.LOAN_OPTION)&&targetYouth!=null->true;o.type in listOf(DealType.LOAN,DealType.LOAN_OPTION)->false;targetYouth!=null->true;previousYouth&&buyer.id!=w.user.clubId&&age<=22->true;else->false}
  if(p.youth)p.youthSquad=targetYouth?:if(age<=19)previousYouthSquad else YouthSquad.U23
  p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;p.youthProfile.seniorTraining=false
  if(previous==w.user.clubId&&buyer.id!=w.user.clubId)w.training.extra.removeAll{it.playerId==p.id}
  p.morale=(p.morale+6).coerceAtMost(100);if(p.personalityType(w.calendar.season) in listOf(PersonalityType.LEADER,PersonalityType.BIG_GAME)&&previous!=0)w.clubs[previous]?.dynamics?.let{it.chemistry=(it.chemistry-3).coerceAtLeast(0);it.leadership=(it.leadership-3).coerceAtLeast(0)};o.stage=TransferStage.COMPLETED;o.status=NegotiationStatus.COMPLETED;o.message="Transfer abgeschlossen.";w.transferHistory.add(0,TransferHistoryEntry(w.calendar.season,w.calendar.absoluteWeek,p.id,previous,buyer.id,o.type,o.fee,if(o.type in listOf(DealType.LOAN,DealType.LOAN_OPTION))"Leihe ${o.loanWeeksRequested} Wochen" else if(p.youth)"Transfer in ${p.youthSquad.label}" else "Transfer"));while(w.transferHistory.size>250)w.transferHistory.removeAt(w.transferHistory.lastIndex);if(previous!=0){if(previous==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,previous)else WorldFactory.autoLineup(w,previous)};if(buyer.id==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,buyer.id)else WorldFactory.autoLineup(w,buyer.id);seller?.let{ClubSystems.changeClubRelation(w,buyer.id,it.id,if(o.sellerScore>=78)2 else -1)};w.agentRelations[p.agentId]=((w.agentRelations[p.agentId]?:50)+2).coerceAtMost(100);if(buyer.id==w.user.clubId)w.news("Transfer abgeschlossen: ${p.name}","${o.type.label} · ${if(p.youth)"${p.youthSquad.label} · " else ""}Rolle ${o.role.label} · ${o.fee} € · ${o.wage} €/Woche","good")
 }
 private fun exerciseOptionForClub(w:World,p:Player,buyerClubId:Int,announce:Boolean):Boolean{val buyer=w.clubs[buyerClubId]?:return false;val parentId=p.loanParentClubId;val option=p.loanOptionFee;if(p.clubId!=buyerClubId||parentId==0||option<=0||buyer.budget<option)return false;val parent=w.clubs[parentId];buyer.budget-=option;parent?.let{it.budget+=option};p.loanParentClubId=0;p.loanBuyerClubId=0;p.loanWeeks=0;p.loanOptionFee=0;p.loanRecallAllowed=true;p.loanReturnYouth=false;p.contractYears=maxOf(3,p.contractYears);p.morale=(p.morale+5).coerceAtMost(100);if(buyerClubId==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,buyerClubId)else WorldFactory.autoLineup(w,buyerClubId);parent?.let{if(it.id==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,it.id)else WorldFactory.autoLineup(w,it.id);ClubSystems.changeClubRelation(w,buyerClubId,it.id,1)};if(announce&&buyerClubId==w.user.clubId)w.news("Kaufoption gezogen","${p.name} bleibt dauerhaft im Verein.","good");return true}
 fun exerciseOption(w:World,playerId:Int){val p=w.players.getValue(playerId);require(p.clubId==w.user.clubId&&p.loanParentClubId!=0&&p.loanOptionFee>0){"Keine Kaufoption verfügbar."};require(w.club().budget>=p.loanOptionFee){"Budget reicht nicht."};check(exerciseOptionForClub(w,p,w.user.clubId,true))}
 private fun aiShouldExerciseOption(w:World,p:Player,buyer:Club):Boolean{if(p.loanOptionFee<=0||buyer.budget<p.loanOptionFee)return false;val value=marketValue(w,p);val squad=w.squad(buyer.id).filter{!it.youth&&!it.retired};val avg=squad.map{it.ca}.average().takeIf{!it.isNaN()}?:p.ca.toDouble();val samePosition=squad.filter{it.id!=p.id&&it.position==p.position};val positionalNeed=samePosition.isEmpty()||samePosition.maxOf{it.ca}<p.ca+2;val upside=p.hidden.potential-p.ca;val sporting=p.ca>=avg-2||upside>=8||positionalNeed;val financial=p.loanOptionFee<=value*13/10&&buyer.budget-p.loanOptionFee>=0;return sporting&&financial}
 fun processLoans(w:World){for(p in w.players.values.filter{it.loanWeeks>0}.toList()){val buyer=w.clubs[p.clubId];if(p.loanWeeks<=1&&p.clubId!=w.user.clubId&&p.loanOptionFee>0&&buyer!=null&&aiShouldExerciseOption(w,p,buyer)&&exerciseOptionForClub(w,p,buyer.id,false))continue;p.loanWeeks--;if(p.loanWeeks<=0){val parent=p.loanParentClubId;if(parent in w.clubs){p.clubId=parent;p.youth=p.loanReturnYouth;if(p.youth)p.youthSquad=p.loanReturnYouthSquad;p.loanParentClubId=0;p.loanBuyerClubId=0;p.loanOptionFee=0;p.loanRecallAllowed=true;p.loanReturnYouth=false;p.temporarySeniorCallUp=false;p.temporaryReturnSquad=null;if(parent==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,parent)else WorldFactory.autoLineup(w,parent);buyer?.let{if(it.id==w.user.clubId)WorldFactory.reconcileMatchdaySelection(w,it.id)else WorldFactory.autoLineup(w,it.id)}}}}}
 fun aiMarket(w:World,rng:SeededRandom){
  processLoans(w);if(w.calendar.absoluteWeek%2!=0)return
  val playersByClub=w.players.values.asSequence().filter{!it.retired}.groupBy{it.clubId}
  val buyers=w.clubs.values.filter{it.id!=w.user.clubId&&it.budget>3000}.shuffledBy(rng).take(3)
  for(b in buyers){
   val squad=(playersByClub[b.id]?:emptyList()).filter{!it.youth};if(squad.size>=28)continue
   val avgCa=squad.map{it.ca}.average().takeIf{!it.isNaN()}?:50.0;val interestCtx=TransferInterestSystem.context(w,b.id)
   val candidates=w.players.values.asSequence().filter{!it.retired&&!it.youth&&it.clubId!=b.id&&it.clubId!=w.user.clubId&&it.ca<=avgCa+12}.filter{p->p.clubId==0||p.wantsMove||w.clubs[p.clubId]?.budget?.let{it<0}==true||TransferInterestSystem.score(w,p,interestCtx)>=68}.toList()
   if(candidates.isEmpty())continue;val p=rng.pick(candidates);if(p.clubId!=0&&!ScoutingTransferSystem.transferWindowOpen(w))continue;val type=if(w.calendar.season-p.birthYear<=22&&rng.chance(.38))DealType.LOAN_OPTION else DealType.BUY;val o=createOffer(w,b.id,p.id,type,if(p.ca>65)SquadRole.STARTER else SquadRole.ROTATION);repeat(3){if(o.status==NegotiationStatus.COUNTER)improve(w,o.id,if(o.sellerScore<62)"fee" else if(o.playerScore<62)"role" else "wage")};if(o.status==NegotiationStatus.AGREED){advanceProcess(w,o.id);if(o.status==NegotiationStatus.AGREED)complete(w,o.id)}
  }
 }
 private fun <T> List<T>.shuffledBy(rng:SeededRandom):List<T>{val a=toMutableList();for(i in a.lastIndex downTo 1){val j=rng.int(0,i);val t=a[i];a[i]=a[j];a[j]=t};return a}
}


object OutboundTransferSystem {
 private fun outbound(w:World,o:TransferOffer)=o.sellerClubId==w.user.clubId&&o.buyerClubId!=w.user.clubId
 fun activeOffers(w:World,playerId:Int=0):List<TransferOffer> = w.negotiations.values.filter{outbound(w,it)&&(playerId==0||it.playerId==playerId)&&it.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN,NegotiationStatus.COMPLETED)}.sortedWith(compareBy<TransferOffer>{it.playerId}.thenByDescending{it.fee}.thenBy{it.id})

 private fun positionalNeed(w:World,buyer:Club,p:Player):Int{
  val squad=w.squad(buyer.id).filter{!it.youth&&!it.retired};val same=squad.filter{it.position==p.position};val best=same.maxOfOrNull{it.ca}?:0
  return when{same.isEmpty()->4;best+5<=p.ca->4;best<p.ca->3;same.size<=2->2;else->1}
 }
 private fun roleFor(w:World,buyer:Club,p:Player):SquadRole{
  val squad=w.squad(buyer.id).filter{!it.youth&&!it.retired};val avg=squad.map{it.ca}.average().takeIf{!it.isNaN()}?:p.ca.toDouble()
  return when{p.ca>=avg+8->SquadRole.STAR;p.ca>=avg+3->SquadRole.STARTER;p.ca>=avg-4->SquadRole.ROTATION;else->SquadRole.PROSPECT}
 }
 private fun saleCeiling(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionalNeed(w,buyer,p);val age=w.calendar.season-p.birthYear
  val upside=(p.hidden.potential-p.ca).coerceAtLeast(0);val sporting=0.78+need*.075+(if(age<=23)upside.coerceAtMost(20)*.008 else 0.0)+(buyer.reputation-w.club().reputation).coerceIn(-20,25)*.003
  return minOf((buyer.budget*.78).toLong().coerceAtLeast(0), (value*sporting.coerceIn(.72,1.30)).toLong()).coerceAtLeast(1_000L)
 }
 private fun loanFeeCeiling(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionalNeed(w,buyer,p);return minOf((buyer.budget*.18).toLong().coerceAtLeast(0),(value*(.045+need*.018)).toLong()).coerceAtLeast(500L)
 }
 private fun optionCeiling(w:World,buyer:Club,p:Player):Long{
  val value=TransferEngine.marketValue(w,p);val need=positionalNeed(w,buyer,p);return minOf((buyer.budget*.80).toLong().coerceAtLeast(0),(value*(.88+need*.055)).toLong()).coerceAtLeast(1_000L)
 }
 private fun deterministicPct(w:World,p:Player,buyer:Club,index:Int):Double{
  val raw=kotlin.math.abs((p.id*31+buyer.id*17+w.calendar.absoluteWeek*13+index*7)%15)
  return .78+raw/100.0
 }
 fun requestOffers(w:World,playerId:Int,type:DealType):List<TransferOffer>{
  require(w.live==null){"Spieler erst außerhalb eines laufenden Spiels anbieten."};require(type in listOf(DealType.BUY,DealType.LOAN,DealType.LOAN_OPTION)){"Nur Verkauf oder Leihe können angeboten werden."};require(ScoutingTransferSystem.transferWindowOpen(w)){"Das Transferfenster ist geschlossen."}
  val p=w.players.getValue(playerId);require(!p.retired&&p.clubId==w.user.clubId&&p.loanParentClubId==0&&p.id!=w.user.playerId){"Dieser Spieler kann aktuell nicht angeboten werden."}
  w.negotiations.values.filter{outbound(w,it)&&it.playerId==p.id&&it.status !in listOf(NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN,NegotiationStatus.COMPLETED)}.forEach{it.status=NegotiationStatus.REJECTED;it.message="Durch eine neue Angebotsrunde ersetzt."}
  val value=TransferEngine.marketValue(w,p)
  val candidates=w.clubs.values.asSequence().filter{it.id!=w.user.clubId&&it.budget>2_000L}.map{b->
   val need=positionalNeed(w,b,p);val interest=TransferInterestSystem.score(w,p,b.id);val repFit=100-kotlin.math.abs(b.reputation-w.club().reputation).coerceAtMost(100)
   Triple(b,need*24+interest+repFit/5,interest)
  }.sortedByDescending{it.second}.take(4).toList()
  require(candidates.size>=2){"Aktuell gibt es nicht genügend ernsthafte Interessenten."}
  val created=mutableListOf<TransferOffer>()
  candidates.forEachIndexed{index,(buyer,_,interest)->
   val role=roleFor(w,buyer,p);val ceiling=if(type==DealType.BUY)saleCeiling(w,buyer,p) else loanFeeCeiling(w,buyer,p);val fee=(ceiling*deterministicPct(w,p,buyer,index)).toLong().coerceAtLeast(500L)
   val wage=maxOf(p.wage,(value/50000L).toInt(),5);val option=if(type==DealType.LOAN_OPTION)(optionCeiling(w,buyer,p)*(.82+index*.035)).toLong() else 0L
   val offer=TransferOffer(id=w.nextIds.negotiation++,buyerClubId=buyer.id,sellerClubId=w.user.clubId,playerId=p.id,type=type,role=role,fee=fee,wage=wage,signingBonus=(wage*4L),sellOnPercent=if(type==DealType.BUY&&index%2==0)5 else 0,durationYears=3,releaseClause=0,buyOption=option,playingTimePromise=when(role){SquadRole.STAR->95;SquadRole.STARTER->82;SquadRole.ROTATION->65;SquadRole.PROSPECT->58;SquadRole.BACKUP->42},status=NegotiationStatus.COUNTER,stage=TransferStage.CLUB,loanWeeksRequested=if(type==DealType.BUY)24 else listOf(24,40,52)[index%3],recallAllowed=type!=DealType.BUY&&index%2==0)
   offer.message="${buyer.name} bietet ${if(type==DealType.BUY)"einen Kauf" else type.label.lowercase()} an · Interesse $interest/100. Du kannst annehmen, ablehnen oder nachverhandeln."
   w.negotiations[offer.id]=offer;created.add(offer)
  }
  w.news("Angebote für ${p.name}","${created.size} Vereine haben auf die ${if(type==DealType.BUY)"Verkaufsliste" else "Leihanfrage"} reagiert.","normal")
  return created
 }
 fun counter(w:World,offerId:Int,kind:String):TransferOffer{
  val o=w.negotiations.getValue(offerId);require(outbound(w,o)&&o.status==NegotiationStatus.COUNTER){"Dieses Angebot kann nicht nachverhandelt werden."};val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId)
  when(kind){
   "fee"->o.fee=(o.fee*1.08+maxOf(500L,TransferEngine.marketValue(w,p)/80L)).toLong()
   "sellon"->o.sellOnPercent=(o.sellOnPercent+5).coerceAtMost(25)
   "option"->if(o.type==DealType.LOAN_OPTION)o.buyOption=(o.buyOption*1.08+maxOf(1_000L,TransferEngine.marketValue(w,p)/60L)).toLong()
   else->error("Unbekannter Verhandlungspunkt.")
  }
  o.round++
  val maxFee=if(o.type==DealType.BUY)saleCeiling(w,buyer,p) else loanFeeCeiling(w,buyer,p);val maxOption=if(o.type==DealType.LOAN_OPTION)optionCeiling(w,buyer,p) else 0L
  val sellOnCost=if(o.type==DealType.BUY)TransferEngine.marketValue(w,p)*o.sellOnPercent/100L/4L else 0L
  val demand=o.fee+sellOnCost;val allowed=maxFee+if(o.type==DealType.BUY)TransferEngine.marketValue(w,p)/16L else 0L
  val optionOk=o.type!=DealType.LOAN_OPTION||o.buyOption<=maxOption;val optionNear=o.type!=DealType.LOAN_OPTION||o.buyOption<=maxOption*108/100
  when{
   buyer.budget<o.fee+o.signingBonus->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} zieht das Angebot zurück: Das Paket passt nicht mehr ins Budget."}
   o.round>=4&&(demand>allowed||!optionOk)->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} bricht die Verhandlung nach mehreren Runden ab."}
   demand<=allowed&&optionOk->{o.status=NegotiationStatus.AGREED;o.message="${buyer.name} akzeptiert dein Gegenangebot. Du kannst den Deal jetzt bestätigen."}
   demand<=allowed*108/100&&optionNear->{o.fee=minOf(o.fee,maxFee);if(o.type==DealType.LOAN_OPTION)o.buyOption=minOf(o.buyOption,maxOption);o.message="${buyer.name} bleibt am Tisch und legt ein letztes Gegenangebot vor.";o.status=NegotiationStatus.COUNTER}
   else->{o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} lehnt deine Forderung ab und steigt aus."}
  }
  return o
 }
 fun reject(w:World,offerId:Int){val o=w.negotiations.getValue(offerId);require(outbound(w,o)&&o.status!=NegotiationStatus.COMPLETED);o.status=NegotiationStatus.REJECTED;o.message="Angebot von dir abgelehnt."}
 fun accept(w:World,offerId:Int){
  require(w.live==null){"Transfer erst außerhalb eines laufenden Spiels bestätigen."};val o=w.negotiations.getValue(offerId);require(outbound(w,o)&&o.status in listOf(NegotiationStatus.COUNTER,NegotiationStatus.AGREED)){"Dieses Angebot kann nicht angenommen werden."};val p=w.players.getValue(o.playerId);val buyer=w.clubs.getValue(o.buyerClubId)
  val interest=TransferInterestSystem.score(w,p,buyer.id);if(interest<28&&!p.wantsMove){o.status=NegotiationStatus.REJECTED;o.message="${p.name} lehnt den Wechsel zu ${buyer.name} ab.";return}
  if(buyer.budget<o.fee+o.signingBonus){o.status=NegotiationStatus.REJECTED;o.message="${buyer.name} kann das Angebot finanziell nicht mehr hinterlegen.";return}
  o.status=NegotiationStatus.AGREED;o.stage=TransferStage.REGISTRATION;o.medicalPassed=true;o.medicalNote="Medizincheck vom aufnehmenden Verein bestanden";o.registrationReady=true
  TransferEngine.complete(w,o.id)
  w.negotiations.values.filter{it.id!=o.id&&it.playerId==p.id&&outbound(w,it)&&it.status !in listOf(NegotiationStatus.COMPLETED,NegotiationStatus.REJECTED,NegotiationStatus.WITHDRAWN)}.forEach{it.status=NegotiationStatus.REJECTED;it.message="Spieler hat sich für ein anderes Angebot entschieden."}
  w.news(if(o.type==DealType.BUY)"Spieler verkauft" else "Leihe vereinbart","${p.name} → ${buyer.name} · ${if(o.fee>0) "${o.fee} €" else "ohne Gebühr"}${if(o.type==DealType.LOAN_OPTION&&o.buyOption>0)" · Kaufoption ${o.buyOption} €" else ""}","good")
 }
}

object MatchIntelligence {
 private fun ai(m:LiveMatch,home:Boolean)=if(home)m.homeAi else m.awayAi
 fun preMatch(w:World,c:Club,opp:Club):MatchAiProfile{val o=w.squad(opp.id).filter{!it.youth};val iv=o.filter{it.position==Position.IV};val keeper=o.filter{it.position==Position.TW}.maxByOrNull{it.ca};val flank=o.filter{it.position in listOf(Position.LV,Position.RV,Position.LA,Position.RA)};val weak=when{iv.isNotEmpty()&&iv.map{it.attributes.pace}.average()<48->"Langsame Innenverteidiger";iv.isNotEmpty()&&iv.map{it.attributes.heading}.average()<50->"Schwache Lufthoheit";keeper!=null&&keeper.attributes.passing<45->"Unsicherer Torwart im Aufbau";flank.isNotEmpty()&&flank.map{it.attributes.tackling}.average()<48->"Verwundbare Außenbahn";opp.dynamics.pressingCoordination<42->"Unsauberes Pressing";else->"Keine klare Schwäche"};val approach=when(weak){"Langsame Innenverteidiger"->BuildUp.COUNTER;"Schwache Lufthoheit"->BuildUp.WIDE;"Unsicherer Torwart im Aufbau"->BuildUp.DIRECT;"Verwundbare Außenbahn"->BuildUp.WIDE;"Unsauberes Pressing"->BuildUp.SHORT;else->c.tactics.buildUp};val target=when(weak){"Unsicherer Torwart im Aufbau"->keeper?.id?:0;"Langsame Innenverteidiger","Schwache Lufthoheit"->iv.minByOrNull{if(weak.contains("Langsame"))it.attributes.pace else it.attributes.heading}?.id?:0;else->0};val trap=when{opp.tactics.buildUp in listOf(BuildUp.SHORT,BuildUp.TIKI_TAKA)->"Aufbau auf schwachen Passspieler lenken";opp.tactics.width>=4->"Außen isolieren und Rückpass schließen";else->"Zentrum verdichten und Seitenpass provozieren"};return MatchAiProfile(weak,approach,trap,target,humanErrorRate=((24-c.dynamics.tacticalUnderstanding/6)-BmwDeveloperSystems.aiErrorReduction(w,c.id)).coerceIn(2,22),tacticalFoulBias=if(c.dynamics.mentalHardness>65)12 else 5,widthBias=if(approach==BuildUp.WIDE)1 else if(approach==BuildUp.SHORT)-1 else 0)}
 fun install(w:World,m:LiveMatch){m.homeAi=preMatch(w,w.clubs.getValue(m.homeId),w.clubs.getValue(m.awayId));m.awayAi=preMatch(w,w.clubs.getValue(m.awayId),w.clubs.getValue(m.homeId))}
 fun playerMentalFactor(w:World,m:LiveMatch,p:Player):Double{val big=m.knockout||w.isDerby(m.homeId,m.awayId)||m.minute>=80;val pressure=abs(m.home.goals-m.away.goals)<=1&&m.minute>=70;return when(p.personalityType(w.calendar.season)){PersonalityType.LEADER->if(big)1.045 else 1.02;PersonalityType.BIG_GAME->if(big)1.08 else 1.01;PersonalityType.CHOKER->if(big||pressure).91 else .99;PersonalityType.LAZY_STAR->if(p.morale<65).92 else .99;PersonalityType.WILD_YOUNGSTER->if(big)if(p.hidden.pressure>=55)1.05 else .94 else 1.02;PersonalityType.PROFESSIONAL->1.025;else->1.0}}
 fun teamFactor(c:Club,attack:Boolean):Double{val d=c.dynamics;val base=if(attack)(d.chemistry*.0012+d.tacticalUnderstanding*.0015+d.opponentPrep*.0008) else (d.chemistry*.0010+d.pressingCoordination*.0016+d.mentalHardness*.0007);return (.82+base).coerceIn(.88,1.14)}
 fun patternFactor(c:Club,key:String)=((c.dynamics.patterns[key]?:30)-50)*.0022+1.0
 fun adapt(w:World,m:LiveMatch,home:Boolean,rng:SeededRandom):String?{val c=w.clubs.getValue(if(home)m.homeId else m.awayId);if(c.id==w.user.clubId)return null;val p=ai(m,home);if(m.minute-p.lastAdjustmentMinute<12)return null;val own=if(home)m.home else m.away;val opp=if(home)m.away else m.home;val behind=own.goals<opp.goals;val ahead=own.goals>opp.goals;val cards=(if(home)m.homeXi else m.awayXi).sumOf{m.yellows[it]?:0}+m.sentOff.count{it in if(home)m.homeXi else m.awayXi};val fatigue=(if(home)m.homeXi else m.awayXi).mapNotNull{w.players[it]?.fitness}.average().takeIf{!it.isNaN()}?:75.0;val patternSeen=opp.possessionTicks>own.possessionTicks*1.25||opp.xg>own.xg+.7;val error=rng.chance(p.humanErrorRate/100.0)
  val mentality=when{behind&&m.minute>=80->5;behind->4;ahead&&m.minute>=72->2;cards>=4||fatigue<58->2;else->3};if(home)m.homeMentality=mentality else m.awayMentality=mentality
  if(error){c.tactics.width=if(c.tactics.width>=4)2 else 5;c.tactics.tempo=(c.tactics.tempo+1).coerceAtMost(5);p.lastAdjustmentMinute=m.minute;p.adaptation++;return "${c.shortName} reagiert unter Druck hektisch und übersteuert die Breite."}
  if(patternSeen){c.tactics.buildUp=p.approach;c.tactics.pressing=(c.tactics.pressing+1).coerceAtMost(if(fatigue<62)3 else 5);p.adaptation++}
  if(behind){c.tactics.tempo=5;c.tactics.line=4;c.tactics.pressing=if(fatigue<60)3 else 5;p.timeWaste=0}else if(ahead&&m.minute>=72){c.tactics.tempo=2;c.tactics.line=2;c.tactics.pressing=if(fatigue<62)2 else 3;p.timeWaste=((m.minute-70)/2).coerceIn(0,10)}
  p.lastAdjustmentMinute=m.minute;return "${c.shortName}: ${if(patternSeen)"Muster erkannt – ${p.weakness}. " else ""}${p.pressingTrap}."
 }
}

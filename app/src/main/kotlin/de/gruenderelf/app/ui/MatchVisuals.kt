package de.gruenderelf.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gruenderelf.app.R
import de.gruenderelf.engine.Formations
import de.gruenderelf.engine.LiveMatch
import de.gruenderelf.engine.MatchEngine
import de.gruenderelf.engine.LivePhase
import de.gruenderelf.engine.Position
import de.gruenderelf.engine.PlayerRole
import de.gruenderelf.engine.PlayerInstruction
import de.gruenderelf.engine.BuildUp
import de.gruenderelf.engine.ShotContext
import de.gruenderelf.engine.ShotModel
import de.gruenderelf.engine.World
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun LiveMatch.effectiveLiveClubId() = liveClubId.takeIf { it == homeId || it == awayId } ?: chainOwnerClubId.takeIf { it == homeId || it == awayId } ?: if (homeInPossession) homeId else awayId

private fun phaseText(m: LiveMatch): String = m.liveDetail.takeIf { it.isNotBlank() && m.livePhase in setOf(LivePhase.SHOT_ON_TARGET, LivePhase.SHOT_OFF_TARGET, LivePhase.WOODWORK, LivePhase.GOAL, LivePhase.PENALTY, LivePhase.SHOOTOUT, LivePhase.OFFSIDE, LivePhase.THROW_IN, LivePhase.VAR) } ?: m.livePhase.label

/**
 * Abbildung der logischen Spielfeldkoordinaten auf das direkt integrierte
 * Stadionfoto. Das Bild selbst ist die Live-Match-Grundlage; diese Projektion
 * legt nur den Ball passend innerhalb der sichtbaren Spielfeldlinien ab.
 */
private fun projectedX(x: Float, y: Float): Float {
    val left = .145f - .10f * y
    val right = .84f + .105f * y
    return left + (right - left) * x
}
private fun projectedY(x: Float, y: Float): Float = .18f + .71f * y

/** Engine x = seitlich, y = Heimtor (0) -> Auswärtstor (1). */
private fun engineProjectedX(lateralX: Float, longitudinalY: Float): Float =
    projectedX((1f - longitudinalY).coerceIn(.02f, .98f), lateralX.coerceIn(.02f, .98f))
private fun engineProjectedY(lateralX: Float, longitudinalY: Float): Float =
    projectedY((1f - longitudinalY).coerceIn(.02f, .98f), lateralX.coerceIn(.02f, .98f))

@Composable
private fun LivePitchImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.live_match_pitch),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
fun LiveMatchAnimation(
    w: World,
    m: LiveMatch,
    frameDurationMs: Long,
    dynamicOverview: Boolean = false,
) {
    val goalCameraPhase = m.livePhase in setOf(
        LivePhase.SHOT_OFF_TARGET,
        LivePhase.SHOT_ON_TARGET,
        LivePhase.WOODWORK,
        LivePhase.GOAL,
        LivePhase.SHOOTOUT,
    )
    // In der neuen Draufsicht springt die Darstellung bei einem Abschluss bewusst
    // wieder in die bekannte Tor-/Schusskamera. Alle übrigen Phasen bleiben in der
    // taktischen Gesamtfeldansicht.
    if (dynamicOverview && !goalCameraPhase) TopDownMatchOverview(w, m, frameDurationMs)
    else ClassicLiveMatchAnimation(w, m, frameDurationMs)
}

/**
 * Taktische 3D-Übersicht für das Live-Spiel.
 *
 * Die Match-Engine bleibt die einzige Quelle für Ball, Ballbesitz, Phase und
 * aktiven Spieler. Die übrigen Spieler erhalten nur eine visuelle Bewegung
 * um ihre echte Formation: Mannschaft verschiebt zum Ball, Gegner kompakt
 * dagegen, nahe Verteidiger pressen und Mitspieler starten Unterstützungs-
 * läufe. So bleiben 22 Spieler sichtbar, ohne eine zweite Matchsimulation
 * neben der Engine aufzubauen.
 */
@Composable
private fun TopDownMatchOverview(w: World, m: LiveMatch, frameDurationMs: Long) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val activeClubId = m.effectiveLiveClubId()
    val activeClub = if (activeClubId == home.id) home else away
    val actor = w.players[m.livePlayerId]
    val homeCoords = remember(m.homeFormation) { Formations.coordinates(m.homeFormation) }
    val awayCoords = remember(m.awayFormation) { Formations.coordinates(m.awayFormation) }
    val homeSlots = remember(m.homeFormation) { Formations.positions(m.homeFormation) }
    val awaySlots = remember(m.awayFormation) { Formations.positions(m.awayFormation) }

    val ballX = remember(m.fixtureId) { Animatable(m.ballX.coerceIn(.025f, .975f)) }
    val ballY = remember(m.fixtureId) { Animatable(m.ballY.coerceIn(.025f, .975f)) }
    val targetX = m.ballX.coerceIn(.025f, .975f)
    val targetY = m.ballY.coerceIn(.025f, .975f)
    val event = m.liveEventSerial

    LaunchedEffect(event, m.livePhase, frameDurationMs, targetX, targetY) {
        val animationMs = (frameDurationMs * .92).toInt().coerceIn(260, 3400)
        coroutineScope {
            launch { ballX.animateTo(targetX, tween(animationMs, easing = FastOutSlowInEasing)) }
            launch { ballY.animateTo(targetY, tween(animationMs, easing = FastOutSlowInEasing)) }
        }
    }

    // Nicht jeder "Angriff" soll sofort dieselbe Teamhöhe erzeugen. Dieser Wert
    // wird weich animiert und bestimmt, wie weit die ganze Mannschaft aufrückt.
    val phaseTarget = when (m.livePhase) {
        LivePhase.POSSESSION, LivePhase.THROW_IN -> 0f
        LivePhase.ATTACK -> .52f
        LivePhase.DANGEROUS_ATTACK -> 1f
        LivePhase.COUNTER -> .78f
        LivePhase.CORNER, LivePhase.DANGEROUS_FREE_KICK, LivePhase.PENALTY -> .92f
        LivePhase.FREE_KICK -> .55f
        else -> .18f
    }
    val phaseShape by animateFloatAsState(
        targetValue = phaseTarget,
        animationSpec = tween((frameDurationMs * .55).toInt().coerceIn(220, 1200), easing = FastOutSlowInEasing),
        label = "team-shape-phase",
    )
    val focusPulse by rememberInfiniteTransition(label = "ball-carrier-focus").animateFloat(
        initialValue = .97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(760, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "ball-carrier-pulse",
    )

    val actorName = actor?.name ?: activeClub.shortName
    val actorNumber = actor?.number?.toString() ?: ""
    val textMeasurer = rememberTextMeasurer()
    val nameLayout = remember(actorName) {
        textMeasurer.measure(
            AnnotatedString(actorName),
            style = TextStyle(color = Chalk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
    val numberLayout = remember(actorNumber) {
        textMeasurer.measure(
            AnnotatedString(actorNumber),
            style = TextStyle(color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black),
        )
    }

    Surface(
        color = Color(0xFF07130B),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamHeader(home.shortName, Color(home.primary))
                Text("${MatchEngine.clockLabel(m)}. Minute", color = Chalk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                TeamHeader(away.shortName, Color(away.primary))
            }

            Box(Modifier.fillMaxWidth().aspectRatio(1.72f)) {
                Canvas(Modifier.matchParentSize()) {
                    val ww = size.width
                    val hh = size.height
                    val fieldLeft = ww * .035f
                    val fieldRight = ww * .965f
                    val fieldTop = hh * .055f
                    val fieldBottom = hh * .945f
                    val fieldW = fieldRight - fieldLeft
                    val fieldH = fieldBottom - fieldTop
                    val liveBallX = ballX.value
                    val liveBallY = ballY.value

                    fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
                    fun teamClub(isHome: Boolean) = if (isHome) home else away
                    fun teamXi(isHome: Boolean) = if (isHome) m.homeXi else m.awayXi
                    fun teamCoords(isHome: Boolean) = if (isHome) homeCoords else awayCoords
                    fun teamSlots(isHome: Boolean) = if (isHome) homeSlots else awaySlots
                    fun hasBall(isHome: Boolean) = activeClubId == teamClub(isHome).id
                    fun ballProgress(isHome: Boolean) = if (isHome) liveBallY else 1f - liveBallY
                    fun toAbsoluteLong(isHome: Boolean, progress: Float) = if (isHome) progress else 1f - progress

                    fun screenPoint(lateral: Float, longitudinal: Float): Offset {
                        return Offset(
                            fieldLeft + longitudinal.coerceIn(.015f, .985f) * fieldW,
                            fieldTop + lateral.coerceIn(.015f, .985f) * fieldH,
                        )
                    }

                    // --- Spielfeld ---
                    drawRoundRect(Color(0xFF4E971C), Offset.Zero, Size(ww, hh), CornerRadius(15.dp.toPx()))
                    val stripeCount = 12
                    repeat(stripeCount) { i ->
                        val x = fieldLeft + fieldW * i / stripeCount
                        drawRect(
                            if (i % 2 == 0) Color(0xFF5EA822).copy(alpha = .72f) else Color(0xFF428A18).copy(alpha = .68f),
                            Offset(x, fieldTop),
                            Size(fieldW / stripeCount + 1f, fieldH),
                        )
                    }
                    val line = Color.White.copy(alpha = .78f)
                    val lw = 1.15.dp.toPx()
                    drawRect(line, Offset(fieldLeft, fieldTop), Size(fieldW, fieldH), style = Stroke(lw))
                    drawLine(line, Offset(fieldLeft + fieldW * .5f, fieldTop), Offset(fieldLeft + fieldW * .5f, fieldBottom), lw)
                    val center = Offset(fieldLeft + fieldW * .5f, fieldTop + fieldH * .5f)
                    drawCircle(line, fieldH * .115f, center, style = Stroke(lw))
                    drawCircle(line, 1.7.dp.toPx(), center)

                    val penaltyW = fieldW * .145f
                    val penaltyH = fieldH * .48f
                    val sixW = fieldW * .058f
                    val sixH = fieldH * .22f
                    val penTop = fieldTop + (fieldH - penaltyH) / 2f
                    val sixTop = fieldTop + (fieldH - sixH) / 2f
                    drawRect(line, Offset(fieldLeft, penTop), Size(penaltyW, penaltyH), style = Stroke(lw))
                    drawRect(line, Offset(fieldRight - penaltyW, penTop), Size(penaltyW, penaltyH), style = Stroke(lw))
                    drawRect(line, Offset(fieldLeft, sixTop), Size(sixW, sixH), style = Stroke(lw))
                    drawRect(line, Offset(fieldRight - sixW, sixTop), Size(sixW, sixH), style = Stroke(lw))
                    drawCircle(line, 1.55.dp.toPx(), Offset(fieldLeft + fieldW * .105f, fieldTop + fieldH * .5f))
                    drawCircle(line, 1.55.dp.toPx(), Offset(fieldRight - fieldW * .105f, fieldTop + fieldH * .5f))
                    val goalDepth = 7.dp.toPx()
                    val goalH = fieldH * .145f
                    val goalY = fieldTop + (fieldH - goalH) / 2f
                    drawRect(Color.White.copy(alpha = .58f), Offset(fieldLeft - goalDepth, goalY), Size(goalDepth, goalH), style = Stroke(1.dp.toPx()))
                    drawRect(Color.White.copy(alpha = .58f), Offset(fieldRight, goalY), Size(goalDepth, goalH), style = Stroke(1.dp.toPx()))

                    /**
                     * Formationen werden hier immer aus Sicht der jeweiligen Mannschaft gelesen:
                     * progress=0 ist das eigene Tor, progress=1 das gegnerische Tor.
                     * Dadurch funktioniert dieselbe Logik für Heim und Auswärts spiegelverkehrt.
                     */
                    fun formationBase(isHome: Boolean, index: Int): Pair<Float, Float> {
                        val base = teamCoords(isHome).getOrNull(index) ?: (.5f to .5f)
                        return base.first to (1f - base.second)
                    }

                    fun slot(isHome: Boolean, index: Int): Position =
                        teamSlots(isHome).getOrElse(index) { Position.ZM }

                    fun role(isHome: Boolean, id: Int): PlayerRole =
                        teamClub(isHome).tactics.roles[id] ?: PlayerRole.AUTO

                    fun instructions(isHome: Boolean, id: Int): List<PlayerInstruction> =
                        teamClub(isHome).tactics.instructions[id].orEmpty()

                    fun isFullback(p: Position) = p == Position.LV || p == Position.RV
                    fun isWing(p: Position) = p == Position.LA || p == Position.RA
                    fun isCentralDefender(p: Position) = p == Position.IV
                    fun isMidfield(p: Position) = p in setOf(Position.DM, Position.ZM, Position.OM)

                    fun attackAdvanceWeight(p: Position): Float = when (p) {
                        Position.TW -> .18f
                        Position.IV -> .95f
                        Position.LV, Position.RV -> 1.08f
                        Position.DM -> 1.00f
                        Position.ZM -> 1.06f
                        Position.OM -> 1.12f
                        Position.LA, Position.RA -> 1.12f
                        Position.ST -> 1.04f
                    }

                    fun attackCap(p: Position): Float = when (p) {
                        Position.TW -> .20f
                        Position.IV -> .60f
                        Position.LV, Position.RV -> .82f
                        Position.DM -> .79f
                        Position.ZM -> .87f
                        Position.OM -> .93f
                        Position.LA, Position.RA, Position.ST -> .955f
                    }

                    fun attackFloor(p: Position): Float = when (p) {
                        Position.TW -> .045f
                        Position.IV -> .16f
                        Position.LV, Position.RV -> .18f
                        Position.DM -> .27f
                        Position.ZM -> .34f
                        Position.OM -> .43f
                        Position.LA, Position.RA -> .48f
                        Position.ST -> .52f
                    }

                    fun attackingBaseShape(isHome: Boolean, index: Int, id: Int): Pair<Float, Float> {
                        val club = teamClub(isHome)
                        val tactics = club.tactics
                        val p = slot(isHome, index)
                        val r = role(isHome, id)
                        val ins = instructions(isHome, id)
                        val (baseLat, baseProg) = formationBase(isHome, index)
                        val bProg = ballProgress(isHome)

                        val mentality = (if (isHome) m.homeMentality else m.awayMentality)
                        val mentalityBias = (mentality - 3) * .012f
                        val lineBias = (tactics.line - 3) * .010f
                        val territorial = (bProg - .50f) * .40f
                        var teamAdvance = territorial + phaseShape * .11f + mentalityBias
                        if ((isHome && m.homeAllOutAttack) || (!isHome && m.awayAllOutAttack)) teamAdvance += .045f
                        if ((isHome && m.homeControlGame) || (!isHome && m.awayControlGame)) teamAdvance -= .018f

                        var weight = attackAdvanceWeight(p)
                        if (PlayerInstruction.HOLD_POSITION in ins || r == PlayerRole.ANCHOR) weight *= .55f
                        if (isCentralDefender(p)) weight += lineBias * 5f
                        if (tactics.buildUp == BuildUp.COUNTER && p in setOf(Position.IV, Position.DM)) weight *= .78f

                        var progress = (baseProg + teamAdvance * weight).coerceIn(attackFloor(p), attackCap(p))

                        // In einem gefährlichen Angriff darf die Restverteidigung nicht mehr am eigenen 16er kleben.
                        // Bei Ball im letzten Drittel stehen IV realistisch um Mittellinie / gegnerische Hälfte,
                        // während Außenverteidiger und Mittelfeld den Angriff absichern bzw. unterstützen.
                        if (phaseShape > .72f && bProg > .64f) {
                            val minimumByRole = when (p) {
                                Position.TW -> .11f
                                Position.IV -> .46f + lineBias
                                Position.LV, Position.RV -> .55f
                                Position.DM -> .59f
                                Position.ZM -> .66f
                                Position.OM -> .75f
                                Position.LA, Position.RA -> .79f
                                Position.ST -> .82f
                            }
                            progress = maxOf(progress, minimumByRole.coerceAtMost(attackCap(p)))
                        }

                        if (r == PlayerRole.OVERLAPPING_FULLBACK || PlayerInstruction.OVERLAP in ins) {
                            progress = (progress + .075f * (.45f + phaseShape * .55f)).coerceAtMost(.86f)
                        }
                        if (r == PlayerRole.BOX_TO_BOX) progress = (progress + .035f * phaseShape).coerceAtMost(.90f)
                        if (r == PlayerRole.INSIDE_FORWARD || PlayerInstruction.RUN_IN_BEHIND in ins) {
                            progress = (progress + .045f * (.5f + phaseShape * .5f)).coerceAtMost(.965f)
                        }
                        if (r == PlayerRole.POACHER) progress = maxOf(progress, .80f + .10f * phaseShape)

                        val widthScale = (.92f + (tactics.width - 3) * .055f + if (tactics.buildUp == BuildUp.WIDE) .08f else 0f)
                            .coerceIn(.76f, 1.12f)
                        var lateral = .5f + (baseLat - .5f) * widthScale
                        lateral += (liveBallX - .5f) * .105f

                        if (r == PlayerRole.INVERTED_FULLBACK) lateral = mix(lateral, .5f, .46f)
                        if (r == PlayerRole.OVERLAPPING_FULLBACK || PlayerInstruction.OVERLAP in ins) {
                            lateral = .5f + (lateral - .5f) * 1.10f
                        }
                        if (r == PlayerRole.INSIDE_FORWARD || PlayerInstruction.CUT_INSIDE in ins) lateral = mix(lateral, .5f, .42f)
                        if (r == PlayerRole.WINGER && isWing(p)) lateral = .5f + (lateral - .5f) * 1.08f

                        return lateral.coerceIn(.035f, .965f) to progress.coerceIn(.025f, .975f)
                    }

                    fun defendingBaseShape(isHome: Boolean, index: Int, id: Int): Pair<Float, Float> {
                        val club = teamClub(isHome)
                        val tactics = club.tactics
                        val p = slot(isHome, index)
                        val r = role(isHome, id)
                        val ins = instructions(isHome, id)
                        val (baseLat, _) = formationBase(isHome, index)
                        val bProg = ballProgress(isHome) // Distanz des Balls vom eigenen Tor.
                        val lineBias = (tactics.line - 3) * .018f
                        val dangerDrop = when (m.livePhase) {
                            LivePhase.COUNTER -> .060f
                            LivePhase.DANGEROUS_ATTACK -> .040f
                            LivePhase.ATTACK -> .015f
                            else -> 0f
                        }

                        val backLine = (bProg + .085f + lineBias - dangerDrop).coerceIn(.155f, .515f)
                        val compact = (1f - phaseShape * .26f).coerceIn(.70f, 1f)
                        val unitOffset = when (p) {
                            Position.TW -> -.115f
                            Position.IV -> 0f
                            Position.LV, Position.RV -> .012f
                            Position.DM -> .090f
                            Position.ZM -> .145f
                            Position.OM -> .205f
                            Position.LA, Position.RA -> .225f
                            Position.ST -> .285f
                        }
                        var progress = if (p == Position.TW) {
                            (backLine + unitOffset).coerceIn(.045f, .185f)
                        } else {
                            (backLine + unitOffset * compact).coerceIn(.12f, .80f)
                        }

                        if (PlayerInstruction.HOLD_POSITION in ins || r == PlayerRole.ANCHOR) {
                            progress = mix(progress, when (p) {
                                Position.DM -> (backLine + .065f)
                                Position.ZM -> (backLine + .11f)
                                else -> progress
                            }, .65f)
                        }

                        // Kompakter Block zur Ballseite statt starrer Viererkette quer über den Platz.
                        val defensiveWidth = (.68f + (tactics.width - 3) * .035f).coerceIn(.58f, .79f)
                        var lateral = .5f + (baseLat - .5f) * defensiveWidth
                        lateral += (liveBallX - .5f) * (.23f + phaseShape * .07f)

                        if (r == PlayerRole.INVERTED_FULLBACK) lateral = mix(lateral, .5f, .25f)
                        if (PlayerInstruction.TIGHT_MARKING in ins) lateral = mix(lateral, liveBallX, .08f)

                        return lateral.coerceIn(.055f, .945f) to progress.coerceIn(.025f, .975f)
                    }

                    fun supportIndexes(isHome: Boolean): Set<Int> {
                        if (!hasBall(isHome)) return emptySet()
                        val xi = teamXi(isHome)
                        val candidates = xi.indices.filter { idx ->
                            val id = xi[idx]
                            if (idx == 0 || id == 0 || id == m.livePlayerId || id in m.sentOff) return@filter false
                            val p = slot(isHome, idx)
                            val ins = instructions(isHome, id)
                            p !in setOf(Position.TW, Position.IV) && PlayerInstruction.HOLD_POSITION !in ins
                        }
                        return candidates.sortedBy { idx ->
                            val id = xi[idx]
                            val shape = attackingBaseShape(isHome, idx, id)
                            val dx = shape.first - liveBallX
                            val dy = shape.second - ballProgress(isHome)
                            dx * dx + dy * dy
                        }.take(if (teamClub(isHome).tactics.buildUp in setOf(BuildUp.SHORT, BuildUp.TIKI_TAKA)) 3 else 2).toSet()
                    }

                    fun presserIndexes(isHome: Boolean): Set<Int> {
                        if (hasBall(isHome)) return emptySet()
                        val xi = teamXi(isHome)
                        val tactics = teamClub(isHome).tactics
                        val wanted = when {
                            m.livePhase == LivePhase.DANGEROUS_ATTACK -> 1
                            tactics.pressing >= 4 -> 2
                            else -> 1
                        }
                        return xi.indices
                            .filter { idx ->
                                val id = xi[idx]
                                if (idx == 0 || id == 0 || id in m.sentOff) return@filter false
                                PlayerInstruction.PRESS_LESS !in instructions(isHome, id)
                            }
                            .sortedBy { idx ->
                                val id = xi[idx]
                                val shape = defendingBaseShape(isHome, idx, id)
                                val dx = shape.first - liveBallX
                                val dy = shape.second - ballProgress(isHome)
                                var score = dx * dx + dy * dy
                                if (PlayerInstruction.PRESS_MORE in instructions(isHome, id) || role(isHome, id) == PlayerRole.PRESSING_FORWARD) score -= .055f
                                score
                            }
                            .take(wanted)
                            .toSet()
                    }

                    val homeSupport = supportIndexes(true)
                    val awaySupport = supportIndexes(false)
                    val homePressers = presserIndexes(true)
                    val awayPressers = presserIndexes(false)

                    fun logicalPlayerPosition(isHome: Boolean, index: Int, id: Int): Pair<Float, Float> {
                        val p = slot(isHome, index)
                        val attacking = hasBall(isHome)
                        val bProg = ballProgress(isHome)

                        if (id == m.livePlayerId) {
                            return liveBallX to bProg
                        }

                        var shape = if (attacking) attackingBaseShape(isHome, index, id) else defendingBaseShape(isHome, index, id)
                        var lateral = shape.first
                        var progress = shape.second

                        if (attacking) {
                            val support = if (isHome) homeSupport else awaySupport
                            if (index in support) {
                                val side = if ((id + index) % 2 == 0) 1f else -1f
                                val supportLat = (liveBallX + side * if (isWing(p)) .105f else .075f).coerceIn(.06f, .94f)
                                val supportProg = (bProg - when (p) {
                                    Position.DM -> .11f
                                    Position.ZM -> .075f
                                    Position.OM -> .035f
                                    else -> .015f
                                }).coerceIn(.10f, .92f)
                                lateral = mix(lateral, supportLat, .34f)
                                progress = mix(progress, supportProg, .38f)
                            }

                            // Frontspieler attackieren Tiefe, aber nicht alle gleichzeitig.
                            val ins = instructions(isHome, id)
                            val r = role(isHome, id)
                            if (phaseShape > .45f && (
                                    p in setOf(Position.ST, Position.LA, Position.RA, Position.OM) ||
                                    PlayerInstruction.RUN_IN_BEHIND in ins ||
                                    r in setOf(PlayerRole.POACHER, PlayerRole.INSIDE_FORWARD)
                                )
                            ) {
                                val stagger = ((id * 31 + index * 17) % 5) / 4f
                                val extra = (.018f + stagger * .032f) * phaseShape
                                progress = (progress + extra).coerceAtMost(.97f)
                            }
                        } else {
                            val pressers = if (isHome) homePressers else awayPressers
                            if (index in pressers) {
                                val pressStrength = (.46f + teamClub(isHome).tactics.pressing * .065f).coerceIn(.52f, .78f)
                                lateral = mix(lateral, liveBallX, pressStrength)
                                // Presser tritt aus dem Block heraus; die restliche Linie bleibt stehen.
                                progress = mix(progress, bProg, pressStrength * .78f)
                            }
                        }

                        return lateral.coerceIn(.025f, .975f) to toAbsoluteLong(isHome, progress.coerceIn(.02f, .98f))
                    }

                    data class DotPlayer(
                        val isHome: Boolean,
                        val index: Int,
                        val id: Int,
                        val point: Offset,
                        val active: Boolean,
                    )

                    val dots = mutableListOf<DotPlayer>()
                    m.homeXi.forEachIndexed { index, id ->
                        if (id != 0 && id !in m.sentOff) {
                            val p = logicalPlayerPosition(true, index, id)
                            dots += DotPlayer(true, index, id, screenPoint(p.first, p.second), id == m.livePlayerId)
                        }
                    }
                    m.awayXi.forEachIndexed { index, id ->
                        if (id != 0 && id !in m.sentOff) {
                            val p = logicalPlayerPosition(false, index, id)
                            dots += DotPlayer(false, index, id, screenPoint(p.first, p.second), id == m.livePlayerId)
                        }
                    }

                    fun drawDot(v: DotPlayer) {
                        val team = if (v.isHome) home else away
                        val primary = Color(team.primary)
                        val secondary = Color(team.secondary)
                        val isKeeper = v.index == 0
                        val baseRadius = if (isKeeper) 6.5.dp.toPx() else 5.8.dp.toPx()
                        val radius = if (v.active) 14.1.dp.toPx() * focusPulse else baseRadius

                        drawCircle(Color.Black.copy(alpha = .30f), radius * 1.08f, Offset(v.point.x + 1.8.dp.toPx(), v.point.y + 2.4.dp.toPx()))
                        if (v.active) {
                            drawCircle(primary.copy(alpha = .18f), radius * 1.52f, v.point)
                            drawCircle(Color.White.copy(alpha = .82f), radius * 1.20f, v.point, style = Stroke(1.2.dp.toPx()))
                        }
                        drawCircle(primary, radius, v.point)
                        drawCircle(
                            if (isKeeper) Color.White.copy(alpha = .80f) else secondary.copy(alpha = .95f),
                            radius,
                            v.point,
                            style = Stroke(if (v.active) 2.0.dp.toPx() else 1.4.dp.toPx()),
                        )

                        if (v.active && actorNumber.isNotBlank()) {
                            drawText(
                                numberLayout,
                                topLeft = Offset(
                                    v.point.x - numberLayout.size.width / 2f,
                                    v.point.y - numberLayout.size.height / 2f,
                                ),
                            )
                        }
                    }

                    dots.filter { !it.active }.forEach(::drawDot)
                    dots.filter { it.active }.forEach(::drawDot)

                    val activeDot = dots.firstOrNull { it.active }
                    val engineBall = screenPoint(liveBallX, liveBallY)
                    val ballPoint = if (activeDot != null) {
                        val direction = if (activeDot.isHome) 1f else -1f
                        Offset(activeDot.point.x + 12.dp.toPx() * direction, activeDot.point.y + 7.dp.toPx())
                    } else engineBall
                    drawCircle(Color.Black.copy(alpha = .42f), 4.6.dp.toPx(), Offset(ballPoint.x + 1.dp.toPx(), ballPoint.y + 1.3.dp.toPx()))
                    drawCircle(Color.White, 4.2.dp.toPx(), ballPoint)
                    drawCircle(Color(0xFF202020), 4.2.dp.toPx(), ballPoint, style = Stroke(.9.dp.toPx()))
                    drawCircle(Color(0xFF202020), 1.25.dp.toPx(), Offset(ballPoint.x - .5.dp.toPx(), ballPoint.y - .5.dp.toPx()))

                    activeDot?.let { v ->
                        val padX = 6.dp.toPx()
                        val padY = 2.5.dp.toPx()
                        val labelW = nameLayout.size.width + padX * 2
                        val labelH = nameLayout.size.height + padY * 2
                        val preferRight = v.point.x < fieldLeft + fieldW * .67f
                        val rawLeft = if (preferRight) v.point.x + 18.dp.toPx() else v.point.x - labelW - 18.dp.toPx()
                        val left = rawLeft.coerceIn(4.dp.toPx(), (ww - labelW - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                        val top = (v.point.y + 5.dp.toPx()).coerceIn(4.dp.toPx(), (hh - labelH - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                        drawRoundRect(Color.Black.copy(alpha = .76f), Offset(left, top), Size(labelW, labelH), CornerRadius(5.dp.toPx()))
                        drawText(nameLayout, topLeft = Offset(left + padX, top + padY))
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 7.dp)
                        .background(Color.Black.copy(alpha = .48f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(phaseText(m), color = Chalk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                "Draufsicht · echte Teamhöhe · Restverteidigung rückt nach · Passdreiecke · kompakter Defensivblock · ballnahes Pressing",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            MatchEngine.stoppageTimeOverlay(m)?.let { added ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        added,
                        color = Chalk.copy(alpha = .88f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.background(Color.Black.copy(alpha = .42f), RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicLiveMatchAnimation(w: World, m: LiveMatch, frameDurationMs: Long) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val activeId = m.effectiveLiveClubId()
    val active = if (activeId == home.id) home else away
    val actor = w.players[m.livePlayerId]

    Surface(color = Color(0xFF071812), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TeamHeader(home.shortName, Color(home.primary))
                Text("${MatchEngine.clockLabel(m)}. Minute", color = Muted, style = MaterialTheme.typography.labelMedium)
                TeamHeader(away.shortName, Color(away.primary))
            }
            when (m.livePhase) {
                LivePhase.SHOT_OFF_TARGET, LivePhase.SHOT_ON_TARGET, LivePhase.WOODWORK, LivePhase.GOAL -> ShotScene(w, m, activeId, actor?.name ?: active.shortName, frameDurationMs)
                LivePhase.VAR -> VarReviewScene(w, m, activeId, actor?.name ?: active.shortName)
                LivePhase.SHOOTOUT -> ShotScene(w,m,activeId,actor?.name?:active.shortName,frameDurationMs)
                LivePhase.PENALTY -> OpenPlayScene(m,home.shortName,away.shortName,activeId,Color(home.primary),Color(away.primary),frameDurationMs)
                LivePhase.CORNER -> CornerScene(w, m, activeId, actor?.name ?: active.shortName, frameDurationMs)
                LivePhase.YELLOW_CARD, LivePhase.YELLOW_RED_CARD, LivePhase.RED_CARD, LivePhase.INJURY -> IncidentScene(w, m, activeId)
                else -> OpenPlayScene(
                    m = m,
                    homeShort = home.shortName,
                    awayShort = away.shortName,
                    activeId = activeId,
                    homeColor = Color(home.primary),
                    awayColor = Color(away.primary),
                    frameDurationMs = frameDurationMs,
                )
            }
            MatchEngine.stoppageTimeOverlay(m)?.let { added ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        added,
                        color = Chalk.copy(alpha = .88f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.background(Color.Black.copy(alpha = .42f), RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamHeader(shortName: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(99.dp)).border(.7.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(99.dp)))
        Text(shortName, color = Chalk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OpenPlayScene(
    m: LiveMatch,
    homeShort: String,
    awayShort: String,
    activeId: Int,
    homeColor: Color,
    awayColor: Color,
    frameDurationMs: Long,
) {
    val activeColor = if (activeId == m.homeId) homeColor else awayColor
    val ballX = remember(m.fixtureId) { Animatable(m.ballX.coerceIn(.04f, .96f)) }
    val ballY = remember(m.fixtureId) { Animatable(m.ballY.coerceIn(.04f, .96f)) }
    val trail = remember(m.fixtureId) { mutableStateListOf<Pair<Float, Float>>() }
    val event = m.liveEventSerial
    val targetX = m.ballX.coerceIn(.04f, .96f)
    val targetY = m.ballY.coerceIn(.04f, .96f)
    // Ausschließlich echte Engine-Frames steuern die sichtbare Ballbewegung. PassEvents sind
    // Analyse-/Statistikdaten und dürfen niemals als zweite, konkurrierende Simulation dienen.
    val engineTrail = m.ballTrace.takeLast(5)
    LaunchedEffect(event, m.livePhase, activeId, frameDurationMs, targetX, targetY) {
        if (trail.isEmpty()) {
            engineTrail.dropLast(1).forEach { trail.add(it.x to it.y) }
            if (trail.isEmpty()) trail.add(ballX.value to ballY.value)
        }
        val animationMs = (frameDurationMs * .88).toInt().coerceIn(260, 3600)
        coroutineScope {
            launch { ballX.animateTo(targetX, tween(animationMs)) }
            launch { ballY.animateTo(targetY, tween(animationMs)) }
        }
        trail.add(targetX to targetY)
        while (trail.size > 5) trail.removeAt(0)
    }

    val club = if (activeId == m.homeId) homeShort else awayShort
    val detail = m.liveDetail.takeIf { it.isNotBlank() } ?: m.livePhase.label
    val textMeasurer = rememberTextMeasurer()
    val clubLayout = remember(club) {
        textMeasurer.measure(
            AnnotatedString(club),
            style = TextStyle(color = Chalk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
    val detailLayout = remember(detail) {
        textMeasurer.measure(
            AnnotatedString(detail),
            style = TextStyle(color = Chalk, fontSize = 10.sp, fontWeight = FontWeight.Bold),
        )
    }

    Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
        LivePitchImage(Modifier.matchParentSize())
        // Ball, Spur und die beiden mitbewegten Labels werden in EINEM Draw-Pass gerendert.
        // Animatable-Reads in der Draw-Phase invalidieren nur die Canvas-Ebene und lösen
        // nicht mehr bei jedem Animationsframe Text-Layout und Compose-Recomposition aus.
        Canvas(Modifier.matchParentSize()) {
            val ww = size.width
            val hh = size.height
            fun p(x: Float, y: Float) = Offset(ww * engineProjectedX(x, y), hh * engineProjectedY(x, y))

            val liveBallX = ballX.value
            val liveBallY = ballY.value
            val points = trail.takeLast(6).map { p(it.first, it.second) } + p(liveBallX, liveBallY)
            for (i in 1 until points.size) {
                val alpha = (.07f + .085f * i).coerceAtMost(.55f)
                drawLine(activeColor.copy(alpha = alpha), points[i - 1], points[i], (1f + i * .14f).dp.toPx())
            }

            val ball = p(liveBallX, liveBallY)
            drawOval(Color.Black.copy(alpha = .34f), Offset(ball.x - 8.dp.toPx(), ball.y + 6.dp.toPx()), Size(16.dp.toPx(), 5.dp.toPx()))
            drawCircle(Color.White, 7.5.dp.toPx(), ball)
            drawCircle(Color(0xFF1C1C1C), 7.5.dp.toPx(), ball, style = Stroke(1.1.dp.toPx()))
            drawCircle(Color(0xFF252525), 2.1.dp.toPx(), Offset(ball.x - 1.dp.toPx(), ball.y - 1.dp.toPx()))

            val padX = 6.dp.toPx()
            val padY = 2.dp.toPx()
            val clubW = clubLayout.size.width + padX * 2
            val clubH = clubLayout.size.height + padY * 2
            val clubLeft = (ball.x - clubW / 2f).coerceIn(4.dp.toPx(), (ww - clubW - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
            val clubTop = (ball.y - 43.dp.toPx()).coerceIn(4.dp.toPx(), hh - clubH - 4.dp.toPx())
            drawRoundRect(Color.Black.copy(alpha = .52f), Offset(clubLeft, clubTop), Size(clubW, clubH), CornerRadius(5.dp.toPx()))
            drawText(clubLayout, topLeft = Offset(clubLeft + padX, clubTop + padY))

            val detailW = (detailLayout.size.width + padX * 2).coerceAtMost(ww - 8.dp.toPx())
            val detailH = detailLayout.size.height + padY * 2
            val detailLeft = (ball.x - detailW / 2f).coerceIn(4.dp.toPx(), (ww - detailW - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
            val detailTop = (ball.y + 16.dp.toPx()).coerceIn(4.dp.toPx(), hh - detailH - 4.dp.toPx())
            drawRoundRect(Color.Black.copy(alpha = .46f), Offset(detailLeft, detailTop), Size(detailW, detailH), CornerRadius(5.dp.toPx()))
            drawText(detailLayout, topLeft = Offset(detailLeft + padX, detailTop + padY))
        }

        if (m.livePhase in setOf(LivePhase.FREE_KICK, LivePhase.DANGEROUS_FREE_KICK, LivePhase.PENALTY)) {
            Text(
                m.livePhase.label.uppercase(),
                color = Chalk,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color.Black.copy(alpha = .66f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ShotScene(w: World, m: LiveMatch, activeId: Int, shooterName: String, frameDurationMs: Long) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val progress = remember(m.fixtureId, m.liveEventSerial) { Animatable(0f) }
    val event = m.liveEventSerial

    LaunchedEffect(event, m.livePhase) {
        progress.snapTo(0f)
        val base = when (m.livePhase) {
            LivePhase.GOAL -> 1080
            LivePhase.SHOT_ON_TARGET -> 980
            else -> 880
        }
        progress.animateTo(1f, tween((frameDurationMs * .72).toInt().coerceIn(base, 2200)))
    }

    val t = progress.value
    // Erst wenn der Ball das letzte Felddrittel der Flugbahn erreicht, blendet die Kamera
    // in die Toransicht. Dadurch springt die Darstellung nicht mehr vor dem Schuss.
    val cam = ((t - .42f) / .34f).coerceIn(0f, 1f)
    val startX = m.lastShotX.coerceIn(.02f, .98f)
    val startY = m.lastShotY.coerceIn(.02f, .98f)
    val fieldGoalX = .50f
    val fieldGoalY = if (activeId == home.id) .02f else .98f
    val bx = startX + (fieldGoalX - startX) * t
    val by = startY + (fieldGoalY - startY) * t
    val shotContext = remember(event, startX, startY, activeId) {
        ShotContext(startX, startY, attackingHome = activeId == home.id)
    }
    val location = ShotModel.locationLabel(shotContext)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            LivePitchImage(Modifier.matchParentSize().graphicsLayer { alpha = 1f - cam })
            Image(
                painter = painterResource(R.drawable.live_goal_view),
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer { alpha = cam },
                contentScale = ContentScale.FillBounds,
            )

            Canvas(Modifier.matchParentSize()) {
                val ww = size.width
                val hh = size.height
                fun fieldPoint(x: Float, y: Float) = Offset(ww * engineProjectedX(x, y), hh * engineProjectedY(x, y))
                fun mix(a: Offset, b: Offset, amount: Float) = Offset(a.x + (b.x - a.x) * amount, a.y + (b.y - a.y) * amount)

                val fieldBall = fieldPoint(bx, by)
                val goalStart = Offset(ww * .50f, hh * .86f)
                val targetX = ww * (.28f + m.lastShotTargetX * .44f)
                val targetY = hh * (.58f - m.lastShotTargetY * .25f)
                val arc = sin(PI * t.toDouble()).toFloat() * hh * .13f
                val goalBall = Offset(goalStart.x + (targetX - goalStart.x) * t, goalStart.y + (targetY - goalStart.y) * t - arc)
                val ball = mix(fieldBall, goalBall, cam)

                for (i in 6 downTo 1) {
                    val tt = (t - i * .055f).coerceIn(0f, 1f)
                    val trailCam = ((tt - .42f) / .34f).coerceIn(0f, 1f)
                    val trailArc = sin(PI * tt.toDouble()).toFloat() * hh * .13f
                    val goalTrail = Offset(goalStart.x + (targetX - goalStart.x) * tt, goalStart.y + (targetY - goalStart.y) * tt - trailArc)
                    val fbx = startX + (fieldGoalX - startX) * tt
                    val fby = startY + (fieldGoalY - startY) * tt
                    val trailPoint = mix(fieldPoint(fbx, fby), goalTrail, trailCam)
                    drawLine(Color(club.primary).copy(alpha = .07f + i * .065f), trailPoint, ball, (1f + i * .13f).dp.toPx())
                }

                drawOval(Color.Black.copy(alpha = .30f), Offset(ball.x - 8.dp.toPx(), ball.y + 6.dp.toPx()), Size(16.dp.toPx(), 5.dp.toPx()))
                if (m.livePhase == LivePhase.GOAL && t > .84f) drawCircle(Color(club.primary).copy(alpha = .24f * cam), 48.dp.toPx(), ball)
                drawCircle(Color.White, 8.dp.toPx(), ball)
                drawCircle(Color(0xFF202020), 8.dp.toPx(), ball, style = Stroke(1.2.dp.toPx()))
                drawCircle(Color(0xFF252525), 2.2.dp.toPx(), Offset(ball.x - 1.dp.toPx(), ball.y - 1.dp.toPx()))
            }

            if (cam > .16f) {
                Text(
                    "TORANSICHT", color = Chalk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = .58f), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(phaseText(m), color = if (m.livePhase == LivePhase.GOAL) Grass else Chalk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text("$shooterName · ${club.shortName} · ${MatchEngine.clockLabel(m)}. Minute", color = Muted, textAlign = TextAlign.Center)
            Text(location, color = Chalk, style = MaterialTheme.typography.labelMedium)
            if(m.lastShotTargetLabel.isNotBlank()) Text("Ziel: ${m.lastShotTargetLabel}", color = Gold, style = MaterialTheme.typography.labelMedium)
            if (m.pendingCornerClubId == club.id) Text("Ecke folgt", color = Grass, style = MaterialTheme.typography.labelMedium)
        }
    }
}


@Composable
private fun VarReviewScene(w: World, m: LiveMatch, activeId: Int, shooterName: String) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val checking = m.varReviewResult.isBlank()
    val decisionColor = when {
        checking -> Gold
        m.varReviewResult.startsWith("Tor zählt") -> Grass
        else -> Clay
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            Image(
                painter = painterResource(R.drawable.live_goal_view),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            Canvas(Modifier.matchParentSize()) {
                val targetX = size.width * (.28f + m.lastShotTargetX * .44f)
                val targetY = size.height * (.58f - m.lastShotTargetY * .25f)
                val target = Offset(targetX, targetY)
                drawCircle(Color(club.primary).copy(alpha = .20f), 34.dp.toPx(), target)
                drawCircle(Color.White, 8.dp.toPx(), target)
                drawCircle(Color(0xFF202020), 8.dp.toPx(), target, style = Stroke(1.2.dp.toPx()))
                drawCircle(Color(0xFF252525), 2.2.dp.toPx(), Offset(target.x - 1.dp.toPx(), target.y - 1.dp.toPx()))
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "VAR",
                    color = Chalk,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = .82f), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Chalk.copy(alpha = .9f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                )
                Text(
                    if (checking) m.varReviewReason.ifBlank { "Treffer wird überprüft" } else m.varReviewResult,
                    color = decisionColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (checking) "VAR-Prüfung" else "VAR-Entscheidung", color = decisionColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text("$shooterName · ${club.shortName} · ${MatchEngine.clockLabel(m)}. Minute", color = Muted, textAlign = TextAlign.Center)
            if (checking) Text(m.varReviewReason, color = Chalk, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            else {
                Text(m.varReviewResult, color = decisionColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                if (m.varReviewReason.isNotBlank()) Text("Geprüft: ${m.varReviewReason}", color = Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
            if (m.lastShotTargetLabel.isNotBlank()) Text("Schussziel: ${m.lastShotTargetLabel}", color = Gold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CornerScene(w: World, m: LiveMatch, activeId: Int, takerName: String, frameDurationMs: Long) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val goalRight = activeId == home.id
    val progress = remember(m.fixtureId, m.liveEventSerial) { Animatable(0f) }
    val lowerCorner = m.ballX > .5f
    LaunchedEffect(m.liveEventSerial, frameDurationMs) { progress.snapTo(0f); progress.animateTo(1f, tween((frameDurationMs * .82).toInt().coerceIn(900, 2300))) }
    val t = progress.value
    val startX = if (goalRight) .99f else .01f
    val startY = if (lowerCorner) .96f else .04f
    val endX = if (goalRight) .74f else .26f
    val endY = .50f
    val bx = startX + (endX - startX) * t
    val groundY = startY + (endY - startY) * t
    val by = groundY - sin(PI * t.toDouble()).toFloat() * .045f

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            LivePitchImage(Modifier.matchParentSize())
            Canvas(Modifier.matchParentSize()) {
                val ww = size.width
                val hh = size.height
                fun q(x: Float, y: Float) = Offset(ww * projectedX(x, y), hh * projectedY(x, y))

                val shadow = q(bx, groundY)
                drawOval(Color.Black.copy(alpha = .29f), Offset(shadow.x - 7.dp.toPx(), shadow.y + 4.dp.toPx()), Size(14.dp.toPx(), 5.dp.toPx()))
                for (i in 5 downTo 1) {
                    val tt = (t - i * .06f).coerceIn(0f, 1f)
                    val px = startX + (endX - startX) * tt
                    val gy = startY + (endY - startY) * tt
                    val py = gy - sin(PI * tt.toDouble()).toFloat() * .045f
                    val nt = (tt + .05f).coerceAtMost(t)
                    val nx = startX + (endX - startX) * nt
                    val ng = startY + (endY - startY) * nt
                    val ny = ng - sin(PI * nt.toDouble()).toFloat() * .045f
                    drawLine(Color(club.primary).copy(alpha = .1f + i * .07f), q(px, py), q(nx, ny), (1f + i * .15f).dp.toPx())
                }
                val ball = q(bx, by)
                drawCircle(Color.White, 7.3.dp.toPx(), ball)
                drawCircle(Color(0xFF202020), 7.3.dp.toPx(), ball, style = Stroke(1.1.dp.toPx()))
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ecke · ${club.shortName}", fontWeight = FontWeight.ExtraBold, color = Chalk)
            Text("$takerName · ${MatchEngine.clockLabel(m)}. Minute", color = Muted)
        }
    }
}

@Composable
private fun IncidentScene(w: World, m: LiveMatch, activeId: Int) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val player = w.players[m.livePlayerId]
    val title = when (m.livePhase) {
        LivePhase.YELLOW_CARD -> "Gelbe Karte"
        LivePhase.YELLOW_RED_CARD -> "Gelb-Rot"
        LivePhase.RED_CARD -> "Rote Karte"
        LivePhase.INJURY -> "Verletzung"
        else -> m.livePhase.label
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1.5f), contentAlignment = Alignment.Center) {
        LivePitchImage(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .58f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (m.livePhase == LivePhase.INJURY) {
                Text("+", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Clay)
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (m.livePhase == LivePhase.YELLOW_RED_CARD) Box(Modifier.size(width = 36.dp, height = 54.dp).background(Color(0xFFFFD54A), RoundedCornerShape(5.dp)).border(1.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(5.dp)))
                Box(Modifier.size(width = 40.dp, height = 60.dp).background(if (m.livePhase == LivePhase.YELLOW_CARD) Color(0xFFFFD54A) else Color(0xFFD7423F), RoundedCornerShape(5.dp)).border(1.dp, Color.White.copy(alpha = .38f), RoundedCornerShape(5.dp)))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Chalk)
            Text("${player?.name ?: "Spieler"} · ${club.shortName}", color = Chalk, fontWeight = FontWeight.Bold)
            Text("${MatchEngine.clockLabel(m)}. Minute · ${if (m.incidentPause) "Spiel unterbrochen" else "Foulentscheidung"}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

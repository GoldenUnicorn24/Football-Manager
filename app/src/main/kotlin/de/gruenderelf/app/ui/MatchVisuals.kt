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
import de.gruenderelf.engine.Position
import de.gruenderelf.engine.PlayerRole
import de.gruenderelf.engine.PlayerInstruction
import de.gruenderelf.engine.LivePhase
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

    val motion by rememberInfiniteTransition(label = "top-down-motion").animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "top-down-player-motion",
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

                    fun screenPoint(lateral: Float, longitudinal: Float): Offset {
                        return Offset(
                            fieldLeft + longitudinal.coerceIn(.015f, .985f) * fieldW,
                            fieldTop + lateral.coerceIn(.015f, .985f) * fieldH,
                        )
                    }

                    drawRoundRect(
                        Color(0xFF4E971C),
                        Offset(0f, 0f),
                        Size(ww, hh),
                        CornerRadius(15.dp.toPx()),
                    )
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
                    drawRect(Color.White.copy(alpha = .58f), Offset(fieldLeft - goalDepth, goalY), Size(goalDepth, goalH), style = Stroke(1.0.dp.toPx()))
                    drawRect(Color.White.copy(alpha = .58f), Offset(fieldRight, goalY), Size(goalDepth, goalH), style = Stroke(1.0.dp.toPx()))

                    /**
                     * Taktische Formationslogik 2.0
                     *
                     * Intern wird jede Mannschaft immer aus Sicht "eigenes Tor = 0,
                     * gegnerisches Tor = 1" berechnet. Dadurch kann die komplette
                     * Formation als Block mit dem Ball nachrücken, statt dass die
                     * Viererkette bei einem Angriff an ihrer Startposition klebt.
                     */
                    fun teamClub(isHome: Boolean) = if (isHome) home else away
                    fun teamXi(isHome: Boolean) = if (isHome) m.homeXi else m.awayXi
                    fun teamFormation(isHome: Boolean) = if (isHome) m.homeFormation else m.awayFormation
                    fun teamSlots(isHome: Boolean) = Formations.positions(teamFormation(isHome))
                    fun teamCoords(isHome: Boolean) = if (isHome) homeCoords else awayCoords
                    fun teamHasBall(isHome: Boolean) = activeClubId == if (isHome) home.id else away.id
                    fun canonicalBallProgress(isHome: Boolean) = if (isHome) liveBallY else 1f - liveBallY
                    fun screenLongitudinal(isHome: Boolean, progress: Float) = if (isHome) progress else 1f - progress

                    fun effectiveLine(isHome: Boolean): Int {
                        val c = teamClub(isHome)
                        return when {
                            if (isHome) m.homeConserveEnergy else m.awayConserveEnergy -> 1
                            if (isHome) m.homeAllOutAttack else m.awayAllOutAttack -> 5
                            if (isHome) m.homeControlGame else m.awayControlGame -> 2
                            else -> c.tactics.line
                        }.coerceIn(1, 5)
                    }

                    fun effectivePressing(isHome: Boolean): Int {
                        val c = teamClub(isHome)
                        return when {
                            if (isHome) m.homeConserveEnergy else m.awayConserveEnergy -> 1
                            if (isHome) m.homeAllOutAttack else m.awayAllOutAttack -> 5
                            if (isHome) m.homeControlGame else m.awayControlGame -> 2
                            else -> c.tactics.pressing
                        }.coerceIn(1, 5)
                    }

                    fun effectiveWidth(isHome: Boolean): Int {
                        val c = teamClub(isHome)
                        val base = c.tactics.width.coerceIn(1, 5)
                        return when {
                            if (isHome) m.homeAllOutAttack else m.awayAllOutAttack -> base.coerceAtLeast(4)
                            if (isHome) m.homeConserveEnergy else m.awayConserveEnergy -> base.coerceAtMost(3)
                            else -> base
                        }
                    }

                    fun effectiveMentality(isHome: Boolean): Int =
                        (if (isHome) m.homeMentality else m.awayMentality).coerceIn(1, 5)

                    fun slotPosition(isHome: Boolean, index: Int, id: Int): Position =
                        teamSlots(isHome).getOrElse(index) { w.players[id]?.position ?: Position.ZM }

                    fun playerRole(isHome: Boolean, id: Int): PlayerRole =
                        teamClub(isHome).tactics.roles[id] ?: w.players[id]?.role ?: PlayerRole.AUTO

                    fun playerInstructions(isHome: Boolean, id: Int): List<PlayerInstruction> =
                        teamClub(isHome).tactics.instructions[id]?.toList() ?: emptyList()

                    fun formationBase(isHome: Boolean, index: Int): Pair<Float, Float> {
                        val base = teamCoords(isHome).getOrNull(index) ?: (.5f to .5f)
                        // Formations.coordinates: GK liegt bei ~.90, Stürmer bei ~.12.
                        // Für unsere taktische Achse drehen wir das zu 0 = eigenes Tor.
                        return base.first to (1f - base.second)
                    }

                    fun depthNorm(isHome: Boolean, index: Int): Float {
                        val depth = formationBase(isHome, index).second
                        return ((depth - .29f) / .59f).coerceIn(0f, 1f)
                    }

                    fun teamBackLine(isHome: Boolean, hasBall: Boolean): Float {
                        val ballProgress = canonicalBallProgress(isHome)
                        val line = effectiveLine(isHome)
                        val mentality = effectiveMentality(isHome)
                        val lineBias = (line - 3) * .022f
                        val mentalityBias = (mentality - 3) * .012f

                        return if (hasBall) {
                            val teamLength = when (m.livePhase) {
                                LivePhase.DANGEROUS_ATTACK -> .34f
                                LivePhase.ATTACK -> .41f
                                LivePhase.COUNTER -> .54f
                                LivePhase.CORNER -> .29f
                                LivePhase.DANGEROUS_FREE_KICK -> .32f
                                LivePhase.FREE_KICK -> .40f
                                else -> .47f
                            }
                            var back = ballProgress - teamLength + lineBias + mentalityBias

                            // Entscheidender Unterschied zur alten Version:
                            // Bei etabliertem Angriff rückt die Restverteidigung wirklich bis
                            // an/über die Mittellinie nach und bleibt nicht am eigenen 16er.
                            if (m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress >= .68f) {
                                back = maxOf(back, ballProgress - .38f)
                                back = maxOf(back, .40f + (ballProgress - .68f) * .38f)
                            } else if (m.livePhase == LivePhase.ATTACK && ballProgress >= .60f) {
                                back = maxOf(back, ballProgress - .45f)
                            }
                            if (if (isHome) m.homeAllOutAttack else m.awayAllOutAttack) back += .045f
                            if (if (isHome) m.homeConserveEnergy else m.awayConserveEnergy) back -= .045f
                            back.coerceIn(.16f, .64f)
                        } else {
                            // Ohne Ball folgt die Kette der Ballhöhe. Je näher der Gegner dem
                            // eigenen Tor kommt, desto tiefer und kompakter wird der Block.
                            var back = .095f + ballProgress * .53f + lineBias + mentalityBias * .35f
                            if (m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress <= .30f) {
                                back = minOf(back, .18f + ballProgress * .10f)
                            }
                            if (m.livePhase == LivePhase.COUNTER && ballProgress <= .42f) back -= .025f
                            back.coerceIn(.085f, .61f)
                        }
                    }

                    fun teamFrontLine(isHome: Boolean, hasBall: Boolean, backLine: Float): Float {
                        val ballProgress = canonicalBallProgress(isHome)
                        return if (hasBall) {
                            val ahead = when (m.livePhase) {
                                LivePhase.COUNTER -> .11f
                                LivePhase.DANGEROUS_ATTACK -> .055f
                                LivePhase.ATTACK -> .065f
                                LivePhase.CORNER -> .035f
                                else -> .08f
                            }
                            maxOf(backLine + .25f, ballProgress + ahead).coerceIn(.42f, .975f)
                        } else {
                            val length = when (m.livePhase) {
                                LivePhase.DANGEROUS_ATTACK -> .285f
                                LivePhase.ATTACK -> .335f
                                LivePhase.COUNTER -> .40f
                                else -> .36f
                            }
                            (backLine + length).coerceAtMost(.79f)
                        }
                    }

                    fun skeletonPosition(isHome: Boolean, index: Int, id: Int): Pair<Float, Float> {
                        val hasBall = teamHasBall(isHome)
                        val (baseLat, _) = formationBase(isHome, index)
                        val position = slotPosition(isHome, index, id)
                        val width = effectiveWidth(isHome)
                        val ballProgress = canonicalBallProgress(isHome)
                        val backLine = teamBackLine(isHome, hasBall)
                        val frontLine = teamFrontLine(isHome, hasBall, backLine)
                        val keeper = position == Position.TW || index == 0

                        if (keeper) {
                            // Torwart rückt als Sweeper mit hoch, aber bleibt klar hinter der Kette.
                            var keeperProgress = .048f + backLine * .38f
                            if (playerRole(isHome, id) == PlayerRole.BUILD_UP_KEEPER) keeperProgress += .025f
                            keeperProgress = minOf(keeperProgress, backLine - .12f).coerceIn(.045f, .28f)
                            val keeperLat = .5f + (liveBallX - .5f) * .22f
                            return keeperLat.coerceIn(.36f, .64f) to keeperProgress
                        }

                        val norm = depthNorm(isHome, index)
                        var progress = backLine + (frontLine - backLine) * norm

                        // Mannschaftsbreite plus ballseitiges Verschieben.
                        val widthScale = if (hasBall) .77f + width * .072f else .61f + width * .054f
                        val ballShift = if (hasBall) .16f else .29f
                        val center = .5f + (liveBallX - .5f) * ballShift
                        var lateral = center + (baseLat - .5f) * widthScale

                        // Defensive Rollen-/Positionsgrundordnung.
                        when (position) {
                            Position.IV -> if (hasBall) progress -= .006f
                            Position.LV, Position.RV -> if (hasBall) progress += .028f
                            Position.DM -> progress -= if (hasBall) .025f else .012f
                            Position.ZM -> Unit
                            Position.OM -> if (hasBall) progress += .018f
                            Position.LA, Position.RA -> if (hasBall) progress += .025f
                            Position.ST -> if (hasBall) progress += .018f
                            Position.TW -> Unit
                        }

                        // Ballferne Seite rückt ein, ballnahe Seite bleibt etwas breiter.
                        if (!hasBall) {
                            val sameSide = (baseLat < .5f) == (liveBallX < .5f)
                            if (!sameSide) lateral += (.5f - lateral) * .18f
                        }

                        // In einem gefährlichen Angriff muss die eigene Kette sichtbar hoch stehen.
                        if (hasBall && m.livePhase == LivePhase.DANGEROUS_ATTACK && ballProgress > .70f) {
                            when (position) {
                                Position.IV -> progress = maxOf(progress, backLine)
                                Position.LV, Position.RV -> progress = maxOf(progress, backLine + .055f)
                                Position.DM -> progress = maxOf(progress, backLine + .11f)
                                else -> Unit
                            }
                        }

                        return lateral.coerceIn(.055f, .945f) to progress.coerceIn(.07f, .975f)
                    }

                    fun supportIndexes(isHome: Boolean): Set<Int> {
                        if (!teamHasBall(isHome)) return emptySet()
                        val xi = teamXi(isHome)
                        val ballProgress = canonicalBallProgress(isHome)
                        return xi.indices
                            .filter { idx ->
                                val id = xi[idx]
                                id != 0 && id != m.livePlayerId && id !in m.sentOff && id !in m.injured &&
                                    slotPosition(isHome, idx, id) != Position.TW &&
                                    PlayerInstruction.HOLD_POSITION !in playerInstructions(isHome, id)
                            }
                            .sortedBy { idx ->
                                val id = xi[idx]
                                val p = skeletonPosition(isHome, idx, id)
                                val dx = p.first - liveBallX
                                val dy = p.second - ballProgress
                                val pos = slotPosition(isHome, idx, id)
                                val positionalBonus = when (pos) {
                                    Position.DM, Position.ZM, Position.OM, Position.LA, Position.RA -> -.035f
                                    else -> 0f
                                }
                                dx * dx + dy * dy + positionalBonus
                            }
                            .take(if (m.livePhase == LivePhase.DANGEROUS_ATTACK) 3 else 2)
                            .toSet()
                    }

                    fun runnerIndexes(isHome: Boolean): Set<Int> {
                        if (!teamHasBall(isHome)) return emptySet()
                        val xi = teamXi(isHome)
                        val count = when (m.livePhase) {
                            LivePhase.DANGEROUS_ATTACK -> 3
                            LivePhase.COUNTER -> 3
                            LivePhase.ATTACK -> 2
                            else -> 1
                        }
                        return xi.indices
                            .filter { idx ->
                                val id = xi[idx]
                                if (id == 0 || id == m.livePlayerId || id in m.sentOff || id in m.injured) false
                                else {
                                    val pos = slotPosition(isHome, idx, id)
                                    pos in setOf(Position.ST, Position.LA, Position.RA, Position.OM, Position.LV, Position.RV) &&
                                        PlayerInstruction.HOLD_POSITION !in playerInstructions(isHome, id)
                                }
                            }
                            .sortedByDescending { idx ->
                                val id = xi[idx]
                                val p = w.players[id]
                                val pos = slotPosition(isHome, idx, id)
                                val instructions = playerInstructions(isHome, id)
                                val role = playerRole(isHome, id)
                                depthNorm(isHome, idx) * 100f +
                                    (p?.attributes?.pace ?: 50) * .12f +
                                    (if (PlayerInstruction.RUN_IN_BEHIND in instructions) 18f else 0f) +
                                    (if (role in setOf(PlayerRole.OVERLAPPING_FULLBACK, PlayerRole.WINGER, PlayerRole.INSIDE_FORWARD, PlayerRole.POACHER)) 7f else 0f) +
                                    (if (pos == Position.ST) 6f else 0f)
                            }
                            .take(count)
                            .toSet()
                    }

                    fun pressingIndexes(isHome: Boolean): Set<Int> {
                        if (teamHasBall(isHome)) return emptySet()
                        val xi = teamXi(isHome)
                        val ballProgress = canonicalBallProgress(isHome)
                        val press = effectivePressing(isHome)
                        var count = when (press) {
                            1 -> 1
                            2, 3 -> 2
                            else -> 3
                        }
                        // Im eigenen Strafraumnähe nicht drei Spieler blind aus der Kette ziehen.
                        if (ballProgress < .23f) count = minOf(count, 2)

                        return xi.indices
                            .filter { idx ->
                                val id = xi[idx]
                                id != 0 && id !in m.sentOff && id !in m.injured &&
                                    slotPosition(isHome, idx, id) != Position.TW
                            }
                            .sortedBy { idx ->
                                val id = xi[idx]
                                val p = skeletonPosition(isHome, idx, id)
                                val dx = p.first - liveBallX
                                val dy = p.second - ballProgress
                                val pos = slotPosition(isHome, idx, id)
                                val role = playerRole(isHome, id)
                                val instructions = playerInstructions(isHome, id)
                                var score = dx * dx + dy * dy
                                if (pos == Position.IV) score += if (ballProgress < .24f) .015f else .075f
                                if (pos == Position.DM) score -= .025f
                                if (role == PlayerRole.PRESSING_FORWARD) score -= .045f
                                if (PlayerInstruction.PRESS_MORE in instructions) score -= .055f
                                if (PlayerInstruction.PRESS_LESS in instructions) score += .12f
                                score
                            }
                            .take(count)
                            .toSet()
                    }

                    val homeSupport = supportIndexes(true)
                    val awaySupport = supportIndexes(false)
                    val homeRunners = runnerIndexes(true)
                    val awayRunners = runnerIndexes(false)
                    val homePressers = pressingIndexes(true)
                    val awayPressers = pressingIndexes(false)

                    fun logicalPlayerPosition(isHome: Boolean, index: Int, id: Int): Pair<Float, Float> {
                        val hasBall = teamHasBall(isHome)
                        val ballProgress = canonicalBallProgress(isHome)
                        val position = slotPosition(isHome, index, id)
                        val role = playerRole(isHome, id)
                        val instructions = playerInstructions(isHome, id)
                        val keeper = position == Position.TW || index == 0
                        val support = if (isHome) homeSupport else awaySupport
                        val runners = if (isHome) homeRunners else awayRunners
                        val pressers = if (isHome) homePressers else awayPressers
                        val skeleton = skeletonPosition(isHome, index, id)
                        var lateral = skeleton.first
                        var progress = skeleton.second

                        if (id == m.livePlayerId) {
                            lateral = liveBallX
                            progress = ballProgress
                        } else if (!keeper) {
                            if (hasBall) {
                                val side = if (formationBase(isHome, index).first < .5f) -1f else 1f

                                if (index in support && PlayerInstruction.HOLD_POSITION !in instructions) {
                                    // Dreiecke um den Ball statt elf Punkte auf parallelen Linien.
                                    lateral += (liveBallX - lateral) * .34f + side * .018f
                                    progress += (ballProgress - progress) * .30f
                                }

                                if (index in runners && PlayerInstruction.HOLD_POSITION !in instructions) {
                                    val run = when (m.livePhase) {
                                        LivePhase.COUNTER -> .105f
                                        LivePhase.DANGEROUS_ATTACK -> .080f
                                        LivePhase.ATTACK -> .055f
                                        else -> .035f
                                    }
                                    progress += run * (.72f + .28f * ((sin(motion + id * .37f) + 1f) / 2f))
                                }

                                when (role) {
                                    PlayerRole.OVERLAPPING_FULLBACK -> {
                                        progress += .060f
                                        lateral += side * .045f
                                    }
                                    PlayerRole.INVERTED_FULLBACK -> {
                                        progress += .025f
                                        lateral += (.5f - lateral) * .38f
                                    }
                                    PlayerRole.ANCHOR -> progress -= .055f
                                    PlayerRole.DEEP_PLAYMAKER -> progress -= .025f
                                    PlayerRole.BOX_TO_BOX -> progress += .030f
                                    PlayerRole.PLAYMAKER -> {
                                        lateral += (liveBallX - lateral) * .12f
                                        progress += (ballProgress - progress) * .10f
                                    }
                                    PlayerRole.INSIDE_FORWARD -> {
                                        lateral += (.5f - lateral) * .33f
                                        progress += .035f
                                    }
                                    PlayerRole.WINGER -> lateral += side * .035f
                                    PlayerRole.POACHER -> progress += .050f
                                    PlayerRole.TARGET_FORWARD -> progress -= .012f
                                    else -> Unit
                                }

                                if (PlayerInstruction.OVERLAP in instructions) {
                                    progress += .050f
                                    lateral += side * .040f
                                }
                                if (PlayerInstruction.CUT_INSIDE in instructions) lateral += (.5f - lateral) * .30f
                                if (PlayerInstruction.RUN_IN_BEHIND in instructions) progress += .055f
                            } else {
                                if (index in pressers) {
                                    val pressing = effectivePressing(isHome)
                                    val pressure = (.38f + pressing * .065f).coerceIn(.44f, .70f)
                                    lateral += (liveBallX - lateral) * pressure
                                    progress += (ballProgress - progress) * pressure
                                } else if (PlayerInstruction.TIGHT_MARKING in instructions) {
                                    lateral += (liveBallX - lateral) * .08f
                                    progress += (ballProgress - progress) * .07f
                                }

                                // Rollen beeinflussen auch die Höhe gegen den Ball.
                                when (role) {
                                    PlayerRole.ANCHOR -> progress -= .025f
                                    PlayerRole.PRESSING_FORWARD -> if (effectivePressing(isHome) >= 3) progress += .025f
                                    else -> Unit
                                }
                            }

                            // "Position halten" reduziert bewusst die individuellen Ausreißer,
                            // die Mannschaftsverschiebung als Ganzes bleibt aber erhalten.
                            if (PlayerInstruction.HOLD_POSITION in instructions) {
                                lateral = skeleton.first + (lateral - skeleton.first) * .18f
                                progress = skeleton.second + (progress - skeleton.second) * .18f
                            }

                            // Sehr kleine Laufbewegung nur als Animation. Die taktische Position
                            // kommt aus Formation/Ball/Phase und nicht aus diesem Sinus.
                            val phaseMotion = if (m.livePhase in setOf(LivePhase.COUNTER, LivePhase.ATTACK, LivePhase.DANGEROUS_ATTACK)) 1.25f else 1f
                            lateral += sin(motion + id * .79f) * .0028f * phaseMotion
                            progress += sin(motion * 1.09f + id * .43f) * .0022f * phaseMotion
                        }

                        // Verhindert unrealistische Überlappung des Torwarts/der Torlinie.
                        progress = if (keeper) progress.coerceIn(.04f, .30f) else progress.coerceIn(.07f, .975f)
                        val screenLong = screenLongitudinal(isHome, progress)
                        return lateral.coerceIn(.035f, .965f) to screenLong.coerceIn(.018f, .982f)
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
                        if (id != 0) {
                            val p = logicalPlayerPosition(true, index, id)
                            dots += DotPlayer(true, index, id, screenPoint(p.first, p.second), id == m.livePlayerId)
                        }
                    }
                    m.awayXi.forEachIndexed { index, id ->
                        if (id != 0) {
                            val p = logicalPlayerPosition(false, index, id)
                            dots += DotPlayer(false, index, id, screenPoint(p.first, p.second), id == m.livePlayerId)
                        }
                    }

                    fun drawDot(v: DotPlayer) {
                        val team = if (v.isHome) home else away
                        val primary = Color(team.primary)
                        val secondary = Color(team.secondary)
                        val isKeeper = v.index == 0
                        val baseRadius = if (isKeeper) 6.6.dp.toPx() else 6.0.dp.toPx()
                        val pulse = .96f + .04f * sin(motion * 1.7f + v.id)
                        val radius = if (v.active) 14.2.dp.toPx() * pulse else baseRadius

                        drawCircle(Color.Black.copy(alpha = .30f), radius * 1.08f, Offset(v.point.x + 1.8.dp.toPx(), v.point.y + 2.4.dp.toPx()))
                        if (v.active) {
                            drawCircle(primary.copy(alpha = .18f), radius * 1.52f, v.point)
                            drawCircle(Color.White.copy(alpha = .82f), radius * 1.20f, v.point, style = Stroke(1.2.dp.toPx()))
                        }
                        drawCircle(primary, radius, v.point)
                        drawCircle(
                            if (isKeeper) Color.White.copy(alpha = .78f) else secondary.copy(alpha = .95f),
                            radius,
                            v.point,
                            style = Stroke(if (v.active) 2.0.dp.toPx() else 1.45.dp.toPx()),
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
                "Draufsicht · alle 22 Spieler · Ballbesitzer groß · Mitspieler verschieben & laufen · Gegner pressen/komprimieren",
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

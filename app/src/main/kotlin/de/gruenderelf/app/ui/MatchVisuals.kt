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
    if (dynamicOverview) DynamicMatchOverview(w, m, frameDurationMs)
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
private fun DynamicMatchOverview(w: World, m: LiveMatch, frameDurationMs: Long) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val activeClubId = m.effectiveLiveClubId()
    val activeClub = if (activeClubId == home.id) home else away
    val actor = w.players[m.livePlayerId]
    val homeCoords = remember(m.homeFormation) { Formations.coordinates(m.homeFormation) }
    val awayCoords = remember(m.awayFormation) { Formations.coordinates(m.awayFormation) }

    val ballX = remember(m.fixtureId) { Animatable(m.ballX.coerceIn(.04f, .96f)) }
    val ballY = remember(m.fixtureId) { Animatable(m.ballY.coerceIn(.04f, .96f)) }
    val targetX = m.ballX.coerceIn(.04f, .96f)
    val targetY = m.ballY.coerceIn(.04f, .96f)
    val event = m.liveEventSerial

    LaunchedEffect(event, m.livePhase, frameDurationMs, targetX, targetY) {
        val animationMs = (frameDurationMs * .90).toInt().coerceIn(240, 3200)
        coroutineScope {
            launch { ballX.animateTo(targetX, tween(animationMs, easing = FastOutSlowInEasing)) }
            launch { ballY.animateTo(targetY, tween(animationMs, easing = FastOutSlowInEasing)) }
        }
    }

    val movement by rememberInfiniteTransition(label = "players-moving").animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "player-stride",
    )

    val actorName = actor?.name ?: activeClub.shortName
    val textMeasurer = rememberTextMeasurer()
    val actorLayout = remember(actorName) {
        textMeasurer.measure(
            AnnotatedString(actorName),
            style = TextStyle(color = Chalk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
        )
    }

    Surface(
        color = Color(0xFF071812),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamHeader(home.shortName, Color(home.primary))
                Text("${MatchEngine.clockLabel(m)}. Minute", color = Muted, style = MaterialTheme.typography.labelMedium)
                TeamHeader(away.shortName, Color(away.primary))
            }

            Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
                LivePitchImage(Modifier.matchParentSize())

                Canvas(Modifier.matchParentSize()) {
                    val ww = size.width
                    val hh = size.height
                    val liveBallX = ballX.value
                    val liveBallY = ballY.value
                    val homePrimary = Color(home.primary)
                    val homeSecondary = Color(home.secondary)
                    val awayPrimary = Color(away.primary)
                    val awaySecondary = Color(away.secondary)

                    fun point(lateral: Float, longitudinal: Float) =
                        Offset(
                            ww * engineProjectedX(lateral.coerceIn(.025f, .975f), longitudinal.coerceIn(.025f, .975f)),
                            hh * engineProjectedY(lateral.coerceIn(.025f, .975f), longitudinal.coerceIn(.025f, .975f)),
                        )

                    fun playerPosition(isHome: Boolean, index: Int, id: Int): Offset {
                        val coords = if (isHome) homeCoords else awayCoords
                        val base = coords.getOrNull(index) ?: (.5f to .5f)
                        val baseX = base.first
                        val baseY = if (isHome) 1f - base.second else base.second
                        val hasBall = if (isHome) activeClubId == home.id else activeClubId == away.id
                        val keeper = index == 0
                        val attackDir = if (isHome) 1f else -1f

                        var x = baseX
                        var y = baseY

                        if (keeper) {
                            // Keeper bleibt im eigenen Drittel, schiebt aber seitlich mit.
                            x += (liveBallX - x) * .22f
                            y += (liveBallY - y) * .035f
                        } else if (hasBall) {
                            // Ballbesitz: Formation wird breiter und schiebt geschlossen nach.
                            x += (liveBallX - x) * .16f
                            y += (liveBallY - y) * .22f
                            val runner = ((id * 37 + index * 13) % 7) / 6f
                            y += attackDir * (.012f + runner * .026f)
                            x += sin(movement + id * .71f) * (.005f + runner * .004f)
                        } else {
                            // Gegen den Ball: kompakter Block. Spieler nahe am Ball pressen stärker.
                            val dx = liveBallX - x
                            val dy = liveBallY - y
                            val distance2 = dx * dx + dy * dy
                            val press = when {
                                distance2 < .035f -> .48f
                                distance2 < .090f -> .32f
                                distance2 < .180f -> .22f
                                else -> .12f
                            }
                            x += dx * press
                            y += dy * (press * .72f)
                            // Hinter dem Ball kompakter werden statt starr auf Formation zu stehen.
                            y -= attackDir * .018f
                        }

                        if (id == m.livePlayerId) {
                            // Ballbesitzer sitzt direkt am Engine-Ball, aber mit kleinem Laufversatz.
                            x = liveBallX + sin(movement * 1.15f + id) * .0045f
                            y = liveBallY + cos(movement * 1.15f + id) * .0035f
                        } else if (!keeper) {
                            // Dauerhafte kleine Laufbewegung – verhindert die starre Brettspiel-Optik.
                            x += sin(movement + id * .83f) * .0065f
                            y += cos(movement * 1.08f + id * .47f) * .0045f
                        }

                        return point(x, y)
                    }

                    fun drawHuman(
                        position: Offset,
                        shirt: Color,
                        trim: Color,
                        active: Boolean,
                        playerId: Int,
                    ) {
                        val depth = (position.y / hh).coerceIn(0f, 1f)
                        val perspective = .78f + depth * .28f
                        val activeBoost = if (active) 1.28f else 1f
                        val scale = perspective * activeBoost
                        val torsoW = 7.8.dp.toPx() * scale
                        val torsoH = 10.5.dp.toPx() * scale
                        val headR = 3.15.dp.toPx() * scale
                        val legLen = 7.0.dp.toPx() * scale
                        val armLen = 6.0.dp.toPx() * scale
                        val stride = sin(movement * 1.65f + playerId * .74f)
                        val bob = kotlin.math.abs(sin(movement * 1.65f + playerId * .74f)) * 1.15.dp.toPx() * scale
                        val hipY = position.y - bob
                        val shoulderY = hipY - torsoH * .63f
                        val head = Offset(position.x, shoulderY - headR * 1.45f)

                        drawOval(
                            Color.Black.copy(alpha = .34f),
                            Offset(position.x - torsoW * .78f, position.y + 4.2.dp.toPx() * scale),
                            Size(torsoW * 1.56f, 4.8.dp.toPx() * scale),
                        )

                        if (active) {
                            drawCircle(shirt.copy(alpha = .18f), 17.dp.toPx() * scale, Offset(position.x, hipY - torsoH * .44f))
                            drawCircle(
                                Chalk.copy(alpha = .72f),
                                13.8.dp.toPx() * scale,
                                Offset(position.x, hipY - torsoH * .44f),
                                style = Stroke(1.3.dp.toPx()),
                            )
                        }

                        // Beine – im Gegentakt, damit die Figuren sichtbar laufen statt stehen.
                        drawLine(
                            trim,
                            Offset(position.x - torsoW * .20f, hipY + torsoH * .30f),
                            Offset(position.x - torsoW * (.20f + .16f * stride), hipY + torsoH * .30f + legLen),
                            2.15.dp.toPx() * scale,
                        )
                        drawLine(
                            trim,
                            Offset(position.x + torsoW * .20f, hipY + torsoH * .30f),
                            Offset(position.x + torsoW * (.20f + .16f * stride), hipY + torsoH * .30f + legLen),
                            2.15.dp.toPx() * scale,
                        )

                        // Arme schwingen gegen die Beine.
                        drawLine(
                            shirt,
                            Offset(position.x - torsoW * .43f, shoulderY + torsoH * .24f),
                            Offset(position.x - torsoW * .43f - armLen * .42f * stride, shoulderY + torsoH * .24f + armLen),
                            2.05.dp.toPx() * scale,
                        )
                        drawLine(
                            shirt,
                            Offset(position.x + torsoW * .43f, shoulderY + torsoH * .24f),
                            Offset(position.x + torsoW * .43f + armLen * .42f * stride, shoulderY + torsoH * .24f + armLen),
                            2.05.dp.toPx() * scale,
                        )

                        drawRoundRect(
                            shirt,
                            Offset(position.x - torsoW / 2f, shoulderY),
                            Size(torsoW, torsoH),
                            CornerRadius(2.7.dp.toPx() * scale),
                        )
                        drawRoundRect(
                            trim.copy(alpha = .90f),
                            Offset(position.x - torsoW / 2f, shoulderY + torsoH * .56f),
                            Size(torsoW, torsoH * .18f),
                            CornerRadius(1.4.dp.toPx() * scale),
                        )
                        drawLine(
                            Chalk.copy(alpha = .50f),
                            Offset(position.x, shoulderY + torsoH * .08f),
                            Offset(position.x, shoulderY + torsoH * .48f),
                            .75.dp.toPx() * scale,
                        )
                        drawCircle(Color(0xFFE6C7AA), headR, head)
                        drawCircle(
                            Color.White.copy(alpha = .55f),
                            headR,
                            head,
                            style = Stroke(.65.dp.toPx()),
                        )
                    }

                    data class VisualPlayer(
                        val isHome: Boolean,
                        val index: Int,
                        val id: Int,
                        val position: Offset,
                        val active: Boolean,
                    )

                    val visuals = mutableListOf<VisualPlayer>()
                    m.homeXi.forEachIndexed { index, id ->
                        if (id != 0) visuals += VisualPlayer(true, index, id, playerPosition(true, index, id), id == m.livePlayerId)
                    }
                    m.awayXi.forEachIndexed { index, id ->
                        if (id != 0) visuals += VisualPlayer(false, index, id, playerPosition(false, index, id), id == m.livePlayerId)
                    }

                    // Nach Tiefe sortieren: weiter oben zuerst, Spieler unten überdecken natürlich.
                    visuals.filter { !it.active }.sortedBy { it.position.y }.forEach { vp ->
                        drawHuman(
                            vp.position,
                            if (vp.isHome) homePrimary else awayPrimary,
                            if (vp.isHome) homeSecondary else awaySecondary,
                            false,
                            vp.id,
                        )
                    }
                    visuals.filter { it.active }.forEach { vp ->
                        drawHuman(
                            vp.position,
                            if (vp.isHome) homePrimary else awayPrimary,
                            if (vp.isHome) homeSecondary else awaySecondary,
                            true,
                            vp.id,
                        )
                    }

                    val ballPoint = point(liveBallX, liveBallY)
                    val ball = Offset(ballPoint.x + 5.0.dp.toPx(), ballPoint.y + 7.0.dp.toPx())
                    drawOval(
                        Color.Black.copy(alpha = .38f),
                        Offset(ball.x - 6.0.dp.toPx(), ball.y + 3.5.dp.toPx()),
                        Size(12.dp.toPx(), 3.8.dp.toPx()),
                    )
                    drawCircle(Color.White, 5.2.dp.toPx(), ball)
                    drawCircle(Color(0xFF1D1D1D), 5.2.dp.toPx(), ball, style = Stroke(1.0.dp.toPx()))
                    drawCircle(Color(0xFF252525), 1.55.dp.toPx(), Offset(ball.x - .8.dp.toPx(), ball.y - .8.dp.toPx()))

                    val activeVisual = visuals.firstOrNull { it.active }
                    if (activeVisual != null) {
                        val padX = 6.dp.toPx()
                        val padY = 2.dp.toPx()
                        val labelW = actorLayout.size.width + padX * 2
                        val labelH = actorLayout.size.height + padY * 2
                        val left = (activeVisual.position.x - labelW / 2f)
                            .coerceIn(4.dp.toPx(), (ww - labelW - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                        val top = (activeVisual.position.y - 49.dp.toPx())
                            .coerceIn(4.dp.toPx(), (hh - labelH - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                        drawRoundRect(
                            Color.Black.copy(alpha = .74f),
                            Offset(left, top),
                            Size(labelW, labelH),
                            CornerRadius(6.dp.toPx()),
                        )
                        drawText(actorLayout, topLeft = Offset(left + padX, top + padY))
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 7.dp)
                        .background(Color.Black.copy(alpha = .66f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        phaseText(m),
                        color = if (m.livePhase == LivePhase.GOAL) Grass else Chalk,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${activeClub.shortName} · $actorName",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            Text(
                "Gesamtfeld · dynamische Formation · laufende Spieler · Ballbesitzer im Fokus",
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

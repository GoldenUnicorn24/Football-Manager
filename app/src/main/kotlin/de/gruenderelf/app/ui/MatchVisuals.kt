package de.gruenderelf.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gruenderelf.app.R
import de.gruenderelf.engine.LiveMatch
import de.gruenderelf.engine.MatchEngine
import de.gruenderelf.engine.LivePhase
import de.gruenderelf.engine.ShotContext
import de.gruenderelf.engine.ShotModel
import de.gruenderelf.engine.World
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
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
fun LiveMatchAnimation(w: World, m: LiveMatch) {
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
                LivePhase.SHOT_OFF_TARGET, LivePhase.SHOT_ON_TARGET, LivePhase.WOODWORK, LivePhase.GOAL -> ShotScene(w, m, activeId, actor?.name ?: active.shortName)
                LivePhase.VAR -> VarReviewScene(w, m, activeId, actor?.name ?: active.shortName)
                LivePhase.SHOOTOUT -> ShotScene(w,m,activeId,actor?.name?:active.shortName)
                LivePhase.PENALTY -> OpenPlayScene(m,home.shortName,away.shortName,activeId,Color(home.primary),Color(away.primary))
                LivePhase.CORNER -> CornerScene(w, m, activeId, actor?.name ?: active.shortName)
                LivePhase.YELLOW_CARD, LivePhase.YELLOW_RED_CARD, LivePhase.RED_CARD, LivePhase.INJURY -> IncidentScene(w, m, activeId)
                else -> OpenPlayScene(
                    m = m,
                    homeShort = home.shortName,
                    awayShort = away.shortName,
                    activeId = activeId,
                    homeColor = Color(home.primary),
                    awayColor = Color(away.primary),
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
) {
    val activeColor = if (activeId == m.homeId) homeColor else awayColor
    // Animiert wird direkt im Engine-Koordinatensystem. So benutzt die offene Szene
    // exakt dieselbe Position wie die nachfolgende Schussanimation.
    val ballX = remember(m.fixtureId) { Animatable(m.ballX.coerceIn(.04f, .96f)) }
    val ballY = remember(m.fixtureId) { Animatable(m.ballY.coerceIn(.04f, .96f)) }
    val trail = remember(m.fixtureId) { mutableStateListOf<Pair<Float, Float>>() }
    val event = m.liveEventSerial

    LaunchedEffect(event, m.livePhase, activeId) {
        val tx = m.ballX.coerceIn(.04f, .96f)
        val ty = m.ballY.coerceIn(.04f, .96f)
        val sx = ballX.value
        val sy = ballY.value
        if (trail.isEmpty()) trail.add(sx to sy)
        val dist = max(abs(tx - sx), abs(ty - sy))
        val pieces = ceil(dist / .115f).toInt().coerceIn(1, 6)
        val specialPass = m.liveDetail.contains("Langer") || m.liveDetail.contains("Seitenwechsel") || m.liveDetail.contains("Steilpass") || m.liveDetail.contains("Torwart")
        val totalMs = when {
            specialPass -> 1050
            m.livePhase == LivePhase.COUNTER -> 430
            m.livePhase == LivePhase.DANGEROUS_ATTACK -> 720
            m.livePhase == LivePhase.ATTACK -> 820
            else -> 900
        }
        for (i in 1..pieces) {
            val f = i / pieces.toFloat()
            val nx = sx + (tx - sx) * f
            val ny = sy + (ty - sy) * f
            coroutineScope {
                launch { ballX.animateTo(nx, tween((totalMs / pieces).coerceAtLeast(90))) }
                launch { ballY.animateTo(ny, tween((totalMs / pieces).coerceAtLeast(90))) }
            }
            trail.add(nx to ny)
            while (trail.size > 5) trail.removeAt(0)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
        LivePitchImage(Modifier.matchParentSize())
        Canvas(Modifier.matchParentSize()) {
            val ww = size.width
            val hh = size.height
            fun p(x: Float, y: Float) = Offset(ww * engineProjectedX(x, y), hh * engineProjectedY(x, y))

            val points = trail.takeLast(5).map { p(it.first, it.second) } + p(ballX.value, ballY.value)
            for (i in 1 until points.size) {
                val alpha = (.08f + .105f * i).coerceAtMost(.56f)
                drawLine(activeColor.copy(alpha = alpha), points[i - 1], points[i], (1f + i * .16f).dp.toPx())
            }
            val ball = p(ballX.value, ballY.value)
            drawOval(Color.Black.copy(alpha = .34f), Offset(ball.x - 8.dp.toPx(), ball.y + 6.dp.toPx()), Size(16.dp.toPx(), 5.dp.toPx()))
            drawCircle(Color.White, 7.5.dp.toPx(), ball)
            drawCircle(Color(0xFF1C1C1C), 7.5.dp.toPx(), ball, style = Stroke(1.1.dp.toPx()))
            drawCircle(Color(0xFF252525), 2.1.dp.toPx(), Offset(ball.x - 1.dp.toPx(), ball.y - 1.dp.toPx()))
        }

        val sx = engineProjectedX(ballX.value, ballY.value).coerceIn(.06f, .94f)
        val sy = engineProjectedY(ballX.value, ballY.value).coerceIn(.17f, .90f)
        val club = if (activeId == m.homeId) homeShort else awayShort
        Text(
            club,
            color = Chalk,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.offset(x = maxWidth * sx - 42.dp, y = maxHeight * sy - 42.dp).width(84.dp).background(Color.Black.copy(alpha = .52f), RoundedCornerShape(5.dp)).padding(vertical = 2.dp),
        )
        if (m.livePhase in setOf(LivePhase.FREE_KICK, LivePhase.DANGEROUS_FREE_KICK, LivePhase.PENALTY)) {
            Text(
                m.livePhase.label.uppercase(),
                color = Chalk,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color.Black.copy(alpha = .66f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(
            m.liveDetail.takeIf { it.isNotBlank() } ?: m.livePhase.label,
            color = Chalk,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.offset(x = maxWidth * sx - 72.dp, y = maxHeight * sy + 15.dp).width(144.dp).background(Color.Black.copy(alpha = .46f), RoundedCornerShape(5.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ShotScene(w: World, m: LiveMatch, activeId: Int, shooterName: String) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val progress = remember(m.fixtureId, m.liveEventSerial) { Animatable(0f) }
    val event = m.liveEventSerial

    LaunchedEffect(event, m.livePhase) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(when (m.livePhase) {
            LivePhase.GOAL -> 1080
            LivePhase.SHOT_ON_TARGET -> 980
            else -> 880
        }))
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
private fun CornerScene(w: World, m: LiveMatch, activeId: Int, takerName: String) {
    val home = w.clubs.getValue(m.homeId)
    val away = w.clubs.getValue(m.awayId)
    val club = if (activeId == home.id) home else away
    val goalRight = activeId == home.id
    val progress = remember(m.fixtureId, m.liveEventSerial) { Animatable(0f) }
    val lowerCorner = m.ballX > .5f
    LaunchedEffect(m.liveEventSerial) { progress.snapTo(0f); progress.animateTo(1f, tween(1900)) }
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

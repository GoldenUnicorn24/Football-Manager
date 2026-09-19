package de.gruenderelf.app.ui

import de.gruenderelf.engine.BuildUp
import de.gruenderelf.engine.Tactics

val tacticPresets = listOf("Individuell", "Ausgewogen", "Tiki-Taka", "Ballbesitz", "Direkt", "Konter", "Über Außen", "Gegenpressing")

fun applyTacticPreset(t: Tactics, preset: String) {
    when (preset) {
        "Ausgewogen" -> { t.mentality=3; t.pressing=3; t.line=3; t.tempo=3; t.width=3; t.buildUp=BuildUp.MIXED }
        "Tiki-Taka" -> { t.mentality=3; t.pressing=4; t.line=4; t.tempo=3; t.width=4; t.buildUp=BuildUp.TIKI_TAKA }
        "Ballbesitz" -> { t.mentality=3; t.pressing=3; t.line=3; t.tempo=2; t.width=4; t.buildUp=BuildUp.SHORT }
        "Direkt" -> { t.mentality=4; t.pressing=3; t.line=3; t.tempo=4; t.width=3; t.buildUp=BuildUp.DIRECT }
        "Konter" -> { t.mentality=2; t.pressing=2; t.line=2; t.tempo=4; t.width=4; t.buildUp=BuildUp.COUNTER }
        "Über Außen" -> { t.mentality=3; t.pressing=3; t.line=3; t.tempo=4; t.width=5; t.buildUp=BuildUp.WIDE }
        "Gegenpressing" -> { t.mentality=4; t.pressing=5; t.line=4; t.tempo=5; t.width=3; t.buildUp=BuildUp.MIXED }
    }
}

fun tacticSummary(t: Tactics): String {
    val build = when (t.buildUp) {
        BuildUp.TIKI_TAKA -> "viele kurze Kombinationen, Seitenwechsel und bewusste Rückpässe"
        BuildUp.SHORT -> "sicherer Kurzpassaufbau mit längeren Ballbesitzphasen"
        BuildUp.MIXED -> "variabler Aufbau mit kurzen und längeren Pässen"
        BuildUp.WIDE -> "frühe Verlagerungen auf die Außenbahnen und mehr Flügelangriffe"
        BuildUp.DIRECT -> "mehr vertikale und lange Bälle, dafür höheres Ballverlustrisiko"
        BuildUp.COUNTER -> "tieferer Aufbau und schnelle Vertikalpässe nach Ballgewinnen"
    }
    val press = when (t.pressing) { 1 -> "sehr passiv"; 2 -> "zurückhaltend"; 3 -> "ausgewogen"; 4 -> "aggressiv"; else -> "maximal aggressiv" }
    val line = when (t.line) { 1 -> "sehr tief"; 2 -> "tief"; 3 -> "mittelhoch"; 4 -> "hoch"; else -> "sehr hoch" }
    return "Spielidee: $build. Pressing $press, Defensivlinie $line. Hohe Werte bei Tempo, Pressing und Linie erhöhen Wirkung, Risiko und Fitnessverbrauch spürbar."
}

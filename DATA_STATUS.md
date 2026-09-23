# Gründerelf 0.5.20 – Datenstand

Der Code baut auf `recover-v0.5.18` auf und übernimmt die bereits begonnenen
Liga- und Pokaldaten aus `feature/multi-country-leagues-cups-v0518`.

- Die fünf ursprünglichen ersten Ligen enthalten den vorhandenen 2026/27-Kaderstand.
  Einzelne Zuordnungen daraus wurden nicht unabhängig bestätigt; Ratings sind Spielwerte.
- Weitere Ligen enthalten reale Vereinsnamen, aber derzeit simulierte Spieler. Diese
  Spieler werden im Profil als simuliert gekennzeichnet. Untere Ligen sind spielbare
  Auswahlgruppen und bilden nicht in jedem Fall die vollständige echte Ligabesetzung ab.
- Kroatiens HNL und Russlands Premjer-Liga wurden mit den im September 2026 gelisteten
  Vereinen ergänzt. Auf- und Abstiege nach dem Start entstehen aus dem Spielverlauf.
- Zusätzlich sind Schweden, Norwegen, Ukraine und Rumänien mit je drei spielbaren
  Ebenen enthalten. Die schwedischen Erst- und Zweitligisten entsprechen dem
  Verbandsplan 2026, ebenso die norwegischen Erst- und Zweitligisten dem Stand
  September 2026. Andere Unterklassen bilden eine Spielauswahl aus echten Clubs.
- Insgesamt sind 23 Länder enthalten, noch nicht alle europäischen Verbände.
- Für alle 16 ukrainischen Erstligisten sind zusammen 342 namentlich geführte
  Spieler aus den aktuellen Registrierungslisten der ukrainischen Premier League
  aufgenommen. Positionen sind auf vier Spielpositionen vereinfacht, Ratings
  bleiben Schätzwerte. Wo die Registrierungsliste weniger als 20 aktive Namen
  ausweist, werden die übrigen Plätze sichtbar als simuliert ergänzt.
- Champions League und Europa League: 36 Clubs und jeweils acht Ligaphasenspiele.
  Die erste Saison nutzt die Startsetzung; ab Saison zwei zählen die Ergebnisse der
  Vorjahrestabellen. Russische Vereine spielen national, sind aber vom UEFA-Pool ausgenommen.
- Club World Cup: spielbares 32er-Feld mit acht Vierergruppen, Achtelfinale,
  Viertelfinale, Halbfinale und Finale. 16 europäische Clubs und 16 Clubs anderer
  Kontinente starten. Das ist eine vereinfachte Spielregel, keine offizielle
  FIFA-Qualifikation oder verbindliche Turnierliste.
- Krone der Kontinente: erste Saison nach Vereinsstärke gesetzt, anschließend je
  Spielklasse Platz 1–6 der Vorsaison. Der Schalter im Trophäenschrank wirkt ab der
  nächsten Saison. Die Preisgelder sind Spielwerte und keine offiziellen Auszahlungen.
- Pokale und Trophäen nutzen lokal gezeichnete Vektorgrafiken und vorhandene
  Vereinswappen. Es sind keine echten Spielerfotos oder lizenzierten Vereinslogos enthalten.

Zum Erzeugen einer APK mit Android SDK 36 und JDK 17:

    ./gradlew test :app:lintDebug :app:assembleDebug

Die Test- und APK-Erzeugung konnte in dieser Arbeitsumgebung mangels erreichbarer
Gradle-Distribution und Android SDK nicht ausgeführt werden.

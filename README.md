# Budget – Android-App

Einfaches monatliches Ausgaben-Tracking (Kotlin + Jetpack Compose). Daten bleiben lokal auf dem Handy.

## Einrichtung

1. **Android Studio installieren** (kostenlos, basiert auf IntelliJ): https://developer.android.com/studio
   Der Installer bringt alles mit: Android SDK, passendes Java (JDK) und den Emulator.
2. Android Studio starten → **Open** → diesen Projektordner wählen.
   Beim ersten Öffnen lädt Gradle die Abhängigkeiten (dauert ein paar Minuten).

## Zwei Varianten der App

| | Release („Budget“) | Debug („Budget Debug“) |
|---|---|---|
| App-ID | `de.meinbudget.app` | `de.meinbudget.app.debug` |
| Wofür | Alltag, echte Daten | Entwickeln und Ausprobieren |
| Installation | APK-Datei aufs Handy | ▶ in Android Studio |

Beide können gleichzeitig installiert sein und haben **getrennte Daten**. Ein Debug-Build aus Android Studio
kann deine echten Daten also nie überschreiben oder löschen.

## Release-APK bauen und installieren

```bash
./gradlew assembleRelease
```

Ergebnis: `app/build/outputs/apk/release/app-release.apk`

Aufs Handy bringen (Kabel, Cloud, Mail an dich selbst), dort antippen. Beim ersten Mal fragt Android,
ob die Datei-App „unbekannte Apps installieren“ darf → erlauben.

**Updates:** Neue Version bauen und die APK einfach drüber installieren – die Daten bleiben erhalten.
Vorher in `app/build.gradle.kts` `versionCode` um 1 erhöhen (und `versionName` anpassen).

## Signierschlüssel – unbedingt sichern!

Der Release-Build wird mit einem eigenen Schlüssel signiert, der **außerhalb des Repos** liegt:

```
~/.easybudget/easybudget-release.jks      ← der Schlüssel
~/.easybudget/keystore.properties          ← Pfad und Passwörter dazu
```

Android installiert Updates nur, wenn sie mit **demselben Schlüssel** signiert sind. Geht er verloren,
lässt sich die App nur noch durch Deinstallieren ersetzen – und dabei gehen alle Budgetdaten verloren.
→ Beide Dateien in ein Backup (z. B. Passwortmanager oder verschlüsselter USB-Stick), **nie** ins Git-Repo.

Anderer Ablageort: `./gradlew assembleRelease -Peasybudget.keystore=/pfad/zu/keystore.properties`.
Fehlt die Datei, wird die Release-APK unsigniert gebaut (und lässt sich nicht installieren).

## Bedienung

- **+** in der Monatsliste legt einen neuen Monat an. Vorgeschlagen wird der Folgemonat des neuesten Monats,
  Einkommen und Fixkosten werden übernommen, der Anfangssaldo wird mit dem Restbudget des Vormonats vorbelegt.
- **Budget** = Anfangssaldo + Einkommen − fixe Ausgaben. **Verfügbar** = Budget + alle Buchungen.
- Die Eingabezeile ist immer unten sichtbar: Beschreibung, **−/+** (Ausgabe/Einnahme), Betrag, Senden.
- Buchung **nach rechts wischen** → Löschen (mit „Rückgängig“), **nach links wischen** → Bearbeiten.
- Stift-Symbol (oder Budget-Karte antippen) → Basiswerte ändern. ⋮ → Monat löschen.
- **Zahnrad** → Einstellungen: Design, Sprache, Wallos-Integration.

## Wallos-Integration

In den Einstellungen URL und API-Key des eigenen Wallos-Servers eintragen. Beim Anlegen eines Monats lädt
„Lade aus Wallos“ die Abo-Kosten des Monats als fixe Ausgaben. Der API-Key wird mit dem Android-Keystore
verschlüsselt gespeichert und nicht mit ins Backup übernommen.

## Aufbau

| Datei | Inhalt |
|---|---|
| `Model.kt` | `Month` und `Entry`, Berechnungen (Budget, Rest) |
| `Money.kt` | Beträge parsen/formatieren (deutsches Format, Cent-genau) |
| `BudgetRepository.kt` | Speichert alles als JSON in `budget.json` im App-Speicher |
| `BudgetViewModel.kt` | Aktionen (Monat anlegen, Buchung hinzufügen, …) |
| `Settings.kt` / `SecretStore.kt` | Einstellungen; verschlüsselte Ablage des API-Keys |
| `WallosClient.kt` | Abruf der Monatskosten von Wallos |
| `ui/MonthListScreen.kt` | Monatsübersicht |
| `ui/MonthScreen.kt` | Monatsansicht mit Budget-Karte, Liste, Eingabezeile |
| `ui/SwipeableEntryRow.kt` | Buchungszeile mit Wisch-Aktionen |
| `ui/SettingsScreen.kt` | Einstellungen |
| `ui/Dialogs.kt` | Dialoge für Basiswerte, Buchung bearbeiten, Löschen bestätigen |

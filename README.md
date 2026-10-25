# Budget – Android-App

Einfaches monatliches Ausgaben-Tracking (Kotlin + Jetpack Compose). Daten bleiben lokal auf dem Handy.

## Einrichtung

1. **Android Studio installieren** (kostenlos, basiert auf IntelliJ): https://developer.android.com/studio
   Der Installer bringt alles mit: Android SDK, passendes Java (JDK) und den Emulator.
   (IntelliJ geht prinzipiell auch, aber dort musst du SDK und JDK selbst einrichten.)
2. Android Studio starten → **Open** → diesen Projektordner wählen.
   Beim ersten Öffnen lädt Gradle die Abhängigkeiten (dauert ein paar Minuten).
   Falls Android Studio ein Update der Gradle- oder AGP-Version anbietet, kannst du das annehmen.
3. **Auf dem Handy starten:**
   - Am Handy: Einstellungen → Über das Telefon → 7× auf „Build-Nummer“ tippen → Entwickleroptionen → **USB-Debugging** an.
   - Handy per USB anschließen, in Android Studio oben das Gerät auswählen → grüner ▶-Button.
   - Alternativ ohne Kabel: Entwickleroptionen → „Kabelloses Debugging“ und in Android Studio „Pair Devices Using Wi-Fi“.
   - Oder im Emulator: Device Manager → virtuelles Gerät anlegen → ▶.

Die App bleibt danach normal auf dem Handy installiert.

## Bedienung

- **+** in der Monatsliste legt einen neuen Monat an. Vorgeschlagen wird der Folgemonat des neuesten Monats,
  Einkommen und Fixkosten werden übernommen, der Anfangssaldo wird mit dem Restbudget des Vormonats vorbelegt.
- **Budget** = Anfangssaldo + Einkommen − fixe Ausgaben.
- **Verfügbar** = Budget + alle Buchungen. Steht groß oben.
- Die Eingabezeile ist immer unten sichtbar: Beschreibung, **−/+** (Ausgabe/Einnahme), Betrag, Senden.
- Buchung antippen → bearbeiten oder löschen.
- Stift-Symbol (oder Budget-Karte antippen) → Basiswerte ändern. ⋮ → Monat löschen.

## Aufbau

| Datei | Inhalt |
|---|---|
| `Model.kt` | `Month` und `Entry`, Berechnungen (Budget, Rest) |
| `Money.kt` | Beträge parsen/formatieren (deutsches Format, Cent-genau) |
| `BudgetRepository.kt` | Speichert alles als JSON in `budget.json` im App-Speicher |
| `BudgetViewModel.kt` | Aktionen (Monat anlegen, Buchung hinzufügen, …) |
| `ui/MonthListScreen.kt` | Monatsübersicht |
| `ui/MonthScreen.kt` | Monatsansicht mit Budget-Karte, Liste, Eingabezeile |
| `ui/Dialogs.kt` | Dialoge für Basiswerte, Buchung bearbeiten, Löschen bestätigen |

Hinweis: Wird die App deinstalliert, sind die Daten weg (außer über Androids automatisches Backup).

# Release-Readiness Plan - Shopping List App

**Datum:** 11. Januar 2026
**Status:** In Umsetzung
**Ziel:** Vorbereitung der App für den offiziellen Release (Google Play Store).

---

## 1. Rechtliche Anforderungen (Erledigt ✅)

Um die App rechtlich abzusichern, wurden Impressum und Datenschutzrichtlinie integriert.

### Schritte:
- [x] **Content Erstellung:** Texte für Impressum und Datenschutz (DSGVO-konform) erstellt.
- [x] **UI Integration:** Kompakte Fußzeile am Ende der `ProfileActivity` hinzugefügt.
- [x] **Logik:** Nativer Dialog mit scrollbarer TextView und anklickbaren Links implementiert.

---

## 2. Technische Release-Vorbereitung

### A. Obfuscation & Optimierung (Erledigt ✅)
- [x] **Minification:** `isMinifyEnabled = true` und `isShrinkResources = true` aktiviert.
- [x] **Regeln:** ProGuard-Regeln für Firebase, Glide und Datenmodelle in `app/proguard-rules.pro` konfiguriert.
- [x] **Signing:** Release-Signing-Prozess mit lokalem Keystore verifiziert.

### B. Fehlerüberwachung & Stabilität (Erledigt ✅)
- [x] **Firebase Crashlytics:** SDK und Gradle-Plugin erfolgreich eingebunden.
- [x] **Logging:** ProGuard-Regel zum automatischen Entfernen von `Log.d` und `Log.v` im Release-Build hinzugefügt.

### C. App Check & Security
- [ ] **Enforcement:** In der Firebase Console auf "Enforcement" umstellen (nach erstem Store-Upload).
- [x] **Keys:** `local.properties` sicher konfiguriert (wird nicht committet).

---

## 3. Store-Präsenz & Assets
- [x] **Launcher Icon:** Adaptive Icons und alle Auflösungen (mdpi bis xxxhdpi) verifiziert.
- [ ] **Screenshots:** Erstellen für Phone und Tablets.
- [ ] **Store-Texte:** Beschreibungen in DE/EN finalisieren.

---

## Checkliste für den nächsten Schritt:
1. [ ] App mit Crashlytics auf einem physischen Gerät testen.
2. [ ] Google Play Store Listing (Screenshots, Beschreibungen) vorbereiten.

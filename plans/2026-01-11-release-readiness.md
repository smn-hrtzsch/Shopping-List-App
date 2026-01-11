# Release-Readiness Plan - Shopping List App

**Datum:** 11. Januar 2026
**Status:** In Planung
**Ziel:** Vorbereitung der App für den offiziellen Release (Google Play Store / App Store).

---

## 1. Rechtliche Anforderungen (Fokus-Anfrage)

Um die App rechtlich abzusichern, müssen Impressum und Datenschutzrichtlinie integriert werden.

### Betroffene Dateien:
- `app/src/main/res/values/strings.xml` (und `values-de/strings.xml`)
- `app/src/main/res/layout/activity_profile.xml` (Hinzufügen der Links)
- `app/src/main/java/com/CapyCode/ShoppingList/ProfileActivity.java` (Logik zum Öffnen der Links)
- `app/src/main/res/layout/dialog_legal.xml` (Natives Styling)

### Schritte:
- [x] **Content Erstellung:** Texte für Impressum und Datenschutz (DSGVO-konform) erstellt und direkt in `strings.xml` integriert.
- [x] **UI Integration:** Kompakte Fußzeile am Ende der `ProfileActivity` hinzugefügt.
- [x] **Logik:** Nativer Dialog mit scrollbarer TextView und anklickbaren Links implementiert.

---

## 2. Technische Release-Vorbereitung (Was fehlt noch?)

Basierend auf der Code-Analyse sind folgende Punkte für einen professionellen Release notwendig:

### A. Obfuscation & Optimierung (ProGuard/R8)
Aktuell ist `isMinifyEnabled = true` in `app/build.gradle.kts`.
- [x] **Aktion:** Auf `true` gesetzt für den Release-Build.
- [x] **Test:** Regeln für Firebase, Glide und Datenmodelle in `proguard-rules.pro` ergänzt und erfolgreich via signierten Release-Build verifiziert.

### B. App Check Enforcements
Die App nutzt bereits App Check (Debug/Play Integrity).
- [ ] **Aktion:** In der Firebase Console sicherstellen, dass "Enforcement" für Firestore und Storage aktiviert wird, sobald die App im Store ist.

### C. Versionierung
- [ ] **Aktion:** `versionCode` und `versionName` in `app/build.gradle.kts` finalisieren.
- [ ] **Automatisierung:** Das vorhandene `scripts/bump_version.py` auf Korrektheit prüfen.

### D. Asset-Bereinigung & Icons
- [ ] **Aktion:** Alle `TODO` Kommentare im Code prüfen.
- [ ] **Icons:** Sicherstellen, dass das `@mipmap/ic_launcher` in allen Auflösungen (HDPI bis XXXHDPI) professionell aussieht und ein adaptives Icon vorhanden ist.
- [ ] **Unused Resources:** Unnötige Drawables oder Layouts entfernen, um die APK-Größe zu minimieren.

### E. Error Handling & Crashlytics
- [ ] **Firebase Crashlytics:** Falls noch nicht geschehen, Crashlytics einbinden, um Fehler bei echten Nutzern zu tracken.
- [ ] **Analytics:** Prüfen, ob die Analytics-Events für den Release ausreichen.

---

## 3. Risiken & Edge-Cases
- **DSGVO:** Da Nutzerprofile und E-Mail-Adressen gespeichert werden, muss die Datenschutzerklärung explizit Firebase (Auth, Firestore, Analytics) erwähnen.
- **Signing Key:** Der Release-Key (Keystore) muss sicher verwahrt werden. Ein Verlust verhindert Updates im Play Store.
- **Offline-Sync:** Da die App lokal puffert, muss sichergestellt werden, dass bei einem Account-Wechsel keine Datenreste des vorherigen Nutzers sichtbar bleiben (Cleanup-Logik prüfen).

---

## Checkliste für den nächsten Schritt:
1. [ ] Impressum/Datenschutz-Texte bereitstellen.
2. [ ] `isMinifyEnabled` im Release-Zweig testen.
3. [ ] Google Play Store Listing (Screenshots, Beschreibungen) vorbereiten.

# Test-Szenarien für Authentifizierung & Registrierung

## 1. Registrierung (E-Mail & Passwort)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| R1 | Registrierung mit gültigen Daten (neue E-Mail) | Bestätigungs-Dialog öffnet sich. E-Mail wird gesendet. | Bereit (Manuell) |
| R2 | Registrierung mit bereits vorhandener E-Mail | Fehlermeldung: "Ein Konto mit dieser E-Mail existiert bereits." | Erledigt (Auto) |
| R3 | Registrierung mit ungültigem E-Mail Format (z.B. "test@") | Fehlermeldung: "Ungültige E-Mail Adresse." (oder Input-Fehler) | Erledigt (Auto) |
| R4 | Registrierung mit zu kurzem Passwort (< 6 Zeichen) | Fehlermeldung: "Passwort muss mindestens 6 Zeichen lang sein." | Erledigt (Auto) |
| R5 | Abbrechen im Bestätigungs-Dialog | Dialog schließt sich. User wird NICHT erstellt/gelöscht (Rollback). | Bereit (Manuell) |
| R6 | Bestätigen klicken OHNE Link in Mail geklickt zu haben | Toast/Info: "E-Mail noch nicht bestätigt." | Bereit (Manuell) |

## 2. Anmeldung (E-Mail & Passwort)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| L1 | Login mit korrekten Daten (verifiziert) | Erfolgreicher Login -> Weiterleitung zum Profil/Liste. | Bereit (Manuell) |
| L2 | Login mit falschem Passwort | Fehlermeldung: "Das eingegebene Passwort ist nicht korrekt." | Erledigt (Auto) |
| L3 | Login mit unbekannter E-Mail | Fehlermeldung: "Noch kein Konto mit dieser E-Mail vorhanden..." | Erledigt (Auto) |
| L4 | Login mit korrekten Daten, aber E-Mail NICHT verifiziert | (Optional: Blockiert oder Hinweis, je nach Implementierung) | Bereit (Manuell) |

## 3. Passwort Vergessen

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| P1 | Reset für existierende E-Mail | Toast: "E-Mail gesendet". Mail kommt an. | Bereit (Manuell) |
| P2 | Reset für nicht existierende E-Mail | Fehlermeldung: "Kein Konto mit dieser E-Mail gefunden." | Bereit (Manuell) |
| P3 | Reset für Google-Account E-Mail | Dialog/Info: "Bitte über Google anmelden." | Bereit (Manuell) |

## 4. Gast-User Verknüpfung (Account Linking)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| A1 | Als Gast User -> Registrieren (neue Mail) | Account wird konvertiert. Daten (Listen) bleiben erhalten. | Bereit (Manuell) |
| A2 | Als Gast User -> Anmelden (existierender Account) | Warn-Dialog ("Daten werden überschrieben"). Bei OK -> Login & Datenwechsel. | Bereit (Manuell) |

## 5. Google Sign-In

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| G1 | Erstmaliger Login mit Google | Account wird erstellt. Login erfolgreich. | Bereit (Manuell) |
| G2 | Login mit Google (Account existiert schon) | Login erfolgreich. | Bereit (Manuell) |
| G3 | Google Login abbrechen (Back Button im Google Screen) | Keine Fehlermeldung, User bleibt auf Auth Screen. | Bereit (Manuell) |
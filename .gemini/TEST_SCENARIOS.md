# Test-Szenarien für Authentifizierung & Registrierung

## 1. Registrierung (E-Mail & Passwort)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| R1 | Registrierung mit gültigen Daten (neue E-Mail) | Bestätigungs-Dialog öffnet sich. E-Mail wird gesendet. | Offen |
| R2 | Registrierung mit bereits vorhandener E-Mail | Fehlermeldung: "Ein Konto mit dieser E-Mail existiert bereits." | Offen |
| R3 | Registrierung mit ungültigem E-Mail Format (z.B. "test@") | Fehlermeldung: "Ungültige E-Mail Adresse." (oder Input-Fehler) | Erledigt |
| R4 | Registrierung mit zu kurzem Passwort (< 6 Zeichen) | Fehlermeldung: "Passwort muss mindestens 6 Zeichen lang sein." | Erledigt |
| R5 | Abbrechen im Bestätigungs-Dialog | Dialog schließt sich. User wird NICHT erstellt/gelöscht (Rollback). | Offen |
| R6 | Bestätigen klicken OHNE Link in Mail geklickt zu haben | Toast/Info: "E-Mail noch nicht bestätigt." | Offen |

## 2. Anmeldung (E-Mail & Passwort)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| L1 | Login mit korrekten Daten (verifiziert) | Erfolgreicher Login -> Weiterleitung zum Profil/Liste. | Offen |
| L2 | Login mit falschem Passwort | Fehlermeldung: "Ungültige E-Mail oder falsches Passwort." | Offen |
| L3 | Login mit unbekannter E-Mail | Fehlermeldung: "Konto existiert nicht..." | Offen |
| L4 | Login mit korrekten Daten, aber E-Mail NICHT verifiziert | (Optional: Blockiert oder Hinweis, je nach Implementierung) | Offen |

## 3. Passwort Vergessen

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| P1 | Reset für existierende E-Mail | Toast: "E-Mail gesendet". Mail kommt an. | Offen |
| P2 | Reset für nicht existierende E-Mail | Fehlermeldung: "Kein Konto mit dieser E-Mail gefunden." | Offen |
| P3 | Reset für Google-Account E-Mail | Dialog/Info: "Bitte über Google anmelden." | Offen |

## 4. Gast-User Verknüpfung (Account Linking)

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| A1 | Als Gast User -> Registrieren (neue Mail) | Account wird konvertiert. Daten (Listen) bleiben erhalten. | Offen |
| A2 | Als Gast User -> Anmelden (existierender Account) | Warn-Dialog ("Daten werden überschrieben"). Bei OK -> Login & Datenwechsel. | Offen |

## 5. Google Sign-In

| ID | Szenario | Erwartetes Ergebnis | Status |
|----|----------|---------------------|--------|
| G1 | Erstmaliger Login mit Google | Account wird erstellt. Login erfolgreich. | Offen |
| G2 | Login mit Google (Account existiert schon) | Login erfolgreich. | Offen |
| G3 | Google Login abbrechen (Back Button im Google Screen) | Keine Fehlermeldung, User bleibt auf Auth Screen. | Offen |

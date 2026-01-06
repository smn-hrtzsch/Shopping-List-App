# To-Dos Shopping List App

## Allgemein

- [ ] Impressum und Datenschutzrichtlinie erstellen und in der App verlinken
- [ ] Ladebildschirm / Splash Screen beim Laden von Daten, wenn UI noch nicht ready oder während längerer Ladezeiten (Anmeldung, Account Wechsel, etc.) Momentan wird manchmal ein Lade Kreis angezeigt, der aber dann über die UI gelegt wird, was nicht so toll aussieht.
- [ ] Hint in EditText Dialog für den Nutzernamen in Profil Activity hinzufügen, z.B. "Nutzernamen eingeben"
- [ ] Reihenfolge der verknüpften Anmeldemethoden in der Profile Activity konsistent machen (z.B. immer Email zuerst, dann Google, dann Apple (für später))
- [ ] Wenn kein User Name festgelegt ist, sollte in der Profile Activity direkt ein Anmeldedialog geöffnet sein, bei dem man sich direkt anmelden kann ohne erst auf den Email Anmelde Button klicken zu müssen. Auch sollte dann der Button für die Email Anmeldung nicht angezeigt werden. Der Dialog sollte dezenter als die activity_auth sein, also kleinere Buttons, die gleich horizontal neben einander sein können (anmelden und registrieren). Die Anmeldung über den Google Button sollte aber weiterhin als Button in der Profile Activity angezeigt werden.

## Fixes

- [x] Ladezeit der MainActivity optimieren, lokale Kopie der Cloud Datenbank verwenden
- [ ] Während Ladezeit (z.B. nach Account Wechsel) einen Ladebildschirm anzeigen

## Accounts

- [x] Erstellen eines Accounts mit E-Mail und Passwort
- [x] Verknüpfung mit Google Account
- [ ] Verknüpfung mit Apple-Account
- [x] Zurücksetzen des Passworts via E-Mail
- [ ] 2FA (Two-Factor Authentication) implementieren
- [x] Profilbild hochladen und ändern
- [ ] Unterscheidung zwischen Anmeldung und Registrierung in der UI klarer machen / zwei verschiedene Layouts?
- [ ] Wenn der Dialog zum Anzeigen des Profilbilds in der ProfileActivity geöffnet ist, sollte zusätzlich zum Schließen Button auch noch ein Bearbeiten Button im Dialog angezeigt werden, der einen dann direkt zum Profil Bearbeiten Dialog führt.

## Platformen

- [ ] iOS Version der App erstellen
- [ ] Web Version der App erstellen

## Geteilte Listen

- [ ] Verwaltung von Nutzerrechten (Admin, Editor, Viewer)
- [ ] Benachrichtigungen bei Änderungen in geteilten Listen

## Members & Einladungen

- [ ] Benachrichtigungen bei Einladung zu einer geteilten Liste (Shortcuts zum Akzeptieren/Ablehnen)
- [ ] Teilen von Listen via Link oder Code (ohne Account)
- [x] Profilbilder der Member in der Member-Liste anzeigen
- [ ] Einladungen blockieren (z.B. Spam-Einladungen verhindern), auch von spezifischen Nutzern

## Einkaufserlebnis & Item-Features

- [ ] **Kategorien:** Artikel kategorisieren (z.B. Obst, Drogerie) und Liste danach gruppieren
- [ ] **Bilder:** Fotos zu Artikeln hinzufügen (z.B. spezifisches Produktfoto)
- [ ] **Preis-Tracking:** Preis pro Artikel hinterlegen und Gesamtsumme der Liste berechnen
- [ ] **Autovervollständigung:** Vorschläge beim Tippen basierend auf zuletzt genutzten Artikeln
- [ ] **Display-Modus:** Option "Bildschirm anlassen" während die Liste geöffnet ist

## Erweiterte Listen-Verwaltung

- [ ] **Listen-Anpassung:** Eigene Farben oder Icons für Listen zur besseren Unterscheidung im Hauptmenü
- [ ] **Sortierung:** Optionen zum Sortieren innerhalb einer Liste (Alphabetisch, nach Kategorie, Erledigte nach unten)

## Backend & Technik

- [ ] **Cleanup verbessern:** Beim Löschen einer Liste via Cloud Functions auch die Subcollection "items" rekursiv löschen (verhindert verwaiste Daten)
- [ ] **Offline-Modus:** Visueller Indikator, wenn die App offline ist und Änderungen nur lokal gespeichert sind
- [ ] **Passwort-Manager:** App Icon wird nicht korrekt angezeigt im Google Passwort Manager

## Bugs

- [ ] Einkaufsliste Activity schließt / stürzt ab, nachdem man eine Liste unsynced hat.
- [x] Nach dem Ändern des Profilbildes wird im Vorschau Dialog immer noch das alte Profilbild angezeigt. Erst nach aktualisieren / neu starten der Activity wird das neue Bild korrekt angezeigt.
- [x] Beim Einladen eines Users zu einer geteilten Liste werden Vorschläge aus dem Passwort Manager gemacht, das sollte nicht so sein.
- [x] Die Nachricht "Abgemeldet" in der ProfileActivity sollte erst angezeigt werden, wenn die Loading Animation abgeschlossen ist und man wieder in der fertig geladenen ProfileActivity ist, um Überschneidung mit dem Loading Overlay zu vermeiden.
- [x] Beim Hinzufügen von neuen Artikeln in der EinkaufslisteActivity flackert das Bild, keine cleane und smoothe UI Aktualisierung.
- [x] Nach bearbeiten eines Artikels wird die UI in der Einkaufsliste Acitivity nicht korrekt aktualisiert. Es wird noch der alte Stand gezeigt und erst beim neuen Öffnen der Liste werden die Artikel korrekt angezeigt.
 
## Fixed Bugs
- [x] Ladezeit der MainActivity optimieren, lokale Kopie der Cloud Datenbank verwenden
- [x] Erstellen eines Accounts mit E-Mail und Passwort
- [x] Verknüpfung mit Google Account
- [x] Zurücksetzen des Passworts via E-Mail
- [x] Profilbild hochladen und ändern
- [x] Profilbilder der Member in der Member-Liste anzeigen
- [x] Nach dem Ändern des Profilbildes wird im Vorschau Dialog immer noch das alte Profilbild angezeigt.
- [x] Beim Einladen eines Users zu einer geteilten Liste werden Vorschläge aus dem Passwort Manager gemacht.
- [x] Die Nachricht "Abgemeldet" in der ProfileActivity wird erst nach dem Laden angezeigt.
- [x] Flackern beim Hinzufügen von Artikeln behoben.
- [x] UI Aktualisierung nach Bearbeiten eines Artikels korrigiert.

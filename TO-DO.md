# To-Dos Shopping List App

## Allgemein

- [ ] Impressum und Datenschutzrichtlinie erstellen und in der App verlinken
- [x] Ladebildschirm / Splash Screen beim Laden von Daten, wenn UI noch nicht ready oder während längerer Ladezeiten (Anmeldung, Account Wechsel, etc.) Momentan wird manchmal ein Lade Kreis angezeigt, der aber dann über die UI gelegt wird, was nicht so toll aussieht.
- [x] Hint in EditText Dialog für den Nutzernamen in Profil Activity hinzufügen, z.B. "Nutzernamen eingeben"
- [x] Reihenfolge der verknüpften Anmeldemethoden in der Profile Activity konsistent machen (z.B. immer Email zuerst, dann Google, dann Apple (für später))
- [x] Wenn kein User Name festgelegt ist, sollte in der Profile Activity direkt ein Anmeldedialog geöffnet sein, bei dem man sich direkt anmelden kann ohne erst auf den Email Anmelde Button klicken zu müssen. Auch sollte dann der Button für die Email Anmeldung nicht angezeigt werden. Der Dialog sollte dezenter als die activity_auth sein, also kleinere Buttons, die gleich horizontal neben einander sein können (anmelden und registrieren). Die Anmeldung über den Google Button sollte aber weiterhin als Button in der Profile Activity angezeigt werden.
- [ ] Je nach ausgewählter Sprache in der App sollten die Account Verifizierungs Emails und Seiten im Browser auch auf dieser Sprache sein.
- [x] Einkaufsliste Activity schließt / stürzt ab, nachdem man eine Liste unsynced hat. Das muss gefixt werden, sodass nach dem Unsync Prozess die Activity einfach offen bleibt und der User direkt weiter machen kann mit der Liste.

## Fixes

- [x] Ladezeit der MainActivity optimieren, lokale Kopie der Cloud Datenbank verwenden
- [x] Während Ladezeit (z.B. nach Account Wechsel) einen Ladebildschirm anzeigen

## Accounts

- [x] Erstellen eines Accounts mit E-Mail und Passwort
- [x] Verknpüfung mit Google Account
- [ ] Verknpüfung mit Apple-Account
- [x] Zurücksetzen des Passworts via E-Mail
- [ ] 2FA (Two-Factor Authentication) implementieren
- [x] Profilbild hochladen und ändern
- [ ] Unterscheidung zwischen Anmeldung und Registrierung in der UI klarer machen, ?-Button im Anmeldedialog, der genauer erklärt, was der Unterschied ist?
- [ ] Wenn der Dialog zum Anzeigen des Profilbilds in der ProfileActivity geöffnet ist, sollte zusätzlich zum Schließen Button auch noch ein Bearbeiten Button im Dialog angezeigt werden, der einen dann direkt zum Profil Bearbeiten Dialog führt.

## Platformen

- [ ] iOS Version der App erstellen
- [ ] Web Version der App erstellen

## Geteilte Listen

- [ ] Verwaltung von Nutzerrechten (Admin, Editor, Viewer)
- [ ] Benachrichtigungen bei Änderungen in geteilten Listen (Name des Akteurs, Anzahl der Artikel, die neu hinzugrfügt wurden und Titel der Liste)

## Members & Einladungen

- [ ] Benachrichtigungen bei Einladung zu einer geteilten Liste (Shortcuts zum Akzeptieren/Ablehnen)
- [ ] Teilen von Listen via Link oder Code (ohne Account)
- [x] Profilbilder der Member in der Member-Liste anzeigen
- [ ] Einladungen blockieren (z.B. Spam-Einladungen verhindern), auch von spezifischen Nutzern

## Einkaufserlebnis & Item-Features

- [ ] **Kategorien:** Artikel kategorisieren (z.B. Obst, Drogerie) und Liste danach gruppieren
- [ ] **Bilder:** Fotos zu Artikeln hinzufügen (z.B. spezifisches Produktfoto)
- [ ] **Preis-Tracking:** Preis pro Artikel hinterlegen und Gesamtsumme der Liste berechnen
- [ ] **Kaufempfehlungen:** Vorschläge beim Tippen basierend auf zuletzt genutzten Artikeln
- [ ] **Display-Modus:** Option "Bildschirm anlassen" während die Liste geöffnet ist

## Erweiterte Listen-Verwaltung

- [ ] **Listen-Anpassung:** Eigene Farben oder icons für Listen zur besseren Unterscheidung im Hauptmenü
- [ ] **Sortierung:** Optionen zum Sortieren innerhalb einer Liste (Alphabetisch, nach Kategorie, Erledigte nach unten)

## Backend & Technik

- [ ] **Cleanup verbessern:** Beim Löschen einer Liste via Cloud Functions auch die Subcollection "items" rekursiv löschen (verhindert verwaiste Daten)
- [ ] **Offline-Modus:** Visueller Indikator, wenn die App offline ist und Änderungen nur lokal gespeichert sind
- [ ] **Passwort-Manager:** App Icon wird nicht korrekt angezeigt im Google Passwort Manager

## Bugs

- [ ] Loading Skeleton beim Setzen eines Benutzernamens (ohne verknpfte Anmelde-Methode) sollte anders aussehen, sieh dir @Screenshot_20260108-140247.png an, so sieht die Activity nach dem setzen des Usernames aus. Und so sollte auch das Skeleton beim laden nach dem setzen des User names aussehen. Erstelle es und nutze es für dieses Szenario.
- [ ] Das Einladen eines Nutzers zu einer geteilten Liste über die Email Adresse ist nur möglich, wenn der User auch einen Nutzernamen gesetzt hat, aber das sollte nicht nötig sein. Wenn ein erfolgreich bestätigtes (Email verifizier) Konto mit der Email existiert, sollte man den Nutzer auch über die Email einladen können, ohne, dass der User einen Username gesetzt hat. Aber sobald der User einen Nutzernamen setzt, sollte dieser auch anstatt der Email in der liste der member einer Liste auftauchen.
- [ ] Google-Verknpüfung vom Konto sollte unabhängig von der E-Mail des Google Kontos geschehen. Momentan gibt es ein Problem, wenn ein User eine Google-Mail Adresse nutzt für den E-Mail login und ein anderer User seinen Account mit dem Google Konto eben gleicher Google-Mail verknpüfen möchte. Es sollte möglich sein, dass beide Konten getrennt voneinander funktionieren, da der eine User die Google-Mail als Email Login nutzt und der andere User die Google Mail zum anmelden und verknüpfen über den Google Account nutzen möchte. Fixe diese Behandlung

## Fixed Bugs

- [x] Beim Klicken auf die Bestätigungstaste auf der Tastatur im Benutzer Einladen dialog, sollte automatisch der einladen button betätigt werden.
- [x] Beim klicken auf Account Löschen bei einem anonymen Nutzer mit Nutzernamen sollten auch alle geteilten Listen, die der Nutzer erstellt hat aus der Datenbank gelöscht werden. Auch sollte er aus den Listen entfernt werden, in denen er Mitglied ist.

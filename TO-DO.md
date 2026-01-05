# To-Dos Shopping List App

## Allgemein

- [ ] Impressum und Datenschutzrichtlinie erstellen und in der App verlinken

## Fixes

- [x] Ladezeit der MainActivity optimieren, lokale Kopie der Clouda Datenbank verwenden

## Accounts

- [x] Erstellen eines Accounts mit E-Mail und Passwort
- [x] Verknüpfung mit Google Account
- [ ] Verknüpfung mit Apple-Account
- [x] Zurücksetzen des Passworts via E-Mail
- [ ] 2FA (Two-Factor Authentication) implementieren
- [x] Profilbild hochladen und ändern
- Unterscheidung zwischen Anmeldung und Registrierung in der UI klarer machen / zwei verschiedene Layouts?

## Platformen

- [ ] iOS Version der App erstellen
- [ ] Web Version der App erstellen

## Geteilte Listen

- [ ] Verwaltung von Nutzerrechten (Admin, Editor, Viewer)
- [ ] Benachrichtigungen bei Änderungen in geteilten Listen

## Members & Einladungen

- [ ] Benachrichtigungen bei Einladung zu einer geteilten Liste (Shortcuts zum Akzeptieren/Ablehnen)
- [ ] Teilen von Listen via Link oder Code (ohne Account)
- [x] Profilbilder der Member in der Member-Liste anzeigen
- [ ] Einladungen blockieren (z.B. Spam-Einladungen verhindern), auch von spezifischen Nutzern

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
- [ ] **Passwort-Manager:** App Icon wird nicht korrekt angezeigt im Google Passwort Manager

## Bugs

- [ ] Einkaufsliste Activity schließt / stürzt ab, nachdem man eine Liste unsynced hat.

## Fixed Bugs

- [x] Einkaufslisten, die man gerade gesynced hat, werden in der Main Activity dann ganz am Ende der Liste angezeigt, obwohl sie an der Position bleiben sollten, an der sie vor dem syncen waren.
- [x] Beim erstellen einer Liste sollte der Curser Fokus automatisch im EditText für den Listennamen sein und die Tastatur geöffnet werden (sowohl für private als auch geteilte Listen).
- [x] Nach Abmelden von Account nicht direkt Edit Profile Dialog anzeigen.
- [x] Beim Erstellen einer geteilten Liste ohne vorher User Name fesgelegt zu haben, Liste nach korrekter Vergabe auch speichern und nicht verwerfen.
- [x] Email Icon Farbe für den Mit Email Anmelden Button ist noch falsch.
- [x] Unsynced Cloud Symbol ist im Hintergrund bei Einladung in der Main Activity sichtbar und überlappt mit Annehmen Button.
- [x] Umrandung für Ablehnen Button bei Einladung zu geteilter Liste fehlt noch (Nutzung des Styles muss noch korrekt angewendet werden).
- [x] Edit Profile Dialog öffnet automatisch, wenn man Profile Activity öffnet ohne Username festgelegt zu haben.
- [x] Ablehnen Button bei Einladung hat nicht mehr die korrekte Farbe colorError
- [x] Unsynced Cloud Icon sollte bei Einladung in der Main Activity nicht sichtbar sein. Ist es aber aktuelle noch.
- [x] Nach Abmeldung werden manchmal fälschlicherweise die unlink Icons in den Anmeldungs Buttons angezeigt anstatt des Email oder Google Icons.
- [x] Beim Input für das Einladen eines Members werden Passwort Manager Vorschläge angezeigt (sollte nicht sein).
- [x] Der Abstand in @app/src/main/res/layout/dialog_edit_profile.xml zwischen Speichern Button und dem Titel von Profilbild ist zu klein. Es wirkt etwas gequetscht.
- [x] Außerdem finde ich den Abstand zwischen den Buttons in der ProfilActivity zu groß, also zwischen Email Button, Google Button, Abmelden Button und Konto Löschen Button, das kannst du etwas enger machen.
- [x] Der Sprache Button funktioniert nicht und du solltest auch nicht so einen text button machen sondern @app/src/main/res/layout/menu_item_language.xml nutzen, also einfach genauso wie in der ProfileActivity.
- [x] Dann möchte ich noch den Übergang zwischen @app/src/main/res/layout/dialog_members_list.xml und @app/src/main/res/layout/dialog_invite_user.xml smoother machen. Momentan flackert es da kurz, weil der eine Dialog geschlossen wird und der andere geöffnet, genauso beim Übergang zurück. Aber es wäre viel eleganter und Smoother, wenn der Dialog nicht schließ, sondern einfach nur überdeckt wird, so flackert es nicht, ist es möglich das umzusetzen?
- [x] Einkaufslisten, die man gerade unsynced hat, werden in der Main Activity dann ganz am Ende der Liste angezeigt, obwohl sie an der Position bleiben sollten, an der sie vor dem Unsyncen waren.
- [x] Position der Einladungs-Benachrichtigung in der Main Activity ist falsch, sie sollten am Ende der Listen auftauchen, aber sie erscheinen irgendwo random (zumindest kann ich kein Muster erkennen).
- [x] Beim Bearbeiten eines Listennamens, kanne es vorkommen, dass der +-Button zum Hinzufügen einer Liste den Speichern-Button überdeckt. Beim Bearbeiten eines Listennamens, sollte der +-Button ausgeblendet werden.
- [x] Konto wechseln? - Dialog sollte nicht angezeigt werden, wenn zum Zeitpunkt der Anmeldung kein Profil vorhanden ist, welches durch die Anmeldung Datenverlust erleiden würde. Auch die Nachricht ergibt irgendwie nicht so wirklich, warum wird das Google Konto erwähnt, wenn der Dialog bei der Anmeldung mit Email/Passwort erscheint?
- [x] Hinzufügen von Artikeln wirkte ruckelig -> Optimiert durch DiffUtil und optimistisches Einfügen mit postDelayed.
- [x] Drag & Drop Positionen sprangen zurück -> Behoben durch Batch-Updates und Collections.swap.
- [x] Snackbar verdeckte Listeninhalte und führte zu Abstürzen -> Ersetzt durch stabile Undo-Bar mit dynamischem Margin.
- [x] Inkonsistente Positionen beim "Undo" (Wiederherstellen) -> Behoben durch Beibehaltung der Item-ID.
- [x] Hinzufügen während Bearbeitung möglich -> Blockiert mit Hinweis.
- [x] Undo-Bar Abstand zu groß -> Layout angepasst und margin optimiert.
- [x] Unnötige Toasts entfernt -> Export-Meldung bereinigt.
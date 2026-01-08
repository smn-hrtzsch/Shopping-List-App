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

- [ ] **Listen-Anpassung:** Eigene Farben oder Icons für Listen zur besseren Unterscheidung im Hauptmenü
- [ ] **Sortierung:** Optionen zum Sortieren innerhalb einer Liste (Alphabetisch, nach Kategorie, Erledigte nach unten)

## Backend & Technik

- [ ] **Cleanup verbessern:** Beim Löschen einer Liste via Cloud Functions auch die Subcollection "items" rekursiv löschen (verhindert verwaiste Daten)
- [ ] **Offline-Modus:** Visueller Indikator, wenn die App offline ist und Änderungen nur lokal gespeichert sind
- [ ] **Passwort-Manager:** App Icon wird nicht korrekt angezeigt im Google Passwort Manager

## Bugs

- [ ] Nach Abmelden von einem Account werden keine Autofill Vorschläge mehr im Email Feld für die Anmelden Section gemacht. Erst nachdem man die Activity neu lädt oder neu öffnet.
- [ ] Wenn lokal Listen erstellt wurden (auch ohne User oder Benutzername) und auf den Google Anmelden Button oder die Anmeldung über Email geklickt wird, sollte vor dem Anmelden und Wechseln auf den Account erst noch der Account wechseln Dialog kommen, der einem klar macht, dass die lokalen Listen verloren gehen, falls man zu einem anderen bestehenden Account wechselt.
- [ ] Google-Verknpüfung vom Konto sollte unabhängig von der E-Mail des Google Kontos geschehen. Momentan gibt es ein Problem, wenn ein User eine Google-Mail Adresse nutzt für den E-Mail login und ein anderer User seinen Account mit dem Google Konto eben gleicher Google-Mail verknpüfen möchte. Es sollte möglich sein, dass beide Konten getrennt voneinander funktionieren, da der eine User die Google-Mail als Email Login nutzt und der andere User die Google Mail zum anmelden und verknüpfen über den Google Account nutzen möchte. Fixe diese Behandlung

## Fixed Bugs

- [x] Email Bestätigen Funktionalität kann ausgetrickst werden, indem man die App komplett schließt und neu öffnet. Dann wird der Account trotzdem korrekt erstellt und registriert, obwohl die Email noch gar nicht bestätigt ist.
- [x] E-Mail bestätigen dialog sollte lieber 'dialog_vertical_buttons' verwenden, um den Text für die Buttons sauberer darstellen zu können.
- [x] Für den Fall, dass ein anonymer Nutzer, der nur bereits einen Nutzernamen gesetzt hat sich mit Goole verknpüfen will, aber dabei darauf stößt, dass schon ein anderes Konto mit dem gewählten Google Account verknüpft ist, sollte in dem "konto wechseln?"-Dialog auch noch eindeutiger beschrieben werden, dass im Falle eines Wechsels das anonyme Konto gelöscht wird (da keine andere Anmelde-Methode festgelegt ist). Überprüfe auch, ob wir das so korrekt umsetzen, also dass die Anonyme Leiche aus der Firestore Datenbank gelöscht wird, falls der Nutzer wirklich auf "Konto wechseln" klickt.
- [x] Es sollte die Fehlermeldung angepasst werden, die man bekommt, wenn man sich mit noch nicht registrierten Nutzerdaten anmelden möchte. Momentan heißt es nur: "E-Mail, Benutzername oder Passwort falsch." Was noch eine gute Ergänzung wäre: Ein Hinweise, dass man sich registrieren muss, wenn man noch kein Konto mit den eingegebenen Daten hat. Dieser sollte als separate String ressource unter der eigentlichen Fehlermeldung angezeigt werden.
- [x] Es wird immer noch nach dem Abmelden von einem Konto automatisch ein anonymer User bei Firestore registriert, das sollte nicht der Fall sein. Erst wenn ein Nutzername festgelegt wird, sollte der User auch registriert werden.
- [x] Beim Wechseln zu einem anderen Konto muss sichergestellt werden, dass im Falle des Wechselns von einem Anonymen Accounts (nur Username gesetzt, keine anmelde methoden) das Konto des anonymen Users auch gelöscht wird. Sonst werden potenzielle Leichen in der Datenbank gepeischert, die nicht mehr erreichbar sind.
- [x] Beim Anmelden über Google wird fälschlicherweise 'skeleton_logout' genutzt anstatt 'skeleton_profile'.
- [x] Nach dem Autofill im Email oder Passwortfeld sollte die Tastatur automatisch schließen, momentan bleibt sie offen und verdeckt den Inhalt der Anmelde Section in der ProfileActivity.
- [x] Beim Abmelden wird noch das falsche Skeleton genutzt als Lade Screen. Es sollte 'skeleton_logout' genutzt werden, aber es wird noch 'skeleton_profile' genutzt
- [x] Beim Abhaken von Artikeln wird manchmal der Fokus / das Scrollen zum Ende der Liste gesetzt. Aber das ist nicht gewollt. Es sollte beim Abhaken kein automatisches Scrollen geben. Nur beim Hinzufügen von Artikeln. Das Problem tritt hauptsächlich bei privaten synced Listen auf.
- [x] Es fehlen noch deutsche Strings für den Abmelden Dialog.
- [x] Beim entfernen einer Anmelde-Methode sollten die Buttons untereinander angezeigt werden, da horizontal der Text zu viel Platz einnimmt mit zwei Buttons nebeneinander. Du kannst dazu den dialog_vertical_buttons.xml verwenden.xml nutzen.
- [x] Nach dem Bearbeiten eines Items und dem Speichern wird die bearbeitte Version erst nach dem erneuten Laden / neu Öffnen der Liste angezeigt. Die UI wird also nicht korrekt aktualisiert.
- [x] Switch zum Entscheiden, ob Private Listen automatisch synchronisiert werden sollten ist schlecht erkennbar, kann man die Farben vielleicht etwas deutlicher vom Hintergrund abheben?
- [x] Momentan wird die Entscheidung, ob Private Listen automatisch synchronisiert werden sollten nicht an den Account gebunden, sondern in der App verwaltet, dass heißt, wenn ich mich abmelde und bei einem anderen Account den Switch betätige, wird diese Entscheidung auch beim Account wechsel mit übernommen, das sollte nicht so sein. Die Entscheidung sollte beim User liegen und der Switch sollte bei der ersten Registrierung des Users automatisch aktiviert werden (nach Verknpfung oder Registrierung per Email oder Google).
- [x] In der Anmeldung Section wird mir nicht immer konsistent Autofill von meinem Passwort Manager vorgeschlagen. Beim ersten mal Anmelden nach dem Öffnen der App schon, aber dann nach dem Abmelden schon nicht mehr.
- [x] Die Tastatur sollte nach einem Autofill immer geschlossen sein, aber sie ist manchmal immer noch offen. Es tritt zwar sehr selten auf, aber eben manchmal schon noch. Das Verdeckt dann UI Elemtente und sollte nicht der Fall sein.
- [x] Im 'dialog_auth' Dialog und in der Anmelden Section gibt es einen Bug, bei dem die Tastatur sich von selbst schließt, während man noch dabei ist eine Eingabe zu tätigen. Das nervt und sollte nicht so sein.
- [x] Wenn man in der Anmelde Sektion im Email Feld ist und etwas eingibt und dann seine Eingabe auf der Tastatur bestätigt schließt sich die Tastatur anstatt einfach zum Passwort Feld den Fokus zu wechseln und offen zu bleiben. Fixe das, ich möchte im Grunde, dass das Email Feld in der Anmelde Sektion in der ProfilActivity genau so funktioniert wie das Email Feld in 'dialog_auth', wenn dieser geöffnet ist.
- [x] Wenn man im 'dialog_auth' oder in der Anmelde Section, die per default angezeigt wird, wenn noch kein Benutzername oder andere Anmelde-Methode gesetzt ist angezeigt wird, im Passwort feld auf bestätigen auf der Tastatur drückt, sollte per default der Anmelde-Button betätigt werden.
- [x] Beim Eingeben eines Nutzernamens und dem klicken auf die Bestätigen Taste auf der Tastatur, sollte der Speichern Button betätigt werden, sodass der User direkt über die Tastatur seine Eingabe abschließen und den Nutzernamen setzen kann.
- [x] Beim Klicken auf das Augen Symbol zum Anzeigen des Passworts schließt sich die Tastatur, aber sie sollte einfach offen bleiben, um im Falle, dass man direkt noch das Passwort anpassen möchte los schreiben kann.
- [x] Es sollte die Fehlermeldung angepasst werden, wenn man auf registrieren klickt, aber schon Daten, die zu einem Account gehören angezeigt wird. Momentan wird wenn man den korrekten Benutzernamen und das korreke Passwort verwendete einfach nur angezeigt: "Bitte gib eine gültige E-Mail Adresse ein." Aber es sollte eher einen Verweis darauf geben, dass bereits ein Konto mit diesem Nutzernamen existiert.
- [x] Autofill für Account-Name-Feld (Username) durch benutzerdefinierte NoAutofillEditText-Klasse und technische Maßnahmen unterbunden, um fehlerhafte Passwort-Manager-Vorschläge zu verhindern.
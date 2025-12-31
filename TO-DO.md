# To-Dos Shopping List App

## Fixes

- [x] Ladezeit der MainActivity optimieren, lokale Kopie der Clouda Datenbank verwenden

## Accounts

- [x] Erstellen eines Accounts mit E-Mail und Passwort
- [x] Verknüpfung mit Google Account
- [ ] Verknüpfung mit Apple-Account
- [x] Zurücksetzen des Passworts via E-Mail
- [ ] 2FA (Two-Factor Authentication) implementieren
- [ ] Profilbild hochladen und ändern

## Platformen

- [ ] iOS Version der App erstellen
- [ ] Web Version der App erstellen

## Getreilte Listen

- [ ] Verwaltung von Nutzerrechten (Admin, Editor, Viewer)
- [ ] Benachrichtigungen bei Änderungen in geteilten Listen

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

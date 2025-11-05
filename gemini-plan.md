# Implementierungsplan: Kollaborative Listen (Hybrid-Ansatz)

## 1. Projektübersicht & Ausgangslage

* **App:** Native Android (Java) Einkaufslisten-App.

* **Aktueller Status:** Die App ist eine reine Offline-Anwendung.

* **Datenhaltung:** Alle Daten (Listen, Artikel) werden lokal in einer **SQLite-Datenbank** gespeichert.

* **Architektur:** Die Datenlogik ist sauber von der UI getrennt (`ShoppingListDatabaseHelper.java`, `ShoppingListRepository.java`, `ShoppingListManager.java`).

* **Dateien:** Der vollständige Quellcode (Activities, Adapter, Layouts, DB-Helper etc.) liegt vor.

* **Manuelles Teilen:** Eine einfache Export/Import-Funktion für Listen via JSON-Datei (`shareShoppingList` / `importShoppingList`) existiert bereits, bietet aber keine Echtzeit-Synchronisierung.

## 2. Ziel: Kollaborative Echtzeit-Listen

Das Ziel ist die Erweiterung der App um **kollaborative Einkaufslisten**. Mehrere Nutzer (z.B. eine Familie) sollen eine geteilte Liste gleichzeitig bearbeiten können. Änderungen (Artikel hinzufügen, abhaken) sollen bei allen Teilnehmern in Echtzeit sichtbar sein.

## 3. Kern-Architektur: Der Hybrid-Ansatz (WICHTIG)

Dies ist die zentrale Anforderung:

* **Kein Zwang zum Cloud-Umbau:** Die bestehende, stabile Offline-Funktionalität (basierend auf SQLite) soll **vollständig erhalten bleiben**.

* **Parallele Datenhaltung:** Firebase wird *parallel* zur lokalen SQLite-Datenbank eingeführt.

* **Zwei Listentypen:** Die App muss zukünftig zwei Arten von Listen verwalten:

    1. **Lokale Listen:** (Wie bisher) Werden nur in der lokalen SQLite-DB gespeichert. Sie sind schnell, offline verfügbar und erfordern keinen Login.

    2. **Geteilte Listen:** (Neu) Werden in der Firebase Cloud (Firestore) gespeichert. Sie erfordern eine Internetverbindung und Authentifizierung, um geteilt und synchronisiert zu werden.

* **User Experience:** Bestehende Nutzer sollen die App ohne Änderungen weiter nutzen können. Die Geteilten Listen sind ein **optionales "Opt-In"-Feature**.

## 4. Technologie-Stack (Neu)

* **Backend-Service:** Firebase (Backend-as-a-Service von Google).

* **Datenbank (Cloud):** **Cloud Firestore**. Wird für die Speicherung und Echtzeit-Synchronisierung der "Geteilten Listen" und deren Artikel verwendet.

* **Authentifizierung:** **Firebase Authentication**. Wird benötigt, um Nutzer zu identifizieren und Listen zuzuordnen.

## 5. Implementierungs-Aktionsplan

### Schritt 1: Firebase-Setup & Anonyme Authentifizierung

1. **Firebase-Projekt:** Ein neues Firebase-Projekt in der Google Cloud Console erstellen.

2. **App registrieren:** Die Android-App (Package Name: `com.example.einkaufsliste` laut `AndroidManifest.xml`) im Firebase-Projekt registrieren und die `google-services.json` Datei dem Projekt hinzufügen.

3. **SDKs hinzufügen:** Die notwendigen Firebase-Bibliotheken in `build.gradle` einbinden (z.B. `firebase-auth`, `firebase-firestore`).

4. **Anonyme Authentifizierung:**

    * Ziel: Jeder Nutzer soll eine User-ID haben, *ohne* einen Login-Screen zu sehen.

    * Aktion: In der `MainActivity.java` beim App-Start prüfen, ob ein Nutzer angemeldet ist.

    * Falls `Firebase.auth.getCurrentUser() == null`, im Hintergrund `Firebase.auth.signInAnonymously()` aufrufen.

    * Der Nutzer merkt davon nichts, hat aber eine stabile, anonyme `userId`. Diese wird für das Erstellen und Einladen zu geteilten Listen benötigt.

### Schritt 2: Datenmodell-Anpassung (Logische Trennung)

1. **`ShoppingList.java` erweitern:**

    * Die Klasse `ShoppingList.java` muss einen neuen Member erhalten, um Cloud-Listen von lokalen Listen zu unterscheiden.

    * `private String firebaseId;` (Die Document-ID aus Firestore).

    * `private String ownerId;` (Die `userId` des Erstellers).

    * `private List<String> members;` (Eine Liste von `userIds`, die Zugriff haben).

    * **Logik:** Wenn `firebaseId` `null` ist, ist es eine lokale Liste (und `id` ist der SQLite-Key). Wenn `firebaseId` *nicht* `null` ist, ist es eine Cloud-Liste.

### Schritt 3: Firestore-Datenstruktur (Cloud-Datenbank)

Folgende Struktur in Cloud Firestore anlegen:

```
/shopping_lists/{listId}/
    - name (String)
    - ownerId (String)
    - members (Array<String>)
    
    /items/{itemId}/
        - name (String)
        - quantity (String)
        - unit (String)
        - isDone (Boolean)
        - position (Number)
        - notes (String)
```

### Schritt 4: UI-Anpassungen

1. **Listen-Erstellung (`MainActivity.java`):**

   * Beim Klick auf den FAB (+) muss der Nutzer gefragt werden: "Lokale Liste" oder "Geteilte Liste" erstellen?

2. **Listen-Anzeige (`ListRecyclerViewAdapter.java`):**

   * Der Adapter muss Listen aus *beiden* Quellen laden (siehe Schritt 5).

   * Geteilte Listen (mit `firebaseId != null`) sollten visuell markiert werden (z.B. durch ein kleines Cloud-Icon).

### Schritt 5: Repository-Anpassung (Die "Router"-Logik)

Das `ShoppingListRepository.java` wird zur zentralen Weiche (Router). Es muss bei *jeder* Operation entscheiden, welche Datenquelle angesprochen wird.

1. **Listen laden (`getAllShoppingLists`):**

   * Muss nun *zwei* Abfragen parallel starten:

     1. Die lokale SQLite-DB abfragen (wie bisher).

     2. Firestore abfragen: `firestore.collection("shopping_lists").whereArrayContains("members", currentUserId).addSnapshotListener(...)`.

   * Die Ergebnisse beider Abfragen müssen zusammengeführt und an den `ListRecyclerViewAdapter` übergeben werden.

2. **Artikel laden (`getItemsForListId`):**

   * Benötigt eine `ShoppingList` als Parameter.

   * `if (list.getFirebaseId() != null)`: Firestore abfragen (z.B. `firestore.collection("shopping_lists").document(list.getFirebaseId()).collection("items").addSnapshotListener(...)`).

   * `else`: SQLite abfragen (wie bisher).

3. **Schreib-Operationen (`addItem`, `updateItem`, `deleteItem` etc.):**

   * Müssen ebenfalls die `if (list.getFirebaseId() != null)`-Prüfung durchführen und entweder die Firestore-API oder den `ShoppingListDatabaseHelper` aufrufen.

### Schritt 6: Echtzeit-Synchronisierung

* Die Firestore-Abfragen (Schritt 5) müssen **`addSnapshotListener`** (nicht `.get()`) verwenden.

* Der Snapshot-Listener wird automatisch von Firebase aufgerufen, wenn sich Daten in der Cloud ändern.

* Im Callback des Listeners müssen die neuen Daten an den jeweiligen Adapter (`ListRecyclerViewAdapter` oder `MyRecyclerViewAdapter`) übergeben und `notifyDataSetChanged()` (oder bessere DiffUtil) aufgerufen werden, um die UI in Echtzeit zu aktualisieren.

### Schritt 7: Sharing-Funktion (Einladungslogik)

1. **Neue UI:** Es wird eine neue UI (z.B. ein Dialog in `EinkaufslisteActivity.java`) benötigt, um andere Nutzer zu einer *bestehenden* Cloud-Liste einzuladen (z.B. über deren E-Mail oder eine zukünftige User-ID).

2. **Backend-Logik:** "Einladen" bedeutet, die `userId` des eingeladenen Nutzers zum `members`-Array des Listendokuments in Firestore hinzuzufügen.

## 6. Zukünftige Erweiterungen (Optional)

* **Echter Login (Google/E-Mail):** Implementieren einer "Konto verknüpfen"-Funktion. Firebase Authentication erlaubt die nahtlose Umwandlung eines *anonymen* Kontos in ein *permanentes* Konto (z.B. Google Sign-In).

* **"Cloud-Upload":** Eine Funktion, um eine *lokale* SQLite-Liste in eine *Geteilte Liste* umzuwandeln (Daten in Firestore kopieren und lokale Liste löschen).
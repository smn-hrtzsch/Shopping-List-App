- Nach jeder Änderung, die du machst, soll eine aussagekräftige Commit Message verfasst werden und mit dieser die Änderung commitet werden.
- Jedes neue Feature muss auf einem dedizierten Branch entwickelt werden.
- Sobald ein Feature abgeschlossen ist und ein erfolgreiches Feedback vom User vorliegt, muss die Änderung über einen Pull Request auf den Haupt-Branch gemergt und der obsolete Branch anschließend gelöscht werden. Zum Erstellen und Mergen des Pull Requests kann die GitHub CLI (`gh`) genutzt werden.
- Texte müssen IMMER korrekt als String-Variablen in den 'strings.xml' Dateien festgelegt werden. Verwende NIEMALS hartcodierte Strings im Java-Code oder in Layout-Dateien, um die Lokalisierung zu gewährleisten.
- Nach JEDER Code-Änderung muss zwingend ein manueller Compile-Vorgang (z.B. './gradlew assembleDebug') durchgeführt werden, um sicherzustellen, dass der Build fehlerfrei läuft.
- Du musst zum starten eines manuellen Compile-Vorgangs IMMER sicherstellen, dass du die korrekte Java-Version für den Build nutzt. Setze dazu manuell die JAVA_HOME Umgebungsvariable auf die passende JDK-Version. 
  - Hier ein Befehl für MacOS mit Homebrew-Installation von OpenJDK 21:

    ```bash
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    ```
  
  - Hier ist ein Befehl für WSL auf Windows:

    ```bash
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    ```

- Nutze für Standard Dialoge das vorgefertigte Layout 'app/src/main/res/layout/dialog_standard.xml' oder 'app/src/main/res/layout/dialog_vertical_buttons.xml'
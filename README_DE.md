# Benchmark: UUID unter Last – wie ein scheinbar kleines Detail zum System-Bottleneck wurde ⚡️🧩

[English](./README.md) | [简体中文](./README_ZH.md)

UUID wird in vielen Systemen ganz selbstverständlich verwendet – als **Request-Trace-ID**, als **Primärschlüssel in
Datenbanken** oder als **Identifier in verteilten Services**.

Dabei wird jedoch oft übersehen, dass **die Art der Erzeugung von UUIDs** unter hoher Parallelität **einen direkten
Einfluss** auf die Systemleistung hat. Insbesondere wenn UUIDs über Standard-Implementierungen erzeugt werden, kann dies
zu **unerwarteten Blockaden** und **messbaren Latenzen** führen.

> Dieses Benchmark-Projekt zeigt, wie ein scheinbar harmloser Mechanismus unter Last zum Performance-Bottleneck wird –
> und wie sich das Problem systematisch analysieren und beheben lässt.

Zusätzlich existieren verschiedene leistungsoptimierte UUID-Implementierungen auf dem Markt. Um jedoch keine
zusätzlichen Bibliotheksabhängigkeiten einzuführen, habe ich eine eigene Variante umgesetzt und **KUID** genannt. Im
Folgenden wird dieser Begriff ohne weitere Erläuterung verwendet.

🔍 **Keine Zeit für Details?**  
[Hier geht’s direkt zu den Ergebnissen.](#ergebnisse)

🔧 **Projekt selbst ausführen?**  
[Hier geht’s direkt zur Ausführung.](#ausführung)

## Hintergrund

**Spring Cloud Gateway** gilt grundsätzlich als eine **leistungsfähige Lösung** für das Routing und die Verwaltung von
API-Anfragen. In einem meiner Projekte erhielt ich jedoch Kundenrückmeldungen, dass ein auf Spring Cloud Gateway
basierendes Gateway unter Last nur **einige hundert Requests pro Sekunde** verarbeiten konnte.

Während der Performance-Analyse stellte sich heraus, dass die Standard-Implementierung zur **UUID-Generierung** in
bestimmten Szenarien einen unerwartet hohen Einfluss auf die Gesamtdurchsatzrate haben kann (in meinem Fall ca. **10%**
Performance-Einbußen).

> *Wichtig:*  
> Dieses Repository ist eine bewusst minimal gehaltene und vollständig von internen oder vertraulichen Informationen
> bereinigte Reproduktion des Befunds. Ziel ist es, die Ursache klar isoliert und nachvollziehbar darzustellen.

Der vollständige Lösungsweg des ursprünglichen Problems sowie die Optimierungsmaßnahmen werden in meinem separaten
Projekt **[performance-test-example](https://github.com/ksewen/performance-test-example)** erläutert. Auch dort wurden *
*sämtliche sensiblen Inhalte entfernt** oder **neutralisiert**.

## Ausführung

### Lokaler Starten

#### Voraussetzungen:

- **Java 8** oder höher
- **Maven 3.6.0** oder höher

#### Repository klonen:

```shell
git clone git@github.com:ksewen/uuid-benchmark.git
```

#### Projekt bauen

```bash
mvn clean package
```

Die ausführbare JAR befindet sich anschließend unter:

```shell
./target/uuid-benchmark.jar
```

#### Starten

1. Das Benchmark kann direkt über die erzeugte JAR-Datei gestartet werden:

```bash
java -jar ./target/uuid-benchmark.jar
```

2. Beim Start werden folgende Eingaben abgefragt:

```bash
please input the benchmark type UUID/KUID: 
# Unterstützte Werte: UUID oder KUID
UUID

# Falls leer bestätigt wird, wird der Standardpfad verwendet: benchmark-{type}-thread-{thread-counts}.log
please input the output file: 
/root/benchmark/benchmark-UUID-thread-8.log

# Unterstützt: ganze Zahlen, 1 als Standardwert
please input the thread count: 
8
```

### Starten über Docker

#### Docker Image bauen:

Im Projektwurzelverzeichnis:

```shell
resources/scripts/build-image.sh -d ..
```

#### Container starten

```shell
docker run -d ksewen/uuid-benchmark:1.0
```

#### In den laufenden Container wechseln

```shell
docker exec -it ${container-id} /bin/sh
```

#### Benchmark ausführen

Im laufenden Container

```shell
java -jar uuid-benchmark.jar
```

Die erforderlichen Eingaben sind im Abschnitt [Starten](#starten) beschreiben.

## Ergebnisse

Die folgende Messung stellt die Resultate eines Lasttests als Beispiel dar.  
Sie zeigt deutlich, dass unterschiedliche Strategien zur UUID-Erzeugung um Größenordnungen im Durchsatz variieren
können.



> *Wichtig:*  
> Die im folgenden Benchmark gemessenen Unterschiede sind isolierte Benchmark-Ergebnisse. Sie fallen deutlich
> höher aus als in einer realen Gateway-Produktivumgebung. Der Benchmark dient dazu, den Effekt klar sichtbar zu machen.

Die nachfolgende Messung zeigt zwei Implementierungen im direkten Vergleich.  
Während `UUID.randomUUID()` unter Java 8 durch die synchronisierte `SecureRandom`-Instanz limitiert ist, nutzt **KUID**
eine vorkonfigurierte, nicht-blockierende Random-Quelle.

![UUID vs KUID Benchmark](https://raw.githubusercontent.com/ksewen/Bilder/main/20251109184252140.png)

|               Methode               |     Durchsatz     |      Differenz      |
|:-----------------------------------:|:-----------------:|:-------------------:|
|  `UUID` (Standard-Implementierung)  |  2 184 584 ops/s  |      Referenz       |
| `KUID` (Optimierte Implementierung) | 223 345 730 ops/s | **~102x schneller** |

> *Kernaussage:*   
> Was im Code wie ein *kleines Detail* aussieht, kann unter realer Last den **Durchsatz um zwei Größenordnungen**
> beeinflussen.

### Testumgebung

Diese Ergebnisse stammen aus einem einmaligen reproduzierbaren Benchmark-Lauf unter **folgenden Bedingungen**:

|           Komponente           |                Wert                 |
|:------------------------------:|:-----------------------------------:|
|             Gerät              | MacBook Pro (2021) mit Apple M1 Pro |
|        Arbeitsspeicher         |                32 GB                |
|         Ausführung-Typ         |               Docker                |
| CPU-Limit vom Docker-Container |               4 Kerne               |
| RAM-Limit vom Docker-Container |                8 GB                 |
|          Java-Version          |          OpenJDK 1.8.0_121          |
|            Threads             |        16 parallele Threads         |

> *Hinweis:*  
> Die Ergebnisse stark von der jeweiligen Testumgebung (Hardware, Betriebssystem, JVM-Konfiguration, Testparameters
> usw.)
> abhängen und daher bei anderen Systemen deutlich abweichen können.  
> Der Abschnitt [Ausführung](#ausfuehrung) lässt sich nutzen, um das Projekt selbst aufzusetzen und eigene Ergebnisse zu
> erhalten.

## Interpretation

Die Analyse ergab, dass die Performance-Einbußen hauptsächlich durch die Verwendung von `java.util.UUID.randomUUID()`
verursacht wurden. Unter Java 8 greift dieser Mechanismus intern auf `SecureRandom` zurück, welches **synchronisiert**
ist. In hochgradig parallelisierten Umgebungen — wie sie bei API-Gateways üblich sind — führt dies zu *
*Thread-Blockierungen** und messbaren Verzögerungen.

> Auch mit dem Parameter `-Djava.security.egd=file:/dev/urandom` zeigte sich in meiner Umgebung weiterhin ein klarer
> Blockierungseffekt.

**Beobachtung während der Analyse:**

- Threads befanden sich wiederholt im Zustand **Blocked**
- Die Blockierung trat während der Entropie-Erzeugung innerhalb von `SecureRandom` auf
- Der Effekt war **reproduzierbar und messbar**: ca. 8 - 12% Durchsatzverlust in meinem Szenario

Während des Benchmarks mit **UUID** tritt nach einer gewissen Laufzeit vermehrt die Warnung:

> *WARNING:* Timestamp over-run: need to reinitialize random sequence auf.

Dieser Effekt könnte indirekt darauf hinweisen, dass die zugrunde liegende SecureRandom-Initialisierung zu Verzögerungen
bzw. Blockierungen führt.

**Verwendetes Werkzeug zur Identifikation:**  
Ich habe das Verhalten mit **JProfiler** untersucht und konnte dort die Blockierungsstellen eindeutig erkennen.
> *Blockierte Threads*  
> ![Thread-Blockierung](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439720.png)
> Die JProfiler-Aufnahme zeigt, dass mehrere Threads gleichzeitig auf denselben java.lang.Object-Monitor warten.   
> Dies bestätigt die durch Synchronisation verursachte Blockierung.

> *Stacktrace*
> ![Call-Duration](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439000.png)
> Innerhalb derselben Blockierung zeigt der Stacktrace, dass SecureRandom einen signifikanten Teil der Ausführungszeit
> beansprucht.
> Damit wird sichtbar, dass die UUID-Erzeugung selbst den Engpass verursacht.


Diese Erkenntnis zeigt, dass selbst ein scheinbar kleiner und oft übersehener Funktionsaufruf - **die UUID-Erzeugung** -
in Lastsituationen zu einem **nicht-trivialen System-Bottleneck** werden kann.

## Fazit

Dieser Benchmark macht deutlich, dass selbst weit verbreitete und vermeintlich neutrale Standardmechanismen
wie `UUID.randomUUID()` unter hoher Parallelität spürbare Auswirkungen auf die Systemleistung haben können.

Die Analyse im Produktionskontext sowie die reproduzierbare Demonstration in diesem Projekt zeigen vor allem zwei
Kernpunkte:

1. **Performance-Probleme entstehen oft an unerwarteten Stellen.** Ein kleines Detail in der Implementierung kann sich
   unter Last zu einem messbaren Bottleneck entwickeln.

2. **Gezielte Messung und Isolierung des Problems sind entscheidend.** Nur durch systematische Reproduktion, Beobachtung
   und Vergleich lässt sich eine fundierte Optimierungsentscheidung treffen.

Insgesamt unterstreicht dieses Projekt die Bedeutung von **Ursachenanalyse**, **Messbarkeit** und **bewusst gewählten
Implementierungsdetails** – besonders in Systemen, die hohen Durchsatz oder geringe Latenz erfordern.
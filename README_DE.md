# Benchmark: UUID unter Last – wie ein scheinbar kleines Detail zum System-Bottleneck wurde ⚡️🧩

[English](./README.md) | [简体中文](./README_ZH.md)

UUID wird in vielen Systemen ganz selbstverständlich verwendet – als **Request-Trace-ID**, als **Primärschlüssel in
Datenbanken** oder als **Identifier in verteilten Services**.

Dabei wird jedoch oft übersehen, dass **die Art der Erzeugung von UUIDs** unter hoher Parallelität **einen direkten
Einfluss** auf die Systemleistung hat. Insbesondere die Standardimplementierung kann unter Last zu **unerwarteten
Blockierungen** und **zusätzlicher Latenz** führen.

> Dieses Benchmark-Projekt zeigt, wie ein vermeintlich unkritischer Mechanismus unter hoher Last zu einem
> Performance-Engpass werden kann – und wie sich dieser systematisch analysieren und nachvollziehbar beheben lässt.

Zusätzlich existieren verschiedene leistungsoptimierte UUID-Implementierungen auf dem Markt. Um jedoch keine
zusätzlichen Bibliotheksabhängigkeiten einzuführen, habe ich eine eigene Variante umgesetzt und **KUID** genannt. Im
Folgenden wird dieser Begriff ohne weitere Erläuterung verwendet.

🔍 **Keine Zeit für Details?**  
[Direkt zu den Ergebnissen.](#ergebnisse)

🔧 **Projekt selbst ausführen?**  
[Zur Ausführung.](#ausführung)

## Hintergrund

**Spring Cloud Gateway** gilt grundsätzlich als eine **leistungsstarke Lösung** zur Weiterleitung und Verwaltung von
API-Anfragen. In einem meiner Projekte erhielt ich jedoch Rückmeldungen aus der Nutzung, dass ein darauf basierendes
Gateway unter Last lediglich **einige hundert Requests pro Sekunde** verarbeiten konnte.

Während der Performance-Analyse stellte sich heraus, dass die Standard-Implementierung zur **UUID-Generierung** in
bestimmten Szenarien einen unerwartet hohen Einfluss auf die Gesamtdurchsatzrate haben kann (in meinem Fall betrug der
Verlust etwa **10%**.).

> *Wichtig:*  
> Dieses Repository bildet den technischen Befund in stark reduzierter Form nach. Sämtliche internen oder sensiblen
> Inhalte wurden vollständig entfernt. Das Ziel ist eine klar isolierte und allgemein nachvollziehbare Darstellung.

Der vollständige Lösungsansatz sowie die Optimierungsmaßnahmen werden in einem separaten Projekt erläutert:
**[performance-test-example](https://github.com/ksewen/performance-test-example)** erläutert. Auch dort wurden
**sämtliche sensiblen Inhalte entfernt** oder **neutralisiert**.

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
> Die hier dargestellten Werte stammen aus einem isolierten Micro-Benchmark. Sie fallen deutlich
> höher aus als in einer realen Produktionsumgebung. Ziel des Benchmarks ist es, den Effekt klar und reproduzierbar
> sichtbar zu machen.

Die nachfolgende Messung zeigt zwei Implementierungen im direkten Vergleich. `UUID.randomUUID()` ist unter Java 8 durch
die synchronisierte `SecureRandom`-Instanz limitiert, während **KUID** eine
vorkonfigurierte, nicht blockierende Zufallsquelle verwendet.

![UUID vs KUID Benchmark](https://raw.githubusercontent.com/ksewen/Bilder/main/20251109184252140.png)

|               Methode               |     Durchsatz     |      Differenz      |
|:-----------------------------------:|:-----------------:|:-------------------:|
|  `UUID` (Standard-Implementierung)  |  2.184.584 ops/s  |      Referenz       |
| `KUID` (Optimierte Implementierung) | 223.345.730 ops/s | **~102x schneller** |

> *Kernaussage:*   
> Was im Code wie ein *kleines Detail* aussieht, kann unter realer Last den **Durchsatz um zwei Größenordnungen**
> beeinflussen.

### Testumgebung

Die unten aufgeführten Werte stammen aus einem reproduzierbaren Benchmark-Lauf unter **folgenden Rahmenbedingungen**:

|      Komponente       |                Wert                 |
|:---------------------:|:-----------------------------------:|
|         Gerät         | MacBook Pro (2021) mit Apple M1 Pro |
|    Arbeitsspeicher    |                32 GB                |
|  Ausführungsumgebung  |               Docker                |
| CPU-Limit (Container) |               4 Kerne               |
| RAM-Limit (Container) |                8 GB                 |
|     Java-Version      |          OpenJDK 1.8.0_121          |
|     Parallelität      |        16 parallele Threads         |

> *Hinweis:*  
> Die Ergebnisse stark von der jeweiligen Testumgebung (Hardware, Betriebssystem, JVM-Konfiguration, Testparameters
> usw.) abhängen.  
> Der Abschnitt [Ausführung](#ausfuehrung) lässt sich nutzen, um das Projekt selbst aufzusetzen und eigene Ergebnisse zu
> erhalten.

## Interpretation

Die Analyse zeigte, dass die beobachteten Performance-Verluste vor allem durch die Verwendung von
`java.util.UUID.randomUUID()` verursacht wurden. Unter Java 8 nutzt diese Methode intern eine synchronisierte
`SecureRandom`-Instanz. In stark parallelisierten Systemen – wie beispielsweise in API-Gateways – führt dies zu
**Thread-Blockierungen** und messbaren Verzögerungen.

> Auch mit dem Parameter `-Djava.security.egd=file:/dev/urandom` zeigte sich in meiner Umgebung weiterhin ein klarer
> Blockierungseffekt.

**Beobachtung während der Analyse:**

- Threads befanden sich wiederholt im Zustand **Blocked**
- Die Blockierung trat während der Entropie-Erzeugung innerhalb von `SecureRandom` auf
- Der Effekt war **reproduzierbar und messbar**: ca. 8 - 12% Durchsatzverlust in meinem Szenario

Während des Benchmarks mit **UUID** tritt nach einer gewissen Laufzeit vermehrt die Warnung:

> *WARNING:* Timestamp over-run: need to reinitialize random sequence auf.

Diese Warnung deutet darauf hin, dass die zugrunde liegende Initialisierung von `SecureRandom` unter Last erschöpft wird
und sich dadurch weitere Verzögerungen ergeben können.

**Zur Identifikation des Engpasses verwendetes Werkzeug:**  
Die Analyse wurde mit **JProfiler** durchgeführt. Dabei konnten die Blockierungen eindeutig sichtbar gemacht werden.

> *Blockierte Threads*  
> ![Thread-Blockierung](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439720.png)
> Die Aufnahme zeigt, dass mehrere Threads gleichzeitig auf denselben `java.lang.Object`-Monitor warten.    
> Dies bestätigt die durch Synchronisation verursachte Blockierung.

> *Stacktrace*
> ![Call-Duration](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439000.png)
> Innerhalb derselben Blockierung zeigt der Stacktrace, dass `SecureRandom` während der Blockierung einen erheblichen
> Teil der Ausführungszeit.  
> Damit wird klar erkennbar, dass die UUID-Erzeugung selbst den Engpass verursacht.


Diese Erkenntnis zeigt, dass selbst ein scheinbar kleiner und oft übersehener Funktionsaufruf - **die UUID-Erzeugung** -
in Lastsituationen zu einem **relevanten Performance-Bottleneck** werden kann.

## Fazit

Dieser Benchmark macht deutlich, dass selbst weit verbreitete und vermeintlich neutrale Standardmechanismen
wie `UUID.randomUUID()` unter hoher Parallelität spürbare Auswirkungen auf die Systemleistung haben können.

Die Analyse im realen Produktionsumfeld sowie die reproduzierbare Darstellung in diesem Projekt verdeutlichen vor allem
zwei Kernpunkte:

1. **Performance-Probleme entstehen oft an unerwarteten Stellen.** Ein kleines Detail in der Implementierung kann sich
   unter Last zu einem messbaren Engpass entwickeln.

2. **Gezielte Messung und klare Trennung des Problems sind entscheidend.**  
   Nur durch reproduzierbare Tests, systematische Beobachtung und strukturierten Vergleich lassen sich verlässliche und
   fundierte Optimierungsentscheidungen treffen.

Insgesamt unterstreicht dieses Projekt die Bedeutung von **Ursachenanalyse**, **Messbarkeit** und **bewusste Entscheidungen
bei Implementierungsdetails** – besonders in Systemen, die hohen Durchsatz oder geringe Latenz erfordern.

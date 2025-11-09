# Benchmark: UUID Under Load – How a Small Detail Became a System Bottleneck ⚡️🧩

[Deutsch](./README_DE.md) | [简体中文](./README_ZH.md)

UUID is commonly used in many systems – as a **request trace ID**, as a **primary key in databases**, or as an *
*identifier in distributed services**.

However, it is often overlooked that **the way UUIDs are generated** can have a **direct effect on performance under
high parallel load**. When UUIDs are generated using the standard implementation, this can lead to **unexpected blocking
** and **measurable latency**.

> This benchmark project shows how a small and harmless-looking mechanism can turn into a performance bottleneck under
> load – and how the problem can be analyzed and solved step by step.

There are different high‑performance UUID implementations available. To keep this project simple and without extra
dependencies, I created my own version called **KUID**. The name will be used directly throughout the document.

🔍 **No time for details?**  
[Jump directly to the results.](#results)

🔧 **Want to run the project yourself?**  
[Jump to the execution section.](#execution)

## Background

**Spring Cloud Gateway** is usually considered a **high‑performance solution** for routing and managing API requests.
However, in one of my projects, a customer reported that a gateway based on Spring Cloud Gateway could only process **a
few hundred requests per second** under load.

During the performance analysis, I found that the **standard UUID generation** had an unexpected effect on system
throughput (in my case around **10% performance loss**).

> *Important:*  
> This repository is a minimal and clean reproduction of the issue. All internal or confidential information has been
> removed.

The full analysis and optimization can be found in my other project: *
*[performance-test-example](https://github.com/ksewen/performance-test-example)**.  
All sensitive information has also been removed there.

## Execution

### Run Locally

#### Requirements:

- **Java 8** or higher
- **Maven 3.6.0** or higher

#### Clone the repository:

```bash
git clone git@github.com:ksewen/uuid-benchmark.git
```

#### Build the project:

```bash
mvn clean package
```

The executable JAR will be located at:

```bash
./target/uuid-benchmark.jar
```

#### Start

```bash
java -jar ./target/uuid-benchmark.jar
```

Example input:

```bash
please input the benchmark type UUID/KUID: 
# Supported values: UUID or KUID
UUID

# Press Enter or use the default: benchmark-{type}-thread-{thread-counts}.log
please input the output file: 
/root/benchmark/benchmark-UUID-thread-8.log

# Supported: positive integers, default = 1
please input the thread count: 
8
```

### Run with Docker

#### Build Docker image:

```bash
resources/scripts/build-image.sh -d ..
```

#### Start container:

```bash
docker run -d ksewen/uuid-benchmark:1.0
```

#### Enter container:

```bash
docker exec -it ${container-id} /bin/sh
```

#### Execute benchmark:

```bash
java -jar uuid-benchmark.jar
```

The required inputs are explained in the section [Start](#start).

## Results

The following measurement shows an example of a load test.  
It shows clearly that different strategies for generating UUIDs can lead to **very different throughput levels**.

> *Important:*  
> The differences shown in this benchmark are micro-benchmark results. They are much higher than what would appear in a
> real gateway production environment. This benchmark is used to make the performance effect easy to see.

The next measurement compares two implementations directly.  
While `UUID.randomUUID()` in Java 8 is limited by the synchronized `SecureRandom` instance, **KUID** uses a non-blocking
random source.

![UUID vs KUID Benchmark](https://raw.githubusercontent.com/ksewen/Bilder/main/20251109184252140.png)

|              Method               |    Throughput     |    Difference    |
|:---------------------------------:|:-----------------:|:----------------:|
| `UUID` (standard implementation)  |  2,184,584 ops/s  |    Reference     |
| `KUID` (optimized implementation) | 223,345,730 ops/s | **~102x faster** |

> *Key Point:*  
> A detail that looks *small* in code can have a **huge impact** under real load.

### Test Environment

These results come from a single reproducible benchmark run under the **following conditions**:

|       Component        |              Value               |
|:----------------------:|:--------------------------------:|
|         Device         | MacBook Pro (2021), Apple M1 Pro |
|         Memory         |              32 GB               |
|    Execution Method    |              Docker              |
| CPU Limit in Container |             4 Cores              |
| RAM Limit in Container |               8 GB               |
|      Java Version      |        OpenJDK 1.8.0_121         |
|        Threads         |       16 parallel threads        |

> *Note:*  
> Results may change depending on hardware, system settings, JVM configuration, and benchmark parameters etc.  
> You can set up the project yourself and run your own benchmark using the section [Execution](#execution).

## Interpretation

The analysis showed that the performance loss was mainly caused by `java.util.UUID.randomUUID()`. In Java 8, this method
uses `SecureRandom` internally, which is **synchronized**. In highly parallel environments — like API gateways — this
causes **thread blocking** and performance delay.

> Even when using the parameter `-Djava.security.egd=file:/dev/urandom`, the blocking effect was still visible in my
> environment.

**Observed during analysis:**

- Threads stayed often in the **Blocked** state
- The blocking happened inside the entropy generation of `SecureRandom`
- The effect was **reproducible and measurable**: about **8–12% throughput loss** in my case

During the benchmark with **UUID**, after some time a lot of warning appeared:

> *WARNING:* Timestamp over-run: need to reinitialize random sequence

This warning suggests that `SecureRandom` may cause delays or blocking in some conditions.

**Tool used in the analysis:**  

I used **JProfiler** to observe the behavior and could clearly see the blocking points.

> **Blocked Threads**  
> ![Thread-Blockierung](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439720.png)  
> Multiple threads wait on the same java.lang.Object monitor.  
> This confirms the blocking caused by synchronization.

> **Stacktrace**  
> ![Call-Duration](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439000.png)  
> Inside the same blocking, the stacktrace shows that `SecureRandom` takes a noticeable amount of execution time.  
> This makes the UUID generation the actual bottleneck.

This shows that a function that looks small and harmless — **UUID generation** — can become a **real system bottleneck**
under load.

## Conclusion

This benchmark demonstrates that even widely used and common mechanisms like `UUID.randomUUID()` can have a strong
effect on system performance under high concurrency.

The production analysis and the benchmark in this project highlight two main points:

1. **Performance problems often appear in places we do not expect.** A small detail can become a measurable bottleneck under load.

2. **Careful measurement and isolation of the problem are essential.** Only by reproducing, observing, and comparing, we can make a well-reasoned decision on optimization.

Overall, this project shows the importance of **root cause analysis**, **measurability**, and **careful implementation
details** — especially in systems that require high throughput or low latency.

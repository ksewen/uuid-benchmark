# Benchmark: UUID under Load – How a Small Detail Became a System Bottleneck ⚡️🧩

[Deutsch](./README_DE.md) | [简体中文](./README_ZH.md)

UUIDs are commonly used in many systems – as **request trace IDs**, **primary keys in databases**, or **identifiers in
distributed services**.

However, it is often overlooked that **the method used to generate UUIDs** can have a **direct impact on system
performance** under high parallelism. When UUIDs are generated through standard implementations, this may lead to *
*unexpected blocking** and **measurable latency**.

> This benchmark project demonstrates how a seemingly harmless mechanism can become a performance bottleneck under
> load – and how the issue can be systematically analyzed and resolved.

Additionally, various high-performance UUID implementations exist. To avoid introducing additional library dependencies,
I implemented my own optimized version, called **KUID**. This term is used below without further elaboration.

🔍 **No time for details?**  
[Jump directly to the results.](#results)

🔧 **Want to run the project yourself?**  
[Jump to the execution section.](#execution)

## Background

**Spring Cloud Gateway** is generally considered a **high-performance solution** for routing and managing API requests.
However, in one of my projects, customers reported that a gateway based on Spring Cloud Gateway could handle only **a
few hundred requests per second** under load.

During the performance analysis, it became evident that the standard implementation of **UUID generation** could have an
unexpectedly strong impact on throughput in certain scenarios (in my case, around **10% performance loss**).

> *Important:*  
> This repository is a deliberately minimized and fully sanitized reproduction of the finding. The goal is to clearly
> isolate and demonstrate the root cause.

The full resolution of the original issue and the corresponding optimization steps are described in my separate
project:  
**https://github.com/ksewen/performance-test-example**  
All sensitive information has also been removed there.

## Execution

### Local Execution

#### Requirements:

- **Java 8** or higher
- **Maven 3.6.0** or higher

#### Clone the repository

```bash
git clone git@github.com:ksewen/uuid-benchmark.git
```

#### Build the project

```bash
mvn clean package
```

The executable JAR will be located at:

```bash
./target/uuid-benchmark.jar
```

#### Start the benchmark

```bash
java -jar ./target/uuid-benchmark.jar
```

During execution, the following inputs will be requested:

```bash
please input the benchmark type UUID/KUID: 
# Supported values: UUID or KUID
UUID

# Press Enter for default file: benchmark-{type}-thread-{thread-counts}.log
please input the output file: 
/root/benchmark/benchmark-UUID-thread-8.log

# Supported: positive integers, default = 1
please input the thread count: 
8
```

### Running via Docker

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

Necessary runtime inputs are described in the [Start](#start-the-benchmark) section.

## Results

The following measurement represents an example benchmark run.  
It shows that different UUID generation strategies can vary by **orders of magnitude** in throughput.

> *Important:*  
> The large differences shown here are **benchmark** results. In a real gateway system, the performance delta is
> smaller; the benchmark isolates the effect for clarity.

Comparison of two implementations:  
While `UUID.randomUUID()` in Java 8 uses a **synchronized SecureRandom**, **KUID** uses a preconfigured **non-blocking**
random source.

![UUID vs KUID Benchmark](https://raw.githubusercontent.com/ksewen/Bilder/main/20251109184252140.png)

|              Method               |    Throughput     | Relative Performance |
|:---------------------------------:|:-----------------:|:--------------------:|
| `UUID` (Standard Implementation)  |  2,184,584 ops/s  |      Reference       |
| `KUID` (Optimized Implementation) | 223,345,730 ops/s |   **~102x faster**   |

> *Key insight:*  
> What may appear to be a *small detail* can affect system throughput by **two orders of magnitude** under real load.

### Test Environment

|       Component       |              Value               |
|:---------------------:|:--------------------------------:|
|        Device         | MacBook Pro (2021), Apple M1 Pro |
|          RAM          |              32 GB               |
|    Execution type     |         Docker container         |
| CPU limit (container) |             4 cores              |
| RAM limit (container) |               8 GB               |
|     Java version      |        OpenJDK 1.8.0_121         |
|        Threads        |       16 parallel threads        |

> *Note:*  
> Performance results vary depending on system environment (hardware, OS, JVM configuration, test parameters etc.).  
> Use the [Execution](#execution) section to run your own benchmark.

## Interpretation

The performance degradation was mainly caused by `java.util.UUID.randomUUID()`.  
In Java 8, this implementation internally uses `SecureRandom`, which is **synchronized**. In highly parallel
environments — such as API gateways — this leads to **thread blocking** and measurable throughput loss.

> Even with `-Djava.security.egd=file:/dev/urandom`, blocking remained clearly visible in my environment.

Observed during analysis:

- Threads repeatedly entered a **Blocked** state
- Blocking occurred during entropy generation within `SecureRandom`
- The effect was **reproducible and measurable** (approx. 8–12% throughput loss in real workload)

During UUID benchmarks, repeated warnings appeared:

> *WARNING:* Timestamp over-run: need to reinitialize random sequence

This suggests internal reset operations in the random source, indicating contention.

### Profiling Evidence (JProfiler)

**Thread Blocking**
![Thread Blocking](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439720.png)

> Multiple threads wait on the same monitor object — confirming synchronization-related blocking.

**Stacktrace**
![Call Duration](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439000.png)

> SecureRandom consumes significant execution time — identifying UUID generation as the bottleneck.

These observations show how a **small and easily overlooked part of the system** — UUID generation — can become a *
*non-trivial bottleneck** under high parallelism.

## Conclusion

This benchmark demonstrates that even widely used mechanisms like `UUID.randomUUID()` can have **significant performance
impact** under parallel load.

Key lessons:

1. **Performance issues often originate in unexpected places.**  
   Small implementation details can scale into critical bottlenecks.

2. **Systematic measurement and isolation are essential.**  
   Only controlled benchmarking and profiling reveal the true cause.

This project highlights the importance of **root cause analysis**, **measurability**, and **intentional implementation
choices** — especially in high-throughput or latency-sensitive systems.

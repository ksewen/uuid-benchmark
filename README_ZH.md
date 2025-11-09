# 基准测试：高并发下的 UUID —— 一个看似微小的细节如何成为系统瓶颈 ⚡️🧩

[English](./README.md) | [Deutsch](./README_DE.md)

在许多系统中，UUID 被理所当然地使用，例如作为 **请求追踪 ID**、**数据库主键**，或者作为 **分布式服务中的标识符**。

然而，人们往往忽略了：在高并发场景中，**UUID 的生成方式** 会对系统性能产生 **直接影响**。尤其是当 UUID 使用标准实现生成时，可能会导致
**意外的阻塞** 和 **可测量的延迟**。

> 本基准测试项目展示了一个看似无害的机制，在压力下如何演变为性能瓶颈，以及如何系统性地分析和解决这个问题。

此外，市面上存在多种高性能的 UUID 实现方案。为了不引入额外的库依赖，我实现了一个自己的版本，并将其命名为 **KUID**
。以下内容中将直接使用该术语，不再重复解释。

🔍 **不关心细节？**  
[点击查看测试结果](#测试结果)

🔧 **想自己试试？**  
[点击查看如何运行](#运行方法)

## 背景

**Spring Cloud Gateway** 被认为是一种 **高性能** 的 API 路由与管理方案。然而在一个真实项目中，我收到客户反馈：某个基于
Spring Cloud Gateway 的网关在高负载下 **每秒只能处理数百个请求**。

在性能分析过程中发现，标准 UUID 生成方式在特定场景下会对总体吞吐量产生 **意外且显著的影响**（在我的案例中约 **10% 性能损耗
**）。

> *重要提示：*  
> 本仓库是一份最小化的，完全脱敏且不包含商业机密的可执行复现案例。其目的仅为清晰、可验证地展示该问题的根因。

原始问题的完整解决方案及优化过程，将在我的另一个项目中详细说明：  
**[performance-test-example](https://github.com/ksewen/performance-test-example)**  
该项目同样完全脱敏且不包含任何商业秘密。

## 运行方法

### 本地运行

#### 环境要求

- **Java 8** 或更高版本
- **Maven 3.6.0** 或更高版本

#### 克隆仓库

```bash
git clone git@github.com:ksewen/uuid-benchmark.git
```

#### 构建项目

```bash
mvn clean package
```

构建完成的 JAR 文件路径：

```bash
./target/uuid-benchmark.jar
```

#### 运行基准测试

```bash
java -jar ./target/uuid-benchmark.jar
```

运行过程中会提示输入以下信息：

```text
please input the benchmark type UUID/KUID: 
# 可选值: UUID or KUID
UUID

# 输入或使用默认文件名: benchmark-{type}-thread-{thread-counts}.log
please input the output file: 
/root/benchmark/benchmark-UUID-thread-8.log

# 请输入正整数，默认值为 1
please input the thread count: 
8
```

### 使用 Docker 运行

#### 构建镜像

```bash
resources/scripts/build-image.sh -d ..
```

#### 启动容器

```bash
docker run -d ksewen/uuid-benchmark:1.0
```

#### 进入容器

```bash
docker exec -it ${container-id} /bin/sh
```

#### 运行测试

```bash
java -jar uuid-benchmark.jar
```

## 测试结果

下表展示了不同 UUID 生成策略在高并发下的吞吐量差异：

|      方法      |        吞吐量        |     对比提升      |
|:------------:|:-----------------:|:-------------:|
| `UUID`（标准实现） |  2 184 584 ops/s  |      基准       |
| `KUID`（优化实现） | 223 345 730 ops/s | **约 102 倍提升** |

![UUID vs KUID Benchmark](https://raw.githubusercontent.com/ksewen/Bilder/main/20251109184252140.png)

> *结论：*  
> 一个看似微不足道的细节，在真实高并发场景下，可能导致 **两个数量级的性能差异**。

### 测试环境

| 组件       | 参数                              |
|----------|---------------------------------|
| 设备       | MacBook Pro (2021) Apple M1 Pro |
| 内存       | 32 GB                           |
| 执行方式     | Docker                          |
| 容器分配 CPU | 4 核                             |
| 容器分配 RAM | 8 GB                            |
| Java 版本  | OpenJDK 1.8.0_121               |
| 并发线程数    | 16                              |

> *注意：*  
> 硬件、JVM 配置、系统环境会显著影响结果。  
> 建议根据本项目自行测试获取结果。

## 结果解读

在 Java 8 中，`java.util.UUID.randomUUID()` 内部依赖 **同步的 SecureRandom 实例**。在多线程环境中，这会导致：

- 线程竞争
- 可见的阻塞
- 可测量的吞吐量下降（约 8% - 12%）

> 即使加上：`-Djava.security.egd=file:/dev/urandom`，在我的测试环境中，阻塞依然存在。

在运行 UUID 基准测试时，会出现大量警告：

> *WARNING:* Timestamp over-run: need to reinitialize random sequence

这进一步说明了随机数初始化的过程中存在阻塞。

我使用 **JProfiler** 对该问题进行了验证，分析截图中可以看到：

**线程阻塞（Blocked Threads）：**
![Thread-Blockierung](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439720.png)
> JProfiler 的记录显示，多个线程同时在等待获取同一个 java.lang.Object 持有的锁。  
> 这印证了系统中存在由同步机制导致的阻塞。

**调用栈（Stacktrace）：**
![Call-Duration](https://raw.githubusercontent.com/ksewen/Bilder/main/202308201439000.png)
> 调用栈显示 SecureRandom 占用了执行过程中的大部分时间。
> 这表明瓶颈确实存在于 UUID 的生成过程。

## 总结

这个基准测试清晰地展示了，即使是像 `UUID.randomUUID()` 这样被广泛使用、看似中性的标准机制，在高并发环境下也可能对系统性能产生明显影响。  

无论是在生产环境中的性能问题分析，还是在本项目中的可重复演示，都突出了两个关键点：
1.	性能问题的来源往往令人意想不到。一个实现在低负载下可能无关紧要，但在高负载下可能发展为明显的性能瓶颈。
2.	有针对性的测量与隔离复杂问题的不同层级至关重要。只有通过重现、观察与对比，才能找到导致性能问题的原因，并针对性优化。

最后，这个项目强调了在高吞吐，低延迟的场景中，**分析复杂问题**、**可观测性**以及**了解组件的底层实现**非常重要。
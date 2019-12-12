package com.github.ksewen.uuid.benchmark;

import com.github.ksewen.uuid.generator.CustomerRandomBaseGenerator;
import com.github.ksewen.uuid.kuid.KUID;
import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author ksewen
 * @date 2019-12-1216:14
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Fork(value = 1, jvmArgs = {"-server", "-Xms2048m", "-Xmx2048m", "-Xmn1600m", "-XX:MaxMetaspaceSize=256m", "-Xss256k", "-XX:+UseConcMarkSweepGC", "-XX:CMSInitiatingOccupancyFraction=80", "-XX:+UseCMSInitiatingOccupancyOnly", "-XX:AutoBoxCacheMax=20000", "-XX:-OmitStackTraceInFastThrow", "-Djava.security.egd=file:/dev/./urandom"})
public class KUIDBenchmark {

    private CustomerRandomBaseGenerator customerRandomBaseGenerator;

    @Setup
    public void init() {
        customerRandomBaseGenerator = new CustomerRandomBaseGenerator();
    }

    @Benchmark
    public void jugWithCustomerRandom(Blackhole bh) {
        bh.consume(customerRandomBaseGenerator.generate(ThreadLocalRandom.current()));
    }

    @Benchmark
    public void jugWithCustomerRandomToString(Blackhole bh) {
        bh.consume(customerRandomBaseGenerator.generate(ThreadLocalRandom.current()).toString());
    }

    @Benchmark
    public void jugWithCustomerRandomToFormatString(Blackhole bh) {
        bh.consume(StringUtils.replaceAll(customerRandomBaseGenerator.generate(ThreadLocalRandom.current()).toString(), "-", ""));
    }

    @Benchmark
    public void jugWithCustomerRandomKUIDToString(Blackhole bh) {
        bh.consume(KUID.randomKUID(ThreadLocalRandom.current()).toString());
    }
}

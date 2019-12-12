package com.github.ksewen.uuid.benchmark;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.github.ksewen.uuid.generator.CustomerRandomBaseGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author ksewen
 * @date 2019-12-1016:14
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Fork(value = 1, jvmArgs = {"-server", "-Xms2048m", "-Xmx2048m", "-Xmn1600m", "-XX:MaxMetaspaceSize=256m", "-Xss256k", "-XX:+UseConcMarkSweepGC", "-XX:CMSInitiatingOccupancyFraction=80", "-XX:+UseCMSInitiatingOccupancyOnly", "-XX:AutoBoxCacheMax=20000", "-XX:-OmitStackTraceInFastThrow", "-Djava.security.egd=file:/dev/./urandom"})
public class UUIDBenchmark {

    private RandomBasedGenerator randomBasedGenerator;
    private RandomBasedGenerator jugRandomGenerator;
    private CustomerRandomBaseGenerator customerRandomBaseGenerator;
    private TimeBasedGenerator timeBasedGenerator;

    @Setup
    public void init() {
        randomBasedGenerator = Generators.randomBasedGenerator();
        timeBasedGenerator = Generators.timeBasedGenerator();
        jugRandomGenerator = Generators.randomBasedGenerator(new Random());
        customerRandomBaseGenerator = new CustomerRandomBaseGenerator();
    }

    @Benchmark
    public void UUIDRandomUUID(Blackhole bh) {
        bh.consume(UUID.randomUUID());
    }

    @Benchmark
    public void jugWithRandom(Blackhole bh) {
        bh.consume(jugRandomGenerator.generate());
    }

    @Benchmark
    public void jugWithSecureRandom(Blackhole bh) {
        bh.consume(randomBasedGenerator.generate());
    }

    @Benchmark
    public void jugWithCustomerRandom(Blackhole bh) {
        bh.consume(customerRandomBaseGenerator.generate(ThreadLocalRandom.current()));
    }

    @Benchmark
    public void jugTime(Blackhole bh) {
        bh.consume(timeBasedGenerator.generate());
    }

}

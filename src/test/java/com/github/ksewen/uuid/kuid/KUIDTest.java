package com.github.ksewen.uuid.kuid;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ksewen
 * @date 2019-12-1213:57
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class KUIDTest {

    @Test
    public void test() {
        KUID tuid = KUID.randomKUID(ThreadLocalRandom.current());
        System.out.println(tuid.toString());
    }

    @Test
    public void bloomTest() {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000);
        int repeat = 0;
        for (int i = 0; i < 10000000; i++) {
            String s = KUID.randomKUID(ThreadLocalRandom.current()).toString();
            if (bloomFilter.mightContain(s)) {
                repeat ++;
            }
            bloomFilter.put(s);
        }
        System.err.println(repeat);
    }

    @Test
    public void bloomTestMultiThread() throws ExecutionException, InterruptedException, TimeoutException {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000);
        final AtomicInteger repeat = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 1, TimeUnit.MINUTES, new ArrayBlockingQueue(1));

        Future<?> task1 = executor.submit(new SubTask(bloomFilter, repeat));
        Future<?> task2 = executor.submit(new SubTask(bloomFilter, repeat));
        Future<?> task3 = executor.submit(new SubTask(bloomFilter, repeat));
        Future<?> task4 = executor.submit(new SubTask(bloomFilter, repeat));

        task1.get(120, TimeUnit.SECONDS);
        task2.get(120, TimeUnit.SECONDS);
        task3.get(120, TimeUnit.SECONDS);
        task4.get(120, TimeUnit.SECONDS);

        System.err.println(repeat.intValue());
    }

    @Test
    public void bloomTestUUID() {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000);
        int repeat = 0;
        for (int i = 0; i < 10000000; i++) {
            String s = UUID.randomUUID().toString();
            if (bloomFilter.mightContain(s)) {
                repeat ++;
            }
            bloomFilter.put(s);
        }
        System.err.println(repeat);
    }

    class SubTask implements Callable<Boolean> {

        private BloomFilter<CharSequence> bloomFilter;
        private AtomicInteger repeat;

        public SubTask(BloomFilter<CharSequence> bloomFilter, AtomicInteger repeat) {
            this.bloomFilter = bloomFilter;
            this.repeat = repeat;
        }

        @Override
        public Boolean call() throws Exception {
            for (int i = 0; i < 2500000; i++) {
                String s = KUID.randomKUID(ThreadLocalRandom.current()).toString();
                if (bloomFilter.mightContain(s)) {
                    repeat.getAndIncrement();
                }
                bloomFilter.put(s);
            }
            return true;
        }
    }
}

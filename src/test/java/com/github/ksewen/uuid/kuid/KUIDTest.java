package com.github.ksewen.uuid.kuid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.concurrent.ThreadLocalRandom;

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
}

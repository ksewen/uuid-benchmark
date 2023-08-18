package com.github.ksewen.uuid.benchmark;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.github.ksewen.uuid.generator.CustomerRandomBaseGenerator;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * @author ksewen
 * @date 2019-12-1019:17
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class UUIDTest {

  private RandomBasedGenerator jugThreadLocalRandomGenerator;
  private CustomerRandomBaseGenerator customerRandomBaseGenerator;

  @Before
  public void setUp() {
    jugThreadLocalRandomGenerator = Generators.randomBasedGenerator(ThreadLocalRandom.current());
    customerRandomBaseGenerator = new CustomerRandomBaseGenerator();
  }

  @Test
  public void nameUUID() {
    byte[] name = "test".getBytes();
    System.out.println(UUID.nameUUIDFromBytes(name));
  }

  @Test
  public void testThreadLocalRandom() {
    for (int i = 0; i < 10; i++) {
      new Thread(() -> System.out.println(jugThreadLocalRandomGenerator.generate())).start();
    }
  }

  @Test
  public void testCustomerThreadLocalRandom() {
    for (int i = 0; i < 10; i++) {
      new Thread(
              () ->
                  System.out.println(
                      customerRandomBaseGenerator.generate(ThreadLocalRandom.current())))
          .start();
    }
  }

  @Test
  public void testCustomerThreadLocalRandom1() {
    System.out.println(customerRandomBaseGenerator.generate(ThreadLocalRandom.current()));
  }
}

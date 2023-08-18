package com.github.ksewen.uuid.generator;

import com.fasterxml.uuid.UUIDType;
import com.fasterxml.uuid.impl.UUIDUtil;
import java.util.Random;
import java.util.UUID;

/**
 * @author ksewen
 * @date 2019-12-1020:47
 */
public class CustomerRandomBaseGenerator {

  public UUID generate(Random random) {
    /* 14-Oct-2010, tatu: Surprisingly, variant for reading byte array is
     *   tad faster for SecureRandom... so let's use that then
     */
    long r1, r2;
    r1 = random.nextLong();
    r2 = random.nextLong();
    return UUIDUtil.constructUUID(UUIDType.RANDOM_BASED, r1, r2);
  }
}

package com.github.ksewen.uuid.util;

import com.fasterxml.uuid.UUIDType;
import com.github.ksewen.uuid.kuid.KUID;

/**
 * @author ksewen
 * @date 2019-12-1214:02
 */
public class TUIDUtils {

    public static KUID constructTUID(UUIDType type, long l1, long l2)
    {
        // first, ensure type is ok
        l1 &= ~0xF000L; // remove high nibble of 6th byte
        l1 |= (long) (type.raw() << 12);
        // second, ensure variant is properly set too (8th byte; most-sig byte of second long)
        l2 = ((l2 << 2) >>> 2); // remove 2 MSB
        l2 |= (2L << 62); // set 2 MSB to '10'
        return new KUID(l1, l2);
    }
}

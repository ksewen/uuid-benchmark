package com.github.ksewen.uuid.kuid;

/**
 * @author ksewen
 * @date 2019-12-1213:39
 */

import java.io.Serializable;
import java.util.Random;

/**
 * A class that represents an immutable universally unique identifier edit by Ksewen (KUID).
 * A KUID represents a 128-bit value.
 */
public final class KUID implements Serializable, Comparable<KUID> {

    private static final long serialVersionUID = -4856846361793249489L;

    /*
     * The most significant 64 bits of this KUID.
     *
     * @serial
     */
    private final long mostSigBits;

    /*
     * The least significant 64 bits of this KUID.
     *
     * @serial
     */
    private final long leastSigBits;


    private static final int NUM_ALPHA_DIFF = 'A' - '9' - 1;
    private static final int LOWER_UPPER_DIFF = 'a' - 'A';

    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    /**
     * Constructs a new {@code KUID} using the specified data.  {@code
     * mostSigBits} is used for the most significant 64 bits of the {@code
     * KUID} and {@code leastSigBits} becomes the least significant 64 bits of
     * the {@code KUID}.
     *
     * @param mostSigBits  The most significant bits of the {@code KUID}
     * @param leastSigBits The least significant bits of the {@code KUID}
     */
    public KUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) KUID.
     * <p>
     * The {@code KUID} is generated using a cryptographically strong pseudo
     * random number generator.
     *
     * @param random
     * @return A randomly generated {@code KUID}
     */
    public static KUID randomKUID(Random random) {
        int version = 4;
        long l1 = random.nextLong();
        long l2 = random.nextLong();
        // remove high nibble of 6th byte
        l1 &= ~0xF000L;
        l1 |= (long) (version << 12);
        // second, ensure variant is properly set too (8th byte; most-sig byte of second long)
        // remove 2 MSB
        l2 = ((l2 << 2) >>> 2);
        // set 2 MSB to '10'
        l2 |= (2L << 62);
        return new KUID(l1, l2);
    }

    // Field Accessor Methods

    /**
     * Returns the least significant 64 bits of this KUID's 128 bit value.
     *
     * @return The least significant 64 bits of this KUID's 128 bit value
     */
    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    /**
     * Returns the most significant 64 bits of this KUID's 128 bit value.
     *
     * @return The most significant 64 bits of this KUID's 128 bit value
     */
    public long getMostSignificantBits() {
        return mostSigBits;
    }

    /**
     * The version number associated with this {@code KUID}.  The version
     * number describes how this {@code KUID} was generated.
     * <p>
     * The version number has the following meaning:
     * <ul>
     * <li>1    Time-based KUID
     * <li>2    DCE security KUID
     * <li>3    Name-based KUID
     * <li>4    Randomly generated KUID
     * </ul>
     *
     * @return The version number of this {@code KUID}
     */
    public int version() {
        // Version is bits masked by 0x000000000000F000 in MS long
        return (int) ((mostSigBits >> 12) & 0x0f);
    }

    // Object Inherited Methods

    /**
     * Returns a {@code String} object representing this {@code KUID}.
     *
     * <p> The KUID string representation is as described by this BNF:
     * <blockquote><pre>
     * {@code
     * KUID                   = <time_low> "-" <time_mid> "-"
     *                          <time_high_and_version> "-"
     *                          <variant_and_sequence> "-"
     *                          <node>
     * time_low               = 4*<hexOctet>
     * time_mid               = 2*<hexOctet>
     * time_high_and_version  = 2*<hexOctet>
     * variant_and_sequence   = 2*<hexOctet>
     * node                   = 6*<hexOctet>
     * hexOctet               = <hexDigit><hexDigit>
     * hexDigit               =
     *       "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
     *       | "a" | "b" | "c" | "d" | "e" | "f"
     *       | "A" | "B" | "C" | "D" | "E" | "F"
     * }</pre></blockquote>
     *
     * @return A string representation of this {@code KUID}
     */
    @Override
    public String toString() {
        return toString(getMostSignificantBits(), getLeastSignificantBits());
    }

    public static String toString(long msb, long lsb) {
        char[] kuidChars = new char[32];

        hexDigits(kuidChars, 0, 8, msb >> 32);
        hexDigits(kuidChars, 8, 4, msb >> 16);
        hexDigits(kuidChars, 12, 4, msb);
        hexDigits(kuidChars, 16, 4, lsb >> 48);
        hexDigits(kuidChars, 20, 12, lsb);

        return new String(kuidChars);
    }

    private static void hexDigits(char[] dest, int offset, int digits, long val) {
        long hi = 1L << (digits * 4);
        toUnsignedString(dest, offset, digits, hi | (val & (hi - 1)), 4);
    }

    private static void toUnsignedString(char[] dest, int offset, int len, long i, int shift) {
        int charPos = len;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            dest[offset + --charPos] = HEX_DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0 && charPos > 0);
    }

    /**
     * Returns val represented by the specified number of hex digits.
     */
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    /**
     * Returns a hash code for this {@code KUID}.
     *
     * @return A hash code value for this {@code KUID}
     */
    @Override
    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    /**
     * Compares this object to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null}, is a {@code KUID}
     * object, has the same variant, and contains the same value, bit for bit,
     * as this {@code KUID}.
     *
     * @param obj The object to be compared
     * @return {@code true} if the objects are the same; {@code false}
     * otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if ((null == obj) || (obj.getClass() != KUID.class)) {
            return false;
        }
        KUID id = (KUID) obj;
        return (mostSigBits == id.mostSigBits &&
                leastSigBits == id.leastSigBits);
    }

    // Comparison Operations

    /**
     * Compares this KUID with the specified KUID.
     *
     * <p> The first of two KUIDs is greater than the second if the most
     * significant field in which the KUIDs differ is greater for the first
     * KUID.
     *
     * @param val {@code KUID} to which this {@code KUID} is to be compared
     * @return -1, 0 or 1 as this {@code KUID} is less than, equal to, or
     * greater than {@code val}
     */
    @Override
    public int compareTo(KUID val) {
        // The ordering is intentionally set up so that the KUIDs
        // can simply be numerically compared as two numbers
        return (this.mostSigBits < val.mostSigBits ? -1 :
                (this.mostSigBits > val.mostSigBits ? 1 :
                        (this.leastSigBits < val.leastSigBits ? -1 :
                                (this.leastSigBits > val.leastSigBits ? 1 :
                                        0))));
    }
}


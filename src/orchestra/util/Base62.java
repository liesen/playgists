package orchestra.util;

import java.math.BigInteger;
import static java.util.Arrays.binarySearch;;

public class Base62 {
  private static final BigInteger BASE = BigInteger.valueOf(62);

  private static final byte[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

  private static final byte[] LOWERCASE_LETTERS =
      {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
          's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

  private static final byte[] UPPERCASE_LETTERS =
      {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
          'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

  // Index of the first lower-case letter
  private static final int LOWERCASE_LETTER_LOW_INDEX = DIGITS.length;

  // Index of the first upper-case letter
  private static final int UPPERCASE_LETTER_LOW_INDEX =
      LOWERCASE_LETTER_LOW_INDEX + LOWERCASE_LETTERS.length;

  /**
   * Returns the index of a byte in the alphabet.
   * 
   * @param key element to search for
   * @return index of key in alphabet
   */
  private static final int getValueForByte(byte key) {
    int index;
    
    // Try letters first since they are more likely to occur (assuming an even
    // distribution).
    if ((index = binarySearch(LOWERCASE_LETTERS, key)) >= 0) {
      return LOWERCASE_LETTER_LOW_INDEX + index;
    }

    if ((index = binarySearch(UPPERCASE_LETTERS, key)) >= 0) {
      return UPPERCASE_LETTER_LOW_INDEX + index;
    }

    return binarySearch(DIGITS, key);
  }

  /**
   * Convert a base-62 string known to be a number.
   * 
   * @param s
   * @return
   */
  public static BigInteger decodeBigInteger(String s) {
    return decodeBigInteger(s.getBytes());
  }

  /**
   * Convert a base-62 string known to be a number.
   * 
   * @param s
   * @return
   */
  public static BigInteger decodeBigInteger(byte[] bytes) {
    BigInteger res = BigInteger.ZERO;
    BigInteger multiplier = BigInteger.ONE;

    for (int i = bytes.length - 1; i >= 0; i--) {
      res = res.add(multiplier.multiply(BigInteger.valueOf(getValueForByte(bytes[i]))));
      multiplier = multiplier.multiply(BASE);
    }

    return res;
  }
}

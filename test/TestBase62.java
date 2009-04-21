

import java.math.BigInteger;

import orchestra.util.Base62;

import org.testng.annotations.Test;

public class TestBase62 {
  private static String decodeEncodeInteger(String s) {
    return Base62.encode(Base62.decodeBigInteger(s));
  }
  
  private static String padLeftWithZero(String s, int length) {
    if (length <= s.length()) {
      return s;
    }
    
    return String.format("%0" + (length - s.length()) + "d%s", 0, s);
  }
  
  @Test(groups={"encode"})
  public void encode0() {
    assert "0".equals(Base62.encode(BigInteger.ZERO));
  }
  
  @Test(groups={"decode"})
  public void decode0() {
    assert BigInteger.ZERO.equals(Base62.decodeBigInteger("0"));
  }
  
  /**
   * Tests decoding of "10", which is 62 (the base).
   */
  @Test(groups={"decode"})
  public void decode10() {
    assert BigInteger.valueOf(62).equals(Base62.decodeBigInteger("10"));
  }
  
  @Test(groups={"decode"})
  public void decodeZ() {
    assert BigInteger.valueOf(62 - 26 - 1).equals(Base62.decodeBigInteger("z"));
    assert BigInteger.valueOf(62 - 1).equals(Base62.decodeBigInteger("Z"));
  }
  
  @Test(groups={"decode", "encode"})
  public void decodeEncodeLargeNumber() {
    String largeBase62 = "ZZZZZZZZZZZZZZZZZZZZZZ"; // 62**23 - 1
    assert largeBase62.equals(decodeEncodeInteger(largeBase62));
  }
  
  /**
   * Tests some general base62-encoded numbers represented as strings.
   */
  @Test(groups={"decode", "encode"})
  public void decodeEncodeGeneralNumber() {
    String[] numbers = {
        "241Oo8Eb4YoaL9mYS6VPHX",
        "2FcWWewZMPQ4xJx1JFOlAM",
        "0mdu7rRY16Kbn3zoqMyRPl", // Zero-padded
        "7ycs5VA6Xyf4o3vLe94hYH",
        "4M8M396xWeOTKrZoNTehGd",
        "0O6k4nkMPNlwTWn27HIled", // Zero-padded
        "0wry0VT5hvaihRxpxbIk3q", // Zero-padded
        "28dguoszXn5JwQa0CIA8ai",
        "7yxyxDs7OCiTN0wu8da5mB"
    };
    
    for (String number : numbers) {
      assert number.equals(
          padLeftWithZero(decodeEncodeInteger(number), number.length()));
    }
  }
}

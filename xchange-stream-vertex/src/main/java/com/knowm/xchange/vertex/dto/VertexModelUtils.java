package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

public class VertexModelUtils {
  public static final BigDecimal NUMBER_CONVERSION_FACTOR = BigDecimal.ONE.scaleByPowerOfTen(18);

  //max value of a 128 byte integer
  public static final BigInteger MAX_128_INTEGER = new BigInteger("170141183460469231731687303715884105727");

  public static BigDecimal convertToDecimal(BigInteger integer) {
    if (integer == null) return null;
    return new BigDecimal(integer).divide(NUMBER_CONVERSION_FACTOR);
  }

  public static BigInteger convertToInteger(BigDecimal decimal) {
    if (decimal == null) return null;
    return decimal.multiply(NUMBER_CONVERSION_FACTOR).toBigInteger();
  }

  public static String buildNonce(int timeoutMillis) {
    return String.valueOf(((Instant.now().toEpochMilli() + timeoutMillis) << 20) + RandomUtils.nextInt(1, 10000));
  }

  public static String buildSender(String walletAddress, String subAccount) {
    byte[] walletBytes = Numeric.hexStringToByteArray(walletAddress);
    if (walletBytes.length != 20) {
      throw new IllegalArgumentException("Wallet address must be 20 bytes long, got " + walletBytes.length + ": " + walletAddress);
    }
    byte[] paddedSubAccount = StringUtils.isEmpty(subAccount) ? new byte[0] : subAccount.getBytes();

    //append byte arrays
    byte[] sender = new byte[32];
    System.arraycopy(walletBytes, 0, sender, 0, walletBytes.length);
    System.arraycopy(paddedSubAccount, 0, sender, walletBytes.length, paddedSubAccount.length);

    return Numeric.toHexString(sender);
  }

  public static BigDecimal readX18Decimal(JsonNode obj, String fieldName) {
    JsonNode jsonNode = obj.get(fieldName);
    if (jsonNode == null) {
      throw new IllegalArgumentException("No such field " + fieldName);
    }
    String text = jsonNode.asText();
    return readX18Decimal(text);
  }

  public static BigDecimal readX18Decimal(String text) {
    if (StringUtils.isEmpty(text)) {
      return BigDecimal.ZERO;
    }
    return convertToDecimal(new BigInteger(text));
  }

  public static void readX18DecimalArray(JsonNode node, String fieldName, List<BigDecimal> outputList) {
    ArrayNode jsonNode = node.withArray(fieldName);
    Iterator<JsonNode> elements = jsonNode.elements();
    while (elements.hasNext()) {
      JsonNode next = elements.next();
      outputList.add(convertToDecimal(new BigInteger(next.asText())));
    }
  }

  public static boolean nonZero(BigInteger num) {
    return num != null && !BigInteger.ZERO.equals(num);
  }

  public static boolean nonZero(BigDecimal num) {
    return num != null && BigDecimal.ZERO.compareTo(num) != 0;
  }

  public static boolean isMaxInt(BigInteger askPrice) {
    return MAX_128_INTEGER.equals(askPrice);
  }
}

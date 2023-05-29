package com.knowm.xchange.vertex.dto;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

public class VertexModelUtils {
    public static final BigDecimal NUMBER_CONVERSION_FACTOR = BigDecimal.ONE.scaleByPowerOfTen(18);

    public static BigDecimal convertToDecimal(BigInteger integer) {
        return new BigDecimal(integer).divide(NUMBER_CONVERSION_FACTOR);
    }

    public static BigInteger convertToInteger(BigDecimal decimal) {
        return decimal.multiply(NUMBER_CONVERSION_FACTOR).toBigInteger();
    }

    public static String buildNonce(int timeoutMillis) {
        return String.valueOf((Instant.now().toEpochMilli() + timeoutMillis << 20) + RandomUtils.nextInt(1, 10000));
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
}

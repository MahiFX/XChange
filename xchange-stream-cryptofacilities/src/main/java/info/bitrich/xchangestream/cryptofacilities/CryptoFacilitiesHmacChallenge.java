package info.bitrich.xchangestream.cryptofacilities;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoFacilitiesHmacChallenge {
    private final byte[] secretKey;

    public CryptoFacilitiesHmacChallenge(String secretKey) {
        this.secretKey = Base64.getDecoder().decode(secretKey);
    }

    public String signChallenge(String challenge) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "Illegal algorithm for post body digest. Check the implementation.");
        }

        sha256.update(challenge.getBytes());

        Mac mac512 = getMac();
        mac512.update(sha256.digest());

        return Base64.getEncoder().encodeToString(mac512.doFinal()).trim();
    }

    private Mac getMac() {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKey secretKey = new SecretKeySpec(this.secretKey, "HmacSHA512");
            mac.init(secretKey);
            return mac;

        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }
}

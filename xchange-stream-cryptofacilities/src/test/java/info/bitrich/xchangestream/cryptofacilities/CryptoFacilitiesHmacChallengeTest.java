package info.bitrich.xchangestream.cryptofacilities;

import org.junit.Assert;
import org.junit.Test;

public class CryptoFacilitiesHmacChallengeTest {
    @Test
    public void testExample() {
        // https://support.cryptofacilities.com/hc/en-us/articles/360000642493-Sign-Challenge-Web-Socket-API-
        CryptoFacilitiesHmacChallenge signer = new CryptoFacilitiesHmacChallenge("7zxMEF5p/Z8l2p2U7Ghv6x14Af+Fx+92tPgUdVQ748FOIrEoT9bgT+bTRfXc5pz8na+hL/QdrCVG7bh9KpT0eMTm");
        Assert.assertEquals("4JEpF3ix66GA2B+ooK128Ift4XQVtc137N9yeg4Kqsn9PI0Kpzbysl9M1IeCEdjg0zl00wkVqcsnG4bmnlMb3A==", signer.signChallenge("c100b894-1729-464d-ace1-52dbce11db42"));
    }
}
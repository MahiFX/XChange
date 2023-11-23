package com.knowm.xchange.vertex.signing.schemas;

import com.knowm.xchange.vertex.signing.EIP712Domain;
import com.knowm.xchange.vertex.signing.EIP712Schema;
import com.knowm.xchange.vertex.signing.EIP712Type;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;

public class StreamAuthentication extends EIP712Schema {

  public StreamAuthentication(EIP712Domain domain, Map<String, Object> message) {
    super(of("StreamAuthentication", List.of(
            new EIP712Type("sender", "bytes32"),
            new EIP712Type("expiration", "uint64")
        )),
        "StreamAuthentication",
        domain,
        message);
  }


  public static StreamAuthentication build(long chainId, String verifyingContract, String sender, BigInteger expiration) {
    EIP712Domain domain = getDomain(chainId, verifyingContract);

    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("sender", sender);
    fields.put("expiration", expiration);

    return new StreamAuthentication(domain, fields);
  }
}

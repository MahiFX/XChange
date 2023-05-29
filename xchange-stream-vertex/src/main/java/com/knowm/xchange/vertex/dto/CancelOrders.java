package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CancelOrders {

    /*
     "tx": {
      "sender": "0x7a5ec2748e9065794491a8d29dcf3f9edb8d7c43746573743000000000000000",
      "productIds": [0],
      "digests": ["0x"],
      "nonce": "1"
    },
    "signature": "0x"
  }

     */

    private final Tx tx;
    private final String signature;

    public CancelOrders(@JsonProperty("tx") Tx tx, @JsonProperty("signature") String signature) {
        this.tx = tx;
        this.signature = signature;
    }


    public Tx getTx() {
        return tx;
    }

    public String getSignature() {
        return signature;
    }
}

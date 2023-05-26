package com.knowm.xchange.vertex.signing;

public class EIP712Domain {

    private final String name;

    private final String version;

    private final String chainId;

    private final String verifyingContract;

    public EIP712Domain(String name, String version, String chainId, String verifyingContract) {
        this.name = name;
        this.version = version;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getChainId() {
        return chainId;
    }

    public String getVerifyingContract() {
        return verifyingContract;
    }
}

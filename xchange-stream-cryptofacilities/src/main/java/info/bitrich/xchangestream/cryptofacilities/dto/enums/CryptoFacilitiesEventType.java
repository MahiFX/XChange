package info.bitrich.xchangestream.cryptofacilities.dto.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum CryptoFacilitiesEventType {
    heartbeat,
    subscribe,
    subscribed,
    subscribed_failed,
    unsubscribe,
    unsubscribed,
    unsubscribed_failed,
    systemStatus,
    subscriptionStatus,
    pingStatus,
    ping,
    pong,
    error,
    alert,
    info;

    public static CryptoFacilitiesEventType getEvent(String event) {
        return Arrays.stream(CryptoFacilitiesEventType.values())
                .filter(e -> StringUtils.equalsIgnoreCase(event, e.name()))
                .findFirst()
                .orElse(null);
    }
}

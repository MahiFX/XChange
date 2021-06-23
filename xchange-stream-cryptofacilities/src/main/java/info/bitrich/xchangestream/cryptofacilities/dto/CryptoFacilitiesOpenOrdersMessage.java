package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class CryptoFacilitiesOpenOrdersMessage {
    private final String feed;
    private final String account;
    private final CryptoFacilitiesOpenOrder order;
    private final CryptoFacilitiesOpenOrder[] orders;
    private final Boolean isCancel;
    private final String reason;

    public CryptoFacilitiesOpenOrdersMessage(
            @JsonProperty("feed") String feed,
            @JsonProperty("account") String account,
            @JsonProperty("order") CryptoFacilitiesOpenOrder order,
            @JsonProperty("orders") CryptoFacilitiesOpenOrder[] orders,
            @JsonProperty("is_cancel") Boolean isCancel,
            @JsonProperty("reason") String reason) {
        this.feed = feed;
        this.account = account;
        this.order = order;
        this.orders = orders;
        this.isCancel = isCancel;
        this.reason = reason;
    }

    public String getFeed() {
        return feed;
    }

    public String getAccount() {
        return account;
    }

    public CryptoFacilitiesOpenOrder[] getOrders() {
        if (orders == null && order != null) {
            return new CryptoFacilitiesOpenOrder[] {order};
        }

        return orders;
    }

    public Boolean getCancel() {
        return isCancel;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "CryptoFacilitiesOpenOrdersMessage{" +
                "feed='" + feed + '\'' +
                ", account='" + account + '\'' +
                ", order=" + order +
                ", orders=" + Arrays.toString(orders) +
                ", isCancel=" + isCancel +
                ", reason='" + reason + '\'' +
                '}';
    }
}

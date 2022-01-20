package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.knowm.xchange.dto.Order;

public class DeribitOrderUpdate {
    public static DeribitOrderUpdate EMPTY = new DeribitOrderUpdate();

    @JsonIgnore
    public Order toOrder() {
        // TODO
        return new Order(null, null, null, null, null) {
        };
    }
}

package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import org.knowm.xchange.dto.Order;

public enum DeribitTimeInForce implements Order.IOrderFlags {
    GTC("good_til_cancelled"),
    GTD("good_til_day"),
    FOK("fill_or_kill"),
    IOC("immediate_or_cancel");

    private final String name;

    DeribitTimeInForce(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}

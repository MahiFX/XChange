package info.bitrich.xchangestream.cryptofacilities;

import org.knowm.xchange.dto.Order;

import java.util.Date;

public class CryptoFacilitiesSimpleCancelOrder extends Order {
    public CryptoFacilitiesSimpleCancelOrder(String id, Date timestamp) {
        super(null, null, null, id, timestamp);
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CANCELED;
    }
}

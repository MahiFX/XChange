package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.knowm.xchange.dto.trade.UserTrade;

public class DeribitUserTrade {
    public static DeribitUserTrade EMPTY = new DeribitUserTrade();

    @JsonIgnore
    public UserTrade toUserTrade() {
        // TODO
        return new UserTrade(null, null, null, null, null, null, null, null, null, null);
    }
}

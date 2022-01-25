package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.instrument.Instrument;

import java.util.Objects;

public class DeribitStreamingUtil {

    private DeribitStreamingUtil() {
    }

    public static String getDirection(Order.OrderType type) {
        return (type == Order.OrderType.BID) ? "buy" : "sell";
    }

    public static Order.OrderType convertDirectionToType(String direction) {
        return (Objects.equals(direction, "buy")) ? Order.OrderType.BID : Order.OrderType.ASK;
    }

    public static String instrumentName(Instrument instrument) {
        return instrument.toString().replace("/", "-");
    }

    public static <T> T tryGetDataAsType(ObjectMapper mapper, JsonNode json, Class<T> dataType) throws JsonProcessingException {
        if (json.has("params")) {
            JsonNode params = json.get("params");

            if (params.has("data")) {
                JsonNode data = params.get("data");

                return mapper.treeToValue(data, dataType);
            }
        }

        return null;
    }

    public static Order.OrderType getType(String side) {
        return "buy".equalsIgnoreCase(side) ? Order.OrderType.BID : Order.OrderType.ASK;
    }
}

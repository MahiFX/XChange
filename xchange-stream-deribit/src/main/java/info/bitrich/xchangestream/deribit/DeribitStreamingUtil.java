package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchange.currency.CurrencyPair;

public class DeribitStreamingUtil {

    private DeribitStreamingUtil() {
    }

    public static String instrumentName(CurrencyPair currencyPair) {
        return currencyPair.toString().replace("/", "-");
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
}

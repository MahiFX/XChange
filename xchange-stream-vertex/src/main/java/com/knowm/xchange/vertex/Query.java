package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Consumer;

public class Query {
    private final String queryMsg;
    private final Consumer<JsonNode> respHandler;

    public Query(String queryMsg, Consumer<JsonNode> respHandler) {
        this.queryMsg = queryMsg;
        this.respHandler = respHandler;
    }

    public String getQueryMsg() {
        return queryMsg;
    }

    public Consumer<JsonNode> getRespHandler() {
        return respHandler;
    }
}

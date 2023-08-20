package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;

public class EpochSecondsDeserializer extends JsonDeserializer<Instant> {

  private static final CacheLoader<String, Instant> parser = new CacheLoader<>() {
    @Override
    public Instant load(String str) {
      BigInteger seconds = new BigInteger(str);
      return Instant.ofEpochSecond(seconds.longValue());
    }
  };

  public static Instant parse(String str) {
    return instantCache.getUnchecked(str);
  }

  private static final LoadingCache<String, Instant> instantCache = CacheBuilder.newBuilder().maximumSize(1000).build(parser);

  @Override
  public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return instantCache.getUnchecked(p.getValueAsString());

  }
}

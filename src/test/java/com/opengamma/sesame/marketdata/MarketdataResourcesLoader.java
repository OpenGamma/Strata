/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalScheme;

/**
 * Load key/value pair marketdata resources as a map of MarketDataRequirement and MarketDataItem
 */
public class MarketdataResourcesLoader {

  public static Map<MarketDataRequirement, MarketDataItem> getData(String path, Class clazz , String scheme)  throws IOException {
    return getData(path, clazz, ExternalScheme.of(scheme));
  }

  public static Map<MarketDataRequirement, MarketDataItem> getData(String path, Class clazz, ExternalScheme scheme) throws IOException {
    Properties properties = new Properties();
    try (InputStream stream = clazz.getResourceAsStream("/" + path);
         Reader reader = new BufferedReader(new InputStreamReader(stream))) {
      properties.load(reader);
    }
    Map<MarketDataRequirement, MarketDataItem> data = Maps.newHashMap();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String id = (String) entry.getKey();
      String value = (String) entry.getValue();
      addValue(data, id, Double.valueOf(value), scheme);
    }
    return data;

  }

  private static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData, String ticker, double value, ExternalScheme scheme) {
    return addValue(marketData, new CurveNodeMarketDataRequirement(ExternalId.of(scheme, ticker), "Market_Value"), value);
  }

  public static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData,
                                         MarketDataRequirement requirement,
                                         double value) {
    return marketData.put(requirement, MarketDataItem.available(value));
  }



}

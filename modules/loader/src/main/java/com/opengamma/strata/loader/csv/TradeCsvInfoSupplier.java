/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Resolves additional information when writing trade CSV files.
 * <p>
 * This allows additional CSV columns to be written.
 */
public interface TradeCsvInfoSupplier {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static TradeCsvInfoSupplier standard() {
    return StandardCsvInfoImpl.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the list of additional headers this supplier provides.
   * 
   * @param trade  the trade to output
   * @return the list of additional headers
   */
  public default List<String> headers(Trade trade) {
    return ImmutableList.of();
  }

  /**
   * Gets the values associated with the headers.
   * <p>
   * This must return a map where the keys are headers that were returned from {@code headers()}.
   * <p>
   * This will be invoked once for each {@link Trade} and will normally examine {@link TradeInfo}.
   * 
   * @param headers  the complete set of headers
   * @param trade  the trade to output
   * @return the map of values by header, not null
   */
  public default Map<String, String> values(List<String> headers, Trade trade) {
    return ImmutableMap.of();
  }

}

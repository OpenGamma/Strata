/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.CdsIndexTrade;

/**
 * Handles the CSV file format for CDS index trades.
 */
final class CdsIndexTradeCsvPlugin implements TradeCsvParserPlugin {

  /**
   * The singleton instance of the plugin.
   */
  public static final CdsIndexTradeCsvPlugin INSTANCE = new CdsIndexTradeCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("CDSINDEX", "CDS INDEX");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(CdsIndexTrade.class)) {
      return Optional.of(resolver.parseCdsIndexTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "CdsIndex";
  }

}

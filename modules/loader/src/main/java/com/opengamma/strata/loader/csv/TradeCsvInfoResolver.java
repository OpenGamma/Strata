/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Resolves additional information when parsing trade CSV files.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 */
public interface TradeCsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static TradeCsvInfoResolver standard() {
    return StandardCsvInfoResolver.of(ReferenceData.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static TradeCsvInfoResolver of(ReferenceData refData) {
    return StandardCsvInfoResolver.of(refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the reference data being used.
   * 
   * @return the reference data
   */
  public abstract ReferenceData getReferenceData();

  /**
   * Parses attributes into {@code TradeInfo}.
   * <p>
   * If they are available, the trade ID, date, time and zone will have been set
   * before this method is called. They may be altered if necessary, although
   * this is not recommended.
   * 
   * @param row  the CSV row to parse
   * @param builder  the builder to update
   */
  public default void parseTradeInfo(CsvRow row, TradeInfoBuilder builder) {
    // do nothing
  }

  /**
   * Completes the FRA trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default FraTrade completeTrade(CsvRow row, FraTrade trade) {
    // do nothing
    return trade;
  }

  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default SecurityTrade completeTrade(CsvRow row, SecurityTrade trade) {
    // do nothing
    return trade;
  }

  /**
   * Completes the FRA trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default SwapTrade completeTrade(CsvRow row, SwapTrade trade) {
    // do nothing
    return trade;
  }

  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default TermDepositTrade completeTrade(CsvRow row, TermDepositTrade trade) {
    // do nothing
    return trade;
  }

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
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
    return StandardCsvInfoImpl.INSTANCE;
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static TradeCsvInfoResolver of(ReferenceData refData) {
    return StandardCsvInfoImpl.of(refData);
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

  //-------------------------------------------------------------------------
  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called by each of the {@code completeTrade} methods.
   *
   * @param <T> the trade type
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default <T extends Trade> T completeTradeCommon(CsvRow row, T trade) {
    //do nothing
    return trade;
  }

  /**
   * Completes the FRA trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default FraTrade completeTrade(CsvRow row, FraTrade trade) {
    // do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default SecurityTrade completeTrade(CsvRow row, SecurityTrade trade) {
    // do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the FRA trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default SwapTrade completeTrade(CsvRow row, SwapTrade trade) {
    // do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default BulletPaymentTrade completeTrade(CsvRow row, BulletPaymentTrade trade) {
    // do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   * 
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default TermDepositTrade completeTrade(CsvRow row, TermDepositTrade trade) {
    // do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the FX Forward trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   *
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default FxSingleTrade completeTrade(CsvRow row, FxSingleTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the FX Swap trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   *
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default FxSwapTrade completeTrade(CsvRow row, FxSwapTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the FX Vanilla Option trade, potentially parsing additional columns.
   * <p>
   * This is called after the trade has been parsed and after
   * {@link #parseTradeInfo(CsvRow, TradeInfoBuilder)}.
   * <p>
   * By default this calls {@link #completeTradeCommon(CsvRow, Trade)}.
   *
   * @param row  the CSV row to parse
   * @param trade  the parsed trade
   * @return the updated trade
   */
  public default FxVanillaOptionTrade completeTrade(CsvRow row, FxVanillaOptionTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

}

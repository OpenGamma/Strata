/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CCP_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DESCRIPTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NAME_FIELD;

import java.util.List;
import java.util.Optional;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorTrade;
import com.opengamma.strata.product.common.CcpId;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.SwaptionTrade;

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
   * @return the resolver
   */
  public static TradeCsvInfoResolver standard() {
    return StandardCsvInfoImpl.INSTANCE;
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the resolver
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
   * Parses standard attributes into {@code TradeInfo}.
   * <p>
   * This parses the standard set of attributes, which are the three constants in {@link AttributeType}.
   * The column names are 'Description', 'Name' and 'CCP'.
   * 
   * @param row  the CSV row to parse
   * @param builder  the builder to update
   */
  public default void parseStandardAttributes(CsvRow row, TradeInfoBuilder builder) {
    row.findValue(DESCRIPTION_FIELD)
        .ifPresent(str -> builder.addAttribute(AttributeType.DESCRIPTION, str));
    row.findValue(NAME_FIELD)
        .ifPresent(str -> builder.addAttribute(AttributeType.NAME, str));
    row.findValue(CCP_FIELD)
        .ifPresent(str -> builder.addAttribute(AttributeType.CCP, CcpId.of(str)));
  }

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
   * Completes the Swap trade, potentially parsing additional columns.
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
   * Completes the Swaption trade, potentially parsing additional columns.
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
  public default SwaptionTrade completeTrade(CsvRow row, SwaptionTrade trade) {
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

  /**
   * Completes the FX NDF trade, potentially parsing additional columns.
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
  public default FxNdfTrade completeTrade(CsvRow row, FxNdfTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the CDS trade, potentially parsing additional columns.
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
  public default CdsTrade completeTrade(CsvRow row, CdsTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the CDS Index trade, potentially parsing additional columns.
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
  public default CdsIndexTrade completeTrade(CsvRow row, CdsIndexTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the CapFloor trade, potentially parsing additional columns.
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
  public default IborCapFloorTrade completeTrade(CsvRow row, IborCapFloorTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  /**
   * Completes the CapFloor trade, potentially parsing additional columns.
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
  public default OvernightInArrearsCapFloorTrade completeTrade(CsvRow row, OvernightInArrearsCapFloorTrade trade) {
    //do nothing
    return completeTradeCommon(row, trade);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a FRA trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the FRA trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default FraTrade parseFraTrade(CsvRow row, TradeInfo info) {
    return FraTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a Security trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the Security trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default SecurityQuantityTrade parseSecurityTrade(CsvRow row, TradeInfo info) {
    return SecurityTradeCsvPlugin.parseTradeWithPriceInfo(row, info, this);
  }

  /**
   * Parses a Swap trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param variableRows   the CSV rows representing variable notional/rate etc
   * @param info  the trade info
   * @return the Swap trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default SwapTrade parseSwapTrade(CsvRow row, List<CsvRow> variableRows, TradeInfo info) {
    return SwapTradeCsvPlugin.parse(row, variableRows, info, this);
  }

  /**
   * Parses a Swaption trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param variableRows   the CSV rows representing variable notional/rate etc
   * @param info  the trade info
   * @return the Swaption trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default SwaptionTrade parseSwaptionTrade(CsvRow row, List<CsvRow> variableRows, TradeInfo info) {
    return SwaptionTradeCsvPlugin.parse(row, variableRows, info, this);
  }

  /**
   * Parses a Bullet Payment trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the Bullet Payment trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default BulletPaymentTrade parseBulletPaymentTrade(CsvRow row, TradeInfo info) {
    return BulletPaymentTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a Term Deposit trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the Term Deposit trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default TermDepositTrade parseTermDepositTrade(CsvRow row, TradeInfo info) {
    return TermDepositTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a FX Single trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the FX Single trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default FxSingleTrade parseFxSingleTrade(CsvRow row, TradeInfo info) {
    return FxSingleTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a FX Swap trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the FX Swap trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default FxSwapTrade parseFxSwapTrade(CsvRow row, TradeInfo info) {
    return FxSwapTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a FX Vanilla Option trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the FX Vanilla Option trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default FxVanillaOptionTrade parseFxVanillaOptionTrade(CsvRow row, TradeInfo info) {
    return FxVanillaOptionTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a FX NDF trade from CSV.
   *
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the FX NDF trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default FxNdfTrade parseFxNdfTrade(CsvRow row, TradeInfo info) {
    return FxNdfTradeCsvPlugin.parse(row, info, this);
  }

  /**
   * Parses a CDS trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the CDS trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default CdsTrade parseCdsTrade(CsvRow row, TradeInfo info) {
    return CdsTradeCsvPlugin.parseCds(row, info, this);
  }

  /**
   * Parses a CDS Index trade from CSV.
   * 
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the CDS trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default CdsIndexTrade parseCdsIndexTrade(CsvRow row, TradeInfo info) {
    return CdsIndexTradeCsvPlugin.parseCdsIndex(row, info, this);
  }

  /**
   * Parses an IBOR CapFloor trade from CSV.
   *
   * @param row the CSV row to parse
   * @param info the trade info
   * @return the IBOR CapFloor trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default IborCapFloorTrade parseIborCapFloorTrade(CsvRow row, TradeInfo info) {
    return IborCapFloorTradeCsvPlugin.parseCapFloor(row, info, this);
  }

  /**
   * Parses an overnight CapFloor trade from CSV.
   *
   * @param row the CSV row to parse
   * @param info the trade info
   * @return the overnight CapFloor trade
   * @throws RuntimeException if the row contains invalid data
   */
  public default OvernightInArrearsCapFloorTrade parseOvernightCapFloorTrade(CsvRow row, TradeInfo info) {
    return OvernightInArrearsCapFloorTradeCsvPlugin.parseCapFloor(row, info, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses any kind of trade from CSV before standard matching.
   * <p>
   * This is called before the standard matching on the 'Product Type' column.
   * As such, it allows the standard parsing to be replaced for a given type.
   * 
   * @param typeUpper  the upper case product type column
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the trade, empty if the product type is not known, or if standard parsing should occur
   * @throws RuntimeException if the product type is known but the row contains invalid data
   */
  public default Optional<Trade> overrideParseTrade(String typeUpper, CsvRow row, TradeInfo info) {
    return Optional.empty();
  }

  /**
   * Parses any kind of trade from CSV after standard matching.
   * <p>
   * This is called after the standard matching on the 'Product Type' column.
   * As such, it allows non-matched rows to be captured.
   * 
   * @param typeUpper  the upper case product type column
   * @param row  the CSV row to parse
   * @param info  the trade info
   * @return the trade, empty if the product type is not known
   * @throws RuntimeException if the product type is known but the row contains invalid data
   */
  public default Optional<Trade> parseOtherTrade(String typeUpper, CsvRow row, TradeInfo info) {
    return Optional.empty();
  }

}

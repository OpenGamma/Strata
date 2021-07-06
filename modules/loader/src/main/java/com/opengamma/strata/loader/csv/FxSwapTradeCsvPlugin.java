/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FAR_FX_RATE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FAR_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Handles the CSV file format for FX Swap trades.
 */
class FxSwapTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<FxSwapTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FxSwapTradeCsvPlugin INSTANCE = new FxSwapTradeCsvPlugin();

  private static final String FAR = "Far ";

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.of(
      LEG_1_DIRECTION_FIELD,
      LEG_1_PAYMENT_DATE_FIELD,
      LEG_1_CURRENCY_FIELD,
      LEG_1_NOTIONAL_FIELD,
      LEG_2_DIRECTION_FIELD,
      LEG_2_PAYMENT_DATE_FIELD,
      LEG_2_CURRENCY_FIELD,
      LEG_2_NOTIONAL_FIELD,
      PAYMENT_DATE_CNV_FIELD,
      PAYMENT_DATE_CAL_FIELD,
      FAR + LEG_1_DIRECTION_FIELD,
      FAR + LEG_1_PAYMENT_DATE_FIELD,
      FAR + LEG_1_CURRENCY_FIELD,
      FAR + LEG_1_NOTIONAL_FIELD,
      FAR + LEG_2_DIRECTION_FIELD,
      FAR + LEG_2_PAYMENT_DATE_FIELD,
      FAR + LEG_2_CURRENCY_FIELD,
      FAR + LEG_2_NOTIONAL_FIELD,
      FAR + PAYMENT_DATE_CNV_FIELD,
      FAR + PAYMENT_DATE_CAL_FIELD);

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("FXSWAP", "FX SWAP");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(FxSwapTrade.class)) {
      return Optional.of(resolver.parseFxSwapTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return FxSwapTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(FxSwapTrade.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static FxSwapTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSwapTrade trade = parseRow(row, info);
    return resolver.completeTrade(row, trade);
  }

  // parses the trade
  private static FxSwapTrade parseRow(CsvRow row, TradeInfo info) {
    if (row.findValue(CONVENTION_FIELD).isPresent() || row.findValue(BUY_SELL_FIELD).isPresent()) {
      return parseConvention(row, info);
    } else {
      return parseFull(row, info);
    }
  }

  // convention-based
  // ideally we'd use the trade date plus "period to start" to get the spot/payment date
  // but we don't have all the data and it gets complicated in places like TRY, RUB and AED
  private static FxSwapTrade parseConvention(CsvRow row, TradeInfo info) {
    CurrencyPair pair = row.getValue(CONVENTION_FIELD, CurrencyPair::parse);
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    CurrencyAmount amount = buySell.normalize(CsvLoaderUtils.parseCurrencyAmount(row, CURRENCY_FIELD, NOTIONAL_FIELD));
    double nearFxRate = row.getValue(FX_RATE_FIELD, LoaderUtils::parseDouble);
    double farFxRate = row.getValue(FAR_FX_RATE_DATE_FIELD, LoaderUtils::parseDouble);
    LocalDate nearPaymentDate = row.getValue(PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    LocalDate farPaymentDate = row.getValue(FAR_PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    Optional<BusinessDayAdjustment> paymentAdj = FxSingleTradeCsvPlugin.parsePaymentDateAdjustment(row);

    FxRate nearRate = FxRate.of(pair, nearFxRate);
    FxRate farRate = FxRate.of(pair, farFxRate);
    FxSwap fx = paymentAdj
        .map(adj -> FxSwap.of(amount, nearRate, nearPaymentDate, farRate, farPaymentDate, adj))
        .orElseGet(() -> FxSwap.of(amount, nearRate, nearPaymentDate, farRate, farPaymentDate));
    return FxSwapTrade.of(info, fx);
  }

  // parse full definition
  private static FxSwapTrade parseFull(CsvRow row, TradeInfo info) {
    FxSingle nearFx = FxSingleTradeCsvPlugin.parseFxSingle(row, "");
    FxSingle farFx = FxSingleTradeCsvPlugin.parseFxSingle(row, "Far ");
    return FxSwapTrade.of(info, FxSwap.of(nearFx, farFx));
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<FxSwapTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxSwapTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "FxSwap");
    CsvWriterUtils.writeFxSingle(csv, "", trade.getProduct().getNearLeg());
    CsvWriterUtils.writeFxSingle(csv, FAR, trade.getProduct().getFarLeg());
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FxSwapTradeCsvPlugin() {
  }

}

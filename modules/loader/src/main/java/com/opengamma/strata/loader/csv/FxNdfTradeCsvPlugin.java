/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Handles the CSV file format for FxNdf trades.
 */
public class FxNdfTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<FxNdfTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FxNdfTradeCsvPlugin INSTANCE = new FxNdfTradeCsvPlugin();

  private static final String FX_INDEX_FIELD = "FX Index";

  private static final String LEG_1_CURRENCY_FIELD = "Leg 1 " + CURRENCY_FIELD;
  private static final String LEG_1_DIRECTION_FIELD = "Leg 1 " + DIRECTION_FIELD;
  private static final String LEG_1_NOTIONAL_FIELD = "Leg 1 " + NOTIONAL_FIELD;

  private static final String LEG_2_CURRENCY_FIELD = "Leg 2 " + CURRENCY_FIELD;
  private static final String LEG_2_DIRECTION_FIELD = "Leg 2 " + DIRECTION_FIELD;
  private static final String LEG_2_NOTIONAL_FIELD = "Leg 2 " + NOTIONAL_FIELD;

  private static final ImmutableSet<String> HEADERS = ImmutableSet.<String>builder()
      .add(PAYMENT_DATE_FIELD)
      .add(LEG_1_CURRENCY_FIELD)
      .add(LEG_1_DIRECTION_FIELD)
      .add(LEG_1_NOTIONAL_FIELD)
      .add(LEG_2_CURRENCY_FIELD)
      .add(LEG_2_DIRECTION_FIELD)
      .add(LEG_2_NOTIONAL_FIELD)
      .add(FX_RATE_FIELD)
      .add(FX_INDEX_FIELD)
      .build();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("FXNDF", "FX NDF", "NDF");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(FxNdfTrade.class)) {
      return Optional.of(resolver.parseFxNdfTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "FxNdf";
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(FxNdfTrade.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   *
   * @param row the CSV row
   * @param info the trade info
   * @param resolver the resolver used to parse additional information
   * @return the parsed trade
   */
  static FxNdfTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxNdfTrade trade = parseRow(row, info);
    return resolver.completeTrade(row, trade);
  }

  //-------------------------------------------------------------------------
  // parses the row to a trade
  private static FxNdfTrade parseRow(CsvRow row, TradeInfo info) {
    CurrencyAmount settlementNotional;
    Currency settlementCurrency;
    Currency nonDeliverableCurrency;
    Optional<String> leg1NotionalOpt = row.findValue(LEG_1_NOTIONAL_FIELD);
    Optional<String> leg2NotionalOpt = row.findValue(LEG_2_NOTIONAL_FIELD);
    if (leg1NotionalOpt.isPresent() && leg2NotionalOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Notional found for both legs; only one leg can contain notional amount to determine settlement leg");
    } else if (leg1NotionalOpt.isPresent()) {
      settlementNotional = CsvLoaderUtils.parseCurrencyAmountWithDirection(
          row, LEG_1_CURRENCY_FIELD, LEG_1_NOTIONAL_FIELD, LEG_1_DIRECTION_FIELD);
      settlementCurrency = row.getField(LEG_1_CURRENCY_FIELD, LoaderUtils::parseCurrency);
      nonDeliverableCurrency = row.getField(LEG_2_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    } else if (row.findValue(LEG_2_NOTIONAL_FIELD).isPresent()) {
      settlementNotional = CsvLoaderUtils.parseCurrencyAmountWithDirection(
          row, LEG_2_CURRENCY_FIELD, LEG_2_NOTIONAL_FIELD, LEG_2_DIRECTION_FIELD);
      settlementCurrency = row.getField(LEG_2_CURRENCY_FIELD, LoaderUtils::parseCurrency);
      nonDeliverableCurrency = row.getField(LEG_1_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    } else {
      throw new IllegalArgumentException("Notional could not be found to determine settlement leg");
    }

    PayReceive leg1Direction = row.getValue(LEG_1_DIRECTION_FIELD, LoaderUtils::parsePayReceive);
    PayReceive leg2Direction = row.getValue(LEG_2_DIRECTION_FIELD, LoaderUtils::parsePayReceive);
    if (leg1Direction.equals(leg2Direction)) {
      throw new IllegalArgumentException(Messages.format(
          "FxNdf legs must not have the same direction: {}, {}", leg1Direction, leg2Direction));
    }

    CurrencyPair currencyPair = CurrencyPair.of(settlementCurrency, nonDeliverableCurrency);
    LocalDate paymentDate = row.getField(PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    FxRate agreedFxRate = FxRate.of(currencyPair, row.getField(FX_RATE_FIELD, LoaderUtils::parseDouble));
    FxIndex index = parseFxIndex(row, currencyPair);
    FxNdf fxNdf = FxNdf.builder()
        .settlementCurrencyNotional(settlementNotional)
        .agreedFxRate(agreedFxRate)
        .index(index)
        .paymentDate(paymentDate)
        .build();
    return FxNdfTrade.of(info, fxNdf);
  }

  // parses the FX Index
  private static FxIndex parseFxIndex(CsvRow row, CurrencyPair currencyPair) {
    // prioritize value in fx index field if present
    return FxIndex.of(row.findValue(FX_INDEX_FIELD).orElse(currencyPair.toString()));
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<FxNdfTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxNdfTrade trade) {
    FxNdf fxNdf = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "FxNdf");
    csv.writeCell(PAYMENT_DATE_FIELD, fxNdf.getPaymentDate());
    csv.writeCell(LEG_1_CURRENCY_FIELD, fxNdf.getSettlementCurrency());
    csv.writeCell(LEG_1_DIRECTION_FIELD,
        fxNdf.getSettlementCurrencyNotional().isNegative() ? PayReceive.PAY : PayReceive.RECEIVE);
    csv.writeCell(LEG_1_NOTIONAL_FIELD, Math.abs(fxNdf.getSettlementCurrencyNotional().getAmount()));
    csv.writeCell(LEG_2_CURRENCY_FIELD, fxNdf.getNonDeliverableCurrency());
    csv.writeCell(LEG_2_DIRECTION_FIELD,
        fxNdf.getSettlementCurrencyNotional().isNegative() ? PayReceive.RECEIVE : PayReceive.PAY);
    csv.writeCell(FX_RATE_FIELD, fxNdf.getAgreedFxRate().fxRate(fxNdf.getCurrencyPair()));
    csv.writeCell(FX_INDEX_FIELD, fxNdf.getIndex());
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FxNdfTradeCsvPlugin() {
  }

}

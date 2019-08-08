/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FX_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Handles the CSV file format for FX Single trades.
 */
class FxSingleTradeCsvPlugin implements TradeTypeCsvWriter<FxSingleTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FxSingleTradeCsvPlugin INSTANCE = new FxSingleTradeCsvPlugin();

  static final String LEG_1_DIRECTION_FIELD = "Leg 1 " + DIRECTION_FIELD;
  static final String LEG_1_PAYMENT_DATE_FIELD = "Leg 1 " + PAYMENT_DATE_FIELD;
  static final String LEG_1_CURRENCY_FIELD = "Leg 1 " + CURRENCY_FIELD;
  static final String LEG_1_NOTIONAL_FIELD = "Leg 1 " + NOTIONAL_FIELD;

  static final String LEG_2_DIRECTION_FIELD = "Leg 2 " + DIRECTION_FIELD;
  static final String LEG_2_PAYMENT_DATE_FIELD = "Leg 2 " + PAYMENT_DATE_FIELD;
  static final String LEG_2_CURRENCY_FIELD = "Leg 2 " + CURRENCY_FIELD;
  static final String LEG_2_NOTIONAL_FIELD = "Leg 2 " + NOTIONAL_FIELD;

  /** The headers. */
  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(LEG_1_DIRECTION_FIELD)
      .add(LEG_1_PAYMENT_DATE_FIELD)
      .add(LEG_1_CURRENCY_FIELD)
      .add(LEG_1_NOTIONAL_FIELD)
      .add(LEG_2_DIRECTION_FIELD)
      .add(LEG_2_PAYMENT_DATE_FIELD)
      .add(LEG_2_CURRENCY_FIELD)
      .add(LEG_2_NOTIONAL_FIELD)
      .add(PAYMENT_DATE_CNV_FIELD)
      .add(PAYMENT_DATE_CAL_FIELD)
      .build();

  //-------------------------------------------------------------------------
  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static FxSingleTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSingleTrade trade = parseRow(row, info);
    return resolver.completeTrade(row, trade);
  }

  // parses the trade
  static FxSingleTrade parseRow(CsvRow row, TradeInfo info) {
    if (row.findValue(CONVENTION_FIELD).isPresent() || row.findValue(BUY_SELL_FIELD).isPresent()) {
      return parseConvention(row, info);
    } else {
      return parseFull(row, info);
    }
  }

  // convention-based
  // ideally we'd use the trade date plus "period to start" to get the spot/payment date
  // but we don't have all the data and it gets complicated in places like TRY, RUB and AED
  private static FxSingleTrade parseConvention(CsvRow row, TradeInfo info) {
    CurrencyPair pair = row.getValue(CONVENTION_FIELD, CurrencyPair::parse);
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    CurrencyAmount amount = buySell.normalize(CsvLoaderUtils.parseCurrencyAmount(row, CURRENCY_FIELD, NOTIONAL_FIELD));
    double fxRate = row.getValue(FX_RATE_FIELD, LoaderUtils::parseDouble);
    LocalDate paymentDate = row.getValue(PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    Optional<BusinessDayAdjustment> paymentAdj = parsePaymentDateAdjustment(row);

    FxSingle fx = paymentAdj
        .map(adj -> FxSingle.of(amount, FxRate.of(pair, fxRate), paymentDate, adj))
        .orElseGet(() -> FxSingle.of(amount, FxRate.of(pair, fxRate), paymentDate));
    return FxSingleTrade.of(info, fx);
  }

  // parse full definition
  private static FxSingleTrade parseFull(CsvRow row, TradeInfo info) {
    FxSingle fx = parseFxSingle(row, "");
    return FxSingleTrade.of(info, fx);
  }

  // parse an FxSingle
  static FxSingle parseFxSingle(CsvRow row, String prefix) {
    CurrencyAmount amount1 = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, prefix + LEG_1_CURRENCY_FIELD, prefix + LEG_1_NOTIONAL_FIELD, prefix + LEG_1_DIRECTION_FIELD);
    LocalDate paymentDate1 = row.findValue(prefix + LEG_1_PAYMENT_DATE_FIELD)
        .map(str -> LoaderUtils.parseDate(str))
        .orElseGet(() -> row.getValue(prefix + PAYMENT_DATE_FIELD, LoaderUtils::parseDate));
    CurrencyAmount amount2 = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, prefix + LEG_2_CURRENCY_FIELD, prefix + LEG_2_NOTIONAL_FIELD, prefix + LEG_2_DIRECTION_FIELD);
    LocalDate paymentDate2 = row.findValue(prefix + LEG_2_PAYMENT_DATE_FIELD)
        .map(str -> LoaderUtils.parseDate(str))
        .orElseGet(() -> row.getValue(prefix + PAYMENT_DATE_FIELD, LoaderUtils::parseDate));
    Optional<BusinessDayAdjustment> paymentAdj = parsePaymentDateAdjustment(row);
    if (amount1.isPositive() == amount2.isPositive()) {
      throw new IllegalArgumentException(Messages.format(
          "FxSingle legs must not have the same direction: {}, {}",
          row.getValue(prefix + LEG_1_DIRECTION_FIELD),
          row.getValue(prefix + LEG_2_DIRECTION_FIELD)));
    }
    Payment payment1 = Payment.of(amount1, paymentDate1);
    Payment payment2 = Payment.of(amount2, paymentDate2);
    return paymentAdj
        .map(adj -> FxSingle.of(payment1, payment2, adj))
        .orElseGet(() -> FxSingle.of(payment1, payment2));
  }

  // parses the payment date adjustment, which consists of two linked optional fields
  static Optional<BusinessDayAdjustment> parsePaymentDateAdjustment(CsvRow row) {
    return CsvLoaderUtils.parseBusinessDayAdjustment(row, PAYMENT_DATE_CNV_FIELD, PAYMENT_DATE_CAL_FIELD)
        .filter(adj -> !adj.equals(BusinessDayAdjustment.NONE));
  }

  //-------------------------------------------------------------------------
  @Override
  public List<String> headers(List<FxSingleTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxSingleTrade trade) {
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "FxSingle");
    writeProduct(csv, "", trade.getProduct());
    csv.writeNewLine();
  }

  // writes the product to CSV
  void writeProduct(CsvRowOutputWithHeaders csv, String prefix, FxSingle product) {
    Payment basePayment = product.getBaseCurrencyPayment();
    csv.writeCell(prefix + LEG_1_DIRECTION_FIELD, PayReceive.ofSignedAmount(basePayment.getAmount()));
    csv.writeCell(prefix + LEG_1_CURRENCY_FIELD, basePayment.getCurrency());
    csv.writeCell(prefix + LEG_1_NOTIONAL_FIELD, basePayment.getAmount());
    csv.writeCell(prefix + LEG_1_PAYMENT_DATE_FIELD, basePayment.getDate());
    Payment counterPayment = product.getCounterCurrencyPayment();
    csv.writeCell(prefix + LEG_2_DIRECTION_FIELD, PayReceive.ofSignedAmount(counterPayment.getAmount()));
    csv.writeCell(prefix + LEG_2_CURRENCY_FIELD, counterPayment.getCurrency());
    csv.writeCell(prefix + LEG_2_NOTIONAL_FIELD, counterPayment.getAmount());
    csv.writeCell(prefix + LEG_2_PAYMENT_DATE_FIELD, counterPayment.getDate());
    product.getPaymentDateAdjustment().ifPresent(bda -> {
      csv.writeCell(PAYMENT_DATE_CAL_FIELD, bda.getCalendar());
      csv.writeCell(PAYMENT_DATE_CNV_FIELD, bda.getConvention());
    });
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FxSingleTradeCsvPlugin() {
  }

}

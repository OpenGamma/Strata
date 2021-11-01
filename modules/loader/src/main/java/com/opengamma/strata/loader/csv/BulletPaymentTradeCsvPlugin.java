/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Handles the CSV file format for Bullet Payment trades.
 */
final class BulletPaymentTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<BulletPaymentTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final BulletPaymentTradeCsvPlugin INSTANCE = new BulletPaymentTradeCsvPlugin();

  /** The headers. */
  private static final Set<String> HEADERS = ImmutableSet.of(
          DIRECTION_FIELD,
          CURRENCY_FIELD,
          NOTIONAL_FIELD,
          PAYMENT_DATE_FIELD,
          PAYMENT_DATE_CNV_FIELD,
          PAYMENT_DATE_CAL_FIELD);

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("BULLET", "BULLETPAYMENT", "BULLET PAYMENT");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(BulletPaymentTrade.class)) {
      return Optional.of(resolver.parseBulletPaymentTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return BulletPaymentTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(BulletPaymentTrade.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static BulletPaymentTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    BulletPaymentTrade trade = parseRow(row, info);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static BulletPaymentTrade parseRow(CsvRow row, TradeInfo info) {
    CurrencyAmount amount = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, CURRENCY_FIELD, NOTIONAL_FIELD, DIRECTION_FIELD);
    AdjustableDate date = CsvLoaderUtils.parseAdjustableDate(
        row, PAYMENT_DATE_FIELD, PAYMENT_DATE_CNV_FIELD, PAYMENT_DATE_CAL_FIELD, FOLLOWING, amount.getCurrency());

    BulletPayment payment = BulletPayment.builder()
        .payReceive(PayReceive.ofSignedAmount(amount.getAmount()))
        .value(amount.positive())
        .date(date)
        .build();
    return BulletPaymentTrade.of(info, payment);
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<BulletPaymentTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, BulletPaymentTrade trade) {
    BulletPayment product = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "BulletPayment");
    csv.writeCell(DIRECTION_FIELD, product.getPayReceive());
    csv.writeCell(CURRENCY_FIELD, product.getValue().getCurrency());
    csv.writeCell(NOTIONAL_FIELD, product.getValue().getAmount());
    csv.writeCell(PAYMENT_DATE_FIELD, product.getDate().getUnadjusted());
    csv.writeCell(PAYMENT_DATE_CAL_FIELD, product.getDate().getAdjustment().getCalendar());
    csv.writeCell(PAYMENT_DATE_CNV_FIELD, product.getDate().getAdjustment().getConvention());
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private BulletPaymentTradeCsvPlugin() {
  }

}

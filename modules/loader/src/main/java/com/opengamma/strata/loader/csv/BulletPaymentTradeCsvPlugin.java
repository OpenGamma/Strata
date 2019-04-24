/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Handles the CSV file format for Bullet Payment trades.
 */
final class BulletPaymentTradeCsvPlugin implements TradeTypeCsvWriter<BulletPaymentTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final BulletPaymentTradeCsvPlugin INSTANCE = new BulletPaymentTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(DIRECTION_FIELD)
      .add(CURRENCY_FIELD)
      .add(NOTIONAL_FIELD)
      .add(PAYMENT_DATE_FIELD)
      .add(PAYMENT_DATE_CNV_FIELD)
      .add(PAYMENT_DATE_CAL_FIELD)
      .build();

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
    BulletPaymentTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static BulletPaymentTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
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
  public List<String> headers(List<BulletPaymentTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, BulletPaymentTrade trade) {
    BulletPayment product = trade.getProduct();
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "BulletPayment");
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

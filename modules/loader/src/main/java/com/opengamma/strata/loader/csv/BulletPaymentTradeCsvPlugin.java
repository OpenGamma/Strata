/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Loads BulletPayment trades from CSV files.
 */
final class BulletPaymentTradeCsvPlugin {

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
        row, PAYMENT_DATE_FIELD, PAYMENT_DATE_CNV_FIELD, PAYMENT_DATE_CAL_FIELD, amount.getCurrency());

    BulletPayment payment = BulletPayment.builder()
        .payReceive(PayReceive.ofSignedAmount(amount.getAmount()))
        .value(amount.positive())
        .date(date)
        .build();
    return BulletPaymentTrade.of(info, payment);
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private BulletPaymentTradeCsvPlugin() {
  }

}

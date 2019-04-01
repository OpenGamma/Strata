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
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
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
    PayReceive payReceive = LoaderUtils.parsePayReceive(row.getValue(DIRECTION_FIELD));
    Currency currency = LoaderUtils.parseCurrency(row.getValue(CURRENCY_FIELD));
    double notional = LoaderUtils.parseDouble(row.getValue(NOTIONAL_FIELD));
    LocalDate paymentDate = LoaderUtils.parseDate(row.getValue(PAYMENT_DATE_FIELD));
    BusinessDayAdjustment paymentAdj = FxSingleTradeCsvPlugin.parsePaymentDateAdjustment(row)
        .orElseGet(() -> BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarId.defaultByCurrency(currency)));
    CurrencyAmount amount = CurrencyAmount.of(currency, notional);
    BulletPayment.Builder builder = BulletPayment.builder()
        .payReceive(payReceive)
        .value(amount)
        .date(AdjustableDate.of(paymentDate, paymentAdj));
    return BulletPaymentTrade.of(info, builder.build());
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private BulletPaymentTradeCsvPlugin() {
  }

}

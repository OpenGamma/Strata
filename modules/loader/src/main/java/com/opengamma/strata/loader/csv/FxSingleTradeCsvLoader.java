/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.product.common.PayReceive.PAY;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Loads FX trades (spot of forward) from CSV files.
 */
class FxSingleTradeCsvLoader {
  
  private static final String PAYMENT_DATE_HEADER = "Payment Date";
  private static final String PAYMENT_DATE_CNV_HEADER = "Payment Date Convention";  // Optional
  private static final String PAYMENT_DATE_CAL_HEADER = "Payment Date Calendar";  // Optional
  private static final String LEG_1_DIRECTION_HEADER = "Leg 1 Direction";
  private static final String LEG_1_CURRENCY_HEADER = "Leg 1 Currency";
  private static final String LEG_1_NOTIONAL_HEADER = "Leg 1 Notional";
  private static final String LEG_2_DIRECTION_HEADER = "Leg 2 Direction";
  private static final String LEG_2_CURRENCY_HEADER = "Leg 2 Currency";
  private static final String LEG_2_NOTIONAL_HEADER = "Leg 2 Notional";

  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information. This is not currently used in this method.
   * @return the parsed trade, as an instance of {@link FxSingleTrade}
   */
  static FxSingleTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSingleTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information. This is not currently used in this method.
   * @return the parsed trade, as an instance of {@link FxSingleTrade}
   */
  private static FxSingleTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    LocalDate paymentDate = LoaderUtils.parseDate(row.getField(PAYMENT_DATE_HEADER));
    PayReceive leg1Direction = LoaderUtils.parsePayReceive(row.getField(LEG_1_DIRECTION_HEADER));
    Currency leg1Currency = Currency.of(row.getField(LEG_1_CURRENCY_HEADER));
    double leg1Notional = LoaderUtils.parseDouble(row.getField(LEG_1_NOTIONAL_HEADER));
    PayReceive leg2Direction = LoaderUtils.parsePayReceive(row.getField(LEG_2_DIRECTION_HEADER));
    Currency leg2Currency = Currency.of(row.getField(LEG_2_CURRENCY_HEADER));
    double leg2Notional = LoaderUtils.parseDouble(row.getField(LEG_2_NOTIONAL_HEADER));
    if (leg1Direction.equals(leg2Direction)) {
      throw new IllegalArgumentException(Messages.format(
          "Detected two legs having the same direction: {}, {}.",
          leg1Direction.toString(),
          leg2Direction.toString()));
    }
    int leg1DirectionMultiplier = leg1Direction.equals(PAY) ? -1 : 1;
    int leg2DirectionMultiplier = leg2Direction.equals(PAY) ? -1 : 1;
    CurrencyAmount firstLeg = CurrencyAmount.of(leg1Currency, leg1DirectionMultiplier * leg1Notional);
    CurrencyAmount secondLeg = CurrencyAmount.of(leg2Currency, leg2DirectionMultiplier * leg2Notional);
    FxSingle fx = null;
    Optional<String> paymentDateCnv = row.findField(PAYMENT_DATE_CNV_HEADER); // Optional field with Business day adjustment
    boolean adjustment = false;
    if (paymentDateCnv.isPresent() && !paymentDateCnv.get().isEmpty()) {
      BusinessDayConvention bdCnv = LoaderUtils.parseBusinessDayConvention(paymentDateCnv.get());
      if (!bdCnv.equals(BusinessDayConventions.NO_ADJUST)) {
        String paymentDateCal = row.getField(PAYMENT_DATE_CAL_HEADER);
        BusinessDayAdjustment paymentDateAdjustment = BusinessDayAdjustment
            .of(LoaderUtils.parseBusinessDayConvention(paymentDateCnv.get()), HolidayCalendarId.of(paymentDateCal));
        fx = FxSingle.of(firstLeg, secondLeg, paymentDate, paymentDateAdjustment);
        adjustment = true;
      }
    }
    if (!adjustment) {
      fx = FxSingle.of(firstLeg, secondLeg, paymentDate);
    }
    return FxSingleTrade.builder()
        .info(info)
        .product(fx)
        .build();
  }
  
}

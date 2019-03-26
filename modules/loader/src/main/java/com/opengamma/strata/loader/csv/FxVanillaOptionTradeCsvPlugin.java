/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_TIME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.LONG_SHORT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DIRECTION_FIELD;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * Loads FX vanilla option trades from CSV files.
 */
class FxVanillaOptionTradeCsvPlugin {

  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static FxVanillaOptionTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSingleTrade singleTrade = FxSingleTradeCsvPlugin.parseRow(row, info);
    FxVanillaOptionTrade trade = parseRow(row, info, singleTrade.getProduct());
    return resolver.completeTrade(row, trade);
  }

  // parses the trade
  private static FxVanillaOptionTrade parseRow(CsvRow row, TradeInfo info, FxSingle underlying) {
    LongShort longShort = LoaderUtils.parseLongShort(row.getValue(LONG_SHORT_FIELD));
    LocalDate expiryDate = LoaderUtils.parseDate(row.getValue(EXPIRY_DATE_FIELD));
    LocalTime expiryTime = LoaderUtils.parseTime(row.getValue(EXPIRY_TIME_FIELD));
    ZoneId expiryZone = LoaderUtils.parseZoneId(row.getValue(EXPIRY_ZONE_FIELD));
    CurrencyAmount amount = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, PREMIUM_CURRENCY_FIELD, PREMIUM_AMOUNT_FIELD, PREMIUM_DIRECTION_FIELD);
    AdjustableDate date = CsvLoaderUtils.parseAdjustableDate(
        row, PREMIUM_DATE_FIELD, PREMIUM_DATE_CNV_FIELD, PREMIUM_DATE_CAL_FIELD);
    AdjustablePayment premium = AdjustablePayment.of(amount, date);

    FxVanillaOption option = FxVanillaOption.builder()
        .longShort(longShort)
        .expiryDate(expiryDate)
        .expiryTime(expiryTime)
        .expiryZone(expiryZone)
        .underlying(underlying)
        .build();
    return FxVanillaOptionTrade.builder()
        .info(info)
        .product(option)
        .premium(premium)
        .build();
  }

}

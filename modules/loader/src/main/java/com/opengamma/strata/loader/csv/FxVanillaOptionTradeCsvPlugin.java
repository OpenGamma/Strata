/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_1_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_1_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_1_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_1_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_2_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_2_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_2_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.FxSingleTradeCsvPlugin.LEG_2_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_TIME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.LONG_SHORT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DIRECTION_FIELD;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * Handles the CSV file format for FX vanilla option trades.
 */
class FxVanillaOptionTradeCsvPlugin implements TradeTypeCsvWriter<FxVanillaOptionTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FxVanillaOptionTradeCsvPlugin INSTANCE = new FxVanillaOptionTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(LONG_SHORT_FIELD)
      .add(EXPIRY_DATE_FIELD)
      .add(EXPIRY_TIME_FIELD)
      .add(EXPIRY_ZONE_FIELD)
      .add(PREMIUM_DATE_FIELD)
      .add(PREMIUM_DATE_CNV_FIELD)
      .add(PREMIUM_DATE_CAL_FIELD)
      .add(PREMIUM_DIRECTION_FIELD)
      .add(PREMIUM_CURRENCY_FIELD)
      .add(PREMIUM_AMOUNT_FIELD)
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
  static FxVanillaOptionTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSingleTrade singleTrade = FxSingleTradeCsvPlugin.parseRow(row, info);
    FxVanillaOptionTrade trade = parseRow(row, info, singleTrade.getProduct());
    return resolver.completeTrade(row, trade);
  }

  // parses the trade
  private static FxVanillaOptionTrade parseRow(CsvRow row, TradeInfo info, FxSingle underlying) {
    LongShort longShort = row.getValue(LONG_SHORT_FIELD, LoaderUtils::parseLongShort);
    LocalDate expiryDate = row.getValue(EXPIRY_DATE_FIELD, LoaderUtils::parseDate);
    LocalTime expiryTime = row.getValue(EXPIRY_TIME_FIELD, LoaderUtils::parseTime);
    ZoneId expiryZone = row.getValue(EXPIRY_ZONE_FIELD, LoaderUtils::parseZoneId);
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

  //-------------------------------------------------------------------------
  @Override
  public List<String> headers(List<FxVanillaOptionTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxVanillaOptionTrade trade) {
    FxVanillaOption product = trade.getProduct();
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "FxVanillaOption");
    csv.writeCell(TradeCsvLoader.LONG_SHORT_FIELD, product.getLongShort());
    csv.writeCell(TradeCsvLoader.EXPIRY_DATE_FIELD, product.getExpiryDate());
    csv.writeCell(TradeCsvLoader.EXPIRY_TIME_FIELD, product.getExpiryTime());
    csv.writeCell(TradeCsvLoader.EXPIRY_ZONE_FIELD, product.getExpiryZone());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_FIELD, trade.getPremium().getDate().getUnadjusted());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_CNV_FIELD, trade.getPremium().getDate().getAdjustment().getConvention());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_CAL_FIELD, trade.getPremium().getDate().getAdjustment().getCalendar());
    csv.writeCell(TradeCsvLoader.PREMIUM_DIRECTION_FIELD, PayReceive.ofSignedAmount(trade.getPremium().getAmount()));
    csv.writeCell(TradeCsvLoader.PREMIUM_CURRENCY_FIELD, trade.getPremium().getCurrency());
    csv.writeCell(TradeCsvLoader.PREMIUM_AMOUNT_FIELD, trade.getPremium().getAmount());
    FxSingleTradeCsvPlugin.INSTANCE.writeProduct(csv, "", product.getUnderlying());
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FxVanillaOptionTradeCsvPlugin() {
  }

}

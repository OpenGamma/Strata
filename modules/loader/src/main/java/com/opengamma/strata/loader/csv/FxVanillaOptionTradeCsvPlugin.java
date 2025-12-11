/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_TIME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LONG_SHORT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PUT_CALL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STRIKE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * Handles the CSV file format for FX vanilla option trades.
 */
class FxVanillaOptionTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<FxVanillaOptionTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FxVanillaOptionTradeCsvPlugin INSTANCE = new FxVanillaOptionTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.of(
      LONG_SHORT_FIELD,
      PUT_CALL_FIELD,
      STRIKE_FIELD,
      EXPIRY_DATE_FIELD,
      EXPIRY_TIME_FIELD,
      EXPIRY_ZONE_FIELD,
      PREMIUM_DATE_FIELD,
      PREMIUM_DATE_CNV_FIELD,
      PREMIUM_DATE_CAL_FIELD,
      PREMIUM_DIRECTION_FIELD,
      PREMIUM_CURRENCY_FIELD,
      PREMIUM_AMOUNT_FIELD,
      LEG_1_CURRENCY_FIELD,
      LEG_1_NOTIONAL_FIELD,
      LEG_2_CURRENCY_FIELD,
      PAYMENT_DATE_FIELD,
      PAYMENT_DATE_CNV_FIELD,
      PAYMENT_DATE_CAL_FIELD);

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("FXVANILLAOPTION", "FX VANILLA OPTION");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(FxVanillaOptionTrade.class)) {
      return Optional.of(resolver.parseFxVanillaOptionTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return FxVanillaOptionTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(FxVanillaOptionTrade.class);
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
  static FxVanillaOptionTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    if (row.findValue(PUT_CALL_FIELD).isPresent()) {
      FxVanillaOptionTrade trade = parseRow(row, info);
      return resolver.completeTrade(row, trade);
    }
    FxSingleTrade singleTrade = FxSingleTradeCsvPlugin.parseRow(row, info);
    FxVanillaOptionTrade trade = parseRow(row, info, singleTrade.getProduct());
    return resolver.completeTrade(row, trade);
  }

  // parses the trade from the underlying FX single
  private static FxVanillaOptionTrade parseRow(CsvRow row, TradeInfo info, FxSingle underlying) {
    LongShort longShort = row.getValue(LONG_SHORT_FIELD, LoaderUtils::parseLongShort);
    LocalDate expiryDate = row.getValue(EXPIRY_DATE_FIELD, LoaderUtils::parseDate);
    LocalTime expiryTime = row.getValue(EXPIRY_TIME_FIELD, LoaderUtils::parseTime);
    ZoneId expiryZone = row.getValue(EXPIRY_ZONE_FIELD, LoaderUtils::parseZoneId);

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
        .premium(CsvLoaderUtils.tryParsePremiumFromDefaultFields(row)
            .orElse(AdjustablePayment.of(option.getCurrencyPair().getBase(), 0d, expiryDate)))
        .build();
  }

  // parse the trade from the currency pairs, base currency notional, and put/call
  private static FxVanillaOptionTrade parseRow(CsvRow row, TradeInfo info) {
    LongShort longShort = row.getValue(LONG_SHORT_FIELD, LoaderUtils::parseLongShort);
    LocalDate expiryDate = row.getValue(EXPIRY_DATE_FIELD, LoaderUtils::parseDate);
    LocalTime expiryTime = row.getValue(EXPIRY_TIME_FIELD, LoaderUtils::parseTime);
    ZoneId expiryZone = row.getValue(EXPIRY_ZONE_FIELD, LoaderUtils::parseZoneId);
    PutCall putCall = row.getValue(PUT_CALL_FIELD, LoaderUtils::parsePutCall);
    double strike = row.getValue(STRIKE_FIELD, LoaderUtils::parseDouble);
    Currency ccy1 = row.getValue(LEG_1_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    Currency ccy2 = row.getValue(LEG_2_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    LocalDate paymentDate = row.getValue(PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    double notional1 = row.getValue(LEG_1_NOTIONAL_FIELD, LoaderUtils::parseDouble);
    Optional<BusinessDayAdjustment> paymentAdj = CsvLoaderUtils
        .parseBusinessDayAdjustment(row, PAYMENT_DATE_CNV_FIELD, PAYMENT_DATE_CAL_FIELD)
        .filter(adj -> !adj.equals(BusinessDayAdjustment.NONE));

    ZonedDateTime expiry = expiryDate.atTime(expiryTime).atZone(expiryZone);
    CurrencyPair ccyPair = CurrencyPair.of(ccy1, ccy2);
    FxVanillaOption option = paymentAdj
        .map(adj -> FxVanillaOption.of(longShort, expiry, ccyPair, putCall, strike, notional1, paymentDate, adj))
        .orElseGet(() -> FxVanillaOption.of(longShort, expiry, ccyPair, putCall, strike, notional1, paymentDate));
    return FxVanillaOptionTrade.builder()
        .info(info)
        .product(option)
        .premium(CsvLoaderUtils.tryParsePremiumFromDefaultFields(row)
            .orElse(AdjustablePayment.of(option.getCurrencyPair().getBase(), 0d, expiryDate)))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<FxVanillaOptionTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxVanillaOptionTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "FxVanillaOption");
    writeFxVanillaOption(csv, trade.getProduct());
    CsvWriterUtils.writePremiumFields(csv, trade.getPremium());
    csv.writeNewLine();
  }

  protected void writeFxVanillaOption(CsvOutput.CsvRowOutputWithHeaders csv, FxVanillaOption product) {
    FxSingle underlying = product.getUnderlying();
    PutCall putCall = underlying.getCounterCurrencyPayment().getAmount() > 0d ? PutCall.PUT : PutCall.CALL;
    double strike = Math.abs(underlying.getCounterCurrencyPayment().getAmount() /
        underlying.getBaseCurrencyPayment().getAmount());
    csv.writeCell(LONG_SHORT_FIELD, product.getLongShort());
    csv.writeCell(PUT_CALL_FIELD, putCall);
    csv.writeCell(STRIKE_FIELD, strike);
    csv.writeCell(EXPIRY_DATE_FIELD, product.getExpiryDate());
    csv.writeCell(EXPIRY_TIME_FIELD, product.getExpiryTime());
    csv.writeCell(EXPIRY_ZONE_FIELD, product.getExpiryZone());
    csv.writeCell(LEG_1_CURRENCY_FIELD, underlying.getBaseCurrencyPayment().getCurrency());
    csv.writeCell(LEG_1_NOTIONAL_FIELD, Math.abs(underlying.getBaseCurrencyPayment().getAmount()));
    csv.writeCell(LEG_2_CURRENCY_FIELD, underlying.getCounterCurrencyPayment().getCurrency());
    csv.writeCell(PAYMENT_DATE_FIELD, underlying.getBaseCurrencyPayment().getDate());
    underlying.getPaymentDateAdjustment().ifPresent(bda -> {
      csv.writeCell(PAYMENT_DATE_CAL_FIELD, bda.getCalendar());
      csv.writeCell(PAYMENT_DATE_CNV_FIELD, bda.getConvention());
    });
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FxVanillaOptionTradeCsvPlugin() {
  }

}

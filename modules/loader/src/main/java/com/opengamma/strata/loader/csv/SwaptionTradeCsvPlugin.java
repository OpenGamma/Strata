/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_TIME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LONG_SHORT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYOFF_SETTLEMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYOFF_SETTLEMENT_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.FullSwapTradeCsvPlugin.VariableElements;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Handles the CSV file format for Swaption trades.
 */
final class SwaptionTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<SwaptionTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final SwaptionTradeCsvPlugin INSTANCE = new SwaptionTradeCsvPlugin();

  private static final String PHYSICAL = "PHYSICAL";

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("SWAPTION");
  }

  @Override
  public boolean isAdditionalRow(CsvRow baseRow, CsvRow additionalRow) {
    return additionalRow.getField(TRADE_TYPE_FIELD).toUpperCase(Locale.ENGLISH).equals("VARIABLE");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(SwaptionTrade.class)) {
      return Optional.of(resolver.parseSwaptionTrade(baseRow, additionalRows, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return SwaptionTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(SwaptionTrade.class);
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
  static SwaptionTrade parse(CsvRow row, List<CsvRow> variableRows, TradeInfo info, TradeCsvInfoResolver resolver) {
    SwapTrade swapTrade = SwapTradeCsvPlugin.parseSwap(row, variableRows, info, resolver.getReferenceData());
    SwaptionTrade trade = parseRow(row, info, swapTrade.getProduct());
    return resolver.completeTrade(row, trade);
  }

  //-------------------------------------------------------------------------
  // parse the row to a trade
  private static SwaptionTrade parseRow(CsvRow row, TradeInfo info, Swap underlying) {
    LongShort longShort = LoaderUtils.parseLongShort(row.getValue(LONG_SHORT_FIELD));
    SwaptionSettlement settlement = parseSettlement(row);
    AdjustableDate expiryDate = CsvLoaderUtils.parseAdjustableDate(
        row, EXPIRY_DATE_FIELD, EXPIRY_DATE_CNV_FIELD, EXPIRY_DATE_CAL_FIELD);
    LocalTime expiryTime = row.getValue(EXPIRY_TIME_FIELD, LoaderUtils::parseTime);
    ZoneId expiryZone = row.getValue(EXPIRY_ZONE_FIELD, LoaderUtils::parseZoneId);
    AdjustablePayment premium = CsvLoaderUtils.tryParsePremiumFromDefaultFields(row)
        .orElse(AdjustablePayment.of(underlying.getLegs().get(0).getCurrency(), 0d, expiryDate));

    Swaption swaption = Swaption.builder()
        .longShort(longShort)
        .swaptionSettlement(settlement)
        .expiryDate(expiryDate)
        .expiryTime(expiryTime)
        .expiryZone(expiryZone)
        .underlying(underlying)
        .build();
    return SwaptionTrade.builder()
        .info(info)
        .product(swaption)
        .premium(premium)
        .build();
  }

  // parses the settlement cash/physical
  private static SwaptionSettlement parseSettlement(CsvRow row) {
    String settlementType = row.getValue(PAYOFF_SETTLEMENT_TYPE_FIELD);
    if (settlementType.equalsIgnoreCase(PHYSICAL)) {
      return PhysicalSwaptionSettlement.DEFAULT;
    }
    CashSwaptionSettlementMethod method = CashSwaptionSettlementMethod.of(settlementType);
    LocalDate date = row.getValue(PAYOFF_SETTLEMENT_DATE_FIELD, LoaderUtils::parseDate);
    return CashSwaptionSettlement.of(date, method);
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<SwaptionTrade> trades) {
    LinkedHashSet<String> headers = new LinkedHashSet<>(
        FullSwapTradeCsvPlugin.INSTANCE.headers(trades.stream()
        .map(t -> t.getProduct().getUnderlying())
        .map(swap -> SwapTrade.of(TradeInfo.empty(), swap))
        .collect(toImmutableList())));
    headers.add(LONG_SHORT_FIELD);
    headers.add(PAYOFF_SETTLEMENT_TYPE_FIELD);
    headers.add(PAYOFF_SETTLEMENT_DATE_FIELD);
    headers.add(EXPIRY_DATE_FIELD);
    if (trades.stream()
        .anyMatch(trade -> !trade.getProduct().getExpiryDate().getAdjustment().equals(BusinessDayAdjustment.NONE))) {
      headers.add(EXPIRY_DATE_CNV_FIELD);
      headers.add(EXPIRY_DATE_CAL_FIELD);
    }
    headers.add(EXPIRY_TIME_FIELD);
    headers.add(EXPIRY_ZONE_FIELD);
    headers.add(PREMIUM_DATE_FIELD);
    headers.add(PREMIUM_DATE_CNV_FIELD);
    headers.add(PREMIUM_DATE_CAL_FIELD);
    headers.add(PREMIUM_DIRECTION_FIELD);
    headers.add(PREMIUM_CURRENCY_FIELD);
    headers.add(PREMIUM_AMOUNT_FIELD);
    return headers;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, SwaptionTrade trade) {
    Swaption product = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "Swaption");
    VariableElements variableElements = FullSwapTradeCsvPlugin.INSTANCE.writeProduct(csv, product.getUnderlying());
    csv.writeCell(LONG_SHORT_FIELD, product.getLongShort());
    writeSettlement(csv, product);
    csv.writeCell(EXPIRY_DATE_FIELD, product.getExpiryDate().getUnadjusted());
    if (!product.getExpiryDate().getAdjustment().equals(BusinessDayAdjustment.NONE)) {
      csv.writeCell(EXPIRY_DATE_CNV_FIELD, product.getExpiryDate().getAdjustment().getConvention());
      csv.writeCell(EXPIRY_DATE_CAL_FIELD, product.getExpiryDate().getAdjustment().getCalendar());
    }
    csv.writeCell(EXPIRY_TIME_FIELD, product.getExpiryTime());
    csv.writeCell(EXPIRY_ZONE_FIELD, product.getExpiryZone().getId());
    CsvWriterUtils.writePremiumFields(csv, trade.getPremium());
    csv.writeNewLine();
    variableElements.writeLines(csv);
  }

  private void writeSettlement(CsvRowOutputWithHeaders csv, Swaption product) {
    if (product.getSwaptionSettlement() instanceof CashSwaptionSettlement) {
      CashSwaptionSettlement cashSettle = (CashSwaptionSettlement) product.getSwaptionSettlement();
      csv.writeCell(PAYOFF_SETTLEMENT_TYPE_FIELD, cashSettle.getMethod().toString());
      csv.writeCell(PAYOFF_SETTLEMENT_DATE_FIELD, cashSettle.getSettlementDate());
    } else {
      // default to physical (FpML does this)
      csv.writeCell(PAYOFF_SETTLEMENT_TYPE_FIELD, PHYSICAL);
    }
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SwaptionTradeCsvPlugin() {
  }

}

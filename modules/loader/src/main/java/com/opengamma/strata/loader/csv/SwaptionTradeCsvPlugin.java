/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.EXPIRY_DATE_CNV_FIELD;
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
import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.FullSwapTradeCsvPlugin.VariableElements;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
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
final class SwaptionTradeCsvPlugin implements TradeTypeCsvWriter<SwaptionTrade> {

  /**
   * The singleton instance of the plugin.
   */
  static final SwaptionTradeCsvPlugin INSTANCE = new SwaptionTradeCsvPlugin();

  private static final String PAYOFF_SETTLEMENT_TYPE_FIELD = "Payoff Settlement Type";
  private static final String PAYOFF_SETTLEMENT_DATE_FIELD = "Payoff Settlement Date";
  private static final String PHYSICAL = "PHYSICAL";

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
    CurrencyAmount amount = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, PREMIUM_CURRENCY_FIELD, PREMIUM_AMOUNT_FIELD, PREMIUM_DIRECTION_FIELD);
    AdjustableDate date = CsvLoaderUtils.parseAdjustableDate(
        row, PREMIUM_DATE_FIELD, PREMIUM_DATE_CNV_FIELD, PREMIUM_DATE_CAL_FIELD);
    AdjustablePayment premium = AdjustablePayment.of(amount, date);

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
  public List<String> headers(List<SwaptionTrade> trades) {
    List<String> headers = new ArrayList<>();
    headers.addAll(FullSwapTradeCsvPlugin.INSTANCE.headers(trades.stream()
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
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "Swaption");
    VariableElements variableElements = FullSwapTradeCsvPlugin.INSTANCE.writeProduct(csv, product.getUnderlying());
    csv.writeCell(TradeCsvLoader.LONG_SHORT_FIELD, product.getLongShort());
    writeSettlement(csv, product);
    csv.writeCell(TradeCsvLoader.EXPIRY_DATE_FIELD, product.getExpiryDate().getUnadjusted());
    if (!product.getExpiryDate().getAdjustment().equals(BusinessDayAdjustment.NONE)) {
      csv.writeCell(TradeCsvLoader.EXPIRY_DATE_CNV_FIELD, product.getExpiryDate().getAdjustment().getConvention());
      csv.writeCell(TradeCsvLoader.EXPIRY_DATE_CAL_FIELD, product.getExpiryDate().getAdjustment().getCalendar());
    }
    csv.writeCell(TradeCsvLoader.EXPIRY_TIME_FIELD, product.getExpiryTime());
    csv.writeCell(TradeCsvLoader.EXPIRY_ZONE_FIELD, product.getExpiryZone().getId());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_FIELD, trade.getPremium().getDate().getUnadjusted());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_CNV_FIELD, trade.getPremium().getDate().getAdjustment().getConvention());
    csv.writeCell(TradeCsvLoader.PREMIUM_DATE_CAL_FIELD, trade.getPremium().getDate().getAdjustment().getCalendar());
    csv.writeCell(TradeCsvLoader.PREMIUM_DIRECTION_FIELD, PayReceive.ofSignedAmount(trade.getPremium().getAmount()));
    csv.writeCell(TradeCsvLoader.PREMIUM_CURRENCY_FIELD, trade.getPremium().getCurrency());
    csv.writeCell(TradeCsvLoader.PREMIUM_AMOUNT_FIELD, trade.getPremium().getAmount());
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

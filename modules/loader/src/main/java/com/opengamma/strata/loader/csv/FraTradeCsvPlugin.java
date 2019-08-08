/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FRA_DISCOUNTING_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PERIOD_TO_START_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.type.FraConvention;

/**
 * Handles the CSV file format for FRA trades.
 */
final class FraTradeCsvPlugin implements TradeTypeCsvWriter<FraTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FraTradeCsvPlugin INSTANCE = new FraTradeCsvPlugin();

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static FraTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FraTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static FraTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    Optional<Currency> currencyOpt = row.findValue(CURRENCY_FIELD).map(s -> Currency.of(s));
    double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
    double fixedRate = row.getValue(FIXED_RATE_FIELD, LoaderUtils::parseDoublePercent);
    Optional<FraConvention> conventionOpt = row.findValue(CONVENTION_FIELD).map(s -> FraConvention.of(s));
    Optional<Period> periodToStartOpt = row.findValue(PERIOD_TO_START_FIELD).map(s -> LoaderUtils.parsePeriod(s));
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<IborIndex> indexOpt = row.findValue(INDEX_FIELD).map(s -> IborIndex.of(s));
    Optional<IborIndex> interpolatedOpt = row.findValue(INTERPOLATED_INDEX_FIELD).map(s -> IborIndex.of(s));
    Optional<DayCount> dayCountOpt = row.findValue(DAY_COUNT_FIELD).map(s -> LoaderUtils.parseDayCount(s));
    Optional<FraDiscountingMethod> discMethodOpt =
        row.findValue(FRA_DISCOUNTING_FIELD).map(s -> FraDiscountingMethod.of(s));
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(s -> LoaderUtils.parseBusinessDayConvention(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    Optional<HolidayCalendarId> dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD).map(s -> HolidayCalendarId.of(s));
    // not parsing paymentDate, fixingDateOffset, discounting

    // use convention if available
    if (conventionOpt.isPresent()) {
      if (indexOpt.isPresent() || interpolatedOpt.isPresent() || dayCountOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Fra trade had invalid combination of fields. When '" + CONVENTION_FIELD +
                "' is present these fields must not be present: " +
                ImmutableList.of(INDEX_FIELD, INTERPOLATED_INDEX_FIELD, DAY_COUNT_FIELD));
      }
      FraConvention convention = conventionOpt.get();
      // explicit dates take precedence over relative ones
      if (startDateOpt.isPresent() && endDateOpt.isPresent()) {
        if (periodToStartOpt.isPresent()) {
          throw new IllegalArgumentException(
              "Fra trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(PERIOD_TO_START_FIELD));
        }
        LocalDate startDate = startDateOpt.get();
        LocalDate endDate = endDateOpt.get();
        // NOTE: payment date assumed to be the start date
        FraTrade trade = convention.toTrade(info, startDate, endDate, startDate, buySell, notional, fixedRate);
        return adjustTrade(trade, currencyOpt, discMethodOpt, dateCnv, dateCalOpt);
      }
      // relative dates
      if (periodToStartOpt.isPresent() && info.getTradeDate().isPresent()) {
        if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
          throw new IllegalArgumentException(
              "Fra trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, PERIOD_TO_START_FIELD, TRADE_DATE_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD));
        }
        LocalDate tradeDate = info.getTradeDate().get();
        Period periodToStart = periodToStartOpt.get();
        FraTrade trade =
            convention.createTrade(tradeDate, periodToStart, buySell, notional, fixedRate, resolver.getReferenceData());
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, currencyOpt, discMethodOpt, dateCnv, dateCalOpt);
      }

    } else if (startDateOpt.isPresent() && endDateOpt.isPresent() && indexOpt.isPresent()) {
      LocalDate startDate = startDateOpt.get();
      LocalDate endDate = endDateOpt.get();
      IborIndex index = indexOpt.get();
      Fra.Builder builder = Fra.builder()
          .buySell(buySell)
          .notional(notional)
          .startDate(startDate)
          .endDate(endDate)
          .fixedRate(fixedRate)
          .index(index);
      interpolatedOpt.ifPresent(interpolated -> builder.indexInterpolated(interpolated));
      dayCountOpt.ifPresent(dayCount -> builder.dayCount(dayCount));
      return adjustTrade(FraTrade.of(info, builder.build()), currencyOpt, discMethodOpt, dateCnv, dateCalOpt);
    }
    // no match
    throw new IllegalArgumentException(
        "Fra trade had invalid combination of fields. These fields are mandatory:" +
            ImmutableList.of(BUY_SELL_FIELD, NOTIONAL_FIELD, FIXED_RATE_FIELD) +
            " and one of these combinations is mandatory: " +
            ImmutableList.of(CONVENTION_FIELD, TRADE_DATE_FIELD, PERIOD_TO_START_FIELD) +
            " or " +
            ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
            " or " +
            ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD, INDEX_FIELD));
  }

  // adjust trade based on additional fields specified
  private static FraTrade adjustTrade(
      FraTrade trade,
      Optional<Currency> currencyOpt,
      Optional<FraDiscountingMethod> discMethodOpt,
      BusinessDayConvention dateCnv,
      Optional<HolidayCalendarId> dateCalOpt) {

    if (!currencyOpt.isPresent() && !discMethodOpt.isPresent() && !dateCalOpt.isPresent()) {
      return trade;
    }
    Fra.Builder builder = trade.getProduct().toBuilder();
    currencyOpt.ifPresent(currency -> builder.currency(currency));
    discMethodOpt.ifPresent(discMethod -> builder.discounting(discMethod));
    dateCalOpt.ifPresent(cal -> builder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
    return trade.toBuilder()
        .product(builder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public List<String> headers(List<FraTrade> trades) {
    List<String> headers = new ArrayList<>();
    headers.add(BUY_SELL_FIELD);
    headers.add(START_DATE_FIELD);
    headers.add(END_DATE_FIELD);
    if (trades.stream()
        .anyMatch(trade -> !trade.getProduct().getCurrency().equals(trade.getProduct().getIndex().getCurrency()))) {
      headers.add(CURRENCY_FIELD);
    }
    headers.add(NOTIONAL_FIELD);
    headers.add(FIXED_RATE_FIELD);
    headers.add(INDEX_FIELD);
    if (trades.stream().anyMatch(trade -> !trade.getProduct().getIndexInterpolated().isPresent())) {
      headers.add(INTERPOLATED_INDEX_FIELD);
    }
    if (trades.stream()
        .anyMatch(trade -> !trade.getProduct().getDayCount().equals(trade.getProduct().getIndex().getDayCount()))) {
      headers.add(DAY_COUNT_FIELD);
    }
    if (trades.stream()
        .anyMatch(trade -> !trade.getProduct().getDiscounting().equals(FraDiscountingMethod.ISDA))) {
      headers.add(FRA_DISCOUNTING_FIELD);
    }
    headers.add(DATE_ADJ_CNV_FIELD);
    headers.add(DATE_ADJ_CAL_FIELD);
    headers.add(PAYMENT_DATE_FIELD);
    headers.add(PAYMENT_DATE_CNV_FIELD);
    headers.add(PAYMENT_DATE_CAL_FIELD);
    return headers;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FraTrade trade) {
    Fra product = trade.getProduct();
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "Fra");
    csv.writeCell(START_DATE_FIELD, product.getStartDate());
    csv.writeCell(END_DATE_FIELD, product.getEndDate());
    csv.writeCell(BUY_SELL_FIELD, product.getBuySell());
    if (!product.getCurrency().equals(product.getIndex().getCurrency())) {
      csv.writeCell(CURRENCY_FIELD, product.getCurrency());
    }
    csv.writeCell(NOTIONAL_FIELD, product.getNotional());
    csv.writeCell(FIXED_RATE_FIELD, formattedPercentage(product.getFixedRate()));
    csv.writeCell(INDEX_FIELD, product.getIndex());
    product.getIndexInterpolated().ifPresent(index -> csv.writeCell(INTERPOLATED_INDEX_FIELD, index));
    if (!product.getDayCount().equals(product.getIndex().getDayCount())) {
      csv.writeCell(DAY_COUNT_FIELD, product.getDayCount());
    }
    if (csv.headers().contains(FRA_DISCOUNTING_FIELD)) {
      csv.writeCell(FRA_DISCOUNTING_FIELD, product.getDiscounting());
    }
    product.getBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(DATE_ADJ_CNV_FIELD, bda.getConvention());
      csv.writeCell(DATE_ADJ_CAL_FIELD, bda.getCalendar());
    });
    csv.writeCell(PAYMENT_DATE_FIELD, product.getPaymentDate().getUnadjusted());
    csv.writeCell(PAYMENT_DATE_CAL_FIELD, product.getPaymentDate().getAdjustment().getCalendar());
    csv.writeCell(PAYMENT_DATE_CNV_FIELD, product.getPaymentDate().getAdjustment().getConvention());
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FraTradeCsvPlugin() {
  }

}

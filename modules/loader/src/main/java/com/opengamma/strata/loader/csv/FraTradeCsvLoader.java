/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PERIOD_TO_START_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.type.FraConvention;

/**
 * Loads FRA trades from CSV files.
 */
final class FraTradeCsvLoader {

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param refData  the reference data
   * @return the loaded trades, all errors are captured in the result
   */
  static FraTrade parse(CsvRow row, TradeInfo info, ReferenceData refData) {
    BuySell buySell = row.findValue(BUY_SELL_FIELD).map(s -> BuySell.of(s)).orElse(BuySell.BUY);
    double notional = TradeCsvLoader.parseDouble(row.getValue(NOTIONAL_FIELD));
    double fixedRate = TradeCsvLoader.parseDoublePercent(row.getValue(FIXED_RATE_FIELD));
    Optional<FraConvention> conventionOpt = row.findValue(CONVENTION_FIELD).map(s -> FraConvention.of(s));
    Optional<Period> periodToStartOpt = row.findValue(PERIOD_TO_START_FIELD).map(s -> Tenor.parse(s).getPeriod());
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<IborIndex> indexOpt = row.findValue(INDEX_FIELD).map(s -> IborIndex.of(s));
    Optional<IborIndex> interpolatedOpt = row.findValue(INTERPOLATED_INDEX_FIELD).map(s -> IborIndex.of(s));
    Optional<DayCount> dayCountOpt = row.findValue(DAY_COUNT_FIELD).map(s -> DayCount.of(s));
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(s -> BusinessDayConvention.of(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
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
        return adjustTrade(trade, dateCnv, dateCalOpt);
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
        FraTrade trade = convention.createTrade(tradeDate, periodToStart, buySell, notional, fixedRate, refData);
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, dateCnv, dateCalOpt);
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
      return adjustTrade(FraTrade.of(info, builder.build()), dateCnv, dateCalOpt);
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
      BusinessDayConvention dateCnv,
      Optional<HolidayCalendarId> dateCalOpt) {

    if (!dateCalOpt.isPresent()) {
      return trade;
    }
    Fra.Builder builder = trade.getProduct().toBuilder();
    dateCalOpt.ifPresent(cal -> builder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
    return trade.toBuilder()
        .product(builder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FraTradeCsvLoader() {
  }

}

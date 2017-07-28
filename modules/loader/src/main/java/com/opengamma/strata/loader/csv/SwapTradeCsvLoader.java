/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.FullSwapTradeCsvLoader.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PERIOD_TO_START_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.SingleCurrencySwapConvention;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConvention;

/**
 * Loads Swap trades from CSV files.
 */
final class SwapTradeCsvLoader {

  // CSV column headers
  private static final String ROLL_CONVENTION_FIELD = "Roll Convention";
  private static final String STUB_CONVENTION_FIELD = "Stub Convention";
  private static final String FIRST_REGULAR_START_DATE_FIELD = "First Regular Start Date";
  private static final String LAST_REGULAR_END_DATE_FIELD = "Last Regular End Date";
  private static final String FX_RATE_FIELD = "FX Rate";

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param refData  the reference data
   * @return the loaded trades, all errors are captured in the result
   */
  static SwapTrade parse(CsvRow row, TradeInfo info, ReferenceData refData) {
    Optional<String> conventionOpt = row.findValue(CONVENTION_FIELD);
    if (conventionOpt.isPresent()) {
      // using an 'unnecessary' nested class to allow parsing logic to be split
      // into smaller methods without passing lots of parameters around
      return parseWithConvention(row, info, refData, conventionOpt.get());
    } else {
      Optional<String> payReceive = row.findValue("Leg 1 " + DIRECTION_FIELD);
      if (payReceive.isPresent()) {
        return FullSwapTradeCsvLoader.parse(row, info, refData);
      }
      throw new IllegalArgumentException(
          "Swap trade had invalid combination of fields. Must include either '" +
              CONVENTION_FIELD + "' or '" + "Leg 1 " + DIRECTION_FIELD + "'");
    }
  }

  // parse a trade based on a convention
  static SwapTrade parseWithConvention(CsvRow row, TradeInfo info, ReferenceData refData, String conventionStr) {
    BuySell buySell = row.findValue(BUY_SELL_FIELD).map(s -> BuySell.of(s)).orElse(BuySell.BUY);
    double notional = TradeCsvLoader.parseDouble(row.getValue(NOTIONAL_FIELD));
    double fixedRate = TradeCsvLoader.parseDoublePercent(row.getValue(FIXED_RATE_FIELD));
    Optional<Period> periodToStartOpt = row.findValue(PERIOD_TO_START_FIELD).map(s -> Tenor.parse(s).getPeriod());
    Optional<Tenor> tenorOpt = row.findValue(TENOR_FIELD).map(s -> Tenor.parse(s));
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<RollConvention> rollCnvOpt = row.findValue(ROLL_CONVENTION_FIELD).map(s -> RollConvention.of(s));
    Optional<StubConvention> stubCnvOpt = row.findValue(STUB_CONVENTION_FIELD).map(s -> StubConvention.of(s));
    Optional<LocalDate> firstRegStartDateOpt =
        row.findValue(FIRST_REGULAR_START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<LocalDate> lastRegEndDateOpt = row.findValue(LAST_REGULAR_END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(s -> BusinessDayConvention.of(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    Optional<HolidayCalendarId> dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD).map(s -> HolidayCalendarId.of(s));
    Optional<Double> fxRateOpt = row.findValue(FX_RATE_FIELD).map(str -> TradeCsvLoader.parseDouble(str));

    // explicit dates take precedence over relative ones
    if (startDateOpt.isPresent() && endDateOpt.isPresent()) {
      if (periodToStartOpt.isPresent() || tenorOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Swap trade had invalid combination of fields. When these fields are found " +
                ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
                " then these fields must not be present " +
                ImmutableList.of(PERIOD_TO_START_FIELD, TENOR_FIELD));
      }
      LocalDate startDate = startDateOpt.get();
      LocalDate endDate = endDateOpt.get();
      if (fxRateOpt.isPresent()) {
        XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionStr);
        double notionalFlat = notional * fxRateOpt.get();
        SwapTrade trade = convention.toTrade(info, startDate, endDate, buySell, notional, notionalFlat, fixedRate);
        return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
      } else {
        SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionStr);
        SwapTrade trade = convention.toTrade(info, startDate, endDate, buySell, notional, fixedRate);
        return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
      }
    }

    // relative dates
    if (periodToStartOpt.isPresent() && tenorOpt.isPresent() && info.getTradeDate().isPresent()) {
      if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Swap trade had invalid combination of fields. When these fields are found " +
                ImmutableList.of(CONVENTION_FIELD, PERIOD_TO_START_FIELD, TENOR_FIELD, TRADE_DATE_FIELD) +
                " then these fields must not be present " +
                ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD));
      }
      LocalDate tradeDate = info.getTradeDate().get();
      Period periodToStart = periodToStartOpt.get();
      Tenor tenor = tenorOpt.get();
      if (fxRateOpt.isPresent()) {
        XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionStr);
        double notionalFlat = notional * fxRateOpt.get();
        SwapTrade trade =
            convention.createTrade(tradeDate, periodToStart, tenor, buySell, notional, notionalFlat, fixedRate, refData);
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
      } else {
        SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionStr);
        SwapTrade trade = convention.createTrade(tradeDate, periodToStart, tenor, buySell, notional, fixedRate, refData);
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
      }
    }
    
    // no match
    throw new IllegalArgumentException(
        "Swap trade had invalid combination of fields. These fields are mandatory:" +
            ImmutableList.of(BUY_SELL_FIELD, NOTIONAL_FIELD, FIXED_RATE_FIELD) +
            " and one of these combinations is mandatory: " +
            ImmutableList.of(CONVENTION_FIELD, TRADE_DATE_FIELD, PERIOD_TO_START_FIELD, TENOR_FIELD) +
            " or " +
            ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD));
  }

  // adjust trade based on additional fields specified
  private static SwapTrade adjustTrade(
      SwapTrade trade,
      Optional<RollConvention> rollConventionOpt,
      Optional<StubConvention> stubConventionOpt,
      Optional<LocalDate> firstRegularStartDateOpt,
      Optional<LocalDate> lastRegEndDateOpt,
      BusinessDayConvention dateCnv,
      Optional<HolidayCalendarId> dateCalOpt) {

    if (!rollConventionOpt.isPresent() &&
        !stubConventionOpt.isPresent() &&
        !firstRegularStartDateOpt.isPresent() &&
        !lastRegEndDateOpt.isPresent() &&
        !dateCalOpt.isPresent()) {
      return trade;
    }
    ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
    for (SwapLeg leg : trade.getProduct().getLegs()) {
      RateCalculationSwapLeg swapLeg = (RateCalculationSwapLeg) leg;
      PeriodicSchedule.Builder scheduleBuilder = swapLeg.getAccrualSchedule().toBuilder();
      rollConventionOpt.ifPresent(rc -> scheduleBuilder.rollConvention(rc));
      stubConventionOpt.ifPresent(sc -> scheduleBuilder.stubConvention(sc));
      firstRegularStartDateOpt.ifPresent(date -> scheduleBuilder.firstRegularStartDate(date));
      lastRegEndDateOpt.ifPresent(date -> scheduleBuilder.lastRegularEndDate(date));
      dateCalOpt.ifPresent(cal -> scheduleBuilder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
      legBuilder.add(swapLeg.toBuilder()
          .accrualSchedule(scheduleBuilder.build())
          .build());
    }
    return trade.toBuilder()
        .product(trade.getProduct().toBuilder()
            .legs(legBuilder.build())
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SwapTradeCsvLoader() {
  }

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;

/**
 * Loads TermDeposit trades from CSV files.
 */
final class TermDepositTradeCsvLoader {

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param refData  the reference data
   * @return the loaded trades, all errors are captured in the result
   */
  static TermDepositTrade parse(CsvRow row, TradeInfo info, ReferenceData refData) {
    BuySell buySell = row.findValue(BUY_SELL_FIELD).map(s -> BuySell.of(s)).orElse(BuySell.BUY);
    double notional = TradeCsvLoader.parseDouble(row.getValue(NOTIONAL_FIELD));
    double fixedRate = TradeCsvLoader.parseDoublePercent(row.getValue(FIXED_RATE_FIELD));
    Optional<TermDepositConvention> conventionOpt = row.findValue(CONVENTION_FIELD).map(s -> TermDepositConvention.of(s));
    Optional<Period> tenorOpt = row.findValue(TENOR_FIELD).map(s -> Tenor.parse(s).getPeriod());
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
    Optional<Currency> currencyOpt = row.findValue(CURRENCY_FIELD).map(s -> Currency.parse(s));
    Optional<DayCount> dayCountOpt = row.findValue(DAY_COUNT_FIELD).map(s -> DayCount.of(s));
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(s -> BusinessDayConvention.of(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    Optional<HolidayCalendarId> dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD).map(s -> HolidayCalendarId.of(s));

    // use convention if available
    if (conventionOpt.isPresent()) {
      if (currencyOpt.isPresent() || dayCountOpt.isPresent()) {
        throw new IllegalArgumentException(
            "TermDeposit trade had invalid combination of fields. When '" + CONVENTION_FIELD +
                "' is present these fields must not be present: " +
                ImmutableList.of(CURRENCY_FIELD, DAY_COUNT_FIELD));
      }
      TermDepositConvention convention = conventionOpt.get();
      // explicit dates take precedence over relative ones
      if (startDateOpt.isPresent() && endDateOpt.isPresent()) {
        if (tenorOpt.isPresent()) {
          throw new IllegalArgumentException(
              "TermDeposit trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(TENOR_FIELD));
        }
        LocalDate startDate = startDateOpt.get();
        LocalDate endDate = endDateOpt.get();
        // NOTE: payment date assumed to be the start date
        TermDepositTrade trade = convention.toTrade(info, startDate, endDate, buySell, notional, fixedRate);
        return adjustTrade(trade, dateCnv, dateCalOpt);
      }
      // relative dates
      if (tenorOpt.isPresent() && info.getTradeDate().isPresent()) {
        if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
          throw new IllegalArgumentException(
              "TermDeposit trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, TENOR_FIELD, TRADE_DATE_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD));
        }
        LocalDate tradeDate = info.getTradeDate().get();
        Period periodToStart = tenorOpt.get();
        TermDepositTrade trade = convention.createTrade(tradeDate, periodToStart, buySell, notional, fixedRate, refData);
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, dateCnv, dateCalOpt);
      }

    } else if (startDateOpt.isPresent() && endDateOpt.isPresent() && currencyOpt.isPresent() && dayCountOpt.isPresent()) {
      LocalDate startDate = startDateOpt.get();
      LocalDate endDate = endDateOpt.get();
      Currency currency = currencyOpt.get();
      DayCount dayCount = dayCountOpt.get();
      TermDeposit.Builder builder = TermDeposit.builder()
          .buySell(buySell)
          .currency(currency)
          .notional(notional)
          .startDate(startDate)
          .endDate(endDate)
          .dayCount(dayCount)
          .rate(fixedRate);
      TermDepositTrade trade = TermDepositTrade.of(info, builder.build());
      return adjustTrade(trade, dateCnv, dateCalOpt);
    }
    // no match
    throw new IllegalArgumentException(
        "TermDeposit trade had invalid combination of fields. These fields are mandatory:" +
            ImmutableList.of(BUY_SELL_FIELD, NOTIONAL_FIELD, FIXED_RATE_FIELD) +
            " and one of these combinations is mandatory: " +
            ImmutableList.of(CONVENTION_FIELD, TRADE_DATE_FIELD, TENOR_FIELD) +
            " or " +
            ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
            " or " +
            ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD, CURRENCY_FIELD, DAY_COUNT_FIELD));
  }

  // adjust trade based on additional fields specified
  private static TermDepositTrade adjustTrade(
      TermDepositTrade trade,
      BusinessDayConvention dateCnv,
      Optional<HolidayCalendarId> dateCalOpt) {

    if (!dateCalOpt.isPresent()) {
      return trade;
    }
    TermDeposit.Builder builder = trade.getProduct().toBuilder();
    dateCalOpt.ifPresent(cal -> builder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
    return trade.toBuilder()
        .product(builder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private TermDepositTradeCsvLoader() {
  }

}

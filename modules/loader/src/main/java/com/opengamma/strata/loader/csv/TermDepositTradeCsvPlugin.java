/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;

/**
 * Handles the CSV file format for Term Deposit trades.
 */
final class TermDepositTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<TermDepositTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final TermDepositTradeCsvPlugin INSTANCE = new TermDepositTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.of(
      BUY_SELL_FIELD,
      CURRENCY_FIELD,
      NOTIONAL_FIELD,
      START_DATE_FIELD,
      END_DATE_FIELD,
      FIXED_RATE_FIELD,
      DAY_COUNT_FIELD,
      DATE_ADJ_CNV_FIELD,
      DATE_ADJ_CAL_FIELD);

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("TERMDEPOSIT", "TERM DEPOSIT");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(TermDepositTrade.class)) {
      return Optional.of(resolver.parseTermDepositTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return TermDepositTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(TermDepositTrade.class);
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
  static TermDepositTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    TermDepositTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static TermDepositTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
    double fixedRate = row.getValue(FIXED_RATE_FIELD, LoaderUtils::parseDoublePercent);
    Optional<TermDepositConvention> conventionOpt = row.findValue(CONVENTION_FIELD, TermDepositConvention::of);
    Optional<Period> tenorOpt = row.findValue(TENOR_FIELD, s -> LoaderUtils.parseTenor(s).getPeriod());
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD, LoaderUtils::parseDate);
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD, LoaderUtils::parseDate);
    Optional<Currency> currencyOpt = row.findValue(CURRENCY_FIELD, Currency::parse);
    Optional<DayCount> dayCountOpt = row.findValue(DAY_COUNT_FIELD, LoaderUtils::parseDayCount);
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(LoaderUtils::parseBusinessDayConvention).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    Optional<HolidayCalendarId> dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD, HolidayCalendarId::of);

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
        TermDepositTrade trade = convention.createTrade(
            tradeDate, periodToStart, buySell, notional, fixedRate, resolver.getReferenceData());
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
  @Override
  public Set<String> headers(List<TermDepositTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, TermDepositTrade trade) {
    TermDeposit product = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "TermDeposit");
    csv.writeCell(BUY_SELL_FIELD, product.getBuySell());
    csv.writeCell(CURRENCY_FIELD, product.getCurrency());
    csv.writeCell(NOTIONAL_FIELD, product.getNotional());
    csv.writeCell(START_DATE_FIELD, product.getStartDate());
    csv.writeCell(END_DATE_FIELD, product.getEndDate());
    csv.writeCell(FIXED_RATE_FIELD, formattedPercentage(product.getRate()));
    csv.writeCell(DAY_COUNT_FIELD, product.getDayCount());
    product.getBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(DATE_ADJ_CNV_FIELD, bda.getConvention());
      csv.writeCell(DATE_ADJ_CAL_FIELD, bda.getCalendar());
    });
    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private TermDepositTradeCsvPlugin() {
  }

}

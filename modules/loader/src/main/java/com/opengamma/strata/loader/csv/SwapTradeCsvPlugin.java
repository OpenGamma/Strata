/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOWN_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PERIOD_TO_START_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ROLL_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STUB_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.SingleCurrencySwapConvention;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConvention;

/**
 * Loads Swap trades from CSV files.
 */
final class SwapTradeCsvPlugin implements TradeCsvParserPlugin {

  /**
   * The singleton instance of the plugin.
   */
  public static final SwapTradeCsvPlugin INSTANCE = new SwapTradeCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("SWAP");
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

    if (requiredJavaType.isAssignableFrom(SwapTrade.class)) {
      return Optional.of(resolver.parseSwapTrade(baseRow, additionalRows, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "Swap";
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
  static SwapTrade parse(CsvRow row, List<CsvRow> variableRows, TradeInfo info, TradeCsvInfoResolver resolver) {
    SwapTrade trade = parseSwap(row, variableRows, info, resolver.getReferenceData());
    return resolver.completeTrade(row, trade);
  }

  // parses the swap without resolving it
  static SwapTrade parseSwap(CsvRow row, List<CsvRow> variableRows, TradeInfo info, ReferenceData refData) {
    SwapTrade trade = parseRow(row, info, refData);
    trade = parseVariableNotional(trade, variableRows);
    trade = parseVariableFixedRate(trade, variableRows);
    trade = parseVariableKnownAmount(trade, variableRows);
    return trade;
  }

  // variable notional
  private static SwapTrade parseVariableNotional(SwapTrade trade, List<CsvRow> variableRows) {
    ImmutableList<SwapLeg> legs = trade.getProduct().getLegs();
    ListMultimap<Integer, ValueStep> steps =
        extractSteps(variableRows, legs, NOTIONAL_FIELD, str -> LoaderUtils.parseDouble(str));
    if (steps.isEmpty()) {
      return trade;
    }
    // adjust the trade, inserting the variable element
    ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
    for (int i = 0; i < legs.size(); i++) {
      SwapLeg swapLeg = legs.get(i);
      List<ValueStep> legSteps = steps.get(i);
      if (!legSteps.isEmpty() && swapLeg instanceof RateCalculationSwapLeg) {
        RateCalculationSwapLeg leg = (RateCalculationSwapLeg) swapLeg;
        NotionalSchedule notionalSchedule = leg.getNotionalSchedule().toBuilder()
            .amount(ValueSchedule.of(leg.getNotionalSchedule().getAmount().getInitialValue(), legSteps))
            .build();
        swapLeg = leg.toBuilder().notionalSchedule(notionalSchedule).build();
      }
      legBuilder.add(swapLeg);
    }
    return replaceLegs(trade, legBuilder.build());
  }

  // variable fixed rate
  private static SwapTrade parseVariableFixedRate(SwapTrade trade, List<CsvRow> variableRows) {
    ImmutableList<SwapLeg> legs = trade.getProduct().getLegs();
    ListMultimap<Integer, ValueStep> steps =
        extractSteps(variableRows, legs, FIXED_RATE_FIELD, str -> LoaderUtils.parseDoublePercent(str));
    if (steps.isEmpty()) {
      return trade;
    }
    // adjust the trade, inserting the variable element
    ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
    for (int i = 0; i < legs.size(); i++) {
      SwapLeg swapLeg = legs.get(i);
      List<ValueStep> legSteps = steps.get(i);
      if (!legSteps.isEmpty() && swapLeg instanceof RateCalculationSwapLeg) {
        RateCalculationSwapLeg leg = (RateCalculationSwapLeg) swapLeg;
        if (leg.getCalculation() instanceof FixedRateCalculation) {
          FixedRateCalculation baseCalc = (FixedRateCalculation) leg.getCalculation();
          swapLeg = leg.toBuilder()
              .calculation(baseCalc.toBuilder()
                  .rate(ValueSchedule.of(baseCalc.getRate().getInitialValue(), legSteps))
                  .build())
              .build();
        }
      }
      legBuilder.add(swapLeg);
    }
    return replaceLegs(trade, legBuilder.build());
  }

  // variable known amount
  private static SwapTrade parseVariableKnownAmount(SwapTrade trade, List<CsvRow> variableRows) {
    ImmutableList<SwapLeg> legs = trade.getProduct().getLegs();
    ListMultimap<Integer, ValueStep> steps =
        extractSteps(variableRows, legs, KNOWN_AMOUNT_FIELD, str -> LoaderUtils.parseDouble(str));
    if (steps.isEmpty()) {
      return trade;
    }
    // adjust the trade, inserting the variable element
    ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
    for (int i = 0; i < legs.size(); i++) {
      SwapLeg swapLeg = legs.get(i);
      List<ValueStep> legSteps = steps.get(i);
      if (!legSteps.isEmpty() && swapLeg instanceof KnownAmountSwapLeg) {
        KnownAmountSwapLeg leg = (KnownAmountSwapLeg) swapLeg;
        swapLeg = leg.toBuilder()
            .amount(ValueSchedule.of(leg.getAmount().getInitialValue(), legSteps))
            .build();
      }
      legBuilder.add(swapLeg);
    }
    return replaceLegs(trade, legBuilder.build());
  }

  // extract the steps for a field, either 'Leg n Foo' or 'Foo'
  private static ListMultimap<Integer, ValueStep> extractSteps(
      List<CsvRow> variableRows,
      ImmutableList<SwapLeg> legs,
      String field,
      Function<String, Double> parser) {

    ListMultimap<Integer, ValueStep> steps = ArrayListMultimap.create();
    for (CsvRow row : variableRows) {
      LocalDate date = row.getValue(START_DATE_FIELD, LoaderUtils::parseDate);
      for (int i = 0; i < legs.size(); i++) {
        int legIndex = i;  // must be effectively final for lambda
        row.findValue("Leg " + (legIndex + 1) + " " + field)
            .map(Optional::of)
            .orElseGet(() -> row.findValue(field))
            .map(parser)
            .map(val -> ValueStep.of(date, ValueAdjustment.ofReplace(val)))
            .ifPresent(step -> steps.put(legIndex, step));
      }
    }
    return steps;
  }

  //-------------------------------------------------------------------------
  // parse the row to a trade
  private static SwapTrade parseRow(CsvRow row, TradeInfo info, ReferenceData refData) {
    Optional<String> conventionOpt = row.findValue(CONVENTION_FIELD);
    if (conventionOpt.isPresent()) {
      return parseWithConvention(row, info, refData, conventionOpt.get());
    } else {
      Optional<String> payReceive = row.findValue("Leg 1 " + DIRECTION_FIELD);
      if (payReceive.isPresent()) {
        return FullSwapTradeCsvPlugin.parse(row, info);
      }
      throw new IllegalArgumentException(
          "Swap trade had invalid combination of fields. Must include either '" +
              CONVENTION_FIELD + "' or '" + "Leg 1 " + DIRECTION_FIELD + "'");
    }
  }

  // parse a trade based on a convention
  static SwapTrade parseWithConvention(CsvRow row, TradeInfo info, ReferenceData refData, String conventionStr) {
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
    double fixedRate = row.getValue(FIXED_RATE_FIELD, LoaderUtils::parseDoublePercent);
    Optional<Period> periodToStartOpt = row.findValue(PERIOD_TO_START_FIELD).map(s -> LoaderUtils.parsePeriod(s));
    Optional<Tenor> tenorOpt = row.findValue(TENOR_FIELD).map(s -> LoaderUtils.parseTenor(s));
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<RollConvention> rollCnvOpt = row.findValue(ROLL_CONVENTION_FIELD).map(s -> LoaderUtils.parseRollConvention(s));
    Optional<StubConvention> stubCnvOpt = row.findValue(STUB_CONVENTION_FIELD).map(s -> StubConvention.of(s));
    Optional<LocalDate> firstRegStartDateOpt =
        row.findValue(FIRST_REGULAR_START_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<LocalDate> lastRegEndDateOpt = row.findValue(LAST_REGULAR_END_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    BusinessDayConvention dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
        .map(s -> LoaderUtils.parseBusinessDayConvention(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    Optional<HolidayCalendarId> dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD).map(s -> HolidayCalendarId.of(s));
    Optional<Double> fxRateOpt = row.findValue(FX_RATE_FIELD).map(str -> LoaderUtils.parseDouble(str));

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
      SwapTrade trade = createSwap(info, conventionStr, startDate, endDate, buySell, notional, fixedRate, fxRateOpt);
      return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
    }

    // start date + tenor
    if (startDateOpt.isPresent() && tenorOpt.isPresent()) {
      if (periodToStartOpt.isPresent() || endDateOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Swap trade had invalid combination of fields. When these fields are found " +
                ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, TENOR_FIELD) +
                " then these fields must not be present " +
                ImmutableList.of(PERIOD_TO_START_FIELD, END_DATE_FIELD));
      }
      LocalDate startDate = startDateOpt.get();
      Tenor tenor = tenorOpt.get();
      LocalDate endDate = startDate.plus(tenor);
      SwapTrade trade = createSwap(info, conventionStr, startDate, endDate, buySell, notional, fixedRate, fxRateOpt);
      return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
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
        SwapTrade trade = convention.createTrade(
            tradeDate, periodToStart, tenor, buySell, notional, notionalFlat, fixedRate, refData);
        trade = trade.toBuilder().info(info).build();
        return adjustTrade(trade, rollCnvOpt, stubCnvOpt, firstRegStartDateOpt, lastRegEndDateOpt, dateCnv, dateCalOpt);
      } else {
        SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionStr);
        SwapTrade trade = convention.createTrade(
            tradeDate, periodToStart, tenor, buySell, notional, fixedRate, refData);
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
            ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, TENOR_FIELD) +
            " or " +
            ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD));
  }

  // create a swap from known start/end dates
  private static SwapTrade createSwap(
      TradeInfo info,
      String conventionStr,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      Optional<Double> fxRateOpt) {

    if (fxRateOpt.isPresent()) {
      XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionStr);
      double notionalFlat = notional * fxRateOpt.get();
      return convention.toTrade(info, startDate, endDate, buySell, notional, notionalFlat, fixedRate);
    } else {
      SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionStr);
      return convention.toTrade(info, startDate, endDate, buySell, notional, fixedRate);
    }
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

    ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
    for (SwapLeg leg : trade.getProduct().getLegs()) {
      RateCalculationSwapLeg swapLeg = (RateCalculationSwapLeg) leg;
      PeriodicSchedule.Builder scheduleBuilder = swapLeg.getAccrualSchedule().toBuilder();
      scheduleBuilder.stubConvention(stubConventionOpt.orElse(StubConvention.SMART_INITIAL));
      rollConventionOpt.ifPresent(rc -> scheduleBuilder.rollConvention(rc));
      firstRegularStartDateOpt.ifPresent(date -> scheduleBuilder.firstRegularStartDate(date));
      lastRegEndDateOpt.ifPresent(date -> scheduleBuilder.lastRegularEndDate(date));
      dateCalOpt.ifPresent(cal -> scheduleBuilder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
      legBuilder.add(swapLeg.toBuilder()
          .accrualSchedule(scheduleBuilder.build())
          .build());
    }
    return replaceLegs(trade, legBuilder.build());
  }

  // replace the legs
  private static SwapTrade replaceLegs(SwapTrade trade, ImmutableList<SwapLeg> legs) {
    return trade.toBuilder()
        .product(trade.getProduct().toBuilder()
            .legs(legs)
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SwapTradeCsvPlugin() {
  }

}

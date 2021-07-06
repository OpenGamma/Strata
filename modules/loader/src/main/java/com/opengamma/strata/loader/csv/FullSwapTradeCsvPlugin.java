/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ACCRUAL_METHOD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.COMPOUNDING_METHOD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FINAL_STUB_AMOUNT_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FINAL_STUB_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FINAL_STUB_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FINAL_STUB_INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FINAL_STUB_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_REGULAR_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXING_OFFSET_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXING_OFFSET_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXING_OFFSET_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXING_OFFSET_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXING_RELATIVE_TO_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FUTURE_VALUE_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_INITIAL_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_OFFSET_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_OFFSET_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_OFFSET_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_OFFSET_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FX_RESET_RELATIVE_TO_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.GEARING_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INFLATION_FIRST_INDEX_VALUE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INFLATION_LAG_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INFLATION_METHOD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INITIAL_STUB_AMOUNT_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INITIAL_STUB_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INITIAL_STUB_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INITIAL_STUB_INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INITIAL_STUB_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOWN_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NEGATIVE_RATE_METHOD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FINAL_EXCHANGE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_INITIAL_EXCHANGE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_OFFSET_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_OFFSET_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_OFFSET_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_OFFSET_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_RELATIVE_TO_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RATE_CUT_OFF_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RESET_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RESET_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RESET_FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RESET_METHOD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ROLL_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SPREAD_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STUB_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedDouble;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FloatingRateIndex;
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.FloatingRateType;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustmentType;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FixedRateStubCalculation;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.FutureValueNotional;
import com.opengamma.strata.product.swap.FxResetCalculation;
import com.opengamma.strata.product.swap.FxResetFixingRelativeTo;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateResetMethod;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.swap.NegativeRateMethod;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentRelativeTo;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateCalculation;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResetSchedule;
import com.opengamma.strata.product.swap.ScheduledSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Handles the CSV file format for Swap trades.
 */
final class FullSwapTradeCsvPlugin implements TradeCsvWriterPlugin<SwapTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final FullSwapTradeCsvPlugin INSTANCE = new FullSwapTradeCsvPlugin();

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @return the parsed trade
   */
  static SwapTrade parse(CsvRow row, TradeInfo info) {
    // parse any number of legs by looking for 'Leg n Pay Receive'
    // this finds the index for each leg, using null for fixed legs
    List<FloatingRateIndex> indices = new ArrayList<>();
    Set<DayCount> dayCounts = new LinkedHashSet<>();
    boolean missingDayCount = false;
    String legPrefix = "Leg 1 ";
    Optional<String> payReceiveOpt = Optional.of(getValue(row, legPrefix, DIRECTION_FIELD));
    int i = 1;
    while (payReceiveOpt.isPresent()) {
      // parse this leg, capturing the day count for floating legs
      FloatingRateIndex index = parseIndex(row, legPrefix);
      indices.add(index);
      if (index != null) {
        dayCounts.add(index.getDefaultFixedLegDayCount());
      }
      // defaulting only triggered if a fixed leg actually has a missing day count
      if (index == null && !findValue(row, legPrefix, DAY_COUNT_FIELD).isPresent()) {
        missingDayCount = true;
      }
      // check if there is another leg
      i++;
      legPrefix = "Leg " + i + " ";
      payReceiveOpt = findValue(row, legPrefix, DIRECTION_FIELD);
    }
    // determine the default day count for the fixed leg (only if there is a fixed leg)
    DayCount defaultFixedLegDayCount = null;
    if (missingDayCount) {
      if (dayCounts.size() != 1) {
        throw new IllegalArgumentException("Invalid swap definition, day count must be defined on each fixed leg");
      }
      defaultFixedLegDayCount = Iterables.getOnlyElement(dayCounts);
    }
    // parse fully now we know the number of legs and the default fixed leg day count
    List<SwapLeg> legs = parseLegs(row, indices, defaultFixedLegDayCount);
    Swap swap = Swap.of(legs);
    return SwapTrade.of(info, swap);
  }

  //-------------------------------------------------------------------------
  // parse the index and default fixed leg day count
  private static FloatingRateIndex parseIndex(CsvRow row, String leg) {
    Optional<String> fixedRateOpt = findValue(row, leg, FIXED_RATE_FIELD);
    Optional<String> indexOpt = findValue(row, leg, INDEX_FIELD);
    Optional<String> knownAmountOpt = findValue(row, leg, KNOWN_AMOUNT_FIELD);

    if (fixedRateOpt.isPresent() || knownAmountOpt.isPresent()) {
      if (fixedRateOpt.isPresent() && knownAmountOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Swap leg must not define both '" + leg + FIXED_RATE_FIELD + "' and '" + leg + KNOWN_AMOUNT_FIELD + "'");
      } else if (indexOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Swap leg must not define both '" + leg + FIXED_RATE_FIELD + "' or '" + leg + KNOWN_AMOUNT_FIELD +
                "' and '" + leg + INDEX_FIELD + "'");
      }
      return null;
    }
    if (!indexOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must define either '" + leg + FIXED_RATE_FIELD + "' or '" + leg + KNOWN_AMOUNT_FIELD +
              "' or '" + leg + INDEX_FIELD + "'");
    }
    // use FloatingRateName to identify Ibor vs other
    String indexStr = indexOpt.get();
    FloatingRateName frn = FloatingRateName.parse(indexStr);
    if (frn.getType() == FloatingRateType.IBOR) {
      // re-parse Ibor using tenor, which ensures tenor picked up from indexStr if present
      Frequency freq = Frequency.parse(getValue(row, leg, FREQUENCY_FIELD));
      Tenor iborTenor = freq.isTerm() ? frn.getDefaultTenor() : Tenor.of(freq.getPeriod());
      return FloatingRateIndex.parse(indexStr, iborTenor);
    }
    return frn.toFloatingRateIndex();
  }

  // parses all the legs
  private static List<SwapLeg> parseLegs(
      CsvRow row,
      List<FloatingRateIndex> indices,
      DayCount defaultFixedLegDayCount) {

    List<SwapLeg> legs = new ArrayList<>();
    for (int i = 0; i < indices.size(); i++) {
      String legPrefix = "Leg " + (i + 1) + " ";
      legs.add(parseLeg(row, legPrefix, indices.get(i), defaultFixedLegDayCount));
    }
    return legs;
  }

  // parse a single leg
  private static SwapLeg parseLeg(
      CsvRow row,
      String leg,
      FloatingRateIndex index,
      DayCount defaultFixedLegDayCount) {

    PayReceive payReceive = LoaderUtils.parsePayReceive(getValue(row, leg, DIRECTION_FIELD));
    PeriodicSchedule accrualSch = parseAccrualSchedule(row, leg);
    PaymentSchedule paymentSch = parsePaymentSchedule(row, leg, accrualSch.getFrequency());
    Currency currency = Currency.of(getValueWithFallback(row, leg, CURRENCY_FIELD));

    Optional<String> knownAmount = findValue(row, leg, KNOWN_AMOUNT_FIELD);
    if (knownAmount.isPresent()) {
      return KnownAmountSwapLeg.builder()
          .payReceive(payReceive)
          .accrualSchedule(accrualSch)
          .paymentSchedule(paymentSch)
          .currency(currency)
          .amount(ValueSchedule.of(LoaderUtils.parseDouble(knownAmount.get())))
          .build();
    }

    return parseRateCalculationLeg(
        row,
        leg,
        index,
        defaultFixedLegDayCount,
        payReceive,
        accrualSch,
        paymentSch);
  }

  // parse a single rate calculation leg
  private static SwapLeg parseRateCalculationLeg(
      CsvRow row,
      String leg,
      FloatingRateIndex index,
      DayCount defaultFixedLegDayCount,
      PayReceive payReceive,
      PeriodicSchedule accrualSch,
      PaymentSchedule paymentSch) {

    NotionalSchedule notionalSch = parseNotionalSchedule(row, leg);

    RateCalculation calc = parseRateCalculation(
        row,
        leg,
        index,
        defaultFixedLegDayCount,
        accrualSch.getBusinessDayAdjustment(),
        notionalSch.getCurrency());

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(accrualSch)
        .paymentSchedule(paymentSch)
        .notionalSchedule(notionalSch)
        .calculation(calc)
        .build();
  }

  //-------------------------------------------------------------------------
  // accrual schedule
  private static PeriodicSchedule parseAccrualSchedule(CsvRow row, String leg) {
    PeriodicSchedule.Builder builder = PeriodicSchedule.builder();
    // basics
    builder.startDate(LoaderUtils.parseDate(getValueWithFallback(row, leg, START_DATE_FIELD)));
    builder.endDate(LoaderUtils.parseDate(getValueWithFallback(row, leg, END_DATE_FIELD)));
    builder.frequency(Frequency.parse(getValue(row, leg, FREQUENCY_FIELD)));
    // adjustments
    BusinessDayAdjustment dateAdj = parseBusinessDayAdjustment(row, leg, DATE_ADJ_CNV_FIELD, DATE_ADJ_CAL_FIELD)
        .orElse(BusinessDayAdjustment.NONE);
    Optional<BusinessDayAdjustment> startDateAdj =
        parseBusinessDayAdjustment(row, leg, START_DATE_CNV_FIELD, START_DATE_CAL_FIELD);
    Optional<BusinessDayAdjustment> endDateAdj =
        parseBusinessDayAdjustment(row, leg, END_DATE_CNV_FIELD, END_DATE_CAL_FIELD);
    builder.businessDayAdjustment(dateAdj);
    if (startDateAdj.isPresent() && !startDateAdj.get().equals(dateAdj)) {
      builder.startDateBusinessDayAdjustment(startDateAdj.get());
    }
    if (endDateAdj.isPresent() && !endDateAdj.get().equals(dateAdj)) {
      builder.endDateBusinessDayAdjustment(endDateAdj.get());
    }
    // optionals
    builder.stubConvention(findValueWithFallback(row, leg, STUB_CONVENTION_FIELD)
        .map(s -> StubConvention.of(s))
        .orElse(StubConvention.SMART_INITIAL));
    findValue(row, leg, ROLL_CONVENTION_FIELD)
        .map(s -> LoaderUtils.parseRollConvention(s))
        .ifPresent(v -> builder.rollConvention(v));
    findValue(row, leg, FIRST_REGULAR_START_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.firstRegularStartDate(v));
    findValue(row, leg, LAST_REGULAR_END_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.lastRegularEndDate(v));
    parseAdjustableDate(
        row, leg, OVERRIDE_START_DATE_FIELD, OVERRIDE_START_DATE_CNV_FIELD, OVERRIDE_START_DATE_CAL_FIELD)
        .ifPresent(d -> builder.overrideStartDate(d));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // payment schedule
  private static PaymentSchedule parsePaymentSchedule(CsvRow row, String leg, Frequency accrualFrequency) {
    PaymentSchedule.Builder builder = PaymentSchedule.builder();
    // basics
    builder.paymentFrequency(findValue(row, leg, PAYMENT_FREQUENCY_FIELD)
        .map(s -> Frequency.parse(s))
        .orElse(accrualFrequency));
    Optional<DaysAdjustment> offsetOpt = parseDaysAdjustment(
        row,
        leg,
        PAYMENT_OFFSET_DAYS_FIELD,
        PAYMENT_OFFSET_CAL_FIELD,
        PAYMENT_OFFSET_ADJ_CNV_FIELD,
        PAYMENT_OFFSET_ADJ_CAL_FIELD);
    builder.paymentDateOffset(offsetOpt.orElse(DaysAdjustment.NONE));
    // optionals
    findValue(row, leg, PAYMENT_RELATIVE_TO_FIELD)
        .map(s -> PaymentRelativeTo.of(s))
        .ifPresent(v -> builder.paymentRelativeTo(v));
    findValue(row, leg, COMPOUNDING_METHOD_FIELD)
        .map(s -> CompoundingMethod.of(s))
        .ifPresent(v -> builder.compoundingMethod(v));
    findValue(row, leg, PAYMENT_FIRST_REGULAR_START_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.firstRegularStartDate(v));
    findValue(row, leg, PAYMENT_LAST_REGULAR_END_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.lastRegularEndDate(v));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // notional schedule
  private static NotionalSchedule parseNotionalSchedule(CsvRow row, String leg) {
    NotionalSchedule.Builder builder = NotionalSchedule.builder();
    // basics
    Currency currency = Currency.of(getValueWithFallback(row, leg, CURRENCY_FIELD));
    builder.currency(currency);
    builder.amount(ValueSchedule.of(LoaderUtils.parseDouble(getValueWithFallback(row, leg, NOTIONAL_FIELD))));
    // fx reset
    Optional<FxIndex> fxIndexOpt = findValue(row, leg, FX_RESET_INDEX_FIELD).map(s -> FxIndex.of(s));
    Optional<Currency> notionalCurrencyOpt = findValue(row, leg, NOTIONAL_CURRENCY_FIELD).map(s -> Currency.of(s));
    Optional<FxResetFixingRelativeTo> fxFixingRelativeToOpt = findValue(row, leg, FX_RESET_RELATIVE_TO_FIELD)
        .map(s -> FxResetFixingRelativeTo.of(s));
    Optional<DaysAdjustment> fxResetAdjOpt = parseDaysAdjustment(
        row,
        leg,
        FX_RESET_OFFSET_DAYS_FIELD,
        FX_RESET_OFFSET_CAL_FIELD,
        FX_RESET_OFFSET_ADJ_CNV_FIELD,
        FX_RESET_OFFSET_ADJ_CAL_FIELD);
    if (fxIndexOpt.isPresent()) {
      FxIndex fxIndex = fxIndexOpt.get();
      FxResetCalculation.Builder fxResetBuilder = FxResetCalculation.builder();
      fxResetBuilder.index(fxIndex);
      fxResetBuilder.referenceCurrency(notionalCurrencyOpt.orElse(fxIndex.getCurrencyPair().other(currency)));
      fxFixingRelativeToOpt.ifPresent(v -> fxResetBuilder.fixingRelativeTo(v));
      fxResetAdjOpt.ifPresent(v -> fxResetBuilder.fixingDateOffset(v));
      findValue(row, leg, FX_RESET_INITIAL_NOTIONAL_FIELD)
          .map(s -> LoaderUtils.parseDouble(s))
          .ifPresent(initialNotional -> fxResetBuilder.initialNotionalValue(initialNotional));
      builder.fxReset(fxResetBuilder.build());
    } else if (notionalCurrencyOpt.isPresent() || fxFixingRelativeToOpt.isPresent() || fxResetAdjOpt.isPresent()) {
      throw new IllegalArgumentException("Swap trade FX Reset must define field '" + leg + FX_RESET_INDEX_FIELD + "'");
    }
    // optionals
    findValue(row, leg, NOTIONAL_INITIAL_EXCHANGE_FIELD)
        .map(s -> LoaderUtils.parseBoolean(s))
        .ifPresent(v -> builder.initialExchange(v));
    findValue(row, leg, NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD)
        .map(s -> LoaderUtils.parseBoolean(s))
        .ifPresent(v -> builder.intermediateExchange(v));
    findValue(row, leg, NOTIONAL_FINAL_EXCHANGE_FIELD)
        .map(s -> LoaderUtils.parseBoolean(s))
        .ifPresent(v -> builder.finalExchange(v));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // rate calculation
  private static RateCalculation parseRateCalculation(
      CsvRow row,
      String leg,
      FloatingRateIndex index,
      DayCount defaultFixedLegDayCount,
      BusinessDayAdjustment bda,
      Currency currency) {

    if (index instanceof IborIndex) {
      return parseIborRateCalculation(row, leg, (IborIndex) index, bda, currency);

    } else if (index instanceof OvernightIndex) {
      Optional<FloatingRateName> frnOpt = FloatingRateName.extendedEnum().find(getValue(row, leg, INDEX_FIELD));
      if (frnOpt.isPresent()) {
        FloatingRateName frn = frnOpt.get();
        if (frn.getType() == FloatingRateType.OVERNIGHT_AVERAGED) {
          return parseOvernightRateCalculation(row, leg, (OvernightIndex) index, OvernightAccrualMethod.AVERAGED);
        }
      }
      return parseOvernightRateCalculation(row, leg, (OvernightIndex) index, OvernightAccrualMethod.COMPOUNDED);

    } else if (index instanceof PriceIndex) {
      return parseInflationRateCalculation(row, leg, (PriceIndex) index, currency);

    } else {
      return parseFixedRateCalculation(row, leg, currency, defaultFixedLegDayCount);
    }
  }

  //-------------------------------------------------------------------------
  // fixed rate calculation
  private static RateCalculation parseFixedRateCalculation(
      CsvRow row,
      String leg,
      Currency currency,
      DayCount defaultFixedLegDayCount) {

    FixedRateCalculation.Builder builder = FixedRateCalculation.builder();
    // basics
    double fixedRate = LoaderUtils.parseDoublePercent(getValue(row, leg, FIXED_RATE_FIELD));
    DayCount dayCount = findValue(row, leg, DAY_COUNT_FIELD)
        .map(s -> LoaderUtils.parseDayCount(s)).orElse(defaultFixedLegDayCount);
    if (dayCount == null) {
      throw new IllegalArgumentException("Swap leg must define day count using '" + leg + DAY_COUNT_FIELD + "'");
    }
    builder.dayCount(dayCount);
    builder.rate(ValueSchedule.of(fixedRate));
    findValue(row, leg, FUTURE_VALUE_NOTIONAL_FIELD)
        .map(s -> LoaderUtils.parseDouble(s))
        .ifPresent(val -> builder.futureValueNotional(FutureValueNotional.of(val)));
    // initial stub
    Optional<Double> initialStubRateOpt = findValue(row, leg, INITIAL_STUB_RATE_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s));
    Optional<Double> initialStubAmountOpt = findValue(row, leg, INITIAL_STUB_AMOUNT_FIELD)
        .map(s -> LoaderUtils.parseDouble(s));
    Optional<Currency> initialStubAmountCcyOpt = findValue(row, leg, INITIAL_STUB_AMOUNT_CURRENCY_FIELD)
        .map(s -> LoaderUtils.parseCurrency(s));
    if (initialStubRateOpt.isPresent() && initialStubAmountOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must not define both '" + leg + INITIAL_STUB_RATE_FIELD + "' and '" + leg + INITIAL_STUB_AMOUNT_FIELD + "'");
    }
    initialStubRateOpt.ifPresent(v -> builder.initialStub(
        FixedRateStubCalculation.ofFixedRate(v)));
    initialStubAmountOpt.ifPresent(v -> builder.initialStub(
        FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(initialStubAmountCcyOpt.orElse(currency), v))));
    // final stub
    Optional<Double> finalStubRateOpt = findValue(row, leg, FINAL_STUB_RATE_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s));
    Optional<Double> finalStubAmountOpt = findValue(row, leg, FINAL_STUB_AMOUNT_FIELD)
        .map(s -> LoaderUtils.parseDouble(s));
    Optional<Currency> finalStubAmountCcyOpt = findValue(row, leg, FINAL_STUB_AMOUNT_CURRENCY_FIELD)
        .map(s -> LoaderUtils.parseCurrency(s));
    if (finalStubRateOpt.isPresent() && finalStubAmountOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must not define both '" + leg + FINAL_STUB_RATE_FIELD + "' and '" + leg + FINAL_STUB_AMOUNT_FIELD + "'");
    }
    finalStubRateOpt.ifPresent(v -> builder.finalStub(
        FixedRateStubCalculation.ofFixedRate(v)));
    finalStubAmountOpt.ifPresent(v -> builder.finalStub(
        FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(finalStubAmountCcyOpt.orElse(currency), v))));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // ibor rate calculation
  private static RateCalculation parseIborRateCalculation(
      CsvRow row,
      String leg,
      IborIndex iborIndex,
      BusinessDayAdjustment bda,
      Currency currency) {

    IborRateCalculation.Builder builder = IborRateCalculation.builder();
    // basics
    builder.index(iborIndex);
    // reset
    Optional<Frequency> resetFrequencyOpt = findValue(row, leg, RESET_FREQUENCY_FIELD).map(v -> Frequency.parse(v));
    IborRateResetMethod resetMethod = findValue(row, leg, RESET_METHOD_FIELD)
        .map(v -> IborRateResetMethod.of(v))
        .orElse(IborRateResetMethod.WEIGHTED);
    BusinessDayAdjustment resetDateAdj =
        parseBusinessDayAdjustment(row, leg, RESET_DATE_CNV_FIELD, RESET_DATE_CAL_FIELD).orElse(bda);
    resetFrequencyOpt.ifPresent(freq -> builder.resetPeriods(ResetSchedule.builder()
        .resetFrequency(freq)
        .resetMethod(resetMethod)
        .businessDayAdjustment(resetDateAdj)
        .build()));
    // optionals, no ability to set firstFixingDateOffset
    findValue(row, leg, DAY_COUNT_FIELD)
        .map(s -> LoaderUtils.parseDayCount(s))
        .ifPresent(v -> builder.dayCount(v));
    findValue(row, leg, FIXING_RELATIVE_TO_FIELD)
        .map(s -> FixingRelativeTo.of(s))
        .ifPresent(v -> builder.fixingRelativeTo(v));
    Optional<DaysAdjustment> fixingAdjOpt = parseDaysAdjustment(
        row,
        leg,
        FIXING_OFFSET_DAYS_FIELD,
        FIXING_OFFSET_CAL_FIELD,
        FIXING_OFFSET_ADJ_CNV_FIELD,
        FIXING_OFFSET_ADJ_CAL_FIELD);
    fixingAdjOpt.ifPresent(v -> builder.fixingDateOffset(v));
    findValue(row, leg, NEGATIVE_RATE_METHOD_FIELD).map(s -> NegativeRateMethod.of(s))
        .ifPresent(v -> builder.negativeRateMethod(v));
    findValue(row, leg, FIRST_RATE_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s))
        .ifPresent(v -> builder.firstRate(v));
    findValue(row, leg, FIRST_REGULAR_RATE_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s))
        .ifPresent(v -> builder.firstRegularRate(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> LoaderUtils.parseDouble(s))
        .ifPresent(v -> builder.gearing(ValueSchedule.of(v)));
    findValue(row, leg, SPREAD_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s))
        .ifPresent(v -> builder.spread(ValueSchedule.of(v)));
    // initial stub
    Optional<IborRateStubCalculation> initialStub = parseIborStub(
        row,
        leg,
        currency,
        builder,
        INITIAL_STUB_RATE_FIELD,
        INITIAL_STUB_AMOUNT_FIELD,
        INITIAL_STUB_INDEX_FIELD,
        INITIAL_STUB_INTERPOLATED_INDEX_FIELD);
    initialStub.ifPresent(stub -> builder.initialStub(stub));
    // final stub
    Optional<IborRateStubCalculation> finalStub = parseIborStub(
        row,
        leg,
        currency,
        builder,
        FINAL_STUB_RATE_FIELD,
        FINAL_STUB_AMOUNT_FIELD,
        FINAL_STUB_INDEX_FIELD,
        FINAL_STUB_INTERPOLATED_INDEX_FIELD);
    finalStub.ifPresent(stub -> builder.finalStub(stub));
    return builder.build();
  }

  // an Ibor stub
  private static Optional<IborRateStubCalculation> parseIborStub(
      CsvRow row,
      String leg,
      Currency currency,
      IborRateCalculation.Builder builder,
      String rateField,
      String amountField,
      String indexField,
      String interpolatedField) {

    Optional<Double> stubRateOpt = findValue(row, leg, rateField).map(s -> LoaderUtils.parseDoublePercent(s));
    Optional<Double> stubAmountOpt = findValue(row, leg, amountField).map(s -> LoaderUtils.parseDouble(s));
    Optional<IborIndex> stubIndexOpt = findValue(row, leg, indexField).map(s -> IborIndex.of(s));
    Optional<IborIndex> stubIndex2Opt = findValue(row, leg, interpolatedField).map(s -> IborIndex.of(s));
    if (stubRateOpt.isPresent() && !stubAmountOpt.isPresent() && !stubIndexOpt.isPresent() && !stubIndex2Opt.isPresent()) {
      return Optional.of(IborRateStubCalculation.ofFixedRate(stubRateOpt.get()));

    } else if (!stubRateOpt.isPresent() && stubAmountOpt.isPresent() && !stubIndexOpt.isPresent() && !stubIndex2Opt.isPresent()) {
      return Optional.of(IborRateStubCalculation.ofKnownAmount(CurrencyAmount.of(currency, stubAmountOpt.get())));

    } else if (!stubRateOpt.isPresent() && !stubAmountOpt.isPresent() && stubIndexOpt.isPresent()) {
      if (stubIndex2Opt.isPresent()) {
        return Optional.of(IborRateStubCalculation.ofIborInterpolatedRate(stubIndexOpt.get(), stubIndex2Opt.get()));
      } else {
        return Optional.of(IborRateStubCalculation.ofIborRate(stubIndexOpt.get()));
      }
    } else if (stubRateOpt.isPresent() || stubAmountOpt.isPresent() ||
        stubIndexOpt.isPresent() || stubIndex2Opt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must define only one of the following fields " +
              ImmutableList.of(leg + rateField, leg + amountField, leg + indexField) +
              ", and '" + leg + interpolatedField + "' is only allowed with '" + leg + indexField + "'");
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  // overnight rate calculation
  private static RateCalculation parseOvernightRateCalculation(
      CsvRow row,
      String leg,
      OvernightIndex overnightIndex,
      OvernightAccrualMethod accrualMethod) {

    OvernightRateCalculation.Builder builder = OvernightRateCalculation.builder();
    // basics
    builder.index(overnightIndex);
    builder.accrualMethod(findValue(row, leg, ACCRUAL_METHOD_FIELD)
        .map(s -> OvernightAccrualMethod.of(s))
        .orElse(accrualMethod));
    // optionals
    findValue(row, leg, DAY_COUNT_FIELD)
        .map(s -> LoaderUtils.parseDayCount(s))
        .ifPresent(v -> builder.dayCount(v));
    findValue(row, leg, RATE_CUT_OFF_DAYS_FIELD)
        .map(s -> Integer.valueOf(s))
        .ifPresent(v -> builder.rateCutOffDays(v));
    findValue(row, leg, NEGATIVE_RATE_METHOD_FIELD).map(s -> NegativeRateMethod.of(s))
        .ifPresent(v -> builder.negativeRateMethod(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> LoaderUtils.parseDouble(s))
        .ifPresent(v -> builder.gearing(ValueSchedule.of(v)));
    findValue(row, leg, SPREAD_FIELD)
        .map(s -> LoaderUtils.parseDoublePercent(s))
        .ifPresent(v -> builder.spread(ValueSchedule.of(v)));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // inflation rate calculation
  private static RateCalculation parseInflationRateCalculation(
      CsvRow row,
      String leg,
      PriceIndex priceIndex,
      Currency currency) {

    InflationRateCalculation.Builder builder = InflationRateCalculation.builder();
    // basics
    builder.index(priceIndex);
    builder.lag(parseInflationLag(findValue(row, leg, INFLATION_LAG_FIELD), currency));
    builder.indexCalculationMethod(parseInflationMethod(findValue(row, leg, INFLATION_METHOD_FIELD), currency));
    // optionals
    findValue(row, leg, INFLATION_FIRST_INDEX_VALUE_FIELD)
        .map(s -> LoaderUtils.parseDouble(s))
        .ifPresent(v -> builder.firstIndexValue(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> LoaderUtils.parseDouble(s))
        .ifPresent(v -> builder.gearing(ValueSchedule.of(v)));
    return builder.build();
  }

  // parse inflation lag with convention defaults
  private static Period parseInflationLag(Optional<String> strOpt, Currency currency) {
    if (!strOpt.isPresent()) {
      if (Currency.GBP.equals(currency)) {
        return Period.ofMonths(2);
      }
      return Period.ofMonths(3);
    }
    String str = strOpt.get();
    Integer months = Ints.tryParse(str);
    if (months != null) {
      return Period.ofMonths(months);
    }
    return Tenor.parse(str).getPeriod();
  }

  // parse inflation method with convention defaults
  private static PriceIndexCalculationMethod parseInflationMethod(Optional<String> strOpt, Currency currency) {
    if (!strOpt.isPresent()) {
      if (Currency.JPY.equals(currency)) {
        return PriceIndexCalculationMethod.INTERPOLATED_JAPAN;
      } else if (Currency.USD.equals(currency)) {
        return PriceIndexCalculationMethod.INTERPOLATED;
      }
      return PriceIndexCalculationMethod.MONTHLY;
    }
    return PriceIndexCalculationMethod.of(strOpt.get());
  }

  //-------------------------------------------------------------------------
  // days adjustment, defaulting business day convention
  private static Optional<BusinessDayAdjustment> parseBusinessDayAdjustment(
      CsvRow row,
      String leg,
      String cnvField,
      String calField) {

    BusinessDayConvention dateCnv = findValue(row, leg, cnvField)
        .map(s -> LoaderUtils.parseBusinessDayConvention(s))
        .orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    return findValue(row, leg, calField)
        .map(s -> HolidayCalendarId.of(s))
        .map(cal -> BusinessDayAdjustment.of(dateCnv, cal));
  }

  // days adjustment, defaulting calendar and adjustment
  private static Optional<DaysAdjustment> parseDaysAdjustment(
      CsvRow row,
      String leg,
      String daysField,
      String daysCalField,
      String cnvField,
      String calField) {

    Optional<Integer> daysOpt = findValue(row, leg, daysField)
        .map(s -> Integer.valueOf(s));
    HolidayCalendarId cal = findValue(row, leg, daysCalField)
        .map(s -> HolidayCalendarId.of(s))
        .orElse(HolidayCalendarIds.NO_HOLIDAYS);
    BusinessDayAdjustment bda = parseBusinessDayAdjustment(row, leg, cnvField, calField)
        .orElse(BusinessDayAdjustment.NONE);
    if (!daysOpt.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(DaysAdjustment.builder()
        .days(daysOpt.get())
        .calendar(cal)
        .adjustment(bda)
        .build());
  }

  // adjustable date, defaulting business day convention and holiday calendar
  private static Optional<AdjustableDate> parseAdjustableDate(
      CsvRow row,
      String leg,
      String dateField,
      String cnvField,
      String calField) {

    Optional<LocalDate> dateOpt = findValue(row, leg, dateField).map(s -> LoaderUtils.parseDate(s));
    if (!dateOpt.isPresent()) {
      return Optional.empty();
    }
    BusinessDayConvention dateCnv = findValue(row, leg, cnvField)
        .map(s -> LoaderUtils.parseBusinessDayConvention(s))
        .orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
    HolidayCalendarId cal = findValue(row, leg, calField)
        .map(s -> HolidayCalendarId.of(s))
        .orElse(HolidayCalendarIds.NO_HOLIDAYS);
    return Optional.of(AdjustableDate.of(dateOpt.get(), BusinessDayAdjustment.of(dateCnv, cal)));
  }

  //-------------------------------------------------------------------------
  // gets value from CSV
  private static String getValue(CsvRow row, String leg, String field) {
    return findValue(row, leg, field)
        .orElseThrow(() -> new IllegalArgumentException("Swap leg must define field: '" + leg + field + "'"));
  }

  // gets value from CSV
  private static String getValueWithFallback(CsvRow row, String leg, String field) {
    return findValueWithFallback(row, leg, field)
        .orElseThrow(() -> new IllegalArgumentException("Swap leg must define field: '" + leg + field + "' or '" + field + "'"));
  }

  // finds value from CSV
  private static Optional<String> findValue(CsvRow row, String leg, String field) {
    return row.findValue(leg + field);
  }

  // finds value from CSV
  private static Optional<String> findValueWithFallback(CsvRow row, String leg, String field) {
    return Guavate.firstNonEmpty(row.findValue(leg + field), row.findValue(field));
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<SwapTrade> trades) {
    // determine what elements of trades are present
    int legs = 0;
    Set<SwapLegType> legTypes = new HashSet<>();
    boolean startConv = false;
    boolean endConv = false;
    boolean overrideStart = false;
    boolean fxReset = false;
    boolean fvNotional = false;
    boolean firstRate = false;
    boolean stubRate = false;
    boolean resetSchedule = false;
    boolean knownAmount = false;
    boolean variable = false;
    for (SwapTrade trade : trades) {
      legs = Math.max(legs, trade.getProduct().getLegs().size());
      for (SwapLeg leg : trade.getProduct().getLegs()) {
        legTypes.add(leg.getType());
        if (leg instanceof ScheduledSwapLeg) {
          ScheduledSwapLeg schLeg = (ScheduledSwapLeg) leg;
          startConv |= schLeg.getAccrualSchedule().getStartDateBusinessDayAdjustment().isPresent();
          endConv |= schLeg.getAccrualSchedule().getEndDateBusinessDayAdjustment().isPresent();
          overrideStart |= schLeg.getAccrualSchedule().getOverrideStartDate().isPresent();
        }
        if (leg instanceof RateCalculationSwapLeg) {
          RateCalculationSwapLeg rcLeg = (RateCalculationSwapLeg) leg;
          fxReset |= rcLeg.getNotionalSchedule().getFxReset().isPresent();
          variable |= !rcLeg.getNotionalSchedule().getAmount().getSteps().isEmpty();
          if (rcLeg.getCalculation() instanceof FixedRateCalculation) {
            FixedRateCalculation calc = (FixedRateCalculation) rcLeg.getCalculation();
            fvNotional |= calc.getFutureValueNotional().isPresent();
            variable |= !calc.getRate().getSteps().isEmpty();
          }
          if (rcLeg.getCalculation() instanceof IborRateCalculation) {
            IborRateCalculation calc = (IborRateCalculation) rcLeg.getCalculation();
            firstRate |= calc.getFirstRate().isPresent() || calc.getFirstRegularRate().isPresent();
            stubRate |= calc.getInitialStub().isPresent() || calc.getFinalStub().isPresent();
            resetSchedule |= calc.getResetPeriods().isPresent();
          }
        }
        if (leg instanceof KnownAmountSwapLeg) {
          knownAmount = true;
          KnownAmountSwapLeg kaLeg = (KnownAmountSwapLeg) leg;
          variable |= !kaLeg.getAmount().getSteps().isEmpty();
        }
      }
    }
    // select the correct headers
    LinkedHashSet<String> headers = new LinkedHashSet<>();
    if (variable) {
      headers.add(START_DATE_FIELD);
    }
    for (int i = 0; i < legs; i++) {
      String prefix = "Leg " + (i + 1) + " ";
      // accrual schedule
      headers.add(prefix + DIRECTION_FIELD);
      headers.add(prefix + START_DATE_FIELD);
      if (startConv) {
        headers.add(prefix + START_DATE_CNV_FIELD);
        headers.add(prefix + START_DATE_CAL_FIELD);
      }
      headers.add(prefix + END_DATE_FIELD);
      if (endConv) {
        headers.add(prefix + END_DATE_CNV_FIELD);
        headers.add(prefix + END_DATE_CAL_FIELD);
      }
      headers.add(prefix + FREQUENCY_FIELD);
      headers.add(prefix + ROLL_CONVENTION_FIELD);
      headers.add(prefix + STUB_CONVENTION_FIELD);
      if (overrideStart) {
        headers.add(prefix + OVERRIDE_START_DATE_FIELD);
        headers.add(prefix + OVERRIDE_START_DATE_CNV_FIELD);
        headers.add(prefix + OVERRIDE_START_DATE_CAL_FIELD);
      }
      headers.add(prefix + FIRST_REGULAR_START_DATE_FIELD);
      headers.add(prefix + LAST_REGULAR_END_DATE_FIELD);
      headers.add(prefix + DATE_ADJ_CNV_FIELD);
      headers.add(prefix + DATE_ADJ_CAL_FIELD);
      // payment schedule
      headers.add(prefix + PAYMENT_FREQUENCY_FIELD);
      headers.add(prefix + PAYMENT_RELATIVE_TO_FIELD);
      headers.add(prefix + PAYMENT_OFFSET_DAYS_FIELD);
      headers.add(prefix + PAYMENT_OFFSET_CAL_FIELD);
      headers.add(prefix + PAYMENT_OFFSET_ADJ_CNV_FIELD);
      headers.add(prefix + PAYMENT_OFFSET_ADJ_CAL_FIELD);
      headers.add(prefix + COMPOUNDING_METHOD_FIELD);
      headers.add(prefix + PAYMENT_FIRST_REGULAR_START_DATE_FIELD);
      headers.add(prefix + PAYMENT_LAST_REGULAR_END_DATE_FIELD);
      // notional
      headers.add(prefix + CURRENCY_FIELD);
      headers.add(prefix + NOTIONAL_FIELD);
      headers.add(prefix + NOTIONAL_INITIAL_EXCHANGE_FIELD);
      headers.add(prefix + NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD);
      headers.add(prefix + NOTIONAL_FINAL_EXCHANGE_FIELD);
      if (fxReset) {
        headers.add(prefix + NOTIONAL_CURRENCY_FIELD);
        headers.add(prefix + FX_RESET_INDEX_FIELD);
        headers.add(prefix + FX_RESET_RELATIVE_TO_FIELD);
        headers.add(prefix + FX_RESET_OFFSET_DAYS_FIELD);
        headers.add(prefix + FX_RESET_OFFSET_CAL_FIELD);
        headers.add(prefix + FX_RESET_OFFSET_ADJ_CNV_FIELD);
        headers.add(prefix + FX_RESET_OFFSET_ADJ_CAL_FIELD);
      }
      // calculation
      headers.add(prefix + DAY_COUNT_FIELD);
      headers.add(prefix + FIXED_RATE_FIELD);
      if (knownAmount) {
        headers.add(prefix + KNOWN_AMOUNT_FIELD);
      }
      if (fvNotional) {
        headers.add(prefix + FUTURE_VALUE_NOTIONAL_FIELD);
      }
      headers.add(prefix + INDEX_FIELD);
      if (firstRate) {
        headers.add(prefix + FIRST_RATE_FIELD);
        headers.add(prefix + FIRST_REGULAR_RATE_FIELD);
      }
      headers.add(prefix + NEGATIVE_RATE_METHOD_FIELD);
      headers.add(prefix + GEARING_FIELD);
      headers.add(prefix + SPREAD_FIELD);
      if (stubRate) {
        headers.add(prefix + INITIAL_STUB_RATE_FIELD);
        headers.add(prefix + INITIAL_STUB_AMOUNT_FIELD);
        headers.add(prefix + INITIAL_STUB_AMOUNT_CURRENCY_FIELD);
        headers.add(prefix + INITIAL_STUB_INDEX_FIELD);
        headers.add(prefix + INITIAL_STUB_INTERPOLATED_INDEX_FIELD);
        headers.add(prefix + FINAL_STUB_RATE_FIELD);
        headers.add(prefix + FINAL_STUB_AMOUNT_FIELD);
        headers.add(prefix + FINAL_STUB_AMOUNT_CURRENCY_FIELD);
        headers.add(prefix + FINAL_STUB_INDEX_FIELD);
        headers.add(prefix + FINAL_STUB_INTERPOLATED_INDEX_FIELD);
      }
      if (legTypes.contains(SwapLegType.IBOR)) {
        headers.add(prefix + FIXING_RELATIVE_TO_FIELD);
        headers.add(prefix + FIXING_OFFSET_DAYS_FIELD);
        headers.add(prefix + FIXING_OFFSET_CAL_FIELD);
        headers.add(prefix + FIXING_OFFSET_ADJ_CNV_FIELD);
        headers.add(prefix + FIXING_OFFSET_ADJ_CAL_FIELD);
        if (resetSchedule) {
          headers.add(prefix + RESET_FREQUENCY_FIELD);
          headers.add(prefix + RESET_DATE_CNV_FIELD);
          headers.add(prefix + RESET_DATE_CAL_FIELD);
          headers.add(prefix + RESET_METHOD_FIELD);
        }
      }
      if (legTypes.contains(SwapLegType.OVERNIGHT)) {
        headers.add(prefix + ACCRUAL_METHOD_FIELD);
        headers.add(prefix + RATE_CUT_OFF_DAYS_FIELD);
      }
      if (legTypes.contains(SwapLegType.INFLATION)) {
        headers.add(prefix + INFLATION_LAG_FIELD);
        headers.add(prefix + INFLATION_METHOD_FIELD);
        headers.add(prefix + INFLATION_FIRST_INDEX_VALUE_FIELD);
      }
    }
    return headers;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, SwapTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "Swap");
    VariableElements variableElements = writeProduct(csv, trade.getProduct());
    csv.writeNewLine();
    variableElements.writeLines(csv);
  }

  @Override
  public String getName() {
    return SwapTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(SwapTrade.class);
  }

  // writes the product to CSV
  VariableElements writeProduct(CsvRowOutputWithHeaders csv, Swap product) {
    VariableElements variableElements = new VariableElements();
    for (int i = 0; i < product.getLegs().size(); i++) {
      String prefix = "Leg " + (i + 1) + " ";
      SwapLeg swapLeg = product.getLegs().get(i);
      csv.writeCell(prefix + DIRECTION_FIELD, swapLeg.getPayReceive());
      if (swapLeg instanceof RateCalculationSwapLeg) {
        RateCalculationSwapLeg leg = (RateCalculationSwapLeg) swapLeg;
        writeAccrualSchedule(csv, prefix, leg);
        writePaymentSchedule(csv, prefix, leg);
        writeNotionalSchedule(csv, prefix, leg, variableElements);
        writeRateCalculation(csv, prefix, leg, variableElements);
      } else if (swapLeg instanceof KnownAmountSwapLeg) {
        KnownAmountSwapLeg leg = (KnownAmountSwapLeg) swapLeg;
        writeAccrualSchedule(csv, prefix, leg);
        writePaymentSchedule(csv, prefix, leg);
        writeKnownAmount(csv, prefix, leg, variableElements);
      } else {
        throw new IllegalArgumentException("Unable to convert swap leg to CSV: " + swapLeg.getClass().getSimpleName());
      }
    }
    return variableElements;
  }

  // writes the accrual schedule
  private void writeAccrualSchedule(CsvRowOutputWithHeaders csv, String prefix, ScheduledSwapLeg leg) {
    PeriodicSchedule accrual = leg.getAccrualSchedule();
    csv.writeCell(prefix + START_DATE_FIELD, accrual.getStartDate());
    accrual.getStartDateBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(prefix + START_DATE_CNV_FIELD, bda.getConvention());
      csv.writeCell(prefix + START_DATE_CAL_FIELD, bda.getCalendar());
    });
    csv.writeCell(prefix + END_DATE_FIELD, accrual.getEndDate());
    accrual.getEndDateBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(prefix + END_DATE_CNV_FIELD, bda.getConvention());
      csv.writeCell(prefix + END_DATE_CAL_FIELD, bda.getCalendar());
    });
    csv.writeCell(prefix + FREQUENCY_FIELD, accrual.getFrequency());
    accrual.getRollConvention().ifPresent(val -> csv.writeCell(prefix + ROLL_CONVENTION_FIELD, val));
    accrual.getStubConvention().ifPresent(val -> csv.writeCell(prefix + STUB_CONVENTION_FIELD, val));
    accrual.getOverrideStartDate().ifPresent(date -> {
      csv.writeCell(prefix + OVERRIDE_START_DATE_FIELD, date.getUnadjusted());
      csv.writeCell(prefix + OVERRIDE_START_DATE_CNV_FIELD, date.getAdjustment().getConvention());
      csv.writeCell(prefix + OVERRIDE_START_DATE_CAL_FIELD, date.getAdjustment().getCalendar());
    });
    accrual.getFirstRegularStartDate().ifPresent(val -> csv.writeCell(prefix + FIRST_REGULAR_START_DATE_FIELD, val));
    accrual.getLastRegularEndDate().ifPresent(val -> csv.writeCell(prefix + LAST_REGULAR_END_DATE_FIELD, val));
    csv.writeCell(prefix + DATE_ADJ_CNV_FIELD, accrual.getBusinessDayAdjustment().getConvention());
    csv.writeCell(prefix + DATE_ADJ_CAL_FIELD, accrual.getBusinessDayAdjustment().getCalendar());
  }

  // writes the payment schedule
  private void writePaymentSchedule(CsvRowOutputWithHeaders csv, String prefix, ScheduledSwapLeg leg) {
    PaymentSchedule payment = leg.getPaymentSchedule();
    csv.writeCell(prefix + PAYMENT_FREQUENCY_FIELD, payment.getPaymentFrequency());
    csv.writeCell(prefix + PAYMENT_RELATIVE_TO_FIELD, payment.getPaymentRelativeTo());
    DaysAdjustment payOffset = payment.getPaymentDateOffset();
    csv.writeCell(prefix + PAYMENT_OFFSET_DAYS_FIELD, payOffset.getDays());
    csv.writeCell(prefix + PAYMENT_OFFSET_CAL_FIELD, payOffset.getCalendar());
    csv.writeCell(prefix + PAYMENT_OFFSET_ADJ_CNV_FIELD, payOffset.getAdjustment().getConvention());
    csv.writeCell(prefix + PAYMENT_OFFSET_ADJ_CAL_FIELD, payOffset.getAdjustment().getCalendar());
    csv.writeCell(prefix + COMPOUNDING_METHOD_FIELD, payment.getCompoundingMethod());
    payment.getFirstRegularStartDate()
        .ifPresent(val -> csv.writeCell(prefix + PAYMENT_FIRST_REGULAR_START_DATE_FIELD, val));
    payment.getLastRegularEndDate()
        .ifPresent(val -> csv.writeCell(prefix + PAYMENT_LAST_REGULAR_END_DATE_FIELD, val));
  }

  // writes the notional schedule
  private void writeNotionalSchedule(
      CsvRowOutputWithHeaders csv,
      String prefix,
      RateCalculationSwapLeg leg,
      VariableElements mutableVariable) {

    NotionalSchedule notional = leg.getNotionalSchedule();
    csv.writeCell(prefix + CURRENCY_FIELD, notional.getCurrency());
    csv.writeCell(prefix + NOTIONAL_FIELD, notional.getAmount().getInitialValue());
    csv.writeCell(prefix + NOTIONAL_INITIAL_EXCHANGE_FIELD, notional.isInitialExchange());
    csv.writeCell(prefix + NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD, notional.isIntermediateExchange());
    csv.writeCell(prefix + NOTIONAL_FINAL_EXCHANGE_FIELD, notional.isFinalExchange());
    notional.getFxReset().ifPresent(reset -> {
      csv.writeCell(prefix + NOTIONAL_CURRENCY_FIELD, reset.getReferenceCurrency());
      csv.writeCell(prefix + FX_RESET_INDEX_FIELD, reset.getIndex());
      csv.writeCell(prefix + FX_RESET_RELATIVE_TO_FIELD, reset.getFixingRelativeTo());
      DaysAdjustment fixingOffset = reset.getFixingDateOffset();
      csv.writeCell(prefix + FX_RESET_OFFSET_DAYS_FIELD, fixingOffset.getDays());
      csv.writeCell(prefix + FX_RESET_OFFSET_CAL_FIELD, fixingOffset.getCalendar());
      csv.writeCell(prefix + FX_RESET_OFFSET_ADJ_CNV_FIELD, fixingOffset.getAdjustment().getConvention());
      csv.writeCell(prefix + FX_RESET_OFFSET_ADJ_CAL_FIELD, fixingOffset.getAdjustment().getCalendar());
      reset.getInitialNotionalValue().ifPresent(val -> csv.writeCell(prefix + FX_RESET_INITIAL_NOTIONAL_FIELD, val));
    });

    // ignore variable notional step sequence and non-replace types
    if (!notional.getAmount().getSteps().isEmpty()) {
      for (ValueStep step : notional.getAmount().getSteps()) {
        if (step.getDate().isPresent() && step.getValue().getType() == ValueAdjustmentType.REPLACE) {
          mutableVariable.add(
              step.getDate().get(),
              prefix + NOTIONAL_FIELD,
              formattedDouble(step.getValue().getModifyingValue()));
        }
      }
    }
  }

  // writes the calculation
  private void writeRateCalculation(
      CsvRowOutputWithHeaders csv,
      String prefix,
      RateCalculationSwapLeg leg,
      VariableElements mutableVariable) {

    csv.writeCell(prefix + DAY_COUNT_FIELD, leg.getCalculation().getDayCount());
    if (leg.getCalculation() instanceof FixedRateCalculation) {
      FixedRateCalculation fixed = (FixedRateCalculation) leg.getCalculation();
      csv.writeCell(prefix + FIXED_RATE_FIELD, formattedPercentage(fixed.getRate().getInitialValue()));
      fixed.getFutureValueNotional().ifPresent(fvn -> {
        // only write the value, not the date/days
        fvn.getValue().ifPresent(val -> csv.writeCell(prefix + FUTURE_VALUE_NOTIONAL_FIELD, val));
      });
      fixed.getInitialStub().ifPresent(stub -> {
        stub.getFixedRate().ifPresent(val -> csv.writeCell(prefix + INITIAL_STUB_RATE_FIELD, formattedPercentage(val)));
        stub.getKnownAmount().ifPresent(val -> csv.writeCell(prefix + INITIAL_STUB_AMOUNT_FIELD, val.getAmount()));
        stub.getKnownAmount().ifPresent(amount -> {
          csv.writeCell(prefix + INITIAL_STUB_AMOUNT_FIELD, amount.getAmount());
          csv.writeCell(prefix + INITIAL_STUB_AMOUNT_CURRENCY_FIELD, amount.getCurrency());
        });
      });
      fixed.getFinalStub().ifPresent(stub -> {
        stub.getFixedRate().ifPresent(val -> csv.writeCell(prefix + FINAL_STUB_RATE_FIELD, formattedPercentage(val)));
        stub.getKnownAmount().ifPresent(amount -> {
          csv.writeCell(prefix + FINAL_STUB_AMOUNT_FIELD, amount.getAmount());
          csv.writeCell(prefix + FINAL_STUB_AMOUNT_CURRENCY_FIELD, amount.getCurrency());
        });
      });
      // ignore variable fixed rate step sequence and non-replace types
      if (!fixed.getRate().getSteps().isEmpty()) {
        for (ValueStep step : fixed.getRate().getSteps()) {
          if (step.getDate().isPresent() && step.getValue().getType() == ValueAdjustmentType.REPLACE) {
            mutableVariable.add(
                step.getDate().get(),
                prefix + FIXED_RATE_FIELD,
                formattedPercentage(step.getValue().getModifyingValue()));
          }
        }
      }

    } else if (leg.getCalculation() instanceof IborRateCalculation) {
      // ignore first fixing date offset and variable gearing/spread
      IborRateCalculation ibor = (IborRateCalculation) leg.getCalculation();
      csv.writeCell(prefix + INDEX_FIELD, ibor.getIndex());
      ibor.getFirstRate().ifPresent(val -> csv.writeCell(prefix + FIRST_RATE_FIELD, formattedPercentage(val)));
      ibor.getFirstRegularRate()
          .ifPresent(val -> csv.writeCell(prefix + FIRST_REGULAR_RATE_FIELD, formattedPercentage(val)));
      csv.writeCell(prefix + NEGATIVE_RATE_METHOD_FIELD, ibor.getNegativeRateMethod());
      ibor.getGearing().ifPresent(val -> csv.writeCell(prefix + GEARING_FIELD, val.getInitialValue()));
      ibor.getSpread()
          .ifPresent(val -> csv.writeCell(prefix + SPREAD_FIELD, formattedPercentage(val.getInitialValue())));
      ibor.getInitialStub().ifPresent(stub -> {
        stub.getFixedRate().ifPresent(val -> csv.writeCell(prefix + INITIAL_STUB_RATE_FIELD, formattedPercentage(val)));
        stub.getKnownAmount().ifPresent(amount -> {
          csv.writeCell(prefix + INITIAL_STUB_AMOUNT_FIELD, amount.getAmount());
          csv.writeCell(prefix + INITIAL_STUB_AMOUNT_CURRENCY_FIELD, amount.getCurrency());
        });
        stub.getIndex().ifPresent(val -> csv.writeCell(prefix + INITIAL_STUB_INDEX_FIELD, val));
        stub.getIndexInterpolated().ifPresent(v -> csv.writeCell(prefix + INITIAL_STUB_INTERPOLATED_INDEX_FIELD, v));
      });
      ibor.getFinalStub().ifPresent(stub -> {
        stub.getFixedRate().ifPresent(val -> csv.writeCell(prefix + FINAL_STUB_RATE_FIELD, formattedPercentage(val)));
        stub.getKnownAmount().ifPresent(amount -> {
          csv.writeCell(prefix + FINAL_STUB_AMOUNT_FIELD, amount.getAmount());
          csv.writeCell(prefix + FINAL_STUB_AMOUNT_CURRENCY_FIELD, amount.getCurrency());
        });
        stub.getIndex().ifPresent(val -> csv.writeCell(prefix + FINAL_STUB_INDEX_FIELD, val));
        stub.getIndexInterpolated().ifPresent(v -> csv.writeCell(prefix + FINAL_STUB_INTERPOLATED_INDEX_FIELD, v));
      });
      csv.writeCell(prefix + FIXING_RELATIVE_TO_FIELD, ibor.getFixingRelativeTo());
      DaysAdjustment fixingOffset = ibor.getFixingDateOffset();
      csv.writeCell(prefix + FIXING_OFFSET_DAYS_FIELD, fixingOffset.getDays());
      csv.writeCell(prefix + FIXING_OFFSET_CAL_FIELD, fixingOffset.getCalendar());
      csv.writeCell(prefix + FIXING_OFFSET_ADJ_CNV_FIELD, fixingOffset.getAdjustment().getConvention());
      csv.writeCell(prefix + FIXING_OFFSET_ADJ_CAL_FIELD, fixingOffset.getAdjustment().getCalendar());
      ibor.getResetPeriods().ifPresent(resets -> {
        csv.writeCell(prefix + RESET_FREQUENCY_FIELD, resets.getResetFrequency());
        csv.writeCell(prefix + RESET_DATE_CNV_FIELD, resets.getBusinessDayAdjustment().getConvention());
        csv.writeCell(prefix + RESET_DATE_CAL_FIELD, resets.getBusinessDayAdjustment().getCalendar());
        csv.writeCell(prefix + RESET_METHOD_FIELD, resets.getResetMethod());
      });

    } else if (leg.getCalculation() instanceof OvernightRateCalculation) {
      // ignore variable gearing/spread
      OvernightRateCalculation on = (OvernightRateCalculation) leg.getCalculation();
      csv.writeCell(prefix + INDEX_FIELD, on.getIndex());
      csv.writeCell(prefix + RATE_CUT_OFF_DAYS_FIELD, on.getRateCutOffDays());
      csv.writeCell(prefix + ACCRUAL_METHOD_FIELD, on.getAccrualMethod());
      csv.writeCell(prefix + NEGATIVE_RATE_METHOD_FIELD, on.getNegativeRateMethod());
      on.getGearing().ifPresent(val -> csv.writeCell(prefix + GEARING_FIELD, val.getInitialValue()));
      on.getSpread().ifPresent(val -> csv.writeCell(prefix + SPREAD_FIELD, formattedPercentage(val.getInitialValue())));

    } else if (leg.getCalculation() instanceof InflationRateCalculation) {
      // ignore variable gearing
      InflationRateCalculation inf = (InflationRateCalculation) leg.getCalculation();
      csv.writeCell(prefix + INDEX_FIELD, inf.getIndex());
      csv.writeCell(prefix + INFLATION_LAG_FIELD, inf.getLag());
      csv.writeCell(prefix + INFLATION_METHOD_FIELD, inf.getIndexCalculationMethod());
      inf.getFirstIndexValue().ifPresent(val -> csv.writeCell(prefix + INFLATION_FIRST_INDEX_VALUE_FIELD, val));
      inf.getGearing().ifPresent(val -> csv.writeCell(prefix + GEARING_FIELD, val.getInitialValue()));

    } else {
      throw new IllegalArgumentException(
          "Unable to convert swap leg rate calculation to CSV: " + leg.getCalculation().getClass().getSimpleName());
    }
  }

  // writes the known amount
  private void writeKnownAmount(
      CsvRowOutputWithHeaders csv,
      String prefix,
      KnownAmountSwapLeg leg,
      VariableElements mutableVariable) {

    csv.writeCell(prefix + CURRENCY_FIELD, leg.getCurrency());
    csv.writeCell(prefix + KNOWN_AMOUNT_FIELD, leg.getAmount().getInitialValue());

    // ignore variable known amount step sequence and non-replace types
    if (!leg.getAmount().getSteps().isEmpty()) {
      for (ValueStep step : leg.getAmount().getSteps()) {
        if (step.getDate().isPresent() && step.getValue().getType() == ValueAdjustmentType.REPLACE) {
          mutableVariable.add(
              step.getDate().get(),
              prefix + KNOWN_AMOUNT_FIELD,
              formattedDouble(step.getValue().getModifyingValue()));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private FullSwapTradeCsvPlugin() {
  }

  //-------------------------------------------------------------------------
  // class to simplify variable elements
  static class VariableElements {
    private final Map<LocalDate, Map<String, String>> entries = new TreeMap<>();

    private VariableElements() {
    }

    private void add(LocalDate date, String column, String value) {
      entries.computeIfAbsent(date, dt -> createInner(dt)).put(column, value);
    }

    private HashMap<String, String> createInner(LocalDate date) {
      HashMap<String, String> innerMap = new HashMap<>();
      innerMap.put(TRADE_TYPE_FIELD, "Variable");
      innerMap.put(START_DATE_FIELD, date.toString());
      return innerMap;
    }

    void writeLines(CsvRowOutputWithHeaders csv) {
      for (Map<String, String> variableForDate : entries.values()) {
        csv.writeLine(variableForDate);
      }
    }
  }

}

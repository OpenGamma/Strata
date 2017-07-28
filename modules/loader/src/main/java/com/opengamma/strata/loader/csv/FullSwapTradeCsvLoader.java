/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.opengamma.strata.basics.ReferenceData;
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
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FixedRateStubCalculation;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.FxResetCalculation;
import com.opengamma.strata.product.swap.FxResetFixingRelativeTo;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateResetMethod;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
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
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Loads Swap trades from CSV files.
 */
final class FullSwapTradeCsvLoader {

  // CSV column headers
  static final String DIRECTION_FIELD = "Direction";
  private static final String FREQUENCY_FIELD = "Frequency";
  private static final String START_DATE_CNV_FIELD = "Start Date Convention";
  private static final String START_DATE_CAL_FIELD = "Start Date Calendar";
  private static final String END_DATE_CNV_FIELD = "End Date Convention";
  private static final String END_DATE_CAL_FIELD = "End Date Calendar";
  private static final String ROLL_CONVENTION_FIELD = "Roll Convention";
  private static final String STUB_CONVENTION_FIELD = "Stub Convention";
  private static final String FIRST_REGULAR_START_DATE_FIELD = "First Regular Start Date";
  private static final String LAST_REGULAR_END_DATE_FIELD = "Last Regular End Date";
  private static final String OVERRIDE_START_DATE_FIELD = "Override Start Date";
  private static final String OVERRIDE_START_DATE_CNV_FIELD = "Override Start Date Convention";
  private static final String OVERRIDE_START_DATE_CAL_FIELD = "Override Start Date Calendar";

  private static final String PAYMENT_FREQUENCY_FIELD = "Payment Frequency";
  private static final String PAYMENT_RELATIVE_TO_FIELD = "Payment Relative To";
  private static final String PAYMENT_OFFSET_DAYS_FIELD = "Payment Offset Days";
  private static final String PAYMENT_OFFSET_CAL_FIELD = "Payment Offset Calendar";
  private static final String PAYMENT_OFFSET_ADJ_CNV_FIELD = "Payment Offset Adjustment Convention";
  private static final String PAYMENT_OFFSET_ADJ_CAL_FIELD = "Payment Offset Adjustment Calendar";
  private static final String COMPOUNDING_METHOD_FIELD = "Compounding Method";

  private static final String NOTIONAL_CURRENCY_FIELD = "Notional Currency";
  private static final String NOTIONAL_INITIAL_EXCHANGE_FIELD = "Notional Initial Exchange";
  private static final String NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD = "Notional Intermediate Exchange";
  private static final String NOTIONAL_FINAL_EXCHANGE_FIELD = "Notional Final Exchange";
  private static final String FX_RESET_INDEX_FIELD = "FX Reset Index";
  private static final String FX_RESET_RELATIVE_TO_FIELD = "FX Reset Relative To";
  private static final String FX_RESET_OFFSET_DAYS_FIELD = "FX Reset Offset Days";
  private static final String FX_RESET_OFFSET_CAL_FIELD = "FX Reset Offset Calendar";
  private static final String FX_RESET_OFFSET_ADJ_CNV_FIELD = "FX Reset Offset Adjustment Convention";
  private static final String FX_RESET_OFFSET_ADJ_CAL_FIELD = "FX Reset Offset Adjustment Calendar";

  private static final String INITIAL_STUB_RATE_FIELD = "Initial Stub Rate";
  private static final String INITIAL_STUB_AMOUNT_FIELD = "Initial Stub Amount";
  private static final String INITIAL_STUB_INDEX_FIELD = "Initial Stub Index";
  private static final String INITIAL_STUB_INTERPOLATED_INDEX_FIELD = "Initial Stub Interpolated Index";
  private static final String FINAL_STUB_RATE_FIELD = "Final Stub Rate";
  private static final String FINAL_STUB_AMOUNT_FIELD = "Final Stub Amount";
  private static final String FINAL_STUB_INDEX_FIELD = "Final Stub Index";
  private static final String FINAL_STUB_INTERPOLATED_INDEX_FIELD = "Final Stub Interpolated Index";
  private static final String RESET_FREQUENCY_FIELD = "Reset Frequency";
  private static final String RESET_DATE_CNV_FIELD = "Reset Date Convention";
  private static final String RESET_DATE_CAL_FIELD = "Reset Date Calendar";
  private static final String RESET_METHOD_FIELD = "Reset Method";
  private static final String FIXING_RELATIVE_TO_FIELD = "Fixing Relative To";
  private static final String FIXING_OFFSET_DAYS_FIELD = "Fixing Offset Days";
  private static final String FIXING_OFFSET_CAL_FIELD = "Fixing Offset Calendar";
  private static final String FIXING_OFFSET_ADJ_CNV_FIELD = "Fixing Offset Adjustment Convention";
  private static final String FIXING_OFFSET_ADJ_CAL_FIELD = "Fixing Offset Adjustment Calendar";
  private static final String NEGATIVE_RATE_METHOD_FIELD = "Negative Rate Method";
  private static final String FIRST_RATE_FIELD = "First Rate";
  private static final String ACCRUAL_METHOD_FIELD = "Accrual Method";
  private static final String RATE_CUT_OFF_DAYS_FIELD = "Rate Cut Off Days";
  private static final String INFLATION_LAG_FIELD = "Inflation Lag";
  private static final String INFLATION_METHOD_FIELD = "Inflation Method";
  private static final String INFLATION_FIRST_VALUE_FIELD = "Inflation First Value";
  
  private static final String GEARING_FIELD = "Gearing";
  private static final String SPREAD_FIELD = "Spread";

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param refData  the reference data
   * @return the loaded trades, all errors are captured in the result
   */
  static SwapTrade parse(CsvRow row, TradeInfo info, ReferenceData refData) {
    List<RateCalculationSwapLeg> legs = new ArrayList<>();
    // parse any number of legs by looking for 'Leg n Pay Receive'
    Optional<String> payReceive = Optional.of(row.getValue("Leg 1 " + DIRECTION_FIELD));
    int i = 1;
    while (payReceive.isPresent()) {
      legs.add(parseLeg(row, "Leg " + i + " "));
      i++;
      payReceive = row.findValue("Leg " + i + " " + DIRECTION_FIELD);
    }
    Swap swap = Swap.of(legs);
    return SwapTrade.of(info, swap);
  }

  // parse a single leg
  private static RateCalculationSwapLeg parseLeg(CsvRow row, String leg) {
    PayReceive payReceive = PayReceive.of(getValue(row, leg, DIRECTION_FIELD));
    PeriodicSchedule accrualSch = parseAccrualSchedule(row, leg);
    PaymentSchedule paymentSch = parsePaymentSchedule(row, leg, accrualSch.getFrequency());
    NotionalSchedule notionalSch = parseNotionalSchedule(row, leg);
    RateCalculation calc = parseRateCalculation(
        row, leg, accrualSch.getBusinessDayAdjustment(), accrualSch.getFrequency(), notionalSch.getCurrency());
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
    builder.startDate(TradeCsvLoader.parseDate(getValueWithFallback(row, leg, START_DATE_FIELD)));
    builder.endDate(TradeCsvLoader.parseDate(getValueWithFallback(row, leg, END_DATE_FIELD)));
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
        .orElse(StubConvention.SHORT_INITIAL));
    findValue(row, leg, ROLL_CONVENTION_FIELD)
        .map(s -> RollConvention.of(s))
        .ifPresent(v -> builder.rollConvention(v));
    findValue(row, leg, FIRST_REGULAR_START_DATE_FIELD)
        .map(s -> TradeCsvLoader.parseDate(s))
        .ifPresent(v -> builder.firstRegularStartDate(v));
    findValue(row, leg, LAST_REGULAR_END_DATE_FIELD)
        .map(s -> TradeCsvLoader.parseDate(s))
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
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // notional schedule
  private static NotionalSchedule parseNotionalSchedule(CsvRow row, String leg) {
    NotionalSchedule.Builder builder = NotionalSchedule.builder();
    // basics
    Currency currency = Currency.of(getValueWithFallback(row, leg, CURRENCY_FIELD));
    builder.currency(currency);
    builder.amount(ValueSchedule.of(TradeCsvLoader.parseDouble(getValueWithFallback(row, leg, NOTIONAL_FIELD))));
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
      builder.fxReset(fxResetBuilder.build());
    } else if (notionalCurrencyOpt.isPresent() || fxFixingRelativeToOpt.isPresent() || fxResetAdjOpt.isPresent()) {
      throw new IllegalArgumentException("Swap trade FX Reset must define field '" + FX_RESET_INDEX_FIELD + "'");
    }
    // optionals
    findValue(row, leg, NOTIONAL_INITIAL_EXCHANGE_FIELD)
        .map(s -> TradeCsvLoader.parseBoolean(s))
        .ifPresent(v -> builder.initialExchange(v));
    findValue(row, leg, NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD)
        .map(s -> TradeCsvLoader.parseBoolean(s))
        .ifPresent(v -> builder.intermediateExchange(v));
    findValue(row, leg, NOTIONAL_FINAL_EXCHANGE_FIELD)
        .map(s -> TradeCsvLoader.parseBoolean(s))
        .ifPresent(v -> builder.finalExchange(v));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // rate calculation
  private static RateCalculation parseRateCalculation(
      CsvRow row,
      String leg,
      BusinessDayAdjustment bda,
      Frequency accrualFrequency,
      Currency currency) {

    Optional<Double> fixedRateOpt = findValue(row, leg, FIXED_RATE_FIELD).map(s -> TradeCsvLoader.parseDoublePercent(s));
    Optional<String> indexOpt = findValue(row, leg, INDEX_FIELD);
    if (fixedRateOpt.isPresent()) {
      if (indexOpt.isPresent()) {
        throw new IllegalArgumentException("Swap leg must not define both '" + FIXED_RATE_FIELD + "' and  '" + INDEX_FIELD + "'");
      }
      return parseFixedRateCalculation(row, leg, fixedRateOpt.get(), currency);
    }
    if (!indexOpt.isPresent()) {
      throw new IllegalArgumentException("Swap leg must define either '" + FIXED_RATE_FIELD + "' or  '" + INDEX_FIELD + "'");
    }
    // index might be whole Ibor Index or Floating Rate Name
    String indexStr = indexOpt.get();
    Optional<FloatingRateName> frnOpt = FloatingRateName.extendedEnum().find(indexStr);
    if (frnOpt.isPresent()) {
      FloatingRateName frn = FloatingRateName.of(indexStr);
      switch (frn.getType()) {
        case IBOR: {
          // imply index from accrual frequency
          IborIndex ibor = frn.toIborIndex(Tenor.of(accrualFrequency.getPeriod()));
          return parseIborRateCalculation(row, leg, ibor, bda, currency);
        }
        case OVERNIGHT_COMPOUNDED: {
          return parseOvernightRateCalculation(row, leg, frn.toOvernightIndex(), OvernightAccrualMethod.COMPOUNDED);
        }
        case OVERNIGHT_AVERAGED: {
          return parseOvernightRateCalculation(row, leg, frn.toOvernightIndex(), OvernightAccrualMethod.AVERAGED);
        }
        case PRICE: {
          return parseInflationRateCalculation(row, leg, frn.toPriceIndex(), currency);
        }
        default:
          throw new IllegalArgumentException("Swap trade index type not known: " + indexStr);
      }
    }
    Optional<IborIndex> iborOpt = IborIndex.extendedEnum().find(indexStr);
    if (iborOpt.isPresent()) {
      return parseIborRateCalculation(row, leg, iborOpt.get(), bda, currency);
    }
    Optional<OvernightIndex> overnightOpt = OvernightIndex.extendedEnum().find(indexStr);
    if (overnightOpt.isPresent()) {
      return parseOvernightRateCalculation(row, leg, overnightOpt.get(), OvernightAccrualMethod.COMPOUNDED);
    }
    Optional<PriceIndex> priceOpt = PriceIndex.extendedEnum().find(indexStr);
    if (priceOpt.isPresent()) {
      return parseInflationRateCalculation(row, leg, priceOpt.get(), currency);
    }
    throw new IllegalArgumentException("Swap trade index not known: " + indexStr);
  }

  //-------------------------------------------------------------------------
  // fixed rate calculation
  private static RateCalculation parseFixedRateCalculation(CsvRow row, String leg, double fixedRate, Currency currency) {
    FixedRateCalculation.Builder builder = FixedRateCalculation.builder();
    // basics
    builder.dayCount(DayCount.of(getValue(row, leg, DAY_COUNT_FIELD)));
    builder.rate(ValueSchedule.of(fixedRate));
    // initial stub
    Optional<Double> initialStubRateOpt = findValue(row, leg, INITIAL_STUB_RATE_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s));
    Optional<Double> initialStubAmountOpt = findValue(row, leg, INITIAL_STUB_AMOUNT_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s));
    if (initialStubRateOpt.isPresent() && initialStubAmountOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must not define both '" + INITIAL_STUB_RATE_FIELD + "' and  '" + INITIAL_STUB_AMOUNT_FIELD + "'");
    }
    initialStubRateOpt.ifPresent(v -> builder.initialStub(
        FixedRateStubCalculation.ofFixedRate(v)));
    initialStubAmountOpt.ifPresent(v -> builder.initialStub(
        FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(currency, v))));
    // final stub
    Optional<Double> finalStubRateOpt = findValue(row, leg, FINAL_STUB_RATE_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s));
    Optional<Double> finalStubAmountOpt = findValue(row, leg, FINAL_STUB_AMOUNT_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s));
    if (finalStubRateOpt.isPresent() && finalStubAmountOpt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must not define both '" + FINAL_STUB_RATE_FIELD + "' and  '" + FINAL_STUB_AMOUNT_FIELD + "'");
    }
    finalStubRateOpt.ifPresent(v -> builder.finalStub(
        FixedRateStubCalculation.ofFixedRate(v)));
    finalStubAmountOpt.ifPresent(v -> builder.finalStub(
        FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(currency, v))));
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
        .orElse(IborRateResetMethod.UNWEIGHTED);
    BusinessDayAdjustment resetDateAdj =
        parseBusinessDayAdjustment(row, leg, RESET_DATE_CNV_FIELD, RESET_DATE_CAL_FIELD).orElse(bda);
    resetFrequencyOpt.ifPresent(freq -> builder.resetPeriods(ResetSchedule.builder()
        .resetFrequency(freq)
        .resetMethod(resetMethod)
        .businessDayAdjustment(resetDateAdj)
        .build()));
    // optionals, no ability to set firstFixingDateOffset
    findValue(row, leg, DAY_COUNT_FIELD)
        .map(s -> DayCount.of(s))
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
        .map(s -> TradeCsvLoader.parseDoublePercent(s))
        .ifPresent(v -> builder.firstRate(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s))
        .ifPresent(v -> builder.gearing(ValueSchedule.of(v)));
    findValue(row, leg, SPREAD_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s))
        .ifPresent(v -> builder.spread(ValueSchedule.of(v)));
    // initial stub
    Optional<Double> initialStubRateOpt = findValue(row, leg, INITIAL_STUB_RATE_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s));
    Optional<Double> initialStubAmountOpt = findValue(row, leg, INITIAL_STUB_AMOUNT_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s));
    Optional<IborIndex> initialStubIndexOpt = findValue(row, leg, INITIAL_STUB_INDEX_FIELD)
        .map(s -> IborIndex.of(s));
    Optional<IborIndex> initialStubIndex2Opt = findValue(row, leg, INITIAL_STUB_INTERPOLATED_INDEX_FIELD)
        .map(s -> IborIndex.of(s));
    if (initialStubRateOpt.isPresent() && !initialStubAmountOpt.isPresent() &&
        !initialStubIndexOpt.isPresent() && !initialStubIndex2Opt.isPresent()) {
      builder.initialStub(IborRateStubCalculation.ofFixedRate(initialStubRateOpt.get()));

    } else if (!initialStubRateOpt.isPresent() && initialStubAmountOpt.isPresent() &&
        !initialStubIndexOpt.isPresent() && !initialStubIndex2Opt.isPresent()) {
      builder.initialStub(IborRateStubCalculation.ofKnownAmount(CurrencyAmount.of(currency, initialStubAmountOpt.get())));

    } else if (!initialStubRateOpt.isPresent() && !initialStubAmountOpt.isPresent() && initialStubIndexOpt.isPresent()) {
      if (initialStubIndex2Opt.isPresent()) {
        builder.initialStub(
            IborRateStubCalculation.ofIborInterpolatedRate(initialStubIndexOpt.get(), initialStubIndex2Opt.get()));
      } else {
        builder.initialStub(IborRateStubCalculation.ofIborRate(initialStubIndexOpt.get()));
      }
    } else if (initialStubRateOpt.isPresent() || initialStubAmountOpt.isPresent() ||
        initialStubIndexOpt.isPresent() || initialStubIndex2Opt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must define only one of the following fields " +
              ImmutableList.of(leg + INITIAL_STUB_RATE_FIELD, leg + INITIAL_STUB_AMOUNT_FIELD, leg + INITIAL_STUB_INDEX_FIELD) +
              ", and '" + leg + INITIAL_STUB_INTERPOLATED_INDEX_FIELD + "' is only allowed with '" +
              leg + INITIAL_STUB_INDEX_FIELD + "'");
    }
    // final stub
    Optional<Double> finalStubRateOpt = findValue(row, leg, FINAL_STUB_RATE_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s));
    Optional<Double> finalStubAmountOpt = findValue(row, leg, FINAL_STUB_AMOUNT_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s));
    Optional<IborIndex> finalStubIndexOpt = findValue(row, leg, FINAL_STUB_INDEX_FIELD)
        .map(s -> IborIndex.of(s));
    Optional<IborIndex> finalStubIndex2Opt = findValue(row, leg, FINAL_STUB_INTERPOLATED_INDEX_FIELD)
        .map(s -> IborIndex.of(s));
    if (finalStubRateOpt.isPresent() && !finalStubAmountOpt.isPresent() &&
        !finalStubIndexOpt.isPresent() && !finalStubIndex2Opt.isPresent()) {
      builder.finalStub(IborRateStubCalculation.ofFixedRate(finalStubRateOpt.get()));

    } else if (!finalStubRateOpt.isPresent() && finalStubAmountOpt.isPresent() &&
        !finalStubIndexOpt.isPresent() && !finalStubIndex2Opt.isPresent()) {
      builder.finalStub(IborRateStubCalculation.ofKnownAmount(CurrencyAmount.of(currency, finalStubAmountOpt.get())));

    } else if (!finalStubRateOpt.isPresent() && !finalStubAmountOpt.isPresent() && finalStubIndexOpt.isPresent()) {
      if (finalStubIndex2Opt.isPresent()) {
        builder.finalStub(
            IborRateStubCalculation.ofIborInterpolatedRate(finalStubIndexOpt.get(), finalStubIndex2Opt.get()));
      } else {
        builder.finalStub(IborRateStubCalculation.ofIborRate(finalStubIndexOpt.get()));
      }
    } else if (finalStubRateOpt.isPresent() || finalStubAmountOpt.isPresent() ||
        finalStubIndexOpt.isPresent() || finalStubIndex2Opt.isPresent()) {
      throw new IllegalArgumentException(
          "Swap leg must define only one of the following fields " +
              ImmutableList.of(leg + FINAL_STUB_RATE_FIELD, leg + FINAL_STUB_AMOUNT_FIELD, leg + FINAL_STUB_INDEX_FIELD) +
              ", and '" + leg + FINAL_STUB_INTERPOLATED_INDEX_FIELD + "' is only allowed with '" +
              leg + FINAL_STUB_INDEX_FIELD + "'");
    }
    return builder.build();
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
        .map(s -> DayCount.of(s))
        .ifPresent(v -> builder.dayCount(v));
    findValue(row, leg, RATE_CUT_OFF_DAYS_FIELD)
        .map(s -> Integer.valueOf(s))
        .ifPresent(v -> builder.rateCutOffDays(v));
    findValue(row, leg, NEGATIVE_RATE_METHOD_FIELD).map(s -> NegativeRateMethod.of(s))
        .ifPresent(v -> builder.negativeRateMethod(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s))
        .ifPresent(v -> builder.gearing(ValueSchedule.of(v)));
    findValue(row, leg, SPREAD_FIELD)
        .map(s -> TradeCsvLoader.parseDoublePercent(s))
        .ifPresent(v -> builder.spread(ValueSchedule.of(v)));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // inflation rate calculation
  private static RateCalculation parseInflationRateCalculation(CsvRow row, String leg, PriceIndex priceIndex, Currency currency) {
    InflationRateCalculation.Builder builder = InflationRateCalculation.builder();
    // basics
    builder.index(priceIndex);
    builder.lag(parseInflationLag(findValue(row, leg, INFLATION_LAG_FIELD), currency));
    builder.indexCalculationMethod(parseInflationMethod(findValue(row, leg, INFLATION_METHOD_FIELD), currency));
    // optionals
    findValue(row, leg, INFLATION_FIRST_VALUE_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s))
        .ifPresent(v -> builder.firstIndexValue(v));
    findValue(row, leg, GEARING_FIELD)
        .map(s -> TradeCsvLoader.parseDouble(s))
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
        .map(s -> BusinessDayConvention.of(s))
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
        .map(s -> new Integer(s));
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

    Optional<LocalDate> dateOpt = findValue(row, leg, dateField).map(s -> TradeCsvLoader.parseDate(s));
    if (!dateOpt.isPresent()) {
      return Optional.empty();
    }
    BusinessDayConvention dateCnv = findValue(row, leg, cnvField)
        .map(s -> BusinessDayConvention.of(s))
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
  // Restricted constructor.
  private FullSwapTradeCsvLoader() {
  }

}

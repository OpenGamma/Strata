/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import java.time.LocalDate;
import java.util.List;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;

/**
 * A key for the map of explanatory values.
 * <p>
 * This key is used with {@link ExplainMap} to create a loosely defined data structure
 * that allows an explanation of a calculation to be represented.
 * 
 * @param <T>  the type of the object associated with the key
 */
public final class ExplainKey<T>
    extends TypedString<ExplainKey<T>> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * The index of this entry within the parent.
   * For example, this could be used to represent the index of the leg within the swap,
   * or the index of the payment period within the leg.
   */
  public static final ExplainKey<Integer> ENTRY_INDEX = of("EntryIndex");
  /**
   * The type of this entry.
   * For example, this could be used to distinguish between a swap leg, swap payment period and a FRA.
   */
  public static final ExplainKey<String> ENTRY_TYPE = of("EntryType");

  /**
   * The list of legs.
   */
  public static final ExplainKey<List<ExplainMap>> LEGS = of("Legs");
  /**
   * The list of payment events.
   */
  public static final ExplainKey<List<ExplainMap>> PAYMENT_EVENTS = of("PaymentEvents");
  /**
   * The list of payment periods.
   */
  public static final ExplainKey<List<ExplainMap>> PAYMENT_PERIODS = of("PaymentPeriods");
  /**
   * The list of accrual periods.
   */
  public static final ExplainKey<List<ExplainMap>> ACCRUAL_PERIODS = of("AccrualPeriods");
  /**
   * The list of reset periods.
   */
  public static final ExplainKey<List<ExplainMap>> RESET_PERIODS = of("ResetPeriods");
  /**
   * The list of rate observations.
   */
  public static final ExplainKey<List<ExplainMap>> OBSERVATIONS = of("Observations");

  /**
   * The present value.
   */
  public static final ExplainKey<CurrencyAmount> PRESENT_VALUE = of("PresentValue");
  /**
   * The forecast value.
   */
  public static final ExplainKey<CurrencyAmount> FORECAST_VALUE = of("ForecastValue");
  /**
   * The flag to indicate that the period has completed.
   * For example, a swap payment period that has already paid would have this set to true.
   * This will generally never be set to false.
   */
  public static final ExplainKey<Boolean> COMPLETED = of("Completed");

  /**
   * The currency of the payment.
   */
  public static final ExplainKey<Currency> PAYMENT_CURRENCY = of("PaymentCurrency");
  /**
   * Whether the entry is being paid or received.
   */
  public static final ExplainKey<PayReceive> PAY_RECEIVE = of("PayReceive");
  /**
   * An indication of the pay-off formula that applies to the leg.
   * For example, this could be used to distinguish between fixed, overnight, IBOR and inflation.
   */
  public static final ExplainKey<String> LEG_TYPE = of("LegType");
  /**
   * The effective notional, which may be converted from the contract notional in the case of FX reset.
   */
  public static final ExplainKey<CurrencyAmount> NOTIONAL = of("Notional");
  /**
   * The notional, as defined in the trade.
   * This is the notional in the trade, which may be converted to the actual notional by FX reset.
   */
  public static final ExplainKey<CurrencyAmount> TRADE_NOTIONAL = of("TradeNotional");

  /**
   * The payment date, adjusted to be a valid business day if necessary.
   */
  public static final ExplainKey<LocalDate> PAYMENT_DATE = of("PaymentDate");
  /**
   * The payment date, before any business day adjustment.
   */
  public static final ExplainKey<LocalDate> UNADJUSTED_PAYMENT_DATE = of("PaymentDate");

  /**
   * The accrual start date, adjusted to be a valid business day if necessary.
   */
  public static final ExplainKey<LocalDate> START_DATE = of("StartDate");
  /**
   * The accrual start date, before any business day adjustment.
   */
  public static final ExplainKey<LocalDate> UNADJUSTED_START_DATE = of("UnadjustedStartDate");
  /**
   * The accrual end date, adjusted to be a valid business day if necessary.
   */
  public static final ExplainKey<LocalDate> END_DATE = of("EndDate");
  /**
   * The accrual end date, before any business day adjustment.
   */
  public static final ExplainKey<LocalDate> UNADJUSTED_END_DATE = of("UnadjustedEndDate");

  /**
   * The day count used to calculate the year fraction.
   */
  public static final ExplainKey<DayCount> ACCRUAL_DAY_COUNT = of("AccrualDayCount");
  /**
   * The year fraction between the start and end dates.
   */
  public static final ExplainKey<Double> ACCRUAL_YEAR_FRACTION = of("AccrualYearFraction");
  /**
   * The number of accrual days between the start and end dates.
   */
  public static final ExplainKey<Integer> ACCRUAL_DAYS = of("AccrualDays");
  /**
   * The actual number of days between the start and end dates.
   */
  public static final ExplainKey<Integer> DAYS = of("Days");

  /**
   * The discount factor, typically derived from a curve.
   */
  public static final ExplainKey<Double> DISCOUNT_FACTOR = of("DiscountFactor");
  /**
   * The fixed rate, as defined in the contract.
   */
  public static final ExplainKey<Double> FIXED_RATE = of("FixedRate");

  /**
   * The observed index, such as an Ibor or Overnight index.
   */
  public static final ExplainKey<Index> INDEX = of("Index");
  /**
   * The fixing date.
   */
  public static final ExplainKey<LocalDate> FIXING_DATE = of("FixingDate");
  /**
   * The observed index value, typically derived from a curve.
   * This may be known exactly if the fixing has occurred.
   */
  public static final ExplainKey<Double> INDEX_VALUE = of("IndexValue");
  /**
   * The flag to indicate that the that the observed value is from a fixing time-series.
   * This will generally never be set to false.
   */
  public static final ExplainKey<Boolean> FROM_FIXING_SERIES = of("FromFixingSeries");
  /**
   * The weight of this observation.
   * Weighting applies when averaging more than one observation to produce the final rate.
   */
  public static final ExplainKey<Double> WEIGHT = of("Weight");
  /**
   * The combined rate, including weighting.
   * This rate differs from the observed rate if there is more than one fixing involved.
   * For example, {@link IborInterpolatedRateComputation} has two observed rates
   * which are combined to create this rate.
   */
  public static final ExplainKey<Double> COMBINED_RATE = of("CombinedRate");
  /**
   * The spread, added to the forward rate.
   */
  public static final ExplainKey<Double> SPREAD = of("Spread");
  /**
   * The gearing, that the rate is multiplied by.
   */
  public static final ExplainKey<Double> GEARING = of("Gearing");
  /**
   * The pay-off rate, which includes adjustments like weighting, spread and gearing.
   */
  public static final ExplainKey<Double> PAY_OFF_RATE = of("PayOffRate");
  /**
   * The unit amount.
   * This is typically the rate multiplied by the year fraction, before multiplication by the notional.
   */
  public static final ExplainKey<Double> UNIT_AMOUNT = of("UnitAmount");
  /**
   * The method of compounding.
   */
  public static final ExplainKey<CompoundingMethod> COMPOUNDING = of("CompoundingMethod");
  /**
   * The strike value.
   */
  public static final ExplainKey<Double> STRIKE_VALUE = of("StrikeValue");
  /**
   * The convexity adjusted rate.
   */
  public static final ExplainKey<Double> CONVEXITY_ADJUSTED_RATE = of("ConvexityAdjustedRate");
  /**
   * The forward rate.
   */
  public static final ExplainKey<Double> FORWARD_RATE = of("ForwardRate");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Field names may contain any character, but must not be empty.
   *
   * @param <R>  the inferred type of the key
   * @param name  the name of the field
   * @return a field with the specified name
   */
  @FromString
  public static <R> ExplainKey<R> of(String name) {
    return new ExplainKey<R>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the field
   */
  private ExplainKey(String name) {
    super(name);
  }

}

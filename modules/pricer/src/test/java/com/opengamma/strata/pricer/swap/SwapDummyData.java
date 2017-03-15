/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.MONTHLY;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;

import java.time.Period;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Basic dummy objects used when the data within is not important.
 */
public final class SwapDummyData {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * The notional.
   */
  public static final double NOTIONAL = 1_000_000d;
  /**
   * NotionalExchange (receive - GBP).
   */
  public static final NotionalExchange NOTIONAL_EXCHANGE_REC_GBP =
      NotionalExchange.of(CurrencyAmount.of(Currency.GBP, NOTIONAL), date(2014, 7, 1));
  /**
   * NotionalExchange (pay - GBP).
   */
  public static final NotionalExchange NOTIONAL_EXCHANGE_PAY_GBP =
      NotionalExchange.of(CurrencyAmount.of(Currency.GBP, -NOTIONAL), date(2014, 7, 1));
  /**
   * NotionalExchange (pay - USD).
   */
  public static final NotionalExchange NOTIONAL_EXCHANGE_PAY_USD =
      NotionalExchange.of(CurrencyAmount.of(Currency.USD, -1.5d * NOTIONAL), date(2014, 7, 1));
  /**
   * NotionalExchange.
   */
  public static final FxResetNotionalExchange FX_RESET_NOTIONAL_EXCHANGE_REC_USD = FxResetNotionalExchange.of(
      CurrencyAmount.of(Currency.USD, NOTIONAL),
      date(2014, 7, 1),
      FxIndexObservation.of(FxIndices.GBP_USD_WM, date(2014, 7, 1), REF_DATA));

  public static final FxResetNotionalExchange FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP = FxResetNotionalExchange.of(
      CurrencyAmount.of(Currency.GBP, -NOTIONAL),
      date(2014, 7, 1),
      FxIndexObservation.of(FxIndices.GBP_USD_WM, date(2014, 7, 1), REF_DATA));

  /**
   * IborRateComputation.
   */
  public static final IborRateComputation IBOR_RATE_COMP =
      IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
  /**
   * RateAccuralPeriod (ibor).
   */
  public static final RateAccrualPeriod IBOR_RATE_ACCRUAL_PERIOD = RateAccrualPeriod.builder()
      .startDate(date(2014, 7, 2))
      .endDate(date(2014, 10, 2))
      .rateComputation(IBOR_RATE_COMP)
      .yearFraction(0.25d)
      .build();
  /**
   * RateAccuralPeriod (ibor).
   */
  public static final RateAccrualPeriod IBOR_RATE_ACCRUAL_PERIOD_2 = RateAccrualPeriod.builder()
      .startDate(date(2014, 10, 2))
      .endDate(date(2015, 1, 2))
      .rateComputation(IborRateComputation.of(GBP_LIBOR_3M, date(2014, 9, 30), REF_DATA))
      .yearFraction(0.25d)
      .build();
  /**
   * RatePaymentPeriod (ibor).
   */
  public static final RatePaymentPeriod IBOR_RATE_PAYMENT_PERIOD_REC_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 6))
      .accrualPeriods(IBOR_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .notional(NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (ibor).
   */
  public static final RatePaymentPeriod IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2 = RatePaymentPeriod.builder()
      .paymentDate(date(2015, 1, 4))
      .accrualPeriods(IBOR_RATE_ACCRUAL_PERIOD_2)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .notional(NOTIONAL)
      .build();
  /**
   * ResolvedSwapLeg (ibor).
   */
  public static final ResolvedSwapLeg IBOR_SWAP_LEG_REC_GBP = ResolvedSwapLeg.builder()
      .type(IBOR)
      .payReceive(RECEIVE)
      .paymentPeriods(IBOR_RATE_PAYMENT_PERIOD_REC_GBP)
      .paymentEvents(NOTIONAL_EXCHANGE_REC_GBP)
      .build();
  /**
   * ResolvedSwapLeg (ibor).
   */
  public static final ResolvedSwapLeg IBOR_SWAP_LEG_REC_GBP_MULTI = ResolvedSwapLeg.builder()
      .type(IBOR)
      .payReceive(RECEIVE)
      .paymentPeriods(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2)
      .paymentEvents(NOTIONAL_EXCHANGE_REC_GBP)
      .build();
  /**
   * ResolvedSwapLeg (known amount).
   */
  public static final ResolvedSwapLeg KNOWN_AMOUNT_SWAP_LEG = KnownAmountSwapLeg.builder()
      .payReceive(PayReceive.RECEIVE)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(date(2014, 4, 2))
          .endDate(date(2014, 10, 2))
          .frequency(Frequency.P3M)
          .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(Frequency.P3M)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
          .build())
      .currency(GBP)
      .amount(ValueSchedule.of(1000))
      .build()
      .resolve(REF_DATA);

  /**
   * FixedRateComputation.
   */
  public static final FixedRateComputation FIXED_RATE_COMP = FixedRateComputation.of(0.0123d);
  /**
   * RateAccuralPeriod (fixed).
   */
  public static final RateAccrualPeriod FIXED_RATE_ACCRUAL_PERIOD = RateAccrualPeriod.builder()
      .startDate(date(2014, 7, 2))
      .endDate(date(2014, 10, 2))
      .rateComputation(FIXED_RATE_COMP)
      .yearFraction(0.25d)
      .build();
  /**
   * RateAccuralPeriod (fixed).
   */
  public static final RateAccrualPeriod FIXED_RATE_ACCRUAL_PERIOD_2 = RateAccrualPeriod.builder()
      .startDate(date(2014, 10, 2))
      .endDate(date(2015, 1, 2))
      .rateComputation(FIXED_RATE_COMP)
      .yearFraction(0.25d)
      .build();
  /**
   * RatePaymentPeriod (fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_REC_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 6))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .notional(NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_CMP_NONE_REC_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2015, 1, 2))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD, FIXED_RATE_ACCRUAL_PERIOD_2)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .compoundingMethod(CompoundingMethod.NONE)
      .notional(NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_CMP_FLAT_REC_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2015, 1, 2))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD, FIXED_RATE_ACCRUAL_PERIOD_2)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .compoundingMethod(CompoundingMethod.FLAT)
      .notional(NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (fixed - payer).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_PAY_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 4))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .notional(-NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (fixed - payer).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_FX_RESET_PERIOD_PAY_GBP = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 4))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.GBP)
      .notional(-NOTIONAL)
      .fxReset(FxReset.of(FxIndexObservation.of(FxIndices.GBP_USD_WM, date(2014, 7, 2), REF_DATA), Currency.USD))
      .build();
  /**
   * ResolvedSwapLeg (GBP - fixed - receiver).
   */
  public static final ResolvedSwapLeg FIXED_SWAP_LEG_REC = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(RECEIVE)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_REC_GBP)
      .paymentEvents(NOTIONAL_EXCHANGE_REC_GBP)
      .build();
  /**
   * ResolvedSwapLeg (GBP - fixed - payer).
   */
  public static final ResolvedSwapLeg FIXED_SWAP_LEG_PAY = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_GBP)
      .paymentEvents(NOTIONAL_EXCHANGE_PAY_GBP)
      .build();
  /**
   * RatePaymentPeriod (USD - fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_REC_USD = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 4))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.USD)
      .notional(NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (USD - fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_PAY_USD = RatePaymentPeriod.builder()
      .paymentDate(date(2014, 10, 4))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD)
      .dayCount(ACT_365F)
      .currency(Currency.USD)
      .notional(-NOTIONAL)
      .build();
  /**
   * RatePaymentPeriod (USD - fixed - receiver).
   */
  public static final RatePaymentPeriod FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2 = RatePaymentPeriod.builder()
      .paymentDate(date(2015, 1, 4))
      .accrualPeriods(FIXED_RATE_ACCRUAL_PERIOD_2)
      .dayCount(ACT_365F)
      .currency(Currency.USD)
      .notional(-NOTIONAL)
      .build();
  /**
   * ResolvedSwapLeg  (USD - fixed - receiver).
   */
  public static final ResolvedSwapLeg FIXED_SWAP_LEG_REC_USD = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(RECEIVE)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_REC_USD)
      .build();
  /**
   * ResolvedSwapLeg  (USD - fixed - receiver).
   */
  public static final ResolvedSwapLeg FIXED_SWAP_LEG_PAY_USD = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD)
      .paymentEvents(NOTIONAL_EXCHANGE_PAY_USD)
      .build();
  /**
   * ResolvedSwapLeg  (USD - fixed - receiver - FX reset).
   */
  public static final ResolvedSwapLeg FIXED_FX_RESET_SWAP_LEG_PAY_GBP = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(FIXED_RATE_PAYMENT_FX_RESET_PERIOD_PAY_GBP)
      .paymentEvents(FX_RESET_NOTIONAL_EXCHANGE_REC_USD)
      .build();
  /**
   * ResolvedSwapLeg  (GBP - fixed - receiver - compounding).
   */
  public static final ResolvedSwapLeg FIXED_CMP_NONE_SWAP_LEG_PAY_GBP = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_CMP_NONE_REC_GBP)
      .build();
  /**
   * ResolvedSwapLeg  (GBP - fixed - receiver - compounding).
   */
  public static final ResolvedSwapLeg FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_CMP_FLAT_REC_GBP)
      .build();
  /**
   * ResolvedSwapLeg (fixed).
   */
  public static final ResolvedSwapLeg FIXED_RATECALC_SWAP_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PayReceive.RECEIVE)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(date(2014, 4, 2))
          .endDate(date(2014, 10, 2))
          .frequency(Frequency.P3M)
          .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(Frequency.P3M)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
          .build())
      .notionalSchedule(NotionalSchedule.of(Currency.GBP, NOTIONAL))
      .calculation(FixedRateCalculation.of(0.0123d, DayCounts.ACT_365F))
      .build()
      .resolve(REF_DATA);
  /**
   * ResolvedSwapLeg (inflation)
   */
  public static final ResolvedSwapLeg INFLATION_MONTHLY_SWAP_LEG_REC_GBP = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(date(2014, 6, 9))
          .endDate(date(2019, 6, 9))
          .frequency(Frequency.ofYears(5))
          .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(Frequency.ofYears(5))
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
          .build())
      .calculation(InflationRateCalculation.builder()
          .index(GB_RPI)
          .indexCalculationMethod(MONTHLY)
          .lag(Period.ofMonths(3))
          .build())
      .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
      .build()
      .resolve(REF_DATA);
  /**
   * ResolvedSwapLeg fixed rate.
   */
  public static final double INFLATION_FIXED_SWAP_LEG_PAY_GBP_FIXED_RATE = 0.0358d;
  /**
   * ResolvedSwapLeg (fixed - to be used as a counterpart of INFLATION_MONTHLY_SWAP_LEG_REC_GBP)
   */
  public static final ResolvedSwapLeg INFLATION_FIXED_SWAP_LEG_PAY_GBP = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(date(2014, 6, 9))
          .endDate(date(2019, 6, 9))
          .frequency(Frequency.P12M)
          .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(Frequency.ofYears(5))
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
          .compoundingMethod(CompoundingMethod.STRAIGHT)
          .build())
      .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
      .calculation(FixedRateCalculation.builder()
          .rate(ValueSchedule.of(INFLATION_FIXED_SWAP_LEG_PAY_GBP_FIXED_RATE))
          .dayCount(DayCounts.ONE_ONE) // year fraction is always 1.
          .build())
      .build()
      .resolve(REF_DATA);

  /**
   * Single currency swap.
   */
  public static final ResolvedSwap SWAP =
      ResolvedSwap.of(IBOR_SWAP_LEG_REC_GBP, FIXED_SWAP_LEG_PAY);
  /**
   * Cross currency swap.
   */
  public static final ResolvedSwap SWAP_CROSS_CURRENCY =
      ResolvedSwap.of(IBOR_SWAP_LEG_REC_GBP, FIXED_SWAP_LEG_PAY_USD);

  /**
   * Inflation Swap.
   */
  public static final ResolvedSwap SWAP_INFLATION = ResolvedSwap.builder()
      .legs(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, INFLATION_FIXED_SWAP_LEG_PAY_GBP)
      .build();

  /**
   * Swap trade.
   */
  public static final ResolvedSwapTrade SWAP_TRADE = ResolvedSwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
      .product(SWAP)
      .build();

  /**
   * Swap trade.
   */
  public static final ResolvedSwapTrade SWAP_TRADE_CROSS_CURRENCY = ResolvedSwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
      .product(SWAP_CROSS_CURRENCY)
      .build();

  /**
   * Restricted constructor.
   */
  private SwapDummyData() {
  }

}

/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;
import com.opengamma.strata.finance.rate.swap.FxReset;
import com.opengamma.strata.finance.rate.swap.NegativeRateMethod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.pricer.CurveSensitivityTestUtil;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test {@link DiscountingRatePaymentPeriodPricer}
 */
@Test
public class DiscountingRatePaymentPeriodPricerFnTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 22);
  private static final LocalDate FX_DATE_1 = LocalDate.of(2014, 1, 22);
  private static final LocalDate CPN_DATE_1 = LocalDate.of(2014, 1, 24);
  private static final LocalDate CPN_DATE_2 = LocalDate.of(2014, 4, 24);
  private static final LocalDate CPN_DATE_3 = LocalDate.of(2014, 7, 24);
  private static final LocalDate CPN_DATE_4 = LocalDate.of(2014, 10, 24);
  private static final double ACCRUAL_FACTOR_1 = 0.245;
  private static final double ACCRUAL_FACTOR_2 = 0.255;
  private static final double ACCRUAL_FACTOR_3 = 0.25;
  private static final double GEARING = 2.0;
  private static final double SPREAD = -0.0025;
  private static final double NOTIONAL_100 = 1.0E8;
  private static final LocalDate PAYMENT_DATE_1 = LocalDate.of(2014, 4, 26);
  private static final LocalDate PAYMENT_DATE_3 = LocalDate.of(2014, 10, 26);
  private static final double RATE_1 = 0.0123d;
  private static final double RATE_2 = 0.0127d;
  private static final double RATE_3 = 0.0135d;
  private static final double RATE_FX = 1.6d;
  private static final double DISCOUNT_FACTOR = 0.976d;
  private static final double TOLERANCE_PV = 1E-7;

  private static final RateAccrualPeriod ACCRUAL_PERIOD_1 = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_NEG = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .gearing(-1d)
      .negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_2_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_2)
      .endDate(CPN_DATE_3)
      .yearFraction(ACCRUAL_FACTOR_2)
      .rateObservation(FixedRateObservation.of(RATE_2))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_3_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_3)
      .endDate(CPN_DATE_4)
      .yearFraction(ACCRUAL_FACTOR_3)
      .rateObservation(FixedRateObservation.of(RATE_3))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_FX = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(WM_GBP_USD, GBP, FX_DATE_1))
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_NEG = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_NEG))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS_FX = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(WM_GBP_USD, GBP, FX_DATE_1))
      .build();

  // all tests use a fixed rate to avoid excessive use of mocks
  // rate observation is separated from this class, so nothing is missed in unit test terms
  // most testing on futureValue as methods only differ in discountFactor
  //-------------------------------------------------------------------------
  public void test_presentValue_single() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.discountFactor(USD, PAYMENT_DATE_1)).thenReturn(DISCOUNT_FACTOR);
    double pvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * DISCOUNT_FACTOR;
    double pvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.presentValue(env, PAYMENT_PERIOD_1);
    assertEquals(pvComputed, pvExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_single() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_1);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_fx() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.fxIndexRate(WM_GBP_USD, GBP, FX_DATE_1)).thenReturn(RATE_FX);
    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_FX);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_gearingSpread() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected = (RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_GS);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_gearingNoNegative() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_NEG);
    assertEquals(fvComputed, 0d, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_compoundNone() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100) +
            ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100) +
            ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_FULL_GS);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundNone_fx() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.fxIndexRate(WM_GBP_USD, GBP, FX_DATE_1)).thenReturn(RATE_FX);
    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX) +
            ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100 * RATE_FX) +
            ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100 * RATE_FX);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, PAYMENT_PERIOD_FULL_GS_FX);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_compoundStraight() {
    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.STRAIGHT).build();
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING + SPREAD);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING + SPREAD);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING + SPREAD);
    double fvExpected = NOTIONAL_100 * (invFactor1 * invFactor2 * invFactor3 - 1.0d);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, period);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundFlat() {
    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).build();
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double cpa1 = NOTIONAL_100 * ACCRUAL_FACTOR_1 * (RATE_1 * GEARING + SPREAD);
    double cpa2 = NOTIONAL_100 * ACCRUAL_FACTOR_2 * (RATE_2 * GEARING + SPREAD) +
        cpa1 * ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double cpa3 = NOTIONAL_100 * ACCRUAL_FACTOR_3 * (RATE_3 * GEARING + SPREAD) +
        (cpa1 + cpa2) * ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = cpa1 + cpa2 + cpa3;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, period);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundFlat_notional() {
    RatePaymentPeriod periodNot = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).build();
    RatePaymentPeriod period1 = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).notional(1.0d).build();
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvComputedNot = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, periodNot);
    double fvComputed1 = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, period1);
    assertEquals(fvComputedNot, fvComputed1 * NOTIONAL_100, TOLERANCE_PV);
  }

  public void test_futureValue_compoundSpreadExclusive() {
    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.SPREAD_EXCLUSIVE).build();
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = NOTIONAL_100 * (invFactor1 * invFactor2 * invFactor3 - 1.0d +
        (ACCRUAL_FACTOR_1 + ACCRUAL_FACTOR_2 + ACCRUAL_FACTOR_3) * SPREAD);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, period);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundSpreadExclusive_fx() {
    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS_FX.toBuilder()
        .compoundingMethod(CompoundingMethod.SPREAD_EXCLUSIVE).build();
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.fxIndexRate(WM_GBP_USD, GBP, FX_DATE_1)).thenReturn(RATE_FX);
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = NOTIONAL_100 * RATE_FX * (invFactor1 * invFactor2 * invFactor3 - 1.0d +
        (ACCRUAL_FACTOR_1 + ACCRUAL_FACTOR_2 + ACCRUAL_FACTOR_3) * SPREAD);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.futureValue(env, period);
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, CPN_DATE_1))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_2_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_2)
      .endDate(CPN_DATE_3)
      .yearFraction(ACCRUAL_FACTOR_2)
      .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, CPN_DATE_2))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_3_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_3)
      .endDate(CPN_DATE_4)
      .yearFraction(ACCRUAL_FACTOR_3)
      .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, CPN_DATE_3))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FLOATING = RatePaymentPeriod
      .builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_FLOATING, ACCRUAL_PERIOD_2_FLOATING, ACCRUAL_PERIOD_3_FLOATING))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();

  /**
  * Test present value sensitivity for ibor, no compounding.
  */
  public void test_presentValueSensitivity_ibor_noCompounding() {
    double paymentTime = 0.75;
    PricingEnvironment env = mock(PricingEnvironment.class);
    RateObservationFn<RateObservation> obsFunc = mock(RateObservationFn.class);

    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.relativeTime(PAYMENT_PERIOD_FLOATING.getPaymentDate())).thenReturn(paymentTime);
    when(env.discountFactor(PAYMENT_PERIOD_FLOATING.getCurrency(), PAYMENT_PERIOD_FLOATING.getPaymentDate()))
        .thenReturn(DISCOUNT_FACTOR);
    PointSensitivityBuilder builder = ZeroRateSensitivity.of(PAYMENT_PERIOD_FLOATING.getCurrency(),
        PAYMENT_PERIOD_FLOATING.getPaymentDate(), -DISCOUNT_FACTOR * paymentTime); // this is implemented in environment
    when(env.discountFactorZeroRateSensitivity(PAYMENT_PERIOD_FLOATING.getCurrency(),
        PAYMENT_PERIOD_FLOATING.getPaymentDate())).thenReturn(builder);

    DiscountingRatePaymentPeriodPricer pricer = new DiscountingRatePaymentPeriodPricer(obsFunc);
    LocalDate[] dates = new LocalDate[] {CPN_DATE_1, CPN_DATE_2, CPN_DATE_3, CPN_DATE_4 };
    double[] rates = new double[] {RATE_1, RATE_2, RATE_3 };
    for (int i = 0; i < 3; ++i) {
      IborRateObservation observation = (IborRateObservation) PAYMENT_PERIOD_FLOATING.getAccrualPeriods().get(i)
          .getRateObservation();
      IborRateSensitivity iborSense = IborRateSensitivity.of(GBP_LIBOR_3M, dates[i], 1.0d);
      when(obsFunc.rateSensitivity(env, observation, dates[i], dates[i + 1])).thenReturn(iborSense);
      when(obsFunc.rate(env, observation, dates[i], dates[i + 1])).thenReturn(rates[i]);
    }
    PointSensitivities senseComputed = pricer.presentValueSensitivity(env, PAYMENT_PERIOD_FLOATING).build();

    double eps = 1.e-7;
    List<IborRateSensitivity> senseExpectedList = futureFwdSensitivityFD(env, PAYMENT_PERIOD_FLOATING, obsFunc, eps);
    PointSensitivities senseExpected = PointSensitivities.of(senseExpectedList).multipliedBy(DISCOUNT_FACTOR);
    List<ZeroRateSensitivity> dscExpectedList = dscSensitivityFD(env, PAYMENT_PERIOD_FLOATING, obsFunc, eps);
    PointSensitivities senseExpectedDsc = PointSensitivities.of(dscExpectedList);

    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected.combinedWith(senseExpectedDsc),
        eps * PAYMENT_PERIOD_FLOATING.getNotional());
  }

  /**
   * test future value sensitivity for ibor, no compounding. 
   */
  public void test_futureValueSensitivity_ibor_noCompounding() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    RateObservationFn<RateObservation> obsFunc = mock(RateObservationFn.class);

    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    DiscountingRatePaymentPeriodPricer pricer = new DiscountingRatePaymentPeriodPricer(obsFunc);
    LocalDate[] dates = new LocalDate[] {CPN_DATE_1, CPN_DATE_2, CPN_DATE_3, CPN_DATE_4 };
    double[] rates = new double[] {RATE_1, RATE_2, RATE_3 };
    for (int i = 0; i < 3; ++i) {
      IborRateObservation observation = (IborRateObservation) PAYMENT_PERIOD_FLOATING.getAccrualPeriods().get(i)
          .getRateObservation();
      IborRateSensitivity iborSense = IborRateSensitivity.of(GBP_LIBOR_3M, dates[i], 1.0d);
      when(obsFunc.rateSensitivity(env, observation, dates[i], dates[i + 1])).thenReturn(iborSense);
      when(obsFunc.rate(env, observation, dates[i], dates[i + 1])).thenReturn(rates[i]);
    }
    PointSensitivities senseComputed = pricer.futureValueSensitivity(env, PAYMENT_PERIOD_FLOATING).build();

    double eps = 1.e-7;
    List<IborRateSensitivity> senseExpectedList = futureFwdSensitivityFD(env, PAYMENT_PERIOD_FLOATING, obsFunc,
        eps);
    PointSensitivities senseExpected = PointSensitivities.of(senseExpectedList);
    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected,
        eps * PAYMENT_PERIOD_FLOATING.getNotional());
  }

  @SuppressWarnings("null")
  private List<IborRateSensitivity> futureFwdSensitivityFD(PricingEnvironment env, RatePaymentPeriod payment,
      RateObservationFn<RateObservation> obsFunc, double eps) {
    LocalDate valuationDate = env.getValuationDate();
    PricingEnvironment envNew = mock(PricingEnvironment.class);
    when(envNew.getValuationDate()).thenReturn(valuationDate);

    ImmutableList<RateAccrualPeriod> periods = payment.getAccrualPeriods();
    int nPeriods = periods.size();
    List<IborRateSensitivity> forwardRateSensi = new ArrayList<>();
    for (int j = 0; j < nPeriods; ++j) {
      RateObservationFn<RateObservation> obsFuncUp = mock(RateObservationFn.class);
      RateObservationFn<RateObservation> obsFuncDown = mock(RateObservationFn.class);
      IborIndex index = null;
      LocalDate fixingDate = null;
      for (int i = 0; i < nPeriods; ++i) {
        RateAccrualPeriod period = periods.get(i);
        IborRateObservation observation = (IborRateObservation) period.getRateObservation();
        double rate = obsFunc.rate(env, observation, period.getStartDate(), period.getEndDate());
        if (i == j) {
          fixingDate = observation.getFixingDate();
          index = observation.getIndex();
          when(obsFuncUp.rate(envNew, observation, period.getStartDate(), period.getEndDate())).thenReturn(rate + eps);
          when(obsFuncDown.rate(envNew, observation, period.getStartDate(), period.getEndDate()))
              .thenReturn(rate - eps);
        } else {
          when(obsFuncUp.rate(envNew, observation, period.getStartDate(), period.getEndDate())).thenReturn(rate);
          when(obsFuncDown.rate(envNew, observation, period.getStartDate(), period.getEndDate())).thenReturn(rate);
        }
      }
      DiscountingRatePaymentPeriodPricer pricerUp = new DiscountingRatePaymentPeriodPricer(obsFuncUp);
      DiscountingRatePaymentPeriodPricer pricerDown = new DiscountingRatePaymentPeriodPricer(obsFuncDown);
      double up = pricerUp.futureValue(envNew, payment);
      double down = pricerDown.futureValue(envNew, payment);
      IborRateSensitivity fwdSense = IborRateSensitivity.of(index, fixingDate, 0.5 * (up - down) / eps);
      forwardRateSensi.add(fwdSense);
    }
    return forwardRateSensi;
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(PricingEnvironment env, RatePaymentPeriod payment,
      RateObservationFn<RateObservation> obsFunc, double eps) {
    LocalDate valuationDate = env.getValuationDate();
    LocalDate paymentDate = payment.getPaymentDate();
    double discountFactor = env.discountFactor(payment.getCurrency(), paymentDate);
    double paymentTime = env.relativeTime(paymentDate);
    Currency currency = payment.getCurrency();

    PricingEnvironment envUp = mock(PricingEnvironment.class);
    PricingEnvironment envDw = mock(PricingEnvironment.class);
    RateObservationFn<RateObservation> obsFuncNewUp = mock(RateObservationFn.class);
    RateObservationFn<RateObservation> obsFuncNewDw = mock(RateObservationFn.class);
    when(envUp.getValuationDate()).thenReturn(valuationDate);
    when(envDw.getValuationDate()).thenReturn(valuationDate);
    when(envUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(envDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));

    ImmutableList<RateAccrualPeriod> periods = payment.getAccrualPeriods();
    for (int i = 0; i < periods.size(); ++i) {
      RateObservation observation = periods.get(i).getRateObservation();
      LocalDate startDate = periods.get(i).getStartDate();
      LocalDate endDate = periods.get(i).getEndDate();
      double rate = obsFunc.rate(env, observation, startDate, endDate);
      when(obsFuncNewUp.rate(envUp, observation, startDate, endDate)).thenReturn(rate);
      when(obsFuncNewDw.rate(envDw, observation, startDate, endDate)).thenReturn(rate);
    }

    DiscountingRatePaymentPeriodPricer pricerUp = new DiscountingRatePaymentPeriodPricer(obsFuncNewUp);
    DiscountingRatePaymentPeriodPricer pricerDw = new DiscountingRatePaymentPeriodPricer(obsFuncNewDw);
    double pvUp = pricerUp.presentValue(envUp, payment);
    double pvDw = pricerDw.presentValue(envDw, payment);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentDate, res));
    return zeroRateSensi;
  }
}

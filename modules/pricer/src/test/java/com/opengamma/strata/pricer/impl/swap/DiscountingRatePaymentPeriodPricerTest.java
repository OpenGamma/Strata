/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.pricer.datasets.RatesProviderDataSets.FX_MATRIX_GBP_USD;
import static com.opengamma.strata.pricer.datasets.RatesProviderDataSets.MULTI_GBP_USD;
import static com.opengamma.strata.pricer.datasets.RatesProviderDataSets.MULTI_GBP_USD_SIMPLE;
import static com.opengamma.strata.pricer.datasets.RatesProviderDataSets.VAL_DATE_2014_01_22;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.NegativeRateMethod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link DiscountingRatePaymentPeriodPricer}
 */
public class DiscountingRatePaymentPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = VAL_DATE_2014_01_22;
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
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

  private static final double EPS_FD = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  private static final double FX_RATE = FX_MATRIX_GBP_USD.fxRate(GBP, USD);
  private static final FxMatrix FX_MATRIX_BUMP = FxMatrix.of(GBP, USD, FX_MATRIX_GBP_USD.fxRate(GBP, USD) + EPS_FD);

  private static final RateAccrualPeriod ACCRUAL_PERIOD_1 = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateComputation(FixedRateComputation.of(RATE_1))
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateComputation(FixedRateComputation.of(RATE_1))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_NEG = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateComputation(FixedRateComputation.of(RATE_1))
      .gearing(-1d)
      .negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_2_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_2)
      .endDate(CPN_DATE_3)
      .yearFraction(ACCRUAL_FACTOR_2)
      .rateComputation(FixedRateComputation.of(RATE_2))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_3_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_3)
      .endDate(CPN_DATE_4)
      .yearFraction(ACCRUAL_FACTOR_3)
      .rateComputation(FixedRateComputation.of(RATE_3))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_FX = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(FxIndexObservation.of(GBP_USD_WM, FX_DATE_1, REF_DATA), GBP))
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_NEG = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_NEG))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS_FX_USD = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(FxIndexObservation.of(GBP_USD_WM, FX_DATE_1, REF_DATA), GBP))
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS_FX_GBP = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(FxIndexObservation.of(GBP_USD_WM, FX_DATE_1, REF_DATA), USD))
      .build();

  // all tests use a fixed rate to avoid excessive use of mocks
  // rate observation is separated from this class, so nothing is missed in unit test terms
  // most testing on forecastValue as methods only differ in discountFactor
  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_single() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double pvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * DISCOUNT_FACTOR;
    double pvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.presentValue(PAYMENT_PERIOD_1, prov);
    assertThat(pvComputed).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue_single() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_1, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_single_fx() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_1_FX, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_single_gearingSpread() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected = (RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_1_GS, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_single_gearingNoNegative() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_1_NEG, prov);
    assertThat(fvComputed).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue_compoundNone() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100) +
            ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100) +
            ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_FULL_GS, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_compoundNone_fx() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX) +
            ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100 * RATE_FX) +
            ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100 * RATE_FX);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(PAYMENT_PERIOD_FULL_GS_FX_USD, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue_compoundStraight() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.STRAIGHT).build();
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING + SPREAD);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING + SPREAD);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING + SPREAD);
    double fvExpected = NOTIONAL_100 * (invFactor1 * invFactor2 * invFactor3 - 1.0d);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(period, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_compoundFlat() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).build();
    double cpa1 = NOTIONAL_100 * ACCRUAL_FACTOR_1 * (RATE_1 * GEARING + SPREAD);
    double cpa2 = NOTIONAL_100 * ACCRUAL_FACTOR_2 * (RATE_2 * GEARING + SPREAD) +
        cpa1 * ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double cpa3 = NOTIONAL_100 * ACCRUAL_FACTOR_3 * (RATE_3 * GEARING + SPREAD) +
        (cpa1 + cpa2) * ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = cpa1 + cpa2 + cpa3;
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(period, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_compoundFlat_notional() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    RatePaymentPeriod periodNot = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).build();
    RatePaymentPeriod period1 = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.FLAT).notional(1.0d).build();
    double fvComputedNot = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(periodNot, prov);
    double fvComputed1 = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(period1, prov);
    assertThat(fvComputedNot).isCloseTo(fvComputed1 * NOTIONAL_100, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_compoundSpreadExclusive() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder()
        .compoundingMethod(CompoundingMethod.SPREAD_EXCLUSIVE).build();
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = NOTIONAL_100 * (invFactor1 * invFactor2 * invFactor3 - 1.0d +
        (ACCRUAL_FACTOR_1 + ACCRUAL_FACTOR_2 + ACCRUAL_FACTOR_3) * SPREAD);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(period, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_compoundSpreadExclusive_fx() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS_FX_USD.toBuilder()
        .compoundingMethod(CompoundingMethod.SPREAD_EXCLUSIVE).build();
    double invFactor1 = 1.0d + ACCRUAL_FACTOR_1 * (RATE_1 * GEARING);
    double invFactor2 = 1.0d + ACCRUAL_FACTOR_2 * (RATE_2 * GEARING);
    double invFactor3 = 1.0d + ACCRUAL_FACTOR_3 * (RATE_3 * GEARING);
    double fvExpected = NOTIONAL_100 * RATE_FX * (invFactor1 * invFactor2 * invFactor3 - 1.0d +
        (ACCRUAL_FACTOR_1 + ACCRUAL_FACTOR_2 + ACCRUAL_FACTOR_3) * SPREAD);
    double fvComputed = DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(period, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateComputation(IborRateComputation.of(GBP_LIBOR_3M, CPN_DATE_1, REF_DATA))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_2_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_2)
      .endDate(CPN_DATE_3)
      .yearFraction(ACCRUAL_FACTOR_2)
      .rateComputation(IborRateComputation.of(GBP_LIBOR_3M, CPN_DATE_2, REF_DATA))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_3_FLOATING = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_3)
      .endDate(CPN_DATE_4)
      .yearFraction(ACCRUAL_FACTOR_3)
      .rateComputation(IborRateComputation.of(GBP_LIBOR_3M, CPN_DATE_3, REF_DATA))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FLOATING = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_FLOATING, ACCRUAL_PERIOD_2_FLOATING, ACCRUAL_PERIOD_3_FLOATING))
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_COMPOUNDING_STRAIGHT = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_FLOATING, ACCRUAL_PERIOD_2_FLOATING, ACCRUAL_PERIOD_3_FLOATING))
      .compoundingMethod(CompoundingMethod.STRAIGHT)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_COMPOUNDING_FLAT = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_FLOATING, ACCRUAL_PERIOD_2_FLOATING, ACCRUAL_PERIOD_3_FLOATING))
      .compoundingMethod(CompoundingMethod.FLAT)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_COMPOUNDING_EXCLUSIVE = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_FLOATING, ACCRUAL_PERIOD_2_FLOATING, ACCRUAL_PERIOD_3_FLOATING))
      .compoundingMethod(CompoundingMethod.SPREAD_EXCLUSIVE)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();

  /**
  * Test present value sensitivity for ibor, no compounding.
  */
  @Test
  public void test_presentValueSensitivity_ibor_noCompounding() {
    LocalDate valDate = PAYMENT_PERIOD_FLOATING.getPaymentDate().minusDays(90);
    double paymentTime = DAY_COUNT.relativeYearFraction(valDate, PAYMENT_PERIOD_FLOATING.getPaymentDate());
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(valDate, mockDf);
    simpleProv.setDayCount(DAY_COUNT);
    RateComputationFn<RateComputation> obsFunc = mock(RateComputationFn.class);

    when(mockDf.discountFactor(PAYMENT_PERIOD_FLOATING.getPaymentDate()))
        .thenReturn(DISCOUNT_FACTOR);
    ZeroRateSensitivity builder = ZeroRateSensitivity.of(
        PAYMENT_PERIOD_FLOATING.getCurrency(), paymentTime, -DISCOUNT_FACTOR * paymentTime);
    when(mockDf.zeroRatePointSensitivity(PAYMENT_PERIOD_FLOATING.getPaymentDate())).thenReturn(builder);

    DiscountingRatePaymentPeriodPricer pricer = new DiscountingRatePaymentPeriodPricer(obsFunc);
    LocalDate[] dates = new LocalDate[] {CPN_DATE_1, CPN_DATE_2, CPN_DATE_3, CPN_DATE_4};
    double[] rates = new double[] {RATE_1, RATE_2, RATE_3};
    for (int i = 0; i < 3; ++i) {
      IborRateComputation rateComputation =
          (IborRateComputation) PAYMENT_PERIOD_FLOATING.getAccrualPeriods().get(i).getRateComputation();
      IborRateSensitivity iborSense = IborRateSensitivity.of(rateComputation.getObservation(), 1d);
      when(obsFunc.rateSensitivity(rateComputation, dates[i], dates[i + 1], simpleProv)).thenReturn(iborSense);
      when(obsFunc.rate(rateComputation, dates[i], dates[i + 1], simpleProv)).thenReturn(rates[i]);
    }
    PointSensitivities senseComputed = pricer.presentValueSensitivity(PAYMENT_PERIOD_FLOATING, simpleProv).build();

    double eps = 1.e-7;
    List<IborRateSensitivity> senseExpectedList = futureFwdSensitivityFD(simpleProv, PAYMENT_PERIOD_FLOATING, obsFunc, eps);
    PointSensitivities senseExpected = PointSensitivities.of(senseExpectedList).multipliedBy(DISCOUNT_FACTOR);
    List<ZeroRateSensitivity> dscExpectedList = dscSensitivityFD(simpleProv, PAYMENT_PERIOD_FLOATING, obsFunc, eps);
    PointSensitivities senseExpectedDsc = PointSensitivities.of(dscExpectedList);

    assertThat(senseComputed.equalWithTolerance(
        senseExpected.combinedWith(senseExpectedDsc), eps * PAYMENT_PERIOD_FLOATING.getNotional())).isTrue();
  }

  /**
   * test forecast value sensitivity for ibor, no compounding.
   */
  @Test
  public void test_forecastValueSensitivity_ibor_noCompounding() {
    RatesProvider mockProv = mock(RatesProvider.class);
    RateComputationFn<RateComputation> obsFunc = mock(RateComputationFn.class);

    when(mockProv.getValuationDate()).thenReturn(VAL_DATE);
    DiscountingRatePaymentPeriodPricer pricer = new DiscountingRatePaymentPeriodPricer(obsFunc);
    LocalDate[] dates = new LocalDate[] {CPN_DATE_1, CPN_DATE_2, CPN_DATE_3, CPN_DATE_4};
    double[] rates = new double[] {RATE_1, RATE_2, RATE_3};
    for (int i = 0; i < 3; ++i) {
      IborRateComputation rateObs =
          (IborRateComputation) PAYMENT_PERIOD_FLOATING.getAccrualPeriods().get(i).getRateComputation();
      IborRateSensitivity iborSense = IborRateSensitivity.of(rateObs.getObservation(), 1.0d);
      when(obsFunc.rateSensitivity(rateObs, dates[i], dates[i + 1], mockProv)).thenReturn(iborSense);
      when(obsFunc.rate(rateObs, dates[i], dates[i + 1], mockProv)).thenReturn(rates[i]);
    }
    PointSensitivities senseComputed = pricer.forecastValueSensitivity(PAYMENT_PERIOD_FLOATING, mockProv).build();

    double eps = 1.e-7;
    List<IborRateSensitivity> senseExpectedList = futureFwdSensitivityFD(mockProv, PAYMENT_PERIOD_FLOATING, obsFunc,
        eps);
    PointSensitivities senseExpected = PointSensitivities.of(senseExpectedList);
    assertThat(senseComputed.equalWithTolerance(senseExpected, eps * PAYMENT_PERIOD_FLOATING.getNotional())).isTrue();
  }

  // test forecast value sensitivity for ibor, with straight, flat and exclusive compounding.
  public static Object[][] data_forecastValueSensitivity_ibor_compounding() {
    return new Object[][] {
        {PAYMENT_PERIOD_COMPOUNDING_STRAIGHT},
        {PAYMENT_PERIOD_COMPOUNDING_FLAT},
        {PAYMENT_PERIOD_COMPOUNDING_EXCLUSIVE},
    };
  }

  @ParameterizedTest
  @MethodSource("data_forecastValueSensitivity_ibor_compounding")
  public void test_forecastValueSensitivity_ibor_compounding(RatePaymentPeriod period) {
    RatesProvider mockProv = mock(RatesProvider.class);
    RateComputationFn<RateComputation> obsFunc = mock(RateComputationFn.class);
    when(mockProv.getValuationDate()).thenReturn(VAL_DATE);
    DiscountingRatePaymentPeriodPricer pricer = new DiscountingRatePaymentPeriodPricer(obsFunc);
    LocalDate[] dates = new LocalDate[] {CPN_DATE_1, CPN_DATE_2, CPN_DATE_3, CPN_DATE_4};
    double[] rates = new double[] {RATE_1, RATE_2, RATE_3};
    for (int i = 0; i < 3; ++i) {
      IborRateComputation rateObs =
          (IborRateComputation) period.getAccrualPeriods().get(i).getRateComputation();
      IborRateSensitivity iborSense = IborRateSensitivity.of(rateObs.getObservation(), 1.0d);
      when(obsFunc.rateSensitivity(rateObs, dates[i], dates[i + 1], mockProv)).thenReturn(iborSense);
      when(obsFunc.rate(rateObs, dates[i], dates[i + 1], mockProv)).thenReturn(rates[i]);
    }
    PointSensitivities senseComputed = pricer.forecastValueSensitivity(period, mockProv).build();
    List<IborRateSensitivity> senseExpectedList = futureFwdSensitivityFD(mockProv, period, obsFunc, EPS_FD);
    PointSensitivities senseExpected = PointSensitivities.of(senseExpectedList);
    assertThat(senseComputed.equalWithTolerance(senseExpected, EPS_FD * period.getNotional())).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValueSensitivity_compoundNone_fx() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD;
    PointSensitivityBuilder pointSensiComputedUSD =
        pricer.forecastValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    CurrencyParameterSensitivities sensiComputedUSD =
        provider.parameterSensitivity(pointSensiComputedUSD.build().normalized());
    CurrencyParameterSensitivities sensiExpectedUSD = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(USD, pricer.forecastValue(PAYMENT_PERIOD_FULL_GS_FX_USD, (p))));
    assertThat(sensiComputedUSD.equalWithTolerance(
        sensiExpectedUSD, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_USD.getNotional())).isTrue();

    PointSensitivityBuilder pointSensiComputedGBP =
        pricer.forecastValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    CurrencyParameterSensitivities sensiComputedGBP =
        provider.parameterSensitivity(pointSensiComputedGBP.build().normalized());
    CurrencyParameterSensitivities sensiExpectedGBP = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(GBP, pricer.forecastValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, (p))));
    assertThat(sensiComputedGBP.equalWithTolerance(
        sensiExpectedGBP, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_GBP.getNotional())).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_compoundNone_fx() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD;
    PointSensitivityBuilder pointSensiComputedUSD =
        pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    CurrencyParameterSensitivities sensiComputedUSD =
        provider.parameterSensitivity(pointSensiComputedUSD.build().normalized());
    CurrencyParameterSensitivities sensiExpectedUSD = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(USD, pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, (p))));
    assertThat(sensiComputedUSD.equalWithTolerance(
        sensiExpectedUSD, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_USD.getNotional())).isTrue();

    PointSensitivityBuilder pointSensiComputedGBP =
        pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    CurrencyParameterSensitivities sensiComputedGBP =
        provider.parameterSensitivity(pointSensiComputedGBP.build().normalized());
    CurrencyParameterSensitivities sensiExpectedGBP = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(GBP, pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, (p))));
    assertThat(sensiComputedGBP.equalWithTolerance(
        sensiExpectedGBP, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_GBP.getNotional())).isTrue();
  }

  @Test
  public void test_forecastValueSensitivity_compoundNone_fx_dfCurve() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD_SIMPLE;
    PointSensitivityBuilder pointSensiComputedUSD =
        pricer.forecastValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    CurrencyParameterSensitivities sensiComputedUSD =
        provider.parameterSensitivity(pointSensiComputedUSD.build().normalized());
    CurrencyParameterSensitivities sensiExpectedUSD = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(USD, pricer.forecastValue(PAYMENT_PERIOD_FULL_GS_FX_USD, (p))));
    assertThat(sensiComputedUSD.equalWithTolerance(
        sensiExpectedUSD, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_USD.getNotional())).isTrue();

    PointSensitivityBuilder pointSensiComputedGBP =
        pricer.forecastValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    CurrencyParameterSensitivities sensiComputedGBP =
        provider.parameterSensitivity(pointSensiComputedGBP.build().normalized());
    CurrencyParameterSensitivities sensiExpectedGBP = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(GBP, pricer.forecastValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, (p))));
    assertThat(sensiComputedGBP.equalWithTolerance(
        sensiExpectedGBP, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_GBP.getNotional())).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_compoundNone_fx_dfCurve() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD_SIMPLE;
    PointSensitivityBuilder pointSensiComputedUSD =
        pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    CurrencyParameterSensitivities sensiComputedUSD =
        provider.parameterSensitivity(pointSensiComputedUSD.build().normalized());
    CurrencyParameterSensitivities sensiExpectedUSD = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(USD, pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, (p))));
    assertThat(sensiComputedUSD.equalWithTolerance(
        sensiExpectedUSD, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_USD.getNotional())).isTrue();

    PointSensitivityBuilder pointSensiComputedGBP =
        pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    CurrencyParameterSensitivities sensiComputedGBP =
        provider.parameterSensitivity(pointSensiComputedGBP.build().normalized());
    CurrencyParameterSensitivities sensiExpectedGBP = CAL_FD.sensitivity(
        provider, (p) -> CurrencyAmount.of(GBP, pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, (p))));
    assertThat(sensiComputedGBP.equalWithTolerance(
        sensiExpectedGBP, EPS_FD * PAYMENT_PERIOD_FULL_GS_FX_GBP.getNotional())).isTrue();
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("null")
  private List<IborRateSensitivity> futureFwdSensitivityFD(RatesProvider provider, RatePaymentPeriod payment,
      RateComputationFn<RateComputation> obsFunc, double eps) {
    LocalDate valuationDate = provider.getValuationDate();
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(valuationDate);

    ImmutableList<RateAccrualPeriod> periods = payment.getAccrualPeriods();
    int nPeriods = periods.size();
    List<IborRateSensitivity> forwardRateSensi = new ArrayList<>();
    for (int j = 0; j < nPeriods; ++j) {
      RateComputationFn<RateComputation> obsFuncUp = mock(RateComputationFn.class);
      RateComputationFn<RateComputation> obsFuncDown = mock(RateComputationFn.class);
      IborIndex index = null;
      LocalDate fixingDate = null;
      for (int i = 0; i < nPeriods; ++i) {
        RateAccrualPeriod period = periods.get(i);
        IborRateComputation observation = (IborRateComputation) period.getRateComputation();
        double rate = obsFunc.rate(observation, period.getStartDate(), period.getEndDate(), provider);
        if (i == j) {
          fixingDate = observation.getFixingDate();
          index = observation.getIndex();
          when(obsFuncUp.rate(observation, period.getStartDate(), period.getEndDate(), provNew)).thenReturn(rate + eps);
          when(obsFuncDown.rate(observation, period.getStartDate(), period.getEndDate(), provNew))
              .thenReturn(rate - eps);
        } else {
          when(obsFuncUp.rate(observation, period.getStartDate(), period.getEndDate(), provNew)).thenReturn(rate);
          when(obsFuncDown.rate(observation, period.getStartDate(), period.getEndDate(), provNew)).thenReturn(rate);
        }
      }
      DiscountingRatePaymentPeriodPricer pricerUp = new DiscountingRatePaymentPeriodPricer(obsFuncUp);
      DiscountingRatePaymentPeriodPricer pricerDown = new DiscountingRatePaymentPeriodPricer(obsFuncDown);
      double up = pricerUp.forecastValue(payment, provNew);
      double down = pricerDown.forecastValue(payment, provNew);
      IborRateSensitivity fwdSense =
          IborRateSensitivity.of(IborIndexObservation.of(index, fixingDate, REF_DATA), 0.5 * (up - down) / eps);
      forwardRateSensi.add(fwdSense);
    }
    return forwardRateSensi;
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(RatesProvider provider, RatePaymentPeriod payment,
      RateComputationFn<RateComputation> obsFunc, double eps) {
    LocalDate valuationDate = provider.getValuationDate();
    LocalDate paymentDate = payment.getPaymentDate();
    double discountFactor = provider.discountFactor(payment.getCurrency(), paymentDate);
    double paymentTime = DAY_COUNT.relativeYearFraction(valuationDate, paymentDate);
    Currency currency = payment.getCurrency();

    RatesProvider provUp = mock(RatesProvider.class);
    RatesProvider provDw = mock(RatesProvider.class);
    RateComputationFn<RateComputation> obsFuncNewUp = mock(RateComputationFn.class);
    RateComputationFn<RateComputation> obsFuncNewDw = mock(RateComputationFn.class);
    when(provUp.getValuationDate()).thenReturn(valuationDate);
    when(provDw.getValuationDate()).thenReturn(valuationDate);
    when(provUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(provDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));

    ImmutableList<RateAccrualPeriod> periods = payment.getAccrualPeriods();
    for (int i = 0; i < periods.size(); ++i) {
      RateComputation observation = periods.get(i).getRateComputation();
      LocalDate startDate = periods.get(i).getStartDate();
      LocalDate endDate = periods.get(i).getEndDate();
      double rate = obsFunc.rate(observation, startDate, endDate, provider);
      when(obsFuncNewUp.rate(observation, startDate, endDate, provUp)).thenReturn(rate);
      when(obsFuncNewDw.rate(observation, startDate, endDate, provDw)).thenReturn(rate);
    }

    DiscountingRatePaymentPeriodPricer pricerUp = new DiscountingRatePaymentPeriodPricer(obsFuncNewUp);
    DiscountingRatePaymentPeriodPricer pricerDw = new DiscountingRatePaymentPeriodPricer(obsFuncNewDw);
    double pvUp = pricerUp.presentValue(payment, provUp);
    double pvDw = pricerDw.presentValue(payment, provDw);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentTime, res));
    return zeroRateSensi;
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_accruedInterest_firstAccrualPeriod() {
    LocalDate valDate = PAYMENT_PERIOD_FULL_GS.getStartDate().plusDays(7);
    SimpleRatesProvider prov = createProvider(valDate);

    double partial = PAYMENT_PERIOD_FULL_GS.getDayCount().yearFraction(ACCRUAL_PERIOD_1_GS.getStartDate(), valDate);
    double fraction = partial / ACCRUAL_FACTOR_1;
    double expected = ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100) * fraction;

    double computed = DiscountingRatePaymentPeriodPricer.DEFAULT.accruedInterest(PAYMENT_PERIOD_FULL_GS, prov);
    assertThat(computed).isCloseTo(expected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_accruedInterest_lastAccrualPeriod() {
    LocalDate valDate = PAYMENT_PERIOD_FULL_GS.getEndDate().minusDays(7);
    SimpleRatesProvider prov = createProvider(valDate);

    double partial = PAYMENT_PERIOD_FULL_GS.getDayCount().yearFraction(ACCRUAL_PERIOD_3_GS.getStartDate(), valDate);
    double fraction = partial / ACCRUAL_FACTOR_3;
    double expected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100) +
            ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100) +
            ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100 * fraction);

    double computed = DiscountingRatePaymentPeriodPricer.DEFAULT.accruedInterest(PAYMENT_PERIOD_FULL_GS, prov);
    assertThat(computed).isCloseTo(expected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_accruedInterest_valDateBeforePeriod() {
    SimpleRatesProvider prov = createProvider(PAYMENT_PERIOD_FULL_GS.getStartDate());

    double computed = DiscountingRatePaymentPeriodPricer.DEFAULT.accruedInterest(PAYMENT_PERIOD_FULL_GS, prov);
    assertThat(computed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  @Test
  public void test_accruedInterest_valDateAfterPeriod() {
    SimpleRatesProvider prov = createProvider(PAYMENT_PERIOD_FULL_GS.getEndDate().plusDays(1));

    double computed = DiscountingRatePaymentPeriodPricer.DEFAULT.accruedInterest(PAYMENT_PERIOD_FULL_GS, prov);
    assertThat(computed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue_single() {
    RatesProvider prov = createProvider(VAL_DATE);

    DiscountingRatePaymentPeriodPricer test = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(PAYMENT_PERIOD_1, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = PAYMENT_PERIOD_1.getCurrency();
    double ua = RATE_1 * ACCRUAL_FACTOR_1;
    double fv = ua * NOTIONAL_100;
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("RatePaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD_1.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.COMPOUNDING).get()).isEqualTo(PAYMENT_PERIOD_1.getCompoundingMethod());
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fv, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(fv * DISCOUNT_FACTOR, offset(TOLERANCE_PV));

    assertThat(explain.get(ExplainKey.ACCRUAL_PERIODS).get()).hasSize(1);
    ExplainMap explainAccrual = explain.get(ExplainKey.ACCRUAL_PERIODS).get().get(0);
    RateAccrualPeriod ap = PAYMENT_PERIOD_1.getAccrualPeriods().get(0);
    int daysBetween = (int) DAYS.between(ap.getStartDate(), ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("AccrualPeriod");
    assertThat(explainAccrual.get(ExplainKey.START_DATE).get()).isEqualTo(ap.getStartDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(ap.getUnadjustedStartDate());
    assertThat(explainAccrual.get(ExplainKey.END_DATE).get()).isEqualTo(ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(ap.getUnadjustedEndDate());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get()).isEqualTo(ap.getYearFraction());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_DAYS).get()).isEqualTo((Integer) daysBetween);
    assertThat(explainAccrual.get(ExplainKey.GEARING).get()).isCloseTo(ap.getGearing(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.SPREAD).get()).isCloseTo(ap.getSpread(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.FIXED_RATE).get()).isCloseTo(RATE_1, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.PAY_OFF_RATE).get()).isCloseTo(RATE_1, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.UNIT_AMOUNT).get()).isCloseTo(ua, offset(TOLERANCE_PV));
  }

  @Test
  public void test_explainPresentValue_single_paymentDateInPast() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);
    prov.setValuationDate(VAL_DATE.plusYears(1));

    DiscountingRatePaymentPeriodPricer test = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(PAYMENT_PERIOD_1, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = PAYMENT_PERIOD_1.getCurrency();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("RatePaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD_1.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.COMPLETED).get()).isEqualTo(Boolean.TRUE);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  public void test_explainPresentValue_single_fx() {
    RatesProvider prov = createProvider(VAL_DATE);

    DiscountingRatePaymentPeriodPricer test = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(PAYMENT_PERIOD_1_FX, prov, builder);
    ExplainMap explain = builder.build();

    FxReset fxReset = PAYMENT_PERIOD_1_FX.getFxReset().get();
    Currency currency = PAYMENT_PERIOD_1_FX.getCurrency();
    Currency referenceCurrency = fxReset.getReferenceCurrency();
    double ua = RATE_1 * ACCRUAL_FACTOR_1;
    double fv = ua * NOTIONAL_100 * RATE_FX;
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("RatePaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD_1_FX.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100 * RATE_FX, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(referenceCurrency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.COMPOUNDING).get()).isEqualTo(PAYMENT_PERIOD_1_FX.getCompoundingMethod());
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fv, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(fv * DISCOUNT_FACTOR, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.OBSERVATIONS).get()).hasSize(1);
    ExplainMap explainFxObs = explain.get(ExplainKey.OBSERVATIONS).get().get(0);
    assertThat(explainFxObs.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FxObservation");
    assertThat(explainFxObs.get(ExplainKey.INDEX).get()).isEqualTo(fxReset.getIndex());
    assertThat(explainFxObs.get(ExplainKey.FIXING_DATE).get()).isEqualTo(fxReset.getObservation().getFixingDate());
    assertThat(explainFxObs.get(ExplainKey.INDEX_VALUE).get()).isCloseTo(RATE_FX, offset(TOLERANCE_PV));

    assertThat(explain.get(ExplainKey.ACCRUAL_PERIODS).get()).hasSize(1);
    ExplainMap explainAccrual = explain.get(ExplainKey.ACCRUAL_PERIODS).get().get(0);
    RateAccrualPeriod ap = PAYMENT_PERIOD_1_FX.getAccrualPeriods().get(0);
    int daysBetween = (int) DAYS.between(ap.getStartDate(), ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("AccrualPeriod");
    assertThat(explainAccrual.get(ExplainKey.START_DATE).get()).isEqualTo(ap.getStartDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(ap.getUnadjustedStartDate());
    assertThat(explainAccrual.get(ExplainKey.END_DATE).get()).isEqualTo(ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(ap.getUnadjustedEndDate());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get()).isEqualTo(ap.getYearFraction());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_DAYS).get()).isEqualTo((Integer) daysBetween);
    assertThat(explainAccrual.get(ExplainKey.GEARING).get()).isCloseTo(ap.getGearing(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.SPREAD).get()).isCloseTo(ap.getSpread(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.FIXED_RATE).get()).isCloseTo(RATE_1, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.PAY_OFF_RATE).get()).isCloseTo(RATE_1, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.UNIT_AMOUNT).get()).isCloseTo(ua, offset(TOLERANCE_PV));
  }

  @Test
  public void test_explainPresentValue_single_gearingSpread() {
    RatesProvider prov = createProvider(VAL_DATE);

    DiscountingRatePaymentPeriodPricer test = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(PAYMENT_PERIOD_1_GS, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = PAYMENT_PERIOD_1_GS.getCurrency();
    double payOffRate = RATE_1 * GEARING + SPREAD;
    double ua = payOffRate * ACCRUAL_FACTOR_1;
    double fv = ua * NOTIONAL_100;
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("RatePaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD_1_GS.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(NOTIONAL_100, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.COMPOUNDING).get()).isEqualTo(PAYMENT_PERIOD_1_GS.getCompoundingMethod());
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fv, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(fv * DISCOUNT_FACTOR, offset(TOLERANCE_PV));

    assertThat(explain.get(ExplainKey.ACCRUAL_PERIODS).get()).hasSize(1);
    ExplainMap explainAccrual = explain.get(ExplainKey.ACCRUAL_PERIODS).get().get(0);
    RateAccrualPeriod ap = PAYMENT_PERIOD_1_GS.getAccrualPeriods().get(0);
    int daysBetween = (int) DAYS.between(ap.getStartDate(), ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("AccrualPeriod");
    assertThat(explainAccrual.get(ExplainKey.START_DATE).get()).isEqualTo(ap.getStartDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(ap.getUnadjustedStartDate());
    assertThat(explainAccrual.get(ExplainKey.END_DATE).get()).isEqualTo(ap.getEndDate());
    assertThat(explainAccrual.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(ap.getUnadjustedEndDate());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get()).isEqualTo(ap.getYearFraction());
    assertThat(explainAccrual.get(ExplainKey.ACCRUAL_DAYS).get()).isEqualTo((Integer) daysBetween);
    assertThat(explainAccrual.get(ExplainKey.GEARING).get()).isCloseTo(ap.getGearing(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.SPREAD).get()).isCloseTo(ap.getSpread(), offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.FIXED_RATE).get()).isCloseTo(RATE_1, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.PAY_OFF_RATE).get()).isCloseTo(payOffRate, offset(TOLERANCE_PV));
    assertThat(explainAccrual.get(ExplainKey.UNIT_AMOUNT).get()).isCloseTo(ua, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure_fx() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    LocalDate valuationDate = VAL_DATE.minusWeeks(1);
    ImmutableRatesProvider provider = RatesProviderDataSets.multiGbpUsd(valuationDate);
    // USD
    MultiCurrencyAmount computedUSD = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    PointSensitivities pointUSD = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider).build();
    MultiCurrencyAmount expectedUSD = provider.currencyExposure(pointUSD.convertedTo(GBP, provider)).plus(CurrencyAmount.of(
        PAYMENT_PERIOD_FULL_GS_FX_USD.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)));
    assertThat(computedUSD.getAmount(GBP).getAmount()).isCloseTo(expectedUSD.getAmount(GBP).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedUSD.contains(USD)).isFalse(); // 0 USD
    // GBP
    MultiCurrencyAmount computedGBP = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    PointSensitivities pointGBP = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider).build();
    MultiCurrencyAmount expectedGBP = provider.currencyExposure(pointGBP.convertedTo(USD, provider)).plus(CurrencyAmount.of(
        PAYMENT_PERIOD_FULL_GS_FX_GBP.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)));
    assertThat(computedGBP.getAmount(USD).getAmount()).isCloseTo(expectedGBP.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedGBP.contains(GBP)).isFalse(); // 0 GBP
    // FD approximation
    ImmutableRatesProvider provUp = RatesProviderDataSets.multiGbpUsd(valuationDate).toBuilder()
        .fxRateProvider(FX_MATRIX_BUMP)
        .build();
    double expectedFdUSD = (pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)) / EPS_FD;
    assertThat(computedUSD.getAmount(GBP).getAmount()).isCloseTo(expectedFdUSD, offset(EPS_FD * NOTIONAL_100));
    double expectedFdGBP = -(pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(computedGBP.getAmount(USD).getAmount()).isCloseTo(expectedFdGBP, offset(EPS_FD * NOTIONAL_100));
  }

  @Test
  public void test_currencyExposure_fx_betweenFixingAndPayment() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    LocalDate valuationDate = VAL_DATE.plusWeeks(1);
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(LocalDate.of(2014, 1, 22), 1.55);
    ImmutableRatesProvider provider = RatesProviderDataSets.multiGbpUsd(valuationDate).toBuilder()
        .timeSeries(FxIndices.GBP_USD_WM, ts)
        .build();
    // USD
    MultiCurrencyAmount computedUSD = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    PointSensitivities pointUSD = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider).build();
    MultiCurrencyAmount expectedUSD = provider.currencyExposure(pointUSD.convertedTo(GBP, provider)).plus(
        CurrencyAmount.of(
            PAYMENT_PERIOD_FULL_GS_FX_USD.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)));
    assertThat(computedUSD.getAmount(USD).getAmount()).isCloseTo(expectedUSD.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedUSD.contains(GBP)).isFalse(); // 0 GBP
    // GBP
    MultiCurrencyAmount computedGBP = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    PointSensitivities pointGBP = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider).build();
    MultiCurrencyAmount expectedGBP = provider.currencyExposure(pointGBP.convertedTo(USD, provider)).plus(
        CurrencyAmount.of(
            PAYMENT_PERIOD_FULL_GS_FX_GBP.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)));
    assertThat(computedGBP.getAmount(GBP).getAmount()).isCloseTo(expectedGBP.getAmount(GBP).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedGBP.contains(USD)).isFalse(); // 0 USD
    // FD approximation
    ImmutableRatesProvider provUp = RatesProviderDataSets.multiGbpUsd(valuationDate).toBuilder()
        .fxRateProvider(FX_MATRIX_BUMP)
        .timeSeries(FxIndices.GBP_USD_WM, ts)
        .build();
    double expectedFdUSD = (pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)) / EPS_FD;
    assertThat(!computedUSD.contains(GBP) && DoubleMath.fuzzyEquals(expectedFdUSD, 0d, TOLERANCE_PV)).isTrue();
    double expectedFdGBP = -(pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(!computedGBP.contains(USD) && DoubleMath.fuzzyEquals(expectedFdGBP, 0d, TOLERANCE_PV)).isTrue();
  }

  @Test
  public void test_currencyExposure_fx_onFixing_noTimeSeries() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD;
    // USD
    MultiCurrencyAmount computedUSD = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_USD, provider);
    PointSensitivities pointUSD = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_USD, provider).build();
    MultiCurrencyAmount expectedUSD = provider.currencyExposure(pointUSD.convertedTo(GBP, provider)).plus(
        CurrencyAmount.of(
            PAYMENT_PERIOD_FULL_GS_FX_USD.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)));
    assertThat(computedUSD.getAmount(GBP).getAmount()).isCloseTo(expectedUSD.getAmount(GBP).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedUSD.contains(USD)).isFalse(); // 0 GBP
    // GBP
    MultiCurrencyAmount computedGBP = pricer.currencyExposure(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider);
    PointSensitivities pointGBP = pricer.presentValueSensitivity(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider).build();
    MultiCurrencyAmount expectedGBP = provider.currencyExposure(pointGBP.convertedTo(USD, provider)).plus(CurrencyAmount.of(
        PAYMENT_PERIOD_FULL_GS_FX_GBP.getCurrency(), pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)));
    assertThat(computedGBP.getAmount(USD).getAmount()).isCloseTo(expectedGBP.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
    assertThat(computedGBP.contains(GBP)).isFalse(); // 0 GBP
    // FD approximation
    ImmutableRatesProvider provUp = RatesProviderDataSets.multiGbpUsd(VAL_DATE).toBuilder()
        .fxRateProvider(FX_MATRIX_BUMP)
        .build();
    double expectedFdUSD = (pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_USD, provider)) / EPS_FD;
    assertThat(computedUSD.getAmount(GBP).getAmount()).isCloseTo(expectedFdUSD, offset(EPS_FD * NOTIONAL_100));
    double expectedFdGBP = -(pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provUp) -
        pricer.presentValue(PAYMENT_PERIOD_FULL_GS_FX_GBP, provider)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(computedGBP.getAmount(USD).getAmount()).isCloseTo(expectedFdGBP, offset(EPS_FD * NOTIONAL_100));
  }

  @Test
  public void test_currencyExposure_single() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD;
    MultiCurrencyAmount computed = pricer.currencyExposure(PAYMENT_PERIOD_COMPOUNDING_STRAIGHT, provider);
    PointSensitivities point = pricer.presentValueSensitivity(PAYMENT_PERIOD_COMPOUNDING_STRAIGHT, provider).build();
    MultiCurrencyAmount expected = provider.currencyExposure(point)
        .plus(CurrencyAmount.of(PAYMENT_PERIOD_COMPOUNDING_STRAIGHT.getCurrency(),
            pricer.presentValue(PAYMENT_PERIOD_COMPOUNDING_STRAIGHT, provider)));
    assertThat(computed.size()).isEqualTo(expected.size());
    assertThat(computed.getAmount(USD).getAmount()).isCloseTo(expected.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  public void test_currentCash_zero() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    ImmutableRatesProvider provider = MULTI_GBP_USD;
    double computed = pricer.currentCash(PAYMENT_PERIOD_COMPOUNDING_FLAT, provider);
    assertThat(computed).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_onPayment() {
    DiscountingRatePaymentPeriodPricer pricer = DiscountingRatePaymentPeriodPricer.DEFAULT;
    RatesProvider provider = createProvider(PAYMENT_PERIOD_1.getPaymentDate());
    double computed = pricer.currentCash(PAYMENT_PERIOD_1, provider);
    assertThat(computed).isEqualTo(RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100);
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(LocalDate valDate) {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(PAYMENT_DATE_1)).thenReturn(DISCOUNT_FACTOR);
    FxIndexRates mockFxRates = mock(FxIndexRates.class);
    when(mockFxRates.rate(FxIndexObservation.of(GBP_USD_WM, FX_DATE_1, REF_DATA), GBP)).thenReturn(RATE_FX);
    SimpleRatesProvider prov = new SimpleRatesProvider(valDate);
    prov.setDayCount(DAY_COUNT);
    prov.setDiscountFactors(mockDf);
    prov.setFxIndexRates(mockFxRates);
    return prov;
  }

}

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fra;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_AFMA;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_NONE;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_PAID;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_TRADE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleIborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.ResolvedFra;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Tests {@link DiscountingFraProductPricer}.
 */
public class DiscountingFraProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ISDA;
  private static final double TOLERANCE = 1E-12;
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FORWARD_RATE = 0.02;

  private static final ResolvedFraTrade RFRA_TRADE = FRA_TRADE.resolve(REF_DATA);
  private static final ResolvedFra RFRA = FRA.resolve(REF_DATA);
  private static final ResolvedFra RFRA_NONE = FRA_NONE.resolve(REF_DATA);
  private static final ResolvedFra RFRA_AFMA = FRA_AFMA.resolve(REF_DATA);

  /**
   * Test forecast value for ISDA FRA Discounting method.
   */
  @Test
  public void test_forecastValue_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    double fixedRate = FRA.getFixedRate();
    double yearFraction = RFRA.getYearFraction();
    double notional = RFRA.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate) / (1.0 + yearFraction * FORWARD_RATE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA, prov);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.forecastValue(RFRA_TRADE, prov)).isEqualTo(test.forecastValue(RFRA, prov));
  }

  /**
   * Test forecast value for NONE FRA Discounting method.
   */
  @Test
  public void test_forecastValue_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    double fixedRate = FRA_NONE.getFixedRate();
    double yearFraction = RFRA_NONE.getYearFraction();
    double notional = RFRA_NONE.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA_NONE, prov);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE));
  }

  /**
   * Test forecast value for AFMA FRA Discounting method.
   */
  @Test
  public void test_forecastValue_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = RFRA_AFMA.getYearFraction();
    double notional = RFRA_AFMA.getNotional();
    double expected = -notional * (1.0 / (1.0 + yearFraction * FORWARD_RATE) - 1.0 / (1.0 + yearFraction * fixedRate));

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA_AFMA, prov);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE));
  }

  /**
   * Test FRA paying in the past.
   */
  @Test
  public void test_forecastValue_inPast() {
    SimpleRatesProvider prov = createProvider(RFRA.toBuilder().paymentDate(VAL_DATE.minusDays(1)).build());

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA.toBuilder().paymentDate(VAL_DATE.minusDays(1)).build(), prov);
    assertThat(computed.getAmount()).isCloseTo(0d, offset(TOLERANCE));
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value for ISDA FRA Discounting method.
   */
  @Test
  public void test_presentValue_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA, prov).multipliedBy(DISCOUNT_FACTOR);
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.presentValue(RFRA_TRADE, prov)).isEqualTo(test.presentValue(RFRA, prov));
  }

  /**
   * Test present value for NONE FRA Discounting method.
   */
  @Test
  public void test_presentValue_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA_NONE, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA_NONE, prov).multipliedBy(DISCOUNT_FACTOR);
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOLERANCE));
  }

  /**
   * Test present value for ISDA FRA Discounting method.
   */
  @Test
  public void test_presentValue_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA_AFMA, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA_AFMA, prov).multipliedBy(DISCOUNT_FACTOR);
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOLERANCE));
  }

  /**
   * Test FRA paying in the past.
   */
  @Test
  public void test_presentValue_inPast() {
    ResolvedFra fra = FRA_PAID.resolve(REF_DATA);
    SimpleRatesProvider prov = createProvider(fra);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.presentValue(fra, prov);
    assertThat(computed.getAmount()).isCloseTo(0d, offset(TOLERANCE));
  }

  //-------------------------------------------------------------------------
  /**
   * Test forecast value sensitivity for ISDA FRA discounting method.
   */
  @Test
  public void test_forecastValueSensitivity_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(FRA.getStartDate());
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA.getNotional() * eps));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.forecastValueSensitivity(RFRA_TRADE, prov)).isEqualTo(test.forecastValueSensitivity(RFRA, prov));
  }

  /**
   * Test forecast value sensitivity for NONE FRA discounting method.
   */
  @Test
  public void test_forecastValueSensitivity_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA_NONE, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA_NONE, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA_NONE.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(FRA_NONE.getStartDate());
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA_NONE.getNotional() * eps));
  }

  /**
   * Test forecast value sensitivity for AFMA FRA discounting method.
   */
  @Test
  public void test_forecastValueSensitivity_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA_AFMA, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA_AFMA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA_AFMA.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(FRA_AFMA.getStartDate());
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA_AFMA.getNotional() * eps));
  }

  /**
   * Test FRA paying in the past.
   */
  @Test
  public void test_forecastValueSensitivity_inPast() {
    ResolvedFra fra = FRA_PAID.resolve(REF_DATA);
    SimpleRatesProvider prov = createProvider(fra);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities computed = test.forecastValueSensitivity(fra, prov);
    assertThat(computed.size()).isEqualTo(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value sensitivity for ISDA  
   */
  @Test
  public void test_presentValueSensitivity_ISDA() {
    RateComputationFn<RateComputation> mockObs = mock(RateComputationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ResolvedFra fraExp = RFRA;
    double forwardRate = 0.05;
    double discountRate = 0.015;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA.getStartDate();
    IborIndexObservation obs = IborIndexObservation.of(FRA.getIndex(), fixingDate, REF_DATA);
    PointSensitivityBuilder sens = IborRateSensitivity.of(obs, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(fraExp.getCurrency(), paymentTime, -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(RFRA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(RFRA, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(fixingDate);
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA.getNotional() * eps));
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertThat(sensitivity1.getCurrency()).isEqualTo(FRA.getCurrency());
    assertThat(sensitivity1.getYearFraction()).isEqualTo(paymentTime);
    assertThat(sensitivity1.getSensitivity()).isCloseTo(fdDscSense, offset(FRA.getNotional() * eps));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.presentValueSensitivity(RFRA_TRADE, simpleProv)).isEqualTo(test.presentValueSensitivity(fraExp, simpleProv));
  }

  /**
   * Test present value sensitivity for NONE FRA discounting method.
   */
  @Test
  public void test_presentValueSensitivity_NONE() {
    RateComputationFn<RateComputation> mockObs = mock(RateComputationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ResolvedFra fraExp = RFRA_NONE;
    double forwardRate = 0.025;
    double discountRate = 0.01;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_NONE.getStartDate();
    IborIndexObservation obs = IborIndexObservation.of(FRA.getIndex(), fixingDate, REF_DATA);
    PointSensitivityBuilder sens = IborRateSensitivity.of(obs, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(fraExp.getCurrency(), paymentTime, -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(RFRA_NONE, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(RFRA_NONE, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA_NONE.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(fixingDate);
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA_NONE.getNotional() * eps));
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertThat(sensitivity1.getCurrency()).isEqualTo(FRA_NONE.getCurrency());
    assertThat(sensitivity1.getYearFraction()).isEqualTo(paymentTime);
    assertThat(sensitivity1.getSensitivity()).isCloseTo(fdDscSense, offset(FRA_NONE.getNotional() * eps));
  }

  /**
   * Test present value sensitivity for AFMA FRA discounting method.
   */
  @Test
  public void test_presentValueSensitivity_AFMA() {
    RateComputationFn<RateComputation> mockObs = mock(RateComputationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ResolvedFra fraExp = RFRA_AFMA;
    double forwardRate = 0.05;
    double discountRate = 0.025;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    IborIndexObservation obs = IborIndexObservation.of(FRA.getIndex(), fixingDate, REF_DATA);
    PointSensitivityBuilder sens = IborRateSensitivity.of(obs, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(fraExp.getCurrency(), paymentTime, -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(RFRA_AFMA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(RFRA_AFMA, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertThat(sensitivities).hasSize(2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertThat(sensitivity0.getIndex()).isEqualTo(FRA_AFMA.getIndex());
    assertThat(sensitivity0.getObservation().getFixingDate()).isEqualTo(fixingDate);
    assertThat(sensitivity0.getSensitivity()).isCloseTo(fdSense, offset(FRA_AFMA.getNotional() * eps));
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertThat(sensitivity1.getCurrency()).isEqualTo(FRA_AFMA.getCurrency());
    assertThat(sensitivity1.getYearFraction()).isEqualTo(paymentTime);
    assertThat(sensitivity1.getSensitivity()).isCloseTo(fdDscSense, offset(FRA_AFMA.getNotional() * eps));
  }

  /**
   * Test FRA paying in the past.
   */
  @Test
  public void test_presentValueSensitivity_inPast() {
    ResolvedFra fra = FRA_PAID.resolve(REF_DATA);
    SimpleRatesProvider prov = createProvider(fra);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities computed = test.presentValueSensitivity(fra, prov);
    assertThat(computed.size()).isEqualTo(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Test par rate for ISDA FRA Discounting method.
   */
  @Test
  public void test_parRate_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertThat(parRate).isEqualTo(FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.parRate(RFRA_TRADE, prov)).isEqualTo(test.parRate(RFRA, prov));
  }

  /**
   * Test par rate for NONE FRA Discounting method.
   */
  @Test
  public void test_parRate_NONE() {
    ResolvedFra fraExp = RFRA_NONE;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertThat(parRate).isEqualTo(FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA_NONE, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  /**
   * Test par rate for AFMA FRA Discounting method.
   */
  @Test
  public void test_parRate_AFMA() {
    ResolvedFra fraExp = RFRA_AFMA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertThat(parRate).isEqualTo(FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA_AFMA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  /**
   * Test par spread for ISDA FRA Discounting method.
   */
  @Test
  public void test_parSpread_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA, FRA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.parSpread(RFRA_TRADE, prov)).isEqualTo(test.parSpread(RFRA, prov));
  }

  /**
   * Test par spread for NONE FRA Discounting method.
   */
  @Test
  public void test_parSpread_NONE() {
    ResolvedFra fraExp = RFRA_NONE;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA_NONE, FRA_NONE.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  /**
   * Test par spread for AFMA FRA Discounting method.
   */
  @Test
  public void test_parSpread_AFMA() {
    ResolvedFra fraExp = RFRA_AFMA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA_AFMA, FRA_AFMA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertThat(pv.getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  private static final double EPS_FD = 1E-7;
  private static final DiscountingFraProductPricer DEFAULT_PRICER = DiscountingFraProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer DEFAULT_TRADE_PRICER = DiscountingFraTradePricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray timeGbp = DoubleArray.of(0.0, 0.1, 0.25, 0.5, 0.75, 1.0, 2.0);
    DoubleArray rateGbp = DoubleArray.of(0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.014);
    InterpolatedNodalCurve dscCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("GBP-Discount", DAY_COUNT), timeGbp, rateGbp, interp);
    DoubleArray timeIndex = DoubleArray.of(0.0, 0.25, 0.5, 1.0);
    DoubleArray rateIndex = DoubleArray.of(0.0180, 0.0180, 0.0175, 0.0165);
    InterpolatedNodalCurve indexCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("GBP-GBPIBOR3M", DAY_COUNT), timeIndex, rateIndex, interp);
    IMM_PROV = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, dscCurve)
        .iborIndexCurve(GBP_LIBOR_3M, indexCurve)
        .build();
  }

  /**
   * Test par spread sensitivity for ISDA FRA Discounting method.
   */
  @Test
  public void test_parSpreadSensitivity_ISDA() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA.getCurrency(), DEFAULT_PRICER.parSpread(RFRA, (p))));
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD)).isTrue();
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA, IMM_PROV);
    assertThat(sensiSpread.equalWithTolerance(sensiRate, EPS_FD)).isTrue();

    // test via FraTrade
    assertThat(DEFAULT_TRADE_PRICER.parRateSensitivity(RFRA_TRADE, IMM_PROV)).isEqualTo(DEFAULT_PRICER.parRateSensitivity(RFRA, IMM_PROV));
    assertThat(DEFAULT_TRADE_PRICER.parSpreadSensitivity(RFRA_TRADE, IMM_PROV)).isEqualTo(DEFAULT_PRICER.parSpreadSensitivity(RFRA, IMM_PROV));
  }

  /**
   * Test par spread sensitivity for NONE FRA Discounting method.
   */
  @Test
  public void test_parSpreadSensitivity_NONE() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA_NONE, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_NONE.getCurrency(), DEFAULT_PRICER.parSpread(RFRA_NONE, (p))));
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD)).isTrue();
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA_NONE, IMM_PROV);
    assertThat(sensiSpread.equalWithTolerance(sensiRate, EPS_FD)).isTrue();
  }

  /**
   * Test par spread sensitivity for AFMA FRA Discounting method.
   */
  @Test
  public void test_parSpreadSensitivity_AFMA() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA_AFMA, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_AFMA.getCurrency(), DEFAULT_PRICER.parSpread(RFRA_AFMA, (p))));
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD)).isTrue();
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA_AFMA, IMM_PROV);
    assertThat(sensiSpread.equalWithTolerance(sensiRate, EPS_FD)).isTrue();
  }

  private ResolvedFra createNewFra(Fra product, double newFixedRate) {
    return Fra.builder()
        .buySell(product.getBuySell())
        .notional(product.getNotional())
        .startDate(product.getStartDate())
        .endDate(product.getEndDate())
        .index(product.getIndex())
        .fixedRate(newFixedRate)
        .currency(product.getCurrency())
        .build()
        .resolve(REF_DATA);
  }

  //-------------------------------------------------------------------------
  /**
   * Test cash flow for ISDA FRA Discounting method.
   */
  @Test
  public void test_cashFlows_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate) / (1.0 + yearFraction * FORWARD_RATE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CashFlows computed = test.cashFlows(fraExp, prov);
    assertThat(computed.getCashFlows()).hasSize(1);
    assertThat(computed.getCashFlows().get(0).getPaymentDate()).isEqualTo(fraExp.getPaymentDate());
    assertThat(computed.getCashFlows().get(0).getForecastValue().getCurrency()).isEqualTo(fraExp.getCurrency());
    assertThat(computed.getCashFlows().get(0).getForecastValue().getAmount()).isCloseTo(expected, offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.cashFlows(RFRA_TRADE, prov)).isEqualTo(test.cashFlows(fraExp, prov));
  }

  //-------------------------------------------------------------------------
  /**
   * Test explain.
   */
  @Test
  public void test_explainPresentValue_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount fvExpected = test.forecastValue(fraExp, prov);
    CurrencyAmount pvExpected = test.presentValue(fraExp, prov);

    ExplainMap explain = test.explainPresentValue(fraExp, prov);
    Currency currency = fraExp.getCurrency();
    int daysBetween = (int) DAYS.between(fraExp.getStartDate(), fraExp.getEndDate());
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FRA");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(fraExp.getPaymentDate());
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(fraExp.getStartDate());
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(fraExp.getEndDate());
    assertThat(explain.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get()).isEqualTo(fraExp.getYearFraction());
    assertThat(explain.get(ExplainKey.DAYS).get()).isEqualTo((Integer) (int) daysBetween);
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.NOTIONAL).get().getAmount()).isCloseTo(fraExp.getNotional(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(fraExp.getNotional(), offset(TOLERANCE));

    assertThat(explain.get(ExplainKey.OBSERVATIONS).get()).hasSize(1);
    ExplainMap explainObs = explain.get(ExplainKey.OBSERVATIONS).get().get(0);
    IborRateComputation floatingRate = (IborRateComputation) fraExp.getFloatingRate();
    assertThat(explainObs.get(ExplainKey.INDEX).get()).isEqualTo(floatingRate.getIndex());
    assertThat(explainObs.get(ExplainKey.FIXING_DATE).get()).isEqualTo(floatingRate.getFixingDate());
    assertThat(explainObs.get(ExplainKey.INDEX_VALUE).get()).isCloseTo(FORWARD_RATE, offset(TOLERANCE));
    assertThat(explainObs.get(ExplainKey.FROM_FIXING_SERIES)).isNotPresent();
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FIXED_RATE).get()).isCloseTo(fraExp.getFixedRate(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PAY_OFF_RATE).get()).isCloseTo(FORWARD_RATE, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.COMBINED_RATE).get()).isCloseTo(FORWARD_RATE, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.UNIT_AMOUNT).get()).isCloseTo(fvExpected.getAmount() / fraExp.getNotional(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fvExpected.getAmount(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOLERANCE));

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertThat(testTrade.explainPresentValue(RFRA_TRADE, prov)).isEqualTo(test.explainPresentValue(RFRA, prov));
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(ResolvedFra fraExp) {
    DiscountFactors mockDf = SimpleDiscountFactors.of(
        GBP, VAL_DATE, ConstantCurve.of(Curves.discountFactors("DSC", DAY_COUNT), DISCOUNT_FACTOR));
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE, FORWARD_RATE);
    IborIndexRates mockIbor = SimpleIborIndexRates.of(
        GBP_LIBOR_3M, VAL_DATE, ConstantCurve.of(Curves.forwardRates("L3M", DAY_COUNT), FORWARD_RATE), timeSeries);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE, mockDf);
    prov.setIborRates(mockIbor);
    return prov;
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_zeroCurve_FD() {
    double eps = 1.0e-6;
    ImmutableRatesProvider prov = RatesProviderDataSets.MULTI_GBP_USD;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra fraExp = RFRA;
    PointSensitivities point = pricer.presentValueSensitivity(fraExp, prov);
    CurrencyParameterSensitivities computed = prov.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = cal.sensitivity(prov, p -> pricer.presentValue(fraExp, p));
    assertThat(computed.equalWithTolerance(expected, eps * FRA.getNotional())).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_dfCurve_FD() {
    double eps = 1.0e-6;
    ImmutableRatesProvider prov = RatesProviderDataSets.MULTI_GBP_USD_SIMPLE;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra fraExp = RFRA;
    PointSensitivities point = pricer.presentValueSensitivity(fraExp, prov);
    CurrencyParameterSensitivities computed = prov.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = cal.sensitivity(prov, p -> pricer.presentValue(fraExp, p));
    assertThat(computed.equalWithTolerance(expected, eps * FRA.getNotional())).isTrue();
  }

  //-------------------------------------------------------------------------
  private double forecastValueFwdSensitivity(ResolvedFra fra, double forwardRate, double eps) {

    RateComputationFn<RateComputation> obsFuncNew = mock(RateComputationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    when(obsFuncNew.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).forecastValue(fra, provNew);
    when(obsFuncNew.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).forecastValue(fra, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double presentValueFwdSensitivity(ResolvedFra fra, double forwardRate, double discountFactor, double eps) {

    RateComputationFn<RateComputation> obsFuncNew = mock(RateComputationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    when(provNew.discountFactor(fra.getCurrency(), fra.getPaymentDate()))
        .thenReturn(discountFactor);
    when(obsFuncNew.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fra, provNew);
    when(obsFuncNew.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fra, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double dscSensitivity(
      ResolvedFra fra, double forwardRate, double discountFactor, double paymentTime, double eps) {

    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    RateComputationFn<RateComputation> obsFuncNew = mock(RateComputationFn.class);
    when(obsFuncNew.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate);
    when(provNew.discountFactor(fra.getCurrency(), fra.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    CurrencyAmount upDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fra, provNew);
    when(provNew.discountFactor(fra.getCurrency(), fra.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(eps * paymentTime));
    CurrencyAmount downDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fra, provNew);
    return upDscValue.minus(downDscValue).multipliedBy(0.5 / eps).getAmount();
  }

}

/**
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
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_TRADE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.amount.CashFlows;
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
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
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
@Test
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
  public void test_forecastValue_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    double fixedRate = FRA.getFixedRate();
    double yearFraction = RFRA.getYearFraction();
    double notional = RFRA.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate) / (1.0 + yearFraction * FORWARD_RATE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.forecastValue(RFRA_TRADE, prov), test.forecastValue(RFRA, prov));
  }

  /**
   * Test forecast value for NONE FRA Discounting method.
   */
  public void test_forecastValue_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    double fixedRate = FRA_NONE.getFixedRate();
    double yearFraction = RFRA_NONE.getYearFraction();
    double notional = RFRA_NONE.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA_NONE, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test forecast value for AFMA FRA Discounting method.
   */
  public void test_forecastValue_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = RFRA_AFMA.getYearFraction();
    double notional = RFRA_AFMA.getNotional();
    double expected = -notional * (1.0 / (1.0 + yearFraction * FORWARD_RATE) - 1.0 / (1.0 + yearFraction * fixedRate));

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA_AFMA, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_forecastValue_inPast() {
    SimpleRatesProvider prov = createProvider(RFRA.toBuilder().paymentDate(VAL_DATE.minusDays(1)).build());

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(RFRA.toBuilder().paymentDate(VAL_DATE.minusDays(1)).build(), prov);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value for ISDA FRA Discounting method.
   */
  public void test_presentValue_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValue(RFRA_TRADE, prov), test.presentValue(RFRA, prov));
  }

  /**
   * Test present value for NONE FRA Discounting method.
   */
  public void test_presentValue_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA_NONE, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA_NONE, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test present value for ISDA FRA Discounting method.
   */
  public void test_presentValue_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(RFRA_AFMA, prov);
    CurrencyAmount pvExpected = test.forecastValue(RFRA_AFMA, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_presentValue_inPast() {
    ResolvedFra fra = RFRA.toBuilder().paymentDate(VAL_DATE.minusDays(1)).build();
    SimpleRatesProvider prov = createProvider(fra);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.presentValue(fra, prov);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test forecast value sensitivity for ISDA FRA discounting method.
   */
  public void test_forecastValueSensitivity_ISDA() {
    SimpleRatesProvider prov = createProvider(RFRA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), FRA.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.forecastValueSensitivity(RFRA_TRADE, prov), test.forecastValueSensitivity(RFRA, prov));
  }

  /**
   * Test forecast value sensitivity for NONE FRA discounting method.
   */
  public void test_forecastValueSensitivity_NONE() {
    SimpleRatesProvider prov = createProvider(RFRA_NONE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA_NONE, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA_NONE, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), FRA_NONE.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test forecast value sensitivity for AFMA FRA discounting method.
   */
  public void test_forecastValueSensitivity_AFMA() {
    SimpleRatesProvider prov = createProvider(RFRA_AFMA);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(RFRA_AFMA, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(RFRA_AFMA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), FRA_AFMA.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value sensitivity for ISDA  
   */
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
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA.getCurrency());
    assertEquals(sensitivity1.getYearFraction(), paymentTime);
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValueSensitivity(RFRA_TRADE, simpleProv), test.presentValueSensitivity(fraExp, simpleProv));
  }

  /**
   * Test present value sensitivity for NONE FRA discounting method.
   */
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
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_NONE.getCurrency());
    assertEquals(sensitivity1.getYearFraction(), paymentTime);
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for AFMA FRA discounting method.
   */
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
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getObservation().getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_AFMA.getCurrency());
    assertEquals(sensitivity1.getYearFraction(), paymentTime);
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  /**
   * Test par rate for ISDA FRA Discounting method.
   */
  public void test_parRate_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.parRate(RFRA_TRADE, prov), test.parRate(RFRA, prov));
  }

  /**
   * Test par rate for NONE FRA Discounting method.
   */
  public void test_parRate_NONE() {
    ResolvedFra fraExp = RFRA_NONE;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA_NONE, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par rate for AFMA FRA Discounting method.
   */
  public void test_parRate_AFMA() {
    ResolvedFra fraExp = RFRA_AFMA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    ResolvedFra fra = createNewFra(FRA_AFMA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par spread for ISDA FRA Discounting method.
   */
  public void test_parSpread_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA, FRA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.parSpread(RFRA_TRADE, prov), test.parSpread(RFRA, prov));
  }

  /**
   * Test par spread for NONE FRA Discounting method.
   */
  public void test_parSpread_NONE() {
    ResolvedFra fraExp = RFRA_NONE;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA_NONE, FRA_NONE.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par spread for AFMA FRA Discounting method.
   */
  public void test_parSpread_AFMA() {
    ResolvedFra fraExp = RFRA_AFMA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    ResolvedFra fra = createNewFra(FRA_AFMA, FRA_AFMA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  private static final double EPS_FD = 1E-7;
  private static final DiscountingFraProductPricer DEFAULT_PRICER = DiscountingFraProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer DEFAULT_TRADE_PRICER = DiscountingFraTradePricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray time_gbp = DoubleArray.of(0.0, 0.1, 0.25, 0.5, 0.75, 1.0, 2.0);
    DoubleArray rate_gbp = DoubleArray.of(0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.014);
    InterpolatedNodalCurve dscCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("GBP-Discount", DAY_COUNT), time_gbp, rate_gbp, interp);
    DoubleArray time_index = DoubleArray.of(0.0, 0.25, 0.5, 1.0);
    DoubleArray rate_index = DoubleArray.of(0.0180, 0.0180, 0.0175, 0.0165);
    InterpolatedNodalCurve indexCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("GBP-GBPIBOR3M", DAY_COUNT), time_index, rate_index, interp);
    IMM_PROV = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, dscCurve)
        .iborIndexCurve(GBP_LIBOR_3M, indexCurve)
        .build();
  }

  /**
   * Test par spread sensitivity for ISDA FRA Discounting method.
   */
  public void test_parSpreadSensitivity_ISDA() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA.getCurrency(), DEFAULT_PRICER.parSpread(RFRA, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA, IMM_PROV);
    assertTrue(sensiSpread.equalWithTolerance(sensiRate, EPS_FD));

    // test via FraTrade
    assertEquals(
        DEFAULT_TRADE_PRICER.parRateSensitivity(RFRA_TRADE, IMM_PROV),
        DEFAULT_PRICER.parRateSensitivity(RFRA, IMM_PROV));
    assertEquals(
        DEFAULT_TRADE_PRICER.parSpreadSensitivity(RFRA_TRADE, IMM_PROV),
        DEFAULT_PRICER.parSpreadSensitivity(RFRA, IMM_PROV));
  }

  /**
   * Test par spread sensitivity for NONE FRA Discounting method.
   */
  public void test_parSpreadSensitivity_NONE() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA_NONE, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_NONE.getCurrency(), DEFAULT_PRICER.parSpread(RFRA_NONE, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA_NONE, IMM_PROV);
    assertTrue(sensiSpread.equalWithTolerance(sensiRate, EPS_FD));
  }

  /**
   * Test par spread sensitivity for AFMA FRA Discounting method.
   */
  public void test_parSpreadSensitivity_AFMA() {
    PointSensitivities sensiSpread = DEFAULT_PRICER.parSpreadSensitivity(RFRA_AFMA, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(sensiSpread);
    CurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_AFMA.getCurrency(), DEFAULT_PRICER.parSpread(RFRA_AFMA, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
    PointSensitivities sensiRate = DEFAULT_PRICER.parRateSensitivity(RFRA_AFMA, IMM_PROV);
    assertTrue(sensiSpread.equalWithTolerance(sensiRate, EPS_FD));
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
  public void test_cashFlows_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate) / (1.0 + yearFraction * FORWARD_RATE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CashFlows computed = test.cashFlows(fraExp, prov);
    assertEquals(computed.getCashFlows().size(), 1);
    assertEquals(computed.getCashFlows().size(), 1);
    assertEquals(computed.getCashFlows().get(0).getPaymentDate(), fraExp.getPaymentDate());
    assertEquals(computed.getCashFlows().get(0).getForecastValue().getCurrency(), fraExp.getCurrency());
    assertEquals(computed.getCashFlows().get(0).getForecastValue().getAmount(), expected, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.cashFlows(RFRA_TRADE, prov), test.cashFlows(fraExp, prov));
  }

  //-------------------------------------------------------------------------
  /**
   * Test explain.
   */
  public void test_explainPresentValue_ISDA() {
    ResolvedFra fraExp = RFRA;
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount fvExpected = test.forecastValue(fraExp, prov);
    CurrencyAmount pvExpected = test.presentValue(fraExp, prov);

    ExplainMap explain = test.explainPresentValue(fraExp, prov);
    Currency currency = fraExp.getCurrency();
    int daysBetween = (int) DAYS.between(fraExp.getStartDate(), fraExp.getEndDate());
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "FRA");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), fraExp.getPaymentDate());
    assertEquals(explain.get(ExplainKey.START_DATE).get(), fraExp.getStartDate());
    assertEquals(explain.get(ExplainKey.END_DATE).get(), fraExp.getEndDate());
    assertEquals(explain.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get(), fraExp.getYearFraction());
    assertEquals(explain.get(ExplainKey.DAYS).get(), (Integer) (int) daysBetween);
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), currency);
    assertEquals(explain.get(ExplainKey.NOTIONAL).get().getAmount(), fraExp.getNotional(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), fraExp.getNotional(), TOLERANCE);

    assertEquals(explain.get(ExplainKey.OBSERVATIONS).get().size(), 1);
    ExplainMap explainObs = explain.get(ExplainKey.OBSERVATIONS).get().get(0);
    IborRateComputation floatingRate = (IborRateComputation) fraExp.getFloatingRate();
    assertEquals(explainObs.get(ExplainKey.INDEX).get(), floatingRate.getIndex());
    assertEquals(explainObs.get(ExplainKey.FIXING_DATE).get(), floatingRate.getFixingDate());
    assertEquals(explainObs.get(ExplainKey.INDEX_VALUE).get(), FORWARD_RATE, TOLERANCE);
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), DISCOUNT_FACTOR, TOLERANCE);
    assertEquals(explain.get(ExplainKey.FIXED_RATE).get(), fraExp.getFixedRate(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.PAY_OFF_RATE).get(), FORWARD_RATE, TOLERANCE);
    assertEquals(explain.get(ExplainKey.COMBINED_RATE).get(), FORWARD_RATE, TOLERANCE);
    assertEquals(explain.get(ExplainKey.UNIT_AMOUNT).get(), fvExpected.getAmount() / fraExp.getNotional(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), fvExpected.getAmount(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), pvExpected.getAmount(), TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.explainPresentValue(RFRA_TRADE, prov), test.explainPresentValue(RFRA, prov));
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(ResolvedFra fraExp) {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE, mockDf);
    prov.setIborRates(mockIbor);

    IborIndexObservation obs = ((IborRateComputation) fraExp.getFloatingRate()).getObservation();
    IborRateSensitivity sens = IborRateSensitivity.of(obs, 1d);
    when(mockIbor.ratePointSensitivity(obs)).thenReturn(sens);
    when(mockIbor.rate(obs)).thenReturn(FORWARD_RATE);

    when(mockDf.discountFactor(fraExp.getPaymentDate())).thenReturn(DISCOUNT_FACTOR);
    return prov;
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_zeroCurve_FD() {
    double eps = 1.0e-6;
    ImmutableRatesProvider prov = RatesProviderDataSets.MULTI_GBP_USD;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra fraExp = RFRA;
    PointSensitivities point = pricer.presentValueSensitivity(fraExp, prov);
    CurrencyParameterSensitivities computed = prov.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = cal.sensitivity(prov, p -> pricer.presentValue(fraExp, p));
    assertTrue(computed.equalWithTolerance(expected, eps * FRA.getNotional()));
  }

  public void test_presentValueSensitivity_dfCurve_FD() {
    double eps = 1.0e-6;
    ImmutableRatesProvider prov = RatesProviderDataSets.MULTI_GBP_USD_SIMPLE;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra fraExp = RFRA;
    PointSensitivities point = pricer.presentValueSensitivity(fraExp, prov);
    CurrencyParameterSensitivities computed = prov.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = cal.sensitivity(prov, p -> pricer.presentValue(fraExp, p));
    assertTrue(computed.equalWithTolerance(expected, eps * FRA.getNotional()));
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

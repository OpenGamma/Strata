/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_AFMA;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_NONE;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_TRADE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.rate.fra.ExpandedFra;
import com.opengamma.strata.product.rate.fra.Fra;

/**
 * Test.
 */
@Test
public class DiscountingFraProductPricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ISDA;
  private static final double TOLERANCE = 1E-12;
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FORWARD_RATE = 0.02;

  /**
   * Test forecast value for ISDA FRA Discounting method.
   */
  public void test_forecastValue_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate) / (1.0 + yearFraction * FORWARD_RATE);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(fraExp, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.forecastValue(FRA_TRADE, prov), test.forecastValue(fraExp, prov));
  }

  /**
   * Test forecast value for NONE FRA Discounting method.
   */
  public void test_forecastValue_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    double fixedRate = FRA_NONE.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = notional * yearFraction * (FORWARD_RATE - fixedRate);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(fraExp, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test forecast value for AFMA FRA Discounting method.
   */
  public void test_forecastValue_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = -notional * (1.0 / (1.0 + yearFraction * FORWARD_RATE) - 1.0 / (1.0 + yearFraction * fixedRate));

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(fraExp, prov);
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_forecastValue_inPast() {
    ExpandedFra fraExp = FRA.expand().toBuilder().paymentDate(VAL_DATE.minusDays(1)).build();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.forecastValue(fraExp, prov);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(fraExp, prov);
    CurrencyAmount pvExpected = test.forecastValue(fraExp, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValue(FRA_TRADE, prov), test.presentValue(fraExp, prov));
  }

  /**
   * Test present value for NONE FRA Discounting method. 
   */
  public void test_presentValue_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(fraExp, prov);
    CurrencyAmount pvExpected = test.forecastValue(fraExp, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount pvComputed = test.presentValue(fraExp, prov);
    CurrencyAmount pvExpected = test.forecastValue(fraExp, prov).multipliedBy(DISCOUNT_FACTOR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_presentValue_inPast() {
    ExpandedFra fraExp = FRA.expand().toBuilder().paymentDate(VAL_DATE.minusDays(1)).build();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    CurrencyAmount computed = test.presentValue(fraExp, prov);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test forecast value sensitivity for ISDA FRA discounting method.
   */
  public void test_forecastValueSensitivity_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(fraExp, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(FRA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), FRA.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.forecastValueSensitivity(FRA_TRADE, prov), test.forecastValueSensitivity(fraExp, prov));
  }

  /**
   * Test forecast value sensitivity for NONE FRA discounting method.
   */
  public void test_forecastValueSensitivity_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(fraExp, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(FRA_NONE, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getFixingDate(), FRA_NONE.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test forecast value sensitivity for AFMA FRA discounting method.
   */
  public void test_forecastValueSensitivity_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    PointSensitivities sensitivity = test.forecastValueSensitivity(fraExp, prov);
    double eps = 1.e-7;
    double fdSense = forecastValueFwdSensitivity(FRA_AFMA, FORWARD_RATE, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), FRA_AFMA.getStartDate());
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value sensitivity for ISDA  
   */
  public void test_presentValueSensitivity_ISDA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ExpandedFra fraExp = FRA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.015;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValueSensitivity(FRA_TRADE, simpleProv), test.presentValueSensitivity(fraExp, simpleProv));
  }

  /**
   * Test present value sensitivity for NONE FRA discounting method. 
   */
  public void test_presentValueSensitivity_NONE() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ExpandedFra fraExp = FRA_NONE.expand();
    double forwardRate = 0.025;
    double discountRate = 0.01;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_NONE.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_NONE, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA_NONE, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_NONE.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for AFMA FRA discounting method. 
   */
  public void test_presentValueSensitivity_AFMA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(VAL_DATE, mockDf);

    ExpandedFra fraExp = FRA_AFMA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.025;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockDf.discountFactor(fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockDf.zeroRatePointSensitivity(fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), simpleProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate(), simpleProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, simpleProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_AFMA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA_AFMA, forwardRate, discountFactor, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_AFMA.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  /**
   * Test par rate for ISDA FRA Discounting method. 
   */
  public void test_parRate_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    Fra fra = createNewFra(FRA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par rate for NONE FRA Discounting method. 
   */
  public void test_parRate_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    Fra fra = createNewFra(FRA_NONE, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par rate for AFMA FRA Discounting method. 
   */
  public void test_parRate_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parRate = test.parRate(fraExp, prov);
    assertEquals(parRate, FORWARD_RATE);
    Fra fra = createNewFra(FRA_AFMA, parRate);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par spread for ISDA FRA Discounting method. 
   */
  public void test_parSpread_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    Fra fra = createNewFra(FRA, FRA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par spread for NONE FRA Discounting method. 
   */
  public void test_parSpread_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    Fra fra = createNewFra(FRA_NONE, FRA_NONE.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  /**
   * Test par spread for AFMA FRA Discounting method. 
   */
  public void test_parSpread_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    SimpleRatesProvider prov = createProvider(fraExp);

    DiscountingFraProductPricer test = DiscountingFraProductPricer.DEFAULT;
    double parSpread = test.parSpread(fraExp, prov);
    Fra fra = createNewFra(FRA_AFMA, FRA_AFMA.getFixedRate() + parSpread);
    CurrencyAmount pv = test.presentValue(fra, prov);
    assertEquals(pv.getAmount(), 0.0, TOLERANCE);
  }

  private static final double EPS_FD = 1E-7;
  private static final DiscountingFraProductPricer DEFAULT_PRICER = DiscountingFraProductPricer.DEFAULT;
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
    IMM_PROV = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .discountCurves(ImmutableMap.of(GBP, dscCurve))
        .indexCurves(ImmutableMap.of(GBP_LIBOR_3M, indexCurve))
        .build();
  }

  /**
   * Test par spread sensitivity for ISDA FRA Discounting method. 
   */
  public void test_parSpreadSensitivity_ISDA() {
    PointSensitivities sensi = DEFAULT_PRICER.parSpreadSensitivity(FRA, IMM_PROV);
    CurveCurrencyParameterSensitivities sensiComputed = IMM_PROV.curveParameterSensitivity(sensi);
    CurveCurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA.getCurrency(), DEFAULT_PRICER.parSpread(FRA, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
  }

  /**
   * Test par spread sensitivity for NONE FRA Discounting method. 
   */
  public void test_parSpreadSensitivity_NONE() {
    PointSensitivities sensi = DEFAULT_PRICER.parSpreadSensitivity(FRA_NONE, IMM_PROV);
    CurveCurrencyParameterSensitivities sensiComputed = IMM_PROV.curveParameterSensitivity(sensi);
    CurveCurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_NONE.getCurrency(), DEFAULT_PRICER.parSpread(FRA_NONE, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
  }

  /**
   * Test par spread sensitivity for AFMA FRA Discounting method. 
   */
  public void test_parSpreadSensitivity_AFMA() {
    PointSensitivities sensi = DEFAULT_PRICER.parSpreadSensitivity(FRA_AFMA, IMM_PROV);
    CurveCurrencyParameterSensitivities sensiComputed = IMM_PROV.curveParameterSensitivity(sensi);
    CurveCurrencyParameterSensitivities sensiExpected = CAL_FD.sensitivity(IMM_PROV,
        (p) -> CurrencyAmount.of(FRA_AFMA.getCurrency(), DEFAULT_PRICER.parSpread(FRA_AFMA, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, EPS_FD));
  }

  private Fra createNewFra(Fra product, double newFixedRate) {
    return Fra.builder()
        .buySell(product.getBuySell())
        .notional(product.getNotional())
        .startDate(product.getStartDate())
        .endDate(product.getEndDate())
        .index(product.getIndex())
        .fixedRate(newFixedRate)
        .currency(product.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Test cash flow for ISDA FRA Discounting method.
   */
  public void test_cashFlows_ISDA() {
    ExpandedFra fraExp = FRA.expand();
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
    assertEquals(testTrade.cashFlows(FRA_TRADE, prov), test.cashFlows(fraExp, prov));
  }

  //-------------------------------------------------------------------------
  /**
   * Test explain.
   */
  public void test_explainPresentValue_ISDA() {
    ExpandedFra fraExp = FRA.expand();
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
    assertEquals(explain.get(ExplainKey.ACCRUAL_DAYS).get(), (Integer) (int) daysBetween);
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), currency);
    assertEquals(explain.get(ExplainKey.NOTIONAL).get().getAmount(), fraExp.getNotional(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), fraExp.getNotional(), TOLERANCE);

    assertEquals(explain.get(ExplainKey.OBSERVATIONS).get().size(), 1);
    ExplainMap explainObs = explain.get(ExplainKey.OBSERVATIONS).get().get(0);
    IborRateObservation floatingRate = (IborRateObservation) fraExp.getFloatingRate();
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
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(ExpandedFra fraExp) {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE, mockDf);
    prov.setIborRates(mockIbor);

    IborRateObservation obs = (IborRateObservation) fraExp.getFloatingRate();
    IborRateSensitivity sens = IborRateSensitivity.of(obs.getIndex(), obs.getFixingDate(), 1d);
    when(mockIbor.ratePointSensitivity(obs.getFixingDate())).thenReturn(sens);
    when(mockIbor.rate(obs.getFixingDate())).thenReturn(FORWARD_RATE);

    when(mockDf.discountFactor(fraExp.getPaymentDate())).thenReturn(DISCOUNT_FACTOR);
    return prov;
  }

  //-------------------------------------------------------------------------
  private double forecastValueFwdSensitivity(Fra fra, double forwardRate, double eps) {

    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    ExpandedFra fraExp = fra.expand();
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).forecastValue(fraExp, provNew);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).forecastValue(fraExp, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double presentValueFwdSensitivity(Fra fra, double forwardRate, double discountFactor, double eps) {

    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    ExpandedFra fraExp = fra.expand();
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double dscSensitivity(
      Fra fra, double forwardRate, double discountFactor, double paymentTime, double eps) {

    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VAL_DATE);
    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    ExpandedFra fraExp = fra.expand();
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate);
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    CurrencyAmount upDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(eps * paymentTime));
    CurrencyAmount downDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    return upDscValue.minus(downDscValue).multipliedBy(0.5 / eps).getAmount();
  }

}

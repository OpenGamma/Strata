/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.sensitivity.BondFutureOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.GenericVolatilitySurfaceYearFractionMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.bond.BondFutureOption;

/**
 * Test {@link BlackBondFutureOptionMarginedProductPricer}.
 */
@Test
public class BlackBondFutureOptionMarginedProductPricerTest {
  // product
  private static final StandardId FUTURE_SECURITY_ID = BondDataSets.FUTURE_SECURITY_ID_EUR;
  private static final BondFutureOption FUTURE_OPTION_PRODUCT = BondDataSets.FUTURE_OPTION_PRODUCT_EUR_116;
  // curves
  private static final LegalEntityDiscountingProvider RATE_PROVIDER =
      LegalEntityDiscountingProviderDataSets.ISSUER_REPO_ZERO_EUR;
  // vol surface
  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIME = DoubleArray.of(0.20, 0.20, 0.20, 0.20, 0.20, 0.45, 0.45, 0.45, 0.45, 0.45);
  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.050, -0.005, 0.000, 0.005, 0.050, -0.050, -0.005, 0.000, 0.005, 0.050);
  private static final DoubleArray VOL = DoubleArray.of(0.50, 0.49, 0.47, 0.48, 0.51, 0.45, 0.44, 0.42, 0.43, 0.46);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionMetadata> list = new ArrayList<GenericVolatilitySurfaceYearFractionMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionMetadata parameterMetadata = GenericVolatilitySurfaceYearFractionMetadata.of(
          TIME.get(i), LogMoneynessStrike.of(MONEYNESS.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final LocalDate VALUATION_DATE = RATE_PROVIDER.getValuationDate();
  private static final LocalTime VALUATION_TIME = LocalTime.of(0, 0);
  private static final ZoneId ZONE = FUTURE_OPTION_PRODUCT.getExpiryZone();
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final BlackVolatilityExpLogMoneynessBondFutureProvider VOL_PROVIDER =
      BlackVolatilityExpLogMoneynessBondFutureProvider.of(SURFACE, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
  private static final double TOL = 1.0E-13;
  private static final double EPS = 1.0e-6;
  // pricer
  private static final DiscountingBondFutureProductPricer FUTURE_PRICER = DiscountingBondFutureProductPricer.DEFAULT;
  private static final BlackBondFutureOptionMarginedProductPricer OPTION_PRICER =
      new BlackBondFutureOptionMarginedProductPricer(FUTURE_PRICER);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_getFuturePricer() {
    assertSame(OPTION_PRICER.getFuturePricer(), FUTURE_PRICER);
  }

  public void test_price() {
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.price(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_price_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.price(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_price_from_generic_provider() {
    BondFutureProvider volProvider = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
        SURFACE, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, volProvider);
    double expected = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_delta() {
    double computed = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.delta(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_delta_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.deltaStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.delta(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_gamma() {
    double computed = OPTION_PRICER.gammaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.gamma(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_gamma_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.gammaStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.gamma(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_theta() {
    double computed = OPTION_PRICER.theta(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.driftlessTheta(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_theta_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.theta(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.driftlessTheta(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivity() {
    PointSensitivities point = OPTION_PRICER.priceSensitivityStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER,
        (p) -> CurrencyAmount.of(EUR, OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, (p), VOL_PROVIDER)));
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double logMoneynessUp = Math.log(strike / (futurePrice + EPS));
    double logMoneynessDw = Math.log(strike / (futurePrice - EPS));
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double volUp = SURFACE.zValue(expiryTime, logMoneynessUp);
    double volDw = SURFACE.zValue(expiryTime, logMoneynessDw);
    double volSensi = 0.5 * (volUp - volDw) / EPS;
    double vega = BlackFormulaRepository.vega(futurePrice, strike, expiryTime, vol);
    CurveCurrencyParameterSensitivities sensiVol = RATE_PROVIDER.curveParameterSensitivity(
        FUTURE_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER)).multipliedBy(-vega * volSensi);
    expected = expected.combinedWith(sensiVol);
    assertTrue(computed.equalWithTolerance(expected, 30d * EPS));
  }

  public void test_priceSensitivity_from_future_price() {
    double futurePrice = 1.1d;
    PointSensitivities point = OPTION_PRICER.priceSensitivityStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point);
    double delta = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    CurveCurrencyParameterSensitivities expected = RATE_PROVIDER.curveParameterSensitivity(
        FUTURE_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER)).multipliedBy(delta);
    assertTrue(computed.equalWithTolerance(expected, TOL));
  }

  public void test_priceSensitivity_from_generic_provider() {
    BondFutureProvider volProvider = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
        SURFACE, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
    PointSensitivities expected = OPTION_PRICER.priceSensitivityStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivities computed = OPTION_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, volProvider);
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivityBlackVolatility() {
    BondFutureOptionSensitivity sensi = OPTION_PRICER.priceSensitivityBlackVolatility(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    testPriceSensitivityBlackVolatility(
        VOL_PROVIDER.surfaceCurrencyParameterSensitivity(sensi),
        (p) -> OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, (p)));
  }

  public void test_priceSensitivityBlackVolatility_from_future_price() {
    double futurePrice = 1.1d;
    BondFutureOptionSensitivity sensi = OPTION_PRICER.priceSensitivityBlackVolatility(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    testPriceSensitivityBlackVolatility(
        VOL_PROVIDER.surfaceCurrencyParameterSensitivity(sensi),
        (p) -> OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, (p), futurePrice));
  }

  private void testPriceSensitivityBlackVolatility(
      SurfaceCurrencyParameterSensitivity computed,
      Function<BlackVolatilityBondFutureProvider, Double> valueFn) {
    List<SurfaceParameterMetadata> list = computed.getMetadata().getParameterMetadata().get();
    int nVol = VOL.size();
    assertEquals(list.size(), nVol);
    for (int i = 0; i < nVol; ++i) {
      double[] volUp = Arrays.copyOf(VOL.toArray(), nVol);
      double[] volDw = Arrays.copyOf(VOL.toArray(), nVol);
      volUp[i] += EPS;
      volDw[i] -= EPS;
      InterpolatedNodalSurface sfUp = InterpolatedNodalSurface.of(
          METADATA, TIME, MONEYNESS, DoubleArray.copyOf(volUp), INTERPOLATOR_2D);
      InterpolatedNodalSurface sfDw = InterpolatedNodalSurface.of(
          METADATA, TIME, MONEYNESS, DoubleArray.copyOf(volDw), INTERPOLATOR_2D);
      BlackVolatilityExpLogMoneynessBondFutureProvider provUp =
          BlackVolatilityExpLogMoneynessBondFutureProvider.of(sfUp, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
      BlackVolatilityExpLogMoneynessBondFutureProvider provDw =
          BlackVolatilityExpLogMoneynessBondFutureProvider.of(sfDw, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
      double expected = 0.5 * (valueFn.apply(provUp) - valueFn.apply(provDw)) / EPS;
      int index = -1;
      for (int j = 0; j < nVol; ++j) {
        GenericVolatilitySurfaceYearFractionMetadata meta = (GenericVolatilitySurfaceYearFractionMetadata) list.get(j);
        if (meta.getYearFraction() == TIME.get(i) && meta.getStrike().getValue() == MONEYNESS.get(i)) {
          index = j;
          continue;
        }
      }
      assertEquals(computed.getSensitivity().get(index), expected, EPS);
    }
  }

  //-------------------------------------------------------------------------
  public void test_marginIndex() {
    double price = 0.12d;
    double computed = OPTION_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, price);
    assertEquals(computed, price * FUTURE_OPTION_PRODUCT.getUnderlying().getNotional());
  }

  public void test_marginIndexSensitivity() {
    PointSensitivities point = OPTION_PRICER.priceSensitivityStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivities computed = OPTION_PRICER.marginIndexSensitivity(FUTURE_OPTION_PRODUCT, point);
    assertEquals(computed, point.multipliedBy(FUTURE_OPTION_PRODUCT.getUnderlying().getNotional()));
  }

  //-------------------------------------------------------------------------
  public void regression_price() {
    double price = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(price, 0.08916005173932573, TOL); // 2.x
  }
}

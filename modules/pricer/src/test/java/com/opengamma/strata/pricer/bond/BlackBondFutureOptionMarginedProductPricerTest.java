/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.bond.ResolvedBondFutureOption;

/**
 * Test {@link BlackBondFutureOptionMarginedProductPricer}.
 */
@Test
public class BlackBondFutureOptionMarginedProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // product
  private static final ResolvedBondFutureOption FUTURE_OPTION_PRODUCT =
      BondDataSets.FUTURE_OPTION_PRODUCT_EUR_116.resolve(REF_DATA);
  // curves
  private static final LegalEntityDiscountingProvider RATE_PROVIDER =
      LegalEntityDiscountingProviderDataSets.ISSUER_REPO_ZERO_EUR;
  // vol surface
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME = DoubleArray.of(0.20, 0.20, 0.20, 0.20, 0.20, 0.45, 0.45, 0.45, 0.45, 0.45);
  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.050, -0.005, 0.000, 0.005, 0.050, -0.050, -0.005, 0.000, 0.005, 0.050);
  private static final DoubleArray VOL = DoubleArray.of(0.50, 0.49, 0.47, 0.48, 0.51, 0.45, 0.44, 0.42, 0.43, 0.46);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionParameterMetadata> list = new ArrayList<GenericVolatilitySurfaceYearFractionParameterMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionParameterMetadata parameterMetadata = GenericVolatilitySurfaceYearFractionParameterMetadata.of(
          TIME.get(i), LogMoneynessStrike.of(MONEYNESS.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = DefaultSurfaceMetadata.builder()
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .parameterMetadata(list)
        .dayCount(ACT_365F)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = RATE_PROVIDER.getValuationDate();
  private static final LocalTime VAL_TIME = LocalTime.of(0, 0);
  private static final ZoneId ZONE = FUTURE_OPTION_PRODUCT.getExpiry().getZone();
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  private static final BlackBondFutureExpiryLogMoneynessVolatilities VOLS =
      BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, SURFACE);
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
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.price(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_price_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.price(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_price_from_generic_provider() {
    BondFutureVolatilities vols = BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, SURFACE);
    double computed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, vols);
    double expected = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_delta() {
    double computed = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.delta(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_delta_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.deltaStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.delta(futurePrice, strike, expiryTime, vol, true);
    assertEquals(computed, expected, TOL);
  }

  public void test_gamma() {
    double computed = OPTION_PRICER.gammaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.gamma(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_gamma_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.gammaStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.gamma(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_theta() {
    double computed = OPTION_PRICER.theta(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.driftlessTheta(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  public void test_theta_from_future_price() {
    double futurePrice = 1.1d;
    double computed = OPTION_PRICER.theta(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double expected = BlackFormulaRepository.driftlessTheta(futurePrice, strike, expiryTime, vol);
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivity() {
    PointSensitivities point = OPTION_PRICER.priceSensitivityRatesStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computed = RATE_PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER,
        (p) -> CurrencyAmount.of(EUR, OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, (p), VOLS)));
    double futurePrice = FUTURE_PRICER.price(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER);
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, FUTURE_OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double logMoneynessUp = Math.log(strike / (futurePrice + EPS));
    double logMoneynessDw = Math.log(strike / (futurePrice - EPS));
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double volUp = SURFACE.zValue(expiryTime, logMoneynessUp);
    double volDw = SURFACE.zValue(expiryTime, logMoneynessDw);
    double volSensi = 0.5 * (volUp - volDw) / EPS;
    double vega = BlackFormulaRepository.vega(futurePrice, strike, expiryTime, vol);
    CurrencyParameterSensitivities sensiVol = RATE_PROVIDER.parameterSensitivity(
            FUTURE_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER)).multipliedBy(
            -vega * volSensi);
    expected = expected.combinedWith(sensiVol);
    assertTrue(computed.equalWithTolerance(expected, 30d * EPS));
  }

  public void test_priceSensitivity_from_future_price() {
    double futurePrice = 1.1d;
    PointSensitivities point = OPTION_PRICER.priceSensitivityRatesStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    CurrencyParameterSensitivities computed = RATE_PROVIDER.parameterSensitivity(point);
    double delta = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(
        FUTURE_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT.getUnderlyingFuture(), RATE_PROVIDER)).multipliedBy(delta);
    assertTrue(computed.equalWithTolerance(expected, TOL));
  }

  public void test_priceSensitivity_from_generic_provider() {
    BondFutureVolatilities volProvider = BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, SURFACE);
    PointSensitivities expected = OPTION_PRICER.priceSensitivityRatesStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    PointSensitivities computed = OPTION_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, volProvider);
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivityBlackVolatility() {
    BondFutureOptionSensitivity sensi = OPTION_PRICER.priceSensitivityModelParamsVolatility(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    testPriceSensitivityBlackVolatility(
        VOLS.parameterSensitivity(sensi),
        (p) -> OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, (p)));
  }

  public void test_priceSensitivityBlackVolatility_from_future_price() {
    double futurePrice = 1.1d;
    BondFutureOptionSensitivity sensi = OPTION_PRICER.priceSensitivityModelParamsVolatility(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS, futurePrice);
    testPriceSensitivityBlackVolatility(
        VOLS.parameterSensitivity(sensi),
        (p) -> OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, (p), futurePrice));
  }

  private void testPriceSensitivityBlackVolatility(
      CurrencyParameterSensitivities computed,
      Function<BlackBondFutureVolatilities, Double> valueFn) {
    List<ParameterMetadata> list = computed.getSensitivities().get(0).getParameterMetadata();
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
      BlackBondFutureExpiryLogMoneynessVolatilities provUp =
          BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, sfUp);
      BlackBondFutureExpiryLogMoneynessVolatilities provDw =
          BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, sfDw);
      double expected = 0.5 * (valueFn.apply(provUp) - valueFn.apply(provDw)) / EPS;
      int index = -1;
      for (int j = 0; j < nVol; ++j) {
        GenericVolatilitySurfaceYearFractionParameterMetadata meta = (GenericVolatilitySurfaceYearFractionParameterMetadata) list.get(j);
        if (meta.getYearFraction() == TIME.get(i) && meta.getStrike().getValue() == MONEYNESS.get(i)) {
          index = j;
          continue;
        }
      }
      assertEquals(computed.getSensitivities().get(0).getSensitivity().get(index), expected, EPS);
    }
  }

  //-------------------------------------------------------------------------
  public void test_marginIndex() {
    double price = 0.12d;
    double computed = OPTION_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, price);
    assertEquals(computed, price * FUTURE_OPTION_PRODUCT.getUnderlyingFuture().getNotional());
  }

  public void test_marginIndexSensitivity() {
    PointSensitivities point = OPTION_PRICER.priceSensitivityRatesStickyStrike(
        FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    PointSensitivities computed = OPTION_PRICER.marginIndexSensitivity(FUTURE_OPTION_PRODUCT, point);
    assertEquals(computed, point.multipliedBy(FUTURE_OPTION_PRODUCT.getUnderlyingFuture().getNotional()));
  }

  //-------------------------------------------------------------------------
  public void regression_price() {
    double price = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, RATE_PROVIDER, VOLS);
    assertEquals(price, 0.08916005173932573, TOL); // 2.x
  }
}

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.testng.Assert.assertEquals;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.sensitivity.BondFutureOptionSensitivity;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.GenericVolatilitySurfaceYearFractionMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.rate.bond.BondFutureOption;
import com.opengamma.strata.product.rate.bond.BondFutureOptionTrade;

/**
 * Test {@link BlackBondFutureOptionMarginedTradePricer}.
 */
@Test
public class BlackBondFutureOptionMarginedTradePricerTest {
  // product and trade
  private static final BondFutureOption OPTION_PRODUCT = BondDataSets.FUTURE_OPTION_PRODUCT_EUR_115;
  private static final BondFutureOptionTrade OPTION_TRADE = BondDataSets.FUTURE_OPTION_TRADE_EUR;
  private static final StandardId FUTURE_SECURITY_ID = BondDataSets.FUTURE_SECURITY_ID_EUR;
  private static final double NOTIONAL = BondDataSets.NOTIONAL_EUR;
  private static final long QUANTITY = BondDataSets.QUANTITY_EUR;
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
  private static final ZoneId ZONE = OPTION_PRODUCT.getExpiryZone();
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final BlackVolatilityExpLogMoneynessBondFutureProvider VOL_PROVIDER =
      BlackVolatilityExpLogMoneynessBondFutureProvider.of(SURFACE, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
  private static final double REFERENCE_PRICE = 0.01;

  private static final double TOL = 1.0E-13;
  private static final double EPS = 1.0e-6;
  // pricer
  private static final DiscountingBondFutureProductPricer FUTURE_PRICER = DiscountingBondFutureProductPricer.DEFAULT;
  private static final BlackBondFutureOptionMarginedProductPricer OPTION_PRODUCT_PRICER =
      new BlackBondFutureOptionMarginedProductPricer(FUTURE_PRICER);
  private static final BlackBondFutureOptionMarginedTradePricer OPTION_TRADE_PRICER =
      new BlackBondFutureOptionMarginedTradePricer(OPTION_PRODUCT_PRICER);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_presentValue() {
    CurrencyAmount computed =
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER, REFERENCE_PRICE);
    double expected = (OPTION_PRODUCT_PRICER.price(OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER) - REFERENCE_PRICE)
        * NOTIONAL * QUANTITY;
    assertEquals(computed.getCurrency(), Currency.EUR);
    assertEquals(computed.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  public void test_presentValue_from_future_price() {
    double futurePrice = 0.975d;
    CurrencyAmount computed =
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER, futurePrice, REFERENCE_PRICE);
    double expected = NOTIONAL * QUANTITY *
        (OPTION_PRODUCT_PRICER.price(OPTION_PRODUCT, RATE_PROVIDER, VOL_PROVIDER, futurePrice) - REFERENCE_PRICE);
    assertEquals(computed.getCurrency(), Currency.EUR);
    assertEquals(computed.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  public void test_presentValue_from_prices_date() {
    double currentPrice = 0.0325;
    double lastClosingPrice = 0.03;
    LocalDate valuationDate1 = LocalDate.of(2014, 3, 30); // before trade date
    CurrencyAmount computed1 =
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, valuationDate1, currentPrice, lastClosingPrice);
    double expected2 = NOTIONAL * QUANTITY * (currentPrice - lastClosingPrice);
    assertEquals(computed1.getCurrency(), Currency.EUR);
    assertEquals(computed1.getAmount(), expected2, TOL * NOTIONAL * QUANTITY);
    LocalDate valuationDate2 = LocalDate.of(2014, 3, 31); // equal to trade date
    CurrencyAmount computed2 =
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, valuationDate2, currentPrice, lastClosingPrice);
    double expected = NOTIONAL * QUANTITY * (currentPrice - OPTION_TRADE.getInitialPrice().getAsDouble());
    assertEquals(computed2.getCurrency(), Currency.EUR);
    assertEquals(computed2.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityBlackVolatility() {
    BondFutureOptionSensitivity sensi = OPTION_TRADE_PRICER.presentValueSensitivityBlackVolatility(
        OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER);
    testPriceSensitivityBlackVolatility(VOL_PROVIDER.surfaceCurrencyParameterSensitivity(sensi),
        (p) -> OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, RATE_PROVIDER, (p), REFERENCE_PRICE).getAmount());
  }

  public void test_presentValueSensitivityBlackVolatility_from_future_price() {
    double futurePrice = 0.975d;
    BondFutureOptionSensitivity sensi = OPTION_TRADE_PRICER.presentValueSensitivityBlackVolatility(
        OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER, futurePrice);
    testPriceSensitivityBlackVolatility(VOL_PROVIDER.surfaceCurrencyParameterSensitivity(sensi), (p) ->
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, RATE_PROVIDER, (p), futurePrice, REFERENCE_PRICE).getAmount());
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
      assertEquals(computed.getSensitivity().get(index), expected, EPS * NOTIONAL * QUANTITY);
    }
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivities point = OPTION_TRADE_PRICER.presentValueSensitivity(OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point);
    double futurePrice = FUTURE_PRICER.price(OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER);
    double strike = OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, OPTION_PRODUCT.getExpiryDate());
    double logMoneyness = Math.log(strike / futurePrice);
    double logMoneynessUp = Math.log(strike / (futurePrice + EPS));
    double logMoneynessDw = Math.log(strike / (futurePrice - EPS));
    double vol = SURFACE.zValue(expiryTime, logMoneyness);
    double volUp = SURFACE.zValue(expiryTime, logMoneynessUp);
    double volDw = SURFACE.zValue(expiryTime, logMoneynessDw);
    double volSensi = 0.5 * (volUp - volDw) / EPS;
    double vega = BlackFormulaRepository.vega(futurePrice, strike, expiryTime, vol);
    CurveCurrencyParameterSensitivities sensiVol = RATE_PROVIDER.curveParameterSensitivity(
        FUTURE_PRICER.priceSensitivity(OPTION_PRODUCT.getUnderlying(), RATE_PROVIDER))
        .multipliedBy(-vega * volSensi * NOTIONAL * QUANTITY);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER,
        (p) -> OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, (p), VOL_PROVIDER, REFERENCE_PRICE));
    assertTrue(computed.equalWithTolerance(expected.combinedWith(sensiVol), 30d * EPS * NOTIONAL * QUANTITY));
  }

  //-------------------------------------------------------------------------
  // regression to 2.x
  public void regression() {
    CurrencyAmount pv =
        OPTION_TRADE_PRICER.presentValue(OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER, REFERENCE_PRICE);
    assertEquals(pv.getAmount(), 1.0044656145806769E7, TOL * NOTIONAL * QUANTITY);
    double[] sensiRepoExpected = new double[] {9266400.007519504, 6037835.299017232, 0.0, 0.0, 0.0, 0.0 };
    double[] sensiIssuerExpected = new double[]
    {0.0, -961498.734103331, -2189527.424010516, -3.7783587809228E7, -3.025330833183195E8, 0.0 };
    PointSensitivities point = OPTION_TRADE_PRICER.presentValueSensitivity(OPTION_TRADE, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvSensi = RATE_PROVIDER.curveParameterSensitivity(point);
    double[] sensiIssuerComputed = pvSensi.getSensitivities().get(0).getSensitivity().toArray();
    double[] sensiRepoComputed = pvSensi.getSensitivities().get(1).getSensitivity().toArray();
    assertEquals(sensiRepoComputed.length, sensiRepoExpected.length);
    assertEquals(sensiIssuerComputed.length, sensiIssuerExpected.length);
    for (int i = 0; i < 6; ++i) {
      assertEquals(sensiRepoComputed[i], sensiRepoExpected[i], TOL * NOTIONAL * QUANTITY);
      assertEquals(sensiIssuerComputed[i], sensiIssuerExpected[i], TOL * NOTIONAL * QUANTITY);
    }
  }
}

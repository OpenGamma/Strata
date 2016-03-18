/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.IborFutureOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;

/**
 * Tests {@link NormalIborFutureOptionMarginedProductPricer}
 */
@Test
public class NormalIborFutureOptionMarginedProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01);
  private static final DoubleArray NORMAL_VOL =
      DoubleArray.of(0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      DefaultSurfaceMetadata.of("Test"), TIMES, MONEYNESS_PRICES, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

  private static final NormalVolatilityIborFutureProvider VOL_SIMPLE_MONEY_PRICE =
      NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
          PARAMETERS_PRICE, true, GBP_LIBOR_2M, ACT_365F, VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE));

  private static final ResolvedIborFutureOption OPTION = IborFutureDummyData.IBOR_FUTURE_OPTION_2.resolve(REF_DATA);

  private static final double RATE = 0.015;

  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  private static final DiscountingIborFutureProductPricer FUTURE_PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final NormalIborFutureOptionMarginedProductPricer OPTION_PRICER =
      new NormalIborFutureOptionMarginedProductPricer(FUTURE_PRICER);

  private static final double TOLERANCE_PRICE = 1.0E-10;
  private static final double TOLERANCE_PRICE_DELTA = 1.0E-8;

  // ----------     price     ----------
  public void price_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiryTime, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionPriceExpected = NORMAL_FUNCTION.getPriceFunction(option).apply(normalPoint);
    double optionPriceComputed = OPTION_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionPriceComputed, optionPriceExpected, TOLERANCE_PRICE);
  }

  public void price_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 1.0 - RATE;
    double optionPriceExpected = OPTION_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionPriceComputed = OPTION_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertEquals(optionPriceComputed, optionPriceExpected, TOLERANCE_PRICE);
  }

  // ----------     delta     ----------
  public void delta_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiryTime, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionDeltaExpected = NORMAL_FUNCTION.getDelta(option, normalPoint);
    double optionDeltaComputed =
        OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionDeltaComputed, optionDeltaExpected, TOLERANCE_PRICE);
  }

  public void delta_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 1.0 - RATE;
    double optionDeltaExpected =
        OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionDeltaComputed = OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertEquals(optionDeltaComputed, optionDeltaExpected, TOLERANCE_PRICE);
  }

  // ----------     priceSensitivity     ----------
  public void priceSensitivityStickyStrike_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 0.9875;
    PointSensitivities futurePriceSensitivity =
        FUTURE_PRICER.priceSensitivity(OPTION.getUnderlyingFuture(), prov);
    double delta = OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed =
        OPTION_PRICER.priceSensitivityStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA));
  }

  public void priceSensitivityStickyStrike_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    PointSensitivities futurePriceSensitivity = OPTION_PRICER.getFuturePricer()
        .priceSensitivity(OPTION.getUnderlyingFuture(), prov);
    double delta = OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed =
        OPTION_PRICER.priceSensitivityStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA));
    PointSensitivities optionPriceSensitivityComputed2 =
        OPTION_PRICER.priceSensitivity(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed2, TOLERANCE_PRICE_DELTA));
  }

  // ----------     priceSensitivityNormalVolatility     ----------
  public void priceSensitivityNormalVolatility_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiryTime, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionVegaExpected = NORMAL_FUNCTION.getVega(option, normalPoint);
    IborFutureOptionSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityNormalVolatility(
        OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionVegaComputed.getSensitivity(), optionVegaExpected, TOLERANCE_PRICE);
    assertEquals(optionVegaComputed.getExpiry(), OPTION.getExpiry());
    assertEquals(optionVegaComputed.getFixingDate(), OPTION.getUnderlyingFuture().getIborRate().getObservation().getFixingDate());
    assertEquals(optionVegaComputed.getStrikePrice(), OPTION.getStrikePrice());
    assertEquals(optionVegaComputed.getFuturePrice(), futurePrice);
  }

  public void priceSensitivityNormalVolatility_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 1.0 - RATE;
    IborFutureOptionSensitivity optionVegaExpected = OPTION_PRICER.priceSensitivityNormalVolatility(
        OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    IborFutureOptionSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityNormalVolatility(
        OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionVegaExpected.compareKey(optionVegaComputed) == 0);
    assertEquals(optionVegaComputed.getSensitivity(), optionVegaExpected.getSensitivity(), TOLERANCE_PRICE_DELTA);
  }

}

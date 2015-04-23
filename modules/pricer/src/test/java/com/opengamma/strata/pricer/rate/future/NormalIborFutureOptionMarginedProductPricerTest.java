/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

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

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.option.IborFutureOptionSensitivityKey;
import com.opengamma.strata.pricer.sensitivity.option.OptionPointSensitivity;

/**
 * Tests {@link NormalIborFutureOptionMarginedProductPricer}
 */
public class NormalIborFutureOptionMarginedProductPricerTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final double[] TIMES =
      new double[] {0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00 };
  private static final double[] MONEYNESS_PRICES =
      new double[] {-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01 };
  private static final double[] NORMAL_VOL =
      new double[] {0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014 };
  private static final InterpolatedDoublesSurface PARAMETERS_PRICE =
      new InterpolatedDoublesSurface(TIMES, MONEYNESS_PRICES, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId VALUATION_ZONE = ZoneId.of("Europe/London");
  
  private static final NormalVolatilityIborFutureParameters VOL_SIMPLE_MONEY_PRICE =
      new NormalVolatilityExpSimpleMoneynessIborFutureParameters(
          PARAMETERS_PRICE, true, GBP_LIBOR_2M, ACT_365F, VALUATION_DATE, VALUATION_TIME, VALUATION_ZONE);
  
  private static final IborFutureOption FUTURE_OPTION_PRODUCT = IborFutureDummyData.IBOR_FUTURE_OPTION_2;
  
  private static final double RATE = 0.015;
  private static final PricingEnvironment ENV_MOCK = mock(PricingEnvironment.class);
  static {
    when(ENV_MOCK.iborIndexRate(FUTURE_OPTION_PRODUCT.getUnderlying().getProduct().getIndex(), 
        FUTURE_OPTION_PRODUCT.getUnderlying().getProduct().getLastTradeDate())).thenReturn(RATE);
  }

  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  private static final DiscountingIborFutureProductPricer FUTURE_PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final NormalIborFutureOptionMarginedProductPricer OPTION_PRICER = 
      new NormalIborFutureOptionMarginedProductPricer(FUTURE_PRICER);
  
  private static final double TOLERANCE_PRICE = 1.0E-10;
  private static final double TOLERANCE_PRICE_DELTA = 1.0E-8;
  
  // ----------     price     ----------
  @Test
  public void price_from_future_price() {
    double futurePrice = 0.9875;
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpirationDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.getZValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiryTime, 
        FUTURE_OPTION_PRODUCT.getPutCall().equals(PutCall.CALL));
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, normalVol);
    double optionPriceExpected = NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
    double optionPriceComputed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionPriceComputed, optionPriceExpected, TOLERANCE_PRICE);
  }
  
  @Test
  public void price_from_env() {
    double futurePrice = 1.0 - RATE;
    double optionPriceExpected = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionPriceComputed = OPTION_PRICER.price(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertEquals(optionPriceComputed, optionPriceExpected, TOLERANCE_PRICE);
  }
  
  // ----------     delta     ----------
  @Test
  public void delta_from_future_price() {
    double futurePrice = 0.9875;
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpirationDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.getZValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiryTime, 
        FUTURE_OPTION_PRODUCT.getPutCall().equals(PutCall.CALL));
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, normalVol);
    double optionDeltaExpected = NORMAL_FUNCTION.getDelta(option, normalPoint);
    double optionDeltaComputed = 
        OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionDeltaComputed, optionDeltaExpected, TOLERANCE_PRICE);
  }
  
  @Test
  public void delta_from_env() {
    double futurePrice = 1.0 - RATE;
    double optionDeltaExpected = 
        OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionDeltaComputed = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertEquals(optionDeltaComputed, optionDeltaExpected, TOLERANCE_PRICE);
  }
  
  // ----------     priceSensitivity     ----------
  @Test
  public void priceSensitivityStickyStrike_from_future_price() {
    double futurePrice = 0.9875;
    PointSensitivities futurePriceSensitivity = 
        FUTURE_PRICER.priceSensitivity(ENV_MOCK, FUTURE_OPTION_PRODUCT.getUnderlying().getProduct());
    double delta = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed = 
        OPTION_PRICER.priceSensitivityStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA));
  }
  
  @Test
  public void priceSensitivityStickyStrike_from_env() {
    PointSensitivities futurePriceSensitivity = OPTION_PRICER.getFuturePricerFn()
        .priceSensitivity(ENV_MOCK, FUTURE_OPTION_PRODUCT.getUnderlying().getProduct());
    double delta = OPTION_PRICER.deltaStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed = 
        OPTION_PRICER.priceSensitivityStickyStrike(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA));
    PointSensitivities optionPriceSensitivityComputed2 = 
        OPTION_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed2, TOLERANCE_PRICE_DELTA));
  }

  // ----------     priceSensitivityNormalVolatility     ----------
  @Test
  public void priceSensitivityNormalVolatility_from_future_price() {
    double futurePrice = 0.9875;
    double strike = FUTURE_OPTION_PRODUCT.getStrikePrice();
    double expiryTime = ACT_365F.relativeYearFraction(VALUATION_DATE, FUTURE_OPTION_PRODUCT.getExpirationDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.getZValue(expiryTime, priceSimpleMoneyness);
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiryTime,
        FUTURE_OPTION_PRODUCT.getPutCall().equals(PutCall.CALL));
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, normalVol);
    double optionVegaExpected = NORMAL_FUNCTION.getVega(option, normalPoint);
    OptionPointSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityNormalVolatility(
        FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertEquals(optionVegaComputed.getValue(), optionVegaExpected, TOLERANCE_PRICE);
    assertTrue(optionVegaComputed.getKey() instanceof IborFutureOptionSensitivityKey);
    IborFutureOptionSensitivityKey key = (IborFutureOptionSensitivityKey) optionVegaComputed.getKey();
    assertEquals(key.getExpiryDate(), FUTURE_OPTION_PRODUCT.getExpirationDate());
    assertEquals(key.getFixingDate(), FUTURE_OPTION_PRODUCT.getUnderlying().getProduct().getFixingDate());
    assertEquals(key.getStrikePrice(), FUTURE_OPTION_PRODUCT.getStrikePrice());
    assertEquals(key.getFuturePrice(), futurePrice);
  }

  @Test
  public void priceSensitivityNormalVolatility_from_env() {
    double futurePrice = 1.0 - RATE;
    OptionPointSensitivity optionVegaExpected = OPTION_PRICER.priceSensitivityNormalVolatility(
        FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    OptionPointSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityNormalVolatility(
        FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(optionVegaExpected.equalWithTolerance(optionVegaComputed, TOLERANCE_PRICE_DELTA));
  }
  
}

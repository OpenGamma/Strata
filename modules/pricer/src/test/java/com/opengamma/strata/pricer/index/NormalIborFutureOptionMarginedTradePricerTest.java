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

import com.opengamma.strata.basics.currency.CurrencyAmount;
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
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Tests {@link NormalIborFutureOptionMarginedTradePricer}
 */
@Test
public class NormalIborFutureOptionMarginedTradePricerTest {

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
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final ResolvedIborFutureOptionTrade FUTURE_OPTION_TRADE_TD = ResolvedIborFutureOptionTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(VAL_DATE)
          .build())
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
      .build();
  private static final ResolvedIborFutureOptionTrade FUTURE_OPTION_TRADE = ResolvedIborFutureOptionTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(TRADE_DATE)
          .build())
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
      .build();

  private static final double RATE = 0.015;

  private static final DiscountingIborFutureProductPricer FUTURE_PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final NormalIborFutureOptionMarginedProductPricer OPTION_PRODUCT_PRICER =
      new NormalIborFutureOptionMarginedProductPricer(FUTURE_PRICER);
  private static final NormalIborFutureOptionMarginedTradePricer OPTION_TRADE_PRICER =
      new NormalIborFutureOptionMarginedTradePricer(OPTION_PRODUCT_PRICER);

  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E-1;

  // ----------     present value     ----------

  public void presentValue_from_option_price_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE_TD, VAL_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, TRADE_PRICE)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void presentValue_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double futurePrice = 0.9875;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice, lastClosingPrice);
    double optionPrice =
        OPTION_PRODUCT_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, lastClosingPrice)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void presentValue_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE, lastClosingPrice);
    double optionPrice =
        OPTION_PRODUCT_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, lastClosingPrice)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  // ----------     present value sensitivity     ----------

  public void presentValueSensitivity_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    PointSensitivities psProduct =
        OPTION_PRODUCT_PRICER.priceSensitivity(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities psExpected = psProduct
        .multipliedBy(OPTION_PRODUCT_PRICER.marginIndex(OPTION, 1) * OPTION_QUANTITY);
    PointSensitivities psComputed = OPTION_TRADE_PRICER
        .presentValueSensitivity(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_PV_DELTA));
  }

  // ----------     present value normal vol sensitivity     ----------

  public void presentvalue_normalVolSensitivity_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    IborFutureOptionSensitivity psProduct =
        OPTION_PRODUCT_PRICER.priceSensitivityNormalVolatility(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    IborFutureOptionSensitivity psExpected = psProduct.withSensitivity(
        psProduct.getSensitivity() * OPTION_PRODUCT_PRICER.marginIndex(OPTION, 1) * OPTION_QUANTITY);
    IborFutureOptionSensitivity psComputed = OPTION_TRADE_PRICER
        .presentValueSensitivityNormalVolatility(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(psExpected.compareKey(psComputed) == 0);
    assertEquals(psComputed.getSensitivity(), psExpected.getSensitivity(), TOLERANCE_PV_DELTA);
  }

}

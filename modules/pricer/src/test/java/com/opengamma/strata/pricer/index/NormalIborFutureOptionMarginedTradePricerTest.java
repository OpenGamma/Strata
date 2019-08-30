/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.model.MoneynessType;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.TradedPrice;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Tests {@link NormalIborFutureOptionMarginedTradePricer}
 */
public class NormalIborFutureOptionMarginedTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.01, 0.0, 0.01, -0.02, -0.01, 0.0, 0.01, -0.02, -0.01, 0.0, 0.01);
  private static final DoubleArray NORMAL_VOL =
      DoubleArray.of(0.01, 0.011, 0.012, 0.010, 0.011, 0.012, 0.013, 0.012, 0.012, 0.013, 0.014, 0.014);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      Surfaces.normalVolatilityByExpirySimpleMoneyness("Test", ACT_365F, MoneynessType.PRICE),
      TIMES,
      MONEYNESS_PRICES,
      NORMAL_VOL,
      INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

  private static final NormalIborFutureOptionVolatilities VOL_SIMPLE_MONEY_PRICE =
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(
          GBP_LIBOR_2M, VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE), PARAMETERS_PRICE);

  private static final ResolvedIborFutureOption OPTION = IborFutureDummyData.IBOR_FUTURE_OPTION_2.resolve(REF_DATA);
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final ResolvedIborFutureOptionTrade FUTURE_OPTION_TRADE_TD = ResolvedIborFutureOptionTrade.builder()
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .tradedPrice(TradedPrice.of(VAL_DATE, TRADE_PRICE))
      .build();
  private static final ResolvedIborFutureOptionTrade FUTURE_OPTION_TRADE = ResolvedIborFutureOptionTrade.builder()
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .tradedPrice(TradedPrice.of(TRADE_DATE, TRADE_PRICE))
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

  @Test
  public void presentValue_from_option_price_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE_TD, VAL_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, TRADE_PRICE)) * OPTION_QUANTITY;
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void presentValue_from_future_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
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
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void presentValue_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE, lastClosingPrice);
    double optionPrice =
        OPTION_PRODUCT_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, lastClosingPrice)) * OPTION_QUANTITY;
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  // ----------     present value sensitivity     ----------

  @Test
  public void presentValueSensitivity_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    PointSensitivities psProduct =
        OPTION_PRODUCT_PRICER.priceSensitivityRatesStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities psExpected = psProduct
        .multipliedBy(OPTION_PRODUCT_PRICER.marginIndex(OPTION, 1) * OPTION_QUANTITY);
    PointSensitivities psComputed = OPTION_TRADE_PRICER
        .presentValueSensitivityRates(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(psComputed.equalWithTolerance(psExpected, TOLERANCE_PV_DELTA)).isTrue();
  }

  // ----------     present value normal vol sensitivity     ----------

  @Test
  public void presentvalue_normalVolSensitivity_from_env() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(OPTION.getUnderlyingFuture().getIborRate().getObservation())).thenReturn(RATE);

    IborFutureOptionSensitivity psProduct =
        OPTION_PRODUCT_PRICER.priceSensitivityModelParamsVolatility(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    IborFutureOptionSensitivity psExpected = psProduct.withSensitivity(
        psProduct.getSensitivity() * OPTION_PRODUCT_PRICER.marginIndex(OPTION, 1) * OPTION_QUANTITY);
    IborFutureOptionSensitivity psComputed = OPTION_TRADE_PRICER
        .presentValueSensitivityModelParamsVolatility(FUTURE_OPTION_TRADE, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(psExpected.compareKey(psComputed) == 0).isTrue();
    assertThat(psComputed.getSensitivity()).isCloseTo(psExpected.getSensitivity(), offset(TOLERANCE_PV_DELTA));
  }

}

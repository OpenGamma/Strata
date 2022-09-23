/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.model.MoneynessType;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.OvernightRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOption;

/**
 * Tests {@link NormalOvernightFutureOptionMarginedProductPricer}
 */
public class NormalOvernightFutureOptionMarginedProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.50, 0.50, 0.50, 0.50, 1.00, 1.00, 1.00, 1.00);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.01, 0.00, 0.01, -0.02, -0.01, 0.00, 0.01, -0.02, -0.01, 0.00, 0.01);
  private static final DoubleArray NORMAL_VOL = DoubleArray.of(
      0.01, 0.011, 0.012, 0.010,
      0.011, 0.012, 0.013, 0.012,
      0.012, 0.013, 0.014, 0.014);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      Surfaces.normalVolatilityByExpirySimpleMoneyness("Test", ACT_365F, MoneynessType.PRICE),
      TIMES,
      MONEYNESS_PRICES,
      NORMAL_VOL,
      INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

  private static final NormalOvernightFutureOptionVolatilities VOL_SIMPLE_MONEY_PRICE =
      NormalOvernightFutureOptionExpirySimpleMoneynessVolatilities.of(
          GBP_SONIA, VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE), PARAMETERS_PRICE);

  private static final ResolvedOvernightFutureOption OPTION =
      OvernightFutureDummyData.OVERNIGHT_FUTURE_OPTION_2.resolve(REF_DATA);
  private static final ResolvedOvernightFuture UNDERLYING = OPTION.getUnderlyingFuture();

  private static final double RATE = 0.015;

  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  private static final DiscountingOvernightFutureProductPricer FUTURE_PRICER =
      DiscountingOvernightFutureProductPricer.DEFAULT;
  private static final NormalOvernightFutureOptionMarginedProductPricer OPTION_PRICER =
      new NormalOvernightFutureOptionMarginedProductPricer(FUTURE_PRICER);

  private static final double TOLERANCE_PRICE = 1.0E-10;
  private static final double TOLERANCE_PRICE_DELTA = 1.0E-8;
  private static final double TOLERANCE_PARAM = 1.0E-8;
  private static final Offset<Double> TOLERANCE_INDEX = offset(1.0E-8);

  // ----------     price     ----------
  @Test
  public void price_from_future_price() {
    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double timeToExpiry = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(timeToExpiry, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionPriceExpected = NORMAL_FUNCTION.getPriceFunction(option).apply(normalPoint);
    double optionPriceComputed = OPTION_PRICER.price(OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertThat(optionPriceComputed).isCloseTo(optionPriceExpected, offset(TOLERANCE_PRICE));
  }

  @Test
  public void price_from_env() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    when(mockOvernight.periodRate(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(RATE);
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    double futurePrice = 1.0 - RATE;
    double optionPriceExpected = OPTION_PRICER.price(OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionPriceComputed = OPTION_PRICER.price(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(optionPriceComputed).isCloseTo(optionPriceExpected, offset(TOLERANCE_PRICE));
  }

  // ----------     delta     ----------
  @Test
  public void delta_from_future_price() {
    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double timeToExpiry = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(timeToExpiry, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionDeltaExpected = NORMAL_FUNCTION.getDelta(option, normalPoint);
    double optionDeltaComputed =
        OPTION_PRICER.deltaStickyStrike(OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertThat(optionDeltaComputed).isCloseTo(optionDeltaExpected, offset(TOLERANCE_PRICE));
  }

  @Test
  public void delta_from_env() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    when(mockOvernight.periodRate(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(RATE);
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    double futurePrice = 1.0 - RATE;
    double optionDeltaExpected =
        OPTION_PRICER.deltaStickyStrike(OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double optionDeltaComputed = OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(optionDeltaComputed).isCloseTo(optionDeltaExpected, offset(TOLERANCE_PRICE));
  }

  // ----------     priceSensitivity     ----------
  @Test
  public void priceSensitivityStickyStrike_from_future_price() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    when(mockOvernight.getFixings()).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    when(mockOvernight.periodRatePointSensitivity(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(OvernightRateSensitivity.ofPeriod(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate(), 1d));
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    double futurePrice = 0.9875;
    PointSensitivities futurePriceSensitivity =
        FUTURE_PRICER.priceSensitivity(OPTION.getUnderlyingFuture(), prov);
    double delta = OPTION_PRICER.deltaStickyStrike(OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed =
        OPTION_PRICER.priceSensitivityRatesStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertThat(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA)).isTrue();
  }

  @Test
  public void priceSensitivityStickyStrike_from_env() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    when(mockOvernight.getFixings()).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    when(mockOvernight.periodRatePointSensitivity(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(OvernightRateSensitivity.ofPeriod(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate(), 1d));
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    PointSensitivities futurePriceSensitivity = OPTION_PRICER.getFuturePricer()
        .priceSensitivity(OPTION.getUnderlyingFuture(), prov);
    double delta = OPTION_PRICER.deltaStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities optionPriceSensitivityExpected = futurePriceSensitivity.multipliedBy(delta);
    PointSensitivities optionPriceSensitivityComputed =
        OPTION_PRICER.priceSensitivityRatesStickyStrike(OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(optionPriceSensitivityExpected.equalWithTolerance(optionPriceSensitivityComputed, TOLERANCE_PRICE_DELTA)).isTrue();
  }

  // ----------     priceSensitivityNormalVolatility     ----------
  @Test
  public void priceSensitivityNormalVolatility_from_future_price() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    when(mockOvernight.getFixings()).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    double futurePrice = 0.9875;
    double strike = OPTION.getStrikePrice();
    double timeToExpiry = ACT_365F.relativeYearFraction(VAL_DATE, OPTION.getExpiryDate());
    double priceSimpleMoneyness = strike - futurePrice;
    double normalVol = PARAMETERS_PRICE.zValue(timeToExpiry, priceSimpleMoneyness);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, OPTION.getPutCall());
    NormalFunctionData normalPoint = NormalFunctionData.of(futurePrice, 1.0, normalVol);
    double optionVegaExpected = NORMAL_FUNCTION.getVega(option, normalPoint);
    OvernightFutureOptionSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityModelParamsVolatility(
        OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    assertThat(optionVegaComputed.getSensitivity()).isCloseTo(optionVegaExpected, offset(TOLERANCE_PRICE));
    assertThat(optionVegaComputed.getExpiry()).isEqualTo(timeToExpiry);
    assertThat(optionVegaComputed.getFixingDate()).isEqualTo(obs.getFixingDate());
    assertThat(optionVegaComputed.getStrikePrice()).isEqualTo(OPTION.getStrikePrice());
    assertThat(optionVegaComputed.getFuturePrice()).isEqualTo(futurePrice);
  }

  @Test
  public void priceSensitivityNormalVolatility_from_env() {
    OvernightIndexRates mockOvernight = mock(OvernightIndexRates.class);
    when(mockOvernight.getFixings()).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockOvernight.getValuationDate()).thenReturn(VAL_DATE);
    OvernightIndexObservation obs = OvernightIndexObservation
        .of(GBP_SONIA, UNDERLYING.getOvernightRate().getStartDate(), REF_DATA);
    when(mockOvernight.periodRatePointSensitivity(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(OvernightRateSensitivity.ofPeriod(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate(), 1d));
    when(mockOvernight.periodRate(obs, OPTION.getUnderlyingFuture().getOvernightRate().getEndDate()))
        .thenReturn(RATE);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setOvernightRates(mockOvernight);
    double futurePrice = 1.0 - RATE;
    OvernightFutureOptionSensitivity optionVegaExpected = OPTION_PRICER.priceSensitivityModelParamsVolatility(
        OPTION, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    OvernightFutureOptionSensitivity optionVegaComputed = OPTION_PRICER.priceSensitivityModelParamsVolatility(
        OPTION, prov, VOL_SIMPLE_MONEY_PRICE);
    assertThat(optionVegaExpected.getStrikePrice()).isEqualTo(optionVegaComputed.getStrikePrice(), offset(TOLERANCE_PARAM));
    assertThat(optionVegaExpected.getFuturePrice()).isEqualTo(optionVegaComputed.getFuturePrice(), offset(TOLERANCE_PARAM));
    assertThat(optionVegaExpected.getExpiry()).isEqualTo(optionVegaComputed.getExpiry(), offset(TOLERANCE_PARAM));
    assertThat(optionVegaComputed.getSensitivity()).isCloseTo(optionVegaExpected.getSensitivity(), offset(TOLERANCE_PRICE_DELTA));
    assertThat(optionVegaComputed.getCurrency()).isEqualTo(optionVegaExpected.getCurrency());
    assertThat(optionVegaComputed.getFixingDate()).isEqualTo(optionVegaExpected.getFixingDate());
  }

  // ----------     Margin Index     ----------

  @Test
  public void marginIndex() {
    double optionPrice = 0.0250;
    double indexComputed = OPTION_PRICER.marginIndex(OPTION, optionPrice);
    double indexExpected = UNDERLYING.getAccrualFactor() * UNDERLYING.getNotional() * optionPrice;
    assertThat(indexComputed).isEqualTo(indexExpected, TOLERANCE_INDEX);
  }

  @Test
  public void marginIndexSensitivity() {
    double sensitivityAmount = 1234.56;
    PointSensitivities priceSensitivity = ZeroRateSensitivity.of(Currency.AUD, 0.75, sensitivityAmount).build();
    PointSensitivities indexSensitivityComputed = OPTION_PRICER.marginIndexSensitivity(OPTION, priceSensitivity);
    assertThat(indexSensitivityComputed.getSensitivities().size()).isEqualTo(1);
    assertThat(indexSensitivityComputed.getSensitivities().get(0).getSensitivity())
        .isEqualTo(sensitivityAmount * UNDERLYING.getAccrualFactor() * UNDERLYING.getNotional(), TOLERANCE_INDEX);
    assertThat(indexSensitivityComputed.getSensitivities().get(0).getCurrency()).isEqualTo(Currency.AUD);
  }

}

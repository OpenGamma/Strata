/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxCollar;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link DiscountingFxCollarProductPricer}.
 */
public class DiscountingFxCollarProductPricerTest {
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 5, 9, 13, 10, 0, 0, ZONE);
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SPOT_DATE = RatesProviderDataSets.SPOT_DATE_2014_01_24;
  private static final ZonedDateTime VAL_DATETIME_AFTER = EXPIRY.plusDays(1);
  private static final LocalDate VAL_DATE_AFTER = VAL_DATETIME_AFTER.toLocalDate();
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE);
  private static final RatesProvider RATES_PROVIDER_EXPIRY =
      RatesProviderFxDataSets.createProviderEURUSD(EXPIRY.toLocalDate());
  private static final RatesProvider RATES_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE_AFTER);
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME);
  private static final BlackFxOptionSmileVolatilities VOLS_EXPIRY =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(EXPIRY);
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME_AFTER);

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double NOTIONAL = 1.0e6;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double STRIKE_RATE_HIGH = 1.44;
  private static final double STRIKE_RATE_LOW = 1.36;
  private static final CurrencyAmount EUR_AMOUNT1 = CurrencyAmount.of(EUR, -NOTIONAL);
  private static final CurrencyAmount EUR_AMOUNT2 = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_HIGH = CurrencyAmount.of(USD, NOTIONAL * STRIKE_RATE_HIGH);
  private static final CurrencyAmount USD_AMOUNT_LOW = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT_HIGH = ResolvedFxSingle.of(EUR_AMOUNT1, USD_AMOUNT_HIGH, PAYMENT_DATE);
  private static final ResolvedFxSingle FX_PRODUCT_LOW = ResolvedFxSingle.of(EUR_AMOUNT2, USD_AMOUNT_LOW, PAYMENT_DATE);
  private static final DiscountingFxCollarProductPricer PRICER = DiscountingFxCollarProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final double PERCENTAGE_TOL = 1.0e-4;
  private static final double FD_EPS = 1.0e-7;
  private static final ResolvedFxVanillaOption CALL_OTM = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH)
      .build();
  private static final ResolvedFxVanillaOption CALL_ITM = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW)
      .build();
  private static final ResolvedFxVanillaOption PUT_OTM = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW)
      .build();
  private static final ResolvedFxVanillaOption PUT_ITM = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH)
      .build();

  private static final ResolvedFxCollar COLLAR_OTM_PRODUCT = ResolvedFxCollar.of(CALL_OTM, PUT_OTM);
  private static final ResolvedFxCollar COLLAR_ITM_PRODUCT = ResolvedFxCollar.of(CALL_ITM, PUT_ITM);

  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  @Test
  public void test_price_presentValue_afterExpiry() {
    double price = PRICER.price(COLLAR_OTM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pv = PRICER.presentValue(COLLAR_OTM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(price).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pv.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_delta_presentValueDelta_afterExpiry() {
    double delta = PRICER.delta(COLLAR_OTM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(COLLAR_OTM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(delta).isCloseTo(0d, offset(TOL));
    assertThat(pvDelta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueSensitivity_atExpiry() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRatesStickyStrike(COLLAR_OTM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(COLLAR_OTM_PRODUCT, (p), VOLS_EXPIRY));
    assertThat(computedCall.equalWithTolerance(expectedCall, NOTIONAL * FD_EPS)).isTrue();
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRatesStickyStrike(COLLAR_OTM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(COLLAR_OTM_PRODUCT, (p), VOLS_EXPIRY));
    assertThat(computedPut.equalWithTolerance(expectedPut, NOTIONAL * FD_EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_afterExpiry() {
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(point).isEqualTo(PointSensitivities.empty());
  }

  @Test
  public void test_gamma_presentValueGamma_atExpiry() {
    double gamma = PRICER.gamma(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(gamma).isCloseTo(0d, offset(TOL));
    assertThat(pvGamma.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_gamma_presentValueGamma_afterExpiry() {
    double gamma = PRICER.gamma(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(gamma).isCloseTo(0d, offset(TOL));
    assertThat(pvGamma.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_vega_presentValueVega_atExpiry() {
    double vega = PRICER.vega(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvVega = PRICER.presentValueVega(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(vega).isCloseTo(0d, offset(TOL));
    assertThat(pvVega.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_vega_presentValueVega_afterExpiry() {
    double vega = PRICER.vega(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvVega = PRICER.presentValueVega(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(vega).isCloseTo(0d, offset(TOL));
    assertThat(pvVega.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_atExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(point).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_afterExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(point).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_theta_presentValueTheta_atExpiry() {
    double theta = PRICER.theta(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(theta).isCloseTo(0d, offset(TOL));
    assertThat(pvTheta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_theta_presentValueTheta_afterExpiry() {
    double theta = PRICER.theta(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(theta).isCloseTo(0d, offset(TOL));
    assertThat(pvTheta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_forwardFxRate() {
    FxRate fxRate = PRICER.forwardFxRate(COLLAR_ITM_PRODUCT, RATES_PROVIDER);
    assertThat(fxRate.getPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(fxRate.fxRate(CURRENCY_PAIR)).isCloseTo(1.399078, withinPercentage(PERCENTAGE_TOL));
  }

  @Test
  public void test_impliedVolatility_atExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(COLLAR_ITM_PRODUCT, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY));
  }

  @Test
  public void test_impliedVolatility_afterExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(COLLAR_ITM_PRODUCT, RATES_PROVIDER_AFTER, VOLS_AFTER));
  }

  @Test
  public void test_currencyExposure() {
    double dfCounterSpot = RATES_PROVIDER.discountFactor(USD, SPOT_DATE);
    double dfBaseSpot = RATES_PROVIDER.discountFactor(EUR, SPOT_DATE);
    double adjustedFxSpotScalingFactor = dfBaseSpot / dfCounterSpot;

    MultiCurrencyAmount computedPricer = PRICER.currencyExposure(COLLAR_OTM_PRODUCT, RATES_PROVIDER, VOLS);
    CurrencyAmount pv = PRICER.presentValue(COLLAR_OTM_PRODUCT, RATES_PROVIDER, VOLS);
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(COLLAR_OTM_PRODUCT, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount computedPoint = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertThat(computedPricer.getAmount(EUR).getAmount() * adjustedFxSpotScalingFactor).isCloseTo(computedPoint.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    assertThat(computedPricer.getAmount(USD).getAmount()).isCloseTo(computedPoint.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }
}

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
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
public class BlackFxVanillaOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 5, 9, 13, 10, 0, 0, ZONE);
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
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
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      FxVolatilitySmileDataSet.getSmileDeltaTermStructure6();

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double NOTIONAL = 1.0e6;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double STRIKE_RATE_HIGH = 1.44;
  private static final double STRIKE_RATE_LOW = 1.36;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_HIGH = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_HIGH);
  private static final CurrencyAmount USD_AMOUNT_LOW = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT_HIGH = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_HIGH, PAYMENT_DATE);
  private static final ResolvedFxSingle FX_PRODUCT_LOW = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_LOW, PAYMENT_DATE);
  private static final ResolvedFxVanillaOption CALL_OTM = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
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
      .underlying(FX_PRODUCT_LOW.inverse())
      .build();
  private static final ResolvedFxVanillaOption PUT_ITM = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH.inverse())
      .build();
  private static final BlackFxVanillaOptionProductPricer PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final double PERCENTAGE_TOL = 1.0e-4;
  private static final double FD_EPS = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  @Test
  public void test_price_presentValue() {
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double volHigh = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double volLow = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_LOW, forward);
    double expectedPriceCallOtm =
        df * BlackFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true);
    double expectedPricePutOtm =
        df * BlackFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false);
    double expectedPvCallOtm = -NOTIONAL * df *
        BlackFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true);
    double expectedPvPutOtm = -NOTIONAL * df *
        BlackFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false);
    assertThat(priceCallOtm).isCloseTo(expectedPriceCallOtm, offset(TOL));
    assertThat(pvCallOtm.getCurrency()).isEqualTo(USD);
    assertThat(pvCallOtm.getAmount()).isCloseTo(expectedPvCallOtm, offset(NOTIONAL * TOL));
    assertThat(pricePutOtm).isCloseTo(expectedPricePutOtm, offset(TOL));
    assertThat(pvPutOtm.getCurrency()).isEqualTo(USD);
    assertThat(pvPutOtm.getAmount()).isCloseTo(expectedPvPutOtm, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_price_presentValue_atExpiry() {
    double df = RATES_PROVIDER_EXPIRY.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER_EXPIRY).fxRate(CURRENCY_PAIR);
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(priceCallOtm).isCloseTo(0d, offset(TOL));
    assertThat(pvCallOtm.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    double priceCallItm = PRICER.price(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvCallItm = PRICER.presentValue(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(priceCallItm).isCloseTo(df * (forward - STRIKE_RATE_LOW), offset(TOL));
    assertThat(pvCallItm.getAmount()).isCloseTo(df * (forward - STRIKE_RATE_LOW) * NOTIONAL, offset(NOTIONAL * TOL));
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(pricePutOtm).isCloseTo(0d, offset(TOL));
    assertThat(pvPutOtm.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutItm = PRICER.presentValue(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(pricePutItm).isCloseTo(df * (STRIKE_RATE_HIGH - forward), offset(TOL));
    assertThat(pvPutItm.getAmount()).isCloseTo(df * (STRIKE_RATE_HIGH - forward) * NOTIONAL, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_price_presentValue_afterExpiry() {
    double price = PRICER.price(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(price).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pv.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_price_presentValue_parity() {
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvPutItm = PRICER.presentValue(PUT_ITM, RATES_PROVIDER, VOLS);
    assertThat(priceCallOtm - pricePutItm).isCloseTo(df * (forward - STRIKE_RATE_HIGH), offset(TOL));
    assertThat(-pvCallOtm.getAmount() - pvPutItm.getAmount()).isCloseTo(df * (forward - STRIKE_RATE_HIGH) * NOTIONAL, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_delta_presentValueDelta() {
    double deltaCall = PRICER.delta(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvDeltaCall = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER, VOLS);
    double deltaPut = PRICER.delta(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPut = PRICER.presentValueDelta(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedDeltaCall = dfFor * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true);
    double expectedDeltaPut = dfFor * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false);
    double expectedPvDeltaCall = -NOTIONAL * dfFor
        * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true);
    double expectedPvDeltaPut = NOTIONAL * dfFor
        * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false);
    assertThat(deltaCall).isCloseTo(expectedDeltaCall, offset(TOL));
    assertThat(pvDeltaCall.getCurrency()).isEqualTo(USD);
    assertThat(pvDeltaCall.getAmount()).isCloseTo(expectedPvDeltaCall, offset(NOTIONAL * TOL));
    assertThat(deltaPut).isCloseTo(expectedDeltaPut, offset(TOL));
    assertThat(pvDeltaPut.getCurrency()).isEqualTo(USD);
    assertThat(pvDeltaPut.getAmount()).isCloseTo(expectedPvDeltaPut, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_delta_presentValueDelta_atExpiry() {
    double dfFor = RATES_PROVIDER_EXPIRY.discountFactor(EUR, PAYMENT_DATE);
    double deltaCallOtm = PRICER.delta(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaCallOtm = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(deltaCallOtm).isCloseTo(0d, offset(TOL));
    assertThat(pvDeltaCallOtm.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    double deltaCallItm = PRICER.delta(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaCallItm = PRICER.presentValueDelta(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(deltaCallItm).isCloseTo(dfFor, offset(TOL));
    assertThat(pvDeltaCallItm.getAmount()).isCloseTo(NOTIONAL * dfFor, offset(NOTIONAL * TOL));
    double deltaPutItm = PRICER.delta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaPutItm = PRICER.presentValueDelta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(deltaPutItm).isCloseTo(-dfFor, offset(TOL));
    assertThat(pvDeltaPutItm.getAmount()).isCloseTo(-NOTIONAL * dfFor, offset(NOTIONAL * TOL));
    double deltaPutOtm = PRICER.delta(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaPutOtm = PRICER.presentValueDelta(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(deltaPutOtm).isCloseTo(0d, offset(TOL));
    assertThat(pvDeltaPutOtm.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_delta_presentValueDelta_afterExpiry() {
    double delta = PRICER.delta(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(delta).isCloseTo(0d, offset(TOL));
    assertThat(pvDelta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRatesStickyStrike(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> PRICER.presentValue(CALL_OTM, (p), VOLS));
    // contribution via implied volatility, to be subtracted.
    CurrencyAmount pvVegaCall = PRICER.presentValueVega(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities impliedVolSenseCall =
        FD_CAL.sensitivity(RATES_PROVIDER,
            (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(CALL_OTM, (p), VOLS)))
            .multipliedBy(-pvVegaCall.getAmount());
    assertThat(computedCall.equalWithTolerance(expectedCall.combinedWith(impliedVolSenseCall), NOTIONAL * FD_EPS)).isTrue();
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRatesStickyStrike(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> PRICER.presentValue(PUT_OTM, (p), VOLS));
    // contribution via implied volatility, to be subtracted.
    CurrencyAmount pvVegaPut = PRICER.presentValueVega(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities impliedVolSensePut = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(PUT_OTM, (p), VOLS)))
        .multipliedBy(-pvVegaPut.getAmount());
    assertThat(computedPut.equalWithTolerance(expectedPut.combinedWith(impliedVolSensePut), NOTIONAL * FD_EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_atExpiry() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRatesStickyStrike(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(CALL_OTM, (p), VOLS_EXPIRY));
    assertThat(computedCall.equalWithTolerance(expectedCall, NOTIONAL * FD_EPS)).isTrue();
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRatesStickyStrike(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(PUT_OTM, (p), VOLS_EXPIRY));
    assertThat(computedPut.equalWithTolerance(expectedPut, NOTIONAL * FD_EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_afterExpiry() {
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(point).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_gamma_presentValueGamma() {
    double gammaCall = PRICER.gamma(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvGammaCall = PRICER.presentValueGamma(CALL_OTM, RATES_PROVIDER, VOLS);
    double gammaPut = PRICER.gamma(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvGammaPut = PRICER.presentValueGamma(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedGamma = dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    double expectedPvGamma = -NOTIONAL * dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertThat(gammaCall).isCloseTo(expectedGamma, offset(TOL));
    assertThat(pvGammaCall.getCurrency()).isEqualTo(USD);
    assertThat(pvGammaCall.getAmount()).isCloseTo(expectedPvGamma, offset(NOTIONAL * TOL));
    assertThat(gammaPut).isCloseTo(expectedGamma, offset(TOL));
    assertThat(pvGammaPut.getCurrency()).isEqualTo(USD);
    assertThat(pvGammaPut.getAmount()).isCloseTo(-expectedPvGamma, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_gamma_presentValueGamma_atExpiry() {
    double gamma = PRICER.gamma(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(gamma).isCloseTo(0d, offset(TOL));
    assertThat(pvGamma.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_gamma_presentValueGamma_afterExpiry() {
    double gamma = PRICER.gamma(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(gamma).isCloseTo(0d, offset(TOL));
    assertThat(pvGamma.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_vega_presentValueVega() {
    double vegaCall = PRICER.vega(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvVegaCall = PRICER.presentValueVega(CALL_OTM, RATES_PROVIDER, VOLS);
    double vegaPut = PRICER.vega(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvVegaPut = PRICER.presentValueVega(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedVega = dfDom * BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    double expectedPvVega = -NOTIONAL * dfDom *
        BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertThat(vegaCall).isCloseTo(expectedVega, offset(TOL));
    assertThat(pvVegaCall.getCurrency()).isEqualTo(USD);
    assertThat(pvVegaCall.getAmount()).isCloseTo(expectedPvVega, offset(NOTIONAL * TOL));
    assertThat(vegaPut).isCloseTo(expectedVega, offset(TOL));
    assertThat(pvVegaPut.getCurrency()).isEqualTo(USD);
    assertThat(pvVegaPut.getAmount()).isCloseTo(-expectedPvVega, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_vega_presentValueVega_atExpiry() {
    double vega = PRICER.vega(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvVega = PRICER.presentValueVega(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(vega).isCloseTo(0d, offset(TOL));
    assertThat(pvVega.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_vega_presentValueVega_afterExpiry() {
    double vega = PRICER.vega(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvVega = PRICER.presentValueVega(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(vega).isCloseTo(0d, offset(TOL));
    assertThat(pvVega.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityBlackVolatility() {
    FxOptionSensitivity computedCall = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_OTM, RATES_PROVIDER, VOLS);
    FxOptionSensitivity computedPut = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        VOLS.getName(), CURRENCY_PAIR, timeToExpiry, STRIKE_RATE_HIGH, forward, USD,
        -NOTIONAL * df * BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol));
    assertThat(computedCall.build().equalWithTolerance(expected.build(), NOTIONAL * TOL)).isTrue();
    assertThat(computedPut.build().equalWithTolerance(expected.build().multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_atExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(point).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_afterExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(point).isEqualTo(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_theta_presentValueTheta() {
    double theta = PRICER.theta(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(CALL_OTM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedTheta = dfDom * BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertThat(theta).isCloseTo(expectedTheta, offset(TOL));
    double expectedPvTheta = -NOTIONAL * dfDom *
        BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertThat(pvTheta.getCurrency()).isEqualTo(USD);
    assertThat(pvTheta.getAmount()).isCloseTo(expectedPvTheta, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_theta_presentValueTheta_atExpiry() {
    double theta = PRICER.theta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(theta).isCloseTo(0d, offset(TOL));
    assertThat(pvTheta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_theta_presentValueTheta_afterExpiry() {
    double theta = PRICER.theta(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(theta).isCloseTo(0d, offset(TOL));
    assertThat(pvTheta.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forwardFxRate() {
    FxRate fxRate = PRICER.forwardFxRate(CALL_ITM, RATES_PROVIDER);
    assertThat(fxRate.getPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(fxRate.fxRate(CURRENCY_PAIR)).isCloseTo(1.39904, withinPercentage(PERCENTAGE_TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    double computedCall = PRICER.impliedVolatility(CALL_OTM, RATES_PROVIDER, VOLS);
    double computedPut = PRICER.impliedVolatility(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double expected = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    assertThat(computedCall).isEqualTo(expected);
    assertThat(computedPut).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_atExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY));
  }

  @Test
  public void test_impliedVolatility_afterExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computedPricer = PRICER.currencyExposure(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(CALL_OTM, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount computedPoint = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertThat(computedPricer.getAmount(EUR).getAmount()).isCloseTo(computedPoint.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    assertThat(computedPricer.getAmount(USD).getAmount()).isCloseTo(computedPoint.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

}

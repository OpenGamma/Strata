/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxDigitalOption;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Test {@link StaticReplicationFxDigitalOptionProductPricer}.
 */
@Test
public class StaticReplicationFxDigitalOptionProductPricerTest {
  private static final ImmutableRatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();
  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] { {-0.010, -0.0050 }, {-0.011, -0.0060 },
    {-0.012, -0.0070 }, {-0.013, -0.0080 }, {-0.014, -0.0090 }, {-0.014, -0.0090 } };
  private static final double[][] STRANGLE = new double[][] { {0.0300, 0.0100 }, {0.0310, 0.0110 }, {0.0320, 0.0120 },
    {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } };
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);

  private static final LocalDate VALUATION_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE_RATE = 1.45;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, STRIKE_RATE);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 5, 9);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(13, 10);

  private static final FxDigitalOption OPTION_EURUSD_USD = FxDigitalOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(USD)
      .strike(STRIKE)
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_EURUSD_EUR = FxDigitalOption.builder()
      .putCall(PUT)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(EUR)
      .strike(STRIKE)
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_USDEUR_USD = FxDigitalOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(USD)
      .strike(STRIKE.inverse())
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_USDEUR_EUR = FxDigitalOption.builder()
      .putCall(PUT)
      .longShort(LONG)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(EUR)
      .strike(STRIKE.inverse())
      .notional(NOTIONAL)
      .build();
  private static final LocalDate EARLY_EXPIRY = LocalDate.of(2014, 1, 9);
  private static final FxDigitalOption OPTION_EXPIRED = FxDigitalOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EARLY_EXPIRY)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(USD)
      .strike(STRIKE)
      .notional(NOTIONAL)
      .build();
  private static final BlackFxDigitalOptionProductPricer PRICER_BLACK = BlackFxDigitalOptionProductPricer.DEFAULT;
  private static final StaticReplicationFxDigitalOptionProductPricer PRICER_REPLI = StaticReplicationFxDigitalOptionProductPricer.DEFAULT;
  private static final double TOL = PRICER_REPLI.getSpread();

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    double priceDomBlack = PRICER_BLACK.price(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double priceForBlack = PRICER_BLACK.price(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double priceDomRepli = PRICER_REPLI.price(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double priceForRepli = PRICER_REPLI.price(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(priceDomRepli, priceDomBlack, TOL);
    assertEquals(priceForRepli, priceForBlack, TOL);
    CurrencyAmount pvDomBlack = PRICER_BLACK.presentValue(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvForBlack = PRICER_BLACK.presentValue(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDomRepli = PRICER_REPLI.presentValue(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvForRepli = PRICER_REPLI.presentValue(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDomRepli.getCurrency(), USD);
    assertEquals(pvForRepli.getCurrency(), USD);
    assertEquals(pvDomRepli.getAmount(), pvDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvForRepli.getAmount(), pvForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_price_presentValue_inverse() {
    double priceDomBlack = PRICER_BLACK.price(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double priceForBlack = PRICER_BLACK.price(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double priceDomRepli = PRICER_REPLI.price(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double priceForRepli = PRICER_REPLI.price(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(priceDomRepli, priceDomBlack, TOL);
    assertEquals(priceForRepli, priceForBlack, TOL);
    CurrencyAmount pvDomBlack = PRICER_BLACK.presentValue(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvForBlack = PRICER_BLACK.presentValue(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDomRepli = PRICER_REPLI.presentValue(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvForRepli = PRICER_REPLI.presentValue(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDomRepli.getCurrency(), EUR);
    assertEquals(pvForRepli.getCurrency(), EUR);
    assertEquals(pvDomRepli.getAmount(), pvDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvForRepli.getAmount(), pvForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_price_presentValue_expired() {
    double price = PRICER_REPLI.price(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pv = PRICER_REPLI.presentValue(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(price, 0d, TOL);
    assertEquals(pv.getCurrency(), USD);
    assertEquals(pv.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_delta_presentValueDelta() {
    double deltaDomBlack = PRICER_BLACK.delta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double deltaForBlack = PRICER_BLACK.delta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double deltaDomRepli = PRICER_REPLI.delta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double deltaForRepli = PRICER_REPLI.delta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(deltaDomRepli, deltaDomBlack, TOL);
    assertEquals(deltaForRepli, deltaForBlack, TOL);
    CurrencyAmount pvDeltaDomBlack = PRICER_BLACK.presentValueDelta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaForBlack = PRICER_BLACK.presentValueDelta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaDomRepli = PRICER_REPLI.presentValueDelta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaForRepli = PRICER_REPLI.presentValueDelta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDeltaDomRepli.getCurrency(), USD);
    assertEquals(pvDeltaForRepli.getCurrency(), USD);
    assertEquals(pvDeltaDomRepli.getAmount(), pvDeltaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvDeltaForRepli.getAmount(), pvDeltaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_inverse() {
    double deltaDomBlack = PRICER_BLACK.delta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double deltaForBlack = PRICER_BLACK.delta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double deltaDomRepli = PRICER_REPLI.delta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double deltaForRepli = PRICER_REPLI.delta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(deltaDomRepli, deltaDomBlack, TOL);
    assertEquals(deltaForRepli, deltaForBlack, TOL);
    CurrencyAmount pvDeltaDomBlack = PRICER_BLACK.presentValueDelta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaForBlack = PRICER_BLACK.presentValueDelta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaDomRepli = PRICER_REPLI.presentValueDelta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaForRepli = PRICER_REPLI.presentValueDelta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDeltaDomRepli.getCurrency(), EUR);
    assertEquals(pvDeltaForRepli.getCurrency(), EUR);
    assertEquals(pvDeltaDomRepli.getAmount(), pvDeltaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvDeltaForRepli.getAmount(), pvDeltaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_expired() {
    double delta = PRICER_REPLI.delta(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDelta = PRICER_REPLI.presentValueDelta(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(delta, 0d, TOL);
    assertEquals(pvDelta.getCurrency(), USD);
    assertEquals(pvDelta.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities curveSensiDomBlack =
        PRICER_BLACK.presentValueSensitivity(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiForBlack =
        PRICER_BLACK.presentValueSensitivity(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiDomRepli =
        PRICER_REPLI.presentValueSensitivity(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiForRepli =
        PRICER_REPLI.presentValueSensitivity(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertTrue(curveSensiDomRepli.equalWithTolerance(curveSensiDomBlack, NOTIONAL * TOL));
    assertTrue(curveSensiForRepli.equalWithTolerance(curveSensiForBlack, NOTIONAL * TOL));
  }

  public void test_presentValueSensitivity_inverse() {
    PointSensitivities curveSensiDomBlack =
        PRICER_BLACK.presentValueSensitivity(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiForBlack =
        PRICER_BLACK.presentValueSensitivity(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiDomRepli =
        PRICER_REPLI.presentValueSensitivity(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities curveSensiForRepli =
        PRICER_REPLI.presentValueSensitivity(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertTrue(curveSensiDomRepli.equalWithTolerance(curveSensiDomBlack, NOTIONAL * TOL));
    assertTrue(curveSensiForRepli.equalWithTolerance(curveSensiForBlack, NOTIONAL * TOL));
  }

  public void test_presentValueSensitivity_expired() {
    PointSensitivities computed = PRICER_REPLI.presentValueSensitivity(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_gamma_presentValueGamma() {
    double gammaDomBlack = PRICER_BLACK.gamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double gammaForBlack = PRICER_BLACK.gamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double gammaDomRepli = PRICER_REPLI.gamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double gammaForRepli = PRICER_REPLI.gamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(gammaDomRepli, gammaDomBlack, TOL);
    assertEquals(gammaForRepli, gammaForBlack, TOL);
    CurrencyAmount pvGammaDomBlack = PRICER_BLACK.presentValueGamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaForBlack = PRICER_BLACK.presentValueGamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaDomRepli = PRICER_REPLI.presentValueGamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaForRepli = PRICER_REPLI.presentValueGamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvGammaDomRepli.getCurrency(), USD);
    assertEquals(pvGammaForRepli.getCurrency(), USD);
    assertEquals(pvGammaDomRepli.getAmount(), pvGammaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvGammaForRepli.getAmount(), pvGammaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_inverse() {
    double gammaDomBlack = PRICER_BLACK.gamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double gammaForBlack = PRICER_BLACK.gamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double gammaDomRepli = PRICER_REPLI.gamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double gammaForRepli = PRICER_REPLI.gamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(gammaDomRepli, gammaDomBlack, TOL);
    assertEquals(gammaForRepli, gammaForBlack, TOL);
    CurrencyAmount pvGammaDomBlack = PRICER_BLACK.presentValueGamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaForBlack = PRICER_BLACK.presentValueGamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaDomRepli = PRICER_REPLI.presentValueGamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaForRepli = PRICER_REPLI.presentValueGamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvGammaDomRepli.getCurrency(), EUR);
    assertEquals(pvGammaForRepli.getCurrency(), EUR);
    assertEquals(pvGammaDomRepli.getAmount(), pvGammaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvGammaForRepli.getAmount(), pvGammaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_expired() {
    double gamma = PRICER_REPLI.gamma(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGamma = PRICER_REPLI.presentValueGamma(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(gamma, 0d, TOL);
    assertEquals(pvGamma.getCurrency(), USD);
    assertEquals(pvGamma.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_vega_presentValueVega() {
    double vegaDomBlack = PRICER_BLACK.vega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double vegaForBlack = PRICER_BLACK.vega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double vegaDomRepli = PRICER_REPLI.vega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double vegaForRepli = PRICER_REPLI.vega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(vegaDomRepli, vegaDomBlack, TOL);
    assertEquals(vegaForRepli, vegaForBlack, TOL);
    CurrencyAmount pvVegaDomBlack = PRICER_BLACK.presentValueVega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaForBlack = PRICER_BLACK.presentValueVega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaDomRepli = PRICER_REPLI.presentValueVega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaForRepli = PRICER_REPLI.presentValueVega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvVegaDomRepli.getCurrency(), USD);
    assertEquals(pvVegaForRepli.getCurrency(), USD);
    assertEquals(pvVegaDomRepli.getAmount(), pvVegaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvVegaForRepli.getAmount(), pvVegaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_inverse() {
    double vegaDomBlack = PRICER_BLACK.vega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double vegaForBlack = PRICER_BLACK.vega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double vegaDomRepli = PRICER_REPLI.vega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double vegaForRepli = PRICER_REPLI.vega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(vegaDomRepli, vegaDomBlack, TOL);
    assertEquals(vegaForRepli, vegaForBlack, TOL);
    CurrencyAmount pvVegaDomBlack = PRICER_BLACK.presentValueVega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaForBlack = PRICER_BLACK.presentValueVega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaDomRepli = PRICER_REPLI.presentValueVega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaForRepli = PRICER_REPLI.presentValueVega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvVegaDomRepli.getCurrency(), EUR);
    assertEquals(pvVegaForRepli.getCurrency(), EUR);
    assertEquals(pvVegaDomRepli.getAmount(), pvVegaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvVegaForRepli.getAmount(), pvVegaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_expired() {
    double vega = PRICER_REPLI.vega(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVega = PRICER_REPLI.presentValueVega(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(vega, 0d, TOL);
    assertEquals(pvVega.getCurrency(), USD);
    assertEquals(pvVega.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityBlackVolatility() {
    PointSensitivityBuilder volSensiDomBlack =
        PRICER_BLACK.presentValueSensitivityBlackVolatility(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiForBlack =
        PRICER_BLACK.presentValueSensitivityBlackVolatility(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiDomRepli =
        PRICER_REPLI.presentValueSensitivityBlackVolatility(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiForRepli =
        PRICER_REPLI.presentValueSensitivityBlackVolatility(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertTrue(volSensiDomRepli.build().equalWithTolerance(volSensiDomBlack.build(), NOTIONAL * TOL));
    assertTrue(volSensiForRepli.build().equalWithTolerance(volSensiForBlack.build(), NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityBlackVolatility_inverse() {
    PointSensitivityBuilder volSensiDomBlack =
        PRICER_BLACK.presentValueSensitivityBlackVolatility(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiForBlack =
        PRICER_BLACK.presentValueSensitivityBlackVolatility(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiDomRepli =
        PRICER_REPLI.presentValueSensitivityBlackVolatility(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder volSensiForRepli =
        PRICER_REPLI.presentValueSensitivityBlackVolatility(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertTrue(volSensiDomRepli.build().equalWithTolerance(volSensiDomBlack.build(), NOTIONAL * TOL));
    assertTrue(volSensiForRepli.build().equalWithTolerance(volSensiForBlack.build(), NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityBlackVolatility_expired() {
    PointSensitivityBuilder computed =
        PRICER_REPLI.presentValueSensitivityBlackVolatility(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(computed, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_theta_presentValueTheta() {
    double thetaDomBlack = PRICER_BLACK.theta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double thetaForBlack = PRICER_BLACK.theta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double thetaDomRepli = PRICER_REPLI.theta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double thetaForRepli = PRICER_REPLI.theta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(thetaDomRepli, thetaDomBlack, TOL);
    assertEquals(thetaForRepli, thetaForBlack, TOL);
    CurrencyAmount pvThetaDomBlack = PRICER_BLACK.presentValueTheta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaForBlack = PRICER_BLACK.presentValueTheta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaDomRepli = PRICER_REPLI.presentValueTheta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaForRepli = PRICER_REPLI.presentValueTheta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvThetaDomRepli.getCurrency(), USD);
    assertEquals(pvThetaForRepli.getCurrency(), USD);
    assertEquals(pvThetaDomRepli.getAmount(), pvThetaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvThetaForRepli.getAmount(), pvThetaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_inverse() {
    double thetaDomBlack = PRICER_BLACK.theta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double thetaForBlack = PRICER_BLACK.theta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double thetaDomRepli = PRICER_REPLI.theta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double thetaForRepli = PRICER_REPLI.theta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(thetaDomRepli, thetaDomBlack, TOL);
    assertEquals(thetaForRepli, thetaForBlack, TOL);
    CurrencyAmount pvThetaDomBlack = PRICER_BLACK.presentValueTheta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaForBlack = PRICER_BLACK.presentValueTheta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaDomRepli = PRICER_REPLI.presentValueTheta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaForRepli = PRICER_REPLI.presentValueTheta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvThetaDomRepli.getCurrency(), EUR);
    assertEquals(pvThetaForRepli.getCurrency(), EUR);
    assertEquals(pvThetaDomRepli.getAmount(), pvThetaDomBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(pvThetaForRepli.getAmount(), pvThetaForBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_expired() {
    double theta = PRICER_REPLI.theta(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvTheta = PRICER_REPLI.presentValueTheta(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(theta, 0d, TOL);
    assertEquals(pvTheta.getCurrency(), USD);
    assertEquals(pvTheta.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount ceDomBlack = PRICER_BLACK.currencyExposure(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceForBlack = PRICER_BLACK.currencyExposure(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceDomRepli = PRICER_REPLI.currencyExposure(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceForRepli = PRICER_REPLI.currencyExposure(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(ceDomRepli.getAmount(EUR).getAmount(), ceDomBlack.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(ceDomRepli.getAmount(USD).getAmount(), ceDomBlack.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(ceForRepli.getAmount(EUR).getAmount(), ceForBlack.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(ceForRepli.getAmount(USD).getAmount(), ceForBlack.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_inverse() {
    MultiCurrencyAmount ceDomBlack = PRICER_BLACK.currencyExposure(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceForBlack = PRICER_BLACK.currencyExposure(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceDomRepli = PRICER_REPLI.currencyExposure(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceForRepli = PRICER_REPLI.currencyExposure(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(ceDomRepli.getAmount(EUR).getAmount(), ceDomBlack.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(ceDomRepli.getAmount(USD).getAmount(), ceDomBlack.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(ceForRepli.getAmount(EUR).getAmount(), ceForBlack.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(ceForRepli.getAmount(USD).getAmount(), ceForBlack.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_expired() {
    MultiCurrencyAmount computed = PRICER_REPLI.currencyExposure(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(computed, MultiCurrencyAmount.empty());
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double volDomBlack = PRICER_BLACK.impliedVolatility(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double volForBlack = PRICER_BLACK.impliedVolatility(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double volDomRepli = PRICER_REPLI.impliedVolatility(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double volForRepli = PRICER_REPLI.impliedVolatility(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(volDomRepli, volDomBlack, TOL);
    assertEquals(volForRepli, volForBlack, TOL);
  }

  public void test_impliedVolatility_inverse() {
    double volDomBlack = PRICER_BLACK.impliedVolatility(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double volForBlack = PRICER_BLACK.impliedVolatility(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double volDomRepli = PRICER_REPLI.impliedVolatility(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double volForRepli = PRICER_REPLI.impliedVolatility(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(volDomRepli, volDomBlack, TOL);
    assertEquals(volForRepli, volForBlack, TOL);
  }

  public void test_impliedVolatility_expired() {
    assertThrowsIllegalArg(() -> PRICER_REPLI.impliedVolatility(OPTION_EXPIRED, RATES_PROVIDER, VOL_PROVIDER));
  }
}

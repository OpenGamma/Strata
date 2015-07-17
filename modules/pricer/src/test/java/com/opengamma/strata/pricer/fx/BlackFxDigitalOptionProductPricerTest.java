/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.analytics.math.statistics.distribution.NormalDistribution;
import com.opengamma.analytics.math.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.FxDigitalOption;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;

@Test
public class BlackFxDigitalOptionProductPricerTest {
  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();
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
  private static final PutCall CALL = PutCall.CALL;
  private static final LongShort SHORT = LongShort.SHORT;
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
      .putCall(CALL)
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
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(EUR)
      .strike(STRIKE.inverse())
      .notional(NOTIONAL)
      .build();

  private static final BlackFxDigitalOptionProductPricer PRICER = BlackFxDigitalOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  public void test_price_presentValue() {
    double priceDom = PRICER.price(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double priceFor = PRICER.price(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, STRIKE_RATE, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double priceAsset = df * f * NORMAL.getCDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t));
    double priceCash = df * NORMAL.getCDF((Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t));
    assertEquals(priceDom, priceCash, TOL);
    assertEquals(priceFor, priceAsset, TOL);
    CurrencyAmount pvDom = PRICER.presentValue(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvFor = PRICER.presentValue(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvAsset = -NOTIONAL * priceAsset;
    double pvCash = -NOTIONAL * priceCash;
    assertEquals(pvDom.getCurrency(), USD);
    assertEquals(pvFor.getCurrency(), USD);
    assertEquals(pvDom.getAmount(), pvCash, NOTIONAL * TOL);
    assertEquals(pvFor.getAmount(), pvAsset, NOTIONAL * TOL);
  }

  // fix this
  public void test_price_presentValue_inverse() {
    double priceDom = PRICER.price(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double priceFor = PRICER.price(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, STRIKE_RATE, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double priceAsset = df * f * NORMAL.getCDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t));
    double priceCash = df * NORMAL.getCDF((Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t));
    assertEquals(priceDom, priceCash, TOL);
    assertEquals(priceFor, priceAsset, TOL);
    CurrencyAmount pvDom = PRICER.presentValue(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvFor = PRICER.presentValue(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvAsset = -NOTIONAL * priceAsset;
    double pvCash = -NOTIONAL * priceCash;
    assertEquals(pvDom.getCurrency(), EUR);
    assertEquals(pvFor.getCurrency(), EUR);
    assertEquals(pvDom.getAmount(), pvCash, NOTIONAL * TOL);
    assertEquals(pvFor.getAmount(), pvAsset, NOTIONAL * TOL);
  }

}

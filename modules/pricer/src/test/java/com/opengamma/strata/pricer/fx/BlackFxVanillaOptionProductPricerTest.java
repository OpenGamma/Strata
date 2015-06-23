/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.volatility.BlackFormulaRepository;
import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.Fx;
import com.opengamma.strata.finance.fx.FxVanillaOption;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
@Test
public class BlackFxVanillaOptionProductPricerTest {
  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();

  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] { {-0.010, -0.0050 }, {-0.011, -0.0060 },
    {-0.012, -0.0070 }, {-0.013, -0.0080 }, {-0.014, -0.0090 },
    {-0.014, -0.0090 } };
  private static final double[][] STRANGLE = new double[][] { {0.0300, 0.0100 }, {0.0310, 0.0110 }, {0.0320, 0.0120 },
    {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } };
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);

  private static final LocalDate VALUATION_DATE = LocalDate.of(2011, 11, 10);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2012, 2, 12);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
  private static final Fx FX_PRODUCT = Fx.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  private static final double STRIKE_RATE = 1.45;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, STRIKE_RATE);
  private static final PutCall CALL = PutCall.CALL;
  private static final LongShort LONG = LongShort.LONG;
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2012, 2, 10);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(13, 10);
  private static final FxVanillaOption OPTION_PRODUCT = FxVanillaOption.builder()
      .putCall(CALL)
      .longShort(LONG)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .strike(STRIKE)
      .build();

  private static final BlackFxVanillaOptionProductPricer PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;

  private static final double TOL = 1.0e-13;

  public void test_presentValue() {
    CurrencyAmount pv = PRICER.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expected =
        NOTIONAL * df * BlackFormulaRepository.price(forward, STRIKE_RATE, timeToExpiry, vol, CALL.isCall());
    assertEquals(pv.getCurrency(), USD);
    assertEquals(pv.getAmount(), expected, NOTIONAL * TOL);

    // The direction of strike will be modified, thus the same result is expected
    FxVanillaOption option1 = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(FX_PRODUCT)
        .strike(STRIKE.inverse())
        .build();
    CurrencyAmount pv1 = PRICER.presentValue(option1, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pv1.getCurrency(), pv.getCurrency());
    assertEquals(pv1.getAmount(), pv.getAmount(), NOTIONAL * TOL);

    // check put-call relation
    CurrencyAmount eurAmount = CurrencyAmount.of(EUR, -NOTIONAL);
    CurrencyAmount usdAmount = CurrencyAmount.of(USD, NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
    Fx fxProduct = Fx.of(eurAmount, usdAmount, PAYMENT_DATE);
    FxVanillaOption option2 = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fxProduct)
        .strike(STRIKE)
        .build();
    CurrencyAmount pv2 = PRICER.presentValue(option2, RATES_PROVIDER, VOL_PROVIDER).convertedTo(USD, FX_MATRIX);
    double factor = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE) / RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);

    FxVanillaOption option3 = FxVanillaOption.builder()
        .putCall(PutCall.PUT)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(FX_PRODUCT)
        .strike(STRIKE)
        .build();
    CurrencyAmount pv3 = PRICER.presentValue(option3, RATES_PROVIDER, VOL_PROVIDER);
    System.out.println(pv2 + "\t" + pv2.multipliedBy(factor) + "\t" + pv3);

  }
}

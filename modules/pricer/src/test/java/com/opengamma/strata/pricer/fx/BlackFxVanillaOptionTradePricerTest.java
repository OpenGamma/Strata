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

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxVanillaOption;
import com.opengamma.strata.product.fx.FxVanillaOptionTrade;

/**
 * Test {@link BlackFxVanillaOptionTradePricer}. 
 */
@Test
public class BlackFxVanillaOptionTradePricerTest {

  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();

  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] {
    {-0.010, -0.0050 }, {-0.011, -0.0060 }, {-0.012, -0.0070 },
    {-0.013, -0.0080 }, {-0.014, -0.0090 }, {-0.014, -0.0090 } };
  private static final double[][] STRANGLE = new double[][] {
      {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120},
      {0.0330, 0.0130}, {0.0340, 0.0140}, {0.0340, 0.0140}};
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);

  private static final LocalDate VALUATION_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
  private static final FxSingle FX_PRODUCT = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  private static final double STRIKE_RATE = 1.45;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, STRIKE_RATE);
  private static final PutCall CALL = PutCall.CALL;
  private static final LongShort SHORT = LongShort.SHORT;
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 5, 9);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(13, 10);
  private static final FxVanillaOption OPTION_PRODUCT = FxVanillaOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .strike(STRIKE)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION_DATE).build();
  private static final LocalDate CASH_SETTLE_DATE = LocalDate.of(2014, 1, 25);
  private static final Payment PREMIUM = Payment.of(EUR, NOTIONAL * 0.027, CASH_SETTLE_DATE);
  private static final FxVanillaOptionTrade OPTION_TRADE = FxVanillaOptionTrade.builder()
      .premium(PREMIUM)
      .product(OPTION_PRODUCT)
      .tradeInfo(TRADE_INFO)
      .build();

  private static final BlackFxVanillaOptionProductPricer PRICER_PRODUCT = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final BlackFxVanillaOptionTradePricer PRICER_TRADE = BlackFxVanillaOptionTradePricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;
  private static final double TOL = 1.0e-13;

  public void test_presentValue() {
    MultiCurrencyAmount pvSensiTrade = PRICER_TRADE.presentValue(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvSensiProduct = PRICER_PRODUCT.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvSensiPremium = PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER);
    assertEquals(pvSensiTrade, MultiCurrencyAmount.of(pvSensiProduct, pvSensiPremium));
  }

  public void test_presentValueSensitivity() {
    PointSensitivities pvSensiTrade = PRICER_TRADE.presentValueSensitivity(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities pvSensiProduct = PRICER_PRODUCT.presentValueSensitivity(OPTION_PRODUCT, RATES_PROVIDER,
        VOL_PROVIDER);
    PointSensitivities pvSensiPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM, RATES_PROVIDER).build();
    assertEquals(pvSensiTrade, pvSensiProduct.combinedWith(pvSensiPremium));
  }

  public void test_presentValueSensitivityBlackVolatility() {
    PointSensitivityBuilder pvSensiTrade =
        PRICER_TRADE.presentValueSensitivityBlackVolatility(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivityBlackVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvSensiTrade, pvSensiProduct);
  }

  public void test_currencyExposure() {
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities point = PRICER_TRADE.presentValueSensitivity(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount pv = PRICER_TRADE.presentValue(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceExpected = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertEquals(ceComputed.size(), 2);
    assertEquals(ceComputed.getAmount(EUR).getAmount(), ceExpected.getAmount(EUR).getAmount(), TOL * NOTIONAL);
    assertEquals(ceComputed.getAmount(USD).getAmount(), ceExpected.getAmount(USD).getAmount(), TOL * NOTIONAL);
  }

  public void test_currentCash_zero() {
    assertEquals(PRICER_TRADE.currentCash(OPTION_TRADE, VALUATION_DATE), CurrencyAmount.zero(PREMIUM.getCurrency()));
  }

  public void test_currentCash_onSettle() {
    assertEquals(PRICER_TRADE.currentCash(OPTION_TRADE, CASH_SETTLE_DATE), PREMIUM.getValue());
  }

}

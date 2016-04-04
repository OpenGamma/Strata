/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.BarrierType;
import com.opengamma.strata.product.fx.KnockType;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fx.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fx.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackFxSingleBarrierOptionTradePricer}.
 */
@Test
public class BlackFxSingleBarrierOptionTradePricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(VAL_DATETIME);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(VAL_DATE);

  private static final double NOTIONAL = 100_000_000d;
  private static final double LEVEL_LOW = 1.35;
  private static final SimpleConstantContinuousBarrier BARRIER_DKI =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, LEVEL_LOW);
  private static final double REBATE_AMOUNT = 50_000d;
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, REBATE_AMOUNT);
  private static final double STRIKE_RATE = 1.45;
  private static final CurrencyAmount EUR_AMOUNT_REC = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_PAY = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE);
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT_REC, USD_AMOUNT_PAY, PAY_DATE);
  private static final ResolvedFxVanillaOption CALL = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT)
      .build();
  private static final ResolvedFxSingleBarrierOption OPTION_PRODUCT =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI, REBATE);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VAL_DATE).build();
  private static final LocalDate CASH_SETTLE_DATE = LocalDate.of(2011, 6, 16);
  private static final Payment PREMIUM = Payment.of(EUR, NOTIONAL * 0.027, CASH_SETTLE_DATE);
  private static final ResolvedFxSingleBarrierOptionTrade OPTION_TRADE = ResolvedFxSingleBarrierOptionTrade.builder()
      .premium(PREMIUM)
      .product(OPTION_PRODUCT)
      .info(TRADE_INFO)
      .build();

  private static final BlackFxSingleBarrierOptionProductPricer PRICER_PRODUCT = BlackFxSingleBarrierOptionProductPricer.DEFAULT;
  private static final BlackFxSingleBarrierOptionTradePricer PRICER_TRADE = BlackFxSingleBarrierOptionTradePricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;
  private static final double TOL = 1.0e-13;

  public void test_presentValue() {
    MultiCurrencyAmount pvSensiTrade = PRICER_TRADE.presentValue(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvSensiProduct = PRICER_PRODUCT.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvSensiPremium = PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER);
    assertEquals(pvSensiTrade, MultiCurrencyAmount.of(pvSensiProduct, pvSensiPremium));
  }

  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pvSensiTrade =
        PRICER_TRADE.presentValueSensitivity(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivity(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvSensiPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM, RATES_PROVIDER);
    assertEquals(pvSensiTrade, pvSensiProduct.combinedWith(pvSensiPremium));
  }

  public void test_presentValueSensitivityBlackVolatility() {
    PointSensitivityBuilder pvSensiTrade =
        PRICER_TRADE.presentValueSensitivityBlackVolatility(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivityVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pvSensiTrade, pvSensiProduct);
  }

  public void test_currencyExposure() {
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(OPTION_TRADE, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ceExpected = PRICER_PRODUCT.currencyExposure(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER)
        .plus(PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER));
    assertEquals(ceComputed.size(), 2);
    assertEquals(ceComputed.getAmount(EUR).getAmount(), ceExpected.getAmount(EUR).getAmount(), TOL * NOTIONAL);
    assertEquals(ceComputed.getAmount(USD).getAmount(), ceExpected.getAmount(USD).getAmount(), TOL * NOTIONAL);
  }

  public void test_currentCash_zero() {
    assertEquals(PRICER_TRADE.currentCash(OPTION_TRADE, VAL_DATE), CurrencyAmount.zero(PREMIUM.getCurrency()));
  }

  public void test_currentCash_onSettle() {
    assertEquals(PRICER_TRADE.currentCash(OPTION_TRADE, CASH_SETTLE_DATE), PREMIUM.getValue());
  }

}

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackFxSingleBarrierOptionTradePricer}.
 */
public class BlackFxSingleBarrierOptionTradePricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  private static final BlackFxOptionSmileVolatilities VOLS =
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

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount pvSensiTrade = PRICER_TRADE.presentValue(OPTION_TRADE, RATES_PROVIDER, VOLS);
    CurrencyAmount pvSensiProduct = PRICER_PRODUCT.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOLS);
    CurrencyAmount pvSensiPremium = PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER);
    assertThat(pvSensiTrade).isEqualTo(MultiCurrencyAmount.of(pvSensiProduct, pvSensiPremium));
  }

  @Test
  public void test_presentValueSensitivity() {
    PointSensitivities pvSensiTrade =
        PRICER_TRADE.presentValueSensitivityRatesStickyStrike(OPTION_TRADE, RATES_PROVIDER, VOLS);
    PointSensitivities pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivityRatesStickyStrike(OPTION_PRODUCT, RATES_PROVIDER, VOLS).build();
    PointSensitivities pvSensiPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM, RATES_PROVIDER).build();
    assertThat(pvSensiTrade).isEqualTo(pvSensiProduct.combinedWith(pvSensiPremium));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility() {
    PointSensitivities pvSensiTrade =
        PRICER_TRADE.presentValueSensitivityModelParamsVolatility(OPTION_TRADE, RATES_PROVIDER, VOLS);
    PointSensitivities pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivityModelParamsVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOLS).build();
    assertThat(pvSensiTrade).isEqualTo(pvSensiProduct);
  }

  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(OPTION_TRADE, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount ceExpected = PRICER_PRODUCT.currencyExposure(OPTION_PRODUCT, RATES_PROVIDER, VOLS)
        .plus(PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER));
    assertThat(ceComputed.size()).isEqualTo(2);
    assertThat(ceComputed.getAmount(EUR).getAmount()).isCloseTo(ceExpected.getAmount(EUR).getAmount(), offset(TOL * NOTIONAL));
    assertThat(ceComputed.getAmount(USD).getAmount()).isCloseTo(ceExpected.getAmount(USD).getAmount(), offset(TOL * NOTIONAL));
  }

  @Test
  public void test_delta() {
    double delta = PRICER_TRADE.delta(OPTION_TRADE, RATES_PROVIDER, VOLS);
    double deltaExpected = PRICER_PRODUCT.delta(OPTION_PRODUCT, RATES_PROVIDER, VOLS);

    assertThat(delta).isEqualTo(deltaExpected);
  }

  @Test
  public void test_currentCash_zero() {
    assertThat(PRICER_TRADE.currentCash(OPTION_TRADE, VAL_DATE)).isEqualTo(CurrencyAmount.zero(PREMIUM.getCurrency()));
  }

  @Test
  public void test_currentCash_onSettle() {
    assertThat(PRICER_TRADE.currentCash(OPTION_TRADE, CASH_SETTLE_DATE)).isEqualTo(PREMIUM.getValue());
  }

  @Test
  public void test_forwardFxRate() {
    FxRate fxRateComputed = PRICER_TRADE.forwardFxRate(OPTION_TRADE, RATES_PROVIDER);
    FxRate fxRateExpected = PRICER_PRODUCT.forwardFxRate(OPTION_PRODUCT, RATES_PROVIDER);
    assertThat(fxRateComputed).isEqualTo(fxRateExpected);
  }

  @Test
  public void test_impliedVolatility() {
    double impVolComputed = PRICER_TRADE.impliedVolatility(OPTION_TRADE, RATES_PROVIDER, VOLS);
    double imlVolExpected = PRICER_PRODUCT.impliedVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOLS);
    assertThat(impVolComputed).isEqualTo(imlVolExpected);
  }

}

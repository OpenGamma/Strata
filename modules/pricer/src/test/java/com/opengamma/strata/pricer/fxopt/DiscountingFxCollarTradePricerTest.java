/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxCollar;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link DiscountingFxCollarTradePricer}.
 */
public class DiscountingFxCollarTradePricerTest {

  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SPOT_DATE = RatesProviderDataSets.SPOT_DATE_2014_01_24;
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  private static final ZonedDateTime OPTION1_EXPIRY = ZonedDateTime.of(2014, 5, 9, 13, 10, 0, 0, ZONE);
  private static final ZonedDateTime OPTION2_EXPIRY = ZonedDateTime.of(2024, 5, 9, 13, 10, 0, 0, ZONE);

  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE);

  private static final DoubleArray TIME_TO_EXPIRY = DoubleArray.of(0.01, 0.252, 0.501, 1.0, 2.0, 5.0);
  private static final DoubleArray ATM = DoubleArray.of(0.175, 0.185, 0.18, 0.17, 0.16, 0.16);
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final DoubleMatrix RISK_REVERSAL = DoubleMatrix.ofUnsafe(new double[][] {
      {-0.010, -0.0050}, {-0.011, -0.0060}, {-0.012, -0.0070},
      {-0.013, -0.0080}, {-0.014, -0.0090}, {-0.014, -0.0090}});
  private static final DoubleMatrix STRANGLE = DoubleMatrix.ofUnsafe(new double[][] {
      {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120},
      {0.0330, 0.0130}, {0.0340, 0.0140}, {0.0340, 0.0140}});
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      InterpolatedStrikeSmileDeltaTermStructure.of(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_365F);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackFxOptionSmileVolatilities VOLS =
      BlackFxOptionSmileVolatilities.of(FxOptionVolatilitiesName.of("Test"), CURRENCY_PAIR, VAL_DATE_TIME, SMILE_TERM);

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2025, 5, 13);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  private static final ResolvedFxVanillaOption OPTION_PRODUCT1 = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(OPTION1_EXPIRY)
      .underlying(FX_PRODUCT)
      .build();
  private static final ResolvedFxVanillaOption OPTION_PRODUCT2 = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(OPTION2_EXPIRY)
      .underlying(FX_PRODUCT)
      .build();
  private static final ResolvedFxCollar COLLAR_PRODUCT = ResolvedFxCollar.of(OPTION_PRODUCT1, OPTION_PRODUCT2);

  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VAL_DATE).build();
  private static final LocalDate CASH_SETTLE_DATE = LocalDate.of(2014, 1, 25);
  private static final Payment PREMIUM = Payment.of(EUR, NOTIONAL * 0.027, CASH_SETTLE_DATE);
  private static final ResolvedFxCollarTrade COLLAR_TRADE = ResolvedFxCollarTrade.of(TRADE_INFO, COLLAR_PRODUCT, PREMIUM);

  private static final DiscountingFxCollarProductPricer PRICER_PRODUCT = DiscountingFxCollarProductPricer.DEFAULT;
  private static final DiscountingFxCollarTradePricer PRICER_TRADE = DiscountingFxCollarTradePricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;
  private static final double TOL = 1.0e-13;

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount pvSensiTrade = PRICER_TRADE.presentValue(COLLAR_TRADE, RATES_PROVIDER, VOLS);
    CurrencyAmount pvSensiProduct = PRICER_PRODUCT.presentValue(COLLAR_PRODUCT, RATES_PROVIDER, VOLS);
    CurrencyAmount pvSensiPremium = PRICER_PAYMENT.presentValue(PREMIUM, RATES_PROVIDER);
    assertThat(pvSensiTrade).isEqualTo(MultiCurrencyAmount.of(pvSensiProduct, pvSensiPremium));
  }

  @Test
  public void test_presentValueSensitivity() {
    PointSensitivities pvSensiTrade = PRICER_TRADE.presentValueSensitivityRatesStickyStrike(
        COLLAR_TRADE, RATES_PROVIDER, VOLS);
    PointSensitivities pvSensiProduct = PRICER_PRODUCT.presentValueSensitivityRatesStickyStrike(
        COLLAR_PRODUCT, RATES_PROVIDER, VOLS);
    PointSensitivities pvSensiPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM, RATES_PROVIDER).build();
    assertThat(pvSensiTrade).isEqualTo(pvSensiProduct.combinedWith(pvSensiPremium));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility() {
    PointSensitivities pvSensiTrade =
        PRICER_TRADE.presentValueSensitivityModelParamsVolatility(COLLAR_TRADE, RATES_PROVIDER, VOLS);
    PointSensitivities pvSensiProduct =
        PRICER_PRODUCT.presentValueSensitivityModelParamsVolatility(COLLAR_PRODUCT, RATES_PROVIDER, VOLS).build();
    assertThat(pvSensiTrade).isEqualTo(pvSensiProduct);
  }

  @Test
  public void test_currencyExposure() {
    double dfBaseSpot = RATES_PROVIDER.discountFactor(EUR, SPOT_DATE);
    double dfCounterSpot = RATES_PROVIDER.discountFactor(USD, SPOT_DATE);
    double adjustedFxSpotScalingFactorInv = dfCounterSpot / dfBaseSpot;

    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(COLLAR_TRADE, RATES_PROVIDER, VOLS);

    PointSensitivities point = PRICER_TRADE.presentValueSensitivityRatesStickyStrike(COLLAR_TRADE, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount pv = PRICER_TRADE.presentValue(COLLAR_TRADE, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount ceExpected = RATES_PROVIDER.currencyExposure(point);
    double eurAmount = ceExpected.getAmount(EUR).getAmount() * adjustedFxSpotScalingFactorInv;
    MultiCurrencyAmount ceExpectedAdj = MultiCurrencyAmount.of(CurrencyAmount.of(EUR, eurAmount), CurrencyAmount.of(USD, ceExpected.getAmount(USD).getAmount())).plus(pv);

    assertThat(ceComputed.size()).isEqualTo(2);
    assertThat(ceComputed.getAmount(EUR).getAmount()).isCloseTo(ceExpectedAdj.getAmount(EUR).getAmount(), offset(TOL * NOTIONAL));
    assertThat(ceComputed.getAmount(USD).getAmount()).isCloseTo(ceExpectedAdj.getAmount(USD).getAmount(), offset(TOL * NOTIONAL));
  }

  @Test
  public void test_currentCash_zero() {
    assertThat(PRICER_TRADE.currentCash(COLLAR_TRADE, VAL_DATE)).isEqualTo(CurrencyAmount.zero(PREMIUM.getCurrency()));
  }

  @Test
  public void test_currentCash_onSettle() {
    assertThat(PRICER_TRADE.currentCash(COLLAR_TRADE, CASH_SETTLE_DATE)).isEqualTo(PREMIUM.getValue());
  }

  @Test
  public void test_forwardFxRate() {
    FxRate fxRateComputed = PRICER_TRADE.forwardFxRate(COLLAR_TRADE, RATES_PROVIDER);
    FxRate fxRateExpected = PRICER_PRODUCT.forwardFxRate(COLLAR_PRODUCT, RATES_PROVIDER);
    assertThat(fxRateComputed).isEqualTo(fxRateExpected);
  }

  @Test
  public void test_impliedVolatility() {
    double impVolComputed = PRICER_TRADE.impliedVolatility(COLLAR_TRADE, RATES_PROVIDER, VOLS);
    double imlVolExpected = PRICER_PRODUCT.impliedVolatility(COLLAR_PRODUCT, RATES_PROVIDER, VOLS);
    assertThat(impVolComputed).isEqualTo(imlVolExpected);
  }
}

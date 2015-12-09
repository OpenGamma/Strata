/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.BuySell.SELL;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link NormalSwaptionCashParYieldTradePricer}.
 */
@Test
public class NormalSwaptionCashParYieldTradePricerTest {

  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SWAPTION_EXERCISE_DATE = VAL_DATE.plusYears(5);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SETTLE_DATE = USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SETTLE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  private static final double NOTIONAL = 100_000_000;
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SETTLE_DATE, SWAP_MATURITY_DATE, SELL, NOTIONAL, STRIKE).getProduct();
  private static final CashSettlement PAR_YIELD = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SETTLE_DATE)
      .build();
  private static final double PREMIUM_AMOUNT = 100_000;
  private static final Swaption SWAPTION_LONG_REC = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Payment PREMIUM_FWD_PAY = Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), SETTLE_DATE);
  private static final SwaptionTrade SWAPTION_PREFWD_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_FWD_PAY)
      .build();
  private static final Payment PREMIUM_TRA_PAY = Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VAL_DATE);
  private static final SwaptionTrade SWAPTION_PRETOD_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_TRA_PAY)
      .build();
  private static final Payment PREMIUM_PAST_PAY =
      Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VAL_DATE.minusDays(1));
  private static final SwaptionTrade SWAPTION_PREPAST_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_PAST_PAY)
      .build();

  private static final ImmutableRatesProvider RATE_PROVIDER = RatesProviderDataSets.MULTI_USD.toBuilder(VAL_DATE)
      .build();
  private static final NormalVolatilityExpiryTenorSwaptionProvider VOL_PROVIDER =
      SwaptionNormalVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_STD;

  private static final NormalSwaptionCashParYieldProductPricer PRICER_PRODUCT = NormalSwaptionCashParYieldProductPricer.DEFAULT;
  private static final NormalSwaptionCashParYieldTradePricer PRICER_TRADE = NormalSwaptionCashParYieldTradePricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  private static final double TOL = 1.0e-12;

  //-------------------------------------------------------------------------
  public void present_value_premium_forward() {
    CurrencyAmount pvTrade = PRICER_TRADE.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_FWD_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
  }

  public void present_value_premium_valuedate() {
    CurrencyAmount pvTrade = PRICER_TRADE.presentValue(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_TRA_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
  }

  public void present_value_premium_past() {
    CurrencyAmount pvTrade = PRICER_TRADE.presentValue(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void currency_exposure_premium_forward() {
    CurrencyAmount pv = PRICER_TRADE.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ce = PRICER_TRADE.currencyExposure(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pv.getAmount(), ce.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void current_cash_forward() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREFWD_LONG_REC, VAL_DATE);
    assertEquals(ccTrade.getAmount(), 0, NOTIONAL * TOL);
  }

  public void current_cash_vd() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PRETOD_LONG_REC, VAL_DATE);
    assertEquals(ccTrade.getAmount(), -PREMIUM_AMOUNT, NOTIONAL * TOL);
  }

  public void current_cash_past() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREPAST_LONG_REC, VAL_DATE);
    assertEquals(ccTrade.getAmount(), 0, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivity_premium_forward() {
    PointSensitivityBuilder pvcsTrade =
        PRICER_TRADE.presentValueSensitivityStickyStrike(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivityStickyStrike(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM_FWD_PAY, RATE_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct =
        RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.combinedWith(pvcsPremium).build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_valuedate() {
    PointSensitivityBuilder pvcsTrade =
        PRICER_TRADE.presentValueSensitivityStickyStrike(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivityStickyStrike(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_past() {
    PointSensitivityBuilder pvcsTrade =
        PRICER_TRADE.presentValueSensitivityStickyStrike(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivityStickyStrike(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void present_value_normal_vol_sensitivity_premium_forward() {
    SwaptionSensitivity vegaTrade =
        PRICER_TRADE.presentValueSensitivityNormalVolatility(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity vegaProduct =
        PRICER_PRODUCT.presentValueSensitivityNormalVolatility(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(vegaTrade.getSensitivity(), vegaProduct.getSensitivity(), NOTIONAL * TOL);
  }

}

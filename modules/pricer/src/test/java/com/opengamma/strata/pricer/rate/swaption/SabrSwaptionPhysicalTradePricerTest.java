/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swaption.PhysicalSettlement;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.finance.rate.swaption.SwaptionSettlement;
import com.opengamma.strata.finance.rate.swaption.SwaptionTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Test {@link SabrSwaptionPhysicalTradePricer}.
 */
@Test
public class SabrSwaptionPhysicalTradePricerTest {
  private static final LocalDate VALUATION = LocalDate.of(2014, 1, 22);
  // swaption trades
  private static final double NOTIONAL = 100000000; //100m
  private static final double RATE = 0.0350;
  private static final int TENOR_YEAR = 7;
  private static final Tenor TENOR = Tenor.ofYears(TENOR_YEAR);
  private static final ZonedDateTime MATURITY_DATE = LocalDate.of(2016, 1, 22).atStartOfDay(ZoneOffset.UTC); // 2Y
  private static final Swap SWAP_REC = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.toTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.SELL, NOTIONAL, RATE).getProduct();
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final Swaption SWAPTION_LONG_REC = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_REC)
      .build();
  private static final double PREMIUM_AMOUNT = 100_000;
  private static final Payment PREMIUM_FWD_PAY = Payment.of(
      CurrencyAmount.of(USD, -PREMIUM_AMOUNT), MATURITY_DATE.toLocalDate());
  private static final SwaptionTrade SWAPTION_PREFWD_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_FWD_PAY)
      .build();
  private static final Payment PREMIUM_TRA_PAY = Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VALUATION);
  private static final SwaptionTrade SWAPTION_PRETOD_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_TRA_PAY)
      .build();
  private static final Payment PREMIUM_PAST_PAY =
      Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VALUATION.minusDays(1));
  private static final SwaptionTrade SWAPTION_PREPAST_LONG_REC = SwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_PAST_PAY)
      .build();
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(VALUATION);
  private static final SabrVolatilitySwaptionProvider VOL_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getVolatilityProviderUsd(VALUATION, true);

  private static final double TOL = 1.0e-12;
  private static final SabrSwaptionPhysicalTradePricer PRICER = SabrSwaptionPhysicalTradePricer.DEFAULT;
  private static final SabrSwaptionPhysicalProductPricer PRICER_PRODUCT = SabrSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void present_value_premium_forward() {
    CurrencyAmount pvTrade =
        PRICER.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_FWD_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
  }

  public void present_value_premium_valuedate() {
    CurrencyAmount pvTrade =
        PRICER.presentValue(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_TRA_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
  }

  public void present_value_premium_past() {
    CurrencyAmount pvTrade =
        PRICER.presentValue(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void currency_exposure_premium_forward() {
    CurrencyAmount pv = PRICER
        .presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount ce = PRICER
        .currencyExposure(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pv.getAmount(), ce.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void current_cash_forward() {
    CurrencyAmount ccTrade = PRICER.currentCash(SWAPTION_PREFWD_LONG_REC, VALUATION);
    assertEquals(ccTrade.getAmount(), 0, NOTIONAL * TOL);
  }

  public void current_cash_vd() {
    CurrencyAmount ccTrade = PRICER.currentCash(SWAPTION_PRETOD_LONG_REC, VALUATION);
    assertEquals(ccTrade.getAmount(), -PREMIUM_AMOUNT, NOTIONAL * TOL);
  }

  public void current_cash_past() {
    CurrencyAmount ccTrade = PRICER.currentCash(SWAPTION_PREPAST_LONG_REC, VALUATION);
    assertEquals(ccTrade.getAmount(), 0, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivity_premium_forward() {
    PointSensitivityBuilder pvcsTrade = PRICER
        .presentValueSensitivity(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivity(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM_FWD_PAY, RATE_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade =
        RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct =
        RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.combinedWith(pvcsPremium).build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_valuedate() {
    PointSensitivityBuilder pvcsTrade = PRICER
        .presentValueSensitivity(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivity(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_past() {
    PointSensitivityBuilder pvcsTrade = PRICER
        .presentValueSensitivity(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivity(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.curveParameterSensitivity(pvcsTrade.build());
    CurveCurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.curveParameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void present_value_vol_sensitivity_premium_forward() {
    SwaptionSabrSensitivity vegaTrade = PRICER
        .presentValueSensitivitySabrParameter(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSabrSensitivity vegaProduct = PRICER_PRODUCT
        .presentValueSensitivitySabrParameter(SWAPTION_LONG_REC, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(vegaTrade.getAlphaSensitivity(), vegaProduct.getAlphaSensitivity(), NOTIONAL * TOL);
    assertEquals(vegaTrade.getBetaSensitivity(), vegaProduct.getBetaSensitivity(), NOTIONAL * TOL);
    assertEquals(vegaTrade.getRhoSensitivity(), vegaProduct.getRhoSensitivity(), NOTIONAL * TOL);
    assertEquals(vegaTrade.getNuSensitivity(), vegaProduct.getNuSensitivity(), NOTIONAL * TOL);
  }
}

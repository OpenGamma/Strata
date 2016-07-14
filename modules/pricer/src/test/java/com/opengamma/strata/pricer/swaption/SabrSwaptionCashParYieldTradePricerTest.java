/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link SabrSwaptionTradePricer} for cash par yield.
 */
@Test
public class SabrSwaptionCashParYieldTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  // swaption trades
  private static final double NOTIONAL = 100000000; //100m
  private static final double RATE = 0.0350;
  private static final int TENOR_YEAR = 7;
  private static final Tenor TENOR = Tenor.ofYears(TENOR_YEAR);
  private static final ZonedDateTime MATURITY_DATE = LocalDate.of(2016, 1, 22).atStartOfDay(ZoneOffset.UTC); // 2Y
  private static final Swap SWAP_REC = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.createTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.SELL, NOTIONAL, RATE, REF_DATA).getProduct();
  private static final LocalDate SETTLE_DATE = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.getFloatingLeg()
      .getIndex().calculateEffectiveFromFixing(MATURITY_DATE.toLocalDate(), REF_DATA);
  private static final CashSwaptionSettlement PAR_YIELD =
      CashSwaptionSettlement.of(SETTLE_DATE, CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_LONG_REC = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PAR_YIELD)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final double PREMIUM_AMOUNT = 100_000;
  private static final Payment PREMIUM_FWD_PAY = Payment.of(
      CurrencyAmount.of(USD, -PREMIUM_AMOUNT), MATURITY_DATE.toLocalDate());
  private static final ResolvedSwaptionTrade SWAPTION_PREFWD_LONG_REC = ResolvedSwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_FWD_PAY)
      .build();
  private static final Payment PREMIUM_TRA_PAY = Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VAL_DATE);
  private static final ResolvedSwaptionTrade SWAPTION_PRETOD_LONG_REC = ResolvedSwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_TRA_PAY)
      .build();
  private static final Payment PREMIUM_PAST_PAY =
      Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), VAL_DATE.minusDays(1));
  private static final ResolvedSwaptionTrade SWAPTION_PREPAST_LONG_REC = ResolvedSwaptionTrade.builder()
      .product(SWAPTION_LONG_REC)
      .premium(PREMIUM_PAST_PAY)
      .build();
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(VAL_DATE);
  private static final SabrSwaptionVolatilities VOLS =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, true);

  private static final double TOL = 1.0e-12;
  private static final VolatilitySwaptionTradePricer PRICER_COMMON = VolatilitySwaptionTradePricer.DEFAULT;
  private static final SabrSwaptionTradePricer PRICER_TRADE = SabrSwaptionTradePricer.DEFAULT;
  private static final SabrSwaptionCashParYieldProductPricer PRICER_PRODUCT =
      SabrSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void present_value_premium_forward() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_FWD_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
    // test via VolatilitySwaptionTradePricer
    CurrencyAmount pv = PRICER_COMMON.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    assertEquals(pv, pvTrade);
  }

  public void present_value_premium_valuedate() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_TRA_PAY, RATE_PROVIDER);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount() + pvPremium.getAmount(), NOTIONAL * TOL);
  }

  public void present_value_premium_past() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void currency_exposure_premium_forward() {
    CurrencyAmount pv = PRICER_TRADE
        .presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ce = PRICER_TRADE
        .currencyExposure(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
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
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM_FWD_PAY, RATE_PROVIDER);
    CurrencyParameterSensitivities pvpsTrade =
        RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct =
        RATE_PROVIDER.parameterSensitivity(pvcsProduct.combinedWith(pvcsPremium).build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_valuedate() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.parameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  public void present_value_sensitivity_premium_past() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.parameterSensitivity(pvcsProduct.build());
    assertTrue(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void present_value_vol_sensitivity_premium_forward() {
    PointSensitivities vegaTrade = PRICER_TRADE
        .presentValueSensitivityModelParamsSabr(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivities vegaProduct = PRICER_PRODUCT
        .presentValueSensitivityModelParamsSabr(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS).build();
    assertEquals(vegaTrade, vegaProduct);
  }

}

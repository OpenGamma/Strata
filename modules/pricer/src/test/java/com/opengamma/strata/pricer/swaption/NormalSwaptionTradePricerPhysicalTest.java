/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Tests {@link NormalSwaptionTradePricer} for physical.
 */
public class NormalSwaptionTradePricerPhysicalTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SWAPTION_EXERCISE_DATE = VAL_DATE.plusYears(5);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE =
      USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  private static final double NOTIONAL = 100_000_000;
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, SELL, NOTIONAL, STRIKE).getProduct();
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;

  private static final double PREMIUM_AMOUNT = 100_000;
  private static final ResolvedSwaption SWAPTION_LONG_REC = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final Payment PREMIUM_FWD_PAY = Payment.of(CurrencyAmount.of(USD, -PREMIUM_AMOUNT), SWAP_EFFECTIVE_DATE);
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

  private static final VolatilitySwaptionTradePricer PRICER_COMMON = VolatilitySwaptionTradePricer.DEFAULT;
  private static final NormalSwaptionTradePricer PRICER_TRADE = NormalSwaptionTradePricer.DEFAULT;
  private static final NormalSwaptionPhysicalProductPricer PRICER_PRODUCT = NormalSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  private static final ImmutableRatesProvider MULTI_USD = RatesProviderDataSets.multiUsd(VAL_DATE);
  private static final NormalSwaptionExpiryTenorVolatilities NORMAL_VOLS_USD =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_STD;

  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E+2;
  private static final double TOLERANCE_PV_VEGA = 1.0E+4;

  //-------------------------------------------------------------------------
  @Test
  void present_value_premium_forward() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_FWD_PAY, MULTI_USD);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount() + pvPremium.getAmount(), offset(TOLERANCE_PV));
    // test via VolatilitySwaptionTradePricer
    CurrencyAmount pv = PRICER_COMMON.presentValue(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    assertThat(pv).isEqualTo(pvTrade);
  }

  @Test
  void present_value_premium_valuedate() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PRETOD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_TRA_PAY, MULTI_USD);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount() + pvPremium.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_premium_past() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREPAST_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount(), offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void currency_exposure_premium_forward() {
    CurrencyAmount pv = PRICER_TRADE
        .presentValue(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    MultiCurrencyAmount ce = PRICER_TRADE
        .currencyExposure(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    assertThat(pv.getAmount()).isCloseTo(ce.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void current_cash_forward() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREFWD_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(0, offset(TOLERANCE_PV));
  }

  @Test
  void current_cash_vd() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PRETOD_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(-PREMIUM_AMOUNT, offset(TOLERANCE_PV));
  }

  @Test
  void current_cash_past() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREPAST_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(0, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_sensitivity_premium_forward() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyStrike(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    PointSensitivityBuilder pvcsPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM_FWD_PAY, MULTI_USD);
    CurrencyParameterSensitivities pvpsTrade = MULTI_USD.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct =
        MULTI_USD.parameterSensitivity(pvcsProduct.combinedWith(pvcsPremium).build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, TOLERANCE_PV_DELTA)).isTrue();
  }

  @Test
  void present_value_sensitivity_premium_valuedate() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PRETOD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyStrike(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyParameterSensitivities pvpsTrade = MULTI_USD.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = MULTI_USD.parameterSensitivity(pvcsProduct.build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, TOLERANCE_PV_DELTA)).isTrue();
  }

  @Test
  void present_value_sensitivity_premium_past() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PREPAST_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyStrike(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    CurrencyParameterSensitivities pvpsTrade = MULTI_USD.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = MULTI_USD.parameterSensitivity(pvcsProduct.build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, TOLERANCE_PV_DELTA)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_normal_vol_sensitivity_premium_forward() {
    PointSensitivities vegaTrade = PRICER_TRADE
        .presentValueSensitivityModelParamsVolatility(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    SwaptionSensitivity vegaProduct = PRICER_PRODUCT
        .presentValueSensitivityModelParamsVolatility(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    assertThat(vegaTrade.getSensitivities().get(0).getSensitivity()).isCloseTo(vegaProduct.getSensitivity(), offset(TOLERANCE_PV_VEGA));
  }

  @Test
  void implied_volatiltity() {
    double impliedVolTrade = PRICER_TRADE.impliedVolatility(SWAPTION_PREFWD_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    double impliedVolProduct = PRICER_PRODUCT.impliedVolatility(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD);
    assertThat(impliedVolProduct).isEqualTo(impliedVolTrade);
  }

  @Test
  void forward_rate() {
    double forwardRateTrade = PRICER_TRADE.forwardRate(SWAPTION_PREFWD_LONG_REC, MULTI_USD);
    double forwardRateProduct = PRICER_PRODUCT.forwardRate(SWAPTION_LONG_REC, MULTI_USD);
    assertThat(forwardRateTrade).isEqualTo(forwardRateProduct);
  }

}

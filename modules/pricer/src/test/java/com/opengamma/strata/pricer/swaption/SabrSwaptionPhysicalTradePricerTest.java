/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

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
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Test {@link SabrSwaptionTradePricer} for physical.
 */
public class SabrSwaptionPhysicalTradePricerTest {

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
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final ResolvedSwaption SWAPTION_LONG_REC = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
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
  private static final SabrSwaptionPhysicalProductPricer PRICER_PRODUCT = SabrSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  //-------------------------------------------------------------------------
  @Test
  void present_value_premium_forward() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_FWD_PAY, RATE_PROVIDER);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount() + pvPremium.getAmount(), offset(NOTIONAL * TOL));
    // test via VolatilitySwaptionTradePricer
    CurrencyAmount pv = PRICER_COMMON.presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    assertThat(pv).isEqualTo(pvTrade);
  }

  @Test
  void present_value_premium_valuedate() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPremium = PRICER_PAYMENT.presentValue(PREMIUM_TRA_PAY, RATE_PROVIDER);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount() + pvPremium.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  void present_value_premium_past() {
    CurrencyAmount pvTrade =
        PRICER_TRADE.presentValue(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyAmount pvProduct =
        PRICER_PRODUCT.presentValue(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    assertThat(pvTrade.getAmount()).isCloseTo(pvProduct.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void currency_exposure_premium_forward() {
    CurrencyAmount pv = PRICER_TRADE
        .presentValue(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ce = PRICER_TRADE
        .currencyExposure(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    assertThat(pv.getAmount()).isCloseTo(ce.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void current_cash_forward() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREFWD_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(0, offset(NOTIONAL * TOL));
  }

  @Test
  void current_cash_vd() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PRETOD_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(-PREMIUM_AMOUNT, offset(NOTIONAL * TOL));
  }

  @Test
  void current_cash_past() {
    CurrencyAmount ccTrade = PRICER_TRADE.currentCash(SWAPTION_PREPAST_LONG_REC, VAL_DATE);
    assertThat(ccTrade.getAmount()).isCloseTo(0, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_sensitivity_premium_forward() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsPremium = PRICER_PAYMENT.presentValueSensitivity(PREMIUM_FWD_PAY, RATE_PROVIDER);
    CurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct =
        RATE_PROVIDER.parameterSensitivity(pvcsProduct.combinedWith(pvcsPremium).build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL)).isTrue();
  }

  @Test
  void present_value_sensitivity_premium_valuedate() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PRETOD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.parameterSensitivity(pvcsProduct.build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL)).isTrue();
  }

  @Test
  void present_value_sensitivity_premium_past() {
    PointSensitivities pvcsTrade = PRICER_TRADE
        .presentValueSensitivityRatesStickyModel(SWAPTION_PREPAST_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pvcsProduct = PRICER_PRODUCT
        .presentValueSensitivityRatesStickyModel(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities pvpsTrade = RATE_PROVIDER.parameterSensitivity(pvcsTrade);
    CurrencyParameterSensitivities pvpsProduct = RATE_PROVIDER.parameterSensitivity(pvcsProduct.build());
    assertThat(pvpsTrade.equalWithTolerance(pvpsProduct, NOTIONAL * NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_vol_sensitivity_premium_forward() {
    PointSensitivities vegaTrade = PRICER_TRADE
        .presentValueSensitivityModelParamsSabr(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    PointSensitivities vegaProduct = PRICER_PRODUCT
        .presentValueSensitivityModelParamsSabr(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS).build();
    assertThat(vegaTrade).isEqualTo(vegaProduct);
  }

  @Test
  void implied_volatiltity() {
    double impliedVolTrade = PRICER_TRADE.impliedVolatility(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER, VOLS);
    double impliedVolProduct = PRICER_PRODUCT.impliedVolatility(SWAPTION_LONG_REC, RATE_PROVIDER, VOLS);
    assertThat(impliedVolProduct).isEqualTo(impliedVolTrade);
  }

  @Test
  void forward_rate() {
    double forwardRateTrade = PRICER_TRADE.forwardRate(SWAPTION_PREFWD_LONG_REC, RATE_PROVIDER);
    double forwardRateProduct = PRICER_PRODUCT.forwardRate(SWAPTION_LONG_REC, RATE_PROVIDER);
    assertThat(forwardRateTrade).isEqualTo(forwardRateProduct);
  }
}

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Test {@link SabrSwaptionPhysicalProductPricer}.
 */
public class SabrSwaptionPhysicalProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  // swaptions
  private static final double NOTIONAL = 100000000; //100m
  private static final double RATE = 0.0350;
  private static final int TENOR_YEAR = 7;
  private static final Tenor TENOR = Tenor.ofYears(TENOR_YEAR);
  private static final ZonedDateTime MATURITY_DATE = LocalDate.of(2016, 1, 22).atStartOfDay(ZoneOffset.UTC); // 2Y
  private static final Swap SWAP_PAY = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.createTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.BUY, NOTIONAL, RATE, REF_DATA).getProduct();
  private static final ResolvedSwap RSWAP_PAY = SWAP_PAY.resolve(REF_DATA);
  private static final Swap SWAP_REC = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.createTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.SELL, NOTIONAL, RATE, REF_DATA).getProduct();
  private static final ResolvedSwap RSWAP_REC = SWAP_REC.resolve(REF_DATA);
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE =
      CashSwaptionSettlement.of(SWAP_REC.getStartDate().getUnadjusted(), CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.SHORT)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.SHORT)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_CASH = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(CASH_SETTLE)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(VAL_DATE);
  private static final ImmutableRatesProvider RATE_PROVIDER_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(MATURITY_DATE.toLocalDate());
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(MATURITY_DATE.toLocalDate().plusDays(1));
  private static final SabrParametersSwaptionVolatilities VOLS =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, true);
  private static final SabrParametersSwaptionVolatilities VOLS_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(MATURITY_DATE.toLocalDate(), true);
  private static final SabrParametersSwaptionVolatilities VOLS_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(MATURITY_DATE.toLocalDate().plusDays(1), true);
  private static final SabrParametersSwaptionVolatilities VOLS_REGRESSION =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, false);
  // test parameters and calculator
  private static final double TOL = 1.0e-13;
  private static final double TOLERANCE_DELTA = 1.0E-2;
  private static final double FD_EPS = 1.0e-6;
  private static final double TOLERANCE_RATE = 1.0E-8;
  private static final double REGRESSION_TOL = 1.0e-4; // due to tenor computation difference
  private static final SabrSwaptionPhysicalProductPricer SWAPTION_PRICER = SabrSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  @Test
  void validate_physical_settlement() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SWAPTION_PRICER.presentValue(SWAPTION_CASH, RATE_PROVIDER, VOLS));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValue() {
    CurrencyAmount computedRec = SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double pvbp = SWAP_PRICER.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(),
        TENOR_YEAR, RATE, forward);
    double maturity = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double expectedRec = pvbp * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility, false);
    double expectedPay = -pvbp * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility, true);
    assertThat(computedRec.getCurrency()).isEqualTo(USD);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(USD);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double swapPV = SWAP_PRICER.presentValue(RSWAP_REC, RATE_PROVIDER_AT_MATURITY).getAmount(USD).getAmount();
    assertThat(computedRec.getAmount()).isCloseTo(swapPV, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_parity() {
    CurrencyAmount pvRecLong = SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvRecShort = SWAPTION_PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayLong = SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayShort = SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvRecLong.getAmount()).isCloseTo(-pvRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvPayLong.getAmount()).isCloseTo(-pvPayShort.getAmount(), offset(NOTIONAL * TOL));
    double swapPV = SWAP_PRICER.presentValue(RSWAP_PAY, RATE_PROVIDER).getAmount(USD).getAmount();
    assertThat(pvPayLong.getAmount() - pvRecLong.getAmount()).isCloseTo(swapPV, offset(NOTIONAL * TOL));
    assertThat(pvPayShort.getAmount() - pvRecShort.getAmount()).isCloseTo(-swapPV, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_parity_atMaturity() {
    CurrencyAmount pvRecLong =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvRecShort =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvPayLong =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvPayShort =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(pvRecLong.getAmount()).isCloseTo(-pvRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvPayLong.getAmount()).isCloseTo(-pvPayShort.getAmount(), offset(NOTIONAL * TOL));
    double swapPV = SWAP_PRICER.presentValue(RSWAP_PAY, RATE_PROVIDER_AT_MATURITY).getAmount(USD).getAmount();
    assertThat(pvPayLong.getAmount() - pvRecLong.getAmount()).isCloseTo(swapPV, offset(NOTIONAL * TOL));
    assertThat(pvPayShort.getAmount() - pvRecShort.getAmount()).isCloseTo(-swapPV, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_currencyExposure() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(expectedRec.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(expectedPay.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(expectedRec.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(expectedPay.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_impliedVolatility() {
    double computedRec = SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    double computedPay = SWAPTION_PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOLS.volatility(MATURITY_DATE, TENOR_YEAR, RATE, forward);
    assertThat(computedRec).isCloseTo(expected, offset(TOL));
    assertThat(computedPay).isCloseTo(expected, offset(TOL));
  }

  @Test
  void test_impliedVolatility_atMaturity() {
    double computedRec =
        SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double computedPay =
        SWAPTION_PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double expected = VOLS_AT_MATURITY.volatility(MATURITY_DATE, TENOR_YEAR, RATE, forward);
    assertThat(computedRec).isCloseTo(expected, offset(TOL));
    assertThat(computedPay).isCloseTo(expected, offset(TOL));
  }

  @Test
  void test_impliedVolatility_afterMaturity() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SWAPTION_PRICER.impliedVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SWAPTION_PRICER.impliedVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueDelta_parity() {
    double pvbp = SWAP_PRICER.getLegPricer()
        .pvbp(SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    CurrencyAmount deltaRec = SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount deltaPay = SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(deltaRec.getAmount() + deltaPay.getAmount()).isCloseTo(-pvbp, offset(TOLERANCE_DELTA));
  }

  @Test
  void test_presentValueDelta_afterMaturity() {
    CurrencyAmount deltaRec =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(deltaRec.getAmount()).isCloseTo(0, offset(TOLERANCE_DELTA));
    CurrencyAmount deltaPay =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(deltaPay.getAmount()).isCloseTo(0, offset(TOLERANCE_DELTA));
  }

  @Test
  void test_presentValueDelta_atMaturity() {
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double pvbp = SWAP_PRICER.getLegPricer()
        .pvbp(SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER_AT_MATURITY);
    CurrencyAmount deltaRec =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(deltaRec.getAmount()).isCloseTo(RATE > forward ? -pvbp : 0, offset(TOLERANCE_DELTA));
    CurrencyAmount deltaPay =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(deltaPay.getAmount()).isCloseTo(RATE > forward ? 0 : pvbp, offset(TOLERANCE_DELTA));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d)).isTrue();
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, (p), VOLS));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d)).isTrue();
  }

  @Test
  void test_presentValueSensitivityRatesStickyModel_stickyStrike() {
    SwaptionVolatilities volSabr = SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, false);
    double impliedVol = SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    SurfaceMetadata blackMeta =
        Surfaces.blackVolatilityByExpiryTenor("CST", VOLS.getDayCount());
    SwaptionVolatilities volCst = BlackSwaptionExpiryTenorVolatilities.of(
        VOLS.getConvention(), VOLS.getValuationDateTime(), ConstantSurface.of(blackMeta, impliedVol));
    // To obtain a constant volatility surface which create a sticky strike sensitivity
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), volCst));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d)).isTrue();

    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, volSabr);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, (p), volCst));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d)).isTrue();
  }

  @Test
  void test_presentValueSensitivityRatesStickyModel_atMaturity() {
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyParameterSensitivities computedRec =
        RATE_PROVIDER_AT_MATURITY.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATE_PROVIDER_AT_MATURITY, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS_AT_MATURITY));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d)).isTrue();
    PointSensitivities pointPay = SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT,
        RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  void test_presentValueSensitivityRatesStickyModel_afterMaturity() {
    PointSensitivities pointRec = SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
    PointSensitivities pointPay = SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  void test_presentValueSensitivity_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build());
    assertThat(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();

    CurrencyParameterSensitivities pvSensiSwap = RATE_PROVIDER.parameterSensitivity(
        SWAP_PRICER.presentValueSensitivity(RSWAP_PAY, RATE_PROVIDER).build());
    assertThat(pvSensiSwap.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiSwap.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueVega_parity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity vegaPay = SWAPTION_PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(vegaRec.getSensitivity()).isCloseTo(-vegaPay.getSensitivity(), offset(TOLERANCE_DELTA));
  }

  @Test
  void test_presentValueVega_atMaturity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(vegaRec.getSensitivity()).isCloseTo(0, offset(TOLERANCE_DELTA));
    SwaptionSensitivity vegaPay = SWAPTION_PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(vegaPay.getSensitivity()).isCloseTo(0, offset(TOLERANCE_DELTA));
  }

  @Test
  void test_presentValueVega_afterMaturity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(vegaRec.getSensitivity()).isCloseTo(0, offset(TOLERANCE_DELTA));
    SwaptionSensitivity vegaPay = SWAPTION_PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(vegaPay.getSensitivity()).isCloseTo(0, offset(TOLERANCE_DELTA));
  }

  @Test
  void test_presentValueVega_SwaptionSensitivity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    assertThat(VOLS.parameterSensitivity(vegaRec)).isEqualTo(CurrencyParameterSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivities sensiRec =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities sensiPay =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build();
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double pvbp = SWAP_PRICER.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(),
        TENOR_YEAR, RATE, forward);
    double maturity = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double[] volSensi = VOLS.getParameters()
        .volatilityAdjoint(maturity, TENOR_YEAR, RATE, forward).getDerivatives().toArray();
    double vegaRec = pvbp * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility);
    double vegaPay = -pvbp * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility);
    assertSensitivity(sensiRec, SabrParameterType.ALPHA, vegaRec * volSensi[2], TOL);
    assertSensitivity(sensiRec, SabrParameterType.BETA, vegaRec * volSensi[3], TOL);
    assertSensitivity(sensiRec, SabrParameterType.RHO, vegaRec * volSensi[4], TOL);
    assertSensitivity(sensiRec, SabrParameterType.NU, vegaRec * volSensi[5], TOL);
    assertSensitivity(sensiPay, SabrParameterType.ALPHA, vegaPay * volSensi[2], TOL);
    assertSensitivity(sensiPay, SabrParameterType.BETA, vegaPay * volSensi[3], TOL);
    assertSensitivity(sensiPay, SabrParameterType.RHO, vegaPay * volSensi[4], TOL);
    assertSensitivity(sensiPay, SabrParameterType.NU, vegaPay * volSensi[5], TOL);
  }

  private void assertSensitivity(PointSensitivities points, SabrParameterType type, double expected, double tol) {
    for (PointSensitivity point : points.getSensitivities()) {
      SwaptionSabrSensitivity sens = (SwaptionSabrSensitivity) point;
      assertThat(sens.getCurrency()).isEqualTo(USD);
      assertThat(sens.getVolatilitiesName()).isEqualTo(VOLS.getName());
      assertThat(sens.getTenor()).isEqualTo(TENOR_YEAR);
      if (sens.getSensitivityType() == type) {
        assertThat(sens.getSensitivity()).isCloseTo(expected, offset(NOTIONAL * tol));
        return;
      }
    }
    fail("Did not find sensitivity: " + type + " in " + points);
  }

  @Test
  void test_presentValueSensitivityModelParamsSabr_atMaturity() {
    PointSensitivities sensiRec = SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    assertSensitivity(sensiRec, SabrParameterType.ALPHA, 0, TOL);
    assertSensitivity(sensiRec, SabrParameterType.BETA, 0, TOL);
    assertSensitivity(sensiRec, SabrParameterType.RHO, 0, TOL);
    assertSensitivity(sensiRec, SabrParameterType.NU, 0, TOL);
    PointSensitivities sensiPay = SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    assertSensitivity(sensiPay, SabrParameterType.ALPHA, 0, TOL);
    assertSensitivity(sensiPay, SabrParameterType.BETA, 0, TOL);
    assertSensitivity(sensiPay, SabrParameterType.RHO, 0, TOL);
    assertSensitivity(sensiPay, SabrParameterType.NU, 0, TOL);
  }

  @Test
  void test_presentValueSensitivityModelParamsSabr_afterMaturity() {
    PointSensitivities sensiRec = SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    assertThat(sensiRec.getSensitivities()).hasSize(0);
    PointSensitivities sensiPay = SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    assertThat(sensiPay.getSensitivities()).hasSize(0);
  }

  @Test
  void test_presentValueSensitivityModelParamsSabr_parity() {
    PointSensitivities pvSensiRecLong =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiRecShort =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiPayLong =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiPayShort =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build();

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.ALPHA, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.ALPHA, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.ALPHA, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.ALPHA, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.BETA, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.BETA, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.BETA, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.BETA, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.RHO, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.RHO, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.RHO, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.RHO, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.NU, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.NU, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.NU, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.NU, 1);
  }

  private void assertSensitivity(
      PointSensitivities points1,
      PointSensitivities points2,
      SabrParameterType type,
      int factor) {

    // use ordinal() as a hack to find correct type
    assertThat(points1.getSensitivities().get(type.ordinal()).getSensitivity()).isCloseTo(points2.getSensitivities().get(type.ordinal()).getSensitivity() * factor, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void regressionPv() {
    CurrencyAmount pvComputed = SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REGRESSION);
    assertThat(pvComputed.getAmount()).isCloseTo(3156216.489577751, offset(REGRESSION_TOL * NOTIONAL));
  }

  @Test
  void regressionPvCurveSensi() {
    PointSensitivityBuilder point =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REGRESSION);
    CurrencyParameterSensitivities sensiComputed = RATE_PROVIDER.parameterSensitivity(point.build());
    final double[] deltaDsc = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 109037.92080563342, 637123.4570377409,
        -931862.187003511, -2556192.7520530378, -4233440.216336116, -5686205.439275854, -6160338.898970505,
        -3709275.494841247, 0.0};
    final double[] deltaFwd = {0.0, 0.0, 0.0, 0.0, -1.0223186788452002E8, 2506923.9169937484, 4980364.73045286,
        1.254633556119663E7, 1.528160539036628E8, 2.5824191204559547E8, 0.0, 0.0, 0.0, 0.0, 0.0};
    CurrencyParameterSensitivities sensiExpected = CurrencyParameterSensitivities.of(
        SwaptionSabrRateVolatilityDataSet.CURVE_DSC_USD.createParameterSensitivity(USD, DoubleArray.copyOf(deltaDsc)),
        SwaptionSabrRateVolatilityDataSet.CURVE_FWD_USD.createParameterSensitivity(USD, DoubleArray.copyOf(deltaFwd)));
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * REGRESSION_TOL)).isTrue();
  }

  @Test
  void regressionPvSurfaceSensi() {
    PointSensitivities pointComputed =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REGRESSION).build();
    assertSensitivity(pointComputed, SabrParameterType.ALPHA, 6.5786313367554754E7, REGRESSION_TOL);
    assertSensitivity(pointComputed, SabrParameterType.BETA, -1.2044275797229866E7, REGRESSION_TOL);
    assertSensitivity(pointComputed, SabrParameterType.RHO, 266223.51118849067, REGRESSION_TOL);
    assertSensitivity(pointComputed, SabrParameterType.NU, 400285.5505271345, REGRESSION_TOL);
    CurrencyParameterSensitivities sensiComputed =
        VOLS_REGRESSION.parameterSensitivity(pointComputed);
    double[][] alphaExp = new double[][] {
        {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {5.0, 1.0, 0.0}, {10.0, 1.0, 0.0},
        {0.0, 5.0, 0.0}, {0.5, 5.0, 0.0}, {1.0, 5.0, 6204.475194599179}, {2.0, 5.0, 3.94631212984123E7},
        {5.0, 5.0, 0.0}, {10.0, 5.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {1.0, 10.0, 4136.961894403858},
        {2.0, 10.0, 2.631285063205345E7}, {5.0, 10.0, 0.0}, {10.0, 10.0, 0.0},};
    double[][] betaExp = new double[][] {
        {0.0, 1.0, -0.0}, {0.5, 1.0, -0.0}, {1.0, 1.0, -0.0}, {2.0, 1.0, -0.0}, {5.0, 1.0, -0.0},
        {10.0, 1.0, -0.0}, {0.0, 5.0, -0.0}, {0.5, 5.0, -0.0}, {1.0, 5.0, -1135.926404680998},
        {2.0, 5.0, -7224978.759366533}, {5.0, 5.0, -0.0}, {10.0, 5.0, -0.0}, {0.0, 10.0, -0.0}, {0.5, 10.0, -0.0},
        {1.0, 10.0, -757.402375482629}, {2.0, 10.0, -4817403.70908317}, {5.0, 10.0, -0.0}, {10.0, 10.0, -0.0}};
    double[][] rhoExp = new double[][] {
        {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {5.0, 1.0, 0.0}, {10.0, 1.0, 0.0},
        {0.0, 5.0, 0.0}, {0.5, 5.0, 0.0}, {1.0, 5.0, 25.10821912392996}, {2.0, 5.0, 159699.03429338703},
        {5.0, 5.0, 0.0}, {10.0, 5.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {1.0, 10.0, 16.741423326578513},
        {2.0, 10.0, 106482.62725265314}, {5.0, 10.0, 0.0}, {10.0, 10.0, 0.0}};
    double[][] nuExp = new double[][] {
        {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {5.0, 1.0, 0.0}, {10.0, 1.0, 0.0},
        {0.0, 5.0, 0.0}, {0.5, 5.0, 0.0}, {1.0, 5.0, 37.751952372314484}, {2.0, 5.0, 240118.59649585965},
        {5.0, 5.0, 0.0}, {10.0, 5.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {1.0, 10.0, 25.171893432592533},
        {2.0, 10.0, 160104.03018547}, {5.0, 10.0, 0.0}, {10.0, 10.0, 0.0}};
    double[][][] exps = new double[][][] {alphaExp, betaExp, rhoExp, nuExp};
    SurfaceMetadata[] metadata = new SurfaceMetadata[] {SwaptionSabrRateVolatilityDataSet.META_ALPHA,
        SwaptionSabrRateVolatilityDataSet.META_BETA_USD, SwaptionSabrRateVolatilityDataSet.META_RHO,
        SwaptionSabrRateVolatilityDataSet.META_NU};
    // x-y-value order does not match sorted order in surface, thus sort it
    CurrencyParameterSensitivities sensiExpected = CurrencyParameterSensitivities.empty();
    for (int i = 0; i < exps.length; ++i) {
      int size = exps[i].length;
      Map<DoublesPair, Double> sensiMap = new TreeMap<>();
      for (int j = 0; j < size; ++j) {
        sensiMap.put(DoublesPair.of(exps[i][j][0], exps[i][j][1]), exps[i][j][2]);
      }
      List<ParameterMetadata> paramMetadata = new ArrayList<>(size);
      List<Double> sensi = new ArrayList<>();
      for (Entry<DoublesPair, Double> entry : sensiMap.entrySet()) {
        paramMetadata.add(SwaptionSurfaceExpiryTenorParameterMetadata.of(
            entry.getKey().getFirst(), entry.getKey().getSecond()));
        sensi.add(entry.getValue());
      }
      SurfaceMetadata surfaceMetadata = metadata[i].withParameterMetadata(paramMetadata);
      sensiExpected = sensiExpected.combinedWith(
          CurrencyParameterSensitivity.of(
              surfaceMetadata.getSurfaceName(),
              surfaceMetadata.getParameterMetadata().get(),
              USD,
              DoubleArray.copyOf(sensi)));
    }
    testSurfaceParameterSensitivities(sensiComputed, sensiExpected, REGRESSION_TOL * NOTIONAL);
  }

  //-------------------------------------------------------------------------
  private void testSurfaceParameterSensitivities(
      CurrencyParameterSensitivities computed,
      CurrencyParameterSensitivities expected,
      double tol) {
    List<CurrencyParameterSensitivity> listComputed = new ArrayList<>(computed.getSensitivities());
    List<CurrencyParameterSensitivity> listExpected = new ArrayList<>(expected.getSensitivities());
    for (CurrencyParameterSensitivity sensExpected : listExpected) {
      int index = Math.abs(Collections.binarySearch(listComputed, sensExpected,
          CurrencyParameterSensitivity::compareKey));
      CurrencyParameterSensitivity sensComputed = listComputed.get(index);
      int nSens = sensExpected.getParameterCount();
      assertThat(sensComputed.getParameterCount()).isEqualTo(nSens);
      for (int i = 0; i < nSens; ++i) {
        assertThat(sensComputed.getSensitivity().get(i)).isCloseTo(sensExpected.getSensitivity().get(i), offset(tol));
      }
      listComputed.remove(index);
    }
  }

}

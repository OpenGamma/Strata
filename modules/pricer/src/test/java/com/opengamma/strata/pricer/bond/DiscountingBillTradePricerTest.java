/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.Bill;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.BillYieldConvention;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Test {@link DiscountingBillTradePricer}
 */
@Test
public class DiscountingBillTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2018, 6, 20);

  // Bill
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG-Ticker", "GOVT1");
  private static final BillYieldConvention YIELD_CONVENTION = BillYieldConvention.INTEREST_AT_MATURITY;
  
  private static final HolidayCalendarId EUR_CALENDAR = HolidayCalendarIds.EUTA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUR_CALENDAR);
  private static final double NOTIONAL_AMOUNT = 1_000_000;
  private static final LocalDate MATURITY_DATE = LocalDate.of(2018, 12, 12);
  private static final AdjustableDate MATURITY_DATE_ADJ = AdjustableDate.of(MATURITY_DATE, BUSINESS_ADJUST);
  private static final AdjustablePayment NOTIONAL = 
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL_AMOUNT), MATURITY_DATE_ADJ);
  
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(2, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final Bill BILL_PRODUCT = Bill.builder()
      .dayCount(DAY_COUNT)
      .legalEntityId(ISSUER_ID)
      .notional(NOTIONAL)
      .securityId(SECURITY_ID)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION).build();
  private static final LocalDate TRADE_DATE_BEFORE_VAL = date(2018, 6, 13);
  private static final LocalDate SETTLEMENT_DATE_BEFORE_VAL = date(2018, 6, 14);
  private static final LocalDate SETTLEMENT_DATE_ON_VAL = date(2018, 6, 20);
  private static final LocalDate SETTLEMENT_DATE_AFTER_VAL = date(2018, 6, 22);
  private static final TradeInfo TRADE_INFO_BEFORE_VAL = TradeInfo.builder()
      .tradeDate(TRADE_DATE_BEFORE_VAL)
      .settlementDate(SETTLEMENT_DATE_BEFORE_VAL)
      .build();
  private static final TradeInfo TRADE_INFO_ON_VAL = TradeInfo.builder()
      .tradeDate(TRADE_DATE_BEFORE_VAL)
      .settlementDate(SETTLEMENT_DATE_ON_VAL)
      .build();
  private static final TradeInfo TRADE_INFO_AFTER_VAL = TradeInfo.builder()
      .tradeDate(TRADE_DATE_BEFORE_VAL)
      .settlementDate(SETTLEMENT_DATE_AFTER_VAL)
      .build();
  private static final double PRICE = 0.99;
  private static final double QUANTITY = 123;
  private static final ResolvedBillTrade BILL_TRADE_SETTLE_BEFORE_VAL = BillTrade.builder()
      .info(TRADE_INFO_BEFORE_VAL)
      .product(BILL_PRODUCT)
      .price(PRICE)
      .quantity(QUANTITY).build().resolve(REF_DATA);
  private static final ResolvedBillTrade BILL_TRADE_SETTLE_ON_VAL = BillTrade.builder()
      .info(TRADE_INFO_ON_VAL)
      .product(BILL_PRODUCT)
      .price(PRICE)
      .quantity(QUANTITY).build().resolve(REF_DATA);
  private static final ResolvedBillTrade BILL_TRADE_SETTLE_AFTER_VAL = BillTrade.builder()
      .info(TRADE_INFO_AFTER_VAL)
      .product(BILL_PRODUCT)
      .price(PRICE)
      .quantity(QUANTITY).build().resolve(REF_DATA);

  // rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO = InterpolatedNodalCurve.of(
      METADATA_REPO, DoubleArray.of(0.1, 2.0, 10.0), DoubleArray.of(0.05, 0.06, 0.09), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO);
  private static final RepoGroup GROUP_REPO = RepoGroup.of("GOVT1 BOND1");
  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER = InterpolatedNodalCurve.of(
      METADATA_ISSUER, DoubleArray.of(0.2, 9.0, 15.0), DoubleArray.of(0.03, 0.05, 0.13), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
  private static final LegalEntityDiscountingProvider PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, EUR), DSC_FACTORS_ISSUER))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_ISSUER))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), DSC_FACTORS_REPO))
      .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
      .valuationDate(VAL_DATE)
      .build();
  
  // pricers
  private static final DiscountingBillProductPricer PRICER_PRODUCT = DiscountingBillProductPricer.DEFAULT;
  private static final DiscountingBillTradePricer PRICER_TRADE = DiscountingBillTradePricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;
  private static final double EPS = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALC = new RatesFiniteDifferenceSensitivityCalculator(EPS);

  private static final double Z_SPREAD = 0.035;
  private static final double TOLERANCE_PV = 1.0e-6;
  private static final double TOLERANCE_PVSENSI = 1.0e-2;

  //-------------------------------------------------------------------------
  public void test_presentValue_settle_before_val() {
    CurrencyAmount pvComputed = PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER);
    CurrencyAmount pvExpected = PRICER_PRODUCT.presentValue(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .multipliedBy(QUANTITY);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    CurrencyAmount cashComputed = PRICER_TRADE.currentCash(BILL_TRADE_SETTLE_BEFORE_VAL, VAL_DATE);
    assertEquals(cashComputed.getCurrency(), EUR);
    assertEquals(cashComputed.getAmount(), 0, TOLERANCE_PV);
  }
  
  public void test_presentValue_settle_on_val() {
    CurrencyAmount pvComputed = PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_ON_VAL, PROVIDER);
    CurrencyAmount pvExpected = PRICER_PRODUCT.presentValue(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .plus(-PRICE * NOTIONAL_AMOUNT)
        .multipliedBy(QUANTITY);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(BILL_TRADE_SETTLE_ON_VAL, PROVIDER);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    CurrencyAmount cashComputed = PRICER_TRADE.currentCash(BILL_TRADE_SETTLE_ON_VAL, VAL_DATE);
    assertEquals(cashComputed.getCurrency(), EUR);
    assertEquals(cashComputed.getAmount(), -PRICE * NOTIONAL_AMOUNT * QUANTITY, TOLERANCE_PV);
  }
  
  public void test_presentValue_settle_after_val() {
    CurrencyAmount pvComputed = PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER);
    CurrencyAmount pvExpected = PRICER_PRODUCT.presentValue(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .multipliedBy(QUANTITY)
        .plus(PRICER_PAYMENT.presentValue(BILL_TRADE_SETTLE_AFTER_VAL.getSettlement().get(), 
            PROVIDER.repoCurveDiscountFactors(BILL_PRODUCT.getSecurityId(), BILL_PRODUCT.getLegalEntityId(), BILL_PRODUCT.getCurrency())
            .getDiscountFactors()));
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE.currencyExposure(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    CurrencyAmount cashComputed = PRICER_TRADE.currentCash(BILL_TRADE_SETTLE_AFTER_VAL, VAL_DATE);
    assertEquals(cashComputed.getCurrency(), EUR);
    assertEquals(cashComputed.getAmount(), 0, TOLERANCE_PV);
  }
  
  public void test_currentcash_on_maturity() {
    CurrencyAmount cashComputed = PRICER_TRADE.currentCash(BILL_TRADE_SETTLE_AFTER_VAL, MATURITY_DATE);
    assertEquals(cashComputed.getCurrency(), EUR);
    assertEquals(cashComputed.getAmount(), NOTIONAL_AMOUNT * QUANTITY, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueZSpread_settle_before_val() {
    CurrencyAmount pvComputed = PRICER_TRADE
        .presentValueWithZSpread(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    CurrencyAmount pvExpected = PRICER_PRODUCT
        .presentValueWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .multipliedBy(QUANTITY);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE
        .currencyExposureWithZSpread(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
  }
  
  public void test_presentValueZSpread_settle_on_val() {
    CurrencyAmount pvComputed = PRICER_TRADE
        .presentValueWithZSpread(BILL_TRADE_SETTLE_ON_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    CurrencyAmount pvExpected = PRICER_PRODUCT
        .presentValueWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .plus(-PRICE * NOTIONAL_AMOUNT)
        .multipliedBy(QUANTITY);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE
        .currencyExposureWithZSpread(BILL_TRADE_SETTLE_ON_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
  }
  
  public void test_presentValueZSpread_settle_after_val() {
    CurrencyAmount pvComputed = PRICER_TRADE
        .presentValueWithZSpread(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    CurrencyAmount pvExpected = PRICER_PRODUCT
        .presentValueWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .multipliedBy(QUANTITY)
        .plus(PRICER_PAYMENT.presentValue(BILL_TRADE_SETTLE_AFTER_VAL.getSettlement().get(), 
            PROVIDER.repoCurveDiscountFactors(BILL_PRODUCT.getSecurityId(), BILL_PRODUCT.getLegalEntityId(), BILL_PRODUCT.getCurrency())
            .getDiscountFactors()));
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
    MultiCurrencyAmount ceComputed = PRICER_TRADE
        .currencyExposureWithZSpread(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    assertEquals(ceComputed.getCurrencies().size(), 1);
    assertTrue(ceComputed.contains(EUR));
    assertEquals(ceComputed.getAmount(EUR).getAmount(), pvExpected.getAmount(), TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_pvsensi_settle_before_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE.presentValueSensitivity(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT.presentValueSensitivity(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .multipliedBy(QUANTITY);
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER, p -> PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_BEFORE_VAL, p));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }
  
  public void test_pvsensi_settle_on_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE.presentValueSensitivity(BILL_TRADE_SETTLE_ON_VAL, PROVIDER);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT.presentValueSensitivity(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .multipliedBy(QUANTITY)
        .combinedWith(RepoCurveZeroRateSensitivity.of(
            (ZeroRateSensitivity) PRICER_PAYMENT.presentValueSensitivity(
                BILL_TRADE_SETTLE_ON_VAL.getSettlement().get(),
                PROVIDER.repoCurveDiscountFactors(
                    BILL_PRODUCT.getSecurityId(),
                    BILL_PRODUCT.getLegalEntityId(),
                    BILL_PRODUCT.getCurrency())
                    .getDiscountFactors()),
            GROUP_REPO).build());
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER, p -> PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_ON_VAL, p));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }
  
  public void test_pvsensi_settle_after_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE.presentValueSensitivity(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT.presentValueSensitivity(BILL_PRODUCT.resolve(REF_DATA), PROVIDER)
        .multipliedBy(QUANTITY)
        .combinedWith(RepoCurveZeroRateSensitivity.of(
            (ZeroRateSensitivity) PRICER_PAYMENT.presentValueSensitivity(
                BILL_TRADE_SETTLE_AFTER_VAL.getSettlement().get(),
                PROVIDER.repoCurveDiscountFactors(
                    BILL_PRODUCT.getSecurityId(),
                    BILL_PRODUCT.getLegalEntityId(),
                    BILL_PRODUCT.getCurrency())
                    .getDiscountFactors()),
            GROUP_REPO).build());
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER, p -> PRICER_TRADE.presentValue(BILL_TRADE_SETTLE_AFTER_VAL, p));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }

  //-------------------------------------------------------------------------
  public void test_pvsensiZSpread_settle_before_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE
        .presentValueSensitivityWithZSpread(BILL_TRADE_SETTLE_BEFORE_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT
        .presentValueSensitivityWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .multipliedBy(QUANTITY);
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER,
        p -> PRICER_TRADE.presentValueWithZSpread(BILL_TRADE_SETTLE_BEFORE_VAL, p, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }
  
  public void test_pvsensiZSpread_settle_on_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE
        .presentValueSensitivityWithZSpread(BILL_TRADE_SETTLE_ON_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT
        .presentValueSensitivityWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .multipliedBy(QUANTITY)
        .combinedWith(RepoCurveZeroRateSensitivity.of(
            (ZeroRateSensitivity) PRICER_PAYMENT.presentValueSensitivity(
                BILL_TRADE_SETTLE_ON_VAL.getSettlement().get(),
                PROVIDER.repoCurveDiscountFactors(
                    BILL_PRODUCT.getSecurityId(),
                    BILL_PRODUCT.getLegalEntityId(),
                    BILL_PRODUCT.getCurrency())
                    .getDiscountFactors()),
            GROUP_REPO).build());
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER,
        p -> PRICER_TRADE.presentValueWithZSpread(BILL_TRADE_SETTLE_ON_VAL, p, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }
  
  public void test_pvsensiZSpread_settle_after_val() {
    PointSensitivities pvsensiComputed = PRICER_TRADE
        .presentValueSensitivityWithZSpread(BILL_TRADE_SETTLE_AFTER_VAL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    PointSensitivities pvsensiExpected = PRICER_PRODUCT
        .presentValueSensitivityWithZSpread(BILL_PRODUCT.resolve(REF_DATA), PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0)
        .multipliedBy(QUANTITY)
        .combinedWith(RepoCurveZeroRateSensitivity.of(
            (ZeroRateSensitivity) PRICER_PAYMENT.presentValueSensitivity(
                BILL_TRADE_SETTLE_AFTER_VAL.getSettlement().get(),
                PROVIDER.repoCurveDiscountFactors(
                    BILL_PRODUCT.getSecurityId(),
                    BILL_PRODUCT.getLegalEntityId(),
                    BILL_PRODUCT.getCurrency())
                    .getDiscountFactors()),
            GROUP_REPO).build());
    assertTrue(pvsensiComputed.equalWithTolerance(pvsensiExpected, TOLERANCE_PVSENSI));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(pvsensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER,
        p -> PRICER_TRADE.presentValueWithZSpread(BILL_TRADE_SETTLE_AFTER_VAL, p, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT * QUANTITY));
  }
  
}

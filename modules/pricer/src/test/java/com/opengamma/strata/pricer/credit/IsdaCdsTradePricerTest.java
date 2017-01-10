/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Test {@link IsdaCdsTradePricer}.
 */
@Test
public class IsdaCdsTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 3);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.USNY;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");

  private static final DoubleArray TIME_YC = DoubleArray.ofUnsafe(new double[] {0.09041095890410959, 0.16712328767123288,
      0.2547945205479452, 0.5041095890410959, 0.7534246575342466, 1.0054794520547945, 2.0054794520547947, 3.008219178082192,
      4.013698630136987, 5.010958904109589, 6.008219178082192, 7.010958904109589, 8.01095890410959, 9.01095890410959,
      10.016438356164384, 12.013698630136986, 15.021917808219179, 20.01917808219178, 30.024657534246575});
  private static final DoubleArray RATE_YC = DoubleArray.ofUnsafe(new double[] {-0.002078655697855299, -0.001686438401304855,
      -0.0013445486228483379, -4.237819925898475E-4, 2.5142499469348057E-5, 5.935063895780138E-4, -3.247081037469503E-4,
      6.147182786549223E-4, 0.0019060597240545122, 0.0033125742254568815, 0.0047766352312329455, 0.0062374324537341225,
      0.007639664176639106, 0.008971003650150983, 0.010167545380711455, 0.012196853322376243, 0.01441082634734099,
      0.016236611610989507, 0.01652439910865982});
  private static final DefaultCurveMetadata METADATA_YC = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("yield")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve NODAL_YC = InterpolatedNodalCurve.of(METADATA_YC, TIME_YC, RATE_YC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final IsdaCreditDiscountFactors YIELD_CRVE =
      IsdaCreditDiscountFactors.of(USD, VALUATION_DATE, NODAL_YC);

  private static final DoubleArray TIME_CC = DoubleArray.ofUnsafe(new double[] {1.2054794520547945, 1.7095890410958905,
      2.712328767123288, 3.712328767123288, 4.712328767123288, 5.712328767123288, 7.715068493150685, 10.717808219178082});
  private static final DoubleArray RATE_CC = DoubleArray.ofUnsafe(new double[] {0.009950492020354761, 0.01203385973637765,
      0.01418821591480718, 0.01684815168721049, 0.01974873350586718, 0.023084203422383043, 0.02696911931489543,
      0.029605642651816415});
  private static final DefaultCurveMetadata METADATA_CC = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("credit")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve NODAL_CC = InterpolatedNodalCurve.of(METADATA_CC, TIME_CC, RATE_CC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final CreditDiscountFactors CREDIT_CRVE =
      IsdaCreditDiscountFactors.of(USD, VALUATION_DATE, NODAL_CC);
  private static final ConstantRecoveryRates RECOVERY_RATES =
      ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, 0.25);
  private static final CreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, CREDIT_CRVE)))
      .discountCurves(ImmutableMap.of(USD, YIELD_CRVE))
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
      .build();

  private static final double NOTIONAL = 1.0e7;
  private static final ResolvedCds PRODUCT = Cds.of(
      BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2013, 12, 20), LocalDate.of(2020, 10, 20), Frequency.P3M, CALENDAR, 0.015)
      .resolve(REF_DATA);
  private static final LocalDate SETTLEMENT_DATE = PRODUCT.getSettlementDateOffset().adjust(VALUATION_DATE, REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(VALUATION_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final Payment UPFRONT = Payment.of(USD, -NOTIONAL * 0.2, SETTLEMENT_DATE);
  private static final ResolvedCdsTrade TRADE = ResolvedCdsTrade.builder()
      .product(PRODUCT)
      .info(TRADE_INFO)
      .upfrontFee(UPFRONT)
      .build();
  private static final ResolvedCdsTrade TRADE_NO_SETTLE_DATE = ResolvedCdsTrade.builder()
      .product(PRODUCT)
      .info(TradeInfo.of(VALUATION_DATE))
      .build();

  private static final IsdaCdsTradePricer PRICER = IsdaCdsTradePricer.DEFAULT;
  private static final IsdaCdsTradePricer PRICER_MF = new IsdaCdsTradePricer(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final IsdaCdsProductPricer PRICER_PRODUCT = IsdaCdsProductPricer.DEFAULT;
  private static final IsdaCdsProductPricer PRICER_PRODUCT_MF = new IsdaCdsProductPricer(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final DiscountingPaymentPricer PRICER_PAYMENT = DiscountingPaymentPricer.DEFAULT;

  private static final double TOL = 1.0e-15;

  public void accFormulaTest() {
    assertEquals(PRICER.getAccrualOnDefaultFormula(), AccrualOnDefaultFormula.ORIGINAL_ISDA);
    assertEquals(PRICER_MF.getAccrualOnDefaultFormula(), AccrualOnDefaultFormula.MARKIT_FIX);
  }

  public void test_price() {
    double computed = PRICER.price(TRADE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    double expected = PRICER_PRODUCT.price(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    double computedMf = PRICER_MF.price(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    double expectedMf = PRICER_PRODUCT_MF.price(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    assertEquals(computed, expected, TOL);
    assertEquals(computedMf, expectedMf, TOL);
  }

  public void test_priceWithCoupon() {
    double coupon = 0.015;
    double computed = PRICER.price(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, coupon, PriceType.CLEAN, REF_DATA);
    double expected = PRICER_PRODUCT.price(PRODUCT, RATES_PROVIDER, coupon, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    double computedMf = PRICER_MF.price(TRADE, RATES_PROVIDER, coupon, PriceType.CLEAN, REF_DATA);
    double expectedMf = PRICER_PRODUCT_MF.price(PRODUCT, RATES_PROVIDER, coupon, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    assertEquals(computed, expected, TOL);
    assertEquals(computedMf, expectedMf, TOL);
  }

  public void test_parSpread() {
    double computed = PRICER.parSpread(TRADE, RATES_PROVIDER, REF_DATA);
    double expected = PRICER_PRODUCT.parSpread(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA);
    double computedMf = PRICER_MF.parSpread(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    double expectedMf = PRICER_PRODUCT_MF.parSpread(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA);
    assertEquals(computed, expected, TOL);
    assertEquals(computedMf, expectedMf, TOL);
  }

  public void test_priceSensitivity() {
    PointSensitivities computed = PRICER.priceSensitivity(TRADE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expected =
        PRICER_PRODUCT.priceSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    PointSensitivities computedMf = PRICER_MF.priceSensitivity(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expectedMf =
        PRICER_PRODUCT_MF.priceSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    assertTrue(computed.equalWithTolerance(expected, TOL));
    assertTrue(computedMf.equalWithTolerance(expectedMf, TOL));
  }

  public void test_parSpreadSensitivity() {
    PointSensitivities computed = PRICER.parSpreadSensitivity(TRADE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expected =
        PRICER_PRODUCT.parSpreadSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    PointSensitivities computedMf = PRICER_MF.parSpreadSensitivity(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expectedMf =
        PRICER_PRODUCT_MF.parSpreadSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    assertTrue(computed.equalWithTolerance(expected, TOL));
    assertTrue(computedMf.equalWithTolerance(expectedMf, TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computed = PRICER.presentValue(TRADE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expected = PRICER_PRODUCT.presentValue(PRODUCT, RATES_PROVIDER, VALUATION_DATE, PriceType.CLEAN, REF_DATA)
        .plus(PRICER_PAYMENT.presentValue(UPFRONT, YIELD_CRVE.toDiscountFactors()));
    CurrencyAmount computedMf = PRICER_MF.presentValue(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expectedMf =
        PRICER_PRODUCT_MF.presentValue(PRODUCT, RATES_PROVIDER, VALUATION_DATE, PriceType.CLEAN, REF_DATA);
    assertEquals(computed.getAmount(), expected.getAmount(), TOL);
    assertEquals(computedMf.getAmount(), expectedMf.getAmount(), TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities computed = PRICER.presentValueSensitivity(TRADE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expected = PRICER_PRODUCT.presentValueSensitivity(PRODUCT, RATES_PROVIDER, VALUATION_DATE, REF_DATA)
        .combinedWith(PRICER_PAYMENT.presentValueSensitivity(UPFRONT, YIELD_CRVE.toDiscountFactors())).build();
    PointSensitivities computedMf = PRICER_MF.presentValueSensitivity(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expectedMf =
        PRICER_PRODUCT_MF.presentValueSensitivity(PRODUCT, RATES_PROVIDER, VALUATION_DATE, REF_DATA).build();
    assertTrue(computed.equalWithTolerance(expected, TOL));
    assertTrue(computedMf.equalWithTolerance(expectedMf, TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueOnSettle() {
    CurrencyAmount computed = PRICER.presentValueOnSettle(TRADE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expected = PRICER_PRODUCT.presentValue(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    CurrencyAmount computedMf = PRICER_MF.presentValueOnSettle(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expectedMf =
        PRICER_PRODUCT_MF.presentValue(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    assertEquals(computed.getAmount(), expected.getAmount(), TOL);
    assertEquals(computedMf.getAmount(), expectedMf.getAmount(), TOL);
  }

  public void test_rpv01OnSettle() {
    CurrencyAmount computed = PRICER.rpv01OnSettle(TRADE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expected = PRICER_PRODUCT.rpv01(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    CurrencyAmount computedMf = PRICER_MF.rpv01OnSettle(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, PriceType.CLEAN, REF_DATA);
    CurrencyAmount expectedMf = PRICER_PRODUCT_MF.rpv01(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, PriceType.CLEAN, REF_DATA);
    assertEquals(computed.getAmount(), expected.getAmount(), TOL);
    assertEquals(computedMf.getAmount(), expectedMf.getAmount(), TOL);
  }

  public void test_recovery01OnSettle() {
    CurrencyAmount computed = PRICER.recovery01OnSettle(TRADE, RATES_PROVIDER, REF_DATA);
    CurrencyAmount expected = PRICER_PRODUCT.recovery01(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA);
    CurrencyAmount computedMf = PRICER_MF.recovery01OnSettle(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    CurrencyAmount expectedMf = PRICER_PRODUCT_MF.recovery01(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA);
    assertEquals(computed.getAmount(), expected.getAmount(), TOL);
    assertEquals(computedMf.getAmount(), expectedMf.getAmount(), TOL);
  }

  public void test_presentValueOnSettleSensitivity() {
    PointSensitivities computed = PRICER.presentValueOnSettleSensitivity(TRADE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expected =
        PRICER_PRODUCT.presentValueSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    PointSensitivities computedMf =
        PRICER_MF.presentValueOnSettleSensitivity(TRADE_NO_SETTLE_DATE, RATES_PROVIDER, REF_DATA);
    PointSensitivities expectedMf =
        PRICER_PRODUCT_MF.presentValueSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA).build();
    assertTrue(computed.equalWithTolerance(expected, TOL));
    assertTrue(computedMf.equalWithTolerance(expectedMf, TOL));
  }

  //-------------------------------------------------------------------------
  public void test_jumpToDefault() {
    JumpToDefault computed = PRICER.jumpToDefault(TRADE, RATES_PROVIDER, REF_DATA);
    JumpToDefault expected = PRICER_PRODUCT.jumpToDefault(PRODUCT, RATES_PROVIDER, SETTLEMENT_DATE, REF_DATA);
    assertEquals(computed, expected);
  }

  public void test_expectedLoss() {
    CurrencyAmount computed = PRICER.expectedLoss(TRADE, RATES_PROVIDER);
    CurrencyAmount expected = PRICER_PRODUCT.expectedLoss(PRODUCT, RATES_PROVIDER);
    assertEquals(computed.getAmount(), expected.getAmount(), TOL);
  }

}

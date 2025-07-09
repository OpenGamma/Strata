/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.pricer.common.PriceType.CLEAN;
import static com.opengamma.strata.pricer.common.PriceType.DIRTY;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.ResolvedCds;

/**
 * Test {@link IsdaCdsProductPricer}.
 * <p>
 * The numbers in the regression tests are from 2.x.
 */
public class IsdaCdsProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 3);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
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
  private static final ImmutableCreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, CREDIT_CRVE)))
      .discountCurves(ImmutableMap.of(USD, YIELD_CRVE))
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
      .build();

  private static final double NOTIONAL = 1.0e7;
  private static final ResolvedCds PRODUCT_NEXTDAY = Cds.of(
      BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2014, 1, 4), LocalDate.of(2020, 10, 20), Frequency.P3M, CALENDAR, 0.05)
      .resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_BEFORE = Cds.of(
      SELL, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2013, 12, 20), LocalDate.of(2024, 9, 20), Frequency.P3M, CALENDAR, 0.05)
      .resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_AFTER = Cds.of(
      BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2014, 3, 20), LocalDate.of(2029, 12, 20), Frequency.P3M, CALENDAR, 0.05)
      .resolve(REF_DATA);

  private static final DaysAdjustment SETTLE_DAY_ADJ_NS = DaysAdjustment.ofBusinessDays(5, CALENDAR);
  private static final DaysAdjustment STEPIN_DAY_ADJ_NS = DaysAdjustment.ofCalendarDays(7);
  private static final ResolvedCds PRODUCT_NS_TODAY = Cds.builder()
      .buySell(BUY)
      .legalEntityId(LEGAL_ENTITY)
      .currency(USD)
      .notional(NOTIONAL)
      .paymentSchedule(
          PeriodicSchedule.builder()
              .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR))
              .startDate(VALUATION_DATE)
              .endDate(LocalDate.of(2021, 4, 25))
              .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
              .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
              .frequency(Frequency.P4M)
              .rollConvention(RollConventions.NONE)
              .stubConvention(StubConvention.SHORT_FINAL)
              .build())
      .dayCount(ACT_360)
      .fixedRate(0.05)
      .paymentOnDefault(PaymentOnDefault.ACCRUED_PREMIUM)
      .protectionStart(ProtectionStartOfDay.NONE)
      .stepinDateOffset(STEPIN_DAY_ADJ_NS)
      .settlementDateOffset(SETTLE_DAY_ADJ_NS)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_NS_STEPIN =
      Cds.builder()
          .buySell(SELL)
          .legalEntityId(LEGAL_ENTITY)
          .currency(USD)
          .notional(NOTIONAL)
          .paymentSchedule(
              PeriodicSchedule.builder()
                  .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR))
                  .startDate(STEPIN_DAY_ADJ_NS.adjust(VALUATION_DATE, REF_DATA))
                  .endDate(LocalDate.of(2019, 1, 26))
                  .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .frequency(Frequency.P6M)
                  .rollConvention(RollConventions.NONE)
                  .stubConvention(StubConvention.LONG_INITIAL)
                  .build())
          .dayCount(ACT_360)
          .fixedRate(0.05)
          .paymentOnDefault(PaymentOnDefault.ACCRUED_PREMIUM)
          .protectionStart(ProtectionStartOfDay.NONE)
          .stepinDateOffset(STEPIN_DAY_ADJ_NS)
          .settlementDateOffset(SETTLE_DAY_ADJ_NS)
          .build()
          .resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_NS_BTW =
      Cds.builder()
          .buySell(BUY)
          .legalEntityId(LEGAL_ENTITY)
          .currency(USD)
          .notional(NOTIONAL)
          .paymentSchedule(
              PeriodicSchedule.builder()
                  .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR))
                  .startDate(VALUATION_DATE.plusDays(4))
                  .endDate(LocalDate.of(2026, 8, 2))
                  .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .frequency(Frequency.P12M)
                  .rollConvention(RollConventions.NONE)
                  .stubConvention(StubConvention.LONG_FINAL)
                  .build())
          .dayCount(ACT_360)
          .fixedRate(0.05)
          .paymentOnDefault(PaymentOnDefault.ACCRUED_PREMIUM)
          .protectionStart(ProtectionStartOfDay.NONE)
          .stepinDateOffset(STEPIN_DAY_ADJ_NS)
          .settlementDateOffset(SETTLE_DAY_ADJ_NS)
          .build()
          .resolve(REF_DATA);

  private static final double TOL = 1.0e-14;
  private static final double EPS = 1.0e-6;
  private static final IsdaCdsProductPricer PRICER = IsdaCdsProductPricer.DEFAULT;
  private static final IsdaCdsProductPricer PRICER_FIX = new IsdaCdsProductPricer(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final IsdaCdsProductPricer PRICER_CORRECT = new IsdaCdsProductPricer(AccrualOnDefaultFormula.CORRECT);
  private static final RatesFiniteDifferenceSensitivityCalculator CALC_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  @Test
  public void accFormulaTest() {
    assertThat(PRICER.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.ORIGINAL_ISDA);
    assertThat(PRICER_FIX.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.MARKIT_FIX);
    assertThat(PRICER_CORRECT.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.CORRECT);
  }

  @Test
  public void endedTest() {
    LocalDate valuationDate = PRODUCT_NEXTDAY.getProtectionEndDate().plusDays(1);
    CreditRatesProvider provider = createCreditRatesProvider(valuationDate);
    double price = PRICER.price(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(price).isEqualTo(0d);
    CurrencyAmount pv = PRICER.presentValue(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(pv).isEqualTo(CurrencyAmount.zero(USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.parSpread(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA));
    double protectionLeg = PRICER.protectionLeg(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(protectionLeg).isEqualTo(0d);
    double riskyAnnuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(riskyAnnuity).isEqualTo(0d);
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(rpv01).isEqualTo(CurrencyAmount.zero(USD));
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(recovery01).isEqualTo(CurrencyAmount.zero(USD));
    PointSensitivities sensi = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA).build();
    assertThat(sensi).isEqualTo(PointSensitivities.empty());
    PointSensitivities sensiPrice = PRICER.priceSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA).build();
    assertThat(sensiPrice).isEqualTo(PointSensitivities.empty());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.parSpreadSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA));
    JumpToDefault jtd = PRICER.jumpToDefault(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(jtd).isEqualTo(JumpToDefault.of(USD, ImmutableMap.of(LEGAL_ENTITY, 0d)));
    CurrencyAmount expectedLoss = PRICER.expectedLoss(PRODUCT_NEXTDAY, provider);
    assertThat(expectedLoss).isEqualTo(CurrencyAmount.zero(USD));
  }

  @Test
  public void consistencyTest() {
    double price = PRICER.price(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double spread = PRICER.parSpread(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    // the following methods are tested by regression tests
    double protPv = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double annuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(price).isCloseTo(protPv - PRODUCT_NEXTDAY.getFixedRate() * annuity, offset(TOL));
    assertThat(rpv01.getCurrency()).isEqualTo(USD);
    assertThat(rpv01.getAmount()).isCloseTo(annuity * NOTIONAL, offset(NOTIONAL * TOL));
    assertThat(recovery01.getCurrency()).isEqualTo(USD);
    assertThat(recovery01.getAmount()).isCloseTo(-protPv / (1d - RECOVERY_RATES.getRecoveryRate()) * NOTIONAL, offset(NOTIONAL * TOL));
    assertThat(spread).isCloseTo(protPv / annuity, offset(TOL));
  }

  @Test
  public void withCouponTest() {
    double coupon = 0.15;
    double price = PRICER.price(PRODUCT_NEXTDAY, RATES_PROVIDER, coupon,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    double protPv = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double annuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(price).isCloseTo(protPv - coupon * annuity, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void pvSensitivityTest() {
    PointSensitivities pointNext = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void pvSensitivityFixTest() {
    PointSensitivities pointNext = PRICER_FIX.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_FIX.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_FIX.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void pvSensitivityCorrectTest() {
    PointSensitivities pointNext = PRICER_CORRECT.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_CORRECT.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_CORRECT.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void priceSensitivityTest() {
    PointSensitivities pointNext = PRICER.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void priceSensitivityFixTest() {
    PointSensitivities pointNext = PRICER_FIX.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_FIX.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_FIX.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void priceSensitivityCorrectTest() {
    PointSensitivities pointNext = PRICER_CORRECT.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_CORRECT.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_CORRECT.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void parSpreadSensitivityTest() {
    PointSensitivities pointNext = PRICER.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void parSpreadSensitivityFixTest() {
    PointSensitivities pointNext = PRICER_FIX.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_FIX.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_FIX.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void parSpreadSensitivityCorrectTest() {
    PointSensitivities pointNext = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void protectionLegSensitivityTest() {
    PointSensitivities pointNext = PRICER.protectionLegSensitivity(
        PRODUCT_NEXTDAY,
        RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER.protectionLeg(
                PRODUCT_NEXTDAY,
                p,
                PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, 10d * EPS);

    PointSensitivities pointBefore = PRICER.protectionLegSensitivity(
        PRODUCT_BEFORE,
        RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER.protectionLeg(
                PRODUCT_BEFORE,
                p,
                PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, 10d * EPS);

    PointSensitivities pointAfter = PRICER.protectionLegSensitivity(
        PRODUCT_AFTER,
        RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER.protectionLeg(
                PRODUCT_AFTER,
                p,
                PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, 20d * EPS);
  }

  @Test
  public void protectionLegSensitivityFixTest() {
    PointSensitivities pointNext = PRICER_FIX.protectionLegSensitivity(
        PRODUCT_NEXTDAY,
        RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_FIX.protectionLeg(
                PRODUCT_NEXTDAY,
                p,
                PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, 10d * EPS);

    PointSensitivities pointBefore = PRICER_FIX.protectionLegSensitivity(
        PRODUCT_BEFORE,
        RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_FIX.protectionLeg(
                PRODUCT_BEFORE,
                p,
                PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, 10d * EPS);

    PointSensitivities pointAfter = PRICER_FIX.protectionLegSensitivity(
        PRODUCT_AFTER,
        RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_FIX.protectionLeg(
                PRODUCT_AFTER,
                p,
                PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, 20d * EPS);
  }

  @Test
  public void protectionLegSensitivityCorrectTest() {
    PointSensitivities pointNext = PRICER_CORRECT.protectionLegSensitivity(
        PRODUCT_NEXTDAY,
        RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_CORRECT.protectionLeg(
                PRODUCT_NEXTDAY,
                p,
                PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, 10d * EPS);

    PointSensitivities pointBefore = PRICER_CORRECT.protectionLegSensitivity(
        PRODUCT_BEFORE,
        RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_CORRECT.protectionLeg(
                PRODUCT_BEFORE,
                p,
                PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, 10d * EPS);

    PointSensitivities pointAfter = PRICER_CORRECT.protectionLegSensitivity(
        PRODUCT_AFTER,
        RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA),
        REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter = CALC_FD.sensitivity(
        RATES_PROVIDER,
        p -> CurrencyAmount.of(
            USD,
            PRICER_CORRECT.protectionLeg(
                PRODUCT_AFTER,
                p,
                PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA),
                REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, 20d * EPS);
  }

  @Test
  public void riskyAnnuitySensitivityTest() {
    PointSensitivities pointNext = PRICER.riskyAnnuitySensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.riskyAnnuity(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER.riskyAnnuitySensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.riskyAnnuity(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER.riskyAnnuitySensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.riskyAnnuity(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void riskyAnnuitySensitivityFixTest() {
    PointSensitivities pointNext = PRICER_FIX.riskyAnnuitySensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.riskyAnnuity(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_FIX.riskyAnnuitySensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.riskyAnnuity(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_FIX.riskyAnnuitySensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.riskyAnnuity(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  @Test
  public void riskyAnnuitySensitivityCorrectTest() {
    PointSensitivities pointNext = PRICER_CORRECT.riskyAnnuitySensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext);
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.riskyAnnuity(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivities pointBefore = PRICER_CORRECT.riskyAnnuitySensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore);
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.riskyAnnuity(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivities pointAfter = PRICER_CORRECT.riskyAnnuitySensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter);
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.riskyAnnuity(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  private void equalWithRelativeTolerance(
      CurrencyParameterSensitivities computed,
      CurrencyParameterSensitivities expected,
      double tolerance) {

    List<CurrencyParameterSensitivity> mutable = new ArrayList<>(expected.getSensitivities());
    // for each sensitivity in this instance, find matching in other instance
    for (CurrencyParameterSensitivity sens1 : computed.getSensitivities()) {
      // list is already sorted so binary search is safe
      int index = Collections.binarySearch(mutable, sens1, CurrencyParameterSensitivity::compareKey);
      if (index >= 0) {
        // matched, so must be equal
        CurrencyParameterSensitivity sens2 = mutable.get(index);
        equalZeroWithRelativeTolerance(sens1.getSensitivity(), sens2.getSensitivity(), tolerance);
        mutable.remove(index);
      } else {
        // did not match, so must be zero
        assertThat(sens1.getSensitivity().equalZeroWithTolerance(tolerance)).isTrue();
      }
    }
    // all that remain from other instance must be zero
    for (CurrencyParameterSensitivity sens2 : mutable) {
      assertThat(sens2.getSensitivity().equalZeroWithTolerance(tolerance)).isTrue();
    }
  }

  private void equalZeroWithRelativeTolerance(DoubleArray computed, DoubleArray expected, double tolerance) {
    int size = expected.size();
    assertThat(size).isEqualTo(computed.size());
    for (int i = 0; i < size; i++) {
      double ref = Math.max(1d, Math.abs(expected.get(i)));
      assertThat(computed.get(i)).isCloseTo(expected.get(i), offset(tolerance * ref));
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void cleanPvTest() {
    double resNext = PRICER.presentValue(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resNext).isCloseTo(-0.20208402732565636 * NOTIONAL, offset(TOL * NOTIONAL));
    double resBefore = PRICER.presentValue(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resBefore).isCloseTo(-0.26741962741508013 * (-NOTIONAL), offset(TOL * NOTIONAL));
    double resAfter = PRICER.presentValue(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resAfter).isCloseTo(-0.32651549808776237 * NOTIONAL, offset(TOL * NOTIONAL));
    double resNsToday = PRICER.presentValue(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resNsToday).isCloseTo(-0.2101704800313836 * NOTIONAL, offset(TOL * NOTIONAL));
    double resNsStepin = PRICER.presentValue(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN,
        REF_DATA).getAmount();
    assertThat(resNsStepin).isCloseTo(-0.1691072048424866 * (-NOTIONAL), offset(TOL * NOTIONAL));
    double resNsBtw = PRICER.presentValue(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resNsBtw).isCloseTo(-0.29068253160089597 * NOTIONAL, offset(TOL * NOTIONAL));
  }

  @Test
  public void cleanPvTruncationTest() {
    CreditRatesProvider ratesAccEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 22));
    double resAccEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesAccEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesAccEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resAccEndDate).isCloseTo(-0.26418577838510354 * (-NOTIONAL), offset(TOL * NOTIONAL));
    CreditRatesProvider ratesEffectiveEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 21));
    double resEffectiveEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesEffectiveEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesEffectiveEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resEffectiveEndDate).isCloseTo(-0.26422628099362094 * (-NOTIONAL), offset(TOL * NOTIONAL));
    CreditRatesProvider ratesProtectionEndDateOne = createCreditRatesProvider(LocalDate.of(2024, 9, 19));
    double resProtectionEndDateOne = PRICER.presentValue(PRODUCT_BEFORE, ratesProtectionEndDateOne,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDateOne.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resProtectionEndDateOne).isCloseTo(-1.1814923847919301E-4 * (-NOTIONAL), offset(TOL * NOTIONAL));
    CreditRatesProvider ratesProtectionEndDate = createCreditRatesProvider(LocalDate.of(2024, 9, 20));
    double resProtectionEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesProtectionEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertThat(resProtectionEndDate).isCloseTo(0d, offset(TOL * NOTIONAL));
  }

  @Test
  public void protectionLegRegressionTest() {
    double resNext = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resNext).isCloseTo(0.11770082424693698, offset(TOL));
    double resBefore = PRICER.protectionLeg(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resBefore).isCloseTo(0.19621836970171463, offset(TOL));
    double resAfter = PRICER.protectionLeg(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resAfter).isCloseTo(0.2744043768251808, offset(TOL));
    double resNsToday = PRICER.protectionLeg(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resNsToday).isCloseTo(0.12920042414763938, offset(TOL));
    double resNsStepin = PRICER.protectionLeg(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resNsStepin).isCloseTo(0.07540932150559641, offset(TOL));
    double resNsBtw = PRICER.protectionLeg(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertThat(resNsBtw).isCloseTo(0.22727774070157128, offset(TOL));
  }

  @Test
  public void premiumLegRegressionTest() {
    double resNext = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resNext).isCloseTo(6.395697031451866, offset(TOL));
    double resBefore = PRICER.riskyAnnuity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resBefore).isCloseTo(9.314426609002561, offset(TOL));
    double resAfter = PRICER.riskyAnnuity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resAfter).isCloseTo(12.018397498258862, offset(TOL));
    double resNsToday = PRICER.riskyAnnuity(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resNsToday).isCloseTo(6.806862528024904, offset(TOL));
    double resNsStepin = PRICER.riskyAnnuity(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resNsStepin).isCloseTo(4.89033052696166, offset(TOL));
    double resNsBtw = PRICER.riskyAnnuity(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resNsBtw).isCloseTo(10.367538779382677, offset(TOL));
  }

  @Test
  public void truncationRegressionTest() {
    CreditRatesProvider ratesAccEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 22));
    double resAccEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesAccEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesAccEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resAccEndDate).isCloseTo(9.140484282937514, offset(TOL));
    CreditRatesProvider ratesEffectiveEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 21));
    double resEffectiveEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesEffectiveEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesEffectiveEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resEffectiveEndDate).isCloseTo(9.139474456128156, offset(TOL));
    CreditRatesProvider ratesProtectionEndDateOne = createCreditRatesProvider(LocalDate.of(2024, 9, 19));
    double resProtectionEndDateOne = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesProtectionEndDateOne,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDateOne.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resProtectionEndDateOne).isCloseTo(0.2583274486014851, offset(TOL));
    CreditRatesProvider ratesProtectionEndDate = createCreditRatesProvider(LocalDate.of(2024, 9, 20));
    double resProtectionEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesProtectionEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertThat(resProtectionEndDate).isCloseTo(0d, offset(TOL));
  }

  @Test
  public void epsilonTest() {
    // Math.abs(dhrt) < 1e-5 is true
    DoubleArray timeDsc = DoubleArray.of(0.5d, 1d, 3d, 5d, 10d, 20d);
    DoubleArray rateDsc = DoubleArray.of(0.01, 0.03, 0.02, 0.01, 0.005, 0.005);
    DoubleArray timeCrd = DoubleArray.of(0.5866887582723792, 1.352879628192491, 2.3701168800050576, 3.10563128282816,
        11.326702860486112, 27.026366789997947);
    DoubleArray rateCrd = DoubleArray.of(-0.010004517471216213, -0.030006261363849242, -0.020009969610648954,
        -0.010001132580906982, -0.0050024950914495684, -0.0050025781457872075);
    InterpolatedNodalCurve nodalYc = InterpolatedNodalCurve.of(METADATA_YC, timeDsc, rateDsc,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    IsdaCreditDiscountFactors yc = IsdaCreditDiscountFactors.of(USD, VALUATION_DATE, nodalYc);
    InterpolatedNodalCurve nodalCc = InterpolatedNodalCurve.of(METADATA_CC, timeCrd, rateCrd,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    CreditDiscountFactors cc = IsdaCreditDiscountFactors.of(USD, VALUATION_DATE, nodalCc);
    CreditRatesProvider ratesProvider = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, cc)))
        .discountCurves(ImmutableMap.of(USD, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
        .build();
    // pv
    CurrencyAmount pv1 = PRICER.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount pv2 = PRICER_FIX.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount pv3 = PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertThat(pv1.getAmount()).isCloseTo(-0.3728276314104907 * NOTIONAL, offset(NOTIONAL * TOL));
    assertThat(pv2.getAmount()).isCloseTo(-0.3728585818359114 * NOTIONAL, offset(NOTIONAL * TOL));
    assertThat(pv3.getAmount()).isCloseTo(-0.3728305887124643 * NOTIONAL, offset(NOTIONAL * TOL));
    // sensitivity
    PointSensitivities point1 = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities res1 = ratesProvider.parameterSensitivity(point1);
    CurrencyParameterSensitivities exp1 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res1, exp1, NOTIONAL * EPS);
    PointSensitivities point2 = PRICER_FIX.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities res2 = ratesProvider.parameterSensitivity(point2);
    CurrencyParameterSensitivities exp2 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER_FIX.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res2, exp2, NOTIONAL * EPS);
    PointSensitivities point3 = PRICER_CORRECT.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA).build();
    CurrencyParameterSensitivities res3 = ratesProvider.parameterSensitivity(point3);
    CurrencyParameterSensitivities exp3 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res3, exp3, NOTIONAL * EPS);
  }

  @Test
  public void accruedInterestTest() {
    double acc = PRODUCT_BEFORE.accruedYearFraction(VALUATION_DATE) * PRODUCT_BEFORE.getFixedRate();
    double accAccEndDate = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 22)) * PRODUCT_BEFORE.getFixedRate();
    double accEffectiveEndDateOne = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 20)) * PRODUCT_BEFORE.getFixedRate();
    double accEffectiveEndDate = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 21)) * PRODUCT_BEFORE.getFixedRate();
    assertThat(acc).isCloseTo(0.0019444444444444446, offset(TOL));
    assertThat(accAccEndDate).isCloseTo(2.777777777777778E-4, offset(TOL));
    assertThat(accEffectiveEndDateOne).isCloseTo(0d, offset(TOL));
    assertThat(accEffectiveEndDate).isCloseTo(1.388888888888889E-4, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void jumpToDefaultTest() {
    JumpToDefault computed = PRICER.jumpToDefault(PRODUCT_BEFORE, RATES_PROVIDER, VALUATION_DATE, REF_DATA);
    LocalDate stepinDate = PRODUCT_BEFORE.getStepinDateOffset().adjust(VALUATION_DATE, REF_DATA);
    double dirtyPv = PRICER.presentValue(PRODUCT_BEFORE, RATES_PROVIDER, VALUATION_DATE, PriceType.DIRTY, REF_DATA).getAmount();
    double accrued = PRODUCT_BEFORE.accruedYearFraction(stepinDate) * PRODUCT_BEFORE.getFixedRate() *
        PRODUCT_BEFORE.getBuySell().normalize(NOTIONAL);
    double protection = PRODUCT_BEFORE.getBuySell().normalize(NOTIONAL) * (1d - RECOVERY_RATES.getRecoveryRate());
    double expected = protection - accrued - dirtyPv;
    assertThat(computed.getCurrency()).isEqualTo(USD);
    assertThat(computed.getAmounts().size() == 1).isTrue();
    assertThat(computed.getAmounts().get(LEGAL_ENTITY)).isCloseTo(expected, offset(NOTIONAL * TOL));
  }

  @Test
  public void expectedLossTest() {
    CurrencyAmount computed = PRICER.expectedLoss(PRODUCT_BEFORE, RATES_PROVIDER);
    double survivalProb = CREDIT_CRVE.discountFactor(PRODUCT_BEFORE.getProtectionEndDate());
    double expected = NOTIONAL * (1d - RECOVERY_RATES.getRecoveryRate()) * (1d - survivalProb);
    assertThat(computed.getCurrency()).isEqualTo(USD);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  private CreditRatesProvider createCreditRatesProvider(LocalDate valuationDate) {
    IsdaCreditDiscountFactors yc = IsdaCreditDiscountFactors.of(USD, valuationDate, NODAL_YC);
    CreditDiscountFactors cc = IsdaCreditDiscountFactors.of(USD, valuationDate, NODAL_CC);
    ConstantRecoveryRates rr = ConstantRecoveryRates.of(LEGAL_ENTITY, valuationDate, 0.25);
    return ImmutableCreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, cc)))
        .discountCurves(ImmutableMap.of(USD, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, rr))
        .build();
  }

}

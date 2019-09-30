/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.ResolvedCdsIndex;

/**
 * Test {@link IsdaHomogenousCdsIndexTradePricer}.
 */
public class IsdaHomogenousCdsIndexProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final StandardId INDEX_ID = StandardId.of("OG", "ABCXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES;
  static {
    Builder<StandardId> builder = ImmutableList.builder();
    for (int i = 0; i < 97; ++i) {
      builder.add(StandardId.of("OG", String.valueOf(i)));
    }
    LEGAL_ENTITIES = builder.build();
  }
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 2, 13);
  private static final DoubleArray TIME_YC = DoubleArray.ofUnsafe(new double[] {
      0.08767123287671233, 0.1726027397260274, 0.2602739726027397, 0.5095890410958904, 1.010958904109589, 2.010958904109589,
      3.0136986301369864, 4.0191780821917815, 5.016438356164384, 6.013698630136987, 7.016438356164384, 8.016438356164384,
      9.016438356164384, 10.021917808219179, 12.01917808219178, 15.027397260273974, 20.024657534246575, 25.027397260273972,
      30.030136986301372});
  private static final DoubleArray RATE_YC = DoubleArray.ofUnsafe(new double[] {
      0.0015967771993938666, 0.002000101499768777, 0.002363431670279865, 0.003338175293899776, 0.005634608399714134,
      0.00440326902435394, 0.007809961130263494, 0.011941089607974827, 0.015908558015433557, 0.019426790989545677,
      0.022365655212981644, 0.02480329609280203, 0.02681632723967965, 0.028566047406753222, 0.031343018999443514,
      0.03409375145707815, 0.036451406286344155, 0.0374228389649933, 0.037841116301420584});
  private static final DefaultCurveMetadata METADATA_YC = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("yield")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve NODAL_YC = InterpolatedNodalCurve.of(METADATA_YC, TIME_YC, RATE_YC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final double TIME_CC_SINGLE = 4.852054794520548;
  private static final double RATE_CC_SINGLE = 0.04666317621551129;
  private static final double INDEX_FACTOR = 93d / 97d;
  private static final DefaultCurveMetadata METADATA_CC_SINGLE = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("credit_single")
      .dayCount(ACT_365F)
      .addInfo(CurveInfoType.CDS_INDEX_FACTOR, INDEX_FACTOR)
      .build();
  private static final ConstantNodalCurve NODAL_CC_SINGLE =
      ConstantNodalCurve.of(METADATA_CC_SINGLE, TIME_CC_SINGLE, RATE_CC_SINGLE);
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
      .addInfo(CurveInfoType.CDS_INDEX_FACTOR, INDEX_FACTOR)
      .build();
  private static final InterpolatedNodalCurve NODAL_CC = InterpolatedNodalCurve.of(METADATA_CC, TIME_CC, RATE_CC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final double RECOVERY_RATE = 0.3;
  private static final CreditRatesProvider RATES_PROVIDER_SINGLE = createCreditRatesProviderSingle(VALUATION_DATE, true);
  private static final CreditRatesProvider RATES_PROVIDER = createCreditRatesProviderSingle(VALUATION_DATE, false);

  private static final double NOTIONAL = 1.0e8;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate MATURITY_DATE = LocalDate.of(2018, 12, 20);
  private static final double COUPON = 0.05;
  private static final ResolvedCdsIndex PRODUCT = CdsIndex.of(
      BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, MATURITY_DATE, P3M, SAT_SUN, COUPON).resolve(REF_DATA);
  private static final ResolvedCdsIndex PRODUCT_SELL = CdsIndex.of(
      SELL, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, MATURITY_DATE, P3M, SAT_SUN, COUPON).resolve(REF_DATA);
  private static final LocalDate SETTLEMENT_STD = PRODUCT.getSettlementDateOffset().adjust(VALUATION_DATE, REF_DATA);

  private static final double TOL = 1.0e-14;
  private static final double EPS = 1.0e-6;
  private static final IsdaHomogenousCdsIndexProductPricer PRICER = IsdaHomogenousCdsIndexProductPricer.DEFAULT;
  private static final IsdaHomogenousCdsIndexProductPricer PRICER_MARKIT =
      new IsdaHomogenousCdsIndexProductPricer(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final IsdaHomogenousCdsIndexProductPricer PRICER_OG =
      new IsdaHomogenousCdsIndexProductPricer(AccrualOnDefaultFormula.CORRECT);
  private static final RatesFiniteDifferenceSensitivityCalculator CALC_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  @Test
  public void accFormulaTest() {
    assertThat(PRICER.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.ORIGINAL_ISDA);
    assertThat(PRICER_MARKIT.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.MARKIT_FIX);
    assertThat(PRICER_OG.getAccrualOnDefaultFormula()).isEqualTo(AccrualOnDefaultFormula.CORRECT);
  }

  @Test
  public void test_regression() {
    CurrencyAmount cleanPvOg = PRICER_OG.presentValue(PRODUCT, RATES_PROVIDER_SINGLE, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(cleanPvOg.getAmount()).isCloseTo(-7305773.195876285, offset(NOTIONAL * TOL));
    assertThat(cleanPvOg.getCurrency()).isEqualTo(USD);
    CurrencyAmount dirtyPvOg = PRICER_OG.presentValue(PRODUCT, RATES_PROVIDER_SINGLE, SETTLEMENT_STD, DIRTY, REF_DATA);
    assertThat(dirtyPvOg.getAmount()).isCloseTo(-8051477.663230239, offset(NOTIONAL * TOL));
    assertThat(dirtyPvOg.getCurrency()).isEqualTo(USD);
    double cleanPriceOg = PRICER_OG.price(PRODUCT, RATES_PROVIDER_SINGLE, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(cleanPriceOg).isCloseTo(-0.07619999999999996, offset(TOL));
    double dirtyPriceOg = PRICER_OG.price(PRODUCT, RATES_PROVIDER_SINGLE, SETTLEMENT_STD, DIRTY, REF_DATA);
    assertThat(dirtyPriceOg).isCloseTo(-0.08397777777777776, offset(TOL));
  }

  @Test
  public void endedTest() {
    LocalDate valuationDate = PRODUCT.getProtectionEndDate().plusDays(1);
    CreditRatesProvider provider = createCreditRatesProviderSingle(valuationDate, false);
    double price = PRICER.price(PRODUCT, provider, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(price).isEqualTo(0d);
    CurrencyAmount pv = PRICER.presentValue(PRODUCT, provider, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(pv).isEqualTo(CurrencyAmount.zero(USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.parSpread(PRODUCT, provider, SETTLEMENT_STD, REF_DATA));
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT, provider, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(rpv01).isEqualTo(CurrencyAmount.zero(USD));
    double riskyAnnuity = PRICER.riskyAnnuity(PRODUCT, provider, SETTLEMENT_STD, CLEAN, REF_DATA);
    assertThat(riskyAnnuity).isEqualTo(0d);
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT, provider, SETTLEMENT_STD, REF_DATA);
    assertThat(recovery01).isEqualTo(CurrencyAmount.zero(USD));
    PointSensitivities sensi = PRICER.presentValueSensitivity(PRODUCT, provider, SETTLEMENT_STD, REF_DATA).build();
    assertThat(sensi).isEqualTo(PointSensitivities.empty());
    PointSensitivities sensiPrice = PRICER.priceSensitivity(PRODUCT, provider, SETTLEMENT_STD, REF_DATA).build();
    assertThat(sensiPrice).isEqualTo(PointSensitivities.empty());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.parSpreadSensitivity(PRODUCT, provider, SETTLEMENT_STD, REF_DATA));
    JumpToDefault jumpToDefault = PRICER.jumpToDefault(PRODUCT, provider, SETTLEMENT_STD, REF_DATA);
    assertThat(jumpToDefault).isEqualTo(JumpToDefault.of(USD, ImmutableMap.of(INDEX_ID, 0d)));
    CurrencyAmount expectedLoss = PRICER.expectedLoss(PRODUCT, provider);
    assertThat(expectedLoss).isEqualTo(CurrencyAmount.zero(USD));
  }

  @Test
  public void consistencyTest() {
    CurrencyAmount pv = PRICER.presentValue(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, CLEAN, REF_DATA);
    CurrencyAmount pvSell = PRICER.presentValue(PRODUCT_SELL, RATES_PROVIDER, SETTLEMENT_STD, CLEAN, REF_DATA);
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, CLEAN, REF_DATA);
    double riskyAnnuity = PRICER.riskyAnnuity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, CLEAN, REF_DATA);
    CurrencyAmount rpv01Sell = PRICER.rpv01(PRODUCT_SELL, RATES_PROVIDER, SETTLEMENT_STD, CLEAN, REF_DATA);
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA);
    CurrencyAmount recovery01Sell = PRICER.recovery01(PRODUCT_SELL, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA);
    double spread = PRICER.parSpread(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA);
    assertThat(pv.getCurrency()).isEqualTo(USD);
    assertThat(pvSell.getCurrency()).isEqualTo(USD);
    assertThat(rpv01.getCurrency()).isEqualTo(USD);
    assertThat(rpv01Sell.getCurrency()).isEqualTo(USD);
    assertThat(recovery01.getCurrency()).isEqualTo(USD);
    assertThat(recovery01Sell.getCurrency()).isEqualTo(USD);
    assertThat(pv.getAmount()).isCloseTo(-(1d - RECOVERY_RATE) * recovery01.getAmount() - PRODUCT.getFixedRate() * rpv01.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pv.getAmount()).isCloseTo(-pvSell.getAmount(), offset(NOTIONAL * TOL));
    assertThat(rpv01.getAmount()).isCloseTo(-rpv01Sell.getAmount(), offset(NOTIONAL * TOL));
    assertThat(riskyAnnuity).isCloseTo(rpv01.getAmount() / NOTIONAL / INDEX_FACTOR, offset(NOTIONAL * TOL));
    assertThat(recovery01.getAmount()).isCloseTo(-recovery01Sell.getAmount(), offset(NOTIONAL * TOL));
    assertThat(spread).isCloseTo(-(1d - RECOVERY_RATE) * recovery01.getAmount() / rpv01.getAmount(), offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void pvSensitivityTest() {
    PointSensitivities point = PRICER.presentValueSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities res = RATES_PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities exp =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA));
    equalWithRelativeTolerance(res, exp, NOTIONAL * EPS);
    PointSensitivities pointMarkit =
        PRICER_MARKIT.presentValueSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resMarkit = RATES_PROVIDER.parameterSensitivity(pointMarkit);
    CurrencyParameterSensitivities expMarkit =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_MARKIT.presentValue(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA));
    equalWithRelativeTolerance(resMarkit, expMarkit, NOTIONAL * EPS);
    PointSensitivities pointOg = PRICER_OG.presentValueSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resOg = RATES_PROVIDER.parameterSensitivity(pointOg);
    CurrencyParameterSensitivities expOg =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_OG.presentValue(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA));
    equalWithRelativeTolerance(resOg, expOg, NOTIONAL * EPS);
  }

  @Test
  public void priceSensitivityTest() {
    PointSensitivities point = PRICER.priceSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities res = RATES_PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities exp = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA)));
    equalWithRelativeTolerance(res, exp, NOTIONAL * EPS);
    PointSensitivities pointMarkit =
        PRICER_MARKIT.priceSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resMarkit = RATES_PROVIDER.parameterSensitivity(pointMarkit);
    CurrencyParameterSensitivities expMarkit = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_MARKIT.price(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resMarkit, expMarkit, NOTIONAL * EPS);
    PointSensitivities pointOg = PRICER_OG.priceSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resOg = RATES_PROVIDER.parameterSensitivity(pointOg);
    CurrencyParameterSensitivities expOg = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_OG.price(PRODUCT, p, SETTLEMENT_STD, CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resOg, expOg, NOTIONAL * EPS);
  }

  @Test
  public void parSpreadSensitivityTest() {
    PointSensitivities point = PRICER.parSpreadSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities res = RATES_PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities exp = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT, p, SETTLEMENT_STD, REF_DATA)));
    equalWithRelativeTolerance(res, exp, NOTIONAL * EPS);
    PointSensitivities pointMarkit =
        PRICER_MARKIT.parSpreadSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resMarkit = RATES_PROVIDER.parameterSensitivity(pointMarkit);
    CurrencyParameterSensitivities expMarkit = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_MARKIT.parSpread(PRODUCT, p, SETTLEMENT_STD, REF_DATA)));
    equalWithRelativeTolerance(resMarkit, expMarkit, NOTIONAL * EPS);
    PointSensitivities pointOg = PRICER_OG.parSpreadSensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resOg = RATES_PROVIDER.parameterSensitivity(pointOg);
    CurrencyParameterSensitivities expOg = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_OG.parSpread(PRODUCT, p, SETTLEMENT_STD, REF_DATA)));
    equalWithRelativeTolerance(resOg, expOg, NOTIONAL * EPS);
  }

  @Test
  public void riskyAnnuitySensitivityTest() {
    PointSensitivities point = PRICER.riskyAnnuitySensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities res = RATES_PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities exp = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.riskyAnnuity(PRODUCT, p, SETTLEMENT_STD, DIRTY, REF_DATA)));
    equalWithRelativeTolerance(res, exp, NOTIONAL * EPS);
    PointSensitivities pointMarkit =
        PRICER_MARKIT.riskyAnnuitySensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resMarkit = RATES_PROVIDER.parameterSensitivity(pointMarkit);
    CurrencyParameterSensitivities expMarkit = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_MARKIT.riskyAnnuity(PRODUCT, p, SETTLEMENT_STD, DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resMarkit, expMarkit, NOTIONAL * EPS);
    PointSensitivities pointOg = PRICER_OG.riskyAnnuitySensitivity(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA).build();
    CurrencyParameterSensitivities resOg = RATES_PROVIDER.parameterSensitivity(pointOg);
    CurrencyParameterSensitivities expOg = CALC_FD.sensitivity(
        RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_OG.riskyAnnuity(PRODUCT, p, SETTLEMENT_STD, DIRTY, REF_DATA)));
    equalWithRelativeTolerance(resOg, expOg, NOTIONAL * EPS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void jumpToDefaultTest() {
    JumpToDefault computed = PRICER.jumpToDefault(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, REF_DATA);
    LocalDate stepinDate = PRODUCT.getStepinDateOffset().adjust(VALUATION_DATE, REF_DATA);
    double dirtyPvMod =
        PRICER.presentValue(PRODUCT, RATES_PROVIDER, SETTLEMENT_STD, PriceType.DIRTY, REF_DATA).getAmount() / INDEX_FACTOR;
    double accrued = PRODUCT.accruedYearFraction(stepinDate) * PRODUCT.getFixedRate() *
        PRODUCT.getBuySell().normalize(NOTIONAL);
    double protection = PRODUCT.getBuySell().normalize(NOTIONAL) * (1d - RECOVERY_RATE);
    double expected = (protection - accrued - dirtyPvMod) / ((double) LEGAL_ENTITIES.size());
    assertThat(computed.getCurrency()).isEqualTo(USD);
    assertThat(computed.getAmounts().size() == 1).isTrue();
    assertThat(computed.getAmounts().get(INDEX_ID)).isCloseTo(expected, offset(NOTIONAL * TOL));

  }

  @Test
  public void expectedLossTest() {
    CurrencyAmount computed = PRICER.expectedLoss(PRODUCT, RATES_PROVIDER);
    double survivalProbability =
        RATES_PROVIDER.survivalProbabilities(INDEX_ID, USD).survivalProbability(PRODUCT.getProtectionEndDate());
    double expected = (1d - RECOVERY_RATE) * (1d - survivalProbability) * NOTIONAL * INDEX_FACTOR;
    assertThat(computed.getCurrency()).isEqualTo(USD);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  private static CreditRatesProvider createCreditRatesProviderSingle(LocalDate valuationDate, boolean isSingle) {
    IsdaCreditDiscountFactors yc = IsdaCreditDiscountFactors.of(USD, valuationDate, NODAL_YC);
    CreditDiscountFactors cc = isSingle ?
        IsdaCreditDiscountFactors.of(USD, valuationDate, NODAL_CC_SINGLE) :
        IsdaCreditDiscountFactors.of(USD, valuationDate, NODAL_CC);
    ConstantRecoveryRates rr = ConstantRecoveryRates.of(INDEX_ID, valuationDate, RECOVERY_RATE);
    return ImmutableCreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .creditCurves(ImmutableMap.of(Pair.of(INDEX_ID, USD), LegalEntitySurvivalProbabilities.of(INDEX_ID, cc)))
        .discountCurves(ImmutableMap.of(USD, yc))
        .recoveryRateCurves(ImmutableMap.of(INDEX_ID, rr))
        .build();
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

}

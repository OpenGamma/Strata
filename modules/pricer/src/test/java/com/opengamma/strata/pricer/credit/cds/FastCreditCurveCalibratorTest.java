/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.credit.cds.type.CdsConvention;
import com.opengamma.strata.product.credit.cds.type.CdsTemplate;
import com.opengamma.strata.product.credit.cds.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.cds.type.ImmutableCdsConvention;

/**
 * Test {@link FastCreditCurveCalibrator}.
 */
@Test
public class FastCreditCurveCalibratorTest extends IsdaCompliantCreditCurveCalibratorBase {

  // calibrators
  private static final FastCreditCurveCalibrator BUILDER_ISDA =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.ORIGINAL_ISDA);
  private static final FastCreditCurveCalibrator BUILDER_MARKIT =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.MARKIT_FIX);

  private static final double ONE_BP = 1.0e-4;
  private static final double TOL = 1e-14;

  public void regression_consistency_test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, TOL);
    testCalibrationAgainstISDA(BUILDER_MARKIT, TOL);
  }

  // TODO puf test, spread test  buildCurveFromPillarsTest in ParVsQuotedSpreadTest
  // test in PUFCreditCurveCalibrationTest

  private static final LocalDate VALUATION_DATE = LocalDate.of(2013, 2, 27); //Today 

  private static final DoubleArray YC_TIME = DoubleArray.ofUnsafe(new double[] {0.09041095890410959, 0.1726027397260274,
      0.26301369863013696, 0.5123287671232877, 0.7616438356164383, 1.010958904109589, 2.008219178082192, 3.008219178082192,
      4.008219178082192, 5.008219178082192, 6.008219178082192, 7.013698630136987, 8.01095890410959, 9.01095890410959,
      10.01095890410959, 12.01917808219178, 15.016438356164384, 20.01917808219178, 25.021917808219175, 30.027397260273972});
  private static final DoubleArray YC_RATE = DoubleArray.ofUnsafe(new double[] {0.0020651105531615476, 0.0024506037920717797,
      0.0028872269869485313, 0.004599628230463427, 0.006160809466806469, 0.0075703969168129295, 0.003965128877560435,
      0.005059104202201957, 0.0069669135253734825, 0.009361825469323602, 0.011916895611422482, 0.014311922779901886,
      0.016519187063048578, 0.018512121993907647, 0.020289623737560873, 0.02329885162861984, 0.026399509889410745,
      0.029087919732133784, 0.03037740056662963, 0.03110021763406523});
  private static final IsdaCompliantZeroRateDiscountFactors YC =
      IsdaCompliantZeroRateDiscountFactors.of(EUR, VALUATION_DATE, CurveName.of("yc_usd"), YC_TIME, YC_RATE, ACT_365F);

  public void quotedSpreadTest() {
    double[] timeNodeExp = new double[] {
        0.5616438356164384, 1.0575342465753426, 2.0575342465753423, 3.0602739726027397, 4.06027397260274, 5.06027397260274,
        6.06027397260274, 7.063013698630137, 8.063013698630137, 9.063013698630137, 10.063013698630137};
    double[] rateNodeExp = new double[] {
        0.008760540897819375, 0.011038924154511172, 0.015948190247157847, 0.02057127880414737, 0.02563420636849379,
        0.030015477161325396, 0.03269880289798303, 0.03473789476853553, 0.03613718351565995, 0.03726728206325338,
        0.03821628558505742};
    CreditRatesProvider ratesProvider = CreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .discountCurves(ImmutableMap.of(EUR, YC))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, 0.25)))
        .creditCurves(ImmutableMap.of())
        .build();
    LocalDate startDate = LocalDate.of(2012, 12, 20);
    LocalDate[] pillarDates = new LocalDate[] {
        LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20), LocalDate.of(2016, 3, 20),
        LocalDate.of(2017, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2019, 3, 20), LocalDate.of(2020, 3, 20),
        LocalDate.of(2021, 3, 20), LocalDate.of(2022, 3, 20), LocalDate.of(2023, 3, 20)};
    double coupon = 100d * ONE_BP;
    int nPillars = pillarDates.length;
    ImmutableMarketDataBuilder builderCredit = ImmutableMarketData.builder(VALUATION_DATE);
    List<CdsCurveNode> nodes = new ArrayList<>(nPillars);
    double[] quotes = new double[] {
        0.006485, 0.008163, 0.011763, 0.015136, 0.018787, 0.021905, 0.023797, 0.025211, 0.02617, 0.026928, 0.027549};
    for (int i = 0; i < nPillars; ++i) {
      CdsConvention conv = ImmutableCdsConvention.of("conv", EUR, ACT_360, Frequency.P3M, BUS_ADJ, CDS_SETTLE_STD);
      CdsTemplate temp = DatesCdsTemplate.of(startDate, pillarDates[i], conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", pillarDates[i].toString()));
      nodes.add(CdsCurveNode.ofQuotedSpread(temp, id, LEGAL_ENTITY, coupon));
      builderCredit.addValue(id, quotes[i]);
    }
    ImmutableMarketData marketData = builderCredit.build();
    LegalEntitySurvivalProbabilities cc =
        BUILDER_ISDA.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurve = ((IsdaCompliantZeroRateDiscountFactors) cc.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getYValues().toArray(), rateNodeExp, TOL));
  }

  public void pufTest() {

  }

}

/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.CdsIndexIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.LegalEntityInformation;
import com.opengamma.strata.market.observable.LegalEntityInformationId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ResolvedTradeParameterMetadata;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.type.CdsConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;
import com.opengamma.strata.product.credit.type.ImmutableCdsConvention;
import com.opengamma.strata.product.credit.type.TenorCdsTemplate;

/**
 * Test {@link IsdaCompliantIndexCurveCalibrator}.
 */
@Test
public class IsdaCompliantIndexCurveCalibratorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
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
  private static final IsdaCompliantZeroRateDiscountFactors CURVE_YC =
      IsdaCompliantZeroRateDiscountFactors.of(EUR, VALUATION_DATE, NODAL_YC);

  private static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
  private static final DaysAdjustment CDS_SETTLE_STD = DaysAdjustment.ofBusinessDays(3, SAT_SUN);
  private static final CdsConvention CONVENTION = ImmutableCdsConvention.of(
      "conv", EUR, ACT_360, Frequency.P3M, BUS_ADJ, CDS_SETTLE_STD);
  private static final StandardId INDEX_ID = StandardId.of("OG", "ABCXX-Series22-Version5");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES;
  private static final ImmutableList<CdsIndexIsdaCreditCurveNode> CURVE_NODES;
  private static final ImmutableList<CdsIndexIsdaCreditCurveNode> CURVE_NODES_PS;
  private static final ImmutableMarketData MARKET_DATA;
  private static final ImmutableMarketData MARKET_DATA_PS;
  private static final int INDEX_SIZE = 97;
  private static final int NUM_PILLARS = 4;
  private static final ImmutableSet<Integer> DEFAULTED_NAMES = ImmutableSet.of(2, 15, 37, 51);
  private static final double COUPON = 0.05;
  private static final double RECOVERY_RATE_VALUE = 0.3;
  private static final double[] PUF_QUOTES = new double[] {-0.0756, -0.0762, -0.0571, -0.0652};
  private static final double[] PS_QUOTES = new double[] {0.0011, 0.0057, 0.0124, 0.0182};
  private static final Tenor[] INDEX_TENORS = new Tenor[] {Tenor.TENOR_3Y, Tenor.TENOR_5Y, Tenor.TENOR_7Y, Tenor.TENOR_10Y};
  static {
    Builder<StandardId> legalEntityIdsbuilder = ImmutableList.builder();
    ImmutableMarketDataBuilder marketDataBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    ImmutableMarketDataBuilder marketDataPsBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    for (Integer i = 0; i < INDEX_SIZE; ++i) {
      StandardId legalEntityId = StandardId.of("OG", "ABC" + i.toString());
      LegalEntityInformation information = DEFAULTED_NAMES.contains(i) ? LegalEntityInformation.isDefaulted(legalEntityId)
          : LegalEntityInformation.isNotDefaulted(legalEntityId);
      legalEntityIdsbuilder.add(legalEntityId);
      marketDataBuilder.addValue(LegalEntityInformationId.of(legalEntityId), information);
      marketDataPsBuilder.addValue(LegalEntityInformationId.of(legalEntityId), information);
    }
    LEGAL_ENTITIES = legalEntityIdsbuilder.build();
    Builder<CdsIndexIsdaCreditCurveNode> curveNodesBuilder = ImmutableList.builder();
    Builder<CdsIndexIsdaCreditCurveNode> curveNodesPsBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_PILLARS; ++i) {
      QuoteId id = QuoteId.of(StandardId.of("OG", INDEX_TENORS[i].toString()));
      CdsTemplate temp = TenorCdsTemplate.of(INDEX_TENORS[i], CONVENTION);
      curveNodesBuilder.add(CdsIndexIsdaCreditCurveNode.ofPointsUpfront(temp, id, INDEX_ID, LEGAL_ENTITIES, COUPON));
      curveNodesPsBuilder.add(CdsIndexIsdaCreditCurveNode.ofParSpread(temp, id, INDEX_ID, LEGAL_ENTITIES));
      marketDataBuilder.addValue(id, PUF_QUOTES[i]);
      marketDataPsBuilder.addValue(id, PS_QUOTES[i]);
    }
    CURVE_NODES = curveNodesBuilder.build();
    MARKET_DATA = marketDataBuilder.build();
    CURVE_NODES_PS = curveNodesPsBuilder.build();
    MARKET_DATA_PS = marketDataPsBuilder.build();
  }
  private static final ImmutableCreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .discountCurves(ImmutableMap.of(EUR, CURVE_YC))
      .recoveryRateCurves(ImmutableMap.of(INDEX_ID, ConstantRecoveryRates.of(INDEX_ID, VALUATION_DATE, RECOVERY_RATE_VALUE)))
      .build();
  private static final CurveName CURVE_NAME = CurveName.of("test_credit");
  private static final IsdaCompliantIndexCurveCalibrator CALIBRATOR = IsdaCompliantIndexCurveCalibrator.DEFAULT;
  private static final double TOL = 1.0e-14;
  private static final double EPS = 1.0e-4;

  public void test_regression() {
    double[] expectedTimes = new double[] {2.852054794520548, 4.852054794520548, 6.854794520547945, 9.854794520547944};
    double[] expectedRates = new double[] {0.03240798261187516, 0.04858422754375164, 0.0616141083562273, 0.06235460926516589};
    IsdaCreditCurveDefinition curveDefinition = IsdaCreditCurveDefinition.of(
        CURVE_NAME, EUR, VALUATION_DATE, ACT_365F, CURVE_NODES, true, false);
    LegalEntitySurvivalProbabilities creditCurve = CALIBRATOR.calibrate(curveDefinition, MARKET_DATA, RATES_PROVIDER, REF_DATA);
    NodalCurve curve = (NodalCurve) creditCurve.getSurvivalProbabilities().findData(CURVE_NAME).get();
    assertTrue(DoubleArrayMath.fuzzyEquals(curve.getXValues().toArray(), expectedTimes, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(curve.getYValues().toArray(), expectedRates, TOL));
    assertTrue(curve.getParameterMetadata(0) instanceof DatedParameterMetadata);
    assertTrue(curve.getParameterMetadata(1) instanceof DatedParameterMetadata);
    assertTrue(curve.getParameterMetadata(2) instanceof DatedParameterMetadata);
    assertTrue(curve.getParameterMetadata(3) instanceof DatedParameterMetadata);
    double computedIndex = curve.getMetadata().getInfo(CurveInfoType.CDS_INDEX_FACTOR);
    assertEquals(computedIndex, 93.0 / 97.0, TOL);
    testJacobian(creditCurve, RATES_PROVIDER, CURVE_NODES, PUF_QUOTES);
  }

  public void test_regression_single() {
    double[] expectedTimes = new double[] {4.852054794520548};
    double[] expectedRates = new double[] {0.04666754810728295};
    ImmutableList<CdsIndexIsdaCreditCurveNode> singleNode = CURVE_NODES.subList(1, 2);
    IsdaCreditCurveDefinition curveDefinition = IsdaCreditCurveDefinition.of(
        CURVE_NAME, EUR, VALUATION_DATE, ACT_365F, singleNode, true, false);
    LegalEntitySurvivalProbabilities creditCurve = CALIBRATOR.calibrate(curveDefinition, MARKET_DATA, RATES_PROVIDER, REF_DATA);
    NodalCurve curve = (NodalCurve) creditCurve.getSurvivalProbabilities().findData(CURVE_NAME).get();
    assertTrue(DoubleArrayMath.fuzzyEquals(curve.getXValues().toArray(), expectedTimes, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(curve.getYValues().toArray(), expectedRates, TOL));
    assertTrue(curve.getParameterMetadata(0) instanceof DatedParameterMetadata);
    double computedIndex = curve.getMetadata().getInfo(CurveInfoType.CDS_INDEX_FACTOR);
    assertEquals(computedIndex, 93.0 / 97.0, TOL);
    testJacobian(creditCurve, RATES_PROVIDER, singleNode, PUF_QUOTES);
  }

  public void test_consistency_singleName() {
    IsdaCreditCurveDefinition curveDefinition = IsdaCreditCurveDefinition.of(
        CURVE_NAME, EUR, VALUATION_DATE, ACT_365F, CURVE_NODES_PS, true, true);
    LegalEntitySurvivalProbabilities creditCurveComputed = CALIBRATOR.calibrate(
        curveDefinition, MARKET_DATA_PS, RATES_PROVIDER, REF_DATA);
    NodalCurve curveComputed = (NodalCurve) creditCurveComputed.getSurvivalProbabilities().findData(CURVE_NAME).get();
    double computedIndex = curveComputed.getMetadata().getInfo(CurveInfoType.CDS_INDEX_FACTOR);
    assertEquals(computedIndex, 93.0 / 97.0, TOL);
    IsdaCompliantCreditCurveCalibrator cdsCalibrator = FastCreditCurveCalibrator.DEFAULT;
    List<CdsIsdaCreditCurveNode> cdsNodes = new ArrayList<>();
    for (int i = 0; i < CURVE_NODES_PS.size(); ++i) {
      cdsNodes.add(CdsIsdaCreditCurveNode.ofParSpread(
          CURVE_NODES_PS.get(i).getTemplate(),
          CURVE_NODES_PS.get(i).getObservableId(),
          CURVE_NODES_PS.get(i).getCdsIndexId()));
      ParameterMetadata metadata = curveComputed.getParameterMetadata(i);
      assertTrue(metadata instanceof ResolvedTradeParameterMetadata);
      ResolvedTradeParameterMetadata tradeMetadata = (ResolvedTradeParameterMetadata) metadata;
      assertTrue(tradeMetadata.getTrade() instanceof ResolvedCdsIndexTrade);
    }
    IsdaCreditCurveDefinition cdsCurveDefinition = IsdaCreditCurveDefinition.of(
        CURVE_NAME, EUR, VALUATION_DATE, ACT_365F, cdsNodes, true, false);
    LegalEntitySurvivalProbabilities creditCurveExpected = cdsCalibrator.calibrate(
        cdsCurveDefinition, MARKET_DATA_PS, RATES_PROVIDER, REF_DATA);
    NodalCurve curveExpected = (NodalCurve) creditCurveExpected.getSurvivalProbabilities().findData(CURVE_NAME).get();
    assertTrue(DoubleArrayMath.fuzzyEquals(curveComputed.getXValues().toArray(), curveExpected.getXValues().toArray(), TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(curveComputed.getYValues().toArray(), curveExpected.getYValues().toArray(), TOL));
    assertEquals(curveComputed.getMetadata().getInfo(CurveInfoType.JACOBIAN),
        curveExpected.getMetadata().getInfo(CurveInfoType.JACOBIAN));
  }

  //-------------------------------------------------------------------------
  protected void testJacobian(
      LegalEntitySurvivalProbabilities curve,
      ImmutableCreditRatesProvider ratesProvider,
      List<CdsIndexIsdaCreditCurveNode> nodes,
      double[] quotes) {

    int nNode = nodes.size();
    IsdaCompliantZeroRateDiscountFactors df = (IsdaCompliantZeroRateDiscountFactors) curve.getSurvivalProbabilities();
    int nCurveNode = df.getParameterCount();
    for (int i = 0; i < nCurveNode; ++i) {
      double[] quotesUp = Arrays.copyOf(quotes, nNode);
      double[] quotesDw = Arrays.copyOf(quotes, nNode);
      quotesUp[i] += EPS;
      quotesDw[i] -= EPS;
      ImmutableMarketDataBuilder builderCreditUp = MARKET_DATA.toBuilder();
      ImmutableMarketDataBuilder builderCreditDw = MARKET_DATA.toBuilder();
      for (int j = 0; j < nNode; ++j) {
        builderCreditUp.addValue(nodes.get(j).getObservableId(), quotesUp[j]);
        builderCreditDw.addValue(nodes.get(j).getObservableId(), quotesDw[j]);
      }
      ImmutableMarketData marketDataUp = builderCreditUp.build();
      ImmutableMarketData marketDataDw = builderCreditDw.build();
      IsdaCreditCurveDefinition definition = IsdaCreditCurveDefinition.of(
          df.getCurve().getName(), df.getCurrency(), df.getValuationDate(), df.getDayCount(), nodes, false, false);
      IsdaCompliantZeroRateDiscountFactors ccUp = (IsdaCompliantZeroRateDiscountFactors) CALIBRATOR
          .calibrate(definition, marketDataUp, ratesProvider, REF_DATA).getSurvivalProbabilities();
      IsdaCompliantZeroRateDiscountFactors ccDw = (IsdaCompliantZeroRateDiscountFactors) CALIBRATOR
          .calibrate(definition, marketDataDw, ratesProvider, REF_DATA).getSurvivalProbabilities();
      for (int j = 0; j < nNode; ++j) {
        double computed = df.getCurve().getMetadata().findInfo(CurveInfoType.JACOBIAN).get().getJacobianMatrix().get(j, i);
        double expected = 0.5 * (ccUp.getCurve().getYValues().get(j) - ccDw.getCurve().getYValues().get(j)) / EPS;
        assertEquals(computed, expected, EPS * 10d);
      }
    }
  }

}

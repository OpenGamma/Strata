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
import java.time.format.DateTimeFormatter;
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
  private static final double ONE_PC = 1.0e-2;
  private static final double TOL = 1e-14;
  private static final double EPS = 1.0e-5;

  public void regression_consistency_test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, TOL);
    testCalibrationAgainstISDA(BUILDER_MARKIT, TOL);
  }

  public void parSpreadTest() {
    LocalDate valuationDate = LocalDate.of(2013, 2, 27);
    DoubleArray ycTime = DoubleArray.ofUnsafe(new double[] {
        0.09041095890410959, 0.1726027397260274, 0.26301369863013696, 0.5123287671232877, 0.7616438356164383, 1.010958904109589,
        2.008219178082192, 3.008219178082192, 4.008219178082192, 5.008219178082192, 6.008219178082192, 7.013698630136987,
        8.01095890410959, 9.01095890410959, 10.01095890410959, 12.01917808219178, 15.016438356164384, 20.01917808219178,
        25.021917808219175, 30.027397260273972});
    DoubleArray ycRate = DoubleArray.ofUnsafe(new double[] {
        0.0020651105531615476, 0.0024506037920717797, 0.0028872269869485313, 0.004599628230463427, 0.006160809466806469,
        0.0075703969168129295, 0.003965128877560435, 0.005059104202201957, 0.0069669135253734825, 0.009361825469323602,
        0.011916895611422482, 0.014311922779901886, 0.016519187063048578, 0.018512121993907647, 0.020289623737560873,
        0.02329885162861984, 0.026399509889410745, 0.029087919732133784, 0.03037740056662963, 0.03110021763406523});
    IsdaCompliantZeroRateDiscountFactors yc =
        IsdaCompliantZeroRateDiscountFactors.of(EUR, valuationDate, CurveName.of("yc_usd"), ycTime, ycRate, ACT_365F);
    double[] timeNodeExp = new double[] {
        0.5616438356164384, 1.0575342465753426, 2.0575342465753423, 3.0602739726027397, 4.06027397260274, 5.06027397260274,
        6.06027397260274, 7.063013698630137, 8.063013698630137, 9.063013698630137, 10.063013698630137};
    double[] rateNodeExp = new double[] {
        0.00876054089781935, 0.011037345646850688, 0.015955126945240167, 0.020617953392829177, 0.025787811343896218,
        0.030329992053915133, 0.03313419899444371, 0.03528129159875671, 0.03675340516560903, 0.037946169956317416,
        0.038951101800190346};
    double[] rateNodeExpMf = new double[] {
        0.008754510260229803, 0.011030502992814844, 0.01594817866773906, 0.02060947097554756, 0.025776720596175737,
        0.030316032527460755, 0.03311839631615255, 0.03526404051997617, 0.03673513322394772, 0.03792689865945585,
        0.03893107891569398};
    CreditRatesProvider ratesProvider = CreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, valuationDate, 0.25)))
        .creditCurves(ImmutableMap.of())
        .build();
    LocalDate startDate = LocalDate.of(2012, 12, 20);
    LocalDate[] pillarDates = new LocalDate[] {
        LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20), LocalDate.of(2016, 3, 20),
        LocalDate.of(2017, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2019, 3, 20), LocalDate.of(2020, 3, 20),
        LocalDate.of(2021, 3, 20), LocalDate.of(2022, 3, 20), LocalDate.of(2023, 3, 20)};
    int nPillars = pillarDates.length;
    ImmutableMarketDataBuilder builderCredit = ImmutableMarketData.builder(valuationDate);
    List<CdsCurveNode> nodes = new ArrayList<>(nPillars);
    double[] quotes = new double[] {
        0.006485, 0.008163, 0.011763, 0.015136, 0.018787, 0.021905, 0.023797, 0.025211, 0.02617, 0.026928, 0.027549};
    for (int i = 0; i < nPillars; ++i) {
      CdsConvention conv = ImmutableCdsConvention.of("conv", EUR, ACT_360, Frequency.P3M, BUS_ADJ, CDS_SETTLE_STD);
      CdsTemplate temp = DatesCdsTemplate.of(startDate, pillarDates[i], conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", pillarDates[i].toString()));
      nodes.add(CdsCurveNode.ofParSpread(temp, id, LEGAL_ENTITY));
      builderCredit.addValue(id, quotes[i]);
    }
    ImmutableMarketData marketData = builderCredit.build();
    LegalEntitySurvivalProbabilities cc =
        BUILDER_ISDA.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurve = ((IsdaCompliantZeroRateDiscountFactors) cc.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getYValues().toArray(), rateNodeExp, TOL));
    testJacobian(BUILDER_ISDA, cc, ratesProvider, nodes, quotes, 1d, EPS);
    LegalEntitySurvivalProbabilities ccMf =
        BUILDER_MARKIT.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurveMf = ((IsdaCompliantZeroRateDiscountFactors) ccMf.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getYValues().toArray(), rateNodeExpMf, TOL));
    testJacobian(BUILDER_MARKIT, ccMf, ratesProvider, nodes, quotes, 1d, EPS);
  }

  public void quotedSpreadTest() {
    LocalDate valuationDate = LocalDate.of(2013, 2, 27);
    DoubleArray ycTime = DoubleArray.ofUnsafe(new double[] {
        0.09041095890410959, 0.1726027397260274, 0.26301369863013696, 0.5123287671232877, 0.7616438356164383, 1.010958904109589,
        2.008219178082192, 3.008219178082192, 4.008219178082192, 5.008219178082192, 6.008219178082192, 7.013698630136987,
        8.01095890410959, 9.01095890410959, 10.01095890410959, 12.01917808219178, 15.016438356164384, 20.01917808219178,
        25.021917808219175, 30.027397260273972});
    DoubleArray ycRate = DoubleArray.ofUnsafe(new double[] {
        0.0020651105531615476, 0.0024506037920717797, 0.0028872269869485313, 0.004599628230463427, 0.006160809466806469,
        0.0075703969168129295, 0.003965128877560435, 0.005059104202201957, 0.0069669135253734825, 0.009361825469323602,
        0.011916895611422482, 0.014311922779901886, 0.016519187063048578, 0.018512121993907647, 0.020289623737560873,
        0.02329885162861984, 0.026399509889410745, 0.029087919732133784, 0.03037740056662963, 0.03110021763406523});
    IsdaCompliantZeroRateDiscountFactors yc =
        IsdaCompliantZeroRateDiscountFactors.of(EUR, valuationDate, CurveName.of("yc_usd"), ycTime, ycRate, ACT_365F);
    double[] timeNodeExp = new double[] {
        0.5616438356164384, 1.0575342465753426, 2.0575342465753423, 3.0602739726027397, 4.06027397260274, 5.06027397260274,
        6.06027397260274, 7.063013698630137, 8.063013698630137, 9.063013698630137, 10.063013698630137};
    double[] rateNodeExp = new double[] {
        0.008760540897819375, 0.011038924154511172, 0.015948190247157847, 0.02057127880414737, 0.02563420636849379,
        0.030015477161325396, 0.03269880289798303, 0.03473789476853553, 0.03613718351565995, 0.03726728206325338,
        0.03821628558505742};
    double[] rateNodeExpMf = new double[] {
        0.008754510260229767, 0.011032176237293093, 0.01594092142956685, 0.02056170591864977, 0.02562102500142847,
        0.029998725498341292, 0.03268003643439498, 0.0347176862923525, 0.03611609798069609, 0.037245360881562876,
        0.0381937470380447};
    CreditRatesProvider ratesProvider = CreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, valuationDate, 0.25)))
        .creditCurves(ImmutableMap.of())
        .build();
    LocalDate startDate = LocalDate.of(2012, 12, 20);
    LocalDate[] pillarDates = new LocalDate[] {
        LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20), LocalDate.of(2016, 3, 20),
        LocalDate.of(2017, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2019, 3, 20), LocalDate.of(2020, 3, 20),
        LocalDate.of(2021, 3, 20), LocalDate.of(2022, 3, 20), LocalDate.of(2023, 3, 20)};
    double coupon = 100d * ONE_BP;
    int nPillars = pillarDates.length;
    ImmutableMarketDataBuilder builderCredit = ImmutableMarketData.builder(valuationDate);
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
    testJacobian(BUILDER_ISDA, cc, ratesProvider, nodes, quotes, 1d, EPS);
    LegalEntitySurvivalProbabilities ccMf =
        BUILDER_MARKIT.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurveMf = ((IsdaCompliantZeroRateDiscountFactors) ccMf.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getYValues().toArray(), rateNodeExpMf, TOL));
    testJacobian(BUILDER_MARKIT, ccMf, ratesProvider, nodes, quotes, 1d, EPS);
  }

  public void pufTest() {
    LocalDate valuationDate = LocalDate.of(2013, 4, 10);
    DoubleArray ycTime = DoubleArray.ofUnsafe(new double[] {0.09041095890410959, 0.1726027397260274, 0.2547945205479452,
        0.5123287671232877, 0.7616438356164383, 1.010958904109589, 2.008219178082192, 3.008219178082192, 4.008219178082192,
        5.008219178082192, 6.008219178082192, 7.013698630136987, 8.01095890410959, 9.01095890410959, 10.01095890410959,
        12.01917808219178, 15.016438356164384, 20.01917808219178, 25.021917808219175, 30.027397260273972});
    DoubleArray ycRate = DoubleArray.ofUnsafe(new double[] {0.0020205071813561414, 0.0024226927083852126, 0.00280147037504029,
        0.004449041082144009, 0.005821804782808804, 0.007254879152733453, 0.00378133614924816, 0.004815163234294319,
        0.006576302084547871, 0.00884241431837336, 0.011358805989279104, 0.013793391727035883, 0.016014197840890115,
        0.01801564209277191, 0.019757164421290663, 0.022773295945438254, 0.025862337032619587, 0.02848646344754061,
        0.029753383126110852, 0.03045277462637107});
    IsdaCompliantZeroRateDiscountFactors yc =
        IsdaCompliantZeroRateDiscountFactors.of(EUR, valuationDate, CurveName.of("yc_usd"), ycTime, ycRate, ACT_365F);

    double[] timeNodeExp = new double[] {
        0.19452054794520549, 0.4465753424657534, 0.6958904109589041, 0.9424657534246575, 1.1945205479452055, 1.4465753424657535,
        1.6958904109589041, 1.9424657534246574, 2.1945205479452055, 2.4465753424657533, 2.695890410958904, 2.9452054794520546,
        3.197260273972603, 3.4493150684931506, 3.6986301369863015, 3.9452054794520546, 4.197260273972603, 4.449315068493151,
        4.698630136986301, 4.945205479452055, 5.197260273972603, 5.449315068493151, 5.698630136986301, 5.945205479452055,
        6.197260273972603, 6.449315068493151, 6.698630136986301, 6.947945205479452, 7.2, 7.4520547945205475, 7.701369863013698,
        7.947945205479452, 8.2, 8.452054794520548, 8.7013698630137, 8.947945205479453, 9.2, 9.452054794520548, 9.7013698630137,
        9.947945205479453, 10.2};
    double[] rateNodeExp = new double[] {
        0.11219168510100914, 0.11085321179769615, 0.11753783265486063, 0.11806409789291543, 0.12007843111645247,
        0.12273722191216528, 0.12541993298405366, 0.12773640093265545, 0.1290535220739981, 0.13294183149211675,
        0.13659302947963856, 0.13988488561043758, 0.1429469312254705, 0.14606538453369572, 0.14916286828444447, 0.15219682906227,
        0.1548315745851032, 0.158141193071526, 0.16163981714033765, 0.1650400193930357, 0.1682351993447916, 0.1683744003954113,
        0.168657453080796, 0.16915067878510565, 0.1694852880010724, 0.16990705130936645, 0.1704456138969621, 0.17105852486248443,
        0.1717088423125347, 0.1727906445582425, 0.17407566745397665, 0.17547300248653266, 0.17679395545074758,
        0.17769841457372118, 0.1788064602071617, 0.18001498257267778, 0.18123747758791092, 0.18253661761388457,
        0.18406319235262744, 0.18582983758830868, 0.18750386499176422};
    double[] rateNodeExpMf = new double[] {
        0.008754510260229767, 0.011032176237293093, 0.01594092142956685, 0.02056170591864977, 0.02562102500142847,
        0.029998725498341292, 0.03268003643439498, 0.0347176862923525, 0.03611609798069609, 0.037245360881562876,
        0.0381937470380447};
    CreditRatesProvider ratesProvider = CreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, valuationDate, 0.4)))
        .creditCurves(ImmutableMap.of())
        .build();
    LocalDate startDate = LocalDate.of(2013, 3, 20);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    String[] maturityString = new String[] {"20/06/2013", "20/09/2013", "20/12/2013", "20/03/2014", "20/06/2014", "20/09/2014",
        "20/12/2014", "20/03/2015", "20/06/2015", "20/09/2015", "20/12/2015", "20/03/2016", "20/06/2016", "20/09/2016",
        "20/12/2016", "20/03/2017", "20/06/2017", "20/09/2017", "20/12/2017", "20/03/2018", "20/06/2018", "20/09/2018",
        "20/12/2018", "20/03/2019", "20/06/2019", "20/09/2019", "20/12/2019", "20/03/2020", "20/06/2020", "20/09/2020",
        "20/12/2020", "20/03/2021", "20/06/2021", "20/09/2021", "20/12/2021", "20/03/2022", "20/06/2022",
        "20/09/2022", "20/12/2022", "20/03/2023", "20/06/2023"};
    int nPillars = maturityString.length;
    double coupon = 500d * ONE_BP;
    ImmutableMarketDataBuilder builderCredit = ImmutableMarketData.builder(valuationDate);
    List<CdsCurveNode> nodes = new ArrayList<>(nPillars);
    double[] quotes = new double[] {
        0.32, 0.69, 1.32, 1.79, 2.36, 3.01, 3.7, 4.39, 5.02, 5.93, 6.85, 7.76, 8.67, 9.6, 10.53, 11.45, 12.33, 13.29, 14.26, 15.2,
        16.11, 16.62, 17.12, 17.62, 18.09, 18.55, 19, 19.44, 19.87, 20.33, 20.79, 21.24, 21.67, 22.04, 22.41, 22.77, 23.12, 23.46,
        23.8, 24.14, 24.46};
    for (int i = 0; i < nPillars; ++i) {
      LocalDate pillarDate = LocalDate.parse(maturityString[i], formatter);
      CdsConvention conv = ImmutableCdsConvention.of("conv", EUR, ACT_360, Frequency.P3M, BUS_ADJ, CDS_SETTLE_STD);
      CdsTemplate temp = DatesCdsTemplate.of(startDate, pillarDate, conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", pillarDate.toString()));
      nodes.add(CdsCurveNode.ofPointsUpfront(temp, id, LEGAL_ENTITY, coupon));
      builderCredit.addValue(id, quotes[i] * ONE_PC);
    }
    ImmutableMarketData marketData = builderCredit.build();
    LegalEntitySurvivalProbabilities cc =
        BUILDER_ISDA.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurve = ((IsdaCompliantZeroRateDiscountFactors) cc.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurve.getYValues().toArray(), rateNodeExp, TOL));
    testJacobian(BUILDER_ISDA, cc, ratesProvider, nodes, quotes, ONE_PC, EPS);
    LegalEntitySurvivalProbabilities ccMf =
        BUILDER_MARKIT.calibrate(nodes, CurveName.of("cc"), marketData, ratesProvider, REF_DATA);
    NodalCurve resCurveMf = ((IsdaCompliantZeroRateDiscountFactors) ccMf.getSurvivalProbabilities()).getCurve();
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getXValues().toArray(), timeNodeExp, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(resCurveMf.getYValues().toArray(), rateNodeExpMf, TOL));
    testJacobian(BUILDER_MARKIT, ccMf, ratesProvider, nodes, quotes, ONE_PC, EPS);
  }

}

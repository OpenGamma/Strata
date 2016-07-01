/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * RatesProvider data sets for testing.
 */
public class StandardDataSets {
  // values ported from StandardDataSetsMulticurveUSD and StandardDataSetsMulticurveEUR

  /** Wednesday. */
  public static final LocalDate VAL_DATE_2014_01_22 = LocalDate.of(2014, 1, 22);

  public static final FxMatrix FX_MATRIX = FxMatrix.empty();
  public static final FxMatrix FX_MATRIX_EUR_USD = FxMatrix.of(EUR, USD, 1.20);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR = CurveExtrapolators.FLAT;

  //-------------------------------------------------------------------------
  // Group 1
  // Discounting, Overnight, Libor 1/3/6
  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP1_USD_DSC_TIMES = DoubleArray.of(
      0.0027397260273972603d,
      0.005479452054794521d,
      0.0958904109589041d,
      0.1726027397260274d,
      0.26301369863013696d,
      0.5123287671232877d,
      0.7643835616438356d,
      1.0164383561643835d,
      2.0135040047907777d,
      3.010958904109589d,
      4.010958904109589d,
      5.016438356164383d,
      6.016236245227937d,
      7.013698630136986d,
      8.01095890410959d,
      9.01095890410959d,
      10.010771764353619d);
  private static final DoubleArray GROUP1_USD_DSC_VALUES = DoubleArray.of(
      0.001774301243044416d,
      0.0016475657039787027d,
      8.009449792765705e-4d,
      7.991342366517293e-4d,
      7.76942929281221e-4d,
      8.011052753850107e-4d,
      8.544769819435054e-4d,
      0.0010101196182894087d,
      0.0025295133435066d,
      0.005928027386129779d,
      0.009984669002766415d,
      0.013910233828704998d,
      0.017362472692574273d,
      0.020265668368085216d,
      0.02272069332675378d,
      0.02478235199041099d,
      0.026505391310201267);
  public static final InterpolatedNodalCurve GROUP1_USD_DSC = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-DSCON-OIS", ACT_ACT_ISDA))
      .xValues(GROUP1_USD_DSC_TIMES)
      .yValues(GROUP1_USD_DSC_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();
  public static final InterpolatedNodalCurve GROUP1_USD_ON = GROUP1_USD_DSC;

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP1_USD_L1M_TIMES = DoubleArray.of(
      0.09041095890410959d,
      0.5013698630136987d,
      0.7534246575342466d,
      1.010958904109589d,
      2.0162362452279363d,
      3.0136986301369864d,
      4.010958904109589d,
      5.005479452054795d,
      7.016438356164383d,
      10.010771764353619d,
      12.01095890410959d,
      15.01095890410959d,
      20.01095890410959d,
      25.008219178082193d,
      30.016236245227937d);
  private static final DoubleArray GROUP1_USD_L1M_VALUES = DoubleArray.of(
      0.001561285369284162d,
      0.001677637032983316d,
      0.0017963184917998965d,
      0.0019241139503541746d,
      0.0037825750499024123d,
      0.00744254801877514d,
      0.011692283234671374d,
      0.015736966207301174d,
      0.02235969769823604d,
      0.028782832293124807d,
      0.03156162339762925d,
      0.03428973495769252d,
      0.03649965088741946d,
      0.0373988585367752d,
      0.037751996205357616d);
  public static final InterpolatedNodalCurve GROUP1_USD_L1M = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-LIBOR1M-FRABS", ACT_ACT_ISDA))
      .xValues(GROUP1_USD_L1M_TIMES)
      .yValues(GROUP1_USD_L1M_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP1_USD_L3M_TIMES = DoubleArray.of(
      0.25205479452054796d,
      0.5013698630136987d,
      0.7534246575342466d,
      1.010958904109589d,
      2.0107717643536196d,
      3.0054794520547947d,
      4.005479452054795d,
      5.005479452054795d,
      7.010958904109589d,
      10.005307283479302d,
      12.01095890410959d,
      15.005479452054795d,
      20.005479452054793d,
      25.008219178082193d,
      30.01077176435362d);
  private static final DoubleArray GROUP1_USD_L3M_VALUES = DoubleArray.of(
      0.0023773794390540754d,
      0.002418692953929592d,
      0.002500627386941208d,
      0.0026475398935223386d,
      0.004482958991370022d,
      0.008123927669512582d,
      0.012380488135102554d,
      0.016448386998565587d,
      0.023026212753825527d,
      0.02933978147314788d,
      0.03208786808445603d,
      0.03475307015968327d,
      0.03689179443401797d,
      0.03776622232525567d,
      0.03810645431268752d);
  public static final InterpolatedNodalCurve GROUP1_USD_L3M = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-LIBOR3M-FRAIRS", ACT_ACT_ISDA))
      .xValues(GROUP1_USD_L3M_TIMES)
      .yValues(GROUP1_USD_L3M_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP1_USD_L6M_TIMES = DoubleArray.of(
      0.5013698630136987d,
      0.7534246575342466d,
      1.0136986301369864d,
      2.0107717643536196d,
      3.008219178082192d,
      4.005479452054795d,
      5.005479452054795d,
      7.010958904109589d,
      10.005307283479302d,
      12.013698630136986d,
      15.005479452054795d,
      20.008219178082193d,
      25.01095890410959d,
      30.01077176435362d);
  private static final DoubleArray GROUP1_USD_L6M_VALUES = DoubleArray.of(
      0.003340024121442567d,
      0.0034093794939005517d,
      0.0035662655276726372d,
      0.005397906145818395d,
      0.00904615066816097d,
      0.013303401778277804d,
      0.0173694782570321d,
      0.02398703895533916d,
      0.030336147956946564d,
      0.03313011714776066d,
      0.03581566678731747d,
      0.03793869351638767d,
      0.038810658906253376d,
      0.03914178215349321d);
  public static final InterpolatedNodalCurve GROUP1_USD_L6M = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-LIBOR6M-FRABS", ACT_ACT_ISDA))
      .xValues(GROUP1_USD_L6M_TIMES)
      .yValues(GROUP1_USD_L6M_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();

  //-------------------------------------------------------------------------
  // Group 2
  // curve group with two curves (ONDSC-OIS/LIBOR3M-FRAIRS). 
  // ONDSC-OIS was calibrated on OIS up to 10Y and LIBOR3M-FRAIRS was calibrated on FRA and IRS up to 30Y
  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP2_USD_DSC_TIMES = DoubleArray.of(
      0.0027397260273972603d,
      0.005479452054794521d,
      0.0958904109589041d,
      0.1726027397260274d,
      0.26301369863013696d,
      0.5123287671232877d,
      0.7643835616438356d,
      1.0164383561643835d,
      2.0135040047907777d,
      3.010958904109589d,
      4.010958904109589d,
      5.016438356164383d,
      6.016236245227937d,
      7.013698630136986d,
      8.01095890410959d,
      9.01095890410959d,
      10.010771764353619d);
  private static final DoubleArray GROUP2_USD_DSC_VALUES = DoubleArray.of(
      0.0016222186172986405d,
      0.00162221861730832d,
      7.299773709349916E-4d,
      8.670549606040686E-4d,
      9.700626686374421E-4d,
      9.177530275077657E-4d,
      0.0010750576630832423d,
      0.0012058612411863369d,
      0.0032359093289564553d,
      0.007163512731127383d,
      0.011437211252834173d,
      0.015530046481112931d,
      0.018986079596038204d,
      0.021785532652164402d,
      0.024156440792908775d,
      0.026125713163116404d,
      0.027830307875488028d);
  public static final InterpolatedNodalCurve GROUP2_USD_DSC = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-DSCON-OIS", ACT_ACT_ISDA))
      .xValues(GROUP2_USD_DSC_TIMES)
      .yValues(GROUP2_USD_DSC_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();
  public static final InterpolatedNodalCurve GROUP2_USD_ON = GROUP2_USD_DSC;

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP2_USD_L3M_TIMES = DoubleArray.of(
      0.25205479452054796d,
      0.5013698630136987d,
      0.7534246575342466d,
      1.010958904109589d,
      2.0107717643536196d,
      3.0054794520547947d,
      4.005479452054795d,
      5.005479452054795d,
      7.010958904109589d,
      10.005307283479302d,
      12.01095890410959d,
      15.005479452054795d,
      20.005479452054793d,
      25.008219178082193d,
      30.01077176435362d);
  private static final DoubleArray GROUP2_USD_L3M_VALUES = DoubleArray.of(
      0.0023981519275776695d,
      0.0025072335113249054d,
      0.002672247938415502d,
      0.0029388810318777664d,
      0.005025004780170895d,
      0.009394666160082296d,
      0.013869551864087672d,
      0.017472135847472845d,
      0.02446348818110633d,
      0.030235440372473818d,
      0.03317759744596232d,
      0.03565652765470901d,
      0.03766720986914944d,
      0.038414911035240557d,
      0.03867657324723196d);
  public static final InterpolatedNodalCurve GROUP2_USD_L3M = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("USD-LIBOR3M-FRAIRS", ACT_ACT_ISDA))
      .xValues(GROUP2_USD_L3M_TIMES)
      .yValues(GROUP2_USD_L3M_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP2_EUR_DSC_TIMES = DoubleArray.of(
      0.0027397260273972603d,
      0.005479452054794521d,
      0.08493150684931507d,
      0.17534246575342466d,
      0.25205479452054796d,
      0.5041095890410959d,
      0.7561643835616438d,
      1.0136986301369864d,
      2.0133018938543303d,
      3.008219178082192d,
      4.008219178082192d,
      5.008219178082192d,
      7.013698630136986d,
      10.007837412980013d,
      12.013698630136986d,
      15.008219178082191d);
  private static final DoubleArray GROUP2_EUR_DSC_VALUES = DoubleArray.of(
      9.88540328028829E-4d,
      0.0016348915995835695d,
      0.0016862448998847426d,
      0.0015546295156997848d,
      0.0014447983014907026d,
      0.0012082816674727926d,
      0.0011379440970582747d,
      0.0010705162326298077d,
      0.0014509089700443698d,
      0.002528095999360014d,
      0.004350134523697582d,
      0.006441998340216978d,
      0.01063335354910032d,
      0.016080880989322904d,
      0.018609566754615454d,
      0.021475823757363427d);
  public static final InterpolatedNodalCurve GROUP2_EUR_DSC = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("EUR-DSCON-OIS", ACT_ACT_ISDA))
      .xValues(GROUP2_EUR_DSC_TIMES)
      .yValues(GROUP2_EUR_DSC_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();
  public static final InterpolatedNodalCurve GROUP2_EUR_ON = GROUP2_EUR_DSC;

  //-------------------------------------------------------------------------
  private static final DoubleArray GROUP2_EUR_L3M_TIMES = DoubleArray.of(
      0.2493150684931507d,
      0.5013698630136987d,
      0.7534246575342466d,
      1.0054794520547945d,
      2.010569653417172d,
      3.008219178082192d,
      4.005479452054795d,
      5.005479452054795d,
      6.005105172542855d,
      7.010958904109589d,
      8.01095890410959d,
      9.008219178082191d,
      10.005105172542855d,
      12.005479452054795d,
      15.005479452054795d,
      20.008219178082193d,
      30.01056965341717d);
  private static final DoubleArray GROUP2_EUR_L3M_VALUES = DoubleArray.of(
      0.002908829285511484d,
      0.002679595161474885d,
      0.002610519134119294d,
      0.0026038833073547614d,
      0.003075239896709737d,
      0.004451934843682878d,
      0.0064044094866778966d,
      0.008564374298554072d,
      0.01069642266568141d,
      0.012743948064905818d,
      0.01463683846130409d,
      0.016353074919227956d,
      0.017875887346528844d,
      0.020470246422656945d,
      0.023099261079406665d,
      0.024998109586262342d,
      0.02549542298737718d);
  public static final InterpolatedNodalCurve GROUP2_EUR_L3M = InterpolatedNodalCurve.builder()
      .metadata(Curves.zeroRates("EUR-EURIBOR3M-FRAIRS", ACT_ACT_ISDA))
      .xValues(GROUP2_EUR_L3M_TIMES)
      .yValues(GROUP2_EUR_L3M_VALUES)
      .extrapolatorLeft(EXTRAPOLATOR)
      .interpolator(INTERPOLATOR)
      .extrapolatorRight(EXTRAPOLATOR)
      .build();

  //-------------------------------------------------------------------------
  /**
   * Provides rates for USD Discounting, Overnight FedFund and Libor 1/3/6 month.
   * 
   * @return the rates provider
   */
  public static ImmutableRatesProvider providerUsdDscOnL1L3L6() {
    // data from group 1
    return ImmutableRatesProvider.builder(VAL_DATE_2014_01_22)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(USD, GROUP1_USD_DSC)
        .overnightIndexCurve(USD_FED_FUND, GROUP1_USD_ON)
        .iborIndexCurve(USD_LIBOR_1M, GROUP1_USD_L1M)
        .iborIndexCurve(USD_LIBOR_3M, GROUP1_USD_L3M)
        .iborIndexCurve(USD_LIBOR_6M, GROUP1_USD_L6M)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Provides rates for USD and EUR Discounting, Libor 3 month and Euribor 3M.
   * 
   * @return the rates provider
   */
  public static ImmutableRatesProvider providerUsdEurDscL3() {
    // data from group 2
    return ImmutableRatesProvider.builder(VAL_DATE_2014_01_22)
        .fxRateProvider(FX_MATRIX_EUR_USD)
        .discountCurve(EUR, GROUP2_EUR_DSC)
        .discountCurve(USD, GROUP2_USD_DSC)
        .overnightIndexCurve(EUR_EONIA, GROUP2_EUR_ON)
        .iborIndexCurve(EUR_EURIBOR_3M, GROUP2_EUR_L3M)
        .overnightIndexCurve(USD_FED_FUND, GROUP2_USD_ON)
        .iborIndexCurve(USD_LIBOR_3M, GROUP2_USD_L3M)
        .build();
  }

}

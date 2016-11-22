/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test {@link ProductLinearCurveInterpolator}.
 */
@Test
public class ProductLinearCurveInterpolatorTest {

  private static final CurveInterpolator INTERP = ProductLinearCurveInterpolator.INSTANCE;
  private static final CurveInterpolator BASE_INTERP = LinearCurveInterpolator.INSTANCE;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  private static final ScalarFirstOrderDifferentiator DIFF_CALC =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator SENS_CALC =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);

  public void positiveDataTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 1.0, 2.5, 4.2, 10.0, 15.0, 30.0);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray pValues = DoubleArray.of(nData, i -> xValues.get(i) * yValues.get(i));
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(Double x) {
        return x >= xValues.get(0) && x <= xValues.get(nData - 1);
      }
    };
    DoubleArray keys = DoubleArray.of(xValues.get(0), 0.7, 1.2, 7.8, 9.99, 17.52, 25.0, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, pValues);
    Function<Double, Double> funcDeriv = x -> bound.interpolate(x);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)) / keys.get(i), TOL);
      // first derivative
      double firstExp = DIFF_CALC.differentiate(funcDeriv, domain).apply(keys.get(i));
      assertEquals(bound.firstDerivative(keys.get(i)), firstExp, EPS);
      // parameter sensitivity
      int index = i;
      Function<DoubleArray, Double> funcSensi = x -> INTERP.bind(xValues, x).interpolate(keys.get(index));
      DoubleArray sensExp = SENS_CALC.differentiate(funcSensi).apply(yValues);
      assertTrue(DoubleArrayMath.fuzzyEquals(bound.parameterSensitivity(keys.get(i)).toArray(), sensExp.toArray(), EPS));
    }
  }

  public void negativeDataTest() {
    DoubleArray xValues = DoubleArray.of(-34.5, -27.0, -22.5, -14.2, -10.0, -5.0, -0.3);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray pValues = DoubleArray.of(nData, i -> xValues.get(i) * yValues.get(i));
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(Double x) {
        return x >= xValues.get(0) && x <= xValues.get(nData - 1);
      }
    };
    DoubleArray keys = DoubleArray.of(xValues.get(0), -27.7, -21.2, -17.8, -9.99, -1.52, -0.35, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, pValues);
    Function<Double, Double> funcDeriv = x -> bound.interpolate(x);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)) / keys.get(i), TOL);
      // first derivative
      double firstExp = DIFF_CALC.differentiate(funcDeriv, domain).apply(keys.get(i));
      assertEquals(bound.firstDerivative(keys.get(i)), firstExp, EPS);
      // parameter sensitivity
      int index = i;
      Function<DoubleArray, Double> funcSensi = x -> INTERP.bind(xValues, x).interpolate(keys.get(index));
      DoubleArray sensExp = SENS_CALC.differentiate(funcSensi).apply(yValues);
      assertTrue(DoubleArrayMath.fuzzyEquals(bound.parameterSensitivity(keys.get(i)).toArray(), sensExp.toArray(), EPS));
    }
  }

  // regression to ISDA curve
  public void curveRegressionTest() {
    double[] xValues = new double[] {0.08767123287671233, 0.1726027397260274, 0.2602739726027397, 0.5095890410958904,
        1.010958904109589, 2.010958904109589, 3.0136986301369864, 4.0191780821917815, 5.016438356164384, 6.013698630136987,
        7.016438356164384, 8.016438356164384, 9.016438356164384, 10.021917808219179, 12.01917808219178, 15.027397260273974,
        20.024657534246575, 25.027397260273972, 30.030136986301372};
    double[] yValues = new double[] {0.0015967771993938666, 0.002000101499768777, 0.002363431670279865, 0.003338175293899776,
        0.005634608399714134, 0.00440326902435394, 0.007809961130263494, 0.011941089607974827, 0.015908558015433557,
        0.019426790989545677, 0.022365655212981644, 0.02480329609280203, 0.02681632723967965, 0.028566047406753222,
        0.031343018999443514, 0.03409375145707815, 0.036451406286344155, 0.0374228389649933, 0.037841116301420584};
    CurveExtrapolator left = CurveExtrapolators.FLAT;
    CurveExtrapolator right = CurveExtrapolators.PRODUCT_LINEAR;
    BoundCurveInterpolator interp = INTERP.bind(DoubleArray.ofUnsafe(xValues), DoubleArray.ofUnsafe(yValues), left, right);
    double[] keys = new double[] {1d / 365d, 17d / 365d, 30d / 365d, 98d / 365d, 1d, 2.35, 6d, 10d, 12.6, 28d, 32d, 39d};
    // interpolate
    double[] expected = new double[] {0.0015967771993938666, 0.0015967771993938666, 0.0015967771993938666, 0.0024244214596922794,
        0.005609029445739646, 0.005880433874305883, 0.01938638577242741, 0.02853165777399691, 0.031976443846372445,
        0.03768939749254428, 0.037969928846136244, 0.038322391316033835};
    for (int i = 0; i < keys.length; ++i) {
      double computed = interp.interpolate(keys[i]);
      assertEquals(computed, expected[i], EPS);
    }
    // sensitivity
    double[][] sensiExp = new double[][] {
        {1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {1.0, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0.9374299170217537, 0.0625700829782464, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0.011138558275319966, 0.98886144172468, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0.5663932037211347, 0.43360679627886545, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.011484520046164301, 0.9885154799538357, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.01965436153932362, 0.9803456384606765, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.7697250253579322, 0.23027497464206784, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.3627229965940976, 0.6372770034059024},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, -0.3079596561838882, 1.3079596561838882},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, -1.1506122199305884, 2.1506122199305886}};
    for (int i = 0; i < keys.length; ++i) {
      DoubleArray computed = interp.parameterSensitivity(keys[i]);
      assertTrue(DoubleArrayMath.fuzzyEquals(computed.toArray(), sensiExp[i], EPS));
    }
    // fwd rate
    double[] fwdExp = new double[] {0.0015967771993938666, 0.0015967771993938666, 0.0015967771993938666, 0.004355764791085397,
        0.007968687949886104, 0.01464196114594003, 0.037124276087345934, 0.04425631735181895, 0.04508415518352911,
        0.03993364832127998, 0.03993364832127998, 0.03993364832127998};
    for (int i = 0; i < keys.length; ++i) {
      double value = interp.interpolate(keys[i]);
      double deriv = interp.firstDerivative(keys[i]);
      double computed = deriv * keys[i] + value;
      assertEquals(computed, fwdExp[i], EPS);
    }
  }

  // regression to ISDA curve
  public void curveNegativeRateRegressionTest() {
    double[] xValues = new double[] {0.09589041095890412, 0.1726027397260274, 0.2547945205479452, 0.5041095890410959,
        0.7561643835616438, 1.0082191780821919, 2.0136986301369864, 3.0109589041095894, 4.008219178082192, 5.010958904109589,
        6.010958904109589, 7.010958904109589, 8.016438356164384, 9.013698630136986, 10.013698630136986, 12.013698630136986,
        15.016438356164384, 20.021917808219175, 30.032876712328765};
    double[] yValues = new double[] {-0.0020786675364765166, -0.0016860241245632032, -0.0013445488774423426,
        -4.237821212705129E-4, 2.5198253623336676E-5, 5.935456094577134E-4, -3.2426565772714425E-4, 6.147334333200949E-4,
        0.0019060366773708986, 0.0033107384678633615, 0.004774430364382846, 0.006237401212672876, 0.007639615209817064,
        0.00896830709117619, 0.010164859928720184, 0.012196812821300893, 0.014410766977011871, 0.01623618497051232,
        0.016522578536714926};
    CurveExtrapolator left = CurveExtrapolators.FLAT;
    CurveExtrapolator right = CurveExtrapolators.PRODUCT_LINEAR;
    BoundCurveInterpolator interp = INTERP.bind(DoubleArray.ofUnsafe(xValues), DoubleArray.ofUnsafe(yValues), left, right);
    double[] keys = new double[] {1d / 365d, 17d / 365d, 30d / 365d, 98d / 365d, 1d, 2.35, 6d, 10d, 12.6, 28d, 32d, 39d};
    // interpolate
    double[] expected = new double[] {-0.0020786675364765166, -0.0020786675364765166, -0.0020786675364765166,
        -0.0012495606047715551, 5.795315650672766E-4, 8.145045403416393E-5, 0.004761034017457297, 0.010150085453826136,
        0.01271200378309255, 0.016480992621622493, 0.016557789252559695, 0.016654277327326956};
    for (int i = 0; i < keys.length; ++i) {
      double computed = interp.interpolate(keys[i]);
      assertEquals(computed, expected[i], EPS);
    }
    // sensitivity
    double[][] sensiExp = new double[][] {
        {1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0.8968378560215295, 0.1031621439784705, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0.024657534246575567, 0.9753424657534244, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0.5679270452660136, 0.43207295473398644, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.009152436354538397, 0.9908475636454616, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.012347532370050315, 0.9876524676299496, 0d, 0d, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.7672982701729827, 0.23270172982701726, 0d, 0.0},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.1452054794520546, 0.8547945205479454},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, -0.12294520547945219, 1.1229452054794522},
        {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, -0.4598524762908325, 1.4598524762908325}};
    for (int i = 0; i < keys.length; ++i) {
      DoubleArray computed = interp.parameterSensitivity(keys[i]);
      assertTrue(DoubleArrayMath.fuzzyEquals(computed.toArray(), sensiExp[i], EPS));
    }
    // fwd rate
    double[] fwdExp = new double[] {-0.0020786675364765166, -0.0020786675364765166, -0.0020786675364765166,
        5.172212669050933E-4, 0.0022985876769608432, 0.0025107892902424037, 0.012108930306120206, 0.02095022660137702,
        0.023268603631019753, 0.017095365669120136, 0.017095365669120136, 0.017095365669120136};
    for (int i = 0; i < keys.length; ++i) {
      double value = interp.interpolate(keys[i]);
      double deriv = interp.firstDerivative(keys[i]);
      double computed = deriv * keys[i] + value;
      assertEquals(computed, fwdExp[i], EPS);
    }
  }

  public void smallKeyTest() {
    DoubleArray xValues = DoubleArray.of(1e-13, 3e-8, 2e-5);
    DoubleArray yValues = DoubleArray.of(1.0, 13.2, 1.5);
    double keyDw = 1.0e-12;
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    assertThrowsIllegalArg(() -> bound.interpolate(keyDw));
    assertThrowsIllegalArg(() -> bound.firstDerivative(keyDw));
    assertThrowsIllegalArg(() -> bound.parameterSensitivity(keyDw));
  }

  public void getterTest() {
    assertEquals(INTERP.getName(), ProductLinearCurveInterpolator.NAME);
    assertEquals(INTERP.toString(), ProductLinearCurveInterpolator.NAME);
  }

  public void test_serialization() {
    assertSerialization(INTERP);
  }

}

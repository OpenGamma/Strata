/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static com.opengamma.strata.math.impl.util.AssertMatrix.assertEqualsMatrix;
import static com.opengamma.strata.math.impl.util.AssertMatrix.assertEqualsVectors;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.IdentityMatrix;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * 
 */
@Test
public class InterpolatedCurveVectorFunctionTest {

  private static final VectorFieldFirstOrderDifferentiator DIFF = new VectorFieldFirstOrderDifferentiator(1e-4);

  public void test() {
    final double[] knots = new double[] {-1, 0, 0.5, 1.5, 3.0};
    Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);

    //sample at the knots 
    InterpolatedCurveVectorFunction vf = new InterpolatedCurveVectorFunction(knots, interpolator, knots);
    DoubleMatrix1D x = DoubleMatrix1D.of(knots.length, i -> Math.sin(knots[i]));

    DoubleMatrix1D y = vf.evaluate(x);
    DoubleMatrix2D jac = vf.calculateJacobian(x);
    assertEqualsVectors(x, y, 1e-15);
    assertEqualsMatrix(new IdentityMatrix(x.size()), jac, 1e-15);

    double[] samplePoints = new double[] {-2, -1, 0, 1, 2, 3, 4};
    vf = new InterpolatedCurveVectorFunction(samplePoints, interpolator, knots);
    y = vf.evaluate(x);
    jac = vf.calculateJacobian(x);

    DoubleMatrix2D jacFD = DIFF.differentiate(vf).evaluate(x);
    assertEqualsMatrix(jac, jacFD, 5e-5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void badKnotsTest() {
    double[] knots = new double[] {-1, 0, -0.5, 1.5, 3.0};
    Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    @SuppressWarnings("unused")
    InterpolatedCurveVectorFunction vf = new InterpolatedCurveVectorFunction(knots, interpolator, knots);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void badKnotsInterpolatedVectorFunctionProviderTest() {
    double[] knots = new double[] {-1, 0, -0.5, 1.5, 3.0};
    Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    @SuppressWarnings("unused")
    InterpolatedVectorFunctionProvider pro = new InterpolatedVectorFunctionProvider(interpolator, knots);
  }

  public void providerTest() {
    double[] knots = new double[] {-1, 0, 0.5, 1.5, 3.0};
    Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);

    InterpolatedVectorFunctionProvider pro = new InterpolatedVectorFunctionProvider(interpolator, knots);
    double[] samplePoints = new double[] {-3, -0.5, 0.5, 1.3, 2.7};

    VectorFunction vf1 = pro.from(samplePoints);
    VectorFunction vf2 = new InterpolatedCurveVectorFunction(samplePoints, interpolator, knots);

    DoubleMatrix1D knotValues = DoubleMatrix1D.of(-1.0, 1.0, -1.0, 1.0, -1.0);
    DoubleMatrix1D y1 = vf1.evaluate(knotValues);
    DoubleMatrix1D y2 = vf2.evaluate(knotValues);
    AssertMatrix.assertEqualsVectors(y1, y2, 1e-13);

    assertEquals(interpolator, pro.getInterpolator());
    double[] knots2 = pro.getKnots();
    int n = knots.length;
    for (int i = 0; i < n; i++) {
      assertEquals(knots[i], knots2[i], 1e-14);
    }
  }

  public void concatTest() {
    double[] samplePoints = new double[] {-2, -1, 0.7, 1, 2, 3, 4};
    double[] knots1 = new double[] {-1, 0, 0.5, 1.5, 3.0};
    Interpolator1D interpolator1 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    Interpolator1D interpolator2 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.LINEAR, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    double[] knots2 = new double[] {1, 2.1, 3, 4};
    VectorFunction vf1 = new InterpolatedCurveVectorFunction(samplePoints, interpolator1, knots1);
    VectorFunction vf2 = new InterpolatedCurveVectorFunction(samplePoints, interpolator2, knots2);

    /**
     * This takes no arguments, i.e. it represents a fixed curve that we are sampling 
     */
    VectorFunction vf3 = new VectorFunction() {
      DoubleMatrix1D _y = DoubleMatrix1D.of(samplePoints.length, i -> Math.sin(samplePoints[i]));

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        return _y;
      }

      @Override
      public DoubleMatrix2D calculateJacobian(DoubleMatrix1D x) {
        return DoubleMatrix2D.EMPTY;
      }

      @Override
      public int getLengthOfDomain() {
        return 0;
      }

      @Override
      public int getLengthOfRange() {
        return samplePoints.length;
      }
    };

    VectorFunction vf = new ConcatenatedVectorFunction(new VectorFunction[] {vf1, vf3, vf2});
    int nKnots1 = knots1.length;
    DoubleMatrix1D x1 = DoubleMatrix1D.of(nKnots1, i -> Math.sin(knots1[i]));
    int nKnots2 = knots2.length;
    DoubleMatrix1D x2 = DoubleMatrix1D.of(nKnots2, i -> Math.sin(knots2[i]));
    int nKnots = nKnots1 + nKnots2;
    DoubleMatrix1D x = x1.concat(x2);
    DoubleMatrix1D y = vf.evaluate(x);

    assertEquals(samplePoints.length * 3, y.size());
    DoubleMatrix2D jac = vf.calculateJacobian(x);

    assertEquals(samplePoints.length * 3, jac.rowCount());
    assertEquals(nKnots, jac.columnCount());

    DoubleMatrix2D jacFD = DIFF.differentiate(vf).evaluate(x);
    assertEqualsMatrix(jac, jacFD, 1e-4);

  }
}

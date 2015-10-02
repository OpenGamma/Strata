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
    final double[] knots = new double[] {-1, 0, 0.5, 1.5, 3.0 };
    final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);

    //sample at the knots 
    InterpolatedCurveVectorFunction vf = new InterpolatedCurveVectorFunction(knots, interpolator, knots);
    final int nKnots = knots.length;
    final DoubleMatrix1D x = new DoubleMatrix1D(nKnots);
    for (int i = 0; i < nKnots; i++) {
      x.getData()[i] = Math.sin(knots[i]);
    }

    DoubleMatrix1D y = vf.evaluate(x);
    DoubleMatrix2D jac = vf.calculateJacobian(x);
    assertEqualsVectors(x, y, 1e-15);
    assertEqualsMatrix(new IdentityMatrix(x.getNumberOfElements()), jac, 1e-15);

    final double[] samplePoints = new double[] {-2, -1, 0, 1, 2, 3, 4 };
    vf = new InterpolatedCurveVectorFunction(samplePoints, interpolator, knots);
    y = vf.evaluate(x);
    jac = vf.calculateJacobian(x);

    final DoubleMatrix2D jacFD = DIFF.differentiate(vf).evaluate(x);
    assertEqualsMatrix(jac, jacFD, 5e-5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void badKnotsTest() {
    final double[] knots = new double[] {-1, 0, -0.5, 1.5, 3.0 };
    final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    @SuppressWarnings("unused")
    final InterpolatedCurveVectorFunction vf = new InterpolatedCurveVectorFunction(knots, interpolator, knots);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void badKnotsInterpolatedVectorFunctionProviderTest() {
    final double[] knots = new double[] {-1, 0, -0.5, 1.5, 3.0 };
    final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    @SuppressWarnings("unused")
    final InterpolatedVectorFunctionProvider pro = new InterpolatedVectorFunctionProvider(interpolator, knots);
  }

  public void providerTest() {
    final double[] knots = new double[] {-1, 0, 0.5, 1.5, 3.0 };
    final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);

    final InterpolatedVectorFunctionProvider pro = new InterpolatedVectorFunctionProvider(interpolator, knots);
    final double[] samplePoints = new double[] {-3, -0.5, 0.5, 1.3, 2.7 };

    final VectorFunction vf1 = pro.from(samplePoints);
    final VectorFunction vf2 = new InterpolatedCurveVectorFunction(samplePoints, interpolator, knots);

    final DoubleMatrix1D knotValues = new DoubleMatrix1D(-1.0, 1.0, -1.0, 1.0, -1.0);
    final DoubleMatrix1D y1 = vf1.evaluate(knotValues);
    final DoubleMatrix1D y2 = vf2.evaluate(knotValues);
    AssertMatrix.assertEqualsVectors(y1, y2, 1e-13);

    assertEquals(interpolator, pro.getInterpolator());
    final double[] knots2 = pro.getKnots();
    final int n = knots.length;
    for (int i = 0; i < n; i++) {
      assertEquals(knots[i], knots2[i], 1e-14);
    }
  }

  public void concatTest() {
    final double[] samplePoints = new double[] {-2, -1, 0.7, 1, 2, 3, 4 };
    final double[] knots1 = new double[] {-1, 0, 0.5, 1.5, 3.0 };
    final Interpolator1D interpolator1 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    final double[] knots2 = new double[] {1, 2.1, 3, 4 };
    final Interpolator1D interpolator2 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    final VectorFunction vf1 = new InterpolatedCurveVectorFunction(samplePoints, interpolator1, knots1);
    final VectorFunction vf2 = new InterpolatedCurveVectorFunction(samplePoints, interpolator2, knots2);

    /**
     * This takes no arguments, i.e. it represents a fixed curve that we are sampling 
     */
    final VectorFunction vf3 = new VectorFunction() {
      final DoubleMatrix1D _y;
      {
        final double[] res = new double[samplePoints.length];
        for (int i = 0; i < samplePoints.length; i++) {
          res[i] = Math.sin(samplePoints[i]);
        }
        _y = new DoubleMatrix1D(res);
      };

      @Override
      public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
        return _y;
      }

      @Override
      public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
        return DoubleMatrix2D.EMPTY_MATRIX;
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

    final VectorFunction vf = new ConcatenatedVectorFunction(new VectorFunction[] {vf1, vf3, vf2 });
    final int nKnots1 = knots1.length;
    final DoubleMatrix1D x1 = new DoubleMatrix1D(nKnots1);
    for (int i = 0; i < nKnots1; i++) {
      x1.getData()[i] = Math.sin(knots1[i]);
    }
    final int nKnots2 = knots2.length;
    final DoubleMatrix1D x2 = new DoubleMatrix1D(nKnots2);
    for (int i = 0; i < nKnots2; i++) {
      x2.getData()[i] = Math.sin(knots2[i]);
    }
    final int nKnots = nKnots1 + nKnots2;
    final DoubleMatrix1D x = new DoubleMatrix1D(nKnots);
    System.arraycopy(x1.getData(), 0, x.getData(), 0, nKnots1);
    System.arraycopy(x2.getData(), 0, x.getData(), nKnots1, nKnots2);
    final DoubleMatrix1D y = vf.evaluate(x);

    assertEquals(samplePoints.length * 3, y.getNumberOfElements());
    final DoubleMatrix2D jac = vf.calculateJacobian(x);

    assertEquals(samplePoints.length * 3, jac.getNumberOfRows());
    assertEquals(nKnots, jac.getNumberOfColumns());

    final DoubleMatrix2D jacFD = DIFF.differentiate(vf).evaluate(x);
    assertEqualsMatrix(jac, jacFD, 1e-4);

  }
}

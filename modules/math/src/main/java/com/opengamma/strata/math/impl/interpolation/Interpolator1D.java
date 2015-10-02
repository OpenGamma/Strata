/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A base class for interpolation in one dimension.
 */
public abstract class Interpolator1D
    implements Interpolator<Interpolator1DDataBundle, Double>, Serializable {

  private static final long serialVersionUID = 1L;
  private static final double EPS = 1e-6;

  @Override
  public abstract Double interpolate(Interpolator1DDataBundle data, Double value);

  /**
   * Computes the gradient of the interpolant at the value.
   * <p>
   * Note: this is computed by finite difference - this method is expected to be overridden for concrete classes with an analytical calculation
   * @param data Interpolation Data
   * @param value The value for which the gradient is computed
   * @return The gradient
   */
  public double firstDerivative(final Interpolator1DDataBundle data, final Double value) {
    double range = data.lastKey() - data.firstKey();
    Function1D<Double, Boolean> domain = new Function1D<Double, Boolean>() {
      @Override
      public Boolean evaluate(Double x) {
        return x >= data.firstKey() && x <= data.lastKey();
      }
    };

    ScalarFirstOrderDifferentiator diff = new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, range * EPS);
    Function1D<Double, Double> func = getFunction(data);
    Function1D<Double, Double> gradFunc = diff.differentiate(func, domain);
    return gradFunc.evaluate(value);
  }

  /**
   * Generate a 1D function of the interpolant from the interpolator and the data bundle
   * @param data The knots and computed values used by the interpolator
   * @return a 1D function
   */
  public Function1D<Double, Double> getFunction(final Interpolator1DDataBundle data) {
    return new Function1D<Double, Double>() {
      @Override
      public Double evaluate(Double x) {
        return interpolate(data, x);
      }
    };
  }

  /**
   * Generate a 1D function representing the gradient of the interpolant from the interpolator and the data bundle
   * @param data The knots and computed values used by the interpolator
   * @return a 1D function of the gradient
   */
  public Function1D<Double, Double> getGradientFunction(final Interpolator1DDataBundle data) {
    /*
     * Implementation note: It would be more efficient to have the finite difference mechanism (found in firstDerivative)
     * here and have firstDerivative call this rather than the other way round. However firstDerivative is overridden in
     * concrete implementations (with an analytic calculation), which would mean calls to getGradientFunction would be
     * computed by FD even if firstDerivative was over ridden.
     */
    return new Function1D<Double, Double>() {
      @Override
      public Double evaluate(Double x) {
        return firstDerivative(data, x);
      }
    };
  }

  /**
   * Computes the sensitivities of the interpolated value to the input data y.
   * @param data The interpolation data.
   * @param value The value for which the interpolation is computed.
   * @param useFiniteDifferenceSensitivities Use finite difference approximation if true
   * @return The sensitivity.
   */
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final Double value, final boolean useFiniteDifferenceSensitivities) {
    return useFiniteDifferenceSensitivities ? getFiniteDifferenceSensitivities(data, value) : getNodeSensitivitiesForValue(data, value);
  }

  /**
   * Computes the sensitivities of the interpolated value to the input data y by using a methodology defined in a respective subclass
   * @param data The interpolation data.
   * @param value The value for which the interpolation is computed.
   * @return The sensitivity.
   */
  public abstract double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, Double value);

  /**
   * Computes the sensitivities of the interpolated value to the input data y by using central finite difference approximation.
   * @param data The interpolation data.
   * @param value The value for which the interpolation is computed.
   * @return The sensitivity.
   */
  protected double[] getFiniteDifferenceSensitivities(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(data, "data");
    final double[] x = data.getKeys();
    final double[] y = data.getValues();
    final int n = x.length;
    final double[] result = new double[n];
    final Interpolator1DDataBundle dataUp = getDataBundleFromSortedArrays(x, y);
    final Interpolator1DDataBundle dataDown = getDataBundleFromSortedArrays(x, y);

    for (int i = 0; i < n; i++) {
      if (i != 0) {
        dataUp.setYValueAtIndex(i - 1, y[i - 1]);
        dataDown.setYValueAtIndex(i - 1, y[i - 1]);
      }
      dataUp.setYValueAtIndex(i, y[i] + EPS);
      dataDown.setYValueAtIndex(i, y[i] - EPS);
      final double up = interpolate(dataUp, value);
      final double down = interpolate(dataDown, value);
      result[i] = (up - down) / 2 / EPS;
    }
    return result;
  }

  /**
   * Construct Interpolator1DDataBundle from unsorted arrays
   * @param x X values of data
   * @param y Y values of data
   * @return Interpolator1DDataBundle
   */
  public abstract Interpolator1DDataBundle getDataBundle(double[] x, double[] y);

  /**
   * Construct Interpolator1DDataBundle from sorted arrays, i.e, x[0] < x[1] < x[2], .....
   * @param x X values of data
   * @param y Y values of data
   * @return Interpolator1DDataBundle
   */
  public abstract Interpolator1DDataBundle getDataBundleFromSortedArrays(double[] x, double[] y);

  /**
   * Construct Interpolator1DDataBundle from Map
   * @param data Data containing x values and y values
   * @return Interpolator1DDataBundle
   */
  public Interpolator1DDataBundle getDataBundle(final Map<Double, Double> data) {
    ArgChecker.notNull(data, "Backing data for interpolation must not be null.");
    ArgChecker.notEmpty(data, "Backing data for interpolation must not be empty.");
    if (data instanceof SortedMap) {
      final double[] keys = Doubles.toArray(data.keySet());
      final double[] values = Doubles.toArray(data.values());
      return getDataBundleFromSortedArrays(keys, values);
    }
    final double[] keys = new double[data.size()];
    final double[] values = new double[data.size()];
    int i = 0;
    for (final Map.Entry<Double, Double> entry : data.entrySet()) {
      keys[i] = entry.getKey();
      values[i] = entry.getValue();
      i++;
    }
    return getDataBundle(keys, values);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result;
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }

  /**
   * @param o Reference class
   * @return true if two objects are the same class
   */
  protected boolean classEquals(final Object o) {
    if (o == null) {
      return false;
    }
    return getClass().equals(o.getClass());
  }
}

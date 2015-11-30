/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A base class for interpolation in one dimension.
 */
public abstract class Interpolator1D {

  private static final double EPS = 1e-6;

  public abstract double interpolate(Interpolator1DDataBundle data, double value);

  /**
   * Computes the gradient of the interpolant at the value.
   * <p>
   * Note: this is computed by finite difference - this method is expected to be overridden
   * for concrete classes with an analytical calculation.
   * 
   * @param data Interpolation Data
   * @param value The value for which the gradient is computed
   * @return The gradient
   */
  public double firstDerivative(Interpolator1DDataBundle data, double value) {
    double range = data.lastKey() - data.firstKey();
    Function<Double, Boolean> domain = x -> x >= data.firstKey() && x <= data.lastKey();

    ScalarFirstOrderDifferentiator diff = new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, range * EPS);
    Function<Double, Double> func = getFunction(data);
    Function<Double, Double> gradFunc = diff.differentiate(func, domain);
    return gradFunc.apply(value);
  }

  /**
   * Generate a 1D function of the interpolant from the interpolator and the data bundle.
   * 
   * @param data The knots and computed values used by the interpolator
   * @return a 1D function
   */
  public Function<Double, Double> getFunction(Interpolator1DDataBundle data) {
    return new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return interpolate(data, x);
      }
    };
  }

  /**
   * Generate a 1D function representing the gradient of the interpolant from the
   * interpolator and the data bundle.
   * 
   * @param data The knots and computed values used by the interpolator
   * @return a 1D function of the gradient
   */
  public Function<Double, Double> getGradientFunction(Interpolator1DDataBundle data) {
    /*
     * Implementation note: It would be more efficient to have the finite difference mechanism (found in firstDerivative)
     * here and have firstDerivative call this rather than the other way round. However firstDerivative is overridden in
     * concrete implementations (with an analytic calculation), which would mean calls to getGradientFunction would be
     * computed by FD even if firstDerivative was over ridden.
     */
    return new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
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
  public double[] getNodeSensitivitiesForValue(
      Interpolator1DDataBundle data,
      double value,
      boolean useFiniteDifferenceSensitivities) {

    return useFiniteDifferenceSensitivities ?
        getFiniteDifferenceSensitivities(data, value) :
        getNodeSensitivitiesForValue(data, value);
  }

  /**
   * Computes the sensitivities of the interpolated value to the input data y by using a
   * methodology defined in a respective subclass.
   * 
   * @param data The interpolation data.
   * @param value The value for which the interpolation is computed.
   * @return The sensitivity.
   */
  public abstract double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value);

  /**
   * Computes the sensitivities of the interpolated value to the input data y by using
   * central finite difference approximation.
   * 
   * @param data The interpolation data.
   * @param value The value for which the interpolation is computed.
   * @return The sensitivity.
   */
  protected double[] getFiniteDifferenceSensitivities(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(data, "data");
    double[] x = data.getKeys();
    double[] y = data.getValues();
    int n = x.length;
    double[] result = new double[n];
    Interpolator1DDataBundle dataUp = getDataBundleFromSortedArrays(x, y);
    Interpolator1DDataBundle dataDown = getDataBundleFromSortedArrays(x, y);

    for (int i = 0; i < n; i++) {
      if (i != 0) {
        dataUp.setYValueAtIndex(i - 1, y[i - 1]);
        dataDown.setYValueAtIndex(i - 1, y[i - 1]);
      }
      dataUp.setYValueAtIndex(i, y[i] + EPS);
      dataDown.setYValueAtIndex(i, y[i] - EPS);
      double up = interpolate(dataUp, value);
      double down = interpolate(dataDown, value);
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
  public Interpolator1DDataBundle getDataBundle(Map<Double, Double> data) {
    ArgChecker.notNull(data, "Backing data for interpolation must not be null.");
    ArgChecker.notEmpty(data, "Backing data for interpolation must not be empty.");
    if (data instanceof SortedMap) {
      double[] keys = Doubles.toArray(data.keySet());
      double[] values = Doubles.toArray(data.values());
      return getDataBundleFromSortedArrays(keys, values);
    }
    double[] keys = new double[data.size()];
    double[] values = new double[data.size()];
    int i = 0;
    for (Map.Entry<Double, Double> entry : data.entrySet()) {
      keys[i] = entry.getKey();
      values[i] = entry.getValue();
      i++;
    }
    return getDataBundle(keys, values);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
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
  protected boolean classEquals(Object o) {
    if (o == null) {
      return false;
    }
    return getClass().equals(o.getClass());
  }
}

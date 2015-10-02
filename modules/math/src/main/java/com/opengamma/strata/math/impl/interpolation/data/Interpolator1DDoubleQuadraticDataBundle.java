/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Objects;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * 
 */
public class Interpolator1DDoubleQuadraticDataBundle implements Interpolator1DDataBundle {
  private final Interpolator1DDataBundle _underlyingData;
  private RealPolynomialFunction1D[] _quadratics;
  private RealPolynomialFunction1D[] _quadraticsFirstDerivative;

  public Interpolator1DDoubleQuadraticDataBundle(final Interpolator1DDataBundle underlyingData) {
    ArgChecker.notNull(underlyingData, "underlying data");
    _underlyingData = underlyingData;
  }

  private RealPolynomialFunction1D[] getQuadratics() {
    final double[] xData = getKeys();
    final double[] yData = getValues();
    final int n = xData.length - 1;
    if (n == 0) {
      final double a = yData[0];
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(a) };
    } else if (n == 1) {
      final double a = yData[1];
      final double b = (yData[1] - yData[0]) / (xData[1] - xData[0]);
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(a, b) };
    } else {
      final RealPolynomialFunction1D[] quadratic = new RealPolynomialFunction1D[n - 1];
      for (int i = 1; i < n; i++) {
        quadratic[i - 1] = getQuadratic(xData, yData, i);
      }
      return quadratic;
    }
  }

  private RealPolynomialFunction1D[] getQuadraticsFirstDerivative() {
    final double[] xData = getKeys();
    final double[] yData = getValues();
    final int n = xData.length - 1;
    if (n == 0) {
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(0.) };
    } else if (n == 1) {
      final double b = (yData[1] - yData[0]) / (xData[1] - xData[0]);
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(b) };
    } else {
      final RealPolynomialFunction1D[] quadraticFirstDerivative = new RealPolynomialFunction1D[n - 1];
      for (int i = 1; i < n; i++) {
        quadraticFirstDerivative[i - 1] = getQuadraticFirstDerivative(xData, yData, i);
      }
      return quadraticFirstDerivative;
    }
  }

  private RealPolynomialFunction1D getQuadratic(final double[] x, final double[] y, final int index) {
    final double a = y[index];
    final double dx1 = x[index] - x[index - 1];
    final double dx2 = x[index + 1] - x[index];
    final double dy1 = y[index] - y[index - 1];
    final double dy2 = y[index + 1] - y[index];
    final double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
    final double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
    return new RealPolynomialFunction1D(new double[] {a, b, c });
  }

  private RealPolynomialFunction1D getQuadraticFirstDerivative(final double[] x, final double[] y, final int index) {
    final double dx1 = x[index] - x[index - 1];
    final double dx2 = x[index + 1] - x[index];
    final double dy1 = y[index] - y[index - 1];
    final double dy2 = y[index + 1] - y[index];
    final double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
    final double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
    return new RealPolynomialFunction1D(new double[] {b, 2. * c });
  }

  public RealPolynomialFunction1D getQuadratic(final int index) {
    if (_quadratics == null) {
      _quadratics = getQuadratics();
    }
    return _quadratics[index];
  }

  /**
   * @param index The index of the interval
   * @return First derivative of the quadratic function at the index
   */
  public RealPolynomialFunction1D getQuadraticFirstDerivative(final int index) {
    if (_quadraticsFirstDerivative == null) {
      _quadraticsFirstDerivative = getQuadraticsFirstDerivative();
    }
    return _quadraticsFirstDerivative[index];
  }

  @Override
  public boolean containsKey(final Double key) {
    return _underlyingData.containsKey(key);
  }

  @Override
  public Double firstKey() {
    return _underlyingData.firstKey();
  }

  @Override
  public Double firstValue() {
    return _underlyingData.firstValue();
  }

  @Override
  public Double get(final Double key) {
    return _underlyingData.get(key);
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(final Double key) {
    return _underlyingData.getBoundedValues(key);
  }

  @Override
  public double[] getKeys() {
    return _underlyingData.getKeys();
  }

  @Override
  public int getLowerBoundIndex(final Double value) {
    return _underlyingData.getLowerBoundIndex(value);
  }

  @Override
  public Double getLowerBoundKey(final Double value) {
    return _underlyingData.getLowerBoundKey(value);
  }

  @Override
  public double[] getValues() {
    return _underlyingData.getValues();
  }

  @Override
  public Double higherKey(final Double key) {
    return _underlyingData.higherKey(key);
  }

  @Override
  public Double higherValue(final Double key) {
    return _underlyingData.higherValue(key);
  }

  @Override
  public Double lastKey() {
    return _underlyingData.lastKey();
  }

  @Override
  public Double lastValue() {
    return _underlyingData.lastValue();
  }

  @Override
  public int size() {
    return _underlyingData.size();
  }

  @Override
  public void setYValueAtIndex(final int index, final double y) {
    ArgChecker.notNegative(index, "index");
    if (index >= size()) {
      throw new IllegalArgumentException("Index was greater than number of data points");
    }
    _underlyingData.setYValueAtIndex(index, y);
    if (_quadratics == null) {
      _quadratics = getQuadratics();
    }
    final double[] keys = getKeys();
    final double[] values = getValues();
    final int n = size() - 1;
    if (index == 0) {
      _quadratics[0] = getQuadratic(keys, values, 1);
      return;
    } else if (index == 1) {
      _quadratics[0] = getQuadratic(keys, values, 1);
      _quadratics[1] = getQuadratic(keys, values, 2);
      return;
    } else if (index == n) {
      _quadratics[n - 2] = getQuadratic(keys, values, n - 1);
    } else if (index == n - 1) {
      _quadratics[n - 3] = getQuadratic(keys, values, n - 2);
      _quadratics[n - 2] = getQuadratic(keys, values, n - 1);
      return;
    } else {
      _quadratics[index - 2] = getQuadratic(keys, values, index - 1);
      _quadratics[index - 1] = getQuadratic(keys, values, index);
      _quadratics[index] = getQuadratic(keys, values, index + 1);
      return;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_underlyingData == null) ? 0 : _underlyingData.hashCode());
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
    final Interpolator1DDoubleQuadraticDataBundle other = (Interpolator1DDoubleQuadraticDataBundle) obj;
    return Objects.equals(_underlyingData, other._underlyingData);
  }

}

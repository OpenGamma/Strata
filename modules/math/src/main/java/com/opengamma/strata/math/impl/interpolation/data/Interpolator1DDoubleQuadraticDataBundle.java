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
public class Interpolator1DDoubleQuadraticDataBundle
    extends ForwardingInterpolator1DDataBundle {

  private RealPolynomialFunction1D[] _quadratics;
  private RealPolynomialFunction1D[] _quadraticsFirstDerivative;

  public Interpolator1DDoubleQuadraticDataBundle(Interpolator1DDataBundle underlyingData) {
    super(underlyingData);
  }

  private RealPolynomialFunction1D[] getQuadratics() {
    double[] xData = getKeys();
    double[] yData = getValues();
    int n = xData.length - 1;
    if (n == 0) {
      double a = yData[0];
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(a) };
    } else if (n == 1) {
      double a = yData[1];
      double b = (yData[1] - yData[0]) / (xData[1] - xData[0]);
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(a, b) };
    } else {
      RealPolynomialFunction1D[] quadratic = new RealPolynomialFunction1D[n - 1];
      for (int i = 1; i < n; i++) {
        quadratic[i - 1] = getQuadratic(xData, yData, i);
      }
      return quadratic;
    }
  }

  private RealPolynomialFunction1D[] getQuadraticsFirstDerivative() {
    double[] xData = getKeys();
    double[] yData = getValues();
    int n = xData.length - 1;
    if (n == 0) {
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(0.) };
    } else if (n == 1) {
      double b = (yData[1] - yData[0]) / (xData[1] - xData[0]);
      return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(b) };
    } else {
      RealPolynomialFunction1D[] quadraticFirstDerivative = new RealPolynomialFunction1D[n - 1];
      for (int i = 1; i < n; i++) {
        quadraticFirstDerivative[i - 1] = getQuadraticFirstDerivative(xData, yData, i);
      }
      return quadraticFirstDerivative;
    }
  }

  private RealPolynomialFunction1D getQuadratic(double[] x, double[] y, int index) {
    double a = y[index];
    double dx1 = x[index] - x[index - 1];
    double dx2 = x[index + 1] - x[index];
    double dy1 = y[index] - y[index - 1];
    double dy2 = y[index + 1] - y[index];
    double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
    double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
    return new RealPolynomialFunction1D(new double[] {a, b, c });
  }

  private RealPolynomialFunction1D getQuadraticFirstDerivative(double[] x, double[] y, int index) {
    double dx1 = x[index] - x[index - 1];
    double dx2 = x[index + 1] - x[index];
    double dy1 = y[index] - y[index - 1];
    double dy2 = y[index + 1] - y[index];
    double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
    double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
    return new RealPolynomialFunction1D(new double[] {b, 2. * c });
  }

  public RealPolynomialFunction1D getQuadratic(int index) {
    if (_quadratics == null) {
      _quadratics = getQuadratics();
    }
    return _quadratics[index];
  }

  /**
   * @param index The index of the interval
   * @return First derivative of the quadratic function at the index
   */
  public RealPolynomialFunction1D getQuadraticFirstDerivative(int index) {
    if (_quadraticsFirstDerivative == null) {
      _quadraticsFirstDerivative = getQuadraticsFirstDerivative();
    }
    return _quadraticsFirstDerivative[index];
  }

  @Override
  public void setYValueAtIndex(int index, double y) {
    ArgChecker.notNegative(index, "index");
    if (index >= size()) {
      throw new IllegalArgumentException("Index was greater than number of data points");
    }
    getUnderlying().setYValueAtIndex(index, y);
    if (_quadratics == null) {
      _quadratics = getQuadratics();
    }
    double[] keys = getKeys();
    double[] values = getValues();
    int n = size() - 1;
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
    int prime = 31;
    int result = 1;
    result = prime * result + ((getUnderlying() == null) ? 0 : getUnderlying().hashCode());
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
    Interpolator1DDoubleQuadraticDataBundle other = (Interpolator1DDoubleQuadraticDataBundle) obj;
    return Objects.equals(getUnderlying(), other.getUnderlying());
  }

}

/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

/**
 * A complex number.
 */
public class ComplexNumber extends Number {

  /** Defining <i>i</i>*/
  public static final ComplexNumber I = new ComplexNumber(0, 1);
  /** Defining <i>-i</i>*/
  public static final ComplexNumber MINUS_I = new ComplexNumber(0, -1);
  /** Defining 0 + 0<i>i</i> */
  public static final ComplexNumber ZERO = new ComplexNumber(0);

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The real part. */
  private final double _real;
  /** The imaginary part. */
  private final double _imaginary;

  /**
   * Creates an instance from the real part.
   * 
   * @param real  the real part
   */
  public ComplexNumber(double real) {
    _real = real;
    _imaginary = 0.0;
  }

  /**
   * Creates an instance from the real and imaginary parts.
   * 
   * @param real  the real part
   * @param imaginary  the imaginary part
   */
  public ComplexNumber(double real, double imaginary) {
    _real = real;
    _imaginary = imaginary;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the real part.
   * 
   * @return the real part
   */
  public double getReal() {
    return _real;
  }

  /**
   * Gets the imaginary part.
   * 
   * @return the imaginary part
   */
  public double getImaginary() {
    return _imaginary;
  }

  //-------------------------------------------------------------------------
  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public double doubleValue() {
    throw new UnsupportedOperationException("Cannot get the doubleValue of a ComplexNumber");
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public float floatValue() {
    throw new UnsupportedOperationException("Cannot get the floatValue of a ComplexNumber");
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public int intValue() {
    throw new UnsupportedOperationException("Cannot get the intValue of a ComplexNumber");
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public long longValue() {
    throw new UnsupportedOperationException("Cannot get the longValue of a ComplexNumber");
  }

  //-------------------------------------------------------------------------
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
    ComplexNumber other = (ComplexNumber) obj;
    if (Double.doubleToLongBits(_imaginary) != Double.doubleToLongBits(other._imaginary)) {
      return false;
    }
    if (Double.doubleToLongBits(_real) != Double.doubleToLongBits(other._real)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_imaginary);
    result = prime * result + (int) (temp ^ temp >>> 32);
    temp = Double.doubleToLongBits(_real);
    result = prime * result + (int) (temp ^ temp >>> 32);
    return result;
  }

  @Override
  public String toString() {
    boolean negative = _imaginary < 0;
    double abs = Math.abs(_imaginary);
    return Double.toString(_real) + (negative ? " - " : " + ") + Double.toString(abs) + "i";
  }

}

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * The standard immutable curve interpolator implementation based on strata-math.
 */
final class StandardCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;
  /**
   * The underlying interpolator.
   */
  private transient final Interpolator1D underlying;

  /**
   * Creates an instance.
   * 
   * @param name  the name of the interpolator
   * @param underlying  the underlying interpolator
   */
  StandardCurveInterpolator(String name, Interpolator1D underlying) {
    this.name = name;
    this.underlying = underlying;
  }

  // resolve instance using name
  private Object readResolve() {
    return CurveInterpolator.of(name);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name.
   * 
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Gets the underlying interpolator.
   * 
   * @return the underlying interpolator
   */
  Interpolator1D getUnderlying() {
    return underlying;
  }

  //-------------------------------------------------------------------------
  @Override
  public BoundCurveInterpolator bind(DoubleArray xValues, DoubleArray yValues) {
    return new StandardBoundCurveInterpolator(xValues, yValues, underlying);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      StandardCurveInterpolator other = (StandardCurveInterpolator) obj;
      return name.equals(other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

}

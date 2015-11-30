/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.math.impl.interpolation.Extrapolator1D;

/**
 * The standard immutable curve extrapolator implementation based on strata-math.
 */
final class ImmutableCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;
  /**
   * The underlying extrapolator.
   */
  private transient final Extrapolator1D underlying;

  /**
   * Creates an instance.
   * 
   * @param name  the name of the extrapolator
   * @param underlying  the underlying extrapolator
   */
  ImmutableCurveExtrapolator(String name, Extrapolator1D underlying) {
    this.name = name;
    this.underlying = underlying;
  }

  // resolve instance using name
  private Object readResolve() {
    return CurveExtrapolator.of(name);
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
   * Gets the underlying extrapolator.
   * 
   * @return the underlying extrapolator
   */
  Extrapolator1D getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableCurveExtrapolator other = (ImmutableCurveExtrapolator) obj;
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

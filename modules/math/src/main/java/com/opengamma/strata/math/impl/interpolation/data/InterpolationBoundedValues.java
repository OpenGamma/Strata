/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public final class InterpolationBoundedValues implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int _lowerBoundIndex;
  private final Double _lowerBoundKey;
  private final Double _lowerBoundValue;
  private final Double _higherBoundKey;
  private final Double _higherBoundValue;

  public InterpolationBoundedValues(final int lowerBoundIndex, final Double lowerBoundKey, final Double lowerBoundValue, final Double higherKey, final Double higherValue) {
    ArgChecker.notNegative(lowerBoundIndex, "lower bound index");
    _lowerBoundIndex = lowerBoundIndex;
    _lowerBoundKey = lowerBoundKey;
    _lowerBoundValue = lowerBoundValue;
    _higherBoundKey = higherKey;
    _higherBoundValue = higherValue;
  }

  /**
   * @return the _lowerBoundKey
   */
  public Double getLowerBoundKey() {
    return _lowerBoundKey;
  }

  /**
   * @return the _lowerBoundValue
   */
  public Double getLowerBoundValue() {
    return _lowerBoundValue;
  }

  /**
   * @return the higherBoundKey
   */
  public Double getHigherBoundKey() {
    return _higherBoundKey;
  }

  /**
   * @return the higherBoundValue
   */
  public Double getHigherBoundValue() {
    return _higherBoundValue;
  }

  public int getLowerBoundIndex() {
    return _lowerBoundIndex;
  }

}

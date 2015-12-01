/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Forwarding data bundle.
 */
abstract class ForwardingInterpolator1DDataBundle
    implements Interpolator1DDataBundle {

  private final Interpolator1DDataBundle underlying;

  ForwardingInterpolator1DDataBundle(Interpolator1DDataBundle underlying) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
  }

  Interpolator1DDataBundle getUnderlying() {
    return underlying;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsKey(double key) {
    return underlying.containsKey(key);
  }

  @Override
  public int indexOf(double key) {
    return underlying.indexOf(key);
  }

  @Override
  public double firstKey() {
    return underlying.firstKey();
  }

  @Override
  public double firstValue() {
    return underlying.firstValue();
  }

  @Override
  public double getIndex(int index) {
    return underlying.getIndex(index);
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(double key) {
    return underlying.getBoundedValues(key);
  }

  @Override
  public double[] getKeys() {
    return underlying.getKeys();
  }

  @Override
  public int getLowerBoundIndex(double value) {
    return underlying.getLowerBoundIndex(value);
  }

  @Override
  public double getLowerBoundKey(double value) {
    return underlying.getLowerBoundKey(value);
  }

  @Override
  public double getLowerBoundValue(double value) {
    return underlying.getLowerBoundValue(value);
  }

  @Override
  public double[] getValues() {
    return underlying.getValues();
  }

  @Override
  public double higherKey(double key) {
    return underlying.higherKey(key);
  }

  @Override
  public double higherValue(double key) {
    return underlying.higherValue(key);
  }

  @Override
  public double lastKey() {
    return underlying.lastKey();
  }

  @Override
  public double lastValue() {
    return underlying.lastValue();
  }

  @Override
  public int size() {
    return underlying.size();
  }

}

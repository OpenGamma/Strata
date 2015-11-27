/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * A pre-constructed version of the data which should be used
 * for interpolation on an {@link Interpolator1D}.
 * 
 */
public interface Interpolator1DDataBundle {

  int getLowerBoundIndex(double value);

  double getLowerBoundKey(double value);

  double getLowerBoundValue(double value);

  int indexOf(double key);

  double getIndex(int index);

  double firstKey();

  double lastKey();

  double firstValue();

  double lastValue();

  double higherKey(double key);

  double higherValue(double key);

  InterpolationBoundedValues getBoundedValues(double key);

  boolean containsKey(double key);

  int size();

  double[] getKeys();

  double[] getValues();

  void setYValueAtIndex(int index, double y);

}

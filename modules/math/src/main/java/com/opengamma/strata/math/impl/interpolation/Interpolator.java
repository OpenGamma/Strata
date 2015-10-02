/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

/**
 * @param <S> The type of the data
 * @param <T> The type of the value to interpolate
 */

public interface Interpolator<S, T> {

  Double interpolate(S data, T value);

}

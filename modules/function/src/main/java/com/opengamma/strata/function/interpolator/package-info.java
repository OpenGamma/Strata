/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Interpolators for interpolating in one and two dimensions.
 * <p>
 * One dimensional interpolators are used during curve calibration. The built in set of interpolators are
 * defined as an extended enum in {@link com.opengamma.strata.function.interpolator.OneDimensionalInterpolators}.
 * This allows them to be referenced statically like a constant but also allows them to be redefined and new
 * instances added.
 * <p>
 * Currently the one dimensional interpolators are all implementations of
 * {@link com.opengamma.analytics.math.interpolation.Interpolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and is likely to change.
 */
package com.opengamma.strata.function.interpolator;

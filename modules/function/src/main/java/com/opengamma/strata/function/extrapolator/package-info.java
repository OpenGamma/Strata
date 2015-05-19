/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Extrapolators for extrapolating in one and two dimensions.
 * <p>
 * Curve extrapolators are used during curve calibration. The built in set of extrapolators are
 * defined as an extended enum in {@link com.opengamma.strata.function.interpolator.CurveExtrapolators}.
 * This allows them to be referenced statically like a constant but also allows them to be redefined and new
 * instances added.
 * <p>
 * Currently the curve extrapolators are all implementations of
 * {@link com.opengamma.analytics.math.interpolation.Extrapolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and is likely to change.
 */
package com.opengamma.strata.function.extrapolator;

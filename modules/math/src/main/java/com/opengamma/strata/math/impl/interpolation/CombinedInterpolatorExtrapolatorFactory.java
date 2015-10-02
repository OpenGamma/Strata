/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;

import com.google.common.collect.ImmutableList;

/**
 * 
 */
public final class CombinedInterpolatorExtrapolatorFactory {

  // Cannot use Named::getName method reference in Eclipse
  private static final Map<String, Extrapolator1D> EXTRAPOLATORS =
      ImmutableList.of(
          new LinearExtrapolator1D(),
          new LogLinearExtrapolator1D(),
          new QuadraticPolynomialLeftExtrapolator(),
          new FlatExtrapolator1D(),
          new ExponentialExtrapolator1D(),
          new InterpolatorExtrapolator())
          .stream()
          .collect(toImmutableMap(xtr -> xtr.getName(), xtr -> xtr));

  private CombinedInterpolatorExtrapolatorFactory() {
  }

  public static CombinedInterpolatorExtrapolator getInterpolator(String interpolatorName) {
    Interpolator1D interpolator = Interpolator1DFactory.getInterpolator(interpolatorName);
    return new CombinedInterpolatorExtrapolator(interpolator);
  }

  public static CombinedInterpolatorExtrapolator getInterpolator(String interpolatorName, String extrapolatorName) {
    Interpolator1D interpolator = Interpolator1DFactory.getInterpolator(interpolatorName);
    if (extrapolatorName == null || extrapolatorName.isEmpty()) {
      return new CombinedInterpolatorExtrapolator(interpolator);
    }
    Extrapolator1D extrapolator = getExtrapolator(extrapolatorName);
    return new CombinedInterpolatorExtrapolator(interpolator, extrapolator, extrapolator);
  }

  public static CombinedInterpolatorExtrapolator getInterpolator(
      String interpolatorName,
      String leftExtrapolatorName,
      String rightExtrapolatorName) {

    Interpolator1D interpolator = Interpolator1DFactory.getInterpolator(interpolatorName);
    if (leftExtrapolatorName == null || leftExtrapolatorName.isEmpty()) {
      if (rightExtrapolatorName == null || rightExtrapolatorName.isEmpty()) {
        return new CombinedInterpolatorExtrapolator(interpolator);
      }
      Extrapolator1D extrapolator = getExtrapolator(rightExtrapolatorName);
      return new CombinedInterpolatorExtrapolator(interpolator, extrapolator);
    }
    if (rightExtrapolatorName == null || rightExtrapolatorName.isEmpty()) {
      Extrapolator1D extrapolator = getExtrapolator(leftExtrapolatorName);
      return new CombinedInterpolatorExtrapolator(interpolator, extrapolator);
    }
    Extrapolator1D leftExtrapolator = getExtrapolator(leftExtrapolatorName);
    Extrapolator1D rightExtrapolator = getExtrapolator(rightExtrapolatorName);
    return new CombinedInterpolatorExtrapolator(interpolator, leftExtrapolator, rightExtrapolator);
  }

  public static Extrapolator1D getExtrapolator(String extrapolatorName) {
    Extrapolator1D extrapolator = EXTRAPOLATORS.get(extrapolatorName);

    if (extrapolator != null) {
      return extrapolator;
    } else {
      throw new IllegalArgumentException("Unknown extrapolator name " + extrapolatorName);
    }
  }
}

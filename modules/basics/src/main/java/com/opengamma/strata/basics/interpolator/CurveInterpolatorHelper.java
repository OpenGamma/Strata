/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.interpolator;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * A helper for {@link CurveInterpolator} holding the {@link ExtendedEnum} used to look up instances.
 */
final class CurveInterpolatorHelper {

  /** The extended enum used for looking up instances of {@code CurveInterpolator}. */
  static final ExtendedEnum<CurveInterpolator> ENUM_LOOKUP = ExtendedEnum.of(CurveInterpolator.class);

  // Private constructor, this class only exists to hold ENUM_LOOKUP
  private CurveInterpolatorHelper() {
  }
}

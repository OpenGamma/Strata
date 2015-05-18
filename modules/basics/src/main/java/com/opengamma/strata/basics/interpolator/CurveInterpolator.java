/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.interpolator;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * Interface for interpolators that interpolate between points on a curve.
 * <p>
 * This is currently a marker interface implemented by legacy interpolator implementations. At some point it will
 * be expanded to include interpolation operations.
 */
public interface CurveInterpolator extends Named {

  /**
   * Obtains a {@code CurveInterpolator} from a unique name.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CurveInterpolator of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code CurveInterpolator} to be lookup up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<CurveInterpolator> extendedEnum() {
    return CurveInterpolatorHelper.ENUM_LOOKUP;
  }
}

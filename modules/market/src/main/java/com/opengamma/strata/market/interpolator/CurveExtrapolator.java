/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * Interface for extrapolators which extrapolate beyond the ends of a curve.
 */
public interface CurveExtrapolator extends Named {

  /**
   * Obtains a {@code CurveExtrapolator} from a unique name.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CurveExtrapolator of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code CurveExtrapolator} to be lookup up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<CurveExtrapolator> extendedEnum() {
    return CurveExtrapolators.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this extrapolator.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}

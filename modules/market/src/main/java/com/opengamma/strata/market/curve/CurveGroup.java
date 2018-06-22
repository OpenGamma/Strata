/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A group of curves.
 * <p>
 * This is used to hold a group of related curves, typically forming a logical set.
 * It is often used to hold the results of a curve calibration.
 * <p>
 * Curve groups can also be created from a set of existing curves.
 * <p>
 * In Strata v2, this type was converted to an interface.
 * If migrating, change your code to {@link RatesCurveGroup}.
 */
public interface CurveGroup {

  /**
   * Gets the name of the curve group.
   * 
   * @return the group name
   */
  public abstract CurveGroupName getName();

  /**
   * Finds the curve with the specified name.
   * <p>
   * If the curve cannot be found, empty is returned.
   * 
   * @param name  the curve name
   * @return the curve, empty if not found
   */
  public abstract Optional<Curve> findCurve(CurveName name);

  /**
   * Returns a stream of all curves in the group.
   *
   * @return a stream of all curves in the group
   */
  public abstract Stream<Curve> stream();

}

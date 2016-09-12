/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The name of a curve group.
 */
public final class CurveGroupName
    extends TypedString<CurveGroupName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Curve group names may contain any character, but must not be empty.
   *
   * @param name  the name of the curve group
   * @return a curve group name
   */
  @FromString
  public static CurveGroupName of(String name) {
    return new CurveGroupName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name  the name of the curve group
   */
  private CurveGroupName(String name) {
    super(name);
  }

}

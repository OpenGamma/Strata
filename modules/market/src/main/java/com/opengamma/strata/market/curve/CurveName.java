/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The name of a curve.
 */
public final class CurveName
    extends TypedString<CurveName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Curve names may contain any character, but must not be empty.
   *
   * @param name  the name of the curve
   * @return a curve with the specified name
   */
  @FromString
  public static CurveName of(String name) {
    return new CurveName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the curve
   */
  private CurveName(String name) {
    super(name);
  }

}

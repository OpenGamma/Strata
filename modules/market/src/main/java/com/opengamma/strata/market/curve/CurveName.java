/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a curve.
 */
public final class CurveName
    extends MarketDataName<Curve>
    implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;

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
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<Curve> getMarketDataType() {
    return Curve.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

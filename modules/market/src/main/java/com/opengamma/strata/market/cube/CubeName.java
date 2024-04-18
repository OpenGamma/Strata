/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a cube.
 */
public class CubeName
    extends MarketDataName<Cube>
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
   * Cube names may contain any character, but must not be empty.
   *
   * @param name  the name of the cube
   * @return a cube name instance with the specified name
   */
  @FromString
  public static CubeName of(String name) {
    return new CubeName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name  the name of the cube
   */
  private CubeName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<Cube> getMarketDataType() {
    return Cube.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

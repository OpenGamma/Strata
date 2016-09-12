/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a surface.
 */
public final class SurfaceName
    extends MarketDataName<Surface>
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
   * Surface names may contain any character, but must not be empty.
   *
   * @param name  the name of the surface
   * @return a surface with the specified name
   */
  @FromString
  public static SurfaceName of(String name) {
    return new SurfaceName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the surface
   */
  private SurfaceName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<Surface> getMarketDataType() {
    return Surface.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a set of swaption volatilities.
 */
public final class SwaptionVolatilitiesName
    extends MarketDataName<SwaptionVolatilities>
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
   * Names may contain any character, but must not be empty.
   *
   * @param name  the name
   * @return the name instance
   */
  @FromString
  public static SwaptionVolatilitiesName of(String name) {
    return new SwaptionVolatilitiesName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private SwaptionVolatilitiesName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<SwaptionVolatilities> getMarketDataType() {
    return SwaptionVolatilities.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

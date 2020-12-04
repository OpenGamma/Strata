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
 * The name of a set of swaption SABR volatilities.
 */
public final class SabrSwaptionVolatilitiesName
    extends MarketDataName<SabrSwaptionVolatilities>
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
  public static SabrSwaptionVolatilitiesName of(String name) {
    return new SabrSwaptionVolatilitiesName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name  the name
   */
  private SabrSwaptionVolatilitiesName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<SabrSwaptionVolatilities> getMarketDataType() {
    return SabrSwaptionVolatilities.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

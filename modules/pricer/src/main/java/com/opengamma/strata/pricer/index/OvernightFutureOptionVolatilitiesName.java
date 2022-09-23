/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a set of overnight future option volatilities.
 */
public final class OvernightFutureOptionVolatilitiesName
    extends MarketDataName<OvernightFutureOptionVolatilities>
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
  public static OvernightFutureOptionVolatilitiesName of(String name) {
    return new OvernightFutureOptionVolatilitiesName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private OvernightFutureOptionVolatilitiesName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<OvernightFutureOptionVolatilities> getMarketDataType() {
    return OvernightFutureOptionVolatilities.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

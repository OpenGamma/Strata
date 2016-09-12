/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.io.Serializable;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;

/**
 * The name of a set of Ibor future option volatilities.
 */
public final class IborFutureOptionVolatilitiesName
    extends MarketDataName<IborFutureOptionVolatilities>
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
  public static IborFutureOptionVolatilitiesName of(String name) {
    return new IborFutureOptionVolatilitiesName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private IborFutureOptionVolatilitiesName(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<IborFutureOptionVolatilities> getMarketDataType() {
    return IborFutureOptionVolatilities.class;
  }

  @Override
  public String getName() {
    return name;
  }

}

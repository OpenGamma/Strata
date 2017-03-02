/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * An identifier for an exchange based on the ISO Market Identifier Code (MIC).
 *
 * @see ExchangeIds
 */
public final class ExchangeId implements Named {

  /**
   * The Market Identifier Code (MIC) identifying the exchange.
   */
  private final String name;

  private ExchangeId(String name) {
    this.name = ArgChecker.notBlank(name, "name");
  }

  /**
   * Returns the Market Identifier Code (MIC) identifying the exchange.
   *
   * @return the Market Identifier Code (MIC) identifying the exchange
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns an identifier for an exchange.
   *
   * @param name the Market Identifier Code (MIC) identifying the exchange
   * @return an identifier for an exchange
   */
  @FromString
  public static ExchangeId of(String name) {
    return new ExchangeId(name);
  }

  @ToString
  @Override
  public String toString() {
    return name;
  }
}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import java.io.Serializable;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * An identifier for an exchange based on the ISO Market Identifier Code (MIC).
 * <p>
 * Identifiers for common exchanges are provided in {@link ExchangeIds}.
 */
public final class ExchangeId implements Named, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
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

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   * <p>
   * The comparison checks the name.
   * 
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExchangeId that = (ExchangeId) obj;
    return name.equals(that.name);
  }

  /**
   * Returns a suitable hash code for the identifier.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @ToString
  @Override
  public String toString() {
    return name;
  }

}

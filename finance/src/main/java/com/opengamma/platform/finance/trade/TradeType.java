/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade;

import java.io.Serializable;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * The type of a trade.
 */
public final class TradeType
    implements Comparable<TradeType>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;

  /**
   * Obtains a {@code TradeType}.
   * 
   * @param name  the trade type name, not null
   * @return the trade type, not null
   */
  @FromString
  public static TradeType of(String name) {
    return new TradeType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  private TradeType(String name) {
    this.name = ArgChecker.notNull(name, "name");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name of the trade type.
   * 
   * @return the name, not null
   */
  public String getName() {
    return name;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this type to another.
   * <p>
   * Instances are compared in alphabetical order based on the name.
   * 
   * @param other  the object to compare to, not null
   * @return the comparison
   */
  @Override
  public int compareTo(TradeType other) {
    return getName().compareTo(other.getName());
  }

  /**
   * Checks if this type equals another.
   * <p>
   * Instances are compared based on the name.
   * 
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof TradeType) {
      TradeType other = (TradeType) obj;
      return name.equals(other.getName());
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return a suitable hash code
   */
  @Override
  public int hashCode() {
    return name.hashCode() + 31;
  }

  /**
   * Returns the name.
   * 
   * @return the string form, not null
   */
  @Override
  @ToString
  public String toString() {
    return name;
  }

}

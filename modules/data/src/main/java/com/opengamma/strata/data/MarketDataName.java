/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.collect.named.Named;

/**
 * A name for an item of market data.
 * <p>
 * The name is used to locate an item in market data.
 * While a {@link MarketDataId} is unique within a system, a {@link MarketDataName} is not.
 * However, it is intended to be unique within any single coherent data set.
 * <p>
 * For example, a curve group contains a set of curves, and within the group the name is unique.
 * But the market data system may contain many curve groups where the same name appears in each group.
 * The {@code MarketDataId} includes both the group name and curve name in order to ensure uniqueness.
 *
 * @param <T>  the type of the market data this identifier refers to
 */
public abstract class MarketDataName<T>
    implements Named, Comparable<MarketDataName<?>> {

  /**
   * Gets the market data name.
   * <p>
   * The name must be unique within any single coherent data set.
   * 
   * @return the unique name
   */
  @Override
  public abstract String getName();

  /**
   * Gets the type of data this name refers to.
   *
   * @return the type of the market data this name refers to
   */
  public abstract Class<T> getMarketDataType();

  //-------------------------------------------------------------------------
  /**
   * Compares this name to another.
   * <p>
   * Instances are compared in alphabetical order based on the name, taking into account the implementation type.
   * 
   * @param other  the object to compare to
   * @return the comparison
   */
  @Override
  public int compareTo(MarketDataName<?> other) {
    if (getClass() == other.getClass()) {
      return getName().compareTo(other.getName());
    }
    return compareSlow(other);
  }

  // compare when classes differ, broken out for inlining
  private int compareSlow(MarketDataName<?> other) {
    return ComparisonChain.start()
        .compare(getClass().getName(), other.getClass().getName())
        .compare(getName(), other.getName())
        .result();
  }

  /**
   * Checks if this instance equals another.
   * <p>
   * Instances are compared based on the name and market data type.
   * 
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == getClass()) {
      MarketDataName<?> other = (MarketDataName<?>) obj;
      return getName().equals(other.getName());
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return a suitable hash code
   */
  @Override
  public final int hashCode() {
    return getName().hashCode() ^ getClass().hashCode();
  }

  /**
   * Returns the name.
   * 
   * @return the name
   */
  @Override
  @ToString
  public final String toString() {
    return getName();
  }

}

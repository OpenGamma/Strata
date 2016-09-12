/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

/**
 * An identifier for a unique item of market data that can has a non-unique name.
 * <p>
 * A {@link MarketDataId} is used to uniquely identify market data within a system.
 * By contrast, a {@link MarketDataName} is only unique within a single coherent data set.
 * <p>
 * For example, a curve group contains a set of curves, and within the group the name is unique.
 * But the market data system may contain many curve groups where the same name appears in each group.
 * The {@code MarketDataId} includes both the group name and curve name in order to ensure uniqueness.
 * But within a specific context, the {@link MarketDataName} is also sufficient to find the same data.
 *
 * @param <T>  the type of the market data this identifier refers to
 */
public interface NamedMarketDataId<T>
    extends MarketDataId<T> {

  /**
   * Gets the market data name.
   * <p>
   * This name can be used to obtain the market data within a single coherent data set.
   *
   * @return the name of the market data this identifier refers to
   */
  public abstract MarketDataName<T> getMarketDataName();

}

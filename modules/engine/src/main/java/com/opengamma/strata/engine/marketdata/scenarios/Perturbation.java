/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenarios;

/**
 * Describes a perturbation applied to a single piece of data as part of a scenario.
 * <p>
 * For example, a 5 basis point parallel shift of a curve, or a 10% increase in the quoted price of a security.
 * <p>
 * Perturbation implementations should generally implement the Joda Beans {@code ImmutableBean} interface
 * which allows them to be serialized and used from remote clients.
 *
 * @param <T>  the type of the market data handled by the perturbation
 */
public interface Perturbation<T> {

  /**
   * Applies the perturbation to some market data, returning a new, modified instance of the data.
   *
   * @param marketData  a piece of market data
   * @return a new item of market data derived by applying the perturbation to the input data
   */
  public abstract T apply(T marketData);
}

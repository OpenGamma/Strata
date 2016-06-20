/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

/**
 * A market data identifier that identifies observable data.
 * <p>
 * Observable data can be requested from an external data provider, for example Bloomberg or Reuters.
 * The data provider is represented by {@link ObservableSource}.
 * <p>
 * Observable data is always represented by a {@code double}.
 */
public interface ObservableId
    extends MarketDataId<Double> {

  /**
   * Gets the type of data this identifier refers to, which is a {@code double}.
   *
   * @return the type of the market data this identifier refers to, {@code Double.class}
   */
  @Override
  public default Class<Double> getMarketDataType() {
    return Double.class;
  }

  /**
   * Gets the source of market data from which the market data should be retrieved.
   * <p>
   * The source identifies the source of data, such as Bloomberg or Reuters.
   *
   * @return the source from which the market data should be retrieved
   */
  public abstract ObservableSource getObservableSource();

  /**
   * Returns an identifier equivalent to this with the specified source.
   *
   * @param obsSource  the source of market data
   * @return the observable identifier
   */
  public abstract ObservableId withObservableSource(ObservableSource obsSource);

}

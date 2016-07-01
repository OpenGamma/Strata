/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import com.opengamma.strata.basics.StandardId;

/**
 * A market data identifier that identifies observable data.
 * <p>
 * Observable data can be requested from an external data provider, for example Bloomberg or Reuters.
 * <p>
 * An observable ID contains three pieces of information:
 * <ul>
 *   <li>A {@link StandardId} identifying the market data. This ID can come from any system. It might be
 *   an OpenGamma ID, for example {@code OpenGammaIndex~GBP_LIBOR_3M}, or it can be an ID from a market
 *   data source, for example {@code BloombergTicker~AAPL US Equity}.</li>
 *
 *   <li>A {@link FieldName} indicating the field in the market data record containing the data. See
 *   the {@code FieldName} documentation for more details.</li>
 *
 *   <li>A {@link ObservableSource} indicating where the data should come from.
 *   It is important to note that the standard ID is not necessarily related to the source.
 *   There is typically a mapping step in the market data system that maps the standard ID
 *   into an ID that can be used to look up the data in the source.</li>
 * </ul>
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
   * Gets the standard identifier identifying the data.
   * <p>
   * The identifier may be the identifier used to identify the item in an underlying data provider,
   * for example a Bloomberg ticker. It also may be any arbitrary unique identifier that can be resolved
   * to one or more data provider identifiers which are used to request the data from the provider.
   *
   * @return a standard identifier, such as a ticker, to identify the desired data
   */
  public abstract StandardId getStandardId();

  /**
   * Gets the field name in the market data record that contains the market data item.
   * <p>
   * Each ticker typically exposes many different fields. The field name specifies the desired field.
   * For example, the {@linkplain FieldName#MARKET_VALUE market value}.
   *
   * @return the field name in the market data record that contains the market data item
   */
  public abstract FieldName getFieldName();

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

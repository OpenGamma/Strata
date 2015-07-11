/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Basic types for modelling the market and market data.
 * <p>
 * There are two key classes in this package.
 * {@link com.opengamma.strata.basics.market.MarketDataKey MarketDataKey} is used by
 * applications to represent the market data that is needed.
 * {@link com.opengamma.strata.basics.market.MarketDataId MarketDataId} is used by
 * the market data system to uniquely represent a specific piece of market data.
 * There may be more than one {@code MarketDataId} associated with a given {@code MarketDataKey}.
 * <p>
 * For example, the application code may use a key to request the IBM share price.
 * However, the share price could be obtained from any number of different sources,
 * including Bloomberg and Reuters. Thus in this case the key represents the concept
 * "IBM share price" and the ID represents the concept "IBM share price from Bloomberg".
 * <p>
 * Note that market data keys and IDs can represent any piece of market data, including
 * quotes, curves, surfaces and cubes.
 */
package com.opengamma.strata.basics.market;


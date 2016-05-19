/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Basic types for modelling the market data and reference data.
 * <p>
 * Market data is used to capture the current market, such as quotes, FX rates,
 * discount curves, forward curves and volatilities.
 * The main market data interface is {@link com.opengamma.strata.basics.market.MarketData MarketData}
 * which is keyed by {@link com.opengamma.strata.basics.market.MarketDataId MarketDataId}.
 * <p>
 * Reference data is used to capture the slow-moving data necessary to perform calculations,
 * such as holiday calendars and securities.
 * The main reference data interface is {@link com.opengamma.strata.basics.market.ReferenceData ReferenceData}
 * which is keyed by {@link com.opengamma.strata.basics.market.ReferenceDataId ReferenceDataId}.
 */
package com.opengamma.strata.basics.market;


/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Basic types to model market data across scenarios.
 * <p>
 * Scenario market data captures the current market, such as quotes, FX rates,
 * discount curves, forward curves and volatilities, with values "bumped" to create scenarios.
 * The main market data interface is {@link com.opengamma.strata.data.scenario.ScenarioMarketData ScenarioMarketData}
 * which is keyed by {@link com.opengamma.strata.data.MarketDataId MarketDataId}.
 */
package com.opengamma.strata.data.scenario;

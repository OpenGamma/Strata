/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Provides the ability to calculate high-level measures on financial instruments.
 * <p>
 * The measures can be accessed in three ways:
 * <ul>
 * <li>for one trade/position and one scenario, such as methods on
 *   {@code com.opengamma.strata.measure.swap.SwapTradeCalculations SwapTradeCalculations}
 * <li>for one trade/position and many scenarios, such as methods on
 *   {@code com.opengamma.strata.measure.swap.SwapTradeCalculations SwapTradeCalculations}
 * <li>for a mixed portfolio of trades/positions and many scenarios,
 *   see {@link com.opengamma.strata.calc.CalculationRunner CalculationRunner} and
 *   functions such as {@link com.opengamma.strata.measure.swap.SwapTradeCalculationFunction SwapCalculationFunction}
 * </ul>
 */
package com.opengamma.strata.measure;

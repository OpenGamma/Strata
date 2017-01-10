/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Calculates risk measures on trades, applies scenarios and manages market data.
 * <p>
 * The strata-pricer module provides the ability to calculate results for a single trade,
 * single measure and single set of market data. The strata-calc module provides the ability
 * to calculate results for many trades, many measures and many sets of market data.
 * <p>
 * The main entry point is {@link com.opengamma.strata.calc.CalculationRunner CalculationRunner}.
 * It provides four "calculate" methods taking the trades, measures, market data and calculation rules.
 */
package com.opengamma.strata.calc;

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * The calculation runner.
 * <p>
 * This package includes the implementation of the calculation runner, which allows
 * a grid of results to be calculated for a list of trades and columns.
 * The primary classes for interacting with the calculation runner are in the
 * {@code com.opengamma.strata.calc} package.
 * <p>
 * The calculations are performed by implementations of
 * {@link com.opengamma.strata.calc.runner.CalculationFunction CalculationFunction}.
 * Each calculation may be customized and controlled by an implementation of
 * {@link com.opengamma.strata.calc.runner.CalculationParameter CalculationParameter}.
 */
package com.opengamma.strata.calc.runner;

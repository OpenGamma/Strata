/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Basic financial tools for working with values.
 * <p>
 * A <i>value</i> in the context of this package is a primitive {@code double}.
 * <p>
 * The {@code ValueAdjustment} class expressed a single change in a value, such as adding 20%.
 * The {@code ValueSchedule} and {@code ValueStep} classes allow an initial value to be changed
 * over time according to a schedule.
 * <p>
 * The {@code Rounding} interface expresses the convention for rounding a value.
 * The standard implementation is {@code HalfUpRounding}.
 */
package com.opengamma.strata.basics.value;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

/**
 * The target of calculation within a system.
 * <p>
 * All financial instruments that can be the target of calculations implement this marker interface.
 * For example, a trade or position.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface CalculationTarget {

}

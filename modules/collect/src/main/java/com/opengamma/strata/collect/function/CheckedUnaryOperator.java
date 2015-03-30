/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code UnaryOperator}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the object parameter
 */
@FunctionalInterface
public interface CheckedUnaryOperator<T> extends CheckedFunction<T, T> {

}

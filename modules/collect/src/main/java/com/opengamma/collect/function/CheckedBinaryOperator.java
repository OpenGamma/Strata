/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.function;

import com.opengamma.collect.Unchecked;

/**
 * A checked version of {@code BinaryOperator}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the object parameter
 */
@FunctionalInterface
public interface CheckedBinaryOperator<T> extends CheckedBiFunction<T, T, T> {

}

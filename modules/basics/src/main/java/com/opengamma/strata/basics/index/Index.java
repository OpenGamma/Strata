/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.Named;

/**
 * An index of values, such as LIBOR, FED FUND or daily exchange rates.
 * <p>
 * An index is an agreed mechanism for determining certain financial indicators,
 * such as exchange rate or interest rates. Most common indices are daily.
 * <p>
 * This is a marker interface, see {@link FxIndex}, {@link IborIndex}
 * and {@link OvernightIndex} for more information.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface Index
    extends Named {

}

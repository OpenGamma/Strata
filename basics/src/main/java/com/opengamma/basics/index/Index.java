/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import com.opengamma.collect.named.Named;

/**
 * An index of values, such as LIBOR, FED FUND or closing exchange rates.
 * <p>
 * An index is an agreed mechanism for determining certain financial indicators,
 * such as exchange rate or interest rates. Most common indices are daily.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface Index
    extends Named {

}

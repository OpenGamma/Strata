/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import org.joda.beans.ImmutableBean;

/**
 * Defines a mechanism for obtaining an interest rate.
 * <p>
 * A floating rate can be calculated in a number of ways, including observing an
 * index, interpolating two indices, averaging an index on specific dates, overnight
 * compounding and overnight averaging.
 * <p>
 * Each implementation contains the necessary information to obtain the rate, apart
 * from the start and end date of the period.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface Rate
    extends ImmutableBean {

}

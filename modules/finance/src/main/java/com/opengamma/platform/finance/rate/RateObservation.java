/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import org.joda.beans.ImmutableBean;

/**
 * Defines a mechanism for observing an interest rate.
 * <p>
 * A floating rate can be observed in a number of ways, including from one index,
 * interpolating two indices, averaging an index on specific dates, overnight compounding
 * and overnight averaging.
 * <p>
 * Each implementation contains the necessary information to observe the rate.
 * <p>
 * This is a marker interface, see implementations such as {@link IborRateObservation} and
 * {@link OvernightCompoundedRateObservation} for more information.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface RateObservation
    extends ImmutableBean {

}

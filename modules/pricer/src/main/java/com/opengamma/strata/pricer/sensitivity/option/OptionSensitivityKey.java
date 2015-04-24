/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity.option;

import org.joda.beans.ImmutableBean;

/**
 * A key used to identify a specific sensitivity.
 * <p>
 * This is used within {@link OptionPointSensitivity}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface OptionSensitivityKey
    extends ImmutableBean {

}
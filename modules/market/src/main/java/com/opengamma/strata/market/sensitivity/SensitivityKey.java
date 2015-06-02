/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import org.joda.beans.ImmutableBean;

/**
 * A key used to identify a specific sensitivity.
 * <p>
 * This is used within {@link CurveParameterSensitivities}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SensitivityKey
    extends ImmutableBean {

}

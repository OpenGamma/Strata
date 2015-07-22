/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing an FX vanilla option.
 * <p>
 * An FX option is a financial instrument that provides an option based on the future value of
 * a foreign exchange. The option is European, exercised only on the exercise date.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface FxVanillaOptionProduct
    extends Product, Expandable<FxVanillaOption>, ImmutableBean {

}

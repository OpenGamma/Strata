/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing a Non-Deliverable Forward (NDF).
 * <p>
 * An NDF is a financial instrument that returns the difference between a fixed FX rate 
 * agreed at the inception of the trade and the FX rate at maturity. 
 * It is primarily used to handle FX requirements for currencies that have settlement restrictions.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface FxNdfProduct
    extends Product, Expandable<ExpandedFxNdf> {

}

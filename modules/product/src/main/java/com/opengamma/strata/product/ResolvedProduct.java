/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;

/**
 * A product that has been resolved for pricing.
 * <p>
 * This is the resolved form of {@link Product}. Applications will typically create
 * a {@code ResolvedProduct} from a {@code Product} using {@link ReferenceData}.
 * <p>
 * Resolved objects may be bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
public interface ResolvedProduct {

}

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;

/**
 * A market convention for swap legs.
 * <p>
 * A convention contains key information that is commonly used in the market.
 * Two legs are often combined to form a {@link TradeConvention} for a swap,
 * such as {@link FixedIborSwapConvention} or {@link FixedOvernightSwapConvention}.
 * <p>
 * Each implementation should provide a method with the name {@code toLeg} with
 * whatever arguments are necessary to complete the leg.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CorporateActionEventLegConvention {

/*  public abstract SecurityId getNeededSecurityId();

  public abstract CorporateActionOption getOption();

  public abstract ImmutableList<CorporateActionWillGetLeg> getPaymentLegs();*/

}

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing a credit default swap.
 * <p>
 * A credit default swap (single name and index).
 * <p>
 * In a credit default swap one party (the protection seller) agrees to compensate another party
 * (the protection buyer) if a specified company or Sovereign (the reference entity)
 * experiences a credit event, indicating it is or may be unable to service its debts.
 * The protection seller is typically paid a fee and/or premium, expressed as an annualized
 * percent of the notional in basis points, regularly over the life of the transaction or
 * otherwise as agreed by the parties.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CdsProduct
    extends Product, Expandable<ExpandedCds>, ImmutableBean {

}

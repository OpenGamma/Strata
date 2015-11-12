/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a credit default swap (CDS), including single-name and index swaps.
 * <p>
 * A CDS is a financial instrument where the protection seller agrees to compensate
 * the protection buyer if a specified specified company or Sovereign entity experiences
 * a credit event, indicating it is or may be unable to service its debts.
 * The protection seller is typically paid a fee and/or premium, expressed as an annualized
 * percentage of the notional in basis points, regularly over the life of the transaction or
 * otherwise as agreed by the parties.
 * <p>
 * For example, a company engaged in another financial instrument with a counterparty may
 * wish to protect itself against the risk of the counterparty defaulting.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CdsProduct
    extends Product, Expandable<ExpandedCds> {

}

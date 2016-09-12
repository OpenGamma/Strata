/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * Identifies the reference that credit protection applies to.
 * <p>
 * This interface represents the underlying of the credit default swap.
 * The underlying is typically either a Single Name Obligation or an Index.
 */
public interface ReferenceInformation {
  // TODO: better name

  /**
   * Gets the type of the underlying.
   * 
   * @return the type, single-name or index
   */
  ReferenceInformationType getType();

}

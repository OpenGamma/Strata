/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * The product details of a security.
 * <p>
 * A {@code SecurityProduct} includes all the financial details of a security.
 * It will include any underlying securities.
 * For example, a bond future option would include the underlying bond future
 * and the basket of bonds.
 * <p>
 * The product of a security is distinct from the {@linkplain ReferenceSecurity security}.
 * The security, referred to using a {@link SecurityId}, does not include underlying securities.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecurityProduct
    extends Product {

  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * It is the key used to lookup the security in {@link ReferenceData}.
   * <p>
   * A real-world security will typically have multiple identifiers.
   * The only restriction placed on the identifier is that it is sufficiently
   * unique for the reference data lookup. As such, it is acceptable to use
   * an identifier from a well-known global or vendor symbology.
   * 
   * @return the security identifier
   */
  public abstract SecurityId getSecurityId();

  /**
   * Gets the currency that the security is traded in.
   * 
   * @return the trading currency
   */
  public abstract Currency getCurrency();

}

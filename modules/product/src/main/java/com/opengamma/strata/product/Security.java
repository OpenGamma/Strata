/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;

/**
 * A single security.
 * <p>
 * A security is one of the building blocks of finance, representing a fungible instrument that can be traded.
 * This is intended to cover instruments such as listed equities and futures.
 * It is intended that Over-The-Counter (OTC) instruments, such as an interest rate swap,
 * are embedded directly within the trade, rather than handled as one-off securities.
 * <p>
 * When referring to a security from another object, such as an underlying on a
 * more complex trade, consideration should be given to using {@link SecurityLink}.
 * <p>
 * Implementations of this interface must be immutable beans.
 * 
 * @param <P>  the type of the product
 */
public interface Security<P extends Product>
    extends IdentifiableBean, Attributable {

  /**
   * The primary standard identifier for the security.
   * <p>
   * The standard identifier is used to identify the security.
   * It will typically be an identifier in an external data system.
   * <p>
   * A security may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * Gets the entire set of additional attributes.
   * <p>
   * Attributes are typically used to tag the object with additional information.
   * 
   * @return the complete set of attributes
   */
  @Override
  public abstract ImmutableMap<String, String> getAttributes();

  /**
   * Gets the product underlying the security.
   * <p>
   * All securities refer to a single underlying product.
   * The product captures the financial details of the security contract.
   * 
   * @return the product
   */
  public abstract P getProduct();

}

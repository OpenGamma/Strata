/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.Link;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.equity.Equity;

/**
 * A shared financial security.
 * <p>
 * A security is one of the building blocks of finance, representing a fungible instrument that can be traded.
 * This is intended to cover instruments such as {@link Equity} and {@code Bond}.
 * It is intended that OTC instruments, such as an interest rate swap, are
 * embedded within a {@link Trade}, rather than handled as one-off securities.
 * <p>
 * When referring to a security from another object, such as an underlying on a
 * more complex trade, the reference should be via a {@link Link}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Security
    extends IdentifiableBean, Attributable, ImmutableBean {

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
   * Gets the security type.
   * 
   * @return the security type
   */
  public abstract SecurityType getSecurityType();

}

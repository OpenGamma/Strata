/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableMap;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * A single fungible security.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Security
    extends IdentifiableBean, ImmutableBean {

  /**
   * The primary standard identifier for the security.
   * <p>
   * The standard identifier is used to identify the security.
   * It will typically be an identifier in an external data system.
   * <p>
   * A security may have multiple active identifiers. Any identifier may be chosen here,
   * however it is strongly recommended to use an identifier that does not change over time.
   */
  @Override
  public StandardId getStandardId();

  /**
   * Gets the security type.
   * 
   * @return the security type, not null
   */
  public SecurityType getSecurityType();

  /**
   * Gets the entire set of additional attributes.
   * <p>
   * Most data in the trade is available as bean properties.
   * Attributes are used to tag the object with additional information.
   * 
   * @return the complete set of attributes, not null
   */
  public ImmutableMap<String, String> getAttributes();

}

/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;

/**
 * A legal entity.
 * <p>
 * A legal entity is one of the building blocks of finance, representing an organization.
 * It is used to capture details for credit worthiness.
 * The legal entity can be looked up in {@link ReferenceData} using the identifier.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface LegalEntity {

  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier uniquely identifies the legal entity within the system.
   * 
   * @return the legal entity identifier
   */
  public abstract LegalEntityId getLegalEntityId();

}

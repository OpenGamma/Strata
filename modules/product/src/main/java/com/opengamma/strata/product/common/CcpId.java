/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import java.io.Serializable;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * An identifier for a Central Counterparty Clearing House (CCP).
 * <p>
 * Identifiers for common CCPs are provided in {@link CcpIds}.
 */
public final class CcpId implements Named, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The code identifying the CCP.
   */
  private final String name;

  //-------------------------------------------------------------------------
  /**
   * Obtains an identifier for the CCP.
   *
   * @param name the code identifying the CCP
   * @return an identifier for the CCP
   */
  @FromString
  public static CcpId of(String name) {
    return new CcpId(name);
  }

  // restricted constructor
  private CcpId(String name) {
    this.name = ArgChecker.notBlank(name, "name");
  }

  // resolve after deserialization
  private Object readResolve() {
    return of(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the code identifying the CCP.
   *
   * @return the code identifying the CCP
   */
  @Override
  public String getName() {
    return name;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   * <p>
   * The comparison checks the name.
   * 
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CcpId that = (CcpId) obj;
    return name.equals(that.name);
  }

  /**
   * Returns a suitable hash code for the identifier.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @ToString
  @Override
  public String toString() {
    return name;
  }

}

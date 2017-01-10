/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * Group used to identify a related set of repo curves when pricing bonds.
 * <p>
 * This class was previously called {@code BondGroup}.
 * It was renamed in version 1.1 of Strata to allow {@link LegalEntityDiscountingProvider}
 * to be used for pricing bills as well as bonds.
 */
public final class RepoGroup
    extends TypedString<RepoGroup> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Group names may contain any character, but must not be empty.
   *
   * @param name  the group name
   * @return a group with the specified String
   */
  @FromString
  public static RepoGroup of(String name) {
    return new RepoGroup(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the group name
   */
  RepoGroup(String name) {
    super(name);
  }

}

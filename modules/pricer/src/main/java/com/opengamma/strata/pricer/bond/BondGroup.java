/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * Bond group.
 */
public final class BondGroup
    extends TypedString<BondGroup> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Bond group names may contain any character, but must not be empty.
   *
   * @param name  the bond group name
   * @return a bond group with the specified String
   */
  @FromString
  public static BondGroup of(String name) {
    return new BondGroup(name);
  }

  /**
   * Creates an instance.
   * 
   * @param group  the bond group name
   */
  private BondGroup(String name) {
    super(name);
  }

}

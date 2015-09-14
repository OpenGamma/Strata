/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Legal entity group.
 */
public final class LegalEntityGroup
    extends TypedString<LegalEntityGroup> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code LegalEntityGroup} by name.
   * <p>
   * Legal entity group names may contain any character, but must not be empty.
   *
   * @param name  the legal entity group name
   * @return a legal entity group with the specified String
   */
  @FromString
  public static LegalEntityGroup of(String name) {
    return new LegalEntityGroup(name);
  }

  /**
   * Creates an instance.
   * 
   * @param group  the legal entity group name
   */
  private LegalEntityGroup(String name) {
    super(name);
  }

}

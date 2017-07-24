/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A CMS payment period type.
 * <p>
 * A CMS payment period is a CMS coupon, CMS caplet or CMS floorlet.
 * All of these payments are defined in a unified manner by {@link CmsPeriod}.
 */
public enum CmsPeriodType implements NamedEnum {

  /**
   * CMS coupon.
   */
  COUPON,
  /**
   * CMS caplet.
   */
  CAPLET,
  /**
   * CMS floorlet.
   */
  FLOORLET;

  // helper for name conversions
  private static final EnumNames<CmsPeriodType> NAMES = EnumNames.of(CmsPeriodType.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CmsPeriodType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}

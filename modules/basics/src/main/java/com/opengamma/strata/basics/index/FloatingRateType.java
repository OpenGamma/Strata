/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The type of a floating rate index.
 * <p>
 * This provides a high-level categorization of the floating rate index.
 * This is used to classify the index and create the right kind of pricing
 * index, {@link IborIndex} or {@link OvernightIndex}.
 */
public enum FloatingRateType {

  /**
   * A floating rate index that is based on an Ibor index.
   * <p>
   * This kind of rate translates to an {@link IborIndex}.
   */
  IBOR,
  /**
   * A floating rate index that is based on an Overnight index with compounding.
   * <p>
   * This kind of rate translates to an {@link OvernightIndex}.
   */
  OVERNIGHT_COMPOUNDED,
  /**
   * A floating rate index that is based on an Overnight index with averaging.
   * <p>
   * This kind of rate translates to an {@link OvernightIndex}.
   * This is typically used only for US Fed Fund swaps.
   */
  OVERNIGHT_AVERAGED,
  /**
   * A floating rate index that is based on a price index.
   * <p>
   * This kind of rate translates to an {@link PriceIndex}.
   */
  PRICE,
  /**
   * A floating rate index of another type.
   */
  OTHER;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FloatingRateType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Ibor'.
   * 
   * @return true if Ibor, false otherwise
   */
  public boolean isIbor() {
    return this == IBOR;
  }

  /**
   * Checks if the type is 'OvernightCompounded' or 'OvernightAveraged'.
   * 
   * @return true if Overnight, false otherwise
   */
  public boolean isOvernight() {
    return this == OVERNIGHT_COMPOUNDED || this == OVERNIGHT_AVERAGED;
  }

  /**
   * Checks if the type is 'Price'.
   *
   * @return true if Price, false otherwise
   */
  public boolean isPrice() {
    return this == PRICE;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}

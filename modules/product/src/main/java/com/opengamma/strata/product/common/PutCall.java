/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Flag indicating whether a trade is "put" or "call".
 * <p>
 * The concepts of put and call apply to options trading.
 * A call gives the owner the right, but not obligation, to buy the underlying at
 * an agreed price in the future. A put gives a similar option to sell.
 */
public enum PutCall {

  /**
   * Put.
   */
  PUT,
  /**
   * Call.
   */
  CALL;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PutCall of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Converts a boolean "is put" flag to the enum value.
   * 
   * @param isPut  the put flag, true for put, false for call
   * @return the equivalent enum
   */
  public static PutCall ofPut(boolean isPut) {
    return isPut ? PUT : CALL;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Put'.
   * 
   * @return true if put, false if call
   */
  public boolean isPut() {
    return this == PUT;
  }

  /**
   * Checks if the type is 'Call'.
   * 
   * @return true if call, false if put
   */
  public boolean isCall() {
    return this == CALL;
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

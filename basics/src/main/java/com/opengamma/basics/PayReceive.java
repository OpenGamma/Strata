/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * Flag indicating whether a financial instrument is "pay" or "receive".
 * <p>
 * Specifies the direction of payments.
 * For example, a swap typically has two legs, a pay leg, where payments are made
 * to the counterparty, and a receive leg, where payments are received.
 */
public enum PayReceive {

  /**
   * Pay.
   */
  PAY,
  /**
   * Receive.
   */
  RECEIVE;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PayReceive of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Converts a boolean "is pay" flag to the enum value.
   * 
   * @param isPay  the pay flag, true for pay, false for receive
   * @return the equivalent enum, not null
   */
  public static PayReceive ofPay(boolean isPay) {
    return isPay ? PAY : RECEIVE;
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the specified amount, returning the negative if 'Pay' or the
   * unaltered value is 'Receive'.
   * 
   * @param amount  the amount to adjust
   * @return the adjusted amount
   */
  public double adjustAmount(double amount) {
    return isPay() ? -amount : amount;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Pay'.
   * 
   * @return true if pay, false if receive
   */
  public boolean isPay() {
    return this == PAY;
  }

  /**
   * Checks if the type is 'Receive'.
   * 
   * @return true if receive, false if pay
   */
  public boolean isReceive() {
    return this == RECEIVE;
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

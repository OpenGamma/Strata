/**
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
   * Obtains an instance from the specified unique name.
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
   * @return the equivalent enum
   */
  public static PayReceive ofPay(boolean isPay) {
    return isPay ? PAY : RECEIVE;
  }

  /**
   * Converts a signed amount to the enum value.
   * <p>
   * A negative value will return 'Pay'.
   * A positive value will return 'Receive'.
   * This effectively parses the result of {@link #normalize(double)}.
   * 
   * @param amount  the amount to check
   * @return the equivalent enum
   */
  public static PayReceive ofSignedAmount(double amount) {
    return Double.compare(amount, 0d) < 0 ? PAY : RECEIVE;
  }

  //-------------------------------------------------------------------------
  /**
   * Normalizes the specified notional amount using this pay/receive rule.
   * <p>
   * This returns a negative signed amount if this is 'Receive', and a positive
   * signed amount if this is 'Pay'. This effectively normalizes the input notional
   * to the pay/receive sign conventions of this library.
   * 
   * @param amount  the amount to adjust
   * @return the adjusted amount
   */
  public double normalize(double amount) {
    double normalized = Math.abs(amount);
    return isPay() ? -normalized : normalized;
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

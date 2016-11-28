/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

/**
 * Provides double precision machine constants
 */
final class D1MACH {

  /**
   * Smallest normalised number representable by a double according to IEEE
   * It's about 2.225E-308
   * @return Smallest normalised number representable by a double according to IEEE 
   */
  static double one() {
    return Double.longBitsToDouble(0x0010000000000000L);
  }

  /**
   * Largest normalised number representable by a double according to IEEE
   * It's about 1.7975E+308
   * @return Largest normalised number representable by a double according to IEEE 
   */
  static double two() {
    return Double.longBitsToDouble(0x7fefffffffffffffL);
  }

  /**
   * Machine precision/machine radix according to IEEE
   * Approximately 1.11E-16 === Math.pow(2,-53)/2 (assuming radix 2)
   * @return Machine precision/machine radix according to IEEE
   */

  static double three() {
    return Double.longBitsToDouble(4368491638549381120L);
  }

  /**
   * Machine precision according to IEEE
   * Approximately 2.22E-16 === Math.pow(2,-53)
   * @return Machine precision according to IEEE
   */
  static double four() {
    return Double.longBitsToDouble(4372995238176751616L);
  }

  /**
   * Log10(Machine radix)
   * @return Log10(Machine radix)
   */
  static double five() {
    return Double.longBitsToDouble(4599094494223104511L);
  }

}

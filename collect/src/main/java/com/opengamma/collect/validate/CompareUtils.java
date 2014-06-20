/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.validate;

/**
 * Utility methods to simplify comparisons.
 * <p>
 * This is a thread-safe static utility class.
 */
public final class CompareUtils {

  /**
   * Restricted constructor.
   */
  private CompareUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Compares two objects finding the maximum.
   * 
   * @param <T>  the object type
   * @param a  item that compareTo is called on, may be null
   * @param b  item that is being compared, may be null
   * @return the maximum of the two objects, null if both null
   */
  public static <T extends Comparable<? super T>> T max(T a, T b) {
    if (a != null && b != null) {
      return a.compareTo(b) >= 0 ? a : b;
    }
    if (a == null) {
      if (b == null) {
        return null;
      } else {
        return b;
      }
    } else {
      return a;
    }
  }

  /**
   * Compares two objects finding the minimum.
   * 
   * @param <T>  the object type
   * @param a  item that compareTo is called on, may be null
   * @param b  item that is being compared, may be null
   * @return the minimum of the two objects, null if both null
   */
  public static <T extends Comparable<? super T>> T min(T a, T b) {
    if (a != null && b != null) {
      return a.compareTo(b) <= 0 ? a : b;
    }
    if (a == null) {
      if (b == null) {
        return null;
      } else {
        return b;
      }
    } else {
      return a;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares two objects, either of which might be null, sorting nulls low.
   * 
   * @param <E>  the type of object being compared
   * @param a  item that compareTo is called on
   * @param b  item that is being compared
   * @return negative when a less than b, zero when equal, positive when greater
   */
  public static <E> int compareWithNullLow(Comparable<E> a, E b) {
    if (a == null) {
      return b == null ? 0 : -1;
    } else if (b == null) {
      return 1; // a not null
    } else {
      return a.compareTo((E) b);
    }
  }

  /**
   * Compares two objects, either of which might be null, sorting nulls high.
   * 
   * @param <E>  the type of object being compared
   * @param a  item that compareTo is called on
   * @param b  item that is being compared
   * @return negative when a less than b, zero when equal, positive when greater
   */
  public static <E> int compareWithNullHigh(Comparable<E> a, E b) {
    if (a == null) {
      return b == null ? 0 : 1;
    } else if (b == null) {
      return -1; // a not null
    } else {
      return a.compareTo((E) b);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compare two doubles to see if they're 'closely' equal.
   * <p>
   * This handles rounding errors which can mean the results of double precision computations
   * lead to small differences in results.
   * The definition 'close' is that the difference is less than 10^-15 (1E-15).
   * If a different maximum allowed difference is required, use the other version of this method.
   * 
   * @param a  the first value
   * @param b  the second value
   * @return true, if a and b are equal to within 10^-15, false otherwise
   */
  public static boolean closeEquals(double a, double b) {
    if (Double.isInfinite(a)) {
      return (a == b);
    }
    return (Math.abs(a - b) < 1E-15);
  }

  /**
   * Compare two doubles to see if they're 'closely' equal.
   * <p>
   * This handles rounding errors which can mean the results of double precision computations
   * lead to small differences in results.
   * The definition 'close' is that the absolute difference is less than the specified difference.
   * 
   * @param a  the first value
   * @param b  the second value
   * @param maxDifference  the maximum difference to allow
   * @return true, if a and b are equal to within the tolerance
   */
  public static boolean closeEquals(double a, double b, double maxDifference) {
    if (Double.isInfinite(a)) {
      return (a == b);
    }
    return (Math.abs(a - b) < maxDifference);
  }

  /**
   * Compare two doubles to see if they're 'closely' equal.
   * <p>
   * This handles rounding errors which can mean the results of double precision computations
   * lead to small differences in results.
   * This method returns the difference to indicate how the first differs from the second.
   * 
   * @param a  the first value
   * @param b  the second value
   * @param maxDifference  the maximum difference to allow while still considering the values equal
   * @return the value 0 if a and b are equal to within the tolerance; a value less than 0 if a is numerically less
   *         than b; and a value greater than 0 if a is numerically greater than b.
   */
  public static int compareWithTolerance(double a, double b, double maxDifference) {
    if (a == Double.POSITIVE_INFINITY) {
      return (b == Double.POSITIVE_INFINITY ? 0 : 1);
    } else if (a == Double.NEGATIVE_INFINITY) {
      return (b == Double.NEGATIVE_INFINITY ? 0 : -1);
    } else if (b == Double.POSITIVE_INFINITY) {
      return -1;
    } else if (b == Double.NEGATIVE_INFINITY) {
      return 1;
    }
    if (Math.abs(a - b) < maxDifference) {
      return 0;
    }
    return (a < b) ? -1 : 1;
  }

}

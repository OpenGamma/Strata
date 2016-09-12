/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.strata.math.impl;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: This is from OG-Maths

/**
 * Tests for values being equal allowing for a level of floating point fuzz
 * Based on the OG-Maths C++ fuzzy equals code .
 */
public class FuzzyEquals {

  private static boolean __LOCALDEBUG = false;
  private static boolean DEBUG = false;

  private static double float64_eps;
  private static double default_tolerance;
  static {
    float64_eps = float64_t_machineEpsilon();
    default_tolerance = 10 * float64_eps;
  }

  /**
   * The logger instance
   */
  private static Logger s_log = LoggerFactory.getLogger(FuzzyEquals.class);

  /**
   * Gets machine precision for double precision floating point numbers on this machine.
   * @return machine precision for double precision floating point numbers on this machine.
   */
  public static double getEps()
  {
    return float64_eps;
  }

  /**
   * Get the default tolerance used in this class.
   * @return the default tolerance.
   */
  public static double getDefaultTolerance()
  {
    return default_tolerance;
  }

  /**
   * Checks if two double precision floating point numbers are approximately "equal"
   * @param val1 the first value
   * @param val2 the second value
   * @param maxabserror determines the minimum threshold for "equal" in terms of the two numbers being very small in magnitude.
   * @param maxrelerror determines the minimum threshold for "equal" in terms of the relative magnitude of the numbers.
   * i.e. invariant of the magnitude of the numbers what is the maximum level of magnitude difference acceptable.
   * @return true if they are considered equal, else false
   */
  public static boolean SingleValueFuzzyEquals(double val1, double val2, double maxabserror, double maxrelerror)
  {

    if (__LOCALDEBUG) {
      DEBUG_PRINT("FuzzyEquals: Comparing %24.16f and %24.16f\n", val1, val2);
    }

    if (Double.isNaN(val1))
    {
      if (__LOCALDEBUG) {
        DEBUG_PRINT("FuzzyEquals: Failed as value 1 is NaN\n");
      }
      return false;
    }

    if (Double.isNaN(val2))
    {
      if (__LOCALDEBUG) {
        DEBUG_PRINT("FuzzyEquals: Failed as value 2 is NaN\n");
      }
      return false;
    }

    // deal with infs in debug mode
    if (__LOCALDEBUG) {
      if (DEBUG) {
        boolean val1isinf = Double.isInfinite(val1);
        boolean val2isinf = Double.isInfinite(val2);
        if (val1isinf || val2isinf)
        {
          if (val1isinf && val2isinf)
          {
            if (Math.signum(val2) == Math.signum(val1))
            {
              DEBUG_PRINT("FuzzyEquals: Inf Branch. Success as both inf of same sign\n");
              return true;
            }
          }

          DEBUG_PRINT("FuzzyEquals: Inf Branch. Fail, non matching infs\n");
          return false;
        }
      }
    }

    if (val1 == val2)
    {
      return true; // (+/-)inf compares == as does (+/-)0.e0
    }

    // check if they are below max absolute error bounds (i.e. small in the first place)
    double diff = (val1 - val2);
    if (maxabserror > Math.abs(diff))
    {
      if (__LOCALDEBUG) {
        DEBUG_PRINT("FuzzyEquals: Match as below diff bounds. maxabserror > diff. (%24.16f >%24.16f)\n",
            maxabserror, Math.abs(diff));
      }
      return true;
    }
    if (__LOCALDEBUG) {
      DEBUG_PRINT("FuzzyEquals: Failed as diff > maxabserror. (%24.16f >  %24.16f)\n",
          Math.abs(diff), maxabserror);
    }

    // check if they are within a relative error bound, div difference by largest of the 2
    double divisor = Math.abs(val1) > Math.abs(val2) ? val1 : val2;
    double relerror = Math.abs(diff / divisor);
    if (maxrelerror > relerror)
    {
      if (__LOCALDEBUG) {
        DEBUG_PRINT("FuzzyEquals: Match as maxrelerror > relerror. (%24.16f >  %24.16f)\n", maxrelerror, relerror);
      }
      return true;
    }
    ;

    if (__LOCALDEBUG) {
      DEBUG_PRINT("FuzzyEquals: Fail as relerror > maxrelerror. (%24.16f >  %24.16f)\n", relerror, maxrelerror);
    }

    return false;
  }

  /**
   * Checks if two double precision floating point numbers are approximately "equal"
   * Default values are used for tolerances
   * @param val1 the first value
   * @param val2 the second value
   * @return true if they are considered equal, else false
   */
  public static boolean SingleValueFuzzyEquals(double val1, double val2)
  {
    return SingleValueFuzzyEquals(val1, val2, default_tolerance, default_tolerance);
  }

  /**
   * Checks if two double precision floating point arrays are approximately "equal"
   * Equal means the arrays have values the are considered fuzzy equals appearing in the same order
   * and the arrays the same length.
   * 
   * @param arr1 the first value
   * @param arr2 the second value
   * @param maxabserror determines the minimum threshold for "equal" in terms of the two numbers being very small in magnitude.
   * @param maxrelerror determines the minimum threshold for "equal" in terms of the relative magnitude of the numbers.
   *  i.e. invariant of the magnitude of the numbers what is the maximum level of magnitude difference acceptable.
   * @return true if they are considered equal, else false
   */
  public static boolean ArrayFuzzyEquals(double[] arr1, double[] arr2, double maxabserror, double maxrelerror)
  {
    if (arr1.length != arr2.length)
    {
      return false;
    }
    for (int i = 0; i < arr1.length; i++)
    {
      if (!SingleValueFuzzyEquals(arr1[i], arr2[i], maxabserror, maxrelerror))
        return false;
    }
    return true;
  }

  /**
   * Checks if two double precision floating point arrays are approximately "equal"
   * Equal means the arrays have values the are considered fuzzy equals appearing in the same order
   * and the arrays the same length.
   * Default values are used for tolerances.
   * 
   * @param arr1 the first value
   * @param arr2 the second value
   * @return true if they are considered equal, else false
   */
  public static boolean ArrayFuzzyEquals(double[] arr1, double[] arr2)
  {
    return ArrayFuzzyEquals(arr1, arr2, default_tolerance, default_tolerance);
  }

  /**
   * Checks if two double precision floating point array of arrays are approximately "equal"
   * Equal means the arrays have values the are considered fuzzy equals appearing in the same order and the arrays the same dimension.
   * Default values are used for tolerances.
   * @param arr1 the first value
   * @param arr2 the second value
   * @return true if they are considered equal, else false
   */
  public static boolean ArrayFuzzyEquals(double[][] arr1, double[][] arr2)
  {
    return ArrayFuzzyEquals(arr1, arr2, default_tolerance, default_tolerance);
  }

  /**
   * Checks if two double precision floating point array of arrays are approximately "equal"
   * Equal means the arrays have values the are considered fuzzy equals appearing in the same order
   * and the arrays the same dimension.
   * 
   * @param arr1 the first value
   * @param arr2 the second value
   * @param maxabserror determines the minimum threshold for "equal" in terms of the two numbers being very small in magnitude.
   * @param maxrelerror determines the minimum threshold for "equal" in terms of the relative magnitude of the numbers.
   *  i.e. invariant of the magnitude of the numbers what is the maximum level of magnitude difference acceptable.
   * @return true if they are considered equal, else false
   */
  public static boolean ArrayFuzzyEquals(double[][] arr1, double[][] arr2, double maxabserror, double maxrelerror)
  {
    if (arr1.length != arr2.length)
    {
      return false;
    }
    int rows = arr1.length;
    for (int k = 0; k < rows; k++)
    {
      if (arr1[k].length != arr2[k].length)
      {
        return false;
      }
      if (ArrayFuzzyEquals(arr1[k], arr2[k], maxabserror, maxrelerror) == false)
      {
        return false;
      }
    }
    return true;

  }

  /**
   * Debug helpers
   * @param str
   */

  private static void DEBUG_PRINT(String str)
  {
    s_log.debug(str);
  }

  private static void DEBUG_PRINT(String str, double a, double b)
  {
    s_log.debug(String.format(Locale.ENGLISH, str, a, b));
  }

  private static double float64_t_machineEpsilon() {
    double eps = 1.e0;
    while ((1.e0 + (eps / 2.e0)) != 1.e0)
    {
      eps /= 2.e0;
    }
    return eps;
  }

}

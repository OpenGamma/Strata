/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import com.opengamma.strata.math.MathException;

/**
 * Computes the number of terms required to achieve a given accuracy in an orthogonal polynomial series.
 * This code is an approximate translation of the equivalent function in the "Public Domain" code from SLATEC, see:
 * http://www.netlib.org/slatec/fnlib/initds.f
 */
final class INITDS {

  /**
   * Computes an orthogonal series based on coefficients "os" including sufficient terms to insure that the error is no larger than 'eta'.
   * @param os array of coefficients of an orthogonal series
   * @param nos number of coefficients in "os"
   * @param eta usually 10% of machine precision, arbitrary!
   * @return the number of terms needed in the series to achieve the desired accuracy
   */
  static int getInitds(double[] os, int nos, double eta) {
    if (os == null) {
      throw new MathException("INITDS: os is null");
    }
    if (nos != os.length) {
      throw new MathException("INITDS: nos and os.length are not equal, they should be!");
    }
    if (nos < 1) {
      throw new MathException("INITDS: Number of coeffs is less than 1");
    }
    double err = 0;
    int i = 0;
    boolean error = true;
    for (int ii = 0; ii < nos; ii++) {
      i = nos - 1 - ii;
      err += Math.abs(os[i]); // Not quite what F77 things, no cast to float.
      if (err > eta) {
        error = false;
        break;
      }
    }
    if (error) {
      throw new MathException("INITDS: Chebyshev series too short for specified accuracy");
    }
    return i;
  }

}

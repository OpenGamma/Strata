/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `cern.jet.random` package.
 * Changes:
 * - package name
 * - missing Javadoc param tags
 * - reformat
 */
/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
*/
package com.opengamma.strata.math.impl.cern;

//CSOFF: ALL
/**
 * Contains various mathematical helper methods.
 *
 * <b>Implementation:</b> High performance implementation.
 * <dt>This is a port of <tt>gen_fun.cpp</tt> from the <A HREF="http://www.cis.tu-graz.ac.at/stat/stadl/random.html">C-RAND / WIN-RAND</A> library.
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
class Fun {
  /**
   * Makes this class non instantiable, but still let's others inherit from it.
   */
  protected Fun() {
    throw new RuntimeException("Non instantiable");
  }

  private static double _fkt_value(double lambda, double z1, double z2, double x_value) {
    double y_value;

    y_value = Math.cos(z1 * x_value) / (Math.pow((x_value * x_value + z2 * z2), (lambda + 0.5)));
    return y_value;
  }

  public static double bessel2_fkt(double lambda, double beta) {
    final double pi = Math.PI;
    double sum, x, step, x1, first_value, new_value;
    double epsilon = 0.01;
    double y, fx, z1, z2, erg;
    double period, border, first_sum, second_sum;
    double my, c, prod = 0.0, diff, value;
    int i, j, nr_per;

    final double b0[] = {-1.5787132, -0.6130827, 0.1735823, 1.4793411,
        2.6667307, 4.9086836, 8.1355339,
    };
    final double b05[] = {-1.9694802, -0.7642538, 0.0826017, 1.4276355,
        2.6303682, 4.8857787, 8.1207968,
    };
    final double b1[] = {-2.9807345, -1.1969943, -0.1843161, 1.2739241,
        2.5218256, 4.8172216, 8.0765633,
    };
    final double b2[] = {-5.9889676, -2.7145389, -1.1781269, 0.6782201,
        2.0954009, 4.5452152, 7.9003173,
    };
    final double b3[] = {-9.6803440, -4.8211925, -2.6533185, -0.2583337,
        1.4091915, 4.0993448, 7.6088310,
    };
    final double b5[] = {-18.1567152, -10.0939408, -6.5819139, -2.9371545,
        -0.6289005, 2.7270412, 6.6936799,
    };
    final double b8[] = {-32.4910195, -19.6065943, -14.0347298, -8.3839439,
        -4.9679730, -0.3567823, 4.5589697,
    };

    if (lambda == 0.0) {
      if (beta == 0.1)
        return (b0[0]);
      if (beta == 0.5)
        return (b0[1]);
      if (beta == 1.0)
        return (b0[2]);
      if (beta == 2.0)
        return (b0[3]);
      if (beta == 3.0)
        return (b0[4]);
      if (beta == 5.0)
        return (b0[5]);
      if (beta == 8.0)
        return (b0[6]);
    }

    if (lambda == 0.5) {
      if (beta == 0.1)
        return (b05[0]);
      if (beta == 0.5)
        return (b05[1]);
      if (beta == 1.0)
        return (b05[2]);
      if (beta == 2.0)
        return (b05[3]);
      if (beta == 3.0)
        return (b05[4]);
      if (beta == 5.0)
        return (b05[5]);
      if (beta == 8.0)
        return (b05[6]);
    }

    if (lambda == 1.0) {
      if (beta == 0.1)
        return (b1[0]);
      if (beta == 0.5)
        return (b1[1]);
      if (beta == 1.0)
        return (b1[2]);
      if (beta == 2.0)
        return (b1[3]);
      if (beta == 3.0)
        return (b1[4]);
      if (beta == 5.0)
        return (b1[5]);
      if (beta == 8.0)
        return (b1[6]);
    }

    if (lambda == 2.0) {
      if (beta == 0.1)
        return (b2[0]);
      if (beta == 0.5)
        return (b2[1]);
      if (beta == 1.0)
        return (b2[2]);
      if (beta == 2.0)
        return (b2[3]);
      if (beta == 3.0)
        return (b2[4]);
      if (beta == 5.0)
        return (b2[5]);
      if (beta == 8.0)
        return (b2[6]);
    }

    if (lambda == 3.0) {
      if (beta == 0.1)
        return (b3[0]);
      if (beta == 0.5)
        return (b3[1]);
      if (beta == 1.0)
        return (b3[2]);
      if (beta == 2.0)
        return (b3[3]);
      if (beta == 3.0)
        return (b3[4]);
      if (beta == 5.0)
        return (b3[5]);
      if (beta == 8.0)
        return (b3[6]);
    }

    if (lambda == 5.0) {
      if (beta == 0.1)
        return (b5[0]);
      if (beta == 0.5)
        return (b5[1]);
      if (beta == 1.0)
        return (b5[2]);
      if (beta == 2.0)
        return (b5[3]);
      if (beta == 3.0)
        return (b5[4]);
      if (beta == 5.0)
        return (b5[5]);
      if (beta == 8.0)
        return (b5[6]);
    }

    if (lambda == 8.0) {
      if (beta == 0.1)
        return (b8[0]);
      if (beta == 0.5)
        return (b8[1]);
      if (beta == 1.0)
        return (b8[2]);
      if (beta == 2.0)
        return (b8[3]);
      if (beta == 3.0)
        return (b8[4]);
      if (beta == 5.0)
        return (b8[5]);
      if (beta == 8.0)
        return (b8[6]);
    }

    if ((beta - 5.0 * lambda - 8.0) >= 0.0) {
      my = 4.0 * lambda * lambda;
      c = -0.9189385 + 0.5 * Math.log(beta) + beta;
      sum = 1.0;
      value = 1.0;
      diff = 8.0;
      i = 1;
      for (;;) { //while (!NULL) {
        if ((factorial(i) * Math.pow((8.0 * beta), i)) > 1.0e250)
          break;
        if (i > 10)
          break;
        if (i == 1)
          prod = my - 1.0;
        else {
          value += diff;
          prod = prod * (my - value);
          diff *= 2.0;
        }
        sum = sum + prod / (factorial(i) * Math.pow((8.0 * beta), i));
        i++;
      }
      erg = c - Math.log(sum);
      return (erg);
    }

    if ((lambda > 0.0) && ((beta - 0.04 * lambda) <= 0.0)) {
      if (lambda < 11.5) {
        erg = -Math.log(gamma(lambda)) - lambda * Math.log(2.0) + lambda * Math.log(beta);
        return (erg);
      } else {
        erg = -(lambda + 1.0) * Math.log(2.0) - (lambda - 0.5) * Math.log(lambda) + lambda + lambda * Math.log(beta) -
            0.5 * Math.log(0.5 * pi);
        return (erg);
      }
    }

    // otherwise numerical integration of the function defined above 

    x = 0.0;

    if (beta < 1.57) {
      fx = (fkt2_value(lambda, beta, x)) * 0.01;
      y = 0.0;
      for (;;) { //while (!NULL) {
        y += 0.1;
        if ((fkt2_value(lambda, beta, y)) < fx)
          break;
      }
      step = y * 0.001;
      x1 = step;
      sum = (0.5 * (10.0 * step + fkt2_value(lambda, beta, x1))) * step;
      first_value = sum;
      for (;;) { //while (!NULL) {
        x = x1;
        x1 += step;
        new_value = (0.5 * (fkt2_value(lambda, beta, x) + fkt2_value(lambda, beta, x1))) * step;
        sum += new_value;
        if ((new_value / first_value) < epsilon)
          break;
      }
      erg = -Math.log(2.0 * sum);
      return (erg);
    } else {
      z2 = 1.57;
      z1 = beta / 1.57;
      sum = 0.0;
      period = pi / z1;
      step = 0.1 * period;
      border = 100.0 / ((lambda + 0.1) * (lambda + 0.1));
      nr_per = (int) Math.ceil((border / period)) + 20;
      x1 = step;
      for (i = 1; i <= nr_per; i++) {
        for (j = 1; j <= 10; j++) {
          new_value = (0.5 * (_fkt_value(lambda, z1, z2, x) + _fkt_value(lambda, z1, z2, x1))) * step;
          sum += new_value;
          x = x1;
          x1 += step;
        }
      }
      for (j = 1; j <= 5; j++) {
        new_value = (0.5 * (_fkt_value(lambda, z1, z2, x) + _fkt_value(lambda, z1, z2, x1))) * step;
        sum += new_value;
        x = x1;
        x1 += step;
      }
      first_sum = sum;
      for (j = 1; j <= 10; j++) {
        new_value = (0.5 * (_fkt_value(lambda, z1, z2, x) + _fkt_value(lambda, z1, z2, x1))) * step;
        sum += new_value;
        x = x1;
        x1 += step;
      }
      second_sum = sum;
      sum = 0.5 * (first_sum + second_sum);
      erg = gamma(lambda + 0.5) * Math.pow((2.0 * z2), lambda) / (Math.sqrt(pi) * Math.pow(z1, lambda)) * sum;
      erg = -Math.log(2.0 * erg);
      return (erg);
    }
  }

  /**
   * Modified Bessel Functions of First Kind - Order 0.
   * @param x x
   * @return result
   */
  public static double bessi0(double x) {
    double ax, ans;
    double y;

    if ((ax = Math.abs(x)) < 3.75) {
      y = x / 3.75;
      y *= y;
      ans = 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.360768e-1 + y * 0.45813e-2)))));
    } else {
      y = 3.75 / ax;
      ans = (Math.exp(ax) / Math.sqrt(ax)) * (0.39894228 + y * (0.1328592e-1 + y * (0.225319e-2 + y * (-0.157565e-2 +
          y * (0.916281e-2 + y * (-0.2057706e-1 + y * (0.2635537e-1 + y * (-0.1647633e-1 + y * 0.392377e-2))))))));
    }
    return ans;
  }

  /**
   * Modified Bessel Functions of First Kind - Order 1.
   * @param x x
   * @return result
   */
  public static double bessi1(double x) {
    double ax, ans;
    double y;

    if ((ax = Math.abs(x)) < 3.75) {
      y = x / 3.75;
      y *= y;
      ans = ax * (0.5 +
          y * (0.87890594 + y * (0.51498869 + y * (0.15084934 + y * (0.2658733e-1 + y * (0.301532e-2 + y * 0.32411e-3))))));
    } else {
      y = 3.75 / ax;
      ans = 0.2282967e-1 + y * (-0.2895312e-1 + y * (0.1787654e-1 - y * 0.420059e-2));
      ans = 0.39894228 + y * (-0.3988024e-1 + y * (-0.362018e-2 + y * (0.163801e-2 + y * (-0.1031555e-1 + y * ans))));
      ans *= (Math.exp(ax) / Math.sqrt(ax));
    }
    return x < 0.0 ? -ans : ans;
  }

  /**
   * Returns <tt>n!</tt>.
   * @param n n
   * @return result
   */
  public static long factorial(int n) {
    return Arithmetic.longFactorial(n);
    /*
    long i,prod;
    
    prod = 1;
    if (n != 0) {
    	for (i = 2; i <= n; i++) prod *= i;
    }
    return prod;
    */
  }

  private static double fkt2_value(double lambda, double beta, double x_value) {
    double y_value;

    y_value = cosh(lambda * x_value) * Math.exp(-beta * cosh(x_value));
    return y_value;
  }

  private static double cosh(double x) {
    return (Math.exp(x) + Math.exp(-x)) / 2.0;
  }

  /**
   * Returns the gamma function <tt>gamma(x)</tt>.
   * @param inX x
   * @return result
   */
  public static double gamma(double inX) {
    double x = logGamma(inX);
    //if (x > Math.log(Double.MAX_VALUE)) return Double.MAX_VALUE;
    return Math.exp(x);
  }

  /**
   * Returns a quick approximation of <tt>log(gamma(x))</tt>.
   * @param inX x
   * @return result
   */
  public static double logGamma(double inX) {
    double x = inX;
    final double c0 = 9.1893853320467274e-01, c1 = 8.3333333333333333e-02,
        c2 = -2.7777777777777777e-03, c3 = 7.9365079365079365e-04,
        c4 = -5.9523809523809524e-04, c5 = 8.4175084175084175e-04,
        c6 = -1.9175269175269175e-03;
    double g, r, z;

    if (x <= 0.0 /* || x > 1.3e19 */ )
      return -999;

    for (z = 1.0; x < 11.0; x++)
      z *= x;

    r = 1.0 / (x * x);
    g = c1 + r * (c2 + r * (c3 + r * (c4 + r * (c5 + r + c6))));
    g = (x - 0.5) * Math.log(x) - x + c0 + g / x;
    if (z == 1.0)
      return g;
    return g - Math.log(z);
  }
}

/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `cern.jet.stat` package.
 * Changes:
 * - package name
 * - added serialization version
 * - missing Javadoc tags
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

// CSOFF: ALL
/**
 * Custom tailored numerical integration of certain probability distributions.
 * <p>
 * <b>Implementation:</b>
 * <dt>
 * Some code taken and adapted from the <A HREF="http://www.sci.usq.edu.au/staff/leighb/graph/Top.html">Java 2D Graph Package 2.4</A>,
 * which in turn is a port from the <A HREF="http://people.ne.mediaone.net/moshier/index.html#Cephes">Cephes 2.2</A> Math Library (C).
 * Most Cephes code (missing from the 2D Graph Package) directly ported.
 *
 * @author peter.gedeck@pharma.Novartis.com
 * @author wolfgang.hoschek@cern.ch
 * @version 0.91, 08-Dec-99
 */
public class Probability extends Constants {
  /*************************************************
   *    COEFFICIENTS FOR METHOD  normalInverse()   *
   *************************************************/
  /* approximation for 0 <= |y - 0.5| <= 3/8 */
  protected static final double P0[] = {
      -5.99633501014107895267E1,
      9.80010754185999661536E1,
      -5.66762857469070293439E1,
      1.39312609387279679503E1,
      -1.23916583867381258016E0,
  };
  protected static final double Q0[] = {
      /* 1.00000000000000000000E0,*/
      1.95448858338141759834E0,
      4.67627912898881538453E0,
      8.63602421390890590575E1,
      -2.25462687854119370527E2,
      2.00260212380060660359E2,
      -8.20372256168333339912E1,
      1.59056225126211695515E1,
      -1.18331621121330003142E0,
  };

  /* Approximation for interval z = sqrt(-2 log y ) between 2 and 8
   * i.e., y between exp(-2) = .135 and exp(-32) = 1.27e-14.
   */
  protected static final double P1[] = {
      4.05544892305962419923E0,
      3.15251094599893866154E1,
      5.71628192246421288162E1,
      4.40805073893200834700E1,
      1.46849561928858024014E1,
      2.18663306850790267539E0,
      -1.40256079171354495875E-1,
      -3.50424626827848203418E-2,
      -8.57456785154685413611E-4,
  };
  protected static final double Q1[] = {
      /*  1.00000000000000000000E0,*/
      1.57799883256466749731E1,
      4.53907635128879210584E1,
      4.13172038254672030440E1,
      1.50425385692907503408E1,
      2.50464946208309415979E0,
      -1.42182922854787788574E-1,
      -3.80806407691578277194E-2,
      -9.33259480895457427372E-4,
  };

  /* Approximation for interval z = sqrt(-2 log y ) between 8 and 64
   * i.e., y between exp(-32) = 1.27e-14 and exp(-2048) = 3.67e-890.
   */
  protected static final double P2[] = {
      3.23774891776946035970E0,
      6.91522889068984211695E0,
      3.93881025292474443415E0,
      1.33303460815807542389E0,
      2.01485389549179081538E-1,
      1.23716634817820021358E-2,
      3.01581553508235416007E-4,
      2.65806974686737550832E-6,
      6.23974539184983293730E-9,
  };
  protected static final double Q2[] = {
      /*  1.00000000000000000000E0,*/
      6.02427039364742014255E0,
      3.67983563856160859403E0,
      1.37702099489081330271E0,
      2.16236993594496635890E-1,
      1.34204006088543189037E-2,
      3.28014464682127739104E-4,
      2.89247864745380683936E-6,
      6.79019408009981274425E-9,
  };

  /**
   * Makes this class non instantiable, but still let's others inherit from it.
   */
  protected Probability() {
  }

  /**
   * Returns the area from zero to <tt>x</tt> under the beta density
   * function.
   * <pre>
   *                          x
   *            -             -
   *           | (a+b)       | |  a-1      b-1
   * P(x)  =  ----------     |   t    (1-t)    dt
   *           -     -     | |
   *          | (a) | (b)   -
   *                         0
   * </pre>
   * This function is identical to the incomplete beta
   * integral function <tt>Gamma.incompleteBeta(a, b, x)</tt>.
   *
   * The complemented function is
   *
   * <tt>1 - P(1-x)  =  Gamma.incompleteBeta( b, a, x )</tt>;
   *
   * @param a a
   * @param b b
   * @param x x
   * @return result
   * @throws ArithmeticException error
   */
  static public double beta(double a, double b, double x) {
    return GammaFunctions.incompleteBeta(a, b, x);
  }

  /**
   * Returns the area under the right hand tail (from <tt>x</tt> to
   * infinity) of the beta density function.
   * 
   * This function is identical to the incomplete beta
   * integral function <tt>Gamma.incompleteBeta(b, a, x)</tt>.
   * 
   * @param a a
   * @param b b
   * @param x x
   * @return result
   * @throws ArithmeticException error
   */
  static public double betaComplemented(double a, double b, double x) {
    return GammaFunctions.incompleteBeta(b, a, x);
  }

  /**
   * Returns the sum of the terms <tt>0</tt> through <tt>k</tt> of the Binomial
   * probability density.
   * <pre>
   *   k
   *   --  ( n )   j      n-j
   *   >   (   )  p  (1-p)
   *   --  ( j )
   *  j=0
   * </pre>
   * The terms are not summed directly; instead the incomplete
   * beta integral is employed, according to the formula
   * <p>
   * <tt>y = binomial( k, n, p ) = Gamma.incompleteBeta( n-k, k+1, 1-p )</tt>.
   * <p>
   * All arguments must be positive, 
   * @param k end term.
   * @param n the number of trials.
   * @param p the probability of success (must be in <tt>(0.0,1.0)</tt>).
   * @return result
   * @throws ArithmeticException error
   */
  static public double binomial(int k, int n, double p) {
    if ((p < 0.0) || (p > 1.0))
      throw new IllegalArgumentException();
    if ((k < 0) || (n < k))
      throw new IllegalArgumentException();

    if (k == n)
      return (1.0);
    if (k == 0)
      return Math.pow(1.0 - p, n - k);

    return GammaFunctions.incompleteBeta(n - k, k + 1, 1.0 - p);
  }

  /**
   * Returns the sum of the terms <tt>k+1</tt> through <tt>n</tt> of the Binomial
   * probability density.
   * <pre>
   *   n
   *   --  ( n )   j      n-j
   *   >   (   )  p  (1-p)
   *   --  ( j )
   *  j=k+1
   * </pre>
   * The terms are not summed directly; instead the incomplete
   * beta integral is employed, according to the formula
   * <p>
   * <tt>y = binomialComplemented( k, n, p ) = Gamma.incompleteBeta( k+1, n-k, p )</tt>.
   * <p>
   * All arguments must be positive, 
   * @param k end term.
   * @param n the number of trials.
   * @param p the probability of success (must be in <tt>(0.0,1.0)</tt>).
   * @return result
   * @throws ArithmeticException error
   */
  static public double binomialComplemented(int k, int n, double p) {
    if ((p < 0.0) || (p > 1.0))
      throw new IllegalArgumentException();
    if ((k < 0) || (n < k))
      throw new IllegalArgumentException();

    if (k == n)
      return (0.0);
    if (k == 0)
      return 1.0 - Math.pow(1.0 - p, n - k);

    return GammaFunctions.incompleteBeta(k + 1, n - k, p);
  }

  /**
   * Returns the area under the left hand tail (from 0 to <tt>x</tt>)
   * of the Chi square probability density function with
   * <tt>v</tt> degrees of freedom.
   * <pre>
   *                                  inf.
   *                                    -
   *                        1          | |  v/2-1  -t/2
   *  P( x | v )   =   -----------     |   t      e     dt
   *                    v/2  -       | |
   *                   2    | (v/2)   -
   *                                   x
   * </pre>
   * where <tt>x</tt> is the Chi-square variable.
   * <p>
   * The incomplete gamma integral is used, according to the
   * formula
   * <p>
   * <tt>y = chiSquare( v, x ) = incompleteGamma( v/2.0, x/2.0 )</tt>.
   * <p>
   * The arguments must both be positive.
   *
   * @param v degrees of freedom.
   * @param x integration end point.
   * @return result
   * @throws ArithmeticException error
   */
  static public double chiSquare(double v, double x) throws ArithmeticException {
    if (x < 0.0 || v < 1.0)
      return 0.0;
    return GammaFunctions.incompleteGamma(v / 2.0, x / 2.0);
  }

  /**
   * Returns the area under the right hand tail (from <tt>x</tt> to
   * infinity) of the Chi square probability density function
   * with <tt>v</tt> degrees of freedom.
   * <pre>
   *                                  inf.
   *                                    -
   *                        1          | |  v/2-1  -t/2
   *  P( x | v )   =   -----------     |   t      e     dt
   *                    v/2  -       | |
   *                   2    | (v/2)   -
   *                                   x
   * </pre>
   * where <tt>x</tt> is the Chi-square variable.
   *
   * The incomplete gamma integral is used, according to the
   * formula
   *
   * <tt>y = chiSquareComplemented( v, x ) = incompleteGammaComplement( v/2.0, x/2.0 )</tt>.
   *
   *
   * The arguments must both be positive.
   *
   * @param v degrees of freedom.
   * @param x x
   * @return result
   * @throws ArithmeticException error
   */
  static public double chiSquareComplemented(double v, double x) throws ArithmeticException {
    if (x < 0.0 || v < 1.0)
      return 0.0;
    return GammaFunctions.incompleteGammaComplement(v / 2.0, x / 2.0);
  }

  /**
   * Returns the error function of the normal distribution; formerly named <tt>erf</tt>.
   * The integral is
   * <pre>
   *                           x 
   *                            -
   *                 2         | |          2
   *   erf(x)  =  --------     |    exp( - t  ) dt.
   *              sqrt(pi)   | |
   *                          -
   *                           0
   * </pre>
   * <b>Implementation:</b>
   * For <tt>0 <= |x| < 1, erf(x) = x * P4(x**2)/Q5(x**2)</tt>; otherwise
   * <tt>erf(x) = 1 - erfc(x)</tt>.
   * <p>
   * Code adapted from the <A HREF="http://www.sci.usq.edu.au/staff/leighb/graph/Top.html">Java 2D Graph Package 2.4</A>,
   * which in turn is a port from the <A HREF="http://people.ne.mediaone.net/moshier/index.html#Cephes">Cephes 2.2</A> Math Library (C).
   *
   * @param x the argument to the function.
   * @return result
   * @throws ArithmeticException error
   */
  static public double errorFunction(double x) throws ArithmeticException {
    double y, z;
    final double T[] = {
        9.60497373987051638749E0,
        9.00260197203842689217E1,
        2.23200534594684319226E3,
        7.00332514112805075473E3,
        5.55923013010394962768E4
    };
    final double U[] = {
        //1.00000000000000000000E0,
        3.35617141647503099647E1,
        5.21357949780152679795E2,
        4.59432382970980127987E3,
        2.26290000613890934246E4,
        4.92673942608635921086E4
    };

    if (Math.abs(x) > 1.0)
      return (1.0 - errorFunctionComplemented(x));
    z = x * x;
    y = x * Polynomial.polevl(z, T, 4) / Polynomial.p1evl(z, U, 5);
    return y;
  }

  /**
   * Returns the complementary Error function of the normal distribution; formerly named <tt>erfc</tt>.
   * <pre>
   *  1 - erf(x) =
   *
   *                           inf. 
   *                             -
   *                  2         | |          2
   *   erfc(x)  =  --------     |    exp( - t  ) dt
   *               sqrt(pi)   | |
   *                           -
   *                            x
   * </pre>
   * <b>Implementation:</b>
   * For small x, <tt>erfc(x) = 1 - erf(x)</tt>; otherwise rational
   * approximations are computed.
   * <p>
   * Code adapted from the <A HREF="http://www.sci.usq.edu.au/staff/leighb/graph/Top.html">Java 2D Graph Package 2.4</A>,
   * which in turn is a port from the <A HREF="http://people.ne.mediaone.net/moshier/index.html#Cephes">Cephes 2.2</A> Math Library (C).
   *
   * @param a the argument to the function.
   * @return result
   * @throws ArithmeticException error
   */
  static public double errorFunctionComplemented(double a) throws ArithmeticException {
    double x, y, z, p, q;

    double P[] = {
        2.46196981473530512524E-10,
        5.64189564831068821977E-1,
        7.46321056442269912687E0,
        4.86371970985681366614E1,
        1.96520832956077098242E2,
        5.26445194995477358631E2,
        9.34528527171957607540E2,
        1.02755188689515710272E3,
        5.57535335369399327526E2
    };
    double Q[] = {
        //1.0
        1.32281951154744992508E1,
        8.67072140885989742329E1,
        3.54937778887819891062E2,
        9.75708501743205489753E2,
        1.82390916687909736289E3,
        2.24633760818710981792E3,
        1.65666309194161350182E3,
        5.57535340817727675546E2
    };

    double R[] = {
        5.64189583547755073984E-1,
        1.27536670759978104416E0,
        5.01905042251180477414E0,
        6.16021097993053585195E0,
        7.40974269950448939160E0,
        2.97886665372100240670E0
    };
    double S[] = {
        //1.00000000000000000000E0, 
        2.26052863220117276590E0,
        9.39603524938001434673E0,
        1.20489539808096656605E1,
        1.70814450747565897222E1,
        9.60896809063285878198E0,
        3.36907645100081516050E0
    };

    if (a < 0.0)
      x = -a;
    else
      x = a;

    if (x < 1.0)
      return 1.0 - errorFunction(a);

    z = -a * a;

    if (z < -MAXLOG) {
      if (a < 0)
        return (2.0);
      else
        return (0.0);
    }

    z = Math.exp(z);

    if (x < 8.0) {
      p = Polynomial.polevl(x, P, 8);
      q = Polynomial.p1evl(x, Q, 8);
    } else {
      p = Polynomial.polevl(x, R, 5);
      q = Polynomial.p1evl(x, S, 6);
    }

    y = (z * p) / q;

    if (a < 0)
      y = 2.0 - y;

    if (y == 0.0) {
      if (a < 0)
        return 2.0;
      else
        return (0.0);
    }

    return y;
  }

  /**
   * Returns the integral from zero to <tt>x</tt> of the gamma probability
   * density function.
   * <pre>
   *                x
   *        b       -
   *       a       | |   b-1  -at
   * y =  -----    |    t    e    dt
   *       -     | |
   *      | (b)   -
   *               0
   * </pre>
   * The incomplete gamma integral is used, according to the
   * relation
   *
   * <tt>y = Gamma.incompleteGamma( b, a*x )</tt>.
   *
   * @param a the paramater a (alpha) of the gamma distribution.
   * @param b the paramater b (beta, lambda) of the gamma distribution.
   * @param x integration end point.
   * @return result
   */
  static public double gamma(double a, double b, double x) {
    if (x < 0.0)
      return 0.0;
    return GammaFunctions.incompleteGamma(b, a * x);
  }

  /**
   * Returns the integral from <tt>x</tt> to infinity of the gamma
   * probability density function:
   * <pre>
   *               inf.
   *        b       -
   *       a       | |   b-1  -at
   * y =  -----    |    t    e    dt
   *       -     | |
   *      | (b)   -
   *               x
   * </pre>
   * The incomplete gamma integral is used, according to the
   * relation
   * <p>
   * y = Gamma.incompleteGammaComplement( b, a*x ).
   *
   * @param a the paramater a (alpha) of the gamma distribution.
   * @param b the paramater b (beta, lambda) of the gamma distribution.
   * @param x integration end point.
   * @return result
   */
  static public double gammaComplemented(double a, double b, double x) {
    if (x < 0.0)
      return 0.0;
    return GammaFunctions.incompleteGammaComplement(b, a * x);
  }

  /**
   * Returns the sum of the terms <tt>0</tt> through <tt>k</tt> of the Negative Binomial Distribution.
   * <pre>
   *   k
   *   --  ( n+j-1 )   n      j
   *   >   (       )  p  (1-p)
   *   --  (   j   )
   *  j=0
   * </pre>
   * In a sequence of Bernoulli trials, this is the probability
   * that <tt>k</tt> or fewer failures precede the <tt>n</tt>-th success.
   * <p>
   * The terms are not computed individually; instead the incomplete
   * beta integral is employed, according to the formula
   * <p>
   * <tt>y = negativeBinomial( k, n, p ) = Gamma.incompleteBeta( n, k+1, p )</tt>.
   *
   * All arguments must be positive, 
   * @param k end term.
   * @param n the number of trials.
   * @param p the probability of success (must be in <tt>(0.0,1.0)</tt>).
   * @return result
   */
  static public double negativeBinomial(int k, int n, double p) {
    if ((p < 0.0) || (p > 1.0))
      throw new IllegalArgumentException();
    if (k < 0)
      return 0.0;

    return GammaFunctions.incompleteBeta(n, k + 1, p);
  }

  /**
   * Returns the sum of the terms <tt>k+1</tt> to infinity of the Negative
   * Binomial distribution.
   * <pre>
   *   inf
   *   --  ( n+j-1 )   n      j
   *   >   (       )  p  (1-p)
   *   --  (   j   )
   *  j=k+1
   * </pre>
   * The terms are not computed individually; instead the incomplete
   * beta integral is employed, according to the formula
   * <p>
   * y = negativeBinomialComplemented( k, n, p ) = Gamma.incompleteBeta( k+1, n, 1-p ).
   *
   * All arguments must be positive, 
   * @param k end term.
   * @param n the number of trials.
   * @param p the probability of success (must be in <tt>(0.0,1.0)</tt>).
   * @return result
   */
  static public double negativeBinomialComplemented(int k, int n, double p) {
    if ((p < 0.0) || (p > 1.0))
      throw new IllegalArgumentException();
    if (k < 0)
      return 0.0;

    return GammaFunctions.incompleteBeta(k + 1, n, 1.0 - p);
  }

  /**
   * Returns the area under the Normal (Gaussian) probability density
   * function, integrated from minus infinity to <tt>x</tt> (assumes mean is zero, variance is one).
   * <pre>
   *                            x
   *                             -
   *                   1        | |          2
   *  normal(x)  = ---------    |    exp( - t /2 ) dt
   *               sqrt(2pi)  | |
   *                           -
   *                          -inf.
   *
   *             =  ( 1 + erf(z) ) / 2
   *             =  erfc(z) / 2
   * </pre>
   * where <tt>z = x/sqrt(2)</tt>.
   * Computation is via the functions <tt>errorFunction</tt> and <tt>errorFunctionComplement</tt>.
   * 
   * @param a a
   * @return result
   * @throws ArithmeticException error
   */
  static public double normal(double a) throws ArithmeticException {
    double x, y, z;

    x = a * SQRTH;
    z = Math.abs(x);

    if (z < SQRTH)
      y = 0.5 + 0.5 * errorFunction(x);
    else {
      y = 0.5 * errorFunctionComplemented(z);
      if (x > 0)
        y = 1.0 - y;
    }

    return y;
  }

  /**
   * Returns the area under the Normal (Gaussian) probability density
   * function, integrated from minus infinity to <tt>x</tt>.
   * <pre>
   *                            x
   *                             -
   *                   1        | |                 2
   *  normal(x)  = ---------    |    exp( - (t-mean) / 2v ) dt
   *               sqrt(2pi*v)| |
   *                           -
   *                          -inf.
   *
   * </pre>
   * where <tt>v = variance</tt>.
   * Computation is via the functions <tt>errorFunction</tt>.
   *
   * @param mean the mean of the normal distribution.
   * @param variance the variance of the normal distribution.
   * @param x the integration limit.
   * @return result
   * @throws ArithmeticException error
   */
  static public double normal(double mean, double variance, double x) throws ArithmeticException {
    if (x > 0)
      return 0.5 + 0.5 * errorFunction((x - mean) / Math.sqrt(2.0 * variance));
    else
      return 0.5 - 0.5 * errorFunction((-(x - mean)) / Math.sqrt(2.0 * variance));
  }

  /**
   * Returns the value, <tt>x</tt>, for which the area under the
   * Normal (Gaussian) probability density function (integrated from
   * minus infinity to <tt>x</tt>) is equal to the argument <tt>y</tt> (assumes mean is zero, variance is one); formerly named <tt>ndtri</tt>.
   * <p>
   * For small arguments <tt>0 < y < exp(-2)</tt>, the program computes
   * <tt>z = sqrt( -2.0 * log(y) )</tt>;  then the approximation is
   * <tt>x = z - log(z)/z  - (1/z) P(1/z) / Q(1/z)</tt>.
   * There are two rational functions P/Q, one for <tt>0 < y < exp(-32)</tt>
   * and the other for <tt>y</tt> up to <tt>exp(-2)</tt>. 
   * For larger arguments,
   * <tt>w = y - 0.5</tt>, and  <tt>x/sqrt(2pi) = w + w**3 R(w**2)/S(w**2))</tt>.
   *
   * @param y0 y
   * @return result
   * @throws ArithmeticException error
   */
  static public double normalInverse(double y0) throws ArithmeticException {
    double x, y, z, y2, x0, x1;
    int code;

    final double s2pi = Math.sqrt(2.0 * Math.PI);

    if (y0 <= 0.0)
      throw new IllegalArgumentException();
    if (y0 >= 1.0)
      throw new IllegalArgumentException();
    code = 1;
    y = y0;
    if (y > (1.0 - 0.13533528323661269189)) { /* 0.135... = exp(-2) */
      y = 1.0 - y;
      code = 0;
    }

    if (y > 0.13533528323661269189) {
      y = y - 0.5;
      y2 = y * y;
      x = y + y * (y2 * Polynomial.polevl(y2, P0, 4) / Polynomial.p1evl(y2, Q0, 8));
      x = x * s2pi;
      return (x);
    }

    x = Math.sqrt(-2.0 * Math.log(y));
    x0 = x - Math.log(x) / x;

    z = 1.0 / x;
    if (x < 8.0) /* y > exp(-32) = 1.2664165549e-14 */
      x1 = z * Polynomial.polevl(z, P1, 8) / Polynomial.p1evl(z, Q1, 8);
    else
      x1 = z * Polynomial.polevl(z, P2, 8) / Polynomial.p1evl(z, Q2, 8);
    x = x0 - x1;
    if (code != 0)
      x = -x;
    return (x);
  }

  /**
   * Returns the sum of the first <tt>k</tt> terms of the Poisson distribution.
   * <pre>
   *   k         j
   *   --   -m  m
   *   >   e    --
   *   --       j!
   *  j=0
   * </pre>
   * The terms are not summed directly; instead the incomplete
   * gamma integral is employed, according to the relation
   * <p>
   * <tt>y = poisson( k, m ) = Gamma.incompleteGammaComplement( k+1, m )</tt>.
   *
   * The arguments must both be positive.
   *
   * @param k number of terms.
   * @param mean the mean of the poisson distribution.
   * @return result
   * @throws ArithmeticException error
   */
  static public double poisson(int k, double mean) throws ArithmeticException {
    if (mean < 0)
      throw new IllegalArgumentException();
    if (k < 0)
      return 0.0;
    return GammaFunctions.incompleteGammaComplement((double) (k + 1), mean);
  }

  /**
   * Returns the sum of the terms <tt>k+1</tt> to <tt>Infinity</tt> of the Poisson distribution.
   * <pre>
   *  inf.       j
   *   --   -m  m
   *   >   e    --
   *   --       j!
   *  j=k+1
   * </pre>
   * The terms are not summed directly; instead the incomplete
   * gamma integral is employed, according to the formula
   * <p>
   * <tt>y = poissonComplemented( k, m ) = Gamma.incompleteGamma( k+1, m )</tt>.
   *
   * The arguments must both be positive.
   *
   * @param k start term.
   * @param mean the mean of the poisson distribution.
   * @return result
   * @throws ArithmeticException error
   */
  static public double poissonComplemented(int k, double mean) throws ArithmeticException {
    if (mean < 0)
      throw new IllegalArgumentException();
    if (k < -1)
      return 0.0;
    return GammaFunctions.incompleteGamma((double) (k + 1), mean);
  }

  /**
   * Returns the integral from minus infinity to <tt>t</tt> of the Student-t 
   * distribution with <tt>k &gt; 0</tt> degrees of freedom.
   * <pre>
   *                                      t
   *                                      -
   *                                     | |
   *              -                      |         2   -(k+1)/2
   *             | ( (k+1)/2 )           |  (     x   )
   *       ----------------------        |  ( 1 + --- )        dx
   *                     -               |  (      k  )
   *       sqrt( k pi ) | ( k/2 )        |
   *                                   | |
   *                                    -
   *                                   -inf.
   * </pre>
   * Relation to incomplete beta integral:
   * <p>
   * <tt>1 - studentT(k,t) = 0.5 * Gamma.incompleteBeta( k/2, 1/2, z )</tt>
   * where <tt>z = k/(k + t**2)</tt>.
   * <p>
   * Since the function is symmetric about <tt>t=0</tt>, the area under the
   * right tail of the density is found by calling the function
   * with <tt>-t</tt> instead of <tt>t</tt>.
   *
   * @param k degrees of freedom.
   * @param t integration end point.
   * @return result
   * @throws ArithmeticException error
   */
  static public double studentT(double k, double t) throws ArithmeticException {
    if (k <= 0)
      throw new IllegalArgumentException();
    if (t == 0)
      return (0.5);

    double cdf = 0.5 * GammaFunctions.incompleteBeta(0.5 * k, 0.5, k / (k + t * t));

    if (t >= 0)
      cdf = 1.0 - cdf; // fixes bug reported by stefan.bentink@molgen.mpg.de

    return cdf;
  }

  /**
   * Returns the value, <tt>t</tt>, for which the area under the
   * Student-t probability density function (integrated from
   * minus infinity to <tt>t</tt>) is equal to <tt>1-alpha/2</tt>.
   * The value returned corresponds to usual Student t-distribution lookup
   * table for <tt>t<sub>alpha[size]</sub></tt>.
   * <p>
   * The function uses the studentT function to determine the return
   * value iteratively.
   *
   * @param alpha probability
   * @param size size of data set
   * @return result
   * @throws ArithmeticException error
   */
  public static double studentTInverse(double alpha, int size) {
    double cumProb = 1 - alpha / 2; // Cumulative probability
    double f1, f2, f3;
    double x1, x2, x3;
    double g, s12;

    cumProb = 1 - alpha / 2; // Cumulative probability
    x1 = normalInverse(cumProb);

    // Return inverse of normal for large size
    if (size > 200) {
      return x1;
    }

    // Find a pair of x1,x2 that braket zero
    f1 = studentT(size, x1) - cumProb;
    x2 = x1;
    f2 = f1;
    do {
      if (f1 > 0) {
        x2 = x2 / 2;
      } else {
        x2 = x2 + x1;
      }
      f2 = studentT(size, x2) - cumProb;
    } while (f1 * f2 > 0);

    // Find better approximation
    // Pegasus-method
    do {
      // Calculate slope of secant and t value for which it is 0.
      s12 = (f2 - f1) / (x2 - x1);
      x3 = x2 - f2 / s12;

      // Calculate function value at x3
      f3 = studentT(size, x3) - cumProb;
      if (Math.abs(f3) < 1e-8) { // This criteria needs to be very tight!
        // We found a perfect value -> return
        return x3;
      }

      if (f3 * f2 < 0) {
        x1 = x2;
        f1 = f2;
        x2 = x3;
        f2 = f3;
      } else {
        g = f2 / (f2 + f3);
        f1 = g * f1;
        x2 = x3;
        f2 = f3;
      }
    } while (Math.abs(x2 - x1) > 0.001);

    if (Math.abs(f2) <= Math.abs(f1)) {
      return x2;
    } else {
      return x1;
    }
  }
}

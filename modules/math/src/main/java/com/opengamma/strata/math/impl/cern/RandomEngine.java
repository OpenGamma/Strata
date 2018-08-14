/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `cern.jet.random.engine` package.
 * Changes:
 * - package name
 * - added serialization version
 * - missing Javadoc tags
 * - reformat
 * - use Java 8 function interfaces
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

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;

//CSOFF: ALL
/**
 * Abstract base class for uniform pseudo-random number generating engines.
 * <p>
 * Most probability distributions are obtained by using a <b>uniform</b> pseudo-random number generation engine 
 * followed by a transformation to the desired distribution.
 * Thus, subclasses of this class are at the core of computational statistics, simulations, Monte Carlo methods, etc.
 * <p>
 * Subclasses produce uniformly distributed <tt>int</tt>'s and <tt>long</tt>'s in the closed intervals <tt>[Integer.MIN_VALUE,Integer.MAX_VALUE]</tt> and <tt>[Long.MIN_VALUE,Long.MAX_VALUE]</tt>, respectively, 
 * as well as <tt>float</tt>'s and <tt>double</tt>'s in the open unit intervals <tt>(0.0f,1.0f)</tt> and <tt>(0.0,1.0)</tt>, respectively.
 * <p>
 * Subclasses need to override one single method only: <tt>nextInt()</tt>.
 * All other methods generating different data types or ranges are usually layered upon <tt>nextInt()</tt>.
 * <tt>long</tt>'s are formed by concatenating two 32 bit <tt>int</tt>'s.
 * <tt>float</tt>'s are formed by dividing the interval <tt>[0.0f,1.0f]</tt> into 2<sup>32</sup> sub intervals, then randomly choosing one subinterval.
 * <tt>double</tt>'s are formed by dividing the interval <tt>[0.0,1.0]</tt> into 2<sup>64</sup> sub intervals, then randomly choosing one subinterval.
 * <p>
 * Note that this implementation is <b>not synchronized</b>.
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * @see MersenneTwister
 * @see MersenneTwister64
 * @see java.util.Random
 */
//public abstract class RandomEngine extends edu.cornell.lassp.houle.RngPack.RandomSeedable implements cern.colt.function.DoubleFunction, cern.colt.function.IntFunction {
public abstract class RandomEngine extends PersistentObject
    implements DoubleUnaryOperator, IntUnaryOperator {

  private static final long serialVersionUID = 1L;

  /**
   * Makes this class non instantiable, but still let's others inherit from it.
   */
  protected RandomEngine() {
  }

  /**
   * Equivalent to <tt>raw()</tt>.
   * This has the effect that random engines can now be used as function objects, returning a random number upon function evaluation.
   */
  @Override
  public double applyAsDouble(double dummy) {
    return raw();
  }

  /**
   * Equivalent to <tt>nextInt()</tt>.
   * This has the effect that random engines can now be used as function objects, returning a random number upon function evaluation.
   */
  @Override
  public int applyAsInt(int dummy) {
    return nextInt();
  }

  /**
   * Constructs and returns a new uniform random number engine seeded with the current time.
   * Currently this is {@link MersenneTwister}.
   * @return the engine
   */
  public static RandomEngine makeDefault() {
    return new MersenneTwister((int) System.currentTimeMillis());
  }

  /**
   * Returns a 64 bit uniformly distributed random number in the open unit interval <code>(0.0,1.0)</code> (excluding 0.0 and 1.0).
   * @return the random number
   */
  public double nextDouble() {
    double nextDouble;

    do {
      // -9.223372036854776E18 == (double) Long.MIN_VALUE
      // 5.421010862427522E-20 == 1 / Math.pow(2,64) == 1 / ((double) Long.MAX_VALUE - (double) Long.MIN_VALUE);
      nextDouble = ((double) nextLong() - -9.223372036854776E18) * 5.421010862427522E-20;
    }
    // catch loss of precision of long --> double conversion
    while (!(nextDouble > 0.0 && nextDouble < 1.0));

    // --> in (0.0,1.0)
    return nextDouble;

    /*
    	nextLong == Long.MAX_VALUE         --> 1.0
    	nextLong == Long.MIN_VALUE         --> 0.0
    	nextLong == Long.MAX_VALUE-1       --> 1.0
    	nextLong == Long.MAX_VALUE-100000L --> 0.9999999999999946
    	nextLong == Long.MIN_VALUE+1       --> 0.0
    	nextLong == Long.MIN_VALUE-100000L --> 0.9999999999999946
    	nextLong == 1L                     --> 0.5
    	nextLong == -1L                    --> 0.5
    	nextLong == 2L                     --> 0.5
    	nextLong == -2L                    --> 0.5
    	nextLong == 2L+100000L             --> 0.5000000000000054
    	nextLong == -2L-100000L            --> 0.49999999999999456
    */
  }

  /**
   * Returns a 32 bit uniformly distributed random number in the open unit interval <code>(0.0f,1.0f)</code> (excluding 0.0f and 1.0f).
   * @return the random number
   */
  public float nextFloat() {
    // catch loss of precision of double --> float conversion
    float nextFloat;
    do {
      nextFloat = (float) raw();
    } while (nextFloat >= 1.0f);

    // --> in (0.0f,1.0f)
    return nextFloat;
  }

  /**
   * Returns a 32 bit uniformly distributed random number in the closed interval <tt>[Integer.MIN_VALUE,Integer.MAX_VALUE]</tt> (including <tt>Integer.MIN_VALUE</tt> and <tt>Integer.MAX_VALUE</tt>);
   * @return the random number
   */
  public abstract int nextInt();

  /**
   * Returns a 64 bit uniformly distributed random number in the closed interval <tt>[Long.MIN_VALUE,Long.MAX_VALUE]</tt> (including <tt>Long.MIN_VALUE</tt> and <tt>Long.MAX_VALUE</tt>).
   * @return the random number
   */
  public long nextLong() {
    // concatenate two 32-bit strings into one 64-bit string
    return ((nextInt() & 0xFFFFFFFFL) << 32) | ((nextInt() & 0xFFFFFFFFL));
  }

  /**
   * Returns a 32 bit uniformly distributed random number in the open unit interval <code>(0.0,1.0)</code> (excluding 0.0 and 1.0).
   * @return the random number
   */
  public double raw() {
    int nextInt;
    do { // accept anything but zero
      nextInt = nextInt(); // in [Integer.MIN_VALUE,Integer.MAX_VALUE]-interval
    } while (nextInt == 0);

    // transform to (0.0,1.0)-interval
    // 2.3283064365386963E-10 == 1.0 / Math.pow(2,32)
    return (double) (nextInt & 0xFFFFFFFFL) * 2.3283064365386963E-10;

    /*
    	nextInt == Integer.MAX_VALUE   --> 0.49999999976716936
    	nextInt == Integer.MIN_VALUE   --> 0.5
    	nextInt == Integer.MAX_VALUE-1 --> 0.4999999995343387
    	nextInt == Integer.MIN_VALUE+1 --> 0.5000000002328306
    	nextInt == 1                   --> 2.3283064365386963E-10
    	nextInt == -1                  --> 0.9999999997671694
    	nextInt == 2                   --> 4.6566128730773926E-10
    	nextInt == -2                  --> 0.9999999995343387
    */
  }
}

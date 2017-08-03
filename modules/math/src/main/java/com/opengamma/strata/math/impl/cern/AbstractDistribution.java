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
 * - use Java 8 function interfaces
 * - make package scoped
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
 * Abstract base class for all random distributions.
 *
 * A subclass of this class need to override method <tt>nextDouble()</tt> and, in rare cases, also <tt>nextInt()</tt>.
 * <p>
 * Currently all subclasses use a uniform pseudo-random number generation engine and transform its results to the target distribution.
 * Thus, they expect such a uniform engine upon instance construction.
 * <p>
 * {@link MersenneTwister} is recommended as uniform pseudo-random number generation engine, since it is very strong and at the same time quick.
 * {@link #makeDefaultGenerator()} will conveniently construct and return such a magic thing.
 * You can also, for example, use {@code DRand}, a quicker (but much weaker) uniform random number generation engine.
 * Of course, you can also use other strong uniform random number generation engines. 
 *
 * <p>
 * <b>Ressources on the Web:</b>
 * <dt>Check the Web version of the <A HREF="http://www.cern.ch/RD11/rkb/AN16pp/node1.html"> CERN Data Analysis Briefbook </A>. This will clarify the definitions of most distributions.
 * <dt>Also consult the <A HREF="http://www.statsoftinc.com/textbook/stathome.html"> StatSoft Electronic Textbook</A> - the definite web book.
 * <p>
 * <b>Other useful ressources:</b>
 * <dt><A HREF="http://www.stats.gla.ac.uk/steps/glossary/probability_distributions.html"> Another site </A> and <A HREF="http://www.statlets.com/usermanual/glossary.htm"> yet another site </A>describing the definitions of several distributions.
 * <dt>You may want to check out a <A HREF="http://www.stat.berkeley.edu/users/stark/SticiGui/Text/gloss.htm"> Glossary of Statistical Terms</A>.
 * <dt>The GNU Scientific Library contains an extensive (but hardly readable) <A HREF="http://sourceware.cygnus.com/gsl/html/gsl-ref_toc.html#TOC26"> list of definition of distributions</A>.
 * <dt>Use this Web interface to <A HREF="http://www.stat.ucla.edu/calculators/cdf"> plot all sort of distributions</A>.
 * <dt>Even more ressources: <A HREF="http://www.animatedsoftware.com/statglos/statglos.htm"> Internet glossary of Statistical Terms</A>,
 * <A HREF="http://www.ruf.rice.edu/~lane/hyperstat/index.html"> a text book</A>,
 * <A HREF="http://www.stat.umn.edu/~jkuhn/courses/stat3091f/stat3091f.html"> another text book</A>.
 * <dt>Finally, a good link list <A HREF="http://www.execpc.com/~helberg/statistics.html"> Statistics on the Web</A>.
 * <p>
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
abstract class AbstractDistribution
    extends PersistentObject
    implements DoubleUnaryOperator, IntUnaryOperator {

  private static final long serialVersionUID = 1L;

  protected RandomEngine randomGenerator;

  /**
   * Makes this class non instantiable, but still let's others inherit from it.
   */
  protected AbstractDistribution() {
  }

  /**
  Equivalent to <tt>nextDouble()</tt>.
  This has the effect that distributions can now be used as function objects, returning a random number upon function evaluation.
  */
  @Override
  public double applyAsDouble(double dummy) {
    return nextDouble();
  }

  /**
  Equivalent to <tt>nextInt()</tt>.
  This has the effect that distributions can now be used as function objects, returning a random number upon function evaluation.
  */
  @Override
  public int applyAsInt(int dummy) {
    return nextInt();
  }

  /**
   * Returns a deep copy of the receiver; the copy will produce identical sequences.
   * After this call has returned, the copy and the receiver have equal but separate state.
   *
   * @return a copy of the receiver.
   */
  @Override
  public Object clone() {
    AbstractDistribution copy = (AbstractDistribution) super.clone();
    if (this.randomGenerator != null)
      copy.randomGenerator = (RandomEngine) this.randomGenerator.clone();
    return copy;
  }

  /**
   * Returns the used uniform random number generator;
   * @return result
   */
  protected RandomEngine getRandomGenerator() {
    return randomGenerator;
  }

  /**
   * Constructs and returns a new uniform random number generation engine seeded with the current time.
   * Currently this is {@link MersenneTwister}.
   * @return result
   */
  public static RandomEngine makeDefaultGenerator() {
    return RandomEngine.makeDefault();
  }

  /**
   * Returns a random number from the distribution.
   * @return result
   */
  public abstract double nextDouble();

  /**
   * Returns a random number from the distribution; returns <tt>(int) Math.round(nextDouble())</tt>.
   * Override this method if necessary.
   * @return result
   */
  public int nextInt() {
    return (int) Math.round(nextDouble());
  }

  /**
   * Sets the uniform random generator internally used.
   * @param randomGenerator input
   */
  protected void setRandomGenerator(RandomEngine randomGenerator) {
    this.randomGenerator = randomGenerator;
  }
}

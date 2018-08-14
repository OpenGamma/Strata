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

import java.util.Date;

// CSOFF: ALL
/**
MersenneTwister (MT19937) is one of the strongest uniform pseudo-random number generators known so far; at the same time it is quick.
Produces uniformly distributed <tt>int</tt>'s and <tt>long</tt>'s in the closed intervals <tt>[Integer.MIN_VALUE,Integer.MAX_VALUE]</tt> and <tt>[Long.MIN_VALUE,Long.MAX_VALUE]</tt>, respectively, 
as well as <tt>float</tt>'s and <tt>double</tt>'s in the open unit intervals <tt>(0.0f,1.0f)</tt> and <tt>(0.0,1.0)</tt>, respectively.
The seed can be any 32-bit integer except <tt>0</tt>. Shawn J. Cokus commented that perhaps the seed should preferably be odd.
<p>
<b>Quality:</b> MersenneTwister is designed to pass the k-distribution test. It has an astronomically large period of 2<sup>19937</sup>-1 (=10<sup>6001</sup>) and 623-dimensional equidistribution up to 32-bit accuracy.
It passes many stringent statistical tests, including the <A HREF="http://stat.fsu.edu/~geo/diehard.html">diehard</A> test of G. Marsaglia and the load test of P. Hellekalek and S. Wegenkittl.
<p>
<b>Performance:</b> Its speed is comparable to other modern generators (in particular, as fast as <tt>java.util.Random.nextFloat()</tt>).
2.5 million calls to <tt>raw()</tt> per second (Pentium Pro 200 Mhz, JDK 1.2, NT).
Be aware, however, that there is a non-negligible amount of overhead required to initialize the data
structures used by a MersenneTwister. Code like
<pre>
  double sum = 0.0;
  for (int i=0; i<100000; ++i) {
     RandomElement twister = new MersenneTwister(new java.util.Date());
     sum += twister.raw();
  }
</pre>
will be wildly inefficient. Consider using
<pre>
  double sum = 0.0;
  RandomElement twister = new MersenneTwister(new java.util.Date());
  for (int i=0; i<100000; ++i) {
     sum += twister.raw();
  }
</pre>
instead.  This allows the cost of constructing the MersenneTwister object
to be borne only once, rather than once for each iteration in the loop.
<p>
<b>Implementation:</b> After M. Matsumoto and T. Nishimura,                                  
"Mersenne Twister: A 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator",                                
ACM Transactions on Modeling and Computer Simulation,           
Vol. 8, No. 1, January 1998, pp 3--30.
<dt>More info on <A HREF="http://www.math.keio.ac.jp/~matumoto/eindex.html"> Masumoto's homepage</A>.
<dt>More info on <A HREF="http://www.ncsa.uiuc.edu/Apps/CMP/RNG/www-rng.html"> Pseudo-random number generators is on the Web</A>.
<dt>Yet <A HREF="http://nhse.npac.syr.edu/random"> some more info</A>.
<p>
The correctness of this implementation has been verified against the published output sequence 
<a href="http://www.math.keio.ac.jp/~nisimura/random/real2/mt19937-2.out">mt19937-2.out</a> of the C-implementation
<a href="http://www.math.keio.ac.jp/~nisimura/random/real2/mt19937-2.c">mt19937-2.c</a>.
(Call <tt>test(1000)</tt> to print the sequence).
<dt>
Note that this implementation is <b>not synchronized</b>.                                  
<p>
<b>Details:</b> MersenneTwister is designed with consideration of the flaws of various existing generators in mind.
It is an improved version of TT800, a very successful generator.
MersenneTwister is based on linear recurrences modulo 2.
Such generators are very fast, have extremely long periods, and appear quite robust. 
MersenneTwister produces 32-bit numbers, and every <tt>k</tt>-dimensional vector of such numbers appears the same number of times as <tt>k</tt> successive values over the
period length, for each <tt>k &lt;= 623</tt> (except for the zero vector, which appears one time less).
If one looks at only the first <tt>n &lt;= 16</tt> bits of each number, then the property holds for even larger <tt>k</tt>, as shown in the following table (taken from the publication cited above):
<div align="center">
<table width="75%" border="1" cellspacing="0" cellpadding="0">
  <tr> 
  <td width="2%"> <div align="center">n</div> </td>
  <td width="6%"> <div align="center">1</div> </td>
  <td width="5%"> <div align="center">2</div> </td>
  <td width="5%"> <div align="center">3</div> </td>
  <td width="5%"> <div align="center">4</div> </td>
  <td width="5%"> <div align="center">5</div> </td>
  <td width="5%"> <div align="center">6</div> </td>
  <td width="5%"> <div align="center">7</div> </td>
  <td width="5%"> <div align="center">8</div> </td>
  <td width="5%"> <div align="center">9</div> </td>
  <td width="5%"> <div align="center">10</div> </td>
  <td width="5%"> <div align="center">11</div> </td>
  <td width="10%"> <div align="center">12 .. 16</div> </td>
  <td width="10%"> <div align="center">17 .. 32</div> </td>
  </tr>
  <tr> 
  <td width="2%"> <div align="center">k</div> </td>
  <td width="6%"> <div align="center">19937</div> </td>
  <td width="5%"> <div align="center">9968</div> </td>
  <td width="5%"> <div align="center">6240</div> </td>
  <td width="5%"> <div align="center">4984</div> </td>
  <td width="5%"> <div align="center">3738</div> </td>
  <td width="5%"> <div align="center">3115</div> </td>
  <td width="5%"> <div align="center">2493</div> </td>
  <td width="5%"> <div align="center">2492</div> </td>
  <td width="5%"> <div align="center">1869</div> </td>
  <td width="5%"> <div align="center">1869</div> </td>
  <td width="5%"> <div align="center">1248</div> </td>
  <td width="10%"> <div align="center">1246</div> </td>
  <td width="10%"> <div align="center">623</div> </td>
  </tr>
</table>
</div>
<p>
MersenneTwister generates random numbers in batches of 624 numbers at a time, so the caching and pipelining of modern systems is exploited.
The generator is implemented to generate the output by using the fastest arithmetic operations only: 32-bit additions and bit operations (no division, no multiplication, no mod).
These operations generate sequences of 32 random bits (<tt>int</tt>'s).
<tt>long</tt>'s are formed by concatenating two 32 bit <tt>int</tt>'s.
<tt>float</tt>'s are formed by dividing the interval <tt>[0.0,1.0]</tt> into 2<sup>32</sup> sub intervals, then randomly choosing one subinterval.
<tt>double</tt>'s are formed by dividing the interval <tt>[0.0,1.0]</tt> into 2<sup>64</sup> sub intervals, then randomly choosing one subinterval.
<p>
@author wolfgang.hoschek@cern.ch
@version 1.0, 09/24/99
@see java.util.Random
*/
public class MersenneTwister extends RandomEngine {
  private static final long serialVersionUID = 1L;

  private int mti;
  private int[] mt = new int[N]; /* set initial seeds: N = 624 words */

  /* Period parameters */
  private static final int N = 624;
  private static final int M = 397;
  private static final int MATRIX_A = 0x9908b0df; /* constant vector a */
  private static final int UPPER_MASK = 0x80000000; /* most significant w-r bits */
  private static final int LOWER_MASK = 0x7fffffff; /* least significant r bits */

  /* for tempering */
  private static final int TEMPERING_MASK_B = 0x9d2c5680;
  private static final int TEMPERING_MASK_C = 0xefc60000;

  private static final int mag0 = 0x0;
  private static final int mag1 = MATRIX_A;
  //private static final int[] mag01=new int[] {0x0, MATRIX_A};
  /* mag01[x] = x * MATRIX_A  for x=0,1 */

  public static final int DEFAULT_SEED = 4357;

  /**
   * Constructs and returns a random number generator with a default seed, which is a <b>constant</b>.
   * Thus using this constructor will yield generators that always produce exactly the same sequence.
   * This method is mainly intended to ease testing and debugging.
   */
  public MersenneTwister() {
    this(DEFAULT_SEED);
  }

  /**
   * Constructs and returns a random number generator with the given seed.
   * @param seed the seed
   */
  public MersenneTwister(int seed) {
    setSeed(seed);
  }

  /**
   * Constructs and returns a random number generator seeded with the given date.
   *
   * @param d typically <tt>new java.util.Date()</tt>
   */
  public MersenneTwister(Date d) {
    this((int) d.getTime());
  }

  /**
   * Returns a copy of the receiver; the copy will produce identical sequences.
   * After this call has returned, the copy and the receiver have equal but separate state.
   *
   * @return a copy of the receiver.
   */
  @Override
  public Object clone() {
    MersenneTwister clone = (MersenneTwister) super.clone();
    clone.mt = (int[]) this.mt.clone();
    return clone;
  }

  /**
   * Generates N words at one time.
   */
  protected void nextBlock() {
    /*
    // ******************** OPTIMIZED **********************
    // only 5-10% faster ?
    int y;
    
    int kk;
    int[] cache = mt; // cached for speed
    int kkM;
    int limit = N-M;
    for (kk=0,kkM=kk+M; kk<limit; kk++,kkM++) {
    y = (cache[kk]&UPPER_MASK)|(cache[kk+1]&LOWER_MASK);
    cache[kk] = cache[kkM] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
    }
    limit = N-1;
    for (kkM=kk+(M-N); kk<limit; kk++,kkM++) {
    y = (cache[kk]&UPPER_MASK)|(cache[kk+1]&LOWER_MASK);
    cache[kk] = cache[kkM] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
    }
    y = (cache[N-1]&UPPER_MASK)|(cache[0]&LOWER_MASK);
    cache[N-1] = cache[M-1] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
    
    this.mt = cache;
    this.mti = 0;
    */

    // ******************** UNOPTIMIZED **********************
    int y;

    int kk;

    for (kk = 0; kk < N - M; kk++) {
      y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
      mt[kk] = mt[kk + M] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
    }
    for (; kk < N - 1; kk++) {
      y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
      mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
    }
    y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
    mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);

    this.mti = 0;

  }

  /**
   * Returns a 32 bit uniformly distributed random number in the closed interval <tt>[Integer.MIN_VALUE,Integer.MAX_VALUE]</tt> (including <tt>Integer.MIN_VALUE</tt> and <tt>Integer.MAX_VALUE</tt>).
   */
  @Override
  public int nextInt() {
    /* Each single bit including the sign bit will be random */
    if (mti == N)
      nextBlock(); // generate N ints at one time

    int y = mt[mti++];
    y ^= y >>> 11; // y ^= TEMPERING_SHIFT_U(y );
    y ^= (y << 7) & TEMPERING_MASK_B; // y ^= TEMPERING_SHIFT_S(y) & TEMPERING_MASK_B;
    y ^= (y << 15) & TEMPERING_MASK_C; // y ^= TEMPERING_SHIFT_T(y) & TEMPERING_MASK_C; 
    // y &= 0xffffffff; //you may delete this line if word size = 32 
    y ^= y >>> 18; // y ^= TEMPERING_SHIFT_L(y);

    return y;
  }

  /**
   * Sets the receiver's seed. 
   * This method resets the receiver's entire internal state.
   * @param seed the seed
   */
  protected void setSeed(int seed) {
    mt[0] = seed & 0xffffffff;
    for (int i = 1; i < N; i++) {
      mt[i] = (1812433253 * (mt[i - 1] ^ (mt[i - 1] >> 30)) + i);
      /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
      /* In the previous versions, MSBs of the seed affect   */
      /* only MSBs of the array mt[].                        */
      /* 2002/01/09 modified by Makoto Matsumoto             */
      mt[i] &= 0xffffffff;
      /* for >32 bit machines */
    }
    //System.out.println("init done");
    mti = N;

    /*
    old version was:  
    for (int i = 0; i < N; i++) {
    mt[i] = seed & 0xffff0000;
    seed = 69069 * seed + 1;
    mt[i] |= (seed & 0xffff0000) >>> 16;
    seed = 69069 * seed + 1;
    }
    //System.out.println("init done");
    mti = N;
    */
  }
}

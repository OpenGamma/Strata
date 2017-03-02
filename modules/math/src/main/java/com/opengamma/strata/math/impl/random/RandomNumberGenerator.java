/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.random;

import java.util.List;

/**
 * Generator of random numbers.
 */
public interface RandomNumberGenerator {

  /**
   * Gets an array of random numbers.
   * 
   * @param size  the size of the resulting array
   * @return the array of random numbers
   */
  double[] getVector(int size);

  /**
   * Gets a list of random number arrays.
   * 
   * @param arraySize  the size of each resulting array
   * @param listSize  the size of the list
   * @return the list of random number arrays
   */
  List<double[]> getVectors(int arraySize, int listSize);

}

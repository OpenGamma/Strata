/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.random;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

import cern.jet.random.engine.RandomEngine;

/**
 * Random number generator based on {@code ProbabilityDistribution}. 
 */
public class NormalRandomNumberGenerator
    implements RandomNumberGenerator {

  /**
   * The underlying distribution.
   */
  private final ProbabilityDistribution<Double> normal;

  /**
   * Creates an instance.
   * 
   * @param mean  the mean
   * @param sigma  the sigma
   */
  public NormalRandomNumberGenerator(double mean, double sigma) {
    ArgChecker.notNegativeOrZero(sigma, "standard deviation");
    this.normal = new NormalDistribution(mean, sigma);
  }

  /**
   * Creates an instance.
   * 
   * @param mean  the mean
   * @param sigma  the sigma
   * @param engine  the random number engine
   */
  public NormalRandomNumberGenerator(double mean, double sigma, RandomEngine engine) {
    ArgChecker.notNegativeOrZero(sigma, "standard deviation");
    ArgChecker.notNull(engine, "engine");
    this.normal = new NormalDistribution(mean, sigma, engine);
  }

  //-------------------------------------------------------------------------
  @Override
  public double[] getVector(int size) {
    ArgChecker.notNegative(size, "size");
    double[] result = new double[size];
    for (int i = 0; i < size; i++) {
      result[i] = normal.nextRandom();
    }
    return result;
  }

  @Override
  public List<double[]> getVectors(int arraySize, int listSize) {
    ArgChecker.notNegative(arraySize, "arraySize");
    ArgChecker.notNegative(listSize, "listSize");
    List<double[]> result = new ArrayList<>(listSize);
    double[] x;
    for (int i = 0; i < listSize; i++) {
      x = new double[arraySize];
      for (int j = 0; j < arraySize; j++) {
        x[j] = normal.nextRandom();
      }
      result.add(x);
    }
    return result;
  }

}

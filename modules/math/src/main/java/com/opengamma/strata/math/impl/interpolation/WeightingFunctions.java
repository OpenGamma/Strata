/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

/**
 * Constants and implementations for standard weighting functions.
 * <p>
 * Each constant returns a standard weighting function.
 */
public final class WeightingFunctions {

  /**
   * Weighting function.
   */
  public static final WeightingFunction LINEAR = LinearWeightingFunction.INSTANCE;
  /**
   * Weighting function based on {@code Math.sin}.
   */
  public static final WeightingFunction SINE = SineWeightingFunction.INSTANCE;

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private WeightingFunctions() {
  }

}

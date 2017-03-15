/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

/**
 * A data bundle of a volatility model.
 * <p>
 * An implementation contains the data required for a volatility model.
 * This is used with {@link VolatilityFunctionProvider}. 
 */
public interface SmileModelData {

  /**
   * Obtains the number of model parameters.
   * 
   * @return the number of model parameters
   */
  public abstract int getNumberOfParameters();

  /**
   * Obtains a model parameter specified by the index.
   * 
   * @param index  the index
   * @return the model parameter
   */
  public abstract double getParameter(int index);

  /**
   * Checks the value satisfies the constraint for a model parameter.
   * <p>
   * The parameter is specified by {@code index}.
   * 
   * @param index  the index 
   * @param value  the value
   * @return true if allowed, false otherwise
   */
  public abstract boolean isAllowed(int index, double value);

  /**
   * Creates a new smile model data bundle with a model parameter replaced.
   * <p>
   * The parameter is specified by {@code index} and replaced by {@code value}.
   * 
   * @param index  the index
   * @param value  the value
   * @return the new bundle
   */
  public abstract SmileModelData with(int index, double value);

}

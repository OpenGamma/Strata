/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Describes the transformation (and its inverse) from  a set of n variables (e.g. model parameters) to a set of m variables
 * (e.g. fitting parameters), where m <= n.The principle use is in constrained optimisation, where the valid values of the parameters
 * of a model live in some hyper-volume in R^n, but we wish to work with unconstrained variables in R^m. <p>
 * The model parameters are denoted as <b>y</b> and the unconstrained variables as <b>y*</b>, which are related by the vector function
 * <b>y*</b> = f(<b>y</b>), and its inverse  <b>y</b> = f<sup>-1</sup>(<b>y*</b>). The i,j element of the Jacobian is the rate of change of 
 * the i<sup>th</sup> element of <b>y*</b> with respect to the  j <sup>th</sup> element of <b>y</b>, which is a (matrix) function of <b>y</b>,
 * i.e. <b>J</b>(<b>y</b>). The inverse Jacobian is the rate of change of <b>y</b> with respect to  <b>y*</b>, i.e. <b>J</b><sup>-1</sup>(<b>y*</b>).
 * These four functions must be provided by implementations of this interface. 
 */
public interface NonLinearParameterTransforms {

  int getNumberOfModelParameters();

  int getNumberOfFittingParameters();

  /**
   * Transforms from a set of model parameters to a (possibly smaller) set of unconstrained fitting parameters
   * @param modelParameters   the model parameters
   * @return The fitting parameters
   */
  DoubleArray transform(DoubleArray modelParameters);

  /**
   * Transforms from a set of unconstrained fitting parameters to a (possibly larger) set of function parameters 
   * @param fittingParameters The fitting parameters
   * @return The model parameters
   */
  DoubleArray inverseTransform(DoubleArray fittingParameters);

  /**
   * Calculates the Jacobian - the rate of change of the fitting parameters WRT the model parameters 
   * @param modelParameters The model parameters 
   * @return The Jacobian 
   */
  DoubleMatrix jacobian(DoubleArray modelParameters);

  /**
   * Calculates the inverse Jacobian  - the rate of change of the model parameters WRT the fitting parameters 
   * @param fittingParameters The fitting parameters
   * @return the inverse Jacobian 
   */
  DoubleMatrix inverseJacobian(DoubleArray fittingParameters);

}

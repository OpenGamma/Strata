/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * For a set of <i>n</i> function parameters, this takes <i>n</i> ParameterLimitsTransform (which can be the NullTransform which does NOT transform the parameter) which transform
 * a constrained function parameter (e.g. must be between -1 and 1) to a unconstrained fit parameter. It also takes a BitSet (of length <i>n</i>) with an element set to <b>true</b> if
 * that parameter is fixed - a set of <i>n</i> startValues must also be provided, with only those corresponding to fixed parameters being used (i.e. the parameter is fixed at the startValue).
 * The purpose is to allow an optimiser to work with unconstrained parameters without modifying the function that one wishes to optimise.
 */
// TODO not tested
public class UncoupledParameterTransforms implements NonLinearParameterTransforms {

  private final DoubleArray _startValues;
  private final ParameterLimitsTransform[] _transforms;
  private final boolean[] _freeParameters;
  private final int _nMP;
  private final int _nFP;

  /**
   *
   * @param startValues fixed parameter values (if no parameters are fixed this is completely ignored)
   * @param transforms Array of ParameterLimitsTransform (which can be the NullTransform which does NOT transform the parameter) which transform
   * a constrained function parameter (e.g. must be between -1 and 1) to a unconstrained fit parameter.
   * @param fixed BitSet with an element set to <b>true</b> if that parameter is fixed
   */
  public UncoupledParameterTransforms(DoubleArray startValues, ParameterLimitsTransform[] transforms, BitSet fixed) {
    ArgChecker.notNull(startValues, "null start values");
    ArgChecker.notEmpty(transforms, "must specify transforms");
    ArgChecker.notNull(fixed, "must specify what is fixed (even if none)");
    _nMP = startValues.size();
    ArgChecker.isTrue(_nMP == transforms.length, "Have {}-dimensional start value but {} transforms", _nMP, transforms.length);
    _freeParameters = new boolean[_nMP];
    for (int i = 0; i < _nMP; i++) {
      if (i < fixed.size()) {
        _freeParameters[i] = !fixed.get(i);
      } else {
        _freeParameters[i] = true;
      }
    }
    int count = fixed.cardinality();
    ArgChecker.isTrue(count < _nMP, "all parameters are fixed");
    _nFP = _nMP - count;
    _startValues = startValues;
    _transforms = transforms;
  }

  /**
   *
   * @return The number of function parameters
   */
  @Override
  public int getNumberOfModelParameters() {
    return _nMP;
  }

  /**
   *
   * @return The number of fitting parameters (equals the number of model parameters minus the number of fixed parameters)
   */
  @Override
  public int getNumberOfFittingParameters() {
    return _nFP;
  }

  /**
   * Transforms from a set of function parameters (some of which may have constrained range and/or be fixed) to a (possibly smaller) set of unconstrained fitting parameters
   * <b>Note:</b> If a parameter is fixed, it is its value as provided by <i>startValues<\i> not the value given here that will be returned by inverseTransform (and thus used in the function)
   * @param functionParameters The function parameters
   * @return The fitting parameters
   */
  @Override
  public DoubleArray transform(DoubleArray functionParameters) {
    ArgChecker.notNull(functionParameters, "function parameters");
    ArgChecker.isTrue(functionParameters.size() == _nMP, "functionParameters wrong dimension");
    double[] fittingParameter = new double[_nFP];
    for (int i = 0, j = 0; i < _nMP; i++) {
      if (_freeParameters[i]) {
        fittingParameter[j] = _transforms[i].transform(functionParameters.get(i));
        j++;
      }
    }
    return DoubleArray.copyOf(fittingParameter);
  }

  /**
   * Transforms from a set of unconstrained fitting parameters to a (possibly larger) set of function parameters (some of which may have constrained range and/or be fixed).
   * @param fittingParameters The fitting parameters
   * @return The function parameters
   */
  @Override
  public DoubleArray inverseTransform(DoubleArray fittingParameters) {
    ArgChecker.notNull(fittingParameters, "fitting parameters");
    ArgChecker.isTrue(fittingParameters.size() == _nFP, "fittingParameter wrong dimension");
    double[] modelParameter = new double[_nMP];
    for (int i = 0, j = 0; i < _nMP; i++) {
      if (_freeParameters[i]) {
        modelParameter[i] = _transforms[i].inverseTransform(fittingParameters.get(j));
        j++;
      } else {
        modelParameter[i] = _startValues.get(i);
      }
    }
    return DoubleArray.copyOf(modelParameter);
  }

  /**
   * Calculates the Jacobian of the transform from function parameters to fitting parameters - the i,j element will be the partial derivative of i^th fitting parameter with respect
   * to the j^th function parameter
   * @param functionParameters The function parameters
   * @return matrix of partial derivative of fitting parameter with respect to function parameters
   */
  // TODO not tested
  @Override
  public DoubleMatrix jacobian(DoubleArray functionParameters) {
    ArgChecker.notNull(functionParameters, "function parameters");
    ArgChecker.isTrue(functionParameters.size() == _nMP, "functionParameters wrong dimension");
    double[][] jac = new double[_nFP][_nMP];
    for (int i = 0, j = 0; i < _nMP; i++) {
      if (_freeParameters[i]) {
        jac[j][i] = _transforms[i].transformGradient(functionParameters.get(i));
        j++;
      }
    }
    return DoubleMatrix.copyOf(jac);
  }

  /**
   * Calculates the Jacobian of the transform from fitting parameters to function parameters - the i,j element will be the partial derivative of i^th function parameter with respect
   * to the j^th  fitting parameter
   * @param fittingParameters  The fitting parameters
   * @return  matrix of partial derivative of function parameter with respect to fitting parameters
   */
  // TODO not tested

  @SuppressWarnings("deprecation")
  @Override
  public DoubleMatrix inverseJacobian(DoubleArray fittingParameters) {
    ArgChecker.notNull(fittingParameters, "fitting parameters");
    ArgChecker.isTrue(fittingParameters.size() == _nFP, "fitting parameters wrong dimension");
    double[][] jac = new double[_nMP][_nFP];
    int[] p = new int[_nMP];
    int[] q = new int[_nMP];
    int t = 0;
    for (int i = 0; i < _nMP; i++) {
      if (_freeParameters[i]) {
        p[t] = i;
        q[t] = t;
        t++;
      }
    }
    int pderef, qderef;
    for (int i = 0; i < t; i++) {
      pderef = p[i];
      qderef = q[i];
      jac[pderef][qderef] = _transforms[pderef].inverseTransformGradient(fittingParameters.get(qderef));
    }
    return DoubleMatrix.copyOf(jac);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_freeParameters);
    result = prime * result + _startValues.hashCode();
    result = prime * result + Arrays.hashCode(_transforms);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    UncoupledParameterTransforms other = (UncoupledParameterTransforms) obj;
    if (!Arrays.equals(_freeParameters, other._freeParameters)) {
      return false;
    }
    if (!Objects.equals(_startValues, other._startValues)) {
      return false;
    }
    return Arrays.equals(_transforms, other._transforms);
  }

}

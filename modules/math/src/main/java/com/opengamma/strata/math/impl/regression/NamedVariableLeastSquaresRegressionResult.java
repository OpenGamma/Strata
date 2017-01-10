/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class NamedVariableLeastSquaresRegressionResult extends LeastSquaresRegressionResult {

  private static final Logger log = LoggerFactory.getLogger(NamedVariableLeastSquaresRegressionResult.class);
  private final List<String> _independentVariableNames;
  private final LeastSquaresRegressionResult _result;
  private static final String INTERCEPT_STRING = "Intercept";

  public NamedVariableLeastSquaresRegressionResult(
      List<String> independentVariableNames,
      LeastSquaresRegressionResult result) {

    super(result);
    if (independentVariableNames == null) {
      throw new IllegalArgumentException("List of independent variable names was null");
    }
    _independentVariableNames = new ArrayList<>();
    if (result.hasIntercept()) {
      if (independentVariableNames.size() != result.getBetas().length - 1) {
        throw new IllegalArgumentException("Length of variable name array did not match number of results in the regression");
      }
      _independentVariableNames.add(INTERCEPT_STRING);
    } else {
      if (independentVariableNames.size() != result.getBetas().length) {
        throw new IllegalArgumentException("Length of variable name array did not match number of results in the regression");
      }
    }
    _independentVariableNames.addAll(independentVariableNames);
    _result = result;
  }

  /**
   * @return the _independentVariableNames
   */
  public List<String> getIndependentVariableNames() {
    return _independentVariableNames;
  }

  /**
   * @return the _result
   */
  public LeastSquaresRegressionResult getResult() {
    return _result;
  }

  public Double getPredictedValue(Map<String, Double> namesAndValues) {
    if (namesAndValues == null) {
      throw new IllegalArgumentException("Map was null");
    }
    if (namesAndValues.isEmpty()) {
      log.warn("Map was empty: returning 0");
      return 0.;
    }
    double[] betas = getBetas();
    double sum = 0;
    if (hasIntercept()) {
      if (namesAndValues.size() < betas.length - 1) {
        throw new IllegalArgumentException("Number of named variables in map was smaller than that in regression");
      }
    } else {
      if (namesAndValues.size() < betas.length) {
        throw new IllegalArgumentException("Number of named variables in map was smaller than that in regression");
      }
    }
    int i = hasIntercept() ? 1 : 0;
    for (String name : getIndependentVariableNames()) {
      if (name.equals(INTERCEPT_STRING)) {
        sum += betas[0];
      } else {
        if (!namesAndValues.containsKey(name) || namesAndValues.get(name) == null) {
          throw new IllegalArgumentException("Do not have value for " + name);
        }
        sum += betas[i++] * namesAndValues.get(name);
      }
    }
    return sum;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + (_independentVariableNames == null ? 0 : _independentVariableNames.hashCode());
    result = prime * result + (_result == null ? 0 : _result.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NamedVariableLeastSquaresRegressionResult other = (NamedVariableLeastSquaresRegressionResult) obj;
    if (_independentVariableNames == null) {
      if (other._independentVariableNames != null) {
        return false;
      }
    } else if (!_independentVariableNames.equals(other._independentVariableNames)) {
      return false;
    }
    if (_result == null) {
      if (other._result != null) {
        return false;
      }
    } else if (!_result.equals(other._result)) {
      return false;
    }
    return true;
  }
}

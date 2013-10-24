/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Details that every function needs available which must be passed
 */
public interface ResultGenerator {

  <T> FunctionResult<T> generateFailureResult(FunctionResult<?> functionResult);

  <T> FunctionResult<T> generateSuccessResult(T resultValue);

  <T> FunctionResult<T> generateFailureResult(ResultStatus missingData, String message, Object... messageParams);
}

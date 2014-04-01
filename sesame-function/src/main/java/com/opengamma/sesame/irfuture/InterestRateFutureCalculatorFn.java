/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function to convert the interest rate future into the OG-Analytics representation, and to prepare the necessary market
 * data requirements needed to perform calculations on that security.
 */
public interface InterestRateFutureCalculatorFn {

  Result<InterestRateFutureCalculator> generateCalculator(Environment env, InterestRateFutureSecurity security);
}

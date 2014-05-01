/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function to return an instance of {@link BlackSTIRFuturesProviderInterface} for a given interest rate future option.
 */
public interface BlackSTIRFuturesProviderFn {

  Result<BlackSTIRFuturesProviderInterface> getBlackSTIRFuturesProvider(Environment env, IRFutureOptionSecurity irFutureOption);
}

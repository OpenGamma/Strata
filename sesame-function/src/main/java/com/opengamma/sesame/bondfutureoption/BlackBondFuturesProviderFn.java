/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackBondFuturesProviderInterface;
import com.opengamma.financial.security.option.BondFutureOptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function to return instances of {@link BlackBondFuturesProviderInterface}.
 */
public interface BlackBondFuturesProviderFn {

  Result<BlackBondFuturesProviderInterface> getBlackBondFuturesProvider(Environment env, BondFutureOptionSecurity bondFutureOption);
}

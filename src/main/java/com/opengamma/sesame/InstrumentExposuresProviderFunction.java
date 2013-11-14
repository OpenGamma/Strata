/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.financial.security.FinancialSecurity;

public interface InstrumentExposuresProviderFunction {

  FunctionResult<Set<String>> getCurveConstructionConfigurationsForSecurity(FinancialSecurity security);
}

/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.OutputFunction;
import com.opengamma.sesame.function.OutputName;

// todo the FunctionResult<> bit is probably always there, would be nice if we could say OutputFunction<CashFlowSecurity, Double>

@DefaultImplementation(EquityPresentValue.class)
@OutputName(OutputNames.PRESENT_VALUE)
public interface EquityPresentValueFunction extends OutputFunction<EquitySecurity, FunctionResult<Double>> {
}

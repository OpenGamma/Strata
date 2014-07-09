/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.measures;

import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * PV function for credit types.
 */
public interface CreditPvFn extends CreditRiskMesasureFn<CurrencyAmount> {
  
  @Override
  @Output(OutputNames.PRESENT_VALUE)
  Result<CurrencyAmount> priceStandardCds(Environment env, StandardCDSSecurity cds);

  @Override
  @Output(OutputNames.PRESENT_VALUE)
  Result<CurrencyAmount> priceLegacyCds(Environment env, LegacyCDSSecurity cds);
  
}

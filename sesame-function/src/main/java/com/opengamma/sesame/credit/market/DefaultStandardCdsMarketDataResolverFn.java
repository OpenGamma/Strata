/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import java.util.Map;

import com.opengamma.analytics.financial.credit.DebtSeniority;
import com.opengamma.analytics.financial.credit.RestructuringClause;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.config.RestructuringSettings;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Default implementation of {@link StandardCdsMarketDataResolverFn}. Derives
 * a {@link CreditCurveDataKey} from a given {@link StandardCDSSecurity} to be
 * used for resolving market data. 
 */
public class DefaultStandardCdsMarketDataResolverFn implements StandardCdsMarketDataResolverFn {

  //typical values:
  //USD -> XR
  //JPY -> CR
  //EUR -> MM
  private final RestructuringSettings _restructuringSettings;
  
  /**
   * Creates an instance of the function.
   * 
   * @param restructuringSettings the settings to use for 
   * inferring restructuring clauses on standard CDSs.
   */
  public DefaultStandardCdsMarketDataResolverFn(RestructuringSettings restructuringSettings) {
    _restructuringSettings = ArgumentChecker.notNull(restructuringSettings, "restructuringSettings");
  }

  @Override
  public Result<CreditCurveDataKey> resolve(Environment env, StandardCDSSecurity security) {
    Currency currency = security.getNotional().getCurrency();
    Map<Currency, RestructuringClause> restructuringMappings = _restructuringSettings.getRestructuringMappings();
    
    if (!restructuringMappings.containsKey(currency)) {
      return Result.failure(FailureStatus.ERROR, 
                            "Failed to infer restructuring clause. No mapping configured for {}", 
                            currency);
    }
    
    RestructuringClause restructuringClause = restructuringMappings.get(currency);
    ExternalId referenceEntity = security.getReferenceEntity();
    DebtSeniority seniority = security.getDebtSeniority();
    
    CreditCurveDataKey key = CreditCurveDataKey.builder()
                                .currency(currency)
                                .curveName(referenceEntity.getValue())
                                .seniority(seniority)
                                .restructuring(restructuringClause)
                                .build();
    
    return Result.success(key);
  }

}

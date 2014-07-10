/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.config.BondCreditCurveDataKeyMap;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A {@link BondCreditMarketDataResolverFn} which uses an underlying map
 * to resolve a key for market data lookup.
 */
public class DefaultBondCreditMarketDataResolverFn implements BondCreditMarketDataResolverFn {
  
  private final BondCreditCurveDataKeyMap _keyMap;
  
  /**
   * Creates an instance.
   * 
   * @param keyMap the key map to use for key resolution
   */
  public DefaultBondCreditMarketDataResolverFn(BondCreditCurveDataKeyMap keyMap) {
    _keyMap = ArgumentChecker.notNull(keyMap, "keyMap");
  }

  @Override
  public Result<CreditCurveDataKey> resolve(Environment env, BondSecurity bondSecurity) {
    ExternalIdBundle bondId = bondSecurity.getExternalIdBundle();
    
    CreditCurveDataKey creditCurveDataKey = _keyMap.getKeyMap().get(bondId);
    
    if (creditCurveDataKey != null) {
      return Result.success(creditCurveDataKey);
    } else {
      return Result.failure(FailureStatus.ERROR, "Failed to resolve a mapping for bond id {}", bondId);
    }
  }

}

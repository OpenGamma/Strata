/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import java.util.Map;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.sesame.credit.config.CreditCurveDataKeyMap;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Default implementation of {@link CreditKeyMapperFn}. It is driven by
 * an instance of {@link CreditCurveDataKeyMap}, a type which can be 
 * stored and/or sourced from the config db.
 */
public class DefaultCreditKeyMapperFn implements CreditKeyMapperFn {

  private final CreditCurveDataKeyMap _keyMap;
  
  /**
   * Constructs an instance of this function.
   * 
   * @param keyMap the key map to use.
   */
  public DefaultCreditKeyMapperFn(CreditCurveDataKeyMap keyMap) {
    _keyMap = ArgumentChecker.notNull(keyMap, "keyMap");
  }

  @Override
  public Result<CreditCurveDataKey> getMapping(CreditCurveDataKey key) {
    
    Map<CreditCurveDataKey, CreditCurveDataKey> keyMap = _keyMap.getKeyMap();
    
    //if a mapping exists, use the target key, else use the passed key
    CreditCurveDataKey resultKey = keyMap.containsKey(key) ? keyMap.get(key) : key;
    
    return Result.success(resultKey);
  }

}

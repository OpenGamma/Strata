/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import java.util.Map;

import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.config.CreditCurveDataKeyMap;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Implementation of {@link CreditKeyMapperFn} which sources a key map
 * from the config db. See {@link CreditKeyMapperFn} for further details.
 */
public class ConfigDbCreditKeyMapperFn implements CreditKeyMapperFn {

  private final ConfigLink<CreditCurveDataKeyMap> _keyMapLink;
  
  /**
   * Constructs an instance of this function.
   * 
   * @param keyMapLink a link to the key map to use.
   */
  public ConfigDbCreditKeyMapperFn(ConfigLink<CreditCurveDataKeyMap> keyMapLink) {
    _keyMapLink = ArgumentChecker.notNull(keyMapLink, "keyMapLink");
  }

  @Override
  public Result<CreditCurveDataKey> map(CreditCurveDataKey key) {
    
    Map<CreditCurveDataKey, CreditCurveDataKey> keyMap = _keyMapLink.resolve().getKeyMap();
    
    //if a mapping exists, use the target key, else use the passed key
    CreditCurveDataKey resultKey = keyMap.containsKey(key) ? keyMap.get(key) : key;
    
    return Result.success(resultKey);
  }

}

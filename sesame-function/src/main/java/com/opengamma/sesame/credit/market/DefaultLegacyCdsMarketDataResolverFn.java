/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.analytics.financial.credit.RestructuringClause;
import com.opengamma.core.legalentity.SeniorityLevel;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Resolves a credit key for {@link LegacyCDSSecurity}s. Since legacy CDSs explicitly define
 * key fields (i.e. reference entity, currency, seniority and restructuring clause) so
 * key construction is a simple case of copying fields over to the key object.
 */
public class DefaultLegacyCdsMarketDataResolverFn implements LegacyCdsMarketDataResolverFn {

  private final CreditKeyMapperFn _creditKeyMapperFn;
  
  /**
   * Creates an instance.
   * 
   * @param creditKeyMapperFn 
   */
  public DefaultLegacyCdsMarketDataResolverFn(CreditKeyMapperFn creditKeyMapperFn) {
    _creditKeyMapperFn = ArgumentChecker.notNull(creditKeyMapperFn, "creditKeyMapperFn");
  }

  @Override
  public Result<CreditCurveDataKey> resolve(Environment env, LegacyCDSSecurity security) {
    ExternalId referenceEntity = security.getReferenceEntity();
    SeniorityLevel seniority = security.getSeniority();
    RestructuringClause restructuringClause = security.getRestructuringClause();
    Currency currency = security.getNotional().getCurrency();
    
    CreditCurveDataKey key = CreditCurveDataKey.builder()
        .currency(currency)
        .curveName(referenceEntity.getValue())
        .seniority(seniority)
        .restructuring(restructuringClause)
        .build();
    
    return _creditKeyMapperFn.getMapping(key);
    
  }

}

/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.math.BigDecimal;
import java.util.Set;

import org.threeten.bp.LocalDate;
import org.threeten.bp.OffsetTime;

import com.google.common.collect.Iterables;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default implementation of IssuerProviderFn that returns a multicurve bundle of curves by issuer.
 */
public class ExposureFunctionsIssuerProviderFn implements IssuerProviderFn {
  
  private final MarketExposureSelectorFn _marketExposureSelectorFn;
  
  private final IssuerProviderBundleFn _issuerProviderBundleFn;
  
  public ExposureFunctionsIssuerProviderFn(MarketExposureSelectorFn marketExposureSelectorFn,
                                           IssuerProviderBundleFn issuerProviderBundleFn) {
    _marketExposureSelectorFn = marketExposureSelectorFn;
    _issuerProviderBundleFn = issuerProviderBundleFn;
  }
  @Override
  public Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> createBundle(Environment env,
                                                                                               FinancialSecurity security,
                                                                                               Result<FXMatrix> fxMatrix) {

    Trade tradeWrapper = new SimpleTrade(security, BigDecimal.ONE, new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "CPARTY")), LocalDate.now(), OffsetTime.now());
    
    return createBundle(env, tradeWrapper, fxMatrix);
  }

  
  @Override
  public com.opengamma.util.result.Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> createBundle(Environment env,
                                                                                                                         Trade trade,
                                                                                                                         Result<FXMatrix> fxMatrix) {
    
                                                                                                                         
    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isSuccess()) {
      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurations(trade);
      
      if (curveConfigs.size() == 1) {
        Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> bundle =
            _issuerProviderBundleFn.generateBundle(env, Iterables.getOnlyElement(curveConfigs));
        if (bundle.isSuccess()) {
          return Result.success(bundle.getValue());
        } else {
          return Result.failure(bundle);
        }
      } else if (curveConfigs.isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "No curve construction configs found for {}", trade);
      } else {
        return Result.failure(FailureStatus.MULTIPLE, "Found {} configs, expected one", curveConfigs.size());
      }
    } else {
      return Result.failure(mesResult);
    }
  }

  
}

/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.google.common.collect.Iterables;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

public class ExposureFunctionsIssuerProviderFn implements IssuerProviderFn {
  
  private final MarketExposureSelectorFn _marketExposureSelectorFn;
  
  private final IssuerProviderBundleFn _issuerProviderBundleFn;
  
  public ExposureFunctionsIssuerProviderFn(MarketExposureSelectorFn marketExposureSelectorFn,
                                           IssuerProviderBundleFn issuerProviderBundleFn) {
    _marketExposureSelectorFn = marketExposureSelectorFn;
    _issuerProviderBundleFn = issuerProviderBundleFn;
  }

  @Override
  public Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> createBundle(Environment env, FinancialSecurity security, Result<FXMatrix> fxMatrix) {
    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isSuccess()) {
      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurationsForSecurity(security);
      
      if (curveConfigs.size() == 1) {
        Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> bundle =
            _issuerProviderBundleFn.generateBundle(env, Iterables.getOnlyElement(curveConfigs));
        if (bundle.isSuccess()) {
          return Result.success(bundle.getValue());
        } else {
          return Result.failure(bundle);
        }
      } else if (curveConfigs.isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "No curve construction configs found for {}", security);
      } else {
        return Result.failure(FailureStatus.MULTIPLE, "Found {} configs, expected one", curveConfigs.size());
      }
    } else {
      return Result.failure(mesResult);
    }
  }

}

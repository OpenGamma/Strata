/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.StandardResultGenerator.failure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.credit.ConfigDBCurveDefinitionSource;
import com.opengamma.financial.analytics.curve.credit.CurveDefinitionSource;
import com.opengamma.util.ArgumentChecker;

/**
 * Returns a curve definition from the configured source. If not available, the
 * return FunctionResult will indicate the reason why.
 */
public class CurveDefinitionProvider implements CurveDefinitionProviderFunction {

  private final CurveDefinitionSource _curveDefinitionSource;

  public CurveDefinitionProvider(ConfigSource configSource) {

    ArgumentChecker.notNull(configSource, "configSource");
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(configSource);
  }

  @Override
  public FunctionResult<CurveDefinition> getCurveDefinition(String curveName) {

    final CurveDefinition curveDefinition = _curveDefinitionSource.getCurveDefinition(curveName);
    if (curveDefinition != null) {
      return success(curveDefinition);
    } else {
      return failure(FailureStatus.MISSING_DATA, "Could not get curve definition called {}", curveName);
    }
  }
}

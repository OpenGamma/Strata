/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

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
  private final ResultGenerator _resultGenerator;

  public CurveDefinitionProvider(ConfigSource configSource, ResultGenerator resultGenerator) {

    ArgumentChecker.notNull(configSource, "configSource");
    ArgumentChecker.notNull(resultGenerator, "resultGenerator");
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(configSource);
    _resultGenerator = resultGenerator;
  }

  @Override
  public FunctionResult<CurveDefinition> getCurveDefinition(String curveName) {

    final CurveDefinition curveDefinition = _curveDefinitionSource.getCurveDefinition(curveName);
    return curveDefinition != null ?
        _resultGenerator.generateSuccessResult(curveDefinition) :
        _resultGenerator.<CurveDefinition>generateFailureResult(
            ResultStatus.MISSING_DATA, "Could not get curve definition called {}", curveName);
  }
}

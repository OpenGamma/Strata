/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.StandardResultGenerator.failure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import org.threeten.bp.LocalDate;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveSpecificationBuilder;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.credit.ConfigDBCurveDefinitionSource;
import com.opengamma.financial.analytics.curve.credit.CurveDefinitionSource;
import com.opengamma.id.VersionCorrection;

/**
 * Provides a curve specification.
 */
public class CurveSpecificationProvider implements CurveSpecificationProviderFunction {

  private final CurveDefinitionSource _curveDefinitionSource;
  private final ConfigDBCurveSpecificationBuilder _curveSpecificationBuilder;
  private final ValuationTimeProviderFunction _valuationTimeProviderFunction;

  public CurveSpecificationProvider(ConfigSource configSource,
                                    ValuationTimeProviderFunction valuationTimeProviderFunction) {

    _valuationTimeProviderFunction = valuationTimeProviderFunction;
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(configSource);
    _curveSpecificationBuilder = new ConfigDBCurveSpecificationBuilder(configSource);
  }

  @Override
  public FunctionResult<CurveSpecification> getCurveSpecification(String curveName) {

    final CurveDefinition curveDefinition =
        _curveDefinitionSource.getCurveDefinition(curveName, VersionCorrection.LATEST);

    if (curveDefinition == null) {
      return failure(FailureStatus.MISSING_DATA, "Could not get curve definition called: {}", curveName);
    } else {
      return success(_curveSpecificationBuilder.buildCurve(
          _valuationTimeProviderFunction.getValuationTime(),
          LocalDate.from(_valuationTimeProviderFunction.getValuationTime()),
          curveDefinition));
    }
  }
}

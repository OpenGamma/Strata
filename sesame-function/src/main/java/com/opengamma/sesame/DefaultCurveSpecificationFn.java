/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.AbstractCurveSpecification;
import com.opengamma.financial.analytics.curve.ConfigDBCurveSpecificationBuilder;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Function implementation that provides a curve specification.
 */
public class DefaultCurveSpecificationFn implements CurveSpecificationFn {

  /**
   * The curve specification builder.
   */
  private final ConfigDBCurveSpecificationBuilder _curveSpecificationBuilder;

  public DefaultCurveSpecificationFn(ConfigSource configSource) {
    // TODO shouldn't be using config source directly
    _curveSpecificationBuilder = new ConfigDBCurveSpecificationBuilder(ArgumentChecker.notNull(configSource, "configSource"));
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<AbstractCurveSpecification> getCurveSpecification(Environment env, AbstractCurveDefinition curveDefinition) {
    // TODO - how can this possibly be correct when it's using LocalDate.now()?
    return Result.success(_curveSpecificationBuilder.buildSpecification(env.getValuationTime().toInstant(),
                                                                LocalDate.now(),
                                                                curveDefinition));
  }
}

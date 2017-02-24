/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketData;

/**
 * Provides the definition of how to calibrate a nodal curve.
 * <p>
 * A nodal curve is built from a number of parameters and described by metadata.
 * Calibration is based on a list of {@link CurveNode} instances, one for each parameter,
 * that specify the underlying instruments.
 */
public interface NodalCurveDefinition extends CurveDefinition {

  @Override
  public default int getParameterCount() {
    return getNodes().size();
  }

  @Override
  public abstract NodalCurveDefinition filtered(LocalDate valuationDate, ReferenceData refData);

  @Override
  public abstract NodalCurve curve(LocalDate valuationDate, CurveMetadata metadata, DoubleArray parameters);

  @Override
  public default ImmutableList<Double> initialGuess(MarketData marketData) {
    ImmutableList.Builder<Double> result = ImmutableList.builder();
    for (CurveNode node : getNodes()) {
      result.add(node.initialGuess(marketData, getYValueType()));
    }
    return result.build();
  }

}

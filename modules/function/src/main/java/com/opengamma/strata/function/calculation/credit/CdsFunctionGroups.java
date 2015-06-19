/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.finance.credit.CdsTrade;

public final class CdsFunctionGroups {

  private static final FunctionGroup<CdsTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(CdsTrade.class).name("CdsDiscounting")
          .addFunction(Measure.PRESENT_VALUE, CdsPvFunction.class)
          .addFunction(Measure.IR01_PARALLEL_PAR, CdsIr01ParallelParFunction.class)
          .addFunction(Measure.IR01_BUCKETED_PAR, CdsIr01BucketedParFunction.class)
          .addFunction(Measure.CS01_PARALLEL_PAR, CdsCs01ParallelParFunction.class)
          .addFunction(Measure.CS01_BUCKETED_PAR, CdsCs01BucketedParFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private CdsFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  public static FunctionGroup<CdsTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.credit;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.finance.credit.CdsTrade;

public final class CdsFunctionGroups {

  private static final FunctionGroup<CdsTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(CdsTrade.class).name("CdsDiscounting")
          .addFunction(Measure.PRESENT_VALUE, CdsPvFunction.class)
          .addFunction(Measure.PV01, CdsPv01ParFunction.class)
          .addFunction(Measure.BUCKETED_PV01, CdsBucketedPv01ParFunction.class)
          .addFunction(Measure.CS01, CdsCs01ParFunction.class)
          .addFunction(Measure.BUCKETED_CS01, CdsBucketedCs01ParFunction.class)
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

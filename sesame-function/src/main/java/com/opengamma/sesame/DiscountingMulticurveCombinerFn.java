/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Combines a collection of multicurve bundles into a single one.
 */
public interface DiscountingMulticurveCombinerFn {

  Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      FinancialSecurity security, Result<FXMatrix> fxMatrix);
}

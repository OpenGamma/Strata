/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Combines a collection of multicurve bundles into a single one.
 */
public interface DiscountingMulticurveCombinerFn {

  /**
   * Returns the merged multicurve bundle for a specified environment, security and FX matrix. This has been deprecated
   * because ExposureFunctions can be selected by trade details such as counterparty or trade attributes.
   *
   * @param env the environment to merge the multicurve bundle for, not null.
   * @param security the security to merge the multicurve bundle for, not null.
   * @param fxMatrix the FX matrix to include inside the multicurve bundle, not null.
   * @return the merged multicurve bundle.
   * 
   * @deprecated use {@link #createMergedMulticurveBundle(Environment, Trade, Result)} using the original trade.
   */
  @Deprecated
  Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      Environment env, FinancialSecurity security, Result<FXMatrix> fxMatrix);
  
  /**
   * Returns the merged multicurve bundle for a specified environment, trade and FX matrix.
   *
   * @param env the environment to merge the multicurve bundle for, not null.
   * @param trade the trade to merge the multicurve bundle for, not null.
   * @param fxMatrix the FX matrix to include inside the multicurve bundle, not null.
   * @return the merged multicurve bundle.
   */
  Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      Environment env, Trade trade, FXMatrix fxMatrix);
}

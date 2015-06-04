/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlock;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.matrix.MatrixAlgebra;
import com.opengamma.analytics.math.matrix.OGMatrixAlgebra;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Calculator to obtain the Market Quote sensitivities from the {@link CurveBuildingBlockBundle} obtained through the 
 * curve calibration.
 * <p>
 * The Market Quote sensitivities are also called Par Rate when the instruments used in the curve calibration are
 * quoted in rate, e.g. IRS, FRA or OIS.
 */
public class MarketQuoteSensitivityCalculator {

  /** Default instance. **/
  public static final MarketQuoteSensitivityCalculator DEFAULT = new MarketQuoteSensitivityCalculator();

  /** The matrix algebra used for matrix inversion. */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new OGMatrixAlgebra();

  //TODO: The method internally uses DoubleMatrix1D and DoubleMatrix2D. This should be reviewed.

  public CurveCurrencyParameterSensitivities fromParameterSensitivity(CurveCurrencyParameterSensitivities parameterSensitivity,
      CurveBuildingBlockBundle blocks) {
    ArgChecker.notNull(parameterSensitivity, "Sensitivity");
    ArgChecker.notNull(blocks, "Units");
    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();
    for (CurveCurrencyParameterSensitivity entry : parameterSensitivity.getSensitivities()) {
      Pair<CurveBuildingBlock, DoubleMatrix2D> blockEntry = blocks.getBlock(entry.getCurveName().toString());
      double[] parameterSensitivityEntry = entry.getSensitivity();
      DoubleMatrix1D parameterSensitivityEntryMatrix = new DoubleMatrix1D(parameterSensitivityEntry);
      double[] keySensi = ((DoubleMatrix1D) MATRIX_ALGEBRA
          .multiply(parameterSensitivityEntryMatrix, blockEntry.getSecond())).getData();
      // split between different curves
      for (final String name2 : blockEntry.getFirst().getAllNames()) {
        int nbParameters = blockEntry.getFirst().getNbParameters(name2);
        int start = blockEntry.getFirst().getStart(name2);
        double[] sensiName2 = new double[nbParameters];
        System.arraycopy(keySensi, start, sensiName2, 0, nbParameters);
        result = result.combinedWith(CurveCurrencyParameterSensitivity.of(name2, entry.getCurrency(), sensiName2));
      }
    }
    return result;
  }

}

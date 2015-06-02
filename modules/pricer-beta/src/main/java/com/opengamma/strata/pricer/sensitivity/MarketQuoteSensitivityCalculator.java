/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlock;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.matrix.MatrixAlgebra;
import com.opengamma.analytics.math.matrix.OGMatrixAlgebra;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.NameSensitivityKey;
import com.opengamma.strata.market.sensitivity.SensitivityKey;

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
  
  public CurveParameterSensitivities fromParameterSensitivity(CurveParameterSensitivities parameterSensitivity,
      CurveBuildingBlockBundle blocks) {
    ArgChecker.notNull(parameterSensitivity, "Sensitivity");
    ArgChecker.notNull(blocks, "Units");
    ImmutableMap<SensitivityKey, double[]> sensitivityMap = parameterSensitivity.getSensitivities();
    CurveParameterSensitivities result = CurveParameterSensitivities.empty();
    for(Entry<SensitivityKey, double[]> entry: sensitivityMap.entrySet()) {
      SensitivityKey key = entry.getKey();
      // TODO: improve types of sensitivity key
      CurveName curveName;
      if(key instanceof NameCurrencySensitivityKey) {
        curveName = ((NameCurrencySensitivityKey) key).getCurveName();
      } else if(key instanceof NameSensitivityKey){
        curveName = ((NameSensitivityKey) key).getCurveName();        
      } else {
        throw new IllegalArgumentException("Invalid sensitivity key type, must have curve name");
      }
      Pair<CurveBuildingBlock, DoubleMatrix2D> blockEntry = blocks.getBlock(curveName.toString());
      double[] parameterSensitivityEntry = entry.getValue();
      DoubleMatrix1D parameterSensitivityEntryMatrix = new DoubleMatrix1D(parameterSensitivityEntry);
      double[] keySensi = ((DoubleMatrix1D) MATRIX_ALGEBRA
          .multiply(parameterSensitivityEntryMatrix, blockEntry.getSecond())).getData();
      // split between different curves
      for (final String name2 : blockEntry.getFirst().getAllNames()) {
        int nbParameters = blockEntry.getFirst().getNbParameters(name2);
        int start = blockEntry.getFirst().getStart(name2);
        double[] sensiName2 = new double[nbParameters];
        System.arraycopy(keySensi, start, sensiName2, 0, nbParameters);
        SensitivityKey key2 = null;
        if(key instanceof NameCurrencySensitivityKey) {
          NameCurrencySensitivityKey keyName = (NameCurrencySensitivityKey) key;
          key2 = NameCurrencySensitivityKey.of(name2, keyName.getCurrency());
        } else if(key instanceof NameSensitivityKey){
          key2 = NameSensitivityKey.of(name2);        
        }
        result = result.combinedWith(key2, sensiName2);        
      }
    }
    return result;
  }

}

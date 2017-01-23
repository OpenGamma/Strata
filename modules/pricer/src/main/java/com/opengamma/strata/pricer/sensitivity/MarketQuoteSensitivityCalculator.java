/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.Map;
import java.util.Map.Entry;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculator to obtain the Market Quote sensitivities.
 * <p>
 * This needs the {@link JacobianCalibrationMatrix} obtained during curve calibration.
 * The Market Quote sensitivities are also called Par Rate when the instruments used
 * in the curve calibration are quoted in rate, e.g. IRS, FRA or OIS.
 */
public class MarketQuoteSensitivityCalculator {

  /**
   * The default instance.
   */
  public static final MarketQuoteSensitivityCalculator DEFAULT = new MarketQuoteSensitivityCalculator();
  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new OGMatrixAlgebra();

  //-------------------------------------------------------------------------
  /**
   * Calculates the market quote sensitivities from parameter sensitivity.
   * 
   * @param paramSensitivities  the curve parameter sensitivities
   * @param provider  the rates provider, containing Jacobian calibration information
   * @return the market quote sensitivities
   */
  public CurrencyParameterSensitivities sensitivity(
      CurrencyParameterSensitivities paramSensitivities,
      RatesProvider provider) {

    ArgChecker.notNull(paramSensitivities, "paramSensitivities");
    ArgChecker.notNull(provider, "provider");

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (CurrencyParameterSensitivity paramSens : paramSensitivities.getSensitivities()) {
      // find the matching calibration info
      Curve curve = provider.findData(paramSens.getMarketDataName())
          .filter(v -> v instanceof Curve)
          .map(v -> (Curve) v)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires curve: " + paramSens.getMarketDataName()));
      JacobianCalibrationMatrix info = curve.getMetadata().findInfo(CurveInfoType.JACOBIAN)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires Jacobian calibration information"));

      // calculate the market quote sensitivity using the Jacobian
      DoubleMatrix jacobian = info.getJacobianMatrix();
      DoubleArray paramSensMatrix = paramSens.getSensitivity();
      DoubleArray marketQuoteSensMatrix = (DoubleArray) MATRIX_ALGEBRA.multiply(paramSensMatrix, jacobian);
      DoubleArray marketQuoteSens = marketQuoteSensMatrix;

      // split between different curves
      Map<CurveName, DoubleArray> split = info.splitValues(marketQuoteSens);
      for (Entry<CurveName, DoubleArray> entry : split.entrySet()) {
        CurveName curveName = entry.getKey();
        CurrencyParameterSensitivity maketQuoteSens = provider.findData(curveName)
            .map(c -> c.createParameterSensitivity(paramSens.getCurrency(), entry.getValue()))
            .orElse(CurrencyParameterSensitivity.of(curveName, paramSens.getCurrency(), entry.getValue()));
        result = result.combinedWith(maketQuoteSens);
      }
    }
    return result;
  }

  /**
   * Calculates the market quote sensitivities from parameter sensitivity.
   * <p>
   * This calculates the market quote sensitivities of credit derivatives.
   * The input parameter sensitivities must be computed based on the credit rates provider.
   * 
   * @param paramSensitivities  the curve parameter sensitivities
   * @param provider  the credit rates provider, containing Jacobian calibration information
   * @return the market quote sensitivities
   */
  public CurrencyParameterSensitivities sensitivity(
      CurrencyParameterSensitivities paramSensitivities,
      CreditRatesProvider provider) {

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (CurrencyParameterSensitivity paramSens : paramSensitivities.getSensitivities()) {
      // find the matching calibration info
      Curve curve = provider.findData(paramSens.getMarketDataName())
          .filter(v -> v instanceof Curve)
          .map(v -> (Curve) v)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires curve: " + paramSens.getMarketDataName()));
      JacobianCalibrationMatrix info = curve.getMetadata().findInfo(CurveInfoType.JACOBIAN)
          .orElseThrow(() -> new IllegalArgumentException(
              "Market Quote sensitivity requires Jacobian calibration information"));

      // calculate the market quote sensitivity using the Jacobian
      DoubleMatrix jacobian = info.getJacobianMatrix();
      DoubleArray paramSensMatrix = paramSens.getSensitivity();
      DoubleArray marketQuoteSensMatrix = (DoubleArray) MATRIX_ALGEBRA.multiply(paramSensMatrix, jacobian);
      DoubleArray marketQuoteSens = marketQuoteSensMatrix;

      // split between different curves
      Map<CurveName, DoubleArray> split = info.splitValues(marketQuoteSens);
      for (Entry<CurveName, DoubleArray> entry : split.entrySet()) {
        CurveName curveName = entry.getKey();
        CurrencyParameterSensitivity maketQuoteSens = provider.findData(curveName)
            .map(c -> c.createParameterSensitivity(paramSens.getCurrency(), entry.getValue()))
            .orElse(CurrencyParameterSensitivity.of(curveName, paramSens.getCurrency(), entry.getValue()));
        result = result.combinedWith(maketQuoteSens);
      }
    }
    return result;
  }

}

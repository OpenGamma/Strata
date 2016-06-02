/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceInfoType;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Calculator to obtain the raw data sensitivities for swaption related products using calibrated SABR data.
 * <p>
 * This needs data sensitivity info obtained during curve calibration.
 * The info is stored in the surface metadata under the {@link SurfaceInfoType#DATA_SENSITIVITY_INFO}
 */
public class SabrSwaptionRawDataSensitivityCalculator {

  /**
   * The default instance.
   */
  public static final SabrSwaptionRawDataSensitivityCalculator DEFAULT = new SabrSwaptionRawDataSensitivityCalculator();
  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new OGMatrixAlgebra();

  //-------------------------------------------------------------------------
  /**
   * Calculates the raw data sensitivities from parameter sensitivity.  
   * 
   * @param paramSensitivities  the curve parameter sensitivities
   * @param volatilities  the SABR parameters, including the data sensitivity metadata
   * @return the raw data sensitivities
   */
  public CurrencyParameterSensitivity parallelSensitivity(
      CurrencyParameterSensitivities paramSensitivities,
      SabrParametersSwaptionVolatilities volatilities) {
    // Check available sensitivity

    boolean[] dataSensitivityInfoAvailable = new boolean[4]; // parameters in Alpha, Beta, Rho, Nu order

    List<Optional<List<DoubleArray>>> sensitivityInfo = new ArrayList<>();
    sensitivityInfo.add(
        volatilities.getParameters().getAlphaSurface().getMetadata().findInfo(SurfaceInfoType.DATA_SENSITIVITY_INFO));
    dataSensitivityInfoAvailable[0] = sensitivityInfo.get(0).isPresent();
    sensitivityInfo.add(
        volatilities.getParameters().getBetaSurface().getMetadata().findInfo(SurfaceInfoType.DATA_SENSITIVITY_INFO));
    dataSensitivityInfoAvailable[1] = sensitivityInfo.get(1).isPresent();
    sensitivityInfo.add(
        volatilities.getParameters().getRhoSurface().getMetadata().findInfo(SurfaceInfoType.DATA_SENSITIVITY_INFO));
    dataSensitivityInfoAvailable[2] = sensitivityInfo.get(2).isPresent();
    sensitivityInfo.add(
        volatilities.getParameters().getNuSurface().getMetadata().findInfo(SurfaceInfoType.DATA_SENSITIVITY_INFO));
    dataSensitivityInfoAvailable[3] = sensitivityInfo.get(3).isPresent();
    
    
    
    for(CurrencyParameterSensitivity s: paramSensitivities.getSensitivities()) {
      MarketDataName<?> name = s.getMarketDataName();
      if(name instanceof SurfaceName) {
        SurfaceName surfaceName = (SurfaceName) name;
      }
    }
    
    return null;
  }
  

}

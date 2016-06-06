/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.SurfaceName;

/**
 * Calculator to obtain the raw data sensitivities for swaption related products using calibrated SABR data.
 * <p>
 * This needs data sensitivity info obtained during curve calibration.
 */
public class SabrSwaptionRawDataSensitivityCalculator {

  /**
   * The default instance.
   */
  public static final SabrSwaptionRawDataSensitivityCalculator DEFAULT = new SabrSwaptionRawDataSensitivityCalculator();

  //-------------------------------------------------------------------------
  /**
   * Calculates the raw data sensitivities from SABR parameter sensitivity.
   * <p>
   * The SABR parameter sensitivities to data are stored in some optional data in the 
   * {@link SabrParametersSwaptionVolatilities}.
   * The sensitivities to the SABR parameters passed in should be compatible with the SABR parameters in term of data order.
   * <p>
   * Only the sensitivity to the SABR parameters for which there is a data sensitivity are taken into account. 
   * At least one of the four parameter must have such sensitivities.
   * 
   * @param paramSensitivities  the curve SABR parameter sensitivities
   * @param volatilities  the SABR parameters, including the data sensitivity metadata
   * @return the raw data sensitivities
   */
  public CurrencyParameterSensitivity parallelSensitivity(
      CurrencyParameterSensitivities paramSensitivities,
      SabrParametersSwaptionVolatilities volatilities) {
    boolean[] dataSensitivityInfoAvailable = new boolean[4]; // parameters in Alpha, Beta, Rho, Nu order
    List<List<DoubleArray>> sensitivityToRawData = new ArrayList<>(4);
    Optional<ImmutableList<DoubleArray>> alphaInfo = volatilities.getDataSensitivityAlpha();
    dataSensitivityInfoAvailable[0] = alphaInfo.isPresent();
    sensitivityToRawData.add(alphaInfo.isPresent() ? alphaInfo.get() : null);
    Optional<ImmutableList<DoubleArray>> betaInfo = volatilities.getDataSensitivityBeta();
    dataSensitivityInfoAvailable[1] = betaInfo.isPresent();
    sensitivityToRawData.add(betaInfo.isPresent() ? betaInfo.get() : null);
    Optional<ImmutableList<DoubleArray>> rhoInfo = volatilities.getDataSensitivityRho();
    dataSensitivityInfoAvailable[2] = rhoInfo.isPresent();
    sensitivityToRawData.add(rhoInfo.isPresent() ? rhoInfo.get() : null);
    Optional<ImmutableList<DoubleArray>> nuInfo = volatilities.getDataSensitivityNu();
    dataSensitivityInfoAvailable[3] = nuInfo.isPresent();
    sensitivityToRawData.add(nuInfo.isPresent() ? nuInfo.get() : null);
    ArgChecker.isTrue(dataSensitivityInfoAvailable[0] || dataSensitivityInfoAvailable[1] 
        || dataSensitivityInfoAvailable[2] || dataSensitivityInfoAvailable[3],
        "at least one SurfaceInfoType#DATA_SENSITIVITY_INFO must be available");
    checkCurrency(paramSensitivities);
    int nbSurfaceNode = sensitivityToRawData.get(0).size();
    double[] sensitivityRawArray = new double[nbSurfaceNode];
    Currency ccy = null;
    List<ParameterMetadata> metadataResult = null;
    for (CurrencyParameterSensitivity s : paramSensitivities.getSensitivities()) {
      ccy = s.getCurrency();
      MarketDataName<?> name = s.getMarketDataName();
      if (name instanceof SurfaceName) {
        SurfaceName surfaceName = (SurfaceName) name;
        if (volatilities.getParameters().getAlphaSurface().getName().equals(surfaceName) && dataSensitivityInfoAvailable[0]) {
          updateSensitivity(s, sensitivityToRawData.get(0), sensitivityRawArray);
          metadataResult = s.getParameterMetadata();
        }
        if (volatilities.getParameters().getBetaSurface().getName().equals(surfaceName) && dataSensitivityInfoAvailable[1]) {
          updateSensitivity(s, sensitivityToRawData.get(1), sensitivityRawArray);
          metadataResult = s.getParameterMetadata();
        }
        if (volatilities.getParameters().getRhoSurface().getName().equals(surfaceName) && dataSensitivityInfoAvailable[2]) {
          updateSensitivity(s, sensitivityToRawData.get(2), sensitivityRawArray);
          metadataResult = s.getParameterMetadata();
        }
        if (volatilities.getParameters().getNuSurface().getName().equals(surfaceName) && dataSensitivityInfoAvailable[3]) {
          updateSensitivity(s, sensitivityToRawData.get(3), sensitivityRawArray);
          metadataResult = s.getParameterMetadata();
        }
      }
    }
    DoubleArray sensitivityRaw = DoubleArray.ofUnsafe(sensitivityRawArray);
    return CurrencyParameterSensitivity.of(SurfaceName.of("RawDataParallelSensitivity"), metadataResult, ccy, sensitivityRaw);
  }

  // Update the parallel sensitivity for one of the SABR parameters
  private static void updateSensitivity(
      CurrencyParameterSensitivity s,
      List<DoubleArray> sensitivityInfoParam,
      double[] sensitivityRawArray) {

    int nbSurfaceNode = sensitivityInfoParam.size();
    ArgChecker.isTrue(s.getSensitivity().size() == nbSurfaceNode, 
        "sensitivity and surface info are not of the same size");
    for (int loopnode = 0; loopnode < nbSurfaceNode; loopnode++) {
      double sum = sensitivityInfoParam.get(loopnode).sum();
      sensitivityRawArray[loopnode] += s.getSensitivity().get(loopnode) * sum;
    }
  }

  // Check that all the sensitivities are in the same currency
  private static void checkCurrency(CurrencyParameterSensitivities paramSensitivities) {
    List<CurrencyParameterSensitivity> sensitivitiesList = paramSensitivities.getSensitivities();
    if (sensitivitiesList.size() > 0) { // When no sensitivity, no check required.
      Currency ccy = sensitivitiesList.get(0).getCurrency();
      for (int i = 1; i < sensitivitiesList.size(); i++) {
        ArgChecker.isTrue(ccy.equals(sensitivitiesList.get(i).getCurrency()),
            "sensitivities must be in the same currency for aggregation");
      }
    }
  }

}

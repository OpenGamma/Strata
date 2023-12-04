/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import com.opengamma.strata.collect.array.DoubleMatrix;

public class CorrelationsUtils {
  
  // Private constructor
  private CorrelationsUtils() {}
  
  /**
   * Returns the correlation matrix between relative returns of price time series.
   * 
   * @param priceTimeSeries  the price time series; dimensions: nbSeries x nbDates
   * @return the correlation matrix
   */
  public static DoubleMatrix correlations(double[][] priceTimeSeries) {
    int nbTimeSeries = priceTimeSeries.length;
    int nbDates = priceTimeSeries[0].length;
    double[][] relativeReturnsTimeSeries = new double[nbTimeSeries][nbDates - 1];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      for (int loopperiod = 0; loopperiod < nbDates - 1; loopperiod++) {
        relativeReturnsTimeSeries[looptimeseries][loopperiod] =
            (priceTimeSeries[looptimeseries][loopperiod + 1] - priceTimeSeries[looptimeseries][loopperiod]) /
                priceTimeSeries[looptimeseries][loopperiod];
      }
    }
    double[][] covarianceMatrix = new double[nbTimeSeries][nbTimeSeries];
    for (int looptimeseries1 = 0; looptimeseries1 < nbTimeSeries; looptimeseries1++) {
      for (int looptimeseries2 = 0; looptimeseries2 < nbTimeSeries; looptimeseries2++) {
        for (int loopperiod = 0; loopperiod < nbDates - 1; loopperiod++) {
          covarianceMatrix[looptimeseries1][looptimeseries2] +=
              relativeReturnsTimeSeries[looptimeseries1][loopperiod] *
                  relativeReturnsTimeSeries[looptimeseries2][loopperiod];
        }
      }
    }
    double[] volatility = new double[nbTimeSeries];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      volatility[looptimeseries] = Math.sqrt(covarianceMatrix[looptimeseries][looptimeseries]);
    }
    double[][] correlationMatrix = new double[nbTimeSeries][nbTimeSeries];
    for (int looptimeseries1 = 0; looptimeseries1 < nbTimeSeries; looptimeseries1++) {
      for (int looptimeseries2 = 0; looptimeseries2 < nbTimeSeries; looptimeseries2++) {
        correlationMatrix[looptimeseries1][looptimeseries2] =
            covarianceMatrix[looptimeseries1][looptimeseries2] 
                / (volatility[looptimeseries1] * volatility[looptimeseries2]);
      }
    }
    return DoubleMatrix.ofUnsafe(correlationMatrix);
  }

}

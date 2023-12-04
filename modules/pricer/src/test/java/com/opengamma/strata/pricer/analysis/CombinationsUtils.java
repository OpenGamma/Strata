/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

public class CombinationsUtils {
  
  public static double[][] combinations(double[][] ids){
    int nbIds = ids.length;
    if (nbIds == 1) {
      double[][] result = new double[ids[0].length][1];
      for (int i = 0; i < ids[0].length; i++) {
        result[i][0] = ids[0][i];
      }
      return result;
    }
    // Else recursively
    double[][] idsMinus1 = new double[nbIds - 1][];
      for (int j = 0; j < nbIds - 1; j++) {
        idsMinus1[j] = ids[j + 1];
      }
      double[][] resultMinus1 = combinations(idsMinus1);
      int nbResultMinus1 = resultMinus1.length;
      int nbIds0 = ids[0].length;
      double[][] result = new double[nbResultMinus1 * nbIds0][nbIds];
      for (int i = 0; i < nbIds0; i++) {
        for (int k = 0; k < nbResultMinus1; k++) {
          result[i * nbResultMinus1 + k][0] = ids[0][i];
          for (int l = 0; l < nbIds - 1; l++) {
            result[i * nbResultMinus1 + k][l + 1] = resultMinus1[k][l];
          }
        }
      }
    return result;
  }

}

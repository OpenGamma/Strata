/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.pricer.calibration.RawOptionData;

/**
 * Utilities for the different tests related to {@link SabrSwaptionCalibrator}
 */
public class SabrSwaptionCalibratorSmileTestUtils {

  /**
   * Create a {@link RawOptionData} object for calibration from data.
   * 
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strik-like dimension
   * @param expiries  the list of expiries
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data. 
   * @return the raw option data object
   */
  public static List<RawOptionData> rawData(
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      List<Period> expiries,
      ValueType dataType,
      double[][][] dataArray) {
    List<RawOptionData> raw = new ArrayList<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      raw.add(RawOptionData.of(strikeLikeData, strikeLikeType, expiries,
          DoubleMatrix.ofUnsafe(dataArray[looptenor]), dataType));
    }
    return raw;
  }

  public static List<RawOptionData> rawDataShift(
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      List<Period> expiries,
      ValueType dataType,
      double[][][] dataArray, int i, int j, int k, double shift) {
    List<RawOptionData> raw = new ArrayList<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      double[][] shiftedData = Arrays.stream(dataArray[looptenor])
          .map(row -> row.clone())
          .toArray(l -> new double[l][]); // deep copy of 2d array
      if (looptenor == i) {
        shiftedData[j][k] += shift;
      }
      raw.add(RawOptionData.of(strikeLikeData, strikeLikeType, expiries,
          DoubleMatrix.ofUnsafe(shiftedData), dataType));
    }
    return raw;
  }

}

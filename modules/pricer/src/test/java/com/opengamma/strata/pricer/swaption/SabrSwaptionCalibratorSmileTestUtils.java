/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static org.testng.Assert.assertTrue;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.pricer.curve.RawOptionData;

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

  /**
   * Create a {@link RawOptionData} object for calibration from data and shift one point.
   * 
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strik-like dimension
   * @param expiries  the list of expiries
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data. 
   * @param i  the index of the tenor to shift
   * @param j  the index of the expiry to shift
   * @param k  the index of the strike-like dimension to shift
   * @param shift  the size of the shift
   * @return the raw option data object
   */
  public static List<RawOptionData> rawDataShiftPoint(
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      List<Period> expiries,
      ValueType dataType,
      double[][][] dataArray,
      int i,
      int j,
      int k,
      double shift) {

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

  /**
   * Create a {@link RawOptionData} object for calibration from data and shift one smile.
   * 
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strik-like dimension
   * @param expiries  the list of expiries
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data. 
   * @param i  the index of the tenor to shift
   * @param j  the index of the expiry to shift
   * @param shift  the size of the shift
   * @return the raw option data object
   */
  public static List<RawOptionData> rawDataShiftSmile(
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      List<Period> expiries,
      ValueType dataType,
      double[][][] dataArray,
      int i,
      int j,
      double shift) {

    List<RawOptionData> raw = new ArrayList<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      double[][] shiftedData = Arrays.stream(dataArray[looptenor])
          .map(row -> row.clone())
          .toArray(l -> new double[l][]); // deep copy of 2d array
      if (looptenor == i) {
        for (int k = 0; k < shiftedData[j].length; k++) {
          shiftedData[j][k] += shift;
        }
      }
      raw.add(RawOptionData.of(strikeLikeData, strikeLikeType, expiries,
          DoubleMatrix.ofUnsafe(shiftedData), dataType));
    }
    return raw;
  }

  /**
   * Check that the results are acceptable by checking that the absolute difference or the relative difference
   * is below a given threshold.
   * 
   * @param value1  the first value
   * @param value2  the second value to compare
   * @param tolerance  the tolerance
   * @param msg  the message to return in case of failure
   */
  public static void checkAcceptable(double value1, double value2, double tolerance, String msg) {
    assertTrue((Math.abs(value1 - value2) < tolerance) ||
        (Math.abs((value1 - value2) / value2) < tolerance),
        msg);
  }

}

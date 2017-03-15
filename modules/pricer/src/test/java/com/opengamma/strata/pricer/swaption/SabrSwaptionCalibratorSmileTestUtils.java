/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static org.testng.Assert.assertTrue;

import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;

/**
 * Utilities for the different tests related to {@link SabrSwaptionCalibrator}
 */
public class SabrSwaptionCalibratorSmileTestUtils {

  /**
   * Create a {@link RawOptionData} object for calibration from data.
   * 
   * @param tenors  the list of tenors
   * @param expiries  the list of expiries
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strike-like dimension
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data.
   * @return the raw option data object
   */
  public static TenorRawOptionData rawData(
      List<Tenor> tenors,
      List<Period> expiries,
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      ValueType dataType,
      double[][][] dataArray) {

    Map<Tenor, RawOptionData> raw = new TreeMap<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      DoubleMatrix matrix = DoubleMatrix.ofUnsafe(dataArray[looptenor]);
      raw.put(tenors.get(looptenor), RawOptionData.of(expiries, strikeLikeData, strikeLikeType, matrix, dataType));
    }
    return TenorRawOptionData.of(raw);
  }

  /**
   * Create a {@link RawOptionData} object for calibration from data and shift one point.
   * 
   * @param tenors  the list of tenors
   * @param expiries  the list of expiries
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strike-like dimension
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data.
   * @param i  the index of the tenor to shift
   * @param j  the index of the expiry to shift
   * @param k  the index of the strike-like dimension to shift
   * @param shift  the size of the shift
   * @return the raw option data object
   */
  public static TenorRawOptionData rawDataShiftPoint(
      List<Tenor> tenors,
      List<Period> expiries,
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      ValueType dataType,
      double[][][] dataArray,
      int i,
      int j,
      int k,
      double shift) {

    Map<Tenor, RawOptionData> raw = new TreeMap<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      double[][] shiftedData = Arrays.stream(dataArray[looptenor])
          .map(row -> row.clone())
          .toArray(l -> new double[l][]); // deep copy of 2d array
      if (looptenor == i) {
        shiftedData[j][k] += shift;
      }
      DoubleMatrix matrix = DoubleMatrix.ofUnsafe(shiftedData);
      raw.put(tenors.get(looptenor), RawOptionData.of(expiries, strikeLikeData, strikeLikeType, matrix, dataType));
    }
    return TenorRawOptionData.of(raw);
  }

  /**
   * Create a {@link RawOptionData} object for calibration from data and shift one smile.
   * 
   * @param tenors  the list of tenors
   * @param expiries  the list of expiries
   * @param strikeLikeType  the type of the strike-like dimension
   * @param strikeLikeData  the data related to the strike-like dimension
   * @param dataType  the type of the data
   * @param dataArray  the array with the raw data, including potential Double.NaN for missing data.
   * @param i  the index of the tenor to shift
   * @param j  the index of the expiry to shift
   * @param shift  the size of the shift
   * @return the raw option data object
   */
  public static TenorRawOptionData rawDataShiftSmile(
      List<Tenor> tenors,
      List<Period> expiries,
      ValueType strikeLikeType,
      DoubleArray strikeLikeData,
      ValueType dataType,
      double[][][] dataArray,
      int i,
      int j,
      double shift) {

    Map<Tenor, RawOptionData> raw = new TreeMap<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      double[][] shiftedData = Arrays.stream(dataArray[looptenor])
          .map(row -> row.clone())
          .toArray(l -> new double[l][]); // deep copy of 2d array
      if (looptenor == i) {
        for (int k = 0; k < shiftedData[j].length; k++) {
          shiftedData[j][k] += shift;
        }
      }
      DoubleMatrix matrix = DoubleMatrix.ofUnsafe(shiftedData);
      raw.put(tenors.get(looptenor), RawOptionData.of(expiries, strikeLikeData, strikeLikeType, matrix, dataType));
    }
    return TenorRawOptionData.of(raw);
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

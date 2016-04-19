/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.meta.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.meta.TenorCurveNodeMetadata;

/**
 * Utilities to transform sensitivities.
 */
public class CurveSensitivityUtils {

  /**
   * Re-buckets a {@link CurveCurrencyParameterSensitivities} to a given set of dates. 
   * <p>
   * The list of dates must be sorted in chronological order. All sensitivities are re-bucketed to the same date list.
   * The re-bucketing is done by linear weighting on the number of days, i.e. the sensitivities for dates outside the 
   * extremes are fully bucketed to the extremes and for date between two re-bucketing dates, the weight on the start 
   * date is the number days between end date and the date re-bucketed divided by the number of days between the 
   * start and the end.
   * The input sensitivity should have a {@link DatedCurveParameterMetadata} for each sensitivity. 
   * 
   * @param sensitivities  the input sensitivities
   * @param targetDates  the list of dates for the re-bucketing
   * @return the sensitivity after the re-bucketing
   */
  public static CurveCurrencyParameterSensitivities linearRebucketing(
      CurveCurrencyParameterSensitivities sensitivities,
      List<LocalDate> targetDates) {
    checkSortedDates(targetDates);
    int nbBuckets = targetDates.size();    
    List<CurveParameterMetadata> pmdTarget = targetDates.stream()
        .map(date -> SimpleCurveNodeMetadata.of(date, date.toString()))
        .collect(toList());
    ImmutableList<CurveCurrencyParameterSensitivity> sensitivitiesList = sensitivities.getSensitivities();
    List<CurveCurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurveCurrencyParameterSensitivity sensitivity : sensitivitiesList) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      CurveMetadata metadataCurve = sensitivity.getMetadata();
      DoubleArray sensitivityAmounts = sensitivity.getSensitivity();
      List<CurveParameterMetadata> parameterMetadataList = metadataCurve.getParameterMetadata()
          .orElseThrow(() -> new IllegalArgumentException("parameter metadata must be present"));
      for (int loopnode = 0; loopnode < sensitivityAmounts.size(); loopnode++) {
        CurveParameterMetadata nodeMetadata = parameterMetadataList.get(loopnode);
        ArgChecker.isTrue(nodeMetadata instanceof DatedCurveParameterMetadata,
            "re-bucketing requires sensitivity date for node {} which is of type {} while 'DatedCurveParameterMetadata' is expected", 
            nodeMetadata.getLabel(), nodeMetadata.getClass().getName());
        DatedCurveParameterMetadata datedParameterMetadata = (DatedCurveParameterMetadata) nodeMetadata;
        LocalDate nodeDate = datedParameterMetadata.getDate();
        rebucketingArray(targetDates, rebucketedSensitivityAmounts, sensitivityAmounts.get(loopnode), nodeDate);
      }
      CurveCurrencyParameterSensitivity rebucketedSensitivity =
          CurveCurrencyParameterSensitivity.of(
              DefaultCurveMetadata.builder().curveName(sensitivity.getCurveName()).parameterMetadata(pmdTarget).build(),
              sensitivity.getCurrency(), DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
      sensitivityTarget.add(rebucketedSensitivity);
    }
    return CurveCurrencyParameterSensitivities.of(sensitivityTarget);
  }

  /**
   * Re-buckets a {@link CurveCurrencyParameterSensitivities} to a given set of dates. 
   * <p>
   * The list of dates must be sorted in chronological order. All sensitivities are re-bucketed to the same date list.
   * The re-bucketing is done by linear weighting on the number of days, i.e. the sensitivities for dates outside the 
   * extremes are fully bucketed to the extremes and for date between two re-bucketing dates, the weight on the start 
   * date is the number days between end date and the date re-bucketed divided by the number of days between the 
   * start and the end. The date of the nodes can be directly in the parameter metadata - when the metadata is of the 
   * type {@link DatedCurveParameterMetadata} - or inferred from the sensitivity date and the tenor when the
   * metadata is of the type {@link TenorCurveNodeMetadata}. Only those types of metadata are accepted.
   * 
   * @param sensitivities  the input sensitivities
   * @param targetDates  the list of dates for the re-bucketing
   * @param sensitivityDate  the date for which the sensitivities are valid
   * @return the sensitivity after the re-bucketing
   */
  public static CurveCurrencyParameterSensitivities linearRebucketing(
      CurveCurrencyParameterSensitivities sensitivities,
      List<LocalDate> targetDates,
      LocalDate sensitivityDate) {
    checkSortedDates(targetDates);
    int nbBuckets = targetDates.size(); 
    List<CurveParameterMetadata> pmdTarget = targetDates.stream()
        .map(date -> SimpleCurveNodeMetadata.of(date, date.toString()))
        .collect(toList());
    ImmutableList<CurveCurrencyParameterSensitivity> sensitivitiesList = sensitivities.getSensitivities();
    List<CurveCurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurveCurrencyParameterSensitivity sensitivity : sensitivitiesList) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      CurveMetadata metadataCurve = sensitivity.getMetadata();
      DoubleArray sensitivityAmounts = sensitivity.getSensitivity();
      List<CurveParameterMetadata> parameterMetadataList = metadataCurve.getParameterMetadata()
          .orElseThrow(() -> new IllegalArgumentException("parameter metadata must be present"));
      for (int loopnode = 0; loopnode < sensitivityAmounts.size(); loopnode++) {
        CurveParameterMetadata nodeMetadata = parameterMetadataList.get(loopnode);
        ArgChecker.isTrue((nodeMetadata instanceof DatedCurveParameterMetadata) ||
            (nodeMetadata instanceof TenorCurveNodeMetadata), 
            "re-bucketing requires sensitivity date or node for node {} which is of type {}", 
            nodeMetadata.getLabel(), nodeMetadata.getClass().getName());
        LocalDate nodeDate;
        if (nodeMetadata instanceof DatedCurveParameterMetadata) {
          DatedCurveParameterMetadata datedParameterMetadata = (DatedCurveParameterMetadata) nodeMetadata;
          nodeDate = datedParameterMetadata.getDate();
        } else {
          TenorCurveNodeMetadata tenorParameterMetadata = (TenorCurveNodeMetadata) nodeMetadata;
          nodeDate = sensitivityDate.plus(tenorParameterMetadata.getTenor());
        }
        rebucketingArray(targetDates, rebucketedSensitivityAmounts, sensitivityAmounts.get(loopnode), nodeDate);
      }
      CurveCurrencyParameterSensitivity rebucketedSensitivity =
          CurveCurrencyParameterSensitivity.of(
              DefaultCurveMetadata.builder().curveName(sensitivity.getCurveName()).parameterMetadata(pmdTarget).build(),
              sensitivity.getCurrency(), DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
      sensitivityTarget.add(rebucketedSensitivity);
    }
    return CurveCurrencyParameterSensitivities.of(sensitivityTarget);
  }

  /**
   * Re-bucket one sensitivity at a specific date and add it to an existing array.
   * 
   * @param targetDates  the list of dates for the re-bucketing
   * @param rebucketedSensitivityAmounts  the array of sensitivities; the array is modified by the method
   * @param sensitivityAmount  the value of the sensitivity at the given data
   * @param sensitivityDate  the date associated to the amount to re-bucket
   */
  private static void rebucketingArray(
      List<LocalDate> targetDates, 
      double[] rebucketedSensitivityAmounts, 
      double sensitivityAmount, 
      LocalDate sensitivityDate) {
    int nbBuckets = targetDates.size();
    if (!sensitivityDate.isAfter(targetDates.get(0))) {
      rebucketedSensitivityAmounts[0] += sensitivityAmount;
    } else if (!sensitivityDate.isBefore(targetDates.get(nbBuckets - 1))) {
      rebucketedSensitivityAmounts[nbBuckets - 1] += sensitivityAmount;
    } else {
      int indexSensitivityDate = 0;
      while (sensitivityDate.isAfter(targetDates.get(indexSensitivityDate))) {
        indexSensitivityDate++;
      } // 'indexSensitivityDate' contains the index of the node after the sensitivity date 
      long intervalLength = targetDates.get(indexSensitivityDate).toEpochDay() - targetDates.get(indexSensitivityDate - 1).toEpochDay();
      double weight = ((double) (targetDates.get(indexSensitivityDate).toEpochDay() - sensitivityDate.toEpochDay())) / intervalLength;
      rebucketedSensitivityAmounts[indexSensitivityDate - 1] += weight * sensitivityAmount;
      rebucketedSensitivityAmounts[indexSensitivityDate] += (1.0d - weight) * sensitivityAmount;
    }
  }

  // Check that the dates in the list are sorted in chronological order.
  private static void checkSortedDates(List<LocalDate> dates) {
    for (int loopdate = 0; loopdate < dates.size() - 1; loopdate++) { 
      ArgChecker.inOrderNotEqual(dates.get(loopdate), dates.get(loopdate + 1), "first date", "following date");
    }
  }

}

/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

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
    int nbBuckets = targetDates.size();
    List<CurveParameterMetadata> pmdTarget = new ArrayList<>();
    for (int i = 0; i < nbBuckets; i++) {
      pmdTarget.add(SimpleCurveNodeMetadata.of(targetDates.get(i), targetDates.get(i).toString()));
    }
    ImmutableList<CurveCurrencyParameterSensitivity> listSensi = sensitivities.getSensitivities();
    List<CurveCurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurveCurrencyParameterSensitivity s : listSensi) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      CurveMetadata m = s.getMetadata();
      DoubleArray sa = s.getSensitivity();
      ArgChecker.isTrue(m.getParameterMetadata().isPresent(), "parameter metadata must be present");
      List<CurveParameterMetadata> lm = m.getParameterMetadata().get();
      for (int loopnode = 0; loopnode < sa.size(); loopnode++) {
        ArgChecker.isTrue(lm.get(loopnode) instanceof DatedCurveParameterMetadata,
            "re-bucketing require sensitivity date");
        DatedCurveParameterMetadata dpm = (DatedCurveParameterMetadata) lm.get(loopnode);
        LocalDate nodeDate = dpm.getDate();
        rebucketingArray(targetDates, nbBuckets, rebucketedSensitivityAmounts, sa.get(loopnode), nodeDate);
      }
      CurveCurrencyParameterSensitivity rebucketedSensitivity =
          CurveCurrencyParameterSensitivity.of(
              DefaultCurveMetadata.builder().curveName(s.getCurveName()).parameterMetadata(pmdTarget).build(),
              s.getCurrency(), DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
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
   * start and the end.
   * The input sensitivity should have a {@link DatedCurveParameterMetadata} for each sensitivity. 
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
    int nbBuckets = targetDates.size();
    List<CurveParameterMetadata> pmdTarget = new ArrayList<>();
    for (int i = 0; i < nbBuckets; i++) {
      pmdTarget.add(SimpleCurveNodeMetadata.of(targetDates.get(i), targetDates.get(i).toString()));
    }
    ImmutableList<CurveCurrencyParameterSensitivity> listSensi = sensitivities.getSensitivities();
    List<CurveCurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurveCurrencyParameterSensitivity s : listSensi) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      CurveMetadata m = s.getMetadata();
      DoubleArray sa = s.getSensitivity();
      ArgChecker.isTrue(m.getParameterMetadata().isPresent(), "parameter metadata must be present");
      List<CurveParameterMetadata> lm = m.getParameterMetadata().get();
      for (int loopnode = 0; loopnode < sa.size(); loopnode++) {
        ArgChecker.isTrue((lm.get(loopnode) instanceof DatedCurveParameterMetadata) ||
            (lm.get(loopnode) instanceof TenorCurveNodeMetadata), "re-bucketing require sensitivity date");
        LocalDate nodeDate;
        if (lm.get(loopnode) instanceof DatedCurveParameterMetadata) {
          DatedCurveParameterMetadata dpm = (DatedCurveParameterMetadata) lm.get(loopnode);
          nodeDate = dpm.getDate();
        } else {
          TenorCurveNodeMetadata tpm = (TenorCurveNodeMetadata) lm.get(loopnode);
          nodeDate = sensitivityDate.plus(tpm.getTenor());
        }
        rebucketingArray(targetDates, nbBuckets, rebucketedSensitivityAmounts, sa.get(loopnode), nodeDate);
      }
      CurveCurrencyParameterSensitivity rebucketedSensitivity =
          CurveCurrencyParameterSensitivity.of(
              DefaultCurveMetadata.builder().curveName(s.getCurveName()).parameterMetadata(pmdTarget).build(),
              s.getCurrency(), DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
      sensitivityTarget.add(rebucketedSensitivity);
    }
    return CurveCurrencyParameterSensitivities.of(sensitivityTarget);
  }

  /**
   * 
   * @param targetDates
   * @param nbBuckets
   * @param rebucketedSensitivityAmounts  the array of sensitivities; the array is modified by the method
   * @param sa
   * @param nodeDate  the date associated to the node re-bucketed
   */
  private static void rebucketingArray(
      List<LocalDate> targetDates, 
      int nbBuckets, 
      double[] rebucketedSensitivityAmounts, 
      double sa, 
      LocalDate nodeDate) {
    if (!nodeDate.isAfter(targetDates.get(0))) {
      rebucketedSensitivityAmounts[0] += sa;
    } else if (!nodeDate.isBefore(targetDates.get(nbBuckets - 1))) {
      rebucketedSensitivityAmounts[nbBuckets - 1] += sa;
    } else {
      int k = 0;
      while (nodeDate.isAfter(targetDates.get(k))) {
        k++;
      } // k contains the index of the node after "dpm" date 
      long intervalLength = targetDates.get(k).toEpochDay() - targetDates.get(k - 1).toEpochDay();
      double weight = ((double) (targetDates.get(k).toEpochDay() - nodeDate.toEpochDay())) / intervalLength;
      rebucketedSensitivityAmounts[k - 1] += weight * sa;
      rebucketedSensitivityAmounts[k] += (1.0d - weight) * sa;
    }
  }

}

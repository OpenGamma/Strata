/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Utilities to transform sensitivities.
 */
public class CurveSensitivityUtils {
  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new CommonsMatrixAlgebra();

  /**
   * Construct the inverse Jacobian matrix from the sensitivities of the trades market quotes to the curve parameters.
   * <p>
   * All the trades and sensitivities must be in the same currency. The data should be coherent with the
   * market quote sensitivities passed in an order coherent with the list of curves.
   * <p>
   * For each trade describing the market quotes, the sensitivity provided should be the sensitivity of that
   * market quote to the curve parameters.
   * 
   * @param curveOrder  the order in which the curves should be represented in the jacobian
   * @param marketQuoteSensitivities  the market quotes sensitivity to the curve parameters
   * @return inverse jacobian matrix, which correspond to the sensitivity of the parameters to the market quotes
   */
  public static DoubleMatrix jacobianFromMarketQuoteSensitivities(
      List<CurveParameterSize> curveOrder,
      List<CurrencyParameterSensitivities> marketQuoteSensitivities) {

    Currency ccy = marketQuoteSensitivities.get(0).getSensitivities().get(0).getCurrency();
    DoubleMatrix jacobianMatrix = DoubleMatrix.ofArrayObjects(
        marketQuoteSensitivities.size(),
        marketQuoteSensitivities.size(),
        i -> row(curveOrder, marketQuoteSensitivities.get(i), ccy));
    return MATRIX_ALGEBRA.getInverse(jacobianMatrix);
  }

  /**
   * Computes the row corresponding to a trade for the Jacobian matrix.
   * 
   * @param curveOrder  the curve order
   * @param sensitivities  the sensitivities 
   * @param ccy  the currency common to all sensitivities
   * @return the row
   */
  private static DoubleArray row(
      List<CurveParameterSize> curveOrder,
      CurrencyParameterSensitivities parameterSensitivities,
      Currency ccy) {

    DoubleArray row = DoubleArray.EMPTY;
    for (CurveParameterSize curveNameAndSize : curveOrder) {
      Optional<CurrencyParameterSensitivity> sensitivityOneCurve =
          parameterSensitivities.findSensitivity(curveNameAndSize.getName(), ccy);
      if (sensitivityOneCurve.isPresent()) {
        row = row.concat(sensitivityOneCurve.get().getSensitivity());
      } else {
        row = row.concat(DoubleArray.filled(curveNameAndSize.getParameterCount()));
      }
    }
    return row;
  }

  /**
   * Construct the inverse Jacobian matrix from the trades and a function used to compute the sensitivities of the 
   * market quotes to the curve parameters.
   * <p>
   * All the trades must be in the same currency. The trades should be coherent with the curves order.
   * 
   * @param curveOrder  the order in which the curves should be represented in the jacobian
   * @param trades  the list of trades
   * @param sensitivityFunction  the function from a trade to the market quote sensitivity to curve parameters
   * @return inverse jacobian matrix, which correspond to the sensitivity of the parameters to the market quotes
   */
  public static DoubleMatrix jacobianFromMarketQuoteSensitivities(
      List<CurveParameterSize> curveOrder,
      List<ResolvedTrade> trades,
      Function<ResolvedTrade, CurrencyParameterSensitivities> sensitivityFunction) {

    List<CurrencyParameterSensitivities> marketQuoteSensitivities = new ArrayList<>();
    for (ResolvedTrade t : trades) {
      marketQuoteSensitivities.add(sensitivityFunction.apply(t));
    }
    return jacobianFromMarketQuoteSensitivities(curveOrder, marketQuoteSensitivities);
  }

  /**
   * Re-buckets a {@link CurrencyParameterSensitivities} to a given set of dates.
   * <p>
   * The list of dates must be sorted in chronological order. All sensitivities are re-bucketed to the same date list.
   * The re-bucketing is done by linear weighting on the number of days, i.e. the sensitivities for dates outside the 
   * extremes are fully bucketed to the extremes and for date between two re-bucketing dates, the weight on the start 
   * date is the number days between end date and the date re-bucketed divided by the number of days between the 
   * start and the end.
   * The input sensitivity should have a {@link DatedParameterMetadata} for each sensitivity.
   * 
   * @param sensitivities  the input sensitivities
   * @param targetDates  the list of dates for the re-bucketing
   * @return the sensitivity after the re-bucketing
   */
  public static CurrencyParameterSensitivities linearRebucketing(
      CurrencyParameterSensitivities sensitivities,
      List<LocalDate> targetDates) {

    checkSortedDates(targetDates);
    int nbBuckets = targetDates.size();
    List<ParameterMetadata> pmdTarget = targetDates.stream()
        .map(date -> LabelDateParameterMetadata.of(date, date.toString()))
        .collect(toList());
    ImmutableList<CurrencyParameterSensitivity> sensitivitiesList = sensitivities.getSensitivities();
    List<CurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurrencyParameterSensitivity sensitivity : sensitivitiesList) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      DoubleArray sensitivityAmounts = sensitivity.getSensitivity();
      List<ParameterMetadata> parameterMetadataList = sensitivity.getParameterMetadata();
      for (int loopnode = 0; loopnode < sensitivityAmounts.size(); loopnode++) {
        ParameterMetadata nodeMetadata = parameterMetadataList.get(loopnode);
        ArgChecker.isTrue(nodeMetadata instanceof DatedParameterMetadata,
            "re-bucketing requires sensitivity date for node {} which is of type {} while 'DatedParameterMetadata' is expected",
            nodeMetadata.getLabel(), nodeMetadata.getClass().getName());
        DatedParameterMetadata datedParameterMetadata = (DatedParameterMetadata) nodeMetadata;
        LocalDate nodeDate = datedParameterMetadata.getDate();
        rebucketingArray(targetDates, rebucketedSensitivityAmounts, sensitivityAmounts.get(loopnode), nodeDate);
      }
      CurrencyParameterSensitivity rebucketedSensitivity = CurrencyParameterSensitivity.of(
          sensitivity.getMarketDataName(),
          pmdTarget,
          sensitivity.getCurrency(),
          DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
      sensitivityTarget.add(rebucketedSensitivity);
    }
    return CurrencyParameterSensitivities.of(sensitivityTarget);
  }

  /**
   * Re-buckets a {@link CurrencyParameterSensitivities} to a given set of dates.
   * <p>
   * The list of dates must be sorted in chronological order. All sensitivities are re-bucketed to the same date list.
   * The re-bucketing is done by linear weighting on the number of days, i.e. the sensitivities for dates outside the 
   * extremes are fully bucketed to the extremes and for date between two re-bucketing dates, the weight on the start 
   * date is the number days between end date and the date re-bucketed divided by the number of days between the 
   * start and the end. The date of the nodes can be directly in the parameter metadata - when the metadata is of the 
   * type {@link DatedParameterMetadata} - or inferred from the sensitivity date and the tenor when the
   * metadata is of the type {@link TenorParameterMetadata}. Only those types of metadata are accepted.
   * 
   * @param sensitivities  the input sensitivities
   * @param targetDates  the list of dates for the re-bucketing
   * @param sensitivityDate  the date for which the sensitivities are valid
   * @return the sensitivity after the re-bucketing
   */
  public static CurrencyParameterSensitivities linearRebucketing(
      CurrencyParameterSensitivities sensitivities,
      List<LocalDate> targetDates,
      LocalDate sensitivityDate) {

    checkSortedDates(targetDates);
    int nbBuckets = targetDates.size();
    List<ParameterMetadata> pmdTarget = targetDates.stream()
        .map(date -> LabelDateParameterMetadata.of(date, date.toString()))
        .collect(toList());
    ImmutableList<CurrencyParameterSensitivity> sensitivitiesList = sensitivities.getSensitivities();
    List<CurrencyParameterSensitivity> sensitivityTarget = new ArrayList<>();
    for (CurrencyParameterSensitivity sensitivity : sensitivitiesList) {
      double[] rebucketedSensitivityAmounts = new double[nbBuckets];
      DoubleArray sensitivityAmounts = sensitivity.getSensitivity();
      List<ParameterMetadata> parameterMetadataList = sensitivity.getParameterMetadata();
      for (int loopnode = 0; loopnode < sensitivityAmounts.size(); loopnode++) {
        ParameterMetadata nodeMetadata = parameterMetadataList.get(loopnode);
        ArgChecker.isTrue((nodeMetadata instanceof DatedParameterMetadata) ||
            (nodeMetadata instanceof TenorParameterMetadata),
            "re-bucketing requires sensitivity date or node for node {} which is of type {}",
            nodeMetadata.getLabel(), nodeMetadata.getClass().getName());
        LocalDate nodeDate;
        if (nodeMetadata instanceof DatedParameterMetadata) {
          DatedParameterMetadata datedParameterMetadata = (DatedParameterMetadata) nodeMetadata;
          nodeDate = datedParameterMetadata.getDate();
        } else {
          TenorParameterMetadata tenorParameterMetadata = (TenorParameterMetadata) nodeMetadata;
          nodeDate = sensitivityDate.plus(tenorParameterMetadata.getTenor());
        }
        rebucketingArray(targetDates, rebucketedSensitivityAmounts, sensitivityAmounts.get(loopnode), nodeDate);
      }
      CurrencyParameterSensitivity rebucketedSensitivity = CurrencyParameterSensitivity.of(
          sensitivity.getMarketDataName(),
          pmdTarget,
          sensitivity.getCurrency(),
          DoubleArray.ofUnsafe(rebucketedSensitivityAmounts));
      sensitivityTarget.add(rebucketedSensitivity);
    }
    return CurrencyParameterSensitivities.of(sensitivityTarget);
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
      long intervalLength =
          targetDates.get(indexSensitivityDate).toEpochDay() - targetDates.get(indexSensitivityDate - 1).toEpochDay();
      double weight =
          ((double) (targetDates.get(indexSensitivityDate).toEpochDay() - sensitivityDate.toEpochDay())) / intervalLength;
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

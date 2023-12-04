/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link CorrelationsUtils}.
 */
public class CorrelationsUtilsTest {

  private static final Offset<Double> TOLERANCE_CORRELATION = Offset.offset(1.0E-8);

  @Test
  public void trivial() {
    double[] prices = {1.0, 1.1, 1.2, 1.1, 1.2, 0.9, 0.8, 1.0};
    int nbDates = prices.length;
    int nbTimeSeries = 3;
    double[][] pricesTimeSeries = new double[nbTimeSeries][nbDates];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      for (int loopdate = 0; loopdate < nbDates; loopdate++) {
        pricesTimeSeries[looptimeseries][loopdate] = prices[loopdate];
      }
    }
    DoubleMatrix correlationsComputed = CorrelationsUtils.correlations(pricesTimeSeries);
    assertThat(correlationsComputed.columnCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.rowCount()).isEqualTo(nbTimeSeries);
    for (int looptimeseries1 = 0; looptimeseries1 < nbTimeSeries; looptimeseries1++) {
      for (int looptimeseries2 = 0; looptimeseries2 < nbTimeSeries; looptimeseries2++) {
        assertThat(correlationsComputed.get(looptimeseries1, looptimeseries2))
            .isCloseTo(1.0d, TOLERANCE_CORRELATION);
      }
    }
  }
  
  @Test
  public void multiple() {
    double[] prices = {1.0, 1.1, 1.2, 1.1, 1.2, 0.9, 0.8, 1.0};
    int nbDates = prices.length;
    int nbTimeSeries = 3;
    double[][] pricesTimeSeries = new double[nbTimeSeries][nbDates];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      for (int loopdate = 0; loopdate < nbDates; loopdate++) {
        pricesTimeSeries[looptimeseries][loopdate] = (loopdate + 1) * prices[loopdate];
      }
    }
    DoubleMatrix correlationsComputed = CorrelationsUtils.correlations(pricesTimeSeries);
    assertThat(correlationsComputed.columnCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.rowCount()).isEqualTo(nbTimeSeries);
    for (int looptimeseries1 = 0; looptimeseries1 < nbTimeSeries; looptimeseries1++) {
      for (int looptimeseries2 = 0; looptimeseries2 < nbTimeSeries; looptimeseries2++) {
        assertThat(correlationsComputed.get(looptimeseries1, looptimeseries2))
            .isCloseTo(1.0d, TOLERANCE_CORRELATION);
      }
    }
  }
  
  @Test
  public void minus1() {
    double[] returns = {0.1, 0.1, -0.1, 0.2, -0.1, -0.1, 0.0};
    int nbDates = returns.length + 1;
    int nbTimeSeries = 5;
    double[][] pricesTimeSeries = new double[nbTimeSeries][nbDates];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      pricesTimeSeries[looptimeseries][0] = 1.0;
      for (int loopdate = 0; loopdate < nbDates - 1; loopdate++) {
        pricesTimeSeries[looptimeseries][loopdate + 1] =
            pricesTimeSeries[looptimeseries][loopdate] *
                ((looptimeseries % 2 == 0) ? (1.0d + returns[loopdate]) : (1.0d - returns[loopdate]));
      }
    }
    DoubleMatrix correlationsComputed = CorrelationsUtils.correlations(pricesTimeSeries);
    assertThat(correlationsComputed.columnCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.rowCount()).isEqualTo(nbTimeSeries);
    for (int looptimeseries1 = 0; looptimeseries1 < nbTimeSeries; looptimeseries1++) {
      for (int looptimeseries2 = 0; looptimeseries2 < nbTimeSeries; looptimeseries2++) {
        assertThat(correlationsComputed.get(looptimeseries1, looptimeseries2))
            .isCloseTo(((looptimeseries1 % 2 == looptimeseries2 % 2) ? 1 : -1), TOLERANCE_CORRELATION);
      }
    }
  }
  
  @Test
  public void zero() {
    double[][] returns = {
        {0.1, 0.1, 0.0, 0.2, -0.2, -0.1, 0.1},
        {0.1, -0.1, 0.3, 0.0, 0.1, -0.3, -0.1}};
    int nbDates = returns[0].length + 1;
    int nbTimeSeries = returns.length;
    double[][] pricesTimeSeries = new double[nbTimeSeries][nbDates];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      pricesTimeSeries[looptimeseries][0] = 1.0;
      for (int loopdate = 0; loopdate < nbDates - 1; loopdate++) {
        pricesTimeSeries[looptimeseries][loopdate + 1] =
            pricesTimeSeries[looptimeseries][loopdate] * (1.0d + returns[looptimeseries][loopdate]);
      }
    }
    DoubleMatrix correlationsComputed = CorrelationsUtils.correlations(pricesTimeSeries);
    assertThat(correlationsComputed.columnCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.rowCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.get(0, 0)).isCloseTo(1.0d, TOLERANCE_CORRELATION);
    assertThat(correlationsComputed.get(1, 0)).isCloseTo(0.0d, TOLERANCE_CORRELATION);
    assertThat(correlationsComputed.get(0, 1)).isCloseTo(correlationsComputed.get(1, 0), TOLERANCE_CORRELATION);
    assertThat(correlationsComputed.get(1, 1)).isCloseTo(1.0d, TOLERANCE_CORRELATION);
  }
  
  @Test
  public void other() {
    double[][] returns = {
        {0.1, 0.1, 0.0, 0.2, -0.2, -0.1, 0.1},
        {0.1, 0.2, 0.3, 0.4, 0.3, 0.2, 0.1}};
    int nbDates = returns[0].length + 1;
    int nbTimeSeries = returns.length;
    double[][] pricesTimeSeries = new double[nbTimeSeries][nbDates];
    for (int looptimeseries = 0; looptimeseries < nbTimeSeries; looptimeseries++) {
      pricesTimeSeries[looptimeseries][0] = 1.0;
      for (int loopdate = 0; loopdate < nbDates - 1; loopdate++) {
        pricesTimeSeries[looptimeseries][loopdate + 1] =
            pricesTimeSeries[looptimeseries][loopdate] * (1.0d + returns[looptimeseries][loopdate]);
      }
    }
    DoubleMatrix correlationsComputed = CorrelationsUtils.correlations(pricesTimeSeries);
    assertThat(correlationsComputed.columnCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.rowCount()).isEqualTo(nbTimeSeries);
    assertThat(correlationsComputed.get(0, 0)).isCloseTo(1.0d, TOLERANCE_CORRELATION);
    assertThat(correlationsComputed.get(1, 0)).isCloseTo(0.174077656, TOLERANCE_CORRELATION); 
        // Hard-coded value from external computation
    assertThat(correlationsComputed.get(0, 1)).isCloseTo(correlationsComputed.get(1, 0), TOLERANCE_CORRELATION);
    assertThat(correlationsComputed.get(1, 1)).isCloseTo(1.0d, TOLERANCE_CORRELATION);
  }

}

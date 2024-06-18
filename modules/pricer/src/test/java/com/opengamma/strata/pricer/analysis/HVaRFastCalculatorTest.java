/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.random.SobolSequenceGenerator;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.math.impl.statistics.descriptive.MidwayInterpolationQuantileMethod;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

// TODO: test Sobol sequences

public class HVaRFastCalculatorTest {


  /* Calibrated parameters */
  private static final DoubleArray STANDALONE_IM_UP = DoubleArray.of(1.0, 1.5, 2.0); // For short
  private static final DoubleArray STANDALONE_IM_DOWN = DoubleArray.of(0.75, 1.10, 1.60); // For long // 0.75, 1.10, 1.60
  private static final int NB_RISK_FACTORS = STANDALONE_IM_UP.size();

  private static final DoubleMatrix IMPLIED_CORRELATION = DoubleMatrix.ofUnsafe(
      new double[][]
          {{1.00d, 0.98d, 0.50d},
              {0.98d, 1.00d, 0.49d},
              {0.50d, 0.49d, 1.00d}}
  );

  /* Data */
  private static final DoubleArray PRICES = DoubleArray.of(4.0d, 6.0d, 8.0d);
  private static final double[][] IMPLIED_VOL = // dimensions: underlying x options
      {{0.25d, 0.30d},
          {},
          {0.30d, 0.31d, 0.29d}}; // 4, 6, 8

  /* Portfolio */
  private static final DoubleArray QUANTITIES_FUTURES = DoubleArray.of(10.0d, -5.0d, 2.0d);
  private static final double[][] STRIKES = // dimensions: underlying x options
      {{4.0, 4.5},
          {},
          {7.0, 7.5, 9.0}}; // 4, 6, 8
  private static final double[][] TIMES = // dimensions: underlying x options
      {{0.5, 0.25},
          {},
          {0.1, 0.2, 0.3}};
  private static final boolean[][] IS_CALL = // dimensions: underlying x options
      {{true, false},
          {},
          {true, false, true}};
  private static final double[][] QUANTITIES_OPTIONS =// dimensions: underlying x options
      {{1.0d, 3.0d},
          {},
          {-2.0d, 1.0d, 2.0d}};

  /* Inner Model settings */
  private static final QuantileCalculationMethod QUANTILE_METHOD = MidwayInterpolationQuantileMethod.DEFAULT;
  private static final double LEVEL = 0.99; // relative
  /* Normal distribution to generate the Monte Carlo scenarios. */
  private static final NormalDistribution NORMAL = new NormalDistribution(0.0d, 1.0d);
  private static final double VOLATILITY_TO_IM_FACTOR = NORMAL.getInverseCDF(LEVEL);

  /* Positions with one futures only. Test standalone IM recovery. */
  // Random generator: 1_000 -> 15.50% // 8_000 -> 3.70% // 64K - 1.00% // 256K -> 0.70%
  // Sobol's sequence: 1_000 -> 3.90% // 8_000 -> 0.61% // 64K - 0.08%
  @Test
  void single_futures() {
    int nbOuterScenarios = 1; // Not used in Sobol's sequence implementation
    int nbInnerScenarios = 64_000;
    HVaRFastCalculator varCalculator = HVaRFastCalculator.DEFAULT;
    List<BoundSurfaceInterpolator> priceFunctions = new ArrayList<>();
    double[][] quantitiesOptions =// dimensions: underlying x options
        {{0.0d, 0.0d},
            {},
            {0.0d, 0.0d, 0.0d}}; // To test replication
    // Short positions
    for (int i = 0; i < NB_RISK_FACTORS; i++) {
      int i2 = i;
      DoubleArray quantitiesFutures = DoubleArray.of(NB_RISK_FACTORS, (k) -> (i2 == k) ? -10 : 0);
      double imSimple = STANDALONE_IM_UP.multipliedBy(quantitiesFutures.map(Math::abs)).sum();
      priceFunctions = new ArrayList<>();
      DoubleArray ims = im_test(varCalculator, quantitiesFutures, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
      checkIm(ims, imSimple);
    }
    // Long positions
    for (int i = 0; i < NB_RISK_FACTORS; i++) {
      int i2 = i;
      DoubleArray quantitiesFutures = DoubleArray.of(NB_RISK_FACTORS, (k) -> (i2 == k) ? 10 : 0);
      double imSimple = STANDALONE_IM_DOWN.multipliedBy(quantitiesFutures).sum();
      priceFunctions = new ArrayList<>();
      DoubleArray ims = im_test(varCalculator, quantitiesFutures, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
      checkIm(ims, imSimple);
    }
  }

  /* Positions with futures only. Test IM recovery using correlation. */
  @Test
  void correlation_futures() { // TODO: long/short?
    int nbOuterScenarios = 10;
    int nbInnerScenarios = 1_000;
    HVaRFastCalculator varCalculator = HVaRFastCalculator.DEFAULT;
    List<BoundSurfaceInterpolator> priceFunctions = new ArrayList<>();
    double[][] quantitiesOptions =// dimensions: underlying x options
        {{0.0d, 0.0d},
            {},
            {0.0d, 0.0d, 0.0d}}; // To test replication
    DoubleArray quantitiesFutures0 = DoubleArray.of(10, -5, 0);
    DoubleArray ims0 =
        im_test(varCalculator, quantitiesFutures0, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    checkIm(ims0, 3.041381265); // Hardcode from Excel
    // 1_000 -> 9.70% // 8_000 -> 3.60%

    DoubleArray quantitiesFutures1 = DoubleArray.of(10, 0, -5);
    priceFunctions = new ArrayList<>();
    DoubleArray ims1 =
        im_test(varCalculator, quantitiesFutures1, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    checkIm(ims1, 10.000);// Hardcode Excel
    // 1_000 -> 10.10% // 8_000 -> 3.70%

    DoubleArray quantitiesFutures2 = DoubleArray.of(10, 5, 0);
    priceFunctions = new ArrayList<>();
    DoubleArray ims2 =
        im_test(varCalculator, quantitiesFutures2, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    checkIm(ims2, 17.41407477); // Hardcode Excel
    // 1_000 -> 11.70% // 8_000 -> 2.90%

    DoubleArray quantitiesFutures3 = DoubleArray.of(10, -5, 2);
    priceFunctions = new ArrayList<>();
    DoubleArray ims3 =
        im_test(varCalculator, quantitiesFutures3, quantitiesOptions, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    checkIm(ims3, 5.987486952); // Hardcode Excel
    // 1_000 -> 6.40% // 8_000 -> 3.10%
  }

  /* Positions with one option only. Test IM recovery using futures IM as shock. */
  @Test
  void single_option() {
    int nbOuterScenarios = 1; // Not used in Sobol's sequence implementation
    int nbInnerScenarios = 64_000;
    HVaRFastCalculator varCalculator = HVaRFastCalculator.DEFAULT;
    DoubleArray quantitiesFutures = DoubleArray.filled(NB_RISK_FACTORS, 0);
    DoubleArray pricePlusShock = PRICES.plus(STANDALONE_IM_UP);
    DoubleArray priceMinusShock = PRICES.minus(STANDALONE_IM_DOWN);
    List<BoundSurfaceInterpolator> priceFunctions = new ArrayList<>();

    double[][] quantitiesOptions0 =// dimensions: underlying x options
        {{1.0d, 0.0d},
            {},
            {0.0d, 0.0d, 0.0d}}; // To test replication
    DoubleArray ims0 = im_test(varCalculator, quantitiesFutures, quantitiesOptions0, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    double pvBase0 = priceFunctions.get(0).interpolate(PRICES.get(0), 1.0);
    double pvShock0 = priceFunctions.get(0).interpolate(priceMinusShock.get(0), 1.0);
    double imExpected0 = pvBase0 - pvShock0;
    checkIm(ims0, imExpected0);
    // Random generator: 1_250 -> 3.60% // 8_000 -> 1.20% // 64_000 -> 0.50%
    // Sobol's sequence: 1_250 -> 0.56% // 8_000 -> 0.05% // 64_000 -> 0.01%

    double[][] quantitiesOptions1 =// dimensions: underlying x options
        {{-10.0d, 0.0d},
            {},
            {0.0d, 0.0d, 0.0d}}; // To test replication
    priceFunctions = new ArrayList<>();
    DoubleArray ims1 = im_test(varCalculator, quantitiesFutures, quantitiesOptions1, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    double pvBase1 = priceFunctions.get(0).interpolate(PRICES.get(0), 1.0);
    double pvShock1 = priceFunctions.get(0).interpolate(pricePlusShock.get(0), 1.0);
    double imExpected1 = Math.abs(pvShock1 - pvBase1);
    checkIm(ims1, imExpected1);
    // Random generator: 1_250 -> 10.70% // 8_000 -> 4.30% // 64_000 -> 1.50%
    // Sobol's sequence: 1_250 -> 0.50% // 8_000 -> 0.14% // 64_000 -> 0.01%

    double[][] quantitiesOptions2 =// dimensions: underlying x options
        {{0.0d, 0.0d},
            {},
            {0.0d, 10.0d, 0.0d}}; // To test replication
    priceFunctions = new ArrayList<>();
    DoubleArray ims2 = im_test(varCalculator, quantitiesFutures, quantitiesOptions1, nbOuterScenarios, nbInnerScenarios, priceFunctions);
    double pvBase2 = priceFunctions.get(0).interpolate(PRICES.get(0), 1.0);
    double pvShock2 = priceFunctions.get(0).interpolate(pricePlusShock.get(0), 1.0);
    double imExpected2 = Math.abs(pvShock2 - pvBase2);
    checkIm(ims2, imExpected2);
    // Random generator: 1_250 -> 0.50% // 8_000 -> 3.70% // 64_000 -> 1.10%
    // Sobol's sequence: 1_250 -> 3.60% // 8_000 -> 0.14% // 64_000 -> 0.01%
  }

  private DoubleArray im_test(
      HVaRFastCalculator varCalculator,
      DoubleArray quantitiesFutures,
      double[][] quantitiesOptions,
      int nbOuterScenarios,
      int nbInnerScenarios,
      List<BoundSurfaceInterpolator> priceFunctions) {

    System.out.println("test");
    DoubleArray standAloneImImpliedVolatilitiesUp = STANDALONE_IM_UP.dividedBy(PRICES).dividedBy(VOLATILITY_TO_IM_FACTOR);
    DoubleArray standAloneImImpliedVolatilitiesDown = STANDALONE_IM_DOWN.dividedBy(PRICES).dividedBy(VOLATILITY_TO_IM_FACTOR);
    List<DoubleArray> pricesOuter = generatePriceOuterScenariosBase(nbOuterScenarios);
    List<double[][]> scenariosPrices = varCalculator.generatePriceInnerScenarios(
        pricesOuter,
        standAloneImImpliedVolatilitiesUp,
        standAloneImImpliedVolatilitiesDown,
        IMPLIED_CORRELATION,
        nbInnerScenarios);
    Pair<DoubleArray, DoubleArray> priceRangeGrid = priceRange(scenariosPrices);
    DoubleArray priceRangeGridMin = priceRangeGrid.getFirst();
    DoubleArray priceRangeGridMax = priceRangeGrid.getSecond();
    List<double[][]> scenariosImpliedVol = varCalculator
        .generateImpliedVolInnerScenarios(NB_RISK_FACTORS,nbOuterScenarios, nbInnerScenarios);
    priceFunctions.addAll(priceFunctions(
        quantitiesFutures, STRIKES, TIMES, IS_CALL, quantitiesOptions, IMPLIED_VOL,
        priceRangeGridMin, priceRangeGridMax));
    List<DoubleArray> scenariosPl = varCalculator.
        generateScenariosPl(priceFunctions, pricesOuter, scenariosPrices, scenariosImpliedVol);
    return DoubleArray.of(nbOuterScenarios, (i) -> QUANTILE_METHOD.quantileFromUnsorted(LEVEL, scenariosPl.get(i)));
  }

  /**
   * Check the IMs is in the acceptable range.
   * @param ims  the Monte Carlo IMs
   * @param imExpected  the expected IM
   */
  void checkIm(DoubleArray ims, double imExpected){
    int nbOuterScenarios = ims.size();
    DoubleArray relativeError = ims.minus(imExpected).dividedBy(imExpected);
    System.out.println(relativeError);
    double maxError = relativeError.map(Math::abs).max();
    double average = relativeError.sum() / nbOuterScenarios;
    double stddev = Math.sqrt(
        relativeError.minus(average).multipliedBy(relativeError.minus(average)).sum() / nbOuterScenarios);
    System.out.println("  |--> Max: " + maxError);
    System.out.println("  |--> Mean: " + average);
    System.out.println("  |--> Std Dev: " + stddev);
    assertThat(maxError).isCloseTo(0, Offset.offset(0.15));
  }

  /* Outer scenarios: all similar to base */
  private static List<DoubleArray> generatePriceOuterScenariosBase(int nbOuterScenarios) {
    double priceRange = 0.50;
    List<DoubleArray> pricesOuter = new ArrayList<>(); // dimension outer sc / risk factors
    for (int loopoutersc = 0; loopoutersc < nbOuterScenarios; loopoutersc++) {
      pricesOuter.add(PRICES);
    }
    return pricesOuter;
  }

  /* Price range to create the interpolation grid */
  private static Pair<DoubleArray, DoubleArray> priceRange(List<double[][]> scenariosPrices){
    int nbRiskFactors = scenariosPrices.get(0)[0].length;
    double[] priceRangeGridMinTmp = new double[nbRiskFactors]; // Lowest price in all scenarios; for grid - dimension: risk factors
    double[] priceRangeGridMaxTmp = new double[nbRiskFactors]; // Highest price in all scenarios; for grid
    Arrays.fill(priceRangeGridMinTmp, Double.MAX_VALUE);
    int nbOuterScenarios = scenariosPrices.size();
    int nbInnerScenarios = scenariosPrices.get(0).length;
    for (double[][] innerScenarios : scenariosPrices) {
      for (int loopinner = 0; loopinner < nbInnerScenarios; loopinner++) {
        for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
          priceRangeGridMinTmp[looprf] =
              Math.min(priceRangeGridMinTmp[looprf], innerScenarios[loopinner][looprf]);
          priceRangeGridMaxTmp[looprf] =
              Math.max(priceRangeGridMaxTmp[looprf], innerScenarios[loopinner][looprf]);
        }
      }
    }
    return Pair.of(DoubleArray.ofUnsafe(priceRangeGridMinTmp), DoubleArray.ofUnsafe(priceRangeGridMaxTmp));
  }

  /** Generate the price functions used in the tests */
  private static List<BoundSurfaceInterpolator> priceFunctions(
      DoubleArray quantitiesFutures,
      double[][] strikes,
      double[][] times,
      boolean[][] isCall,
      double[][] quantitiesOptions,
      double[][] impliedVol,
      DoubleArray priceRangeGridMin,
      DoubleArray priceRangeGridMax) {

    /* Create pricing functions by 2D-interpolation */
    int nbInterpolationPrices = 21;
    int nbInterpolationVols = 3;
    double volRangeGridMin = 0.90;
    double volRangeGridMax = 1.10;

    List<BoundSurfaceInterpolator> priceFunctions = new ArrayList<>(); // dimension: risk factors
    for (int looprf = 0; looprf < NB_RISK_FACTORS; looprf++) {
      int looprf2 = looprf;
      DoubleArray pricesInterpolation =
          DoubleArray.of(
              nbInterpolationPrices,
              (i) -> priceRangeGridMin.get(looprf2) +
                  i * (priceRangeGridMax.get(looprf2) - priceRangeGridMin.get(looprf2)) / (nbInterpolationPrices - 1.0d));
      DoubleArray volsInterpolation =
          DoubleArray.of(
              nbInterpolationVols,
              (i) -> (volRangeGridMin
                  + i * (volRangeGridMax - volRangeGridMin) / (nbInterpolationVols - 1.0d)));
      double[][] pvInterpolationTmp = new double[nbInterpolationPrices][nbInterpolationVols];
      for (int loopintprice = 0; loopintprice < nbInterpolationPrices; loopintprice++) {
        for (int loopintvol = 0; loopintvol < nbInterpolationVols; loopintvol++) {
          for (int loopopt = 0; loopopt < strikes[looprf].length; loopopt++) {
            pvInterpolationTmp[loopintprice][loopintvol] +=
                quantitiesOptions[looprf][loopopt] *
                    BlackFormulaRepository
                        .price(
                            pricesInterpolation.get(loopintprice),
                            strikes[looprf][loopopt],
                            times[looprf][loopopt],
                            volsInterpolation.get(loopintvol) * impliedVol[looprf][loopopt],
                            isCall[looprf][loopopt]);
          }
          pvInterpolationTmp[loopintprice][loopintvol] +=
              quantitiesFutures.get(looprf) * pricesInterpolation.get(loopintprice);
        }
      }
      BiLinearSimpleBoundInterpolator pvSurface = new BiLinearSimpleBoundInterpolator(
          pricesInterpolation.toArrayUnsafe(),
          volsInterpolation.toArrayUnsafe(),
          pvInterpolationTmp);
      priceFunctions.add(pvSurface);
    }
    return priceFunctions;
  }

}

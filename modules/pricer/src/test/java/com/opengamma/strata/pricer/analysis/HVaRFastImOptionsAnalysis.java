/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.math.impl.cern.MersenneTwister64;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionCommons;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.descriptive.MidwayInterpolationQuantileMethod;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Analysis of a basic prototype of fast IM based on variance/covariance calibrated on HVaR-like approach.
 */
public class HVaRFastImOptionsAnalysis {

  /* Normal distribution to generate the Monte Carlo scenarios. */
  private static final RandomEngine RANDOM_ENGINE = new MersenneTwister64(0); // Fixed seed for testing
  private static final NormalDistribution NORMAL = new NormalDistribution(0.0d, 1.0d, RANDOM_ENGINE);
  /* Decomposition method to transform uncorrelated draws into correlated draws */
  private static final CholeskyDecompositionCommons DECOMPOSITION_METHOD = new CholeskyDecompositionCommons();
  private static final OGMatrixAlgebra ALGEBRA = new OGMatrixAlgebra();

  /* Inner Model settings */
  private static final QuantileCalculationMethod QUANTILE_METHOD = MidwayInterpolationQuantileMethod.DEFAULT;
  private static final double LEVEL = 0.99; // relative
  private static final double VOLATILITY_TO_IM_FACTOR = NORMAL.getInverseCDF(LEVEL);
  private static final int NB_INNER_SCENARIOS = 2500;

  /* Outer scenarios */
  private static final int NB_OUTER_SCENARIOS = 1000;

  /* Calibrated parameters */ // TODO: replace by actual calibration
  private static final DoubleArray STANDALONE_IM_UP = DoubleArray.of(1.0, 1.5, 2.0); // For short
  private static final DoubleArray STANDALONE_IM_DOWN = DoubleArray.of(0.75, 1.10, 1.60); // For long
  private static final int NB_RISK_FACTORS = STANDALONE_IM_UP.size();
  private static final DoubleMatrix IMPLIED_CORRELATION = DoubleMatrix.ofUnsafe(
      new double[][]
          {{1.00d, 0.98d, 0.50d},
           {0.98d, 1.00d, 0.49d},
           {0.50d, 0.49d, 1.00d}}
  );

  /* Data */ // TODO: replace by actual calibration
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

  /**
   * Scenario based VaR with full revaluation.
   * Full revaluation is done by interpolation.
   * For each underlying a 2D-interpolation on price and implied volatility is generated.
   * Inner price scenarios are generated based on calibrated variance/covariance.
   * Inner implied volatility scenarios are parallel smile shift for a given underlying; // TODO: incorporate implied volatility scenario
   * // TODO: asymmetrical IMs
   */
  @Test
  void full_reval() {

    long start, end;

    start = System.currentTimeMillis();

    DoubleArray standAloneImImpliedVolatilitiesUp = STANDALONE_IM_UP.dividedBy(PRICES).dividedBy(VOLATILITY_TO_IM_FACTOR);
    DoubleArray standAloneImImpliedVolatilitiesDown = STANDALONE_IM_DOWN.dividedBy(PRICES).dividedBy(VOLATILITY_TO_IM_FACTOR);

    List<DoubleArray> pricesOuter = generatePriceOuterScenarios(NB_OUTER_SCENARIOS);

    HVaRFastCalculator varCalculator = HVaRFastCalculator.DEFAULT;

    List<double[][]> scenariosPrices = varCalculator.generatePriceInnerScenarios(
        pricesOuter,
        standAloneImImpliedVolatilitiesUp,
        standAloneImImpliedVolatilitiesDown,
        IMPLIED_CORRELATION,
        NB_INNER_SCENARIOS);

    Pair<DoubleArray, DoubleArray> priceRangeGrid = priceRange(scenariosPrices);
    DoubleArray priceRangeGridMin = priceRangeGrid.getFirst();
    DoubleArray priceRangeGridMax = priceRangeGrid.getSecond();
    List<double[][]> scenariosImpliedVol = varCalculator
        .generateImpliedVolInnerScenarios(NB_RISK_FACTORS, NB_OUTER_SCENARIOS, NB_INNER_SCENARIOS);

    end = System.currentTimeMillis();
    System.out.println("Computation time to generate scenarios: " + (end-start) + " ms.");


    start = System.currentTimeMillis();

    List<BoundSurfaceInterpolator> priceFunctions = priceFunctions(
        QUANTITIES_FUTURES, STRIKES, TIMES, IS_CALL, QUANTITIES_OPTIONS, IMPLIED_VOL,
        priceRangeGridMin, priceRangeGridMax);

    end = System.currentTimeMillis();
    System.out.println("Computation time to create 2D-interpolation scheme: " + (end - start) + " ms.");

    /* Compute PL vectors for each underlying and each scenario */
    start = System.currentTimeMillis();

    List<DoubleArray> scenariosPl = varCalculator.
        generateScenariosPl(priceFunctions, pricesOuter, scenariosPrices, scenariosImpliedVol);

    end = System.currentTimeMillis();
    System.out.println("Computation time to generate pv: " + (end-start) + " ms.");

    /* IM */
    start = System.currentTimeMillis();
    DoubleArray ims =
        DoubleArray.of(NB_OUTER_SCENARIOS, (i) -> QUANTILE_METHOD.quantileFromUnsorted(LEVEL, scenariosPl.get(i)));
    end = System.currentTimeMillis();
    System.out.println("Computation time to compute quantiles: " + (end-start) + " ms.");

    System.out.println("IMs: " + ims);
    double averageIms = ims.sum() / NB_OUTER_SCENARIOS;
    DoubleArray imsMinusAverage = ims.minus(averageIms);
    double standardDeviationIms = Math.sqrt(imsMinusAverage.multipliedBy(imsMinusAverage).sum() / NB_OUTER_SCENARIOS);
    System.out.println("  |-> Average: " + averageIms);
    System.out.println("  |-> Std dev: " + standardDeviationIms);
  }

  /* Outer scenarios: should be user provided, here generated for testing */ 
  // TODO: load external scenarios
  private static List<DoubleArray> generatePriceOuterScenarios(int nbOuterScenarios) {
    double priceRange = 0.50;
    List<DoubleArray> pricesOuter = new ArrayList<>(); // dimension outer sc / risk factors
    for (int loopoutersc = 0; loopoutersc < nbOuterScenarios; loopoutersc++) {
      pricesOuter.add(PRICES
          .multipliedBy(priceRange + loopoutersc * (1.0d/priceRange - priceRange) / (nbOuterScenarios - 1.0d)));
    }
    return pricesOuter;
  }

  private static Pair<DoubleArray, DoubleArray> priceRange(List<double[][]> scenariosPrices){
    double[] priceRangeGridMinTmp = new double[NB_RISK_FACTORS]; // Lowest price in all scenarios; for grid - dimension: risk factors
    double[] priceRangeGridMaxTmp = new double[NB_RISK_FACTORS]; // Highest price in all scenarios; for grid
    Arrays.fill(priceRangeGridMinTmp, Double.MAX_VALUE);
    int nbOuterScenarios = scenariosPrices.size();
    int nbInnerScenarios = scenariosPrices.get(0).length;
    for (double[][] scenariosPrice : scenariosPrices) {
      for (int loopinner = 0; loopinner < nbInnerScenarios; loopinner++) {
        for (int looprf = 0; looprf < NB_RISK_FACTORS; looprf++) {
          priceRangeGridMinTmp[looprf] =
              Math.min(priceRangeGridMinTmp[looprf], scenariosPrice[loopinner][looprf]);
          priceRangeGridMaxTmp[looprf] =
              Math.max(priceRangeGridMaxTmp[looprf], scenariosPrice[loopinner][looprf]);
        }
      }
    }
    return Pair.of(DoubleArray.ofUnsafe(priceRangeGridMinTmp), DoubleArray.ofUnsafe(priceRangeGridMaxTmp));
  }

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

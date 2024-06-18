/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.random.SobolSequenceGenerator;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.math.impl.cern.MersenneTwister64;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionResult;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.descriptive.MidwayInterpolationQuantileMethod;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;

/**
 * Fast IM calculator associated to historical VaR.
 */
public class HVaRFastCalculator {

  /** Default normal distribution to generate the Monte Carlo scenarios. */
  private static final RandomEngine RANDOM_ENGINE_DEFAULT =
      new MersenneTwister64(0); // Fixed seed for testing
  /** Default quantile method */
  private static final QuantileCalculationMethod QUANTILE_METHOD_DEFAULT =
      MidwayInterpolationQuantileMethod.DEFAULT;
  /** Default probability level */
  private static final double LEVEL_DEFAULT = 0.99; // relative
  /** The default calculator with MidwayInterpolationQuantileMethod and level = 99%. */
  public static final HVaRFastCalculator DEFAULT = 
      new HVaRFastCalculator(QUANTILE_METHOD_DEFAULT, LEVEL_DEFAULT, RANDOM_ENGINE_DEFAULT);

  /** Decomposition method to transform uncorrelated draws into correlated draws */
  private static final CholeskyDecompositionCommons DECOMPOSITION_METHOD = new CholeskyDecompositionCommons();
  private static final OGMatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  
  /** The quantile calculation method */
  private final QuantileCalculationMethod quantileMethod;
  /** The probability level */
  private final double level;
  /** The normal distribution */
  private final NormalDistribution normal;

  /** 
   * Constructor 
   * 
   * @param quantileMethod  the quantile calculation method
   * @param level  the probability level
   */
  public HVaRFastCalculator(
      QuantileCalculationMethod quantileMethod,
      double level,
      RandomEngine randomEngine) {
    this.quantileMethod = quantileMethod;
    this.level = ArgChecker.inRange(level, 0.0d, 1.0d, "level");
    this.normal = new NormalDistribution(0.0d, 1.0d, randomEngine);
  }

  /**
   * Generate the inner scenarios from a given outer scenario for the prices.
   *
   * @param pricesScenario  the risk factor outer prices for a given scenario
   * @param imImpliedVolatilitiesUp  the IMs implied volatilities for each risk factor
   * @param imImpliedVolatilitiesDown  the IMs implied volatilities associated to down shocks for each risk factor
   * @param lower  the lower matrix from the correlation decomposition
   * @param nbInnerScenarios  the number of inner scenarios
   * @return  the inner scenarios associated to the outer prices scenario
   */
  private double[][] generatePriceInnerScenarios(
      DoubleArray pricesScenario,
      DoubleArray imImpliedVolatilitiesUp,
      DoubleArray imImpliedVolatilitiesDown,
      DoubleMatrix lower,
      int nbInnerScenarios) {

    int nbRiskFactors = pricesScenario.size();


    double[][] independentScenarios = new double[nbInnerScenarios][nbRiskFactors];
    /* Normal random generator */
    //for (int loopinnersc = 0; loopinnersc < nbInnerScenarios; loopinnersc++) {
    //  for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
    //    double shock = normal.nextRandom();
    //    independentScenarios[loopinnersc][looprf] = shock;
    //  }
    //}
    /* Sobol' sequences */ // FIXME: choose between random generator and Sobel' sequences
    SobolSequenceGenerator sobolGenerator = new SobolSequenceGenerator(nbRiskFactors);
    sobolGenerator.skipTo(0);
    for (int loopinnersc = 0; loopinnersc < nbInnerScenarios; loopinnersc++) {
      double[] shocks = sobolGenerator.nextVector(); // Uniform [0,1]
        for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
          independentScenarios[loopinnersc][looprf] = normal.getInverseCDF(shocks[looprf]);
        }
    }
    double[][] scenariosPricesOuter = new double[nbInnerScenarios][nbRiskFactors];
    for (int loopinner = 0; loopinner < nbInnerScenarios; loopinner++) {
      DoubleArray correlatedScenario =
          ((DoubleArray) ALGEBRA.multiply(
              lower,
              DoubleArray.ofUnsafe(independentScenarios[loopinner])));
      for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
        double volatility = ((correlatedScenario.get(looprf) >= 0.0d)
            ? imImpliedVolatilitiesUp.get(looprf)
            : imImpliedVolatilitiesDown.get(looprf));
        scenariosPricesOuter[loopinner][looprf] =
            pricesScenario.get(looprf) * (1.0d + correlatedScenario.get(looprf) * volatility);
      }
    }
    return scenariosPricesOuter;
  }

  /**
   * Generate the price inner scenarios from a list of given outer scenario for the prices.
   *
   * @param pricesOuter  the list of outer prices; dimension: nb outer scenarios / nb risk factors
   * @param imImpliedVolatilitiesUp  the IMs implied volatilities associated to up shocks for each risk factor
   * @param imImpliedVolatilitiesDown  the IMs implied volatilities associated to down shocks for each risk factor
   * @param imImpliedCorrelations  the IMs implied correlations between the risk factors
   * @param nbInnerScenarios  the number of inner scenarios
   * @return  the price inner-scenarios associated to each outer prices scenario; dimensions: outer sc / inner sc / risk factors
   */
  public List<double[][]> generatePriceInnerScenarios(
      List<DoubleArray> pricesOuter,
      DoubleArray imImpliedVolatilitiesUp,
      DoubleArray imImpliedVolatilitiesDown,
      DoubleMatrix imImpliedCorrelations,
      int nbInnerScenarios) {

    List<double[][]> scenariosPrices = new ArrayList<>(); // dimensions: outer sc / inner sc / risk factors
    for (DoubleArray prices : pricesOuter) {
      CholeskyDecompositionResult decompositionResults = DECOMPOSITION_METHOD.apply(imImpliedCorrelations);
      DoubleMatrix lower = decompositionResults.getL();
      double[][] scenariosPricesOuter =
          generatePriceInnerScenarios(prices, imImpliedVolatilitiesUp, imImpliedVolatilitiesDown, lower, nbInnerScenarios);
      scenariosPrices.add(scenariosPricesOuter);
    }
    return scenariosPrices;
  }

  /**
   * Generate the implied volatility inner scenarios. // TODO: currently constant scenarios unchanged
   *
   * @param nbRiskFactors  the number of risk factors
   * @param nbOuterScenarios  the number of outer scenarios
   * @param nbInnerScenarios  the number of inner scenarios
   * @return the implied volatility inner-scenarios associated to each outer prices scenario
   */
  public List<double[][]> generateImpliedVolInnerScenarios(
      int nbRiskFactors,
      int nbOuterScenarios,
      int nbInnerScenarios) {

    List<double[][]> scenariosImpliedVol = new ArrayList<>(); // dimensions: outer sc / inner sc / risk factors
    for (int loopoutersc = 0; loopoutersc < nbOuterScenarios; loopoutersc++) {
      double[][] independentScenarios = new double[nbInnerScenarios][nbRiskFactors];
      for (int loopinnersc = 0; loopinnersc < nbInnerScenarios; loopinnersc++) {
        for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
          independentScenarios[loopinnersc][looprf] = 1.0; // TODO: actual scenarios
        }
      }
      scenariosImpliedVol.add(independentScenarios);
    }
    return scenariosImpliedVol;
  }


  /**
   *
   * @param priceFunctions
   * @param pricesOuter
   * @param scenariosPrices
   * @param scenariosImpliedVol
   * @return
   */
  public List<DoubleArray> generateScenariosPl(
      List<BoundSurfaceInterpolator> priceFunctions,
      List<DoubleArray> pricesOuter,
      List<double[][]> scenariosPrices,
      List<double[][]> scenariosImpliedVol) {

    int nbRiskFactors = pricesOuter.get(0).size();
    int nbOuterScenarios = scenariosPrices.size();
    int nbInnerScenarios = scenariosPrices.get(0).length;
    List<DoubleArray> scenariosPl = new ArrayList<>(); // dimensions: outer sc / inner sc
    for (int loopoutersc = 0; loopoutersc < nbOuterScenarios; loopoutersc++) {
      double pvBase = 0.0;
      for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
        pvBase += priceFunctions.get(looprf)
            .interpolate(pricesOuter.get(loopoutersc).get(looprf), 1.0d);
      }
      double[] plByInnerSc = new double[nbInnerScenarios];
      for (int loopinnersc = 0; loopinnersc < nbInnerScenarios; loopinnersc++) {
        plByInnerSc[loopinnersc] += pvBase;
        for (int looprf = 0; looprf < nbRiskFactors; looprf++) {
          double pvScenario = priceFunctions.get(looprf)
              .interpolate(
                  scenariosPrices.get(loopoutersc)[loopinnersc][looprf],
                  scenariosImpliedVol.get(loopoutersc)[loopinnersc][looprf]);
          plByInnerSc[loopinnersc] -= pvScenario;
        }
      }
      scenariosPl.add(DoubleArray.ofUnsafe(plByInnerSc));
    }
    return scenariosPl;
  }

}

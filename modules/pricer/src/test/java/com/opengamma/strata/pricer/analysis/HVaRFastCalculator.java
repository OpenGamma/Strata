/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import java.util.List;
import java.util.Map;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.statistics.descriptive.MidwayInterpolationQuantileMethod;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;

/**
 * Fast IM calculator associated to historical VaR.
 */
public class HVaRFastCalculator {

  /** Default quantile method */
  private static final QuantileCalculationMethod QUANTILE_METHOD_DEFAULT =
      MidwayInterpolationQuantileMethod.DEFAULT;
  /** Default probability level */
  private static final double LEVEL_DEFAULT = 0.90; // relative
  /** The default calculator with MidwayInterpolationQuantileMethod and level = 99%. */
  public static final HVaRFastCalculator DEFAULT = 
      new HVaRFastCalculator(QUANTILE_METHOD_DEFAULT, LEVEL_DEFAULT);
  
  /** The quantile calculation method */
  private final QuantileCalculationMethod quantileMethod;
  /** The probability level */
  private final double level;
  
  /** 
   * Constructor 
   * 
   * @param quantileMethod  the quantile calculation method
   * @param level  the probability level
   */
  public HVaRFastCalculator(QuantileCalculationMethod quantileMethod, double level) {
    this.quantileMethod = quantileMethod;
    ArgChecker.inRange(level, 0.0d, 1.0d, "level");
    this.level = level;
  }

  public double[] calculate(
      Map<StandardId, DoubleArray> innerScenariosPls,
      Map<StandardId, DoubleArray> outerScenariosPricesScalings,
      Map<StandardId, DoubleArray> outerScenariosFilterings,
      List<Pair<StandardId, Double>> portfolio) { // Position ? Change to include options, with IV?

    int nbOuterScenarios = outerScenariosPricesScalings.get(portfolio.get(0).getFirst()).size();
    int nbInnerScenarios = innerScenariosPls.get(portfolio.get(0).getFirst()).size();
    int nbUnderlyings = portfolio.size();
    double[][] plPortfolio = new double[nbOuterScenarios][nbInnerScenarios];
    for (int loopund = 0; loopund < nbUnderlyings; loopund++) {
      DoubleArray innerScenariosPl = innerScenariosPls.get(portfolio.get(loopund).getFirst());
      DoubleArray outerScenariosPriceScaling = outerScenariosPricesScalings.get(portfolio.get(loopund).getFirst());
      DoubleArray outerScenariosFiltering = outerScenariosFilterings.get(portfolio.get(loopund).getFirst());
      for (int loopouter = 0; loopouter < nbOuterScenarios; loopouter++) {
        double plScaling = outerScenariosPriceScaling.get(loopouter) * outerScenariosFiltering.get(loopouter);
        for (int loopinner = 0; loopinner < nbInnerScenarios; loopinner++) {
          plPortfolio[loopouter][loopinner] += innerScenariosPl.get(loopinner) * plScaling;
        }
      }
    }

    double[] imOuterScenarios = new double[nbOuterScenarios];
    for (int loopouter = 0; loopouter < nbOuterScenarios; loopouter++) {
      imOuterScenarios[loopouter] =
          quantileMethod.quantileFromUnsorted(level, DoubleArray.ofUnsafe(plPortfolio[loopouter]));
    }

//    double[] imOuterScenarios = new double[nbOuterScenarios];
//    for (int loopouter = 0; loopouter < nbOuterScenarios; loopouter++) {
//      imOuterScenarios[loopouter] = plPortfolio[loopouter][0]; // Doing nothing, for testing.
//    }

    return imOuterScenarios;
  }

}

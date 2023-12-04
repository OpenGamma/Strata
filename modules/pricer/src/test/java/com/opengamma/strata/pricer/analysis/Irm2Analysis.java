/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.math.impl.statistics.descriptive.MidwayInterpolationQuantileMethod;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Analysis IRM 2 fast IM.
 */
public class Irm2Analysis {

  private static final double[] STRIKES = {100.0, 99.0, 100.1, 100.5, 98.0};
  private static final double[] TIMES = {0.5, 0.25, 0.4, 0.3, 0.75};
  private static final int NB_TRADES = STRIKES.length;

  private static final double PRICE = 100;
  private static final double VOL = 0.20;
  private static final double PRICE_RANGE_INNER = 0.20; // relative
  private static final double PRICE_RANGE_OUTER = 0.40; // relative
  private static final double SCALING_RANGE_OUTER = 0.05; // relative
  private static final double VOL_RANGE = 0.20; // relative

  private static final int NB_OUTER_SCENARIOS = 5000;
  private static final int NB_INNER_SCENARIOS = 750;
  
  private static final CurveInterpolator INTERPOLATOR_1D = LINEAR;
  private static final SurfaceInterpolator INTERPOLATOR_2D = 
      GridSurfaceInterpolator.of(INTERPOLATOR_1D, INTERPOLATOR_1D);
  
  private static final QuantileCalculationMethod QUANTILE_METHOD = 
      MidwayInterpolationQuantileMethod.DEFAULT;
  private static final double LEVEL = 0.99; // relative
  
//  private static final double BLACK_REPOSITORY = BlackFormulaRepository.
  
  @Test
  void full_reval() {

    long start, end;

    start = System.currentTimeMillis();

    double test = 0;
    double[] var = new double[NB_OUTER_SCENARIOS];
    for (int loopoutsc = 0; loopoutsc < NB_OUTER_SCENARIOS; loopoutsc++) {
      DoubleArray pricesScenarios =
          DoubleArray.of(NB_INNER_SCENARIOS,
              (i) -> PRICE * (1.0d - ((NB_INNER_SCENARIOS - 1) - 2.0d * i) / (NB_INNER_SCENARIOS - 1.0d) * PRICE_RANGE_INNER));
      DoubleArray volScenarios =
          DoubleArray.of(NB_INNER_SCENARIOS,
              (i) -> VOL * (1.0d - ((NB_INNER_SCENARIOS - 1) - 2.0d * i) / (NB_INNER_SCENARIOS - 1.0d) * VOL_RANGE));

      double[] pv = new double[NB_INNER_SCENARIOS];
      for (int loopinsc = 0; loopinsc < NB_INNER_SCENARIOS; loopinsc++) {
        for (int looptrade = 0; looptrade < NB_TRADES; looptrade++) {
          pv[loopinsc] += BlackFormulaRepository
              .price(pricesScenarios.get(loopinsc), STRIKES[looptrade], TIMES[looptrade], volScenarios.get(loopinsc), true);
        }
        test += pv[loopinsc];
      }
      var[loopoutsc] = QUANTILE_METHOD.quantileFromUnsorted(LEVEL, DoubleArray.ofUnsafe(pv));
    }
    end = System.currentTimeMillis();
    System.out.println("Full Reval - Time: " + (end - start) + " ms. " 
        + NB_TRADES + " trades x " + NB_INNER_SCENARIOS + " scenarios x " + NB_OUTER_SCENARIOS + " rep " + test);

  }
  
  // Using interpolation is not numerically efficient. Probably due to building interpolation object.
//  @Test
//  void interpolation() {
//
//    long start, end;
//    int nbInterpolation = 51;
//    start = System.currentTimeMillis();
//
//    DoubleArray pricesInterpolation =
//        DoubleArray.of(nbInterpolation,
//            (i) -> PRICE * (1.0d - ((nbInterpolation - 1) - 2.0d * i) / (nbInterpolation - 1.0d) * PRICE_RANGE));
//
//    double[] pvInterpolationA = new double[nbInterpolation];
//    for (int loopint = 0; loopint < nbInterpolation; loopint++) {
//      for (int looptrade = 0; looptrade < NB_TRADES; looptrade++) {
//        pvInterpolationA[loopint] += BlackFormulaRepository
//            .price(pricesInterpolation.get(loopint), STRIKES[looptrade], TIMES[looptrade], VOL, true);
//      }
//    }
//
//    InterpolatedNodalCurve pvCurve = InterpolatedNodalCurve
//        .of(DefaultCurveMetadata.of("pv"), pricesInterpolation, DoubleArray.ofUnsafe(pvInterpolationA), LINEAR);
//
//    DoubleArray pricesScenarios =
//        DoubleArray.of(NB_SCENARIOS,
//            (i) -> PRICE * (1.0d - ((NB_SCENARIOS - 1) - 2.0d * i) / (NB_SCENARIOS - 1.0d) * PRICE_RANGE));
//    DoubleArray volScenarios =
//        DoubleArray.of(NB_SCENARIOS,
//            (i) -> VOL * (1.0d - ((NB_SCENARIOS - 1) - 2.0d * i) / (NB_SCENARIOS - 1.0d) * VOL_RANGE));
//
//    double test = 0;
//    for (int looprep = 0; looprep < NB_OUTER_SCENARIOS; looprep++) {
//      double[] pv = new double[NB_SCENARIOS];
//      for (int loopsc = 0; loopsc < NB_SCENARIOS; loopsc++) {
//        pv[loopsc] = pvCurve.yValue(pricesScenarios.get(loopsc));
//        test += pv[loopsc];
//      }
//    }
//
//    end = System.currentTimeMillis();
//    System.out.println("Interpolation 1D - Time: " + (end - start) + " ms. " 
//        + NB_TRADES + " trades x " + NB_SCENARIOS + " scenarios x " + NB_OUTER_SCENARIOS + " rep " + test);
//    // Creating the interpolated nodal curve is very slow! Almost independently of the number of points.
//
//  }

//  @Test
//  void interpolation_2D() {
//
//    long start, end;
//
//    int nbScenarios = 2500;
//    int nbInterpolationPrices = 51;
//    int nbInterpolationVols = 11;
//    start = System.currentTimeMillis();
//
//    DoubleArray pricesInterpolation =
//        DoubleArray.of(nbInterpolationPrices,
//            (i) -> PRICE * (1.0d - ((nbInterpolationPrices - 1) - 2.0d * i) / (nbInterpolationPrices - 1.0d) * PRICE_RANGE));
//    DoubleArray volsInterpolation =
//        DoubleArray.of(nbInterpolationVols,
//            (i) -> VOL * (1.0d - ((nbInterpolationVols - 1) - 2.0d * i) / (nbInterpolationVols - 1.0d) * VOL_RANGE));
//
//    double[] pvInterpolationA = new double[nbInterpolationVols * nbInterpolationPrices];
//    double[] pricesInterpolation2A = new double[nbInterpolationVols * nbInterpolationPrices];
//    double[] volsInterpolation2A = new double[nbInterpolationVols * nbInterpolationPrices];
//
//    for (int loopintprice = 0; loopintprice < nbInterpolationPrices; loopintprice++) {
//      for (int loopintvol = 0; loopintvol < nbInterpolationVols; loopintvol++) {
//        pricesInterpolation2A[loopintprice * nbInterpolationVols + loopintvol] = pricesInterpolation.get(loopintprice);
//        volsInterpolation2A[loopintprice * nbInterpolationVols + loopintvol] = volsInterpolation.get(loopintvol);
//        for (int looptrade = 0; looptrade < NB_TRADES; looptrade++) {
//          pvInterpolationA[loopintprice * nbInterpolationVols + loopintvol] += BlackFormulaRepository
//              .price(pricesInterpolation.get(loopintprice), STRIKES[looptrade], TIMES[looptrade], volsInterpolation.get(loopintvol), true);
//        }
//      }
//    }
//
//    InterpolatedNodalSurface pvSurface = InterpolatedNodalSurface.of(
//        DefaultSurfaceMetadata.of("pv"), 
//        DoubleArray.ofUnsafe(pricesInterpolation2A), 
//        DoubleArray.ofUnsafe(volsInterpolation2A), 
//        DoubleArray.ofUnsafe(pvInterpolationA),  INTERPOLATOR_2D);
//
//    DoubleArray pricesScenarios =
//        DoubleArray.of(nbScenarios,
//            (i) -> PRICE * (1.0d - ((nbScenarios - 1) - 2.0d * i) / (nbScenarios - 1.0d) * PRICE_RANGE));
//    DoubleArray volScenarios =
//        DoubleArray.of(nbScenarios,
//            (i) -> VOL * (1.0d - ((nbScenarios - 1) - 2.0d * i) / (nbScenarios - 1.0d) * VOL_RANGE));
//
//    double test = 0;
//    for (int looprep = 0; looprep < NB_OUTER_SCENARIOS; looprep++) {
//      double[] pv = new double[nbScenarios];
//      for (int loopsc = 0; loopsc < nbScenarios; loopsc++) {
//        pv[loopsc] = pvSurface.zValue(pricesScenarios.get(loopsc), volScenarios.get(loopsc));
//        test += pv[loopsc];
//      }
//    }
//
//    end = System.currentTimeMillis();
//    System.out.println("Interpolation 2D - Time: " + (end - start) + " ms. " 
//        + NB_TRADES + " trades x " + nbScenarios + " scenarios x " + NB_OUTER_SCENARIOS + " rep " + test);
//    // Creating the interpolated surface is very slow! It recreates the bound on the x-interpolator for each value
//
//  }

  /* Reval of all scenario combinations (outer/inner) using a 2D fast interpolator for the 
   * option prices */
  @Test
  void interpolation_2D_fast() {

    long start, end;

    int nbInnerScenarios = NB_INNER_SCENARIOS;
    int nbInterpolationPrices = 201;
    int nbInterpolationVols = 51;
    start = System.currentTimeMillis();

    DoubleArray pricesInterpolation =
        DoubleArray.of(nbInterpolationPrices,
            (i) -> PRICE * (1.0d - ((nbInterpolationPrices - 1) - 2.0d * i) / (nbInterpolationPrices - 1.0d) * PRICE_RANGE_INNER));
    DoubleArray volsInterpolation =
        DoubleArray.of(nbInterpolationVols,
            (i) -> VOL * (1.0d - ((nbInterpolationVols - 1) - 2.0d * i) / (nbInterpolationVols - 1.0d) * VOL_RANGE));

    double[][] pvInterpolationA = new double[nbInterpolationPrices][nbInterpolationVols];

    for (int loopintprice = 0; loopintprice < nbInterpolationPrices; loopintprice++) {
      for (int loopintvol = 0; loopintvol < nbInterpolationVols; loopintvol++) {
        for (int looptrade = 0; looptrade < NB_TRADES; looptrade++) {
          pvInterpolationA[loopintprice][loopintvol] += BlackFormulaRepository
              .price(pricesInterpolation.get(loopintprice), STRIKES[looptrade], TIMES[looptrade], volsInterpolation.get(loopintvol), true);
        }
      }
    }

    BiLinearSimpleBoundInterpolator pvSurface = new BiLinearSimpleBoundInterpolator(
        pricesInterpolation.toArrayUnsafe(), 
        volsInterpolation.toArrayUnsafe(), pvInterpolationA);

    DoubleArray pricesScenarios =
        DoubleArray.of(nbInnerScenarios,
            (i) -> PRICE * (1.0d - ((nbInnerScenarios - 1) - 2.0d * i) / (nbInnerScenarios - 1.0d) * PRICE_RANGE_INNER));
    DoubleArray volScenarios =
        DoubleArray.of(nbInnerScenarios,
            (i) -> VOL * (1.0d - ((nbInnerScenarios - 1) - 2.0d * i) / (nbInnerScenarios - 1.0d) * VOL_RANGE));

    double test = 0;
    double[] var = new double[NB_OUTER_SCENARIOS];
    for (int loopoutsc = 0; loopoutsc < NB_OUTER_SCENARIOS; loopoutsc++) {
      double[] pv = new double[nbInnerScenarios];
      for (int loopinsc = 0; loopinsc < nbInnerScenarios; loopinsc++) {
        pv[loopinsc] = pvSurface.interpolate(pricesScenarios.get(loopinsc), volScenarios.get(loopinsc));
        test += pv[loopinsc];
      }
      var[loopoutsc] = QUANTILE_METHOD.quantileFromUnsorted(LEVEL, DoubleArray.ofUnsafe(pv));
    }
    end = System.currentTimeMillis();
    System.out.println("Interpolation 2D fast - Time: " + (end - start) + " ms. " 
        + NB_TRADES + " trades x " + nbInnerScenarios + " scenarios x " + NB_OUTER_SCENARIOS + " rep " + test);
  }
  
  private static final HVaRFastCalculator CALCULATOR = HVaRFastCalculator.DEFAULT;
 
  @Test
  void interpolation_2D_fast_calc() {

    long start, end;

    int nbInnerScenarios = 750; // NB_INNER_SCENARIOS;
    int nbOuterScenarios = 5000; // NB_OUTER_SCENARIOS;
    
    start = System.currentTimeMillis();
    
    double[] prices = {120.0d, 80.0d, 10.0d, 10.0d, 10.0d};
    double[] quantity = {10.0d, 5.0d, -2.0d, -1.0d, 5.0d};
    int nbUnderlyings = prices.length;

    Map<StandardId, DoubleArray> innerScenariosPls = new HashMap<>();
    Map<StandardId, DoubleArray> outerScenariosPricesScalings = new HashMap<>();
    Map<StandardId, DoubleArray> outerScenariosFilterings = new HashMap<>();
    List<Pair<StandardId, Double>> portfolio = new ArrayList<>();

    for (int loopund = 0; loopund < nbUnderlyings; loopund++) {
      StandardId id = StandardId.of("OG", "FUT" + loopund);
      int loopund2 = loopund;
      DoubleArray innerScenariosPlsUnd =
          DoubleArray.of(nbInnerScenarios,
              (i) -> prices[loopund2] * ((nbInnerScenarios - 1) - 2.0d * i) / (nbInnerScenarios - 1.0d) * PRICE_RANGE_INNER);
      innerScenariosPls.put(id, innerScenariosPlsUnd);
      DoubleArray outerScenariosPricesScalingsUnd =
          DoubleArray.of(nbOuterScenarios,
              (i) -> (1.0d - ((nbInnerScenarios - 1) - 2.0d * i) / (nbInnerScenarios - 1.0d) * PRICE_RANGE_OUTER));
      DoubleArray outerScenariosFilteringsUnd =
          DoubleArray.of(nbOuterScenarios,
              (i) -> (1.0d - ((nbInnerScenarios - 1) - 2.0d * i) / (nbInnerScenarios - 1.0d) * SCALING_RANGE_OUTER));
      outerScenariosPricesScalings.put(id, outerScenariosPricesScalingsUnd);
      outerScenariosFilterings.put(id, outerScenariosFilteringsUnd);
      portfolio.add(Pair.of(id, quantity[loopund]));
    }
    
    DoubleArray imScenarios = DoubleArray.ofUnsafe(CALCULATOR.calculate(
        innerScenariosPls,
        outerScenariosPricesScalings,
        outerScenariosFilterings,
        portfolio));
    
    end = System.currentTimeMillis();

    System.out.println(
        "Time fast futures " 
        + nbUnderlyings + " underlyings "
        + nbInnerScenarios + " inners " 
        + nbOuterScenarios + " outers in " 
        + (end - start) + "ms. " + imScenarios.sum() / imScenarios.size());
  }

}

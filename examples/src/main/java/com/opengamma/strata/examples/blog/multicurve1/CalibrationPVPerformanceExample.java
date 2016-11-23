/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.blog.multicurve1;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Calibrates one set of curve, computes sensitivity (Bucketed PV01) and estimate computation time.
 * <p>
 * Code used for the blog "Strata and multi-curve - Blog 1: Curve calibration and bucketed PV01" available at
 * XXX
 */
public class CalibrationPVPerformanceExample {

  /* Reference data contains calendar. Here we use build-in holiday calendar. 
   * It is possible to override them with customized versions.*/
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = LocalDate.of(2016, 8, 1);

  // Configuration with discounting curve using OIS up to final maturity; Libor forward curve using IRS.
  private static final String CONFIG_STR = "GBP-DSCONOIS-L6MIRS";
  private static final CurveGroupName CONFIG_NAME = CurveGroupName.of(CONFIG_STR);

  /* Swap description. */
  private static final Period SWAP_PERIOD_TO_START = Period.ofMonths(3);
  private static final double SWAP_COUPON = 0.0250;
  private static final double SWAP_NOTIONAL = 10_000_000;

  /* Path to files */
  private static final String PATH_CONFIG = "src/main/resources/example-calibration/curves/";
  private static final String PATH_QUOTES = "src/main/resources/example-calibration/quotes/";
  /* Files utilities */
  private static final String SUFFIX_CSV = ".csv";
  private static final String GROUPS_SUFFIX = "-group";
  private static final String NODES_SUFFIX = "-nodes";
  private static final String SETTINGS_SUFFIX = "-settings";

  private static final ResourceLocator GROUP_RESOURCE =
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + GROUPS_SUFFIX + SUFFIX_CSV);
  private static final ResourceLocator SETTINGS_RESOURCE =
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + SETTINGS_SUFFIX + SUFFIX_CSV);
  private static final ResourceLocator NODES_RESOURCE =
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + NODES_SUFFIX + SUFFIX_CSV);

  /* Raw data */
  private static final String QUOTES_FILE = PATH_QUOTES + "MARKET-QUOTES-GBP-20160801.csv";
  private static final Map<QuoteId, Double> MAP_MQ = QuotesCsvLoader.load(VALUATION_DATE, ResourceLocator.of(QUOTES_FILE));
  private static final ImmutableMarketData MARKET_QUOTES =
      ImmutableMarketData.builder(VALUATION_DATE).values(MAP_MQ).build();

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);

  private static final DiscountingSwapTradePricer PRICER_SWAP = DiscountingSwapTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final int NB_COUPONS = 100;
  private static final double SWAP_COUPON_RANGE = 0.0100;
  private static final int NB_TENORS = 20;
  private static final int TENOR_START = 1;

  @SuppressWarnings("null")
  public static void main(String[] arg) {

    int nbRrpWarm = 2;
    int nbRunPerf = 2;

    /* Load the curve configurations from csv files */
    Map<CurveGroupName, CurveGroupDefinition> configs =
        RatesCalibrationCsvLoader.load(GROUP_RESOURCE, SETTINGS_RESOURCE, NODES_RESOURCE);

    /* Construct a swaps */
    ResolvedSwapTrade[] swaps = new ResolvedSwapTrade[NB_COUPONS * NB_TENORS];
    for (int loopswap = 0; loopswap < NB_COUPONS; loopswap++) {
      for (int looptenor = 0; looptenor < NB_TENORS; looptenor++) {
        double coupon = SWAP_COUPON + loopswap * SWAP_COUPON_RANGE / NB_COUPONS;
        swaps[looptenor * NB_COUPONS + loopswap] = GBP_FIXED_6M_LIBOR_6M.createTrade(
            VALUATION_DATE, SWAP_PERIOD_TO_START, Tenor.of(Period.ofYears(TENOR_START + looptenor)),
            BuySell.BUY, SWAP_NOTIONAL, coupon, REF_DATA).resolve(REF_DATA);
      }
    }

    /* Warm-up */
    Pair<MultiCurrencyAmount[], CurrencyParameterSensitivities[]> r = null;
    for (int i = 0; i < nbRrpWarm; i++) {
      r = computation(configs, swaps);
    }

    long start, end;
    start = System.currentTimeMillis();
    for (int i = 0; i < nbRunPerf; i++) {
      r = computation(configs, swaps);
    }

    end = System.currentTimeMillis();
    System.out.println("Computation time: " + (end - start) + " ms");

    System.out.println("Performance estimate for curve calibration, " + (NB_COUPONS * NB_TENORS) + " trades and " +
        nbRunPerf + " repetitions.\n" + r.getFirst() + r.getSecond());

  }

  private static Pair<MultiCurrencyAmount[], CurrencyParameterSensitivities[]> computation(
      Map<CurveGroupName, CurveGroupDefinition> configs,
      ResolvedSwapTrade[] swaps) {

    int nbSwaps = swaps.length;

    /* Calibrate curves */
    ImmutableRatesProvider multicurve = CALIBRATOR.calibrate(configs.get(CONFIG_NAME), MARKET_QUOTES, REF_DATA);

    /* Computes PV and bucketed PV01 */
    MultiCurrencyAmount[] pv = new MultiCurrencyAmount[nbSwaps];
    CurrencyParameterSensitivities[] mqs = new CurrencyParameterSensitivities[nbSwaps];
    for (int loopswap = 0; loopswap < nbSwaps; loopswap++) {
      pv[loopswap] = PRICER_SWAP.presentValue(swaps[loopswap], multicurve);
      PointSensitivities pts = PRICER_SWAP.presentValueSensitivity(swaps[loopswap], multicurve);
      CurrencyParameterSensitivities ps = multicurve.parameterSensitivity(pts);
      mqs[loopswap] = MQC.sensitivity(ps, multicurve);
    }

    return Pair.of(pv, mqs);

  }

}

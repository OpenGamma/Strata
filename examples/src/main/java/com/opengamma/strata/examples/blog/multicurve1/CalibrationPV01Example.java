/*
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
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.examples.data.export.ExportUtils;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Calibrates one set of curve, computes sensitivity (Bucketed PV01) and exports results in Excel for visualization.
 * <p>
 * Code used for the blog "Strata and multi-curve - Blog 1: Curve calibration and bucketed PV01" available at
 * https://opengamma.com/blog/strata-and-multi-curve-curve-calibration-and-bucketed-pv01
 */
public class CalibrationPV01Example {

  /* Reference data contains calendar. Here we use build-in holiday calendar. 
   * It is possible to override them with customized versions.*/
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = LocalDate.of(2016, 8, 1);

  // Configuration with discounting curve using OIS up to final maturity; Libor forward curve using IRS.
  private static final String CONFIG_STR = "GBP-DSCONOIS-L6MIRS-FRTB";
  private static final CurveGroupName CONFIG_NAME = CurveGroupName.of(CONFIG_STR);

  /* Swap description. */
  private static final Period SWAP_TENOR = Period.ofYears(7);
  private static final Period SWAP_PERIOD_TO_START = Period.ofMonths(3);
  private static final double SWAP_COUPON = 0.025;
  private static final double SWAP_NOTIONAL = 10_000_000;

  /* Path to files */
  private static final String PATH_CONFIG = "src/main/resources/example-calibration/curves/";
  private static final String PATH_QUOTES = "src/main/resources/example-calibration/quotes/";
  private static final String PATH_RESULTS = "target/example-output/";
  /* Files utilities */
  private static final String SUFFIX_CSV = ".csv";
  private static final String GROUPS_SUFFIX = "-group";
  private static final String NODES_SUFFIX = "-nodes";
  private static final String SETTINGS_SUFFIX = "-linear-settings";

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
  private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);

  private static final DiscountingSwapTradePricer PRICER_SWAP = DiscountingSwapTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final double BP1 = 1.0E-4; // Scaling by 1 bp.

  public static void main(String[] arg) {

    /* Load the curve configurations from csv files */
    Map<CurveGroupName, RatesCurveGroupDefinition> configs =
        RatesCalibrationCsvLoader.load(GROUP_RESOURCE, SETTINGS_RESOURCE, NODES_RESOURCE);

    /* Calibrate curves */
    ImmutableRatesProvider multicurve = CALIBRATOR.calibrate(configs.get(CONFIG_NAME), MARKET_QUOTES, REF_DATA);

    /* Construct a swap */
    ResolvedSwapTrade swap = GBP_FIXED_6M_LIBOR_6M.createTrade(
        VALUATION_DATE, SWAP_PERIOD_TO_START, Tenor.of(SWAP_TENOR), BuySell.BUY, SWAP_NOTIONAL, SWAP_COUPON, REF_DATA)
        .resolve(REF_DATA);

    /* Computes PV and bucketed PV01 */
    MultiCurrencyAmount pv = PRICER_SWAP.presentValue(swap, multicurve);
    PointSensitivities pts = PRICER_SWAP.presentValueSensitivity(swap, multicurve);
    CurrencyParameterSensitivities ps = multicurve.parameterSensitivity(pts);
    CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, multicurve);

    /* Export to csv files. */
    ExportUtils.export(mqs, BP1, PATH_RESULTS + CONFIG_STR + "-delta" + SUFFIX_CSV);
    ExportUtils.export(pv, PATH_RESULTS + CONFIG_STR + "-pv" + SUFFIX_CSV);

    System.out.println("Calibration and export finished: " + CONFIG_STR);

  }

}

/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.blog.multicurve2;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.examples.data.export.ExportUtils;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Calibrates one set of curve with several interpolators, computes sensitivity (Bucketed PV01) and exports results in Excel for visualization.
 * <p>
 * Code used for the blog "Strata and multi-curve - Blog 2: Interpolation and risk" available at
 * XXX
 */
public class CalibrationInterpolationExample {

  /* Reference data contains calendar. Here we use build-in holiday calendar. 
   * It is possible to override them with customized versions.*/
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = LocalDate.of(2016, 8, 1);

  // Configuration with discounting curve using OIS up to final maturity; Libor forward curve using IRS.
  private static final String CONFIG_STR = "GBP-DSCONOIS-L6MIRS-FRTB";
  private static final CurveGroupName CONFIG_NAME = CurveGroupName.of(CONFIG_STR);

  /* Swap description. */
  private static final Period SWAP_TENOR = Period.ofYears(8);
  private static final Period SWAP_PERIOD_TO_START = Period.ofMonths(6);
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
  private static final String[] SETTINGS_SUFFIX = new String[]{"-linear-settings", "-dq-settings", "-ncs-settings"};
  private static final int NB_SETTINGS = 3;

  private static final ResourceLocator GROUP_RESOURCE =
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + GROUPS_SUFFIX + SUFFIX_CSV);
  private static final ResourceLocator[] SETTINGS_RESOURCE = new ResourceLocator[]{
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + SETTINGS_SUFFIX[0] + SUFFIX_CSV),
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + SETTINGS_SUFFIX[1] + SUFFIX_CSV),
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + SETTINGS_SUFFIX[2] + SUFFIX_CSV)};
  private static final ResourceLocator NODES_RESOURCE =
      ResourceLocator.of(PATH_CONFIG + CONFIG_STR + "/" + CONFIG_STR + NODES_SUFFIX + SUFFIX_CSV);

  /* Raw data */
  private static final String QUOTES_FILE = PATH_QUOTES + "MARKET-QUOTES-GBP-20160801.csv";
  private static final Map<QuoteId, Double> MAP_MQ = QuotesCsvLoader.load(VALUATION_DATE, ResourceLocator.of(QUOTES_FILE));
  private static final ImmutableMarketData MARKET_QUOTES =
      ImmutableMarketData.builder(VALUATION_DATE).values(MAP_MQ).build();

  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.standard();

  private static final DiscountingSwapTradePricer PRICER_SWAP = DiscountingSwapTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final double BP1 = 1.0E-4; // Scaling by 1 bp.

  public static void main(String[] arg) {

    /* Load the curve configurations from csv files */
    List<Map<CurveGroupName, CurveGroupDefinition>> configs = new ArrayList<>();
    for (int loopconfig = 0; loopconfig < NB_SETTINGS; loopconfig++) {
      configs.add(RatesCalibrationCsvLoader.load(GROUP_RESOURCE, SETTINGS_RESOURCE[loopconfig], NODES_RESOURCE));
    }

    /* Construct a swap */
    ResolvedSwapTrade swap = GBP_FIXED_6M_LIBOR_6M.createTrade(
        VALUATION_DATE, SWAP_PERIOD_TO_START, Tenor.of(SWAP_TENOR), BuySell.BUY, SWAP_NOTIONAL, SWAP_COUPON, REF_DATA)
        .resolve(REF_DATA);

    /* Calibrate curves */
    ImmutableRatesProvider[] multicurve = new ImmutableRatesProvider[3];
    for (int loopconfig = 0; loopconfig < NB_SETTINGS; loopconfig++) {
      multicurve[loopconfig] = CALIBRATOR.calibrate(configs.get(loopconfig).get(CONFIG_NAME), MARKET_QUOTES, REF_DATA);
    }

    /* Computes PV and bucketed PV01 */
    MultiCurrencyAmount[] pv = new MultiCurrencyAmount[NB_SETTINGS];
    CurrencyParameterSensitivities[] mqs = new CurrencyParameterSensitivities[NB_SETTINGS];
    for (int loopconfig = 0; loopconfig < NB_SETTINGS; loopconfig++) {
      pv[loopconfig] = PRICER_SWAP.presentValue(swap, multicurve[loopconfig]);
      PointSensitivities pts = PRICER_SWAP.presentValueSensitivity(swap, multicurve[loopconfig]);
      CurrencyParameterSensitivities ps = multicurve[loopconfig].parameterSensitivity(pts);
      mqs[loopconfig] = MQC.sensitivity(ps, multicurve[loopconfig]);
    }

    /* Export to csv files. */
    for (int loopconfig = 0; loopconfig < NB_SETTINGS; loopconfig++) {
      ExportUtils.export(mqs[loopconfig], BP1, PATH_RESULTS + CONFIG_STR + SETTINGS_SUFFIX[loopconfig] + "-mqs" + SUFFIX_CSV);
      ExportUtils.export(pv[loopconfig], PATH_RESULTS + CONFIG_STR + SETTINGS_SUFFIX[loopconfig] + "-pv" + SUFFIX_CSV);
    }

    System.out.println("Calibration and export finished: " + CONFIG_STR);

  }

}

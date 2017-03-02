/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

public class CalibrationEurStandard {

  private static final DayCount CURVE_DC = ACT_365F;

  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "EUR_EONIA_EOD";
  public static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String FWD3_NAME = "EUR_EURIBOR_3M";
  public static final CurveName FWD3_CURVE_NAME = CurveName.of(FWD3_NAME);
  private static final String FWD6_NAME = "EUR_EURIBOR_6M";
  public static final CurveName FWD6_CURVE_NAME = CurveName.of(FWD6_NAME);
  private static final String CURVE_GROUP_NAME_STR = "EUR-DSCON-EURIBOR3M-EURIBOR6M";
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of(CURVE_GROUP_NAME_STR);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  static {
    DSC_NAMES.put(DSCON_CURVE_NAME, EUR);
    Set<Index> eurEoniaSet = new HashSet<>();
    eurEoniaSet.add(EUR_EONIA);
    IDX_NAMES.put(DSCON_CURVE_NAME, eurEoniaSet);
    Set<Index> eurEuribor3Set = new HashSet<>();
    eurEuribor3Set.add(EUR_EURIBOR_3M);
    IDX_NAMES.put(FWD3_CURVE_NAME, eurEuribor3Set);
    Set<Index> eurEuriabor6Set = new HashSet<>();
    eurEuriabor6Set.add(EUR_EURIBOR_6M);
    IDX_NAMES.put(FWD6_CURVE_NAME, eurEuriabor6Set);
  }
  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;

  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100);

  public static RatesProvider calibrateEurStandard(
      LocalDate valuationDate,
      double[] dscOisQuotes,
      Period[] dscOisTenors,
      double fwd3FixingQuote,
      double[] fwd3FraQuotes,
      double[] fwd3IrsQuotes,
      Period[] fwd3FraTenors,
      Period[] fwd3IrsTenors,
      double fwd6FixingQuote,
      double[] fwd6FraQuotes,
      double[] fwd6IrsQuotes,
      Period[] fwd6FraTenors,
      Period[] fwd6IrsTenors) {
    /* Curve Discounting/EUR-EONIA */
    String[] dscIdValues = dscIdValues(dscOisTenors);
    /* Curve EUR-EURIBOR-3M */
    double[] fwd3MarketQuotes = fwdMarketQuotes(fwd3FixingQuote, fwd3FraQuotes, fwd3IrsQuotes);
    String[] fwd3IdValues = fwdIdValue(3, fwd3FixingQuote, fwd3FraQuotes, fwd3IrsQuotes, fwd3FraTenors, fwd3IrsTenors);
    /* Curve EUR-EURIBOR-6M */
    double[] fwd6MarketQuotes = fwdMarketQuotes(fwd6FixingQuote, fwd6FraQuotes, fwd6IrsQuotes);
    String[] fwd6IdValues = fwdIdValue(6, fwd6FixingQuote, fwd6FraQuotes, fwd6IrsQuotes, fwd6FraTenors, fwd6IrsTenors);
    /* All quotes for the curve calibration */
    MarketData allQuotes =
        allQuotes(valuationDate, dscOisQuotes, dscIdValues, fwd3MarketQuotes, fwd3IdValues, fwd6MarketQuotes, fwd6IdValues);
    /* All nodes by groups. */
    CurveGroupDefinition config = config(dscOisTenors, dscIdValues, fwd3FraTenors, fwd3IrsTenors, fwd3IdValues,
        fwd6FraTenors, fwd6IrsTenors, fwd6IdValues);
    /* Results */
    return CALIBRATOR.calibrate(config, allQuotes, REF_DATA);
  }

  public static String[] dscIdValues(Period[] dscOisTenors) {
    String[] dscIdValues = new String[dscOisTenors.length];
    for (int i = 0; i < dscOisTenors.length; i++) {
      dscIdValues[i] = "OIS" + dscOisTenors[i].toString();
    }
    return dscIdValues;
  }

  public static String[] fwdIdValue(
      int tenor,
      double fwdFixingQuote,
      double[] fwdFraQuotes,
      double[] fwdIrsQuotes,
      Period[] fwdFraTenors,
      Period[] fwdIrsTenors) {
    String[] fwdIdValue = new String[1 + fwdFraQuotes.length + fwdIrsQuotes.length];
    fwdIdValue[0] = "FIXING" + tenor + "M";
    for (int i = 0; i < fwdFraQuotes.length; i++) {
      fwdIdValue[i + 1] = "FRA" + fwdFraTenors[i].toString() + "x" + fwdFraTenors[i].plusMonths(tenor).toString();
    }
    for (int i = 0; i < fwdIrsQuotes.length; i++) {
      fwdIdValue[i + 1 + fwdFraQuotes.length] = "IRS" + tenor + "M-" + fwdIrsTenors[i].toString();
    }
    return fwdIdValue;
  }

  public static double[] fwdMarketQuotes(
      double fwdFixingQuote,
      double[] fwdFraQuotes,
      double[] fwdIrsQuotes) {
    int fwdNbFraNodes = fwdFraQuotes.length;
    int fwdNbIrsNodes = fwdIrsQuotes.length;
    int fwdNbNodes = 1 + fwdNbFraNodes + fwdNbIrsNodes;
    double[] fwdMarketQuotes = new double[fwdNbNodes];
    fwdMarketQuotes[0] = fwdFixingQuote;
    System.arraycopy(fwdFraQuotes, 0, fwdMarketQuotes, 1, fwdNbFraNodes);
    System.arraycopy(fwdIrsQuotes, 0, fwdMarketQuotes, 1 + fwdNbFraNodes, fwdNbIrsNodes);
    return fwdMarketQuotes;
  }

  public static CurveGroupDefinition config(
      Period[] dscOisTenors,
      String[] dscIdValues,
      Period[] fwd3FraTenors,
      Period[] fwd3IrsTenors,
      String[] fwd3IdValues,
      Period[] fwd6FraTenors,
      Period[] fwd6IrsTenors,
      String[] fwd6IdValues) {
    CurveNode[] dscNodes = new CurveNode[dscOisTenors.length];
    for (int i = 0; i < dscOisTenors.length; i++) {
      dscNodes[i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(dscOisTenors[i]), EUR_FIXED_1Y_EONIA_OIS),
          QuoteId.of(StandardId.of(SCHEME, dscIdValues[i])));
    }
    CurveNode[] fwd3Nodes = new CurveNode[fwd3IdValues.length];
    fwd3Nodes[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(EUR_EURIBOR_3M),
        QuoteId.of(StandardId.of(SCHEME, fwd3IdValues[0])));
    for (int i = 0; i < fwd3FraTenors.length; i++) {
      fwd3Nodes[i + 1] = FraCurveNode.of(FraTemplate.of(fwd3FraTenors[i], EUR_EURIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, fwd3IdValues[i + 1])));
    }
    for (int i = 0; i < fwd3IrsTenors.length; i++) {
      fwd3Nodes[i + 1 + fwd3FraTenors.length] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(fwd3IrsTenors[i]), EUR_FIXED_1Y_EURIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, fwd3IdValues[i + 1 + fwd3FraTenors.length])));
    }
    CurveNode[] fwd6Nodes = new CurveNode[fwd6IdValues.length];
    fwd6Nodes[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(EUR_EURIBOR_6M),
        QuoteId.of(StandardId.of(SCHEME, fwd6IdValues[0])));
    for (int i = 0; i < fwd6FraTenors.length; i++) {
      fwd6Nodes[i + 1] = FraCurveNode.of(FraTemplate.of(fwd6FraTenors[i], EUR_EURIBOR_6M),
          QuoteId.of(StandardId.of(SCHEME, fwd6IdValues[i + 1])));
    }
    for (int i = 0; i < fwd6IrsTenors.length; i++) {
      fwd6Nodes[i + 1 + fwd6FraTenors.length] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(fwd6IrsTenors[i]), EUR_FIXED_1Y_EURIBOR_6M),
          QuoteId.of(StandardId.of(SCHEME, fwd6IdValues[i + 1 + fwd6FraTenors.length])));
    }
    InterpolatedNodalCurveDefinition DSC_CURVE_DEFN =
        InterpolatedNodalCurveDefinition.builder()
            .name(DSCON_CURVE_NAME)
            .xValueType(ValueType.YEAR_FRACTION)
            .yValueType(ValueType.ZERO_RATE)
            .dayCount(CURVE_DC)
            .interpolator(INTERPOLATOR_LINEAR)
            .extrapolatorLeft(EXTRAPOLATOR_FLAT)
            .extrapolatorRight(EXTRAPOLATOR_FLAT)
            .nodes(dscNodes).build();
    InterpolatedNodalCurveDefinition FWD3_CURVE_DEFN =
        InterpolatedNodalCurveDefinition.builder()
            .name(FWD3_CURVE_NAME)
            .xValueType(ValueType.YEAR_FRACTION)
            .yValueType(ValueType.ZERO_RATE)
            .dayCount(CURVE_DC)
            .interpolator(INTERPOLATOR_LINEAR)
            .extrapolatorLeft(EXTRAPOLATOR_FLAT)
            .extrapolatorRight(EXTRAPOLATOR_FLAT)
            .nodes(fwd3Nodes).build();
    InterpolatedNodalCurveDefinition FWD6_CURVE_DEFN =
        InterpolatedNodalCurveDefinition.builder()
            .name(FWD6_CURVE_NAME)
            .xValueType(ValueType.YEAR_FRACTION)
            .yValueType(ValueType.ZERO_RATE)
            .dayCount(CURVE_DC)
            .interpolator(INTERPOLATOR_LINEAR)
            .extrapolatorLeft(EXTRAPOLATOR_FLAT)
            .extrapolatorRight(EXTRAPOLATOR_FLAT)
            .nodes(fwd6Nodes).build();
    return CurveGroupDefinition.builder()
        .name(CURVE_GROUP_NAME)
        .addCurve(DSC_CURVE_DEFN, EUR, EUR_EONIA)
        .addForwardCurve(FWD3_CURVE_DEFN, EUR_EURIBOR_3M)
        .addForwardCurve(FWD6_CURVE_DEFN, EUR_EURIBOR_6M).build();
  }

  public static MarketData allQuotes(
      LocalDate valuationDate,
      double[] dscOisQuotes,
      String[] dscIdValues,
      double[] fwd3MarketQuotes,
      String[] fwd3IdValue,
      double[] fwd6MarketQuotes,
      String[] fwd6IdValue) {
    /* All quotes for the curve calibration */
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(valuationDate);
    for (int i = 0; i < dscOisQuotes.length; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, dscIdValues[i])), dscOisQuotes[i]);
    }
    for (int i = 0; i < fwd3MarketQuotes.length; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, fwd3IdValue[i])), fwd3MarketQuotes[i]);
    }
    for (int i = 0; i < fwd6MarketQuotes.length; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, fwd6IdValue[i])), fwd6MarketQuotes[i]);
    }
    return builder.build();
  }

}

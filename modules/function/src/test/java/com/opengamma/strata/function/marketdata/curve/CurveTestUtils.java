/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.Period;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.finance.rate.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.finance.rate.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.definition.FraCurveNode;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;

/**
 * Helper methods for testing curves.
 */
final class CurveTestUtils {

  private static final String TEST_SCHEME = "test";

  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(
      BusinessDayConventions.FOLLOWING,
      HolidayCalendars.GBLO);

  private static final IborRateSwapLegConvention FLOATING_CONVENTION =
      IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M);

  private static final FixedRateSwapLegConvention FIXED_CONVENTION =
      FixedRateSwapLegConvention.of(Currency.USD, DayCounts.ACT_360, Frequency.P6M, BDA_FOLLOW);

  private static final FixedIborSwapConvention SWAP_CONVENTION =
      FixedIborSwapConvention.of(FIXED_CONVENTION, FLOATING_CONVENTION);

  private CurveTestUtils() {
  }

  static InterpolatedNodalCurveDefinition fraCurveDefinition() {
    String fra1x4 = "fra1x4";
    String fra2x5 = "fra2x5";
    String fra3x6 = "fra3x6";
    String fra6x9 = "fra6x9";
    String fra9x12 = "fra9x12";
    String fra12x15 = "fra12x15";
    String fra18x21 = "fra18x21";

    FraCurveNode fra1x4Node   = fraNode(1, fra1x4);
    FraCurveNode fra2x5Node   = fraNode(2, fra2x5);
    FraCurveNode fra3x6Node   = fraNode(3, fra3x6);
    FraCurveNode fra6x9Node   = fraNode(6, fra6x9);
    FraCurveNode fra9x12Node  = fraNode(9, fra9x12);
    FraCurveNode fra12x15Node = fraNode(12, fra12x15);
    FraCurveNode fra18x21Node = fraNode(18, fra18x21);

    CurveName curveName = CurveName.of("FRA Curve");

    List<CurveNode> nodes =
        ImmutableList.of(fra1x4Node, fra2x5Node, fra3x6Node, fra6x9Node, fra9x12Node, fra12x15Node, fra18x21Node);

    return InterpolatedNodalCurveDefinition.builder()
        .name(curveName)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(DayCounts.ACT_ACT_ISDA)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
  }

  static InterpolatedNodalCurveDefinition fraSwapCurveDefinition() {
    String fra3x6 = "fra3x6";
    String fra6x9 = "fra6x9";
    String swap1y = "swap1y";
    String swap2y = "swap2y";
    String swap3y = "swap3y";

    FraCurveNode fra3x6Node = CurveTestUtils.fraNode(3, fra3x6);
    FraCurveNode fra6x9Node = CurveTestUtils.fraNode(6, fra6x9);
    FixedIborSwapCurveNode swap1yNode = fixedIborSwapNode(Tenor.TENOR_1Y, swap1y);
    FixedIborSwapCurveNode swap2yNode = fixedIborSwapNode(Tenor.TENOR_2Y, swap2y);
    FixedIborSwapCurveNode swap3yNode = fixedIborSwapNode(Tenor.TENOR_3Y, swap3y);

    CurveName curveName = CurveName.of("FRA and Fixed-Float Swap Curve");
    List<CurveNode> nodes = ImmutableList.of(fra3x6Node, fra6x9Node, swap1yNode, swap2yNode, swap3yNode);

    return InterpolatedNodalCurveDefinition.builder()
        .name(curveName)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(DayCounts.ACT_ACT_ISDA)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
  }


  static FraCurveNode fraNode(int startMonths, String id) {
    Period periodToStart = Period.ofMonths(startMonths);
    QuoteKey quoteKey = QuoteKey.of(StandardId.of(TEST_SCHEME, id));
    return FraCurveNode.of(FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M), quoteKey);
  }

   static FixedIborSwapCurveNode fixedIborSwapNode(Tenor tenor, String id) {
    QuoteKey quoteKey = QuoteKey.of(StandardId.of(TEST_SCHEME, id));
    FixedIborSwapTemplate template = FixedIborSwapTemplate.of(Period.ZERO, tenor, SWAP_CONVENTION);
    return FixedIborSwapCurveNode.of(template, quoteKey);
  }

  static ObservableId id(String nodeName) {
    return QuoteId.of(StandardId.of(TEST_SCHEME, nodeName));
  }

  static ObservableId id(CurveNode node) {
    if (node instanceof FraCurveNode) {
      return ((FraCurveNode) node).getRateKey().toObservableId(MarketDataFeed.NONE);
    } else if (node instanceof FixedIborSwapCurveNode) {
      return ((FixedIborSwapCurveNode) node).getRateKey().toObservableId(MarketDataFeed.NONE);
    } else {
      throw new IllegalArgumentException("Unsupported node type " + node.getClass().getName());
    }
  }
}

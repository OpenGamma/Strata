/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.STRIKE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.INTERPOLATOR;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.math.impl.interpolation.PenaltyMatrixGenerator;
import com.opengamma.strata.pricer.option.RawOptionData;

/**
 * Test {@link DirectIborCapletFloorletFlatVolatilityDefinition}.
 */
@Test
public class DirectIborCapletFloorletFlatVolatilityDefinitionTest {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final double LAMBDA_EXPIRY = 0.07;
  private static final ImmutableList<Period> EXPIRIES = ImmutableList.of(Period.ofYears(1), Period.ofYears(3), Period.ofYears(5));
  private static final DoubleArray STRIKES = DoubleArray.of(0.01);
  private static final DoubleMatrix DATA = DoubleMatrix.copyOf(new double[][] {{0.18}, {0.15}, {0.115}});
  private static final RawOptionData SAMPLE_BLACK = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, BLACK_VOLATILITY);
  private static final RawOptionData SAMPLE_NORMAL = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, NORMAL_VOLATILITY);

  public void test_of() {
    DirectIborCapletFloorletFlatVolatilityDefinition test = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), TIME_SQUARE);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), FLAT);
    assertEquals(test.getLambda(), LAMBDA_EXPIRY);
    assertEquals(test.getName(), NAME);
  }

  public void test_of_shift() {
    DirectIborCapletFloorletFlatVolatilityDefinition test = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE, LINEAR, INTERPOLATOR);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), TIME_SQUARE);
    assertEquals(test.getExtrapolatorLeft(), LINEAR);
    assertEquals(test.getExtrapolatorRight(), INTERPOLATOR);
    assertEquals(test.getLambda(), LAMBDA_EXPIRY);
    assertEquals(test.getName(), NAME);
  }

  public void test_builder() {
    DirectIborCapletFloorletFlatVolatilityDefinition test = DirectIborCapletFloorletFlatVolatilityDefinition.builder()
        .name(NAME)
        .index(USD_LIBOR_3M)
        .dayCount(ACT_ACT_ISDA)
        .lambda(LAMBDA_EXPIRY)
        .interpolator(TIME_SQUARE)
        .extrapolatorLeft(LINEAR)
        .extrapolatorRight(INTERPOLATOR)
        .build();
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), TIME_SQUARE);
    assertEquals(test.getExtrapolatorLeft(), LINEAR);
    assertEquals(test.getExtrapolatorRight(), INTERPOLATOR);
    assertEquals(test.getLambda(), LAMBDA_EXPIRY);
    assertEquals(test.getName(), NAME);
  }

  public void test_createMetadata() {
    DirectIborCapletFloorletFlatVolatilityDefinition base = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE);
    assertEquals(base.createCurveMetadata(SAMPLE_BLACK), Curves.blackVolatilityByExpiry(NAME.getName(), ACT_ACT_ISDA));
    assertEquals(base.createCurveMetadata(SAMPLE_NORMAL), Curves.normalVolatilityByExpiry(NAME.getName(), ACT_ACT_ISDA));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.createCurveMetadata(RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, ValueType.PRICE)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.createMetadata(SAMPLE_NORMAL));
  }

  public void test_computePenaltyMatrix() {
    DirectIborCapletFloorletFlatVolatilityDefinition base = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE);
    DoubleArray expiries1 = DoubleArray.of(1d, 2d, 5d);
    DoubleMatrix computed = base.computePenaltyMatrix(expiries1);
    DoubleMatrix expected = PenaltyMatrixGenerator.getPenaltyMatrix(
        expiries1.toArray(), 2).multipliedBy(LAMBDA_EXPIRY);
    assertEquals(computed, expected);
    DoubleArray expiries2 = DoubleArray.of(2d);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.computePenaltyMatrix(expiries2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DirectIborCapletFloorletFlatVolatilityDefinition test1 = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE);
    coverImmutableBean(test1);
    DirectIborCapletFloorletFlatVolatilityDefinition test2 = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        IborCapletFloorletVolatilitiesName.of("other"),
        GBP_LIBOR_3M,
        ACT_365F,
        0.01,
        CurveInterpolators.LINEAR,
        INTERPOLATOR,
        FLAT);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DirectIborCapletFloorletFlatVolatilityDefinition test = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, TIME_SQUARE);
    assertSerialization(test);
  }

}

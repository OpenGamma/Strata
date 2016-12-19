/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.STRIKE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.math.impl.interpolation.PenaltyMatrixGenerator;
import com.opengamma.strata.pricer.option.RawOptionData;

/**
 * Test {@link DirectIborCapletFloorletVolatilityDefinition}.
 */
@Test
public class DirectIborCapletFloorletVolatilityDefinitionTest {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final GridSurfaceInterpolator INTERPOLATOR =
      GridSurfaceInterpolator.of(CurveInterpolators.DOUBLE_QUADRATIC, CurveInterpolators.DOUBLE_QUADRATIC);
  private static final Curve SHIFT_CURVE = ConstantCurve.of("Black Shift", 0.05);
  private static final double LAMBDA_EXPIRY = 0.07;
  private static final double LAMBDA_STRIKE = 0.05;
  private static final ImmutableList<Period> EXPIRIES = ImmutableList.of(Period.ofYears(1), Period.ofYears(3));
  private static final DoubleArray STRIKES = DoubleArray.of(0.01, 0.02, 0.03);
  private static final DoubleMatrix DATA = DoubleMatrix.copyOf(new double[][] {{0.22, 0.18, 0.18}, {0.17, 0.15, 0.165}});
  private static final RawOptionData SAMPLE_BLACK = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, BLACK_VOLATILITY);
  private static final RawOptionData SAMPLE_NORMAL = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, NORMAL_VOLATILITY);

  public void test_of() {
    DirectIborCapletFloorletVolatilityDefinition test = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), INTERPOLATOR);
    assertEquals(test.getLambdaExpiry(), LAMBDA_EXPIRY);
    assertEquals(test.getLambdaStrike(), LAMBDA_STRIKE);
    assertEquals(test.getName(), NAME);
    assertFalse(test.getShiftCurve().isPresent());
  }

  public void test_of_shift() {
    DirectIborCapletFloorletVolatilityDefinition test = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR, SHIFT_CURVE);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), INTERPOLATOR);
    assertEquals(test.getLambdaExpiry(), LAMBDA_EXPIRY);
    assertEquals(test.getLambdaStrike(), LAMBDA_STRIKE);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getShiftCurve().get(), SHIFT_CURVE);
  }

  public void test_builder() {
    DirectIborCapletFloorletVolatilityDefinition test = DirectIborCapletFloorletVolatilityDefinition.builder()
        .name(NAME)
        .index(USD_LIBOR_3M)
        .dayCount(ACT_ACT_ISDA)
        .lambdaExpiry(LAMBDA_EXPIRY)
        .lambdaStrike(LAMBDA_STRIKE)
        .interpolator(INTERPOLATOR)
        .shiftCurve(SHIFT_CURVE)
        .build();
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInterpolator(), INTERPOLATOR);
    assertEquals(test.getLambdaExpiry(), LAMBDA_EXPIRY);
    assertEquals(test.getLambdaStrike(), LAMBDA_STRIKE);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getShiftCurve().get(), SHIFT_CURVE);
  }

  public void test_createMetadata() {
    DirectIborCapletFloorletVolatilityDefinition base = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR);
    assertEquals(base.createMetadata(SAMPLE_BLACK), Surfaces.blackVolatilityByExpiryStrike(NAME.getName(), ACT_ACT_ISDA));
    assertEquals(base.createMetadata(SAMPLE_NORMAL), Surfaces.normalVolatilityByExpiryStrike(NAME.getName(), ACT_ACT_ISDA));
    assertThrowsIllegalArg(() -> base.createMetadata(RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, ValueType.PRICE)));
  }

  public void test_computePenaltyMatrix() {
    DirectIborCapletFloorletVolatilityDefinition base = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR);
    DoubleArray strikes1 = DoubleArray.of(0.1);
    DoubleArray expiries1 = DoubleArray.of(1d, 2d, 5d);
    assertThrowsIllegalArg(() -> base.computePenaltyMatrix(strikes1, expiries1));
    DoubleArray strikes2 = DoubleArray.of(0.01, 0.05, 0.1);
    DoubleArray expiries2 = DoubleArray.of(2d);
    assertThrowsIllegalArg(() -> base.computePenaltyMatrix(strikes2, expiries2));
    DoubleArray strikes3 = DoubleArray.of(0.05, 0.1, 0.15);
    DoubleArray expiries3 = DoubleArray.of(1d, 2d, 5d);
    DoubleMatrix computed = base.computePenaltyMatrix(strikes3, expiries3);
    DoubleMatrix expected = PenaltyMatrixGenerator.getPenaltyMatrix(
        new double[][] {expiries3.toArray(), strikes3.toArray()}, new int[] {2, 2}, new double[] {LAMBDA_EXPIRY, LAMBDA_STRIKE});
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DirectIborCapletFloorletVolatilityDefinition test1 = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR, SHIFT_CURVE);
    coverImmutableBean(test1);
    DirectIborCapletFloorletVolatilityDefinition test2 = DirectIborCapletFloorletVolatilityDefinition.of(
        IborCapletFloorletVolatilitiesName.of("other"),
        GBP_LIBOR_3M,
        ACT_365F,
        0.01,
        0.02,
        GridSurfaceInterpolator.of(CurveInterpolators.LINEAR, CurveInterpolators.LINEAR));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DirectIborCapletFloorletVolatilityDefinition test = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LAMBDA_EXPIRY, LAMBDA_STRIKE, INTERPOLATOR, SHIFT_CURVE);
    assertSerialization(test);
  }

}

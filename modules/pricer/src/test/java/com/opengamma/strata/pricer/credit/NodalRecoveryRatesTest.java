/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.SurfaceName;

/**
 * Test {@link NodalRecoveryRates}.
 */
public class NodalRecoveryRatesTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2016, 5, 6);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final LocalDate DATE_AFTER = LocalDate.of(2017, 2, 24);

  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(
      DefaultCurveMetadata.builder()
          .curveName("test")
          .dayCount(ACT_360)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.RECOVERY_RATE)
          .build(),
      DoubleArray.of(1.0, 3.0, 5.0, 10.0),
      DoubleArray.of(0.4, 0.35, 0.35, 0.3),
      CurveInterpolators.LINEAR);

  @Test
  public void test_of() {
    NodalRecoveryRates test = NodalRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, CURVE);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getValuationDate()).isEqualTo(VALUATION_DATE);
    assertThat(test.recoveryRate(DATE_AFTER)).isEqualTo(
        CURVE.yValue(ACT_360.relativeYearFraction(VALUATION_DATE, DATE_AFTER)));
    assertThat(test.findData(CurveName.of("test"))).isEqualTo(Optional.of(CURVE));
    assertThat(test.findData(SurfaceName.of("foo"))).isEqualTo(Optional.empty());
    assertThat(test.getParameter(1)).isEqualTo(0.35);
    assertThat(test.getParameterCount()).isEqualTo(4);
    assertThat(test.getParameterMetadata(3)).isEqualTo(CURVE.getParameterMetadata(3));
    assertThat(test.withParameter(2, 0.5)).isEqualTo(
        NodalRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, CURVE.withParameter(2, 0.5)));
    assertThat(test.withPerturbation((i, v, m) -> 2d * v)).isEqualTo(
        NodalRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, CURVE.withPerturbation((i, v, m) -> 2d * v)));
    RecoveryRates test2 = RecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, CURVE);
    assertThat(test).isEqualTo(test2);
  }

  //-------------------------------------------------------------------------
  
  @Test
  public void coverage() {
    NodalRecoveryRates test1 = NodalRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, CURVE);
    coverImmutableBean(test1);
    NodalRecoveryRates test2 = NodalRecoveryRates.of(
        StandardId.of("OG", "DEF"),
        DATE_AFTER,
        ConstantNodalCurve.of(
            DefaultCurveMetadata.builder().curveName("other")
                .dayCount(ACT_360)
                .xValueType(ValueType.YEAR_FRACTION)
                .yValueType(ValueType.RECOVERY_RATE).build(),
            5.0, 0.2));
    coverBeanEquals(test1, test2);
  }

}

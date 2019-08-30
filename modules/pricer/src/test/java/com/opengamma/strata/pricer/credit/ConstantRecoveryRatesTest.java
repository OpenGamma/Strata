/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link ConstantRecoveryRates}.
 */
public class ConstantRecoveryRatesTest {

  private static final LocalDate VALUATION = LocalDate.of(2016, 5, 6);
  private static final double RECOVERY_RATE = 0.35;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final LocalDate DATE_AFTER = LocalDate.of(2017, 2, 24);

  @Test
  public void test_of() {
    ConstantRecoveryRates test = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, RECOVERY_RATE);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getRecoveryRate()).isEqualTo(RECOVERY_RATE);
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.recoveryRate(DATE_AFTER)).isEqualTo(RECOVERY_RATE);
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameter(0)).isEqualTo(RECOVERY_RATE);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 0.5)).isEqualTo(ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, 0.5));
  }

  @Test
  public void test_of_interface() {
    ConstantCurve curve = ConstantCurve.of(
        DefaultCurveMetadata.builder()
            .yValueType(ValueType.RECOVERY_RATE)
            .curveName("recoveryRate")
            .build(),
        RECOVERY_RATE);
    ConstantRecoveryRates test = (ConstantRecoveryRates) RecoveryRates.of(LEGAL_ENTITY, VALUATION, curve);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getRecoveryRate()).isEqualTo(RECOVERY_RATE);
    assertThat(test.getValuationDate()).isEqualTo(VALUATION);
    assertThat(test.recoveryRate(DATE_AFTER)).isEqualTo(RECOVERY_RATE);
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameter(0)).isEqualTo(RECOVERY_RATE);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 0.5)).isEqualTo(ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, 0.5));
  }

  @Test
  public void test_of_rateOutOfRange() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, -0.5));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, 1.5));
    ConstantRecoveryRates test = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, RECOVERY_RATE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getParameter(1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.withParameter(1, 0.5));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ConstantRecoveryRates test1 = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION, RECOVERY_RATE);
    coverImmutableBean(test1);
    ConstantRecoveryRates test2 = ConstantRecoveryRates.of(StandardId.of("OG", "DEF"), DATE_AFTER, 0.2d);
    coverBeanEquals(test1, test2);
  }

}

/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link DefaultCubeMetadata}.
 */
public class DefaultCubeMetadataTest {

  private static final String NAME = "TestCube";
  private static final CubeName CUBE_NAME = CubeName.of(NAME);
  private static final LabelParameterMetadata LABEL_METADATA = LabelParameterMetadata.of("LABEL");
  private static final LabelParameterMetadata LABEL_METADATA2 = LabelParameterMetadata.of("LABEL2");
  private static final CubeInfoType<String> DESCRIPTION = CubeInfoType.of("Description");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_String_noMetadata() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(NAME);
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  @Test
  public void test_of_CubeName_noMetadata() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME);
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
    assertThat(test.findParameterIndex(ParameterMetadata.empty())).isEmpty();
    assertThat(test.findParameterIndex(LABEL_METADATA)).isEmpty();
  }

  @Test
  public void test_builder1() {
    DefaultCubeMetadata test = DefaultCubeMetadata.builder()
        .cubeName(CUBE_NAME.toString())
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.DISCOUNT_FACTOR)
        .wValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(DESCRIPTION, "Hello")
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .build();
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getZValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getWValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getInfo(CubeInfoType.DAY_COUNT)).isEqualTo(ACT_365F);
    assertThat(test.findInfo(CubeInfoType.DAY_COUNT)).isEqualTo(Optional.of(ACT_365F));
    assertThat(test.getInfo(DESCRIPTION)).isEqualTo("Hello");
    assertThat(test.findInfo(DESCRIPTION)).isEqualTo(Optional.of("Hello"));
    assertThat(test.findInfo(CubeInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  @Test
  public void test_builder2() {
    DefaultCubeMetadata test = DefaultCubeMetadata.builder()
        .cubeName(CUBE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .zValueType(ValueType.STRIKE)
        .wValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(CubeInfoType.DAY_COUNT, null)
        .addInfo(DESCRIPTION, "Hello")
        .parameterMetadata(ImmutableList.of(LABEL_METADATA))
        .build();
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getZValueType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getWValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getInfo(CubeInfoType.DAY_COUNT));
    assertThat(test.findInfo(CubeInfoType.DAY_COUNT)).isEmpty();
    assertThat(test.getInfo(DESCRIPTION)).isEqualTo("Hello");
    assertThat(test.findInfo(DESCRIPTION)).isEqualTo(Optional.of("Hello"));
    assertThat(test.findInfo(CubeInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(LABEL_METADATA);
    assertThat(test.findParameterIndex(ParameterMetadata.empty()).isPresent()).isFalse();
    assertThat(test.findParameterIndex(LABEL_METADATA)).hasValue(0);
    assertThat(test.findParameterIndex(LABEL_METADATA2)).isEmpty();
  }

  @Test
  public void test_builder3() {
    DefaultCubeMetadata test = DefaultCubeMetadata.builder()
        .cubeName(CUBE_NAME)
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .clearParameterMetadata()
        .build();
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withInfo() {
    DefaultCubeMetadata base = DefaultCubeMetadata.of(CUBE_NAME);
    assertThat(base.findInfo(CubeInfoType.DAY_COUNT).isPresent()).isFalse();
    DefaultCubeMetadata test = base.withInfo(CubeInfoType.DAY_COUNT, ACT_360);
    assertThat(base.findInfo(CubeInfoType.DAY_COUNT).isPresent()).isFalse();
    assertThat(test.findInfo(CubeInfoType.DAY_COUNT).isPresent()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withParameterMetadata() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME)
        .withParameterMetadata(ImmutableList.of(ParameterMetadata.empty()));
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  @Test
  public void test_withParameterMetadata_clearWhenEmpty() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME).withParameterMetadata(null);
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  @Test
  public void test_withParameterMetadata_clearWhenNonEmpty() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME)
        .withParameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .withParameterMetadata(null);
    assertThat(test.getCubeName()).isEqualTo(CUBE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getWValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME);
    coverImmutableBean(test);
    DefaultCubeMetadata test2 = DefaultCubeMetadata.builder()
        .cubeName(CubeName.of("Test"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.DISCOUNT_FACTOR)
        .wValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(ParameterMetadata.empty())
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    DefaultCubeMetadata test = DefaultCubeMetadata.of(CUBE_NAME);
    assertSerialization(test);
  }

}

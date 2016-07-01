/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link DoubleScenarioArray}.
 */
@Test
public class DoubleScenarioArrayTest {

  public void create() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    DoubleScenarioArray test = DoubleScenarioArray.of(values);
    assertThat(test.getValues()).isEqualTo(values);
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1d);
    assertThat(test.get(1)).isEqualTo(2d);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).containsExactly(1d, 2d, 3d);
  }

  public void create_fromList() {
    List<Double> values = ImmutableList.of(1d, 2d, 3d);
    DoubleScenarioArray test = DoubleScenarioArray.of(values);
    assertThat(test.getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1d);
    assertThat(test.get(1)).isEqualTo(2d);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).containsExactly(1d, 2d, 3d);
  }

  public void create_fromFunction() {
    List<Double> values = ImmutableList.of(1d, 2d, 3d);
    DoubleScenarioArray test = DoubleScenarioArray.of(3, i -> values.get(i));
    assertThat(test.getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1d);
    assertThat(test.get(1)).isEqualTo(2d);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).containsExactly(1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    DoubleScenarioArray test = DoubleScenarioArray.of(values);
    coverImmutableBean(test);
    DoubleArray values2 = DoubleArray.of(1, 2, 3);
    DoubleScenarioArray test2 = DoubleScenarioArray.of(values2);
    coverBeanEquals(test, test2);
  }

}

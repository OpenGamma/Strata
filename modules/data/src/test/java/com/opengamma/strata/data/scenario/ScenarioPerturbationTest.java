/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ScenarioPerturbation}.
 */
public class ScenarioPerturbationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_none() {
    ScenarioPerturbation<Double> test = ScenarioPerturbation.none();
    assertThat(test.getScenarioCount()).isEqualTo(1);
    MarketDataBox<Double> box1 = MarketDataBox.ofScenarioValues(1d, 2d, 3d);
    assertThat(test.applyTo(box1, REF_DATA)).isEqualTo(box1);
    MarketDataBox<Double> box2 = MarketDataBox.ofSingleValue(1d);
    assertThat(test.applyTo(box2, REF_DATA)).isEqualTo(box2);
  }

//  public void test_generics() {
//    // Number perturbation should be able to alter a Double box, returning a Number box
//    ScenarioPerturbation<Number> test = ScenarioPerturbation.none();
//    assertThat(test.getScenarioCount()).isEqualTo(1);
//    MarketDataBox<Double> box = MarketDataBox.ofScenarioValues(1d, 2d, 3d);
//    MarketDataBox<Number> perturbed = test.applyTo(box);
//    assertThat(perturbed).isEqualTo(box);
//  }

  @Test
  public void coverage() {
    ScenarioPerturbation<Double> test = ScenarioPerturbation.none();
    coverImmutableBean((ImmutableBean) test);
  }

}

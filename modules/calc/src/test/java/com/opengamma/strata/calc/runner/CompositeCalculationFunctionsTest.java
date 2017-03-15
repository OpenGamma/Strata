/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

@Test
public class CompositeCalculationFunctionsTest {

  public void compose() {
    Fn1 fn1a = new Fn1();
    Fn1 fn1b = new Fn1();
    Fn2 fn2 = new Fn2();

    CalculationFunctions fns1 = CalculationFunctions.of(fn1a);
    CalculationFunctions fns2 = CalculationFunctions.of(fn1b, fn2);
    CalculationFunctions composed = fns1.composedWith(fns2);

    assertEquals(composed.getFunction(new Target1()), fn1a);
    assertEquals(composed.getFunction(new Target2()), fn2);
  }

  //-------------------------------------------------------------------------
  private static final class Fn1 implements CalculationFunction<Target1> {

    @Override
    public Class<Target1> targetType() {
      return Target1.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      throw new UnsupportedOperationException("supportedMeasures not implemented");
    }

    @Override
    public Currency naturalCurrency(Target1 target, ReferenceData refData) {
      throw new UnsupportedOperationException("naturalCurrency not implemented");
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        Target1 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      throw new UnsupportedOperationException("calculate not implemented");
    }

    @Override
    public FunctionRequirements requirements(
        Target1 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      throw new UnsupportedOperationException("requirements not implemented");
    }
  }

  private static final class Fn2 implements CalculationFunction<Target2> {

    @Override
    public Class<Target2> targetType() {
      return Target2.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      throw new UnsupportedOperationException("supportedMeasures not implemented");
    }

    @Override
    public Currency naturalCurrency(Target2 target, ReferenceData refData) {
      throw new UnsupportedOperationException("naturalCurrency not implemented");
    }

    @Override
    public FunctionRequirements requirements(
        Target2 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      throw new UnsupportedOperationException("requirements not implemented");
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        Target2 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      throw new UnsupportedOperationException("calculate not implemented");
    }
  }

  private static final class Target1 implements CalculationTarget {

  }

  private static final class Target2 implements CalculationTarget {

  }
}

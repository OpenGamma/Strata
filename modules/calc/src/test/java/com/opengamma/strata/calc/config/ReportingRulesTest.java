/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;

@Test
public class ReportingRulesTest {

  private static final CalculationTarget TRADE1 = new TestTrade();
  private static final CalculationTarget TRADE2 = new TestTrade();
  private static final CalculationTarget TRADE3 = new TestTrade();
  private static final CalculationTarget TRADE4 = new TestTrade();
  private static final TestRule RULE1 = new TestRule(TRADE1, Currency.GBP);
  private static final TestRule RULE2 = new TestRule(TRADE2, Currency.EUR);
  private static final TestRule RULE3 = new TestRule(TRADE3, Currency.USD);

  public void ofEmpty() {
    ReportingRules rule = ReportingRules.of();
    Optional<Currency> currency = rule.reportingCurrency(TRADE1);

    assertThat(rule).isInstanceOf(EmptyReportingRules.class);
    assertThat(currency).isEmpty();
  }

  public void ofSingle() {
    ReportingRules rule = ReportingRules.of(RULE1);
    Optional<Currency> currency = rule.reportingCurrency(TRADE1);

    assertThat(rule).isInstanceOf(TestRule.class);
    assertThat(currency).hasValue(Currency.GBP);
  }

  public void ofMultiple() {
    ReportingRules rule = ReportingRules.of(RULE1, RULE2);
    Optional<Currency> currency1 = rule.reportingCurrency(TRADE1);
    Optional<Currency> currency2 = rule.reportingCurrency(TRADE2);
    Optional<Currency> currency3 = rule.reportingCurrency(TRADE3);

    assertThat(currency1).hasValue(Currency.GBP);
    assertThat(currency2).hasValue(Currency.EUR);
    assertThat(currency3).isEmpty();
  }

  public void composedWithComposite() {
    CompositeReportingRules compositeRule = CompositeReportingRules.builder().rules(RULE1, RULE2).build();
    ReportingRules rule = compositeRule.composedWith(RULE3);
    Optional<Currency> currency1 = rule.reportingCurrency(TRADE1);
    Optional<Currency> currency2 = rule.reportingCurrency(TRADE2);
    Optional<Currency> currency3 = rule.reportingCurrency(TRADE3);
    Optional<Currency> currency4 = rule.reportingCurrency(TRADE4);

    assertThat(currency1).hasValue(Currency.GBP);
    assertThat(currency2).hasValue(Currency.EUR);
    assertThat(currency3).hasValue(Currency.USD);
    assertThat(currency4).isEmpty();
  }

  public void composedWithEmpty() {
    ReportingRules rule = ReportingRules.empty().composedWith(RULE1);
    Optional<Currency> currency = rule.reportingCurrency(TRADE1);

    assertThat(rule).isInstanceOf(TestRule.class);
    assertThat(currency).hasValue(Currency.GBP);
  }

  public void composedWithSingle() {
    ReportingRules rule = RULE1.composedWith(RULE2);
    Optional<Currency> currency1 = rule.reportingCurrency(TRADE1);
    Optional<Currency> currency2 = rule.reportingCurrency(TRADE2);
    Optional<Currency> currency3 = rule.reportingCurrency(TRADE3);

    assertThat(currency1).hasValue(Currency.GBP);
    assertThat(currency2).hasValue(Currency.EUR);
    assertThat(currency3).isEmpty();
  }

  private static final class TestTrade implements CalculationTarget { }
  
  private static final class TestRule implements ReportingRules {

    private final CalculationTarget trade;
    private final Currency currency;

    private TestRule(CalculationTarget trade, Currency currency) {
      this.trade = trade;
      this.currency = currency;
    }

    @Override
    public Optional<Currency> reportingCurrency(CalculationTarget trade) {
      return (this.trade == trade) ?
          Optional.of(currency) :
          Optional.<Currency>empty();
    }
  }
}

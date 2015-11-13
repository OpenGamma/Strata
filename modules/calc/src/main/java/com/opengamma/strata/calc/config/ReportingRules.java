/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A set of rules that decide how calculation results should be reported.
 */
public interface ReportingRules {

  /**
   * Returns a rule that tries each of the delegate rules in turn and returns the first currency it finds.
   *
   * @param rules  the delegate rules
   * @return a rule that tries each of the rules in turn and returns the first currency it finds
   */
  public static ReportingRules of(ReportingRules... rules) {
    switch (rules.length) {
      case 0:
        return ReportingRules.empty();
      case 1:
        return rules[0];
      default:
        return CompositeReportingRules.builder().rules(ImmutableList.copyOf(rules)).build();
    }
  }

  /**
   * Returns an empty set of rules.
   *
   * @return an empty set of rules
   */
  public static ReportingRules empty() {
    return EmptyReportingRules.INSTANCE;
  }

  /**
   * Returns the currency which should be used when reporting calculation results for the target.
   *
   * @param target  a target
   * @return the currency which should be used when reporting calculation results for the target
   */
  public abstract Optional<Currency> reportingCurrency(CalculationTarget target);

  /**
   * Returns a rule that returns a currency from this rule if available, otherwise returning one from the other rule.
   *
   * @param rule  a reporting rule
   * @return a rule that returns a currency from this rule if available, otherwise returning one from the other rule
   */
  public default ReportingRules composedWith(ReportingRules rule) {
    return of(this, rule);
  }

  /**
   * Returns a rule that always returns the same reporting currency.
   *
   * @param currency  the reporting currency
   * @return a rule that always returns the same reporting currency
   */
  public static ReportingRules fixedCurrency(Currency currency) {
    return FixedReportingRules.of(currency);
  }

  /**
   * Returns a rule that uses the target's primary currency as the reporting currency.
   *
   * @return a rule that uses the target's primary currency as the reporting currency
   */
  public static ReportingRules targetCurrency() {
    return target -> Optional.empty();
  }

  /**
   * Returns a rule that uses the target's pay leg currency as the reporting currency.
   *
   * @return a rule that uses the target's pay leg currency as the reporting currency
   */
  public static ReportingRules payLegCurrency() {
    throw new UnsupportedOperationException("payLeg not implemented");
  }

  /**
   * Returns a rule that uses the target's receive leg currency as the reporting currency.
   *
   * @return a rule that uses the target's receive leg currency as the reporting currency
   */
  public static ReportingRules receiveLegCurrency() {
    throw new UnsupportedOperationException("receiveLeg not implemented");
  }
}

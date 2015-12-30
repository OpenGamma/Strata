/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Reporting rules specify how calculation results should be reported.
 * <p>
 * For example, the rules can be use to request output in a specific currency.
 */
public interface ReportingRules {

  /**
   * Returns a rule set that tries each of the specified rule sets in turn and returns the first currency it finds.
   *
   * @param rules  the rule sets
   * @return a rule set that tries each of the rule sets in turn and returns the first currency it finds
   */
  public static ReportingRules of(ReportingRules... rules) {
    switch (rules.length) {
      case 0:
        return ReportingRules.empty();
      case 1:
        return rules[0];
      default:
        return CompositeReportingRules.of(rules);
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

  //-------------------------------------------------------------------------
  /**
   * Returns the currency which should be used when reporting calculation results for the target.
   *
   * @param target  the target
   * @return the currency which should be used when reporting calculation results for the target
   */
  public abstract Optional<Currency> reportingCurrency(CalculationTarget target);

  //-------------------------------------------------------------------------
  /**
   * Combines these rules with the specified rules.
   * <p>
   * The resulting rules will return mappings from this rule if available,
   * otherwise mappings will be returned from the other rule.
   *
   * @param otherRules  the other rules
   * @return the combined rules
   */
  public default ReportingRules composedWith(ReportingRules otherRules) {
    return CompositeReportingRules.of(this, otherRules);
  }

}

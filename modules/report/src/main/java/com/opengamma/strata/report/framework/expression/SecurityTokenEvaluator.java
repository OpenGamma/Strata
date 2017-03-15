/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;

import com.google.common.collect.Sets;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;

/**
 * Evaluates a token against a security to produce another object.
 * <p>
 * This merges the {@link Security}, {@link SecurityInfo} and {@link SecurityPriceInfo}
 * objects, giving priority to {@code Security}.
 */
public class SecurityTokenEvaluator extends TokenEvaluator<Security> {

  @Override
  public Class<Security> getTargetType() {
    return Security.class;
  }

  @Override
  public Set<String> tokens(Security security) {
    MetaBean metaBean = JodaBeanUtils.metaBean(security.getClass());
    return Sets.union(
        Sets.union(metaBean.metaPropertyMap().keySet(), security.getInfo().propertyNames()),
        security.getInfo().getPriceInfo().propertyNames());
  }

  @Override
  public EvaluationResult evaluate(
      Security security,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    MetaBean metaBean = JodaBeanUtils.metaBean(security.getClass());

    // security
    Optional<String> securityPropertyName = metaBean.metaPropertyMap().keySet().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();
    if (securityPropertyName.isPresent()) {
      Object propertyValue = metaBean.metaProperty(securityPropertyName.get()).get((Bean) security);
      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // security info
    Optional<String> securityInfoPropertyName = security.getInfo().propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();
    if (securityInfoPropertyName.isPresent()) {
      Object propertyValue = security.getInfo().property(securityInfoPropertyName.get()).get();
      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // security price info
    Optional<String> securityPriceInfoPropertyName = security.getInfo().getPriceInfo().propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();
    if (securityPriceInfoPropertyName.isPresent()) {
      Object propertyValue = security.getInfo().getPriceInfo().property(securityPriceInfoPropertyName.get()).get();
      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // not found
    return invalidTokenFailure(security, firstToken);
  }

}

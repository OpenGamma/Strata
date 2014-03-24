/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.integration.marketdata.manipulator.dsl.SimulationUtils;
import com.opengamma.util.ArgumentChecker;

/**
 * Tests if a {@link CurveSpecification} matches a rule.
 * Used for deciding whether a curve should be affected by a scenario.
 */
public abstract class CurveSpecificationMatcher {

  /**
   * Returns true if the curve specification matches the matcher's rule.
   *
   * @param curveSpec a curve specification
   * @return true if the curve specification matches the matcher's rule
   */
  public abstract boolean matches(CurveSpecification curveSpec);

  /**
   * Creates a matcher that matches a curve by name.
   *
   * @param name the name to match against
   * @param names other names to match against
   * @return a matcher that matches curves by name
   */
  public static CurveSpecificationMatcher named(String name, String... names) {
    return new Named(name, names);
  }

  /**
   * Creates a matcher that matches curves whose names match a wildcard pattern.
   * The special characters recognized in the pattern are ? (match any character), * (match any number of characters)
   * and % (same as *). The other characters in the pattern string are escaped before the pattern is created
   * so it can safely contain regular expression characters. Escaping is not supported in the pattern string,
   * i.e. there's no way to match any of the special characters themselves.
   *
   * @param pattern the pattern to match curve names against
   * @return a matcher that matches curves whose names match the pattern
   */
  public static CurveSpecificationMatcher nameLike(String pattern) {
    return new NameLike(pattern);
  }

  /**
   * Performs an exact match on curve name.
   */
  private static class Named extends CurveSpecificationMatcher {

    private final Set<String> _names;

    private Named(String name, String... names) {
      ArgumentChecker.notEmpty(name, "name");

      _names = new HashSet<>(1 + names.length);
      _names.add(name);
      Collections.addAll(_names, names);
    }

    @Override
    public boolean matches(CurveSpecification curveSpec) {
      ArgumentChecker.notNull(curveSpec, "curveSpec");
      return _names.contains(curveSpec.getName());
    }
  }

  /**
   * Matches the curve name against a glob pattern.
   *
   * @see SimulationUtils#patternForGlob
   */
  private static class NameLike extends CurveSpecificationMatcher {

    private final Pattern _pattern;

    private NameLike(String glob) {
      ArgumentChecker.notEmpty(glob, "glob");
      _pattern = SimulationUtils.patternForGlob(glob);
    }

    @Override
    public boolean matches(CurveSpecification curveSpec) {
      ArgumentChecker.notNull(curveSpec, "curveSpec");
      return _pattern.matcher(curveSpec.getName()).matches();
    }
  }
}

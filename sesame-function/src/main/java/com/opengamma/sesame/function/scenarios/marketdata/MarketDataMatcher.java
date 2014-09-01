/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ExternalScheme;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.RegexUtils;

/**
 * Tests if an {@link ExternalIdBundle} representing a piece of market data matches a rule.
 * Used for deciding whether the market data should be affected by a scenario.
 * TODO this lot might have to become Joda beans
 */
public abstract class MarketDataMatcher {

  /**
   * Returns true if any ID in the ID bundle matches this matcher's rule.
   *
   * @param id an ID bundle
   * @return true if any ID in the bundle matches this matcher's rule
   */
  public abstract boolean matches(ExternalIdBundle id);

  /**
   * Creates a matcher that looks for an exact match with an ID.
   *
   * @param scheme the scheme of the IDs to match
   * @param value1 an ID value to match
   * @param values the other ID values to match
   * @return a matcher that matches if the bundle contains any of the IDs
   */
  public static MarketDataMatcher idEquals(String scheme, String value1, String... values) {
    ArgumentChecker.notEmpty(scheme, "scheme");
    ArgumentChecker.notEmpty(value1, "value1");

    Set<ExternalId> ids = new HashSet<>(values.length + 1);
    ids.add(ExternalId.of(scheme, value1));

    for (String value : values) {
      if (StringUtils.isEmpty(value)) {
        throw new IllegalArgumentException("External ID values must not be empty");
      }
      ids.add(ExternalId.of(scheme, value));
    }
    return new Equals(ids);
  }

  /**
   * Creates a matcher that looks for IDs whose values match a wildcard pattern.
   * The special characters recognized in the pattern are ? (match any character), * (match any number of characters)
   * and % (same as *). The other characters in the pattern string are escaped before the pattern is created
   * so it can safely contain regular expression characters. Escaping is not supported in the pattern string,
   * i.e. there's no way to match any of the special characters themselves.
   *
   * @param scheme the ID scheme to match against
   * @param glob the pattern to match values against
   * @return a matcher that matches if the ID bundle contains an ID that matches the pattern
   */
  public static MarketDataMatcher idLike(String scheme, String glob) {
    return new Like(scheme, glob);
  }

  private static class Equals extends MarketDataMatcher {

    private final Set<ExternalId> _ids;

    private Equals(Set<ExternalId> ids) {
      _ids = ids;
    }

    @Override
    public boolean matches(ExternalIdBundle id) {
      Set<ExternalId> intersection = Sets.intersection(id.getExternalIds(), _ids);
      return !intersection.isEmpty();
    }
  }

  private static class Like extends MarketDataMatcher {

    private final ExternalScheme _scheme;
    private final Pattern _pattern;

    private Like(String scheme, String glob) {
      _scheme = ExternalScheme.of(scheme);
      _pattern = RegexUtils.globToPattern(glob);
    }

    @Override
    public boolean matches(ExternalIdBundle id) {
      for (ExternalId externalId : id.getExternalIds()) {
        if (_scheme.equals(externalId.getScheme()) && _pattern.matcher(externalId.getValue()).matches()) {
          return true;
        }
      }
      return false;
    }
  }
}

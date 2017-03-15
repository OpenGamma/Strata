/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.collect.ListMultimap;

/**
 * Finds the party representing "us" in FpML.
 * <p>
 * The FpML data structure is neutral as to the direction of a trade, choosing to
 * represent the two parties throughout the structure. The Strata data model takes
 * the opposite view, with each trade stored with Pay/Receive or Buy/Sell concepts
 * expressed from "our" point of view. This selector is used to bridge the gap,
 * picking the party that represents "us" in the FpML data.
 */
@FunctionalInterface
public interface FpmlPartySelector {

  /**
   * Returns a selector that will choose any party from the trade.
   * <p>
   * The party chosen varies by trade type and will be consistent for a given input file.
   * For example, in a FRA the 'Buy' party will be chosen, whereas in a swap
   * the 'Pay' party of the first leg will be chosen.
   * In general, it is not recommended to rely on this implementation.
   * 
   * @return the selector that will choose the party from the first leg
   */
  public static FpmlPartySelector any() {
    return FpmlDocument.ANY_SELECTOR;
  }

  /**
   * Returns a selector that matches the specified party ID.
   * <p>
   * This examines the party IDs included in the FpML document and returns the
   * href ID for the party that exactly matches the specified party ID.
   * 
   * @param partyId  the party ID to match
   * @return the selector that will choose the party based on the specified party ID
   */
  public static FpmlPartySelector matching(String partyId) {
    return allParties -> allParties.entries().stream()
        .filter(e -> e.getValue().equals(partyId))
        .findFirst()
        .map(e -> e.getKey());
  }

  /**
   * Returns a selector that matches the specified party ID regular expression.
   * <p>
   * This examines the party IDs included in the FpML document and returns the
   * href ID for the party that matches the specified party ID regular expression.
   * 
   * @param partyIdRegex  the party ID regular expression to match
   * @return the selector that will choose the party based on the specified party ID
   */
  public static FpmlPartySelector matchingRegex(Pattern partyIdRegex) {
    return allParties -> allParties.entries().stream()
        .filter(e -> partyIdRegex.matcher(e.getValue()).matches())
        .findFirst()
        .map(e -> e.getKey());
  }

  //-------------------------------------------------------------------------
  /**
   * Selects "our" party from the specified set.
   * 
   * @param allParties  the multimap of party href id to associated partyId
   * @return the party href id to use, empty if unable to find "our" party
   */
  public abstract Optional<String> selectParty(ListMultimap<String, String> allParties);

}

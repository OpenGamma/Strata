/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit.api;

import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Trade;

import java.util.List;
import java.util.Map;

public interface TradeSource {
  List<Trade> trades();

  default Trade tradeById(StandardId id) {
    Map<StandardId, Trade> tradeLookup = trades()
        .stream()
        .collect(Guavate.toImmutableMap(t -> t.getTradeInfo().getId().get()));
    return tradeLookup.get(id);
  }
}

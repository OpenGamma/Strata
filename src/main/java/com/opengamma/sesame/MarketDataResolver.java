/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO this name is provisional
 */
/* package */ class MarketDataResolver {

  /* package */ List<Item> resolve(MarketDataRequirement requirement) {
    throw new UnsupportedOperationException();
  }

  /* package */ static class Item {

    private final String _dataField;
    private final ExternalIdBundle _ids;

    /* package */ Item(String dataField, ExternalIdBundle ids) {
      _dataField = ArgumentChecker.notEmpty(dataField, "dataField");
      _ids = ArgumentChecker.notNull(ids, "idBundle");
    }

    public ExternalIdBundle getIds() {
      return _ids;
    }

    public String getDataField() {
      return _dataField;
    }
  }
}

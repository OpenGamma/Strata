package com.opengamma.strata.examples.exampleccp;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;

import java.util.List;

public class MyColumns {

  public static List<Column> create() {
    return ImmutableList.of(
        Column.of(Measure.ID),
        Column.of(Measure.COUNTERPARTY),
        Column.of(Measure.SETTLEMENT_DATE),
        Column.of(Measure.MATURITY_DATE),
        Column.of(Measure.LEG_INITIAL_NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.LEG_PRESENT_VALUE),
        Column.of(Measure.ACCRUED_INTEREST));
  }

}

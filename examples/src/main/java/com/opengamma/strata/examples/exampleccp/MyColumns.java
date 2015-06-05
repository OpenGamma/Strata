package com.opengamma.strata.examples.exampleccp;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;

import java.util.List;

public class MyColumns {

  public static List<Column> create() {
    return ImmutableList.of(
        Column.of(Measure.TRADE_INFO),
        Column.of(Measure.PRODUCT),
        Column.of(Measure.PAR_RATE),
        Column.of(Measure.PV01),
        Column.of(Measure.LEG_INITIAL_NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.LEG_PRESENT_VALUE),
        Column.of(Measure.ACCRUED_INTEREST));
  }

}

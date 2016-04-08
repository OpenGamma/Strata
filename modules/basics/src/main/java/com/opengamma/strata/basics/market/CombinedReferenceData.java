/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Optional;

/**
 * A set of reference data which combines the data from two other {@link ReferenceData} instances.
 * <p>
 * When an item of data is requested the underlying sets of reference data are checked in order.
 * If the item is present in the first set of data it is returned. If the item is not found
 * it is looked up in the second set of data.
 */
class CombinedReferenceData implements ReferenceData {

  /** The first set of reference data. */
  private final ReferenceData refData1;

  /** The second set of reference data. */
  private final ReferenceData refData2;

  /**
   * Creates an instance.
   *
   * @param refData1  the first set of reference data
   * @param refData2  the second set of reference data
   */
  CombinedReferenceData(ReferenceData refData1, ReferenceData refData2) {
    this.refData1 = refData1;
    this.refData2 = refData2;
  }

  @Override
  public <T> Optional<T> findValue(ReferenceDataId<T> id) {
    Optional<T> value1 = refData1.findValue(id);
    return value1.isPresent() ? value1 : refData2.findValue(id);
  }
}

/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.ResolvableCalculationTarget;

/**
 * A position that has a security identifier that can be resolved using reference data.
 * <p>
 * This represents those positions that hold a security identifier. It allows the position
 * to be resolved, returning an alternate representation of the same position with complete
 * security information.
 */
public interface ResolvableSecurityPosition
    extends Position, ResolvableCalculationTarget {

  /**
   * Resolves the security identifier using the specified reference data.
   * <p>
   * This takes the security identifier of this position, looks it up in reference data,
   * and returns the equivalent position with full security information.
   * If the security has underlying securities, they will also have been resolved in the result.
   * <p>
   * The resulting position is bound to data from reference data.
   * If the data changes, the resulting position form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data to use when resolving
   * @return the resolved position
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  @Override
  public abstract SecuritizedProductPosition<?> resolveTarget(ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  @Override
  public abstract ResolvableSecurityPosition withInfo(PositionInfo info);

  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  @Override
  public abstract ResolvableSecurityPosition withQuantity(double quantity);

}

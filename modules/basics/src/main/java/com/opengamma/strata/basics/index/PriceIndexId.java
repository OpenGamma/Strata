/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.io.Serializable;

import org.joda.beans.PropertyDefinition;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.ReferenceDataId;
import com.opengamma.strata.basics.market.Resolvable;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * An immutable identifier for a Price index.
 * <p>
 * This identifier is used to obtain a {@link PriceIndex} from {@link ReferenceData}.
 * The index itself relates to lending over one night, such as SONIA or EONIA.
 * <p>
 * Identifiers for common indices are provided in {@link PriceIndexIds}.
 */
public final class PriceIndexId
    implements ReferenceDataId<PriceIndex>, Resolvable<PriceIndex>, Named, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The identifier, expressed as a unique name.
   */
  @PropertyDefinition(validate = "notNull")
  private final String name;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * <p>
   * The name uniquely identifies the calendar.
   * The {@link PriceIndex} is resolved from {@link ReferenceData} when required.
   * 
   * @param uniqueName  the unique name
   * @return the identifier
   */
  @FromString
  public static PriceIndexId of(String uniqueName) {
    return new PriceIndexId(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a identifier.
   *
   * @param name  the unique name
   */
  private PriceIndexId(String name) {
    this.name = ArgChecker.notNull(name, "name");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public String getName() {
    return name;
  }

  /**
   * Gets the type of data this identifier refers to.
   * <p>
   * A {@code PriceIndexId} refers to a {@code PriceIndex}.
   *
   * @return the type of the reference data this identifier refers to
   */
  @Override
  public Class<PriceIndex> getReferenceDataType() {
    return PriceIndex.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this identifier to an Price index using the specified reference data.
   * <p>
   * This returns an instance of {@link PriceIndex} that can perform calculations.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data, used to resolve the reference
   * @return the resolved Price index
   * @throws IllegalArgumentException if the identifier is not found
   */
  @Override
  public PriceIndex resolve(ReferenceData refData) {
    return refData.getValue(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   * <p>
   * The comparison checks the name.
   * 
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof PriceIndexId) {
      PriceIndexId other = (PriceIndexId) obj;
      return name.equals(other.name);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the identifier.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Returns the name of the identifier.
   *
   * @return the name
   */
  @Override
  public String toString() {
    return name;
  }

}

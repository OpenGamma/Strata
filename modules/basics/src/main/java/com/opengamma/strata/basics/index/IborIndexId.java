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
 * An immutable identifier for an Ibor index.
 * <p>
 * This identifier is used to obtain a {@link IborIndex} from {@link ReferenceData}.
 * The index itself represents an inter-bank lending rate index, such as LIBOR or EURIBOR.
 * <p>
 * Identifiers for common indices are provided in {@link IborIndexIds}.
 */
public final class IborIndexId
    implements ReferenceDataId<IborIndex>, Resolvable<IborIndex>, Named, Serializable {

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
   * The {@link IborIndex} is resolved from {@link ReferenceData} when required.
   * 
   * @param uniqueName  the unique name
   * @return the identifier
   */
  @FromString
  public static IborIndexId of(String uniqueName) {
    return new IborIndexId(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a identifier.
   *
   * @param name  the unique name
   */
  private IborIndexId(String name) {
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
   * An {@code IborIndexId} refers to a {@code IborIndex}.
   *
   * @return the type of the reference data this identifier refers to
   */
  @Override
  public Class<IborIndex> getReferenceDataType() {
    return IborIndex.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this identifier to an Ibor index using the specified reference data.
   * <p>
   * This returns an instance of {@link IborIndex} that can perform calculations.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data, used to resolve the reference
   * @return the resolved Ibor index
   * @throws IllegalArgumentException if the identifier is not found
   */
  @Override
  public IborIndex resolve(ReferenceData refData) {
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
    if (obj instanceof IborIndexId) {
      IborIndexId other = (IborIndexId) obj;
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

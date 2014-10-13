/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.Tenor;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.named.ExtendedEnum;
import com.opengamma.collect.named.Named;

/**
 * A index of interest rates, such as an Overnight or Inter-Bank rate.
 * <p>
 * Many financial products require knowledge of such as Libor.
 * Implementations of this interface define these rates.
 * <p>
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 * <p>
 * The most common implementations are provided in {@link RateIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RateIndex
    extends Named {

  /**
   * Obtains a {@code RateIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the rate index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RateIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code RateIndex} to be lookup up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<RateIndex> extendedEnum() {
    return RateIndices.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type of the index.
   * 
   * @return the type of the index
   */
  public abstract RateIndexType getType();

  /**
   * Gets the currency of the index.
   * 
   * @return the currency of the index
   */
  public abstract Currency getCurrency();

  /**
   * Gets the tenor of the index.
   * 
   * @return the tenor
   */
  public Tenor getTenor();

  /**
   * Gets the day count convention of the index.
   * 
   * @return the day count convention
   */
  public DayCount getDayCount();

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * In most cases, the rate is published on the fixing date.
   * A few indices, such as the US Fed Fund, publish the rate on the following business day.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next business day and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  public LocalDate calculatePublicationFromFixing(LocalDate fixingDate);

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * For an overnight index, these two dates are typically the same.
   * For an IBOR-like index, they typically differ by two business days.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next business day and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  public LocalDate calculateEffectiveFromFixing(LocalDate fixingDate);

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * For an overnight index, these two dates are typically the same.
   * For an IBOR-like index, they typically differ by two business days.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next business day and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate);

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * For an overnight index, these two dates are one day apart.
   * For an IBOR-like index, they differ by the tenor.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next business day and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  public LocalDate calculateMaturityFromEffective(LocalDate effectiveDate);

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
  public String getName();

}

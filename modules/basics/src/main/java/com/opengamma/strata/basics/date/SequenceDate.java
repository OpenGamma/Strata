/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Instructions to obtain a specific date from a sequence of dates.
 * <p>
 * A {@link DateSequence} can be complex, with interlinked sub-sequences.
 * This class allows the instructions for specifying a single date from the sequence to be expressed.
 * <p>
 * For example, the "base sequence" of a future is often March, June, September and December.
 * But additionally, the nearest two "serial" months are also listed.
 * Together these make the "full sequence".
 * <p>
 * This class can be setup to select from either the base or full sequence, and starting from a specific
 * year-month or from the input date plus a period.
 */
@BeanDefinition(style = "light")
public final class SequenceDate
    implements ImmutableBean, Serializable {

  /**
   * The base year-month.
   * <p>
   * The start of this month is used instead of the input date when starting to count the sequence.
   */
  @PropertyDefinition(get = "optional")
  private final YearMonth yearMonth;
  /**
   * The minimum period before using the sequence number.
   * <p>
   * This is added to the input date before starting to count the sequence.
   */
  @PropertyDefinition(get = "optional")
  private final Period minimumPeriod;
  /**
   * The 1-based sequence number.
   * <p>
   * A value of 1 obtains the first date in the sequence.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final int sequenceNumber;
  /**
   * Whether to use the full sequence (true) or base sequence (false).
   * <p>
   * Many date sequences have two interlinked sequences.
   * One is considered to be the base sequence, selected by setting this to false.
   * The other is considered to be the full sequence, selected by setting this to true.
   * <p>
   * For example, the "base sequence" of a future is often March, June, September and December.
   * But additionally, the nearest two "serial" months are also listed.
   * Together these make the "full sequence".
   */
  @PropertyDefinition
  private final boolean fullSequence;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that selects the next base sequence date on or after the start of the specified month.
   * 
   * @param yearMonth  the month to start from
   * @return the sequence date
   */
  public static SequenceDate base(YearMonth yearMonth) {
    return new SequenceDate(yearMonth, null, 1, false);
  }

  /**
   * Obtains an instance that selects the nth base sequence date on or after the start of the specified month.
   * 
   * @param yearMonth  the month to start from
   * @param sequenceNumber  the 1-based sequence number of the futures, not zero or negative
   * @return the sequence date
   */
  public static SequenceDate base(YearMonth yearMonth, int sequenceNumber) {
    return new SequenceDate(yearMonth, null, sequenceNumber, false);
  }

  /**
   * Obtains an instance that selects the nth base sequence date on or after the input date.
   * 
   * @param sequenceNumber  the 1-based sequence number of the futures
   * @return the sequence date
   */
  public static SequenceDate base(int sequenceNumber) {
    return new SequenceDate(null, null, sequenceNumber, false);
  }

  /**
   * Obtains an instance that selects the nth base sequence date on or after the input date
   * once the minimum period is added.
   * 
   * @param minimumPeriod  minimum period between the input date and the first sequence date
   * @param sequenceNumber  the 1-based sequence number of the futures, not zero or negative
   * @return the sequence date
   */
  public static SequenceDate base(Period minimumPeriod, int sequenceNumber) {
    return new SequenceDate(null, minimumPeriod, sequenceNumber, false);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that selects the next full sequence date on or after the start of the specified month.
   * 
   * @param yearMonth  the month to start from
   * @return the sequence date
   */
  public static SequenceDate full(YearMonth yearMonth) {
    return new SequenceDate(yearMonth, null, 1, true);
  }

  /**
   * Obtains an instance that selects the nth full sequence date on or after the start of the specified month.
   * 
   * @param yearMonth  the month to start from
   * @param sequenceNumber  the 1-based sequence number of the futures, not zero or negative
   * @return the sequence date
   */
  public static SequenceDate full(YearMonth yearMonth, int sequenceNumber) {
    return new SequenceDate(yearMonth, null, sequenceNumber, true);
  }

  /**
   * Obtains an instance that selects the nth full sequence date on or after the input date.
   * 
   * @param sequenceNumber  the 1-based sequence number of the futures
   * @return the sequence date
   */
  public static SequenceDate full(int sequenceNumber) {
    return new SequenceDate(null, null, sequenceNumber, true);
  }

  /**
   * Obtains an instance that selects the nth full sequence date on or after the input date
   * once the minimum period is added.
   * 
   * @param minimumPeriod  minimum period between the input date and the first sequence date
   * @param sequenceNumber  the 1-based sequence number of the futures, not zero or negative
   * @return the sequence date
   */
  public static SequenceDate full(Period minimumPeriod, int sequenceNumber) {
    return new SequenceDate(null, minimumPeriod, sequenceNumber, true);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private SequenceDate(
      YearMonth yearMonth,
      Period minimumPeriod,
      int sequenceNumber,
      boolean fullSequence) {

    if (yearMonth != null && minimumPeriod != null) {
      throw new IllegalArgumentException("Minimum period cannot be set when year-month is present");
    }
    if (minimumPeriod != null && minimumPeriod.isNegative()) {
      throw new IllegalArgumentException("Minimum period cannot be negative");
    }
    this.yearMonth = yearMonth;
    this.minimumPeriod = Period.ZERO.equals(minimumPeriod) ? null : minimumPeriod;
    this.sequenceNumber = ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
    this.fullSequence = fullSequence;
  }

  //-------------------------------------------------------------------------
  // finds the matching date in the sequence
  LocalDate selectDate(LocalDate inputDate, DateSequence sequence, boolean allowSame) {
    DateSequence seq = this.fullSequence ? sequence : sequence.baseSequence();
    if (yearMonth != null) {
      return seq.nthOrSame(yearMonth.atDay(1), sequenceNumber);
    }
    LocalDate startDate = minimumPeriod != null ? inputDate.plus(minimumPeriod) : inputDate;
    return allowSame ? seq.nthOrSame(startDate, sequenceNumber) : seq.nth(startDate, sequenceNumber);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SequenceDate}.
   */
  private static final TypedMetaBean<SequenceDate> META_BEAN =
      LightMetaBean.of(
          SequenceDate.class,
          MethodHandles.lookup(),
          new String[] {
              "yearMonth",
              "minimumPeriod",
              "sequenceNumber",
              "fullSequence"},
          new Object[0]);

  /**
   * The meta-bean for {@code SequenceDate}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SequenceDate> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public TypedMetaBean<SequenceDate> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base year-month.
   * <p>
   * The start of this month is used instead of the input date when starting to count the sequence.
   * @return the optional value of the property, not null
   */
  public Optional<YearMonth> getYearMonth() {
    return Optional.ofNullable(yearMonth);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the minimum period before using the sequence number.
   * <p>
   * This is added to the input date before starting to count the sequence.
   * @return the optional value of the property, not null
   */
  public Optional<Period> getMinimumPeriod() {
    return Optional.ofNullable(minimumPeriod);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the 1-based sequence number.
   * <p>
   * A value of 1 obtains the first date in the sequence.
   * @return the value of the property
   */
  public int getSequenceNumber() {
    return sequenceNumber;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether to use the full sequence (true) or base sequence (false).
   * <p>
   * Many date sequences have two interlinked sequences.
   * One is considered to be the base sequence, selected by setting this to false.
   * The other is considered to be the full sequence, selected by setting this to true.
   * <p>
   * For example, the "base sequence" of a future is often March, June, September and December.
   * But additionally, the nearest two "serial" months are also listed.
   * Together these make the "full sequence".
   * @return the value of the property
   */
  public boolean isFullSequence() {
    return fullSequence;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SequenceDate other = (SequenceDate) obj;
      return JodaBeanUtils.equal(yearMonth, other.yearMonth) &&
          JodaBeanUtils.equal(minimumPeriod, other.minimumPeriod) &&
          (sequenceNumber == other.sequenceNumber) &&
          (fullSequence == other.fullSequence);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(yearMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(minimumPeriod);
    hash = hash * 31 + JodaBeanUtils.hashCode(sequenceNumber);
    hash = hash * 31 + JodaBeanUtils.hashCode(fullSequence);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SequenceDate{");
    buf.append("yearMonth").append('=').append(JodaBeanUtils.toString(yearMonth)).append(',').append(' ');
    buf.append("minimumPeriod").append('=').append(JodaBeanUtils.toString(minimumPeriod)).append(',').append(' ');
    buf.append("sequenceNumber").append('=').append(JodaBeanUtils.toString(sequenceNumber)).append(',').append(' ');
    buf.append("fullSequence").append('=').append(JodaBeanUtils.toString(fullSequence));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

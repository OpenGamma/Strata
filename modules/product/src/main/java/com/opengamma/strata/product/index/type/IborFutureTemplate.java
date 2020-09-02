/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * A template for creating an Ibor Future trade.
 */
@BeanDefinition(style = "light")
@SuppressWarnings("deprecation")
public final class IborFutureTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
   * The instructions that define which future is desired.
   */
  @PropertyDefinition(validate = "notNull")
  private final SequenceDate sequenceDate;
  /**
   * The underlying contract specification.
   * <p>
   * This specifies the contract of the Ibor Futures to be created.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFutureContractSpec contractSpec;

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified contract specification and sequence date.
   * <p>
   * The specific future is defined by two date-related inputs -
   * the sequence date and the date sequence embedded in the contract specification.
   * 
   * @param sequenceDate  the instructions that define which future is desired
   * @param contractSpec  the contract specification
   * @return the template
   */
  public static IborFutureTemplate of(SequenceDate sequenceDate, IborFutureContractSpec contractSpec) {
    return new IborFutureTemplate(sequenceDate, contractSpec);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified convention using a relative definition of time.
   * <p>
   * The specific future is defined by two date-related inputs, the minimum period and the 1-based future number.
   * For example, the 2nd future of the series where the 1st future is at least 1 week after the value date
   * would be represented by a minimum period of 1 week and future number 2.
   * 
   * @param minimumPeriod  the minimum period between the base date and the first future
   * @param sequenceNumber  the 1-based index of the future after the minimum period, must be 1 or greater
   * @param convention  the future convention
   * @return the template
   * @deprecated Use {@link #of(SequenceDate, IborFutureContractSpec)}
   */
  @Deprecated
  public static IborFutureTemplate of(Period minimumPeriod, int sequenceNumber, IborFutureConvention convention) {
    IborFutureContractSpec contractSpec = IborFutureContractSpec.of(convention.getName());
    return IborFutureTemplate.of(SequenceDate.base(minimumPeriod, sequenceNumber), contractSpec);
  }

  /**
   * Obtains a template based on the specified convention using an absolute definition of time.
   * <p>
   * The future is selected from a sequence of futures based on a year-month.
   * In most cases, the date of the future will be in the same month as the specified month,
   * but this is not guaranteed.
   * 
   * @param yearMonth  the year-month to use to select the future
   * @param convention  the future convention
   * @return the template
   * @deprecated Use {@link #of(SequenceDate, IborFutureContractSpec)}
   */
  @Deprecated
  public static IborFutureTemplate of(YearMonth yearMonth, IborFutureConvention convention) {
    IborFutureContractSpec contractSpec = IborFutureContractSpec.of(convention.getName());
    return IborFutureTemplate.of(SequenceDate.base(yearMonth), contractSpec);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying index.
   * 
   * @return the index
   */
  public IborIndex getIndex() {
    return contractSpec.getIndex();
  }

  /**
   * Gets the market convention of the Ibor future.
   * 
   * @return the convention
   * @deprecated Use {@link #getContractSpec()}
   */
  @Deprecated
  public IborFutureConvention getConvention() {
    // this should smooth over the transition to contract specs in most cass
    ImmutableIborFutureContractSpec spec = (ImmutableIborFutureContractSpec) getContractSpec();
    return ImmutableIborFutureConvention.builder()
        .name(spec.getName())
        .index(spec.getIndex())
        .dateSequence(spec.getDateSequence())
        .businessDayAdjustment(spec.getBusinessDayAdjustment())
        .build();
  }

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * 
   * @param tradeDate  the date of the trade
   * @param securityId  the identifier of the security
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param price  the trade price
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double price,
      ReferenceData refData) {

    return contractSpec.createTrade(tradeDate, securityId, sequenceDate, quantity, price, refData);
  }

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * The notional is unsigned, with the quantity determining the direction of the trade.
   * 
   * @param tradeDate  the date of the trade
   * @param securityId  the identifier of the security
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param notional  the notional amount of one future contract
   * @param price  the trade price
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @deprecated Use {@link #createTrade(LocalDate, SecurityId, double, double, ReferenceData)}
   */
  @Deprecated
  public IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double notional,
      double price,
      ReferenceData refData) {

    return createTrade(tradeDate, securityId, quantity, price, refData);
  }

  /**
   * Calculates the reference date of the trade.
   * 
   * @param tradeDate  the date of the trade
   * @param refData  the reference data, used to resolve the date
   * @return the future reference date
   */
  public LocalDate calculateReferenceDateFromTradeDate(LocalDate tradeDate, ReferenceData refData) {
    return contractSpec.calculateReferenceDate(tradeDate, sequenceDate, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code IborFutureTemplate}.
   */
  private static final TypedMetaBean<IborFutureTemplate> META_BEAN =
      LightMetaBean.of(
          IborFutureTemplate.class,
          MethodHandles.lookup(),
          new String[] {
              "sequenceDate",
              "contractSpec"},
          new Object[0]);

  /**
   * The meta-bean for {@code IborFutureTemplate}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<IborFutureTemplate> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborFutureTemplate(
      SequenceDate sequenceDate,
      IborFutureContractSpec contractSpec) {
    JodaBeanUtils.notNull(sequenceDate, "sequenceDate");
    JodaBeanUtils.notNull(contractSpec, "contractSpec");
    this.sequenceDate = sequenceDate;
    this.contractSpec = contractSpec;
  }

  @Override
  public TypedMetaBean<IborFutureTemplate> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the instructions that define which future is desired.
   * @return the value of the property, not null
   */
  public SequenceDate getSequenceDate() {
    return sequenceDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying contract specification.
   * <p>
   * This specifies the contract of the Ibor Futures to be created.
   * @return the value of the property, not null
   */
  public IborFutureContractSpec getContractSpec() {
    return contractSpec;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborFutureTemplate other = (IborFutureTemplate) obj;
      return JodaBeanUtils.equal(sequenceDate, other.sequenceDate) &&
          JodaBeanUtils.equal(contractSpec, other.contractSpec);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(sequenceDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractSpec);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("IborFutureTemplate{");
    buf.append("sequenceDate").append('=').append(JodaBeanUtils.toString(sequenceDate)).append(',').append(' ');
    buf.append("contractSpec").append('=').append(JodaBeanUtils.toString(contractSpec));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

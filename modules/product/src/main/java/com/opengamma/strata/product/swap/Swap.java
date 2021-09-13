/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.util.stream.Collectors.joining;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.SummarizerUtils;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * A rate swap.
 * <p>
 * A rate swap is a financial instrument that represents the exchange of streams of payments.
 * The swap is formed of legs, where each leg typically represents the obligations
 * of the seller or buyer of the swap. In the simplest vanilla interest rate swap,
 * there are two legs, one with a fixed rate and the other a floating rate.
 * Many other more complex swaps can also be represented.
 * <p>
 * For example, a swap might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate every 3 months for 2 years.
 */
@BeanDefinition
public final class Swap
    implements Product, Resolvable<ResolvedSwap>, ImmutableBean, Serializable {

  /**
   * The legs of the swap.
   * <p>
   * A swap consists of one or more legs.
   * The legs of a swap are essentially unordered, however it is more efficient
   * and closer to user expectation to treat them as being ordered.
   */
  @PropertyDefinition(validate = "notEmpty", builderType = "List<? extends SwapLeg>")
  private final ImmutableList<SwapLeg> legs;

  //-------------------------------------------------------------------------
  /**
   * Creates a swap from one or more swap legs.
   * <p>
   * While most swaps have two legs, other combinations are possible.
   * 
   * @param legs  the array of legs
   * @return the swap
   */
  public static Swap of(SwapLeg... legs) {
    ArgChecker.notEmpty(legs, "legs");
    return new Swap(ImmutableList.copyOf(legs));
  }

  /**
   * Creates a swap from one or more swap legs.
   * <p>
   * While most swaps have two legs, other combinations are possible.
   * 
   * @param legs  the list of legs
   * @return the swap
   */
  public static Swap of(List<? extends SwapLeg> legs) {
    ArgChecker.notEmpty(legs, "legs");
    return new Swap(ImmutableList.copyOf(legs));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the legs of the swap with the specified type.
   * <p>
   * This returns all the legs with the given type.
   * 
   * @param type  the type to find
   * @return the matching legs of the swap
   */
  public ImmutableList<SwapLeg> getLegs(SwapLegType type) {
    return legs.stream().filter(leg -> leg.getType() == type).collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the first pay or receive leg of the swap.
   * <p>
   * This returns the first pay or receive leg of the swap, empty if no matching leg.
   * 
   * @param payReceive  the pay or receive flag
   * @return the first matching leg of the swap
   */
  public Optional<SwapLeg> getLeg(PayReceive payReceive) {
    return legs.stream().filter(leg -> leg.getPayReceive() == payReceive).findFirst();
  }

  /**
   * Gets the first pay leg of the swap.
   * <p>
   * This returns the first pay leg of the swap, empty if no pay leg.
   * 
   * @return the first pay leg of the swap
   */
  public Optional<SwapLeg> getPayLeg() {
    return getLeg(PayReceive.PAY);
  }

  /**
   * Gets the first receive leg of the swap.
   * <p>
   * This returns the first receive leg of the swap, empty if no receive leg.
   * 
   * @return the first receive leg of the swap
   */
  public Optional<SwapLeg> getReceiveLeg() {
    return getLeg(PayReceive.RECEIVE);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the swap.
   * <p>
   * This is the earliest accrual date of the legs, often known as the effective date.
   * The latest date is chosen by examining the unadjusted end date.
   * 
   * @return the start date of the swap
   */
  @DerivedProperty
  public AdjustableDate getStartDate() {
    return legs.stream()
        .map(SwapLeg::getStartDate)
        .min(Comparator.comparing(adjDate -> adjDate.getUnadjusted()))
        .get();  // always at least one leg, so get() is safe
  }

  /**
   * Gets the accrual end date of the swap.
   * <p>
   * This is the latest accrual date of the legs, often known as the termination date.
   * The latest date is chosen by examining the unadjusted end date.
   * 
   * @return the end date of the swap
   */
  @DerivedProperty
  public AdjustableDate getEndDate() {
    return legs.stream()
        .map(SwapLeg::getEndDate)
        .max(Comparator.comparing(adjDate -> adjDate.getUnadjusted()))
        .get();  // always at least one leg, so get() is safe
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of payment currencies referred to by the swap.
   * <p>
   * This returns the complete set of payment currencies for the swap.
   * This will typically return one or two currencies.
   * <p>
   * If there is an FX reset, then this set contains the currency of the payment,
   * not the currency of the notional. Note that in many cases, the currency of
   * the FX reset notional will be the currency of the other leg.
   * 
   * @return the set of payment currencies referred to by this swap
   */
  @Override
  public ImmutableSet<Currency> allPaymentCurrencies() {
    return legs.stream().map(leg -> leg.getCurrency()).collect(toImmutableSet());
  }

  /**
   * Returns the set of currencies referred to by the swap.
   * <p>
   * This returns the complete set of currencies for the swap, not just the payment currencies.
   * 
   * @return the set of currencies referred to by this swap
   */
  @Override
  public ImmutableSet<Currency> allCurrencies() {
    ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
    legs.stream().forEach(leg -> leg.collectCurrencies(builder));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of indices referred to by the swap.
   * <p>
   * A swap will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Calling this method will return the complete list of indices, including
   * any associated with FX reset.
   * 
   * @return the set of indices referred to by this swap
   */
  public ImmutableSet<Index> allIndices() {
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    legs.stream().forEach(leg -> leg.collectIndices(builder));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Summarizes this swap into string form.
   *
   * @return the summary description
   */
  public String summaryDescription() {
    // 5Y USD 2mm Rec USD-LIBOR-6M / Pay 1% : 21Jan17-21Jan22
    StringBuilder buf = new StringBuilder(64);
    buf.append(SummarizerUtils.datePeriod(getStartDate().getUnadjusted(), getEndDate().getUnadjusted()));
    buf.append(' ');
    if (getLegs().size() == 2 &&
        getPayLeg().isPresent() &&
        getReceiveLeg().isPresent() &&
        getLegs().stream().allMatch(leg -> leg instanceof RateCalculationSwapLeg)) {

      // normal swap
      SwapLeg payLeg = getPayLeg().get();
      SwapLeg recLeg = getReceiveLeg().get();
      String payNotional = notional(payLeg);
      String recNotional = notional(recLeg);
      if (payNotional.equals(recNotional)) {
        buf.append(recNotional);
        buf.append(" Rec ");
        buf.append(legSummary(recLeg));
        buf.append(" / Pay ");
        buf.append(legSummary(payLeg));
      } else {
        buf.append("Rec ");
        buf.append(legSummary(recLeg));
        buf.append(' ');
        buf.append(recNotional);
        buf.append(" / Pay ");
        buf.append(legSummary(payLeg));
        buf.append(' ');
        buf.append(payNotional);
      }
    } else {
      // abnormal swap
      buf.append(getLegs().stream()
          .map(leg -> (SummarizerUtils.payReceive(leg.getPayReceive()) + " " + legSummary(leg) + " " + notional(leg)).trim())
          .collect(joining(" / ")));
    }
    buf.append(" : ");
    buf.append(SummarizerUtils.dateRange(getStartDate().getUnadjusted(), getEndDate().getUnadjusted()));
    return buf.toString();
  }

  // the notional, with trailing space if present
  private String notional(SwapLeg leg) {
    if (leg instanceof RateCalculationSwapLeg) {
      RateCalculationSwapLeg rcLeg = (RateCalculationSwapLeg) leg;
      NotionalSchedule notionalSchedule = rcLeg.getNotionalSchedule();
      ValueSchedule amount = notionalSchedule.getAmount();
      double notional = amount.getInitialValue();
      String vary = !amount.getSteps().isEmpty() || amount.getStepSequence().isPresent() ? " variable" : "";
      Currency currency = notionalSchedule.getFxReset().map(fxr -> fxr.getReferenceCurrency()).orElse(rcLeg.getCurrency());
      return SummarizerUtils.amount(currency, notional) + vary;
    }
    if (leg instanceof RatePeriodSwapLeg) {
      RatePeriodSwapLeg rpLeg = (RatePeriodSwapLeg) leg;
      return SummarizerUtils.amount(rpLeg.getPaymentPeriods().get(0).getNotionalAmount());
    }
    return "";
  }

  // a summary of the leg
  private String legSummary(SwapLeg leg) {
    if (leg instanceof RateCalculationSwapLeg) {
      RateCalculationSwapLeg rcLeg = (RateCalculationSwapLeg) leg;
      RateCalculation calculation = rcLeg.getCalculation();
      if (calculation instanceof FixedRateCalculation) {
        FixedRateCalculation calc = (FixedRateCalculation) calculation;
        String vary = !calc.getRate().getSteps().isEmpty() || calc.getRate().getStepSequence().isPresent() ? " variable" : "";
        return SummarizerUtils.percent(calc.getRate().getInitialValue()) + vary;
      }
      if (calculation instanceof IborRateCalculation) {
        IborRateCalculation calc = (IborRateCalculation) calculation;
        String gearing = calc.getGearing().map(g -> " * " + SummarizerUtils.value(g.getInitialValue())).orElse("");
        String spread = calc.getSpread().map(s -> " + " + SummarizerUtils.percent(s.getInitialValue())).orElse("");
        return calc.getIndex().getName() + gearing + spread;
      }
      if (calculation instanceof OvernightRateCalculation) {
        OvernightRateCalculation calc = (OvernightRateCalculation) calculation;
        String avg = calc.getAccrualMethod() == OvernightAccrualMethod.AVERAGED ? " avg" : "";
        String gearing = calc.getGearing().map(g -> " * " + SummarizerUtils.value(g.getInitialValue())).orElse("");
        String spread = calc.getSpread().map(s -> " + " + SummarizerUtils.percent(s.getInitialValue())).orElse("");
        return calc.getIndex().getName() + avg + gearing + spread;
      }
      if (calculation instanceof InflationRateCalculation) {
        InflationRateCalculation calc = (InflationRateCalculation) calculation;
        String gearing = calc.getGearing().map(g -> " * " + SummarizerUtils.value(g.getInitialValue())).orElse("");
        return calc.getIndex().getName() + gearing;
      }
    }
    if (leg instanceof KnownAmountSwapLeg) {
      KnownAmountSwapLeg kaLeg = (KnownAmountSwapLeg) leg;
      String vary =
          !kaLeg.getAmount().getSteps().isEmpty() || kaLeg.getAmount().getStepSequence().isPresent() ? " variable" : "";
      return SummarizerUtils.amount(kaLeg.getCurrency(), kaLeg.getAmount().getInitialValue()) + vary;
    }
    ImmutableSet<Index> allIndices = leg.allIndices();
    return allIndices.isEmpty() ? "Fixed" : allIndices.toString();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance based on this swap with the start date replaced.
   * <p>
   * This is used to change the start date of the swap, and is currently used by {@link Swaption}.
   * 
   * @param adjustedStartDate the new adjusted start date
   * @return the updated swap
   * @throws IllegalArgumentException if the start date cannot be replaced with the proposed start date
   * @throws UnsupportedOperationException if changing the start date is not supported
   */
  public Swap replaceStartDate(LocalDate adjustedStartDate) {
    return new Swap(legs.stream().map(leg -> leg.replaceStartDate(adjustedStartDate)).collect(toImmutableList()));
  }

  @Override
  public ResolvedSwap resolve(ReferenceData refData) {
    // avoid streams as profiling showed a hotspot
    // most efficient to loop around legs once
    ImmutableList.Builder<ResolvedSwapLeg> resolvedLegs = ImmutableList.builder();
    ImmutableSet.Builder<Currency> currencies = ImmutableSet.builder();
    ImmutableSet.Builder<Index> indices = ImmutableSet.builder();
    for (SwapLeg leg : legs) {
      ResolvedSwapLeg resolvedLeg = leg.resolve(refData);
      resolvedLegs.add(resolvedLeg);
      currencies.add(resolvedLeg.getCurrency());
      leg.collectIndices(indices);
    }
    return new ResolvedSwap(resolvedLegs.build(), currencies.build(), indices.build());
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Swap}.
   * @return the meta-bean, not null
   */
  public static Swap.Meta meta() {
    return Swap.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Swap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Swap.Builder builder() {
    return new Swap.Builder();
  }

  private Swap(
      List<? extends SwapLeg> legs) {
    JodaBeanUtils.notEmpty(legs, "legs");
    this.legs = ImmutableList.copyOf(legs);
  }

  @Override
  public Swap.Meta metaBean() {
    return Swap.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legs of the swap.
   * <p>
   * A swap consists of one or more legs.
   * The legs of a swap are essentially unordered, however it is more efficient
   * and closer to user expectation to treat them as being ordered.
   * @return the value of the property, not empty
   */
  public ImmutableList<SwapLeg> getLegs() {
    return legs;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Swap other = (Swap) obj;
      return JodaBeanUtils.equal(legs, other.legs);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legs);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("Swap{");
    buf.append("legs").append('=').append(JodaBeanUtils.toString(legs)).append(',').append(' ');
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(getStartDate())).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Swap}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code legs} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SwapLeg>> legs = DirectMetaProperty.ofImmutable(
        this, "legs", Swap.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<AdjustableDate> startDate = DirectMetaProperty.ofDerived(
        this, "startDate", Swap.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<AdjustableDate> endDate = DirectMetaProperty.ofDerived(
        this, "endDate", Swap.class, AdjustableDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legs",
        "startDate",
        "endDate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return legs;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Swap.Builder builder() {
      return new Swap.Builder();
    }

    @Override
    public Class<? extends Swap> beanType() {
      return Swap.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code legs} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SwapLeg>> legs() {
      return legs;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> endDate() {
      return endDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return ((Swap) bean).getLegs();
        case -2129778896:  // startDate
          return ((Swap) bean).getStartDate();
        case -1607727319:  // endDate
          return ((Swap) bean).getEndDate();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Swap}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Swap> {

    private List<? extends SwapLeg> legs = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Swap beanToCopy) {
      this.legs = beanToCopy.getLegs();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return legs;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          this.legs = (List<? extends SwapLeg>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Swap build() {
      return new Swap(
          legs);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the legs of the swap.
     * <p>
     * A swap consists of one or more legs.
     * The legs of a swap are essentially unordered, however it is more efficient
     * and closer to user expectation to treat them as being ordered.
     * @param legs  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder legs(List<? extends SwapLeg> legs) {
      JodaBeanUtils.notEmpty(legs, "legs");
      this.legs = legs;
      return this;
    }

    /**
     * Sets the {@code legs} property in the builder
     * from an array of objects.
     * @param legs  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder legs(SwapLeg... legs) {
      return legs(ImmutableList.copyOf(legs));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("Swap.Builder{");
      buf.append("legs").append('=').append(JodaBeanUtils.toString(legs)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(null));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

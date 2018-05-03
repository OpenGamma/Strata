/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A curve formed from two curves, the base curve and the spread curve.
 * <p>
 * The parameters of this curve are the combination of the base curve parameters and spread curve parameters.
 * The node sensitivities are calculated in terms of the nodes on the base curve and spread curve.
 * <p>
 * If one of the two curves must be fixed, use {@link AddFixedCurve}.
 */
@BeanDefinition(builderScope = "private")
public final class CombinedCurve
    implements Curve, ImmutableBean, Serializable {

  /**
   * The base curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve baseCurve;
  /**
   * The spread curve. 
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve spreadCurve;
  /**
   * The curve metadata.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;

  //-------------------------------------------------------------------------
  /**
   * Creates a curve as the sum of a base curve and a spread curve with a specified curve metadata.
   * 
   * @param baseCurve  the base curve 
   * @param spreadCurve  the spread curve
   * @param metadata  the metadata
   * @return the combined curve
   */
  public static CombinedCurve of(Curve baseCurve, Curve spreadCurve, CurveMetadata metadata) {

    return new CombinedCurve(baseCurve, spreadCurve, metadata);
  }

  /**
   * Creates a curve as the sum of a base curve and a spread curve.
   * <p>
   * The metadata of the combined curve will be created form the base curve and spread curve.
   * 
   * @param baseCurve  the base curve
   * @param spreadCurve  the spread curve
   * @return the combined curve
   */
  public static CombinedCurve of(Curve baseCurve, Curve spreadCurve) {
    CurveMetadata baseMetadata = baseCurve.getMetadata();
    CurveMetadata spreadMetadata = spreadCurve.getMetadata();

    List<ParameterMetadata> paramMeta = Stream.concat(
        IntStream.range(0, baseCurve.getParameterCount())
            .mapToObj(i -> baseCurve.getParameterMetadata(i)),
        IntStream.range(0, spreadCurve.getParameterCount())
            .mapToObj(i -> spreadCurve.getParameterMetadata(i)))
        .collect(toImmutableList());

    DefaultCurveMetadataBuilder metadataBuilder = DefaultCurveMetadata.builder()
        .curveName(baseCurve.getName().getName() + "+" + spreadMetadata.getCurveName().getName())
        .xValueType(baseMetadata.getXValueType())
        .yValueType(baseMetadata.getYValueType())
        .parameterMetadata(paramMeta);

    if (baseMetadata.findInfo(CurveInfoType.DAY_COUNT).isPresent()) {
      metadataBuilder.addInfo(
          CurveInfoType.DAY_COUNT, baseMetadata.getInfo(CurveInfoType.DAY_COUNT));
    }

    return of(baseCurve, spreadCurve, metadataBuilder.build());
  }

  @ImmutableConstructor
  private CombinedCurve(
      Curve baseCurve,
      Curve spreadCurve,
      CurveMetadata metadata) {

    JodaBeanUtils.notNull(baseCurve, "baseCurve");
    JodaBeanUtils.notNull(spreadCurve, "spreadCurve");
    JodaBeanUtils.notNull(metadata, "metadata");

    CurveMetadata baseMetadata = baseCurve.getMetadata();
    CurveMetadata spreadMetadata = spreadCurve.getMetadata();
    // check value type
    if (!baseMetadata.getXValueType().equals(metadata.getXValueType())) {
      throw new IllegalArgumentException(Messages.format(
          "xValueType is {} in baseCurve, but {} in CombinedCurve",
          baseMetadata.getXValueType(),
          metadata.getXValueType()));
    }
    if (!spreadMetadata.getXValueType().equals(metadata.getXValueType())) {
      throw new IllegalArgumentException(Messages.format(
          "xValueType is {} in spreadCurve, but {} in CombinedCurve",
          spreadMetadata.getXValueType(),
          metadata.getXValueType()));
    }
    if (!baseMetadata.getYValueType().equals(metadata.getYValueType())) {
      throw new IllegalArgumentException(Messages.format(
          "yValueType is {} in baseCurve, but {} in CombinedCurve",
          baseMetadata.getYValueType(),
          metadata.getYValueType()));
    }
    if (!spreadMetadata.getYValueType().equals(metadata.getYValueType())) {
      throw new IllegalArgumentException(Messages.format(
          "yValueType is {} in spreadCurve, but {} in CombinedCurve",
          spreadMetadata.getYValueType(),
          metadata.getYValueType()));
    }
    // check day count
    Optional<DayCount> dccOpt = metadata.findInfo(CurveInfoType.DAY_COUNT);
    if (dccOpt.isPresent()) {
      DayCount dcc = dccOpt.get();
      if (!baseMetadata.findInfo(CurveInfoType.DAY_COUNT).isPresent() ||
          !baseMetadata.getInfo(CurveInfoType.DAY_COUNT).equals(dcc)) {
        throw new IllegalArgumentException(
            Messages.format("DayCount in baseCurve should be {}", dcc));
      }
      if (!spreadMetadata.findInfo(CurveInfoType.DAY_COUNT).isPresent() ||
          !spreadMetadata.getInfo(CurveInfoType.DAY_COUNT).equals(dcc)) {
        throw new IllegalArgumentException(
            Messages.format("DayCount in spreadCurve should be {}", dcc));
      }
    }

    this.metadata = metadata;
    this.baseCurve = baseCurve;
    this.spreadCurve = spreadCurve;
  }

  //-------------------------------------------------------------------------
  @Override
  public CombinedCurve withMetadata(CurveMetadata metadata) {
    return new CombinedCurve(baseCurve, spreadCurve, metadata);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return baseCurve.getParameterCount() + spreadCurve.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    if (parameterIndex < baseCurve.getParameterCount()) {
      return baseCurve.getParameter(parameterIndex);
    }
    return spreadCurve.getParameter(parameterIndex - baseCurve.getParameterCount());
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    if (parameterIndex < baseCurve.getParameterCount()) {
      return baseCurve.getParameterMetadata(parameterIndex);
    }
    return spreadCurve.getParameterMetadata(parameterIndex - baseCurve.getParameterCount());
  }

  @Override
  public CombinedCurve withParameter(int parameterIndex, double newValue) {
    if (parameterIndex < baseCurve.getParameterCount()) {
      return new CombinedCurve(
          baseCurve.withParameter(parameterIndex, newValue),
          spreadCurve,
          metadata);
    }
    return new CombinedCurve(
        baseCurve,
        spreadCurve.withParameter(parameterIndex - baseCurve.getParameterCount(), newValue),
        metadata);
  }

  @Override
  public CombinedCurve withPerturbation(ParameterPerturbation perturbation) {

    Curve newBaseCurve = baseCurve.withPerturbation(
        (idx, value, meta) -> perturbation.perturbParameter(
            idx,
            baseCurve.getParameter(idx),
            baseCurve.getParameterMetadata(idx)));
    int offset = baseCurve.getParameterCount();
    Curve newSpreadCurve = spreadCurve.withPerturbation(
        (idx, value, meta) -> perturbation.perturbParameter(
            idx + offset,
            spreadCurve.getParameter(idx),
            spreadCurve.getParameterMetadata(idx)));

    List<ParameterMetadata> newParamMeta = Stream.concat(
        IntStream.range(0, newBaseCurve.getParameterCount())
            .mapToObj(i -> newBaseCurve.getParameterMetadata(i)),
        IntStream.range(0, newSpreadCurve.getParameterCount())
            .mapToObj(i -> newSpreadCurve.getParameterMetadata(i)))
        .collect(toImmutableList());

    return CombinedCurve.of(
        newBaseCurve,
        newSpreadCurve,
        metadata.withParameterMetadata(newParamMeta));
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return baseCurve.yValue(x) + spreadCurve.yValue(x);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    UnitParameterSensitivity baseSens = baseCurve.yValueParameterSensitivity(x);
    UnitParameterSensitivity spreadSens = spreadCurve.yValueParameterSensitivity(x);
    return UnitParameterSensitivity.combine(getName(), baseSens, spreadSens);
  }

  @Override
  public double firstDerivative(double x) {
    return baseCurve.firstDerivative(x) + spreadCurve.firstDerivative(x);
  }

  //-------------------------------------------------------------------------
  @Override
  public UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    UnitParameterSensitivity baseSens = baseCurve.createParameterSensitivity(
        sensitivities.subArray(0, baseCurve.getParameterCount()));
    UnitParameterSensitivity spreadSens = spreadCurve.createParameterSensitivity(
        sensitivities.subArray(baseCurve.getParameterCount(), sensitivities.size()));
    return UnitParameterSensitivity.combine(getName(), baseSens, spreadSens);
  }

  @Override
  public CurrencyParameterSensitivity createParameterSensitivity(
      Currency currency,
      DoubleArray sensitivities) {
    CurrencyParameterSensitivity baseSensi = baseCurve.createParameterSensitivity(
        currency, sensitivities.subArray(0, baseCurve.getParameterCount()));
    CurrencyParameterSensitivity spreadSensi = spreadCurve.createParameterSensitivity(
        currency, sensitivities.subArray(baseCurve.getParameterCount(), sensitivities.size()));
    return CurrencyParameterSensitivity.combine(getName(), baseSensi, spreadSensi);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableList<Curve> split() {
    return ImmutableList.of(baseCurve, spreadCurve);
  }

  @Override
  public CombinedCurve withUnderlyingCurve(int curveIndex, Curve curve) {
    if (curveIndex == 0) {
      return new CombinedCurve(curve, spreadCurve, metadata);
    }
    if (curveIndex == 1) {
      return new CombinedCurve(baseCurve, curve, metadata);
    }
    throw new IllegalArgumentException("curveIndex is outside the range");
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code CombinedCurve}.
   * @return the meta-bean, not null
   */
  public static CombinedCurve.Meta meta() {
    return CombinedCurve.Meta.INSTANCE;
  }

  static {
    MetaBean.register(CombinedCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public CombinedCurve.Meta metaBean() {
    return CombinedCurve.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base curve.
   * @return the value of the property, not null
   */
  public Curve getBaseCurve() {
    return baseCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread curve.
   * @return the value of the property, not null
   */
  public Curve getSpreadCurve() {
    return spreadCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve metadata.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CombinedCurve other = (CombinedCurve) obj;
      return JodaBeanUtils.equal(baseCurve, other.baseCurve) &&
          JodaBeanUtils.equal(spreadCurve, other.spreadCurve) &&
          JodaBeanUtils.equal(metadata, other.metadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(baseCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(spreadCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("CombinedCurve{");
    buf.append("baseCurve").append('=').append(baseCurve).append(',').append(' ');
    buf.append("spreadCurve").append('=').append(spreadCurve).append(',').append(' ');
    buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CombinedCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code baseCurve} property.
     */
    private final MetaProperty<Curve> baseCurve = DirectMetaProperty.ofImmutable(
        this, "baseCurve", CombinedCurve.class, Curve.class);
    /**
     * The meta-property for the {@code spreadCurve} property.
     */
    private final MetaProperty<Curve> spreadCurve = DirectMetaProperty.ofImmutable(
        this, "spreadCurve", CombinedCurve.class, Curve.class);
    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CurveMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", CombinedCurve.class, CurveMetadata.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseCurve",
        "spreadCurve",
        "metadata");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1842240354:  // baseCurve
          return baseCurve;
        case 2130054972:  // spreadCurve
          return spreadCurve;
        case -450004177:  // metadata
          return metadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CombinedCurve> builder() {
      return new CombinedCurve.Builder();
    }

    @Override
    public Class<? extends CombinedCurve> beanType() {
      return CombinedCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code baseCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> baseCurve() {
      return baseCurve;
    }

    /**
     * The meta-property for the {@code spreadCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> spreadCurve() {
      return spreadCurve;
    }

    /**
     * The meta-property for the {@code metadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> metadata() {
      return metadata;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1842240354:  // baseCurve
          return ((CombinedCurve) bean).getBaseCurve();
        case 2130054972:  // spreadCurve
          return ((CombinedCurve) bean).getSpreadCurve();
        case -450004177:  // metadata
          return ((CombinedCurve) bean).getMetadata();
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
   * The bean-builder for {@code CombinedCurve}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CombinedCurve> {

    private Curve baseCurve;
    private Curve spreadCurve;
    private CurveMetadata metadata;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1842240354:  // baseCurve
          return baseCurve;
        case 2130054972:  // spreadCurve
          return spreadCurve;
        case -450004177:  // metadata
          return metadata;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1842240354:  // baseCurve
          this.baseCurve = (Curve) newValue;
          break;
        case 2130054972:  // spreadCurve
          this.spreadCurve = (Curve) newValue;
          break;
        case -450004177:  // metadata
          this.metadata = (CurveMetadata) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CombinedCurve build() {
      return new CombinedCurve(
          baseCurve,
          spreadCurve,
          metadata);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("CombinedCurve.Builder{");
      buf.append("baseCurve").append('=').append(JodaBeanUtils.toString(baseCurve)).append(',').append(' ');
      buf.append("spreadCurve").append('=').append(JodaBeanUtils.toString(spreadCurve)).append(',').append(' ');
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

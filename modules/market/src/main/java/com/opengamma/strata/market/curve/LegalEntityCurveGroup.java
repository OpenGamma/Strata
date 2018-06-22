/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * A group of repo curves and issuer curves.
 * <p>
 * This is used to hold a group of related curves, typically forming a logical set.
 * It is often used to hold the results of a curve calibration.
 * <p>
 * Curve groups can also be created from a set of existing curves.
 */
@BeanDefinition
public final class LegalEntityCurveGroup
    implements CurveGroup, ImmutableBean, Serializable {

  /**
   * The name of the curve group.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveGroupName name;
  /**
   * The repo curves in the curve group, keyed by repo group and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<RepoGroup, Currency>, Curve> repoCurves;
  /**
   * The issuer curves in the curve group, keyed by legal entity group and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve group containing the specified curves.
   *
   * @param name  the name of the curve group
   * @param repoCurves  the repo curves, keyed by pair of repo group and currency
   * @param issuerCurves  the issuer curves, keyed by pair of legal entity group and currency
   * @return a curve group containing the specified curves
   */
  public static LegalEntityCurveGroup of(
      CurveGroupName name,
      Map<Pair<RepoGroup, Currency>, Curve> repoCurves,
      Map<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves) {

    return new LegalEntityCurveGroup(name, repoCurves, issuerCurves);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the curve with the specified name.
   * <p>
   * If the curve cannot be found, empty is returned.
   * 
   * @param name  the curve name
   * @return the curve, empty if not found
   */
  @Override
  public Optional<Curve> findCurve(CurveName name) {
    return Stream.concat(repoCurves.values().stream(), issuerCurves.values().stream())
        .filter(c -> c.getName().equals(name))
        .findFirst();
  }

  /**
   * Finds the repo curve for the repo group and currency if there is one in the group.
   * <p>
   * If the curve is not found, optional empty is returned.
   * 
   * @param repoGroup  the repo group
   * @param currency  the currency
   * @return the repo curve for the repo group and currency if there is one in the group
   */
  public Optional<Curve> findRepoCurve(RepoGroup repoGroup, Currency currency) {
    return Optional.ofNullable(repoCurves.get(Pair.of(repoGroup, currency)));
  }

  /**
   * Finds the issuer curve for the legal entity group and currency if there is one in the group.
   * <p>
   * If the curve is not found, optional empty is returned. 
   * 
   * @param legalEntityGroup  the legal entity group
   * @param currency  the currency
   * @return the issuer curve for the legal entity group and currency if there is one in the group
   */
  public Optional<Curve> findIssuerCurve(LegalEntityGroup legalEntityGroup, Currency currency) {
    return Optional.ofNullable(issuerCurves.get(Pair.of(legalEntityGroup, currency)));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a stream of all curves in the group.
   *
   * @return Returns a stream of all curves in the group
   */
  @Override
  public Stream<Curve> stream() {
    return Stream.concat(repoCurves.values().stream(), issuerCurves.values().stream());
  }

  /**
   * Returns a stream of all repo curves in the group.
   *
   * @return Returns a stream of all repo curves in the group
   */
  public Stream<Curve> repoCurveStream() {
    return repoCurves.values().stream();
  }

  /**
   * Returns a stream of all issuer curves in the group.
   *
   * @return Returns a stream of all issuer curves in the group
   */
  public Stream<Curve> issuerCurveStream() {
    return issuerCurves.values().stream();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code LegalEntityCurveGroup}.
   * @return the meta-bean, not null
   */
  public static LegalEntityCurveGroup.Meta meta() {
    return LegalEntityCurveGroup.Meta.INSTANCE;
  }

  static {
    MetaBean.register(LegalEntityCurveGroup.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static LegalEntityCurveGroup.Builder builder() {
    return new LegalEntityCurveGroup.Builder();
  }

  private LegalEntityCurveGroup(
      CurveGroupName name,
      Map<Pair<RepoGroup, Currency>, Curve> repoCurves,
      Map<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(repoCurves, "repoCurves");
    JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
    this.name = name;
    this.repoCurves = ImmutableMap.copyOf(repoCurves);
    this.issuerCurves = ImmutableMap.copyOf(issuerCurves);
  }

  @Override
  public LegalEntityCurveGroup.Meta metaBean() {
    return LegalEntityCurveGroup.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the curve group.
   * @return the value of the property, not null
   */
  @Override
  public CurveGroupName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the repo curves in the curve group, keyed by repo group and currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Pair<RepoGroup, Currency>, Curve> getRepoCurves() {
    return repoCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the issuer curves in the curve group, keyed by legal entity group and currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Pair<LegalEntityGroup, Currency>, Curve> getIssuerCurves() {
    return issuerCurves;
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
      LegalEntityCurveGroup other = (LegalEntityCurveGroup) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(repoCurves, other.repoCurves) &&
          JodaBeanUtils.equal(issuerCurves, other.issuerCurves);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurves);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("LegalEntityCurveGroup{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("repoCurves").append('=').append(repoCurves).append(',').append(' ');
    buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LegalEntityCurveGroup}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<CurveGroupName> name = DirectMetaProperty.ofImmutable(
        this, "name", LegalEntityCurveGroup.class, CurveGroupName.class);
    /**
     * The meta-property for the {@code repoCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<RepoGroup, Currency>, Curve>> repoCurves = DirectMetaProperty.ofImmutable(
        this, "repoCurves", LegalEntityCurveGroup.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code issuerCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<LegalEntityGroup, Currency>, Curve>> issuerCurves = DirectMetaProperty.ofImmutable(
        this, "issuerCurves", LegalEntityCurveGroup.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "repoCurves",
        "issuerCurves");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 587630454:  // repoCurves
          return repoCurves;
        case -1909076611:  // issuerCurves
          return issuerCurves;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public LegalEntityCurveGroup.Builder builder() {
      return new LegalEntityCurveGroup.Builder();
    }

    @Override
    public Class<? extends LegalEntityCurveGroup> beanType() {
      return LegalEntityCurveGroup.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveGroupName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code repoCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<RepoGroup, Currency>, Curve>> repoCurves() {
      return repoCurves;
    }

    /**
     * The meta-property for the {@code issuerCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<LegalEntityGroup, Currency>, Curve>> issuerCurves() {
      return issuerCurves;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((LegalEntityCurveGroup) bean).getName();
        case 587630454:  // repoCurves
          return ((LegalEntityCurveGroup) bean).getRepoCurves();
        case -1909076611:  // issuerCurves
          return ((LegalEntityCurveGroup) bean).getIssuerCurves();
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
   * The bean-builder for {@code LegalEntityCurveGroup}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<LegalEntityCurveGroup> {

    private CurveGroupName name;
    private Map<Pair<RepoGroup, Currency>, Curve> repoCurves = ImmutableMap.of();
    private Map<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(LegalEntityCurveGroup beanToCopy) {
      this.name = beanToCopy.getName();
      this.repoCurves = beanToCopy.getRepoCurves();
      this.issuerCurves = beanToCopy.getIssuerCurves();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 587630454:  // repoCurves
          return repoCurves;
        case -1909076611:  // issuerCurves
          return issuerCurves;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveGroupName) newValue;
          break;
        case 587630454:  // repoCurves
          this.repoCurves = (Map<Pair<RepoGroup, Currency>, Curve>) newValue;
          break;
        case -1909076611:  // issuerCurves
          this.issuerCurves = (Map<Pair<LegalEntityGroup, Currency>, Curve>) newValue;
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
    public LegalEntityCurveGroup build() {
      return new LegalEntityCurveGroup(
          name,
          repoCurves,
          issuerCurves);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name of the curve group.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(CurveGroupName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the repo curves in the curve group, keyed by repo group and currency.
     * @param repoCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurves(Map<Pair<RepoGroup, Currency>, Curve> repoCurves) {
      JodaBeanUtils.notNull(repoCurves, "repoCurves");
      this.repoCurves = repoCurves;
      return this;
    }

    /**
     * Sets the issuer curves in the curve group, keyed by legal entity group and currency.
     * @param issuerCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurves(Map<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves) {
      JodaBeanUtils.notNull(issuerCurves, "issuerCurves");
      this.issuerCurves = issuerCurves;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("LegalEntityCurveGroup.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("repoCurves").append('=').append(JodaBeanUtils.toString(repoCurves)).append(',').append(' ');
      buf.append("issuerCurves").append('=').append(JodaBeanUtils.toString(issuerCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

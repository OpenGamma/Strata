/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A byte source implementation that obtains data from a URI.
 * <p>
 * This implementation differs from Guava in that it is is a Joda-Bean.
 * In addition, {@link #read()} throws {@link UncheckedIOException} instead of {@link IOException}.
 */
public final class UriByteSource extends BeanByteSource implements ImmutableBean, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    MetaBean.register(Meta.META);
  }

  /**
   * The underlying URI.
   */
  private final URI uri;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance based on the underlying URI.
   * 
   * @param uri  the URI
   * @return the byte source
   */
  public static UriByteSource of(URI uri) {
    return new UriByteSource(uri);
  }

  /**
   * Creates an instance based on the underlying URL.
   * 
   * @param url  the URL
   * @return the byte source
   */
  public static UriByteSource of(URL url) {
    try {
      return new UriByteSource(url.toURI());
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param uri  the URI
   */
  private UriByteSource(URI uri) {
    this.uri = ArgChecker.notNull(uri, "uri");
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  /**
   * Gets the URI.
   * 
   * @return the URI
   */
  public URI getUri() {
    return uri;
  }

  //-------------------------------------------------------------------------
  @Override
  public InputStream openStream() throws IOException {
    try {
      return uri.toURL().openStream();
    } catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object other) {
    if (other instanceof UriByteSource) {
      return uri.equals(((UriByteSource) other).uri);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public String toString() {
    return "UriByteSource[" + uri + "]";
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<URI> PROP_URI = new BasicMetaProperty<URI>("uri") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return UriByteSource.class;
      }

      @Override
      public Class<URI> propertyType() {
        return URI.class;
      }

      @Override
      public Type propertyGenericType() {
        return URI.class;
      }

      @Override
      public PropertyStyle style() {
        return PropertyStyle.IMMUTABLE;
      }

      @Override
      public List<Annotation> annotations() {
        return ImmutableList.of();
      }

      @Override
      public URI get(Bean bean) {
        return ((UriByteSource) bean).uri;
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP = ImmutableMap.of("uri", PROP_URI);

    private Meta() {
    }

    @Override
    public boolean isBuildable() {
      return true;
    }

    @Override
    public BeanBuilder<UriByteSource> builder() {
      return new BasicImmutableBeanBuilder<UriByteSource>(this) {
        private URI uri;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(PROP_URI.name())) {
            return uri;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<UriByteSource> set(String propertyName, Object value) {
          if (propertyName.equals(PROP_URI.name())) {
            this.uri = ((URI) ArgChecker.notNull(value, "value"));
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public UriByteSource build() {
          return new UriByteSource(uri);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return UriByteSource.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}

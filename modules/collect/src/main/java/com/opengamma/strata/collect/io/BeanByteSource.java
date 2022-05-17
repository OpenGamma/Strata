/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A byte source implementation that is also a Joda-Bean.
 * <p>
 * See {@link ArrayByteSource}, {@link UriByteSource} and {@link FileByteSource}.
 */
public abstract class BeanByteSource extends ByteSource implements ImmutableBean {

  /**
   * Creates an instance.
   */
  protected BeanByteSource() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the file name of the source.
   * <p>
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   * 
   * @return the file name, empty if not known
   */
  public Optional<String> getFileName() {
    return Optional.empty();
  }

  /**
   * Gets the file name of the source.
   * <p>
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   *
   * @return the file name
   * @throws IllegalArgumentException if the file name is not known
   */
  public String getFileNameOrThrow() {
    return getFileName().orElseThrow(() -> new IllegalArgumentException("No file name present on byte source"));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the byte source is empty, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public boolean isEmpty() {
    try {
      return super.isEmpty();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Gets the size of the byte source, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public long size() {
    try {
      return super.size();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Reads the source as a byte array, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @return the byte array
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public byte[] read() {
    try {
      return super.read();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Reads the source, converting to UTF-8.
   * 
   * @return the UTF-8 string
   * @throws UncheckedIOException if an IO error occurs
   */
  public String readUtf8() {
    return new String(read(), StandardCharsets.UTF_8);
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the UTF-8 string
   * @throws UncheckedIOException if an IO error occurs
   */
  public String readUtf8UsingBom() {
    return UnicodeBom.toString(read());
  }

  @Override
  public BeanCharSource asCharSource(Charset charset) {
    // no need to bridge, as javac is already doing that
    return new AsBeanCharSource(this, charset);
  }

  /**
   * Returns a {@code CharSource} for the same bytes, converted to UTF-8.
   * 
   * @return the equivalent {@code CharSource}
   */
  public BeanCharSource asCharSourceUtf8() {
    // bridged below for backwards compatibility
    return asCharSource(StandardCharsets.UTF_8);
  }

  /**
   * @hidden
   * @return the source
   */
  public CharSource asCharSourceUtf8$$bridge() { // CSIGNORE
    return asCharSourceUtf8();
  }

  /**
   * Returns a {@code CharSource} for the File, converted to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the equivalent {@code CharSource}
   */
  public BeanCharSource asCharSourceUtf8UsingBom() {
    // bridged below for backwards compatibility
    return UnicodeBom.toCharSource(this);
  }

  /**
   * @hidden
   * @return the source
   */
  public CharSource asCharSourceUtf8UsingBom$$bridge() { // CSIGNORE
    return UnicodeBom.toCharSource((ByteSource) this);
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the content of the byte source into memory.
   * 
   * @return the byte array
   * @throws UncheckedIOException if an IO error occurs
   */
  public ArrayByteSource load() {
    return ArrayByteSource.from(this);
  }

  //-------------------------------------------------------------------------
  @Override
  public HashCode hash(HashFunction hashFunction) {
    try {
      return super.hash(hashFunction);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Returns a new byte source containing the hash of the content of this byte source.
   * <p>
   * The returned hash is in byte form.
   * 
   * @param hashFunction  the hash function to use, see {@link Hashing}
   * @return the new byte source representing the hash
   * @throws UncheckedIOException if an IO error occurs
   */
  public ArrayByteSource toHash(HashFunction hashFunction) {
    return ArrayByteSource.ofUnsafe(hash(hashFunction).asBytes());
  }

  /**
   * Returns a new byte source containing the hash of the content of this byte source.
   * <p>
   * The returned hash is in string form.
   * This form is intended to be compatible with tools like the UNIX {@code md5sum} command.
   * 
   * @param hashFunction  the hash function to use, see {@link Hashing}
   * @return the new byte source representing the hash
   * @throws UncheckedIOException if an IO error occurs
   */
  public String toHashString(HashFunction hashFunction) {
    return hash(hashFunction).toString();
  }

  /**
   * Encodes the byte source using base-64.
   * 
   * @return the base-64 encoded form
   * @throws UncheckedIOException if an IO error occurs
   */
  public ArrayByteSource toBase64() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (InputStream in = openBufferedStream();
        OutputStream out = Base64.getEncoder().wrap(baos)) {
      ByteStreams.copy(in, out);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return ArrayByteSource.ofUnsafe(baos.toByteArray());
  }

  /**
   * Encodes the byte source using base-64, returning a string.
   * <p>
   * Equivalent to {@code toBase64().readUtf8()}.
   * 
   * @return the base-64 encoded string
   */
  public String toBase64String() {
    return toBase64().readUtf8();
  }

  //-------------------------------------------------------------------------
  // a char source that decorates this byte source
  static class AsBeanCharSource extends BeanCharSource {

    static {
      MetaBean.register(AsBeanCharSource.Meta.META);
    }

    private final BeanByteSource underlying;
    private final Charset charset;

    AsBeanCharSource(BeanByteSource underlying, Charset charset) {
      this.underlying = ArgChecker.notNull(underlying, "underlying");
      this.charset = ArgChecker.notNull(charset, "charset");
    }

    @Override
    public MetaBean metaBean() {
      return Meta.META;
    }

    @Override
    public Optional<String> getFileName() {
      return underlying.getFileName();
    }

    @Override
    public BeanByteSource asByteSource(Charset charset) {
      if (charset.equals(this.charset)) {
        return underlying;
      }
      return super.asByteSource(charset);
    }

    @Override
    public Reader openStream() throws IOException {
      return new InputStreamReader(underlying.openStream(), charset);
    }

    @Override
    public String read() {
      return new String(underlying.read(), charset);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj != null && obj.getClass() == this.getClass()) {
        AsBeanCharSource other = (AsBeanCharSource) obj;
        return JodaBeanUtils.equal(underlying, other.underlying) &&
            JodaBeanUtils.equal(charset, other.charset);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int hash = getClass().hashCode();
      hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
      hash = hash * 31 + JodaBeanUtils.hashCode(charset);
      return hash;
    }

    @Override
    public String toString() {
      return underlying.toString() + ".asCharSource(" + charset + ")";
    }

    //-------------------------------------------------------------------------
    /**
     * Meta bean.
     */
    static final class Meta extends BasicMetaBean {

      private static final MetaBean META = new Meta();
      private static final MetaProperty<BeanByteSource> UNDERLYING = new BasicMetaProperty<BeanByteSource>("underlying") {

        @Override
        public MetaBean metaBean() {
          return META;
        }

        @Override
        public Class<?> declaringType() {
          return AsBeanCharSource.class;
        }

        @Override
        public Class<BeanByteSource> propertyType() {
          return BeanByteSource.class;
        }

        @Override
        public Type propertyGenericType() {
          return BeanByteSource.class;
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
        public BeanByteSource get(Bean bean) {
          return ((AsBeanCharSource) bean).underlying;
        }

        @Override
        public void set(Bean bean, Object value) {
          throw new UnsupportedOperationException("Property cannot be written: " + name());
        }
      };
      private static final MetaProperty<String> CHARSET = new BasicMetaProperty<String>("charset") {

        @Override
        public MetaBean metaBean() {
          return META;
        }

        @Override
        public Class<?> declaringType() {
          return AsBeanCharSource.class;
        }

        @Override
        public Class<String> propertyType() {
          return String.class;
        }

        @Override
        public Type propertyGenericType() {
          return String.class;
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
        public String get(Bean bean) {
          return ((AsBeanCharSource) bean).charset.name();
        }

        @Override
        public void set(Bean bean, Object value) {
          throw new UnsupportedOperationException("Property cannot be written: " + name());
        }
      };
      private static final ImmutableMap<String, MetaProperty<?>> MAP =
          ImmutableMap.of("underlying", UNDERLYING, "charset", CHARSET);

      private Meta() {
      }

      @Override
      public boolean isBuildable() {
        return true;
      }

      @Override
      public BeanBuilder<AsBeanCharSource> builder() {
        return new BasicImmutableBeanBuilder<AsBeanCharSource>(this) {
          private BeanByteSource underlying;
          private Charset charset;

          @Override
          public Object get(String propertyName) {
            if (propertyName.equals(UNDERLYING.name())) {
              return underlying;
            } else if (propertyName.equals(CHARSET.name())) {
              return charset.name();
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName);
            }
          }

          @Override
          public BeanBuilder<AsBeanCharSource> set(String propertyName, Object value) {
            if (propertyName.equals(UNDERLYING.name())) {
              this.underlying = (BeanByteSource) ArgChecker.notNull(value, "underlying");
            } else if (propertyName.equals(CHARSET.name())) {
              this.charset = Charset.forName((String) ArgChecker.notNull(value, "charset"));
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
          }

          @Override
          public AsBeanCharSource build() {
            ArgChecker.notNull(underlying, "underlying");
            ArgChecker.notNull(charset, "charset");
            return new AsBeanCharSource(underlying, charset);
          }
        };
      }

      @Override
      public Class<? extends Bean> beanType() {
        return AsBeanCharSource.class;
      }

      @Override
      public Map<String, MetaProperty<?>> metaPropertyMap() {
        return MAP;
      }
    }
  }
}

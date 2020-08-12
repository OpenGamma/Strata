/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Utility class to simplify accessing and creating zip files, and other packed formats.
 */
public final class ZipUtils {
  // need to watch out for ZIP slip attack when unzipping to file system
  // https://github.com/snyk/zip-slip-vulnerability
  private static final Path DUMMY_PATH = Paths.get("/dummy/");

  private ZipUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source returning the file names that are contained.
   * <p>
   * The result is a set of relative path names within the zip.
   * Only files are returned, not folders.
   * 
   * @param source  the byte source to unzip
   * @return the set of relative path names
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Set<String> unzipPathNames(BeanByteSource source) {
    ImmutableSet.Builder<String> entryNames = ImmutableSet.builder();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        validateZipPathName(DUMMY_PATH, entry);
        if (!entry.isDirectory()) {
          entryNames.add(entry.getName());
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return entryNames.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips a single file from the source in memory.
   * <p>
   * Callers can use {@link #unzipPathNames(BeanByteSource)} to find the available names.
   * The relative path name must match exactly that in the zip.
   * 
   * @param source  the byte source to unzip
   * @param relativePathName  the exact relative path name that the file is stored as
   * @return the unzipped file, empty if not found
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Optional<ArrayByteSource> unzipPathNameInMemory(BeanByteSource source, String relativePathName) {
    ArgChecker.notNull(relativePathName, "relativePathName");
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        validateZipPathName(DUMMY_PATH, entry);
        if (!entry.isDirectory() && entry.getName().equals(relativePathName)) {
          ArrayByteSource extractedBytes = extractInputStream(in, entry.getName());
          return Optional.of(extractedBytes);
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source to a file path.
   * <p>
   * Empty folders in the zip will not be created.
   * 
   * @param source  the byte source to unzip
   * @param path  the path to unzip to
   * @throws UncheckedIOException if an IO error occurs
   * @throws SecurityException if the path is not absolute and the calling thread cannot access system property "user.dir"
   */
  public static void unzip(BeanByteSource source, Path path) {
    Path absolutePath = path.toAbsolutePath();
    Set<ZipKey> deduplicate = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        Path resolved = validateZipPathName(absolutePath, entry);
        if (!entry.isDirectory()) {
          if (deduplicate.add(new ZipKey(entry, resolved))) {
            Files.createDirectories(resolved);
            Files.copy(in, resolved, StandardCopyOption.REPLACE_EXISTING);
          }
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a zip file from the list of files in memory.
   * <p>
   * Each source must have a file name.
   * The output will not contain subfolders.
   * 
   * @param sources  the byte sources to zip
   * @return the zip file
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource zipInMemory(List<? extends BeanByteSource> sources) {
    long instantMillis = System.currentTimeMillis();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ZipOutputStream out = new ZipOutputStream(baos)) {
        for (BeanByteSource source : sources) {
          ZipEntry entry = new ZipEntry(
              source.getFileName()
                  .orElseThrow(() -> new IllegalArgumentException("ByteSource must have a name in order to be zipped")));
          entry.setTime(instantMillis);
          out.putNextEntry(entry);
          source.copyTo(out);
          out.closeEntry();
        }
      }
      return ArrayByteSource.ofUnsafe(baos.toByteArray());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, returning a map.
   * <p>
   * This method loads the whole unpacked file into memory.
   * Where possible, {@link #unpackInMemory(BeanByteSource, BiConsumer)} should be preferred
   * as it only loads files one at a time.
   * <p>
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * 
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Map<String, ArrayByteSource> unpackInMemory(BeanByteSource source) {
    ImmutableMap.Builder<String, ArrayByteSource> builder = ImmutableMap.builder();
    unpackInMemory(source, builder::put);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, invoking the consumer for each entry.
   * <p>
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * This method is not recursive.
   * 
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws UncheckedIOException if an IO error occurs
   */
  public static void unpackInMemory(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    String fileName = source.getFileName().orElse("");
    if (suffixMatches(fileName, ".zip")) {
      unzipInMemory(source, consumer);

    } else if (suffixMatches(fileName, ".gz")) {
      ungzInMemory(source, fileName, consumer);

    } else if (suffixMatches(fileName, ".base64")) {
      try (InputStream in = Base64.getDecoder().wrap(source.openBufferedStream())) {
        String shortFileName = fileName.substring(0, fileName.length() - 7);
        ArrayByteSource extracted = extractInputStream(in, shortFileName);
        consumer.accept(shortFileName, extracted);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }

    } else {
      consumer.accept(fileName, source.load());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, returning a map.
   * <p>
   * This method loads the whole unpacked file into memory.
   * Where possible, {@link #unzipInMemory(BeanByteSource, BiConsumer)} should be preferred
   * as it only loads files one at a time.
   * <p>
   * Unlike {@link #unpackInMemory(BeanByteSource)}, this method always treats the input as a zip file.
   * 
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Map<String, ArrayByteSource> unzipInMemory(BeanByteSource source) {
    ImmutableMap.Builder<String, ArrayByteSource> builder = ImmutableMap.builder();
    unzipInMemory(source, builder::put);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, invoking the consumer for each entry.
   * <p>
   * Unlike {@link #unpackInMemory(BeanByteSource, BiConsumer)}, this method always treats the input as a zip file.
   * 
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws UncheckedIOException if an IO error occurs
   */
  public static void unzipInMemory(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    Set<ZipKey> deduplicate = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        Path resolved = validateZipPathName(DUMMY_PATH, entry);
        if (!entry.isDirectory()) {
          if (deduplicate.add(new ZipKey(entry, resolved))) {
            ArrayByteSource extractedBytes = extractInputStream(in, entry.getName());
            consumer.accept(entry.getName(), extractedBytes);
          }
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Provides a new source that decrypts the specified source ZIP.
   * <p>
   * This returns a wrapper around the input source that provides decryption.
   * The result is normally passed directly into one of the other methods on this class.
   * 
   * @param source  the byte source to unpack
   * @param password  the password to decrypt the input
   * @return the decrypted zip file
   */
  public static BeanByteSource decryptZip(BeanByteSource source, String password) {
    return new ZipDecryptByteSource(source, password);
  }

  //-------------------------------------------------------------------------
  // ungz the contents
  private static void ungzInMemory(BeanByteSource source, String fileName, BiConsumer<String, ArrayByteSource> consumer) {
    try (GZIPInputStream in = new GZIPInputStream(source.openStream())) {
      String shortFileName = fileName.substring(0, fileName.length() - 3);
      ArrayByteSource extractedBytes = extractInputStream(in, shortFileName);
      consumer.accept(shortFileName, extractedBytes);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // safely extract the suffix
  private static boolean suffixMatches(String name, String suffix) {
    return name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length());
  }

  // unzip the input, trying to recover from large files or ZIP bombs
  private static ArrayByteSource extractInputStream(InputStream in, String fileName) throws IOException {
    try {
      return ArrayByteSource.from(in).withFileName(fileName);
    } catch (OutOfMemoryError ex) {
      System.gc();
      throw new IOException("Unzipped input too large: " + fileName);
    }
  }

  // prevent ZIP slip attack
  private static Path validateZipPathName(Path rootPath, ZipEntry entry) throws ZipException {
    Path normalizedRootPath = rootPath.normalize();
    Path resolved = normalizedRootPath.resolve(entry.getName()).normalize();
    if (!resolved.startsWith(normalizedRootPath)) {
      throw new ZipException("ZIP file contains illegal file name: " + entry.getName());
    }
    return resolved;
  }

  //-------------------------------------------------------------------------
  // handle duplicate entries in a zip file
  // while such a zip file is stupid, it is apparently valid
  private static class ZipKey {
    private final Path resolvedPath;
    private final long crc;
    private final long size;

    private ZipKey(ZipEntry entry, Path resolvedPath) {
      this.resolvedPath = resolvedPath;
      this.crc = entry.getCrc();
      this.size = entry.getSize();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ZipKey) {
        ZipKey key = (ZipKey) obj;
        return resolvedPath.equals(key.resolvedPath) &&
            crc == key.crc &&
            size == key.size;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  //-------------------------------------------------------------------------
  // byte source to wrap the underlying source
  private static final class ZipDecryptByteSource extends BeanByteSource implements ImmutableBean {

    @PropertyDefinition
    private final BeanByteSource underlying;
    private final char[] password;

    //-------------------------------------------------------------------------
    private ZipDecryptByteSource(BeanByteSource underlying) {
      throw new IllegalStateException("ZipDecryptByteSource cannot be deserialized as it contains a password");
    }

    private ZipDecryptByteSource(BeanByteSource underlying, String password) {
      this.underlying = ArgChecker.notNull(underlying, "underlying");
      this.password = ArgChecker.notNull(password, "password").toCharArray();
    }

    @Override
    public Optional<String> getFileName() {
      return underlying.getFileName();
    }

    @Override
    public InputStream openStream() throws IOException {
      return new ZipDecryptInputStream(underlying.openStream(), password);
    }

    // originally Joda-Bean generated
    //-----------------------------------------------------------------------
    private static final TypedMetaBean<ZipDecryptByteSource> META_BEAN =
        LightMetaBean.of(ZipDecryptByteSource.class, MethodHandles.lookup(), new String[] {"underlying"}, new Object[0]);
    static {
      MetaBean.register(META_BEAN);
    }

    /**
     * The meta-bean for {@code ZipDecryptByteSource}.
     * @return the meta-bean, not null
     */
    @SuppressWarnings("unused")  // method used by Joda-Beans
    public static TypedMetaBean<ZipDecryptByteSource> meta() {
      return META_BEAN;
    }

    @Override
    public TypedMetaBean<ZipDecryptByteSource> metaBean() {
      return META_BEAN;
    }

    /**
     * Gets the underlying.
     * @return the value of the property, not null
     */
    @SuppressWarnings("unused")  // method used by Joda-Beans
    public BeanByteSource getUnderlying() {
      return underlying;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj != null && obj.getClass() == this.getClass()) {
        ZipDecryptByteSource other = (ZipDecryptByteSource) obj;
        return JodaBeanUtils.equal(underlying, other.underlying);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int hash = getClass().hashCode();
      hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
      return hash;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ZipDecryptByteSource{");
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
      buf.append('}');
      return buf.toString();
    }
  }
}

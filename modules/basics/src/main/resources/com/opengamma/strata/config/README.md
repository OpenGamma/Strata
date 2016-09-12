Resource configuration system
-----------------------------

The resource configuration system is used to load and merge files from the classpath.
The standard location used by the configuration system is `com/opengamma/strata/config`.
See the class `ResourceConfig` for more information.

Two subdirectories are recognized by default:

* `base` - the directory of configuration supplied by the Strata project
* `application` - the directory of configuration supplied by applications

For example, a typical setup might have these two files:

* `com/opengamma/strata/config/base/Foo.ini`
* `com/opengamma/strata/config/application/Foo.ini`

When the resource `Foo.ini` is requested from `ResourceConfig`, the two files are loaded and merged.
Sections and properties from the `application` directory take precedence.

The system property `com.opengamma.strata.config.directories` can be used to change the set of
directories that are loaded. For example, it could be changed to `base,override,application,user`,
which would allow up to 4 files to be loaded and merged.

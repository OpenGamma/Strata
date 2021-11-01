Strata
======

[![Build Status](https://travis-ci.org/OpenGamma/Strata.svg?branch=master)](https://travis-ci.org/OpenGamma/Strata) [![License](http://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

This repository contains the source code of Strata, the open source analytics and market risk library from OpenGamma.

Strata is released as Open Source Software under the
[Apache v2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html). 
Commercial support is [available](https://opengamma.com/) from the authors.

[![OpenGamma](https://s3-eu-west-1.amazonaws.com/og-public-downloads/og-logo-alpha.png "OpenGamma")](https://opengamma.com/)


Using Strata
------------

Documentation for Strata can be found at https://strata.opengamma.io.

To use Strata Java SE 8u40 or later is required.
The JAR files are available in [Maven Central](https://search.maven.org/search?q=g:com.opengamma.strata):

```
<dependency>
  <groupId>com.opengamma.strata</groupId>
  <artifactId>strata-measure</artifactId>
  <version>2.10.0</version>
</dependency>
```

The JAR files, along with the command line tool and examples, can also be obtained from
the [Strata Releases](https://github.com/OpenGamma/Strata/releases) page on GitHub.


Building Strata
---------------

The source code can be cloned using [git](http://git-scm.com/) from GitHub:

```
  git clone https://github.com/OpenGamma/Strata.git
```

The projects use [Apache Maven](https://maven.apache.org/) as the build system.
Version 3.5.0 or later is required.
Simply run this command to compile and install the source code locally:

```
  mvn install
```

Strata is based on Java SE 8.
Our continuous integration regularly builds on both Java 8 and Java 11.
When using Java 8, version 8u40 or later is required due to bugs in earlier versions.
We do not recommend use of non-LTS releases, such as Java 9, 10 and 12 to 16.

The Strata examples project includes a GUI based on JavaFX.
On Java 8, this will be excluded from compilation if JavaFX is not available in the JDK.
On Java 11, OpenJFX is included as a jar file from Maven Central, so the GUI is always compiled.

We recommend builds of OpenJDK from providers other than Oracle, notably
[Amazon Corretto](https://aws.amazon.com/corretto/) and [AdoptOpenJDK](https://adoptopenjdk.net/).

For more information about developing code on Strata
see the [documentation](https://strata.opengamma.io).


Status
------

Strata is well-maintained, tested and functional.
It is used in production as the core of [OpenGamma SaaS Analytics](https://opengamma.com/).
The API will be maintained with backwards compatibility in mind.


Strata modules
--------------

Strata is formed from a number of modules:

* [Examples](examples/README.md)
* [Report](modules/report/README.md)
* [Measure](modules/measure/README.md)
* [Calc](modules/calc/README.md)
* [Loader](modules/loader/README.md)
* [Pricer](modules/pricer/README.md)
* [Market](modules/market/README.md)
* [Product](modules/product/README.md)
* [Data](modules/data/README.md)
* [Basics](modules/basics/README.md)
* [Collect](modules/collect/README.md)

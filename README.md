Strata
======

[![Build Status](https://travis-ci.org/OpenGamma/Strata.svg?branch=master)](https://travis-ci.org/OpenGamma/Strata)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

This repository contains the source code of Strata, OpenGamma's next-generation, open source toolkit for market risk.

Strata is released as Open Source Software under the
[Apache v2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html). 

[![OpenGamma](http://developers.opengamma.com/res/display/default/chrome/masthead_logo.png "OpenGamma")](http://www.opengamma.com)


Documentation
-------------

Documentation for Strata can be found at http://opengamma.github.io/StrataDocs.


Building Strata
---------------

The source code can be cloned using [git](http://git-scm.com/) from GitHub:
```
  git clone https://github.com/OpenGamma/Strata.git
```

The projects use [Apache Maven](http://maven.apache.org/) as the build system.
Version 3.2.0 or later is required.
Simply run this command to compile and install the source code locally:

```
  mvn install
```

Note that Strata is based on Java SE 8.
Version 8u40 or later is required to compile the code.

For more information about developing code on Strata
see the [documentation](http://opengamma.github.io/StrataDocs).


Status
------

Strata is well-maintained, tested, functional, and is being used in production.
We are currently making the final enhancements to Strata's APIs and functionality in preparation for version 1.0, expected in June 2016.
Until then, please be aware that breaking changes to classes and methods are possible at any time.


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

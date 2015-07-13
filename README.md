OpenGamma Strata
================
This repository contains the source code of OpenGamma Strata.
See the [developers website](http://developers.opengamma.com) for more details.

OpenGamma Strata is released as Open Source Software using the
[Apache v2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).  
Commercial support is [available](http://www.opengamma.com/) from the authors.

[![OpenGamma](http://developers.opengamma.com/res/display/default/chrome/masthead_logo.png "OpenGamma")](http://developers.opengamma.com)


Building OpenGamma Strata
-------------------------
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

For more information about developing code on OpenGamma Strata
see the [developer website](http://developers.opengamma.com).


Strata modules
--------------

Strata is formed from a number of modules:

* [Examples](examples/README.md)
* [Report](modules/report-beta/README.md)
* [Function](modules/function/README.md)
* [Engine](modules/engine/README.md)
* [Pricer](modules/pricer/README.md)
* [Market](modules/market/README.md)
* [Finance](modules/finance/README.md)
* [Basics](modules/basics/README.md)
* [Collect](modules/collect/README.md)

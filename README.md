<p align="right">
English description | <a href="README_RU.md">Описание на русском</a>
</p>

# Datajack
[![Build Status](https://travis-ci.org/sbtqa/datajack.svg?branch=master)](https://travis-ci.org/sbtqa/datajack) [![GitHub release](https://img.shields.io/github/release/sbtqa/datajack.svg?style=flat-square)](https://github.com/sbtqa/datajack/releases) [![Maven Central](https://img.shields.io/maven-central/v/ru.sbtqa.tag.datajack/datajack-parent.svg)](https://search.maven.org/search?q=g:ru.sbtqa.tag.datajack%20AND%20a:datajack-parent&core=gav)

DataJack - opensource java framework which help you work with you test data.

### About
DataJack have two important think:
* **Stash** - use this class, if you want store something when tests execute.
* **TestDataProvider** - interface, which using to work with different test data storage systems.   
We have the following implementations:
  * [JSON-Provider](https://github.com/sbtqa/datajack/tree/master/providers/json-provider)
  * [Properties-Provider](https://github.com/sbtqa/datajack/tree/master/providers/properties-provider)
  * [Mongo-Provider](https://github.com/sbtqa/datajack/tree/master/providers/mongo-provider)
  * [Excel-Provider](https://github.com/sbtqa/datajack/tree/master/providers/excel-provider)


### Documentation
Example how to use [here](https://github.com/sbtqa/datajack-example) and [here](https://github.com/sbtqa/datajack/tree/master/providers/json-provider/src/test).

### Contact
If you found error or you have a question? [Check](https://github.com/sbtqa/datajack/issues), maybe someone asked before, not found? Just [create a new issue](https://github.com/sbtqa/datajack/issues/new)!

### License 
DataJack is released under the Apache 2.0. [Details here](https://github.com/sbtqa/datajack/blob/master/LICENSE).


ixa-pipe-convert
=================

[![Build Status](https://travis-ci.org/ragerri/ixa-pipe-convert.svg?branch=master)](https://travis-ci.org/ragerri/ixa-pipe-convert)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/apache/opennlp/master/LICENSE)

This module implements several scripts to do corpus format conversions usually required to train
or to evaluate IXA pipes models (http://ixa2.si.ehu.es/ixa-pipes).

## TABLE OF CONTENTS

1. [Aspect-Based Sentiment Analysis (ABSA) conversions](#absa)
2. [Cluster Lexicon conversions](#clusters)
3. [NAF to CoNLL conversions](#naf)
4. [Markyt Formats conversions](#markyt)
5. [Installation](#installation)

## ABSA

There are several parameters to convert from and to the format used in the SemEval ABSA 2014-2016 shared tasks:

+ absa2015ToWFs: It converts ABSA SemEval 2014-2016 corpus to a tokenized NAF document. This function is used to obtain the evaluation set in the NAF format so that it can be annotated with ixa-pipe-opinion models for testing with the ABSA evaluation script.
+ nafToAbsa2014: The opinion-annotated NAF is converted to ABSA 2014 format for evaluation with the task official evaluation script.
+ nafToAbsa2015: The opinion-annotated NAF is converted to ABSA 2015 and 2016 formats for evaluation with the task official evaluation scripts.
+ absa2014ToCoNLL2002: It converts the ABSA 2014 corpus into CoNLL 2002 format for training ixa-pipe-opinion models.
+ absa2015ToCoNLL2002: It converts the ABSA 2015 and 2016 corpus into CoNLL 2002 format for training ixa-pipe-opinion models.
+ yelpGetText: It extracts the text element from the json formatted YELP dataset.

## Clusters

There are several parameters to pre-process cluster lexicons obtained following the methods described in (https://github.com/ragerri/cluster-preprocessing)

+ brownClean: It removes paragraph from a corpus if 90% of characters are not lowercase. Argument can be a file or a directory.
+ serializeBrownCluster: It serializes Brown cluster lexicons to be used to train models with (https://github.com/ixa-ehu/ixa-pipe-ml). Argument can be a file or a directory.
+ serializeClarkCluster: It serializes Clark and Word2vec cluster lexicons to be used to train models with  (https://github.com/ixa-ehu/ixa-pipe-ml). Argument can be a file or a directory.
+ serializeEntityDictionary: It serializes (https://github.com/ixa-ehu/ixa-pipe-nerc) entity dictionaries for training or tagging.
+ serializeLemmaDictionary: It serializes (https://github.com/ixa-ehu/ixa-pipe-pos) lemma dictionaries.

## NAF

+ nafToCoNLL02: It converts NAF containing named entities layer (entities) into CoNLL 2002 format.
+ nafToCoNLL03: It converts NAF containing named entities layer (entities) into CoNLL 2003 format.

## MARKYT

+ barrToWFs: It converts markyt BARR 2017 corpus to a tokenized NAF document. This function is used to obtain the evaluation set in the NAF format so that it can be annotated with ixa-pipe-nerc models for testing.
+ nafToBARR: The entity-annotated NAF is converted to BARR format for evaluation with the task official evaluation scripts.
+ barrToCoNLL2002: It converts the BARR corpus into CoNLL 2002 format for training ixa-pipe-ml sequence models.

## INSTALLATION

### Install MAVEN

Download MAVEN 3 from

````shell
wget http://www.apache.org/dyn/closer.cgi/maven/maven-3/3.0.4/binaries/apache-maven-3.0.4-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.0.4
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.4
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

### Get module source code

````shell
git clone https://github.com/ragerri/ixa-pipe-convert.git
cd ixa-pipe-convert
mvn clean package
````

# Usage

````shell
java -jar target/ixa-pipe-convert-$version-exec.jar -help
````

# GENERATING JAVADOC

You can also generate the javadoc of the module by executing:

````shell
mvn javadoc:jar
````

### Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````

/*
 *Copyright 2016 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

/**
 * ixa-pipe-convert.
 * 
 * @author ragerri
 * @version 2017-05-17
 * 
 */

public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-convert by looking at the MANIFEST
   * file.
   */
  private final static String version = CLI.class.getPackage()
      .getImplementationVersion();

  Namespace parsedArguments = null;

  // create Argument Parser
  ArgumentParser parser = ArgumentParsers
      .newArgumentParser("ixa-pipe-convert-" + version + ".jar").description(
          "ixa-pipe-convert-" + version + " converts corpora formats.\n");
  /**
   * Sub parser instance.
   */
  private final Subparsers subParsers = parser.addSubparsers()
      .help("sub-command help");
  /**
   * The parser that manages the absa sub-command.
   */
  private final Subparser absaParser;
  private final Subparser interstockParser;
  private final Subparser timemlParser;
  /**
   * The parser that manages cluster lexicon related functions.
   */
  private final Subparser clusterParser;
  /**
   * The parser to manage markyt conversions.
   */
  private final Subparser markytParser;
  /**
   * The parser that manages treebank conversions.
   */
  private final Subparser treebankParser;
  /**
   * The parser that manages NAF to other formats conversions.
   */
  private final Subparser nafParser;
  private final Subparser epecParser;
  /**
   * The parser that manages the general conversion functions.
   */
  private final Subparser convertParser;

  private static final String ABSA_CONVERSOR_NAME = "absa";
  private static final String INTERSTOCK_CONVERSOR_NAME = "interstock";
  private static final String TIMEML_CONVERSOR_NAME = "timeml";
  private static final String CLUSTER_CONVERSOR_NAME = "cluster";
  private static final String MARKYT_CONVERSOR_NAME = "markyt";
  private static final String TREEBANK_CONVERSOR_NAME = "treebank";
  private static final String NAF_CONVERSOR_NAME = "naf";
  private static final String EPEC_CONVERSOR_NAME = "epec";
  private static final String OTHER_CONVERSOR_NAME = "convert";

  /**
   * Reading the CLI.
   */
  public CLI() {
    absaParser = subParsers.addParser(ABSA_CONVERSOR_NAME)
        .help("ABSA tasks at SemEval conversion functions.");
    loadAbsaParameters();
    interstockParser = subParsers.addParser(INTERSTOCK_CONVERSOR_NAME).help("Interstock data conversion functions.");
    loadInterstockParameters();
    timemlParser = subParsers.addParser(TIMEML_CONVERSOR_NAME).help("TimeML Conversion functions.");
    loadTimeMLParameters();
    clusterParser = subParsers.addParser(CLUSTER_CONVERSOR_NAME)
        .help("Cluster lexicon conversion functions.");
    loadClusterParameters();
    markytParser = subParsers.addParser(MARKYT_CONVERSOR_NAME)
        .help("Markyt conversion functions.\n");
    loadMarkytParameters();
    treebankParser = subParsers.addParser(TREEBANK_CONVERSOR_NAME)
        .help("Treebank conversion functions.");
    loadTreebankParameters();
    nafParser = subParsers.addParser(NAF_CONVERSOR_NAME)
        .help("NAF to other formats conversion functions.");
    loadNafParameters();
    epecParser = subParsers.addParser(EPEC_CONVERSOR_NAME).help("EPEC format conversion functions.");
    loadEpecParameters();
    convertParser = subParsers.addParser(OTHER_CONVERSOR_NAME)
        .help("Other conversion functions.");
    loadConvertParameters();
  }

  public static void main(String[] args) throws IOException,
      ParserConfigurationException, SAXException, JDOMException {

    final CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  /**
   * Parse the command interface parameters with the argParser.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if problems with the incoming data
   * @throws JDOMException
   *           a xml exception
   */
  public final void parseCLI(final String[] args)
      throws IOException, JDOMException {
    try {
      parsedArguments = parser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      switch (args[0]) {
      case ABSA_CONVERSOR_NAME:
        absa();
        break;
      case INTERSTOCK_CONVERSOR_NAME:
        interstock();
        break;
      case TIMEML_CONVERSOR_NAME:
        timeml();
        break;
      case CLUSTER_CONVERSOR_NAME:
        cluster();
        break;
      case MARKYT_CONVERSOR_NAME:
        markyt();
        break;
      case TREEBANK_CONVERSOR_NAME:
        treebank();
        break;
      case NAF_CONVERSOR_NAME:
        naf();
        break;
      case EPEC_CONVERSOR_NAME:
        epec();
        break;
      case OTHER_CONVERSOR_NAME:
        convert();
        break;
      }
    } catch (final ArgumentParserException e) {
      parser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-convert-" + version
          + "-exec.jar (absa|interstock|timeml|cluster|markyt|treebank|naf|epec|convert) -help for details");
      System.exit(1);
    }
  }

  public final void absa() throws IOException {

    String language = parsedArguments.getString("language");
    if (parsedArguments.get("absa2015ToCoNLL2002") != null) {
      String inputFile = parsedArguments.getString("absa2015ToCoNLL2002");
      String conllFile = AbsaSemEval.absa2015ToCoNLL2002(inputFile, language);
      System.out.print(conllFile);
    } else if (parsedArguments.get("absa2015ToWFs") != null) {
      String inputFile = parsedArguments.getString("absa2015ToWFs");
      String kafString = AbsaSemEval.absa2015ToWFs(inputFile, language);
      System.out.print(kafString);
    } else if (parsedArguments.get("nafToAbsa2015") != null) {
      String inputNAF = parsedArguments.getString("nafToAbsa2015");
      String xmlFile = AbsaSemEval.nafToAbsa2015(inputNAF);
      System.out.print(xmlFile);
    } else if (parsedArguments.get("absa2015PrintTargets") != null) {
      String inputNAF = parsedArguments.getString("absa2015PrintTargets");
      AbsaSemEval.absa2015PrintTargets(inputNAF, language);
    } else if (parsedArguments.get("absa2014ToCoNLL2002") != null) {
      String inputFile = parsedArguments.getString("absa2014ToCoNLL2002");
      String conllFile = AbsaSemEval.absa2014ToCoNLL2002(inputFile, language);
      System.out.println(conllFile);
    } else if (parsedArguments.get("nafToAbsa2014") != null) {
      String inputFile = parsedArguments.getString("nafToAbsa2014");
      System.out.print(AbsaSemEval.nafToAbsa2014(inputFile));
    } else if (parsedArguments.get("yelpGetText") != null) {
      String inputFile = parsedArguments.getString("yelpGetText");
      AbsaSemEval.getYelpText(inputFile);
    } else if (parsedArguments.get("absa2014PrintTargets") != null) {
      String inputNAF = parsedArguments.getString("absa2014PrintTargets");
      AbsaSemEval.absa2014PrintTargets(inputNAF, language);
    }
  }
  
  public final void interstock() throws IOException {
    if (parsedArguments.get("getJsonFinanceBinaryDataset") != null) {
      String inputFile = parsedArguments.getString("getJsonFinanceBinaryDataset");
      Interstock.getJsonFinanceBinaryDataset(inputFile);
    } else if (parsedArguments.get("getJsonMultipleOpinions") != null) {
      String inputFile = parsedArguments.getString("getJsonMultipleOpinions");
      Interstock.getJsonMultipleOpinions(inputFile);
    } else if (parsedArguments.get("getJsonAllOpinions") != null) {
      String inputFile = parsedArguments.getString("getJsonAllOpinions");
      Interstock.getJsonAllOpinions(inputFile);
    }
  }
  
  public final void timeml() {
    String language = parsedArguments.getString("language");
    if (parsedArguments.get("timemlToCoNLL2002") != null) {
      String inputFile = parsedArguments.getString("timemlToCoNLL2002");
      String conllFile = TimeMLToNAF.timeMLToCoNLL2002(inputFile, language);
      System.out.print(conllFile);
    } else if (parsedArguments.get("timemlToRawNAF") != null) {
      String inputFile = parsedArguments.getString("timemlToRawNAF");
      String kafString = TimeMLToNAF.timeMLToRawNAF(inputFile, language);
      System.out.print(kafString);
    }
  }

  public final void cluster() throws IOException {
    if (parsedArguments.get("brownClean") != null) {
      Path inputFile = Paths.get(parsedArguments.getString("brownClean"));
      Convert.brownClusterClean(inputFile);
    } else if (parsedArguments.getString("serializeBrownCluster") != null) {
      Path clusterFile = Paths
          .get(parsedArguments.getString("serializeBrownCluster"));
      boolean lowercase = Boolean
          .valueOf((boolean) parsedArguments.get("lowercase"));
      SerializeResources.serializeBrownClusters(clusterFile, lowercase);
    } else if (parsedArguments.getString("serializeClarkCluster") != null) {
      Path clusterFile = Paths
          .get(parsedArguments.getString("serializeClarkCluster"));
      boolean lowercase = Boolean
          .valueOf((boolean) parsedArguments.get("lowercase"));
      SerializeResources.serializeClusters(clusterFile, lowercase);
    } else if (parsedArguments.getString("serializeEntityDictionary") != null) {
      Path dictionaryFile = Paths
          .get(parsedArguments.getString("serializeEntityDictionary"));
      SerializeResources.serializeEntityGazetteers(dictionaryFile);
    } else if (parsedArguments.getString("serializeLemmaDictionary") != null) {
      Path lemmaDict = Paths
          .get(parsedArguments.getString("serializeLemmaDictionary"));
      SerializeResources.serializeLemmaDictionary(lemmaDict);
    }
  }

  public final void markyt() throws IOException {
    String language = parsedArguments.getString("language");
    if (parsedArguments.get("barrToCoNLL2002") != null) {
      String docName = parsedArguments.getString("barrToCoNLL2002");
      String entitiesFile = parsedArguments.getString("entities");
      String conllFile = MarkytFormat.barrToCoNLL2002(docName, entitiesFile,
          language);
      System.out.print(conllFile);
    } else if (parsedArguments.get("barrToWFs") != null) {
      String inputFile = parsedArguments.getString("barrToWFs");
      String kafString = MarkytFormat.barrToWFs(inputFile, language);
      System.out.print(kafString);
    } else if (parsedArguments.get("nafToBARR") != null) {
      String inputNAF = parsedArguments.getString("nafToBARR");
      String barrEntities = MarkytFormat.nafToBARREntities(inputNAF);
      System.out.print(barrEntities);
    }
  }

  public final void treebank() throws IOException {
    if (parsedArguments.getString("treebank2WordPos") != null) {
      Path inputTree = Paths.get(parsedArguments.getString("treebank2WordPos"));
      PennTreebankUtils.treebank2WordPos(inputTree);
    } else if (parsedArguments.get("ancora2treebank") != null) {
      Path inputXML = Paths.get(parsedArguments.getString("ancora2treebank"));
      AncoraTreebankReader.processAncoraConstituentXMLCorpus(inputXML);
    } else if (parsedArguments.getString("treebank2tokens") != null) {
      Path inputTree = Paths.get(parsedArguments.getString("treebank2tokens"));
      PennTreebankUtils.treebank2tokens(inputTree);
    } else if (parsedArguments.get("normalizePennTreebank") != null) {
      Path inputTree = Paths
          .get(parsedArguments.getString("normalizePennTreebank"));
      PennTreebankUtils.getCleanPennTrees(inputTree);
    } else if (parsedArguments.get("parseToChunks") != null) {
      Path inputTree = Paths.get(parsedArguments.getString("parseToChunks"));
      ParseToChunks.parseToChunks(inputTree);
    } else if (parsedArguments.get("parseToTabulated") != null) {
      Path inputTree = Paths.get(parsedArguments.getString("parseToTabulated"));
      ParseToTabulated.parseToTabulated(inputTree);
    }
  }

  public final void naf() throws IOException {
    if (parsedArguments.get("nafToCoNLL02") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("nafToCoNLL02"));
      ConllUtils.nafToCoNLL2002(inputDir);
    } else if (parsedArguments.get("nafToCoNLL03") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("nafToCoNLL03"));
      ConllUtils.nafToCoNLL2003(inputDir);
    } else if (parsedArguments.getString("printNER") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("printNER"));
      Convert.getNERFromNAF(inputDir);
    } else if (parsedArguments.getString("printNED") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("printNED"));
      Convert.getNEDFromNAF(inputDir);
    } else if (parsedArguments.getString("removeEntities") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("removeEntities"));
      Convert.removeEntities(inputDir);
    }
  }
  
  public final void epec() throws IOException {
    if (parsedArguments.get("threeLevel") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("threeLevel"));
      String conllCorpus = EpecCorpus.formatCorpus(inputDir, "threeLevel");
      System.out.println(conllCorpus);
    } else if (parsedArguments.get("twoLevel") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("twoLevel"));
      String conllCorpus = EpecCorpus.formatCorpus(inputDir, "twoLevel");
      System.out.println(conllCorpus);
    } else if (parsedArguments.get("oneLevel") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("oneLevel"));
      String conllCorpus = EpecCorpus.formatCorpus(inputDir, "oneLevel");
      System.out.println(conllCorpus);
    }
  }

  public final void convert() throws IOException {
    if (parsedArguments.getString("createMonosemicDictionary") != null) {
      Path inputDir = Paths
          .get(parsedArguments.getString("createMonosemicDictionary"));
      Convert.createMonosemicDictionary(inputDir);
    } else if (parsedArguments.getString("createPOSDictionary") != null) {
      Path inputDir = Paths
          .get(parsedArguments.getString("createPOSDictionary"));
      Convert.convertLemmaToPOSDict(inputDir);
    } else if (parsedArguments.getList("addLemmaDict2POSDict") != null) {
      List<Object> fileArgs = parsedArguments.getList("addLemmaDict2POSDict");
      Path lemmaDict = Paths.get((String) fileArgs.get(0));
      Path xmlDict = Paths.get((String) fileArgs.get(1));
      Convert.addLemmaToPOSDict(lemmaDict, xmlDict);
    }
  }

  public void loadAbsaParameters() {
    this.absaParser.addArgument("-l", "--language")
        .choices("en", "es", "fr", "nl", "tr", "ru").required(true)
        .help("Choose a language.");
    absaParser.addArgument("--absa2015ToCoNLL2002").help(
        "Convert ABSA SemEval 2015 and 2016 Opinion Target Extraction to CoNLL 2002 format.\n");
    absaParser.addArgument("--absa2015ToWFs").help(
        "Convert ABSA SemEval 2015 and 2016 to tokenized WF NAF layer.\n");
    absaParser.addArgument("--nafToAbsa2015").help(
        "Convert NAF containing Opinions into ABSA 2015 and 2016 format.\n");
    absaParser.addArgument("--absa2015PrintTargets").help("Print all targets in ABSA 2015 and 2016 datasets.\n");
    absaParser.addArgument("--absa2014ToCoNLL2002").help(
        "Convert ABSA SemEval 2014 Aspect Term Extraction to CoNLL 2002 format.\n");
    absaParser.addArgument("--nafToAbsa2014")
        .help("Convert NAF containing opinions into ABSA SemEval 2014 format");
    absaParser.addArgument("--absa2014PrintTargets").help("Print all targets in ABSA 2014 dataset.\n");
    absaParser.addArgument("--yelpGetText")
        .help("Extract text attribute from JSON yelp dataset");
  }
  
  public void loadInterstockParameters() {
    interstockParser.addArgument("--getJsonFinanceBinaryDataset")
    .help("Convert JSON interstock dataset into Document Classifier finance binary dataset.\n");
    interstockParser.addArgument("--getJsonMultipleOpinions")
    .help("Print every document that contains multiple opinions in JSON Interstock dataset\n");
    interstockParser.addArgument("--getJsonAllOpinions")
    .help("Print every opinion in JSON Interstock dataset\n");
  }
  
  public void loadTimeMLParameters() {
    this.timemlParser.addArgument("-l","--language").choices("en","es").required(true).help("Choose a language.");
    timemlParser.addArgument("--timemlToRawNAF").help("Convert TimemL from Tempeval3 task to Raw NAF layer.\n");
    timemlParser.addArgument("--timemlToCoNLL2002").help("Convert TimeML from Tempeval3 task to CoNLL 2002 format.\n");
  }

  public void loadClusterParameters() {
    // cluster lexicons functions
    clusterParser.addArgument("--brownClean")
        .help("Remove paragraph if 90% of its characters are not lowercase.\n");
    clusterParser.addArgument("--serializeBrownCluster")
        .help("Serialize Brown cluster lexicons to an object.\n");
    clusterParser.addArgument("--serializeClarkCluster")
        .help("Serialize Clark cluster lexicons and alike to an object.\n");
    clusterParser.addArgument("--serializeEntityDictionary")
        .help("Serialize ixa-pipe-nerc entity gazetteers to an object.\n");
    clusterParser.addArgument("--serializeLemmaDictionary")
        .help("Serialize DictionaryLemmatizer files to an object.\n");
    clusterParser.addArgument("--lowercase").action(Arguments.storeTrue())
        .help("Lowercase input text.\n");
  }

  public void loadMarkytParameters() {
    this.markytParser.addArgument("-l", "--language").choices("en", "es")
        .required(true).help("Choose a language.");
    markytParser.addArgument("--barrToCoNLL2002")
        .help("Document file to convert BARR 2017 to CoNLL 2002 format.\n");
    markytParser.addArgument("--entities")
        .help("Entities file to convert BARR 2017 to CoNLL 2002 format.");
    markytParser.addArgument("--barrToWFs").help(
        "Convert BARR 2017 document file format to tokenized WF NAF layer.\n");
    markytParser.addArgument("--nafToBARR").help(
        "Convert NAF containing entities into BARR 2017 prediction format.\n");
  }

  public void loadTreebankParameters() {
    treebankParser.addArgument("--ancora2treebank")
        .help("Converts ancora constituent parsing annotation into "
            + "Penn Treebank bracketing format.\n");
    treebankParser.addArgument("--treebank2tokens")
        .help("Converts Penn Treebank into tokenized oneline text.\n");
    treebankParser.addArgument("--treebank2WordPos").help(
        "Converts Penn Treebank into Apache OpenNLP POS training format.\n");
    treebankParser.addArgument("--normalizePennTreebank")
        .help("Normalizes Penn Treebank removing -NONE- nodes "
            + "and funcional tags.\n");
    treebankParser.addArgument("--parseToChunks")
        .help("Extracts chunks from Penn Treebank constituent trees.\n");
    treebankParser.addArgument("--parseToTabulated").help(
        "Extracts POS tagging tabulated format from Penn Treebank constituent trees.\n");
  }

  public void loadNafParameters() {
    nafParser.addArgument("--nafToCoNLL02")
        .help("Convert NAF to CoNLL02 format.\n");
    nafParser.addArgument("--nafToCoNLL03")
        .help("Convert NAF to CoNLL03 format.\n");

    nafParser.addArgument("--filterNameTypes")
        .help("Filter Name Entity types.\n");
    nafParser.addArgument("--neTypes").help(
        "Choose named entity type to use with the --filterNameTypes option.\n");

    nafParser.addArgument("--printNER")
        .help("Prints named entity string if NER available in NAF.\n");
    nafParser.addArgument("--printNED")
        .help("Prints named entity string if NED link available in NAF.\n");
    nafParser.addArgument("--removeEntities")
        .help("Removes the entity NAF layer.\n");
  }
  
  public void loadEpecParameters() {
    epecParser.addArgument("--threeLevel").help("Convert Epec to tabulated format containing category, subcategory, case and lemma.\n");
    epecParser.addArgument("--twoLevel").help("Convert Epec to tabulated format containing category and subcategory.\n");
    epecParser.addArgument("--oneLevel").help("Convert Epec to tabulated format containing category.\n");
  }

  public void loadConvertParameters() {
    convertParser.addArgument("--createMonosemicDictionary")
        .help("Create monosemic dictionary from a lemmatizer dictionary.\n");
    convertParser.addArgument("--createPOSDictionary")
        .help("Create POSTagger OpenNLP dictionary from "
            + "lemmatizer dictionary.\n");
    convertParser.addArgument("--addLemmaDict2POSDict").nargs(2)
        .help("Aggregate a lemmatizer dictionary to a POSTagger OpenNLP "
            + "dictionary: first input is lemmatizer dictionary and second output the XML dictionary to be expanded.\n");
  }
}

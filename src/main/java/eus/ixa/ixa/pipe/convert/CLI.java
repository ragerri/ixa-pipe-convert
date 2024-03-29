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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * @version 2018-06-07
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
  private final Subparser hyperPartisanParser;
  /**
   * The parser to manage Interstock JSON data.
   */
  private final Subparser interstockParser;
  /**
   * The parser to manage Tempeval3 datasets.
   */
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
   * The parser to manage DIANN 2018 shared task data.
   */
  private final Subparser diannParser;
  /**
   * The parser to manage TASS shared tasks data.
   */
  private final Subparser tassParser;
  /**
   * The parser that manages treebank conversions.
   */
  private final Subparser treebankParser;
  /**
   * The parser that manages NAF to other formats conversions.
   */
  private final Subparser nafParser;
  /**
   * The parser to manage EPEC corpus.
   */
  private final Subparser epecParser;
  /**
   * The parser that manages the general conversion functions.
   */
  private final Subparser convertParser;

  private static final String ABSA_CONVERSOR_NAME = "absa";
  private static final String HYPERPARTISAN_CONVERSOR_NAME = "hyperpartisan";
  private static final String INTERSTOCK_CONVERSOR_NAME = "interstock";
  private static final String TIMEML_CONVERSOR_NAME = "timeml";
  private static final String CLUSTER_CONVERSOR_NAME = "cluster";
  private static final String MARKYT_CONVERSOR_NAME = "markyt";
  private static final String DIANN_CONVERSOR_NAME = "diann";
  private static final String TASS_CONVERSOR_NAME = "tass";
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
    hyperPartisanParser = subParsers.addParser(HYPERPARTISAN_CONVERSOR_NAME).help("HyperPartisan task at Semeval");
    loadHyperPartisanParameters();
    interstockParser = subParsers.addParser(INTERSTOCK_CONVERSOR_NAME)
        .help("Interstock data conversion functions.");
    loadInterstockParameters();
    timemlParser = subParsers.addParser(TIMEML_CONVERSOR_NAME)
        .help("TimeML Conversion functions.");
    loadTimeMLParameters();
    clusterParser = subParsers.addParser(CLUSTER_CONVERSOR_NAME)
        .help("Cluster lexicon conversion functions.");
    loadClusterParameters();
    diannParser = subParsers.addParser(DIANN_CONVERSOR_NAME)
        .help("DIANN conversion functions.\n");
    loadDiannParameters();
    tassParser = subParsers.addParser(TASS_CONVERSOR_NAME)
        .help("TASS corpora conversion functions.\n");
    loadTassParameters();
    markytParser = subParsers.addParser(MARKYT_CONVERSOR_NAME)
        .help("Markyt conversion functions.\n");
    loadMarkytParameters();
    treebankParser = subParsers.addParser(TREEBANK_CONVERSOR_NAME)
        .help("Treebank conversion functions.");
    loadTreebankParameters();
    nafParser = subParsers.addParser(NAF_CONVERSOR_NAME)
        .help("NAF to other formats conversion functions.");
    loadNafParameters();
    epecParser = subParsers.addParser(EPEC_CONVERSOR_NAME)
        .help("EPEC format conversion functions.");
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
      case HYPERPARTISAN_CONVERSOR_NAME:
        hyperpartisan();
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
      case DIANN_CONVERSOR_NAME:
        diann();
        break;
      case TASS_CONVERSOR_NAME:
        tass();
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
          + "-exec.jar (absa|hyperpartisan|interstock|timeml|cluster|diann|markyt|treebank|naf|epec|convert) -help for details");
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
    } else if (parsedArguments.get("absa2015ToPolarity") != null) {
      int min = 1000;
      int max = 1000;
      if (parsedArguments.get("window") != null) {
        String window = parsedArguments.getString("window");
        min = Integer.parseInt(window.split(":")[0]);
        max = Integer.parseInt(window.split(":")[1]);
      }
      String inputFile = parsedArguments.getString("absa2015ToPolarity");
      String text = AbsaSemEval.absa2015ToDocCatFormatForPolarity(inputFile,
          language, min, max);
      System.out.print(text);
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
  
  public final void hyperpartisan() throws IOException {

    if (parsedArguments.get("hyperPartisanToTrainDoc") != null) {
      Path inputFile = Paths
          .get(parsedArguments.getString("hyperPartisanToTrainDoc"));
      Path truthFile = Paths.get(parsedArguments.getString("truthFile"));
      HyperPartisan.hyperPartisanToTrainDoc(inputFile, truthFile);
    } else if (parsedArguments.get("hyperPartisanTest") != null) {
      Path inputFile = Paths.get(parsedArguments.getString("hyperPartisanTest"));
      String inputModel = parsedArguments.getString("model");
      System.out.print(HyperPartisan.hyperPartisanToTest(inputFile, inputModel));
    }
  }

  public final void interstock() throws IOException {
    if (parsedArguments.get("getJsonFinanceBinaryDataset") != null) {
      String inputFile = parsedArguments
          .getString("getJsonFinanceBinaryDataset");
      Interstock.getJsonFinanceBinaryDataset(inputFile);
    } else if (parsedArguments.get("getJsonMultipleOpinions") != null) {
      String inputFile = parsedArguments.getString("getJsonMultipleOpinions");
      Interstock.getJsonMultipleOpinions(inputFile);
    } else if (parsedArguments.get("getJsonAllOpinionsBinary") != null) {
      String inputFile = parsedArguments.getString("getJsonAllOpinionsBinary");
      Interstock.getJsonAllOpinionsBinary(inputFile);
    } else if (parsedArguments.get("getJsonFinanceOpinionsBinary") != null) {
      String inputFile = parsedArguments
          .getString("getJsonFinanceOpinionsBinary");
      Interstock.getJsonFinanceOpinionsBinary(inputFile);
    } else if (parsedArguments.get("getJsonFinanceAllOpinionsBinary") != null) {
      String inputFile = parsedArguments
          .getString("getJsonFinanceAllOpinionsBinary");
      Interstock.getJsonFinanceAllOpinionsBinary(inputFile);
    } else if (parsedArguments.get("getJsonFinanceOpinionsPolarity") != null) {
      String inputFile = parsedArguments
          .getString("getJsonFinanceOpinionsPolarity");
      Interstock.getJsonFinanceOpinionsPolarity(inputFile);
    } else if (parsedArguments
        .get("getJsonFinanceAllOpinionsPolarity") != null) {
      String inputFile = parsedArguments
          .getString("getJsonFinanceAllOpinionsPolarity");
      Interstock.getJsonFinanceAllOpinionsPolarity(inputFile);
    }
  }

  public final void timeml() throws IOException {
    String language = parsedArguments.getString("language");
    if (parsedArguments.get("timemlToCoNLL2002") != null) {
      Path inputFile = Paths
          .get(parsedArguments.getString("timemlToCoNLL2002"));
      TimeMLFormat.timeMLToCoNLL2002(inputFile, language);
    } else if (parsedArguments.get("timemlToRawNAF") != null) {
      String inputFile = parsedArguments.getString("timemlToRawNAF");
      String kafString = TimeMLFormat.timeMLToRawNAF(inputFile, language);
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

  public final void diann() throws IOException {
    String language = parsedArguments.getString("language");
    if (parsedArguments.get("diannToCoNLL02") != null) {
      Path inputFile = Paths.get(parsedArguments.getString("diannToCoNLL02"));
      String conllFile = DiannFormat.diannToNAFNER(inputFile, language);
      System.out.print(conllFile);
    } else if (parsedArguments.get("addScope") != null) {
      Path inputFile = Paths.get(parsedArguments.getString("addScope"));
      DiannFormat.addScope(inputFile);
    }
  }

  public final void tass() throws IOException, JDOMException {
    if (parsedArguments.get("generalToTabulated") != null) {
      String inputFile = parsedArguments.getString("generalToTabulated");
      TassFormat.generalToTabulated(inputFile);
    } else if (parsedArguments.get("generalToWFs") != null) {
      String inputFile = parsedArguments.getString("generalToWFs");
      TassFormat.generalToWFs(inputFile);
    } else if (parsedArguments.get("nafToGeneralTest") != null) {
      Path inputNAF = Paths.get(parsedArguments.getString("nafToGeneralTest"));
      String outputTest = TassFormat.nafToGeneralTest(inputNAF);
      System.out.print(outputTest);
    } else if (parsedArguments.get("annotateGeneralTest") != null) {
      String inputFile = parsedArguments.getString("annotateGeneralTest");
      String model = parsedArguments.getString("model");
      TassFormat.annotateGeneralTest(inputFile, model);
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
    } else if (parsedArguments.getString("printTerm") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("printTerm"));
      Convert.getTermsFromNAF(inputDir);
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
    if (parsedArguments.getString("convertToUTF8") != null) {
      Path inputDir = Paths
              .get(parsedArguments.getString("convertToUTF8"));
      Convert.unicodeForDirectories(inputDir, false);
    } else if (parsedArguments.getString("getSES") != null) {
      Path inputFile = Paths.get(parsedArguments.getString("getSES"));
      String conllSesCorpus = Convert.getSES(inputFile);
      System.out.print(conllSesCorpus);
    } else if (parsedArguments.getString("createMonosemicDictionary") != null) {
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
    } else if (parsedArguments.get("classifyDocuments") != null) {
      Path inputDir = Paths.get(parsedArguments.getString("classifyDocuments"));
      String model = parsedArguments.getString("model");
      String language = parsedArguments.getString("language");
      StringUtils.classifyDocuments(inputDir, model, language);
    }
  }

  public void loadAbsaParameters() {
    this.absaParser.addArgument("-l", "--language")
        .choices("en", "es", "fr", "nl", "tr", "ru").required(true)
        .help("Choose a language.");
    this.absaParser.addArgument("-w", "--window").required(false).help(
        "Define window size around target for document classification for polarity: absa2015ToPolarity. Example: 5:5");
    absaParser.addArgument("--absa2015ToCoNLL2002").help(
        "Convert ABSA SemEval 2015 and 2016 Opinion Target Extraction to CoNLL 2002 format.\n");
    absaParser.addArgument("--absa2015ToWFs").help(
        "Convert ABSA SemEval 2015 and 2016 to tokenized WF NAF layer.\n");
    absaParser.addArgument("--absa2015ToPolarity").help(
        "Convert ABSA SemEval 2015 and 2016 to Document Classifier format for polarity classification.\n");
    absaParser.addArgument("--nafToAbsa2015").help(
        "Convert NAF containing Opinions into ABSA 2015 and 2016 format.\n");
    absaParser.addArgument("--absa2015PrintTargets")
        .help("Print all targets in ABSA 2015 and 2016 datasets.\n");
    absaParser.addArgument("--absa2014ToCoNLL2002").help(
        "Convert ABSA SemEval 2014 Aspect Term Extraction to CoNLL 2002 format.\n");
    absaParser.addArgument("--nafToAbsa2014")
        .help("Convert NAF containing opinions into ABSA SemEval 2014 format");
    absaParser.addArgument("--absa2014PrintTargets")
        .help("Print all targets in ABSA 2014 dataset.\n");
    absaParser.addArgument("--yelpGetText")
        .help("Extract text attribute from JSON yelp dataset");
  }
  
  public void loadHyperPartisanParameters() {
    hyperPartisanParser.addArgument("--hyperPartisanToTrainDoc")
        .help("Document file to convert HyperPartisanNews for Document Classification.\n");
    hyperPartisanParser.addArgument("--truthFile")
        .help("Ground truth file to convert HyperPartisanNews 2019 for Document Classification.");
    hyperPartisanParser.addArgument("--hyperPartisanTest")
        .help("Process test file from HyperPartisanNews task.\n");
    hyperPartisanParser.addArgument("--model");
  }

  public void loadInterstockParameters() {
    // getting all documents into various formats
    interstockParser.addArgument("--getJsonFinanceBinaryDataset").help(
        "Print the first opinion from each document in JSON Interstock dataset into finance nofinance categories.\n");
    interstockParser.addArgument("--getJsonMultipleOpinions").help(
        "Print every document that contains multiple opinions in JSON Interstock dataset.\n");
    interstockParser.addArgument("--getJsonAllOpinionsBinary").help(
        "Print every opinion in JSON Interstock dataset into finance nofinance categories.\n");
    // getting financial opinions into various formats
    interstockParser.addArgument("--getJsonFinanceOpinionsBinary").help(
        "Print the first financial opinion from each document in JSON Interstock dataset into subjective objective categories.\n");
    interstockParser.addArgument("--getJsonFinanceOpinionsPolarity").help(
        "Print the first financial opinion in JSON Interstock dataset into positive and negative categories.\n");
    interstockParser.addArgument("--getJsonFinanceAllOpinionsBinary").help(
        "Print every financial opinion from each document in JSON Interstock dataset into subjective objective categories.\n");
    interstockParser.addArgument("--getJsonFinanceAllOpinionsPolarity").help(
        "Print every financial opinion in JSON Interstock dataset into positive and negative categories.\n");
  }

  public void loadTimeMLParameters() {
    this.timemlParser.addArgument("-l", "--language")
        .choices("en", "es", "eu", "it").required(false).setDefault("es")
        .help("Choose a language.");
    timemlParser.addArgument("--timemlToRawNAF")
        .help("Convert TimemL from Tempeval3 task to Raw NAF layer.\n");
    timemlParser.addArgument("--timemlToCoNLL2002")
        .help("Convert TimeML from Tempeval3 task to CoNLL 2002 format.\n");
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

  public void loadDiannParameters() {
    this.diannParser.addArgument("-l", "--language").choices("en", "es")
        .required(true).help("Choose a language.");
    diannParser.addArgument("--diannToCoNLL02")
        .help("Convert DIANN format into CoNLL 2002.\n");
    diannParser.addArgument("--addScope")
        .help("Add scope labels after negations in DIANN format.\n");
  }

  public void loadTassParameters() {
    tassParser.addArgument("--generalToTabulated").help(
        "Converts TASS General Corpus into tabulated format for document classication with ixa-pipe-doc.\n");
    tassParser.addArgument("--generalToWFs").help(
        "Converts TASS General Corpus into a NAF containing the tokens and the tweeId in the NAF header. One NAF document per tweet.\n");
    tassParser.addArgument("--nafToGeneralTest").help(
        "Converts NAF containing polarity classification in Topics element into TASS General Corpus test format\n.");
    tassParser.addArgument("--annotateGeneralTest")
        .help("Reads and annotates the testset from TASS General Corpus.\n");
    tassParser.addArgument("--model")
        .help("Chooses the model to annotateGeneralTest argument.\n");
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
    markytParser.addArgument("--diannToCoNLL")
        .help("Convert DIANN format into CoNLL 2002.\n");
    markytParser.addArgument("--addScope")
        .help("Add scope labels after negations in DIANN format.\n");
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
    nafParser.addArgument("--printTerm")
            .help("Prints terms available in NAF.\n");
    nafParser.addArgument("--printNER")
        .help("Prints named entity string if NER available in NAF.\n");
    nafParser.addArgument("--printNED")
        .help("Prints named entity string if NED link available in NAF.\n");
    nafParser.addArgument("--removeEntities")
        .help("Removes the entity NAF layer.\n");
  }

  public void loadEpecParameters() {
    epecParser.addArgument("--threeLevel").help(
        "Convert Epec to tabulated format containing category, subcategory, case and lemma.\n");
    epecParser.addArgument("--twoLevel").help(
        "Convert Epec to tabulated format containing category and subcategory.\n");
    epecParser.addArgument("--oneLevel")
        .help("Convert Epec to tabulated format containing category.\n");
  }

  public void loadConvertParameters() {
    convertParser.addArgument("--convertToUTF8").help("Convert texts to UTF-8.\n");
    convertParser.addArgument("--getSES").help("Convert lemmas to SES.\n");
    convertParser.addArgument("--createMonosemicDictionary")
        .help("Create monosemic dictionary from a lemmatizer dictionary.\n");
    convertParser.addArgument("--createPOSDictionary")
        .help("Create POSTagger OpenNLP dictionary from "
            + "lemmatizer dictionary.\n");
    convertParser.addArgument("--addLemmaDict2POSDict").nargs(2)
        .help("Aggregate a lemmatizer dictionary to a POSTagger OpenNLP "
            + "dictionary: first input is lemmatizer dictionary and second output the XML dictionary to be expanded.\n");
    convertParser.addArgument("--classifyDocuments")
        .help("Classify documents in a file where each document is a line.\n");
    convertParser.addArgument("-l", "--language").choices("eu", "en", "es")
        .required(false).help("Choose a language.");
    convertParser.addArgument("-m", "--model").required(false)
        .help("Choose a model.");
  }
}

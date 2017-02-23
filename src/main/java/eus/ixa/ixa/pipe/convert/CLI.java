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


import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * ixa-pipe-convert.
 * 
 * @author ragerri
 * @version 2016-12-14
 * 
 */

public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-convert by looking at the MANIFEST
   * file.
   */
  private final static String version = CLI.class.getPackage()
      .getImplementationVersion();
  

  public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, JDOMException {

    Namespace parsedArguments = null;

    // create Argument Parser
    ArgumentParser parser = ArgumentParsers.newArgumentParser(
        "ixa-pipe-convert-" + version + ".jar").description(
        "ixa-pipe-convert-" + version + " converts corpora formats.\n");
    
    //cluster lexicons functions
    parser.addArgument("--brownClean").help("Remove paragraph if 90% of its characters are not lowercase.\n");
    parser.addArgument("--serializeBrownCluster").help("Serialize Brown cluster lexicons to an object.\n");
    parser.addArgument("--serializeClarkCluster").help("Serialize Clark cluster lexicons and alike to an object.\n");
    parser.addArgument("--serializeEntityDictionary").help("Serialize ixa-pipe-nerc entity gazetteers to an object.\n");
    parser.addArgument("--serializeLemmaDictionary").help("Serialize DictionaryLemmatizer files to an object.\n");
    
    //pos tagging functions
    parser.addArgument("--createMonosemicDictionary").help("Create monosemic dictionary from a lemmatizer dictionary.\n");
    parser.addArgument("--createPOSDictionary").help("Create POSTagger OpenNLP dictionary from " +
            "lemmatizer dictionary.\n");
    parser.addArgument("--addLemmaDict2POSDict").nargs(2).help("Aggregate a lemmatizer dictionary to a POSTagger OpenNLP " +
            "dictionary: first input is lemmatizer dictionary and second output the XML dictionary to be expanded.\n");
   
    //parsing functions
    parser.addArgument("--ancora2treebank").help("Converts ancora constituent parsing annotation into " +
    		"Penn Treebank bracketing format.\n");
    parser.addArgument("--treebank2tokens").help("Converts Penn Treebank into tokenized oneline text.\n");
    parser.addArgument("--treebank2WordPos").help("Converts Penn Treebank into Apache OpenNLP POS training format.\n");
    parser.addArgument("--normalizePennTreebank").help("Normalizes Penn Treebank removing -NONE- nodes " +
    		"and funcional tags.\n");
    parser.addArgument("--parseToChunks").help("Extracts chunks from Penn Treebank constituent trees.\n");
    parser.addArgument("--parseToTabulated").help("Extracts POS tagging tabulated format from Penn Treebank constituent trees.\n");
    
    //sequence labeling functions
    parser.addArgument("--filterNameTypes").help("Filter Name Entity types.\n");
    parser.addArgument("--neTypes").help("Choose named entity type to use with the --filterNameTypes option.\n");
    
    parser.addArgument("--printNER").help("Prints named entity string if NER available in NAF.\n");
    parser.addArgument("--printNED").help("Prints named entity string if NED link available in NAF.\n");
    parser.addArgument("--removeEntities").help("Removes the entity NAF layer.\n");
 
    parser.addArgument("--nafToCoNLL02").help("Convert NAF to CoNLL02 format.\n");
    parser.addArgument("--nafToCoNLL03").help("Convert NAF to CoNLL03 format.\n");
    
    //opinion arguments
    parser.addArgument("--absa2015ToCoNLL2002").help("Convert ABSA SemEval 2015 and 2016 Opinion Target Extraction to CoNLL 2002 format.\n");
    parser.addArgument("--absa2015ToNAF").help("Convert ABSA SemEval 2015 and 2016 Opinion Target Extraction to NAF.\n");
    parser.addArgument("--absa2015ToWFs").help("Convert ABSA SemEval 2015 and 2016 to tokenized WF NAF layer.\n");
    parser.addArgument("--absa2015Text").help("Extract text sentences from ABSA 2015 and 2016 SemEval corpora.\n");
    parser.addArgument("--nafToAbsa2015").help("Convert NAF containing Opinions into ABSA 2015 and 2016 format.\n");
    parser.addArgument("--absa2014ToCoNLL2002").help("Convert ABSA SemEval 2014 Aspect Term Extraction to CoNLL 2002 format.\n");
    parser.addArgument("--nafToAbsa2014").help("Convert NAF containing opinions into ABSA SemEval 2014 format");
    parser.addArgument("--yelpGetText").help("Extract text attribute from JSON yelp dataset");
    parser.addArgument("--trivagoAspectsToCoNLL02").help("Convert Trivago Aspects Elements to CoNLL02.\n");
    parser.addArgument("--dsrcToCoNLL02").help("Convert DSRC corpus in MMAX format to CoNLL02 for Opinion Target Extraction");
    
    //utils
    parser.addArgument("--lowercase")
        .action(Arguments.storeTrue())
        .help("Lowercase input text.\n");
    
    /*
     * Parse the command line arguments
     */

    // catch errors and print help
    try {
      parsedArguments = parser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.out
          .println("Run java -jar target/ixa-pipe-converter-" + version + ".jar -help for details");
      System.exit(1);
    }
    
    //cluster lexicons options
    if (parsedArguments.get("brownClean") != null) {
      File inputFile = new File(parsedArguments.getString("brownClean"));
      Convert.brownClusterClean(inputFile); 
    }
    else if (parsedArguments.getString("serializeBrownCluster") != null) {
      File clusterFile = new File(parsedArguments.getString("serializeBrownCluster"));
      boolean lowercase = Boolean.valueOf((boolean) parsedArguments.get("lowercase"));
      SerializeResources.serializeBrownClusterFiles(clusterFile, lowercase);
    }
    else if (parsedArguments.getString("serializeClarkCluster") != null) {
      File clusterFile = new File(parsedArguments.getString("serializeClarkCluster"));
      boolean lowercase = Boolean.valueOf((boolean) parsedArguments.get("lowercase"));
      SerializeResources.serializeClusterFiles(clusterFile, lowercase);
    }
    else if (parsedArguments.getString("serializeEntityDictionary") != null) {
      File dictionaryFile = new File(parsedArguments.getString("serializeEntityDictionary"));
      SerializeResources.serializeEntityGazetteers(dictionaryFile);
    }
    else if (parsedArguments.getString("serializeLemmaDictionary") != null) {
      File lemmaDict = new File(parsedArguments.getString("serializeLemmaDictionary"));
      SerializeResources.serializeLemmaDictionary(lemmaDict);
    }
    // pos taggging functions
    else if (parsedArguments.getString("createMonosemicDictionary") != null) {
      File inputDir = new File(parsedArguments.getString("createMonosemicDictionary"));
      Convert.createMonosemicDictionary(inputDir);
    }
    else if (parsedArguments.getString("createPOSDictionary") != null) {
      File inputDir = new File(parsedArguments.getString("createPOSDictionary"));
     Convert.convertLemmaToPOSDict(inputDir);
    }
    else if (parsedArguments.getList("addLemmaDict2POSDict") != null) {
      List<Object> fileArgs = parsedArguments.getList("addLemmaDict2POSDict");
      File lemmaDict = new File((String) fileArgs.get(0));
      File xmlDict = new File((String) fileArgs.get(1));
      Convert.addLemmaToPOSDict(lemmaDict, xmlDict);
    }
    //parsing functions
    else if (parsedArguments.getString("treebank2WordPos") != null) {
      File inputTree = new File(parsedArguments.getString("treebank2WordPos"));
      Convert.treebank2WordPos(inputTree);
    }
    else if (parsedArguments.get("ancora2treebank") != null) { 
      File inputXML = new File(parsedArguments.getString("ancora2treebank"));
      Convert.processAncoraConstituentXMLCorpus(inputXML);
    }
    else if (parsedArguments.getString("treebank2tokens") != null) {
      File inputTree = new File(parsedArguments.getString("treebank2tokens"));
      Convert.treebank2tokens(inputTree);
    }
    else if (parsedArguments.get("normalizePennTreebank") != null) {
      File inputTree = new File(parsedArguments.getString("normalizePennTreebank"));
      Convert.getCleanPennTrees(inputTree);
    }
    else if (parsedArguments.get("parseToChunks") != null) {
      File inputTree = new File(parsedArguments.getString("parseToChunks"));
      ParseToChunks.parseToChunks(inputTree);
    }
    else if (parsedArguments.get("parseToTabulated") != null) {
      File inputTree = new File(parsedArguments.getString("parseToTabulated"));
      ParseToTabulated.parseToTabulated(inputTree);
    }
    // sequence labelling functions
    else if (parsedArguments.getString("printNER") != null) {
      File inputDir = new File(parsedArguments.getString("printNER"));
      Convert.getNERFromNAF(inputDir);
    }
    else if (parsedArguments.getString("printNED") != null) {
      File inputDir = new File(parsedArguments.getString("printNED"));
      Convert.getNEDFromNAF(inputDir);
    }
    else if (parsedArguments.getString("removeEntities") != null) {
      File inputDir = new File(parsedArguments.getString("removeEntities"));
      Convert.removeEntities(inputDir);
    }
    else if (parsedArguments.get("nafToCoNLL02") != null) {
      File inputDir = new File(parsedArguments.getString("nafToCoNLL02"));
      Convert.nafToCoNLL2002(inputDir);
    }
    else if (parsedArguments.get("nafToCoNLL03") != null) {
      File inputDir = new File(parsedArguments.getString("nafToCoNLL03"));
      Convert.nafToCoNLL2003(inputDir);
    }
    // opinion functions
    else if (parsedArguments.get("absa2015ToCoNLL2002") != null) {
      String inputFile = parsedArguments.getString("absa2015ToCoNLL2002");
      String conllFile = AbsaSemEval.absa2015ToCoNLL2002(inputFile);
      System.out.print(conllFile);
    }
    else if (parsedArguments.get("absa2015ToNAF") != null) {
      String inputFile = parsedArguments.getString("absa2015ToNAF");
      String kafString = AbsaSemEval.absa2015ToNAF(inputFile);
      System.out.println(kafString);
    }
    else if (parsedArguments.get("absa2015ToWFs") != null) {
      String inputFile = parsedArguments.getString("absa2015ToWFs");
      String kafString = AbsaSemEval.absa2015ToWFs(inputFile);
      System.out.print(kafString);
    }
    else if (parsedArguments.get("absa2015Text") != null) {
      String inputFile = parsedArguments.getString("absa2015Text");
      String text = AbsaSemEval.absa2015Text(inputFile);
      System.out.print(text);
    }
    else if (parsedArguments.get("nafToAbsa2015") != null) {
      String inputNAF = parsedArguments.getString("nafToAbsa2015");
      String xmlFile = AbsaSemEval.nafToAbsa2015(inputNAF);
      System.out.print(xmlFile);
    }
    else if (parsedArguments.get("absa2014ToCoNLL2002") != null) {
      String inputFile = parsedArguments.getString("absa2014ToCoNLL2002");
      String conllFile = AbsaSemEval.absa2014ToCoNLL2002(inputFile);
      System.out.println(conllFile);
    }
    else if (parsedArguments.get("nafToAbsa2014") != null) {
      String inputFile = parsedArguments.getString("nafToAbsa2014");
      System.out.print(AbsaSemEval.nafToAbsa2014(inputFile));
    }
    else if (parsedArguments.get("yelpGetText") != null) {
      String inputFile = parsedArguments.getString("yelpGetText");
      AbsaSemEval.getYelpText(inputFile);
    }
    else if (parsedArguments.get("trivagoAspectsToCoNLL02") != null) {
      File inputDir = new File(parsedArguments.getString("trivagoAspectsToCoNLL02"));
      Convert.trivagoAspectsToCoNLL02(inputDir);
    }
    else if (parsedArguments.get("dsrcToCoNLL02") != null) {
      String inputDir = parsedArguments.getString("dsrcToCoNLL02");
      DSRCCorpus.DSRCToCoNLL2002(inputDir);
    }
  }
}

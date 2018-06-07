/*
 *Copyright 2018 Rodrigo Agerri

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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;

/**
 * Class for conversors of TASS tasks datasets.
 * 
 * @author ragerri
 * @version 2018-06-07
 */
public class TassFormat {
  
  private static String LANGUAGE = "es";

  // do not instantiate this class
  private TassFormat() {
  }

  public static void generalToTabulated(String fileName)
      throws JDOMException, IOException {
    StringBuilder sb = new StringBuilder();
    // reading the TASS General Corpus xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    Document doc = sax.build(fileName);
    XPathExpression<Element> expr = xFactory.compile("//tweet",
        Filters.element());
    List<Element> tweets = expr.evaluate(doc);

    for (Element tweet : tweets) {
      String tokenizedTweetContent = null;
      String tweetPolarity = null;
      String tweetId = tweet.getChildText("tweetid");
      String tweetContentString = tweet.getChildText("content");
      // the list contains just one list of tokens
      List<List<Token>> segmentedSentences = StringUtils
          .tokenizeSentence(tweetContentString, LANGUAGE);
      for (List<Token> tokenizedSentence : segmentedSentences) {
        String[] tokenizedTweetArray = eus.ixa.ixa.pipe.ml.utils.StringUtils
            .convertListTokenToArrayStrings(tokenizedSentence);
        tokenizedTweetContent = StringUtils
            .getStringFromTokens(tokenizedTweetArray);
      }
      if (tweet.getChild("sentiments").getChild("polarity")
          .getChildText("value") != null) {
        tweetPolarity = tweet.getChild("sentiments").getChild("polarity")
            .getChildText("value");
      }
      sb.append(tweetId).append("\t").append(tweetPolarity).append("\t").append(tokenizedTweetContent)
          .append("\n");
    }
    System.out.println(sb.toString());
  }

  public static void generalToWFs(String fileName) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//tweet",
          Filters.element());
      List<Element> tweets = expr.evaluate(doc);

      for (Element tweet : tweets) {
        String tweetId = tweet.getChildText("tweetid");
        KAFDocument kaf = new KAFDocument(LANGUAGE, "v1.naf");
        kaf.createPublic().publicId = tweetId;
       
        String tweetContentString = tweet.getChildText("content");
        List<List<Token>> segmentedSentences = StringUtils
            .tokenizeSentence(tweetContentString, LANGUAGE);
        for (List<Token> sentence : segmentedSentences) {
          for (Token token : sentence) {
            kaf.newWF(token.startOffset(), token.getTokenValue(), 1);
          }
        }
        Path outfile = Files.createFile(Paths.get(tweetId + ".naf"));
        Files.write(outfile,
            kaf.toString().getBytes(StandardCharsets.UTF_8));
        System.err.println(">> Wrote naf document to " + outfile);
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void nafToGeneralTest(String inputNAF) throws IOException {
    Path kafPath = Paths.get(inputNAF);
    KAFDocument kaf = KAFDocument.createFromFile(kafPath.toFile());
    String tweetId = kaf.getPublic().publicId;
    String polarity = kaf.getTopics().get(0).getTopicValue();
    System.out.println(tweetId + "\t" + polarity);
  }
}

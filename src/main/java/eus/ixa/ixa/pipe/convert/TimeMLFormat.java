package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;

public class TimeMLFormat {

  private static final Pattern eventPattern = Pattern.compile("(<EVENT.*?>)(.*?)(</EVENT>)", Pattern.DOTALL);
  private static final Pattern timexPattern = Pattern.compile("(<TIMEX3.*?>)(.*?)(</TIMEX3>)");
  private static final Pattern timexTokenizedPattern = Pattern
      .compile("<\\s+TIMEX3.*?>\\s+(.*?)<\\s+/TIMEX3\\s+>", Pattern.DOTALL);
  private static final Pattern timex3Pattern = Pattern
      .compile("<maitenaTIMEX3.*?typemaitena=maitena\"maitena(\\S+?)maitena\".*?>maitena(.*?)maitena<maitena/TIMEX3maitena>", Pattern.DOTALL);
  
  
                                                                                                                                                                                                                                                                                                                                            
  private TimeMLFormat() {
  }

  private static void timeMLToBIO(Path fileName, String language) throws IOException {
    // reading the TimeML xml file
    StringBuilder sb = new StringBuilder();
    SAXBuilder sax = new SAXBuilder();
    try {
      Document doc = sax.build(fileName.toFile());
      Element rootElement = doc.getRootElement();
      // getting everything in the TEXT element
      Element textElement = rootElement.getChild("TEXT");
      List<Content> textElements = textElement.getContent();
      StringWriter sw = new StringWriter();
      XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
      xmlOutput.output(textElements, sw);
      StringBuffer sbuffer = sw.getBuffer();
      String text = sbuffer.toString().trim();
      // we do not use event elements
      text = eventPattern.matcher(text).replaceAll("$2");
      text = text.replaceAll("``", "\"");
      text = text.replaceAll("''", "\"");
      //remove empty lines before tokenization
      text = text.replaceAll("(?m)^\\s+", "");
      System.out.print("-> DOC: " + text);
      text = convertTimex(text);
      //remove spaces from temporal expression prior tokenization
      List<List<Token>> tokens = StringUtils.tokenizeDocument(text, language);
      // iterate over the tokenized sentences
      for (List<Token> sentence : tokens) {
        String[] tokensArray = eus.ixa.ixa.pipe.ml.utils.StringUtils
            .convertListTokenToArrayStrings(sentence);
        String sentenceString = StringUtils.getStringFromTokens(tokensArray);
        //modify timex expression for formatting
        sentenceString = convertSpaceToTabTimex(sentenceString);
        String[] textArray = sentenceString.split(" ");
        for (int i = 0; i < textArray.length; i++) {
          if (textArray[i].startsWith("<maitenaTIMEX3")) {
            String timexType = timex3Pattern.matcher(textArray[i])
                .replaceAll("$1").trim();
            String timexText = timex3Pattern.matcher(textArray[i])
                .replaceAll("$2").trim();
            String[] timexExpression = timexText.split("(maitena)+");
            sb.append(timexExpression[0]).append("\t").append("B-").append(timexType).append("\n");
            for (int j = 1; j < timexExpression.length; j++) {
              sb.append(timexExpression[j]).append("\t").append("I-").append(timexType).append("\n");
            }
          } else {
            sb.append(textArray[i]).append("\t").append("O").append("\n");
          }
        }
        sb.append("\n");
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    Path outfile = Files.createFile(Paths.get(fileName.toString() + ".conll02"));
    Files.write(outfile,
        sb.toString().getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote conll02 document to " + outfile);
  }
  
  private static String convertTimex(String line) {
    //System.out.println("-> " + line);
    final Matcher timexMatcher = timexPattern.matcher(line);
    final StringBuffer sb = new StringBuffer();
    while (timexMatcher.find()) {
      timexMatcher.appendReplacement(sb,
          timexMatcher.group().replaceAll("\\s+", "maitena"));
    }
    timexMatcher.appendTail(sb);
    line = sb.toString();
    return line;
  }
  
  private static String convertSpaceToTabTimex(String line) {
    //System.out.println("-> " + line);
    final Matcher timexMatcher = timexTokenizedPattern.matcher(line);
    final StringBuffer sb = new StringBuffer();
    while (timexMatcher.find()) {
      timexMatcher.appendReplacement(sb,
          timexMatcher.group().replaceAll("\\s+", "maitena"));
    }
    timexMatcher.appendTail(sb);
    line = sb.toString();
    return line;
  }
  
  public static void timeMLToCoNLL2002(Path dir, String language) throws IOException {
    if (Files.isRegularFile(dir) && !dir.toString().endsWith("conll02")) {
      timeMLToBIO(dir, language);
    }
    else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            timeMLToCoNLL2002(dir, language);
          } else {
            if (!file.toString().endsWith("conll02")) {
              timeMLToBIO(file, language);
            }
          }
        }
      }
    }
  }

  public static String timeMLToRawNAF(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    SAXBuilder sax = new SAXBuilder();
    try {
      Document doc = sax.build(fileName);
      Element rootElement = doc.getRootElement();
      // getting the Document Creation Time
      Element dctElement = rootElement.getChild("DCT");
      Element dctTimex = dctElement.getChild("TIMEX3");
      String dctTimexValue = dctTimex.getAttributeValue("value");
      kaf.createFileDesc().creationtime = dctTimexValue;
      // getting the TEXT
      Element textElement = rootElement.getChild("TEXT");
      String words = textElement.getValue();
      kaf.setRawText(words);
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    return kaf.toString();
  }
  
}

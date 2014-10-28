package es.ehu.si.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.NameSampleTypeFilter;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

/**
 * Class to filter named entity types in opennlp formatted documents.
 * @author ragerri
 * @version 2014-10-28
 *
 */
public class FilterNameByType {

  /**
   * The pattern of the named entity type.
   */
  Pattern spanTag = Pattern.compile("<.*?<END>");
  /**
   * The object stream of the named entities.
   */
  private ObjectStream<NameSample> samples;

  /**
   * Construct a named entity filter for a opennlp formatted named entities document.
   * @param infile the document
   * @param types the types to be extracted separated by commas
   * @throws UnsupportedEncodingException encoding exception
   * @throws FileNotFoundException io problems
   */
  public FilterNameByType(String infile, String types) throws UnsupportedEncodingException, FileNotFoundException {
    BufferedReader breader = new BufferedReader(new InputStreamReader(
        new FileInputStream(infile), "UTF-8"));
    ObjectStream<String> lineStream = new PlainTextByLineStream(breader);
    samples = new NameSampleDataStream(lineStream);
    if (types != null) {
      String[] neTypes = types.split(",");
      samples = new NameSampleTypeFilter(neTypes, samples);
    }
  }
  
  /**
   * Extract the named entity types.
   * @return the named entity type
   * @throws IOException
   */
  public Object getNamesByType() throws IOException {
    while(true) {
      NameSample nameSample = samples.read();
      if (nameSample != null) {
        String nameString = nameSample.toString();
        Matcher matcher = spanTag.matcher(nameString);
        while (matcher.find()) {
          System.out.println(matcher.group(0));
        }
      }  
      else {
        return null;
      }
    }
  }
  
}

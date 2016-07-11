package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.resources.ClarkCluster;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

/**
* Class to load and serialize as objects:
* <li>
* <ol> Brown cluster documents: word\tword_class\tprob http://metaoptimize.com/projects/wordreprs/ </ol>
* <ol> Clark cluster documents: word\\s+word_class\\s+prob https://github.com/ninjin/clark_pos_induction </ol>
* <ol> Word2Vec cluster documents: word\\s+word_class http://code.google.com/p/word2vec/ </ol>
* </li>
*
* @author ragerri
* @version 2016-07-11
*/
public class SerializeClusters {
  
  private static final Pattern spacePattern = Pattern.compile(" ");
  private static final Pattern tabPattern = Pattern.compile("\t");
  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130", Pattern.UNICODE_CHARACTER_CLASS);
  
  private SerializeClusters() {
  }
  
  public static void serializeClusterFiles(File clusterFile) throws IOException {
    
    Map<String, String> tokenToClusterMap = new HashMap<String, String>();
    
    //TODO parametrize lower and uppercase
    //read Clark or  Word2vec cluster lexicon files
    BufferedReader breader = new BufferedReader(new InputStreamReader(new FileInputStream(clusterFile), Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = spacePattern.split(line);
      if (lineArray.length == 3) {
        String normalizedToken = dotInsideI.matcher(lineArray[0]).replaceAll("i");
        tokenToClusterMap.put(normalizedToken.toLowerCase(), lineArray[1].intern());
      }
      else if (lineArray.length == 2) {
        String normalizedToken = dotInsideI.matcher(lineArray[0]).replaceAll("i");
        tokenToClusterMap.put(normalizedToken.toLowerCase(), lineArray[1].intern());
      }
    }
    String outputFile = clusterFile.getName() + ".ser.gz";
    IOUtils.writeObjectToFile(tokenToClusterMap, outputFile);
    breader.close();
  }
  
  public static void serializeBrownClusterFiles(File clusterFile) throws NumberFormatException, IOException {
    
    Map<String, String> tokenToClusterMap = new HashMap<String, String>();
    BufferedReader breader = new BufferedReader(new InputStreamReader(new FileInputStream(clusterFile), Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      if (lineArray.length == 3) {
        int freq = Integer.parseInt(lineArray[2]);
          if (freq > 5 ) {
            String normalizedToken = ClarkCluster.dotInsideI.matcher(lineArray[1]).replaceAll("I");
            tokenToClusterMap.put(normalizedToken, lineArray[0].intern());
        }
      }
      else if (lineArray.length == 2) {
        String normalizedToken = ClarkCluster.dotInsideI.matcher(lineArray[0]).replaceAll("I");
        tokenToClusterMap.put(normalizedToken, lineArray[1].intern());
      }
    }
    String outputFile = clusterFile.getName() + ".ser.gz";
    IOUtils.writeObjectToFile(tokenToClusterMap, outputFile);
    breader.close();
  }

}


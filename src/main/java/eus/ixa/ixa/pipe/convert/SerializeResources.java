/*
 *  Copyright 2016 Rodrigo Agerri

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import opennlp.tools.cmdline.CmdLineUtil;

/**
 * Class to load and serialize ixa-pipes resources.
 * <ol>
 * <li>Brown clusters: word\tword_class\tprob
 * http://metaoptimize.com/projects/wordreprs/</li>
 * <li>Clark clusters: word\\s+word_class\\s+prob
 * https://github.com/ninjin/clark_pos_induction</li>
 * <li>Word2Vec clusters: word\\s+word_class http://code.google.com/p/word2vec/
 * </li>
 * </ol>
 *
 * @author ragerri
 * @version 2016-07-11
 */
public class SerializeResources {

  private static final Pattern spacePattern = Pattern.compile(" ");
  private static final Pattern tabPattern = Pattern.compile("\t");
  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130",
      Pattern.UNICODE_CHARACTER_CLASS);
  public static final String SER_GZ = ".gz";

  private SerializeResources() {
  }

  public static void serializeClusters(Path dir, boolean lowercase)
      throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      serializeClusterFiles(dir, lowercase);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            serializeClusters(file, lowercase);
          } else {
            serializeClusterFiles(file, lowercase);
          }
        }
      }
    }
  }

  public static void serializeClusterFiles(Path clusterFile, boolean lowercase)
      throws IOException {

    Map<String, String> tokenToClusterMap = new HashMap<String, String>();

    InputStream inputStream = CmdLineUtil.openInFile(clusterFile.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = spacePattern.split(line);
      if (lineArray.length == 3) {
        String normalizedToken = dotInsideI.matcher(lineArray[0])
            .replaceAll("i");
        if (lowercase) {
          tokenToClusterMap.put(normalizedToken.toLowerCase(),
              lineArray[1].intern());
        } else {
          tokenToClusterMap.put(normalizedToken, lineArray[1].intern());
        }
      } else if (lineArray.length == 2) {
        String normalizedToken = dotInsideI.matcher(lineArray[0])
            .replaceAll("i");
        if (lowercase) {
          tokenToClusterMap.put(normalizedToken.toLowerCase(),
              lineArray[1].intern());
        } else {
          tokenToClusterMap.put(normalizedToken, lineArray[1].intern());
        }
      }
    }
    String outputFile = clusterFile.toRealPath().toString() + SER_GZ;
    IOUtils.writeClusterToFile(tokenToClusterMap, outputFile,
        IOUtils.SPACE_DELIMITER);
    System.err.println("-> Cluster serialized to " + outputFile);
    breader.close();
  }

  public static void serializeBrownClusters(Path dir, boolean lowercase)
      throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      serializeBrownClusterFiles(dir, lowercase);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            serializeBrownClusters(file, lowercase);
          } else {
            serializeBrownClusterFiles(file, lowercase);
          }
        }
      }
    }
  }

  public static void serializeBrownClusterFiles(Path clusterFile,
      boolean lowercase) throws NumberFormatException, IOException {

    Map<String, String> tokenToClusterMap = new HashMap<String, String>();
    InputStream inputStream = CmdLineUtil.openInFile(clusterFile.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      if (lineArray.length == 3) {
        int freq = Integer.parseInt(lineArray[2]);
        if (freq > 5) {
          String normalizedToken = dotInsideI.matcher(lineArray[1])
              .replaceAll("I");
          if (lowercase) {
            tokenToClusterMap.put(normalizedToken.toLowerCase(),
                lineArray[0].intern());
          } else {
            tokenToClusterMap.put(normalizedToken, lineArray[0].intern());
          }
        }
      } else if (lineArray.length == 2) {
        String normalizedToken = dotInsideI.matcher(lineArray[0])
            .replaceAll("I");
        if (lowercase) {
          tokenToClusterMap.put(normalizedToken.toLowerCase(),
              lineArray[0].intern());
        } else {
          tokenToClusterMap.put(normalizedToken, lineArray[0].intern());
        }
      }
    }
    String outputFile = clusterFile.toRealPath().toString() + SER_GZ;
    IOUtils.writeClusterToFile(tokenToClusterMap, outputFile,
        IOUtils.SPACE_DELIMITER);
    System.err.println("-> Cluster serialized to " + outputFile);
    breader.close();
  }

  public static void serializeEntityGazetteers(Path dictionaryFile)
      throws IOException {
    Map<String, String> dictionary = new HashMap<String, String>();
    InputStream inputStream = CmdLineUtil.openInFile(dictionaryFile.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      if (lineArray.length == 2) {
        String normalizedToken = dotInsideI.matcher(lineArray[0])
            .replaceAll("i");
        dictionary.put(normalizedToken.toLowerCase(), lineArray[1].intern());
      } else {
        System.err.println(lineArray[0] + " is not well formed!");
      }
    }
    String outputFile = dictionaryFile.toString() + SER_GZ;
    IOUtils.writeClusterToFile(dictionary, outputFile, IOUtils.TAB_DELIMITER);
    breader.close();
  }

  public static void serializeLemmaDictionary(Path lemmaDict)
      throws IOException {
    Map<List<String>, String> dictMap = new HashMap<List<String>, String>();
    InputStream inputStream = CmdLineUtil.openInFile(lemmaDict.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      final String[] elems = tabPattern.split(line);
      if (elems.length == 3) {
        String normalizedToken = dotInsideI.matcher(elems[0]).replaceAll("I");
        dictMap.put(Arrays.asList(normalizedToken, elems[2]), elems[1]);
      } else {
        System.err.println(elems[0] + " is not well formed!");
      }
    }
    String outputFile = lemmaDict.toString() + SER_GZ;
    IOUtils.writeDictionaryLemmatizerToFile(dictMap, outputFile,
        IOUtils.TAB_DELIMITER);
    breader.close();
  }
}

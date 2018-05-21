package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiannFormat {

  private static final Pattern disPattern = Pattern.compile("(<dis>)(.*?)(</dis>)");
  private static final Pattern disTokenizedBegin = Pattern.compile("(<\\s+dis\\s+>)\\s+");
  private static final Pattern disTokenizedEnd = Pattern.compile("(\\s+<\\s+/dis\\s+>)");
  
  private static final Pattern negPattern = Pattern.compile("(<neg>)(.*?)(</neg>)");
  private static final Pattern negTokenizedBegin = Pattern.compile("(<\\s+neg\\s+>)\\s+");
  private static final Pattern negTokenizedEnd = Pattern.compile("(\\s+<\\s+/neg\\s+>)");
  
  private static final Pattern scpTokenizedBegin = Pattern.compile("(<\\s+scp\\s+>)\\s+");
  private static final Pattern scpTokenizedEnd = Pattern.compile("(\\s+<\\s+/scp\\s+>)");
  private static final Pattern scopePattern = Pattern.compile("(<neg>.*</dis>)");
  
  
  private DiannFormat() {
  }
  
  public static String diannToNAFNER(Path fileName, String language) throws IOException {
    StringBuilder sb = new StringBuilder();
    //reading one Diann file
    if (Files.isRegularFile(fileName)) {
      List<String> inputLines = com.google.common.io.Files.readLines(fileName.toFile(), Charset.forName("UTF-8"));
      for (String line : inputLines) {
        line = line.trim();
        line = disTokenizedBegin.matcher(line).replaceAll("<dis>");
        line = disTokenizedEnd.matcher(line).replaceAll("</dis>");
        line = negTokenizedBegin.matcher(line).replaceAll("<neg>");
        line = negTokenizedEnd.matcher(line).replaceAll("</neg>");
        line = scpTokenizedBegin.matcher(line).replaceAll("");
        line = scpTokenizedEnd.matcher(line).replaceAll("");
        //convert spaces inside <dis></dis> into tabs
        line = convertSpaceToTabDis(line);
        line = convertSpaceToTabNeg(line);
        String[] lines = line.split(" ");
        //iterate over words and <dis></dis> entities
        for (int i = 0; i < lines.length; i++) {
          if (lines[i].startsWith("<dis>")) {
            String entity = disPattern.matcher(lines[i]).replaceAll("$2");
            String[] entityElems = entity.split("\t");
            sb.append(entityElems[0] + "\t" + "B-DIS").append("\n");
            for (int j = 1; j < entityElems.length; j++) {
              sb.append(entityElems[j] + "\t" + "I-DIS").append("\n");
            }
          } else if (lines[i].startsWith("<neg>")) {
            String entity = negPattern.matcher(lines[i]).replaceAll("$2");
            String[] entityElems = entity.split("\t");
            sb.append(entityElems[0] + "\t" + "B-NEG").append("\n");
            for (int j = 1; j < entityElems.length; j++) {
                sb.append(entityElems[j] + "\t" + "I-NEG").append("\n");
            }
          } else {
           sb.append(lines[i] + "\t" + "O").append("\n");
          }
        }//end of sentence
        sb.append("\n");
      }
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
    return sb.toString();
  }
  

  private static String convertSpaceToTabDis(String line) {
    final Matcher disMatcher = disPattern.matcher(line);
    final StringBuffer sb = new StringBuffer();
    while (disMatcher.find()) {
      disMatcher.appendReplacement(sb,
          disMatcher.group().replaceAll("\\s", "\t"));
    }
    disMatcher.appendTail(sb);
    line = sb.toString();
    return line;
  }
  
  private static String convertSpaceToTabNeg(String line) {
    final Matcher negMatcher = negPattern.matcher(line);
    final StringBuffer sb = new StringBuffer();
    while (negMatcher.find()) {
      negMatcher.appendReplacement(sb,
          negMatcher.group().replaceAll("\\s", "\t"));
    }
    negMatcher.appendTail(sb);
    line = sb.toString();
    return line;
  }
  
  public static void addScope(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir) && dir.toString().endsWith("tag")) {
      processScope(dir);
    } // process one file
    else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            addScope(dir);
          } else {
            if (file.toString().endsWith("tag")) {
              processScope(file);
            }
          }
        }
      }
    }
  }
  
  private static void processScope(Path fileName) throws IOException {
    // reading the TimeML xml file
    StringBuilder sb = new StringBuilder();
    List<String> sentences = com.google.common.io.Files.readLines(fileName.toFile(), StandardCharsets.UTF_8);
    for (String sentence : sentences) {
       String scopedSentence = scopePattern.matcher(sentence).replaceAll("<scp>$1</scp>");
       sb.append(scopedSentence).append("\n");
    }
    Path outfile = Files.createFile(Paths.get(fileName.toString() + ".scp"));
    Files.write(outfile,
        sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote scp document to " + outfile);
  }

}

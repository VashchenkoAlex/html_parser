package com.ae;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solution for parsing html-page and printing path to the expected
 * element on the page by selector ID
 */
public class FindById {

   /**
    * Stores logger for the class
    */
   private static Logger LOGGER = LoggerFactory.getLogger(FindById.class);
   /**
    * Charset schema
    */
   private static final String CHARSET_NAME = "utf8";
   /**
    * Expected element ID
    */
   private static final String TARGET_ELEMENT_ID = "make-everything-ok-button";
   /**
    * XML path separator
    */
   private static final String TAG_SEPARATOR = " > ";
   /**
    * Path to the comparison output file
    */
   private static final String OUTPUT_FILE_PATH = "./comparison_output.txt";

   private static final String HREF_KEY = "href";
   private static final String CLASS_KEY = "class";
   private static final String TITLE_KEY = "title";

   private static final Pattern hrefPattern = Pattern.compile("^#.*ok.*$");
   private static final Pattern classPattern = Pattern.compile("^btn.*$");
   private static final Pattern titlePattern = Pattern.compile("^.*Make-Button.*$");

   /**
    * Runs the program with parameters:
    * args[0] - path to the origin html file
    * args[1] - path to the modified html file
    *
    * @param args - command line arguments
    */
   public static void main(String[] args) {
      String originResourcePath = args[0];
      String diffResourcePath = args[1];

      Optional<Element> buttonOpt = findElementById(new File(originResourcePath), TARGET_ELEMENT_ID);
      HashMap<String, String> map = new HashMap<>();
      buttonOpt.get().attributes().asList().forEach(attribute -> map.put(attribute.getKey(), attribute.getValue()));

      Optional<Element> targetBtn = getTargetedElement(diffResourcePath);

      String output = getXMLPathToThe(targetBtn);
      output = originResourcePath + "\n" + diffResourcePath + "\n" + output + "\n\n";
      LOGGER.info(output);
      saveComparisonOutput(output);

   }

   /**
    * Searches targeted element by selectors (href, class, title)
    * at the file by given path
    * @param diffResourcePath - given path
    * @return targeted element
    */
   private static Optional<Element> getTargetedElement(String diffResourcePath) {
      File file = new File(diffResourcePath);
      Elements modElements;
      Document modHtml;
      Optional<Element> targetBtn;
      try {
         modHtml = Jsoup.parse(file,
                 CHARSET_NAME,
                 file.getAbsolutePath());
         modElements = modHtml.getElementsByAttributeValueMatching(HREF_KEY,hrefPattern);
         modElements.removeIf(element -> !element.attr(TITLE_KEY).isEmpty() && !titlePattern.matcher(element.attr(TITLE_KEY)).matches());
         modElements.removeIf(element -> !element.attr(CLASS_KEY).isEmpty() && !classPattern.matcher(element.attr(CLASS_KEY)).matches());
         targetBtn = Optional.of(modElements.first());
      } catch (IOException e) {
         LOGGER.error("Error reading [{}] file", file.getAbsolutePath(), e);
         targetBtn = Optional.empty();
      }
      return targetBtn;
   }

   /**
    * Getter for XML path to the given element
    *
    * @param buttonOpt - given element
    */
   private static String getXMLPathToThe(Optional<Element> buttonOpt) {
      StringBuilder sb = new StringBuilder();

      if (buttonOpt.isPresent()) {
         buttonOpt.get().parents().forEach((parent) -> sb.insert(0, parent.tagName() + TAG_SEPARATOR));
         sb.append(buttonOpt.get().tagName());
      }

      return sb.toString();
   }

   /**
    * Parses given html file and finds element by given target element ID
    *
    * @param htmlFile        - given html file
    * @param targetElementId - given target element ID
    * @return found element
    */
   private static Optional<Element> findElementById(File htmlFile, String targetElementId) {
      try {
         Document doc = Jsoup.parse(
                 htmlFile,
                 CHARSET_NAME,
                 htmlFile.getAbsolutePath());

         return Optional.of(doc.getElementById(targetElementId));

      } catch (IOException e) {
         LOGGER.error("Error reading [{}] file", htmlFile.getAbsolutePath(), e);
         return Optional.empty();
      }
   }

   /**
    * Saves given output to the txt file
    *
    * @param output - given output string
    */
   private static void saveComparisonOutput(String output) {
      File outputFile = new File(OUTPUT_FILE_PATH);
      try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
         fos.write(output.getBytes());
      } catch (IOException e) {
         LOGGER.error("Error writing [{}] file", outputFile.getAbsolutePath(), e);
      }
   }

}
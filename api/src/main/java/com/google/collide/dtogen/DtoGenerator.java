// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.dtogen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import xapi.reflect.X_Reflect;

/**
 * Simple source generator that takes in a jar of interface definitions and
 * generates client and server DTO impls.
 *
 */
public class DtoGenerator {

  private static final String SERVER = "server";
  private static final String CLIENT = "client";

  /** Flag: location of the input source dto jar. */
  static String dto_jar = null;

  /** Flag: Name of the generated java class file that contains the DTOs. */
  static String gen_file_name = "DataObjects.java";

  /** Flag: The type of impls to be generated, either CLIENT or SERVER. */
  static String impl = CLIENT;

  /**
   * Flag: A pattern we can use to search an absolute path and find the start
   * of the package definition.")
   */
  static String package_base = "java.";

  /**
   * @param args
   */
  public static void main(String[] args) {

    // First, calculate defaults.

    String myBase = X_Reflect.getFileLoc(DtoGenerator.class);
    if (File.separatorChar != '/') {
      myBase = myBase.replace(File.separatorChar, '/');
    }
    String buildDir = "api" + File.separator + "build" + File.separator;
    myBase = myBase
        // first, replace the path when running from compiled classes (via IDE)
        .replace(buildDir + "classes" + File.separator + "main" + File.separator, "")
        // then, if running from compiled jar;
        // if you are creating another executable jar somewhere else,
        // then you will need to supply command line argument overrides.
        .replaceFirst(buildDir + "libs" + File.separator + ".*[.]jar", "")
    ;
    dto_jar = myBase + "shared/build/libs/shared-0.6-SNAPSHOT.jar";
    gen_file_name = myBase + "client/src/main/java/com/google/collide/dto/client/DtoClientImpls.java";
    impl = "client";
    package_base = "java";

    // Now, check for cli overrides
    for (String arg : args) {
      if (arg.startsWith("--dto_jar=")) {
        dto_jar = arg.substring("--dto_jar=".length());
      } else if (arg.startsWith("--gen_file_name=")) {
        gen_file_name = arg.substring("--gen_file_name=".length());
      } else if (arg.startsWith("--impl=")) {
        impl = arg.substring("--impl=".length());
      } else if (arg.startsWith("--package_base=")) {
        package_base = arg.substring("--package_base=".length());
      } else {
        System.err.println("Unknown flag: " + arg);
        System.exit(1);
      }
    }

    String outputFilePath = gen_file_name;

    // Extract the name of the output file that will contain all the DTOs and
    // its package.
    int packageStart = outputFilePath.indexOf(package_base) + package_base.length();
    int packageEnd = outputFilePath.lastIndexOf('/');
    String fileName = outputFilePath.substring(packageEnd + 1);
    String className = fileName.substring(0, fileName.indexOf(".java"));
    String packageName = outputFilePath.substring(packageStart + 1, packageEnd).replace('/', '.');

    File outFile = new File(outputFilePath);
    File interfaceJar = new File(dto_jar);

    try {
      DtoTemplate dtoTemplate = new DtoTemplate(
          packageName, className, getApiHash(interfaceJar), impl.equals(SERVER));

      // Crack open the JAR that contains the class files for the DTO
      // interfaces. Collect class files to load.
      List<String> classFilePaths = new ArrayList<String>();

      try(JarFile jarFile = new JarFile(interfaceJar);){
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          String entryFilePath = entries.nextElement().getName();
          if (isValidClassFile(entryFilePath)) {
            classFilePaths.add(entryFilePath);
          }
        }
      }finally{}

      // Load the classes that we found above.
      URL[] urls = {interfaceJar.toURI().toURL()};
      URLClassLoader loader = new URLClassLoader(urls);

      // We sort alphabetically to ensure deterministic order of routing types.
      Collections.sort(classFilePaths);

      for (String classFilePath : classFilePaths) {
        URL resource = loader.findResource(classFilePath);
        if (resource != null) {
          String javaName =
              classFilePath.replace('/', '.').substring(0, classFilePath.lastIndexOf(".class"));
          Class<?> dtoInterface = Class.forName(javaName, false, loader);
          if (dtoInterface.isInterface()) {
            // Add interfaces to the DtoTemplate.
            dtoTemplate.addInterface(dtoInterface);
          }
        }
      }

      // Emit the generated file, only if it has changed (prevents spurious gwt recompile).
      String outputFile = dtoTemplate.toString();
      if (outFile.exists()) {
        String current = Files.toString(outFile, Charsets.UTF_8);
        if (current.equals(outputFile)) {
          System.err.println("Skipping dto generation as output file already matches generated values");
          return;
        }
      }
      BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
      writer.write(outputFile);
      writer.close();
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private static boolean isValidClassFile(String file) {
    return file.endsWith(".class") && file.contains("dto");
  }

  private static String getApiHash(File interfaceJar) throws IOException {
    byte[] fileBytes = Files.toByteArray(interfaceJar);
    HashCode hashCode = Hashing.sha1().hashBytes(fileBytes);
    return hashCode.toString();
  }
}

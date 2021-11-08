package eu.europeana.metis.sandbox.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TestUtils {

  public String readFileToString(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
  }

  public byte[] readFileToBytes(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return inputStream.readAllBytes();
  }
}

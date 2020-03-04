package eu.europeana.metis.sandbox.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

  public String readFileToString(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    var path = classLoader.getResource(file);
    if (path == null) {
      throw new IOException("Failed reading file " + file);
    }
    return Files.readString(Paths.get(path.getPath()));
  }

  public byte[] readFileToBytes(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    var path = classLoader.getResource(file);
    if (path == null) {
      throw new IOException("Failed reading file " + file);
    }
    return Files.readAllBytes(Paths.get(path.getPath()));
  }
}

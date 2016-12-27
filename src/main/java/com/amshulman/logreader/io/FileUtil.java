package com.amshulman.logreader.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class FileUtil {
    public static Stream<Path> walk(Path startPath, boolean recurse) {
        try {
          if (recurse) {
            return Files.walk(startPath).parallel().filter(Files::isRegularFile);
          } else {
            return StreamSupport.stream(Files.newDirectoryStream(startPath).spliterator(), true).filter(Files::isRegularFile);
          }
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static Stream<String> lines(Path path) {
        try {
            return Files.lines(path, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }
}

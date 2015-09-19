package com.amshulman.logreader.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class FileUtil {
    public static Stream<Path> walk(Path startPath) {
        try {
            return Files.walk(startPath).parallel().filter(Files::isRegularFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }
}

package com.mizookie.packagemapper.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@UtilityClass
public class FileService {
    // Get the file extension
    public String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return fileName.substring(lastIndexOf);
    }

    // Get the filename with no extension
    public String getFileNameWithoutExtension(String fileName) {
        int lastIndexOfSlash = Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\"));
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1 || lastIndexOfDot < lastIndexOfSlash) {
            return fileName;
        }
        return fileName.substring(lastIndexOfSlash + 1, lastIndexOfDot);
    }

    // Get the filename with extension
    public String getFileNameWithExtension(String fileName) {
        int lastIndexOfSlash = Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\"));
        if (lastIndexOfSlash == -1) {
            return fileName;
        }
        return fileName.substring(lastIndexOfSlash + 1);
    }

    // Get the filename only without any path
    public String getFileNameOnly(String fileName) {
        return Paths.get(fileName).getFileName().toString();
    }

    // Get all files in a directory and its subdirectories but ignore hidden files
    public List<String> getFiles(String directoryPath) {
        VisibleFileVisitor visitor = new VisibleFileVisitor();
        try {
            Files.walkFileTree(Paths.get(directoryPath), visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return visitor.getFiles().stream().map(Path::toString).toList();
    }

    public List<String> getCurrentDirectory(String directoryPath) {
        ArrayList<String> directories = new ArrayList<>();
        File folder = new File(directoryPath);
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                directories.add(file.getName());
            }
        }
        return directories;
    }


    // Get all directories in a directory (excluding subdirectories)
    public List<String> getDirectories(String directoryPath) {
        List<String> directories = null;
        try {
            directories = Files.walk(Paths.get(directoryPath), 1)
                    .filter(Files::isDirectory)
                    .map(Object::toString)
                    .toList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return directories;
    }

    // Helper method to delete a directory and its contents recursively
    public void removeRecursively(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                removeRecursively(c);
            }
        }
        if (!f.delete()) {
            log.error("Failed to delete file: {}", f.getAbsolutePath());
        }
    }

    class VisibleFileVisitor extends SimpleFileVisitor<Path> {
        private final ArrayList<Path> files = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (Files.isHidden(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isRegularFile() && !Files.isHidden(file)) {
                files.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getFiles() {
            return files;
        }
    }
}

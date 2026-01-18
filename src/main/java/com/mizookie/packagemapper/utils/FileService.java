package com.mizookie.packagemapper.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        List<String> files = null;
        try {
            files = Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .map(Object::toString)
                    .toList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
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

    // This method crawls the code in the repository and extracts the imports
    // Probably not a good idea to make this concurrent: https://stackoverflow.com/a/18972018
    static public Boolean findInCrawl(String path, String[] targets) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            String target = String.join("|", targets);
            while ((line = reader.readLine()) != null) {
                if (line.matches(String.format(".*\\b(%s)\\b.*", target))) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println(path);
        }
        return false;
    }
}

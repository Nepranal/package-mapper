package com.mizookie.packagemapper.visitors;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class VisibleFileVisitor extends FileVisitor {
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

    @Override
    public List<Path> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "VisibleFileVisitor";
    }
}

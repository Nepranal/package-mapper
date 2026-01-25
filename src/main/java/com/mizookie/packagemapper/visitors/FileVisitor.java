package com.mizookie.packagemapper.visitors;

import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.List;

public abstract class FileVisitor extends SimpleFileVisitor<Path> {
    public abstract List<Path> getFiles();
}

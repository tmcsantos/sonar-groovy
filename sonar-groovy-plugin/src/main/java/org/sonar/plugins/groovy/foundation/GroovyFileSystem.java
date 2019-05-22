/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.foundation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;

@ScannerSide
public class GroovyFileSystem {

  private final FileSystem fileSystem;
  private final FilePredicates predicates;
  private final FilePredicate isGroovyLanguage;
  private final FilePredicate isMainTypeFile;

  public GroovyFileSystem(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    this.predicates = fileSystem.predicates();
    this.isGroovyLanguage = predicates.hasLanguage(Groovy.KEY);
    this.isMainTypeFile = predicates.hasType(Type.MAIN);
  }

  public boolean hasGroovyFiles() {
    return fileSystem.hasFiles(isGroovyLanguage);
  }

  public List<File> sourceFiles() {
    Iterable<File> files = fileSystem.files(predicates.and(isGroovyLanguage, isMainTypeFile));
    List<File> list = new ArrayList<>();
    files.iterator().forEachRemaining(list::add);
    return list;
  }

  public List<InputFile> groovyInputFiles() {
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(isGroovyLanguage);
    List<InputFile> list = new ArrayList<>();
    inputFiles.iterator().forEachRemaining(list::add);
    return list;
  }

  public List<InputFile> sourceInputFiles() {
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(predicates.and(isGroovyLanguage, isMainTypeFile));
    List<InputFile> list = new ArrayList<>();
    inputFiles.iterator().forEachRemaining(list::add);
    return list;
  }

  @CheckForNull
  public InputFile sourceInputFileFromRelativePath(String relativePath) {
    return fileSystem.inputFile(predicates.and(predicates.matchesPathPattern("**/" + relativePath), isGroovyLanguage, isMainTypeFile));
  }

  public File baseDir() {
    return fileSystem.baseDir();
  }

}

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
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.groovy.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class GroovyHighlighterAndTokenizerTest {

  @Test
  public void should_highlight_keywords() throws Exception {
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Greet.groovy");

    Metadata metadata = new FileMetadata().readMetadata(
            new FileInputStream(file), StandardCharsets.UTF_8, file.getName());

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(
            "", file.toPath().getParent(), "Greet.groovy", Groovy.KEY
    );

    SensorContextTester context = SensorContextTester.create(file.getParentFile());
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata));

    context.fileSystem().add(inputFile);

    GroovyHighlighterAndTokenizer highlighter = new GroovyHighlighterAndTokenizer(inputFile);
    context = Mockito.spy(context);
    highlighter.processFile(context);

    assertThat(context.highlightingTypeAt(":Greet.groovy", 1, 0)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 2, 2)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 4, 2)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 4, 25)).containsOnly(TypeOfText.STRING);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 4, 32)).containsOnly(TypeOfText.STRING);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 7, 0)).containsOnly(TypeOfText.STRUCTURED_COMMENT);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 8, 1)).containsOnly(TypeOfText.STRUCTURED_COMMENT);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 9, 1)).containsOnly(TypeOfText.STRUCTURED_COMMENT);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 10, 0)).containsOnly(TypeOfText.ANNOTATION);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 10, 21)).containsOnly(TypeOfText.ANNOTATION);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 12, 2)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 12, 13)).containsOnly(TypeOfText.CONSTANT);
    assertThat(context.highlightingTypeAt(":Greet.groovy", 12, 17)).containsOnly(TypeOfText.COMMENT);
    Mockito.verify(context, Mockito.times(1)).newHighlighting();
  }

  @Test
  public void should_tokenize_for_cpd() throws Exception {
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Greet.groovy");

    Metadata metadata = new FileMetadata().readMetadata(
            new FileInputStream(file), StandardCharsets.UTF_8, file.getName()
    );

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(
            "", file.toPath().getParent(), "Greet.groovy", Groovy.KEY
    );

    SensorContextTester context = SensorContextTester.create(file.getParentFile());
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata));

    context.fileSystem().add(inputFile);

    GroovyHighlighterAndTokenizer highlighter = new GroovyHighlighterAndTokenizer(inputFile);
    context = Mockito.spy(context);
    highlighter.processFile(context);

    assertThat(context.cpdTokens(":Greet.groovy")).extracting("value").containsExactly("classGreet{",
            "defname",
            "Greet(who){name=who}",
            "defsalute(){printlnLITERALnameLITERALnameLITERAL}",
            "}",
            "/** * Javadoc style */",
            "@groovy.beans.Bindable",
            "classCool{",
            "doublex=1.4// Comment",
            "}");
    Mockito.verify(context, Mockito.times(1)).newCpdTokens();
  }

  @Test
  public void should_highlight_nothing_if_file_is_missing() throws Exception {
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Greet.groovy");

    Metadata metadata = new FileMetadata().readMetadata(
            new FileInputStream(file), StandardCharsets.UTF_8, file.getName()
    );

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(
            "", file.toPath().getParent(), "Greet-fake.groovy", Groovy.KEY
    );

    SensorContextTester context = SensorContextTester.create(file.getParentFile());
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata));

    context.fileSystem().add(inputFile);

    GroovyHighlighterAndTokenizer highlighter = new GroovyHighlighterAndTokenizer(inputFile);

    context = Mockito.spy(context);
    highlighter.processFile(context);

    Mockito.verify(context, Mockito.never()).newHighlighting();
  }

  @Test
  public void should_highlight_only_partially_if_file_can_not_be_lexed() throws Exception {
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Error.groovy");

    Metadata metadata = new FileMetadata().readMetadata(
            new FileInputStream(file), StandardCharsets.UTF_8, file.getName()
    );

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(
            "", file.toPath().getParent(), "Error.groovy", Groovy.KEY
    );

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata));

    SensorContextTester context = SensorContextTester.create(file.getParentFile());
    context.fileSystem().add(inputFile);

    GroovyHighlighterAndTokenizer highlighter = new GroovyHighlighterAndTokenizer(inputFile);

    context = Mockito.spy(context);
    highlighter.processFile(context);

    assertThat(context.highlightingTypeAt(":Error.groovy", 1, 0)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Error.groovy", 2, 2)).containsOnly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(":Error.groovy", 3, 2)).isEmpty();
    Mockito.verify(context, Mockito.times(1)).newHighlighting();
  }

}

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
package org.sonar.plugins.groovy.surefire;

import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.surefire.api.SurefireUtils;

import java.io.File;

@DependedUpon("surefire-java")
public class GroovySurefireSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(GroovySurefireSensor.class);

  private final GroovySurefireParser groovySurefireParser;
  private final Configuration configuration;
  private final FileSystem fs;
  private final PathResolver pathResolver;

  public GroovySurefireSensor(GroovySurefireParser groovySurefireParser, Configuration configuration, FileSystem fs, PathResolver pathResolver) {
    this.groovySurefireParser = groovySurefireParser;
    this.configuration = configuration;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Groovy.KEY).name("GroovySurefireSensor");
  }

  @Override
  public void execute(SensorContext context) {
    File dir = SurefireUtils.getReportsDirectory(configuration, fs, pathResolver);
    collect(context, dir);
  }

  protected void collect(SensorContext context, File reportsDir) {
    LOGGER.info("parsing {}", reportsDir);
    groovySurefireParser.collect(context, reportsDir);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}

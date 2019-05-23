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
package org.sonar.plugins.groovy.jacoco;

import java.io.File;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

public class JaCoCoSensor implements Sensor {

  private final JaCoCoConfiguration jaCoCoConfiguration;
  private final GroovyFileSystem fileSystem;
  private final PathResolver pathResolver;
  private final Configuration configuration;

  public JaCoCoSensor(JaCoCoConfiguration jaCoCoConfiguration, GroovyFileSystem fileSystem, PathResolver pathResolver, Configuration configuration) {
    this.jaCoCoConfiguration = jaCoCoConfiguration;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.configuration = configuration;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Groovy JaCoCo")
      .onlyOnLanguage(Groovy.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    if (shouldExecuteOnProject()) {
      new UnitTestsAnalyzer().analyse(context);
    }
  }

  // VisibleForTesting
  boolean shouldExecuteOnProject() {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), jaCoCoConfiguration.getReportPath());
    boolean foundReport = report.exists() && report.isFile();
    boolean shouldExecute = jaCoCoConfiguration.shouldExecuteOnProject(foundReport);
    if (!foundReport && shouldExecute) {
      JaCoCoExtensions.logger().info("JaCoCoSensor: JaCoCo report not found.");
    }
    return shouldExecute;
  }

  class UnitTestsAnalyzer extends AbstractAnalyzer {
    public UnitTestsAnalyzer() {
      super(fileSystem, pathResolver, configuration);
    }

    @Override
    protected String getReportPath() {
      return jaCoCoConfiguration.getReportPath();
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.UNIT;
    }
  }

}

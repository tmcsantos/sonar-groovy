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

public class JaCoCoOverallSensor implements Sensor {

  public static final String JACOCO_OVERALL = "jacoco-overall.exec";

  private final JaCoCoConfiguration jaCoCoConfiguration;
  private final GroovyFileSystem fileSystem;
  private final PathResolver pathResolver;
  private final Configuration configuration;

  public JaCoCoOverallSensor(JaCoCoConfiguration jaCoCoConfiguration, GroovyFileSystem fileSystem, PathResolver pathResolver, Configuration configuration) {
    this.jaCoCoConfiguration = jaCoCoConfiguration;
    this.pathResolver = pathResolver;
    this.fileSystem = fileSystem;
    this.configuration = configuration;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Groovy JaCoCo Overall")
      .onlyOnLanguage(Groovy.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    File reportUTs = pathResolver.relativeFile(fileSystem.baseDir(), jaCoCoConfiguration.getReportPath());
    File reportITs = pathResolver.relativeFile(fileSystem.baseDir(), jaCoCoConfiguration.getItReportPath());
    if (shouldExecuteOnProject()) {
      File reportOverall = new File(context.fileSystem().workDir(), JACOCO_OVERALL);
      reportOverall.getParentFile().mkdirs();
      JaCoCoReportMerger.mergeReports(reportOverall, reportUTs, reportITs);
      new OverallAnalyzer(reportOverall).analyse(context);
    }
  }

  // VisibleForTesting
  boolean shouldExecuteOnProject() {
    File baseDir = fileSystem.baseDir();
    File reportUTs = pathResolver.relativeFile(baseDir, jaCoCoConfiguration.getReportPath());
    File reportITs = pathResolver.relativeFile(baseDir, jaCoCoConfiguration.getItReportPath());
    boolean foundOneReport = reportUTs.exists() || reportITs.exists();
    boolean shouldExecute = jaCoCoConfiguration.shouldExecuteOnProject(foundOneReport);
    if (!foundOneReport && shouldExecute) {
      JaCoCoExtensions.logger().info("JaCoCoOverallSensor: JaCoCo reports not found.");
    }
    return shouldExecute;
  }

  class OverallAnalyzer extends AbstractAnalyzer {
    private final File report;

    OverallAnalyzer(File report) {
      super(fileSystem, pathResolver, configuration);
      this.report = report;
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.OVERALL;
    }

    @Override
    protected String getReportPath() {
      return report.getAbsolutePath();
    }
  }

}

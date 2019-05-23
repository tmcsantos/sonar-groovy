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
package org.sonar.plugins.groovy.codenarc;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarWayProfileTest {

  @Test
  public void shouldCreateProfile() {
    BuiltInQualityProfilesDefinition profileDefinition = new SonarWayProfile();
    ValidationMessages messages = ValidationMessages.create();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDefinition.define(context);

    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(Groovy.KEY, "Sonar way");
    Assert.assertNotNull(profile);
    assertThat(profile.rules()).hasSize(59);
    assertThat(messages.hasErrors()).isFalse();

    CodeNarcRulesDefinition definition = new CodeNarcRulesDefinition();
    RulesDefinition.Context rulesContext = new RulesDefinition.Context();
    definition.define(rulesContext);
    RulesDefinition.Repository repository = rulesContext.repository(CodeNarcRulesDefinition.REPOSITORY_KEY);

    Map<String, RulesDefinition.Rule> rules = new HashMap<>();
    for (RulesDefinition.Rule rule : repository.rules()) {
      rules.put(rule.key(), rule);
    }
  }
}

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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.groovy.foundation.Groovy;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SonarWayProfile implements BuiltInQualityProfilesDefinition {

  public SonarWayProfile() {}

  @Override public void define(Context context) {
    NewBuiltInQualityProfile profile = parseResource(getClass().getClassLoader(), "org/sonar/plugins/groovy/profile-sonar-way.xml", context);
    profile.done();
  }

  public NewBuiltInQualityProfile parseResource(ClassLoader classloader, String xmlClassPath, Context context) {
    Reader reader = new InputStreamReader(Objects.requireNonNull(classloader.getResourceAsStream(xmlClassPath)), StandardCharsets.UTF_8);
    try {
      return parse(reader, context);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public NewBuiltInQualityProfile parse(Reader reader, Context context) {
    ValidationMessages messages = ValidationMessages.create();
    SMInputFactory inputFactory = initStax();
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();

    String name = null, language = null;
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("rules", nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          processRules(rulesCursor, activeRulesBuilder);

        } else if (StringUtils.equals("name", nodeName)) {
          name = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals("language", nodeName)) {
          language = StringUtils.trim(cursor.collectDescendantText(false));
        }
      }
    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }


    if (StringUtils.isBlank(name)) {
      messages.addErrorText("The mandatory node <name> is missing.");
    }
    if (StringUtils.isBlank(language)) {
      messages.addErrorText("The mandatory node <language> is missing.");
    }

    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(name, language);
    ActiveRules activeRules = activeRulesBuilder.build();
    for (ActiveRule activeRule : activeRules.findByRepository(Groovy.KEY)) {
      profile.activateRule(activeRule.ruleKey().repository(), activeRule.ruleKey().rule());
    }
    return profile;
  }

  private void processRules(SMInputCursor rulesCursor, ActiveRulesBuilder activeRulesBuilder) throws XMLStreamException {
    Map<String, String> parameters = new HashMap<>();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();

      String repositoryKey = null;
      String key = null;
      String severity = null;
      parameters.clear();

      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();

        if (StringUtils.equals("repositoryKey", nodeName)) {
          repositoryKey = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("priority", nodeName)) {
          severity = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("parameters", nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor("parameter");
          processParameters(propsCursor, parameters);
        }
      }

      NewActiveRule.Builder rule = new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(repositoryKey, key))
          .setSeverity(severity);
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        rule.setParam(entry.getKey(), entry.getValue());
      }
      activeRulesBuilder.addRule(rule.build());
    }
  }

  private static void processParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
    while (propsCursor.getNext() != null) {
      SMInputCursor propCursor = propsCursor.childElementCursor();
      String key = null;
      String value = null;
      while (propCursor.getNext() != null) {
        String nodeName = propCursor.getLocalName();
        if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(propCursor.collectDescendantText(false));

        } else if (StringUtils.equals("value", nodeName)) {
          value = StringUtils.trim(propCursor.collectDescendantText(false));
        }
      }
      if (key != null) {
        parameters.put(key, value);
      }
    }
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }
}

/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.field;

import com.qwazr.utils.json.JsonMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class FieldDefinitionTest {

	@Test
	public void readCustomFieldDefinitionTest() throws IOException {
		Map<String, FieldDefinition> fields = JsonMapper.MAPPER.readValue(
				com.qwazr.search.test.JavaTest.class.getResourceAsStream("fields.json"),
				FieldDefinition.MapStringFieldTypeRef);
		Assert.assertNotNull(fields);
		fields.forEach((name, field) -> Assert.assertTrue(field instanceof CustomFieldDefinition));
	}

	@Test
	public void readSmartFieldDefinitionTest() throws IOException {
		Map<String, FieldDefinition> fields = JsonMapper.MAPPER.readValue(
				FieldDefinitionTest.class.getResourceAsStream("smart_fields.json"),
				FieldDefinition.MapStringFieldTypeRef);
		Assert.assertNotNull(fields);
		fields.forEach((name, field) -> {
			Assert.assertTrue(field instanceof SmartFieldDefinition);
			Assert.assertNotNull(((SmartFieldDefinition) field).type);
		});

		String json = JsonMapper.MAPPER.writeValueAsString(fields);
		fields = JsonMapper.MAPPER.readValue(json, FieldDefinition.MapStringFieldTypeRef);
		fields.forEach((name, field) -> Assert.assertTrue(field instanceof SmartFieldDefinition));
	}

	void checkFieldDef(SmartFieldDefinition.SmartBuilder builder, SmartFieldDefinition.Type type, Boolean facet,
			Boolean index, Boolean sort, Boolean stored, final String analyzer, final String queryAnalyzer,
			final String[] copyFrom) {
		final SmartFieldDefinition fieldDef = builder.build();
		Assert.assertNotNull(fieldDef);
		Assert.assertEquals(type, fieldDef.type);
		Assert.assertEquals(facet, fieldDef.facet);
		Assert.assertEquals(index, fieldDef.index);
		Assert.assertEquals(sort, fieldDef.sort);
		Assert.assertEquals(stored, fieldDef.stored);
		Assert.assertEquals(analyzer, fieldDef.analyzer);
		Assert.assertEquals(queryAnalyzer, fieldDef.queryAnalyzer);
		Assert.assertArrayEquals(copyFrom, fieldDef.copyFrom);
	}

	@Test
	public void smartFieldBuilderTest() {
		final SmartFieldDefinition.SmartBuilder builder = SmartFieldDefinition.of();

		checkFieldDef(builder, null, null, null, null, null, null, null, null);

		builder.type(SmartFieldDefinition.Type.DOUBLE);
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, null, null, null, null, null, null, null);

		builder.facet(true);
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, null, null, null, null, null, null);

		builder.index(true);
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, null, null, null, null, null);

		builder.sort(true);
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, true, null, null, null, null);

		builder.stored(true);
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, true, true, null, null, null);

		builder.analyzer("analyzer");
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, true, true, "analyzer", null, null);

		builder.queryAnalyzer("queryAnalyzer");
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, true, true, "analyzer", "queryAnalyzer",
				null);

		builder.copyFrom("field");
		checkFieldDef(builder, SmartFieldDefinition.Type.DOUBLE, true, true, true, true, "analyzer", "queryAnalyzer",
				new String[] { "field" });

	}
}

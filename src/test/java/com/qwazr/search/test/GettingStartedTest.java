/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.test;

import com.qwazr.search.field.FieldDefinition;
import com.qwazr.search.index.IndexServiceInterface;
import com.qwazr.search.index.IndexStatus;
import com.qwazr.search.index.QueryDefinition;
import com.qwazr.search.index.ResultDefinition;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.qwazr.search.test.JsonAbstractTest.getDocs;
import static com.qwazr.search.test.JsonAbstractTest.getFieldMap;
import static com.qwazr.search.test.JsonAbstractTest.getQuery;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GettingStartedTest {

	private final static String MY_SCHEMA = "my_schema";
	private final static String MY_INDEX = "my_index";
	public static final LinkedHashMap<String, FieldDefinition> MY_FIELDS_JSON = getFieldMap("my_fields.json");
	public static final Collection<Map<String, Object>> MY_DOCS_JSON = getDocs("my_docs.json");
	public static final QueryDefinition MY_SEARCH_JSON = getQuery("my_search.json");

	@Test
	public void test000startServer() throws Exception {
		TestServer.startServer();
		Assert.assertTrue(TestServer.serverStarted);
	}

	@Test
	public void test0100CreateSchema() throws URISyntaxException {
		IndexServiceInterface client = TestServer.getRemoteClient();
		Assert.assertNotNull(client.createUpdateSchema(MY_SCHEMA));
	}

	@Test
	public void test0110CreateIndex() throws URISyntaxException, IOException {
		IndexServiceInterface client = TestServer.getRemoteClient();
		IndexStatus indexStatus = client.createUpdateIndex(MY_SCHEMA, MY_INDEX, null);
		Assert.assertNotNull(indexStatus);
	}

	@Test
	public void test130SetFields() throws URISyntaxException, IOException {
		IndexServiceInterface client = TestServer.getRemoteClient();
		final LinkedHashMap<String, FieldDefinition> fields = client.setFields(MY_SCHEMA, MY_INDEX, MY_FIELDS_JSON);
		Assert.assertEquals(fields.size(), MY_FIELDS_JSON.size());
	}

	@Test
	public void test200UpdateDocs() throws URISyntaxException, IOException {
		IndexServiceInterface client = TestServer.getRemoteClient();
		final Integer result = client.postMappedDocuments(MY_SCHEMA, MY_INDEX, MY_DOCS_JSON);
		Assert.assertNotNull(result);
		Assert.assertEquals(Integer.valueOf(MY_DOCS_JSON.size()), result);
	}

	@Test
	public void test300Query() throws URISyntaxException, IOException {
		IndexServiceInterface client = TestServer.getRemoteClient();
		final ResultDefinition.WithMap result = client.searchQuery(MY_SCHEMA, MY_INDEX, MY_SEARCH_JSON, null);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.total_hits);
		Assert.assertEquals(2, result.total_hits.intValue());
	}

}

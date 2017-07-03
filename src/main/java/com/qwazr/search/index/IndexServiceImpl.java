/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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
package com.qwazr.search.index;

import com.qwazr.search.analysis.AnalyzerDefinition;
import com.qwazr.search.field.FieldDefinition;
import com.qwazr.search.query.TermQuery;
import com.qwazr.server.AbstractServiceImpl;
import com.qwazr.server.AbstractStreamingOutput;
import com.qwazr.server.ServerException;
import com.qwazr.binder.FieldMapWrapper;
import com.qwazr.utils.LoggerUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.replicator.LocalReplicator;
import org.apache.lucene.replicator.SessionToken;
import org.apache.lucene.search.MatchAllDocsQuery;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

final class IndexServiceImpl extends AbstractServiceImpl implements IndexServiceInterface, AnnotatedServiceInterface {

	private static final Logger LOGGER = LoggerUtils.getLogger(IndexServiceImpl.class);

	private static final String QWAZR_INDEX_ROOT_USER;

	private IndexManager indexManager;

	static {
		String v = System.getProperty("QWAZR_INDEX_ROOT_USER");
		if (v == null)
			v = System.getenv("QWAZR_INDEX_ROOT_USER");
		if (v == null)
			v = System.getenv("QWAZR_ROOT_USER");
		QWAZR_INDEX_ROOT_USER = v;
		if (QWAZR_INDEX_ROOT_USER != null)
			LOGGER.info(() -> "QWAZR_ROOT_USER: " + QWAZR_INDEX_ROOT_USER);
	}

	@Context
	private HttpServletRequest request;

	@Context
	private HttpServletResponse response;

	public IndexServiceImpl() {
	}

	IndexServiceImpl(final IndexManager indexManager) {
		this.indexManager = indexManager;
	}

	@PostConstruct
	public void init() {
		indexManager = getContextAttribute(IndexManager.class);
	}

	/**
	 * Check the right permissions
	 *
	 * @param schemaName
	 * @throws ServerException
	 */
	private void checkRight(final String schemaName) throws ServerException {
		if (QWAZR_INDEX_ROOT_USER == null || request == null)
			return;
		final Principal principal = request.getUserPrincipal();
		if (principal == null)
			throw new ServerException(Response.Status.UNAUTHORIZED);
		final String name = principal.getName();
		if (name == null)
			throw new ServerException(Response.Status.UNAUTHORIZED);
		if (name.equals(QWAZR_INDEX_ROOT_USER))
			return;
		if (name.equals(schemaName))
			return;
		throw new ServerException(Response.Status.UNAUTHORIZED);
	}

	@Override
	final public Set<String> getIndexes(final String schemaName) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).nameSet();
		} catch (ServerException e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public SchemaSettingsDefinition createUpdateSchema(final String schemaName) {
		try {
			checkRight(null);
			indexManager.createUpdate(schemaName, null);
			return indexManager.get(schemaName).getSettings();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public SchemaSettingsDefinition createUpdateSchema(final String schemaName,
			final SchemaSettingsDefinition settings) {
		try {
			checkRight(null);
			return indexManager.createUpdate(schemaName, settings);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Set<String> getSchemas() {
		try {
			checkRight(null);
			return indexManager.nameSet();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response deleteSchema(final String schemaName) {
		try {
			checkRight(null);
			indexManager.delete(schemaName);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public IndexStatus createUpdateIndex(final String schemaName, final String indexName,
			final IndexSettingsDefinition settings) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).createUpdate(indexName,
					settings == null ? IndexSettingsDefinition.EMPTY : settings).getStatus();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public IndexStatus createUpdateIndex(final String schemaName, final String indexName) {
		return createUpdateIndex(schemaName, indexName, IndexSettingsDefinition.EMPTY);
	}

	@Override
	final public LinkedHashMap<String, FieldDefinition> getFields(final String schemaName, final String indexName) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).getFields();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public FieldDefinition getField(final String schemaName, final String indexName, final String fieldName) {
		try {
			checkRight(schemaName);
			Map<String, FieldDefinition> fieldMap = indexManager.get(schemaName).get(indexName, false).getFields();
			FieldDefinition fieldDef = (fieldMap != null) ? fieldMap.get(fieldName) : null;
			if (fieldDef == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "Field not found: " + fieldName + " - Schema/index:" + schemaName + '/' + indexName);
			return fieldDef;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	final public LinkedHashMap<String, FieldDefinition> setFields(final String schemaName, final String indexName,
			final LinkedHashMap<String, FieldDefinition> fields) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).setFields(fields);
			return fields;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	private List<TermDefinition> doAnalyzer(final String schemaName, final String indexName, final String fieldName,
			final String text, final boolean index) throws ServerException, IOException {
		checkRight(schemaName);
		IndexInstance indexInstance = indexManager.get(schemaName).get(indexName, false);
		final Analyzer analyzer = index ? indexInstance.getIndexAnalyzer(fieldName) : indexInstance.getQueryAnalyzer(
				fieldName);
		if (analyzer == null)
			throw new ServerException(
					() -> "No analyzer found for " + fieldName + " - Schema/index: " + schemaName + '/' + indexName);
		return TermDefinition.buildTermList(analyzer, fieldName, text);
	}

	@Override
	final public List<TermDefinition> doAnalyzeIndex(final String schemaName, final String indexName,
			final String fieldName, final String text) {
		try {
			return doAnalyzer(schemaName, indexName, fieldName, text, true);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public FieldStats getFieldStats(String schemaName, String indexName, String fieldName) {
		checkRight(schemaName);
		try {
			return indexManager.get(schemaName).get(indexName, false).getFieldStats(fieldName);
		} catch (IOException e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public List<TermEnumDefinition> doExtractTerms(final String schemaName, final String indexName,
			final String fieldName, final Integer start, final Integer rows) {
		return doExtractTerms(schemaName, indexName, fieldName, null, start, rows);
	}

	@Override
	final public List<TermEnumDefinition> doExtractTerms(final String schemaName, final String indexName,
			final String fieldName, final String prefix, final Integer start, final Integer rows) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).getTermsEnum(fieldName, prefix, start, rows);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public List<TermDefinition> doAnalyzeQuery(final String schemaName, final String indexName,
			final String fieldName, final String text) {
		try {
			return doAnalyzer(schemaName, indexName, fieldName, text, false);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public FieldDefinition setField(final String schemaName, final String indexName, final String fieldName,
			final FieldDefinition field) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).setField(fieldName, field);
			return field;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response deleteField(final String schemaName, final String indexName, final String fieldName) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).deleteField(fieldName);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public LinkedHashMap<String, AnalyzerDefinition> getAnalyzers(final String schemaName,
			final String indexName) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).getAnalyzers();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public AnalyzerDefinition getAnalyzer(final String schemaName, final String indexName,
			final String analyzerName) {
		try {
			checkRight(schemaName);
			final Map<String, AnalyzerDefinition> analyzerMap = indexManager.get(schemaName)
					.get(indexName, false)
					.getAnalyzers();
			final AnalyzerDefinition analyzerDef = (analyzerMap != null) ? analyzerMap.get(analyzerName) : null;
			if (analyzerDef == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "Analyzer not found: " + analyzerName + " - Schema/index: " + schemaName + '/' +
								indexName);
			return analyzerDef;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public void refreshAnalyzers(final String schemaName, final String indexName) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).refreshAnalyzers();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public AnalyzerDefinition setAnalyzer(final String schemaName, final String indexName,
			final String analyzerName, final AnalyzerDefinition analyzer) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).setAnalyzer(analyzerName, analyzer);
			return analyzer;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	final public LinkedHashMap<String, AnalyzerDefinition> setAnalyzers(final String schemaName, final String indexName,
			final LinkedHashMap<String, AnalyzerDefinition> analyzers) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).setAnalyzers(analyzers);
			return analyzers;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response deleteAnalyzer(final String schemaName, final String indexName, final String analyzerName) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).deleteAnalyzer(analyzerName);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public List<TermDefinition> testAnalyzer(final String schemaName, final String indexName,
			final String analyzerName, final String text) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).testAnalyzer(analyzerName, text);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public String testAnalyzerDot(final String schemaName, final String indexName, final String analyzerName,
			final String text) {
		try {
			checkRight(schemaName);
			return TermDefinition.toDot(indexManager.get(schemaName)
					.get(indexName, false)
					.testAnalyzer(analyzerName, text));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public IndexStatus getIndex(final String schemaName, final String indexName) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).getStatus();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public IndexStatus mergeIndex(final String schemaName, final String indexName, final String mergedIndexName,
			final Map<String, String> commitUserData) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).mergeIndex(indexName, mergedIndexName, commitUserData);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public IndexCheckStatus checkIndex(String schemaName, String indexName) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).checkIndex(indexName);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response deleteIndex(final String schemaName, final String indexName) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).delete(indexName);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Integer postMappedDocument(final String schemaName, final String indexName,
			final PostDefinition.Document post) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, true).postMappedDocument(post);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Integer postMappedDocuments(final String schemaName, final String indexName,
			final PostDefinition.Documents post) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, true).postMappedDocuments(post);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public <T> int postDocument(final String schemaName, final String indexName, final Map<String, Field> fields,
			final T document, final Map<String, String> commitUserData) throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).postDocument(fields, document, commitUserData, true);
	}

	@Override
	final public <T> int postDocuments(final String schemaName, final String indexName, final Map<String, Field> fields,
			final Collection<T> documents, final Map<String, String> commitUserData)
			throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).postDocuments(fields, documents, commitUserData, true);
	}

	@Override
	final public <T> int addDocument(final String schemaName, final String indexName, final Map<String, Field> fields,
			final T document, final Map<String, String> commitUserData) throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).postDocument(fields, document, commitUserData, false);
	}

	@Override
	final public <T> int addDocuments(final String schemaName, final String indexName, final Map<String, Field> fields,
			final Collection<T> documents, final Map<String, String> commitUserData)
			throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).postDocuments(fields, documents, commitUserData,
				false);
	}

	@Override
	final public Integer updateMappedDocValues(final String schemaName, final String indexName,
			final PostDefinition.Document post) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, true).updateMappedDocValues(post);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Integer updateMappedDocsValues(final String schemaName, final String indexName,
			final PostDefinition.Documents post) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, true).updateMappedDocsValues(post);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public <T> int updateDocValues(final String schemaName, final String indexName,
			final Map<String, Field> fields, final T document, final Map<String, String> commitUserData)
			throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).updateDocValues(fields, document, commitUserData);
	}

	@Override
	final public <T> int updateDocsValues(final String schemaName, final String indexName,
			final Map<String, Field> fields, final Collection<T> documents, final Map<String, String> commitUserData)
			throws IOException, InterruptedException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, true).updateDocsValues(fields, documents, commitUserData);
	}

	@Override
	final public SortedMap<String, SortedMap<String, BackupStatus>> doBackup(final String schemaName,
			final String indexName, final String backupName) {
		try {
			checkRight(null);
			return indexManager.backups(schemaName, indexName, backupName);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public SortedMap<String, SortedMap<String, SortedMap<String, BackupStatus>>> getBackups(final String schemaName,
			final String indexName, final String backupName, final Boolean extractVersion) {
		try {
			checkRight(null);
			return indexManager.getBackups(schemaName, indexName, backupName, extractVersion != null && extractVersion);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public Integer deleteBackups(final String schemaName, final String indexName, final String backupName) {
		try {
			checkRight(null);
			return indexManager.deleteBackups(schemaName, indexName, backupName);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public AbstractStreamingOutput replicationObtain(final String schemaName, final String indexName,
			final String masterUuid, final String sessionID, final String source, final String fileName) {
		try {
			checkRight(null);
			final LocalReplicator localReplicator = indexManager.get(schemaName)
					.get(indexName, false)
					.getLocalReplicator(masterUuid);
			final InputStream input = localReplicator.obtainFile(sessionID, source, fileName);
			if (input == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "File not found: " + fileName + " - Schema/index: " + schemaName + '/' + indexName);
			return AbstractStreamingOutput.with(input);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response replicationRelease(final String schemaName, final String indexName, final String masterUuid,
			final String sessionID) {
		try {
			checkRight(null);
			indexManager.get(schemaName).get(indexName, false).getLocalReplicator(masterUuid).release(sessionID);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public AbstractStreamingOutput replicationUpdate(final String schemaName, final String indexName,
			final String masterUuid, final String currentVersion) {
		try {
			checkRight(null);

			final SessionToken token = indexManager.get(schemaName)
					.get(indexName, false)
					.getLocalReplicator(masterUuid)
					.checkForUpdate(currentVersion);
			if (token == null) // Returns a 204 (no content)
				return null;

			try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				try (final DataOutputStream dataOutput = new DataOutputStream(outputStream)) {
					token.serialize(dataOutput);
					dataOutput.flush();
				}
				outputStream.flush();
				return AbstractStreamingOutput.with(new ByteArrayInputStream(outputStream.toByteArray()));
			}
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response replicationCheck(final String schemaName, final String indexName) {
		try {
			checkRight(null);
			indexManager.get(schemaName).get(indexName, false).replicationCheck();
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public LinkedHashMap<String, IndexInstance.ResourceInfo> getResources(final String schemaName,
			final String indexName) {
		try {
			checkRight(null);
			return indexManager.get(schemaName).get(indexName, false).getResources();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public AbstractStreamingOutput getResource(final String schemaName, final String indexName,
			final String resourceName) {
		try {
			checkRight(null);
			final InputStream input = indexManager.get(schemaName).get(indexName, false).getResource(resourceName);
			if (input == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "Resource not found: " + resourceName + " - Schema/index: " + schemaName + '/' +
								indexName);
			return AbstractStreamingOutput.with(input);
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public Response postResource(final String schemaName, final String indexName, final String resourceName,
			final long lastModified, final InputStream inputStream) {
		try {
			checkRight(null);
			indexManager.get(schemaName).get(indexName, false).postResource(resourceName, lastModified, inputStream);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public Response deleteResource(final String schemaName, final String indexName, final String resourceName) {
		try {
			checkRight(null);
			indexManager.get(schemaName).get(indexName, false).deleteResource(resourceName);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public Response deleteAll(final String schemaName, final String indexName) {
		try {
			checkRight(schemaName);
			indexManager.get(schemaName).get(indexName, false).deleteAll(null);
			return Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	private QueryDefinition getDocumentQuery(final Object id) {
		final QueryBuilder builder = QueryDefinition.of(
				new TermQuery(FieldDefinition.ID_FIELD, BytesRefUtils.fromAny(id)));
		builder.rows(1);
		builder.returnedField("*");
		return builder.build();
	}

	private QueryDefinition getMatchAllDocQuery(final Integer start, final Integer rows,
			final FieldMapWrapper<?> wrapper) {
		final QueryBuilder builder = QueryDefinition.of(new MatchAllDocsQuery()).start(start).rows(rows);
		if (wrapper == null)
			builder.returnedField("*");
		else
			builder.returnedField(wrapper.fieldMap.keySet());
		return builder.build();
	}

	private ResultDefinition doSearchMap(final String schemaName, final String indexName, final QueryDefinition query)
			throws InterruptedException, ReflectiveOperationException, QueryNodeException, ParseException, IOException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, false).query(null, context -> context.searchMap(query));
	}

	private ResultDefinition doSearchObject(final String schemaName, final String indexName,
			final QueryDefinition query, final FieldMapWrapper<?> wrapper)
			throws InterruptedException, ReflectiveOperationException, QueryNodeException, ParseException, IOException {
		checkRight(schemaName);
		return indexManager.get(schemaName).get(indexName, false).query(null,
				context -> context.searchObject(query, wrapper));
	}

	@Override
	final public LinkedHashMap<String, Object> getDocument(final String schemaName, final String indexName,
			final String id) {
		try {
			if (id != null) {
				final ResultDefinition result = doSearchMap(schemaName, indexName, getDocumentQuery(id));
				if (result != null) {
					List<ResultDocumentMap> docs = result.getDocuments();
					if (docs != null && !docs.isEmpty())
						return docs.get(0).getFields();
				}
			}
			throw new ServerException(Response.Status.NOT_FOUND,
					() -> "Document not found: " + id + " - Schema/index: " + schemaName + '/' + indexName);

		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public List<Map<String, Object>> getDocuments(final String schemaName, final String indexName,
			final Integer start, final Integer rows) {
		try {
			ResultDefinition result = doSearchMap(schemaName, indexName, getMatchAllDocQuery(start, rows, null));
			if (result == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "No document found" + " - Schema/index: " + schemaName + '/' + indexName);
			List<Map<String, Object>> documents = new ArrayList<>();
			List<ResultDocumentMap> docs = result.getDocuments();
			if (docs != null)
				docs.forEach(resultDocument -> documents.add(resultDocument.fields));
			return documents;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public <T> T getDocument(final String schemaName, final String indexName, final Object id,
			final FieldMapWrapper<T> wrapper) {
		try {
			final ResultDefinition result = doSearchObject(schemaName, indexName, getDocumentQuery(id), wrapper);
			if (result == null)
				return null;
			final List<ResultDocumentObject<T>> docs = result.getDocuments();
			if (docs == null || docs.isEmpty())
				return null;
			return docs.get(0).record;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public <T> List<T> getDocuments(final String schemaName, final String indexName, final Integer start,
			final Integer rows, final FieldMapWrapper<T> wrapper) {
		try {
			final ResultDefinition result = doSearchObject(schemaName, indexName,
					getMatchAllDocQuery(start, rows, wrapper), wrapper);
			if (result == null)
				throw new ServerException(Response.Status.NOT_FOUND,
						() -> "No document found" + " - Schema/index: " + schemaName + '/' + indexName);
			final List<T> documents = new ArrayList<>();
			final List<ResultDocumentObject<T>> docs = result.getDocuments();
			if (docs != null)
				docs.forEach(resultDocument -> documents.add(resultDocument.record));
			return documents;
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public ResultDefinition.WithMap searchQuery(final String schemaName, final String indexName,
			final QueryDefinition query, final Boolean delete) {
		try {
			checkRight(schemaName);
			final IndexInstance index = indexManager.get(schemaName).get(indexName, delete != null && delete);
			if (delete != null && delete)
				return index.deleteByQuery(query);
			else
				return index.query(null, context -> context.searchMap(query));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	final public <T> ResultDefinition.WithObject<T> searchQuery(final String schemaName, final String indexName,
			final QueryDefinition query, final FieldMapWrapper<T> wrapper) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).query(null,
					context -> context.searchObject(query, wrapper));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public ResultDefinition.Empty searchQuery(String schemaName, String indexName, QueryDefinition query,
			ResultDocumentsInterface resultDocuments) {
		try {
			checkRight(schemaName);
			return indexManager.get(schemaName).get(indexName, false).query(null,
					context -> context.searchInterface(query, resultDocuments));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public ExplainDefinition explainQuery(final String schemaName, final String indexName, final QueryDefinition query,
			int docId) {
		try {
			checkRight(schemaName);
			final IndexInstance index = indexManager.get(schemaName).get(indexName, false);
			return new ExplainDefinition(index.explain(query, docId));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public String explainQueryText(final String schemaName, final String indexName, final QueryDefinition query,
			final int docId) {
		try {
			checkRight(schemaName);
			final IndexInstance index = indexManager.get(schemaName).get(indexName, false);
			return index.explain(query, docId).toString();
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public String explainQueryDot(final String schemaName, final String indexName, final QueryDefinition query,
			final int docId, final Integer descriptionWrapSize) {
		try {
			return ExplainDefinition.toDot(explainQuery(schemaName, indexName, query, docId),
					descriptionWrapSize == null ? 28 : descriptionWrapSize);
		} catch (IOException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public <T> T query(final String schemaName, final String indexName, final FieldMapWrapper.Cache fieldMapWrappers,
			final QueryActions<T> actions) throws IOException {
		checkRight(schemaName);
		final IndexInstance index = indexManager.get(schemaName).get(indexName, false);
		return index.query(fieldMapWrappers, actions);
	}

	@Override
	public <T> T write(final String schemaName, final String indexName, final WriteActions<T> actions)
			throws IOException {
		checkRight(schemaName);
		final IndexInstance index = indexManager.get(schemaName).get(indexName, false);
		return index.write(actions);
	}

}

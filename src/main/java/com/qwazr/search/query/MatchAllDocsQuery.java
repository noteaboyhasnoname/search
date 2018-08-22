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
package com.qwazr.search.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.qwazr.search.index.QueryContext;
import org.apache.lucene.search.Query;

public class MatchAllDocsQuery extends AbstractQuery<MatchAllDocsQuery> {

    @JsonCreator
    public MatchAllDocsQuery() {
        super(MatchAllDocsQuery.class);
    }

    @Override
    final public Query getQuery(final QueryContext queryContext) {
        return new org.apache.lucene.search.MatchAllDocsQuery();
    }

    @Override
    protected boolean isEqual(MatchAllDocsQuery query) {
        return query != null;
    }

    @Override
    public int hashCode() {
        return MatchAllDocsQuery.class.hashCode();
    }
}

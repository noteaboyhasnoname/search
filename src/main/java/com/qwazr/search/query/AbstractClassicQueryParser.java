/*
 * Copyright 2015-2018 Emmanuel Keller / QWAZR
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

package com.qwazr.search.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.search.index.FieldMap;
import com.qwazr.search.index.QueryContext;
import com.qwazr.utils.ArrayUtils;
import com.qwazr.utils.CollectionsUtils;
import org.apache.lucene.analysis.Analyzer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractClassicQueryParser<T extends AbstractClassicQueryParser> extends AbstractQueryParser<T> {

    final public String[] fields;
    final public LinkedHashMap<String, Float> boosts;
    final public Boolean allow_leading_wildcard;
    final public QueryParserOperator default_operator;
    final public Integer phrase_slop;
    final public Boolean auto_generate_phrase_query;
    final public Float fuzzy_min_sim;
    final public Integer fuzzy_prefix_length;
    final public Integer max_determinized_states;

    @JsonProperty("split_on_whitespace")
    final public Boolean splitOnWhitespace;

    protected AbstractClassicQueryParser(Class<T> queryClass) {
        super(queryClass);
        fields = null;
        boosts = null;
        allow_leading_wildcard = null;
        default_operator = null;
        phrase_slop = null;
        auto_generate_phrase_query = null;
        fuzzy_min_sim = null;
        fuzzy_prefix_length = null;
        max_determinized_states = null;
        splitOnWhitespace = null;
    }

    protected AbstractClassicQueryParser(Class<T> queryClass, AbstractParserBuilder builder) {
        super(queryClass, builder);
        this.fields = builder.fields == null ? null : ArrayUtils.toArray(builder.fields);
        this.boosts = builder.boosts;
        this.allow_leading_wildcard = builder.allow_leading_wildcard;
        this.default_operator = builder.default_operator;
        this.phrase_slop = builder.phrase_slop;
        this.auto_generate_phrase_query = builder.auto_generate_phrase_query;
        this.fuzzy_min_sim = builder.fuzzy_min_sim;
        this.fuzzy_prefix_length = builder.fuzzy_prefix_length;
        this.max_determinized_states = builder.max_determinized_states;
        this.splitOnWhitespace = builder.splitOnWhitespace;
    }

    @JsonIgnore
    @Override
    protected boolean isEqual(T q) {
        return super.isEqual(q) && Arrays.equals(fields, q.fields) && CollectionsUtils.equals(boosts, q.boosts) &&
                Objects.equals(allow_leading_wildcard, q.allow_leading_wildcard) &&
                Objects.equals(default_operator, q.default_operator) && Objects.equals(phrase_slop, q.phrase_slop) &&
                Objects.equals(auto_generate_phrase_query, q.auto_generate_phrase_query) &&
                Objects.equals(fuzzy_min_sim, q.fuzzy_min_sim) &&
                Objects.equals(fuzzy_prefix_length, q.fuzzy_prefix_length) &&
                Objects.equals(max_determinized_states, q.max_determinized_states) &&
                Objects.equals(splitOnWhitespace, q.splitOnWhitespace);
    }

    protected void setParserParameters(final org.apache.lucene.queryparser.classic.QueryParser parser) {
        setQueryBuilderParameters(parser);
        if (default_operator != null)
            parser.setDefaultOperator(default_operator.queryParseroperator);
        if (allow_leading_wildcard != null)
            parser.setAllowLeadingWildcard(allow_leading_wildcard);
        if (phrase_slop != null)
            parser.setPhraseSlop(phrase_slop);
        if (auto_generate_phrase_query != null)
            parser.setAutoGeneratePhraseQueries(auto_generate_phrase_query);
        if (fuzzy_min_sim != null)
            parser.setFuzzyMinSim(fuzzy_min_sim);
        if (fuzzy_prefix_length != null)
            parser.setFuzzyPrefixLength(fuzzy_prefix_length);
        if (max_determinized_states != null)
            parser.setMaxDeterminizedStates(max_determinized_states);
        if (splitOnWhitespace != null)
            parser.setSplitOnWhitespace(splitOnWhitespace);
    }

    protected Map<String, Float> resolvedBoosts(final FieldMap fieldMap) {
        return boosts != null && fieldMap != null ?
                FieldMap.resolveFieldNames(boosts, new HashMap<>(), fieldMap::resolveQueryFieldName) :
                boosts;
    }

    protected String[] resolveFields(final FieldMap fieldMap) {
        return fields != null && fieldMap != null ?
                FieldMap.resolveFieldNames(fields, fieldMap::resolveQueryFieldName) :
                fields;
    }

    protected Analyzer resolveAnalyzer(final QueryContext queryContext) {
        return analyzer == null ? queryContext.getQueryAnalyzer() : analyzer;
    }

    public static abstract class AbstractParserBuilder<B extends AbstractParserBuilder, T extends AbstractClassicQueryParser>
            extends AbstractBuilder<B, T> {

        private Set<String> fields;
        private LinkedHashMap<String, Float> boosts;
        private Boolean allow_leading_wildcard;
        private QueryParserOperator default_operator;
        private Integer phrase_slop;
        private Boolean auto_generate_phrase_query;
        private Float fuzzy_min_sim;
        private Integer fuzzy_prefix_length;
        private Integer max_determinized_states;
        private Boolean splitOnWhitespace;

        protected AbstractParserBuilder(Class<B> builderClass) {
            super(builderClass);
        }

        public abstract T build();

        public B addField(String... fieldSet) {
            if (fields == null)
                fields = new LinkedHashSet<>();
            Collections.addAll(fields, fieldSet);
            return me();
        }

        public B addBoost(String field, Float boost) {
            if (boosts == null)
                boosts = new LinkedHashMap<>();
            boosts.put(field, boost);
            return me();
        }

        public B setAllowLeadingWildcard(Boolean allow_leading_wildcard) {
            this.allow_leading_wildcard = allow_leading_wildcard;
            return me();
        }

        public B setDefaultOperator(QueryParserOperator default_operator) {
            this.default_operator = default_operator;
            return me();
        }

        public B setPhraseSlop(Integer phrase_slop) {
            this.phrase_slop = phrase_slop;
            return me();
        }

        public B setAutoGeneratePhraseQuery(Boolean auto_generate_phrase_query) {
            this.auto_generate_phrase_query = auto_generate_phrase_query;
            return me();
        }

        public B setFuzzyMinSim(Float fuzzy_min_sim) {
            this.fuzzy_min_sim = fuzzy_min_sim;
            return me();
        }

        public B setFuzzyPrefixLength(Integer fuzzy_prefix_length) {
            this.fuzzy_prefix_length = fuzzy_prefix_length;
            return me();
        }

        public B setMaxDeterminizedStates(Integer max_determinized_states) {
            this.max_determinized_states = max_determinized_states;
            return me();
        }

        public B setSplitOnWhitespace(Boolean splitOnWhitespace) {
            this.splitOnWhitespace = splitOnWhitespace;
            return me();
        }

    }
}

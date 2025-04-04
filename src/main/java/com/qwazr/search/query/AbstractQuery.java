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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.qwazr.search.index.QueryContext;
import com.qwazr.utils.Equalizer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.Query;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = BlendedTermQuery.class),
        @JsonSubTypes.Type(value = BooleanQuery.class),
        @JsonSubTypes.Type(value = BoostQuery.class),
        @JsonSubTypes.Type(value = CommonTermsQuery.class),
        @JsonSubTypes.Type(value = ConstantScoreQuery.class),
        @JsonSubTypes.Type(value = DisjunctionMaxQuery.class),
        @JsonSubTypes.Type(value = DocValuesFieldExistsQuery.class),
        @JsonSubTypes.Type(value = DoubleDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = DoubleDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = DoubleExactQuery.class),
        @JsonSubTypes.Type(value = DoubleMultiRangeQuery.class),
        @JsonSubTypes.Type(value = DoubleRangeQuery.class),
        @JsonSubTypes.Type(value = DoubleSetQuery.class),
        @JsonSubTypes.Type(value = DrillDownQuery.class),
        @JsonSubTypes.Type(value = FacetPathQuery.class),
        @JsonSubTypes.Type(value = FloatDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = FloatDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = FloatExactQuery.class),
        @JsonSubTypes.Type(value = FloatMultiRangeQuery.class),
        @JsonSubTypes.Type(value = FloatRangeQuery.class),
        @JsonSubTypes.Type(value = FloatSetQuery.class),
        @JsonSubTypes.Type(value = FunctionQuery.class),
        @JsonSubTypes.Type(value = FunctionScoreQuery.class),
        @JsonSubTypes.Type(value = FuzzyQuery.class),
        @JsonSubTypes.Type(value = Geo3DDistanceQuery.class),
        @JsonSubTypes.Type(value = Geo3DBoxQuery.class),
        @JsonSubTypes.Type(value = IntDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = IntDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = IntExactQuery.class),
        @JsonSubTypes.Type(value = IntMultiRangeQuery.class),
        @JsonSubTypes.Type(value = IntRangeQuery.class),
        @JsonSubTypes.Type(value = IntSetQuery.class),
        @JsonSubTypes.Type(value = JoinQuery.class),
        @JsonSubTypes.Type(value = LatLonPointBBoxQuery.class),
        @JsonSubTypes.Type(value = LatLonPointDistanceQuery.class),
        @JsonSubTypes.Type(value = LatLonPointPolygonQuery.class),
        @JsonSubTypes.Type(value = LongDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = LongDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = LongExactQuery.class),
        @JsonSubTypes.Type(value = LongMultiRangeQuery.class),
        @JsonSubTypes.Type(value = LongRangeQuery.class),
        @JsonSubTypes.Type(value = LongSetQuery.class),
        @JsonSubTypes.Type(value = MatchAllDocsQuery.class),
        @JsonSubTypes.Type(value = MatchNoDocsQuery.class),
        @JsonSubTypes.Type(value = MultiPhraseQuery.class),
        @JsonSubTypes.Type(value = MoreLikeThisQuery.class),
        @JsonSubTypes.Type(value = MultiFieldQuery.class),
        @JsonSubTypes.Type(value = MultiFieldQueryParser.class),
        @JsonSubTypes.Type(value = NGramPhraseQuery.class),
        @JsonSubTypes.Type(value = PayloadScoreQuery.class),
        @JsonSubTypes.Type(value = PhraseQuery.class),
        @JsonSubTypes.Type(value = PrefixQuery.class),
        @JsonSubTypes.Type(value = QueryParser.class),
        @JsonSubTypes.Type(value = RegexpQuery.class),
        @JsonSubTypes.Type(value = SimpleQueryParser.class),
        @JsonSubTypes.Type(value = SortedDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SortedDoubleDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedDoubleDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SortedFloatDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedFloatDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SortedIntDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedIntDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SortedLongDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedLongDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SortedSetDocValuesExactQuery.class),
        @JsonSubTypes.Type(value = SortedSetDocValuesRangeQuery.class),
        @JsonSubTypes.Type(value = SpanContainingQuery.class),
        @JsonSubTypes.Type(value = SpanFirstQuery.class),
        @JsonSubTypes.Type(value = SpanNearQuery.class),
        @JsonSubTypes.Type(value = SpanNotQuery.class),
        @JsonSubTypes.Type(value = SpanOrQuery.class),
        @JsonSubTypes.Type(value = SpanPositionsQuery.class),
        @JsonSubTypes.Type(value = SpanQueryWrapper.class),
        @JsonSubTypes.Type(value = SpanTermQuery.class),
        @JsonSubTypes.Type(value = SpanWithinQuery.class),
        @JsonSubTypes.Type(value = SynonymQuery.class),
        @JsonSubTypes.Type(value = TermQuery.class),
        @JsonSubTypes.Type(value = TermRangeQuery.class),
        @JsonSubTypes.Type(value = TermsQuery.class),
        @JsonSubTypes.Type(value = WildcardQuery.class)})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public abstract class AbstractQuery<T extends AbstractQuery> extends Equalizer<T> {

    protected AbstractQuery(Class<T> queryClass) {
        super(queryClass);
    }

    @JsonIgnore
    public abstract Query getQuery(final QueryContext queryContext)
            throws IOException, ParseException, QueryNodeException, ReflectiveOperationException;

}

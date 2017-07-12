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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.search.annotations.Copy;
import com.qwazr.search.annotations.SmartField;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SmartFieldDefinition extends FieldDefinition {

	final public Type type;
	final public Boolean fulltext;
	final public Boolean facet;
	final public Boolean filter;
	final public Boolean sort;
	final public Boolean stored;
	final public Boolean snippet;
	final public Boolean autocomplete;

	public enum Type {
		TEXT, LONG, INTEGER, DOUBLE, FLOAT
	}

	@JsonCreator
	SmartFieldDefinition(@JsonProperty("type") Type type, @JsonProperty("fulltext") Boolean fulltext,
			@JsonProperty("facet") Boolean facet, @JsonProperty("filter") Boolean filter,
			@JsonProperty("sort") Boolean sort, @JsonProperty("stored") Boolean stored,
			@JsonProperty("snippet") Boolean snippet, @JsonProperty("autocomplete") Boolean autocomplete,
			@JsonProperty("copy_from") String[] copyFrom) {
		super(Template.SmartField, copyFrom);
		this.type = type;
		this.fulltext = fulltext;
		this.facet = facet;
		this.filter = filter;
		this.sort = sort;
		this.stored = stored;
		this.snippet = snippet;
		this.autocomplete = autocomplete;
	}

	private SmartFieldDefinition(SmartBuilder builder) {
		super(builder);
		type = builder.type;
		fulltext = builder.fulltext;
		facet = builder.facet;
		filter = builder.filter;
		sort = builder.sort;
		stored = builder.stored;
		snippet = builder.snippet;
		autocomplete = builder.autocomplete;
	}

	public SmartFieldDefinition(final String fieldName, final SmartField smartField, final Map<String, Copy> copyMap) {
		super(Template.SmartField, from(fieldName, copyMap));
		type = smartField.type();
		fulltext = smartField.fulltext();
		facet = smartField.facet();
		filter = smartField.filter();
		sort = smartField.sort();
		stored = smartField.stored();
		snippet = smartField.snippet();
		autocomplete = smartField.autocomplete();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof SmartFieldDefinition))
			return false;
		if (o == this)
			return true;
		if (!super.equals(o))
			return false;
		final SmartFieldDefinition f = (SmartFieldDefinition) o;
		if (!Objects.equals(type, f.type))
			return false;
		if (!Objects.equals(fulltext, f.fulltext))
			return false;
		if (!Objects.equals(facet, f.facet))
			return false;
		if (!Objects.equals(filter, f.filter))
			return false;
		if (!Objects.equals(sort, f.sort))
			return false;
		if (!Objects.equals(stored, f.stored))
			return false;
		if (!Objects.equals(snippet, f.snippet))
			return false;
		if (!Objects.equals(autocomplete, f.autocomplete))
			return false;
		return true;
	}

	public static Builder of(Type type) {
		return new SmartBuilder().type(type);
	}

	public static class SmartBuilder extends Builder {

		public Type type;
		public Boolean fulltext;
		public Boolean facet;
		public Boolean filter;
		public Boolean sort;
		public Boolean stored;
		public Boolean snippet;
		public Boolean autocomplete;

		public SmartBuilder type(Type type) {
			this.type = type;
			return this;
		}

		public SmartBuilder copyFrom(String copyFrom) {
			return (SmartBuilder) super.copyFrom(copyFrom);
		}

		public SmartBuilder fulltext(Boolean fulltext) {
			this.fulltext = fulltext;
			return this;
		}

		public SmartBuilder facet(Boolean facet) {
			this.facet = facet;
			return this;
		}

		public SmartBuilder filter(Boolean filter) {
			this.filter = filter;
			return this;
		}

		public SmartBuilder sort(Boolean sort) {
			this.sort = sort;
			return this;
		}

		public SmartBuilder stored(Boolean stored) {
			this.stored = stored;
			return this;
		}

		public SmartBuilder snippet(Boolean snippet) {
			this.snippet = snippet;
			return this;
		}

		public SmartBuilder autocomplete(Boolean autocomplete) {
			this.autocomplete = autocomplete;
			return this;
		}

		public SmartFieldDefinition build() {
			return new SmartFieldDefinition(this);
		}

	}

}

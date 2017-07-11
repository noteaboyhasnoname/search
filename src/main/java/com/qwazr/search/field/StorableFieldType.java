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
package com.qwazr.search.field;

import org.apache.lucene.document.Field;

abstract class StorableFieldType extends CustomFieldTypeAbstract {

	protected final Field.Store store;

	StorableFieldType(final Builder<CustomFieldDefinition> builder) {
		super(builder);
		this.store = definition == null ?
				Field.Store.NO :
				(definition.stored != null && definition.stored) ? Field.Store.YES : Field.Store.NO;
	}

}

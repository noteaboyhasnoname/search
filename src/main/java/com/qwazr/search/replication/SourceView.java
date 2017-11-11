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

package com.qwazr.search.replication;

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.store.Directory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

class SourceView {

	private final Path directoryPath;
	private final Map<String, ReplicationSession.Item> items;

	SourceView(final Path directoryPath, final Iterable<String> itemNames) throws IOException {
		this.directoryPath = directoryPath;
		final Map<String, ReplicationSession.Item> f = new LinkedHashMap<>();
		for (String itemName : itemNames) {
			final Path itemPath = directoryPath.resolve(itemName);
			f.put(itemName, new ReplicationSession.Item(itemPath));
		}
		items = Collections.unmodifiableMap(f);
	}

	/**
	 * Check wether the item is already present and is identical. Same size, same version
	 *
	 * @param refName the name of the item to check
	 * @param refItem the details of the item
	 * @return
	 */
	private boolean isUpToDate(final String refName, final ReplicationSession.Item refItem) {
		final ReplicationSession.Item item = items.get(refName);
		return item != null && item.equals(refItem);
	}

	/**
	 * Compute which items are missing, and which items should be deleted
	 *
	 * @param referenceItems the item of the reference (master)
	 * @param itemsToGet     the items to get
	 * @param itemsToDelete  the items to delete
	 */
	void differential(final Map<String, ReplicationSession.Item> referenceItems, final Collection<String> itemsToGet,
			final Collection<String> itemsToDelete) {
		referenceItems.forEach((name, item) -> {
			if (!isUpToDate(name, item))
				itemsToGet.add(name);
		});
		for (final String itemName : items.keySet())
			if (!referenceItems.containsKey(itemName))
				itemsToDelete.add(itemName);
	}

	/**
	 * Return the items to get and the items to delete for a full replication
	 *
	 * @param referenceItems the items of the reference (master)
	 * @param itemsToGet     the items to get
	 * @param itemsToDelete  the items to delete
	 */
	void full(final Map<String, ReplicationSession.Item> referenceItems, final Collection<String> itemsToGet,
			final Collection<String> itemsToDelete) {
		itemsToGet.addAll(referenceItems.keySet());
		itemsToDelete.addAll(items.keySet());
	}

	final Map<String, ReplicationSession.Item> getItems() {
		return items;
	}

	final InputStream getItem(String itemName) throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(directoryPath.resolve(itemName).toFile()));
	}

	static class FromCommit extends SourceView implements Closeable {

		private final SnapshotDeletionPolicy indexSnapshots;
		private final IndexCommit indexCommit;

		private FromCommit(final Path directoryPath, final SnapshotDeletionPolicy indexSnapshots,
				final IndexCommit indexCommit) throws IOException {
			super(directoryPath, indexCommit.getFileNames());
			this.indexSnapshots = indexSnapshots;
			this.indexCommit = indexCommit;
		}

		FromCommit(final Path directoryPath, final SnapshotDeletionPolicy indexSnapshots) throws IOException {
			this(directoryPath, indexSnapshots, indexSnapshots.snapshot());
		}

		@Override
		public void close() throws IOException {
			indexSnapshots.release(indexCommit);
		}
	}

	static class FromDirectory extends SourceView {

		FromDirectory(final Path directoryPath, final Directory directory) throws IOException {
			super(directoryPath, SegmentInfos.readLatestCommit(directory).files(true));
		}

	}

	static class FromPathDirectory extends SourceView {

		FromPathDirectory(final Path directoryPath) throws IOException {
			super(directoryPath, Files.list(directoryPath)
					.filter(p -> Files.isRegularFile(p))
					.map(p -> p.getFileName().toString())
					.collect(Collectors.toList()));
		}
	}

	static class FromPathFiles extends SourceView {

		FromPathFiles(final Path directoryPath, final String... names) throws IOException {
			super(directoryPath, Arrays.asList(names));
		}
	}
}

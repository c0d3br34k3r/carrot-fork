package com.catascopic.template;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.catascopic.template.parse.Node;
import com.catascopic.template.parse.Parser;
import com.catascopic.template.parse.TemplateParser;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

/**
 * Helper class used to cache parsed template files.
 */
public class ParseCache {

	private static final long DEFAULT_SIZE = 256;

	private final LoadingCache<Path, CacheEntry> cache;

	// TODO: Dammit
	public ParseCache(final Configuration config) {
		cache = CacheBuilder.newBuilder().maximumSize(DEFAULT_SIZE)
				.build(new CacheLoader<Path, CacheEntry>() {

					@Override
					public CacheEntry load(Path key) throws IOException, CarrotException {
						try (Reader reader = Files.newBufferedReader(key, config.getCharset())) {
							return new CacheEntry(
									TemplateParser.parse(new Parser(reader), config),
									Files.getLastModifiedTime(key));
						}
					}
				});
	}

	public Node getDocument(Path path) throws CarrotException {
		try {
			return cache.get(path).node;
		} catch (ExecutionException e) {
			throw new CarrotException(e);
		}
	}

	private static class CacheEntry {
		Node node;
		FileTime modified;

		public CacheEntry(Node node, FileTime modifiedTime) {
			this.node = node;
			this.modified = modifiedTime;
		}
	}
}
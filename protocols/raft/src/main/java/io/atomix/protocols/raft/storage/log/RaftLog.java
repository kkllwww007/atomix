/*
 * Copyright 2017-present Open Networking Laboratory
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
package io.atomix.protocols.raft.storage.log;

import io.atomix.protocols.raft.storage.log.entry.RaftLogEntry;
import io.atomix.serializer.Serializer;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.Journal;
import io.atomix.storage.journal.JournalDelegate;
import io.atomix.storage.journal.SegmentedJournal;

import java.io.File;

/**
 * Raft log.
 */
public class RaftLog extends JournalDelegate<RaftLogEntry> {

  /**
   * Returns a new Raft log builder.
   *
   * @return A new Raft log builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  private final Journal<RaftLogEntry> delegate;
  private final boolean flushOnCommit;
  private final RaftLogWriter writer;
  private volatile long commitIndex;

  public RaftLog(
      Journal<RaftLogEntry> delegate,
      boolean flushOnCommit) {
    super(delegate);
    this.delegate = delegate;
    this.flushOnCommit = flushOnCommit;
    this.writer = new RaftLogWriter(delegate.writer(), this);
  }

  @Override
  public RaftLogWriter writer() {
    return writer;
  }

  @Override
  public RaftLogReader openReader(long index) {
    return openReader(index, RaftLogReader.Mode.ALL);
  }

  /**
   * Opens a new Raft log reader with the given reader mode.
   *
   * @param index The index from which to begin reading entries.
   * @param mode The mode in which to read entries.
   * @return The Raft log reader.
   */
  public RaftLogReader openReader(long index, RaftLogReader.Mode mode) {
    return new RaftLogReader(delegate.openReader(index), this, mode);
  }

  /**
   * Returns whether {@code flushOnCommit} is enabled for the log.
   *
   * @return Indicates whether {@code flushOnCommit} is enabled for the log.
   */
  boolean isFlushOnCommit() {
    return flushOnCommit;
  }

  /**
   * Commits entries up to the given index.
   *
   * @param index The index up to which to commit entries.
   */
  void setCommitIndex(long index) {
    this.commitIndex = index;
  }

  /**
   * Returns the Raft log commit index.
   *
   * @return The Raft log commit index.
   */
  long getCommitIndex() {
    return commitIndex;
  }

  /**
   * Raft log builder.
   */
  public static class Builder implements io.atomix.utils.Builder<RaftLog> {
    private static final boolean DEFAULT_FLUSH_ON_COMMIT = false;
    private final SegmentedJournal.Builder<RaftLogEntry> journalBuilder = SegmentedJournal.newBuilder();
    private boolean flushOnCommit = DEFAULT_FLUSH_ON_COMMIT;

    protected Builder() {
    }

    /**
     * Sets the storage name.
     *
     * @param name The storage name.
     * @return The storage builder.
     */
    public Builder withName(String name) {
      journalBuilder.withName(name);
      return this;
    }

    /**
     * Sets the log storage level, returning the builder for method chaining.
     * <p>
     * The storage level indicates how individual entries should be persisted in the journal.
     *
     * @param storageLevel The log storage level.
     * @return The storage builder.
     */
    public Builder withStorageLevel(StorageLevel storageLevel) {
      journalBuilder.withStorageLevel(storageLevel);
      return this;
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder withDirectory(String directory) {
      journalBuilder.withDirectory(directory);
      return this;
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder withDirectory(File directory) {
      journalBuilder.withDirectory(directory);
      return this;
    }

    /**
     * Sets the journal serializer, returning the builder for method chaining.
     *
     * @param serializer The journal serializer.
     * @return The journal builder.
     */
    public Builder withSerializer(Serializer serializer) {
      journalBuilder.withSerializer(serializer);
      return this;
    }

    /**
     * Sets the maximum segment size in bytes, returning the builder for method chaining.
     * <p>
     * The maximum segment size dictates when logs should roll over to new segments. As entries are written to
     * a segment of the log, once the size of the segment surpasses the configured maximum segment size, the
     * log will create a new segment and append new entries to that segment.
     * <p>
     * By default, the maximum segment size is {@code 1024 * 1024 * 32}.
     *
     * @param maxSegmentSize The maximum segment size in bytes.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
     */
    public Builder withMaxSegmentSize(int maxSegmentSize) {
      journalBuilder.withMaxSegmentSize(maxSegmentSize);
      return this;
    }

    /**
     * Sets the maximum number of allows entries per segment, returning the builder for method chaining.
     * <p>
     * The maximum entry count dictates when logs should roll over to new segments. As entries are written to
     * a segment of the log, if the entry count in that segment meets the configured maximum entry count, the
     * log will create a new segment and append new entries to that segment.
     * <p>
     * By default, the maximum entries per segment is {@code 1024 * 1024}.
     *
     * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxEntriesPerSegment} not greater than the default max entries per
     *                                  segment
     */
    public Builder withMaxEntriesPerSegment(int maxEntriesPerSegment) {
      journalBuilder.withMaxEntriesPerSegment(maxEntriesPerSegment);
      return this;
    }

    /**
     * Sets the entry buffer size.
     * <p>
     * The entry buffer size dictates the number of entries to hold in memory at the tail of the log. Increasing
     * the buffer size increases the number of entries that will be held in memory and thus implies greater memory
     * consumption, but server performance may be improved due to reduced disk access.
     *
     * @param entryBufferSize The entry buffer size.
     * @return The storage builder.
     * @throws IllegalArgumentException if the buffer size is not positive
     */
    public Builder withEntryBufferSize(int entryBufferSize) {
      journalBuilder.withEntryBufferSize(entryBufferSize);
      return this;
    }

    /**
     * Enables flushing buffers to disk when entries are committed to a segment, returning the builder
     * for method chaining.
     * <p>
     * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time
     * an entry is committed in a given segment.
     *
     * @return The storage builder.
     */
    public Builder withFlushOnCommit() {
      return withFlushOnCommit(true);
    }

    /**
     * Sets whether to flush buffers to disk when entries are committed to a segment, returning the builder
     * for method chaining.
     * <p>
     * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time
     * an entry is committed in a given segment.
     *
     * @param flushOnCommit Whether to flush buffers to disk when entries are committed to a segment.
     * @return The storage builder.
     */
    public Builder withFlushOnCommit(boolean flushOnCommit) {
      this.flushOnCommit = flushOnCommit;
      return this;
    }

    @Override
    public RaftLog build() {
      return new RaftLog(journalBuilder.build(), flushOnCommit);
    }
  }
}

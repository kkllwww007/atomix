/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.protocols.gossip.protocol;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Status of anti-entropy exchange, returned by the receiver.
 */
public class AntiEntropyResponse<K> {

  /**
   * Anti-entropy response status.
   */
  public enum Status {
    /**
     * Signifies a successfully processed anti-entropy message.
     */
    PROCESSED,

    /**
     * Signifies a unexpected failure during anti-entropy message processing.
     */
    FAILED,

    /**
     * Signifies a ignored anti-entropy message, potentially due to the receiver operating under high load.
     */
    IGNORED
  }

  private final Status status;
  private final Set<K> keys;

  public AntiEntropyResponse(Status status, Set<K> keys) {
    this.status = status;
    this.keys = keys;
  }

  /**
   * Returns the anti-entropy response status.
   *
   * @return the anti-entropy response status
   */
  public Status status() {
    return status;
  }

  /**
   * Returns a set of keys to update.
   *
   * @return the set of keys that need updating
   */
  public Set<K> keys() {
    return keys;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("status", status)
        .add("keys", keys)
        .toString();
  }
}

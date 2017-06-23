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
package io.atomix.protocols.raft.protocol.messaging;

import com.google.common.base.Preconditions;
import io.atomix.cluster.NodeId;
import io.atomix.cluster.messaging.ClusterCommunicationService;
import io.atomix.cluster.messaging.MessageSubject;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.protocol.CloseSessionRequest;
import io.atomix.protocols.raft.protocol.CloseSessionResponse;
import io.atomix.protocols.raft.protocol.CommandRequest;
import io.atomix.protocols.raft.protocol.CommandResponse;
import io.atomix.protocols.raft.protocol.KeepAliveRequest;
import io.atomix.protocols.raft.protocol.KeepAliveResponse;
import io.atomix.protocols.raft.protocol.MetadataRequest;
import io.atomix.protocols.raft.protocol.MetadataResponse;
import io.atomix.protocols.raft.protocol.OpenSessionRequest;
import io.atomix.protocols.raft.protocol.OpenSessionResponse;
import io.atomix.protocols.raft.protocol.PublishRequest;
import io.atomix.protocols.raft.protocol.QueryRequest;
import io.atomix.protocols.raft.protocol.QueryResponse;
import io.atomix.protocols.raft.protocol.RaftClientProtocol;
import io.atomix.protocols.raft.protocol.ResetRequest;
import io.atomix.serializer.Serializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Raft client protocol that uses a cluster communicator.
 */
public class RaftClientCommunicator implements RaftClientProtocol {
  private final RaftMessageContext context;
  private final Serializer serializer;
  private final ClusterCommunicationService clusterCommunicator;

  public RaftClientCommunicator(Serializer serializer, ClusterCommunicationService clusterCommunicator) {
    this(null, serializer, clusterCommunicator);
  }

  public RaftClientCommunicator(String prefix, Serializer serializer, ClusterCommunicationService clusterCommunicator) {
    this.context = new RaftMessageContext(prefix);
    this.serializer = Preconditions.checkNotNull(serializer, "serializer cannot be null");
    this.clusterCommunicator = Preconditions.checkNotNull(clusterCommunicator, "clusterCommunicator cannot be null");
  }

  private <T, U> CompletableFuture<U> sendAndReceive(MessageSubject subject, T request, MemberId memberId) {
    return clusterCommunicator.sendAndReceive(request, subject, serializer::encode, serializer::decode, NodeId.from(memberId.id()));
  }

  @Override
  public CompletableFuture<OpenSessionResponse> openSession(MemberId memberId, OpenSessionRequest request) {
    return sendAndReceive(context.openSessionSubject, request, memberId);
  }

  @Override
  public CompletableFuture<CloseSessionResponse> closeSession(MemberId memberId, CloseSessionRequest request) {
    return sendAndReceive(context.closeSessionSubject, request, memberId);
  }

  @Override
  public CompletableFuture<KeepAliveResponse> keepAlive(MemberId memberId, KeepAliveRequest request) {
    return sendAndReceive(context.keepAliveSubject, request, memberId);
  }

  @Override
  public CompletableFuture<QueryResponse> query(MemberId memberId, QueryRequest request) {
    return sendAndReceive(context.querySubject, request, memberId);
  }

  @Override
  public CompletableFuture<CommandResponse> command(MemberId memberId, CommandRequest request) {
    return sendAndReceive(context.commandSubject, request, memberId);
  }

  @Override
  public CompletableFuture<MetadataResponse> metadata(MemberId memberId, MetadataRequest request) {
    return sendAndReceive(context.metadataSubject, request, memberId);
  }

  @Override
  public void reset(ResetRequest request) {
    clusterCommunicator.broadcast(request, context.resetSubject(request.session()), serializer::encode);
  }

  @Override
  public void registerPublishListener(long sessionId, Consumer<PublishRequest> listener, Executor executor) {
    clusterCommunicator.addSubscriber(context.publishSubject(sessionId), serializer::decode, listener, executor);
  }

  @Override
  public void unregisterPublishListener(long sessionId) {
    clusterCommunicator.removeSubscriber(context.publishSubject(sessionId));
  }
}

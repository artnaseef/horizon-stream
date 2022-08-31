package org.opennms.horizon.shared.ignite.remoteasync.manager.model;

import org.apache.ignite.lang.IgniteOutClosure;

import java.util.concurrent.CompletableFuture;

public interface RemoteOperation<T> extends IgniteOutClosure<CompletableFuture<T>> {
}

/*
 * Copyright 2018-Present The CloudEvents Authors
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
 *
 */

package io.cloudevents.message.impl;

import io.cloudevents.SpecVersion;
import io.cloudevents.message.BinaryMessageVisitor;
import io.cloudevents.message.BinaryMessageVisitorFactory;
import io.cloudevents.message.MessageVisitException;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * This class implements a BinaryMessage, providing commong logic to most protocol bindings
 * which supports both Binary and Structured mode.
 * Content-type is handled separately using a key not prefixed with CloudEvents header prefix.
 *
 * @param <HK> Header key type
 * @param <HV> Header value type
 */
public abstract class BaseGenericBinaryMessageImpl<HK, HV> extends BaseBinaryMessage {

    private final SpecVersion version;
    private final byte[] body;

    protected BaseGenericBinaryMessageImpl(SpecVersion version, byte[] body) {
        Objects.requireNonNull(version);
        this.version = version;
        this.body = body;
    }

    @Override
    public <T extends BinaryMessageVisitor<V>, V> V visit(BinaryMessageVisitorFactory<T, V> visitorFactory) throws MessageVisitException, IllegalStateException {
        BinaryMessageVisitor<V> visitor = visitorFactory.createBinaryMessageVisitor(this.version);

        // Grab from headers the attributes and extensions
        this.forEachHeader((key, value) -> {
            if (isContentTypeHeader(key)) {
                visitor.setAttribute("datacontenttype", toCloudEventsValue(value));
            } else if (isCloudEventsHeader(key)) {
                String name = toCloudEventsKey(key);
                if (name.equals("specversion")) {
                    return;
                }
                if (this.version.getAllAttributes().contains(name)) {
                    visitor.setAttribute(name, toCloudEventsValue(value));
                } else {
                    visitor.setExtension(name, toCloudEventsValue(value));
                }
            }
        });

        // Set the payload
        if (this.body != null && this.body.length != 0) {
            visitor.setBody(this.body);
        }

        return visitor.end();
    }

    protected abstract boolean isContentTypeHeader(HK key);

    protected abstract boolean isCloudEventsHeader(HK key);

    protected abstract String toCloudEventsKey(HK key);

    protected abstract void forEachHeader(BiConsumer<HK, HV> fn);

    protected abstract String toCloudEventsValue(HV value);

}

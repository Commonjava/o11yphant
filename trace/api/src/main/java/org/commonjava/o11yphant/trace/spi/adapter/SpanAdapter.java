/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.o11yphant.trace.spi.adapter;

import java.util.Map;
import java.util.Optional;

public interface SpanAdapter
{
    default boolean isWrapper()
    {
        return false;
    }

    default SpanAdapter getBaseInstance()
    {
        return this;
    }

    boolean isLocalRoot();

    String getTraceId();

    String getSpanId();

    void addField( String name, Object value );

    Map<String, Object> getFields();

    void close();

    void setInProgressField(String key, Double value);

    Double getInProgressField( String key, Double defValue );

    Double updateInProgressField( String key, Double value );

    void clearInProgressField( String key );

    Map<String, Double> getInProgressFields();

    Optional<SpanContext> getSpanContext();

}

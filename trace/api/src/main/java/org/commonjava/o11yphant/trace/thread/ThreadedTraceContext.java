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
package org.commonjava.o11yphant.trace.thread;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.Optional;

public class ThreadedTraceContext
{
    private final Optional<SpanAdapter> activeSpan;

    public ThreadedTraceContext( Optional<SpanAdapter> activeSpan )
    {
        this.activeSpan = activeSpan;
    }

    public Optional<SpanAdapter> getActiveSpan()
    {
        return activeSpan;
    }
}

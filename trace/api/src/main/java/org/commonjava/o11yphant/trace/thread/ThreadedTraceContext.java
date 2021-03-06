/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
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

import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ThreadedTraceContext
{
    private Optional<SpanAdapter> activeSpan;

    private final Map<String, Timer> timers = new HashMap<>();

    private Map<String, Meter> meters = new HashMap<>();

    private SpanContext<? extends TracerType> spanCtx;

    public ThreadedTraceContext( Optional<SpanAdapter> activeSpan )
    {
        this.activeSpan = activeSpan;
    }

    public Map<String, Timer> getTimers()
    {
        return timers;
    }

    public Map<String, Meter> getMeters()
    {
        return meters;
    }

    public Optional<SpanAdapter> getActiveSpan()
    {
        return activeSpan;
    }
}

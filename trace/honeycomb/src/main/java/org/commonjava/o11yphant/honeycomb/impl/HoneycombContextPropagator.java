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
package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.propagation.W3CPropagationCodec;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpan;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpanContext;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class HoneycombContextPropagator implements ContextPropagator<HoneycombType>
{
    @Override
    public Optional<SpanContext<HoneycombType>> extractContext( Supplier<Map<String, String>> headerSupplier )
    {
        PropagationContext context = W3CPropagationCodec.getInstance().decode( headerSupplier.get() );

        return context.isTraced() ?
                        Optional.of( new HoneycombSpanContext(
                                        new PropagationContext( context.getTraceId(), context.getSpanId(), null,
                                                                null ) ) ) :
                        Optional.empty();
    }

    @Override
    public void injectContext( BiConsumer<String, String> injectorFunction, SpanAdapter spanAdapter )
    {
        HoneycombSpan span = (HoneycombSpan) spanAdapter;
        Optional<Map<String, String>> propMap = W3CPropagationCodec.getInstance().encode( span.getPropagationContext() );
        propMap.ifPresent( stringStringMap -> stringStringMap.forEach( injectorFunction ) );
    }

    @Override
    public Optional<SpanContext<HoneycombType>> extractContext( ThreadedTraceContext threadedContext )
    {
        if ( threadedContext != null && threadedContext.getActiveSpan().isPresent() )
        {
            HoneycombSpan span = (HoneycombSpan) threadedContext.getActiveSpan().get().getBaseInstance();
            return Optional.of( new HoneycombSpanContext(
                            new PropagationContext( span.getTraceId(), span.getSpanId(), null, null ) ) );
        }

        return Optional.empty();
    }
}

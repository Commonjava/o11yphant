/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.o11yphant.honeycomb;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.SpanBuilderFactory;
import io.honeycomb.beeline.tracing.SpanPostProcessor;
import io.honeycomb.beeline.tracing.Tracer;
import io.honeycomb.beeline.tracing.Tracing;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.sampling.Sampling;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Map;

import static org.commonjava.o11yphant.metrics.RequestContextHelper.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.CUMULATIVE_COUNTS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REQUEST_PARENT_SPAN;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REQUEST_PHASE_START;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.TRACE_ID;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.getContext;

@ApplicationScoped
public class HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private HoneyClient client;

    private Beeline beeline;

    @Inject
    private HoneycombContextualizer honeycombContextualizer;

    @Inject
    private HoneycombConfiguration configuration;

    @Inject
    private DefaultTraceSampler traceSampler;

    @Inject
    private DefaultTracingContext tracingContext;

    @Inject
    private EventPostProcessor eventPostProcessor;

    public HoneycombManager()
    {
    }

    @PostConstruct
    public void init()
    {
        if ( configuration.isEnabled() )
        {
            String writeKey = configuration.getWriteKey();
            String dataset = configuration.getDataset();

            logger.debug( "Init Honeycomb manager, dataset: {}", dataset );
            client = new HoneyClient( LibHoney.options().setDataset( dataset ).setWriteKey( writeKey )
                                              .setEventPostProcessor( eventPostProcessor ).build() ); //, new ConsoleTransport( new ResponseObservable() ) );
            LibHoney.setDefault( client );

            SpanPostProcessor postProcessor = Tracing.createSpanProcessor( client, Sampling.alwaysSampler() );
            SpanBuilderFactory factory = Tracing.createSpanBuilderFactory( postProcessor, traceSampler );

            Tracer tracer = Tracing.createTracer( factory, tracingContext );
            beeline = Tracing.createBeeline( tracer, factory );
        }
    }

    public HoneyClient getClient()
    {
        return client;
    }

    public Beeline getBeeline()
    {
        return beeline;
    }

    public Span startRootTracer( String spanName )
    {
        return startRootTracer( spanName, null );
    }

    public Span startRootTracer( String spanName, SpanContext parentContext )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span;
            if ( parentContext != null )
            {
                PropagationContext propContext =
                        new PropagationContext( parentContext.getTraceId(), parentContext.getParentSpanId(), null,
                                                null );

                logger.debug( "Starting root span: {} based on parent context: {}, thread: {}", spanName, propContext, Thread.currentThread().getId() );
                span = beeline.getSpanBuilderFactory()
                              .createBuilder()
                              .setParentContext( propContext )
                              .setSpanName( spanName )
                              .setServiceName( "indy" )
                              .build();
            }
            else
            {
                String traceId = RequestContextHelper.getContext( TRACE_ID );
                String parentId = RequestContextHelper.getContext( REQUEST_PARENT_SPAN );

                span = beeline.getSpanBuilderFactory().createBuilder()
                              //.setParentContext( parentContext )
                              .setSpanName( spanName ).setServiceName( "indy" ).build();
            }

            span = beeline.getTracer().startTrace( span );

            logger.debug( "Started root span: {} (ID: {}, trace ID: {} and parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(),
                          Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public Span startChildSpan( final String spanName )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span;
            if ( tracingContext.isEmpty() )
            {
                logger.debug( "Parent span from context: {} is a NO-OP, starting root trace instead in: {}", tracingContext, Thread.currentThread().getId() );
                span = startRootTracer( spanName );
            }
            else
            {
                span = beeline.startChildSpan( spanName );
            }

            logger.debug( "Child span: {} (id: {}, trace: {}, parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(),
                          Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public void addFields( Span span )
    {
        if ( beeline != null )
        {
            ThreadContext ctx = ThreadContext.getContext( false );
            if ( ctx != null )
            {
                configuration.getFieldSet().forEach( field -> {
                    Object value = getContext( field );
                    if ( value != null )
                    {
                        span.addField( field, value );
                    }
                } );

                Map<String, Double> cumulativeTimings = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );
                if ( cumulativeTimings != null )
                {
                    cumulativeTimings.forEach( ( k, v ) -> span.addField( CUMULATIVE_TIMINGS + "." + k, v ) );
                }

                Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNTS );
                if ( cumulativeCounts != null )
                {
                    cumulativeCounts.forEach( ( k, v ) -> span.addField( CUMULATIVE_COUNTS + "." + k, v ) );
                }
            }
        }
    }

    public void endTrace()
    {
        if ( beeline != null )
        {
            logger.debug( "Ending trace: {}", Thread.currentThread().getId() );
            getBeeline().getTracer().endTrace();
        }
    }

    public Span getActiveSpan()
    {
        if ( beeline != null )
        {
            return beeline.getActiveSpan();
        }
        return null;
    }

    public void addCumulativeField( Span span, String name, long elapse )
    {
        span.addField( name, elapse );

        Map<String, Object> fields = span.getFields();

        // add cumulative timing field
        String cumulativeTimingName = CUMULATIVE_TIMINGS + "." + name;
        Long cumulativeMs = (Long) fields.get( cumulativeTimingName );
        if ( cumulativeMs != null )
        {
            cumulativeMs += elapse;
        }
        else
        {
            cumulativeMs = elapse;
        }
        span.addField( cumulativeTimingName, cumulativeMs );

        // add cumulative counts field
        String cumulativeCountsName = CUMULATIVE_COUNTS + "." + name;
        Integer cumulativeCounts = (Integer) fields.get( cumulativeCountsName );
        if ( cumulativeCounts != null )
        {
            cumulativeCounts += 1;
        }
        else
        {
            cumulativeCounts = 1;
        }
        span.addField( cumulativeCountsName, cumulativeCounts );

        // update average
        String averageName = AVERAGE_TIME_MS + "." + name;
        span.addField( averageName, ( cumulativeMs / cumulativeCounts ) );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulativeMs: {}, cumulativeCounts: {}",
                      span, name, elapse, cumulativeMs, cumulativeCounts );
    }

    public void addStartField( Span span, String name, long begin )
    {
        String startFieldName = REQUEST_PHASE_START + "." + name;
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.addField( startFieldName, begin );
    }

    public void addEndField( Span span, String name, long end )
    {
        String startFieldName = REQUEST_PHASE_START + "." + name;
        Long begin = (Long) span.getFields().get( startFieldName );
        if ( begin == null )
        {
            logger.warn( "Failed to get START field, span: {}, name: {}", span, name );
            return;
        }
        logger.trace( "addEndField, span: {}, name: {}, end: {}", span, name, end );
        long elapse = end - begin;
        addCumulativeField( span, name, elapse );
        span.addField( startFieldName, null ); // clear start field
    }
}

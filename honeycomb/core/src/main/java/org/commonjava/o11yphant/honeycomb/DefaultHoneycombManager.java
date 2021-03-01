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
package org.commonjava.o11yphant.honeycomb;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.libhoney.EventPostProcessor;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.commonjava.o11yphant.metrics.MetricsConstants.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.MAX_TIME_MS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_PHASE_START;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.getContext;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@Alternative
@ApplicationScoped
public class DefaultHoneycombManager
                extends HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombContextualizer honeycombContextualizer;

    @Inject
    private Instance<CustomTraceIdProvider> traceIdProviderInstance;

    @Inject
    private Instance<RootSpanFields> rootSpanFieldsInstance;

    @Inject
    public DefaultHoneycombManager( HoneycombConfiguration honeycombConfiguration,
                                    DefaultTraceSampler defaultTraceSampler,
                                    DefaultTracingContext defaultTracingContext,
                                    EventPostProcessor defaultEventPostProcessor )
    {
        super( honeycombConfiguration, defaultTraceSampler, defaultTracingContext, defaultEventPostProcessor );

        if ( rootSpanFieldsInstance != null )
        {
            rootSpanFieldsInstance.forEach( instance -> rootSpanFieldsList.add( instance ) );
        }
    }

    public DefaultHoneycombManager( HoneycombConfiguration honeycombConfiguration,
                                    DefaultTraceSampler defaultTraceSampler )
    {
        super( honeycombConfiguration, defaultTraceSampler );
    }

    public void addSpanFields ( List<RootSpanFields> fields )
    {
        this.rootSpanFieldsList = fields;
    }

    @PostConstruct
    public void init()
    {
        super.init();
    }

    @Override
    protected CustomTraceIdProvider getCustomTraceIdProvider()
    {
        if ( traceIdProviderInstance != null && !traceIdProviderInstance.isUnsatisfied() )
        {
            return traceIdProviderInstance.get();
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

                Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNT );
                if ( cumulativeCounts != null )
                {
                    cumulativeCounts.forEach( ( k, v ) -> span.addField( CUMULATIVE_COUNT + "." + k, v ) );
                }
            }

            addRootSpanFields( span ); // add custom root span fields via RootSpanFields
        }
    }

    public Optional<Timer> getSpanTimer( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Timer timer = spanContext.getTimer( name );
            if ( timer == null )
            {
                timer = new Timer();
                spanContext.putTimer( name, timer );
            }
            return Optional.of( timer );
        }
        return Optional.empty();
    }

    public Optional<Timer.Context> startSpanTimer( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Timer timer = getSpanTimer( name ).get();
            if ( timer != null )
            {
                return Optional.of( timer.time() );
            }
        }
        return Optional.empty();
    }

    public Optional<Meter> getSpanMeter( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Meter meter = spanContext.getMeter( name );
            if ( meter == null )
            {
                meter = new Meter();
                spanContext.putMeter( name, meter );
            }
            return Optional.of( meter );
        }
        return Optional.empty();
    }

    /**
     * Add cumulative fields to specified span, i.e., cumulative-timings/count/avg/max.
     */
    public void addCumulativeField( Span span, String name, long elapse )
    {
        final Map<String, Object> fields = span.getFields();

        // cumulative timing
        String cumulativeTimingName = name( name, CUMULATIVE_TIMINGS );
        Long cumulativeMs = (Long) fields.getOrDefault( cumulativeTimingName, 0L );
        cumulativeMs += elapse;
        span.addField( cumulativeTimingName, cumulativeMs );

        // cumulative count
        String cumulativeCountName = name( name, CUMULATIVE_COUNT );
        Integer cumulativeCount = (Integer) fields.getOrDefault( cumulativeCountName, 0 );
        cumulativeCount += 1;
        span.addField( cumulativeCountName, cumulativeCount );

        // max
        String maxTimingName = name( name, MAX_TIME_MS );
        Long max = (Long) fields.getOrDefault( maxTimingName, 0L );
        if ( elapse > max )
        {
            span.addField( maxTimingName, elapse );
        }

        // average
        String averageName = name( name, AVERAGE_TIME_MS );
        span.addField( averageName, ( cumulativeMs / cumulativeCount ) );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulative-ms: {}, count: {}", span, name,
                      elapse, cumulativeMs, cumulativeCount );
    }

    public void addStartField( Span span, String name, long begin )
    {
        String startFieldName = name( name, REQUEST_PHASE_START );
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.addField( startFieldName, begin );
    }

    public void addEndField( Span span, String name, long end )
    {
        String startFieldName = name( name, REQUEST_PHASE_START );
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

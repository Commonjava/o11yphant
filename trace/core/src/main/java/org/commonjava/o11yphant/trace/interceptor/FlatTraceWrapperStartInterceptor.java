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
package org.commonjava.o11yphant.trace.interceptor;

import org.commonjava.o11yphant.metrics.annotation.MetricWrapperStart;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.util.InterceptorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;

@Interceptor
@MetricWrapperStart
public class FlatTraceWrapperStartInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TracerConfiguration config;

    @Inject
    private TraceManager traceManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        String name = InterceptorUtils.getMetricNameFromContext( context );
        logger.trace( "START: trace metrics-start wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: trace metrics-start wrapper: {}", name );
            return context.proceed();
        }

        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( context.getMethod() ) < 1 )
        {
            logger.trace( "SKIP: trace metrics-start wrapper (no span name or span not configured: {})", name );
            return context.proceed();
        }

        long begin = currentTimeMillis();
        try
        {
            Optional<SpanAdapter> span = TraceManager.getActiveSpan();
            span.ifPresent( s->traceManager.addStartField( s, name, begin ) );
        }
        finally
        {
            logger.trace( "END: trace metrics-start wrapper: {}", name );
        }

        return context.proceed();
    }

}

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
package org.commonjava.o11yphant.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.o11yphant.metrics.RequestContextConstants.*;

public interface TracerConfiguration
{
    Integer DEFAULT_BASE_SAMPLE_RATE = 100;

    Set<String> DEFAULT_FIELDS = Collections.unmodifiableSet(
                    new HashSet<>( Arrays.asList( CONTENT_TRACKING_ID, HTTP_METHOD, HTTP_STATUS, CLIENT_ADDR,
                                                  PATH, PACKAGE_TYPE, REST_ENDPOINT_PATH, REQUEST_LATENCY_MILLIS ) ) );

    default boolean isThreadSpanEnabled(){
        return true;
    }

    boolean isEnabled();

    boolean isConsoleTransport();

    /**
     * Service/application name, e,g. indy.
     */
    String getServiceName();

    /**
     * Common context fields which will be injected as span fields. HoneycombManager.getContext
     * must be implemented accordingly in order to get their values.
     */
    default Set<String> getFieldSet() { return DEFAULT_FIELDS; }

    /**
     * Env vars which will be injected as span fields.
     */
    default String getEnvironmentMappings() { return null; }

    /**
     * Connection pools whose status values will be injected as span fields, e.g, active-connections, etc.
     */
    default String getCPNames() { return null; }

    /**
     * Node id of clustered service.
     */
    String getNodeId();

    /**
     * Span rates are to control the sample rate for spans. It returns a map. The keys are so called 'function',
     * 'classifier', 'category', and so on. They are like 'a', 'a.b', etc, which is in line with the java package.class
     * so that we can control the rates by levels. The priority is bottom up, e.g, 'a.b' over 'a'.
     *
     * This method is used in below two getSampleRate methods. Normally the subclass only implement this method
     * and use these two methods when it comes to building customized TraceSampler.
     */
    default Map<String, Integer> getSpanRates()
    {
        return Collections.emptyMap();
    }

    /**
     * Base sample rate is used when none specific rate found for a function, or so called 'classifier'.
     */
    default Integer getBaseSampleRate()
    {
        return DEFAULT_BASE_SAMPLE_RATE;
    }

    default int getSampleRate( Method method )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        Map<String, Integer> spanRates = getSpanRates();
        if ( !spanRates.isEmpty() )
        {
            String[] keys = { method.getName(), method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                            method.getDeclaringClass().getName() + "." + method.getName(),
                            method.getDeclaringClass().getSimpleName(), method.getDeclaringClass().getName() };

            for ( String key : keys )
            {
                Integer rate = spanRates.get( key );
                if ( rate != null )
                {
                    logger.trace( "Found sampling rate for: {} = {}", key, rate );
                    return rate;
                }
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", method, getBaseSampleRate() );
        return getBaseSampleRate();
    }

    default Integer getSampleRate( final String classifier )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        Map<String, Integer> spanRates = getSpanRates();
        Integer rate = spanRates.get( classifier );
        if ( rate != null )
        {
            logger.trace( "Found sampling rate for: {} = {}", classifier, rate );
            return rate;
        }

        String[] parts = classifier.split( "\\." );
        for ( String part : parts )
        {
            rate = spanRates.get( part );
            if ( rate != null )
            {
                logger.trace( "Found sampling rate for: {} = {}", part, rate );
                return rate;
            }
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = parts.length; i > 0; i-- )
        {
            sb.setLength( 0 );
            for ( int j = 0; j < i; j++ )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( '.' );
                }
                sb.append( parts[j] );
            }

            rate = spanRates.get( sb.toString() );
            if ( rate != null )
            {
                logger.trace( "Found sampling rate for: {} = {}", sb, rate );
                return rate;
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", classifier, getBaseSampleRate() );
        return getBaseSampleRate();
    }

}

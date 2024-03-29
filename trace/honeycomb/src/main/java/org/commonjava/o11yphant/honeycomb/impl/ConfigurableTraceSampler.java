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

import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.TrafficClassifier;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// FIXME: This is probably broken, based on how sampling actually uses trace-id or a Span instance. It's also probably slow.
public class ConfigurableTraceSampler
        implements TraceSampler<String>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String SAMPLE_OVERRIDE = "honeycomb.sample-override";

    private final Map<String, Integer> sampleRateCache = new ConcurrentHashMap<>();

    private TrafficClassifier classifier;

    private TracerConfiguration config;

    public ConfigurableTraceSampler( TrafficClassifier classifier, TracerConfiguration config )
    {
        this.classifier = classifier;
        this.config = config;
    }

    private final Function<String, Integer> samplerFunction = input -> {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx == null )
        {
            logger.trace( "No ThreadContext for functional diagnosis; skipping span: {}", input );
            return config.getBaseSampleRate();
        }

        if ( ctx.get( SAMPLE_OVERRIDE ) != null )
        {
            logger.trace( "Including span via override (span: {})", input );
            return 1;
        }

        Optional<List<String>> functionClassifiers = classifier.getCachedFunctionClassifiers();
        Integer rate = config.getSampleRate( input );

        if ( Objects.equals( rate, config.getBaseSampleRate() ) && functionClassifiers.isPresent() )
        {
            Optional<Integer> mostAggressive = functionClassifiers.get()
                                                                  .stream()
                                                                  .map( config::getSampleRate )
                                                                  .filter( theRate -> theRate > 0 )
                                                                  .min( ( one, two ) -> two - one );

            if ( mostAggressive.isPresent() )
            {
                rate = mostAggressive.get();
            }
        }

        return rate;
    };

    /**
     * Decides whether to sample the input.
     * If it returns 0, it should not be sampled.
     * If positive then it should be sampled and the concrete value represents the {@code sampleRate}.
     *
     * @param input to test. According to {@link io.honeycomb.beeline.tracing.SpanBuilderFactory} this is the traceId.
     * @return Positive int if the input is to be sampled.
     */
    @Override
    public int sample( final String input )
    {
        return sampleRateCache.computeIfAbsent( input, samplerFunction );
    }
}

/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/o11yphant)
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

import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.TrafficClassifier;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static org.commonjava.o11yphant.honeycomb.util.InterceptorUtils.SAMPLE_OVERRIDE;

@ApplicationScoped
public class DefaultTraceSampler
                implements TraceSampler<String>
{
    private final Random random = new Random();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TrafficClassifier classifier;

    @Inject
    private HoneycombConfiguration configuration;

    @Override
    public int sample( final String input )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx == null )
        {
            logger.debug( "No ThreadContext for functional diagnosis; skipping span: {}", input );
            return 0;
        }

        if ( ctx.get( SAMPLE_OVERRIDE ) != null )
        {
            logger.debug( "Including span via override (span: {})", input );
            return 1;
        }

        Optional<List<String>> functionClassifiers = classifier.getCachedFunctionClassifiers();
        Integer rate = configuration.getSampleRate( input );

        if ( Objects.equals( rate, configuration.getBaseSampleRate() ) && functionClassifiers.isPresent() )
        {
            Optional<Integer> mostAggressive = functionClassifiers.get()
                                                                  .stream()
                                                                  .map( classifier -> configuration.getSampleRate(
                                                                                  classifier ) )
                                                                  .filter( theRate -> theRate > 0 )
                                                                  .min( ( one, two ) -> two - one );

            if ( mostAggressive.isPresent() )
            {
                rate = mostAggressive.get();
            }
        }

        if ( rate == 1 || Math.abs( random.nextInt() ) % rate == 0 )
        {
            logger.debug( "Including span due to sampling rate: {} (span: {})", rate, input );
            return 1;
        }

        logger.debug( "Skipping span due to sampling rate: {} (span: {})", rate, input );
        return 0;
    }
}

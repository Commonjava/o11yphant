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
package org.commonjava.o11yphant.metrics.sli;

import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.api.healthcheck.CompoundHealthCheck;
import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class GoldenSignalsMetricSet
                implements MetricSet, CompoundHealthCheck
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Map<String, GoldenSignalsFunctionMetrics> functionMetrics = new HashMap<>();

    protected abstract Collection<String> getFunctions();

    public GoldenSignalsMetricSet()
    {
        getFunctions().forEach( function -> {
            logger.info( "Wiring SLI metrics for: {}", function );
            functionMetrics.put( function, new GoldenSignalsFunctionMetrics( function ) );
        } );
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        Map<String, Metric> metrics = new HashMap<>();
        functionMetrics.values().forEach( ms -> metrics.putAll( ms.getMetrics() ) );

        return metrics;
    }

    public Optional<GoldenSignalsFunctionMetrics> function( String name )
    {
        return functionMetrics.containsKey( name ) ? Optional.of( functionMetrics.get( name ) ) : Optional.empty();
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks()
    {
        Map<String, HealthCheck> checks = new HashMap<>();
        functionMetrics.forEach( ( key, value ) -> checks.put( "sli.golden." + key, value.getHealthCheck() ) );
        return checks;
    }

    public Map<String, GoldenSignalsFunctionMetrics> getFunctionMetrics() {
        return functionMetrics;
    }
}

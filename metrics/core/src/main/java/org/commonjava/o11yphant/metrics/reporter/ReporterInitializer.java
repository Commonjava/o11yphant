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
package org.commonjava.o11yphant.metrics.reporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.commonjava.o11yphant.metrics.conf.ConsoleConfig;
import org.commonjava.o11yphant.metrics.conf.GraphiteConfig;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.o11yphant.metrics.conf.MetricsConfig.REPORTER_CONSOLE;
import static org.commonjava.o11yphant.metrics.conf.MetricsConfig.REPORTER_GRAPHITE;

@ApplicationScoped
public class ReporterInitializer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final static String FILTER_JVM = "jvm";

    @Inject
    private MetricsConfig config;

    @Inject
    private MetricRegistry metrics;

    public ReporterInitializer()
    {
    }

    private volatile boolean initiated;

    @PostConstruct
    public void init()
            throws Exception
    {
        if ( initiated )
        {
            logger.warn( "Metrics reporter already initialized" );
            return;
        }

        String reporter = config.getReporter();
        if ( isBlank( reporter ) )
        {
            initConsoleReporter( metrics, config.getConsoleConfig() );
            initiated = true;
            return;
        }

        if ( reporter.contains( REPORTER_GRAPHITE ) )
        {
            initGraphiteReporterForSimpleMetric( metrics, config.getGraphiteConfig() );
            initGraphiteReporterForJVMMetric( metrics, config.getGraphiteConfig() );
        }

        if ( reporter.contains( REPORTER_CONSOLE ) )
        {
            initConsoleReporter( metrics, config.getConsoleConfig() );
        }

        initiated = true;
    }

    private boolean isJvmMetric( String name )
    {
        return name.contains( FILTER_JVM );
    }

    private boolean isApplicationMetric( String name )
    {
        return !isJvmMetric( name );
    }


    private void initConsoleReporter( MetricRegistry metrics, ConsoleConfig config )
    {
        try (ConsoleReporter reporter = ConsoleReporter.forRegistry( metrics ).build())
        {
            reporter.start( config.getConsolePeriodInSeconds(), TimeUnit.SECONDS );
        }
    }

    private void initGraphiteReporterForSimpleMetric( MetricRegistry metrics, GraphiteConfig config )
    {
        final Graphite graphite =
                new Graphite( new InetSocketAddress( config.getGraphiteHostName(), config.getGraphitePort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGraphitePrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( ( name, metric ) -> isApplicationMetric( name ) )
                                                          .build( graphite );
        reporter.start( config.getGraphitePeriodInSeconds(), TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForJVMMetric( MetricRegistry metrics, GraphiteConfig config )
    {
        final Graphite graphite =
                new Graphite( new InetSocketAddress( config.getGraphiteHostName(), config.getGraphitePort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGraphitePrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( ( name, metric ) -> isJvmMetric( name ) )
                                                          .build( graphite );
        reporter.start( config.getGraphiteJVMPeriodInSeconds(), TimeUnit.SECONDS );
    }

}

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
package org.commonjava.o11yphant.metrics.util;

import com.codahale.metrics.Metric;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.DefaultMetricRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;

import java.util.HashMap;
import java.util.Map;

public class MetricUtils
{
    // for test
    public static DefaultMetricRegistry newDefaultMetricRegistry()
    {
        return new DefaultMetricRegistry( new com.codahale.metrics.MetricRegistry(), new HealthCheckRegistry() );
    }

    public static MetricSet wrapGaugeSet( Map<String, Metric> metrics )
    {
        return new WrappedMetricSet( metrics );
    }

    private static class WrappedMetricSet
                    implements MetricSet
    {
        private final Map<String, Metric> metrics;

        public WrappedMetricSet( Map<String, Metric> metrics )
        {
            this.metrics = metrics;
        }

        @Override
        public Map<String, org.commonjava.o11yphant.metrics.api.Metric> getMetrics()
        {
            Map<String, org.commonjava.o11yphant.metrics.api.Metric> result = new HashMap<>();
            for(Map.Entry<String, Metric> metricEntry: metrics.entrySet() )
            {
                result.put( metricEntry.getKey(),
                            (Gauge<Object>) () -> ( (com.codahale.metrics.Gauge<?>) metricEntry.getValue() ).getValue() );
            }

            return result;
        }

        @Override
        public void reset()
        {
            // No-op
        }
    }
}

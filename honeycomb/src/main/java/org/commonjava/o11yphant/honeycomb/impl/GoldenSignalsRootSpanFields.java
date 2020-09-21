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
package org.commonjava.o11yphant.honeycomb.impl;

import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GoldenSignalsRootSpanFields
                implements RootSpanFields
{
    private GoldenSignalsMetricSet goldenSignalsMetricSet;

    @Inject
    public GoldenSignalsRootSpanFields( GoldenSignalsMetricSet goldenSignalsMetricSet )
    {
        this.goldenSignalsMetricSet = goldenSignalsMetricSet;
    }

    @Override
    public Map<String, Object> get()
    {
        final Map<String, Object> ret = new HashMap<>();

        final Map<String, Metric> metrics = goldenSignalsMetricSet.getMetrics();
        metrics.forEach( ( k, v ) -> {
            Object value = null;
            if ( v instanceof Gauge )
            {
                value = ( (Gauge) v ).getValue();
            }
            else if ( v instanceof Timer )
            {
                value = ( (Timer) v ).getSnapshot().get95thPercentile();
            }
            else if ( v instanceof Meter )
            {
                value = ( (Meter) v ).getOneMinuteRate();
            }
            ret.put( "golden." + k, value );
        } );
        return ret;
    }
}

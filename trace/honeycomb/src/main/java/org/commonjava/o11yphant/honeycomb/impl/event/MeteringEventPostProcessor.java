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
package org.commonjava.o11yphant.honeycomb.impl.event;

import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.eventdata.EventData;
import org.commonjava.o11yphant.metrics.MetricsManager;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.util.NameUtils;

import static org.commonjava.o11yphant.metrics.MetricsConstants.METER;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getDefaultName;

public class MeteringEventPostProcessor
                implements EventPostProcessor
{
    private final MetricsManager metricsManager;

    private final MetricsConfig metricsConfig;

    private final static String TRANSFER_HONEYCOMB_EVENT = "transferred.honeycomb.event";

    public MeteringEventPostProcessor( MetricsManager metricsManager, MetricsConfig metricsConfig )
    {
        this.metricsManager = metricsManager;
        this.metricsConfig = metricsConfig;
    }

    @Override
    public void process( EventData<?> eventData )
    {
        if ( metricsConfig != null && metricsManager != null )
        {
            String name = NameUtils.getName( metricsConfig.getNodePrefix(), TRANSFER_HONEYCOMB_EVENT,
                                             getDefaultName( MeteringEventPostProcessor.class, "process" ), METER );
            Meter meter = metricsManager.getMeter( name );
            meter.mark();
        }
    }
}

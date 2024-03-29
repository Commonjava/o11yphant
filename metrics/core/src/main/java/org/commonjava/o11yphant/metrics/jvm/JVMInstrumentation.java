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
package org.commonjava.o11yphant.metrics.jvm;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.commonjava.o11yphant.metrics.api.MetricSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.o11yphant.metrics.util.MetricUtils.wrapGaugeSet;

@ApplicationScoped
public class JVMInstrumentation
{
    private static final String JVM_MEMORY = "jvm.memory";

    private static final String JVM_GARBAGE = "jvm.garbage";

    private static final String JVM_THREADS = "jvm.threads";

    private static final String JVM_FILES = "jvm.files";

    private static final String JVM_BUFFERS = "jvm.buffers";

    private static final String JVM_CLASSLOADING = "jvm.classloading";

    private final MetricRegistry registry;

    private final MemoryUsageGaugeSet memoryUsageGaugeSet = new MemoryUsageGaugeSet();

    private final ThreadStatesGaugeSet threadStatesGaugeSet = new CachedThreadStatesGaugeSet( 60, TimeUnit.SECONDS );

    @Inject
    public JVMInstrumentation( MetricRegistry registry )
    {
        this.registry = registry;
    }

    public void registerJvmMetric( String nodePrefix )
    {
        registry.register( name( nodePrefix, JVM_MEMORY ), memoryUsageGaugeSet );
        registry.register( name( nodePrefix, JVM_GARBAGE ), new GarbageCollectorMetricSet() );
        registry.register( name( nodePrefix, JVM_THREADS ), threadStatesGaugeSet );
        registry.register( name( nodePrefix, JVM_FILES ), new FileDescriptorRatioGauge() );
        registry.register( name( nodePrefix, JVM_CLASSLOADING ), new ClassLoadingGaugeSet() );
        registry.register( name( nodePrefix, JVM_BUFFERS ),
                           new BufferPoolMetricSet( ManagementFactory.getPlatformMBeanServer() ) );
    }

    public MetricSet getMemoryUsageGaugeSet()
    {
        return wrapGaugeSet( memoryUsageGaugeSet.getMetrics() );
    }

    public MetricSet getThreadStatesGaugeSet()
    {
        return wrapGaugeSet( threadStatesGaugeSet.getMetrics() );
    }

}

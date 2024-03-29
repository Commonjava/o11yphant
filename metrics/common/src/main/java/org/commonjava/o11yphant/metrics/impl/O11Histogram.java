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
package org.commonjava.o11yphant.metrics.impl;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import org.commonjava.o11yphant.metrics.api.Histogram;
import org.commonjava.o11yphant.metrics.api.Snapshot;

public class O11Histogram implements Histogram
{
    private final com.codahale.metrics.Histogram codehaleHistogram;

    public O11Histogram()
    {
        this.codehaleHistogram = new com.codahale.metrics.Histogram( new ExponentiallyDecayingReservoir() );
    }

    public O11Histogram( com.codahale.metrics.Histogram codehaleHistogram )
    {
        this.codehaleHistogram = codehaleHistogram;
    }
    @Override
    public void update( int value )
    {
        codehaleHistogram.update( value );
    }

    @Override
    public void update( long value )
    {
        codehaleHistogram.update( value );
    }

    @Override
    public long getCount()
    {
        return codehaleHistogram.getCount();
    }

    @Override
    public Snapshot getSnapshot()
    {
        return new O11Snapshot( codehaleHistogram.getSnapshot() );
    }

    public com.codahale.metrics.Histogram getCodehaleHistogram()
    {
        return codehaleHistogram;
    }
}

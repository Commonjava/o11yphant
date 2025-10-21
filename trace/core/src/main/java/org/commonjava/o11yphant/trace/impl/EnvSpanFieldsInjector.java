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
package org.commonjava.o11yphant.trace.impl;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Stream;

/**
 * Allow configuration of a set of environment variables to extract from the system and add to the Span as fields.
 */
@ApplicationScoped
public class EnvSpanFieldsInjector
                implements SpanFieldsInjector
{
    @Inject
    private TracerConfiguration configuration;

    @Override
    public void decorateSpanAtStart( SpanAdapter span )
    {
        String environmentMappings = configuration.getEnvironmentMappings();

        String[] mappings = environmentMappings == null ? new String[0] : environmentMappings.split( "\\s*,\\s*" );
        Stream.of( mappings ).forEach( kv -> {
            String[] keyAlias = kv.split( "\\s*=\\s*" );
            if ( keyAlias.length > 1 )
            {
                String value = System.getenv( keyAlias[0].trim() );
                if ( StringUtils.isEmpty( value ) )
                {
                    value = "Unknown";
                }

                span.addField( keyAlias[1].trim(), value );
            }
        } );
    }
}

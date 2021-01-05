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
package org.commonjava.o11yphant.metrics;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public class RequestContextConstants
{
    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface Header{}

    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface MDC{}

    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface Thread{}

    //
    public static final String HEADER_COMPONENT_ID = "component-id";

    @Thread
    public static final String RAW_IO_WRITE_NANOS = "raw-io-write-nanos";

    @Thread
    public static final String RAW_IO_READ_NANOS = "raw-io-read-nanos";

    @Thread
    public static final String END_NANOS = "latency-end-nanos";

    @Thread @MDC
    public static final String REST_CLASS_PATH = "REST-class-path";

    @Thread @MDC
    public static final String REST_METHOD_PATH = "REST-method-path";

    @Thread @MDC
    public static final String REST_ENDPOINT_PATH = "REST-endpoint-path";

    @Thread @MDC
    public static final String REST_CLASS = "REST-class";

    @MDC
    public static final String CONTENT_TRACKING_ID = "tracking-id";

    @MDC
    public static final String ACCESS_CHANNEL = "access-channel";

    @MDC
    public static final String REQUEST_LATENCY_NS = "request-latency-ns";

    @Thread
    public static final String REQUEST_LATENCY_MILLIS = "latency_ms";

    @MDC
    public static final String REQUEST_PHASE = "request-phase";

    @Thread @MDC
    public static final String PACKAGE_TYPE = "package-type";

    @Thread @MDC
    public static final String METADATA_CONTENT = "metadata-content";

    @Thread @MDC
    public static final String CONTENT_ENTRY_POINT = "content-entry-point";

    @Thread @MDC
    public static final String HTTP_METHOD = "http-method";

    @Thread @MDC
    public static final String HTTP_REQUEST_URI = "http-request-uri";

    @Thread @MDC
    public static final String PATH = "path";

    @Thread @MDC
    public static final String HTTP_STATUS = "http-status";

    @Header @MDC
    public static final String COMPONENT_ID = HEADER_COMPONENT_ID;

    @Header
    public static final String X_FORWARDED_FOR = "x-forwarded-for";

    @Header @Thread @MDC
    public static final String EXTERNAL_ID = "external-id";

    @Header
    public static final String SPAN_ID_HEADER = "span-id";

    @Thread @MDC
    public static final String REQUEST_PARENT_SPAN = "parent-span";

    @Thread @MDC
    public static final String CLIENT_ADDR = "client-addr";

    @Thread @MDC
    public static final String INTERNAL_ID = "internal-id";

    @Thread @MDC
    public static final String TRACE_ID = "trace-id";

    @Thread
    public static final String FORCE_METERED = "force-metered";

    @Thread
    public static final String IS_METERED = "is-metered";

    public static final String REQUEST_PHASE_START = "start";

    public static final String REQUEST_PHASE_END = "end";

}

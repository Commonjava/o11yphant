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
package org.commonjava.o11yphant.honeycomb.util;

/**
 * Used to register the headers transferred from client
 * and then trace in server
 */
public class HeaderMetricConstants
{
    public final static String HEADER_INDY_CLIENT_TRACE_ID = "Indy-Client-Trace-Id";

    public final static String HEADER_INDY_CLIENT_SPAN_ID = "Indy-Client-Span-Id";

    public static String[] HEADERS = { HEADER_INDY_CLIENT_TRACE_ID, HEADER_INDY_CLIENT_SPAN_ID };
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.http.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class AcceptDatetimeTest {

    @Test
    public void testDatetime() {
        final AcceptDatetime datetime = AcceptDatetime.valueOf("Mon, 1 May 2017 13:43:22 GMT");
        assertEquals("2017-05-01T13:43:22Z", datetime.toString(), "Incorrect datetime string!");
        assertEquals("2017-05-01T13:43:22Z", datetime.getInstant().toString(), "Incorrect stringified datetime!");
    }

    @Test
    public void testInvalidDatetime() {
        assertNull(AcceptDatetime.valueOf("Mon, 2 May 2017 13:43:22 GMT"), "Unexpected invalid datetime!");
    }

    @Test
    public void testNullDatetime() {
        assertNull(AcceptDatetime.valueOf(null), "Check null datetime");
    }
}

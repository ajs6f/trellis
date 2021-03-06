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
package org.trellisldp.vocabulary;

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * JSON-LD vocabulary terms.
 *
 * @see <a href="https://www.w3.org/ns/json-ld">The JSON-LD Vocabulary</a>
 *
 * @author acoburn
 */
public final class JSONLD {

    /* Namespace */
    private static final String URI = "http://www.w3.org/ns/json-ld#";

    /* Profiles */
    public static final IRI context = createIRI(getNamespace() + "context");

    /* Extra definitions */
    public static final IRI compacted = createIRI(getNamespace() + "compacted");
    public static final IRI compacted_flattened = createIRI(getNamespace() + "compacted-flattened");
    public static final IRI expanded = createIRI(getNamespace() + "expanded");
    public static final IRI expanded_flattened = createIRI(getNamespace() + "expanded-flattened");
    public static final IRI flattened = createIRI(getNamespace() + "flattened");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private JSONLD() {
        // prevent instantiation
    }
}

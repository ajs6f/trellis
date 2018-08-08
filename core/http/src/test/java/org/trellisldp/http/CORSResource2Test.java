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
package org.trellisldp.http;

import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CORSResource2Test extends JerseyTest {

    protected static final IOService ioService = new JenaIOService();

    private static final int timestamp = 1496262729;

    private static final Instant time = ofEpochSecond(timestamp);

    private static final RDF rdf = getInstance();

    private static final IRI agent = rdf.createIRI("user:agent");

    private static final BlankNode bnode = rdf.createBlankNode();

    private static final String REPO1 = "repo1";

    private static final String BASE_URL = "http://example.org/";

    private static final String ORIGIN = "http://example.com";

    private static final String RANDOM_VALUE = "randomValue";

    private static final String RESOURCE_PATH = REPO1 + "/resource";
    private static final String CHILD_PATH = RESOURCE_PATH + "/child";
    private static final String BINARY_PATH = REPO1 + "/binary";
    private static final String NON_EXISTENT_PATH = REPO1 + "/nonexistent";

    private static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH);
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX + REPO1);
    private static final IRI binaryIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + BINARY_PATH);
    private static final IRI nonexistentIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH);
    private static final IRI childIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + CHILD_PATH);

    protected static final Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Append);
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Control);
    }

    private final MementoService mementoService = new NoopMementoService();

    @Mock
    protected ResourceService mockResourceService;

    @Mock
    protected BinaryService mockBinaryService;

    @Mock
    protected AccessControlService mockAccessControlService;

    @Mock
    protected AgentService mockAgentService;

    @Mock
    private Resource mockResource, mockBinaryResource;

    @Mock
    private ServiceBundler mockBundler;

    @Override
    public Application configure() {

        initMocks(this);

        final ResourceConfig config = new ResourceConfig();
        config.register(new LdpResource(mockBundler));
        config.register(new CrossOriginResourceSharingFilter(asList(ORIGIN),
                    asList("GET", "HEAD", "PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Language", "Accept-Datetime"),
                    asList("Accept-Patch"), true, 180));
        return config;
    }

    @Override
    protected Client getClient() {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newClient(clientConfig);
    }

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
    }

    @BeforeEach
    public void setUpMocks() {
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getIOService()).thenReturn(ioService);
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getAgentService()).thenReturn(mockAgentService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);

        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(root))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(childIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.get(eq(binaryIdentifier))).thenAnswer(inv -> completedFuture(mockBinaryResource));
        when(mockResourceService.get(eq(nonexistentIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.generateIdentifier()).thenReturn(RANDOM_VALUE);

        when(mockAgentService.asAgent(anyString())).thenReturn(agent);

        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(allModes);

        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.isMemento()).thenReturn(false);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(BASE_URL)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + iri.substring(BASE_URL.length()));
                }
            }
            return term;
        });
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(TRELLIS_DATA_PREFIX)) {
                    return rdf.createIRI(BASE_URL + iri.substring(TRELLIS_DATA_PREFIX.length()));
                }
            }
            return term;
        });

        when(mockResourceService.unskolemize(any(IRI.class)))
            .thenAnswer(inv -> {
                final String uri = ((IRI) inv.getArgument(0)).getIRIString();
                if (uri.startsWith(TRELLIS_BNODE_PREFIX)) {
                    return bnode;
                }
                return (IRI) inv.getArgument(0);
            });

        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.create(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(),
                        any())).thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));
    }

    @Test
    public void testGetCORSInvalid() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
    }

    @Test
    public void testGetCORS() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testGetCORSSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
    }

    @Test
    public void testOptionsPreflightSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertTrue(res.getHeaderString("Access-Control-Allow-Headers").contains("accept"));
        assertFalse(res.getHeaderString("Access-Control-Allow-Methods").contains("POST"));
        assertTrue(res.getHeaderString("Access-Control-Allow-Methods").contains("PATCH"));
    }

    @Test
    public void testCorsPreflight() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language, Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));

        final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
            .collect(toList());
        assertEquals(4L, headers.size());
        assertTrue(headers.contains("accept"));
        assertTrue(headers.contains("link"));
        assertTrue(headers.contains("content-type"));
        assertTrue(headers.contains("accept-datetime"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
    }

    @Test
    public void testCorsPreflightNoRequestHeaders() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
    }

    @Test
    public void testCorsPreflightNoMatch() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));

        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
    }

    @Test
    public void testOptionsPreflightInvalid() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", "http://foo.com")
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testOptionsPreflightInvalid2() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link, Bar").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testOptionsPreflightInvalid3() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "FOO")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

}
package org.cyberborean.rdfbeans.datatype;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.entities.DatatypeTestClass;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListTest {
    private SailRepository repo;
    private RDFBeanManager manager;

    @Before
    public void setupManager() throws Exception {
        repo = new SailRepository(new MemoryStore());
        // repo.initialize();
        RepositoryConnection initialFillConn = repo.getConnection();
        initialFillConn.add(getClass().getResourceAsStream("listUnmarshal.ttl"), "", RDFFormat.TURTLE);
        initialFillConn.close();
        manager = new RDFBeanManager(repo);
    }

    @Test
    public void decodeHeadTailList() throws RDF4JException, RDFBeanException, URISyntaxException {
        DatatypeTestClass data =
        manager.get(repo.getValueFactory().createIRI("http://example.com/list/dataClass"), DatatypeTestClass.class);
        assertThat(data, notNullValue());
        List list = data.getListValue();
        assertThat(list, notNullValue());
        assertThat(list.size(), is(3));
        assertThat(list.get(0), is(URI.class));
        assertThat((URI) list.get(0), equalTo(new URI("http://example.com/list/first")));
        assertThat((URI) list.get(list.size()-1), equalTo(new URI("http://example.com/list/last")));
    }

    @Test
    public void encodeEmptyHeadTailList() throws RepositoryException, RDFBeanException {
        DatatypeTestClass data = new DatatypeTestClass();
        data.setHeadTailList(new ArrayList<>());
        Resource marshaled = manager.add(data);
        RepositoryConnection checkConn = repo.getConnection();
        IRI property = checkConn.getValueFactory().createIRI("http://cyberborean.org/rdfbeans/2.0/test/datatype/headTailList");
        RepositoryResult<Statement> listStatement = checkConn.getStatements(marshaled, property, null, false);
        try {
            assertTrue("A list statement is generated", listStatement.hasNext());
            Value object = listStatement.next().getObject();
            assertThat(object, is(Resource.class));
            assertTrue("Empty List encodes as 'L rdf:rest rdf:nil'", checkConn.hasStatement(
                    (Resource)object, // the blank node of the list head
                    RDF.REST,
                    RDF.NIL,
                    false
            ));
            assertFalse("No further statements are generated", listStatement.hasNext());
        } finally {
            listStatement.close();
            checkConn.close();
            manager.delete(marshaled);
        }
    }

    @Test
    public void encodeHeadTailList() throws RepositoryException, RDFBeanException, URISyntaxException {
        DatatypeTestClass data = new DatatypeTestClass();
        List<Object> elements = Arrays.asList((Object)new URI("http://example.com/first"), (Object)new URI("http://example.com/last"));
        data.setHeadTailList(elements);
        Resource marshaled = manager.add(data);
        RepositoryConnection checkConn = repo.getConnection();
        try {
            IRI property = checkConn.getValueFactory().createIRI("http://cyberborean.org/rdfbeans/2.0/test/datatype/headTailList");
            RepositoryResult<Statement> listStatement = checkConn.getStatements(marshaled, property, null, false);
            Resource listHead;
            try {
                assertTrue("A list statement is generated", listStatement.hasNext());
                listHead = (Resource)listStatement.next().getObject();
                assertFalse("No further list heads are generated", listStatement.hasNext());
            } finally {
                listStatement.close();
            }
            assertTrue("First element is encoded as L rdf:first ex:first'", checkConn.hasStatement(
                    listHead,
                    RDF.FIRST,
                    checkConn.getValueFactory().createIRI(elements.get(0).toString()),
                    false
            ));
            assertFalse("List does not end after first element", checkConn.hasStatement(
                    listHead,
                    RDF.REST,
                    RDF.NIL,
                    false
            ));
            RepositoryResult<Statement> tailStatements = checkConn.getStatements(listHead, RDF.REST, null, false);
            Resource listTail;
            try {
                assertTrue("A tail statement is generated", tailStatements.hasNext());
                listTail = (Resource)tailStatements.next().getObject();
                assertFalse("No further list tails are generated", tailStatements.hasNext());
            } finally {
                tailStatements.close();
            }
            assertTrue("Second element is encoded as <rest> rdf:first ex:last'", checkConn.hasStatement(
                    listTail,
                    RDF.FIRST,
                    checkConn.getValueFactory().createIRI(elements.get(1).toString()),
                    false
            ));
            assertTrue("List ends after second element", checkConn.hasStatement(
                    listTail,
                    RDF.REST,
                    RDF.NIL,
                    false
            ));
        } finally {
            checkConn.close();
        }
        manager.delete(marshaled);
    }

    @After
    public void teardownManager() throws Exception {
        manager.close();
        repo.shutDown();
    }
}

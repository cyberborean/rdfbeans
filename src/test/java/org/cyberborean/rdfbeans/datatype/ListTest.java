package org.cyberborean.rdfbeans.datatype;


import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.entities.DatatypeTestClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;
import java.net.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ListTest {
    private SailRepository repo;
    private RDFBeanManager manager;

    @Before
    public void setupManager() throws Exception {
        repo = new SailRepository(new MemoryStore());
        repo.initialize();
        RepositoryConnection initialFillConn = repo.getConnection();
        initialFillConn.add(getClass().getResourceAsStream("listUnmarshal.ttl"), "", RDFFormat.TURTLE);
        initialFillConn.close();
        manager = new RDFBeanManager(repo.getConnection());
    }

    @Test
    public void decodeHeadTailList() throws OpenRDFException, RDFBeanException, URISyntaxException {
        DatatypeTestClass data =
        manager.get(repo.getValueFactory().createURI("http://example.com/list/dataClass"), DatatypeTestClass.class);
        assertThat(data, notNullValue());
        List list = data.getListValue();
        assertThat(list, notNullValue());
        assertThat(list.size(), is(3));
        assertThat(list.get(0), is(URI.class));
        assertThat((URI) list.get(0), equalTo(new URI("http://example.com/list/first")));
        assertThat((URI) list.get(list.size()-1), equalTo(new URI("http://example.com/list/last")));
    }

    @After
    public void teardownManager() throws Exception {
        manager.getRepositoryConnection().close();
        repo.shutDown();
    }
}

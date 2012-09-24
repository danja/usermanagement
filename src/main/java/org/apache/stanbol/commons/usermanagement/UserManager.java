package org.apache.stanbol.commons.usermanagement;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.PLATFORM;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.ldviewable.LdViewable;

@Component
@Service(UserManager.class)
@Path("user-management")
public class UserManager {
	@Reference(target = SystemConfig.SYSTEM_GRAPH_FILTER)
	private LockableMGraph systemGraph;
	
	@GET
	public String index() {
		return "hello";
	}
	
	public GraphNode foo() {
		GraphNode graphNode = new GraphNode(new UriRef("http://foo/"), new SimpleMGraph());
		graphNode.addProperty(RDFS.label, new PlainLiteralImpl("That's the label", new Language("en")));
		return graphNode;
	}
	
	@GET
	@Path("foo2")
	public LdViewable foo2() {
		return new LdViewable("tests/test.ftl", foo());
	}
	
	@GET
	@Path("foo3")
	public LdViewable foo4() {
		return new LdViewable("test.ftl", foo(), this.getClass());
	}
	
	@GET
	@Path("edit-user")
	public GraphNode editUser(@QueryParam("userName") String userName) {
		GraphNode user = getUser(userName);
		MGraph resultGraph = new SimpleMGraph();
		GraphNode result = new GraphNode(user.getNode(), new UnionMGraph(resultGraph, user.getGraph()));
		result.addProperty(RDF.type, Ontology.EditableUser);
		return result;
	}
	
	@GET
	@Path("view-user") 
	public LdViewable viewUser(@QueryParam("userName") String userName) {
		return new LdViewable("EditableUser.ftl", getUser(userName), this.getClass());
	}
	
	private GraphNode getUser(@QueryParam("userName") String userName) {
		Iterator<Triple> iter = systemGraph.filter(null, PLATFORM.userName, new PlainLiteralImpl(userName));
		if (!iter.hasNext()) {
			return null;
		}
		return new GraphNode(iter.next().getSubject(), systemGraph);
	}
	
	public GraphNode getUserType() {
		return new GraphNode(FOAF.Agent, systemGraph);
	}
	
	public Set<GraphNode> getUsers() {
		return getResourcesOfType(FOAF.Agent);
	}
	
	private Set<GraphNode> getResourcesOfType(UriRef type) {
		Lock readLock = systemGraph.getLock().readLock();
		readLock.lock();
		try {
			final Iterator<Triple> triples = systemGraph.filter(null, RDF.type, type);
			Set<GraphNode> userRoles = new HashSet<GraphNode>();
			while (triples.hasNext()) {
				userRoles.add(new GraphNode(triples.next().getSubject(), systemGraph));
			}
			return userRoles;
		} finally {
			readLock.unlock();
		}
	}

}

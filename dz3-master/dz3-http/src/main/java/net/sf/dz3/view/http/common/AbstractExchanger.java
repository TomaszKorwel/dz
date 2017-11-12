package net.sf.dz3.view.http.common;

import java.net.URL;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import net.sf.dz3.view.http.v1.HttpConnector;
import net.sf.jukebox.service.ActiveService;

/**
 * The facilitator between the client {@link #send(Object) sending} data and the server
 * possibly returning some.
 * 
 * @param <DataBlock> Data type to send out to the server.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2017
 */
public abstract class AbstractExchanger<DataBlock> extends ActiveService {
    
    protected final HttpClient httpClient;
    protected final HttpHost targetHost;

    protected final URL serverContextRoot;

    protected final BlockingQueue<DataBlock> upstreamQueue;
    
    public AbstractExchanger(URL serverContextRoot, String username, String password, BlockingQueue<DataBlock> upstreamQueue) {
        
        this.serverContextRoot = serverContextRoot;
        this.targetHost = new HttpHost(
                serverContextRoot.getHost(),
                serverContextRoot.getPort(),
                serverContextRoot.getProtocol());

        this.httpClient = buildHttpClient(username, password);

        this.upstreamQueue = upstreamQueue;
    }

    private HttpClient buildHttpClient(String username, String password) {

        CredentialsProvider cp = new BasicCredentialsProvider();
        AuthScope authscope = new AuthScope(serverContextRoot.getHost(), serverContextRoot.getPort());
        Credentials credentials = new UsernamePasswordCredentials(username, password);

        cp.setCredentials(authscope, credentials);

        return HttpClientBuilder.create().setDefaultCredentialsProvider(cp).build();
    }

    @Override
    protected void startup() throws Throwable {

        logger.info("Using " + serverContextRoot);

        // Do absolutely nothing
    }

    /**
     * Keep sending data that appears in {@link HttpConnector#upstreamQueue} to the server,
     * and accepting whatever they have to say.
     * 
     * Exact strategy is determined by the implementation subclass.
     */
    protected abstract void execute() throws Throwable;

    @Override
    protected void shutdown() throws Throwable {

        // Do absolutely nothing
        // VT: FIXME: Tell the server that we're gone and invalidate the session?
    }
}

/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.servlet;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import com.hp.hpl.jena.sparql.util.Utils;

import org.squin.Constants;
import org.squin.cache.QueryResultCache;
import org.squin.cache.impl.QueryResultCacheImpl;
import org.squin.engine.LinkedDataCacheWrappingDataset;
import org.squin.engine.LinkTraversalBasedQueryEngine;
import org.squin.ldcache.jenaimpl.JenaIOBasedLinkedDataCache;


/**
 * The servlet that processes requests to the SQUIN service.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class DirectResultRequestServlet extends Servlet
{
	// members

	static private Logger logger = LoggerFactory.getLogger( DirectResultRequestServlet.class );


	// initialization

	public DirectResultRequestServlet ()
	{
	}


	// implementation of the HttpServlet interface

	protected void doGet ( HttpServletRequest req, HttpServletResponse resp )
	{
		QueryResultCache cache = null;
		if ( getConfig().getUseQueryResultCache() ) {
			cache = new QueryResultCacheImpl( getConfig().getPathOfQueryResultCache(),
			                                  getConfig().getMaxQueryResultCacheEntryDuration() );
		}

		logger.info( "Start processing request {} with {}.", req.hashCode(), getLinkedDataCache().toString() );
// logger.info( "real path: {}", getServletContext().getRealPath("WW") );
logger.info( "getInitialFilesDirectory: {}", getInitialFilesDirectory() );

		String accept = req.getHeader( "ACCEPT" );
		if (    accept != null
		     && ! accept.contains(Constants.MIME_TYPE_RESULT_XML)
		     && ! accept.contains(Constants.MIME_TYPE_XML1)
		     && ! accept.contains(Constants.MIME_TYPE_XML2)
		     && ! accept.contains(Constants.MIME_TYPE_RESULT_JSON)
		     && ! accept.contains(Constants.MIME_TYPE_JSON)
		     && ! accept.contains("application/*")
		     && ! accept.contains("*/*") )
		{
			logger.info( "NOT ACCEPTABLE for request {} (ACCEPT header field: {})", req.hashCode(), accept );

			try {
				resp.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, "Your client does not seem to accept one of the possible content types (e.g. '" + Constants.MIME_TYPE_RESULT_XML + "', '" + Constants.MIME_TYPE_RESULT_JSON + "')." );
			} catch ( IOException e ) {
				logger.error( "Sending the error reponse to request " + req.hashCode() + " caused a " + Utils.className(e) + ": " + e.getMessage(), e );
			}

			return;
		}

		// get (and check) the request parameters
		DirectResultRequestParameters params = new DirectResultRequestParameters ();
		if ( ! params.process(req) )
		{
			logger.info( "BAD REQUEST for request {}: {}", req.hashCode(), params.getErrorMsgs() );

			try {
				resp.sendError( HttpServletResponse.SC_BAD_REQUEST, params.getErrorMsgs() );
			} catch ( IOException e ) {
				logger.error( "Sending the error reponse to request " + req.hashCode() + " caused a " + Utils.className(e) + ": " + e.getMessage(), e );
			}

			return;
		}

		InputStream cachedResultSet = null;
		ResultSetMem resultSet = null;
		if ( ! params.getIgnoreQueryCache() && cache != null && cache.hasResults(params.getQueryString(),params.getResponseContentType()) )
		{
			logger.info( "Found cached result set for request {} with query: {}", req.hashCode(), params.getQueryString() );

			cachedResultSet = cache.getResults( params.getQueryString(), params.getResponseContentType() );
		}
		else
		{
			// execute the query
			logger.info( "Start executing request {} with query:", req.hashCode() );
			logger.info( params.getQueryString() );

			LinkTraversalBasedQueryEngine.register();
			JenaIOBasedLinkedDataCache ldcache = getLinkedDataCache();
			QueryExecution qe = QueryExecutionFactory.create( params.getQuery(),
			                                                  new LinkedDataCacheWrappingDataset(ldcache) );
			resultSet = new ResultSetMem( qe.execSelect() );

			logger.info( "Created the result set (size: {}) for request {}.", resultSet.size(), req.hashCode() );
		}

		// create the response
		OutputStream out = null;
		try {
			out = resp.getOutputStream();
		}
		catch ( IOException e )
		{
			logger.error( "Getting the response output stream for request " + req.hashCode() + " caused a " + Utils.className(e) + ": " + e.getMessage(), e );

			try {
				resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
			} catch ( IOException e2 ) {
				logger.error( "Sending the error reponse for request " + req.hashCode() + " caused a " + Utils.className(e2) + ": " + e2.getMessage(), e2 );
			}

			return;
		}

		// write the response
		resp.setContentType( params.getResponseContentType() );
		try
		{
			if ( cachedResultSet == null )
			{
				if ( params.getResponseContentType() == Constants.MIME_TYPE_RESULT_JSON ) {
					ResultSetFormatter.outputAsJSON( out, resultSet );
				} else {
					ResultSetFormatter.outputAsXML( out, resultSet );
				}
			}
			else {
				copy( cachedResultSet, out );
			}

			logger.info( "Result written to the response stream for request {}.", req.hashCode() );
		}
		catch ( Exception e )
		{
			logger.error( "Writing the model to the response stream for request {} caused a {}: {}", new Object[] {req.hashCode(),Utils.className(e),e.getMessage()} );
			try {
				resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
			} catch ( IOException e2 ) {
				logger.error( "Sending an error response for request " + req.hashCode() + " caused a " + Utils.className(e2) + ": " + e2.getMessage(), e2 );
			}
		}

		// finish
		try
		{
			out.flush();
			resp.flushBuffer();
			out.close();
			logger.info( "Response buffer for request {} flushed.", req.hashCode() );
		}
		catch ( IOException e )
		{
			logger.error( "Flushing the response buffer for request " + req.hashCode() + " caused a " + Utils.className(e) + ": " + e.getMessage(), e );
		}

		logger.info( "Finished processing request {} with {}.", req.hashCode(), getLinkedDataCache().toString() );

		if ( cache != null && cachedResultSet == null ) {
			cache.cacheResults( params.getQueryString(), resultSet );
		}
	}


	// helper methods

	static public void copy ( InputStream in, OutputStream out ) throws IOException
	{
		byte[] buffer = new byte[ 1024 ];
		int count = 0;
		int n = 0;
		while ( (n = in.read(buffer)) != -1 ) {
			out.write( buffer, 0, n );
		}
	}
}
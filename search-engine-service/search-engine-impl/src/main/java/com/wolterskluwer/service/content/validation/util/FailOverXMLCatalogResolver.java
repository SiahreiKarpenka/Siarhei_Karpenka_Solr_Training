package com.wolterskluwer.service.content.validation.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wolterskluwer.service.util.UrlHelper;

/**
 * XMLCatalogResolver that uses a fail over mechanism.
 * <p/>
 * The main reason for subclassing the Xerces XMLCatalogResolver is that it does
 * not work as required for schemas that load other schemas using "include" with
 * a schema location that should be resolved on the local file system.
 */
public class FailOverXMLCatalogResolver extends XMLCatalogResolver {
	private static final Logger LOG = LoggerFactory.getLogger(FailOverXMLCatalogResolver.class);

	public static FailOverXMLCatalogResolver initializeAndGetXmlCatalogResolver(List<String> catalogList) {
		return initializeAndGetXmlCatalogResolver(catalogList.toArray(new String[catalogList.size()]));
	}

	public static FailOverXMLCatalogResolver initializeAndGetXmlCatalogResolver(String... catalogList) {
		FailOverXMLCatalogResolver resolver = new FailOverXMLCatalogResolver();
		resolver.setCatalogList(catalogList);
		resolver.setUseLiteralSystemId(false);
		return resolver;
	}

	@Override
	public String resolveIdentifier(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
		String resolvedId = null;
		String publicId = resourceIdentifier.getPublicId();
		String systemId = getUseLiteralSystemId() ? resourceIdentifier.getLiteralSystemId() : resourceIdentifier
				.getExpandedSystemId();
		if (publicId != null && systemId != null) {
			// If both public id and (expanded) system id are available, use the
			// catalog to resolve them.
			resolvedId = resolvePublic(publicId, systemId);
		} else if (systemId != null) {
			// If no public is available, but (expanded) system id is available,
			// then use the system id bypassing the catalog.
			resolvedId = systemId;
		}

		// If resolved id is null or the resolved resource is not available,
		// then resolve name space using catalog.

		if (resolvedId == null || !uriRepresentsExistingFile(resolvedId)) {
			String namespace = resourceIdentifier.getNamespace();
			if (namespace != null) {
				resolvedId = resolveURI(namespace);
			}
		}

		if ((null != resolvedId) && resolvedId.toLowerCase().startsWith("http:")) {
			String msg = String
					.format("resolved to '%s' - Namespace '%s' - BaseSystemId '%s' - ExpandedSystemId '%s' - LiteralSystemId '%s' - PublicId '%s'",
							resolvedId, resourceIdentifier.getNamespace(), resourceIdentifier.getBaseSystemId(),
							resourceIdentifier.getExpandedSystemId(), resourceIdentifier.getLiteralSystemId(),
							resourceIdentifier.getPublicId());
			LOG.warn(msg);
		} else if (null == resolvedId) {
			String msg = String
					.format("NOT RESOLVED - Namespace '%s' - BaseSystemId '%s' - ExpandedSystemId '%s' - LiteralSystemId '%s' - PublicId '%s'",
							resourceIdentifier.getNamespace(), resourceIdentifier.getBaseSystemId(),
							resourceIdentifier.getExpandedSystemId(), resourceIdentifier.getLiteralSystemId(),
							resourceIdentifier.getPublicId());
			LOG.warn(msg);
		}

		return resolvedId;
	}

	public boolean uriRepresentsExistingFile(String uri) {
		try {
			URL url = new URL(uri);
			File file = UrlHelper.getFile(url);
			return file != null && file.exists();
		} catch (MalformedURLException e) {
			return false;
		}
	}

}

/*******************************************************************************
* Copyright (c) 2006, 2008 Steffen Pingel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package net.sf.webissues.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @author Shawn Minto
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class WebIssuesImages {

	private static final URL baseURL = WebIssuesUiPlugin.getDefault().getBundle().getEntry("/icons/");

	public static final String T_VIEW = "eview16";

	public static final ImageDescriptor OVERLAY_BUGS = create(T_VIEW, "overlay-bugs.gif");
	public static final ImageDescriptor OVERLAY_FEATURES = create(T_VIEW, "overlay-features.gif");
    public static final ImageDescriptor ALL = create(T_VIEW, "all.gif");

	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (baseURL == null) {
			throw new MalformedURLException();
		}

		StringBuilder buffer = new StringBuilder(prefix);
		if (prefix != "") {
			buffer.append('/');
		}
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}

}

package net.sf.webissues.ui;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.swt.graphics.Image;
import org.webissues.api.View;

@SuppressWarnings("restriction")
public class ViewLabelProvider implements ILabelProvider {

    public Image getImage(Object arg0) {
        if (arg0 instanceof View) {
            View view= (View) arg0;
            if (view.isPublicView()) {
                return CommonImages.getImage(WebIssuesImages.PEOPLE);
            } else {
                return CommonImages.getImage(WebIssuesImages.YOU);
            } 
        }
        return null;
    }

    public String getText(Object arg0) {
        if (arg0 instanceof View) {
            View view = (View) arg0;
            if (view.getId() == -1) {
                return "";
            }
            return view.getName();
        }
        return arg0.toString();
    }

    public void addListener(ILabelProviderListener arg0) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object arg0, String arg1) {
        return false;
    }

    public void removeListener(ILabelProviderListener arg0) {
    }

}
package net.sf.webissues.ui;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.swt.graphics.Image;
import org.webissues.api.Access;
import org.webissues.api.User;

@SuppressWarnings("restriction")
public class UserLabelProvider implements ILabelProvider {

    private User me;
    private String emptyUserText = "Nobody";
    private Image emptyUserIcon = CommonImages.getImage(WebIssuesImages.REMOVE);

    public UserLabelProvider(User me) {
        this.me = me;
    }

    public Image getImage(Object arg0) {
        if (arg0 instanceof User) {
            User owner = (User) arg0;
            if (owner.getId() == -1) {
                return emptyUserIcon;
            } else if (owner.equals(me)) {
                return CommonImages.getImage(WebIssuesImages.YOU);
            } else if (owner.getAccess().equals(Access.ADMIN)) {
                return CommonImages.getImage(WebIssuesImages.ADMIN);
            } else {
                return CommonImages.getImage(WebIssuesImages.PERSON);
            }
        }
        return null;
    }

    public void setEmptyUserIcon(Image emptyUserIcon) {
        this.emptyUserIcon = emptyUserIcon;
    }

    public void setEmptyUserText(String emptyUserText) {
        this.emptyUserText = emptyUserText;
    }

    public String getText(Object arg0) {
        if (arg0 instanceof User) {
            User owner = (User) arg0;
            if (owner.getId() == -1) {
                return emptyUserText;
            }
            return owner.getName();
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
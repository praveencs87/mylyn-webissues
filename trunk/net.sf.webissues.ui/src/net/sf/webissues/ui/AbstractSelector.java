package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.NamedEntity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractSelector<T extends NamedEntity> extends Composite {

    public static final String LABEL_CHOOSE = "Choose...";

    private Text itemText;

    private Button pickButton;

    private final List<T> items = new ArrayList<T>();

    private final List<SelectionListener> pickerListeners = new LinkedList<SelectionListener>();

    private String initialText = LABEL_CHOOSE;

    private final boolean allowMultiple;

    private final Environment environment;

    public AbstractSelector(Environment environment, Composite parent, int style, String initialText, boolean allowMultiple) {
        super(parent, style);
        this.environment = environment;
        this.initialText = initialText;
        this.allowMultiple = allowMultiple;
        initialize((style & SWT.FLAT) != 0 ? SWT.FLAT : 0);
    }

    private void initialize(int style) {
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        this.setLayout(gridLayout);

        itemText = new Text(this, style);
        GridData dateTextGridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        dateTextGridData.grabExcessHorizontalSpace = true;
        dateTextGridData.verticalAlignment = SWT.FILL;
        itemText.setEditable(false);

        itemText.setLayoutData(dateTextGridData);
        itemText.setText(initialText);
        itemText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                notifyPickerListeners();
            }
        });

        itemText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // items.clear();
                // T item = getItemForText(itemText.getText());
                // if (item != null) {
                // items.add(item);
                // }
                // updatePickerText();

            }
        });

        pickButton = new Button(this, style | SWT.ARROW | SWT.DOWN);
        GridData pickButtonGridData = new GridData(SWT.RIGHT, SWT.FILL, false, true);
        pickButtonGridData.verticalIndent = 0;
        pickButton.setLayoutData(pickButtonGridData);
        pickButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Shell shell = null;
                if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                    shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                } else {
                    shell = new Shell(PlatformUI.getWorkbench().getDisplay());
                }
                pickButton.setEnabled(false);
                itemText.setEnabled(false);
                showPicker(shell);
            }
        });

        pack();
    }

    public abstract T getItemForText(String text);

    public void addPickerSelectionListener(SelectionListener listener) {
        pickerListeners.add(listener);
    }

    @Override
    public void setBackground(Color backgroundColor) {
        itemText.setBackground(backgroundColor);
        pickButton.setBackground(backgroundColor);
        super.setBackground(backgroundColor);
    }

    public void setItems(Collection<T> items) {
        this.items.clear();
        this.items.addAll(items);
        updatePickerText();
    }

    public void setItemNames(Collection<String> itemNames) {
        clearItems();
        for (String name : itemNames) {
            T itemForText = getItemForText(name);
            if (itemForText != null) {
                addItem(itemForText);
            }
        }
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    protected abstract void showPicker(Shell shell);

    /** Called when the user has selected a component */
    @SuppressWarnings("unchecked")
    protected void componentSelected(boolean canceled, Object[] selectedComponents) {
        if (!canceled) {
            items.clear();
            if (selectedComponents != null) {
                for (Object sel : selectedComponents) {
                    if (sel == null) {
                        throw new Error("Null selection");
                    }
                    items.add((T) sel);
                }
            }
            updatePickerText();
            notifyPickerListeners();
        }

        pickButton.setEnabled(true);
        itemText.setEnabled(true);
    }

    private void notifyPickerListeners() {
        for (SelectionListener listener : pickerListeners) {
            listener.widgetSelected(null);
        }
    }

    private void updatePickerText() {
        if (items.size() == 1) {
            T t = items.get(0);
            itemText.setText(t.getName());
        } else if (items.size() > 1) {
            itemText.setText(items.size() + " items");
        } else {
            itemText.setEnabled(false);
            itemText.setText(LABEL_CHOOSE);
            itemText.setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        itemText.setEnabled(enabled);
        pickButton.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    protected Environment getEnvironment() {
        return environment;
    }

    public void clearItems() {
        items.clear();
        updatePickerText();
    }

    public void addItem(T component) {
        if (component == null) {
            throw new IllegalArgumentException("Item may not be null");
        }
        items.add(component);
        updatePickerText();
    }

    public Collection<T> getItems() {
        return items;
    }

    public void setItem(T component) {
        if (component == null) {
            throw new IllegalArgumentException("Item may not be null");
        }
        items.clear();
        items.add(component);
        updatePickerText();
    }

}

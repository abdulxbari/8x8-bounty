/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.media.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPanel.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ContactList</tt> is a JList that represents the contact list. A
 * custom data model and a custom list cell renderer is used. This class
 * manages all meta contact list events, like <code>metaContactAdded</code>,
 * <code>metaContactMoved</code>, <code>metaContactGroupAdded</code>, etc. 
 *
 * @author Yana Stamcheva
 */
public class ContactList extends JList
    implements  MetaContactListListener,
                MouseListener {
    
    private Logger logger = Logger.getLogger(ContactList.class.getName()); 

    private MetaContactListService contactList;

    private ContactListModel listModel;

    private MetaContact currentlySelectedContact;

    private Vector contactListListeners = new Vector();
    
    private Vector excContactListListeners = new Vector();
    
    private MainFrame mainFrame;
    /**
     * Creates an instance of the <tt>ContactList</tt>.
     *
     * @param contactList The related meta contactlist.
     */
    public ContactList(MainFrame mainFrame) {

        this.mainFrame = mainFrame;
        
        this.contactList = mainFrame.getContactList();

        this.listModel = new ContactListModel(contactList);
        
        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.contactList.addMetaContactListListener(this);

        this.addMouseListener(this);
        
        this.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (getSelectedValue() instanceof MetaContact) {
                    currentlySelectedContact = (MetaContact) getSelectedValue();
                }
            }
        });
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>.
     * Refreshes the list model.
     */
    public void metaContactAdded(MetaContactEvent evt) {
        int index = this.listModel.indexOf(evt.getSourceMetaContact());

        this.listModel.contentAdded(index, index);
    }

    /**
     * Handles the <tt>MetaContactRenamedEvent</tt>.
     * Refreshes the list when a meta contact is renamed.
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been added.
     */
    public void protoContactAdded(ProtoContactEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been removed.
     */
    public void protoContactRemoved(ProtoContactEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>ProtoContactEvent</tt>.
     * Refreshes the list when a protocol contact has been moved.
     */
    public void protoContactMoved(ProtoContactEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactEvent</tt>.
     * Refreshes the list when a meta contact has been removed.
     */
    public void metaContactRemoved(MetaContactEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactMovedEvent</tt>.
     * Refreshes the list when a meta contact has been moved.
     */
    public void metaContactMoved(MetaContactMovedEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list model when a new meta contact group has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {
        MetaContactGroup sourceGroup = evt.getSourceMetaContactGroup();

        this.groupAdded(sourceGroup);

        //this.ensureIndexIsVisible(0);
        
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list when a meta contact group has been modified.
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list when a meta contact group has been removed.
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {
        this.revalidate();
        this.repaint();
    }

    /**
     * Handles the <tt>MetaContactGroupEvent</tt>.
     * Refreshes the list model when the contact list groups has been
     * reordered. Moves the selection index to the index of the contact
     * that was selected before the reordered event. This way the selection
     * depends on the contact and not on the index.
     */
    public void childContactsReordered(MetaContactGroupEvent evt) {

        MetaContactGroup group = evt.getSourceMetaContactGroup();

        int startIndex = this.listModel.indexOf(group.getMetaContact(0));
        int endIndex = this.listModel.indexOf(group.getMetaContact(group
                .countChildContacts() - 1));

        this.listModel.contentChanged(startIndex, endIndex);

        if (currentlySelectedContact != null)
            this.setSelectedValue(currentlySelectedContact, false);
    }

    /**
     * Refreshes the list model when a group is added.
     *
     * @param group The group which is added.
     */
    private void groupAdded(MetaContactGroup group) {

        int index = this.listModel.indexOf(group);

        this.listModel.contentAdded(index, index);

        Iterator childContacts = group.getChildContacts();

        while (childContacts.hasNext()) {
            MetaContact contact = (MetaContact) childContacts.next();

            int contactIndex = this.listModel.indexOf(contact);
            this.listModel.contentAdded(contactIndex, contactIndex);
        }

        Iterator subGroups = group.getSubgroups();

        while (subGroups.hasNext()) {
            MetaContactGroup subGroup = (MetaContactGroup) subGroups.next();

            this.groupAdded(subGroup);
        }
    }

    /**
     * Returns the next list element that starts with
     * a prefix.
     *
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either
     * Position.Bias.Forward or Position.Bias.Backward.
     * @return the index of the next list element that
     * starts with the prefix; otherwise -1
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ContactListModel model = (ContactListModel) this.getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();

        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            Object o = model.getElementAt(index);

            if (o != null) {
                String contactName = null;

                if (o instanceof MetaContact) {
                    contactName = ((MetaContact) o).getDisplayName()
                            .toUpperCase();
                }

                if (contactName != null && contactName.startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /**
     * Returns the list of all groups. 
     * @return The list of all groups.
     */
    public Iterator getAllGroups() {        
        return contactList.getRoot().getSubgroups();
    }
    
    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     * 
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID) {
        Iterator i = contactList.getRoot().getSubgroups();
        while(i.hasNext()){
            MetaContactGroup group = (MetaContactGroup)i.next();
            
            if(group.getMetaUID().equals(metaUID)) {
                return group;
            }
        }
        return null;
    }
    
    /**
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addContactListListener(ContactListListener listener)
    {
        synchronized (contactListListeners)
        {
            if(!contactListListeners.contains(listener))
                this.contactListListeners.add(listener);
        }
    }
    
    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeContactListListener(ContactListListener listener)
    {
        synchronized (contactListListeners)
        {
            this.contactListListeners.remove(listener);
        }
    }
    
    /**
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addExcContactListListener(ContactListListener listener)
    {
        synchronized (excContactListListeners)
        {
            if(!excContactListListeners.contains(listener))
                this.excContactListListeners.add(listener);
        }
    }
    
    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeExcContactListListener(ContactListListener listener)
    {
        synchronized (excContactListListeners)
        {
            this.excContactListListeners.remove(listener);
        }
    }
    
    /**
     * Creates the corresponding ContactListEvent and notifies all
     * <tt>ContactListListener</tt>s that a contact is selected.
     *
     * @param sourceContact the contact that this event is about. 
     * @param eventID the id indicating the exact type of the event to fire.
     */
    public void fireContactListEvent(MetaContact sourceContact,
            int eventID)
    {
        ContactListEvent evt
            = new ContactListEvent(sourceContact, eventID);
        
        if(excContactListListeners.size() > 0) {
            synchronized (excContactListListeners)
            {
                Iterator listeners = new Vector( this.excContactListListeners)
                    .iterator();
    
                while (listeners.hasNext())
                {
                    ContactListListener listener
                        = (ContactListListener) listeners.next();
                    switch (evt.getEventID())
                    {
                        case ContactListEvent.CONTACT_SELECTED:
                            listener.contactSelected(evt);
                            break;
                        case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                            listener.protocolContactSelected(evt);
                            break;
                        default:
                            logger.error("Unknown event type "
                                        + evt.getEventID());
                    }
                }
            }
        }
        else 
        {
            synchronized (contactListListeners)
            {
                Iterator listeners = this.contactListListeners
                    .iterator();
    
                while (listeners.hasNext())
                {
                    ContactListListener listener
                        = (ContactListListener) listeners.next();
                    switch (evt.getEventID())
                    {
                        case ContactListEvent.CONTACT_SELECTED:
                            listener.contactSelected(evt);
                            break;
                        case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                            listener.protocolContactSelected(evt);
                            break;
                        default:
                            logger.error("Unknown event type "
                                        + evt.getEventID());
                    }
                }
            }
        }
    }

    /**
     * Creates the corresponding ContactListEvent and notifies all
     * <tt>ContactListListener</tt>s that a contact is selected.
     *
     * @param sourceContact the contact that this event is about
     * @param protocolContact the protocol contact the this event is about 
     * @param eventID the id indicating the exact type of the event to fire.
     */
    public void fireContactListEvent(MetaContact sourceContact,
            Contact protocolContact,
            int eventID)
    {
        ContactListEvent evt
        = new ContactListEvent(sourceContact, protocolContact, eventID);
    
        synchronized (contactListListeners)
        {
            Iterator listeners = this.contactListListeners
                .iterator();
    
            while (listeners.hasNext())
            {
                ContactListListener listener
                    = (ContactListListener) listeners.next();
                switch (evt.getEventID())
                {
                    case ContactListEvent.CONTACT_SELECTED:
                        listener.contactSelected(evt);
                        break;
                    case ContactListEvent.PROTOCOL_CONTACT_SELECTED:
                        listener.protocolContactSelected(evt);
                        break;
                    default:
                        logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
    }
    
    /**
     * Closes or opens a group on a double click.
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {

            int selectedIndex = this.locationToIndex(e.getPoint());

            ContactListModel listModel = (ContactListModel) this.getModel();

            Object element = listModel.getElementAt(selectedIndex);

            if (element instanceof MetaContactGroup) {

                MetaContactGroup group = (MetaContactGroup) element;

                if (listModel.isGroupClosed(group)) {
                    listModel.openGroup(group);
                } else {
                    listModel.closeGroup(group);
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e)
    {}
    
    public void mouseExited(MouseEvent e)
    {}

    /**
     * Manages a mouse press over the contact list. 
     * 
     * When the left mouse button is pressed on a contact cell different things
     * may happen depending on the contained component under the mouse. If the
     * mouse is pressed on the "contact name" the chat window is opened, 
     * configured to use the default protocol contact for the selected
     * MetaContact. If the mouse is pressed on one of the protocol icons, the
     * chat window is opened, configured to use the protocol contact
     * corresponding to the given icon.
     * 
     * When the right mouse button is pressed on a contact cell, the cell is
     * selected and the <tt>ContactRightButtonMenu</tt> is opened.
     * 
     * When the right mouse button is pressed on a group cell, the cell is
     * selected and the <tt>GroupRightButtonMenu</tt> is opened.
     * 
     * When the middle mouse button is pressed on a cell, the cell is selected.
     */
    public void mousePressed(MouseEvent e) {
        // Select the contact under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
                || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown())) {
            this.setSelectedIndex(locationToIndex(e.getPoint()));
        }
        
        int selectedIndex = this.getSelectedIndex();
        Object selectedValue = this.getSelectedValue();

        ContactListCellRenderer renderer 
            = (ContactListCellRenderer) 
                this.getCellRenderer().getListCellRendererComponent(
                        this, selectedValue, selectedIndex, true,
                        true);

        Point selectedCellPoint = this.indexToLocation(selectedIndex);

        int translatedX = e.getX() - selectedCellPoint.x;

        int translatedY = e.getY() - selectedCellPoint.y;
        
        if(selectedValue instanceof MetaContactGroup) {
            MetaContactGroup group = (MetaContactGroup) selectedValue;
            
            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                    || (e.isControlDown() && !e.isMetaDown())) {
                
                GroupRightButtonMenu popupMenu
                    = new GroupRightButtonMenu(mainFrame, group);

                SwingUtilities.convertPointToScreen(selectedCellPoint,
                        renderer);

                popupMenu.setInvoker(this);

                popupMenu.setLocation(selectedCellPoint.x,
                        selectedCellPoint.y + renderer.getHeight());

                popupMenu.setVisible(true);
            }
        }
        
        // Open message window, right button menu or contact info when
        // mouse is pressed. Distinguish on which component was pressed
        // the mouse and make the appropriate work.
        if (selectedValue instanceof MetaContact) {
            MetaContact contact = (MetaContact) selectedValue;
            
            //get the component under the mouse
            Component component = renderer.getComponentAt(translatedX,
                    translatedY);
            if (component instanceof JLabel) {
                //Right click and Ctrl+LeftClick on the contact label opens
                //Popup menu
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                        || (e.isControlDown() && !e.isMetaDown())) {
                    
                    ContactRightButtonMenu popupMenu
                        = new ContactRightButtonMenu(mainFrame, contact);

                    SwingUtilities.convertPointToScreen(selectedCellPoint,
                            renderer);

                    popupMenu.setInvoker(this);

                    popupMenu.setLocation(selectedCellPoint.x,
                            selectedCellPoint.y + renderer.getHeight());

                    popupMenu.setVisible(true);
                }
                //Left click on the contact label opens Chat window
                else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {                    
                    fireContactListEvent(contact,
                            ContactListEvent.CONTACT_SELECTED);
                }
            } 
            else if (component instanceof JButton) {                
                //Click on the info button opens the info popup panel
                SwingUtilities.invokeLater(new RunInfoWindow(selectedCellPoint,
                        contact));
            } 
            else if (component instanceof JPanel) {
                if(component.getName() != null
                        && component.getName().equals("buttonsPanel")){
                    JPanel panel = (JPanel) component;
    
                    int internalX = translatedX
                            - (renderer.getWidth() - panel.getWidth() - 2);
                    int internalY = translatedY
                            - (renderer.getHeight() - panel.getHeight());
    
                    Component c = panel.getComponentAt(4, 4);
    
                    if (c instanceof ContactProtocolButton) {
                        fireContactListEvent(contact,
                            ((ContactProtocolButton) c).getProtocolContact(),
                            ContactListEvent.PROTOCOL_CONTACT_SELECTED);
                    }
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e)
    {}
    
    /**
     * Runs the info window for the specified contact at the
     * appropriate position.
     *
     * @author Yana Stamcheva
     */
    private class RunInfoWindow implements Runnable {

        private MetaContact contactItem;

        private Point p;

        private RunInfoWindow(Point p, MetaContact contactItem) {

            this.p = p;
            this.contactItem = contactItem;
        }

        public void run() {

            ContactInfoPanel contactInfoPanel = new ContactInfoPanel(mainFrame,
                    contactItem);

            SwingUtilities.convertPointToScreen(p, ContactList.this);

            // TODO: to calculate popup window posititon properly.
            contactInfoPanel.setPopupLocation(p.x - 140, p.y - 15);

            contactInfoPanel.setVisible(true);

            contactInfoPanel.requestFocusInWindow();
        }
    }

}

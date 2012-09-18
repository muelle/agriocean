/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.content.export;

import org.dspace.content.Item;

/**
 * Interface IDCTextBasedExport
 * Declares methods a class must implement to convert a DSpace item into a String representation.
 * @author 
 */
public interface IDCTextBasedExport {
    /**
     * Gets a textual representation of a DSpace item.
     * @param item the DSpace item to represent in textual form.
     * @return the textual representation of the given item
     */
    public String export(Item item) throws NoDCTypeException, NoTargetBibtexTypeException, NoTargetBibtexFieldsException, TargetBibtexTypeInitException;
}

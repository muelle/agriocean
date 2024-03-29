/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.log4j.Logger;

import org.dspace.AbstractUnitTest;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * DCValue is a deprecated class with no methods (just a Data Object). This
 * class does no real testing, is just here for the sake of coberture completeness
 * and in case the class is refactored and requires some extra testing.
 * @author pvillega
 */
public class DCValueTest extends AbstractUnitTest {

    /** log4j category */
    private final static Logger log = Logger.getLogger(DCValueTest.class);

    /**
     * Object to use in the tests
     */
    private DCValue dcval;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        dcval = new DCValue();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        dcval = null;
        super.destroy();
    }

    /**
     * Dummy test to avoid initialization errors
     */
    @Test
    public void testDummy() {
        assertTrue(true);
    }
}


//~ Formatted by Jindent --- http://www.jindent.com

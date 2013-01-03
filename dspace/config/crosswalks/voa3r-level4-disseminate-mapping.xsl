<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

    <xsl:variable name="mapping">
        <!-- MAP REVIEW STATUS: which internal dc.type.refereed value maps to which VOA3R review status -->
        <reviewstatus>
			<!-- Add an internal-value element for each internal value that should be mapped to this voa3r value -->
            <voa3r value="Non Reviewed">
                <internal-value>Non-Refereed</internal-value>
            </voa3r>
            <voa3r value="Peer Reviewed">
                <internal-value>Refereed</internal-value>
            </voa3r>
            <voa3r value="Accepted">
                <internal-value>Accepted</internal-value>
            </voa3r>
            <voa3r value="Rejected"></voa3r>
            <voa3r value="CommunityCommented"></voa3r>
            <voa3r value="CommunityRated"></voa3r>
        </reviewstatus>

        <!-- MAP PUBLICATION STATUS: which internal dc.description.status value maps to which VOA3R publication status -->
        <publicationstatus>
            <!-- Add an internal-value element for each internal value that should be mapped to this voa3r value -->
            <voa3r value="WorkingDraft">
                <internal-value>Unpublished</internal-value>
            </voa3r>
            <voa3r value="Final">
                <internal-value>In press</internal-value>
            </voa3r>
            <voa3r value="Submitted">
                <internal-value>Submitted</internal-value>
            </voa3r>
            <voa3r value="Published">
                <internal-value>Published</internal-value>
            </voa3r>
        </publicationstatus>


        <!-- MAP RESOURCE TYPE: which internal dc.type value maps to which VOA3R resource type -->
        <resourcetype>
            <!-- Add an internal-value element for each internal value that should be mapped to this voa3r value -->
            <voa3r value="Book"></voa3r>
            <voa3r value="Journal"></voa3r>
            <voa3r value="Conference Proceedings">
                <internal-value>
                    <type>Proceedings Paper</type>
                </internal-value>
            </voa3r>
            <voa3r value="Thesis">
                <internal-value>
                    <type>Theses and Dissertations</type>
                </internal-value>
            </voa3r>
            <voa3r value="Bachelor Thesis">
                <internal-value>
                    <type>Theses and Dissertations</type>
                    <subtype>Bachelor thesis</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Master Thesis">
                <internal-value>
                    <type>Theses and Dissertations</type>
                    <subtype>Master thesis</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Doctoral Thesis">
                <internal-value>
                    <type>Theses and Dissertations</type>
                    <subtype>Phd thesis</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Research Report">
                <internal-value>
                    <type>Report</type>
                </internal-value>
                <internal-value>
                    <type>Working Paper</type>
                </internal-value>
            </voa3r>
            <voa3r value="Magazine"></voa3r>
            <voa3r value="Standard"></voa3r>
            <voa3r value="Book Section">
                <internal-value>
                    <type>Book Section</type>
                </internal-value>
            </voa3r>
            <voa3r value="Journal Contribution">
                <internal-value>
                    <type>Journal Contribution</type>
                </internal-value>
            </voa3r>
            <voa3r value="Article">
                <internal-value>
                    <type>Journal Contribution</type>
                    <subtype>Article</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Review">
                <internal-value>
                    <type>Journal Contribution</type>
                    <subtype>Review</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Editorial">
                <internal-value>
                    <type>Journal Contribution</type>
                    <subtype>Editorial material</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Letter"></voa3r>
            <voa3r value="Meeting Abstract">
                <internal-value>
                    <type>Journal Contribution</type>
                    <subtype>Meeting abstract</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Note"></voa3r>
            <voa3r value="Conference Contribution">
                <internal-value>
                    <type>Conference Material</type>
                </internal-value>
            </voa3r>
            <voa3r value="Paper">
                <internal-value>
                    <type>Conference Material</type>
                    <subtype>Paper</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Poster">
                <internal-value>
                    <type>Conference Material</type>
                    <subtype>Poster</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Presentation">
                <internal-value>
                    <type>Conference Material</type>
                    <subtype>Presentation</subtype>
                </internal-value>
            </voa3r>
            <voa3r value="Magazine Article"></voa3r>
            <voa3r value="Learning Resource"></voa3r>
            <voa3r value="Multimedia Resource"></voa3r>
            <voa3r value="Data Set">
                <internal-value>
                    <type>Data set</type>
                </internal-value>
            </voa3r>
            <voa3r value="Conference"></voa3r>
            <voa3r value="Project"></voa3r>
            <voa3r value="Other">
                <internal-value>
                    <type>Map</type>
                </internal-value>
                <internal-value>
                    <type>Other</type>
                </internal-value>
            </voa3r>
        </resourcetype>

    </xsl:variable>
    
</xsl:stylesheet>

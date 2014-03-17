<%--

    Copyright (C) 2013
    by 52 North Initiative for Geospatial Open Source Software GmbH

    Contact: Andreas Wytzisk
    52 North Initiative for Geospatial Open Source Software GmbH
    Martin-Luther-King-Weg 24
    48155 Muenster, Germany
    info@52north.org

    This program is free software; you can redistribute and/or modify it under
    the terms of the GNU General Public License version 2 as published by the
    Free Software Foundation.

    This program is distributed WITHOUT ANY WARRANTY; even without the implied
    WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License along with
    this program (see gnu-gpl v2.txt). If not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
    visit the Free Software Foundation web page, http://www.fsf.org.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:include page="../common/header.jsp">
    <jsp:param name="activeMenu" value="admin" />
</jsp:include>

<jsp:include page="../common/logotitle.jsp">
    <jsp:param name="title" value="Test Data Panel" />
    <jsp:param name="leadParagraph" value="Here you can insert or delete test data." />
</jsp:include>

<h4>Warning</h4>
<p>Although test data insertion and deletion aims to not interfere with other data in
the database, no guarantee is provided. Insert/delete test data on production systems
at <strong>your own risk</strong>, and always <strong>back up the database</strong>
first!</p>

<p>Test data is identified by using "test" for the authority in asset URNs, e.g.
urn:ioos:network:test:all, urn:ioos:station:test:1, etc. All matching procedures
and their observations will be deleted on test data removal, and orphan records
of associated tables (features of interest, observable properties, etc) will be cleaned up.</p> 

<button id="testdata" type="button" class="btn btn-danger"></button>

<script type="text/javascript">
$(function() {
    var $button = $("#testdata");
    var hasTestData = ${hasTestData};

	function updateTestDataButton() {
	    if (hasTestData){
    		$button.removeAttr("disabled");
    	    $button.text("Remove test data");
	    } else {
	    	$button.removeAttr("disabled");
	        $button.text("Insert test data");
	    }
	}
	
	updateTestDataButton();
	
    function create() {
        $button.attr("disabled", true);
        $.ajax({
            "url": "<c:url value="/admin/testdata/create" />",
            "type": "POST"
        }).fail(function(error) {
            showError("Request failed: " + error.status + " " + error.statusText);
            $button.removeAttr("disabled");
        }).done(function() {
            showSuccess("Test data set was inserted.");
            hasTestData = true;
            updateTestDataButton();
        });
    }

    function remove() {
        $button.attr("disabled", true);
        $.ajax({
            "url": "<c:url value="/admin/testdata/remove" />",
            "type": "POST"
        }).fail(function(error) {
            if (error.responseText) {
                showError(error.responseText);
            } else {
                showError("Request failed: " + error.status + " " + error.statusText);
            }
            $button.removeAttr("disabled");
        }).done(function() {
            showSuccess("The test data was removed.");
            hasTestData = false;
            updateTestDataButton();
        });
    }
	
    $button.click(function() {
        if (hasTestData) {
            remove();
        } else {
            create();
        }
    });
});
</script>

<jsp:include page="../common/footer.jsp" />

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
<div>
	<h3>52&deg;North SOS - IOOS Build</h3>
    <small><a target="_blank" href="<c:url value="/sos/kvp?service=SOS&request=GetCapabilities&AcceptVersions=1.0.0" />">GetCapabilities (v1)</a> 
    &#8226; 
    Version: ${project.version} 
    &#8226;  
    Build time: ${timestamp}</small>
</div>
<div class="row">
	<div class="span9">
		<h2>${param.title}</h2>
		<p class="lead">${param.leadParagraph}</p>
	</div>
	<div class="span3 header-img-span">
		<img src="<c:url value="/static/images/52n-logo-220x80.png"/>"/>
		<img style="padding:10px 0 10px 20px;" src="<c:url value="/static/images/ioos_logo.png"/>"/>		
	</div>
</div>

<div></div>

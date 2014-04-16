<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<div>
	<h3>52&deg;North SOS - IOOS Build</h3>
    <small><a target="_blank" href="<c:url value="/sos/kvp?service=SOS&request=GetCapabilities&AcceptVersions=1.0.0" />">GetCapabilities (v1)</a> 
    &#8226; 
    Version: ${project.version} 
    &#8226;  
    Build time: ${timestamp}</small>
</div>
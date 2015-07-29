# jsaga-adaptor-jocci

<h2>Requirements</h2>
- JDK 6+
- Maven
- JSAGA libs (v1.1.2+)
- jocci-api (v0.1.5)
- jocci-core (v0.1.4)

<h2>About the JSAGA Adaptor</h2>
Using the JSAGA adaptor for OCCI-compliant cloud middleware stacks supporting the OCCI standard, user can: 
- switching on the VM pre-installed with the required application, 
- establishing a secure connection to it signed using a digital “robot” certificate, 
- staging the input file(s) in the VM, 
- executing the application, 
- retrieving the output file(s) at the end of the computation and
- killing the VM.

This JSAGA adaptor uses jOCCI-api, a Java library developed by <a href="http://www.cesnet.cz/">CESNET</a>, to implement transport functions for rendered <a href="http://occi-wg.org/about/specification/">OCCI (Open Cloud Computing Interface)</a> queries. 
jOCCI-api is built on top of <a href="https://github.com/EGI-FCTF/jOCCI-core">jOCCI-core</a> and currently provides:
- HTTP transport functionality with set of authentication methods,
- basic requesting interface to easily communicate with OCCI servers.

<h2>Contribute</h2>
- Fork it
- Create a branch (git checkout -b my_markup)
- Commit your changes (git commit -am "My changes")
- Push to the branch (git push origin my_markup)
- Create an Issue with a link to your branch
 
<h2>Sponsors</h2>
<table border="0">
<tr>
<td>
<a href="http://www.infn.it/"><img width="150" src="http://www.infn.it/logo/weblogo1.gif" border="0" title="The INFN Home Page"></a>
</td>
<td></td>
<td>
<a href="http://software.in2p3.fr/jsaga"><img width="250" src="http://software.in2p3.fr/jsaga/latest-release/images/logo-jsaga.png" 
border="0" title="JSAGA"></a>
</td>
</tr>
</table>

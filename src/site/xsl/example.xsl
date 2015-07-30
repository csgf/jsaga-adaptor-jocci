<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml"/>

    <xsl:template match="/">
        <document><body>
            <section name="Configuration example">
                <p>Configuration used for integration tests of this module</p>
                <source>
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <xsl:copy-of select="*"/>
                    <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                </source>
            </section>
        </body></document>
    </xsl:template>
</xsl:stylesheet>